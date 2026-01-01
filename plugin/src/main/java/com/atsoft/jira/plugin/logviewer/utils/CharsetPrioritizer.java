package ut.com.atsoft.jira.plugin.logviewer.utils;

import org.mozilla.universalchardet.UniversalDetector;

import java.nio.ByteBuffer;
import java.nio.charset.*;

public class CharsetPrioritizer {

    /**
     * 오판을 줄이기 위해 UTF 계열을 최우선으로 검사하고,
     * 실패할 경우에만 레거시(MS949) 추측을 수행합니다.
     */
    public static Charset detect(byte[] data) {
        if (data == null || data.length == 0) return StandardCharsets.UTF_8;

        // 1단계: BOM(Byte Order Mark) 확인 (가장 확실)
        Charset bomCharset = checkBom(data);
        if (bomCharset != null) {
            return bomCharset;
        }

        // 2단계: 엄격한 UTF-8 검증
        // (MS949 데이터가 우연히 완벽한 UTF-8 규칙을 지킬 확률은 거의 0에 수렴함)
        if (isValidUtf8(data)) {
            return StandardCharsets.UTF_8;
        }

        // 3단계: UTF-16BE/LE 가능성 타진 (Null Byte 체크)
        // MS949 텍스트 파일에는 0x00(Null)이 절대 존재하지 않는다는 점을 이용
        if (containsNullByte(data)) {
            // 0x00이 있는데 UTF-8이 아니라면 UTF-16 계열일 확률이 매우 높음
            if (isValidUtf16(data, StandardCharsets.UTF_16BE)) {
                return StandardCharsets.UTF_16BE;
            }
            if (isValidUtf16(data, StandardCharsets.UTF_16LE)) {
                return StandardCharsets.UTF_16LE;
            }
        }

        // 4단계: 여기까지 왔으면 UTF 계열이 아님 -> 라이브러리 추측 사용
        return detectWithLibrary(data);
    }

    // --- 헬퍼 메서드들 ---

    private static Charset checkBom(byte[] data) {
        if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF)
            return StandardCharsets.UTF_8;
        if (data.length >= 2 && (data[0] & 0xFF) == 0xFE && (data[1] & 0xFF) == 0xFF)
            return StandardCharsets.UTF_16BE;
        if (data.length >= 2 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xFE)
            return StandardCharsets.UTF_16LE;
        return null;
    }

    private static boolean isValidUtf8(byte[] data) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            decoder.decode(ByteBuffer.wrap(data));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    // UTF-16은 짝수 바이트여야 함 + 디코딩 테스트
    private static boolean isValidUtf16(byte[] data, Charset charset) {
        if (data.length % 2 != 0) return false; // UTF-16은 반드시 짝수 길이

        CharsetDecoder decoder = charset.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            decoder.decode(ByteBuffer.wrap(data));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    private static boolean containsNullByte(byte[] data) {
        for (byte b : data) {
            if (b == 0) return true;
        }
        return false;
    }

    private static Charset detectWithLibrary(byte[] data) {
        UniversalDetector detector = new UniversalDetector(null);
        detector.handleData(data, 0, data.length);
        detector.dataEnd();
        String encoding = detector.getDetectedCharset();
        detector.reset();

        if (encoding != null) {
            // 라이브러리가 EUC-KR 등을 뱉으면 MS949로 통일
            if (encoding.toUpperCase().contains("KR") || encoding.toUpperCase().contains("949")) {
                return Charset.forName("MS949");
            }
            try {
                return Charset.forName(encoding);
            } catch (Exception e) {
            }
        }
        return Charset.forName("MS949"); // 최후의 보루
    }
}