package eu.fordo.mailadmin.service;

import eu.fordo.mailadmin.model.Role;
import eu.fordo.mailadmin.model.WebAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Stores only plain attributes (not the WebAccount object itself) in the session, so there's
 * no serialization surface to worry about if Tomcat ever persists sessions to disk, and so
 * role is always re-read fresh rather than cached stale across a long session.
 */
public final class SessionUtil {

    private static final String ATTR_ID = "webAccountId";
    private static final String ATTR_USERNAME = "webAccountUsername";
    private static final String ATTR_ROLE = "webAccountRole";

    private SessionUtil() {}

    public static void login(HttpServletRequest req, WebAccount account) {
        HttpSession session = req.getSession(true);
        session.setAttribute(ATTR_ID, account.getId());
        session.setAttribute(ATTR_USERNAME, account.getUsername());
        session.setAttribute(ATTR_ROLE, account.getRole().name());
    }

    public static void logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public static Long currentWebAccountId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        return (Long) session.getAttribute(ATTR_ID);
    }

    public static String currentUsername(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (String) session.getAttribute(ATTR_USERNAME);
    }

    public static Role currentRole(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        String role = (String) session.getAttribute(ATTR_ROLE);
        return role == null ? null : Role.valueOf(role);
    }

    public static boolean isLoggedIn(HttpServletRequest req) {
        return currentWebAccountId(req) != null;
    }

    public static boolean isAdmin(HttpServletRequest req) {
        return currentRole(req) == Role.ADMIN;
    }
}
