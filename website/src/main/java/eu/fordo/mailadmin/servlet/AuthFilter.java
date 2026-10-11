package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.service.SessionUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Not logged in -> redirect to login. Logged in but USER hitting an admin-only path -> 403.
 * Role is always re-read from the session (see SessionUtil), never trusted from the client.
 * Mapped explicitly in web.xml (not @WebFilter) so it's guaranteed to run after
 * SetupGateFilter — annotation-scanned filter order isn't spec-guaranteed.
 */
public class AuthFilter extends HttpFilter {

    private static final String LOGIN_PATH = "/login";
    private static final String SETUP_PATH = "/setup";

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        String path = req.getServletPath();

        if (path.equals(LOGIN_PATH) || path.equals(SETUP_PATH) || path.startsWith("/css/") || path.startsWith("/assets/")) {
            chain.doFilter(req, resp);
            return;
        }

        if (!SessionUtil.isLoggedIn(req)) {
            resp.sendRedirect(req.getContextPath() + LOGIN_PATH);
            return;
        }

        if (path.startsWith("/admin/") && !SessionUtil.isAdmin(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return;
        }

        chain.doFilter(req, resp);
    }
}
