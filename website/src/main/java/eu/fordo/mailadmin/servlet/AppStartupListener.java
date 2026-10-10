package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.service.SentryReporter;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppStartupListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        SentryReporter.init();
    }
}
