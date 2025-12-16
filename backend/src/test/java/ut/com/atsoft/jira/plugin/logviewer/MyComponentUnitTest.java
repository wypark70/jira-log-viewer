package ut.com.atsoft.jira.plugin.logviewer;

import org.junit.Test;
import com.atsoft.jira.plugin.logviewer.api.MyPluginComponent;
import com.atsoft.jira.plugin.logviewer.impl.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest {
    @Test
    public void testMyName() {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent", component.getName());
    }
}