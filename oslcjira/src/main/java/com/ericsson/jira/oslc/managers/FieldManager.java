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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.PriorityManager;
import com.atlassian.jira.config.ResolutionManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.LabelManager;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.issue.vote.VoteManager;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;
import com.ericsson.jira.oslc.PluginConfig;
import com.ericsson.jira.oslc.exceptions.NoResourceException;
import com.ericsson.jira.oslc.exceptions.PermissionException;
import com.ericsson.jira.oslc.resources.JiraChangeRequest;
import com.ericsson.jira.oslc.resources.JiraHistoryRequest;

/**
 * A class offering the methods for operations with JIRA field
 *
 */
public class FieldManager {
  
  /**
   * It returns the name of priority according to ID
   * @param id the ID od priority
   * @return the name of priority according to ID
   */
  public static String getPriorityName(String id) {
    PriorityManager prMngr = ComponentAccessor.getComponent(PriorityManager.class);
    List<Priority> priorities = prMngr.getPriorities();
    Iterator<Priority> pit = priorities.iterator();
    while (pit.hasNext()) {
      Priority pr = pit.next();
      if (pr != null) {
        if (pr.getId().compareToIgnoreCase(id) == 0) {
          return pr.getName();
        }
      }
    }
    return null;
  }

  /**
   * It returns the status of the issue according to defined ID of status
   * @param issueId the ID of issue
   * @param issueStatusId the ID of status
   * @return the status of the issue according to defined ID of status
   */
  public static String getStatusNameForIssue(String issueId, String issueStatusId) {
    final IssueManager issueManager = ComponentAccessor.getIssueManager();
    final MutableIssue issue = issueManager.getIssueObject(issueId);

    if (issue != null) {
      WorkflowManager wMngr = ComponentAccessor.getWorkflowManager();
      JiraWorkflow workflow = wMngr.getWorkflow(issue);
      List<Status> statuses = workflow.getLinkedStatusObjects();
      for (Status st : statuses) {
        if (st.getId().compareToIgnoreCase(issueStatusId) == 0) {
          return st.getName();
        }
      }
    }

    return null;
  }

  /**
   * It returns the resolution name issue according to ID of resolution
   * @param resolutionId ID of resolution
   * @return resolution name
   */
  public static String getResolutionName(String resolutionId) {
    ResolutionManager resMngr = ComponentAccessor.getComponent(ResolutionManager.class);
    List<Resolution> resolutions = resMngr.getResolutions();
    Iterator<Resolution> rit = resolutions.iterator();
    while (rit.hasNext()) {
      Resolution res = rit.next();
      if (res != null) {
        if (res.getId().compareToIgnoreCase(resolutionId) == 0) {
          return res.getName();
        }
      }
    }
    return null;
  }

  /**
   * Get all issue types
   * @param request
   * @return all issue types
   * @throws IOException
   */
  public static String[] getIssueTypes() throws IOException {
    List<String> types = ComponentAccessor.getConstantsManager().getAllIssueTypeIds();
    return types.toArray(new String[] {});
  }
  
  /**
   * Get a list of issue types which are filtered according to Plugin configuration
   * @param request
   * @return list of filtered issue types
   * @throws Exception 
   */
  public static List<String> getFilteredIssueTypes() throws Exception {
    List<String> issueTypes = null;
    
    PluginConfig config = PluginConfig.getInstance();
    Set<Long> filteredTypes = config.getFilteredTypes();
    
    if(filteredTypes == null || filteredTypes.isEmpty()){
      issueTypes = ComponentAccessor.getConstantsManager().getAllIssueTypeIds();
    }else{
      issueTypes = new ArrayList<String>();
      for (Long id : filteredTypes) {
        IssueType issueType = ComponentAccessor.getConstantsManager().getIssueTypeObject(id.toString());
        if(issueType != null){
          issueTypes.add(issueType.getId());
        }
      }
    }

    return issueTypes;
  }
  

   


