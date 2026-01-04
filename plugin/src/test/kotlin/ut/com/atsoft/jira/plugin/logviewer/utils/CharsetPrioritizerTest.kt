package ut.com.atsoft.jira.plugin.logviewer.utils

import com.atsoft.jira.plugin.logviewer.utils.CharsetPrioritizer.detect
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.charset.Charset
import kotlin.math.min

@TestMethodOrder(MethodOrderer.DisplayName::class)
internal class CharsetPrioritizerTest {
    private val log = KotlinLogging.logger {}

    typealias ResponseType = Map<String, Any?>
    typealias ItemType = Map<String, Any?>
    typealias ItemListType = List<ItemType>

    @Test
    @DisplayName("1. BOM 없는 UTF-8 한글을 정확히 감지해야 한다")
    fun testUtf8NoBom() {
        val data = SAMPLE_RESPONSE_JSON_STRING.toByteArray(Charsets.UTF_8)
        val detected = detect(data)
        log.info { "UTF-8 감지 결과: ${detected.name()}" }
        Assertions.assertEquals(Charsets.UTF_8, detected)
        val decoded = data.toString(detected)
        Assertions.assertEquals(SAMPLE_RESPONSE_JSON_STRING, decoded)
    }

    @Test
    @DisplayName("2. MS949(EUC-KR, CP949) 한글을 정확히 감지해야 한다")
    fun testEucKr() {
        val eucKr = Charset.forName("MS949")
        val data = SAMPLE_RESPONSE_JSON_STRING.toByteArray(eucKr)
        val detected = detect(data)
        log.info { "MS949 감지 결과: ${detected.name()}" }
        Assertions.assertTrue(
            setOf("EUC", "KR", "949").any { detected.name().contains(it, ignoreCase = true) },
            "감지된 인코딩이 EUC 계열이어야 합니다."
        )
        val decoded = data.toString(detected)
        Assertions.assertEquals(SAMPLE_RESPONSE_JSON_STRING, decoded)
    }

    @Test
    @DisplayName("3. BOM이 있는 UTF-8 데이터는 100% 정확하게 감지해야 한다")
    fun testUtf8WithBom() {
        val originalBytes = SAMPLE_RESPONSE_JSON_STRING.toByteArray(Charsets.UTF_8)
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val dataWithBom = bom + originalBytes
        val detected = detect(dataWithBom)
        log.info { "BOM UTF-8 감지 결과: ${detected.name()}" }
        Assertions.assertEquals(Charsets.UTF_8, detected)
    }

    @Test
    @DisplayName("4. 영문(ASCII)만 있는 경우 기본값(UTF-8) 혹은 호환 인코딩이 나와야 한다")
    fun testAscii() {
        val english = "Hello World! 12345"
        val data = english.toByteArray(Charsets.US_ASCII)
        val detected = detect(data)
        log.info { "ASCII 데이터 감지 결과: ${detected.name()}" }
        val decoded = data.toString(detected)
        Assertions.assertEquals(english, decoded)
    }

    @Test
    @DisplayName("5. 일본어 (Shift_JIS)를 정확히 감지해야 한다")
    fun testJapaneseShiftJis() {
        val japaneseText = "こんにちは。これは日本語のエンコーディングテストです。"
        val shiftJis = Charset.forName("Shift_JIS")
        val data = japaneseText.toByteArray(shiftJis)
        val detected = detect(data)
        log.info { "일본어(Shift_JIS) 감지 결과: ${detected.name()}" }
        Assertions.assertTrue(
            setOf("Shift_JIS", "windows-31j", "EUC-JP").any { detected.name().contains(it, ignoreCase = true) },
            "일본어 인코딩(Shift_JIS 계열)이어야 합니다."
        )
        val decoded = data.toString(detected)
        Assertions.assertEquals(japaneseText, decoded)
    }

