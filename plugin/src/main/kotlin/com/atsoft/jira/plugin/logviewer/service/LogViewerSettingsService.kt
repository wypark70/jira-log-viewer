package com.atsoft.jira.plugin.logviewer.service

interface LogViewerSettingsService {
    fun getLogFilePath(): String
    fun setLogFilePath(path: String)
    fun getLineCount(): Int
    fun setLineCount(count: Int)
}
