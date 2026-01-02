package ut.com.atsoft.jira.plugin.logviewer.utils

import com.atsoft.jira.plugin.logviewer.utils.CharsetPrioritizer.detect
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import lombok.extern.slf4j.Slf4j
import mu.KotlinLogging
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Stream
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import kotlin.math.min

@Slf4j
@TestMethodOrder(MethodOrderer.DisplayName::class)
internal class CharsetPrioritizerTest {
    private val log = KotlinLogging.logger {}

    typealias ResponseType = MutableMap<String, MutableMap<String, Any?>>
    typealias ItemType = MutableMap<String, Any?>
    typealias ItemListType = MutableList<ItemType?>

    @Test
    @DisplayName("1. BOM 없는 UTF-8 한글을 정확히 감지해야 한다")
    fun testUtf8NoBom() {
        // Given
        val data = SAMPLE_RESPONSE_JSON_STRING!!.toByteArray(StandardCharsets.UTF_8)

        // When
        val detected = detect(data)

        // Then
        log.info("UTF-8 감지 결과: {}", detected.name())
        Assertions.assertEquals(StandardCharsets.UTF_8, detected)

        // 검증: 감지된 인코딩으로 다시 문자열을 만들었을 때 원본과 같아야 함
        val decoded = String(data, detected)
        Assertions.assertEquals(SAMPLE_RESPONSE_JSON_STRING, decoded)
    }

    @Test
    @DisplayName("2. MS949(EUC-KR, CP949) 한글을 정확히 감지해야 한다")
    fun testEucKr() {
        // Given
        val eucKr = Charset.forName("MS949")
        val data = SAMPLE_RESPONSE_JSON_STRING!!.toByteArray(eucKr)

        // When
        val detected = detect(data)

        // Then
        log.info("MS949 감지 결과: {}", detected.name())

        // 이름은 다를 수 있어도(Alias), 실제 동작은 EUC-KR과 호환되어야 함
        Assertions.assertTrue(
            mutableSetOf("EUC", "KR", "949").any { detected.name().contains(it, ignoreCase = true) },
            "감지된 인코딩이 EUC 계열이어야 합니다."
        )

        // 핵심 검증: 감지된 인코딩으로 디코딩했을 때 글자가 깨지지 않아야 함
        val decoded = String(data, detected)
        Assertions.assertEquals(SAMPLE_RESPONSE_JSON_STRING, decoded)
    }

    @Test
    @DisplayName("3. BOM이 있는 UTF-8 데이터는 100% 정확하게 감지해야 한다")
    fun testUtf8WithBom() {
        // Given
        val originalBytes = SAMPLE_RESPONSE_JSON_STRING!!.toByteArray(StandardCharsets.UTF_8)
        // UTF-8 BOM 바이트: EF BB BF
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

        // BOM + 데이터 합치기
        val dataWithBom = ByteArray(bom.size + originalBytes.size)
        System.arraycopy(bom, 0, dataWithBom, 0, bom.size)
        System.arraycopy(originalBytes, 0, dataWithBom, bom.size, originalBytes.size)

        // When
        val detected = detect(dataWithBom)

        // Then
        log.info("BOM UTF-8 감지 결과: {}", detected.name())
        Assertions.assertEquals(StandardCharsets.UTF_8, detected)
    }

    @Test
    @DisplayName("4. 영문(ASCII)만 있는 경우 기본값(UTF-8) 혹은 호환 인코딩이 나와야 한다")
    fun testAscii() {
        // Given
        val english = "Hello World! 12345"
        val data = english.toByteArray(StandardCharsets.US_ASCII)

        // When
        val detected = detect(data)

        // Then
        // ASCII는 UTF-8의 서브셋이므로 UTF-8로 감지되거나 ASCII로 감지될 수 있음.
        // 중요한 건 이걸로 디코딩했을 때 문제가 없어야 한다는 점.
        log.info("ASCII 데이터 감지 결과: {}", detected.name())

        val decoded = String(data, detected)
        Assertions.assertEquals(english, decoded)
    }

    @Test
    @DisplayName("5. 일본어 (Shift_JIS)를 정확히 감지해야 한다")
    fun testJapaneseShiftJis() {
        // Given: 일본어 문장 (길이가 어느 정도 있어야 정확도 상승)
        val japaneseText = "こんにちは。これは日本語のエンコーディングテストです。"
        val shiftJis = Charset.forName("Shift_JIS")
        val data = japaneseText.toByteArray(shiftJis)

        // When
        val detected = detect(data)

        // Then
        log.info("일본어(Shift_JIS) 감지 결과: {}", detected.name())

        // Windows-31J는 Shift_JIS의 확장판이므로 둘 중 하나로 잡히면 성공
        Assertions.assertTrue(
            mutableSetOf("Shift_JIS", "windows-31j", "EUC-JP").any { detected.name().contains(it, ignoreCase = true) },
            "일본어 인코딩(Shift_JIS 계열)이어야 합니다."
        )

        // 디코딩 검증
        val decoded = String(data, detected)
        Assertions.assertEquals(japaneseText, decoded)
    }

    @Test
    @DisplayName("6. 중국어 간체 (GB18030/GBK)를 정확히 감지해야 한다")
    fun testChineseSimplified() {
        // Given: 중국어 간체 문장
        val chineseText = "你好，这是一个中文编码测试。我们正在测试字符集检测功能。"
        // GB18030은 GBK와 GB2312를 포함하는 최신 표준
        val gb18030 = Charset.forName("GB18030")
        val data = chineseText.toByteArray(gb18030)

        // When
        val detected = detect(data)

        // Then
        log.info("중국어 간체(GB18030) 감지 결과: {}", detected.name())

        // GB18030, GBK, GB2312는 호환됨
        Assertions.assertTrue(
            detected.name().contains("GB", ignoreCase = true),
            "중국어 간체 인코딩(GB 계열)이어야 합니다."
        )

        // 디코딩 검증
        val decoded = String(data, detected)
        Assertions.assertEquals(chineseText, decoded)
    }

