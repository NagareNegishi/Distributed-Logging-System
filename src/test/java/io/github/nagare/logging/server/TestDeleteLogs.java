package io.github.nagare.logging.server;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


public class TestDeleteLogs {

    private LogsServlet servlet;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private static EntityManagerFactory emf;
    private LogEventRepository repo;

    @BeforeAll
    public static void setUpClass() {
        // Create EMF once for all tests in this class
        emf = TestDatabaseSetup.createTestEMF();
    }

    @BeforeEach
    public void setUp() throws ServletException {
        repo = new LogEventRepository(emf);

        MockServletContext context = new MockServletContext();
        context.setAttribute(ServletAttributes.EMF_ATTRIBUTE, emf);
        MockServletConfig config = new MockServletConfig(context);
        servlet = new LogsServlet();
        servlet.init(config);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        TestDatabaseSetup.clearDatabase(emf);
    }


    @Test
    public void testDoDelete1() throws ServletException, IOException {
        // Delete existing logs
        TestHelper.populateDB(repo, 20);
        servlet.doDelete(request, response);
        assertEquals(200, response.getStatus());
        assertTrue(repo.getAllLogs().isEmpty());
        request.setParameter("limit", "5");
        request.setParameter("level", "all");
        servlet.doGet(request, response);
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("[]", response.getContentAsString());
    }

    @Test
    public void testDoDelete2() throws ServletException, IOException {
        // Delete already empty
        assertTrue(repo.getAllLogs().isEmpty());
        servlet.doDelete(request, response);
        assertEquals(200, response.getStatus());
        assertTrue(repo.getAllLogs().isEmpty());
    }
}
