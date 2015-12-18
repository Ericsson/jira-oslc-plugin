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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.xpath.jaxp.XPathFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.customfields.impl.GenericTextCFType;
import com.atlassian.jira.issue.customfields.impl.TextAreaCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.ericsson.eif.leansync.mapping.SyncConfigLoader;
import com.ericsson.eif.leansync.mapping.SyncConstants;
import com.ericsson.eif.leansync.mapping.SyncHelper;
import com.ericsson.eif.leansync.mapping.data.ActionType;
import com.ericsson.eif.leansync.mapping.data.SyncConfiguration;
import com.ericsson.eif.leansync.mapping.data.SyncField;
import com.ericsson.eif.leansync.mapping.data.SyncMapping;
import com.ericsson.eif.leansync.mapping.data.SyncSnapshot;
import com.ericsson.eif.leansync.mapping.data.SyncTemplate;
import com.ericsson.eif.leansync.mapping.data.SyncXmlFieldConfig;
import com.ericsson.jira.oslc.Constants;
import com.ericsson.jira.oslc.constants.JiraConstants;
import com.ericsson.jira.oslc.managers.FieldManager;
import com.ericsson.jira.oslc.resources.JiraChangeRequest;
import com.ericsson.jira.oslc.utils.JiraIssueInputParameters;

/**
 * The class offers the helping methods for synchronization from the external system to JIRa
 *
 */
public class InboundSyncUtils {
  private static Logger logger = LoggerFactory.getLogger(InboundSyncUtils.class);
  
  /**
   * Updates JiraIssueInputParameters according to incoming values which are saved in JiraChangeRequest
   * @param jcr JiraChangeRequest containing incoming values which will be put tu the JIRA issue
   * @param leanSyncConfig LeanSync configuration containing the field mapping
   * @param jiraIssueInputParams JiraIssueInputParameters
   * @param actionType - it servers for a recognition if the JIRA issue will be create or update
   * @throws XPathExpressionException
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws IOException
   */
  public static void updateInputParameters(JiraChangeRequest jcr, SyncConfiguration leanSyncConfig, JiraIssueInputParameters jiraIssueInputParams, ActionType actionType) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {

    SyncMapping mapping = leanSyncConfig.getFirstInMapping();
    if(mapping == null){
      return;
    }

    List<SyncSnapshot> snapshots = new ArrayList<SyncSnapshot>();
    
    List<SyncTemplate> templates = mapping.getTemplates();
    if (templates != null) {
      for (SyncTemplate syncTemplate : templates) {
        snapshots.add(new SyncSnapshot(syncTemplate, syncTemplate.getTemplate()));
      }
    }

    
    List<SyncField> fields = mapping.getFields();
    Map<QName, Object> extProperties = jcr.getExtendedProperties();
    
    if(fields != null){
      for (SyncField field : fields) {
        String ns = field.getNs();
        String name = field.getName();
        if(ns == null || ns.isEmpty() || name == null || name.isEmpty()){
          continue;
        }

        String fullname = ns + name;
        if(  Constants.DCTERMS_TITLE.equals(fullname)){
          updateInboundFields(snapshots, field, jcr.getTitle(), jiraIssueInputParams, actionType);
        }else if(Constants.DCTERMS_DESCRIPTION.equals(fullname)){
          updateInboundFields(snapshots, field, jcr.getDescription(), jiraIssueInputParams, actionType);
        }else if(Constants.DCTERMS_IDENTIFIER.equals(fullname)){
          updateInboundFields(snapshots, field, jcr.getIdentifier(), jiraIssueInputParams, actionType);
        } else if (Constants.OSLC_CM_STATUS.equals(fullname)) {
          updateInboundFields(snapshots, field, jcr.getStatus(), jiraIssueInputParams, actionType);
        }else if(extProperties != null){
            Object obj = extProperties.get(new QName(ns, name));
            if(obj == null){
              updateInboundFields(snapshots, field, "", jiraIssueInputParams, actionType);
            }else{
              updateInboundFields(snapshots, field, obj.toString(), jiraIssueInputParams, actionType);
            }
        }
      }
    }
    
    List<SyncXmlFieldConfig> xmlFieldConfigs = mapping.getXmlFieldConfigs();
    for (SyncXmlFieldConfig xmlFieldConfig : xmlFieldConfigs) {
      String ns = xmlFieldConfig.getNs();
      String name = xmlFieldConfig.getName();
      
      Object xml = extProperties.get(new QName(ns, name));
      if(xml instanceof String){
        processIncomingXmlContent((String)xml, snapshots, xmlFieldConfig, jiraIssueInputParams, actionType);
      }else{
        processIncomingXmlContent(null, snapshots, xmlFieldConfig, jiraIssueInputParams, actionType);
      }
    }


    if(snapshots != null){
      for (SyncSnapshot snapshot : snapshots) {
        ActionType defTemplAction = snapshot.getTemplateConfig().getAction();
        if(defTemplAction != null && defTemplAction != ActionType.UNDEF && defTemplAction != actionType){
          continue;
        }
        
        String value = SyncHelper.mapToValue(snapshot.getValue(), snapshot.getTemplateConfig());
        
        try {
          putValueToIssue(snapshot.getTemplateConfig().getMapTo(), value, snapshot.getTemplateConfig().getFieldType(), jiraIssueInputParams);
        }
        catch (Exception e) {
          String mapTo = snapshot.getTemplateConfig().getMapTo();
          if (mapTo == null) {
            mapTo = "Unknown value for mapTo";
          }
          jiraIssueInputParams.getErrorSyncHandler().addMessage(e.getMessage(), mapTo);
        }
      }
    }
  }
  
