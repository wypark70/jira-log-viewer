package com.atsoft.jira.plugin.logviewer.action

import com.atlassian.jira.web.action.JiraWebActionSupport
import com.atlassian.jira.security.request.SupportedMethods
import com.atlassian.jira.security.request.RequestMethod
@SupportedMethods(RequestMethod.GET)
open class LogViewerAction() : JiraWebActionSupport() {

    val logs: List<String> = listOf(
        "2026-01-04 04:20 INFO Application started",
        "2026-01-04 04:21 WARN Disk usage high",
        "2026-01-04 04:22 ERROR Connection timeout"
    )

    override fun doExecute(): String {
        return INPUT
    }
}
