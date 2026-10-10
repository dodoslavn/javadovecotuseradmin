package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.service.AppConfig;
import eu.fordo.mailadmin.service.PasswordService;
import eu.fordo.mailadmin.service.SchemaInstaller;
import eu.fordo.mailadmin.service.SentryReporter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * First-run setup wizard, reachable only while AppConfig.hasDbCredentials() is false (see
 * SetupGateFilter, which otherwise routes every request here). Flow:
 *   1. choose DB system + database name
 *   2. shown the exact commands to create that database/user, fill in the real connection
 *      details (prefilled with a suggested username/password matching the shown commands)
 *   3. on connect: create any missing tables (idempotent — an existing install's data is left
 *      alone); if web_accounts is empty, prompt for the first admin account; either way,
 *      finally write /etc/mailadmin/config.properties and send the admin to /login.
 * Deliberately does NOT use Database/WebAccountDao here — those read connection settings
 * from AppConfig, which isn't written yet during setup. Everything in this servlet connects
 * with whatever the admin just typed into the form instead.
 */
@WebServlet("/setup")
public class SetupServlet extends HttpServlet {

    static {
        // Database.java's static init normally does this, but this servlet deliberately
        // calls DriverManager directly instead of going through Database/AppConfig.
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MariaDB JDBC driver not on classpath", e);
        }
    }

    private final PasswordService passwordService = new PasswordService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/setup_step1.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        switch (action) {
            case "step1" -> handleStep1(req, resp);
            case "install" -> handleInstall(req, resp);
            case "createAdmin" -> handleCreateAdmin(req, resp);
            default -> resp.sendRedirect(req.getContextPath() + "/setup");
        }
    }

    private void handleStep1(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String dbName = req.getParameter("dbName");
        if (dbName == null || dbName.isBlank()) {
            req.setAttribute("error", "Database name is required");
            req.getRequestDispatcher("/WEB-INF/jsp/setup_step1.jsp").forward(req, resp);
            return;
        }
        dbName = dbName.trim();
        String suggestedUser = dbName.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        String suggestedPassword = randomPassword();

        populateStep2(req, dbName, "127.0.0.1", "3306", suggestedUser, suggestedPassword, null);
        req.getRequestDispatcher("/WEB-INF/jsp/setup_step2.jsp").forward(req, resp);
    }

    private void handleInstall(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String dbName = req.getParameter("dbName");
        String host = req.getParameter("host");
        String port = req.getParameter("port");
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String url = buildUrl(host, port, dbName);

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            SchemaInstaller.ensureSchema(conn);
            if (SchemaInstaller.hasAnyWebAccount(conn)) {
                // Existing installation's data found — don't touch it, just adopt these credentials.
                finishSetup(req, resp, url, username, password);
            } else {
                populateStep2(req, dbName, host, port, username, password, null);
                req.setAttribute("dbUrl", url);
                req.getRequestDispatcher("/WEB-INF/jsp/setup_step3.jsp").forward(req, resp);
            }
        } catch (SQLException e) {
            populateStep2(req, dbName, host, port, username, password,
                    "Could not connect or create tables: " + e.getMessage() + " — make sure you ran the commands above.");
            req.getRequestDispatcher("/WEB-INF/jsp/setup_step2.jsp").forward(req, resp);
        }
    }

    private void handleCreateAdmin(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String dbName = req.getParameter("dbName");
        String host = req.getParameter("host");
        String port = req.getParameter("port");
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String url = buildUrl(host, port, dbName);

        String adminUsername = req.getParameter("adminUsername");
        String adminPassword = req.getParameter("adminPassword");

        if (adminUsername == null || adminUsername.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            req.setAttribute("error", "Admin username and password are required");
            req.setAttribute("dbUrl", url);
            populateStep2(req, dbName, host, port, username, password, null);
            req.getRequestDispatcher("/WEB-INF/jsp/setup_step3.jsp").forward(req, resp);
            return;
        }

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String hash = passwordService.hashWebPassword(adminPassword);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO web_accounts (username, password_hash, role, enabled) VALUES (?, ?, 'ADMIN', TRUE)")) {
                ps.setString(1, adminUsername.trim());
                ps.setString(2, hash);
                ps.executeUpdate();
            }
            finishSetup(req, resp, url, username, password);
        } catch (SQLException e) {
            req.setAttribute("error", "Could not create the admin account: " + e.getMessage());
            req.setAttribute("dbUrl", url);
            populateStep2(req, dbName, host, port, username, password, null);
            req.getRequestDispatcher("/WEB-INF/jsp/setup_step3.jsp").forward(req, resp);
        }
    }

    private void finishSetup(HttpServletRequest req, HttpServletResponse resp, String url, String username, String password)
            throws IOException {
        Map<String, String> updates = new LinkedHashMap<>();
        updates.put("db.url", url);
        updates.put("db.user", username);
        updates.put("db.password", password);
        try {
            AppConfig.update(updates);
        } catch (IOException e) {
            SentryReporter.captureException(e);
            throw e;
        }
        resp.sendRedirect(req.getContextPath() + "/login");
    }

    private void populateStep2(HttpServletRequest req, String dbName, String host, String port,
                                String username, String password, String error) {
        req.setAttribute("dbName", dbName);
        req.setAttribute("host", host);
        req.setAttribute("port", port);
        req.setAttribute("username", username);
        req.setAttribute("password", password);
        req.setAttribute("commands", buildCommands(dbName, username, password));
        if (error != null) {
            req.setAttribute("error", error);
        }
    }

    private String buildCommands(String dbName, String username, String password) {
        return "sudo mkdir -p /etc/mailadmin && sudo chown root:tomcat /etc/mailadmin && sudo chmod 2770 /etc/mailadmin\n\n"
                + "sudo mysql -e \"\n"
                + "CREATE DATABASE IF NOT EXISTS `" + dbName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n"
                + "CREATE USER IF NOT EXISTS '" + username + "'@'localhost' IDENTIFIED BY '" + password + "';\n"
                + "GRANT ALL PRIVILEGES ON `" + dbName + "`.* TO '" + username + "'@'localhost';\n"
                + "FLUSH PRIVILEGES;\"";
    }

    private String buildUrl(String host, String port, String dbName) {
        return "jdbc:mariadb://" + host + ":" + port + "/" + dbName;
    }

    private String randomPassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
