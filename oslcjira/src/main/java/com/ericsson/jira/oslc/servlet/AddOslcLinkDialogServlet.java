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
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.ericsson.jira.oslc.constants.JiraConstants;
import com.ericsson.jira.oslc.exceptions.GetIssueException;
import com.ericsson.jira.oslc.exceptions.PermissionException;
import com.ericsson.jira.oslc.managers.JiraManager;
import com.ericsson.jira.oslc.managers.PermissionManager;
import com.ericsson.jira.oslc.resources.ao.ServiceProvEntity;

/**
 * A servlet for displaying a dialog for adding a remote resource
 *
 */
public class AddOslcLinkDialogServlet extends HttpServlet {

  private static final long serialVersionUID = -8167984766476265125L;
  private final TemplateRenderer renderer;
  private final ActiveObjects ao;

  public AddOslcLinkDialogServlet(com.atlassian.jira.user.util.UserManager jiraUserManager, TemplateRenderer renderer, ActiveObjects ao) {
    this.renderer = renderer;
    this.ao = ao;
  }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String issueKey = request.getParameter("issuekey");
            
            final IssueManager issueManager = ComponentAccessor.getIssueManager();
            final MutableIssue issue = issueManager.getIssueByCurrentKey(issueKey);
            
            if (issue == null) {
                throw new GetIssueException("Issue not available");
            }
            
            ApplicationUser user = PermissionManager.getLoggedUser();
            PermissionManager.checkPermissionWithUser(user, (MutableIssue) issue, Permissions.EDIT_ISSUE);
            // Add all OSLC test providers - test purposed
            Map<String, Object> params = new HashMap<String, Object>();
            ArrayList<Map<String, String>> AppLinks = new ArrayList<Map<String, String>>();
            
            // Get all stored service providers.
            final ArrayList<ServiceProvEntity> savedProviders = new ArrayList<ServiceProvEntity>();
            ao.executeInTransaction(new TransactionCallback<Void>() {
                @Override
                public Void doInTransaction() {
                    for (ServiceProvEntity e : ao.find(ServiceProvEntity.class))
                        savedProviders.add(e);
                    return null;
                }
            });
            
            // add all read providers to array
            for (ServiceProvEntity e : savedProviders) {
                Map<String, String> map = new HashMap<String, String>();
                map.put("Label", e.getServerTitle()+": " + e.getTitle());
                map.put("ProviderLink", e.getURI());
                AppLinks.add(map);
            }
            
            params.put("OSLCproviders", AppLinks);
            params.put("issueKey", issueKey);
            params.put("baseURL", JiraManager.getBaseUrl());
            params.put("restURL", JiraManager.getRestUrl());
            params.put("addOslcLinkToRemoteAppURL", JiraConstants.ADD_OSLC_LINK_TO_REMOTE_APP);
            params.put("addOslcLinkToJiraURL", JiraConstants.ADD_OSLC_LINK_TO_JIRA);
            params.put("getOslcLinkTypes", JiraConstants.GET_OSLC_LINK_TYPES);
            params.put("oauthcallback2", JiraManager.getBaseUrl() + JiraConstants.OAUTH_CALLBACK_SERVICE_URL);
            
            response.setContentType("text/html;charset=utf-8");
            
            renderer.render("templates/AddOslcLinkDialog.vm", params, response.getWriter());
        } catch (PermissionException e) {
            response.sendError(403, "Permission denied.");
        } catch (Exception e) {
            response.sendError(500, e.getMessage());
        }
    }
}
