package ut.com.atsoft.jira.plugin.logviewer.utils

import com.atsoft.jira.plugin.logviewer.utils.CharsetPrioritizer
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import lombok.extern.slf4j.Slf4j
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.stream.Stream
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import kotlin.math.min

@Slf4j
@TestMethodOrder(MethodOrderer.DisplayName::class)
class CharsetPrioritizerTest {
    private val log = KotlinLogging.logger {}

    typealias ResponseType = MutableMap<String, MutableMap<String, Any?>>
    typealias ItemType = MutableMap<String, Any?>
    typealias ItemListType = MutableList<ItemType?>

    @Test
    @DisplayName("1. BOM 없는 UTF-8 한글을 정확히 감지해야 한다")
    fun testUtf8NoBom() {
        val data = SAMPLE_RESPONSE_JSON_STRING!!.toByteArray(StandardCharsets.UTF_8)
        val detected = CharsetPrioritizer.detect(data)
        log.info("UTF-8 감지 결과: {}", detected.name())
        Assertions.assertEquals(StandardCharsets.UTF_8, detected)
        val decoded = String(data, detected)
        Assertions.assertEquals(SAMPLE_RESPONSE_JSON_STRING, decoded)
    }

    @Test
    @DisplayName("2. MS949(EUC-KR, CP949) 한글을 정확히 감지해야 한다")
    fun testEucKr() {
        val eucKr = Charset.forName("MS949")
        val data = SAMPLE_RESPONSE_JSON_STRING!!.toByteArray(eucKr)
        val detected = CharsetPrioritizer.detect(data)
        log.info("MS949 감지 결과: {}", detected.name())
        Assertions.assertTrue(
            mutableSetOf("EUC", "KR", "949").any { detected.name().contains(it, ignoreCase = true) },
            "감지된 인코딩이 EUC 계열이어야 합니다."
        )
        val decoded = String(data, detected)
        Assertions.assertEquals(SAMPLE_RESPONSE_JSON_STRING, decoded)
    }

    @Test
    @DisplayName("3. BOM이 있는 UTF-8 데이터는 100% 정확하게 감지해야 한다")
    fun testUtf8WithBom() {
        val originalBytes = SAMPLE_RESPONSE_JSON_STRING!!.toByteArray(StandardCharsets.UTF_8)
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val dataWithBom = ByteArray(bom.size + originalBytes.size)
        System.arraycopy(bom, 0, dataWithBom, 0, bom.size)
        System.arraycopy(originalBytes, 0, dataWithBom, bom.size, originalBytes.size)
        val detected = CharsetPrioritizer.detect(dataWithBom)
        log.info("BOM UTF-8 감지 결과: {}", detected.name())
        Assertions.assertEquals(StandardCharsets.UTF_8, detected)
    }

    @Test
    @DisplayName("4. 영문(ASCII)만 있는 경우 기본값(UTF-8) 혹은 호환 인코딩이 나와야 한다")
    fun testAscii() {
        val english = "Hello World! 12345"
        val data = english.toByteArray(StandardCharsets.US_ASCII)
        val detected = CharsetPrioritizer.detect(data)
        log.info("ASCII 데이터 감지 결과: {}", detected.name())
        val decoded = String(data, detected)
        Assertions.assertEquals(english, decoded)
    }

    @Test
    @DisplayName("5. 일본어 (Shift_JIS)를 정확히 감지해야 한다")
    fun testJapaneseShiftJis() {
        val japaneseText = "こんにちは。これは日本語のエンコーディングテストです。"
        val shiftJis = Charset.forName("Shift_JIS")
        val data = japaneseText.toByteArray(shiftJis)
        val detected = CharsetPrioritizer.detect(data)
        log.info("일본어(Shift_JIS) 감지 결과: {}", detected.name())
        Assertions.assertTrue(
            mutableSetOf("Shift_JIS", "windows-31j", "EUC-JP").any { detected.name().contains(it, ignoreCase = true) },
            "일본어 인코딩(Shift_JIS 계열)이어야 합니다."
        )
        val decoded = String(data, detected)
        Assertions.assertEquals(japaneseText, decoded)
    }

    @Test
    @DisplayName("6. 중국어 간체 (GB18030/GBK)를 정확히 감지해야 한다")
    fun testChineseSimplified() {
        val chineseText = "你好，这是一个中文编码测试。我们正在测试字符集检测功能。"
        val gb18030 = Charset.forName("GB18030")
        val data = chineseText.toByteArray(gb18030)
        val detected = CharsetPrioritizer.detect(data)
        log.info("중국어 간체(GB18030) 감지 결과: {}", detected.name())
        Assertions.assertTrue(
            detected.name().contains("GB", ignoreCase = true),
            "중국어 간체 인코딩(GB 계열)이어야 합니다."
        )
        val decoded = String(data, detected)
        Assertions.assertEquals(chineseText, decoded)
    }

    @Test
    @DisplayName("7. 중국어 번체 (Big5)를 정확히 감지해야 한다")
    fun testChineseTraditional() {
        val traditionalText = "你好，這是一個繁體中文編碼測試。"
        val big5 = Charset.forName("Big5")
        val data = traditionalText.toByteArray(big5)
        val detected = CharsetPrioritizer.detect(data)
        log.info("중국어 번체(Big5) 감지 결과: {}", detected.name())
        Assertions.assertTrue(detected.name().equals("Big5", ignoreCase = true), "중국어 번체 인코딩(Big5)이어야 합니다.")
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
        val detected = CharsetPrioritizer.detect(responseBytes)
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
        private var SAMPLE_RESPONSE_JSON_STRING: String? = null

        @JvmStatic
        @BeforeAll
        @Throws(JsonProcessingException::class)
        fun setUp() {
            val response: MutableMap<String?, Any?> = LinkedHashMap()
            val header: MutableMap<String?, Any?> = HashMap()
            header["resultCode"] = 200
            header["resultMessage"] = "SUCCESS"
            response["header"] = header
            val data: MutableMap<String?, Any?> = sampleData
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
                val data: MutableMap<String?, Any?> = HashMap()
                val pagination: MutableMap<String?, Any?> = HashMap()
                pagination["page"] = 1
                pagination["total"] = 150
                data["pagination"] = pagination
                val items: MutableList<MutableMap<String?, Any?>?> = ArrayList()
                val item1: MutableMap<String?, Any?> = HashMap()
                item1["id"] = "LB-1001"
                item1["status"] = "안녕하세요."
                items.add(item1)
                val item2: MutableMap<String?, Any?> =
                    HashMap()
                item2["id"] = "LB-1002"
                item2["status"] = "이것은 한글 인코딩 감지 테스트입니다."
                items.add(item2)
                val item3: MutableMap<String?, Any?> = HashMap()
                item3["id"] = "LB-1003"
                item3["status"] = "믜, 쀍 같은 특이한 글자도 포함해봅니다."
                items.add(item3)
                data["items"] = items
                return data
            }
    }
}