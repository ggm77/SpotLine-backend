package com.pohanghang.spotline.global.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        // 내부적 설정을 한 곳에서 관리 (싱글톤)
        OBJECT_MAPPER.registerModule(new JavaTimeModule()); // LocalDateTime 등 자바8 이후 시간 API 지원
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // JSON에 있고 객체에 없는 필드 무시
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); // 날짜를 타임스탬프 배열 대신 ISO-8601 문자열로 저장
    }

    private JsonUtil() {
        // 인스턴스화 방지
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 객체를 JSON 문자열로 직렬화합니다.
     */
    public static String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 중 오류가 발생했습니다. 대상 객체: " + obj.getClass().getName(), e);
        }
    }

    /**
     * JSON 문자열을 단일 객체로 역직렬화합니다.
     */
    public static <T> T toObject(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 역직렬화 중 오류가 발생했습니다. 대상 클래스: " + clazz.getName(), e);
        }
    }

    /**
     * JSON 문자열을 List, Map 등 제네릭 컬렉션 객체로 역직렬화합니다.
     * 사용 예시: List<User> users = JsonUtils.toObject(json, new TypeReference<List<User>>() {});
     */
    public static <T> T toObject(String json, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 컬렉션 역직렬화 중 오류가 발생했습니다. 대상 타입: " + typeReference.getType().getTypeName(), e);
        }
    }
}
