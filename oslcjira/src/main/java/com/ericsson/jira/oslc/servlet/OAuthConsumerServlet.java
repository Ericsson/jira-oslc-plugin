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
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.lyo.server.oauth.core.OAuthConfiguration;
import org.eclipse.lyo.server.oauth.core.consumer.ConsumerStore;
import org.eclipse.lyo.server.oauth.core.consumer.ConsumerStoreException;
import org.eclipse.lyo.server.oauth.core.consumer.LyoOAuthConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.ericsson.jira.oslc.constants.JiraConstants;
import com.ericsson.jira.oslc.handlers.OAuthHandler;
import com.ericsson.jira.oslc.managers.JiraManager;
import com.ericsson.jira.oslc.resources.data.OAuthConsumerView;
import com.ericsson.jira.oslc.utils.ServletUtils;

/**
 * A servlet which is responsible for loadinf and saving OAuth consumers
 *
 */
public class OAuthConsumerServlet extends HttpServlet {
  private static final String CURRENT_CLASS = "OAuthConsumerServlet";
  private static Logger logger =  LoggerFactory.getLogger(OAuthConsumerServlet.class);
  private static final long serialVersionUID = 5673676040162113184L;
  private final UserManager userManager;
  private final LoginUriProvider loginUriProvider;
  private final TemplateRenderer renderer;

  public OAuthConsumerServlet(UserManager userManager, LoginUriProvider loginUriProvider, TemplateRenderer renderer) {
    this.userManager = userManager;
    this.loginUriProvider = loginUriProvider;
    this.renderer = renderer;
  }

  /**
   * Get the list of OAuth consumers and show them in the palge
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    loadPage(request, response);
  }
  
  /**
   * Load the page from adding and removing OAuth cosumers
   * @param request a request
   * @param response a response 
   * @throws IOException
   * @throws ServletException
   */
  private void loadPage(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException{
    String currentMethod = "loadPage";
    
    String username = userManager.getRemoteUsername(request);
    if (username == null || !userManager.isSystemAdmin(username)) {
      ServletUtils.redirectToLogin(request, response, loginUriProvider);
      return;
    }

    Map<String, Object> params = new HashMap<String, Object>();
    OAuthConfiguration config = OAuthConfiguration.getInstance();
    try {
      OAuthHandler.loadOAuthConsumers(config);
    } catch (ConsumerStoreException e) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      throw new ServletException(e.getMessage());
    } catch (ClassNotFoundException e) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      throw new ServletException(e.getMessage());
    } catch (SQLException e) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      throw new ServletException(e.getMessage());
    }
    ConsumerStore consumerStore = config.getConsumerStore();

    OAuthConsumerView view;
    try {
      view = new OAuthConsumerView(consumerStore.getAllConsumers());
    } catch (ConsumerStoreException e) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      throw new ServletException(e.getMessage());
    }
    params.put("consumers", view);
    params.put("restURL", JiraManager.getRestUrl());
    params.put("activateConsumerURL", JiraManager.getRestUrl()+JiraConstants.ACTIVATE_CONSUMER_URL);
    params.put("removeConsumerURL", JiraManager.getRestUrl()+JiraConstants.REMOVE_CONSUMER_URL);
    params.put("OAuthConsumerPageURL", JiraManager.getBaseUrl()+JiraConstants.OAUTH_CONSUMER_PAGE_URL);

    response.setContentType("text/html;charset=utf-8");
    renderer.render("templates/oAuthConsumers.vm", params, response.getWriter());
  }


  /**
   * Save a consumer to db
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String currentMethod = "doPost";
    String username = userManager.getRemoteUsername(request);
    if (username == null || !userManager.isSystemAdmin(username)) {
      ServletUtils.redirectToLogin(request, response, loginUriProvider);
      return;
    }

    // get values from form
    final String consumerName= request.getParameter("consumerName").trim();
    final String consumerKey = request.getParameter("consumerKey").trim();
    final String cosumerSecret = request.getParameter("consumerSecret").trim();

    LyoOAuthConsumer consumer = new LyoOAuthConsumer(consumerKey, cosumerSecret);
    consumer.setName(consumerName);
    consumer.setProvisional(false);

    OAuthConfiguration config = OAuthConfiguration.getInstance();
    try {
      LyoOAuthConsumer consumerByKey = OAuthHandler.getConsumerByKey(consumerKey);
      if(consumerByKey != null){
        throw new ServletException("The consumer with same key already exists.");
      }

      ConsumerStore consumerStore = OAuthHandler.addConsumer(consumer);
      if (consumerStore != null) {
        config.setConsumerStore(consumerStore);
      }
      loadPage(request, response);
    } catch (ConsumerStoreException e) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      throw new ServletException(e.getMessage());
    } catch (ClassNotFoundException e) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      throw new ServletException(e.getMessage());
    } catch (SQLException e) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      throw new ServletException(e.getMessage());
    }

  }
}
