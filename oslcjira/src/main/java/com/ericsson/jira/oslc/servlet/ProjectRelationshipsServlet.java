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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.ericsson.jira.oslc.constants.JiraConstants;
import com.ericsson.jira.oslc.handlers.OAuthHandler;
import com.ericsson.jira.oslc.managers.JiraManager;
import com.ericsson.jira.oslc.managers.PermissionManager;
import com.ericsson.jira.oslc.oslcclient.Client;
import com.ericsson.jira.oslc.resources.ao.AOManager;
import com.ericsson.jira.oslc.resources.ao.RootServicesEntity;
import com.ericsson.jira.oslc.resources.ao.ServiceProvEntity;
import com.ericsson.jira.oslc.utils.ServletUtils;

/**
 * A servlet which is responsible for loading OSLC project relationship and creating new one
 *
 */
public class ProjectRelationshipsServlet extends HttpServlet {

  private static final long serialVersionUID = -5136031129517400935L;
  private static final String CURRENT_CLASS = "ProjectRelationshipsServlet";
  private static Logger logger =  LoggerFactory.getLogger(ProjectRelationshipsServlet.class);
  private final UserManager userManager;
  private final LoginUriProvider loginUriProvider;
  private final TemplateRenderer renderer;
  private final ActiveObjects ao;
    
  public ProjectRelationshipsServlet(UserManager userManager, LoginUriProvider loginUriProvider, 
      TemplateRenderer renderer, ActiveObjects ao) {
    this.userManager = userManager;
    this.loginUriProvider = loginUriProvider;
    this.renderer = renderer;
    this.ao = ao;
  }

  /**
   * Reload the list of project relationships and displays on the page
   * @param request a request
   * @param response a response
   * @param errorMessage an error message which will be displayed on the page
   * @throws IOException
   * @throws ServletException
   */
  private void reloadPage(HttpServletRequest request, HttpServletResponse response, String errorMessage) 
      throws IOException, ServletException {
    String serverId = request.getParameter("serverId");
    String currentMethod = "reloadPage";
    
    Map<String, Object> params = new HashMap<String, Object>();
    
    //Get all stored root services and put them to servers list. They
    //will be displayed in combobox on page.
    AOManager mngr = AOManager.getInstance();
    final List<RootServicesEntity> servers = mngr.all(RootServicesEntity.class);
    
    //Check for selected value from page servers combobox:
    //Combobox selection invokes page reload with additional paramater with root services entity id.
    RootServicesEntity e = null;
    int id = -1;
    if (serverId != null) {
      //load selected
      try {
        id = Integer.parseInt(serverId);
        e = ao.get(RootServicesEntity.class, id);
      }
      catch (NumberFormatException ex) {
        logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + ex.getMessage());
        ex.printStackTrace();
      }
    }
    
    //Get service providers for selected rootservices. These will be placed in multi select
    //on page.
    //Service providers are provided as title,link,title,link,...
    ArrayList<String> serviceProviders = null;
    String catalogLink = "";
    params.put("startOAuthDance", false);
    if (e != null) {
      Client client = new Client();
      
      if (e.isRootServices() == true) {
        catalogLink = client.getServicesProviderCatalogURI(e.getRootServicesURI());
      }
      else {
        //in case of catalog, its url is stored in rootservices uri field
        catalogLink = e.getRootServicesURI();
      }
      
      if (client.getLastResponseCode() == 200 || (catalogLink != null && catalogLink.isEmpty() == false)) {
        OAuthConsumer consumer = OAuthHandler.getConsumer(catalogLink);
        if (consumer != null) {
          OAuthAccessor accessor = OAuthHandler.getAccessorFromSession(
              request.getSession(), consumer.consumerKey, catalogLink);
          
          serviceProviders = client.getServiceProviders(catalogLink, accessor);
          
          if (client.getLastResponseCode() == 401) {
            //Here we assume the OAuth authentication failed, so lets start with OAuth dance
            params.put("startOAuthDance", true);
          }
        }
        else {
          //Consumer doesn't exist!
          //Note: this should not happen, if providers information about OAuth domain is correct.
          errorMessage = "Fatal error!\\nGetting consumer failed (possible incorrect OAuth domain)!";
          id = -1; //reset selection
        }
      }
      else {
        //get catalog uri failed
        errorMessage = "Getting service providers failed!\\nError " + client.getLastResponseCode()
            + "\\nMessage: " + client.getLastResponsePhrase();
        id = -1; //reset selection
      }
    }
    
    if (serviceProviders == null)
      serviceProviders = new ArrayList<String>(); //empty
    //Get all stored service providers.
    final List<ServiceProvEntity> savedProviders = mngr.all(ServiceProvEntity.class);
    
    //Render page.
    params.put("errorMsg", errorMessage);
    params.put("selectedServer", id);
    params.put("servers", servers);
    params.put("providers", serviceProviders);
    params.put("savedProviders", savedProviders);
    params.put("PrjRelationshipsURL", JiraManager.getBaseUrl() + JiraConstants.PROJECT_RELATIONSHIPS_PAGE);
    params.put("removeSPURL", JiraManager.getRestUrl() + JiraConstants.REMOVE_SERVICEPROVIDER_LINK);
    params.put("domainRefURL", catalogLink);
    params.put("oauthcallback", JiraManager.getBaseUrl() + JiraConstants.OAUTH_CALLBACK_SERVICE_URL);
    
    response.setContentType("text/html;charset=utf-8");
    renderer.render("templates/admin_project_relationships.vm", params, response.getWriter());
  }
  
  /**
   * Load the list of project relationships and displays on the page 
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

    String username = userManager.getRemoteUsername(request);
    if (username == null || !userManager.isSystemAdmin(username)) {
      ServletUtils.redirectToLogin(request, response, loginUriProvider);
      return;
    }

    reloadPage(request, response, "");
  }

  // Create new project relationship and save it to db
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
    
    ApplicationUser user = PermissionManager.getLoggedUser();
    if (user == null) {
      reloadPage(req, response, "Can't continue.\\nMessage: User not defined!");
      return;
    }
    
    if (!PermissionManager.isSystemAdmin(userManager)) {
      reloadPage(req, response, "Can't continue.\\nMessage: User not system admin!");
      return;
    }
    
    //Process request to add new provider. Providers are taken from multi select and
    //provided in format: "title link" (space is delimiter).
    String server = req.getParameter("select_server");
    if (server == null) {
      reloadPage(req, response, "Server not specified!");
      return;
    }
    
    String [] providers = req.getParameterValues("service_provider_select");
    if (providers == null) {
      reloadPage(req, response, "Providers not selected!");
      return;
    }
    if (providers.length == 0) {
      reloadPage(req, response, "Providers not selected!");
      return;
    }
    
    final int sid = Integer.parseInt(server);
    final RootServicesEntity e1 = ao.get(RootServicesEntity.class, sid);
    
    for (String provider : providers) {
      int idx = provider.lastIndexOf(' '); //last space splits title and uri
      final String title = provider.substring(0, idx);
      final String URI = provider.substring(idx+1, provider.length());
      
      AOManager mngr = AOManager.getInstance();
      boolean spExists = mngr.exist(ServiceProvEntity.class, "TITLE = ? AND URI = ?", title, URI);
            
      if (spExists == false) {
        mngr.createServiceproviderEntiry(e1.getID(), e1.getTitle(), title, URI);
      }
      else {
        reloadPage(req, response, "Service provider already registered!");
        return;
      }
    }
    
    reloadPage(req, response, "");
  }
}
