package io.github.nagare.logging.server;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <a href="https://jsoup.org/cookbook/input/parse-document-from-string">...</a>
 */
public class TestStatsHTML {

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
        // test ContentType, Status
        TestHelper.populateDB(repo, 5);
        servlet.doGet(request, response);
        assertEquals("text/html", response.getContentType());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDoGet2() throws ServletException, IOException {
        // test html structure
        TestHelper.populateWithLogger(repo, 5, "test.Logger");
        servlet.doGet(request, response);
        String htmlContent = response.getContentAsString();
        assertTrue(htmlContent.startsWith("<!DOCTYPE html>"));
        // Parse a document from a String
        Document doc = Jsoup.parse(htmlContent);
        // check if it's valid DocumentType
        DocumentType type = doc.documentType();
        assertNotNull(type);
        assertEquals("html", type.name());
        // check table structure
        Element table = doc.selectFirst("table");
        assertNotNull(table);
        Elements rows = table.select("tr");
        assertEquals(6, rows.size());
        // Check header row
        Element header = rows.get(0);
        Elements headers = header.select("th");
        List<String> expectedLevels = StatsHelper.getLevels();
        assertEquals(expectedLevels.size() + 1, headers.size());
        assertEquals("logger", headers.get(0).text());
        for (int i = 0; i < expectedLevels.size(); i++) {
            assertEquals(expectedLevels.get(i), headers.get(i + 1).text());
        }
        // data rows
        for (int r = 1; r < rows.size(); r++) {
            Element row = rows.get(r);
            Elements cols = row.select("td");
            assertEquals(headers.size(), cols.size());
            assertTrue(cols.get(0).text().startsWith("test.Logger"));
            for (int c = 1; c < cols.size(); c++) {
                assertTrue(cols.get(c).text().matches("\\d+"));
                long count = Long.parseLong(cols.get(c).text());
                assertTrue(count >= 0);
            }
        }
    }

    @Test
    public void testDoGet3() throws ServletException, IOException {
        // test data correctness
        TestHelper.populateWithSameLogger(repo, 6, "test.Logger1"); // all 1
        TestHelper.populateWithSameLogger(repo, 5, "test.Logger1"); // +1 except fatal
        TestHelper.populateWithSameLogger(repo, 18, "test.Logger2"); // all 3
        TestHelper.populateWithSameLogger(repo, 2, "test.Logger2"); // +1 trace, debug
        servlet.doGet(request, response);
        String htmlContent = response.getContentAsString();
        Document doc = Jsoup.parse(htmlContent);
        Element table = doc.selectFirst("table");
        assertNotNull(table);
        Elements rows = table.select("tr");
        // skip header, it already verified
        Map<String, List<String>> loggerData = new HashMap<>();
        for (int r = 1; r < rows.size(); r++) {
            Element row = rows.get(r);
            Elements cols = row.select("td");
            String loggerName = cols.get(0).text();
            List<String> counts = new ArrayList<>();
            for (int c = 1; c < cols.size(); c++) {
                counts.add(cols.get(c).text());
            }
            loggerData.put(loggerName, counts);
        }
        // Verify Logger1 counts
        assertTrue(loggerData.containsKey("test.Logger1"));
        List<String> logger1Row = loggerData.get("test.Logger1");
        assertEquals("0", logger1Row.get(0)); // ALL
        assertEquals("2", logger1Row.get(1)); // TRACE
        assertEquals("2", logger1Row.get(2)); // DEBUG
        assertEquals("2", logger1Row.get(3)); // INFO
        assertEquals("2", logger1Row.get(4)); // WARN
        assertEquals("2", logger1Row.get(5)); // ERROR
        assertEquals("1", logger1Row.get(6)); // FATAL
        assertEquals("0", logger1Row.get(7)); // OFF
        // Verify Logger2 counts
        assertTrue(loggerData.containsKey("test.Logger2"));
        List<String> logger2Row = loggerData.get("test.Logger2");
        assertEquals("0", logger2Row.get(0)); // ALL
        assertEquals("4", logger2Row.get(1)); // TRACE
        assertEquals("4", logger2Row.get(2)); // DEBUG
        assertEquals("3", logger2Row.get(3)); // INFO
        assertEquals("3", logger2Row.get(4)); // WARN
        assertEquals("3", logger2Row.get(5)); // ERROR
        assertEquals("3", logger2Row.get(6)); // FATAL
        assertEquals("0", logger2Row.get(7)); // OFF
    }

    @Test
    public void testDoGet4() throws ServletException, IOException {
        // test empty database case
        servlet.doGet(request, response);
        String htmlContent = response.getContentAsString();
        Document doc = Jsoup.parse(htmlContent);
        Element table = doc.selectFirst("table");
        assertNotNull(table);
        Elements rows = table.select("tr");
        assertEquals(1, rows.size());
        // Verify header row still exists
        Elements headers = rows.get(0).select("th");
        assertEquals(9, headers.size());
    }

}