  /**
   * Store concrete value to a JiraIssueInputParameters object to concrete filed name.
   * @param name Name of field to be updated
   * @param value Value to be stored in the specific field
   * @param fieldType Filed type. For example "custom"
   * @param dataType Type of the specific value
   * @param jiraInputParams Object to be updated. 
   * @throws NumberFormatException If the input parameter is not possible formating.
   * @throws ParseException If the input parameter is not possible parse.  
   */
  public static void putValueToIssue(String name, String value, String fieldType, JiraIssueInputParameters jiraInputParams) throws NumberFormatException, ParseException {
    int multiTextLimit = SyncUtils.getTextLimit();
    
    if (SyncConstants.CONFIG_FIELD_TYPE_CUSTOM.equalsIgnoreCase(fieldType)) {
      CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
      CustomField customField = customFieldManager.getCustomFieldObjectByName(name);
      if (customField != null) {
        if (SyncUtils.isCFTextMaxLengthSupported()) {
          if (customField.getCustomFieldType() instanceof TextAreaCFType) {
            value = SyncUtils.trimTextWithWarning(jiraInputParams, name, value, multiTextLimit);
          } else if (customField.getCustomFieldType() instanceof GenericTextCFType) {
            value = SyncUtils.trimTextWithWarning(jiraInputParams, name, value, multiTextLimit);
          }
        }
        jiraInputParams.getIssueInputParameters().addCustomFieldValue(customField.getId(), value);
      }
    } else if (JiraObjectMapping.SUMMARY.compare(name)) {
      value = SyncUtils.trimTextWithWarning(jiraInputParams, name, value, JiraConstants.SINGLE_TEXT_LIMIT);
      jiraInputParams.getIssueInputParameters().setSummary(value);

    } else if (JiraObjectMapping.DESCRIPTION.compare(name)) {
      value = SyncUtils.trimTextWithWarning(jiraInputParams, name, value, multiTextLimit);
      jiraInputParams.getIssueInputParameters().setDescription(value);
    }

    else if (JiraObjectMapping.ASSIGNEE_ID.compare(name)) {
      jiraInputParams.getIssueInputParameters().setAssigneeId(value);

    } else if (JiraObjectMapping.ISSUE_KEY.compare(name)) {
      // it is not possible to change the issueKey value in JIRA
    } else if (JiraObjectMapping.ISSUE_TYPE_ID.compare(name)) {
      // values: 1-Bug, 2-NewFeature, 3-Task, 4-Improvement
      jiraInputParams.getIssueInputParameters().setIssueTypeId(value);

    } else if (JiraObjectMapping.RESOLUTION_ID.compare(name)) {
      // not working - the Resolution isn't changed in JIRA
      jiraInputParams.getIssueInputParameters().setResolutionId(value);

    } else if (JiraObjectMapping.REPORTER_ID.compare(name)) {
      jiraInputParams.getIssueInputParameters().setReporterId(value); // JIRA user

    } else if (JiraObjectMapping.CREATOR_ID.compare(name)) {
      // JIRA set the CREATOR automatically during create issue

    } else if (JiraObjectMapping.ENVIRONMENT.compare(name)) {
      value = SyncUtils.trimTextWithWarning(jiraInputParams, name, value, multiTextLimit);
      jiraInputParams.getIssueInputParameters().setEnvironment(value);
    } else if (JiraObjectMapping.PROJECT_KEY.compare(name)) {
      // not working - isn't changed in JIRA
      if (value != null && !value.isEmpty()) {
        Long lProjectKey = Long.valueOf(value);
        if (lProjectKey != null) {
          jiraInputParams.getIssueInputParameters().setProjectId(lProjectKey); // Long
        }
      }
    }

    else if (JiraObjectMapping.PRIORITY_ID.compare(name)) {
      // map the priority in a configuration
      jiraInputParams.getIssueInputParameters().setPriorityId(value);
    }

    else if (JiraObjectMapping.STATUS_ID.compare(name)) {
      // not working - isn't changed in JIRA
      // NOTE: changing to different status is not enough to actually change
      // issue status.
      // Status must be changed via proper action (e.g. 'Start progress' from
      // 'Open' state
      // to 'In progress' state). So here we just set the status to
      // corresponding field
      // in IssueInputParameters instance.
      jiraInputParams.getIssueInputParameters().setStatusId(value);

    }

    else if (JiraObjectMapping.COMPONENT_NAMES.compare(name)) {
      if (value != null && !value.isEmpty()) {
        Long[] components = FieldManager.componentNamesToIds(jiraInputParams.getProject(), value);
        if (components != null) {
          jiraInputParams.getIssueInputParameters().setComponentIds(components);
        }else{
          jiraInputParams.getIssueInputParameters().setComponentIds(new Long[0]);
        }
      }else{
        jiraInputParams.getIssueInputParameters().setComponentIds(new Long[0]);
      }
    }

    else if (JiraObjectMapping.AFFECTED_VERSION_NAMES.compare(name)) {
      if (value != null && !value.isEmpty()) {
        Long[] versions = FieldManager.versionNamesToIds(jiraInputParams.getProject(), value);
        if (versions != null) {
          jiraInputParams.getIssueInputParameters().setAffectedVersionIds(versions); 
        }else{
          jiraInputParams.getIssueInputParameters().setAffectedVersionIds(new Long[0]); 
        }
      }else{
        jiraInputParams.getIssueInputParameters().setAffectedVersionIds(new Long[0]); 
      }
    } 

    else if (JiraObjectMapping.FIX_VERSION_NAMES.compare(name)) {
      if (value != null && !value.isEmpty()) {
        Long[] versions = FieldManager.versionNamesToIds(jiraInputParams.getProject(), value);
        if (versions != null) {
          jiraInputParams.getIssueInputParameters().setFixVersionIds(versions); 
        }else{
          jiraInputParams.getIssueInputParameters().setFixVersionIds(new Long[0]); 
        }
      }else{
        jiraInputParams.getIssueInputParameters().setFixVersionIds(new Long[0]); 
      }
    }

    else if (JiraObjectMapping.DUE_DATE.compare(name)) {
      jiraInputParams.getIssueInputParameters().setDueDate(value);
    }


    else if (JiraObjectMapping.ORIGINAL_ESTIMATE.compare(name)) {
      // If you want to change both in a single request use
      // setOriginalAndRemainingEstimate and setOriginalAndRemainingEstimate.
      // Calls to setOriginalEstimate and setRemainingEstimate are mutually
      // exclusive,
      // so calling both on the same instance will not produce the desired
      // effect.
      if (value != null && !value.isEmpty()) {
        Long lOriginalEstimate = Long.valueOf(value);
        if (lOriginalEstimate != null) {
          jiraInputParams.getIssueInputParameters().setOriginalEstimate(lOriginalEstimate); // Long - in minutes
        }
      }
    }

    else if (JiraObjectMapping.ESTIMATE.compare(name)) {
      // If you want to change both in a single request use
      // setOriginalAndRemainingEstimate and setOriginalAndRemainingEstimate.
      // Calls to setOriginalEstimate and setRemainingEstimate are mutually
      // exclusive,
      // so calling both on the same instance will not produce the desired
      // effect.
      if (value != null && !value.isEmpty()) {
        Long lEstimate = Long.valueOf(value);
        if (lEstimate != null) {
          jiraInputParams.getIssueInputParameters().setRemainingEstimate(value); // Long - in minutes
        }
      }
    }

    else if (JiraObjectMapping.TIME_SPENT.compare(name)) {
      // not working - isn't changed in JIRA
      if (value != null && !value.isEmpty()) {
        Long lTimeSpent = Long.valueOf(value);
        if (lTimeSpent != null) {
          jiraInputParams.getIssueInputParameters().setTimeSpent(lTimeSpent); // JIRA - Long in minutes
        }
      }
    } 

    else if (JiraObjectMapping.COMMENT.compare(name)) {
      value = SyncUtils.trimTextWithWarning(jiraInputParams, name, value, multiTextLimit);
      jiraInputParams.getIssueInputParameters().setComment(value);
    } else if (JiraObjectMapping.RESOLUTION_DATE.compare(name)) {
      // not working
      jiraInputParams.getIssueInputParameters().setResolutionDate(value);
    }
  }
  
