package it.com.atsoft.jira.plugin.logviewer

import org.junit.Test
import org.junit.runner.RunWith
import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner
import com.atsoft.jira.plugin.logviewer.api.MyPluginComponent
import com.atlassian.sal.api.ApplicationProperties
import org.junit.Assert.assertEquals

@RunWith(AtlassianPluginsTestRunner::class)
class MyComponentWiredTest(private val applicationProperties: ApplicationProperties, private val myPluginComponent: MyPluginComponent) {
    @Test
    fun testMyName() {
        assertEquals("names do not match!", "myComponent:" + applicationProperties.getDisplayName(), myPluginComponent.name)
    }
}
