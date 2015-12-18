package com.ericsson.jira.oslc.services;

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
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;

import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.managers.DefaultIssueManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.ericsson.jira.oslc.constants.JiraConstants;
import com.ericsson.jira.oslc.exceptions.GetIssueException;
import com.ericsson.jira.oslc.exceptions.PermissionException;
import com.ericsson.jira.oslc.handlers.OAuthHandler;
import com.ericsson.jira.oslc.managers.JiraManager;
import com.ericsson.jira.oslc.managers.PermissionManager;
import com.ericsson.jira.oslc.oslcclient.Client;
import com.ericsson.jira.oslc.resources.ServiceProviderDialog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A services for OSLC links
 *
 */
@Path("/oslc")
@AnonymousAllowed
public class OSLCLinksServices extends BaseService {
    private static final String CURRENT_CLASS = "OSLCLinksServices";
    
    @Context private HttpServletRequest httpServletRequest;
    @Context private HttpServletResponse httpServletResponse;
    
    public OSLCLinksServices() {
        super();
    }
    
    private static Logger logger = LoggerFactory.getLogger(OAuthServices.class);
    
    /**
     * Removes OSLC link from a remote resource
     * @param issueId the unique id of the issue
     * @param URItoRemove URI of the link which will be removed
     * @return a response of the request
     * @throws GetIssueException
     * @throws PermissionException
     */
    @DELETE
    @AnonymousAllowed
    @Path("/links/removeFromRemoteApp")
    public Response removeOslcLinkFromRemoteApp(@QueryParam("jiraissueid") final String issueId, @QueryParam("uritoremove") final String URItoRemove) throws GetIssueException, PermissionException {
        String currentMethod = "removeOslcLinkFromRemoteApp";
        try {
            logger.debug(CURRENT_CLASS + "." + currentMethod + " Params: " + issueId + " " + URItoRemove);
            
            final IssueManager issueManager = ComponentAccessor.getIssueManager();
            long iid = -1;
            try {
                iid = Long.parseLong(issueId);
            } catch (Exception e) {
                logger.error(CURRENT_CLASS + "." + currentMethod + "Exception: " + e.getMessage());
                throw new GetIssueException("Issue not available");
            }
            
            final MutableIssue issue = issueManager.getIssueObject(iid);
            
            if (issue == null) {
                throw new GetIssueException("Issue not available");
            }
            
            ApplicationUser user = PermissionManager.getLoggedUser();
            PermissionManager.checkPermissionWithUser(user, (MutableIssue) issue, Permissions.EDIT_ISSUE);

            OAuthConsumer consumer = OAuthHandler.getConsumer(URItoRemove);
            if(consumer == null){
              throw new Exception("There is no consumer for the url: " + URItoRemove);
            }
            OAuthAccessor accessor = OAuthHandler.getAccessorFromSession(httpServletRequest.getSession(), consumer.consumerKey, URItoRemove);
            
            Client client = new Client();
            client.removeLinkFromRemoteURL(issue.getKey(), issue.getProjectId(), URItoRemove, accessor);
            
            if (client.getLastResponseCode() < 300) {
                return Response.ok().build();
            } else if (client.getLastResponseCode() == 401) {
                // Here we assume the OAuth authentication failed, so lets start
                // with OAuth dance
                // Return back 401 code to indicate that authentication failed.
                return Response.status(401).entity("").build();
            } else {
                return Response.status(client.getLastResponseCode()).entity(client.getLastResponsePhrase()).build();
            }
        } catch (Exception e) {
            logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
            return handleException(e);
        }
    }
    
    /**
     * Removes OSLC link from a JIRA issue
     * @param issueId the ID of the issue
     * @param URItoRemove the URI of the link which will be removed
     * @return
     * @throws GetIssueException
     */
    @DELETE
    @AnonymousAllowed
    @Path("/links/removeFromJira")
    public Response removeOslcLinkFromJira(@QueryParam("jiraissueid") final String issueId, @QueryParam("uritoremove") final String URItoRemove) throws GetIssueException {
        String currentMethod = "removeOslcLinkFromJira";
        try {
            logger.debug(CURRENT_CLASS + "." + currentMethod + " Params: " + issueId + " " + URItoRemove);
            Boolean removeRespond = false;
            removeRespond = JiraManager.removeOSLCLink(issueId, URItoRemove);

            if (removeRespond) {
                return Response.ok().build();
            } else {
                return Response.notModified("Removing link failed!").build();
            }
        } catch (Exception e) {
            logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
            return handleException(e);
        }
    }
    
