package io.github.nagare.logging.server;

import java.util.List;
import java.util.ArrayList;


/**
 * Simulates database persistence for log storage.
 * This class provides in-memory storage for log entries as required by the assignment.
 * No real database or file persistence is implemented, logs will be lost when the server stops.
 */
public class Persistency {
    public static List<LogEvent> DB = new ArrayList<>();
}
