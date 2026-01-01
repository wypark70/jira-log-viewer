package ut.com.atsoft.jira.plugin.logviewer.utils;

import com.atsoft.jira.plugin.logviewer.utils.CharsetPrioritizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@TestMethodOrder(MethodOrderer.DisplayName.class)
class CharsetPrioritizerTest {

    // 테스트용 샘플 데이터 (길이가 너무 짧으면 통계적 추측이 실패할 수 있으므로 문장형으로 작성)
    private static String SAMPLE_RESPONSE_JSON_STRING;

    @BeforeAll
    static void setUp() throws JsonProcessingException {
        // 1. 전체 응답을 담을 Map (Root)
        Map<String, Object> response = new LinkedHashMap<>(); // 순서 보장을 위해 LinkedHashMap 사용

        // 2. 헤더 구성
        Map<String, Object> header = new HashMap<>();
        header.put("resultCode", 200);
        header.put("resultMessage", "SUCCESS");
        response.put("header", header);

        // 3. 데이터 영역 구성
        Map<String, Object> data = getSampleData();

        // 최종적으로 data를 response에 넣음
        response.put("data", data);

        ObjectMapper mapper = new ObjectMapper();
        SAMPLE_RESPONSE_JSON_STRING = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
    }

    private static @NonNull Map<String, Object> getSampleData() {
        Map<String, Object> data = new HashMap<>();

        // 3-1. 페이징 정보
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", 1);
        pagination.put("total", 150);
        data.put("pagination", pagination);

        // 3-2. 아이템 리스트 (List<Map>)
        List<Map<String, Object>> items = new ArrayList<>();

        // 아이템 1
        Map<String, Object> item1 = new HashMap<>();
        item1.put("id", "LB-1001");
        item1.put("status", "안녕하세요.");
        items.add(item1);

        // 아이템 2
        Map<String, Object> item2 = new HashMap<>();
        item2.put("id", "LB-1002");
        item2.put("status", "이것은 한글 인코딩 감지 테스트입니다.");
        items.add(item2);

        Map<String, Object> item3 = new HashMap<>();
        item3.put("id", "LB-1003");
        item3.put("status", "믜, 쀍 같은 특이한 글자도 포함해봅니다.");
        items.add(item3);

        data.put("items", items);
        return data;
    }

    @Test
    @DisplayName("1. BOM 없는 UTF-8 한글을 정확히 감지해야 한다")
    void testUtf8NoBom() {
        // Given
        byte[] data = SAMPLE_RESPONSE_JSON_STRING.getBytes(StandardCharsets.UTF_8);

        // When
        Charset detected = CharsetPrioritizer.detect(data);

        // Then
        log.info("UTF-8 감지 결과: {}", detected.name());
        assertEquals(StandardCharsets.UTF_8, detected);

        // 검증: 감지된 인코딩으로 다시 문자열을 만들었을 때 원본과 같아야 함
        String decoded = new String(data, detected);
        assertEquals(SAMPLE_RESPONSE_JSON_STRING, decoded);
    }

    @Test
    @DisplayName("2. MS949(EUC-KR, CP949) 한글을 정확히 감지해야 한다")
    void testEucKr() {
        // Given
        Charset eucKr = Charset.forName("MS949");
        byte[] data = SAMPLE_RESPONSE_JSON_STRING.getBytes(eucKr);

        // When
        Charset detected = CharsetPrioritizer.detect(data);

        // Then
        log.info("MS949 감지 결과: {}", detected.name());

        // 이름은 다를 수 있어도(Alias), 실제 동작은 EUC-KR과 호환되어야 함
        assertTrue(
                detected.name().toUpperCase().contains("EUC")
                        || detected.name().contains("KR")
                        || detected.name().contains("949"),
                "감지된 인코딩이 EUC 계열이어야 합니다."
        );

        // 핵심 검증: 감지된 인코딩으로 디코딩했을 때 글자가 깨지지 않아야 함
        String decoded = new String(data, detected);
        assertEquals(SAMPLE_RESPONSE_JSON_STRING, decoded);
    }

    @Test
    @DisplayName("3. BOM이 있는 UTF-8 데이터는 100% 정확하게 감지해야 한다")
    void testUtf8WithBom() {
        // Given
        byte[] originalBytes = SAMPLE_RESPONSE_JSON_STRING.getBytes(StandardCharsets.UTF_8);
        // UTF-8 BOM 바이트: EF BB BF
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

        // BOM + 데이터 합치기
        byte[] dataWithBom = new byte[bom.length + originalBytes.length];
        System.arraycopy(bom, 0, dataWithBom, 0, bom.length);
        System.arraycopy(originalBytes, 0, dataWithBom, bom.length, originalBytes.length);

        // When
        Charset detected = CharsetPrioritizer.detect(dataWithBom);

        // Then
        log.info("BOM UTF-8 감지 결과: {}", detected.name());
        assertEquals(StandardCharsets.UTF_8, detected);
    }

