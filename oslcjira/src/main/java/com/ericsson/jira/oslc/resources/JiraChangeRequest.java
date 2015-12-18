/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *  
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *
 *	   Sam Padgett	       - initial API and implementation
 *     Michael Fiedler     - adapted for OSLC4J
 *     
 *******************************************************************************/
package com.ericsson.jira.oslc.resources;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.lyo.oslc4j.core.annotation.OslcDescription;
import org.eclipse.lyo.oslc4j.core.annotation.OslcName;
import org.eclipse.lyo.oslc4j.core.annotation.OslcNamespace;
import org.eclipse.lyo.oslc4j.core.annotation.OslcOccurs;
import org.eclipse.lyo.oslc4j.core.annotation.OslcPropertyDefinition;
import org.eclipse.lyo.oslc4j.core.annotation.OslcReadOnly;
import org.eclipse.lyo.oslc4j.core.annotation.OslcResourceShape;
import org.eclipse.lyo.oslc4j.core.annotation.OslcTitle;
import org.eclipse.lyo.oslc4j.core.model.Link;
import org.eclipse.lyo.oslc4j.core.model.Occurs;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;

import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.issue.link.RemoteIssueLinkManager;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.issue.vote.VoteManager;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.issue.worklog.WorklogManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.user.ApplicationUser;
import com.ericsson.eif.leansync.mapping.data.ActionType;
import com.ericsson.eif.leansync.mapping.data.SyncConfiguration;
import com.ericsson.jira.oslc.Constants;
import com.ericsson.jira.oslc.constants.JiraConstants;
import com.ericsson.jira.oslc.managers.FieldManager;
import com.ericsson.jira.oslc.managers.JiraManager;
import com.ericsson.jira.oslc.sync.InboundSyncUtils;
import com.ericsson.jira.oslc.utils.AppLink;
import com.ericsson.jira.oslc.utils.AppLinksRepository;
import com.ericsson.jira.oslc.utils.JiraIssueInputParameters;
import com.ericsson.jira.oslc.utils.OSLCUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * It represents OSLC JIRA Change Request. It serves as data class which is sending between OSLC system  
 * It extended attributes beyond OSLC base ChangeRequest
 */
@OslcNamespace(Constants.CHANGE_MANAGEMENT_NAMESPACE)
@OslcName(Constants.CHANGE_REQUEST) 
@OslcResourceShape(title = "Change Request Resource Shape", describes = Constants.TYPE_CHANGE_REQUEST)
public final class JiraChangeRequest extends ChangeRequest {
  private String project = null;
  private List<String> components = new ArrayList<String>();
  private List<String> affects_versions = new ArrayList<String>();
  private List<String> fix_versions = new ArrayList<String>();
  private JiraIssuePriority priority = null;
  private JiraIssueResolution resolution = null;
  private String environment = null;
  private String reporter = null;
  private String assignee = null;
  private String creator = null;
  private Long projectId = null;
  private Date resolutionDate = null;
  private JiraIssueType issueType = null;
  private String dueDate = null;
  private Long originalEstimate;
  private Long remainingEstimate;
  private Long loggedHours;
  private Set<String> labels = new HashSet<String>();
  private List<URI> subTasks = new ArrayList<URI>();
  private List<JiraIssueComment> comments = new ArrayList<JiraIssueComment>();
  private List<JiraIssueWorklog> worklogs = new ArrayList<JiraIssueWorklog>();
  private List<String> voters = new ArrayList<String>();
  private List<String> watchers = new ArrayList<String>();
  private JiraIssueHistory history = null;
  private List<JiraIssueInternalLink> insideLinks = new ArrayList<JiraIssueInternalLink>();
  private List<JiraIssueWebLink> outsideLinks = new ArrayList<JiraIssueWebLink>();
  private JiraIssueStatus jiraStatus;
  private List<JiraIssueCustomField> customFields = new ArrayList<JiraIssueCustomField>();
  
  public JiraChangeRequest() throws URISyntaxException {
    super();
  }
  
  public JiraChangeRequest(URI about) throws URISyntaxException {
    super(about);
  }
  
