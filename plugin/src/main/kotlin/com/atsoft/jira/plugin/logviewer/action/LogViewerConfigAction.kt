package com.atsoft.jira.plugin.logviewer.action

import com.atlassian.jira.security.request.RequestMethod
import com.atlassian.jira.security.request.SupportedMethods
import com.atlassian.jira.web.action.JiraWebActionSupport
import com.atsoft.jira.plugin.logviewer.service.LogViewerSettingsService

@SupportedMethods(RequestMethod.GET, RequestMethod.POST)
open class LogViewerConfigAction(
    private val settingsService: LogViewerSettingsService
) : JiraWebActionSupport() {

    var logFilePath: String = ""
    var lineCount: Int = 100
    var isSaved: Boolean = false

    override fun doDefault(): String {
        logFilePath = settingsService.getLogFilePath()
        lineCount = settingsService.getLineCount()
        return INPUT
    }

    override fun doValidation() {
        if (logFilePath.isBlank()) {
            addError("logFilePath", getText("logviewer.config.error.path.required"))
        }
        if (lineCount <= 0) {
            addError("lineCount", getText("logviewer.config.error.linecount.invalid"))
        }
    }

    override fun doExecute(): String {
        settingsService.setLogFilePath(logFilePath)
        settingsService.setLineCount(lineCount)
        
        isSaved = true
        
        return INPUT // Stay on the same page
    }
}
