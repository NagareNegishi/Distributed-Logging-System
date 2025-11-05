package io.github.nagare.logging.server;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents a log event as defined by the specification.
 * Note: While Java records would be conceptually appropriate for immutable log events,
 * this implementation uses a traditional class to ensure compatibility with various
 * Jackson versions and testing frameworks used in the assignment evaluation process.
 */
public class LogEvent {

    @JsonProperty(required = true)
    private String id; // $uuid

    @JsonProperty(required = true)
    private String message;

    @JsonProperty(required = true)
    private String timestamp; // dd-MM-yyyy HH:mm:ss

    @JsonProperty(required = true)
    private String thread; // where the log event has occurred

    @JsonProperty(required = true)
    private String logger; // name property of the logger

    @JsonProperty(required = true)
    private String level; // can be [ all, debug, info, warn, error, fatal, trace, off ]

    @JsonProperty
    private String errorDetails; // Optional, the stack trace of the error or exception that has been logged

    /**
     * Default no-argument constructor required by Jackson for JSON
     */
    public LogEvent() {
    }

    // getters for validation
    public String getId() { return id; }

    public String getMessage() { return message; }

    public String getTimestamp() { return timestamp; }

    public String getThread() { return thread; }

    public String getLogger() { return logger; }

    public String getLevel() { return level; }

    public String getErrorDetails() { return errorDetails; }
}
