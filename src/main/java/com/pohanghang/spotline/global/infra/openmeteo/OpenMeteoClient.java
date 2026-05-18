package com.pohanghang.spotline.global.infra.openmeteo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pohanghang.spotline.domain.analytics.entity.Weather;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import com.pohanghang.spotline.global.infra.openmeteo.dto.OpenMeteoResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class OpenMeteoClient {

    private static final double SEOUL_LATITUDE = 37.5665;
    private static final double SEOUL_LONGITUDE = 126.9780;
    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final String FORECAST_API_HOST = "api.open-meteo.com";
    private static final String FORECAST_API_PATH = "/v1/forecast";
    private static final String HISTORICAL_API_HOST = "archive-api.open-meteo.com";
    private static final String HISTORICAL_API_PATH = "/v1/archive";

    private final WebClient openMeteoWebClient;

    public OpenMeteoClient(@Qualifier("openMeteoWebClient") final WebClient openMeteoWebClient) {
        this.openMeteoWebClient = openMeteoWebClient;
    }

    public WeatherData getSeoulWeatherData() {
        return getWeatherData(SEOUL_LATITUDE, SEOUL_LONGITUDE);
    }

    public WeatherData getSeoulWeatherData(final LocalDateTime targetAt) {
        return getWeatherData(SEOUL_LATITUDE, SEOUL_LONGITUDE, targetAt);
    }

    public WeatherData getWeatherData(final double latitude, final double longitude) {
        OpenMeteoResponseDto response = openMeteoWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/forecast")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("current", "temperature_2m,precipitation,weather_code")
                        .queryParam("timezone", "Asia/Seoul")
                        .build())
                .retrieve()
                .bodyToMono(OpenMeteoResponseDto.class)
                .block();

        if (response == null || response.current() == null) {
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        Double temperature = response.current().temperature2m();
        Double precipitation = response.current().precipitation();
        Integer weatherCode = response.current().weatherCode();

        Weather weather = mapWeatherCode(weatherCode);

        return new WeatherData(temperature, precipitation, weather);
    }

    public WeatherData getWeatherData(
            final double latitude,
            final double longitude,
            final LocalDateTime targetAt
    ) {
        if (targetAt == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        final LocalDate targetDate = targetAt.toLocalDate();
        OpenMeteoHourlyResponseDto response = openMeteoWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(resolveApiHost(targetDate))
                        .replacePath(resolveApiPath(targetDate))
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("hourly", "temperature_2m,precipitation,weather_code")
                        .queryParam("start_date", targetDate)
                        .queryParam("end_date", targetDate)
                        .queryParam("timezone", "Asia/Seoul")
                        .build())
                .retrieve()
                .bodyToMono(OpenMeteoHourlyResponseDto.class)
                .block();

        if (response == null || response.hourly() == null || response.hourly().time() == null) {
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        final int targetIndex = findTargetHourlyIndex(response.hourly(), targetAt);
        final Double temperature = getValue(response.hourly().temperature2m(), targetIndex, "temperature_2m");
        final Double precipitation = getValue(response.hourly().precipitation(), targetIndex, "precipitation");
        final Integer weatherCode = getValue(response.hourly().weatherCode(), targetIndex, "weather_code");

        return new WeatherData(temperature, precipitation, mapWeatherCode(weatherCode));
    }

    private String resolveApiHost(final LocalDate targetDate) {
        if (targetDate.isBefore(LocalDate.now(SEOUL_ZONE_ID))) {
            return HISTORICAL_API_HOST;
        }

        return FORECAST_API_HOST;
    }

    private String resolveApiPath(final LocalDate targetDate) {
        if (targetDate.isBefore(LocalDate.now(SEOUL_ZONE_ID))) {
            return HISTORICAL_API_PATH;
        }

        return FORECAST_API_PATH;
    }

    private int findTargetHourlyIndex(
            final OpenMeteoHourlyResponseDto.Hourly hourly,
            final LocalDateTime targetAt
    ) {
        final LocalDateTime targetHour = targetAt.withMinute(0).withSecond(0).withNano(0);
        for (int index = 0; index < hourly.time().size(); index++) {
            if (LocalDateTime.parse(hourly.time().get(index)).equals(targetHour)) {
                return index;
            }
        }

        throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
    }

    private <T> T getValue(
            final List<T> values,
            final int index,
            final String fieldName
    ) {
        if (values == null || values.size() <= index) {
            throw new CustomException(ExceptionCode.INTERNAL_SERVER_ERROR);
        }

        return values.get(index);
    }

    private Weather mapWeatherCode(final Integer code) {
        if (code == null) return Weather.SUNNY;

        return switch (code) {
            case 0 -> Weather.SUNNY;
            case 1, 2, 3, 45, 48 -> Weather.CLOUDY;
            case 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99 -> Weather.RAINY;
            case 71, 73, 75, 77, 85, 86 -> Weather.SNOW;
            default -> Weather.SUNNY;
        };
    }

    public record WeatherData(
            Double temperature,
            Double precipitation,
            Weather weather
    ) {}

    public record OpenMeteoHourlyResponseDto(
            Hourly hourly
    ) {
        public record Hourly(
                List<String> time,
                @JsonProperty("temperature_2m") List<Double> temperature2m,
                List<Double> precipitation,
                @JsonProperty("weather_code") List<Integer> weatherCode
        ) {
        }
    }
}
