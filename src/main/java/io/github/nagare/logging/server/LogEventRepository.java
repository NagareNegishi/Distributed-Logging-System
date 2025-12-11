package io.github.nagare.logging.server;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Class handle Basic CRUD operations for LogServlet
 * Basic JPQL structure:
 * em.createQuery("SELECT/FROM entity WHERE conditions ORDER BY field", Class)
 * <a href="https://docs.oracle.com/javaee/7/api/javax/persistence/EntityManager.html">...</a>
 */
public class LogEventRepository {

    private final EntityManagerFactory emf;

    /**
     * Public constructor, servlet need to pass EntityManagerFactory
     * @param emf EntityManagerFactory from Servlet or class use it
     */
    public LogEventRepository(EntityManagerFactory emf){
        this.emf = emf;
    }

    /**
     * LogServlet has Get, Post, Delete so at least need those 3 method
     */

    /**
     * Get logs filtered by level and limited by count, ordered by timestamp descending
     */
    public List<LogEvent> filterLogs(String limit, String level){
        EntityManager em = emf.createEntityManager();

        // Get ALL logs, ordered by timestamp
        List<LogEvent> allLogs = em.createQuery("FROM LogEvent ORDER BY timestamp DESC", LogEvent.class)
                .getResultList();
        em.close();

        return allLogs.stream()
            .filter(log -> filterLevel(log.getLevel(), level))
            .limit(Integer.parseInt(limit))
            .toList();
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
        return LogsServlet.LEVELS.indexOf(target) >= LogsServlet.LEVELS.indexOf(levelParam);
    }




    // Post need to save
    // Delete need to delete




}
