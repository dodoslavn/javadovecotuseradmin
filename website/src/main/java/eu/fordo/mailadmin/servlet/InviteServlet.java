package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.dao.InviteDao;
import eu.fordo.mailadmin.dao.WebAccountDao;
import eu.fordo.mailadmin.model.WebAccountInvite;
import eu.fordo.mailadmin.service.AuditService;
import eu.fordo.mailadmin.service.PasswordService;
import eu.fordo.mailadmin.service.SentryReporter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Public (unauthenticated) accept page for an invite link. The invitee opens /invite?token=...,
 * chooses their own username and password, and a web account with the invite's preset role is
 * created. The token is single-use and time-limited (see InviteDao / web_account_invites).
 * Exempted from AuthFilter so a logged-out invitee can reach it.
 */
@WebServlet("/invite")
public class InviteServlet extends HttpServlet {

    private final InviteDao inviteDao = new InviteDao();
    private final WebAccountDao webAccountDao = new WebAccountDao();
    private final PasswordService passwordService = new PasswordService();
    private final AuditService auditService = new AuditService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        try {
            Optional<WebAccountInvite> invite = token == null ? Optional.empty() : inviteDao.findByToken(token);
            if (invite.isEmpty() || !invite.get().isPending()) {
                req.setAttribute("invalid", Boolean.TRUE);
            } else {
                req.setAttribute("token", token);
                req.setAttribute("suggestedUsername", invite.get().getSuggestedUsername());
            }
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }
        req.getRequestDispatcher("/WEB-INF/jsp/invite_accept.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        try {
            Optional<WebAccountInvite> maybe = token == null ? Optional.empty() : inviteDao.findByToken(token);
            if (maybe.isEmpty() || !maybe.get().isPending()) {
                req.setAttribute("invalid", Boolean.TRUE);
                req.getRequestDispatcher("/WEB-INF/jsp/invite_accept.jsp").forward(req, resp);
                return;
            }
            WebAccountInvite invite = maybe.get();

            if (username == null || username.isBlank()) {
                renderError(req, resp, token, username, "webAccounts.error.usernameRequired");
                return;
            }
            username = username.trim();
            if (password == null || password.length() < 8) {
                renderError(req, resp, token, username, "account.error.tooShort");
                return;
            }
            if (webAccountDao.findByUsername(username).isPresent()) {
                renderError(req, resp, token, username, "webAccounts.error.duplicate");
                return;
            }

            // Claim the invite first — if someone else just used this same link, we lose the
            // race here and must not create a second account.
            String hash = passwordService.hashWebPassword(password);
            long newId = webAccountDao.create(username, hash, invite.getRole());
            boolean claimed = inviteDao.markUsedIfPending(invite.getId(), newId);
            if (!claimed) {
                // Extremely unlikely race: the invite was consumed between our check and now.
                webAccountDao.delete(newId);
                req.setAttribute("invalid", Boolean.TRUE);
                req.getRequestDispatcher("/WEB-INF/jsp/invite_accept.jsp").forward(req, resp);
                return;
            }
            auditService.recordActor(newId, "web_account_created", "web_account", newId,
                    "via invite, username=" + username + " role=" + invite.getRole());

            resp.sendRedirect(req.getContextPath() + "/login?invited=1");
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }
    }

    private void renderError(HttpServletRequest req, HttpServletResponse resp, String token, String username, String errorKey)
            throws ServletException, IOException {
        req.setAttribute("token", token);
        req.setAttribute("suggestedUsername", username);
        req.setAttribute("errorKey", errorKey);
        req.getRequestDispatcher("/WEB-INF/jsp/invite_accept.jsp").forward(req, resp);
    }
}
