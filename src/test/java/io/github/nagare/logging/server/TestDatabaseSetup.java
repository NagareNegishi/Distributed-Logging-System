package io.github.nagare.logging.server;

import jakarta.persistence.Persistence;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

/**
 * Utility class providing Database Setup for unit test.
 */
public class TestDatabaseSetup {

    private static EntityManagerFactory emf;

    public static EntityManagerFactory createTestEMF() {
        if (emf == null) {
            emf = Persistence.createEntityManagerFactory("logDB_dev");
        }
        return emf;
    }

    public static void clearDatabase(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("DELETE FROM LogEvent").executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
