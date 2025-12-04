package io.github.nagare.logging.log4j;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.AppenderSkeleton;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import javax.management.MalformedObjectNameException;
import java.lang.IllegalStateException;


/**
 * Appender that does not use I/O to append logs, but stores logs in memory.
 * <br>
 * Implements MBean interface to allow JMX monitoring.
 * The name of the MBean must include the value of the name property of the appender
 * <a href="https://www.baeldung.com/java-management-extensions">...</a>
 * <a href="https://docs.oracle.com/javase/tutorial/jmx/mbeans/standard.html">...</a>
 * <br>
 * Extends AppenderSkeleton to be compatible with org.apache.log4j.Logger.
 * This allows the appender to be added to loggers via logger.addAppender().
 * <a href="https://logging.apache.org/log4j/1.x/apidocs/org/apache/log4j/AppenderSkeleton.html">...</a>
 */
public class MemAppender extends AppenderSkeleton implements MemAppenderMBean {

    private long maxSize = 1000;
    private long discardedLogCount = 0;
    private final List<LoggingEvent> logs = new ArrayList<>();
    private static int instanceCounter = 0;

    /**
     * A public zero-argument constructor
     * Create an MBean object for each instance of the MemAppender
     */
    public MemAppender() {
        instanceCounter++;
        setName("MemAppenderMBean-" + instanceCounter);
    }


    /**
     * Overrides AppenderSkeleton's setName() to add MBean registration.
     * @param name name of this appender
     */
    @Override
    public void setName(String name) {
        // Basic validation only. Comprehensive validation prevents testing error scenarios
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        unregisterMBean();
        super.setName(name);
        registerMBean();
    }


    /**
     * Get the maximum number of logs it can store
     * @return the maximum number of logs it can store
     */
    public long getMaxSize() {
        return maxSize;
    }


    /**
     * Sets the maxSize of this appender and adjust the maximum number of logs accordingly
     * @param maxSize the maximum number of logs it can store
     */
    public void setMaxSize(long maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("maxSize cannot be negative");
        }
        this.maxSize = maxSize;
        // maxSize logs is acceptable
        while (logs.size() > maxSize) {
            logs.remove(0); // oldest log
            discardedLogCount++;
        }
    }


    /**
     * Get the number of discarded logs
     * @return the number of discarded logs
     */
    @Override
    public long getDiscardedLogCount(){
        return discardedLogCount;
    }


    /**
     * Getter for the unmodifiable list of all log events
     * @return the unmodifiable list of all log events
     */
    public List<LoggingEvent> getCurrentLogs(){
        return Collections.unmodifiableList(logs);
    }


    /**
     * Get the number of currently stored logs (excluding discarded logs)
     * @return the number of currently stored logs
     */
    @Override
    public long getLogCount() {
        return logs.size();
    }


    /**
     * Get log events converted to Array of strings,
     * the string representation of a LoggingEvent is obtained using
     * org.apache.log4j.PatternLayout with the default conversion pattern.
     * 'Currently set to the string "%m%n" which just prints the application supplied message' by Apache
     * @return array of formatted log strings
     */
    @Override
    public String[] getLogs() {
        PatternLayout layout = new PatternLayout(); // default conversion pattern
        return logs.stream()
                .map(layout::format)
                .toArray(String[]::new);
    }


    /**
     * Add LoggingEvent to list of all log events.
     * If the number of logs reaches maxSize, the oldest logs are discarded
     * @param loggingEvent log to add to the list of all log events
     */
    public void addLog(LoggingEvent loggingEvent){
        logs.add(loggingEvent);
        while (logs.size() > maxSize) { // can be if, but make it robust
            logs.remove(0); // oldest log
            discardedLogCount++;
        }
    }


    /**
     * Export stored log events to a JSON file (JSON array containing JSON objects).
     * @param fileName relative file names relative to the application working directory
     */
    @Override
    public void export(String fileName) {
        if (fileName == null) { // Basic validation only. Comprehensive validation prevents testing error scenarios
            throw new IllegalArgumentException("fileName cannot be null");
        }
        JsonLayout layout = new JsonLayout();
        List<String> jsonStrings = new ArrayList<>();
        // Convert all LoggingEvent to String
        for (LoggingEvent log: logs) {
            jsonStrings.add(layout.format(log));
        }
        String jsonArray = "[" + String.join(",", jsonStrings) + "]";
        try {
            Files.write(Paths.get(fileName), jsonArray.getBytes());
        } catch (java.io.IOException e){
            throw new RuntimeException("Error writing file: " + e.getMessage(), e);
        }
    }


    /**
     * Register this MemAppender as an MBean with the platform MBeanServer
     */
    private void registerMBean() {
        try {
            // package name + type = class name + name = instance name
            ObjectName objectName = new ObjectName("io.github.nagare.logging.log4j:type=MemAppender,name=" + this.name);
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            server.registerMBean(this, objectName);
        } catch (InstanceAlreadyExistsException e) {
            throw new RuntimeException("MBean already registered with this name: " + e.getMessage(), e);
        } catch (MBeanRegistrationException | NotCompliantMBeanException e) {
            throw new RuntimeException("MBean registration error: " + e.getMessage(), e);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Invalid MBean name: " + e.getMessage(), e);
        }
    }


    /**
     * Unregisters this MemAppender as an MBean with the platform MBeanServer
     */
    private void unregisterMBean() {
        if (this.name == null) {
            return;
        }
        try {
            ObjectName objectName = new ObjectName("io.github.nagare.logging.log4j:type=MemAppender,name=" + this.name);
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            if (server.isRegistered(objectName)) {
                server.unregisterMBean(objectName);
            }
        } catch (Exception e) { // checked exceptions are difficult to simulate for this private method
            throw new RuntimeException("Failed to unregister MBean: " + e.getMessage(), e);
        }
    }


    /**
     * Subclasses of AppenderSkeleton should implement this method to perform actual logging
     * @param loggingEvent event to be logged
     */
    @Override
    protected void append(LoggingEvent loggingEvent) {
        // If Appender is closed, AppenderSkeleton's doAppend() prevent this method to be called
        // However since I need to test append() directly, I need this extra check
        if (closed) {
            throw new IllegalStateException("Cannot append to a closed appender");
        }
        addLog(loggingEvent);
    }


    /**
     * Release any resources allocated within the appender.
     * It is a programming error to append to a closed appender.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        logs.clear();
        closed = true; // AppenderSkeleton's doAppend() will check this
    }


    /**
     * Configurators call this method to determine if the appender requires a layout.
     * @return false, a layout is not required
     */
    @Override
    public boolean requiresLayout() {
        return false;
    }
}
