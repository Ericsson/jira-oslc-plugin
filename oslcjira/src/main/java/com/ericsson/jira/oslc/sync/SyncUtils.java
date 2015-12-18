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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.changehistory.ChangeHistory;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.issue.managers.DefaultCustomFieldManager;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.user.ApplicationUser;
import com.ericsson.eif.leansync.mapping.SyncConstants;
import com.ericsson.eif.leansync.mapping.SyncHelper;
import com.ericsson.eif.leansync.mapping.data.SyncConfiguration;
import com.ericsson.eif.leansync.mapping.data.SyncTemplate;
import com.ericsson.jira.oslc.resources.JiraChangeRequest;
import com.ericsson.jira.oslc.resources.JiraIssueType;
import com.ericsson.jira.oslc.utils.ErrorSyncHandler;
import com.ericsson.jira.oslc.utils.JiraIssueInputParameters;

/**
 * Helping class for synchronization 
 *
 */
public class SyncUtils {
  private static Logger logger = LoggerFactory.getLogger(SyncUtils.class);


  /**
   * Convert Remote links to text form
   * @param links the link which will be converted
   * @return text form of links
   */
  public static String convertRemoteIssueLinksToText(List<RemoteIssueLink> links){
    StringBuilder sb = new StringBuilder();
    for (RemoteIssueLink link : links) {
      sb.append(link.getTitle());
      sb.append(" - ");
      sb.append(link.getUrl());
      sb.append(SyncConstants.END_OF_LINE);
    }
    return sb.toString();
  }

  /**
   * Convert Issue links to text form
   * @param issueLinks the link which will be converted
   * @param inward if it's true - it's inward , otherwise outward
   * @return text form of links
   */
  public static String convertIssueLinksToText(List<IssueLink> issueLinks, boolean inward){
    StringBuilder sb = new StringBuilder();
    for (IssueLink link : issueLinks) {
      Issue srcIssue = link.getSourceObject();
      IssueLinkType type = link.getIssueLinkType();
      
      if(inward){
        sb.append(type.getInward());
      }else{
        type.getOutward();
      }
      sb.append( "- ");
      sb.append(srcIssue.getKey());

      sb.append(SyncConstants.END_OF_LINE);
    }
    return sb.toString();
  }

  /**
   * Convert custom field value to text form
   * @param issue the issue containing custom fields
   * @param cf custum field
   * @return string representation of custom field value
   */
  public static String convertCustomFieldToText(Issue issue, CustomField cf){
    if (cf != null){
      Object value = cf.getValue(issue);
      if(value != null){
        return value.toString();
      }
    }
    return null;
  }
  
  /**
   * Convert custom field value to text form
   * @param date the date which will be converted to string form
   * @return string representation of date value
   */
  public static String convertDateToText(Date date){
    if (date != null) {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
      return formatter.format(date);
    }
    return null;
  }

  /**
   * Convert the date or time to defined format mat.<br>
   * @param dateTimeValue date/time value whihc will ne comverted
   * @param fromDateTemplate the template of source date/time
   * @param toDateTemplate the template of target date/time
   * @return converted date
   * @throws ParseException if the error occurs during parse date/time value
   */
  public static String convertDateValue(String dateTimeValue, String fromDateTemplate, String toDateTemplate) throws ParseException {
    if (fromDateTemplate != null && toDateTemplate != null && dateTimeValue != null && !dateTimeValue.isEmpty()) {
      SimpleDateFormat toDateFormat = new SimpleDateFormat(toDateTemplate);
      SimpleDateFormat fromDateFormat = new SimpleDateFormat(fromDateTemplate);;

      // convert date/time according correct format
      String dateTimeResult = null;
      if (fromDateFormat != null && toDateFormat != null) {
        Date dateTimeParse = fromDateFormat.parse(dateTimeValue);
        dateTimeResult = toDateFormat.format(dateTimeParse);
      }
      return dateTimeResult;
    }
    return null;
  }
  
  /**
   * Convert custom field value to text form
   * @param time the time which will be converted to string form
   * @return string representation of time
   */
  public static String convertLongTimeToText(Long time){
    if (time != null) {
      time /= 60; //to minutes
      return time.toString();
    }   
    return null;
  }
  

