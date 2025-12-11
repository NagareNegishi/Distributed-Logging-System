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
 * Servlet for returning log statistics as an HTML page with a table.
 * Accessible at GET /logstore/stats/html.
 * Response is text/html containing a <table> element with rows and columns as specified in the assignment:
 * <a href="https://docs.google.com/drawings/d/1v_dpZ0XKiqTaygmOaTThWgR9swRDatY6sxYmGEn9GVM/edit?usp=sharing">...</a>
 * Table uses standard HTML tags: <table>, <tr>, <th>, <td>.
 * Status: 200 on success
 */
public class StatsHTMLServlet extends HttpServlet  {

    private StatsHelper helper;

    // Explicitly defined default constructor
    public StatsHTMLServlet() {
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
     * Handles GET requests and writes HTML table statistics to the response.
     * @param req  HTTP request
     * @param resp HTTP response
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        String htmlData = generateHTML(helper.getLogStatistics());
        resp.setStatus(200);
        resp.getWriter().write(htmlData);
    }


    /**
     * Generate an HTML page containing a statistics table.
     * <a href="https://www.w3schools.com/html/html_tables.asp">...</a>
     * @param stats nested map of logger → level → count
     * @return HTML string with a complete table
     */
    private String generateHTML(Map<String, Map<String, Long>> stats) {
        StringBuilder html = new StringBuilder();
        // Basic HTML structure
        html.append("<!DOCTYPE html>");
        html.append("<html><body>");

        // table start
        html.append("<table>");
        List<String> levels = helper.getLevels();

        // header row
        html.append("<tr>");
        html.append("<th>logger</th>");
        for (String level: levels) {
            html.append("<th>").append(level).append("</th>");
        }
        html.append("</tr>");

        // Loggers row
        for (String logger: stats.keySet()) {
            html.append("<tr>");
            html.append("<td>").append(logger).append("</td>");
            for (String level: levels) {
                html.append("<td>").append(stats.get(logger).get(level)).append("</td>");
            }
            html.append("</tr>");
        }

        html.append("</table>");
        html.append("</body></html>");
        return html.toString();
    }

}
