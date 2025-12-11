package io.github.nagare.logging.server;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;

import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * Servlet for returning log statistics in CSV format.
 * Accessible at GET /logstore/stats/csv.
 * Response is tab-separated (\t) with newline (\n) line breaks.
 * Column structure follows the assignment specification:
 * <a href="https://docs.google.com/drawings/d/1v_dpZ0XKiqTaygmOaTThWgR9swRDatY6sxYmGEn9GVM/edit?usp=sharing">...</a>
 * Content type: text/csv
 * Status: 200 on success
 */
public class StatsCSVServlet extends HttpServlet {

    private StatsHelper helper;

    // Explicitly defined default constructor
    public StatsCSVServlet() {
    }


    /**
     * Initialize servlet - get EntityManagerFactory from ServletContext
     */
    @Override
    public void init() throws ServletException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute(ServletAttributes.EMF_ATTRIBUTE);
        if (emf == null) {
            throw new ServletException("EntityManagerFactory not found");
        }
        LogEventRepository repository = new LogEventRepository(emf);
        this.helper = new StatsHelper(repository);
    }


    /**
     * Handles GET requests and writes CSV statistics to the response.
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/csv");
        String csvData = generateCSV(helper.getLogStatistics());
        resp.setStatus(200);
        resp.getWriter().write(csvData);
    }


    /**
     * Generate tab-separated CSV data from aggregated log statistics.
     * @param stats nested map of logger → level → count
     * @return CSV string with header and rows for each logger
     */
    private String generateCSV(Map<String, Map<String, Long>> stats) {
        StringBuilder csv = new StringBuilder();
        // header row
        csv.append("logger");
        List<String> levels = StatsHelper.getLevels();
        for (String level: levels) {
            csv.append("\t").append(level);
        }
        csv.append("\n");

        // Loggers row
        for (String logger: stats.keySet()) {
            csv.append(logger);
            for (String level: levels) {
                csv.append("\t").append(stats.get(logger).get(level));
            }
            csv.append("\n");
        }
        return csv.toString();
    }
}
