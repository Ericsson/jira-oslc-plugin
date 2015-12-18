package com.ericsson.jira.oslc.sync;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.eclipse.lyo.oslc4j.core.model.Link;
import org.eclipse.lyo.oslc4j.provider.jena.OslcRdfXmlProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.changehistory.ChangeHistory;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
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
import com.ericsson.eif.leansync.mapping.SyncConstants;
import com.ericsson.eif.leansync.mapping.SyncHelper;
import com.ericsson.eif.leansync.mapping.data.SyncConfiguration;
import com.ericsson.eif.leansync.mapping.data.SyncField;
import com.ericsson.eif.leansync.mapping.data.SyncMapping;
import com.ericsson.eif.leansync.mapping.data.SyncTemplate;
import com.ericsson.jira.oslc.constants.JiraConstants;
import com.ericsson.jira.oslc.oslcclient.Client;
import com.ericsson.jira.oslc.provider.OslcXmlRdfProvider;
import com.ericsson.jira.oslc.resources.JiraChangeRequest;
import com.ericsson.jira.oslc.utils.ErrorSyncHandler;
import com.ericsson.jira.oslc.utils.OSLCUtils;

/**
 * The class offers the helping methods for synchronization from JIRA to the external system
 *
 */
public class OutboundSyncUtils {
  private static Logger logger = LoggerFactory.getLogger(SyncUtils.class);
  private static final String CURRENT_CLASS = "OutboundSyncUtils";
  
