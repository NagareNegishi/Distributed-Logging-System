package io.github.nagare.logging.server;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


/**
 * Utility class providing helper methods for testing log servlets.
 * Contains methods for JSON parsing, test data generation, and database population.
 */
public class TestHelper {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final List<String> LEVELS = Arrays.asList(
            "all", "trace", "debug", "info", "warn", "error", "fatal", "off"
    );
    private static final List<String> LOG_EVENT_LEVELS = List.of(
            "trace", "debug", "info", "warn", "error", "fatal"
    );
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");


    /**
     * Returns an unmodifiable list of all log levels including filter levels.
     * @return list of all log levels
     */
    public static List<String> getLevels() {
        return Collections.unmodifiableList(LEVELS);
    }


    /**
     * Parses a JSON string into a LogEvent object.
     * @param json the JSON string to parse
     * @return the parsed LogEvent
     * @throws IOException if JSON parsing fails
     */
    public static LogEvent createLogEvent(String json) throws IOException {
        return mapper.readValue(json, LogEvent.class);
    }


    /**
     * Parses a JSON array string into an array of LogEvent objects.
     * @param jsonArray the JSON array string to parse
     * @return array of parsed LogEvents
     * @throws IOException if JSON parsing fails
     */
    public static LogEvent[] createLogEventArray(String jsonArray) throws IOException {
        return mapper.readValue(jsonArray, LogEvent[].class);
    }


    /**
     * Creates a JSON string for a log event with specified parameters.
     * @param id the UUID for the log event
     * @param message the log message
     * @param level the log level
     * @param order minutes to subtract from current time for timestamp ordering
     * @return JSON string representation of the log event
     */
    public static String createLogJson(String id, String message, String level, int order) {
        String timestamp = LocalDateTime.now().minusMinutes(order).format(FORMATTER);
        return String.format("""
            {
                "id": "%s",
                "message": "%s",
                "timestamp": "%s",
                "thread": "main",
                "logger": "test.Logger",
                "level": "%s"
            }
            """, id, message, timestamp, level);
    }


    /**
     * Creates a JSON string for a log event with a custom logger name.
     * @param id the UUID for the log event
     * @param message the log message
     * @param logger the logger name
     * @param level the log level
     * @param order minutes to subtract from current time for timestamp ordering
     * @return JSON string representation of the log event
     */
    public static String createLogJsonWithLogger(String id, String message, String logger, String level, int order) {
        String timestamp = LocalDateTime.now().minusMinutes(order).format(FORMATTER);
        return String.format("""
            {
                "id": "%s",
                "message": "%s",
                "timestamp": "%s",
                "thread": "main",
                "logger": "%s",
                "level": "%s"
            }
            """, id, message, timestamp, logger, level);
    }


    /**
     * Populates the database with the specified number of test log events.
     * Each log has a unique ID and timestamp, with levels cycling through valid log event levels.
     * @param count number of log events to create
     * @throws IOException if JSON parsing fails
     */
    public static void populateDB(int count) throws IOException {
        for (int i = 0; i < count; i++) {
            String json = createLogJson(
                    generateId(), // Chance of duplication is astronomically small
                    "Test message " + i,
                    LOG_EVENT_LEVELS.get(i % LOG_EVENT_LEVELS.size()),
                    i
                    );
            LogEvent logEvent = createLogEvent(json);
            Persistency.DB.add(logEvent);
        }
    }


    /**
     * Populates the database with log events, each having a different logger name.
     * @param count number of log events to create
     * @param logger base logger name (will be appended with index)
     * @throws IOException if JSON parsing fails
     */
    public static void populateWithLogger(int count, String logger) throws IOException {
        for (int i = 0; i < count; i++) {
            String json = createLogJsonWithLogger(
                    generateId(), // Chance of duplication is astronomically small
                    "Test message " + i,
                    logger + i,
                    LOG_EVENT_LEVELS.get(i % LOG_EVENT_LEVELS.size()),
                    i
            );
            LogEvent logEvent = createLogEvent(json);
            Persistency.DB.add(logEvent);
        }
    }


    /**
     * Populates the database with log events all having the same logger name.
     * @param count number of log events to create
     * @param logger logger name to use for all log events
     * @throws IOException if JSON parsing fails
     */
    public static void populateWithSameLogger(int count, String logger) throws IOException {
        for (int i = 0; i < count; i++) {
            String json = createLogJsonWithLogger(
                    generateId(), // Chance of duplication is astronomically small
                    "Test message " + i,
                    logger,
                    LOG_EVENT_LEVELS.get(i % LOG_EVENT_LEVELS.size()),
                    i
            );
            LogEvent logEvent = createLogEvent(json);
            Persistency.DB.add(logEvent);
        }
    }


    /**
     * Generates a random UUID string.
     * @return a valid UUID string
     */
    public static String generateId() {
        return UUID.randomUUID().toString();
    }
}
