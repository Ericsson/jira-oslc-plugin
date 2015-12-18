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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.server.OAuthServlet;

import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;
import org.eclipse.lyo.server.oauth.core.Application;
import org.eclipse.lyo.server.oauth.core.OAuthConfiguration;
import org.eclipse.lyo.server.oauth.core.OAuthRequest;
import org.eclipse.lyo.server.oauth.core.token.SimpleTokenStrategy;
import org.eclipse.lyo.server.oauth.core.utils.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.security.login.LoginResult;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.login.LoginManager;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.jira.user.util.UserManager;
import com.ericsson.jira.oslc.Credentials;
import com.ericsson.jira.oslc.constants.JiraConstants;
import com.ericsson.jira.oslc.handlers.OAuthHandler;
import com.ericsson.jira.oslc.services.OAuthServices;

/**
 * Called by the web container when a request is processed
 */
public class CredentialsFilter implements Filter {

  private static Logger logger = LoggerFactory.getLogger(CredentialsFilter.class);
  
  public static final String CONNECTOR_ATTRIBUTE = "org.eclipse.lyo.oslc4j.jira.JiraConnector";
  public static final String USERNAME_ATTRIBUTE = "org.eclipse.lyo.oslc4j.jira.Credentials";
  public static final String JAZZ_INVALID_EXPIRED_TOKEN_OAUTH_PROBLEM = "invalid_expired_token";
 
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
  private static final String BASIC_AUTHORIZATION_PREFIX = "Basic ";
  private static final String BASIC_AUTHENTICATION_CHALLENGE = BASIC_AUTHORIZATION_PREFIX + "realm=\"" + "JIRA"+ "\"";
  private static final String OAUTH_AUTHORIZATION_PREFIX = "OAuth ";
  private static final String OAUTH_AUTHENTICATION_CHALLENGE = OAUTH_AUTHORIZATION_PREFIX + "realm=\"" + "JIRA" + "\"";
  public static Map<String, String> tokenToUserNameMap = new HashMap<String, String>();

  private static final String CURRENT_CLASS = "CredentialsFilter";
  

  
  public void destroy() {
  }
  
