/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *  
 *  The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *  and the Eclipse Distribution License is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 *  
 *  Contributors:
 *  
 *     Sam Padgett         - initial API and implementation
 *     Michael Fiedler     - adapted for OSLC4J
 *******************************************************************************/

package com.ericsson.jira.oslc.managers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import org.ofbiz.core.entity.GenericEntityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.CreateValidationResult;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.issue.IssueService.IssueValidationResult;
import com.atlassian.jira.bc.issue.IssueService.TransitionValidationResult;
import com.atlassian.jira.bc.issue.IssueService.UpdateValidationResult;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueInputParametersImpl;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.managers.DefaultCustomFieldManager;
import com.atlassian.jira.issue.managers.DefaultIssueManager;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;
import com.ericsson.eif.leansync.mapping.data.SyncConfiguration;
import com.ericsson.jira.oslc.Constants;
import com.ericsson.jira.oslc.PluginConfig;
import com.ericsson.jira.oslc.constants.JiraConstants;
import com.ericsson.jira.oslc.events.IssueEventType;
import com.ericsson.jira.oslc.exceptions.GetIssueException;
import com.ericsson.jira.oslc.exceptions.IssueTransitionException;
import com.ericsson.jira.oslc.exceptions.IssueValidationException;
import com.ericsson.jira.oslc.exceptions.NoResourceException;
import com.ericsson.jira.oslc.exceptions.PermissionException;
import com.ericsson.jira.oslc.exceptions.PreconditionException;
import com.ericsson.jira.oslc.exceptions.StatusException;
import com.ericsson.jira.oslc.resources.JiraChangeRequest;
import com.ericsson.jira.oslc.services.ServiceHelper;
import com.ericsson.jira.oslc.servlet.ServiceProviderCatalogSingleton;
import com.ericsson.jira.oslc.sync.SyncUtils;
import com.ericsson.jira.oslc.utils.AppLinksRepository;
import com.ericsson.jira.oslc.utils.JiraIssueInputParameters;
import com.ericsson.jira.oslc.utils.OSLCUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opensymphony.workflow.loader.ActionDescriptor;
import com.opensymphony.workflow.loader.StepDescriptor;

/**
 * 
 * It contains the methods for operations with JIRA issue
 *
 */
public class JiraManager {
   private static final String CURRENT_CLASS = "JiraManager";
   private static Logger logger = LoggerFactory.getLogger(JiraManager.class);
   

