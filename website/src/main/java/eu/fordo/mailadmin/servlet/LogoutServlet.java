package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.service.SessionUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        SessionUtil.logout(req);
        resp.sendRedirect(req.getContextPath() + "/login");
    }
}
