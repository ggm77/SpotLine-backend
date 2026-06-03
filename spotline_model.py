import pandas as pd
import numpy as np
import joblib
import os
import json
import argparse
import sys
from sklearn.model_selection import train_test_split, GridSearchCV, TimeSeriesSplit
from sklearn.linear_model import Ridge
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import mean_absolute_error, r2_score

SCALER_PATH = 'scaler.pkl'
MODEL_PATH = 'spotline_model.pkl'
FEATURES_PATH = 'spotline_features.pkl'


def load_and_preprocess_data(file_path):
    df = pd.read_csv(file_path)

    df['captured_at'] = pd.to_datetime(df['captured_at'])
    df = df.sort_values('captured_at').reset_index(drop=True)

    df['day_of_week'] = df['captured_at'].dt.dayofweek
    df['is_weekend'] = df['day_of_week'].apply(lambda x: 1 if x >= 5 else 0)

    df['prev_day_count'] = df['total_count'].shift(1)

    df_clean = df.dropna().reset_index(drop=True)
    return df_clean


def retrain_and_save_model(file_path='output.csv', verbose=False):
    """
    전체 데이터를 로드해 모델을 학습하고 scaler/model/features를 디스크에 저장합니다.
    Spring Boot CLI 호출 시 verbose=False로 stdout을 오염시키지 않습니다.
    """
    df_clean = load_and_preprocess_data(file_path)

    df_encoded = pd.get_dummies(df_clean, columns=['weather'], drop_first=True)

    features = ['temperature', 'is_weekend', 'prev_day_count']
    features += [col for col in df_encoded.columns if col.startswith('weather_')]

    X = df_encoded[features]
    y = df_encoded['total_count']

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, shuffle=False
    )

    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)

    param_grid = {
        'alpha': [0.01, 0.05, 0.1, 0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 50.0, 100.0]
    }

    n_samples = len(X_train)
    n_splits = 5 if n_samples >= 15 else (2 if n_samples >= 5 else 1)

    if n_splits > 1:
        tscv = TimeSeriesSplit(n_splits=n_splits)
        grid_search = GridSearchCV(
            estimator=Ridge(),
            param_grid=param_grid,
            scoring='neg_mean_absolute_error',
            cv=tscv
        )
        grid_search.fit(X_train_scaled, y_train)
        best_model = grid_search.best_estimator_
        best_alpha = grid_search.best_params_['alpha']
    else:
        best_model = Ridge(alpha=1.0)
        best_model.fit(X_train_scaled, y_train)
        best_alpha = 1.0

    y_pred = best_model.predict(X_test_scaled)
    mae = mean_absolute_error(y_test, y_pred)
    r2 = r2_score(y_test, y_pred)

    joblib.dump(scaler, SCALER_PATH)
    joblib.dump(best_model, MODEL_PATH)
    joblib.dump(features, FEATURES_PATH)

    if verbose:
        print("=================== 📊 배치 모델 재학습 및 저장 완료 ===================")
        print(f"데이터 파일 경로: {file_path}")
        print(f"총 학습 데이터 수: {len(df_clean)}행")
        print(f"최적의 규제 하이퍼파라미터 (Best alpha): {best_alpha}")
        print(f"Test 세트 평가 - 평균 절대 오차 (MAE): {mae:.2f} 명")
        print(f"Test 세트 평가 - 결정계수 (R² Score): {r2:.4f}")
        print("======================================================================\n")

    return best_model, scaler, features, mae


def _predict_single(temp, weather, prev_count, target_date, model, scaler, feature_columns):
    """Spring Boot 다일 연쇄 예측용 단일 날짜 추론 (출력 없음)"""
    day_of_week = target_date.dayofweek
    is_weekend = 1 if day_of_week >= 5 else 0

    input_data = {
        'temperature': temp,
        'is_weekend': is_weekend,
        'prev_day_count': prev_count
    }

    for col in feature_columns:
        if col.startswith('weather_'):
            weather_type = col.replace('weather_', '')
            input_data[col] = 1 if weather.upper() == weather_type.upper() else 0

    input_df = pd.DataFrame([input_data])
    for col in feature_columns:
        if col not in input_df.columns:
            input_df[col] = 0
    input_df = input_df[feature_columns]

    input_scaled = scaler.transform(input_df)
    pred_raw = model.predict(input_scaled)[0]
    return max(0, int(round(pred_raw)))