  private static String createOutboundSnapshot(Issue issue, SyncMapping mapping, SyncTemplate template){
    if(issue == null || mapping == null || template == null){
      return null;
    }
    
    String text = template.getTemplate();
    List<SyncField> fields = mapping.getFields();
    if(text == null || fields == null){
      return null;
    }

    for (SyncField field : fields) {
      String name = field.getName();
      if( name == null || name.isEmpty()){
        continue;
      }

      if(SyncConstants.CONFIG_FIELD_TYPE_CUSTOM.equalsIgnoreCase(field.getFieldType())){
        CustomFieldManager cfm = ComponentAccessor.getCustomFieldManager();
        CustomField customField = cfm.getCustomFieldObjectByName(name);
        text = updateOutboundField(text, template, field, SyncUtils.convertCustomFieldToText(issue, customField));
      }else if(JiraObjectMapping.SUMMARY.compare(name)){
        text = updateOutboundField(text, template, field, issue.getSummary());
      }else if(JiraObjectMapping.DESCRIPTION.compare(name)){
        text = updateOutboundField(text, template, field, issue.getDescription());
      }else if(JiraObjectMapping.ASSIGNEE_ID.compare(name)){
        text = updateOutboundField(text, template, field, issue.getAssigneeId());
      }else if(JiraObjectMapping.ISSUE_KEY.compare(name)){
        text = updateOutboundField(text, template, field, issue.getKey());
      }else if(JiraObjectMapping.ISSUE_TYPE_NAME.compare(name)){
        IssueType issueTypeObject = issue.getIssueTypeObject();
        text = updateOutboundField(text, template, field, (issueTypeObject == null)?null:issueTypeObject.getName());
      }else if(JiraObjectMapping.RESOLUTION_NAME.compare(name)){
        Resolution resolution = issue.getResolutionObject();
        text = updateOutboundField(text, template, field, (resolution == null)?"Unresolved":resolution.getName());
      }else if(JiraObjectMapping.REPORTER_ID.compare(name)){
        text = updateOutboundField(text, template, field, issue.getReporterId());
      }else if(JiraObjectMapping.CREATOR_ID.compare(name)){
        text = updateOutboundField(text, template, field, issue.getCreatorId());
      }else if(JiraObjectMapping.ENVIRONMENT.compare(name)){
        text = updateOutboundField(text, template, field, issue.getEnvironment());
      }else if(JiraObjectMapping.PROJECT_KEY.compare(name)){
        Project project = issue.getProjectObject();
        text = updateOutboundField(text, template, field, (project == null)?null:project.getKey());
      }else if(JiraObjectMapping.PROJECT_NAME.compare(name)){
        Project project = issue.getProjectObject();
        text = updateOutboundField(text, template, field, (project == null)?null:project.getName());
      }else if(JiraObjectMapping.PRIORITY_NAME.compare(name)){
        Priority priority = issue.getPriorityObject();
        text = updateOutboundField(text, template, field, (priority == null)?null:priority.getName());
      }else if(JiraObjectMapping.STATUS_NAME.compare(name)){
        Status status = issue.getStatusObject();
        text = updateOutboundField(text, template, field, (status == null)?null:status.getName());
      }else if(JiraObjectMapping.COMPONENT_NAMES.compare(name)){
        Collection<ProjectComponent> cc = issue.getComponentObjects();
        List<String> components = new ArrayList<String>();
        for (ProjectComponent pc : cc) {
          components.add(pc.getName());
        }
        text = updateOutboundField(text, template, field, SyncHelper.convertStringListToText(components));
      }else if(JiraObjectMapping.AFFECTED_VERSION_NAMES.compare(name)){
        Collection<Version> avc = issue.getAffectedVersions();
        List<String> avl = new ArrayList<String>();
        if(avl != null){
          for (Version v : avc) {
            avl.add(v.getName());
          }
        }
        text = updateOutboundField(text, template, field, SyncHelper.convertStringListToText(avl));
      }else if(JiraObjectMapping.FIX_VERSION_NAMES.compare(name)){
        List<String> fvl = new ArrayList<String>();
        Collection<Version> fvc = issue.getFixVersions();
        if(fvc != null){
          for (Version v : fvc) {
            fvl.add(v.getName());
          }
        }
        text = updateOutboundField(text, template, field, SyncHelper.convertStringListToText(fvl));
      }else if(JiraObjectMapping.DUE_DATE.compare(name)){
        text = updateOutboundField(text, template, field, SyncUtils.convertDateToText(issue.getDueDate()));
      }else if(JiraObjectMapping.CREATED_DATE.compare(name)){
        text = updateOutboundField(text, template, field, SyncUtils.convertDateToText(issue.getCreated()));
      }else if(JiraObjectMapping.UPDATED_DATE.compare(name)){
        text = updateOutboundField(text, template, field, SyncUtils.convertDateToText(issue.getUpdated()));
      }else if(JiraObjectMapping.ORIGINAL_ESTIMATE.compare(name)){
        text = updateOutboundField(text, template, field, SyncUtils.convertLongTimeToText(issue.getOriginalEstimate()));
      }else if(JiraObjectMapping.ESTIMATE.compare(name)){
        text = updateOutboundField(text, template, field, SyncUtils.convertLongTimeToText(issue.getEstimate()));
      }else if(JiraObjectMapping.TIME_SPENT.compare(name)){
        text = updateOutboundField(text, template, field, SyncUtils.convertLongTimeToText(issue.getTimeSpent()));
      }else if(JiraObjectMapping.LABEL_NAMES.compare(name)){
        Set<Label> ls = issue.getLabels();
        List<String> labels = new ArrayList<String>();
        for (Label l : ls) {
          labels.add(l.getLabel());
        }
        text = updateOutboundField(text, template, field, SyncHelper.convertStringListToText(labels));
      }else if(JiraObjectMapping.SUBTASK_NAMES.compare(name)){
        Collection<Issue> subtasks = issue.getSubTaskObjects();
        List<String> subTaskList = new ArrayList<String>();
        for (Issue st : subtasks) {
          subTaskList.add(st.getKey());
        }
        text = updateOutboundField(text, template, field, SyncHelper.convertStringListToText(subTaskList));
      }else if(JiraObjectMapping.COMMENTS.compare(name)){
        CommentManager cmntMngr = ComponentAccessor.getCommentManager();
        List<Comment> cmnts = cmntMngr.getComments(issue);
        text = updateOutboundField(text, template, field, SyncUtils.convertCommentsToText(cmnts));
      }else if(JiraObjectMapping.WORK_LOG.compare(name)){
        WorklogManager wMngr = ComponentAccessor.getWorklogManager();
        List<Worklog> worklogs = wMngr.getByIssue(issue);
        text = updateOutboundField(text, template, field, SyncUtils.convertWorklogsToText(worklogs));
      }else if(JiraObjectMapping.HISTORY.compare(name)){
        ChangeHistoryManager hMngr = ComponentAccessor.getChangeHistoryManager();
        List<ChangeHistory> changeHistories = hMngr.getChangeHistories(issue);
        text = updateOutboundField(text, template, field, SyncUtils.convertHistoryToText(changeHistories));
      }else if(JiraObjectMapping.RESOLUTION_DATE.compare(name)){
        text = updateOutboundField(text, template, field, SyncUtils.convertDateToText(issue.getResolutionDate()));
      }else if(JiraObjectMapping.VOTER_NAMES.compare(name)){
        List<String> voters = new ArrayList<String>();
        VoteManager vMngr = ComponentAccessor.getVoteManager();
        List<ApplicationUser> v = vMngr.getVotersFor(issue, ComponentAccessor.getJiraAuthenticationContext().getLocale());
        for (ApplicationUser u : v) {
          voters.add(u.getName());
        }
        text = updateOutboundField(text, template, field, SyncHelper.convertStringListToText(voters));
      }else if(JiraObjectMapping.WATCHER_NAMES.compare(name)){
        List<String> watchers = new ArrayList<String>();
        WatcherManager watcherMngr = ComponentAccessor.getWatcherManager();
        List<ApplicationUser> wList = watcherMngr.getWatchers(issue, ComponentAccessor.getJiraAuthenticationContext().getLocale());
        for (ApplicationUser u : wList) {
          watchers.add(u.getName());
        }
      
        text = updateOutboundField(text, template, field, SyncHelper.convertStringListToText(watchers));
      }else if(JiraObjectMapping.INWARD_LINKS.compare(name)){
        IssueLinkManager ilMngr = ComponentAccessor.getIssueLinkManager();
        List<IssueLink> inwards = ilMngr.getInwardLinks(issue.getId());
        text = updateOutboundField(text, template, field, SyncUtils.convertIssueLinksToText(inwards, true));
      }else if(JiraObjectMapping.OUTWARD_LINKS.compare(name)){
        IssueLinkManager ilMngr = ComponentAccessor.getIssueLinkManager();
        List<IssueLink> outwards = ilMngr.getOutwardLinks(issue.getId());
        text = updateOutboundField(text, template, field, SyncUtils.convertIssueLinksToText(outwards, false));
      }else if(JiraObjectMapping.OUTSIDE_LINKS.compare(name)){
        RemoteIssueLinkManager rilMngr = ComponentAccessor.getComponent(RemoteIssueLinkManager.class);
        List<RemoteIssueLink> outsideLinks = rilMngr.getRemoteIssueLinksForIssue(issue);
        text = updateOutboundField(text, template, field, SyncUtils.convertRemoteIssueLinksToText(outsideLinks));
      }
      
    }

    return text;
  }
  /**
   * It updates outgoing field - the fields which will be sent to the external system
   * @param text the value of the template
   * @param config the configuration of the template (snapshot)
   * @param field the configuration of the field
   * @param value the value which will ne added to the template
   * @return updated template which will be sent to the external system
   */
  private static String updateOutboundField(String text, SyncTemplate config, SyncField field, String value){
    String id = field.getId();

    if(id != null && !id.isEmpty()){
      if(value == null){
        value="";
      }
      if(field.isEncodeHtml()){
        value = SyncHelper.encodeHTML(value);
      }
      if(SyncConstants.CONFIG_CONTENT_TYPE_HTML.equals(config.getContentType())){
        value = SyncHelper.convertToHTML(value);
      }
      text = text.replace(config.getIdPrefix() + id + config.getIdSuffix(), value);
    }
    return text;
  }
  