    @Test
    @DisplayName("6. 중국어 간체 (GB18030/GBK)를 정확히 감지해야 한다")
    fun testChineseSimplified() {
        val chineseText = "你好，这是一个中文编码测试。我们正在测试字符集检测功能。"
        val gb18030 = Charset.forName("GB18030")
        val data = chineseText.toByteArray(gb18030)
        val detected = detect(data)
        log.info { "중국어 간체(GB18030) 감지 결과: ${detected.name()}" }
        Assertions.assertTrue(
            detected.name().contains("GB", ignoreCase = true),
            "중국어 간체 인코딩(GB 계열)이어야 합니다."
        )
        val decoded = data.toString(detected)
        Assertions.assertEquals(chineseText, decoded)
    }

    @Test
    @DisplayName("7. 중국어 번체 (Big5)를 정확히 감지해야 한다")
    fun testChineseTraditional() {
        val traditionalText = "你好，這是一個繁體中文編碼測試。"
        val big5 = Charset.forName("Big5")
        val data = traditionalText.toByteArray(big5)
        val detected = detect(data)
        log.info { "중국어 번체(Big5) 감지 결과: ${detected.name()}" }
        Assertions.assertTrue(detected.name().equals("Big5", ignoreCase = true), "중국어 번체 인코딩(Big5)이어야 합니다.")
        val decoded = data.toString(detected)
        Assertions.assertEquals(traditionalText, decoded)
    }

    @DisplayName("8. 주요 charset별 변환결과 확인")
    @ParameterizedTest
    @MethodSource("provideCharsets")
    @Throws(
        JsonProcessingException::class
    )
    fun conversionTest(index: Int, charsetName: String) {
        if (index == 0) log.info { "\n\n$SAMPLE_RESPONSE_JSON_STRING\n\n" }
        log.info { "charsetName: $charsetName" }
        val responseBytes = SAMPLE_RESPONSE_JSON_STRING.toByteArray(Charset.forName(charsetName))
        val previewBytes = responseBytes.copyOf(min(16, responseBytes.size))
        log.info { "responseBytes[0:${previewBytes.size}]: ${previewBytes.contentToString()}" }
        val detected = detect(responseBytes)
        log.info { "detected: $detected" }
        val responseString = responseBytes.toString(detected)

        val mapper = ObjectMapper()
        val typeRef = object : TypeReference<Map<String, Map<String, Any?>>>() {}
        val responseMap: Map<String, Map<String, Any?>> = mapper.readValue(responseString, typeRef)

        val dataMap = responseMap["data"]
        val itemsRaw = dataMap?.get("items")

        val items: MutableList<ItemType> =
            mapper.convertValue(itemsRaw, object : TypeReference<MutableList<ItemType>>() {})
        items.sortByDescending { it["id"] as String }

        log.info { "Response items: ${items}\n\n" }
    }

    companion object {
        private var SAMPLE_RESPONSE_JSON_STRING: String = ""

        @JvmStatic
        @BeforeAll
        @Throws(JsonProcessingException::class)
        fun setUp() {
            val response = linkedMapOf(
                "header" to mapOf(
                    "resultCode" to 200,
                    "resultMessage" to "SUCCESS"
                ),
                "data" to sampleData
            )
            val mapper = ObjectMapper()
            this.SAMPLE_RESPONSE_JSON_STRING = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response)
        }

        @JvmStatic
        fun provideCharsets(): List<Arguments> {
            return listOf(
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

        private val sampleData: Map<String, Any?>
            get() = mapOf(
                "pagination" to mapOf(
                    "page" to 1,
                    "total" to 150
                ),
                "items" to listOf(
                    mapOf(
                        "id" to "LB-1001",
                        "status" to "안녕하세요."
                    ),
                    mapOf(
                        "id" to "LB-1002",
                        "status" to "이것은 한글 인코딩 감지 테스트입니다."
                    ),
                    mapOf(
                        "id" to "LB-1003",
                        "status" to "믜, 쀍 같은 특이한 글자도 포함해봅니다."
                    )
                )
            )
    }
}
