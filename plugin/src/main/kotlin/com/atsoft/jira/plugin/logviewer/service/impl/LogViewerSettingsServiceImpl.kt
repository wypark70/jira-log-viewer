package com.atsoft.jira.plugin.logviewer.service.impl

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atsoft.jira.plugin.logviewer.service.LogViewerSettingsService
import org.springframework.stereotype.Component
import jakarta.inject.Inject

@Component // Spring 빈으로 등록 (필요 시)
class LogViewerSettingsServiceImpl @Inject constructor(
    @ComponentImport private val pluginSettingsFactory: PluginSettingsFactory
) : LogViewerSettingsService {

    companion object {
        // 상수 네이밍 컨벤션 준수 및 가독성 향상
        private const val PLUGIN_KEY = "com.atsoft.jira.plugin.logviewer"
        private const val KEY_LOG_FILE_PATH = "$PLUGIN_KEY.logFilePath"
        private const val KEY_LINE_COUNT = "$PLUGIN_KEY.lineCount"
        private const val DEFAULT_LINE_COUNT = 100
    }

    // lazy 대신 getter를 사용하여 호출 시마다 가져오거나(안전), 
    // 혹은 생성 시점에 초기화하는 방식을 고려할 수 있습니다.
    // 여기서는 호출 시마다 안전하게 팩토리를 통해 가져오는 방식을 예시로 듭니다.
    private val pluginSettings
        get() = pluginSettingsFactory.createGlobalSettings()

    override fun getLogFilePath(): String {
        // 저장된 값이 없을 경우 null 처리를 명확히
        return pluginSettings.get(KEY_LOG_FILE_PATH) as? String ?: ""
    }

    override fun setLogFilePath(path: String) {
        pluginSettings.put(KEY_LOG_FILE_PATH, path)
    }

    override fun getLineCount(): Int {
        // 저장된 값이 문자열이든 숫자든 유연하게 처리
        return when (val value = pluginSettings.get(KEY_LINE_COUNT)) {
            is Int -> value
            is String -> value.toIntOrNull() ?: DEFAULT_LINE_COUNT
            else -> DEFAULT_LINE_COUNT
        }
    }

    override fun setLineCount(count: Int) {
        // 가능하다면 Int 타입 그대로 저장 (구현체가 지원한다는 가정 하에)
        // 문자열 저장이 필수라면 count.toString() 유지
        pluginSettings.put(KEY_LINE_COUNT, count.toString())
    }
}
