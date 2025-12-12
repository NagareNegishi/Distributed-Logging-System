package io.github.nagare.logging.server;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Util class for easy access to EntityManagerFactory -> Entity
 * This is common pattern for project with JPA, however it will not manage Lifecycle of EntityManagerFactory
 * While I implement it for test and future use, for this project I will use official way "ServletContextListener"
 */
public class JPAUtil {
    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("logDB_dev");

    public static EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }

    public static void shutdown() {
        emf.close();
    }
}