  /**
   * Updates remote resources according LeanSync configuration. It only updates the links which starts with domain.
   * If there is defined the custom field for error logging the error is written to this  field
   * @param issue JIRA issue
   * @param config LeanSync configuration
   * @param errorHandler the handler which handles error messages
   * @throws URISyntaxException
   */
  public static void updateRemoteResource(Issue issue, SyncConfiguration config, ErrorSyncHandler errorHandler) throws URISyntaxException{
    CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
    SyncMapping mapping = config.getFirstOutMapping();
    if(mapping == null){
      logger.debug(CURRENT_CLASS+".updateRemoteResource - ", "The configuration doesn't contain outbound mapping.");
      return;
    }
    logger.debug(CURRENT_CLASS + ".updateRemoteResource: " +  " Update remote resource "+issue.getKey());
    
    Set<String> domains = config.getDomains();
    CustomField customField = customFieldManager.getCustomFieldObjectByName(JiraConstants.OSLC_CUSTOM_FIELD_NAME);
    if (customField != null) {
      String value = (String) customField.getValue(issue);
      Link[] links = OSLCUtils.convertToLinks(value);
      if(links != null){
        for (Link link : links) {
          if(link != null){
            updateLink(link, domains, issue, mapping, errorHandler);
          }else{
            logger.debug(CURRENT_CLASS + ".updateRemoteResource: " + " The link is null for issue " + issue.getKey());
          }
        }
      }
    }
  }
  