  /**
   * Check for OAuth or BasicAuth credentials and challenge if not found.
   */
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) 
        throws IOException, ServletException {
    String currentMethod = "doFilter";
      
    if (servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse) {
          
      HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
      HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
      String requestUri = httpRequest.getRequestURI();
      
      logger.debug(CURRENT_CLASS + "." + currentMethod + " Requested URI = " + requestUri);
      
      // Because requests could be consumed be other filters in chain (especially OAuth related),
      // some of these requests are handled manually.

      if (requestUri.endsWith("/oauth/requestToken")) {
        new OAuthServices().requestToken(httpRequest, httpResponse);
        return;
      } 
      else if (requestUri.endsWith("/oauth/accessToken")) {
        new OAuthServices().accessToken(httpRequest, httpResponse);
        return;
      } 
      else if (requestUri.endsWith("/oauth/requestKey")) {
        logger.debug(CURRENT_CLASS + "." + currentMethod + " Pass requested URI");
      }
      else if (requestUri.contains("/oauth/authorizationexternalcallback")) {
        new OAuthServices().oAuthAuthorizationCallback(httpRequest, httpResponse);
        return;
      }
      else if (requestUri.contains("/changeRequests/") && httpRequest.getHeader("accept").contains(MediaType.TEXT_HTML)) {
        // do nothing
        // this request will be caught in JiraUserFilter
      }
      else if (isProtectedResource(requestUri)) {
        // Assume an OAuth protected resource request      
        logger.debug("CredentialsFilter OAuth protected resource request " + requestUri);
          
        OAuthMessage message = OAuthServlet.getMessage(httpRequest, null);
        if (message.getToken() != null) {
          try {
            servletRequest = oAuthAuthenticate(OAuthServlet.getMessage(httpRequest, null), httpRequest, httpResponse);
          } 
          catch (Exception e) {
            logger.warn(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());		
            sendUnauthorizedResponse(httpResponse, e, OAUTH_AUTHENTICATION_CHALLENGE);
            return;
          }
        } 
        else {
          try {
            basicAuthenticate(httpRequest, httpResponse);
          } 
          catch (Exception e) {
            logger.warn(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
            sendUnauthorizedResponse(httpResponse, e, BASIC_AUTHENTICATION_CHALLENGE);
            return;
          }
        }         
      } 
      else {
        if (requestUri.endsWith("/rootservices")) {
          //prevent request to be consumed by other filters in chain by disabling 'Authorization' headers
          servletRequest = new HttpServletRequestWrapper(httpRequest) {
            @Override
            public String getHeader(String name) {
              if (name.equalsIgnoreCase("authorization")) {
                return null;
              }
              return super.getHeader(name);
            }
            
            @Override
            public Enumeration<String> getHeaders(String name) {
              if (name.equalsIgnoreCase("authorization")) {
                return Collections.enumeration(new ArrayList<String>());
              }
              return super.getHeaders(name);
            }
            
            @Override
            public Enumeration<String> getHeaderNames() {
              Enumeration<String> headerNames = super.getHeaderNames();
              ArrayList<String> result = new ArrayList<String>();
              while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                if (!name.equalsIgnoreCase("authorization")) {
                  result.add(name);
                }
              }
              headerNames = Collections.enumeration(result);
              return headerNames;
            }
          }; //end of HttpServletRequestWrapper
        }
      }
    }
    chain.doFilter(servletRequest, servletResponse);

  }//end of doFilter method

  /**
   * Check if the resource is protected
   * @param requestUri the uri of the resource which will be checked
   * @return true - the resource is protected, otherwise false
   */
  private boolean isProtectedResource(String requestUri) {
      if (requestUri.endsWith("/rootservices")
        || requestUri.endsWith("/oauth/internal/approveToken") || requestUri.endsWith("/oauth/login")) {
      return false;
    }
    return true;
  }

  /**
   * Initialize the filter
   */
  public void init(FilterConfig arg0) throws ServletException {
    System.setProperty("org.eclipse.lyo.oslc4j.alwaysXMLAbbrev", "true");
    
    OAuthConfiguration config = OAuthConfiguration.getInstance();
    
    // Validates a user's ID and password.
    config.setApplication(new Application() {
      public void login(HttpServletRequest request, String id, String password){}
      
      public String getName() {
        // Display name for this application.
        return "Jira";
      }
      
      public boolean isAdminSession(HttpServletRequest request) {
          return false;
      }

      public String getRealm(HttpServletRequest request) {
        return JiraConstants.REALM_NAME;
      }

      public boolean isAuthenticated(HttpServletRequest request) {
        return true;
      }
    }
    
    ); //end of Application
    
    config.setTokenStrategy(new SimpleTokenStrategy() {
      @Override
      public void generateAccessToken(OAuthRequest oAuthRequest)
          throws OAuthProblemException, IOException {
        String requestToken = oAuthRequest.getMessage().getToken();
        String userName = tokenToUserNameMap.remove(requestToken);
        super.generateAccessToken(oAuthRequest);
        tokenToUserNameMap.put(oAuthRequest.getAccessor().accessToken, userName);
      }
    });// end of SimpleTokenStrategy
    
    try {
      OAuthHandler.loadOAuthConsumers(config);
    } 
    catch (Exception e) {
      logger.error(CURRENT_CLASS + " Exception: " + e.getMessage());
      throw new ServletException(e.getMessage());
    }
    
  }//end of init method

  /**
   * Authenticate incoming request
   * @param oAuthMessage OAuth message
   * @param httpRequest a request
   * @param responsea a response
   * @return wrapper of the requests which handles authorization headers
   * @throws Exception
   */
  private HttpServletRequestWrapper oAuthAuthenticate(OAuthMessage oAuthMessage, 
      HttpServletRequest httpRequest, HttpServletResponse response) throws Exception {
    try {
      OAuthRequest oAuthRequest = new OAuthRequest(httpRequest);
      logger.debug(httpRequest.getHeader("Authorization"));
      
      oAuthRequest.validate();
      
      logger.debug("CredentialsFilter request validated");
      
      httpRequest.setAttribute(USERNAME_ATTRIBUTE, tokenToUserNameMap.get(oAuthRequest.getMessage().getToken()));
      
      // If we have validated the access token continue with the filter
      // chain but mask out any authorization headers
      HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(httpRequest) {
        @Override
        public String getHeader(String name) {
          if (name.equalsIgnoreCase("authorization")) {
            return null;
          }
          return super.getHeader(name);
        }
        
        @Override
        public Enumeration<String> getHeaders(String name) {
          if (name.equalsIgnoreCase("authorization")) {
            return Collections.enumeration(new ArrayList<String>());
          }
          return super.getHeaders(name);
        }
        
        @Override
        public Enumeration<String> getHeaderNames() {
          Enumeration<String> headerNames = super.getHeaderNames();
          ArrayList<String> result = new ArrayList<String>();
          while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (!name.equalsIgnoreCase("authorization")) {
              result.add(name);
            }
          }
          headerNames = Collections.enumeration(result);
          return headerNames;
        }
      }; //end of HttpServletRequestWrapper
      
      return requestWrapper;
    } 
    catch (OAuthProblemException e) {
      StringBuilder builder = new StringBuilder();
      Map<String, Object> parameters = e.getParameters();
      for (String key : parameters.keySet()) {
        builder.append(' ');
        builder.append(key);
        builder.append('=');
        builder.append(parameters.get(key));
      }
      
      logger.error("CredentialsFilter exception = " + e.getMessage() + builder.toString());
      throw new UnauthorizedException("invalid_expired_token");
    } 
    catch (OAuthException e) {
      logger.error(CURRENT_CLASS + " Exception: " + e.getMessage());
      throw new UnauthorizedException("invalid_expired_token");
    }
  }//end of oAuthAuthenticate method

  /**
   * Send error response when the request was not authorized
   * @param response an error response
   * @param e Exception with error message
   * @param authChallenge OAuth challenge
   * @throws IOException
   * @throws ServletException
   */
  private static void sendUnauthorizedResponse(
      HttpServletResponse response, Exception e, String authChallenge) throws IOException, ServletException {
    response.reset();
    response.addHeader(WWW_AUTHENTICATE_HEADER, authChallenge);       
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.resetBuffer();
    
    ServletOutputStream outputStream = response.getOutputStream();
    outputStream.write(e.getMessage().getBytes());    
  }

  /**
   * Authenticate a request with basic authentication
   * @param httpRequest a request
   * @param httpResponse a response
   * @throws Exception
   */
  private void basicAuthenticate(HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
      throws Exception {
    logger.debug("CredetialFilter - basicAuthenticate");
    
    Credentials credentials = getCredentials(httpRequest);
    
    UserManager userManager = ComponentAccessor.getComponent(UserManager.class);
    User user = ApplicationUsers.toDirectoryUser(userManager.getUserByName(credentials.getUsername()));
    if (user == null) {
      logger.warn("User is not registered in the system");
      throw new UnauthorizedException("User is not registered in the system");
    }
    
   
    LoginManager loginManager = (LoginManager)ComponentAccessor.getComponent(LoginManager.class);
    LoginResult result = loginManager.authenticate(user, credentials.getPassword());
    if (result.isOK()) {
      logger.debug("CredentialsFilter request - user authorized " + credentials.getUsername() + "authorized");
      httpRequest.setAttribute(USERNAME_ATTRIBUTE, credentials.getUsername());
    }
    else{
      logger.warn("User is not registered in the system");
      throw new UnauthorizedException("Verification of user failed.");
    }
  }

  /**
   * Get the credentials for the request
   * @param request a request
   * @return credentials containing username and password
   * @throws UnauthorizedException
   */
  public static Credentials getCredentials(HttpServletRequest request) throws UnauthorizedException {
    String currentMethod = "getCredentials";
    logger.debug("CredentialsFilter request - getCredentials");
    String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
    if (authorizationHeader == null || "".equals(authorizationHeader)) {
      logger.warn("Invalid Authorization header.");
      throw new UnauthorizedException("Invalid Authorization header.");
    }
    
    Credentials credentials = new Credentials();
    if (!authorizationHeader.startsWith(BASIC_AUTHORIZATION_PREFIX)) {
      logger.warn("Only basic access authentication is supported.");
      throw new UnauthorizedException("Only basic access authentication is supported.");
    }
    
    String encodedString = authorizationHeader.substring(BASIC_AUTHORIZATION_PREFIX.length());
    try {
      String unencodedString = new String(Base64.decode(encodedString), "UTF-8");
      int seperator = unencodedString.indexOf(':');
      if (seperator == -1) {
        logger.warn("Invalid Authorization header value.");
        throw new UnauthorizedException("Invalid Authorization header value.");
      }
      
      credentials.setUsername(unencodedString.substring(0, seperator));
      credentials.setPassword(unencodedString.substring(seperator + 1));
      
    } 
    catch (UnsupportedEncodingException e) {
      logger.warn(CURRENT_CLASS + "." + currentMethod + "Invalid Authorization header value. - " +" Exception: " + e.getMessage());
      throw new UnauthorizedException("Invalid Authorization header value.");
    } 
    catch (Base64DecodingException e) {
      logger.warn(CURRENT_CLASS + "." + currentMethod + "Invalid Authorization header value. - " +" Exception: " + e.getMessage());
      throw new UnauthorizedException("Invalid Authorization header value.");
    }
    
    return credentials;
  }
}
