package ut.com.atsoft.jira.plugin.logviewer;

import org.junit.jupiter.api.Test;
import com.atsoft.jira.plugin.logviewer.api.MyPluginComponent;
import com.atsoft.jira.plugin.logviewer.impl.MyPluginComponentImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MyComponentUnitTest {
    @Test
    public void testMyName() {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("myComponent", component.getName(), "names do not match!");
    }
}