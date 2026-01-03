package com.atsoft.jira.plugin.logviewer.utils

import lombok.extern.slf4j.Slf4j
import org.mozilla.universalchardet.UniversalDetector
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.*

@Slf4j
object CharsetPrioritizer {
    /**
     * [감지 우선순위]
     * 1. BOM (Generic 타입 리턴 -> BOM 자동 제거)
     * 2. UTF-8 (Strict 검증)
     * 3. UTF-16 (Null 바이트 패턴으로 BE/LE 수동 판별)
     * 4. Library 추측 (MS949 오탐 보정)
     */
    @JvmStatic
    fun detect(data: ByteArray?): Charset {
        if (data == null || data.isEmpty()) return StandardCharsets.UTF_8

        // 1단계: BOM 확인
        // (BOM이 있으면 'UTF-16' 같은 Generic 타입을 리턴해서
        //  Java가 디코딩 시 BOM을 자동으로 떼어내게 함)
        val bomCharset = checkBom(data)
        if (bomCharset != null) {
            return bomCharset
        }

        // 2단계: 엄격한 UTF-8 검증
        if (isValidUtf8(data)) {
            return StandardCharsets.UTF_8
        }

        // 3단계: Null 바이트가 포함된 경우 -> UTF-16일 확률 매우 높음
        if (containsNullByte(data)) {
            if (data.size % 2 == 0) {
                // BOM이 없으므로 구체적인 BE/LE를 리턴해야 함
                if (isValidCharset(data, StandardCharsets.UTF_16BE)) return StandardCharsets.UTF_16BE
                if (isValidCharset(data, StandardCharsets.UTF_16LE)) return StandardCharsets.UTF_16LE
            }
        }

        // 4단계: 여기까지 왔으면 UTF 계열 아님 -> 라이브러리 추측 사용
        return detectWithLibrary(data)
    }

    // --- Helper Methods ---
    private fun checkBom(data: ByteArray): Charset? {
        val len = data.size

        // 1. 3바이트 BOM 검사 (UTF-8)
        if (len >= 3) {
            if ((data[0].toInt() and 0xFF) == 0xEF && (data[1].toInt() and 0xFF) == 0xBB && (data[2].toInt() and 0xFF) == 0xBF) return StandardCharsets.UTF_8
        }

        // 2. 2바이트 BOM 검사 (UTF-16)
        if (len >= 2) {
            // UTF-16BE BOM: FE FF
            if ((data[0].toInt() and 0xFF) == 0xFE && (data[1].toInt() and 0xFF) == 0xFF) return StandardCharsets.UTF_16 // Generic 리턴 (BOM 자동 제거)


            // UTF-16LE BOM: FF FE
            if ((data[0].toInt() and 0xFF) == 0xFF && (data[1].toInt() and 0xFF) == 0xFE) return StandardCharsets.UTF_16 // Generic 리턴 (BOM 자동 제거)
        }

        return null
    }

    private fun isValidUtf8(data: ByteArray): Boolean {
        return isValidCharset(data, StandardCharsets.UTF_8)
    }

    private fun isValidCharset(data: ByteArray, charset: Charset): Boolean {
        val decoder = charset.newDecoder()
        decoder.onMalformedInput(CodingErrorAction.REPORT)
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT)
        try {
            decoder.decode(ByteBuffer.wrap(data))
            return true
        } catch (e: CharacterCodingException) {
            return false
        }
    }

    private fun containsNullByte(data: ByteArray): Boolean {
        for (b in data) {
            if (b.toInt() == 0) return true
        }
        return false
    }

    private fun detectWithLibrary(data: ByteArray): Charset {
        val detector = UniversalDetector(null)
        detector.handleData(data, 0, data.size)
        detector.dataEnd()
        val encoding = detector.detectedCharset
        detector.reset()

        if (encoding != null) {
            val name = encoding.uppercase(Locale.getDefault())

            // Windows-1252 오탐 방지 (한국 환경 필승 로직)
            if (name == "WINDOWS-1252" ||
                name == "ISO-8859-1" ||
                name == "US-ASCII"
            ) {
                return Charset.forName("MS949")
            }

            val validEncodings = mutableSetOf("IBM", "EUC", "KR", "949")
            if (validEncodings.any { name.contains(it, ignoreCase = true) }) {
                return Charset.forName("MS949")
            }

            try {
                return Charset.forName(encoding)
            } catch (e: Exception) {
                // CharsetPrioritizer.log.warn("Charset {} is not supported.", encoding)
            }
        }

        return Charset.forName("MS949")
    }
}