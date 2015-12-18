package com.ericsson.jira.oslc.managers;

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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.ericsson.jira.oslc.Constants;
import com.ericsson.jira.oslc.exceptions.PermissionException;
import com.ericsson.jira.oslc.resources.JiraChangeRequest;
import com.ericsson.jira.oslc.resources.JiraIssueStatus;
import com.ericsson.jira.oslc.servlet.CredentialsFilter;
import com.ericsson.jira.oslc.utils.OSLCUtils;


/**
 * A class for operations with the users and theirs permissions
 *
 */
public class PermissionManager {
  private static Logger logger = LoggerFactory.getLogger(PermissionManager.class);
  
  /**
   * Checks a permission for logged user and the issue.
   * @param request HttpServletRequest
   * @param issue JIRA issue
   * @param permission the permission which will be checked
   * @throws PermissionException if the user doesn't have the permission the exception will be thrown
   */
  public static void checkPermission(final HttpServletRequest request, final Issue issue, int permission) throws PermissionException {
    ApplicationUser appUser = null;
    if (request != null) {
      appUser = getAppUserFromRequest(request);
    }
    if (appUser == null) {
      appUser = getLoggedUser();
      if (appUser == null) {
        throw new PermissionException("User is not defined.");
      }
    }

    boolean allowed = hasPermission(appUser, issue, permission);
    if (!allowed) {
      throw new PermissionException();
    }
  }

  /**
   * Checks a permission for defined user and the issue.
   * @param appUser the user which will be checked  
   * @param issue JIRA issue
   * @param permission the permission which will be checked
   * @throws PermissionException if the user doesn't have the permission the exception will be thrown
   */
  public static void checkPermissionWithUser(ApplicationUser appUser, final Issue issue, int permission) throws PermissionException {
    boolean allowed = ComponentAccessor.getPermissionManager().hasPermission(permission, issue, appUser);
    if (!allowed) {
      throw new PermissionException();
    }
  }

  /**
   * Checks a permission for defined user and the issue. 
   * If the user doesn't have the permission the exception will be thrown with defined message {additionalMesage}
   * @param appUser the user which will be checked  
   * @param issue JIRA issue
   * @param permission the permission which will be checked
   * @param additionalMesage if the user doesn't have the permission the exception will be thrown with this message
   * @throws PermissionException if the user doesn't have the permission the exception will be thrown
   */
  public static void checkPermissionWithUser(ApplicationUser appUser, final Issue issue, int permission, String additionalMesage) throws PermissionException {
    boolean allowed = ComponentAccessor.getPermissionManager().hasPermission(permission, issue, appUser);
    if (!allowed) {
      throw new PermissionException(additionalMesage);
    }
  }

  /**
   * Checks a permission for defined user and the issue.
   * @param appUser the user that will be checked
   * @param issue JIRA issue 
   * @param permission the permission which will be checked
   * @return true - the user has a permission, otherwise false
   */
  public static boolean hasPermission(ApplicationUser appUser, final Issue issue, int permission) {
    return ComponentAccessor.getPermissionManager().hasPermission(permission, issue, appUser);
  }

  /**
   * Checks a permission for defined user and the project.
   * @param appUser the user that will be checked
   * @param project the project that will be checked
   * @param permission the permission which will be checked
   * @return true - the user has a permission on the project, otherwise false
   */
  public static boolean hasPermission(ApplicationUser appUser, final Project project, int permission) {
    return ComponentAccessor.getPermissionManager().hasPermission(permission, project, appUser);
  }

  /**
   * Checks a permission for defined user and the project.
   * @param appUser the user that will be checked
   * @param project the project that will be checked
   * @param permission the permission which will be checked
   * @return false - the user hasn't a permission on the project
   * @throws PermissionException if the user doesn't have the permission the exception will be thrown
   */
  public static boolean checkPermission(ApplicationUser appUser, final Project project, int permission) throws PermissionException {
    boolean allowed = ComponentAccessor.getPermissionManager().hasPermission(permission, project, appUser);
    if (!allowed) {
      throw new PermissionException();
    }
    return allowed;
  }

