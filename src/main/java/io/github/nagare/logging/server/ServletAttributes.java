package io.github.nagare.logging.server;

/**
 * Constants for ServletContext attribute names.
 * Centralizes all attribute keys to prevent typos and duplication.
 */
public final class ServletAttributes {

    /**
     * Key for EntityManagerFactory stored in ServletContext
     */
    public static final String EMF_ATTRIBUTE = "EntityManagerFactory";

    /**
     * Private constructor prevents instantiation
     */
    private ServletAttributes() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}
