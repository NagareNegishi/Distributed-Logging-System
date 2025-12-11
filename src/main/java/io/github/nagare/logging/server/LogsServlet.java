package io.github.nagare.logging.server;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;
import java.util.UUID;
import java.util.Comparator;
import java.time.Instant;

import java.time.format.DateTimeParseException;
import java.io.IOException;


/**
 * Servlet for managing log events via HTTP requests.
 * Supports GET (retrieve logs), POST (create log), and DELETE (clear all logs) operations.
 * Logs are stored in memory and returned as JSON.
 */
public class LogsServlet extends HttpServlet{

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final List<String> LEVELS = List.of("ALL", "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "OFF");
    private EntityManagerFactory emf;
    private LogEventRepository repository;


    // Explicitly defined default constructor
    public LogsServlet() {
    }

    /**
     * Initialize servlet - get EntityManagerFactory from ServletContext
     */
    @Override
    public void init() throws ServletException {
        this.emf = (EntityManagerFactory) getServletContext().getAttribute(ServletAttributes.EMF_ATTRIBUTE);
        if (emf == null) {
            throw new ServletException("EntityManagerFactory not found");
        }
        this.repository = new LogEventRepository(emf);
    }


    /**
     * By passing in the appropriate options, you can search for available logs in the system.
     * Logs are returned ordered by timestamp, the latest logs first.
     * Logs are JSON encoded, the returned value is a JSON array.
     * If there are no logs found, then an empty array is returned.
     * @param req the HttpServletRequest
     * @param resp the HttpServletResponse
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Parse and verify limit/level parameters
        String limitParam = req.getParameter("limit");
        String tempLevel = req.getParameter("level");
        String levelParam = (tempLevel == null) ? null : tempLevel.toUpperCase();
        String validationError = validateGetParameters(limitParam, levelParam); // Validate required fields
        if (validationError != null) {
            sendError(resp, 400, validationError);
            return;
        }
        // Filter and sort Persistency.DB
        List<LogEvent> logList = Persistency.DB.stream()
                .filter(log -> filterLevel(log.getLevel(), levelParam))
                // the latest logs first
                .sorted(Comparator.comparing(this::parseTimestamp).reversed())
                .limit(Integer.parseInt(limitParam))
                .toList();
        // Convert to JSON array
        String jsonArray = mapper.writeValueAsString(logList);
        resp.setContentType("application/json");
        resp.setStatus(200);
        resp.getWriter().write(jsonArray);
    }


    /**
     * Parses the timestamp string from a log event into a Instant object.
     * @param log the LogEvent containing the timestamp to parse
     * @return the parsed Instant
     */
    private Instant parseTimestamp(LogEvent log) {
        return Instant.parse(log.getTimestamp());
    }


    /**
     * Filters log events based on the minimum log level threshold.
     * @param target the log level of the event to be filtered
     * @param levelParam the minimum log level threshold for filtering
     * @return true if the log should be included, false otherwise
     */
    private boolean filterLevel(String target, String levelParam){
        if (levelParam.equals("ALL")) return true;
        if (levelParam.equals("OFF")) return false;
        return LEVELS.indexOf(target) >= LEVELS.indexOf(levelParam);
    }


    /**
     * Validate parameter for GET
     * @param limitParam the limit query parameter (maximum number of records to return)
     * @param levelParam the level query parameter (minimum log level filter)
     * @return null if parameters are valid, corresponding error message otherwise
     */
    private String validateGetParameters(String limitParam, String levelParam) {
        if (limitParam == null) return "Missing required parameters: limit";
        if (levelParam == null) return "Missing required parameters: level";
        String error = isValidLimit(limitParam);
        if (error != null) return error;
        if (!isValidLevel(levelParam)) {
            return "Invalid log level. Must be one of: ALL, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF";
        }
        return null;
    }


    /**
     * Validates that the limit parameter is a positive integer.
     * @param limitParam must be between 1 and 2147483647
     * @return null if parameters are valid, corresponding error message otherwise
     */
    private String isValidLimit(String limitParam) {
        int limit;
        try {
            limit = Integer.parseInt(limitParam);
            if (limit < 1) return "Limit must be a positive integer";
        } catch (NumberFormatException e) {
            return "Invalid limit format";
        }
        return null;
    }


    /**
     * Handles POST requests to create a new log event.
     * Validates content type, parses JSON body, checks for duplicates, and stores the log.
     * @param req the HttpServletRequest
     * @param resp the HttpServletResponse
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Validate content of requestBody
        String contentType = req.getContentType();
        if (contentType == null || !contentType.contains("application/json")) {
            sendError(resp, 400, "Content-Type must be application/json");
            return;
        }
        // Parse request body to LogEvent
        LogEvent logEvent;
        try {
            logEvent = mapper.readValue(req.getInputStream(), LogEvent.class);
        } catch (JsonProcessingException e) { // Covers both StreamReadException and DatabindException
            sendError(resp, 400, "Invalid JSON format");
            return;
        }
        String validationError = validateLogEvent(logEvent); // Validate parameters
        if (validationError != null) {
            sendError(resp, 400, validationError);
            return;
        }
        // Prevent duplication
        boolean duplicate = Persistency.DB.stream()
                .anyMatch(existing -> logEvent.getId().equals(existing.getId()));
        if (duplicate) {
            sendError(resp, 409, "A log event with this id already exists");
            return;
        }
        Persistency.DB.add(logEvent);
        resp.setStatus(201);
    }


    /**
     * Validates all required fields of a LogEvent object.
     * @param logEvent the LogEvent to validate
     * @return null if parameters are valid, corresponding error message otherwise
     */
    private String validateLogEvent(LogEvent logEvent) {
        if (logEvent.getMessage() == null) return "Missing required field: message";
        if (logEvent.getTimestamp() == null) return "Missing required field: timestamp";
        if (logEvent.getThread() == null) return "Missing required field: thread";
        if (logEvent.getLogger() == null) return "Missing required field: logger";
        if (logEvent.getLevel() == null) return "Missing required field: level";
        if (!isValidId(logEvent)) return "Invalid UUID format for id";
        if (!isValidTimestamp(logEvent.getTimestamp())) {
            return "Invalid timestamp format. Expected: ISO-8601 format";
        }

        // Normalize level to uppercase
        logEvent.setLevel(logEvent.getLevel().toUpperCase());
        String level = logEvent.getLevel();
        if (!isValidLevel(level)) {
            return "Invalid log level. Must be one of: TRACE, DEBUG, INFO, WARN, ERROR, FATAL";
        }
        if (level.equals("ALL") || level.equals("OFF")) {
            return "Invalid log level. ALL and OFF are filter settings, not valid log levels";
        }
        return null;
    }


    /**
     * Validates that the given string is a valid UUID format.
     * If ID is not provided, generate one for LogEvent.
     * <a href="https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html">...</a>
     * @param logEvent the LogEvent to validate
     * @return true if valid UUID format, false otherwise
     */
    private boolean isValidId(LogEvent logEvent) {
        String id = logEvent.getId();
        if (id == null) { // generate UUID
            logEvent.setId(UUID.randomUUID().toString());
            return true;
        }
        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    /**
     * Validates that the timestamp matches ISO-8601 format.
     * @param timestamp the timestamp string to validate
     * @return true if valid format, false otherwise
     */
    private boolean isValidTimestamp(String timestamp) {
        try {
            Instant.parse(timestamp);
            return true;
        } catch (DateTimeParseException e){
            return false;
        }
    }


    /**
     * Validates that the level is one of the accepted log levels.
     * @param level the log level to validate
     * @return true if valid level, false otherwise
     */
    private boolean isValidLevel(String level) {
        return LEVELS.contains(level);
    }


    /**
     * Sends an HTTP error response with the specified status code and message.
     * @param resp the HttpServletResponse to write the error to
     * @param statusCode the HTTP status code
     * @param message the error message to include in the response body
     * @throws IOException if an I/O error occurs while writing the response
     */
    private void sendError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("text/plain");
        resp.getWriter().write(message);
    }


    /**
     * Handles DELETE requests to clear all stored log events.
     * @param req the HttpServletRequest
     * @param resp the HttpServletResponse
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Clear Persistency.DB
        Persistency.DB.clear();
        resp.setStatus(200);
    }
}
