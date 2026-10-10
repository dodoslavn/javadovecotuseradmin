package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.service.SentryReporter;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Catches whatever the container routes here per web.xml <error-page> and reports it to Sentry.
 * The container's error forward preserves the original request's HTTP method — so a failure
 * during a POST arrives here as a POST too. Without a doPost override, HttpServlet's default
 * would reply 405 instead of showing the error page, hiding the real exception. Both methods
 * funnel through the same handler.
 */
@WebServlet("/error")
public class ErrorServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Throwable t = (Throwable) req.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        if (t != null) {
            SentryReporter.captureException(t);
        }
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        req.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
