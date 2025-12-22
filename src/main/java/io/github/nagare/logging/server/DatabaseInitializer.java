package io.github.nagare.logging.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;


/**
 * Official way of managing Lifecycle of EntityManagerFactory
 * ServletContext is like "Global HashMap"
 * One ServletContext per Web Application, it shared by ALL servlets in the application
 * <br>
 * with `@WebListener`:
 * Servlet container scans for '@WebListener' -> find this class
 * call 'contextInitialized()' during startup
 * call 'contextDestroyed()' during shutdown
 */
@WebListener
public class DatabaseInitializer implements ServletContextListener{

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            EntityManagerFactory emf = createEMF();
            sce.getServletContext().setAttribute(ServletAttributes.EMF_ATTRIBUTE, emf);
        } catch (Exception e) {
            System.err.println("EntityManagerFactory initialization failed!");
            e.printStackTrace();

            // For debugging root cause
            // Throwable cause = e;
            // while (cause.getCause() != null) {
            //     cause = cause.getCause();
            //     System.err.println("ROOT CAUSE: " + cause.getClass().getName() + ": " + cause.getMessage());
            // }
            
            throw  new RuntimeException("Cannot start application", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        EntityManagerFactory emf = (EntityManagerFactory) sce.getServletContext()
                .getAttribute(ServletAttributes.EMF_ATTRIBUTE);
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }


/**
 * Create EntityManagerFactory based on available configuration sources.
 * Configuration priority (checked in this order):
 * 1. Environment variables (for Docker deployment):
 *    - DB_URL=jdbc:postgresql://host:5432/database
 *    - DB_USER=username
 *    - DB_PASSWORD=password
 *    - DB_DRIVER=org.postgresql.Driver (optional)
 *
 * 2. config.properties (for local development):
 *    - db.url=jdbc:postgresql://host:5432/database
 *    - db.user=username
 *    - db.password=password
 *    - db.driver=org.postgresql.Driver (optional)
 *
 * 3. H2 in-memory database (fallback):
 *    - WARNING: All data will be lost when application stops
 *    - NOT recommended for production use
 * @return EntityManagerFactory configured with the first available source
 */
    private EntityManagerFactory createEMF() {
        // Check environment variables (for Docker)
        String dbUrl = System.getenv("DB_URL");
        if (dbUrl != null && !dbUrl.isEmpty()) {
            System.out.println("Using database from environment variables");
            Map<String, String> properties = new HashMap<>();
            properties.put("jakarta.persistence.jdbc.url", dbUrl);
            properties.put("jakarta.persistence.jdbc.user", System.getenv("DB_USER"));
            properties.put("jakarta.persistence.jdbc.password", System.getenv("DB_PASSWORD"));
            String driver = System.getenv("DB_DRIVER");
            if (driver != null && !driver.isEmpty()) {
                properties.put("jakarta.persistence.jdbc.driver", driver);
            }
            return Persistence.createEntityManagerFactory("logDB", properties);
        }

        // Check config.properties (for local development)
        try {
            InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties");
            if (input == null) { // No config = H2
                System.out.println("config.properties not found, falling back to H2 database");
                return Persistence.createEntityManagerFactory("logDB_dev");
            }

            System.out.println("Using database from config.properties");
            Properties props = new Properties();
            props.load(input);
            Map<String, String> properties = new HashMap<>();
            properties.put("jakarta.persistence.jdbc.url", props.getProperty("db.url"));
            properties.put("jakarta.persistence.jdbc.user", props.getProperty("db.user"));
            properties.put("jakarta.persistence.jdbc.password", props.getProperty("db.password"));
            // Let user override driver if they want
            String driver = props.getProperty("db.driver");
            if (driver != null) { properties.put("jakarta.persistence.jdbc.driver", driver); }

            return Persistence.createEntityManagerFactory("logDB", properties);
        } catch (IOException e) {
            System.err.println("Failed to load config.properties, falling back to H2: " + e.getMessage());
            return Persistence.createEntityManagerFactory("logDB_dev");
        }
    }

}
