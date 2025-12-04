package io.github.nagare.logging.log4j;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MemAppenderTest {

    private Logger logger;
    private MemAppender appender;

    // Set up object can be reused for tests
    @BeforeEach
    public void setUp() {
        logger = Logger.getLogger("MemAppender");
        appender = new MemAppender();
    }

    // Unregisters MBean
    @AfterEach
    public void cleanUp() {
        // Because of setup, we know it exist
        cleanupAppender(appender);
    }

    // Helper method to Unregisters MBean
    private void cleanupAppender(MemAppender appender) {
        try {
            ObjectName objectName = new ObjectName("io.github.nagare.logging.log4j:type=MemAppender,name=" + appender.getName());
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            if (server.isRegistered(objectName)) {
                server.unregisterMBean(objectName);
            }
        } catch (Exception ignored) {}
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

    @Test
    public void testConstructor1() {
        // test default behavior
        assertNotNull(appender.getName());
        assertTrue(appender.getName().startsWith("MemAppenderMBean-"));
        assertEquals(1000, appender.getMaxSize());
        assertEquals(0, appender.getDiscardedLogCount());
        assertTrue(appender.getCurrentLogs().isEmpty());
    }

    @Test
    public void testConstructor2() {
        // test default behavior with different instance
        MemAppender appender2 = new MemAppender();
        assertTrue(appender2.getName().startsWith("MemAppenderMBean-"));
        assertNotEquals(appender.getName(), appender2.getName());
        cleanupAppender(appender2);
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
    public void testAddLog1() {
        LoggingEvent event = createEvent(Level.ALL, "test1");
        appender.addLog(event);
        List<LoggingEvent> logs = appender.getCurrentLogs();
        assertEquals(1, logs.size());
        assertEquals(event, logs.get(0));
        assertEquals(0, appender.getDiscardedLogCount());
    }

    @Test
    public void testAddLog2() {
        // test the oldest logs should be discarded
        LoggingEvent event1 = createEvent(Level.ALL, "test1");
        LoggingEvent event2 = createEvent(Level.TRACE, "test2");
        LoggingEvent event3 = createEvent(Level.DEBUG, "test3");
        appender.addLog(event1);
        appender.addLog(event2);
        List<LoggingEvent> logs = appender.getCurrentLogs();
        assertEquals(2, logs.size());
        assertEquals(event1, logs.get(0));
        assertEquals(event2, logs.get(1));
        assertEquals(0, appender.getDiscardedLogCount());

        appender.setMaxSize(2);
        assertEquals(2, logs.size());
        assertEquals(event1, logs.get(0));
        assertEquals(event2, logs.get(1));
        assertEquals(0, appender.getDiscardedLogCount());

        appender.addLog(event3);
        assertEquals(2, logs.size());
        assertFalse(logs.contains(event1));
        assertEquals(event2, logs.get(0));
        assertEquals(event3, logs.get(1));
        assertEquals(1, appender.getDiscardedLogCount());
    }

    @Test
    public void testSetMaxSize1() {
        // test adjust the maximum number of logs accordingly
        LoggingEvent event1 = createEvent(Level.ALL, "test1");
        LoggingEvent event2 = createEvent(Level.TRACE, "test2");
        LoggingEvent event3 = createEvent(Level.DEBUG, "test3");
        appender.addLog(event1);
        appender.addLog(event2);
        appender.addLog(event3);

        List<LoggingEvent> logs = appender.getCurrentLogs();
        assertEquals(3, logs.size());
        assertEquals(event1, logs.get(0));
        assertEquals(event2, logs.get(1));
        assertEquals(event3, logs.get(2));
        assertEquals(0, appender.getDiscardedLogCount());

        appender.setMaxSize(2);
        assertEquals(2, logs.size());
        assertFalse(logs.contains(event1));
        assertEquals(event2, logs.get(0));
        assertEquals(event3, logs.get(1));
        assertEquals(1, appender.getDiscardedLogCount());
    }

    @Test
    public void testSetMaxSize2() {
        // test invalid max size
        LoggingEvent event = createEvent(Level.ALL, "test1");
        appender.addLog(event);
        appender.setMaxSize(0);
        List<LoggingEvent> logs = appender.getCurrentLogs();
        assertEquals(0, logs.size());
        assertFalse(logs.contains(event));
        assertEquals(1, appender.getDiscardedLogCount());
        assertThrows(IllegalArgumentException.class, () -> appender.setMaxSize(-1));
    }

    @Test
    public void testGetCurrentLogs() {
        // test list not be modifiable
        List<LoggingEvent> events = createEvents(5);
        events.forEach(appender::addLog);
        List<LoggingEvent> logs = appender.getCurrentLogs();
        assertThrows(UnsupportedOperationException.class, () -> logs.add(createEvent(Level.TRACE, "Add")));
        assertThrows(UnsupportedOperationException.class, () -> logs.remove(0));
        assertThrows(UnsupportedOperationException.class, logs::clear);
    }

    @Test
    public void testExport1() throws java.io.IOException {
        // test no logs
        String fileName = "test-export.json";
        appender.export(fileName);
        String content = Files.readString(Paths.get(fileName));
        assertEquals("[]", content);
        Files.deleteIfExists(Paths.get(fileName));
    }

    @Test
    public void testExport2()  throws java.io.IOException {
        // test one log
        LoggingEvent event = createEvent(Level.ALL, "test1");
        appender.addLog(event);
        JsonLayout layout = new JsonLayout();
        String expected = layout.format(event);
        String fileName = "test-export.json";

        appender.export(fileName);
        String content = Files.readString(Paths.get(fileName));
        assertEquals("[" + expected + "]", content);
        Files.deleteIfExists(Paths.get(fileName));
    }

    @Test
    public void testExport3()  throws java.io.IOException {
        // test multiple logs
        List<LoggingEvent> events = createEvents(10);
        events.forEach(appender::addLog);
        JsonLayout layout = new JsonLayout();
        String expected = "[" +
                events.stream()
                        .map(layout::format)
                        .collect(Collectors.joining(","))
                + "]";
        String fileName = "test-export.json";

        appender.export(fileName);
        String content = Files.readString(Paths.get(fileName));
        assertEquals(expected, content);
        Files.deleteIfExists(Paths.get(fileName));
    }

    @Test
    public void testExport4() {
        // test null and empty
        List<LoggingEvent> events = createEvents(10);
        events.forEach(appender::addLog);

        IllegalArgumentException testNull = assertThrows(
                IllegalArgumentException.class,
                () -> appender.export(null)
        );
        assertEquals("fileName cannot be null", testNull.getMessage());

        RuntimeException emptyTest = assertThrows(
                RuntimeException.class,
                () -> appender.export("")
        );
        assertTrue(emptyTest.getMessage().startsWith("Error writing file: "));
    }

    // Additional test after MBean

    @Test
    public void testGetLogCount1() {
        // test no logs
        assertEquals(0, appender.getLogCount());
    }

    @Test
    public void testGetLogCount2() {
        // test multiple logs
        List<LoggingEvent> events = createEvents(10);
        events.forEach(appender::addLog);
        assertEquals(10, appender.getLogCount());
    }

    @Test
    public void testGetLogCount3() {
        // test adjust the maximum number of logs
        List<LoggingEvent> events = createEvents(10);
        events.forEach(appender::addLog);
        appender.setMaxSize(5);
        assertEquals(5, appender.getLogCount());
    }

    @Test
    public void testGetLogs1() {
        // test no logs
        assertArrayEquals(new String[0], appender.getLogs());
        assertEquals(0, appender.getLogs().length);
    }

    @Test
    public void testGetLogs2() {
        // test one logs
        LoggingEvent event = createEvent(Level.ALL, "test1");
        PatternLayout patternLayout = new PatternLayout(); // "%m%n"
        String expectedString = patternLayout.format(event);
        String[] expected = {expectedString};
        appender.addLog(event);
        String[] actual = appender.getLogs();
        assertArrayEquals(expected, actual);
        assertEquals(1, actual.length);
        assertTrue(actual[0].contains("test1"));
        assertEquals(expectedString, actual[0]);
    }

    @Test
    public void testGetLogs3() {
        // test multiple logs
        List<LoggingEvent> events = createEvents(20);
        PatternLayout patternLayout = new PatternLayout(); // "%m%n"
        String[] expected = events.stream()
                .map(patternLayout::format)
                .toArray(String[]::new);
        events.forEach(appender::addLog);
        String[] actual = appender.getLogs();
        assertArrayEquals(expected, actual);
        assertEquals(20, actual.length);
    }

    @Test
    public void testSetName2() throws Exception {
        // verify registration
        appender.setName("TestRegister");
        ObjectName expected = new ObjectName("io.github.nagare.logging.log4j:type=MemAppender,name=" + "TestRegister");
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        assertTrue(server.isRegistered(expected));
        server.unregisterMBean(expected);
    }

    @Test
    public void testSetName3() throws Exception {
        // test unregister
        appender.setName("TestRegister");
        ObjectName expected = new ObjectName("io.github.nagare.logging.log4j:type=MemAppender,name=" + "TestRegister");
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        appender.setName("TestRegister"); // unregistered and re-registered
        assertTrue(server.isRegistered(expected));

        // test InstanceAlreadyExistsException
        MemAppender appender2 = new MemAppender(); // Need another instance to cause name conflict
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

    @Test
    public void testAppend() {
        List<LoggingEvent> events = createEvents(3);
        events.forEach(appender::append);
        assertEquals(3, appender.getLogCount());
        assertEquals(events, appender.getCurrentLogs());
    }

    @Test
    public void testClose1() {
        // test with doAppend
        List<LoggingEvent> events = createEvents(5);
        events.forEach(appender::append);
        appender.close();
        assertEquals(0, appender.getLogCount());
        // doAppend will log and return. You will see "Attempted to append to closed appender" messages,
        // this expected behaviour is suppressed below
        org.apache.log4j.helpers.LogLog.setQuietMode(true);
        events.forEach(appender::doAppend);
        org.apache.log4j.helpers.LogLog.setQuietMode(false);
        assertEquals(0, appender.getLogCount());
    }

    @Test
    public void testClose2() {
        // test with append
        List<LoggingEvent> events = createEvents(5);
        events.forEach(appender::append);
        appender.close();
        assertEquals(0, appender.getLogCount());
        assertThrows(IllegalStateException.class, () -> events.forEach(appender::append));
        assertEquals(0, appender.getLogCount());

        // test close, closed appender. Do nothing
        appender.close();
    }

    @Test
    public void testRequiresLayout() {
        assertFalse(appender.requiresLayout());
    }

}

