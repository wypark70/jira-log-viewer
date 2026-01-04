package com.atsoft.jira.plugin.logviewer.utils

import mu.KotlinLogging
import org.mozilla.universalchardet.UniversalDetector
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

private val log = KotlinLogging.logger {}

object CharsetPrioritizer {
    private val MS949 = Charset.forName("MS949")
    private val DEFAULT_FALLBACK_ENCODINGS = setOf("WINDOWS-1252", "ISO-8859-1", "US-ASCII")
    private val KOREAN_ENCODING_KEYWORDS = setOf("IBM", "EUC", "KR", "949")

    // 성능 최적화: 인코딩 감지를 위해 검사할 최대 바이트 수 (16KB)
    private const val VALIDATION_LIMIT = 16 * 1024

    @JvmStatic
    fun detect(data: ByteArray?): Charset {
        if (data == null || data.isEmpty()) return StandardCharsets.UTF_8

        // 1단계: BOM 확인
        checkBom(data)?.let { return it }

        // Null 바이트 존재 여부 확인 (성능을 위해 앞부분만 스캔)
        val limit = data.size.coerceAtMost(VALIDATION_LIMIT)
        val hasNull = (0 until limit).any { data[it] == 0.toByte() }

        // 2단계: 엄격한 UTF-8 검증
        // [중요 수정] 데이터 중간에 Null 바이트(0x00)가 있다면 UTF-16일 확률이 매우 높으므로
        // UTF-8 검증을 건너뜁니다. (UTF-8 디코더는 0x00을 에러로 보지 않기 때문에 오판 발생 가능)
        if (!hasNull && data.isValidCharset(StandardCharsets.UTF_8)) {
            return StandardCharsets.UTF_8
        }

        // 3단계 & 4단계: UTF-16 패턴 확인 후 실패 시 라이브러리 사용
        return data.tryDetectUtf16() ?: detectWithLibrary(data)
    }

    /**
     * 1단계: BOM(Byte Order Mark) 검사
     */
    private fun checkBom(data: ByteArray): Charset? = when {
        data.startsWith(0xEF, 0xBB, 0xBF) -> StandardCharsets.UTF_8
        // [수정] BOM이 있는 경우 구체적인 BE/LE 대신 Generic UTF-16을 리턴해야
        // Java의 Reader가 자동으로 BOM을 소비(Consume)하고 올바르게 디코딩합니다.
        data.startsWith(0xFE, 0xFF) -> StandardCharsets.UTF_16
        data.startsWith(0xFF, 0xFE) -> StandardCharsets.UTF_16
        else -> null
    }

    /**
     * 3단계: Null 바이트 패턴을 이용한 UTF-16 감지 시도
     */
    private fun ByteArray.tryDetectUtf16(): Charset? {
        if (size % 2 != 0) return null

        val limit = size.coerceAtMost(VALIDATION_LIMIT)
        var evenNulls = 0
        var oddNulls = 0

        for (i in 0 until limit step 2) {
            if (this[i] == 0.toByte()) evenNulls++
            if (this[i + 1] == 0.toByte()) oddNulls++
        }

        if (evenNulls == 0 && oddNulls == 0) return null

        return when {
            evenNulls > oddNulls && isValidCharset(StandardCharsets.UTF_16BE) -> StandardCharsets.UTF_16BE
            oddNulls > evenNulls && isValidCharset(StandardCharsets.UTF_16LE) -> StandardCharsets.UTF_16LE
            else -> null
        }
    }

    /**
     * 4단계: 라이브러리 기반 추측
     */
    private fun detectWithLibrary(data: ByteArray): Charset {
        val len = data.size.coerceAtMost(VALIDATION_LIMIT)

        val detectedName = UniversalDetector(null).run {
            handleData(data, 0, len)
            dataEnd()
            detectedCharset
        }?.uppercase() ?: return MS949

        return when {
            detectedName in DEFAULT_FALLBACK_ENCODINGS -> MS949
            KOREAN_ENCODING_KEYWORDS.any { detectedName.contains(it, ignoreCase = true) } -> MS949
            else -> runCatching {
                Charset.forName(detectedName)
            }.getOrElse {
                log.warn { "Charset $detectedName is not supported. Fallback to MS949." }
                MS949
            }
        }
    }

    // --- Extension Helper Methods ---

    private fun ByteArray.startsWith(vararg prefix: Int): Boolean {
        if (size < prefix.size) return false
        for (i in prefix.indices) {
            if (this[i] != prefix[i].toByte()) return false
        }
        return true
    }

    private fun ByteArray.isValidCharset(charset: Charset): Boolean {
        return try {
            val limit = size.coerceAtMost(VALIDATION_LIMIT)
            val buffer = ByteBuffer.wrap(this, 0, limit)

            charset.newDecoder().apply {
                onMalformedInput(CodingErrorAction.REPORT)
                onUnmappableCharacter(CodingErrorAction.REPORT)
            }.decode(buffer)
            true
        } catch (e: CharacterCodingException) {
            false
        }
    }
}