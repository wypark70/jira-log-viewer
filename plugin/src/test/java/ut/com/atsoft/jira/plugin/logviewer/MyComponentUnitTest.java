package ut.com.atsoft.jira.plugin.logviewer;

import com.atsoft.jira.plugin.logviewer.api.MyPluginComponent;
import com.atsoft.jira.plugin.logviewer.impl.MyPluginComponentImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigpwned.chardet4j.com.ibm.icu.text.CharsetDetector;
import com.sigpwned.chardet4j.com.ibm.icu.text.CharsetMatch;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class MyComponentUnitTest {
    private static final String CONTENT_TYPE_JSON_UTF8 = MediaType.APPLICATION_JSON + ";charset=UTF-8";
    private static final String CONTENT_TYPE_JSON_UTF16 = MediaType.APPLICATION_JSON + ";charset=UTF-16";

    @Test
    public void testMyName() {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("myComponent", component.getName(), "names do not match!");
    }

}