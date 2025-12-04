package io.github.nagare.logging.log4j;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.Level;

import java.util.List;
import java.util.ArrayList;
import java.time.Duration;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Assumptions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HttpAppenderTest {

    private Logger logger;
    private HttpAppender appender;
    private static final HttpClient TEST_CLIENT = HttpClient.newHttpClient();

    // Set up object can be reused for tests
    @BeforeEach
    public void setUp() {
        logger = Logger.getLogger("HttpAppender");
        appender = new HttpAppender();
    }

    // Helper method to create events when needed
    private LoggingEvent createEvent(Level level, String message, Throwable throwable) {
        return new LoggingEvent(
                logger.getName(),   // logger name
                logger,             // logger
                level,              // level
                message,            // message
                throwable           // throwable (null = no exception)
        );
    }

    // Helper method to create a list of events when needed
    private List<LoggingEvent> createEvents(int count) {
        List<LoggingEvent> events = new ArrayList<>();
        Level[] levels = {Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL};
        for (int i = 0; i < count; i++) {
            events.add(createEvent(levels[i % levels.length], "test" + i, null));
        }
        return events;
    }

    // Helper method to check server
    private boolean isServerRunning(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            HttpResponse<String> response = TEST_CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofString());

            return response.statusCode() < 500;  // Server responded
        } catch (Exception e) {
            return false;  // Server not reachable
        }
    }


    @Test
    public void testConstructor1() {
        // test default behavior
        assertNotNull(appender.getName());
        assertEquals("http://localhost:8080/logstore/logs", appender.getUrl());
        assertEquals(0, appender.getSuccessCount());
        assertEquals(0, appender.getFailureCount());
    }

    @Test
    public void testSetUrl() {
        appender.setUrl("http://test:8080/test/test");
        assertEquals("http://test:8080/test/test", appender.getUrl());
    }

    @Test
    public void testAppend() {
        Assumptions.assumeTrue(isServerRunning("http://localhost:8080/logstore/logs"),
                "Server must be running for this test");
        List<LoggingEvent> events = createEvents(3);
        events.forEach(appender::append);
        assertEquals(3, appender.getSuccessCount());
        assertEquals(0, appender.getFailureCount());
    }

    @Test
    public void testClose1() {
        // test with doAppend
        List<LoggingEvent> events = createEvents(5);
        events.forEach(appender::append);
        appender.close();
        assertEquals(5, appender.getSuccessCount());
        assertEquals(0, appender.getFailureCount());
        assertEquals("http://localhost:8080/logstore/logs", appender.getUrl());
        // doAppend will log and return. You will see "Attempted to append to closed appender" messages,
        // this expected behaviour is suppressed below
        org.apache.log4j.helpers.LogLog.setQuietMode(true);
        events.forEach(appender::doAppend);
        org.apache.log4j.helpers.LogLog.setQuietMode(false);
        assertEquals(5, appender.getSuccessCount());
        assertEquals(0, appender.getFailureCount());
        assertEquals("http://localhost:8080/logstore/logs", appender.getUrl());
    }

    @Test
    public void testClose2() {
        // test with append
        List<LoggingEvent> events = createEvents(5);
        events.forEach(appender::append);
        appender.close();
        assertEquals(5, appender.getSuccessCount());
        assertEquals(0, appender.getFailureCount());
        assertEquals("http://localhost:8080/logstore/logs", appender.getUrl());
        assertThrows(IllegalStateException.class, () -> events.forEach(appender::append));
        assertEquals(5, appender.getSuccessCount());
        assertEquals(0, appender.getFailureCount());
        assertEquals("http://localhost:8080/logstore/logs", appender.getUrl());

        // test close, closed appender. Do nothing
        appender.close();
    }

    @Test
    public void testRequiresLayout() {
        assertFalse(appender.requiresLayout());
    }



}
