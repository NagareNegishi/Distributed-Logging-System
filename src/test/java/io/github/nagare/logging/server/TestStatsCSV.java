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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


public class TestStatsCSV {

    private StatsCSVServlet servlet;
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
        servlet = new StatsCSVServlet();
        servlet.init(config);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        TestDatabaseSetup.clearDatabase(emf);
    }


    @Test
    public void testDoGet1() throws ServletException, IOException {
        // test ContentType, Status
        TestHelper.populateDB(repo, 5);
        servlet.doGet(request, response);
        assertEquals("text/csv", response.getContentType());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDoGet2() throws ServletException, IOException {
        // test csv structure
        TestHelper.populateWithLogger(repo, 5, "test.Logger");
        servlet.doGet(request, response);
        String csvContent = response.getContentAsString();
        // row size
        String[] lines = csvContent.split("\n");
        assertEquals(6, lines.length);
        // header
        String header = lines[0];
        String[] headers = header.split("\t");
        List<String> expectedLevels = StatsHelper.getLevels();
        assertEquals(expectedLevels.size() + 1, headers.length);
        assertEquals("logger", headers[0]);
        for (int i = 0; i < expectedLevels.size(); i++) {
            assertEquals(expectedLevels.get(i), headers[i + 1]);
        }
        // data rows
        for (int row = 1; row < lines.length; row++) {
            assertFalse(lines[row].isEmpty());
            String[] dataRow = lines[row].split("\t");
            assertEquals(headers.length, dataRow.length);
            assertTrue(dataRow[0].startsWith("test.Logger"));
            for (int col = 1; col < dataRow.length; col++) {
                assertTrue(dataRow[col].matches("\\d+"));
                long count = Long.parseLong(dataRow[col]);
                assertTrue(count >= 0);
            }
        }
    }

    @Test
    public void testDoGet3() throws ServletException, IOException {
        // test data correctness
        TestHelper.populateWithSameLogger(repo,6, "test.Logger1"); // all 1
        TestHelper.populateWithSameLogger(repo,5, "test.Logger1"); // +1 except fatal
        TestHelper.populateWithSameLogger(repo,18, "test.Logger2"); // all 3
        TestHelper.populateWithSameLogger(repo,2, "test.Logger2"); // +1 trace, debug
        servlet.doGet(request, response);
        String csvContent = response.getContentAsString();
        String[] lines = csvContent.split("\n");
        // skip header, it already verified
        Map<String, String[]> loggerData = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String[] row = lines[i].split("\t");
            loggerData.put(row[0], row);
        }
        // Verify Logger1 counts
        assertTrue(loggerData.containsKey("test.Logger1"));
        String[] logger1Row = loggerData.get("test.Logger1");
        assertEquals("0", logger1Row[1]); // ALL
        assertEquals("2", logger1Row[2]); // TRACE
        assertEquals("2", logger1Row[3]); // DEBUG
        assertEquals("2", logger1Row[4]); // INFO
        assertEquals("2", logger1Row[5]); // WARN
        assertEquals("2", logger1Row[6]); // ERROR
        assertEquals("1", logger1Row[7]); // FATAL
        assertEquals("0", logger1Row[8]); // OFF
        // Verify Logger2 counts
        assertTrue(loggerData.containsKey("test.Logger2"));
        String[] logger2Row = loggerData.get("test.Logger2");
        assertEquals("0", logger2Row[1]); // ALL
        assertEquals("4", logger2Row[2]); // TRACE
        assertEquals("4", logger2Row[3]); // DEBUG
        assertEquals("3", logger2Row[4]); // INFO
        assertEquals("3", logger2Row[5]); // WARN
        assertEquals("3", logger2Row[6]); // ERROR
        assertEquals("3", logger2Row[7]); // FATAL
        assertEquals("0", logger2Row[8]); // OFF
    }

    @Test
    public void testDoGet4() throws ServletException, IOException {
        // test empty database case
        servlet.doGet(request, response);
        String csvContent = response.getContentAsString();
        String[] lines = csvContent.split("\n");
        assertEquals(1, lines.length);
        String header = lines[0];
        String[] headers = header.split("\t");;
        assertEquals(9, headers.length);
    }
}
