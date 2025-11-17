package io.github.nagare.logging.log4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.Level;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * JUnit tests to test JsonLayout:
 * - Parse the JSON produced
 * - Comparing the parsed data with the attributes of the original log events
 * - Achieve coverage of 80% or better
 * - Use annotation-based JUnit5 tests.
 * How to parse JSON string with Jackson
 * <a href="https://mkyong.com/java/jackson-how-to-parse-json/">...</a>
 */
public class JsonLayoutTest {

    private Logger logger;
    private ObjectMapper mapper;
    private JsonLayout layout;

    // Set up object can be reused for tests
    @BeforeEach
    public void setUp(){
        logger = Logger.getLogger("JsonLayout");
        mapper = new ObjectMapper();
        layout = new JsonLayout();
    }

    // Helper method to create events when needed
    private LoggingEvent createEvent(Level level, String message) {
        return new LoggingEvent(
                logger.getName(),   // logger name
                logger,             // logger
                level,              // level
                message,            // message
                null                // throwable (null = no exception)
        );
    }

    // Tests for format(LoggingEvent loggingEvent)

    @Test
    public void testFormat1() {
        // Validate JSON strings
        LoggingEvent event = createEvent(Level.WARN, "something went wrong");
        String result = layout.format(event);
        System.out.println(result);
        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
        assertTrue(result.contains("\"logger\":\"JsonLayout\""));
        assertTrue(result.contains("\"level\":\"WARN\""));
        assertTrue(result.contains("\"timestamp\":"));
        String afterTimestamp = result.split("\"timestamp\":\"")[1];
        String timestampValue = afterTimestamp.split("\"")[0];
        assertDoesNotThrow(() -> Instant.parse(timestampValue));
        assertTrue(result.contains("\"thread\":\"main\""));
        assertTrue(result.contains("\"message\":\"something went wrong\""));
    }

    @Test
    public void testFormat2() throws JsonProcessingException {
        // Validate JSON object
        LoggingEvent event = createEvent(Level.ERROR, "some error");
        String result = layout.format(event);
        JsonNode node = mapper.readTree(result); // parse the JSON produced
        assertEquals(7, node.size());
        assertTrue(node.has("logger"));
        assertTrue(node.has("level"));
        assertTrue(node.has("timestamp"));
        assertTrue(node.has("thread"));
        assertTrue(node.has("message"));
        assertEquals(event.getLoggerName(), node.get("logger").asText());
        assertEquals(event.getLevel().toString(), node.get("level").asText());
        long stamp = event.getTimeStamp();
        Instant instant = Instant.ofEpochMilli(stamp);
        String expectedStamp = DateTimeFormatter.ISO_INSTANT.format(instant);
        assertEquals(expectedStamp, node.get("timestamp").asText());
        assertEquals(event.getThreadName(), node.get("thread").asText());
        assertEquals(event.getMessage().toString(), node.get("message").asText());
        assertTrue(node.get("id").isNull());
        assertTrue(node.get("errorDetails").isNull());
    }

    @Test
    public void testFormat3() throws JsonProcessingException {
        // Validate problematic characters
        String[] messages = {
                null,
                "",
                "Message with \"quotes\"",
                "Line\nbreak",
                "Tab\there",
                "Backslash\\here"
        };
        for (String message: messages) {
            LoggingEvent event = createEvent(Level.INFO, message);
            String result = layout.format(event);
            JsonNode node = mapper.readTree(result); // parse the JSON produced
            if (message == null) {
                assertTrue(node.get("message").isNull());
            } else {
                assertEquals(message, node.get("message").asText());
            }
        }
    }

    // Tests for ignoresThrowable()

    @Test
    public void testIgnoresThrowable() {
        assertTrue(layout.ignoresThrowable()); // always true
    }

    // Tests for activateOptions()

    @Test
    public void testActivateOptions() {
        // Test method exists and doesn't throw exceptions
        layout.activateOptions();
        // Verify no state change
        String hashBefore = String.valueOf(layout.hashCode());
        layout.activateOptions();
        String hashAfter = String.valueOf(layout.hashCode());
        assertEquals(hashBefore, hashAfter);
    }

}