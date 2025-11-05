package io.github.nagare.logging.server;

import jakarta.servlet.ServletException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestStatsXLS {

    private StatsExcelServlet servlet;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    public void setUp() {
        servlet = new StatsExcelServlet();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        Persistency.DB.clear();
    }

    @Test
    public void testDoGet1() throws ServletException, IOException {
        // test ContentType, Status
        TestHelper.populateDB(5);
        servlet.doGet(request, response);
        assertEquals(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                response.getContentType()
        );
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDoGet2() throws ServletException, IOException {
        // test XLS structure
        TestHelper.populateWithLogger(5, "test.Logger");
        servlet.doGet(request, response);
        byte[] xlsContent = response.getContentAsByteArray(); // binary content
        // create XSSFWorkbook for parsing
        InputStream is = new ByteArrayInputStream(xlsContent);
        XSSFWorkbook workbook = new XSSFWorkbook(is);
        assertNotNull(workbook);
        XSSFSheet sheet = workbook.getSheet("stats");
        assertNotNull(sheet);
        assertEquals(6, sheet.getPhysicalNumberOfRows()); // row size
        // header
        XSSFRow header = sheet.getRow(0);
        List<String> expectedLevels = StatsHelper.getLevels();
        assertEquals(expectedLevels.size() + 1, header.getPhysicalNumberOfCells());
        assertEquals("logger", header.getCell(0).getStringCellValue());
        for (int i = 0; i < expectedLevels.size(); i++) {
            assertEquals(expectedLevels.get(i), header.getCell(i + 1).getStringCellValue());
        }
        // data rows
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // header
            assertEquals(header.getPhysicalNumberOfCells(), row.getPhysicalNumberOfCells());
            for (Cell cell : row) {
                assertNotNull(cell);
                if (cell.getColumnIndex() == 0) {
                    assertTrue(cell.getStringCellValue().startsWith("test.Logger"));
                } else {
                    double cellValue = cell.getNumericCellValue();
                    assertTrue(cellValue >= 0);
                    assertEquals(0.0, cellValue % 1); // no decimal part
                }
            }
        }
    }

    @Test
    public void testDoGet3() throws ServletException, IOException {
        // test data correctness
        TestHelper.populateWithSameLogger(6, "test.Logger1"); // all 1
        TestHelper.populateWithSameLogger(5, "test.Logger1"); // +1 except fatal
        TestHelper.populateWithSameLogger(18, "test.Logger2"); // all 3
        TestHelper.populateWithSameLogger(2, "test.Logger2"); // +1 trace, debug
        servlet.doGet(request, response);
        byte[] xlsContent = response.getContentAsByteArray(); // binary content
        InputStream is = new ByteArrayInputStream(xlsContent);
        XSSFWorkbook workbook = new XSSFWorkbook(is);
        XSSFSheet sheet = workbook.getSheet("stats");
        Map<String, Row> loggerRows = new HashMap<>();
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // skip header, it already verified
            loggerRows.put(row.getCell(0).getStringCellValue(), row);
        }
        // Verify Logger1 counts
        assertTrue(loggerRows.containsKey("test.Logger1"));
        Row logger1Row = loggerRows.get("test.Logger1");
        assertEquals(0, logger1Row.getCell(1).getNumericCellValue()); // ALL
        assertEquals(2, logger1Row.getCell(2).getNumericCellValue()); // TRACE
        assertEquals(2, logger1Row.getCell(3).getNumericCellValue()); // DEBUG
        assertEquals(2, logger1Row.getCell(4).getNumericCellValue()); // INFO
        assertEquals(2, logger1Row.getCell(5).getNumericCellValue()); // WARN
        assertEquals(2, logger1Row.getCell(6).getNumericCellValue()); // ERROR
        assertEquals(1, logger1Row.getCell(7).getNumericCellValue()); // FATAL
        assertEquals(0, logger1Row.getCell(8).getNumericCellValue()); // OFF
        // Verify Logger2 counts
        assertTrue(loggerRows.containsKey("test.Logger2"));
        Row logger2Row = loggerRows.get("test.Logger2");
        assertEquals(0, logger2Row.getCell(1).getNumericCellValue()); // ALL
        assertEquals(4, logger2Row.getCell(2).getNumericCellValue()); // TRACE
        assertEquals(4, logger2Row.getCell(3).getNumericCellValue()); // DEBUG
        assertEquals(3, logger2Row.getCell(4).getNumericCellValue()); // INFO
        assertEquals(3, logger2Row.getCell(5).getNumericCellValue()); // WARN
        assertEquals(3, logger2Row.getCell(6).getNumericCellValue()); // ERROR
        assertEquals(3, logger2Row.getCell(7).getNumericCellValue()); // FATAL
        assertEquals(0, logger2Row.getCell(8).getNumericCellValue()); // OFF
    }

    @Test
    public void testDoGet4() throws ServletException, IOException {
        // test empty database case
        servlet.doGet(request, response);
        byte[] xlsContent = response.getContentAsByteArray(); // binary content
        InputStream is = new ByteArrayInputStream(xlsContent);
        XSSFWorkbook workbook = new XSSFWorkbook(is);
        XSSFSheet sheet = workbook.getSheet("stats");
        assertEquals(1, sheet.getPhysicalNumberOfRows());
        XSSFRow header = sheet.getRow(0);
        assertEquals(9, header.getPhysicalNumberOfCells());
    }

}