  /**
   * Updates JiraIssueInputParameters according to incoming values which are saved in the JiraChangeRequest
   * @param snapshots the list of snapshots
   * @param field the synchronization configuration of the field
   * @param value new value of the field
   * @param jiraIssueInputParams JiraIssueInputParameters - containing new value of JIRA issue
   * @param actionType  it servers for a recognition if the JIRA issue will be create or update
   */
  private static void updateInboundFields(List<SyncSnapshot> snapshots, SyncField field, String value, JiraIssueInputParameters jiraIssueInputParams, ActionType actionType) {
      //map value according to mapping value table
      value = SyncHelper.mapToValue(value, field);
      //convert value according to defined data type
      value = convertDateValue(value, field);
      //encode html string
      if(field.isEncodeHtml()){
        value = SyncHelper.encodeHTML(value);
      }
      
      ActionType definedActionType = field.getAction();
      //map to general field
      if(field.getMapTo() != null && (definedActionType == null || definedActionType == ActionType.UNDEF || definedActionType == actionType)){
        String val = value;
        
        if(SyncConstants.CONFIG_CONTENT_TYPE_HTML.equals(field.getContentType())){
          val = SyncHelper.convertToHTML(val);
        }
       
        try { 
          putValueToIssue(field.getMapTo(), val, field.getFieldType(), jiraIssueInputParams);
        }
        catch (Exception e) {
          String mapTo = field.getMapTo();
          if (mapTo == null) {
            mapTo = "Unknown value for mapTo";
          }
          jiraIssueInputParams.getErrorSyncHandler().addMessage(e.getMessage(), mapTo);
        }
      }

      String id = field.getId();
      if(id != null && !id.isEmpty()){
      //map to template field
      for (SyncSnapshot snapshotValue : snapshots) {
        ActionType defTemplAction = snapshotValue.getTemplateConfig().getAction();
        if(defTemplAction != null && defTemplAction != ActionType.UNDEF && defTemplAction != actionType){
          continue;
        }
        
        
        String val = value;
        if(SyncConstants.CONFIG_CONTENT_TYPE_HTML.equals(snapshotValue.getTemplateConfig().getContentType())){
          val = SyncHelper.convertToHTML(val);
        }
               
        String tmplText = snapshotValue.getValue();
        if(tmplText != null){
          if(val == null){
            val="";
          }
          tmplText = tmplText.replace(snapshotValue.getTemplateConfig().getIdPrefix() + id + snapshotValue.getTemplateConfig().getIdSuffix(), val);
        }
        snapshotValue.setValue(tmplText);
      }
    }
  }
  
