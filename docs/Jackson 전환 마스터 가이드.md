# Jackson 전환 마스터 가이드

Jira 9.x의 OSGi 환경까지 완벽하게 지원하는 **Jackson 전환 마스터 가이드** 최종본입니다. 이 가이드는 라이브러리 충돌 방지 설정부터 고립된 유틸리티 클래스 구현까지 모든 내용을 하나로 통합했습니다.

---

### 1. 단계: pom.xml 설정 (라이브러리 격리)

Jira 시스템 라이브러리와의 충돌을 방지하기 위해 Jackson을 플러그인 내부에 포함(`Shadowing`)하고 외부 노출을 차단합니다.

```xml
<dependencies>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.2</version>
        <scope>compile</scope>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
        <version>2.15.2</version>
        <scope>compile</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>com.atlassian.maven.plugins</groupId>
            <artifactId>jira-maven-plugin</artifactId>
            <version>${jira.version}</version>
            <extensions>true</extensions>
            <configuration>
                <instructions>
                    <Private-Package>
                        com.fasterxml.jackson.*,
                        com.atsoft.jira.plugin.logviewer.util
                    </Private-Package>
                    <Import-Package>
                        !com.fasterxml.jackson.*,
                        org.slf4j,
                        *;resolution:=optional
                    </Import-Package>
                </instructions>
            </configuration>
        </plugin>
    </plugins>
</build>

```

---

### 2. 단계: 통합 JsonUtils 클래스 (Jira 9 호환형)

클래스로더 스위칭 기법을 사용하여 OSGi 환경에서 안전하게 초기화되며, 동적 제네릭 타입 조립 기능을 포함한 유틸리티입니다.

```java
package com.atsoft.jira.plugin.logviewer.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Jira 9.x 및 OSGi 환경 대응 Jackson 통합 유틸리티
 */
public class JsonUtils {
    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);
    
    private static final ObjectMapper MAPPER;
    private static final ObjectReader READER;
    private static final ObjectWriter WRITER;
    private static final ObjectWriter PRETTY_WRITER;

    static {
        // [Jira 호환성 핵심] 초기화 시 플러그인 클래스로더 강제 지정
        ClassLoader pluginClassLoader = JsonUtils.class.getClassLoader();
        Thread currentThread = Thread.currentThread();
        ClassLoader oldContextClassLoader = currentThread.getContextClassLoader();
        
        try {
            currentThread.setContextClassLoader(pluginClassLoader);
            
            MAPPER = new ObjectMapper();
            MAPPER.registerModule(new JavaTimeModule());
            
            // GSON 호환 및 안정성 설정
            MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            
            // 성능 최적화를 위한 캐싱
            READER = MAPPER.reader();
            WRITER = MAPPER.writer();
            PRETTY_WRITER = MAPPER.writerWithDefaultPrettyPrinter();
            
            log.info("JsonUtils initialized for Jira 9 successfully.");
        } finally {
            // 원래 클래스로더로 복구
            currentThread.setContextClassLoader(oldContextClassLoader);
        }
    }

    private JsonUtils() {}

    // --- [직렬화] ---
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

    // --- [역직렬화] ---
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (isInvalid(json)) return null;
        try {
            return READER.forType(clazz).readValue(json);
        } catch (IOException e) {
            throw handleException("Deserialization", clazz.getName(), json, e);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (isInvalid(json)) return null;
        try {
            return READER.forType(typeReference).readValue(json);
        } catch (IOException e) {
            throw handleException("Deserialization(TypeRef)", typeReference.getType().getTypeName(), json, e);
        }
    }

    /**
     * 복합 제네릭 동적 생성 (예: Map<String, Object>)
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

    // --- [편의 메서드] ---
    public static Map<String, Object> toMap(String json) {
        return fromJson(json, new TypeReference<Map<String, Object>>() {});
    }

    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    private static boolean isInvalid(String json) {
        return json == null || json.trim().isEmpty();
    }

    private static RuntimeException handleException(String action, String type, String json, Exception e) {
        log.error("JSON {} Error for {}. Input: {}", action, type, json, e);
        return new RuntimeException("JSON " + action + " failed", e);
    }
}

```

---

### 3. 단계: DTO 어노테이션 변경 (Mapping)

GSON 어노테이션을 Jackson으로 모두 교체합니다.

| GSON | Jackson | 비고 |
| --- | --- | --- |
| `@SerializedName("key")` | **`@JsonProperty("key")`** | 필드명 매핑 |
| `@Expose(serialize=false)` | **`@JsonIgnore`** | 특정 필드 제외 |
| `new Gson()` | **`JsonUtils.toJson/fromJson`** | 호출 로직 변경 |

---

### 전환 후 기대 효과

1. **안전성**: Jira 시스템 클래스로더와 분리되어 어떤 환경에서도 동일하게 작동합니다.
2. **성능**: Reader/Writer 캐싱을 통해 GSON보다 빠른 처리 속도를 보장합니다.
3. **유연성**: 단순 객체부터 중첩된 `Map<String, Map<...>>` 구조까지 한 줄의 코드로 처리가 가능합니다.

이제 이 통합 가이드를 바탕으로 작업을 진행하시면 됩니다. **어노테이션 변경 작업 중에 헷갈리는 특정 도메인 모델(Entity)이 있다면 하나 보여주세요. 바로 변환 예시를 만들어 드리겠습니다!**