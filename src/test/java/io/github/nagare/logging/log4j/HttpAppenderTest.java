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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

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

    // Unregisters MBean
    @AfterEach
    public void cleanUp() {
        cleanupAppender(appender);
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

    // Helper method to create a list of events when needed
    private List<LoggingEvent> createEvents(int count) {
        List<LoggingEvent> events = new ArrayList<>();
        Level[] levels = {Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL};
        for (int i = 0; i < count; i++) {
            events.add(createEvent(levels[i % levels.length], "test" + i));
        }
        return events;
    }

    // Helper method to check server
    private boolean isServerRunning(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(2))
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> response = TEST_CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofString());

            return response.statusCode() < 500;  // Server responded
        } catch (Exception e) {
            return false;  // Server not reachable
        }
    }

    // Helper method to Unregisters MBean
    private void cleanupAppender(HttpAppender appender) {
        try {
            ObjectName objectName = new ObjectName("io.github.nagare.logging.log4j:type=HttpAppender,name=" + appender.getName());
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            if (server.isRegistered(objectName)) {
                server.unregisterMBean(objectName);
            }
        } catch (Exception ignored) {}
    }


    @Test
    public void testConstructor1() {
        // test default behavior
        assertNotNull(appender.getName());
        assertTrue(appender.getName().startsWith("HttpAppenderMBean-"));
        assertEquals("http://localhost:8080/logstore/logs", appender.getUrl());
        assertEquals(0, appender.getSuccessCount());
        assertEquals(0, appender.getFailureCount());
    }

    @Test
    public void testConstructor2() {
        // test default behavior with different instance
        HttpAppender appender2 = new HttpAppender();
        assertTrue(appender2.getName().startsWith("HttpAppenderMBean-"));
        assertNotEquals(appender.getName(), appender2.getName());
        cleanupAppender(appender2);
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
        Assumptions.assumeTrue(isServerRunning("http://localhost:8080/logstore/logs"),
                "Server must be running for this test");
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
        Assumptions.assumeTrue(isServerRunning("http://localhost:8080/logstore/logs"),
                "Server must be running for this test");
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

    @Test
    public void testSetName1() {
        appender.setName("Test1");
        assertEquals("Test1", appender.getName());
        appender.setName("Test2");
        assertNotEquals("Test1", appender.getName());
        assertEquals("Test2", appender.getName());
    }

    @Test
    public void testSetName2() throws Exception {
        // verify registration
        appender.setName("TestRegister");
        ObjectName expected = new ObjectName("io.github.nagare.logging.log4j:type=HttpAppender,name=" + "TestRegister");
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        assertTrue(server.isRegistered(expected));
        server.unregisterMBean(expected);
    }

    @Test
    public void testSetName3() throws Exception {
        // test unregister
        appender.setName("TestRegister");
        ObjectName expected = new ObjectName("io.github.nagare.logging.log4j:type=HttpAppender,name=" + "TestRegister");
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        appender.setName("TestRegister"); // unregistered and re-registered
        assertTrue(server.isRegistered(expected));

        // test InstanceAlreadyExistsException
        HttpAppender appender2 = new HttpAppender(); // Need another instance to cause name conflict
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> appender2.setName("TestRegister"));
        assertTrue(exception.getMessage().startsWith("MBean already registered with this name: "));
        server.unregisterMBean(expected);
    }

    @Test
    public void testSetName4() {
        // test MalformedObjectNameException
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> appender.setName("="));
        assertTrue(exception.getMessage().startsWith("Invalid MBean name: "));
    }

    @Test
    public void testSetName5() {
        // test null and empty
        assertThrows(IllegalArgumentException.class, () -> appender.setName(null));
        assertThrows(IllegalArgumentException.class, () -> appender.setName(""));
    }

}
