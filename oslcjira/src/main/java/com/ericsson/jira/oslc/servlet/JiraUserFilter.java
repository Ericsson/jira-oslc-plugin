/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *
 *     Michael Fiedler     - initial API and implementation for Bugzilla adapter
 *     
 *******************************************************************************/
package com.ericsson.jira.oslc.servlet;

import java.io.IOException;
import java.net.URI;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.ericsson.jira.oslc.managers.PermissionManager;
import com.ericsson.jira.oslc.services.OAuthServices;

/**
 * It's a filter for the requests from our plugin pages. It has different the operations with the requests than 
 * how the requests are handled from outside. That's the reason why we have two filters - 
 * CredentialsFilter and this one.
 * 
 */
public class JiraUserFilter implements Filter {
    
    private static Logger logger = LoggerFactory.getLogger(CredentialsFilter.class);
    
    public static final String CONNECTOR_ATTRIBUTE = "org.eclipse.lyo.oslc4j.jira.JiraConnector";
    public static final String CREDENTIALS_ATTRIBUTE = "org.eclipse.lyo.oslc4j.jira.Credentials";
    public static final String JAZZ_INVALID_EXPIRED_TOKEN_OAUTH_PROBLEM = "invalid_expired_token";
    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    private static final String BASIC_AUTHORIZATION_PREFIX = "Basic ";
    private static final String BASIC_AUTHENTICATION_CHALLENGE = BASIC_AUTHORIZATION_PREFIX + "realm=\"" + "JIRA" + "\"";
    private static final String OAUTH_AUTHORIZATION_PREFIX = "OAuth ";
    private static final String OAUTH_AUTHENTICATION_CHALLENGE = OAUTH_AUTHORIZATION_PREFIX + "realm=\"" + "JIRA" + "\"";
    private static final String CURRENT_CLASS = "JiraUserFilter";
    private final LoginUriProvider loginUriProvider;
    private com.atlassian.sal.api.user.UserManager userManager;
    private TemplateRenderer TR;
    
    public JiraUserFilter(LoginUriProvider loginUriProvider, com.atlassian.sal.api.user.UserManager userManager, TemplateRenderer templateRenderer) {
        this.loginUriProvider = loginUriProvider;
        this.userManager = userManager;
        this.TR = templateRenderer;
    }
    
    @Override
    public void destroy() {
    }
    
    /**
     * Check for OAuth or BasicAuth credentials and challenge if not found.
     */
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        String currentMethod = "doFilter";
        