  /**
   * Convert the value of Work logs to text form
   * @param list the time which will be converted to string form
   * @return string representation of Work logs
   */
  public static String convertWorklogsToText(List<Worklog> list){
    if(list == null){
      return null;
    }
    
    StringBuilder sb = new StringBuilder();
 
    for (Worklog w : list) {
      sb.append("Author: ");
      sb.append(w.getAuthorKey());
      sb.append(", ");
      
      sb.append("Start date: ");
      sb.append(convertDateToText(w.getStartDate()));
      sb.append(", ");
      
      sb.append("Time spent: ");
      sb.append(convertLongTimeToText(w.getTimeSpent()));
      sb.append(", ");
      
      sb.append("Comment: ");
      sb.append(w.getComment());
      sb.append(SyncConstants.END_OF_LINE);

    }
    return sb.toString();
  }
  
  /**
   * Convert the value of History to text form
   * @param list the History which will be converted to string form
   * @return  string representation of History
   */
  public static String convertHistoryToText(List<ChangeHistory> list){
    if(list == null){
      return null;
    }
    
    StringBuilder sb = new StringBuilder();
 
    for (ChangeHistory ch : list) {
      sb.append("History[");
      for (ChangeItemBean cib : ch.getChangeItemBeans()) {
        sb.append(SyncConstants.END_OF_LINE);
        sb.append("Field: ");
        sb.append(cib.getField());
        sb.append(", ");
        
        sb.append("Created: ");
        sb.append(convertDateToText(cib.getCreated()));
        sb.append(", ");
        
        sb.append("From: ");
        sb.append(cib.getFromString());
        sb.append(", ");
        
        sb.append("To: ");
        sb.append(cib.getToString());
        sb.append(SyncConstants.END_OF_LINE);
      }
      sb.append("]");
      sb.append(SyncConstants.END_OF_LINE);
    }
    return sb.toString();
  }
  
  /**
   * Convert the value of Comment to text form
   * @param list the Comment which will be converted to string form
   * @return  string representation of Comment
   */
  public static String convertCommentsToText(List<Comment> list){
    if(list == null){
      return null;
    }
    
    StringBuilder sb = new StringBuilder();
    
    for (Comment c : list) {
      
      ApplicationUser authorUser = c.getAuthorApplicationUser();
      if(authorUser != null){
        sb.append("Author: ");
        sb.append(authorUser.getName());
        sb.append(", ");
      }

      sb.append("Created: ");
      sb.append(convertDateToText(c.getCreated()));
      sb.append(", ");
      
      sb.append("Updated: ");
      sb.append(convertDateToText(c.getUpdated()));
      sb.append(", ");
      
      sb.append("Comment: ");
      sb.append(c.getBody());
      sb.append(SyncConstants.END_OF_LINE);
    }
    return sb.toString();
  }

  /**
   * Update one custom filed record in in JiraIssueInputParameters by set value
   * @param issue Issue the object which will be updated
   * @param jiraIssueParameter JiraIssueInputParameters to be updated
   * @param customFieldName Custom filed name to be updated
   * @param value New value of custom field
   */
  public static void putValueToErrorField (Issue issue, JiraIssueInputParameters jiraIssueParameter, String customFieldName, ErrorSyncHandler errorHandler ) {
    if (customFieldName == null) {
      return;
    }
    String value = null;
    if(errorHandler != null){
      value = errorHandler.getMessagesAsString();
    }
    
    IssueInputParameters issueInputParams = jiraIssueParameter.getIssueInputParameters();
    CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
    CustomField customField = customFieldManager.getCustomFieldObjectByName(customFieldName);
    if (customField == null) {
      return; 
    }
    String customFieldId = customField.getId();
    
    Object oldValue = null;
    if(issue != null){
      oldValue = customField.getValue(issue);
    }
        
    if (oldValue == null || oldValue instanceof String) {
      value = SyncHelper.createSyncStatus((String) oldValue, value, true);
      if(isCFTextMaxLengthSupported()){
        value = SyncUtils.trimCFTextToMaxLength(value);
      }
      issueInputParams.addCustomFieldValue(customFieldId, value);
    }
  }
  