  /**
   * set the value of specific xpath. XML can contain more tags. In this case we have to save all tags
   * @param xml the value in xml format
   * @param snapshots the list of the snapshot containing the value of the snapshot and also configuration
   * @param xmlFieldConfig the configuration of xml field
   * @param jiraInputParams JiraIssueInputParameters
   * @param actionType it servers for recognition if the JIRA issue will be create or update
   * @throws XPathExpressionException
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws IOException
   */
  private static void processIncomingXmlContent(String xml, List<SyncSnapshot> snapshots, SyncXmlFieldConfig xmlFieldConfig, JiraIssueInputParameters jiraInputParams, ActionType actionType) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
    if (xml == null || xml.isEmpty()) {
      List<SyncField> fields = xmlFieldConfig.getFields();
      for (SyncField field : fields) {
          // xml content is empty -> update fields with empty string
          updateInboundFields( snapshots, field, "", jiraInputParams, actionType);
      }
      return;
    }

    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
    domFactory.setNamespaceAware(true);
    DocumentBuilder builder = domFactory.newDocumentBuilder();
    ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());
    Document doc = builder.parse(is);
    if(doc == null){
      return;
    }
    
    XPathFactory xpathFactory = new XPathFactoryImpl();
    XPath xpath = xpathFactory.newXPath();

    List<SyncField> fields = xmlFieldConfig.getFields();
    for (SyncField field : fields) {
      String strXpath = field.getXpath();
      if (strXpath == null || strXpath.isEmpty()) {
        continue;
      }

      //xml can contain 1 or more same tags -> save all tags
      NodeList valueList = (NodeList) xpath.evaluate(strXpath, doc, XPathConstants.NODESET);
      if (valueList != null && valueList.getLength() > 0) {
        String value = null;
        for (int i = 0; i < valueList.getLength(); i++) {
          if(i != 0 && value != null){
            value+= SyncConstants.END_OF_LINE; 
          }
          
          Node node = valueList.item(i);
          
          if(field.isKeepTags()){
            value = SyncHelper.appendText(value, SyncConfigLoader.loadXMLNodeValue(node));
          }else{
            value = SyncHelper.appendText(value, node.getTextContent());
          }
        }

        updateInboundFields(snapshots, field, value, jiraInputParams, actionType);
      } else {
        updateInboundFields(snapshots, field, null, jiraInputParams, actionType);
      }

    }
  }
  
  /**
   * It converts Date value to strong form
   * @param value date value
   * @param field the configuration field which contains the definitiaon how to convert the value
   * @return converted date
   */
  public static String convertDateValue(String value, SyncField field){
    String convertedValue = null;
    try {
      convertedValue = SyncUtils.convertDateValue(value, field.getFromDateFormat(), field.getToDateFormat());
    } catch (Exception e) {
      logger.warn("Warning - converting incoming value failed: " + value, e);
    }
    if (convertedValue != null) {
      value = convertedValue;
    }
    return value;
  }
  


}
