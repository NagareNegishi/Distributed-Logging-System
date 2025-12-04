package io.github.nagare.logging.log4j;

/**
 * MBean interface for MemAppender.
 * By implementing this interface, the MemAppender class becomes JMX-compliant,
 * and can be registered as a managed bean.
 * The MBean class MUST implement an interface with the following name: “class name” plus MBean.
 */
public interface MemAppenderMBean {

    /**
     * Get log events converted to Array of strings,
     * the string representation of a LoggingEvent is obtained using
     * org.apache.log4j.PatternLayout with the default conversion pattern.
     * @return array of formatted log strings
     */
    public String[] getLogs();

    /**
     * Get the number of currently stored logs (excluding discarded logs)
     * @return the number of currently stored logs
     */
    public long getLogCount();

    /**
     * Get the number of discarded logs
     * @return the number of discarded logs
     */
    public long getDiscardedLogCount();

    /**
     * Export the log events in the memory appender to a file in JSON format
     * @param fileName target file name
     */
    public void export(String fileName);
}
