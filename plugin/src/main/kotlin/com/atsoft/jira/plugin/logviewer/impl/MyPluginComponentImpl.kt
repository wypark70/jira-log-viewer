package com.atsoft.jira.plugin.logviewer.impl;

import com.atsoft.jira.plugin.logviewer.api.MyPluginComponent;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;

@ExportAsService(MyPluginComponent::class)
class MyPluginComponentImpl : MyPluginComponent {
    override val name: String? = "myComponent"
}