  /**
   * Save error messages to Error Custom field
   * @param issue the issue where the error messages will be written to
   * @param errorHandler contains error messages
   * @param customFieldName the name of Error custom field
   */
  public static void saveErrorStatus(MutableIssue issue, ErrorSyncHandler errorHandler, String customFieldName) {
    if (customFieldName == null || errorHandler == null || issue == null) {
      return;
    }
    
    String value = errorHandler.getMessagesAsString();
    CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
    CustomField customField = customFieldManager.getCustomFieldObjectByName(customFieldName);
    if (customField == null) {
      return; 
    }
    
    Object oldValue = customField.getValue(issue);
        
    if (oldValue == null || oldValue instanceof String) {
      value = SyncHelper.createSyncStatus((String) oldValue, value, true);
      if(isCFTextMaxLengthSupported()){
        value = SyncUtils.trimCFTextToMaxLength(value);
      }
      customField.createValue(issue, value);
    }
  }
  


  
  /**
   * It saves the values of the custom fields which have to be saved even when the errors occurs
   * @param newJiraIssueInputParam object to be updated
   * @param oldJiraIssueParameter object contains input value for new object
   * @param leanSyncConfiguration configuration that contain leanSync configuration
   * @param save if it's set on 'true' - the values will be saved to the issue directly.
   * If it's set on 'false' - the values will be saved to IssueiInputParameters  
   */
  public static void handleErrorState(Issue issue, JiraIssueInputParameters newJiraIssueInputParam, JiraIssueInputParameters oldJiraIssueParameter, SyncConfiguration leanSyncConfiguration, boolean save) {
    if(leanSyncConfiguration == null || leanSyncConfiguration.getFirstInMapping() == null){
      return;
    }
    
    if (!save && newJiraIssueInputParam == null) {
      newJiraIssueInputParam = new JiraIssueInputParameters();
    }
    
    List<String> customFieldsSnapshot = getAlwaysSavedFields(leanSyncConfiguration);
    if(customFieldsSnapshot == null){
      return;
    }
    
    IssueInputParameters parsedIssueInputParameters = oldJiraIssueParameter.getIssueInputParameters();
    if (parsedIssueInputParameters == null) {
      return;
    }
    
    //get custom field already parsed
    CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();

    for (String customFiledName : customFieldsSnapshot) {
      StringBuilder newCustomFieldsValues = new StringBuilder();
      
      CustomField customField = customFieldManager.getCustomFieldObjectByName(customFiledName);
      String customFieldId = customField.getId();
      if (customField != null) {
        String[] customFieldValues = parsedIssueInputParameters.getCustomFieldValue(customFieldId);

        //create error message
        if (customFieldValues != null) {
          for (String customFiledValue : customFieldValues) {
            newCustomFieldsValues.append(customFiledValue);  
          }
        }
        else {
          newCustomFieldsValues.append("Error: Empty value");
        }
        
        String value = newCustomFieldsValues.toString();
        if(isCFTextMaxLengthSupported()){
          value = SyncUtils.trimCFTextToMaxLength(value);
        }
        
        if(save){
          customField.createValue(issue, value);
        }else{
          //copy all necessary value to new IssueInput parameter
          newJiraIssueInputParam.getIssueInputParameters().addCustomFieldValue(customFieldId, value);
        }
      }
    }
  }

  /**
   * Returns the list on names of custom fields which have to be saved even when the error occurs
   * @param leanSyncConfiguration the LeanSync configuration
   * @return the list on names of custom fields which have to be saved even when the error occurs
   */
  private static List<String> getAlwaysSavedFields(SyncConfiguration leanSyncConfiguration) {
    List<String> customFieldsSnapshot = new ArrayList<String>();
    List<SyncTemplate> templates = leanSyncConfiguration.getFirstInMapping().getTemplates();
    if (templates != null) {
      for (SyncTemplate syncTemplate : templates) {
        String mapTo = syncTemplate.getMapTo();
        String alwaysSave = syncTemplate.getAlwaysSave();
        if (alwaysSave == null) {
          alwaysSave = SyncConstants.BOOLEAN_FALSE; // default value
        }
        if (mapTo != null && alwaysSave != null && syncTemplate.getFieldType().equalsIgnoreCase(SyncConstants.CONFIG_FIELD_TYPE_CUSTOM)) {
          if (alwaysSave.equalsIgnoreCase(SyncConstants.BOOLEAN_TRUE)) {
            customFieldsSnapshot.add(syncTemplate.getMapTo());
          }
        }
      }
    }
    return customFieldsSnapshot;
  }
  
