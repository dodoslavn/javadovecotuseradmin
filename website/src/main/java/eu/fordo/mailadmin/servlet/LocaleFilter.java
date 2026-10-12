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
 * A "?lang=xx" switch and session persistence are wired here; adding a language is just:
 * add the messages_<lang>.properties file, add its code (and native display name) here and
 * in nav.jspf's LANGUAGE_NAMES. Same language set as the other admin tools in this family
 * (see phpopenvpnadmin's web/lang/).
 * Mapped explicitly in web.xml (not @WebFilter) so it's guaranteed to run after
 * SetupGateFilter — annotation-scanned filter order isn't spec-guaranteed.
 */
public class LocaleFilter extends HttpFilter {

    public static final String ATTR_LOCALE = "resolvedLocale";
    private static final String SESSION_KEY = "locale";
    private static final List<String> ALLOWED_LANGUAGES = List.of("en", "cs", "de", "es", "hu", "it", "pl", "sk", "uk");

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