  /**
   * Get the name of issue type according to ID
   * @param id ID of issue type
   * @return the name of issue type
   * @throws IOException
   */
  public static String getIssueType(String id) throws IOException {
    Collection<IssueType> types = ComponentAccessor.getConstantsManager().getAllIssueTypeObjects();
    for (IssueType t : types)
      if (t.getId().compareTo(id) == 0) return t.getName();

    return null;
  }

  /**
   * Get History of Issue as Resource which will be sent to the client
   * @param request HttpServletRequest
   * @param issueKey the key of Issue
   * @return History of Issue as Resource which will be sent to the client
   * @throws PermissionException
   * @throws NoResourceException
   */
  public static JiraHistoryRequest getHistoryOfIssue(final HttpServletRequest request, String issueKey) throws PermissionException, NoResourceException {
    final IssueManager issueManager = ComponentAccessor.getIssueManager();
    final MutableIssue issue = issueManager.getIssueObject(issueKey);

    if (issue == null) {
      throw new NoResourceException("The issue " + issueKey + " doesn't exist.");
    }

    PermissionManager.checkPermission(request, issue, Permissions.BROWSE);

    ChangeHistoryManager hMngr = ComponentAccessor.getChangeHistoryManager();
    JiraHistoryRequest history = new JiraHistoryRequest(hMngr.getChangeHistories(issue));

    return history;
  }
  
  /**
   * Function prepares list of components  to set to issue. Requested list is compared against
   * allowed project components. Components which are not allowed are ignored.
   * @param issue JIRA issue
   * @param cList the list of project component names 
   * @return the list of project components
   */
  public static Collection<ProjectComponent> componentsToSet(final Issue issue, List<String> cList) {
    Project prj = issue.getProjectObject();
    Collection<ProjectComponent> pcc = prj.getProjectComponents();
    Collection<ProjectComponent> toSet = new ArrayList<ProjectComponent>(); 
    for (String c : cList) {
      for (ProjectComponent pc : pcc) {
        if (pc.getName().compareTo(c) == 0) {
          toSet.add(pc);
          break;
        }
      }
    }
    return toSet;
  }
  
  /**
   * Function prepares list of component ids to set to issue. Requested list is compared against
   * allowed component. The component which is not allowed is ignored.
   * @param issue JIRA issue
   * @param name the version names 
   * @return the list of versions
   */
  public static Long[] componentNamesToIds(final Project prj, String name) {
    if(name == null){
      return null;
    }
    
    ArrayList<String> list = new ArrayList<String>();
    list.add(name);
    return componentNamesToIds(prj, list);
  }
  
  /**
   * Function prepares list of component ids to set to issue. Requested list is compared against
   * allowed components. The components which are not allowed are ignored.
   * @param issue JIRA issue
   * @param list the list of component names 
   * @return the list of components
   */
  public static Long[] componentNamesToIds(final Project prj, List<String> list) {
    if(prj == null || list == null){
      return null;
    }
    
    Collection<ProjectComponent> col = prj.getProjectComponents();
    Collection<Long> ids = new ArrayList<Long>(); 
    for (String s : list) {
      for (ProjectComponent elem : col) {
        if (elem.getName().compareTo(s) == 0) {
          ids.add(elem.getId());
          break;
        }
      }
    }
    return ids.toArray(new Long[ids.size()]);
  }
  
  /**
   * Function prepares list of versions to set to issue. Requested list is compared against
   * allowed versions. Versions which are not allowed are ignored.
   * @param issue JIRA issue
   * @param list the list of version names 
   * @return the list of versions
   */
  public static Collection<Version> versionsToSet(final Issue issue, List<String> list) {
    Project prj = issue.getProjectObject();
    Collection<Version> col = prj.getVersions();
    Collection<Version> toSet = new ArrayList<Version>(); 
    for (String s : list) {
      for (Version elem : col) {
        if (elem.getName().compareTo(s) == 0) {
          toSet.add(elem);
          break;
        }
      }
    }
    return toSet;
  }
  
