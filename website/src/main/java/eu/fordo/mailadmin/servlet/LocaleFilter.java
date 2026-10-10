package eu.fordo.mailadmin.servlet;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Only English ships today (ALLOWED_LANGUAGES has one entry) but a "?lang=xx" switch and
 * session persistence are already wired, so adding a language later is just: add the
 * properties file, add its code here.
 * Mapped explicitly in web.xml (not @WebFilter) so it's guaranteed to run after
 * SetupGateFilter — annotation-scanned filter order isn't spec-guaranteed.
 */
public class LocaleFilter extends HttpFilter {

    public static final String ATTR_LOCALE = "resolvedLocale";
    private static final String SESSION_KEY = "locale";
    private static final List<String> ALLOWED_LANGUAGES = List.of("en");

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpSession session = req.getSession(true);
        String requested = req.getParameter("lang");
        if (requested != null && ALLOWED_LANGUAGES.contains(requested)) {
            session.setAttribute(SESSION_KEY, requested);
        }
        String lang = (String) session.getAttribute(SESSION_KEY);
        if (lang == null || !ALLOWED_LANGUAGES.contains(lang)) {
            lang = "en";
        }
        req.setAttribute(ATTR_LOCALE, Locale.forLanguageTag(lang));
        chain.doFilter(req, resp);
    }
}
