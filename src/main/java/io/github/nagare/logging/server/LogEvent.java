package io.github.nagare.logging.server;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Unified log event model used across all system components.
 */
public class LogEvent {

    private String id; // $uuid - generated server-side if not provided

    @JsonProperty(required = true)
    private String message;

    @JsonProperty(required = true)
    private String timestamp; // ISO-8601 format: "2024-11-17T14:22:15.123Z"

    @JsonProperty(required = true)
    private String thread; // where the log event has occurred

    @JsonProperty(required = true)
    private String logger; // name property of the logger

    @JsonProperty(required = true)
    private String level; // ALL, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF

    @JsonProperty
    private String errorDetails; // Optional, the stack trace of the error or exception that has been logged

    /**
     * Default no-argument constructor for Jackson
     */
    public LogEvent() {
    }

    // getters
    public String getId() { return id; }
    public String getMessage() { return message; }
    public String getTimestamp() { return timestamp; }
    public String getThread() { return thread; }
    public String getLogger() { return logger; }
    public String getLevel() { return level; }
    public String getErrorDetails() { return errorDetails; }

    // setters
    public void setId(String id) {
        this.id = id;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public void setLevel(String level) {
        this.level = level.toUpperCase();
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LogEvent logEvent = (LogEvent) o;
        return Objects.equals(message, logEvent.message) &&
                Objects.equals(timestamp, logEvent.timestamp) &&
                Objects.equals(thread, logEvent.thread) &&
                Objects.equals(logger, logEvent.logger) &&
                Objects.equals(level, logEvent.level);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, timestamp, thread, logger, level);
    }
}
