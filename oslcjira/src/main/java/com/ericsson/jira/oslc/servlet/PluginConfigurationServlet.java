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
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.ericsson.jira.oslc.PluginConfig;
import com.ericsson.jira.oslc.managers.PermissionManager;
import com.ericsson.jira.oslc.resources.ao.AOManager;
import com.ericsson.jira.oslc.utils.OSLCUtils;
import com.ericsson.jira.oslc.utils.ServletUtils;

/**
 * A servlet which is responsible for loading and saving a configuration from/to db
 * The configuration contain only simple values.
 *
 */
public class PluginConfigurationServlet extends HttpServlet {

  private static final long serialVersionUID = -6468802507370592016L;
  private final UserManager userManager;
  private final LoginUriProvider loginUriProvider;
  private final TemplateRenderer renderer;
  private static Logger logger =  LoggerFactory.getLogger(PluginConfigurationServlet.class);


  public PluginConfigurationServlet(UserManager userManager, LoginUriProvider loginUriProvider, TemplateRenderer renderer) {
    this.userManager = userManager;
    this.loginUriProvider = loginUriProvider;
    this.renderer = renderer;
  }

  /**
   * Load the configurations from db and displays them on the page
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    reloadPage(request, response, "", null, null);
  }

  /**
   * Reload the configurations from db and displays them on the page
   * @param request a request
   * @param response a response
   * @param errorMessage an error message which will be displayed on the page
   * @param filteredProjects the list of the projects - only these project will be visible from outside
   * @param filteredTypes the list of the issue types projects - only these types will be visible from outside
   * @throws IOException
   * @throws ServletException
   */
  private void reloadPage(HttpServletRequest request, HttpServletResponse response, String errorMessage, String filteredProjects, String filteredTypes) throws IOException, ServletException {

    String username = userManager.getRemoteUsername(request);
    if (username == null || !userManager.isSystemAdmin(username)) {
      ServletUtils.redirectToLogin(request, response, loginUriProvider);
      return;
    }

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("errorMsg", errorMessage);


    AOManager mngr = AOManager.getInstance();
    Map<String, String> configValues = mngr.getConfigValues();
    
    
    if (configValues != null) {
      if (filteredProjects == null) {
        filteredProjects = configValues.get(PluginConfig.FILTERED_PROJECTS);
      }
      if (filteredTypes == null) {
        filteredTypes = configValues.get(PluginConfig.FILTERED_TYPES);
      }

    }
    
    ServiceProviderCatalogSingleton.deregisterAllServiceProvider();

    params.put("filteredProjects", OSLCUtils.replaceNullForEmptyString(filteredProjects));
    params.put("filteredTypes", OSLCUtils.replaceNullForEmptyString(filteredTypes));

    response.setContentType("text/html;charset=utf-8");
    renderer.render("templates/pluginConfiguration.vm", params, response.getWriter());
  }

  /**
   * Save the configuration to db
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
    final String inputFilteredProjects = req.getParameter("filteredProjects");
    final String inputFilteredTypes = req.getParameter("filteredTypes");

    ApplicationUser user = PermissionManager.getLoggedUser();
    if (user == null) {
      reloadPage(req, response, "Can't continue.\\nMessage: User not defined!", inputFilteredProjects, inputFilteredTypes);
      return;
    }

    if (!PermissionManager.isSystemAdmin(userManager)) {
      reloadPage(req, response, "Can't continue.\\nMessage: User not system admin!", inputFilteredProjects, inputFilteredTypes);
      return;
    }

    savePluginConfig(req, response, inputFilteredProjects, inputFilteredTypes);
  }
  
  /**
   * Save the configuration to db
   * @param request a request
   * @param response a response
   * @param filteredProjects the list of the projects - only these project will be visible from outside
   * @param filteredTypes the list of the issue types projects - only these types will be visible from outside
   * @throws IOException
   * @throws ServletException
   */
  private void savePluginConfig(HttpServletRequest request, HttpServletResponse response, String filteredProjects, String filteredTypes) throws IOException, ServletException{
    PluginConfig config;
    try {
      config = PluginConfig.getInstance(false);
      config.loadConfiguration(filteredProjects, filteredTypes);
      
      Map<String, String> configValues = new HashMap<String, String>();
      configValues.put(PluginConfig.FILTERED_PROJECTS, filteredProjects);
      configValues.put(PluginConfig.FILTERED_TYPES, filteredTypes);
      
      AOManager mngr = AOManager.getInstance();
      mngr.saveConfigValues(configValues);
      reloadPage(request, response, "", null, null);
    } catch(NumberFormatException e){
      logger.error("Error", e);
      String msg = "The IDs are not in correct format.";
      reloadPage(request, response, msg, filteredProjects, filteredTypes);
    } catch (Exception e) {
      logger.error("Error", e);
      String msg = (e.getMessage() != null)? e.getMessage() : e.toString();
      reloadPage(request, response, msg, filteredProjects, filteredTypes);
    } 
  }


}