  /**
  * Get a list of Issues for a project ID using paging
  * 
  * @param httpServletRequest HttpServletRequest
  * @param projectKeyString the key of project as String
  * @return The list of change requests
  * @throws IOException
  * @throws ServletException
  * @throws URISyntaxException
  * @throws PermissionException 
  */
  public static List<JiraChangeRequest> getIssuesByProject(final HttpServletRequest httpServletRequest,
      final String projectKeyString) throws IOException, ServletException,
      URISyntaxException, PermissionException {
    String currentMethod = "getIssuesByProject";
    String userName = PermissionManager.getUserName(httpServletRequest);
    
    UserManager um = ComponentAccessor.getComponent(UserManager.class);
    User user = ApplicationUsers.toDirectoryUser(um.getUserByName(userName));
    
    //get issue for project 
    ProjectManager projectManager = ComponentAccessor.getProjectManager();
    long prjid = Long.parseLong(projectKeyString);
    Project prj = projectManager.getProjectObj(prjid);
    
    ApplicationUser appUser = PermissionManager.getAppUser(httpServletRequest);
    PermissionManager.checkPermission(appUser, prj, Permissions.BROWSE);
    
    IssueManager issueManager = ComponentAccessor.getIssueManager();
    IssueService issueService = ComponentAccessor.getIssueService(); 
    Collection<Long> ids;
    try {
      ids = issueManager.getIssueIdsForProject(prj.getId());
    } 
    catch (GenericEntityException e1) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e1.getMessage());
      return null;
    }
    
    List<JiraChangeRequest> results = new ArrayList<JiraChangeRequest>();
    Iterator<Long> it = ids.iterator();
    while (it.hasNext())
    {
      final IssueService.IssueResult issueResult = issueService.getIssue(appUser, it.next());
      final MutableIssue issue = issueResult.getIssue();
      
      if(!PermissionManager.hasPermission(appUser, issue, Permissions.BROWSE)){
        continue;
      }
      
      JiraChangeRequest jcr = JiraChangeRequest.fromJiraIssue(issue);
      jcr.setServiceProvider(ServiceProviderCatalogSingleton.getServiceProvider(httpServletRequest, projectKeyString).getAbout());
      
      URI about;
      try {
        about = new URI(ServiceHelper.getOslcBaseUri(httpServletRequest) + "/" + projectKeyString + "/changeRequests/" + jcr.getIdentifier());
      } 
      catch (URISyntaxException e) {
        logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
        throw new WebApplicationException(e);
      }
      
      jcr.setAbout(about);
      results.add(jcr);
    }
    
    return results;
   }


   /**
    * Get a Jira Issue by id
    * 
    * @param request
    * @param IssueId
    * @return Issue
    * @throws IOException
    * @throws ServletException
    */
   public static JiraChangeRequest getIssueById(final HttpServletRequest request, final Long issueId)
         throws IOException, ServletException, URISyntaxException {
     
      final IssueManager issueManager = ComponentAccessor.getIssueManager();
      final MutableIssue issue = issueManager.getIssueObject(issueId); 

      JiraChangeRequest jcr = JiraChangeRequest.fromJiraIssue(issue);
      return jcr;
   }
   
   /**
    * Get a Jira Issue by id
    * 
    * @param request
    * @param IssueIdString
    * @return Issue
    * @throws IOException
    * @throws ServletException
   * @throws PermissionException 
   * @throws NoResourceException 
    */
   public static JiraChangeRequest getIssueById(final HttpServletRequest request, final String issueId)
         throws IOException, ServletException, URISyntaxException, PermissionException, NoResourceException {
     
      final IssueManager issueManager = ComponentAccessor.getIssueManager();
      final MutableIssue issue = issueManager.getIssueObject(issueId); 
      
      if(issue == null){
        throw new NoResourceException("The issue " + issueId + " doesn't exist.");
      }

      PermissionManager.checkPermission(request, issue, Permissions.BROWSE);
      
      JiraChangeRequest jcr = JiraChangeRequest.fromJiraIssue(issue);
      return jcr;
   }

   /**
    * Create a new JIRA issue from a JIRAChangeRequest
    * 
    * @param httpServletRequest
    * @param jcr JIRAChangeRequest
    * @param productIdString the id of product
    * @param syncType the type of synchronization
    * @return id of the new JIRA issue
    * @throws IOException
    * @throws ServletException
    */
    public static Long createIssue(HttpServletRequest httpServletRequest, final JiraChangeRequest jcr, final String projectIdString, String syncType) throws Exception {
      logger.debug("JiraManager - createIssue");
      
      String userName = PermissionManager.getUserName(httpServletRequest);
      UserManager um = ComponentAccessor.getComponent(UserManager.class);
      ApplicationUser appUser = um.getUserByName(userName);
      
      JiraAuthenticationContext authenticationContext = ComponentAccessor.getJiraAuthenticationContext();
      authenticationContext.setLoggedInUser(appUser);

      
      ProjectManager projectManager = ComponentAccessor.getComponent(ProjectManager.class);
      Project p = projectManager.getProjectObj(new Long(projectIdString)) ;
      if (p == null) {
        logger.warn("Project does not exist!");
        throw new PreconditionException("Project with id " + projectIdString + " does not exist!");
      }
      
      PermissionManager.checkPermission(appUser, p, Permissions.CREATE_ISSUE);

      jcr.setProjectId(p.getId());
      jcr.setProject(p.getKey());

      if(jcr.getReporter() == null || jcr.getReporter().isEmpty()){
        jcr.setReporter(appUser.getName());
      }

      SyncConfiguration leanSyncConfiguration = SyncUtils.getLeanSyncConfiguration(jcr, null);

      JiraIssueInputParameters jiraIssueInputParams = new JiraIssueInputParameters();
      boolean generateStatusLog = false;  
      try {
        jiraIssueInputParams = JiraChangeRequest.toIssueParameters(jcr, null, null, syncType, leanSyncConfiguration, p);
        IssueInputParameters issueInputParams = jiraIssueInputParams.getIssueInputParameters();
        
        String errorLogName = null;
        if (leanSyncConfiguration != null) {
          errorLogName = leanSyncConfiguration.getErrorLog();
        }
        
        // handle errors only for LeanSync
        if (syncType != null) { 
          
          if (!jiraIssueInputParams.getErrorSyncHandler().isLogEmpty()) {
            generateStatusLog = true;
            logger.warn("Error during parsing issue");
            JiraIssueInputParameters newJiraIssueInputParameters = new JiraIssueInputParameters();

            SyncUtils.putValueToErrorField(null, newJiraIssueInputParameters, errorLogName, jiraIssueInputParams.getErrorSyncHandler());
            SyncUtils.handleErrorState(null, newJiraIssueInputParameters, jiraIssueInputParams, leanSyncConfiguration, false);
            
            issueInputParams = newJiraIssueInputParameters.getIssueInputParameters();
          }else if (!jiraIssueInputParams.getWarnSyncHandler().isLogEmpty()) {
            SyncUtils.putValueToErrorField(null, jiraIssueInputParams, errorLogName, jiraIssueInputParams.getWarnSyncHandler());
          } else {
            // only clear  Error custom filed
            SyncUtils.putValueToErrorField(null, jiraIssueInputParams, errorLogName, null);

          }
        }

        IssueService issueService = ComponentAccessor.getComponent(IssueService.class);
        IssueValidationResult validationResult = issueService.validateCreate(appUser, issueInputParams);

        if (!validationResult.isValid()) {
          logger.warn("Create issue - parameters are not valid.");
          ErrorCollection errorCollection = validationResult.getErrorCollection();
          throwErrorException(errorCollection);
        }

        IssueResult issueResult = issueService.create(appUser, (CreateValidationResult)validationResult);
        if (!issueResult.isValid()) {
          logger.warn("Error during creating issue");
          ErrorCollection errorCollection = issueResult.getErrorCollection();
          throwErrorException(errorCollection);
        }

        MutableIssue createdIssue = issueResult.getIssue();
        if (createdIssue != null && syncType == null) {
          List<String> cList = jcr.getComponents();
          Collection<ProjectComponent> toSet1 = FieldManager.componentsToSet(createdIssue, cList);
          createdIssue.setComponent(toSet1);

          List<String> avList = jcr.getAffectsVersions();
          Collection<Version> toSet2 = FieldManager.versionsToSet(createdIssue, avList);
          createdIssue.setAffectedVersions(toSet2);

          List<String> fvList = jcr.getFixVersions();
          Collection<Version> toSet3 = FieldManager.versionsToSet(createdIssue, fvList);
          createdIssue.setFixVersions(toSet3);
        }

        return issueResult.getIssue().getId();

      } 
      catch (Exception e) {
        logger.error("Error", e);
        if (syncType == null) {
          throw e;
        }
        generateStatusLog = true;
        String exceptionMessage = "CreateIssue error: Occurs error during parsing and validating input values.";

        if (jiraIssueInputParams != null) {
          jiraIssueInputParams.getErrorSyncHandler().addMessage(e.getMessage());
          exceptionMessage += ": Message:" + jiraIssueInputParams.getErrorSyncHandler().getMessagesAsString();
        }
        logger.warn(exceptionMessage);
      }finally {
        if (generateStatusLog && syncType != null) {
          String statusMessage = "Error";
          final String errorValues = jiraIssueInputParams.getErrorSyncHandler().getMessagesAsString();
          
          if (errorValues != null) {
            statusMessage += ": " + errorValues; 
          }
          logger.warn(statusMessage);
          throw new StatusException(statusMessage);
        }
      }
      return null;
    }

    /**
     * Returns the list of projects where the users has Browse permission. The list is filtered by defined filter
     * in the plugin configuration
     * @param httpServletRequest HttpServletRequest
     * @return the list of projects where the users has Browse permission
     * @throws Exception
     */
    public static Project[] getProjects(HttpServletRequest httpServletRequest) throws Exception {
      ProjectManager projectManager = ComponentAccessor.getComponent(ProjectManager.class);
      if (projectManager == null){
        logger.debug("Project manager is null!");
        return null;
      }
      
      List<com.atlassian.jira.project.Project> filteredProjects = new ArrayList<Project>();
      ApplicationUser appUser = PermissionManager.getAppUser(httpServletRequest);
      
      PluginConfig config = PluginConfig.getInstance();
      Set<Long> filteredProjectIDs = config.getFilteredProjects();
      
      if(filteredProjectIDs == null || filteredProjectIDs.isEmpty()){
        List<com.atlassian.jira.project.Project> allProjects = projectManager.getProjectObjects();
        for (Project project : allProjects) {
          if(PermissionManager.hasPermission(appUser,project, Permissions.BROWSE)){
            filteredProjects.add(project);
          }
        }
      }else{
        for (Long projId : filteredProjectIDs) {
          Project project = projectManager.getProjectObj(projId);
          if(project != null && PermissionManager.hasPermission(appUser,project, Permissions.BROWSE)){
              filteredProjects.add(project);
          }
        }
      }
            
      if (filteredProjects.size() <= 0) {
        logger.debug("There are no projects!");
        return null;
      }

      return filteredProjects.toArray(new Project[]{});
    }
   
  /**
   * Returns the URL where the REST API is
   * @return the URL where the REST API is
   */
  public static String getRestUrl() {
    return ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL) + JiraConstants.REST_URL;
  }
  
  /**
   * Returns the base URL
   * @return the base URL
   */
  public static String getBaseUrl() {
    return ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
  }

  /**
   * Add External link 
   * @param issueId the ID of issue where the links will be added to
   * @param linksList the list of the links which will be added to the issue
   * @return true - if the links will be added to the issue successfully, otherwise false
   * @throws IOException
   * @throws ServletException
   * @throws PermissionException
   */
  public static Boolean addOSLCLink(final Long issueId, final ArrayList linksList) throws IOException, ServletException, PermissionException {
      logger.debug("JiraManager - addOSLCLink");
      DefaultIssueManager issueManager = ComponentAccessor.getComponent(DefaultIssueManager.class);
      MutableIssue issue = issueManager.getIssueObject(issueId);
    
      PermissionManager.checkPermission(null, issue, Permissions.EDIT_ISSUE);
    
      DefaultCustomFieldManager cfManager = (DefaultCustomFieldManager) ComponentAccessor.getCustomFieldManager();
      CustomField cf = cfManager.getCustomFieldObjectByName(JiraConstants.OSLC_CUSTOM_FIELD_NAME);

      if (cf == null){
        return false;
      }

      // prepare GSON
      GsonBuilder gsonBuilder = new GsonBuilder();
      Gson gson = gsonBuilder.create();

      String uri;
      String label;

      for (int iLink = 0; iLink < linksList.size(); iLink++) {
        ArrayList onelink = (ArrayList)linksList.get(iLink);
        if (onelink.size() > 1){
          label = (String) onelink.get(0);
          uri = (String) onelink.get(1);
        }
        else {
          continue;
        }
        // get all application links which are already saved in custom
        // field
        String links = (String) cf.getValue(issue);
        AppLinksRepository appLinkList = new AppLinksRepository();

        if (links != "") {
          try {
            appLinkList = gson.fromJson(links, AppLinksRepository.class);
            } catch (com.google.gson.JsonSyntaxException e) {
              logger.debug("JiraManager - addOSLCLink - " + e.getMessage());
            }
  
          if (appLinkList == null){
            appLinkList = new AppLinksRepository();
          }
  
          appLinkList.addAppLink(label, uri, true);
        }
  
        String updatedLinks = "";
        updatedLinks = gson.toJson(appLinkList);

        if (updatedLinks != null && updatedLinks != "") {
          cf.createValue(issue, updatedLinks);
        }

    } // for
      
    ApplicationUser user = PermissionManager.getLoggedUser();
    OSLCUtils.fireRestIssueEvent(issue, user, IssueEventType.ADD_EXT_LINK); 

    return true;
  }

  
  /**
   * It removes the external link from the issue
   * @param issueId the ID of issue
   * @param URItoRemove URI of the link which will be removed from the issue
   * @return true - if the links will be removed to the issue successfully, otherwise false
   * @throws GetIssueException
   * @throws PermissionException
   */
  public static Boolean removeOSLCLink(final String issueId, final String URItoRemove) throws GetIssueException, PermissionException{
    String currentMethod = "removeOSLCLink";
    logger.debug(CURRENT_CLASS + "." + currentMethod);

    
    DefaultIssueManager issueManager = ComponentAccessor.getComponent(DefaultIssueManager.class);
    Long issueIdLong = (long) -1;
    
    try {
        issueIdLong = Long.valueOf(issueId).longValue();
        } catch (Exception ex) {
            logger.error(CURRENT_CLASS + "." + currentMethod + "Exception: " + ex.getMessage());
            throw new GetIssueException("Issue not available");
        }   
    
    MutableIssue issue = issueManager.getIssueObject(issueIdLong);
        ApplicationUser user = PermissionManager.getLoggedUser();
        PermissionManager.checkPermissionWithUser(user, issue, Permissions.EDIT_ISSUE);
        
    if(issue == null) throw new GetIssueException("Issue not available");
    
    DefaultCustomFieldManager cfManager = (DefaultCustomFieldManager) ComponentAccessor.getCustomFieldManager();
    CustomField cf = cfManager.getCustomFieldObjectByName(JiraConstants.OSLC_CUSTOM_FIELD_NAME);

    if (cf == null){
      throw new GetIssueException("Custom field ("+ JiraConstants.OSLC_CUSTOM_FIELD_NAME + ") not available");
    }

    // prepare GSON
    GsonBuilder gsonBuilder = new GsonBuilder();
    Gson gson = gsonBuilder.create();

    
    // get all application links which are already saved in custom field
    String appLinks = (String) cf.getValue(issue);
    AppLinksRepository appLinkList = new AppLinksRepository();

    if (appLinks == null){
      return false;
    }else if (appLinks == ""){
      return true;
    }

    try{
        appLinkList = gson.fromJson(appLinks, AppLinksRepository.class);
      } catch (com.google.gson.JsonSyntaxException e) {
         logger.error(CURRENT_CLASS + "." + currentMethod + "Exception: " + e.getMessage());
      }

      if (appLinkList == null || URItoRemove == null){
        return false;
      }
      
      if (appLinkList.removeAppLink(URItoRemove)){
        String updatedAppLinks = "";
        if(appLinkList.GetAllAppLinks().size() == 0){
          updatedAppLinks = "";
        }
        else {
          updatedAppLinks = gson.toJson(appLinkList);
        }
        
        if (updatedAppLinks != null) {
          cf.createValue(issue, updatedAppLinks);
          return true;
        }
      }

      return false;
    
  }

  /**
   * It changes the status of the issue
   * @param issue
   * @param targetStatus the target status
   * @throws Exception
   */
  public static void changeIssueState(Issue issue, String targetStatus) throws Exception {
    Status current_status = issue.getStatusObject();
    
    if (current_status.getId() == targetStatus) {
      //same states - do nothing
      return;
    }
    
    WorkflowManager wMngr = ComponentAccessor.getWorkflowManager();
    JiraWorkflow workflow = wMngr.getWorkflow(issue);
    
    List<Status> statuses = workflow.getLinkedStatusObjects();
    Status next_status = null;
    for (Status st : statuses) {
      if (st.getId().compareToIgnoreCase(targetStatus) == 0) {
        next_status = st;
        break;
      }
    }
    
    if (next_status == null) {
      throw new IssueTransitionException("Requested status (" + targetStatus + ") does not exist!");
    }
    
    if (current_status.getId() == next_status.getId()) {
      //same states - do nothing
      return;
    }
    
    StepDescriptor step1 = workflow.getLinkedStep(current_status);
    StepDescriptor step2 = workflow.getLinkedStep(next_status);
    
    List<ActionDescriptor> actions1 = step1.getActions();
    Collection<ActionDescriptor> actions2 = workflow.getActionsWithResult(step2);
    
    int actionId = -1;
    for (ActionDescriptor d1 : actions1) {
      for (ActionDescriptor d2 : actions2) {
        if (d1.getId() == d2.getId()) {
          actionId = d1.getId();
          break;
        }
      }
    }
    
    if (actionId == -1) {
      throw new IssueTransitionException("Transition from " + current_status.getName() 
          + " to " + next_status.getName() + " is not allowed!");
    }
    
    IssueService issueService = ComponentAccessor.getComponent(IssueService.class);
    IssueService.IssueResult transResult;
    
    IssueInputParameters issueInputParameters = new IssueInputParametersImpl();
    
    ApplicationUser appUser = ComponentAccessor.getJiraAuthenticationContext().getUser();
    
    TransitionValidationResult validationResult = issueService.validateTransition(
        appUser, issue.getId(), actionId, issueInputParameters);
    if (validationResult.isValid() == false) {
      logger.warn("Change issue state - parameters are not valid.");
      ErrorCollection errorCollection = validationResult.getErrorCollection();
      throwErrorException(errorCollection);
    }
    
    transResult = issueService.transition(appUser, validationResult);
    if (transResult.isValid() == false) {
      logger.warn("Error during issue state change!");
      ErrorCollection errorCollection = transResult.getErrorCollection();
      throwErrorException(errorCollection);
    }
  }
  
  /**
   * Update existing JIRA issue from a JIRAChangeRequest
   * 
   * @param httpServletRequest
   * @param jcr JIRAChangeRequest
   * @param issueId the id of updated issue
   * @param selectedProperties OSLC slected properties
   * @param syncType the type of synchronization
   * @return id of the new JIRA issue
   * @throws IOException
   * @throws ServletException
   */
  public static void updateIssue(HttpServletRequest httpServletRequest, final JiraChangeRequest jcr, final String issueId, Map<String, Object> selectedProperties, String syncType) throws Exception {
    logger.debug("JiraManager - updateIssue");
    
    final IssueManager issueManager = ComponentAccessor.getIssueManager();
    final MutableIssue issue = issueManager.getIssueObject(issueId); 
    
    if (issue == null) {
      logger.warn("Issue does not exist!");
      throw new NoResourceException("Issue with id " + issueId + " does not exist!");
    }
    
    PermissionManager.checkPermission(httpServletRequest, issue, Permissions.EDIT_ISSUE);
    
    String userName = PermissionManager.getUserName(httpServletRequest);
    UserManager um = ComponentAccessor.getComponent(UserManager.class);
    ApplicationUser appUser = um.getUserByName(userName);
    JiraAuthenticationContext authenticationContext = ComponentAccessor.getJiraAuthenticationContext();
    authenticationContext.setLoggedInUser(appUser);    

    IssueService issueService = ComponentAccessor.getIssueService();
    
    if (selectedProperties != null && selectedProperties.isEmpty()) {
      selectedProperties = null;
    }
    
    // If user has the permission to edit issue (checked above), there still could be restrictions
    // to edit certain field (e.g. user can't change reporter). So following statement checks
    // also for some of sub-permissions.
    if (selectedProperties != null && syncType == null) {
      PermissionManager.checkUpdatePermissions(appUser, issue, jcr, selectedProperties);
    }

    // check configuration. If occurs error => not possible to set to ErrorSnapshot. Unknown "projectId"  
    SyncConfiguration leanSyncConfiguration = SyncUtils.getLeanSyncConfiguration(jcr, issue);
    if (syncType != null && (leanSyncConfiguration == null || leanSyncConfiguration.getFirstInMapping() == null)) {
      logger.debug("Issue with id " + issueId + " has wrong lean sync setting (projectId or issueTypeId!");
      throw new Exception("Issue with id " + issueId + " has wrong lean sync setting (projectId or issueTypeId!");
    }
    
    String errorLogName = null;
    JiraIssueInputParameters jiraIssueInputParams = new JiraIssueInputParameters();
    if (leanSyncConfiguration != null) {
      errorLogName = leanSyncConfiguration.getErrorLog();
    }
    
    // flag if occurs some error during parsing. true-will be updated status(error) log and thrown exception to client
    boolean generateStatusLog = false;  
    try {
      jiraIssueInputParams = JiraChangeRequest.toIssueParameters(jcr, issue, selectedProperties, syncType, leanSyncConfiguration, issue.getProjectObject()); 
      IssueInputParameters issueInputParams = jiraIssueInputParams.getIssueInputParameters(); 
      
      // handle errors only for LeanSync
      if (syncType != null) { 
        if (!jiraIssueInputParams.getErrorSyncHandler().isLogEmpty()) {
          generateStatusLog = true;
          logger.warn("Error during parsing issue");
          JiraIssueInputParameters newJiraIssueInputParameters = new JiraIssueInputParameters();

          SyncUtils.putValueToErrorField(issue, newJiraIssueInputParameters, errorLogName, jiraIssueInputParams.getErrorSyncHandler());
          SyncUtils.handleErrorState(issue, newJiraIssueInputParameters, jiraIssueInputParams, leanSyncConfiguration, false);
          
          issueInputParams = newJiraIssueInputParameters.getIssueInputParameters();
        }else if (!jiraIssueInputParams.getWarnSyncHandler().isLogEmpty()) {
          SyncUtils.putValueToErrorField(issue, jiraIssueInputParams, errorLogName, jiraIssueInputParams.getWarnSyncHandler());
        } else {
          // only clear  Error custom filed
          SyncUtils.putValueToErrorField(issue, jiraIssueInputParams, errorLogName, null);

        }
      }

      IssueValidationResult validationResult = issueService.validateUpdate(appUser, issue.getId(), issueInputParams);
      if (!validationResult.isValid()) {
        logger.warn("Update issue - parameters are not valid.");
        ErrorCollection errorCollection = validationResult.getErrorCollection();
        throwErrorException(errorCollection);
      }

      IssueService.IssueResult issueResult = issueService.update(appUser, (UpdateValidationResult)validationResult);
      if (!issueResult.isValid()) {
        logger.warn("Error during updating issue");
        ErrorCollection errorCollection = issueResult.getErrorCollection();
        throwErrorException(errorCollection);
      }

      //after successful update, check if there is also request for change state
      MutableIssue updatedIssue = issueResult.getIssue();
      if (updatedIssue != null && syncType == null) {
        String statusId = issueInputParams.getStatusId();
        if (statusId != null) {
          changeIssueState(issueResult.getIssue(), statusId);
        }

        if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_LABEL)) {
          FieldManager.updateLabels(updatedIssue, jcr, appUser);
        }

        if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_VOTER)) {
          FieldManager.updateVoters(updatedIssue, jcr);
        }

        if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_WATCHER)) {
          FieldManager.updateWatchers(updatedIssue, jcr);
        }
      }
    } catch (Exception e) {
      logger.error("Error", e);
      if (syncType == null) {
        throw e;
      }
      //continue for LeanSync
      generateStatusLog = true; // occurs error => generate snapshot and status information
      jiraIssueInputParams.getErrorSyncHandler().addMessage(e.getMessage());

      logger.warn(jiraIssueInputParams.getErrorSyncHandler().getMessagesAsString());

      SyncUtils.saveErrorStatus(issue, jiraIssueInputParams.getErrorSyncHandler(), errorLogName);
      SyncUtils.handleErrorState(issue, null, jiraIssueInputParams, leanSyncConfiguration, true);
    } finally {
      if (generateStatusLog && syncType != null) {
        String statusMessage = "Error";
        final String errorValues = jiraIssueInputParams.getErrorSyncHandler().getMessagesAsString();
        
        if (errorValues != null) {
          statusMessage += ": " + errorValues; 
        }
        logger.warn("Error", statusMessage);
        throw new StatusException(statusMessage);
      }
    }

  }
  
  /**
   * It prepares error message from Error Collection and then throws IssueValidationException
   * @param errorCollection collection of errors
   * @throws IssueValidationException if the errors is not empty then throws IssueValidationException
   */
  private static void throwErrorException(ErrorCollection errorCollection) throws IssueValidationException{
    StringBuilder sb = new StringBuilder();
    
    Map<String, String> errors = errorCollection.getErrors();
    Collection<String> errorMessages = errorCollection.getErrorMessages();

    if (errors != null && !errors.isEmpty()) {
      sb.append(errors.toString());
    }
    if (errorMessages != null && !errorMessages.isEmpty()) {
      sb.append(errorMessages.toString());
    }
    String message = sb.toString();
    logger.warn("Validation result: " + message);
    throw new IssueValidationException(message);
  }


} 