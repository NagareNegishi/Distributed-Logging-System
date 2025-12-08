package io.github.nagare.logging.server;

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

    // this will be 'the key' for EntityManagerFactory object
    private static final String EMF_ATTRIBUTE = "EntityManagerFactory";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory("logDB");
            sce.getServletContext().setAttribute(EMF_ATTRIBUTE, emf);
        } catch (Exception e) {
            System.err.println("EntityManagerFactory initialization failed!");
            e.printStackTrace();
            throw  new RuntimeException("Cannot start application", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContextListener.super.contextDestroyed(sce);
    }
}
