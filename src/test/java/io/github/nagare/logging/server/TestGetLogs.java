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
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test for the following:
// possible status codes, content types and data returned (by GET requests).
public class TestGetLogs {

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

        //Persistency.DB.clear();

        TestDatabaseSetup.clearDatabase(emf);
    }


    @Test
    public void testDoGet1() throws ServletException, IOException {
        // test valid case
        String jsonLog = """
                {
                  "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                  "message": "application started",
                  "timestamp": "2024-12-05T14:30:45.000Z",
                  "thread": "main",
                  "logger": "com.example.Foo",
                  "level": "debug",
                  "errorDetails": "string"
                }
                """;
        LogEvent logEvent = TestHelper.createLogEvent(jsonLog);
        //Persistency.DB.add(logEvent);
        repo.save(logEvent);

        request.setParameter("limit", "10");
        request.setParameter("level", "debug");
        servlet.doGet(request, response);

        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        String result = response.getContentAsString();
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("d290f1ee-6c54-4b01-90e6-d701748f0851"));
        assertTrue(result.contains("2024-12-05T14:30:45.000Z"));
        LogEvent[] resultEvents = TestHelper.createLogEventArray(result);
        assertEquals(1, resultEvents.length);
        LogEvent resultEvent = resultEvents[0];
        assertEquals(logEvent.getId(), resultEvent.getId());
        assertEquals(logEvent.getMessage(), resultEvent.getMessage());
        assertEquals(logEvent.getTimestamp(), resultEvent.getTimestamp());
        assertEquals(logEvent.getThread(), resultEvent.getThread());
        assertEquals(logEvent.getLogger(), resultEvent.getLogger());
        assertEquals(logEvent.getLevel(), resultEvent.getLevel());
        assertEquals(logEvent.getErrorDetails(), resultEvent.getErrorDetails());
    }

    @Test
    public void testDoGet2() throws ServletException, IOException {
        // test missing both input parameters
        servlet.doGet(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Missing required parameters: limit"));
    }

    @Test
    public void testDoGet3() throws ServletException, IOException {
        // test missing limit
        request.setParameter("level", "debug");
        servlet.doGet(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Missing required parameters: limit"));
    }

    @Test
    public void testDoGet4() throws ServletException, IOException {
        // test missing level
        request.setParameter("limit", "1");
        servlet.doGet(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Missing required parameters: level"));
    }

    @Test
    public void testDoGet5() throws ServletException, IOException {
        // test invalid limit too small
        request.setParameter("limit", "0");
        request.setParameter("level", "debug");
        servlet.doGet(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Limit must be a positive integer"));
    }

    @Test
    public void testDoGet6() throws ServletException, IOException {
        // test invalid limit too large
        request.setParameter("limit", "2147483648");
        request.setParameter("level", "debug");
        servlet.doGet(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Invalid limit format"));
    }

    @Test
    public void testDoGet7() throws ServletException, IOException {
        // test invalid limit not int
        request.setParameter("limit", "limit");
        request.setParameter("level", "debug");
        servlet.doGet(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Invalid limit format"));
    }

    @Test
    public void testDoGet8() throws ServletException, IOException {
        // test max limit with no logs
        request.setParameter("limit", "2147483647");
        request.setParameter("level", "debug");
        servlet.doGet(request, response);
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("[]", response.getContentAsString());
    }

    @Test
    public void testDoGet9() throws ServletException, IOException {
        // test invalid level
        request.setParameter("limit", "1");
        request.setParameter("level", "invalid");
        servlet.doGet(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains(
                "Invalid log level. Must be one of: ALL, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF"));
    }

    @Test
    public void testDoGet10() throws ServletException, IOException {
        // test invalid limit negative
        request.setParameter("limit", "-10");
        request.setParameter("level", "debug");
        servlet.doGet(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Limit must be a positive integer"));
    }

    @Test
    public void testDoGet11() throws ServletException, IOException {
        // test multiple logs and order
        TestHelper.populateDB(repo, 10);
        request.setParameter("limit", "5");
        request.setParameter("level", "all");
        servlet.doGet(request, response);
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        String result = response.getContentAsString();
        LogEvent[] resultEvents = TestHelper.createLogEventArray(result);
        assertEquals(5, resultEvents.length);
        assertTrue(resultEvents[0].getMessage().contains("Test message 0"));

        // keep order but extract timestamp
        List<Instant> timestamps = Arrays.stream(resultEvents)
                .map(LogEvent::getTimestamp)
                .map(Instant::parse)
                .toList();
        boolean isDescending = IntStream.range(0, timestamps.size() - 1)
                .allMatch(i -> timestamps.get(i).isAfter(timestamps.get(i + 1)));
        assertTrue(isDescending);
    }

    @Test
    public void testDoGet12() throws ServletException, IOException {
        // test multiple logs and level filter
        TestHelper.populateDB(repo, 20);
        request.setParameter("limit", "20");
        request.setParameter("level", "warn");
        servlet.doGet(request, response);
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        String result = response.getContentAsString();
        LogEvent[] resultEvents = TestHelper.createLogEventArray(result);

        List<String> expectedLevels = Arrays.asList("WARN", "ERROR", "FATAL"); // "off" shouldn't exist
        List<LogEvent> filteredDB = Persistency.DB.stream()
                .filter(log -> expectedLevels.contains(log.getLevel()))
                .limit(20)
                .toList();
        assertEquals(filteredDB.size(), resultEvents.length);
        boolean match = IntStream.range(0, resultEvents.length)
                .allMatch(i -> resultEvents[i].getId().equals(filteredDB.get(i).getId()));
        assertTrue(match);
    }

    @Test
    public void testDoGet13() throws ServletException, IOException {
        // test multiple logs and limit filter
        TestHelper.populateDB(repo, 20);
        request.setParameter("limit", "5");
        request.setParameter("level", "all");
        servlet.doGet(request, response);
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        String result = response.getContentAsString();
        LogEvent[] resultEvents = TestHelper.createLogEventArray(result);
        assertEquals(5, resultEvents.length);
    }

    @Test
    public void testDoGet14() throws ServletException, IOException {
        // test level=off
        TestHelper.populateDB(repo, 10);
        request.setParameter("limit", "10");
        request.setParameter("level", "off");
        servlet.doGet(request, response);
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("[]", response.getContentAsString()); // should be no log events with this level
    }

    @Test
    public void testDoGet15() throws ServletException, IOException {
        // test level=all
        TestHelper.populateDB(repo, 20);
        request.setParameter("limit", "20");
        request.setParameter("level", "all");
        servlet.doGet(request, response);
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        LogEvent[] resultEvents = TestHelper.createLogEventArray(response.getContentAsString());
        assertEquals(20, resultEvents.length); // return all logs
    }

    @Test
    public void testDoGet16() throws ServletException, IOException {
        // test level is not case-sensitive (auto convert to Upper case)
        TestHelper.populateDB(repo, 3);
        request.setParameter("limit", "20");
        request.setParameter("level", "Debug");
        servlet.doGet(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDoGet17() throws ServletException, IOException {
        // test invalid limit empty
        request.setParameter("limit", "");
        request.setParameter("level", "debug");
        servlet.doGet(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Invalid limit format"));
    }

    @Test
    public void testDoGet18() throws ServletException, IOException {
        // test invalid level empty
        request.setParameter("limit", "10");
        request.setParameter("level", "");
        servlet.doGet(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains(
                "Invalid log level. Must be one of: ALL, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF"));
    }

}