    @Test
    @DisplayName("7. 중국어 번체 (Big5)를 정확히 감지해야 한다")
    fun testChineseTraditional() {
        // Given: 대만/홍콩에서 쓰이는 번체 문장
        val traditionalText = "你好，這是一個繁體中文編碼測試。"
        val big5 = Charset.forName("Big5")
        val data = traditionalText.toByteArray(big5)

        // When
        val detected = detect(data)

        // Then
        log.info("중국어 번체(Big5) 감지 결과: {}", detected.name())

        // Big5 감지 확인
        Assertions.assertTrue(detected.name().equals("Big5", ignoreCase = true), "중국어 번체 인코딩(Big5)이어야 합니다.")

        // 디코딩 검증
        val decoded = String(data, detected)
        Assertions.assertEquals(traditionalText, decoded)
    }

    @DisplayName("8. 주요 charset별 변환결과 확인")
    @ParameterizedTest
    @MethodSource("provideCharsets")
    @Throws(
        JsonProcessingException::class
    )
    fun conversionTest(index: Int, charsetName: String) {
        if (index == 0) log.info("\n\n{}\n\n", SAMPLE_RESPONSE_JSON_STRING)

        log.info("charsetName: {}", charsetName)
        val responseBytes = SAMPLE_RESPONSE_JSON_STRING!!.toByteArray(Charset.forName(charsetName))
        val previewBytes = responseBytes.copyOf(min(16, responseBytes.size))
        log.info("responseBytes[0:{}]: {}", previewBytes.size, previewBytes)
        val detected = detect(responseBytes)
        log.info("detected: {}", detected)
        val responseString = String(responseBytes, detected)

        val mapper = ObjectMapper()
        val responseTypeRef = object : TypeReference<ResponseType>() {}
        val itemsTypeRef = object : TypeReference<ItemListType>() {}
        val responseMap = mapper.readValue(responseString, responseTypeRef)
        val items = mapper.convertValue(responseMap["data"]!!["items"], itemsTypeRef)
        items.sortWith(Comparator { map1: ItemType, map2: ItemType ->
            val id1 = map1["id"] as String
            val id2 = map2["id"] as String
            id2.compareTo(id1)
        })

        val response = Response.ok()
            .type(MediaType.APPLICATION_JSON)
            .entity(items)
            .build()
        log.info("Response metadata: {}", response.metadata)
        log.info("Response entity: {}\n\n", response.entity)
    }


    companion object {
        // 테스트용 샘플 데이터 (길이가 너무 짧으면 통계적 추측이 실패할 수 있으므로 문장형으로 작성)
        private var SAMPLE_RESPONSE_JSON_STRING: String? = null

        @JvmStatic
        @BeforeAll
        @Throws(JsonProcessingException::class)
        fun setUp() {
            // 1. 전체 응답을 담을 Map (Root)
            val response: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>() // 순서 보장을 위해 LinkedHashMap 사용

            // 2. 헤더 구성
            val header: MutableMap<String?, Any?> = HashMap<String?, Any?>()
            header["resultCode"] = 200
            header["resultMessage"] = "SUCCESS"
            response["header"] = header

            // 3. 데이터 영역 구성
            val data: MutableMap<String?, Any?> = sampleData

            // 최종적으로 data를 response에 넣음
            response["data"] = data

            val mapper = ObjectMapper()
            this.SAMPLE_RESPONSE_JSON_STRING = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response)
        }

        @JvmStatic
        fun provideCharsets(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(0, "UTF-8"),
                Arguments.of(1, "UTF-16"),
                Arguments.of(2, "UTF-16BE"),
                Arguments.of(3, "UTF-16LE"),
                Arguments.of(4, "US-ASCII"),
                Arguments.of(5, "ISO-8859-1"),
                Arguments.of(6, "EUC-KR"),
                Arguments.of(7, "MS949")
            )
        }

        private val sampleData: MutableMap<String?, Any?>
            get() {
                val data: MutableMap<String?, Any?> = HashMap<String?, Any?>()

                // 3-1. 페이징 정보
                val pagination: MutableMap<String?, Any?> = HashMap<String?, Any?>()
                pagination["page"] = 1
                pagination["total"] = 150
                data["pagination"] = pagination

                // 3-2. 아이템 리스트 (List<Map>)
                val items: MutableList<MutableMap<String?, Any?>?> = ArrayList<MutableMap<String?, Any?>?>()

                // 아이템 1
                val item1: MutableMap<String?, Any?> = HashMap<String?, Any?>()
                item1["id"] = "LB-1001"
                item1["status"] = "안녕하세요."
                items.add(item1)

                // 아이템 2
                val item2: MutableMap<String?, Any?> =
                    HashMap<String?, Any?>()
                item2["id"] = "LB-1002"
                item2["status"] = "이것은 한글 인코딩 감지 테스트입니다."
                items.add(item2)

                // 아이템 3
                val item3: MutableMap<String?, Any?> = HashMap<String?, Any?>()
                item3["id"] = "LB-1003"
                item3["status"] = "믜, 쀍 같은 특이한 글자도 포함해봅니다."
                items.add(item3)

                data["items"] = items
                return data
            }
    }
}