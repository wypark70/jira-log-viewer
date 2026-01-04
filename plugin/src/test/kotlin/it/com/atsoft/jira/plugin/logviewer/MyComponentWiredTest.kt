package it.com.atsoft.jira.plugin.logviewer

import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner
import com.atlassian.sal.api.ApplicationProperties
import com.atsoft.jira.plugin.logviewer.api.MyPluginComponent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AtlassianPluginsTestRunner::class)
class MyComponentWiredTest(private val applicationProperties: ApplicationProperties, private val myPluginComponent: MyPluginComponent) {
    @Test
    fun testMyName() {
        assertEquals("names do not match!", "myComponent:" + applicationProperties.getDisplayName(), myPluginComponent.name)
    }
}
