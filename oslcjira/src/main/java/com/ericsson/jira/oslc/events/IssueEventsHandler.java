package com.ericsson.jira.oslc.events;

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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
import com.ericsson.eif.leansync.mapping.data.SyncConfiguration;
import com.ericsson.eif.leansync.mapping.data.SyncField;
import com.ericsson.eif.leansync.mapping.data.SyncMapping;
import com.ericsson.eif.leansync.mapping.data.SyncTemplate;
import com.ericsson.eif.leansync.mapping.data.SyncXmlFieldConfig;
import com.ericsson.jira.oslc.sync.JiraObjectMapping;
import com.ericsson.jira.oslc.sync.OutboundSyncUtils;
import com.ericsson.jira.oslc.sync.SyncConfig;
import com.ericsson.jira.oslc.sync.SyncUtils;
import com.ericsson.jira.oslc.utils.ErrorSyncHandler;
/**
 * 
 * A Listener which is called whenever events occur on JIRA issue
 */
public class IssueEventsHandler implements InitializingBean, DisposableBean {
  private static Logger logger = LoggerFactory.getLogger(IssueEventsHandler.class);
  private static final String CURRENT_CLASS = "JiraIssueEventsHandler";
  private final EventPublisher eventPublisher;

  /**
   * Constructor.
   * 
   * @param eventPublisher injected {@code EventPublisher} implementation.
   */
  public IssueEventsHandler(EventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void destroy() throws Exception {
    this.eventPublisher.unregister(this);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    this.eventPublisher.register(this);
  }

  /**
   * Receives any {@code IssueEvent}s sent by JIRA.
   * 
   * @param issueEvent the IssueEvent passed to us
   */
  @EventListener
  public void onIssueEvent(IssueEvent issueEvent) {
    Issue issue = issueEvent.getIssue();

    if (issue == null) {
      logger.debug(CURRENT_CLASS + ".onIssueEvent for issue null");
      return;
    }
    
    logger.debug(CURRENT_CLASS + ".onIssueEvent: IssueEvent for issue "
          + issue.getKey());

      
    try {
      Project project = issueEvent.getProject();
      
      if(EventType.ISSUE_CREATED_ID.equals(issueEvent.getEventTypeId())){
        issueEvent = null;
      }
      updateSnapshot(issue, project, issueEvent);
    } catch (Exception e) {
      logger.error("CURRENT_CLASS", e);
    }
  }

  /**
   * Receives any {@code IssueEvent}s sent by JIRA.
   * 
   * @param issueEvent the RestIssueEvent passed to us. It's custom event which is fired from our code manually
   */
   @EventListener
   public void onIssueEvent(RestIssueEvent event) {
     Issue issue = event.getIssue();
     if(issue == null){
       logger.error(CURRENT_CLASS + ".onIssueEvent: RestIssueEvent for issue null");
       return;
     }else{
       logger.debug(CURRENT_CLASS + ".onIssueEvent: RestIssueEvent for issue "
           + issue.getKey());
     }
     

     Project project = issue.getProjectObject();
     try {
       updateSnapshot(issue, project, null);
     } catch (Exception e) {
       logger.error("CURRENT_CLASS", e);
     }
   }
   
  /**
   * It update the JIRA snapshot in a remote resource 
   * @param issue JIRA issue which fires a event
   * @param project the project of JIRA issue
   * @param issueEvent the event which was fired
   * @throws Exception
   */
  private void updateSnapshot(Issue issue, Project project, IssueEvent issueEvent) throws Exception {

    SyncConfiguration leanSyncConfig = getLeanSyncConfiguration(issue, project);
    if (leanSyncConfig == null) {
      return;
    }

    ErrorSyncHandler errorHandler = new ErrorSyncHandler();

    try {
      SyncMapping firstInMapping = leanSyncConfig.getFirstInMapping();
      if (firstInMapping == null) {
        logger.debug(CURRENT_CLASS + ".updateSnapshot: " + " In mapping is null");
        return;
      }

      List<SyncTemplate> templates = firstInMapping.getTemplates();
      List<SyncField> fields = firstInMapping.getFields();
      List<SyncXmlFieldConfig> xmlFieldConfigs = firstInMapping.getXmlFieldConfigs();
      if (allowUpdate(issueEvent, templates, fields, xmlFieldConfigs, leanSyncConfig.getErrorLog())) {
        OutboundSyncUtils.updateRemoteResource(issue, leanSyncConfig, errorHandler);
      } else {
        logger.debug(CURRENT_CLASS + ".updateSnapshot: " + " Update remote resource " + issue.getKey() + " was not allowed.");
      }
    } catch (Exception e) {
      errorHandler.addMessage(e.getMessage());
      logger.error("CURRENT_CLASS", e);
    } finally {
      //Save error log to error custom field if it's configured
      String errorMessage = (errorHandler != null && !errorHandler.isLogEmpty())?errorHandler.getMessagesAsString():null;
      SyncUtils.updateErrorLog(leanSyncConfig, issue, errorMessage, false);
    }

  }
 
   /**
    * It checks if the update of the remote resource is allowed. It verifies if the fields
    * which have been changed are in the notification list.
    * @param issueEvent the event which was fired
    * @param templates the list of templates
    * @param fields the list of configurations of fields
    * @param xmlFieldConfigs the list of configurations of xml fields
    * @param errorLogName the name of Error custom field
    * @return true - the remote resource can be updated, otherwise false
    * @throws GenericEntityException
    */
   private boolean allowUpdate(IssueEvent issueEvent, List<SyncTemplate> templates, List<SyncField> fields, List<SyncXmlFieldConfig> xmlFieldConfigs, String errorLogName)
       throws GenericEntityException {
    if(issueEvent == null || issueEvent.getWorklog() != null){
      return true;
    }
    
    Set<String> notNotifiedFields = new HashSet<String>();
    
    if (errorLogName != null && !"".equals(errorLogName)) {
      notNotifiedFields.add(errorLogName);
    }

    if (templates != null) {
      for (SyncTemplate template : templates) {
        String mapTo = template.getMapTo();
        if (mapTo != null && !"".equals(mapTo) && !template.isNotifyChange()) {
          notNotifiedFields.add(mapTo);
        }
      }
    }
    
    if (fields != null) {
      for (SyncField field : fields) {
        String mapTo = field.getMapTo();
        if (mapTo != null && !"".equals(mapTo) && !field.isNotifyChange()) {
          notNotifiedFields.add(mapTo);
        }
      }
    }
    
    if (xmlFieldConfigs != null) {
      for (SyncXmlFieldConfig xmlFieldConfig : xmlFieldConfigs) {
        for (SyncField field : xmlFieldConfig.getFields()) {
          String mapTo = field.getMapTo();
          if (mapTo != null && !"".equals(mapTo) && !field.isNotifyChange()) {
            notNotifiedFields.add(mapTo);
          }
        }
      }
    }
    
    if(issueEvent.getComment() != null && !notNotifiedFields.contains(JiraObjectMapping.COMMENTS.getName())){
      return true;
    }

    GenericValue changeLog = issueEvent.getChangeLog();
    if (changeLog != null) {
      List<GenericValue> changeItemList = changeLog.getRelated("ChildChangeItem");
      if (changeItemList != null) {
        if (notNotifiedFields.size() < 1 && changeItemList.size() > 0) {
          return true;
        }
        for (GenericValue changeItem : changeItemList) {
          String name = changeItem.get("field").toString();
          String fieldType = changeItem.get("fieldtype").toString();
          if (fieldType != null && "jira".equals(fieldType)) {
            name = JiraObjectMapping.getFieldIdsToLabels().get(name);
          }
          if (!notNotifiedFields.contains(name)) {
            return true;
          }
        }
      }
    }
    return false;
  }
  
   /**
    * Fetch LeanSync Configuration based on project and issue type
    * @param issue JIRA issue which has been changed
    * @param project the project of JIRA issue
    * @return LeanSync configuration fro the issue
    * @throws Exception
    */
  public SyncConfiguration getLeanSyncConfiguration(Issue issue, Project project) throws Exception {
    Long projectId = null;
    if (project != null && project.getId() != null ) {
      projectId = project.getId();
    }else{
      logger.debug(CURRENT_CLASS + ".onIssueEvent: event " + " for issue " + issue.getKey() + " - project is null");
      return null;
    }
    
    String issueTypeId = null;
    if (issue.getIssueTypeObject() != null && issue.getIssueTypeObject().getId() != null) {
      issueTypeId = issue.getIssueTypeObject().getId();
    } else {
      logger.debug(CURRENT_CLASS + ".updateSnapshot: " +  " for issue " + issue.getKey() + " - issue type is null");
      return null;
    }
    
    SyncConfig config = SyncConfig.getInstance();
    Map<Object, SyncConfiguration> fieldConfMap = config.getConfigurationMap();

    logger.debug(CURRENT_CLASS + ".onIssueEvent: event " + " for issue " + issue.getKey() + " - project is " + projectId  + " - issueType is " + issueTypeId);
    if (projectId != null && fieldConfMap != null) {
      SyncConfiguration leanSyncConfig = fieldConfMap.get(projectId.toString());

      if (leanSyncConfig != null && issueTypeId != null && leanSyncConfig.containsIssueType(issueTypeId)) {
        logger.debug(CURRENT_CLASS + ".getLeanSyncConfiguration: " +  " Configuration found");
        return leanSyncConfig;
      }
    }else{
      logger.debug(CURRENT_CLASS + ".getLeanSyncConfiguration: " +  " Configuration not found for project " + projectId  + " and issueType " + issueTypeId);
    }
    
    return null;
  }
}