  /**
   * Updates remote resource according LeanSync configuration. It only updates the links which starts with domain.
   * If there is defined the custom field for error logging the error is written to this  field
   * @param link the link to remote resource. The resource will be updated by JIRA
   * @param domains only the remote resource which belongs to the domain will be updated
   * @param issue JIRA issue
   * @param mapping the mapping table
   * @param errorHandler the handler which handles error messages
   */
  private static void updateLink(Link link, Set<String> domains, Issue issue, SyncMapping mapping, ErrorSyncHandler errorHandler) {
    HttpResponse httpResponse = null;
    URI uri = link.getValue();
    try {
      
      // check if the link starts with predefined domain name
      if (allowUpdate(uri, domains)) {
        logger.debug(CURRENT_CLASS + ".updateLink: " + " Update remote resource " + uri.toString() + " for issue " + issue.getKey());
        httpResponse = updateRemoteResource(issue, mapping, uri);
      }else if (uri != null){
        logger.debug(CURRENT_CLASS + ".updateLink: " + uri.toString() + " doesn't match domain.");

      }
    } catch (Exception e) {
      logger.error(CURRENT_CLASS + ". updateRemoteResource-issue id= " + issue.getKey() + " link = " + link, e);
      errorHandler.addMessage(e.getMessage());
    } finally {
      handleHttpResponse(httpResponse, uri, errorHandler);
    }
  }
  
  /**
   * Check if the remote resource can be updated
   * @param uri the URI of remote resource
   * @param domains the list of domains which can be updated
   * @return true - the uri belongs to the set of domains which can be update, otherwise false
   */
  private static boolean allowUpdate(URI uri, Set<String> domains){
    if(uri == null || uri.toString() == null){
      return false;
    }else if(domains == null || domains.isEmpty()){
      return true;
    }
    
    String res = uri.toString().toLowerCase();
    for (String domain : domains) {
      if(res.startsWith(domain.toLowerCase())){
        return true;
      }
    }
    return false;
  }
  
  /**
   * It handles the response from remote system. It the error occurs the message is added to the ErrorSyncHandler
   * @param httpResponse HttpResponse
   * @param uri URI or remote resource
   * @param errorHandler it handles error messages
   */
  private static void handleHttpResponse(HttpResponse httpResponse, URI uri, ErrorSyncHandler errorHandler){
    if (httpResponse != null) {
      StatusLine status = httpResponse.getStatusLine();
      if (status != null) {
        if (status.getStatusCode() != 200 && httpResponse.getStatusLine().getStatusCode() != 201) {
          errorHandler.addMessage("Update of resource " + uri + " failed: " + status.getStatusCode() + " - " + status.getReasonPhrase());
        }
      } else {
        errorHandler.addMessage("Update of resource " + uri + " failed: HTTP response is empty.");
      }
    }
  }
  
  /**
   * It updates remote resource according to LeanSync configuration
   * @param issue JIRA issue
   * @param mapping the mapping table
   * @param uri URI of the remote resource which will be updated
   * @return the responce from remote system
   * @throws Exception 
   */
  private static HttpResponse updateRemoteResource(Issue issue, SyncMapping mapping, URI uri) throws Exception{
    Client client = new Client();
    HttpResponse responseGet = client.getRemoteResource(uri.toString(), mapping.getUsername(), mapping.getPassword(), mapping.getHeaders());
    if(!isResponseOk(responseGet)){
      return responseGet;
    }
    
    InputStream content = responseGet.getEntity().getContent();
    Object[] remoteObj = createIssuesFromRDFData(content);
    JiraChangeRequest remoteChangeRequest;
    Map<QName, Object> extPropsToAdd = new HashMap<QName, Object>();
    
    if (remoteObj != null && remoteObj.length >= 1) {
      remoteChangeRequest = (JiraChangeRequest) remoteObj[0];
      Map<QName, Object> remoteExtendedProps = remoteChangeRequest.getExtendedProperties();
      if(remoteExtendedProps != null){
        QName qNameAttachment = new QName("http://open-services.net/rdf#", "attachment");
        Object propVal = remoteExtendedProps.get(qNameAttachment);
        
        if(propVal != null){
          extPropsToAdd.put(qNameAttachment, propVal);
        }
      }
    } else {
      throw new Exception("Failed to GET remote resiurce from URI: " + uri);
    }
    
    
    String rdfData = createRDFData(issue, mapping, uri, extPropsToAdd);
    if(rdfData == null){
      throw new Exception("Can't create rdf data for issue " + issue.getKey() + "Outbound content is empty.");
    }

    
    return client.updateRemoteResource(uri.toString(), rdfData, mapping.getUsername(), mapping.getPassword(), mapping.getHeaders());
  }
  
