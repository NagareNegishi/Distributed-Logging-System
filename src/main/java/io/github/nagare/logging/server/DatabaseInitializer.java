package io.github.nagare.logging.server;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


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
     * Create EntityManagerFactory based on config.properties
     * IMPORTANT: If config.properties is missing, use H2 database by default and it will drop all data on shutdown
     * config.properties format:
     *  db.url=url_to_your_database
     *  db.user=yourusername
     *  db.password=yourpassword
     *  db.driver=your.jdbc.DriverClassName (optional)
     * @return EntityManagerFactory
     */
    private EntityManagerFactory createEMF() {
        try {
            InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties");
            if (input == null) { // No config = H2
                return Persistence.createEntityManagerFactory("logDB-dev");
            }

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
            return Persistence.createEntityManagerFactory("logDB-dev");
        }
    }

}
