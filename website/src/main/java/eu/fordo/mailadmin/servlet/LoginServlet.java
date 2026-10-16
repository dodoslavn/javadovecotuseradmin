package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.dao.WebAccountDao;
import eu.fordo.mailadmin.model.WebAccount;
import eu.fordo.mailadmin.service.PasswordService;
import eu.fordo.mailadmin.service.SentryReporter;
import eu.fordo.mailadmin.service.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final WebAccountDao webAccountDao = new WebAccountDao();
    private final PasswordService passwordService = new PasswordService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (SessionUtil.isLoggedIn(req)) {
            resp.sendRedirect(req.getContextPath() + "/");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            Optional<WebAccount> account = webAccountDao.findByUsername(username == null ? "" : username.trim());
            if (account.isPresent() && account.get().isEnabled()
                    && passwordService.verifyWebPassword(password == null ? "" : password, account.get().getPasswordHash())) {
                SessionUtil.login(req, account.get());
                // Adopt the account's saved language (if any) so it follows the user to this
                // browser, overriding whatever was selected anonymously beforehand.
                LocaleFilter.remember(req, resp, account.get().getLanguage());
                resp.sendRedirect(req.getContextPath() + "/");
                return;
            }
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException("Login failed due to a server error", e);
        }

        req.setAttribute("error", Boolean.TRUE);
        req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
    }
}
