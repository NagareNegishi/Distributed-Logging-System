package io.github.nagare.logging.server;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestPostLogs {

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
    public void testDoPost1() throws ServletException, IOException {
        // test missing contentType
        servlet.doPost(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Content-Type must be application/json"));
    }

    @Test
    public void testDoPost2() throws ServletException, IOException {
        // test wrong contentType
        request.setContentType("text/plain");
        servlet.doPost(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Content-Type must be application/json"));
    }

    @Test
    public void testDoPost3() throws ServletException, IOException {
        // test valid input
        String jsonLog = """
                {
                  "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                  "message": "application started",
                  "timestamp": "04-05-2021 13:30:45",
                  "thread": "main",
                  "logger": "com.example.Foo",
                  "level": "debug",
                  "errorDetails": "string"
                }
                """;
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(201, response.getStatus());
        assertEquals(1, Persistency.DB.size());
        LogEvent logEvent = TestHelper.createLogEvent(jsonLog);
        LogEvent resultEvent = Persistency.DB.get(0);
        assertEquals(logEvent.getId(), resultEvent.getId());
        assertEquals(logEvent.getMessage(), resultEvent.getMessage());
        assertEquals(logEvent.getTimestamp(), resultEvent.getTimestamp());
        assertEquals(logEvent.getThread(), resultEvent.getThread());
        assertEquals(logEvent.getLogger(), resultEvent.getLogger());
        assertEquals(logEvent.getLevel(), resultEvent.getLevel());
        assertEquals(logEvent.getErrorDetails(), resultEvent.getErrorDetails());
    }

    @Test
    public void testDoPost4() throws ServletException, IOException {
        // test Missing id
        String jsonLog = """
                {
                  "message": "application started",
                  "timestamp": "04-05-2021 13:30:45",
                  "thread": "main",
                  "logger": "com.example.Foo",
                  "level": "debug",
                  "errorDetails": "string"
                }
                """;
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Missing required field: id"));
    }

    @Test
    public void testDoPost5() throws ServletException, IOException {
        // test Missing message
        String jsonLog = """
                {
                  "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                  "timestamp": "04-05-2021 13:30:45",
                  "thread": "main",
                  "logger": "com.example.Foo",
                  "level": "debug",
                  "errorDetails": "string"
                }
                """;
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Missing required field: message"));
    }

    @Test
    public void testDoPost6() throws ServletException, IOException {
        // test Missing timestamp
        String jsonLog = """
                {
                  "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                  "message": "application started",
                  "thread": "main",
                  "logger": "com.example.Foo",
                  "level": "debug",
                  "errorDetails": "string"
                }
                """;
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Missing required field: timestamp"));
    }

    @Test
    public void testDoPost7() throws ServletException, IOException {
        // test Missing thread
        String jsonLog = """
                {
                  "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                  "message": "application started",
                  "timestamp": "04-05-2021 13:30:45",
                  "logger": "com.example.Foo",
                  "level": "debug",
                  "errorDetails": "string"
                }
                """;
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Missing required field: thread"));
    }

    @Test
    public void testDoPost8() throws ServletException, IOException {
        // test Missing logger
        String jsonLog = """
                {
                  "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                  "message": "application started",
                  "timestamp": "04-05-2021 13:30:45",
                  "thread": "main",
                  "level": "debug",
                  "errorDetails": "string"
                }
                """;
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Missing required field: logger"));
    }

    @Test
    public void testDoPost9() throws ServletException, IOException {
        // test Missing level
        String jsonLog = """
                {
                  "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                  "message": "application started",
                  "timestamp": "04-05-2021 13:30:45",
                  "thread": "main",
                  "logger": "com.example.Foo",
                  "errorDetails": "string"
                }
                """;
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Missing required field: level"));
    }

    @Test
    public void testDoPost10() throws ServletException, IOException {
        // test Missing errorDetails
        String jsonLog = """
                {
                  "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                  "message": "application started",
                  "timestamp": "04-05-2021 13:30:45",
                  "thread": "main",
                  "logger": "com.example.Foo",
                  "level": "debug"
                }
                """;
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(201, response.getStatus());
        assertEquals(1, Persistency.DB.size());
    }

    @Test
    public void testDoPost11() throws ServletException, IOException {
        // test Invalid UUID
        String jsonLog = """
                {
                  "id": "Invalid-UUID-4b01-90e6-d701748f0851",
                  "message": "application started",
                  "timestamp": "04-05-2021 13:30:45",
                  "thread": "main",
                  "logger": "com.example.Foo",
                  "level": "debug",
                  "errorDetails": "string"
                }
                """;
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Invalid UUID format for id"));
    }

    @Test
    public void testDoPost12() throws ServletException, IOException {
        // test Invalid timestamp
        String jsonLog = """
                {
                  "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                  "message": "application started",
                  "timestamp": "04-05-2021 13:30:4500",
                  "thread": "main",
                  "logger": "com.example.Foo",
                  "level": "debug",
                  "errorDetails": "string"
                }
                """;
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Invalid timestamp format. Expected: dd-MM-yyyy HH:mm:ss"));
    }

    @Test
    public void testDoPost13() throws ServletException, IOException {
        // test Invalid log level
        String jsonLog = """
                {
                  "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                  "message": "application started",
                  "timestamp": "04-05-2021 13:30:45",
                  "thread": "main",
                  "logger": "com.example.Foo",
                  "level": "Invalid",
                  "errorDetails": "string"
                }
                """;
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains(
                "Invalid log level. Must be one of: trace, debug, info, warn, error, fatal"));
    }

    @Test
    public void testDoPost14() throws ServletException, IOException {
        // test id already exists
        TestHelper.populateDB(5);
        String existId = Persistency.DB.get(1).getId();
        String jsonLog = TestHelper.createLogJson(existId, "message", "debug", 0);
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(409, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("A log event with this id already exists"));
    }

    @Test
    public void testDoPost15() throws ServletException, IOException {
        // test Invalid log level off
        String jsonLog = """
                {
                  "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                  "message": "application started",
                  "timestamp": "04-05-2021 13:30:45",
                  "thread": "main",
                  "logger": "com.example.Foo",
                  "level": "off",
                  "errorDetails": "string"
                }
                """;
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains(
                "Invalid log level. all and off are filter settings, not valid log levels"));
    }

    @Test
    public void testDoPost16() throws ServletException, IOException {
        // test Invalid log level all
        String jsonLog = """
                {
                  "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                  "message": "application started",
                  "timestamp": "04-05-2021 13:30:45",
                  "thread": "main",
                  "logger": "com.example.Foo",
                  "level": "all",
                  "errorDetails": "string"
                }
                """;
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains(
                "Invalid log level. all and off are filter settings, not valid log levels"));
    }

    @Test
    public void testDoPost17() throws ServletException, IOException {
        // test malformed JSON missing "
        String jsonLog = """
                {
                  "id": d290f1ee-6c54-4b01-90e6-d701748f0851",
                  "message": "application started",
                  "timestamp": "04-05-2021 13:30:45",
                  "thread": "main",
                  "logger": "com.example.Foo",
                  "level": "debug",
                  "errorDetails": "string"
                }
                """;
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Invalid JSON format"));
    }

    @Test
    public void testDoPost18() throws ServletException, IOException {
        // test malformed JSON missing bracket
        String jsonLog = """
                {
                  "id": d290f1ee-6c54-4b01-90e6-d701748f0851",
                  "message": "application started",
                  "timestamp": "04-05-2021 13:30:45",
                  "thread": "main",
                  "logger": "com.example.Foo",
                  "level": "debug",
                  "errorDetails": "string"
                """;
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains("Invalid JSON format"));
    }

    @Test
    public void testDoPost19() throws ServletException, IOException {
        // test level is case-sensitive
        String jsonLog = """
                {
                  "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                  "message": "application started",
                  "timestamp": "04-05-2021 13:30:45",
                  "thread": "main",
                  "logger": "com.example.Foo",
                  "level": "DEBUG",
                  "errorDetails": "string"
                }
                """;
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains(
                "Invalid log level. Must be one of: trace, debug, info, warn, error, fatal"));
    }

    @Test
    public void testDoPost20() throws ServletException, IOException {
        // test empty JSON object
        String jsonLog = "{}";
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(400, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains(
                "Missing required field: id"));
    }

    @Test
    public void testDoPost21() throws ServletException, IOException {
        // test Content-Type with charset parameter
        String jsonLog = """
            {
              "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
              "message": "application started",
              "timestamp": "04-05-2021 13:30:45",
              "thread": "main",
              "logger": "com.example.Foo",
              "level": "debug"
            }
            """;
        request.setContentType("application/json; charset=UTF-8");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(201, response.getStatus());
        assertEquals(1, Persistency.DB.size());
    }

    @Test
    public void testDoPost22() throws ServletException, IOException {
        // test that DELETE allows previously duplicate ID to be posted again
        String jsonLog = """
            {
              "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
              "message": "application started",
              "timestamp": "04-05-2021 13:30:45",
              "thread": "main",
              "logger": "com.example.Foo",
              "level": "debug",
              "errorDetails": "string"
            }
            """;

        // First POST - should succeed
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(201, response.getStatus());
        assertEquals(1, Persistency.DB.size());

        // Second POST with same ID - should fail with 409
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(409, response.getStatus());

        // DELETE all logs
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        servlet.doDelete(request, response);
        assertEquals(200, response.getStatus());

        // POST with same ID again - should succeed
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent(jsonLog.getBytes());
        servlet.doPost(request, response);
        assertEquals(201, response.getStatus());
        assertEquals(1, Persistency.DB.size());
    }

}
