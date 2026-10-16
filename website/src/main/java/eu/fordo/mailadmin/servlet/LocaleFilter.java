package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.dao.WebAccountDao;
import eu.fordo.mailadmin.service.SentryReporter;
import eu.fordo.mailadmin.service.SessionUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/**
 * Resolves the UI language for each request and remembers the choice. A "?lang=xx" switch wins
 * for the current request; the chosen language is then persisted three ways so it survives:
 *   1. the session (fast path for the rest of this browser session),
 *   2. a long-lived cookie (survives new sessions on this browser, logged in or not),
 *   3. the logged-in account's profile (web_accounts.language) — so it follows the user to any
 *      browser once they log in. On login (see LoginServlet) the profile language is pushed back
 *      into the session + cookie, so it wins over whatever this browser last used anonymously.
 * Resolution order per request: ?lang= → session → cookie → "en".
 *
 * Adding a language is just: add the messages_<lang>.properties file, add its code (and native
 * display name) here/in ALLOWED_LANGUAGES and in nav/footer. Same language set as the other admin
 * tools in this family (see phpopenvpnadmin's web/lang/).
 * Mapped explicitly in web.xml (not @WebFilter) so it's guaranteed to run after SetupGateFilter —
 * annotation-scanned filter order isn't spec-guaranteed.
 */
public class LocaleFilter extends HttpFilter {

    public static final String ATTR_LOCALE = "resolvedLocale";
    private static final String SESSION_KEY = "locale";
    private static final String COOKIE_NAME = "lang";
    private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 365; // one year
    private static final List<String> ALLOWED_LANGUAGES = List.of("en", "cs", "de", "es", "hu", "it", "pl", "sk", "uk");

    private final WebAccountDao webAccountDao = new WebAccountDao();

    public static boolean isAllowed(String lang) {
        return lang != null && ALLOWED_LANGUAGES.contains(lang);
    }

    /**
     * Record {@code lang} as the active language in the session and in a long-lived cookie.
     * No-op if {@code lang} isn't a supported code. Used both by the ?lang= switch below and by
     * LoginServlet when applying a logged-in account's saved language.
     */
    public static void remember(HttpServletRequest req, HttpServletResponse resp, String lang) {
        if (!isAllowed(lang)) {
            return;
        }
        req.getSession(true).setAttribute(SESSION_KEY, lang);
        Cookie cookie = new Cookie(COOKIE_NAME, lang);
        String ctx = req.getContextPath();
        cookie.setPath(ctx == null || ctx.isEmpty() ? "/" : ctx);
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        resp.addCookie(cookie);
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpSession session = req.getSession(true);

        String requested = req.getParameter("lang");
        if (isAllowed(requested)) {
            remember(req, resp, requested);
            // If a logged-in user explicitly switches, persist it to their profile too so the
            // choice follows them to other browsers. Best-effort — a DB hiccup must not break
            // page rendering, so log to Sentry and carry on with the session/cookie value.
            Long accountId = SessionUtil.currentWebAccountId(req);
            if (accountId != null) {
                try {
                    webAccountDao.setLanguage(accountId, requested);
                } catch (SQLException e) {
                    SentryReporter.captureException(e);
                }
            }
        }

        String lang = (String) session.getAttribute(SESSION_KEY);
        if (!isAllowed(lang)) {
            lang = cookieLanguage(req);
            if (isAllowed(lang)) {
                session.setAttribute(SESSION_KEY, lang); // cache the cookie value for the session
            }
        }
        if (!isAllowed(lang)) {
            lang = "en";
        }
        req.setAttribute(ATTR_LOCALE, Locale.forLanguageTag(lang));
        chain.doFilter(req, resp);
    }

    private static String cookieLanguage(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (COOKIE_NAME.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
