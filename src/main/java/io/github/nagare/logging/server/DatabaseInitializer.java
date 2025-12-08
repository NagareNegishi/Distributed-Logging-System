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

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContextListener.super.contextInitialized(sce);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContextListener.super.contextDestroyed(sce);
    }
}
