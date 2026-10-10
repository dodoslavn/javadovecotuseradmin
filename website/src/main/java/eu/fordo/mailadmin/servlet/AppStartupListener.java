package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.dao.Database;
import eu.fordo.mailadmin.service.AppConfig;
import eu.fordo.mailadmin.service.SchemaInstaller;
import eu.fordo.mailadmin.service.SentryReporter;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Connection;
import java.sql.SQLException;

@WebListener
public class AppStartupListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        SentryReporter.init();

        // If DB credentials aren't configured yet, there's nothing to migrate — the setup
        // wizard (SetupServlet) runs SchemaInstaller itself once they're entered. If they
        // ARE already configured, this is what makes upgrading to a new WAR version with
        // schema changes just a matter of restarting Tomcat — no manual migration step.
        if (AppConfig.hasDbCredentials()) {
            try (Connection conn = Database.getConnection()) {
                SchemaInstaller.ensureSchema(conn);
            } catch (SQLException e) {
                SentryReporter.captureException(e);
            }
        }
    }
}
