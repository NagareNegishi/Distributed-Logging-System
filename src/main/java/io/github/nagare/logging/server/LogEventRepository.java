package io.github.nagare.logging.server;

import jakarta.persistence.EntityManagerFactory;

/**
 * Class handle Basic CRUD operations for LogServlet
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

    // Get need filter database based on level and limit, limit is irrelevant on database side





    // Post need to save
    // Delete need to delete




}