        if (servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse) {
            
            HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            String requestUri = httpRequest.getRequestURI();
            
            if (requestUri.endsWith("/oauth/authorize")) {
              try {
                new OAuthServices(TR, loginUriProvider).authorize(httpRequest, httpResponse);
              } catch (Exception e) {
                logger.warn(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
                sendUnauthorizedResponse(httpResponse, e, OAUTH_AUTHENTICATION_CHALLENGE);
              }
              return;
            }
            
            if (requestUri.endsWith("/selectissue") || 
                requestUri.endsWith("/createissue") || 
                requestUri.endsWith("/addoslclinkdialog") || 
                requestUri.contains("/oslc/links/removeFromRemoteApp") || 
                requestUri.contains("/oslc/links/removeFromJira") || 
                requestUri.contains("/oslc/links/addToRemoteApp") || 
                requestUri.contains("/oslc/links/addToJira") || 
                requestUri.contains("/oauth/authorizationcallback") ||
                (requestUri.contains("/changeRequests/") && httpRequest.getHeader("accept").contains(MediaType.TEXT_HTML))) {
                
                try {
                    logger.debug(CURRENT_CLASS + "." + currentMethod + " UserAuthenticate: Requested URI = " + requestUri);
                    if (basicUserAuthenticate(httpRequest, httpResponse)) {
                        return;
                    }
                } catch (Exception e) {
                    logger.warn(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
                    sendUnauthorizedResponse(httpResponse, e, BASIC_AUTHENTICATION_CHALLENGE);
                    return;
                }
            } else if (requestUri.endsWith("/OAuthConsumerServlet") || 
                       requestUri.endsWith("/RootServicesManagementServlet") || 
                       requestUri.endsWith("/ProjectRelationshipsServlet") || 
                       requestUri.endsWith("/ServiceProviderCatalogsManagementServlet") ||
                       requestUri.contains("/oauth/consumers") || 
                       requestUri.contains("/oauth/rootserviceslinks") || 
                       requestUri.contains("/oauth/serviceprovidercatalogslinks") || 
                       requestUri.contains("/oauth/serviceproviderslinks")) {
                try {
                    logger.debug(CURRENT_CLASS + "." + currentMethod + " AdminAuthenticate: Requested URI = " + requestUri);
                    if (basicAdminAuthenticate(httpRequest, httpResponse)) {
                        return;
                    }
                } catch (Exception e) {
                    logger.warn(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
                    sendUnauthorizedResponse(httpResponse, e, BASIC_AUTHENTICATION_CHALLENGE);
                    return;
                }
            }
        }
        chain.doFilter(servletRequest, servletResponse);
        
    }
    
    public void init(FilterConfig arg0) throws ServletException {
    }
    
    /**
     * Send error response when the request was not authorized
     * @param response an error response
     * @param e Exception with error message
     * @param authChallenge OAuth challenge
     * @throws IOException
     * @throws ServletException
     */
    private static void sendUnauthorizedResponse(HttpServletResponse response, Exception e, String authChallenge) throws IOException, ServletException {
        
        response.reset();
        response.addHeader(WWW_AUTHENTICATE_HEADER, authChallenge);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.resetBuffer();
        
        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write(e.getMessage().getBytes());
    }
    
    /**
     * Authenticate a request with basic authentication. Authentication is done for common user, not admin.
     * @param httpRequest a request
     * @param httpResponse a response
     * @throws Exception
     */
    private boolean basicUserAuthenticate(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
        logger.debug("CredentialFilterB - basicAuthenticate");
        
        ApplicationUser user = null;
        user = PermissionManager.getLoggedUser();
        
        if (user == null) {
            redirectToLogin(httpRequest, httpResponse);
            return true;
        }
        
        return false;
    }
    
    /**
     * Authenticate a request with basic authentication. Authentication is done for admin.
     * @param httpRequest a request
     * @param httpResponse a response
     * @throws Exception
     */
    private boolean basicAdminAuthenticate(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
        logger.debug("CredentialFilterB - basicAuthenticate");
        
        ApplicationUser user = null;
        user = PermissionManager.getLoggedUser();
        
        if (user == null) {
            redirectToAdminLogin(httpRequest, httpResponse);
            return true;
        }
        
        if (!userManager.isSystemAdmin(user.getUsername())) {
            ComponentAccessor.getJiraAuthenticationContext().clearLoggedInUser();
            user = PermissionManager.getLoggedUser();
            if (user == null) {
                redirectToAdminLogin(httpRequest, httpResponse);
                return true;
            }
            return false;
        }
        
        return false;
    }
    
    /**
     * In case of unauthorized request the user is redirect to login page
     * @param request a request
     * @param response a response
     * @throws IOException
     */
    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
    }
    
    /**
     * In case of unauthorized request the admin is redirect to admin login page
     * @param request a request
     * @param response a response
     * @throws IOException
     */
    private void redirectToAdminLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString().replaceAll("user_role=USER", "user_role=ADMIN"));
    }
    
    /**
     * Get uri from a request
     * @param request a request
     * @return uri from a request
     */
    private URI getUri(HttpServletRequest request) {
        StringBuffer builder = request.getRequestURL();
        if (request.getQueryString() != null) {
            builder.append("?");
            builder.append(request.getQueryString());
        }
        return URI.create(builder.toString());
    }
}
