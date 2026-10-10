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
import java.util.logging.Level;
import java.util.logging.Logger;

@WebListener
public class AppStartupListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppStartupListener.class.getName());

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
            } catch (SQLException | RuntimeException e) {
                // Logged here too, not just Sentry — Sentry is a no-op without a configured
                // DSN, which would otherwise swallow this with zero visibility anywhere.
                LOG.log(Level.SEVERE, "Schema migration failed on startup", e);
                SentryReporter.captureException(e);
            }
        }
    }
}
