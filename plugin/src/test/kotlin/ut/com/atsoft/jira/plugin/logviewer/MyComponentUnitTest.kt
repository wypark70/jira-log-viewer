package ut.com.atsoft.jira.plugin.logviewer

import com.atsoft.jira.plugin.logviewer.api.MyPluginComponent
import com.atsoft.jira.plugin.logviewer.impl.MyPluginComponentImpl
import lombok.extern.slf4j.Slf4j
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@Slf4j
class MyComponentUnitTest {
    @Test
    fun testMyName() {
        val component: MyPluginComponent = MyPluginComponentImpl(null)
        Assertions.assertEquals("myComponent", component.name, "names do not match!")
    }
}
