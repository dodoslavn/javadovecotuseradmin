package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.service.AppConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Must run before every other filter (explicit ordering in web.xml, not @WebFilter —
 * annotation-scanned filter order isn't guaranteed by the servlet spec). While DB
 * credentials haven't been written to /etc/mailadmin/config.properties yet, every request
 * except the wizard itself and static assets is redirected to /setup — otherwise any other
 * page would hit a DAO and throw a raw connection-refused exception.
 */
public class SetupGateFilter extends HttpFilter {

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        String path = req.getServletPath();
        boolean isSetupPath = path.equals("/setup");
        boolean isStatic = path.startsWith("/css/") || path.startsWith("/assets/");
        boolean configured = AppConfig.hasDbCredentials();

        if (!configured && !isSetupPath && !isStatic) {
            resp.sendRedirect(req.getContextPath() + "/setup");
            return;
        }
        if (configured && isSetupPath) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        chain.doFilter(req, resp);
    }
}
