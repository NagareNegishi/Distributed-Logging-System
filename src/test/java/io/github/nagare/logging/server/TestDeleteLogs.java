package io.github.nagare.logging.server;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


public class TestDeleteLogs {

    private LogsServlet servlet;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    public void setUp() {
        servlet = new LogsServlet();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        Persistency.DB.clear();
    }

    @Test
    public void testDoDelete1() throws ServletException, IOException {
        // Delete existing logs
        TestHelper.populateDB(20);
        servlet.doDelete(request, response);
        assertEquals(200, response.getStatus());
        assertTrue(Persistency.DB.isEmpty());
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
        assertTrue(Persistency.DB.isEmpty());
        servlet.doDelete(request, response);
        assertEquals(200, response.getStatus());
        assertTrue(Persistency.DB.isEmpty());
    }
}
