package io.github.nagare.logging.server;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

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
     * Get logs filtered by level and limited by count, ordered by timestamp descending
     * @param limit maximum number of logs to return
     * @param level minimum log level threshold for filtering
     * @return List of LogEvent objects that match the criteria
     */
    public List<LogEvent> filterLogs(String limit, String level){
        try (EntityManager em = emf.createEntityManager()) {
            // Get ALL logs, ordered by timestamp
            List<LogEvent> allLogs = em.createQuery("FROM LogEvent ORDER BY timestamp DESC", LogEvent.class)
                    .getResultList();

            return allLogs.stream()
                    .filter(log -> filterLevel(log.getLevel(), level))
                    .limit(Integer.parseInt(limit))
                    .toList();
        }
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


    /**
     * Save log event to database
     * @param logEvent log event to be saved
     */
    public void save(LogEvent logEvent){
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(logEvent);
            tx.commit();
        }
        catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        }
        finally {
            em.close();
        }
    }


    /**
     * Check if log event with given ID exists
     * @param id log event ID
     * @return true if exists, false otherwise
     */
    public boolean is_exist(String id) {
        try (EntityManager em = emf.createEntityManager()) {
            // JPQL requires alias for COUNT
            Long count = em.createQuery("SELECT COUNT(L) FROM LogEvent L WHERE L.id = :id", Long.class)
                    .setParameter("id", id)
                    .getSingleResult();

            return count != null && count > 0;
        }
    }


    /**
     * Delete all log events from database
     */
    public void deleteAll() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("DELETE FROM LogEvent").executeUpdate();
            tx.commit();
        }
        catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        }
        finally {
            em.close();
        }
    }

    /**
     * Delete log event by ID
     * @param id log event ID
     */
    public void deleteById(String id) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("DELETE FROM LogEvent WHERE id = :id")
                    .setParameter("id", id)
                    .executeUpdate();
            tx.commit();
        }
        catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        }
        finally {
            em.close();
        }
    }


    /**
     * Get all logs from database
     * @return List of all LogEvent objects
     */
    public List<LogEvent> getAllLogs(){
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("FROM LogEvent", LogEvent.class).getResultList();
        }
    }

    
    /**
     * Get log event by ID
     * @param id log event ID
     * @return LogEvent object if found, null otherwise
     */
    public LogEvent getById(String id){
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(LogEvent.class, id);
        }
    }

}