    @Test
    @DisplayName("4. 영문(ASCII)만 있는 경우 기본값(UTF-8) 혹은 호환 인코딩이 나와야 한다")
    void testAscii() {
        // Given
        String english = "Hello World! 12345";
        byte[] data = english.getBytes(StandardCharsets.US_ASCII);

        // When
        Charset detected = CharsetPrioritizer.detect(data);

        // Then
        // ASCII는 UTF-8의 서브셋이므로 UTF-8로 감지되거나 ASCII로 감지될 수 있음.
        // 중요한 건 이걸로 디코딩했을 때 문제가 없어야 한다는 점.
        log.info("ASCII 데이터 감지 결과: {}", detected.name());

        String decoded = new String(data, detected);
        assertEquals(english, decoded);
    }

    @Test
    @DisplayName("5. 일본어 (Shift_JIS)를 정확히 감지해야 한다")
    void testJapaneseShiftJis() {
        // Given: 일본어 문장 (길이가 어느 정도 있어야 정확도 상승)
        String japaneseText = "こんにちは。これは日本語のエンコーディングテストです。";
        Charset shiftJis = Charset.forName("Shift_JIS");
        byte[] data = japaneseText.getBytes(shiftJis);

        // When
        Charset detected = CharsetPrioritizer.detect(data);

        // Then
        log.info("일본어(Shift_JIS) 감지 결과: {}", detected.name());

        // Windows-31J는 Shift_JIS의 확장판이므로 둘 중 하나로 잡히면 성공
        assertTrue(
                detected.name().equalsIgnoreCase("Shift_JIS")
                        || detected.name().equalsIgnoreCase("windows-31j")
                        || detected.name().equalsIgnoreCase("EUC-JP"), // 드물게 EUC로 오인될 경우 디코딩 확인 필수
                "일본어 인코딩(Shift_JIS 계열)이어야 합니다."
        );

        // 디코딩 검증
        String decoded = new String(data, detected);
        assertEquals(japaneseText, decoded);
    }

    @Test
    @DisplayName("6. 중국어 간체 (GB18030/GBK)를 정확히 감지해야 한다")
    void testChineseSimplified() {
        // Given: 중국어 간체 문장
        String chineseText = "你好，这是一个中文编码测试。我们正在测试字符集检测功能。";
        // GB18030은 GBK와 GB2312를 포함하는 최신 표준
        Charset gb18030 = Charset.forName("GB18030");
        byte[] data = chineseText.getBytes(gb18030);

        // When
        Charset detected = CharsetPrioritizer.detect(data);

        // Then
        log.info("중국어 간체(GB18030) 감지 결과: {}", detected.name());

        // GB18030, GBK, GB2312는 호환됨
        assertTrue(detected.name().toUpperCase().contains("GB"), "중국어 간체 인코딩(GB 계열)이어야 합니다.");

        // 디코딩 검증
        String decoded = new String(data, detected);
        assertEquals(chineseText, decoded);
    }

    @Test
    @DisplayName("7. 중국어 번체 (Big5)를 정확히 감지해야 한다")
    void testChineseTraditional() {
        // Given: 대만/홍콩에서 쓰이는 번체 문장
        String traditionalText = "你好，這是一個繁體中文編碼測試。";
        Charset big5 = Charset.forName("Big5");
        byte[] data = traditionalText.getBytes(big5);

        // When
        Charset detected = CharsetPrioritizer.detect(data);

        // Then
        log.info("중국어 번체(Big5) 감지 결과: {}", detected.name());

        // Big5 감지 확인
        assertTrue(detected.name().equalsIgnoreCase("Big5"), "중국어 번체 인코딩(Big5)이어야 합니다.");

        // 디코딩 검증
        String decoded = new String(data, detected);
        assertEquals(traditionalText, decoded);
    }

    @Nested
    @DisplayName("8. 변환 테스트")
    class ConversionTest {
        @BeforeAll
        static void setUp() {
            log.info("\n\nSample response JSON: {}\n\n", SAMPLE_RESPONSE_JSON_STRING);
        }

        @DisplayName("1. 주요 charset별 변환결과 확인")
        @ParameterizedTest(name = "{index} => charsetName={0}")
        @ValueSource(strings = {"UTF-8", "UTF-16", "UTF-16BE", "UTF-16LE", "US-ASCII", "ISO-8859-1", "EUC-KR", "MS949"})
        public void anotherTest(String charsetName) throws JsonProcessingException {
            log.info("charsetName: {}", charsetName);
            byte[] responseBytes = SAMPLE_RESPONSE_JSON_STRING.getBytes(Charset.forName(charsetName));
            log.info("responseBytes: {}", responseBytes);
            Charset detected = CharsetPrioritizer.detect(responseBytes);
            log.info("detected: {}", detected);
            String responseString = new String(responseBytes, detected);
            TypeReference<Map<String, Map<String, Object>>> typeRef = new TypeReference<>() {};

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Map<String, Object>> responseMap = mapper.readValue(responseString, typeRef);
            List<Map<String, Object>> items = mapper.convertValue(responseMap.get("data").get("items"), new TypeReference<>() {});
            items.sort((map1, map2) -> {
                String id1 = (String) map1.get("id");
                String id2 = (String) map2.get("id");

                // 뒤의 값(age2)에서 앞의 값(age1)을 비교하면 내림차순이 됨
                return id2.compareTo(id1);
            });

            Response response = Response.ok()
                    .type(MediaType.APPLICATION_JSON)
                    .entity(items)
                    .build();
            log.info("Response metadata: {}", response.getMetadata());
            log.info("Response entity: {}\n\n", response.getEntity());
        }
    }

}