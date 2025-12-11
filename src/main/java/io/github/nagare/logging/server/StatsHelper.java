package io.github.nagare.logging.server;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Utility class for computing log statistics.
 * Provides access to the fixed set of log levels and aggregates log events from Persistency
 * into the structure required by the assignment table spec.
 */
public class StatsHelper {

    private final LogEventRepository repository;
    private static final List<String> LEVELS = List.of("ALL", "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "OFF");

    /**
     *
     * @param repo
     */
    public StatsHelper(LogEventRepository repo){
        this.repository = repo;
    }


    /**
     * Return the list of supported log levels, in fixed order.
     * @return list of log level names
     */
    public static List<String> getLevels() {
        return LEVELS;
    }


    /**
     * Compute aggregated statistics of log events.
     * Groups events by logger name and log level,
     * counts the number of events per level,
     * and fills in missing levels with zero counts.
     * @return nested map: logger → (level → count)
     */
    public Map<String, Map<String, Long>> getLogStatistics() {
        // logger, <level,count>
        Map<String, Map<String, Long>> stats =  repository.getAllLogs()
                .stream()
                .collect(Collectors.groupingBy(
                        LogEvent::getLogger,
                        Collectors.groupingBy(
                                log -> log.getLevel().toUpperCase(),
                                Collectors.counting()
                        )
                ));
        // fill missing levels
        List<String> allLevels = getLevels();
        stats.forEach((logger, levelMap) -> {
            allLevels.forEach(level ->
                    levelMap.putIfAbsent(level, 0L)
            );
        });
        return stats;
    }
}
