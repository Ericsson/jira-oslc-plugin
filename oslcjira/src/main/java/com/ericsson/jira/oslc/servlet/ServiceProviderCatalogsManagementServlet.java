package com.ericsson.jira.oslc.servlet;

/*
* Copyright (C) 2015 Ericsson AB. All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer
*    in the documentation and/or other materials provided with the
*    distribution.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
* "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
* LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
* A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
* OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
* DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
* THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
* OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.UrlValidator;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.ericsson.jira.oslc.constants.JiraConstants;
import com.ericsson.jira.oslc.handlers.OAuthHandler;
import com.ericsson.jira.oslc.managers.JiraManager;
import com.ericsson.jira.oslc.managers.PermissionManager;
import com.ericsson.jira.oslc.resources.ao.AOManager;
import com.ericsson.jira.oslc.resources.ao.RootServicesEntity;
import com.ericsson.jira.oslc.utils.OSLCUtils;

/**
 * A servlet which is responsible for loading and saving OSLC catalogs from/to db
 *
 */
public class ServiceProviderCatalogsManagementServlet extends HttpServlet {
  
  private static final long serialVersionUID = -5943393454095634269L;
  private final UserManager userManager; 
  private final LoginUriProvider loginUriProvider;
  private final TemplateRenderer renderer;
  
  public ServiceProviderCatalogsManagementServlet(UserManager userManager, 
      LoginUriProvider loginUriProvider, TemplateRenderer renderer) {
    this.userManager = userManager;
    this.loginUriProvider = loginUriProvider;
    this.renderer = renderer;
  }

  /**
   * Reload the page with the list of OSLC catalogs
   * @param response a response
   * @param errorMessage the error message which will be displayed on the page
   * @throws IOException
   * @throws ServletException
   */
  private void reloadPage(HttpServletResponse response, String errorMessage) 
      throws IOException, ServletException {
    Map<String, Object> params = new HashMap<String, Object>();
    
    AOManager mngr = AOManager.getInstance();
    final List<RootServicesEntity> entities = mngr.all(RootServicesEntity.class);
    ListIterator<RootServicesEntity> it = entities.listIterator();
    while (it.hasNext()) {
      RootServicesEntity rse = it.next();
      if (rse.isRootServices() == true) {
        it.remove();
      }
    }
    params.put("errorMsg", errorMessage);
    params.put("entities", entities);
    params.put("catalogsManagementPage", JiraManager.getBaseUrl() + JiraConstants.CATALOGS_MANAGEMENT_PAGE);
    params.put("removeSPCLink", JiraManager.getRestUrl() + JiraConstants.REMOVE_SP_CATALOG_LINK);
    
    response.setContentType("text/html;charset=utf-8");
    renderer.render("templates/admin_spcatalogs.vm", params, response.getWriter());
  }
  
  /**
   * Load the page with the list of OSLC catalogs
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

    String username = userManager.getRemoteUsername(request);
    if (username == null || !userManager.isSystemAdmin(username)) {
      redirectToLogin(request, response);
      return;
    }

    OAuthHandler.clearRootServicesCache();
    reloadPage(response, "");
  }

  /**
   * In case of unauthorized access redirect a user to login page
   * @param request a request
   * @param response a response
   * @throws IOException
   */
  private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
  }

  /**
   * Gets a URI of a request
   * @param request  HttpServletRequest
   * @return the URI of the request
   */
  private URI getUri(HttpServletRequest request) {
    StringBuffer builder = request.getRequestURL();
    if (request.getQueryString() != null) {
      builder.append("?");
      builder.append(request.getQueryString());
    }
    return URI.create(builder.toString());
  }
  
  /**
   * Save OSLC catalog to db
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
    
    ApplicationUser user = PermissionManager.getLoggedUser();
    if (user == null) {
      reloadPage(response, "Can't continue.\\nMessage: User not defined!");
      return;
    }
    
    if (!PermissionManager.isSystemAdmin(userManager)) {
      reloadPage(response, "Can't continue.\\nMessage: User not system admin!");
      return;
    }
    
    //get values from form
    final String title = req.getParameter("title");
    final String catalogURI = req.getParameter("spcuri").trim();
    final String consKey = req.getParameter("oauthconsumerkey");
    final String consSecret = req.getParameter("oauthconsumersecret");
    final String reqTokenURI = req.getParameter("oauthrequesttokenURI");
    final String accTokenURI = req.getParameter("oauthaccesstokenURI");
    final String userAuthURI = req.getParameter("oauthuserauthURI");
    final String oAuthDomain = req.getParameter("oauthdomain");
    
    //check urls validity
    //catalog url is mandatory
    if (UrlValidator.isValid(catalogURI) == false) {
      reloadPage(response, "Can't continue.\\nMessage: Invalid catalog URL!");
      return;
    }
    //OAuth domain has url format and is mandatory
    if (UrlValidator.isValid(oAuthDomain) == false) {
      reloadPage(response, "Can't continue.\\nMessage: Invalid OAuth domain URL!");
      return;
    }
    //request token url is optional
    if (OSLCUtils.isNullOrEmpty(reqTokenURI) == false && UrlValidator.isValid(reqTokenURI) == false) {
      reloadPage(response, "Can't continue.\\nMessage: Invalid request token URL!");
      return;
    }
    //access token url is optional
    if (OSLCUtils.isNullOrEmpty(accTokenURI) == false && UrlValidator.isValid(accTokenURI) == false) {
      reloadPage(response, "Can't continue.\\nMessage: Invalid access token URL!");
      return;
    }
    //user uthorization url is optional
    if (OSLCUtils.isNullOrEmpty(userAuthURI) == false && UrlValidator.isValid(userAuthURI) == false) {
      reloadPage(response, "Can't continue.\\nMessage: Invalid user authorization URL!");
      return;
    }
    
    //check & store
    AOManager mngr = AOManager.getInstance();
    boolean rsExists = mngr.exist(RootServicesEntity.class, "RSURI = ?", catalogURI);
    
    if (rsExists == true) {
      reloadPage(response, "Catalog already exists!");
      return;
    }
    
    if (title != null && catalogURI != null && consKey != null && consSecret != null) {
      mngr.createServiceProviderCatalogEntity(title, catalogURI, consSecret, consKey, 
          reqTokenURI, userAuthURI, accTokenURI, oAuthDomain);
    }
    
    reloadPage(response, "");
  }
}
