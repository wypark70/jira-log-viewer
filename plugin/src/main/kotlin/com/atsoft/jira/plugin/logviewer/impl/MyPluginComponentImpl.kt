package com.atsoft.jira.plugin.logviewer.impl

import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.atlassian.sal.api.ApplicationProperties
import com.atsoft.jira.plugin.logviewer.api.MyPluginComponent
import javax.inject.Inject
import javax.inject.Named

@ExportAsService(MyPluginComponent::class)
@Named("myPluginComponent")
class MyPluginComponentImpl @Inject constructor(
    @param:ComponentImport private val applicationProperties: ApplicationProperties?
) : MyPluginComponent {

    override val name: String
        get() = "myComponent" + (applicationProperties?.displayName?.let { ":$it" } ?: "")
}
