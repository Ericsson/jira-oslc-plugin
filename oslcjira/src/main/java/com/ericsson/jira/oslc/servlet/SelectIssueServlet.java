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

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.VelocityParamFactory;
import com.atlassian.templaterenderer.RenderingException;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.ericsson.jira.oslc.exceptions.PermissionException;
import com.ericsson.jira.oslc.managers.PermissionManager;
import com.ericsson.jira.oslc.utils.OSLCUtils;

/**
 * Servlet class, which is responsible for rendering and handling "Select issue dialog"
 * for OSLC delegation UI. 
 *
 */
public class SelectIssueServlet extends HttpServlet {
  private static final long serialVersionUID = 4616381257205165765L;
  private static final Logger log = LoggerFactory.getLogger(SelectIssueServlet.class);
  private final TemplateRenderer templateRenderer;

  public SelectIssueServlet(TemplateRenderer tr) {
    this.templateRenderer = tr;
  }

  private void sendError(HttpServletResponse resp, String msg) {
    Map<String, Object> context = new HashMap<String, Object>();
    context.put("errorMessage", msg);
    try {
      templateRenderer.render("templates/error_page.vm", context, resp.getWriter());
    } 
    catch (RenderingException e) {
      log.error("CreateIssueServlet.sendError: " + e.getMessage());
      e.printStackTrace();
    } 
    catch (IOException e) {
      log.error("CreateIssueServlet.sendError: " + e.getMessage());
      e.printStackTrace();
    }
  }
  
  /**
   * Load the issue and display them in the dialog
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    long pid = Long.parseLong(req.getParameter("projectId"));
    String issueType = req.getParameter("issueType");
    
    // get project object based on incoming project id (project id is part of
    // request)
    ProjectManager pm = ComponentAccessor.getProjectManager();
    Project prj = pm.getProjectObj(pid);
    if (prj == null) {
      String msg = "Project with id " + pid + " does not exist!";
      log.error(msg);
      sendError(resp, msg);
      return;
    }
    
    ApplicationUser user = PermissionManager.getLoggedUser();
    try {
      PermissionManager.checkPermission(user, prj, Permissions.BROWSE);
    } 
    catch (PermissionException e) {
      String msg = "User " + user.getName() + " doesn't have permission to access project " + prj.getKey();
      log.error(msg);
      sendError(resp, msg);
      return;
    }
    
    VelocityParamFactory vpf = ComponentAccessor.getVelocityParamFactory();
    JiraAuthenticationContext jac = ComponentAccessor.getJiraAuthenticationContext();
    Map<String, Object> context = vpf.getDefaultVelocityParams(jac);
    
    IssueManager issueManager = ComponentAccessor.getIssueManager();
    
    DateTimeFormatterFactory dateTimeFormatterFactory = 
        ComponentAccessor.getComponent(DateTimeFormatterFactory.class);
    DateTimeFormatter formatter = dateTimeFormatterFactory.formatter().forLoggedInUser();
    
    context.put("projectId", pid);
    context.put("issueType", issueType);
    context.put("issueManager", issueManager);
    context.put("dateFormatter", formatter);
    
    templateRenderer.render("templates/select_issue.vm", context, resp.getWriter());
  }

  /**
   * After selection of the issue, the response has to be sent to remote server
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
    long pid = Long.parseLong(req.getParameter("projectId"));
    
    ProjectManager pm = ComponentAccessor.getProjectManager();
    Project prj = pm.getProjectObj(pid);
    if (prj == null) {
      String msg = "Project with id " + pid + " does not exist!";
      log.error(msg);
      sendError(response, msg);
      return;
    }
    
    ApplicationUser user = PermissionManager.getLoggedUser();
    try {
      PermissionManager.checkPermission(user, prj, Permissions.BROWSE);
    } 
    catch (PermissionException e) {
      String msg = "User " + user.getName() + " doesn't have permission to access project " + prj.getKey();
      log.error(msg);
      sendError(response, msg);
      return;
    }
    
    String selectedIssueKey = req.getParameter("issueKey");

    IssueManager issueManager = ComponentAccessor.getIssueManager();
    long selectedIssueId = issueManager.getIssueObject(selectedIssueKey).getId();

    // send new response with id,link back
    VelocityParamFactory vpf = ComponentAccessor.getVelocityParamFactory();
    JiraAuthenticationContext jac = ComponentAccessor.getJiraAuthenticationContext();
    Map<String, Object> context = vpf.getDefaultVelocityParams(jac);

    context.put("responseMessage", OSLCUtils.getOSLCResponseMessage(selectedIssueId, true));
    context.put("responseCoreMessage", OSLCUtils.getOSLCResponseCoreMessage(selectedIssueId, true));
    context.put("responseCommonMessage", OSLCUtils.getOSLCResponseCommonMessage(selectedIssueId, true));

    templateRenderer.render("templates/create_issue_response.vm", context, response.getWriter());
  }
}
