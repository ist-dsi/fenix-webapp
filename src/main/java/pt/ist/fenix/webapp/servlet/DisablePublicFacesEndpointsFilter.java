package pt.ist.fenix.webapp.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebFilter(urlPatterns = { "*.faces" })
public class DisablePublicFacesEndpointsFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(DisablePublicFacesEndpointsFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("registering DisableFacesPublicEndpointsFilter");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        final String requestURI = ((HttpServletRequest) request).getRequestURI();
        final String contextPath = ((HttpServletRequest) request).getContextPath();

        if (requestURI.startsWith(contextPath + "/publico/")) {
            logger.debug("request prevented for public faces: {}", requestURI);
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}