def predict_tomorrow_live(tomorrow_temp, tomorrow_weather, today_count):
    """
    [실시간 API 파이프라인] 저장된 모델 파일을 로드하여 내일 예측을 수행합니다.
    retrain_and_save_model()이 먼저 실행되어 있어야 합니다.
    """
    if not (os.path.exists(SCALER_PATH) and os.path.exists(MODEL_PATH) and os.path.exists(FEATURES_PATH)):
        raise FileNotFoundError("모델 파일이 없습니다. retrain_and_save_model()을 먼저 실행해주세요.")

    scaler = joblib.load(SCALER_PATH)
    model = joblib.load(MODEL_PATH)
    feature_columns = joblib.load(FEATURES_PATH)

    tomorrow_date = pd.Timestamp.now() + pd.Timedelta(days=1)
    day_of_week = tomorrow_date.dayofweek
    is_weekend = 1 if day_of_week >= 5 else 0

    input_data = {
        'temperature': tomorrow_temp,
        'is_weekend': is_weekend,
        'prev_day_count': today_count
    }

    for col in feature_columns:
        if col.startswith('weather_'):
            weather_type = col.replace('weather_', '')
            input_data[col] = 1 if tomorrow_weather.upper() == weather_type.upper() else 0

    input_df = pd.DataFrame([input_data])
    for col in feature_columns:
        if col not in input_df.columns:
            input_df[col] = 0
    input_df = input_df[feature_columns]

    input_scaled = scaler.transform(input_df)
    pred_raw = model.predict(input_scaled)[0]
    final_pred = max(0, int(round(pred_raw)))

    kor_days = ["월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일"]

    print(f"🔮 [SPOTLINE AI 내일 예측 보고서 (실시간 추론 모드)]")
    print(f"  - 예측 기준 일자: {tomorrow_date.strftime('%Y-%m-%d')} ({kor_days[day_of_week]})")
    print(f"  - 내일 최고 기온: {tomorrow_temp} ℃")
    print(f"  - 내일 기상 상태: {tomorrow_weather}")
    print(f"  - 오늘 최종 방문자수: {today_count} 명")
    print(f"--------------------------------------------------")
    print(f"👉 내일 삼겹살집 예상 방문객 수: 【 {final_pred} 명 】")
    return final_pred


def main():
    """
    Spring Boot에서 호출하는 CLI 진입점.
    retrain → 모델 저장 → 다일 연쇄 예측 후 stdout에 JSON 배열만 출력합니다.
    """
    parser = argparse.ArgumentParser()
    parser.add_argument('--csv', required=True, help='일별 집계 CSV 경로')
    parser.add_argument('--today-count', type=int, required=True, dest='today_count', help='오늘 실제 방문자 수')
    parser.add_argument('--days-json', required=True, dest='days_json',
                        help='예측 대상 날씨/기온 JSON 배열: [{"temp": float, "weather": str}, ...]')
    args = parser.parse_args()

    model, scaler, features, mae = retrain_and_save_model(args.csv, verbose=False)

    days = json.loads(args.days_json)
    results = []
    base_date = pd.Timestamp.now() + pd.Timedelta(days=1)
    prev_count = args.today_count

    for i, day in enumerate(days):
        target_date = base_date + pd.Timedelta(days=i)
        expected = _predict_single(day['temp'], day['weather'], prev_count, target_date, model, scaler, features)
        margin = max(5, int(round(mae)))
        results.append({
            'expected': expected,
            'min': max(0, expected - margin),
            'max': expected + margin
        })
        prev_count = expected

    print(json.dumps(results))


if __name__ == '__main__':
    if len(sys.argv) > 1:
        main()
    else:
        print("[1. 배치 학습 파이프라인 가동]")
        dataset_path = 'studio_results_20260603_1957.csv' if os.path.exists('studio_results_20260603_1957.csv') else 'output.csv'
        retrain_and_save_model(dataset_path, verbose=True)

        print("[2. 백엔드 API 실시간 예측 요청 처리 시나리오]")
        predict_tomorrow_live(
            tomorrow_temp=21.5,
            tomorrow_weather='RAINY',
            today_count=42
        )