  // issue type
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "issueType")
  @OslcName("issueType")
  @OslcTitle("Issue Type")
  public JiraIssueType getIssueType() {
    return issueType;
  }
  
  public void setIssueType(JiraIssueType issueType) {
    this.issueType = issueType;
  }
  
  //comments
  @OslcDescription("The Jira product definition for this change request.")
  @OslcOccurs(Occurs.ZeroOrMany)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "component")
  @OslcName("component")
  @OslcReadOnly(false)
  @OslcTitle("Jira Issue Components")
  public List<String> getComponents() {
    return this.components;
  }
  
  public void setComponents(List<String> components) {
    this.components = components;
  }
    
  @OslcDescription("The Jira affects version for this change request.")
  @OslcOccurs(Occurs.ZeroOrMany)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "affectsVersion")
  @OslcName("affectsVersion")
  @OslcReadOnly(false)
  @OslcTitle("AffectsVersion")
  public List<String> getAffectsVersions() {
    return affects_versions;
  }
  
  public void setAffectsVersions(List<String> versions) {
    this.affects_versions = versions;
  }
  
  //fix version
  @OslcDescription("The Jira fix version for this change request.")
  @OslcOccurs(Occurs.ZeroOrMany)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "fixVersion")
  @OslcName("fixVersion")
  @OslcReadOnly(false)
  @OslcTitle("FixVersion")
  public List<String> getFixVersions() {
    return fix_versions;
  }
  
  public void setFixVersions(List<String> versions) {
    this.fix_versions = versions;
  }
  
  //priority
  @OslcDescription("The Jira priority for this change request.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "issuePriority")
  @OslcName("issuePriority")
  @OslcTitle("Jira priority")
  public JiraIssuePriority getPriority() {
    return priority;
  }
  
  public void setPriority(JiraIssuePriority priority) {
    this.priority = priority;
  }
  
  //resolution
  @OslcDescription("The Jira resolution for this change request.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "resolution")
  @OslcTitle("Resolution")
  public JiraIssueResolution getResolution() {
    return resolution;
  }
  
  public void setResolution(JiraIssueResolution resolution) {
    this.resolution = resolution;
  }
  
  //reporter
  @OslcDescription("The Jira reporter for this change request.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "reporter")
  @OslcTitle("Reporter")
  public String getReporter() {
    return reporter;
  }
  
  public void setReporter(String reporter) {
    this.reporter = reporter;
  }
  
  //assignee
  @OslcDescription("The Jira assignee for this change request.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "assignee")
  @OslcTitle("Assignee")
  public String getAssignee() {
    return assignee;
  }
  
  public void setAssignee(String assignee) {
    this.assignee = assignee;
  }
  
  //creator
  @OslcDescription("The Jira creator for this change request.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcReadOnly
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "creator")
  @OslcTitle("Creator")
  public String getCreator() {
    return creator;
  }
  
  public void setCreator(String creator) {
    this.creator = creator;
  }
  
  //project
  @OslcDescription("The Jira project for this change request.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcReadOnly
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "project")
  @OslcTitle("Project")
  public String getProject() {
    return project;
  }
  
  public void setProject(String project) {
    this.project = project;
  }
  
  //environment
  @OslcDescription("The Jira environment for this change request.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcReadOnly
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "environment")
  @OslcTitle("Environment")
  public String getEnvironment() {
    return environment;
  }
  
  public void setEnvironment(String environment) {
    this.environment = environment;
  }
  
  //project id
  @OslcDescription("The Jira project ID for this change request.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcReadOnly
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "projectId")
  @OslcTitle("Project ID")
  public Long getProjectId() {
    return projectId;
  }
  
  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }
  
  //resolution date
  @OslcDescription("Date when the issue has been resolved")
  @OslcPropertyDefinition(OslcConstants.DCTERMS_NAMESPACE + "resolutionDate")
  @OslcReadOnly
  @OslcTitle("Resolution date")
  public Date getResolutionDate() {
    return resolutionDate;
  }
  
  public void setResolutionDate(Date resolutionDate) {
    this.resolutionDate = resolutionDate;
  }
  
  //due date
  @OslcDescription("Date when the issue should be resolved")
  @OslcPropertyDefinition(OslcConstants.DCTERMS_NAMESPACE + "dueDate")
  @OslcReadOnly
  @OslcTitle("Due date")
  public String getDueDate() {
    return dueDate;
  }
  
  public void setDueDate(String dueDate) {
    this.dueDate = dueDate;
  }
  
  //original estimate
  @OslcDescription("The Jira original estimate for this change request.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcReadOnly
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "originalEstimate")
  @OslcTitle("Original estimate")
  public Long getOriginalEstimate() {
    return this.originalEstimate;
  }
  
  public void setOriginalEstimate(Long value) {
    this.originalEstimate = value;
  }
  
  //remaining estimate
  @OslcDescription("The Jira original estimate for this change request.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcReadOnly(false)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "remainingEstimate")
  @OslcTitle("Remaining estimate")
  public Long getRemainingEstimate() {
    return this.remainingEstimate;
  }
  
  public void setRemainingEstimate(Long value) {
    this.remainingEstimate = value;
  }
  
  //logged time
  @OslcDescription("The Jira time spent for this change request.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcReadOnly
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "timeSpent")
  @OslcTitle("Time spent")
  public Long getTimeSpent() {
    return this.loggedHours;
  }
  
  public void setTimeSpent(Long value) {
    this.loggedHours = value;
  }
  
  //labels
  @OslcDescription("The Jira label for this change request.")
  @OslcOccurs(Occurs.ZeroOrMany)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "label")
  @OslcName("label")
  @OslcTitle("Label")
  public Set<String> getLabels() {
    return this.labels;
  }
 
  public void setLabels(Set<String> labels) {
    this.labels = labels;
  }
  
  //sub-tasks (just links to sub-tasks)
  @OslcDescription("The Jira sub-task for this change request.")
  @OslcOccurs(Occurs.ZeroOrMany)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "subTask")
  @OslcName("subTask")
  @OslcTitle("Sub-Task")
  public List<URI> getSubTasks() {
    return this.subTasks;
  }
 
  public void setSubTasks(List<URI> subtask) {
    this.subTasks = subtask;
  }
  
  //comments
  @OslcDescription("The Jira comment for this change request.")
  @OslcOccurs(Occurs.ZeroOrMany)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "issueComment")
  @OslcName("issueComment")
  @OslcTitle("Comment")
  public List<JiraIssueComment> getIssueComments() {
    return this.comments;
  }
 
  public void setIssueComments(List<JiraIssueComment> comments) {
    this.comments = comments;
  }
  
  //worklogs
  @OslcDescription("The Jira worklog for this change request.")
  @OslcOccurs(Occurs.ZeroOrMany)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "issueWorklog")
  @OslcName("issueWorklog")
  @OslcTitle("Worklog")
  public List<JiraIssueWorklog> getIssueWorklogs() {
    return this.worklogs;
  }
 
  public void setIssueWorklogs(List<JiraIssueWorklog> worklogs) {
    this.worklogs = worklogs;
  }
  
  //voting
  @OslcDescription("The voters for this change request.")
  @OslcOccurs(Occurs.ZeroOrMany)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "voter")
  @OslcName("voter")
  @OslcTitle("Voter")
  public List<String> getVoters() {
    return this.voters;
  }
  
  public void setVoters(List<String> voters) {
    this.voters = voters;
  }
  
  //watchers
  @OslcDescription("The watchers for this change request.")
  @OslcOccurs(Occurs.ZeroOrMany)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "watcher")
  @OslcName("watcher")
  @OslcTitle("Watcher")
  public List<String> getWatchers() {
    return this.watchers;
  }
  
  public void setWatchers(List<String> watchers) {
    this.watchers = watchers;
  }
  
  @OslcDescription("The Jira history item for this change request.")
  @OslcOccurs(Occurs.ZeroOrMany)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "history")
  @OslcName("history")
  @OslcTitle("Issue history")
  public JiraIssueHistory getIssueHistory() {
    return this.history;
  }
 
  public void setIssueHistory(JiraIssueHistory history) {
    this.history = history;
  }
  
  //jira classin links (inside, outside)
  @OslcDescription("Link to another issue inside Jira.")
  @OslcOccurs(Occurs.ZeroOrMany)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "jiraInsideLink")
  @OslcName("jiraInsideLink")
  @OslcTitle("Jira inside link")
  public List<JiraIssueInternalLink> getJiraInsideLinks() {
    return this.insideLinks;
  }
 
  public void setJiraInsideLinks(List<JiraIssueInternalLink> links) {
    this.insideLinks = links;
  }
  
  @OslcDescription("Web link outside of Jira.")
  @OslcOccurs(Occurs.ZeroOrMany)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "jiraOutsideLink")
  @OslcName("jiraOutsideLink")
  @OslcTitle("Jira outside link")
  public List<JiraIssueWebLink> getJiraOutsideLinks() {
    return this.outsideLinks;
  }
 
  public void setJiraOutsideLinks(List<JiraIssueWebLink> links) {
    this.outsideLinks = links;
  }
  
  //status
  @OslcDescription("The Jira status for this change request.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "issueStatus")
  @OslcName("issueStatus")
  @OslcTitle("Jira issue status")
  public JiraIssueStatus getIssueStatus() {
    return this.jiraStatus;
  }
  
  public void setIssueStatus(JiraIssueStatus status) {
    this.jiraStatus = status;
  }
  
  //custom fields
  @OslcDescription("The Jira custom field for this change request.")
  @OslcOccurs(Occurs.ZeroOrMany)
  @OslcPropertyDefinition(Constants.JIRA_TYPE_CUSTOM_FIELD)
  @OslcName("customField")
  @OslcTitle("Custom field")
  public List<JiraIssueCustomField> getCustomFields() {
    return this.customFields;
  }
 
  public void setCustomFields(List<JiraIssueCustomField> customFields) {
    this.customFields = customFields;
  }
  
  /**
   * Converts a {@link Issue} to an OSLC-CM JiraChangeRequest.
   * 
   * @param Issue
   * @return the ChangeRequest to be serialized
   * @throws URISyntaxException
   *             on errors setting the bug URI
   * @throws UnsupportedEncodingException
   */
  public static JiraChangeRequest fromJiraIssue(MutableIssue issue) throws URISyntaxException {
    JiraChangeRequest jcr = new JiraChangeRequest();
    jcr.setIdentifier(issue.getKey());
    
    jcr.setTitle(issue.getSummary());
    jcr.setShortTitle(issue.getKey());
    jcr.setDescription(issue.getDescription());
    
    //status
    Status status = issue.getStatusObject();
    if (status != null){
      JiraIssueStatus jStatus = new JiraIssueStatus();
      jStatus.setAbout(
          new URI(
              JiraManager.getRestUrl() + JiraConstants.ISSUE_STATUS_PATH + issue.getKey() 
              + "/" + status.getId()));
      jcr.setIssueStatus(jStatus);
    }
    
    //issue type
    IssueType issueType = issue.getIssueTypeObject();
    if(issueType != null){
      String id = issueType.getId();
      JiraIssueType jiraIssueType = new JiraIssueType();
      jiraIssueType.setAbout(new URI(JiraManager.getRestUrl() + JiraConstants.ISSUE_TYPE_PATH + id));
      JiraIssueType[] issueTypes = new JiraIssueType[1];
      issueTypes[0] = jiraIssueType;
      jcr.setIssueType(issueTypes[0]);
    }
    
    //created date
    jcr.setCreated(issue.getCreated());
    
    //modified date
    jcr.setModified(issue.getUpdated());

    //resolution date
    jcr.setResolutionDate(issue.getResolutionDate());
    
    //resolution
    Resolution resolution = issue.getResolutionObject();
    if (resolution == null) {
      jcr.setResolution(null);
    }
    else {
      JiraIssueResolution jir = new JiraIssueResolution();
      jir.setAbout(new URI(JiraManager.getRestUrl() + JiraConstants.ISSUE_RESOLUTION_PATH + resolution.getId()));
      jcr.setResolution(jir);
    }
    
    //reporter
    jcr.setReporter(issue.getReporterId());
    
    //assignee
    jcr.setAssignee(issue.getAssigneeId());
    
    //creator
    jcr.setCreator(issue.getCreatorId());
    
    //project
    Project project = issue.getProjectObject();
    if(project != null){
      String projectKey = project.getKey();
      jcr.setProject(projectKey);
      jcr.setProjectId(project.getId());
    }
    
    //environment
    jcr.setEnvironment(issue.getEnvironment());
    
    //external (OSLC) links -> oslc_cm:relatedChangeRequest
    //Note: field for related change requests is in base class ChangeRequest
    CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
    CustomField customField = customFieldManager.getCustomFieldObjectByName(JiraConstants.OSLC_CUSTOM_FIELD_NAME);
    if (customField != null) {
      String value = (String) customField.getValue(issue);
      Link[] links = OSLCUtils.convertToLinks(value);
      jcr.setRelatedChangeRequests(links);
    }
    
    //priority
    Priority priority = issue.getPriorityObject();
    if (priority != null) {
      JiraIssuePriority jiraIssuePriority = new JiraIssuePriority();
      jiraIssuePriority.setAbout(
        new URI(JiraManager.getRestUrl() + JiraConstants.ISSUE_PRIORITY_PATH + priority.getId()));
      jcr.setPriority(jiraIssuePriority);
    }
    
    //components
    Collection<ProjectComponent> cc = issue.getComponentObjects();
    List<String> components = new ArrayList<String>();
    for (ProjectComponent pc : cc) {
      components.add(pc.getName());
    }
    jcr.setComponents(components);
    
    //affects versions
    Collection<Version> avc = issue.getAffectedVersions();
    List<String> avl = new ArrayList<String>();
    for (Version v : avc) {
      avl.add(v.getName());
    }
    jcr.setAffectsVersions(avl);
    
    //fix versions
    Collection<Version> fvc = issue.getFixVersions();
    List<String> fvl = new ArrayList<String>();
    for (Version v : fvc) {
      fvl.add(v.getName());
    }
    jcr.setFixVersions(fvl);
    
    //duedate
    Date dd = issue.getDueDate();
    SimpleDateFormat formatter = new SimpleDateFormat("d/MMM/yy");
    if (dd != null) {
      jcr.setDueDate(formatter.format(dd));
    }
    
    //original estimate
    Long oe = issue.getOriginalEstimate(); //seconds !!!
    if (oe != null) {
      oe /= 60; //to minutes
    }
    jcr.setOriginalEstimate(oe);
    
    //remaining time
    Long re = issue.getEstimate(); //seconds !!!
    if (re != null) {
      re /= 60; //to minutes
    }
    jcr.setRemainingEstimate(re);
    
    //logged hours
    Long lh = issue.getTimeSpent(); //seconds !!!
    if (lh != null) {
      lh /= 60; //to minutes
    }
    jcr.setTimeSpent(lh);
    
    //labels
    Set<Label> ls = issue.getLabels();
    Set<String> labels = new HashSet<String>();
    for (Label l : ls) {
      labels.add(l.getLabel());
    }
    jcr.setLabels(labels);
    
    //sub-tasks - presented as REST links to individual issues
    Collection<Issue> subtasks = issue.getSubTaskObjects();
    List<URI> uris = new ArrayList<URI>();
    for (Issue st : subtasks) {
      uris.add(OSLCUtils.getRestUriForIssue(st));
    }
    jcr.setSubTasks(uris);
    
    //comments
    List<JiraIssueComment> jcmnts = new ArrayList<JiraIssueComment>();
    CommentManager cmntMngr = ComponentAccessor.getCommentManager();
    List<Comment> cmnts = cmntMngr.getComments(issue);
    for (Comment cmnt : cmnts) {
      jcmnts.add(new JiraIssueComment(cmnt));
    }
    jcr.setIssueComments(jcmnts);
    
    //worklog
    List<JiraIssueWorklog> jWorklogs = new ArrayList<JiraIssueWorklog>();
    WorklogManager wMngr = ComponentAccessor.getWorklogManager();
    List<Worklog> worklogs = wMngr.getByIssue(issue);
    for (Worklog w : worklogs) {
      jWorklogs.add(new JiraIssueWorklog(w));
    }
    jcr.setIssueWorklogs(jWorklogs);
    
    //history
    JiraIssueHistory history = new JiraIssueHistory();
    history.setAbout(new URI(JiraManager.getRestUrl() + issue.getProjectId() + "/changeRequests/" 
                    + issue.getKey() + "/history"));
    jcr.setIssueHistory(history);
    
    //voting
    List<String> voters = new ArrayList<String>();
    VoteManager vMngr = ComponentAccessor.getVoteManager();
    List<ApplicationUser> v = vMngr.getVotersFor(issue, ComponentAccessor.getJiraAuthenticationContext().getLocale());
    for (ApplicationUser u : v) {
      voters.add(u.getName());
    }
    jcr.setVoters(voters);
    
    //watchers
    List<String> watchers = new ArrayList<String>();
    WatcherManager watcherMngr = ComponentAccessor.getWatcherManager();
    List<ApplicationUser> wList = watcherMngr.getWatchers(issue, ComponentAccessor.getJiraAuthenticationContext().getLocale());
    for (ApplicationUser u : wList) {
      watchers.add(u.getName());
    }
    jcr.setWatchers(watchers);
    
    //classic links
    
    //inside
    List<JiraIssueInternalLink> insideLinks = new ArrayList<JiraIssueInternalLink>();
    IssueLinkManager ilMngr = ComponentAccessor.getIssueLinkManager();
    
    List<IssueLink> inwards = ilMngr.getInwardLinks(issue.getId());
    for (IssueLink link : inwards) {
      Issue srcIssue = link.getSourceObject();
      IssueLinkType type = link.getIssueLinkType();
      
      JiraIssueInternalLink jiil = new JiraIssueInternalLink(
          OSLCUtils.getRestUriForIssue(srcIssue), type.getInward(), "inward");
      
      insideLinks.add(jiil);
    }
    
    List<IssueLink> outwards = ilMngr.getOutwardLinks(issue.getId());
    for (IssueLink link : outwards) {
      Issue dstIssue = link.getDestinationObject();
      IssueLinkType type = link.getIssueLinkType();
      
      JiraIssueInternalLink jiil = new JiraIssueInternalLink(
          OSLCUtils.getRestUriForIssue(dstIssue), type.getOutward(), "outward");
      
      insideLinks.add(jiil);
    }
    
    jcr.setJiraInsideLinks(insideLinks);
    
    //outside
    List<JiraIssueWebLink> outsideLinks = new ArrayList<JiraIssueWebLink>();
    RemoteIssueLinkManager rilMngr = ComponentAccessor.getComponent(RemoteIssueLinkManager.class);
    List<RemoteIssueLink> links2 = rilMngr.getRemoteIssueLinksForIssue(issue);
    for (RemoteIssueLink link : links2) {
      JiraIssueWebLink jiol = new JiraIssueWebLink(link.getUrl(), link.getTitle());
      outsideLinks.add(jiol);
    }
    
    jcr.setJiraOutsideLinks(outsideLinks);
    
    //custom fields
    List<CustomField> cfList = customFieldManager.getCustomFieldObjects();
    List<JiraIssueCustomField> jicfList = new ArrayList<JiraIssueCustomField>();
    for (CustomField cf : cfList) {
      //skip "External Links" custom field, because content is exported as related change request(s)
      if (cf.getName().compareTo(JiraConstants.OSLC_CUSTOM_FIELD_NAME) != 0) {
        JiraIssueCustomField jicf = new JiraIssueCustomField(cf, issue);
        jicfList.add(jicf);
      }
    }
    jcr.setCustomFields(jicfList);
    
    return jcr;
  }
  
  
  /**
   * Converts a {@link Issue} to an OSLC-CM JiraChangeRequest.
   * 
   * @param Issue
   * @return the ChangeRequest to be serialized
   * @throws URISyntaxException
   *             on errors setting the bug URI
   * @throws UnsupportedEncodingException
   */

  /**
   * Converts an OSLC-CM JiraChangeRequest to a JiraIssueInputParameters
   * @param jcr JiraIssueInputParameters
   * @param issue JIRA issue
   * @param selectedProperties OSLC properties
   * @param syncType the type of synchronization
   * @param leanSyncConfiguration LeanSync configuration. If the syncType is null the configuration is ignored.
   * @param p Project
   * @return JiraIssueInputParameters
   * @throws Exception
   */
  public static JiraIssueInputParameters toIssueParameters(JiraChangeRequest jcr, MutableIssue issue, 
      Map<String, Object> selectedProperties, String syncType, SyncConfiguration leanSyncConfiguration, Project p) throws Exception {
    
    JiraIssueInputParameters jiraIssueInputParams = new JiraIssueInputParameters();
    jiraIssueInputParams.setProject(p);
    
    boolean isCreate = (issue == null);

    // assignee
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_ASIGNEE, syncType, false)) {
      jiraIssueInputParams.getIssueInputParameters().setAssigneeId(jcr.getAssignee());
    }
    
    // reporter
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_REPORTER, syncType, true&isCreate )) {
      jiraIssueInputParams.getIssueInputParameters().setReporterId(jcr.getReporter());
    }
    
    // title
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.DCTERMS_TITLE, syncType, true&isCreate)) {
      jiraIssueInputParams.getIssueInputParameters().setSummary(jcr.getTitle());
    }
    
    // description
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_DESCRIPTION, syncType, false)) {
      jiraIssueInputParams.getIssueInputParameters().setDescription(jcr.getDescription());
    }
    
    // environment
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_ENVIRONMNET, syncType, false)) {
      jiraIssueInputParams.getIssueInputParameters().setEnvironment(jcr.getEnvironment());
    }
    
    // priority
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_PRIORITY, syncType, false)){
      JiraIssuePriority pr = jcr.getPriority();
      if (pr != null && pr.getAbout() != null) {
        String prId = parseId(pr.getAbout().toString());
        jiraIssueInputParams.getIssueInputParameters().setPriorityId(prId);
      }
      else {
        jiraIssueInputParams.getIssueInputParameters().setPriorityId(null);
      }
    }
    
    // project id
    // Moving between projects could be quite complicated process, e.g.
    // due to different workflow.
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_PROJECT_ID, syncType, true&isCreate)) {
      jiraIssueInputParams.getIssueInputParameters().setProjectId(jcr.getProjectId());
    }
    
    // status
    //NOTE: changing to different status is not enough to actually change issue status.
    //      Status must be changed via proper action (e.g. 'Start progress' from 'Open' state
    //      to 'In progress' state). So here we just set the status to corresponding field
    //      in IssueInputParameters instance. If set, it will be used further 
    //      in JiraManager.updateIssue method.
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_STATUS, syncType, false)) {
      JiraIssueStatus jst = jcr.getIssueStatus();
      if (jst != null && jst.getAbout() != null) {
        String stId = parseId(jst.getAbout().toString());
        jiraIssueInputParams.getIssueInputParameters().setStatusId(stId);
      }
      else {
        jiraIssueInputParams.getIssueInputParameters().setStatusId(null);
      }
    }
    
    //Get issue type id from change request
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_ISSUE_TYPE, syncType, true&isCreate)) {
      JiraIssueType issueType = jcr.getIssueType();
      if (issueType != null && issueType.getAbout() != null) {
        String issueTypeId = parseId(issueType.getAbout().toString());
        jiraIssueInputParams.getIssueInputParameters().setIssueTypeId(issueTypeId);
      }
      else {
        jiraIssueInputParams.getIssueInputParameters().setIssueTypeId(null);
      }
    }

    //OSLC Links
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.TYPE_RELATED_CHANGE_REQUEST, syncType, true&isCreate)) {
      Link[] oldLinks = null;
      CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
      CustomField customField = customFieldManager.getCustomFieldObjectByName(JiraConstants.OSLC_CUSTOM_FIELD_NAME);
      
      if (issue != null && customField != null) {
        String value = (String) customField.getValue(issue);
        oldLinks = OSLCUtils.convertToLinks(value);
      }
      
      Link[] relatedChangeRequests = jcr.getRelatedChangeRequests();
      if (customField != null && relatedChangeRequests != null) {
        //Some system don't send dcterms:title in relatedChangeRequest. 
        //We have to use the title from the current value of relatedChangeRequest 
        if (oldLinks != null) {
          for (Link link : relatedChangeRequests) {
            if (link.getLabel() == null || !link.getLabel().trim().isEmpty()) {
              for (Link oldLink : oldLinks) {
                if (oldLink.getValue().equals(link.getValue())) {
                  link.setLabel(oldLink.getLabel());
                }
              }
            }
          }
        }
        
        List<AppLink> appLinks = OSLCUtils.convertToAppLinks(relatedChangeRequests);
        
        String parseOSLCLinks = parseOSLCLinks(appLinks);
        jiraIssueInputParams.getIssueInputParameters().addCustomFieldValue(customField.getId(), parseOSLCLinks);
      }
    }
    
    //duedate
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.DCTERMS_DUEDATE, syncType, false)) {
      String dString = jcr.getDueDate();
      jiraIssueInputParams.getIssueInputParameters().setDueDate(dString);
    }
    
    //Note: in case of creating new issue, setting components, affect and fix versions is
    // postponed, after issue is successfully created (see JiraManager.createIssue).
    
    //components
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_COMPONENT, syncType, false)) {
      if (issue != null) {
        List<String> cList = jcr.getComponents();
        Collection<ProjectComponent> toSet = FieldManager.componentsToSet(issue, cList);
        Long [] ids = new Long[toSet.size()];
        int idx = 0;
        for (ProjectComponent comp : toSet) {
          ids[idx++] = comp.getId();
        }
        jiraIssueInputParams.getIssueInputParameters().setComponentIds(ids);
      }
    }
    
    //affects version(s)
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_AFFECTS_VERSION, syncType, false)) {
      if (issue != null) {
        List<String> list = jcr.getAffectsVersions();
        Collection<Version> toSet = FieldManager.versionsToSet(issue, list);
        Long [] ids = new Long[toSet.size()];
        int idx = 0;
        for (Version comp : toSet) {
          ids[idx++] = comp.getId();
        }
        jiraIssueInputParams.getIssueInputParameters().setAffectedVersionIds(ids);
      }
    }
    
    //fix version(s)
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_FIX_VERSION, syncType, false)) {
      if (issue != null) {
        List<String> list = jcr.getFixVersions();
        Collection<Version> toSet = FieldManager.versionsToSet(issue, list);
        Long [] ids = new Long[toSet.size()];
        int idx = 0;
        for (Version comp : toSet) {
          ids[idx++] = comp.getId();
        }
        jiraIssueInputParams.getIssueInputParameters().setFixVersionIds(ids);
      }
    }
    
    //original estimate
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_ORIGINAL_ESTIMATE, syncType, false)) {
      Long value = jcr.getOriginalEstimate();
      jiraIssueInputParams.getIssueInputParameters().setOriginalEstimate(value);
    }
    
    //remaining estimate
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_REMAINING_ESTIMATE, syncType, false)) {
      Long value = jcr.getRemainingEstimate();
      jiraIssueInputParams.getIssueInputParameters().setRemainingEstimate(value);
    }
    
    //time spent
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_TIME_SPENT, syncType, false)) {
      Long value = jcr.getTimeSpent();
      jiraIssueInputParams.getIssueInputParameters().setTimeSpent(value);
    }
    
    //resolution
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_RESOLUTION, syncType, false)){
      JiraIssueResolution res = jcr.getResolution();
      if (res != null && res.getAbout() != null) {
        String resId = parseId(res.getAbout().toString());
        jiraIssueInputParams.getIssueInputParameters().setResolutionId(resId);
      }
      else {
        jiraIssueInputParams.getIssueInputParameters().setResolutionId(null);
      }
    }
    
    //custom fields
    if (OSLCUtils.allowUpdate(selectedProperties, Constants.JIRA_TYPE_CUSTOM_FIELD, syncType, false)) {
      CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
      
      for (JiraIssueCustomField jicf : jcr.getCustomFields()) {
        CustomField cf = customFieldManager.getCustomFieldObject(jicf.getId());
        if (cf != null) {
          jiraIssueInputParams.getIssueInputParameters().addCustomFieldValue(cf.getId(), jicf.getValue());
        }
      }
    }
    
    //handle LeanSync if the syncType is not null
    if(syncType != null){
      handleLeanSync(jcr, issue, jiraIssueInputParams, leanSyncConfiguration, isCreate);
    }
    
    //labels, voters, watchers
    //NOTE: listed fields can't be set to IssueInputParameters instance. 
    // Instead they are set to successfully created issue (see JiraManager.updateIssue).
    
    return jiraIssueInputParams;
  }

  /**
   * It add the values jiraIssueInputParams according to LeanSync configuration
   * @param jcr JIRA Change Request
   * @param issue JIRA issue
   * @param jiraIssueInputParams JIRA IssueInputParameters
   * @param leanSyncConfig LeanSync configuration
   * @param isCreate true - JIRA issue will be create, false - the JIRA issue will be updated
   * @throws Exception
   */
  private static void handleLeanSync(JiraChangeRequest jcr, MutableIssue issue, JiraIssueInputParameters jiraIssueInputParams, SyncConfiguration leanSyncConfig, boolean isCreate) throws Exception {
    if (leanSyncConfig != null) {
      ActionType actionType = (isCreate)?ActionType.CREATE:ActionType.UPDATE;
      InboundSyncUtils.updateInputParameters(jcr, leanSyncConfig, jiraIssueInputParams, actionType);
    }
  }

  /**
   * Extract id from last segment of uri
   * @param uri the uri containing if in the last segment of uri
   * @return id from last segment of uri
   */
  public static String parseId(String uri) {
    if (uri != null) {
      int idx = uri.lastIndexOf('/');
      if (idx != -1 && idx < uri.length() - 1) {
        return uri.substring(idx + 1);
      }
    }
    return null;
  }

  /**
   * Converts data class of Application link to GSON format
   * @param  the list of Application links
   * @return Application links in GSON format
   */
  public static String parseOSLCLinks(final List<AppLink> appLinkList) {
    // prepare GSON
    GsonBuilder gsonBuilder = new GsonBuilder();
    Gson gson = gsonBuilder.create();
    
    AppLinksRepository appLinkRepository = new AppLinksRepository();
    
    // return empty string to save empty content to customfield
    if (appLinkList.isEmpty()){
        return "";
    }
    for (AppLink appLink : appLinkList) {
      appLinkRepository.addAppLink(appLink, true);
    }
    
    String jsonAppLinks = gson.toJson(appLinkRepository);
    if(jsonAppLinks == null){
      jsonAppLinks = "";
    }
    
    return jsonAppLinks;
  }
}