  /**
   * Create IRA issue from rd data
   * @param rdfData
   * @return tasks created from RDF data
   * @throws WebApplicationException
   * @throws IOException
   */
  public static Object[] createIssuesFromRDFData(InputStream inputStream) {
    OslcXmlRdfProvider provider = new OslcXmlRdfProvider();
    MediaType mediaType = new MediaType("application", "rdf+xml", new HashMap<String, String>());
    Object[] issues = provider.readFrom(JiraChangeRequest.class, mediaType, null, inputStream);
      
    return issues;
  }
  
  /**
   * Add the template value to the map of extended properties
   * @param issue JIRA issue
   * @param mapping LeanSync mapping table
   * @param template the configuration of the template
   * @param extProps It handles extended properties which will be added to the JIRAChangeRequest and sent to the remote system
   */
  private static void addSnapshotField(Issue issue, SyncMapping mapping, SyncTemplate template, Map<QName, Object> extProps ){
    String snapshot = createOutboundSnapshot(issue, mapping,template);
    String rdfName = template.getName();
    String ns = template.getNs();
    
    if(rdfName == null || ns == null){
      logger.error(CURRENT_CLASS+".updateRemoteResource - Snapshot rdf name is not defined in the configuration");
      return;
    }

    if(snapshot == null){
      snapshot = "";
    }

    extProps.put(new QName(ns, SyncHelper.encodeTagName(rdfName)), snapshot);
  }
  
  /**
   * It create RDF data based on LeanSync configuration which will be sent to the remote system 
   * @param issue JIRA issue
   * @param mapping the LeanSync mapping table
   * @param link the remote link 
   * @return created RDF data based on LeanSync configuration which will be sent to the remote system 
   * @throws WebApplicationException
   * @throws IOException
   * @throws URISyntaxException
   */
  private static String createRDFData(Issue issue, SyncMapping mapping, URI link, Map<QName, Object> extPropsToAdd) throws WebApplicationException, IOException, URISyntaxException {
    List<SyncTemplate> templates = mapping.getTemplates();
    if(templates == null){
      logger.debug(CURRENT_CLASS+".createRDFData - ", "The configuration doesn't contain outbound templates.");
      return null;
    }

    Map<QName, Object> extProps = new HashMap<QName, Object>();
    if(extPropsToAdd != null){
      extProps.putAll(extPropsToAdd);
    }
    for (SyncTemplate template : templates) {
      addSnapshotField(issue, mapping, template, extProps);
    }

    JiraChangeRequest req = new JiraChangeRequest(link);
    req.setExtendedProperties(extProps);

    Set<String> rdfTypes = mapping.getRdfTypes();
    for (String rdfType : rdfTypes) {
      if(rdfType != null && !rdfType.isEmpty()){
        req.addRdfType(new URI(rdfType));
      }
    }

    OutputStream outputStream = new ByteArrayOutputStream();
    OslcRdfXmlProvider provider = new OslcRdfXmlProvider();
   
    provider.writeTo(req, JiraChangeRequest.class, JiraChangeRequest.class, null, null, null, outputStream);
    return outputStream.toString();
  }
  

  /**
   * Check if response code is OK,
   * @param httpResponse - if response code is 200 or 201 returns true, otherwise false
   * @return true if response code is 200 or 201, otherwise false
   */
  private static boolean isResponseOk(HttpResponse httpResponse){
    if (httpResponse != null) {
      StatusLine status = httpResponse.getStatusLine();
      if (status != null && (status.getStatusCode() == 200 || status.getStatusCode() == 201)) {
        return true;
      } 
    }
    return true;
  }
}
