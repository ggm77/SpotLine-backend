package com.pohanghang.spotline.domain.analytics.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pohanghang.spotline.domain.analytics.dto.PredictionTomorrowResponseDto;
import com.pohanghang.spotline.domain.analytics.entity.Weather;
import com.pohanghang.spotline.domain.analytics.model.AnalyticsRow;
import com.pohanghang.spotline.global.infra.openmeteo.OpenMeteoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

@Component
public class PythonPredictionClient {

    @Value("${prediction.python-bin:python3}")
    private String pythonBin;

    @Value("${prediction.script-path:spotline_model.py}")
    private String scriptPath;

    private final ObjectMapper objectMapper;

    public PythonPredictionClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<PredictionTomorrowResponseDto> predict(
            List<AnalyticsRow> rows,
            int todayCount,
            List<OpenMeteoClient.WeatherData> forecasts
    ) {
        Path csvPath = null;
        try {
            csvPath = writeDailyCsv(rows);
            String daysJson = buildDaysJson(forecasts);

            Process process = new ProcessBuilder(
                    pythonBin, scriptPath,
                    "--csv", csvPath.toString(),
                    "--today-count", String.valueOf(todayCount),
                    "--days-json", daysJson
            ).start();

            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();

            return parseResult(output);
        } catch (Exception e) {
            throw new RuntimeException("Python 예측 모델 호출 실패: " + e.getMessage(), e);
        } finally {
            if (csvPath != null) {
                try { Files.deleteIfExists(csvPath); } catch (IOException ignored) {}
            }
        }
    }

    // VisionData 스냅샷(복수)을 날짜별로 합산해 CSV로 변환
    private Path writeDailyCsv(List<AnalyticsRow> rows) throws IOException {
        TreeMap<LocalDate, int[]> dailyCount = new TreeMap<>();
        Map<LocalDate, Weather> dailyWeather = new HashMap<>();
        Map<LocalDate, Double> dailyTemp = new HashMap<>();

        for (AnalyticsRow row : rows) {
            if (row.getStartAt() == null || row.getTotalCount() == null) continue;
            LocalDate date = row.getStartAt().toLocalDate();
            dailyCount.computeIfAbsent(date, d -> new int[]{0})[0] += row.getTotalCount();
            if (row.getWeather() != null) dailyWeather.put(date, row.getWeather());
            if (row.getTemperature() != null) dailyTemp.put(date, row.getTemperature());
        }

        Path path = Files.createTempFile("spotline_", ".csv");
        StringBuilder sb = new StringBuilder("captured_at,total_count,weather,temperature\n");
        for (Map.Entry<LocalDate, int[]> entry : dailyCount.entrySet()) {
            LocalDate date = entry.getKey();
            sb.append(date).append(",")
              .append(entry.getValue()[0]).append(",")
              .append(dailyWeather.getOrDefault(date, Weather.SUNNY).name()).append(",")
              .append(dailyTemp.getOrDefault(date, 15.0)).append("\n");
        }
        Files.writeString(path, sb.toString());
        return path;
    }

    private String buildDaysJson(List<OpenMeteoClient.WeatherData> forecasts) throws Exception {
        List<Map<String, Object>> days = forecasts.stream()
                .map(f -> Map.<String, Object>of(
                        "temp", f.temperature() != null ? f.temperature() : 15.0,
                        "weather", f.weather().name()
                ))
                .toList();
        return objectMapper.writeValueAsString(days);
    }

    private List<PredictionTomorrowResponseDto> parseResult(String json) throws Exception {
        List<Map<String, Object>> parsed = objectMapper.readValue(
                json, new TypeReference<>() {}
        );
        return parsed.stream()
                .map(m -> new PredictionTomorrowResponseDto(
                        ((Number) m.get("expected")).intValue(),
                        ((Number) m.get("min")).intValue(),
                        ((Number) m.get("max")).intValue()
                ))
                .toList();
    }
}