  /**
   * Function prepares list of version ids to set to issue. Requested list is compared against
   * allowed version. The version which is not allowed are ignored.
   * @param issue JIRA issue 
   * @param name version name 
   * @return the list of version ids
   */
  public static Long[] versionNamesToIds(final Project prj, String name) {
    if(name == null){
      return null;
    }
    
    ArrayList<String> list = new ArrayList<String>();
    list.add(name);
    return versionNamesToIds(prj, list);
  }
  
  
  
  /**
   * Function prepares list of version ids to set to issue. Requested list is compared against
   * allowed version ids. The versions which are not allowed are ignored.
   * @param issue JIRA issue
   * @param list the list of version names 
   * @return the list of versions
   */
  public static Long[] versionNamesToIds(final Project prj, List<String> list) {
    if(prj == null || list == null){
      return null;
    }
    
    Collection<Version> col = prj.getVersions();
    Collection<Long> ids = new ArrayList<Long>(); 
    for (String s : list) {
      for (Version elem : col) {
        if (elem.getName().compareTo(s) == 0) {
          ids.add(elem.getId());
          break;
        }
      }
    }
    
    return ids.toArray(new Long[ids.size()]);
  }
  
  /**
   * It updates labels from JiraChangeRequest
   * @param issue the issue which will be updated
   * @param jcr JIRA Change request
   * @param user the updated will be done by the user
   */
  public static void updateLabels(MutableIssue issue, JiraChangeRequest jcr, User user) {
    Set<String> names = jcr.getLabels();
    LabelManager lMngr = ComponentAccessor.getComponent(LabelManager.class);
    lMngr.setLabels(user, issue.getId(), names, false, false);
  }
  
  /**
   * It updates voters from JiraChangeRequest
   * @param issue the issue which will be updated
   * @param jcr JIRA Change request
   * @param user the updated will be done by the user
   */
  public static void updateVoters(MutableIssue issue, JiraChangeRequest jcr, User user) {
    List<String> names = jcr.getVoters();
    
    List<ApplicationUser> newVoters = new ArrayList<ApplicationUser>();
    UserManager um = ComponentAccessor.getUserManager();
    for (String name : names) {
      ApplicationUser u = um.getUserByName(name);
      if (u != null) {
        newVoters.add(u);
      }
    }
    
    VoteManager vMngr = ComponentAccessor.getVoteManager();
    List<ApplicationUser> voters = vMngr.getVotersFor(issue, ComponentAccessor.getJiraAuthenticationContext().getLocale());
    
    //remove existing voters, who aren't in incoming list
    for (ApplicationUser voter : voters) { 
      if (newVoters.contains(voter) == false) {
        vMngr.removeVote(voter, issue);
      }
    }
    
    //add voters from incoming list
    for (ApplicationUser voter : newVoters) {
      vMngr.addVote(voter, issue);
    }
  }
  
  /**
   * It updates watchers from JiraChangeRequest
   * @param issue the issue which will be updated
   * @param jcr JIRA Change request
   * @param user the updated will be done by the user
   */
  public static void updateWatchers(MutableIssue issue, JiraChangeRequest jcr, User user) {
    List<String> names = jcr.getWatchers();
    
    List<ApplicationUser> newWatchers = new ArrayList<ApplicationUser>();
    UserManager um = ComponentAccessor.getUserManager();
    for (String name : names) {
      ApplicationUser u = um.getUserByName(name);
      if (u != null) {
        newWatchers.add(u);
      }
    }
    
    WatcherManager watcherMngr = ComponentAccessor.getWatcherManager();
    List<ApplicationUser> wList = watcherMngr.getWatchers(issue, ComponentAccessor.getJiraAuthenticationContext().getLocale());
    
    //remove existing watchers, who aren't in incoming list
    for (ApplicationUser u : wList) { 
      if (newWatchers.contains(u) == false) {
        watcherMngr.stopWatching(u, issue);
      }
    }
    
    //add watchers from incoming list
    for (ApplicationUser u : newWatchers) {
      watcherMngr.startWatching(u, issue);
    }
  }
}