  /**
   * Check if the user has update permission on the JIRA issue
   * @param appUser the user that will be checked
   * @param issue JIRA issue
   * @param jcr JiraChangeRequest
   * @param selectedProperties OSLC properties
   * @return the user has a permission on the issue, otherwise false
   * @throws PermissionException if the user doesn't have the permission the exception will be thrown
   */
  public static boolean checkUpdatePermissions(ApplicationUser appUser, final Issue issue, final JiraChangeRequest jcr, final Map<String, Object> selectedProperties) throws PermissionException {

    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_WATCHER)) {
      checkPermissionWithUser(appUser, issue, Permissions.MANAGE_WATCHER_LIST, "User " + appUser.getName() + " has not permission to manage watchers!");
    }

    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_REPORTER)) {
      checkPermissionWithUser(appUser, issue, Permissions.MODIFY_REPORTER, "User " + appUser.getName() + " has not permission to change reporter!");
    }

    if (OSLCUtils.allowUpdate(selectedProperties, Constants.DCTERMS_DUEDATE) || OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_ORIGINAL_ESTIMATE) || OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_REMAINING_ESTIMATE)) {
      checkPermissionWithUser(appUser, issue, Permissions.SCHEDULE_ISSUE, "User " + appUser.getName() + " has not permission to schedule issue!");
    }

    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_STATUS)) {
      checkPermissionWithUser(appUser, issue, Permissions.TRANSITION_ISSUE, "User " + appUser.getName() + " has not permission to change status!");

      // user can change status, but could be restricted from closing issue
      JiraIssueStatus status = jcr.getIssueStatus();
      if (status != null) {
        String statusId = OSLCUtils.getValueFromAbout(status.getAbout());
        if (statusId.compareTo("6") == 0) { // 6 - Closed status 
          checkPermissionWithUser(appUser, issue, Permissions.CLOSE_ISSUE, "User " + appUser.getName() + " has not permission to close issue!");
        }
      }
      // NOTE:
      // Previous code will work only for standard Jira workflow, which contains
      // Closed status.
      // If there is different workflow for certain issue type, which has
      // different end state,
      // it won't work.
    }

    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_ASIGNEE)) {
      checkPermissionWithUser(appUser, issue, Permissions.ASSIGN_ISSUE, "User " + appUser.getName() + " has not permission to change asignee!");

      // user can assign someone to issue, but that person could be restricted
      // from beeing assigned
      UserManager um = ComponentAccessor.getUserManager();
      ApplicationUser asignee = um.getUserByName(jcr.getAssignee());
      // If incoming assignee is not defined (null), we assume that 'Unassigned'
      // should be set.
      // In other cases do permission checking.
      if (asignee != null) {
        checkPermissionWithUser(asignee, issue, Permissions.ASSIGNABLE_USER, "User " + jcr.getAssignee() + " can't be assigned to issue " + issue.getKey() + "!");
      }
    }

    return true;
  }
  
  /**
   * Return true if the user has specified permission
   * @param user Application use
   * @param permission specific permission
   * @return true if the user has specified permission else false
   */
  public static boolean hasPermission(ApplicationUser user, GlobalPermissionKey permission) {
    return ComponentAccessor.getGlobalPermissionManager().hasPermission(permission, user);
  }
  
  /**
   * Check if the user is admin
   * @param userManager User manager
   * @return true the user is a admin, otherwise false
   */
  public static boolean isSystemAdmin(com.atlassian.sal.api.user.UserManager userManager){
    ApplicationUser loggedUser = getLoggedUser();
    if(loggedUser != null){
      String username = getLoggedUser().getUsername();
      return userManager.isSystemAdmin(username);
    }
    
    return false;
  }
  
  /**
   * Returns a logged user
   * @param request HttpServletRequest
   * @return logged user
   */
  public static ApplicationUser getAppUserFromRequest(final HttpServletRequest request){
    String userName = getUserName(request);
    
    if(userName != null && !userName.trim().isEmpty()){
      UserManager um = ComponentAccessor.getComponent(UserManager.class);
      ApplicationUser appUser = um.getUserByName(userName);
      return appUser;
    }
    
    return null;
  }
  
  /**
   * Returns logged user
   * @return logged user
   */
  public static ApplicationUser getLoggedUser() {
    return ComponentAccessor.getJiraAuthenticationContext().getUser();
  }
  
  /**
   * Returns a user name of logged a user
   * @param request HttpServletRequest
   * @return the user name of logged the user
   */
  public static String getUserName(HttpServletRequest request) {
    Object attribute = request.getAttribute(CredentialsFilter.USERNAME_ATTRIBUTE);
    if (attribute == null) {
       logger.error("Session does not contain any credentials");
       throw new WebApplicationException(401);
    }
    return (String) attribute;
 }
}
