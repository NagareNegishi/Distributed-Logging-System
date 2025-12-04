package io.github.nagare.logging.log4j;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.AppenderSkeleton;

import io.github.nagare.logging.log4j.JsonLayout;

/**
 * Appender sends logs via HTTP POST to LogServlet.
 * It will use JsonLayout to convert LoggingEvent for valid input for LogServlet.
 * It will use localhost for development, but user need to provide url of where LogServlet runs.
 */
public class HttpAppender extends AppenderSkeleton  {

    private String url = "http://localhost:8080/logstore/logs"; // for development stage
    private JsonLayout jsonLayout = new JsonLayout();

    /**
     * Subclasses of AppenderSkeleton should implement this method to perform actual logging
     * @param loggingEvent event to be logged
     */
    @Override
    protected void append(LoggingEvent loggingEvent) {

    }

    /**
     * Release any resources allocated within the appender.
     * It is a programming error to append to a closed appender.
     */
    @Override
    public void close() {

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
