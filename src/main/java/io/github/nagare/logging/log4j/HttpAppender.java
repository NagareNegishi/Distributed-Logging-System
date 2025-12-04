package io.github.nagare.logging.log4j;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.AppenderSkeleton;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.net.ConnectException;

/**
 * Appender sends logs via HTTP POST to LogServlet.
 * It will use JsonLayout to convert LoggingEvent for valid input for LogServlet.
 * It will use localhost for development, but user need to provide url of where LogServlet runs.
 */
public class HttpAppender extends AppenderSkeleton  {

    private String url = "http://localhost:8080/logstore/logs"; // for development stage
    private JsonLayout jsonLayout = new JsonLayout();
    private HttpClient httpClient;

    private long successCount = 0;
    private long failureCount = 0;

    // Getter
    public String getUrl() {
        return url;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public long getFailureCount() {
        return failureCount;
    }

    // Setter
    public void setUrl(String url) {
        this.url = url;
    }


    /**
     * Subclasses of AppenderSkeleton should implement this method to perform actual logging
     * @param loggingEvent event to be logged
     */
    @Override
    protected void append(LoggingEvent loggingEvent) {

    }

    /**
     * Sends JSON log data to the server via HTTP POST.
     * @param json LogEvent in json format
     */
    private void sendHttpPost(String json) {
        // HTTP POST implementation
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
