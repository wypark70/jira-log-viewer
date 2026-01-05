package com.atsoft.jira.plugin.logviewer.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Jackson 기반 통합 JSON 유틸리티 (최종본)
 * - 격리 전략: 내부 ObjectMapper를 고립시켜 버전 충돌 방지
 * - 성능 최적화: Reader/Writer 캐싱 및 TypeFactory 활용
 * - 유연성: 복합 제네릭 구조(Map, List 중첩) 지원
 */
public class JsonUtils {
    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ObjectReader READER;
    private static final ObjectWriter WRITER;
    private static final ObjectWriter PRETTY_WRITER;

    static {
        // 1. 기본 설정 및 모듈 등록
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 2. 고성능 처리를 위한 캐싱
        READER = MAPPER.reader();
        WRITER = MAPPER.writer();
        PRETTY_WRITER = MAPPER.writerWithDefaultPrettyPrinter();
    }

    private JsonUtils() {}

    // --- [직렬화: Object -> JSON] ---

    public static String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return WRITER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw handleException("Serialization", obj.getClass().getName(), null, e);
        }
    }

    public static String toPrettyJson(Object obj) {
        if (obj == null) return null;
        try {
            return PRETTY_WRITER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return toJson(obj);
        }
    }

    // --- [역직렬화: JSON -> Object] ---

    /**
     * 기본 역직렬화 (일반 클래스)
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (isInvalid(json)) return null;
        try {
            return READER.forType(clazz).readValue(json);
        } catch (IOException e) {
            throw handleException("Deserialization", clazz.getName(), json, e);
        }
    }

    /**
     * 제네릭 역직렬화 (TypeReference 활용 - 가장 권장되는 방식)
     * 예: JsonUtils.fromJson(json, new TypeReference<Map<String, Object>>(){})
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (isInvalid(json)) return null;
        try {
            return READER.forType(typeReference).readValue(json);
        } catch (IOException e) {
            throw handleException("Deserialization(TypeReference)", typeReference.getType().getTypeName(), json, e);
        }
    }

    /**
     * 동적 제네릭 역직렬화 (여러 클래스 인자를 받아 타입을 조립)
     * 예: Map<String, Object> -> fromGenericJson(json, Map.class, String.class, Object.class)
     */
    public static <T> T fromGenericJson(String json, Class<?> parametrized, Class<?>... parameterClasses) {
        if (isInvalid(json)) return null;
        try {
            JavaType type = MAPPER.getTypeFactory().constructParametricType(parametrized, parameterClasses);
            return MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw handleException("Deserialization(Generic)", parametrized.getName(), json, e);
        }
    }

    // --- [편의 메서드: 자주 사용되는 Map 구조] ---

    public static Map<String, Object> toMap(String json) {
        return fromJson(json, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * 중첩 맵 구조 변환 (Map<String, Map<String, Object>>)
     */
    public static Map<String, Map<String, Object>> toNestedMap(String json) {
        return fromJson(json, new TypeReference<Map<String, Map<String, Object>>>() {});
    }

    // --- [내부 헬퍼] ---

    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    private static boolean isInvalid(String json) {
        return json == null || json.trim().isEmpty();
    }

    private static RuntimeException handleException(String action, String typeName, String json, Exception e) {
        String message = String.format("JSON %s Error. Target: %s", action, typeName);
        log.error("{}. Input JSON: {}", message, json, e);
        return new RuntimeException(message, e);
    }
}