    /**
     * Adds OSLC link to a remote resource
     * @param issueKey the key of the issue
     * @param URItoAdd the URI which will be added to the remote resource
     * @param label the label of the OSLC link
     * @return a reposne of a request
     * @throws IOException
     * @throws ServletException
     * @throws GetIssueException
     */
    @POST
    @AnonymousAllowed
    @Path("/links/addToRemoteApp")
    public Response addOslcLinkToRemoteApp(@QueryParam("jiraissuekey") final String issueKey, @QueryParam("uritoadd") final String URItoAdd, @QueryParam("label") final String label) throws IOException, ServletException, GetIssueException {
        String currentMethod = "addOslcLinkToRemoteApp";
        try {
            logger.debug(CURRENT_CLASS + "." + currentMethod + " Params: " + issueKey + " " + URItoAdd);
            
            DefaultIssueManager DIM = ComponentAccessor.getComponent(DefaultIssueManager.class);
            MutableIssue issue = DIM.getIssueByCurrentKey(issueKey);
            
            if (issue == null)
                return handleException(new GetIssueException("Issue not available"));
            
            ApplicationUser user = PermissionManager.getLoggedUser();
            PermissionManager.checkPermissionWithUser(user, issue, Permissions.EDIT_ISSUE);
            
            OAuthConsumer consumer = OAuthHandler.getConsumer(URItoAdd);
            if(consumer == null){
              throw new Exception("There is no consumer for the url: " + URItoAdd);
            }
            
            OAuthAccessor accessor = OAuthHandler.getAccessorFromSession(httpServletRequest.getSession(), consumer.consumerKey, URItoAdd);
            
            Client client = new Client();
            Project project = issue.getProjectObject();
            String projectName = "";
            if(project != null){
              projectName = project.getName();
            }
             
            client.addLinkToRemoteURL(issue.getKey(), issue.getProjectId(), projectName, URItoAdd, accessor);
            
            if (client.getLastResponseCode() < 300) {
                return Response.ok().build();
            } else if (client.getLastResponseCode() == 401) {
                // Here we assume the OAuth authentication failed, so lets start
                // with OAuth dance
                // Return back 401 code to indicate that authentication failed.
                return Response.status(401).entity("").build();
            } else {
                return Response.status(client.getLastResponseCode()).entity(client.getLastResponsePhrase()).build();
            }
        } catch (Exception e) {
            logger.error("EError: " + e);
            return handleException(e);
        }
    }
    
    /**
     * Adds OSLC link to a JIRA issue
     * @param issueKey the key of a issue
     * @param URItoAdd a URI which will be added to the issue
     * @param label a label of the link
     * @return a response of a issue
     * @throws IOException
     * @throws ServletException
     */
    @POST
    @AnonymousAllowed
    @Path("/links/addToJira")
    public Response addOslcLinkToJira(@QueryParam("jiraissuekey") final String issueKey, @QueryParam("uritoadd") final String URItoAdd, @QueryParam("label") final String label) throws IOException, ServletException {
        String currentMethod = "addOslcLinkToJira";
        try {
            logger.debug(CURRENT_CLASS + "." + currentMethod + " Params: " + issueKey + " " + URItoAdd);
            
            Long issueId = (long) (-1);
            MutableIssue mutable_issue;
            DefaultIssueManager DIM = ComponentAccessor.getComponent(DefaultIssueManager.class);
            
            mutable_issue = DIM.getIssueByCurrentKey(issueKey);
            
            if (mutable_issue != null) {
                issueId = mutable_issue.getId();
            }
            if (issueId != -1) {
                ArrayList<ArrayList<String>> linksList = new ArrayList<ArrayList<String>>();
                ArrayList<String> oneLink = new ArrayList<String>();
                oneLink.add(label);
                oneLink.add(URItoAdd);
                linksList.add(oneLink);
                Boolean addRespond = false;
                addRespond = JiraManager.addOSLCLink(issueId, linksList);
                
                if (addRespond) {
                    return Response.ok().build();
                } else {
                    return Response.notModified().build();
                }
            } else {
                return Response.notModified().build();
            }
        } catch (Exception e) {
            logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
            return handleException(e);
        }
    }
    
    /**
     * Gets the list of creation and selection dialogs
     * @param uri the uir of remote system
     * @return a reponse of a request
     * @throws IOException
     * @throws ServletException
     */
    @GET
    @AnonymousAllowed
    @Path("/links/types")
    public Response getProviderTypes(@QueryParam("uri") final String uri) throws IOException, ServletException {
        
        String currentMethod = "getProviderTypes";
        logger.debug(CURRENT_CLASS + "." + currentMethod + " Params: " + uri);
        
        // prepare GSON
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        
        OAuthConsumer consumer = OAuthHandler.getConsumer(uri);
        OAuthAccessor accessor = OAuthHandler.getAccessorFromSession(
            httpServletRequest.getSession(), consumer.consumerKey, uri);
        
        Client providerClient = new Client();
        com.ericsson.jira.oslc.resources.ServiceProvider sp = 
            providerClient.getServiceProviderDetails(uri, accessor);
        
        if (providerClient.getLastResponseCode() < 300) {
          // response OK
          
          int idx1 = sp.getSelectionDialogsCount();
          for (int i = 0; i < idx1; ++i) {
              ServiceProviderDialog spd = sp.getSelectionDialog(i);
              if (spd != null) {
                  spd.setLink(spd.getLink().replaceAll("%3F", "?") + JiraConstants.OSLC_RESPONSE_TYPE_4);
              }
          }
          
          int idx2 = sp.getCreationDialogsCount();
          for (int i = 0; i < idx2; ++i) {
              ServiceProviderDialog spd = sp.getCreationDialog(i);
              if (spd != null) {
                  spd.setLink(spd.getLink().replaceAll("%3F", "?") + JiraConstants.OSLC_RESPONSE_TYPE_4);
              }
          }
          
          if(sp.getCreationDialogs() != null){
            Collections.sort(sp.getCreationDialogs());
          }
            
          if(sp.getSelectionDialogs() != null){
            Collections.sort(sp.getSelectionDialogs());
          }  
          
          
          String JSON_ProviderTypes = "";
          
          JSON_ProviderTypes = gson.toJson(sp);
          
          return Response.ok(JSON_ProviderTypes).type(OslcMediaType.APPLICATION_JSON).build();
        }
        else if (providerClient.getLastResponseCode() == 401) {
          //Here we assume the OAuth authentication failed, so lets start with OAuth dance
          //Return back 401 code to indicate that authentication failed.
          return Response.status(401).entity("").build();
        }
        else {
            return Response.status(providerClient.getLastResponseCode())
                            .entity(providerClient.getLastResponsePhrase()).build();
        }
    }
}