  /**
   * Get LeanSync configuration based of defined the project and issue types
   * @param jcr JIRA Change Request
   * @param issue the JIRA issue
   * @return LeanSync configuration based of defined the project and issue types
   * @throws Exception
   */
  public static SyncConfiguration getLeanSyncConfiguration (JiraChangeRequest jcr, MutableIssue issue) throws Exception {
    SyncConfig config = SyncConfig.getInstance();
    Map<Object, SyncConfiguration> fieldConfMap = config.getConfigurationMap();
    CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();

    Long projectId = null;
    if(issue != null && issue.getProjectObject() != null &&  issue.getProjectObject().getId() !=null){
      projectId = issue.getProjectObject().getId();
    }else{
      projectId =jcr.getProjectId();
    }

    String issueTypeId = null;
    if(issue != null && issue.getIssueTypeObject() != null && issue.getIssueTypeObject().getId() !=null){
      issueTypeId = issue.getIssueTypeObject().getId();
    }else{
      JiraIssueType type = jcr.getIssueType();
      if (type != null && type.getAbout() != null) {
        issueTypeId = JiraChangeRequest.parseId(type.getAbout().toString());
      }
    }
    
    if (customFieldManager == null || projectId == null || fieldConfMap == null) {
      return null;
    }
    
    SyncConfiguration leanSyncConfig = fieldConfMap.get(projectId.toString());
    if (leanSyncConfig == null || issueTypeId == null || !leanSyncConfig.containsIssueType(issueTypeId)) {
      return null;
    }

    return leanSyncConfig;
  }
  
  /**
   * Updates the value of Error custom field
   * @param syncConfig LeanSync configuration
   * @param issue the issue containing Error custom field
   * @param message error messages
   * @param isInbound - true - the inbound section will be updated, false the outbound section will be updated
   */
  public static void updateErrorLog(SyncConfiguration syncConfig, Issue issue, String message, boolean isInbound){
    DefaultCustomFieldManager cfManager = (DefaultCustomFieldManager) ComponentAccessor.getCustomFieldManager();
    if (cfManager != null && syncConfig != null && issue != null) {
      String errorLogName = syncConfig.getErrorLog();
      if (errorLogName != null) {
        CustomField customField = cfManager.getCustomFieldObjectByName(errorLogName);
        if (customField != null) {
          Object value = customField.getValue(issue);
          if (value == null || value instanceof String) {
            String syncStatus = SyncHelper.createSyncStatus((String) value, message, isInbound);
            customField.createValue(issue, syncStatus);
          }
        }
      }
    }
  }
  
  /**
   * Returns the max length of the value of custom field
   * @return the max length of the value of custom field
   */
  public static int getTextLimit() {
    int limit = 0;
    String strLimit = ComponentAccessor.getApplicationProperties().getText(APKeys.JIRA_TEXT_FIELD_CHARACTER_LIMIT);
    if (strLimit == null) {
      return limit;
    }

    try {
      limit = new Integer(strLimit);
    } catch (NumberFormatException ex) {
      logger.error("Error", ex);
    }
    return limit;
  }
  
  /**
   * Returns the version of JIRA
   * @return the version of JIRA
   */
  public static String getJiraVersion() {
    return ComponentAccessor.getApplicationProperties().getText(APKeys.JIRA_VERSION);
  }
  
  /**
   * It cuts the text if it exceeds the max length of the value of the custom field
   * @param jiraInputParams JiraIssueInputParameters 
   * @param fieldName the name of custom field
   * @param text the value which will be trimmed
   * @param maxLength the max length of the value of the custom field
   * @return trimmed text if it was needed
   */
  public static String trimTextWithWarning(JiraIssueInputParameters jiraInputParams, String fieldName, String text, int maxLength){
    if(maxLength > 0 && text != null && text.length() > maxLength){
      text = text.substring(0, maxLength);
      jiraInputParams.getWarnSyncHandler().addMessage("Warning: '" + fieldName +"' field - The text  was too long and has been trimmed to " + maxLength +" chars");
    }
    return text;
  }

  /**
   * Returns true if the max length is supported, otherwise false.
   * It's supported from version 6.4.1
   * @return true if the max length is supported, otherwise false.
   */
  public static boolean isCFTextMaxLengthSupported(){
    String jiraVersion =  SyncUtils.getJiraVersion();
    return (jiraVersion != null && !jiraVersion.startsWith("6.2") && !jiraVersion.startsWith("6.3") && !jiraVersion.equals("6.4"));
  }
  
  /**
   * It cuts the text if it exceeds the max length of the value of the custom field
   * @param value the value which will be trimmed
   * @return trimmed text if it was needed
   */
  public static String trimCFTextToMaxLength(String value){
    int multiTextLimit = SyncUtils.getTextLimit();
    return trimText(value, multiTextLimit);
  }
  
  /**
   * It cuts the text if it exceeds the max length of the value of the custom field
   * @param text  the value which will be trimmed
   * limit the max length of text in custom field
   * @return trimmed text if it was needed
   */
  public static String trimText(String text, int limit){
    if(limit > 0 && text != null && text.length() > limit){
      text = text.substring(0, limit);
    }
    return text;
  }
}
