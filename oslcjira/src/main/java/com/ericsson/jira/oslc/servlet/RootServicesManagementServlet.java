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
import com.ericsson.jira.oslc.oslcclient.Client;
import com.ericsson.jira.oslc.resources.RootServices;
import com.ericsson.jira.oslc.resources.ao.AOManager;
import com.ericsson.jira.oslc.resources.ao.RootServicesEntity;
import com.ericsson.jira.oslc.utils.ServletUtils;

/**
 * A servlet which is responsible for loading rootservices and creating new one
 *
 */
public class RootServicesManagementServlet extends HttpServlet {
  
  private static final long serialVersionUID = 4082969679736956897L;
  private final UserManager userManager;
  private final LoginUriProvider loginUriProvider;
  private final TemplateRenderer renderer;

  
  public RootServicesManagementServlet(UserManager userManager, LoginUriProvider loginUriProvider, 
      TemplateRenderer renderer) {
    this.userManager = userManager;
    this.loginUriProvider = loginUriProvider;
    this.renderer = renderer;
  }

  /**
   * Reload the list of project relationships and displays on the page
   * @param request a request
   * @param response a response
   * @param errorMessage an error message which will be displayed on the page
   * @throws IOException
   * @throws ServletException
   */
  private void reloadPage(HttpServletResponse response, String errorMessage) throws IOException, ServletException {
    Map<String, Object> params = new HashMap<String, Object>();
    
    AOManager mngr = AOManager.getInstance();
    final List<RootServicesEntity> entities = mngr.all(RootServicesEntity.class);
    ListIterator<RootServicesEntity> it = entities.listIterator();
    while (it.hasNext()) {
      RootServicesEntity rse = it.next();
      if (rse.isRootServices() == false) {
        it.remove();
      }
    }
    params.put("errorMsg", errorMessage);
    params.put("entities", entities);
    params.put("RSManagementURL", JiraManager.getBaseUrl() + JiraConstants.ROOTSEERVICES_MANAGEMENTT_PAGE);
    params.put("removeRSLinkURL", JiraManager.getRestUrl() + JiraConstants.REMOVE_RS_LINK);
    
    response.setContentType("text/html;charset=utf-8");
    renderer.render("templates/admin_rootservices.vm", params, response.getWriter());
    
  }
  
  /**
   * Load the list of rootservices and displays on the page 
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

    String username = userManager.getRemoteUsername(request);
    if (username == null || !userManager.isSystemAdmin(username)) {
      ServletUtils.redirectToLogin(request, response, loginUriProvider);
      return;
    }
    
    OAuthHandler.clearRootServicesCache();
    reloadPage(response, "");
  }

  /**
   *  Create new project relationship and save it to db
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
    final String rootServicesURI = req.getParameter("rsuri").trim();
    final String oAuthSecret = req.getParameter("oauthsecret");
    
    //check url validity
    if (UrlValidator.isValid(rootServicesURI) == false) {
      reloadPage(response, "Can't continue.\\nMessage: Invalid root services URL!");
      return;
    }
    
    //get stored rs to check if its already present
    AOManager mngr = AOManager.getInstance();
    boolean rsExists = mngr.exist(RootServicesEntity.class, "RSURI = ?", rootServicesURI);
    
    if (rsExists == true) {
      reloadPage(response, "Root service already exists!");
      return;
    }
    
    if (title != null) {
      String key = "";
      
      //get root services details using its uri
      Client client = new Client();
      RootServices rs = client.getRootServicesDetails(rootServicesURI);
      if (client.getLastResponseCode() != 200) {
          reloadPage(response, "Can't get root services details.\\nError code: " + client.getLastResponseCode()+", Message: " 
              + client.getLastResponsePhrase());
          return;
      }
      
      if (rs != null) {
        //from details get uri for consumers key request
        String ckUrl = rs.getOAuthRequestConsumerKeyURL();
        //make request for consumer key
        key = client.getConsumerKey(ckUrl, title, oAuthSecret, title);
        if (key != null) {
          //store only if we have everything, including consumer key
          mngr.createRootServicesEntity(title, rootServicesURI, oAuthSecret, key);
        }
        else {
            reloadPage(response, "Can't get consumer key from external system.\\nMessage: " 
                + client.getLastResponsePhrase());
            return; 
        }
      }
    }
    
    reloadPage(response, "");
  }
}
