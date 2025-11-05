package io.github.nagare.logging.server;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;

import org.apache.poi.xssf.usermodel.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * Servlet for returning excel-encoded log statistics.
 * Accessible at GET /logstore/stats/excel.
 * Response is an Excel 2007 OOXML workbook (.xlsx) with a single sheet named "stats".
 * Column structure follows the assignment specification:
 * <a href="https://docs.google.com/drawings/d/1v_dpZ0XKiqTaygmOaTThWgR9swRDatY6sxYmGEn9GVM/edit?usp=sharing">...</a>
 * References:
 * <a href="https://poi.apache.org/components/spreadsheet/quick-guide.html">...</a>
 * <a href="https://poi.apache.org/components/spreadsheet/examples.html">...</a>
 * <a href="https://www.baeldung.com/java-microsoft-excel">...</a>
 * <a href="https://jakarta.ee/specifications/servlet/6.1/apidocs/jakarta.servlet/jakarta/servlet/servletresponse#getOutputStream()">...</a>
 * <a href="https://poi.apache.org/apidocs/5.0/org/apache/poi/ooxml/POIXMLDocument.html#write-java.io.OutputStream-">...</a>
 * Content type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
 * Status: 200 on success
 */
public class StatsExcelServlet extends HttpServlet {

    // Explicitly defined default constructor
    public StatsExcelServlet() {
    }


    /**
     * Handles GET requests and writes Excel statistics to the response.
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        try (XSSFWorkbook workbook = generateExcel(StatsHelper.getLogStatistics())) {
            resp.setStatus(200);
            workbook.write(resp.getOutputStream());
        }
    }


    /**
     * Generate an Excel workbook containing a statistics sheet.
     * XSSF Workbook → ZIP compression → Binary bytes → OutputStream → HTTP Response
     * @param stats nested map of logger → level → count
     * @return XSSFWorkbook with a single sheet "stats"
     */
    private XSSFWorkbook generateExcel(Map<String, Map<String, Long>> stats) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        // must contain a single sheet names "stats"
        XSSFSheet sheet = workbook.createSheet("stats");

        // header row
        XSSFRow header = sheet.createRow(0);
        header.createCell(0).setCellValue("logger");
        List<String> levels = StatsHelper.getLevels();
        for (int i = 0; i < levels.size(); i++) {
            header.createCell(i + 1).setCellValue(levels.get(i));
        }

        // Loggers row
        int nextRow = 1;
        for (String logger: stats.keySet()) {
            XSSFRow row = sheet.createRow(nextRow);
            row.createCell(0).setCellValue(logger);
            for (int i = 0; i < levels.size(); i++) {
                row.createCell(i + 1).setCellValue(stats.get(logger).get(levels.get(i)));
            }
            nextRow++;
        }
        return workbook;
    }
}
