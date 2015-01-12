package pt.ist.fenix.webapp;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import pt.ist.fenixWebFramework.servlets.filters.contentRewrite.RequestChecksumFilter;

@WebListener
public class OAuthChecksumHack implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        RequestChecksumFilter.registerFilterRule(request -> {
            if (request.getRequestURI().equals(request.getContextPath() + "/person/externalApps.do")
                    && "appLogo".equals(request.getParameter("method"))) {
                return false;
            }
            return true;
        });
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
