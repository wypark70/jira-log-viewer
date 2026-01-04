package com.atsoft.jira.plugin.logviewer.service.impl

import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
import com.atsoft.jira.plugin.logviewer.service.LogViewerSettingsService

@ExportAsService(LogViewerSettingsService::class)
open class LogViewerSettingsServiceImpl(
    private val pluginSettingsFactory: PluginSettingsFactory
) : LogViewerSettingsService {

    private val PLUGIN_KEY = "com.atsoft.jira.plugin.logviewer"
    private val KEY_LOG_FILE_PATH = "$PLUGIN_KEY.logFilePath"
    private val KEY_LINE_COUNT = "$PLUGIN_KEY.lineCount"

    override fun getLogFilePath(): String {
        return pluginSettingsFactory.createGlobalSettings().get(KEY_LOG_FILE_PATH) as? String ?: ""
    }

    override fun setLogFilePath(path: String) {
        pluginSettingsFactory.createGlobalSettings().put(KEY_LOG_FILE_PATH, path)
    }

    override fun getLineCount(): Int {
        val value = pluginSettingsFactory.createGlobalSettings().get(KEY_LINE_COUNT) as? String
        return value?.toIntOrNull() ?: 100 // Default to 100 lines
    }

    override fun setLineCount(count: Int) {
        pluginSettingsFactory.createGlobalSettings().put(KEY_LINE_COUNT, count.toString())
    }
}
