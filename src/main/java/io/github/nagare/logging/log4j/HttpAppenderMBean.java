package io.github.nagare.logging.log4j;

/**
 * MBean interface for HttpAppender.
 * By implementing this interface, the HttpAppender class becomes JMX-compliant,
 * and can be registered as a managed bean.
 * The MBean class MUST implement an interface with the following name: “class name” plus MBean.
 */
public interface HttpAppenderMBean {

    /**
     * Get the number of successful append operation
     * @return the number of successful append operation
     */
    public long getSuccessCount();

    /**
     * Get the number of failed append operation
     * @return the number of failed append operation
     */
    public long getFailureCount();

}
