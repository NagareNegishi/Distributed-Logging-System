package io.github.nagare.logging.log4j;

import java.lang.management.ManagementFactory;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;


/**
 * Appender sends logs via HTTP POST to LogServlet.
 * It will use JsonLayout to convert LoggingEvent for valid input for LogServlet.
 * It will use localhost for development, but user need to provide url of where LogServlet runs.
 */
public class HttpAppender extends AppenderSkeleton implements HttpAppenderMBean {

    private static final String DEFAULT_URL = "http://localhost:8080/logstore/logs"; // for development stage
    private static int instanceCounter = 0;

    private String url = DEFAULT_URL;
    private final JsonLayout jsonLayout = new JsonLayout();
    private HttpClient httpClient;
    private long successCount = 0;
    private long failureCount = 0;

    /**
     * Constructor - initialize HTTP client
     */
    public HttpAppender() {
        this.httpClient = HttpClient.newHttpClient();
        instanceCounter++;
        setName("HttpAppenderMBean-" + instanceCounter);
    }

    // Getter
    public String getUrl() { return url; }
    public long getSuccessCount() { return successCount; }
    public long getFailureCount() { return failureCount; }

    // Setter
    public void setUrl(String url) { this.url = url; }

    /**
     * Overrides AppenderSkeleton's setName() to add MBean registration.
     * @param name name of this appender
     */
    @Override
    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        unregisterMBean();
        super.setName(name);
        registerMBean();
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
        String json = jsonLayout.format(loggingEvent);
        sendHttpPost(json);
    }

    /**
     * Sends JSON log data to the server via HTTP POST.
     * Failed attempt do not throw RuntimeException, as Logging failure shouldn't stop the app
     * @param json LogEvent in json format
     */
    private void sendHttpPost(String json) {
        try {
            var request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .uri(URI.create(url))
                    .headers("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Success check
            int status = response.statusCode();
            if (status == 200 || status == 201) {
                successCount++;
                return;
            }
            failureCount++;
            System.err.println("HttpAppender: Server returned status " + status + " - " + response.body());

        } catch (ConnectException e) {
            failureCount++;
            System.err.println("HttpAppender: Server not available at " + url);
        } catch (InterruptedException e) {
            failureCount++;
            System.err.println("HttpAppender: Request interrupted");
            Thread.currentThread().interrupt(); // Restore interrupt status
        } catch (Exception e) {
            failureCount++;
            System.err.println("HttpAppender: " + e.getMessage());
        }
    }

    /**
     * Release any resources allocated within the appender.
     * It is a programming error to append to a closed appender.
     */
    @Override
    public void close() {
        if (closed) { return; }
        httpClient = null;
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

    /**
     * Register this MemAppender as an MBean with the platform MBeanServer
     */
    private void registerMBean() {
        try {
            // package name + type = class name + name = instance name
            ObjectName objectName = new ObjectName("io.github.nagare.logging.log4j:type=HttpAppender,name=" + this.name);
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
     * Unregistered this MemAppender as an MBean with the platform MBeanServer
     */
    private void unregisterMBean() {
        if (this.name == null) {
            return;
        }
        try {
            ObjectName objectName = new ObjectName("io.github.nagare.logging.log4j:type=HttpAppender,name=" + this.name);
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            if (server.isRegistered(objectName)) {
                server.unregisterMBean(objectName);
            }
        } catch (Exception e) { // checked exceptions are difficult to simulate for this private method
            throw new RuntimeException("Failed to unregister MBean: " + e.getMessage(), e);
        }
    }

}
