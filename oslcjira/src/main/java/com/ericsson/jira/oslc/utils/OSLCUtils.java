package com.ericsson.jira.oslc.utils;

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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.lyo.oslc4j.core.model.Link;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.ericsson.jira.oslc.constants.JiraConstants;
import com.ericsson.jira.oslc.events.IssueEventType;
import com.ericsson.jira.oslc.events.RestIssueEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
/*
 * Utility class for OSLC operations
 * 
 */
public final class OSLCUtils {
  
  private static final int OSLC_V_2 = 2; 
  
  /**
   * Function prepares response, which is sent after issue is created/selected.
   * Response is created for 'oslc-windowName-1.0' or 'oslc-postMessage-1.0'.
   * @param issueId Id of issue
   * @param create If true, create response is created. If false, selection response is created.
   * @return string with response messages in corresponding OSLC format
   */
  public static String getOSLCResponseMessage(long issueId, final boolean create) {
    return getOSLCResponseMessage(issueId, 1, create);
  }
  
  /**
   * Function prepares response, which is sent after issue is created/selected.
   * Response is created for 'oslc-core-windowName-1.0' of 'oslc-core-postMessage-1.0'.
   * @param issueId Id of issue
   * @param create If true, create response is created. If false, selection response is created.
   * @return string with response messages in corresponding OSLC format
   */
  public static String getOSLCResponseCoreMessage(long issueId, final boolean create) {
    return getOSLCResponseMessage(issueId, OSLC_V_2, create);
  }
  
  /**
   * Function prepares response, which is sent after issue is created/selected.
   * Response is used, when method is not specified (response contains both variants).
   * @param issueId Id of issue
   * @param create If true, create response is created. If false, selection response is created.
   * @return string with response messages in corresponding OSLC format
   */
  public static String getOSLCResponseCommonMessage(long issueId, final boolean create) {
    return getOSLCResponseMessage(issueId, 0, create);
  }
  
  private static String getOSLCResponseMessage(final long issueId, final int oslcVersion, final boolean create) {
    IssueManager issueManager = ComponentAccessor.getIssueManager();
    MutableIssue issue = issueManager.getIssueObject(issueId);
    
    String iid = issue.getKey();
    String link = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL) +
                  "/rest/jirarestresource/1.0/" + issue.getProjectId() + "/changeRequests/" + issue.getKey();
    String oslcType = (create == true) ? "\\\"oslc_cm:create\\\"" : "\\\"oslc_cm:select\\\"";
    String s = "";
    
    if (oslcVersion == 1) {
      s = "{\\\"oslc_cm:message\\\":" + oslcType + ",\\\"oslc_cm:results\\\":[";
      s += "{\\\"oslc_cm:label\\\":\\\"" + iid + "\\\", \\\"rdf:resource\\\":\\\"" + link + "\\\"}]}";
    } 
    else if (oslcVersion == OSLC_V_2) {
      s = "{\\\"oslc:results\\\":[";
      s += "{\\\"oslc:label\\\":\\\"" + iid + "\\\", \\\"rdf:resource\\\":\\\"" + link + "\\\"}]}";
    } 
    else {
      s = "{\\\"oslc_cm:message\\\":" + oslcType + ",\\\"oslc_cm:results\\\":[";
      s += "{\\\"oslc_cm:label\\\":\\\"" + iid + "\\\", \\\"rdf:resource\\\":\\\"" + link + "\\\"}],";
      
      s += "\\\"oslc:results\\\":[";
      s += "{\\\"oslc:label\\\":\\\"" + iid + "\\\", \\\"rdf:resource\\\":\\\"" + link + "\\\"}]}";
    }
    
    return s;
  }
  
  /**
   * It converts the remote links from OSLC Link to JIRA Link
   * @param links the remote link in OSLC representaion
   * @return the remote link in OSLC representaion
   */
  public static List<AppLink> convertToAppLinks(Link[] links){
    List<AppLink> appLinkList = new ArrayList<AppLink>();
    for (Link link : links) {
      AppLink appLink = new AppLink(link.getLabel(), link.getValue().toString());
        appLinkList.add(appLink);
    }
    
    return appLinkList;
  }
  
  /**
   * It converts the remote link saved in JSON to OSLC representation of the link
   * @param value the link saved in the JSON
   * @return  OSLC representation of the link
   * @throws URISyntaxException
   */
  public static Link[] convertToLinks(String value) throws URISyntaxException{
    GsonBuilder gsonBuilder = new GsonBuilder();
    Gson gson = gsonBuilder.create();

    AppLinksRepository appLinkList = gson.fromJson(value, AppLinksRepository.class);
    if (appLinkList != null) {
      List<Link> oslcLinks = new ArrayList<Link>();

      ArrayList<Map<String, String>> getAllAppLinks = appLinkList.GetAllAppLinks();
      for (Map<String, String> map : getAllAppLinks) {
        Link link = new Link(new URI(map.get(JiraConstants.OSLC_CUSTOM_FIELD_URI)), map.get(JiraConstants.OSLC_CUSTOM_FIELD_LABEL));
        oslcLinks.add(link);
      }
      return oslcLinks.toArray(new Link[0]);
    }
    return null;
  }
  
  /**
   * Function gets property from rdf model.
   * @param rdfModel
   * @param namespace
   * @param predicate
   * @return returns string with property content
   */
  public static String getProperty(Model rdfModel, String namespace, String predicate) {
    String returnVal = null;
        
    Property prop = rdfModel.createProperty(namespace, predicate);
    Statement stmt = rdfModel.getProperty((Resource) null, prop);
    
    if (stmt != null && stmt.getObject() != null)
      returnVal = stmt.getObject().toString();
    
    return returnVal;
  }
  
  /**
   * Function gets literal value from rdf model.
   * @param resource
   * @param property
   * @return
   */
  public static String getResourceLiteralValue(Resource resource, Property property) {
    StmtIterator listProperties = resource.listProperties(property);
    while (listProperties.hasNext()) {
      Statement next = listProperties.next();
      try {
        com.hp.hpl.jena.rdf.model.Literal literal = next.getLiteral();
        if (literal != null) {
          return literal.getString();
        }
      } 
      catch (com.hp.hpl.jena.rdf.model.LiteralRequiredException e) {
        return null;
      }
    }
    return null;

  }
  
  /**
   * It returns the OSLC REST URI to the JIRA issue
   * @param issue JIRA issue
   * @return OSLC REST URI to the JIRA issue
   */
  public static URI getRestUriForIssue(Issue issue) {
    URI uri = null;
    
    String uriString = 
        ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL) + 
        JiraConstants.REST_URL +
        issue.getProjectId() + "/changeRequests/" + 
        issue.getKey();
    
    try {
      uri = new URI(uriString);
    } 
    catch (URISyntaxException e) {
    }
    
    return uri;
  }
  
  /**
   * It determines if the issue can be updated. 
   * If the selectedProperties is null or contains the defined property then the issue can be updated
   * @param selectedProperties OSLC properties
   * @param property defined property
   * @return true - the issue can be updated, otherwise false
   */
  public static boolean allowUpdate(Map<String, Object> selectedProperties, String property) {
    return allowUpdate(selectedProperties, property, null, false);
  }
  
  /**
   * It determines if the issue can be updated. 
   * If the sync type is not defined and the selectedProperties is null or contains the defined property then the issue can be updated
   * The issue also can be updated ig the field is mandatory and sync type is defined
   * @param selectedProperties OSLC properties
   * @param property defined property
   * @param sync sync type
   * @param mandatory true - the field is mandatory, otherwise false
   * @return the issue can be updated, otherwise false
   */
  public static boolean allowUpdate(Map<String, Object> selectedProperties, String property, String sync, boolean mandatory) {
    return ((sync == null) && (selectedProperties == null || selectedProperties.containsKey(property)) )
        ||((sync != null) && mandatory );
  }
  
  /**
   * It return the id of the resource which is in the last segment of URI
   * @param about the URI containing the ID of the resource
   * @return ID of resource
   */
  public static String getValueFromAbout(URI about) {
    String aboutString = about.toString();
    int idx = aboutString.lastIndexOf("/");
    if (idx != -1) {
      return aboutString.substring(idx + 1);
    }
    return null;
  }
  
  /**
   * It fires Issue event. It's called in the situation when the IRA system doesn't fire own event.
   * @param issue JIRA issue
   * @param user legged user
   * @param type the type of event
   */
  public static void fireRestIssueEvent(final MutableIssue issue, final ApplicationUser user, IssueEventType type) {
    EventPublisher evPublisher = ComponentAccessor.getComponent(EventPublisher.class);
    
    User dirUser = null; 
    if(user != null){
      dirUser = ApplicationUsers.toDirectoryUser(user);
    }
    RestIssueEvent event = new RestIssueEvent(issue, dirUser, type);
    evPublisher.publish(event);
  }
  
  /**
   * It returns empty text if input text is null, otherwise returns the same text
   * @param text input text 
   * @return empty text if input text is null, otherwise returns the same text
   */
  public static String replaceNullForEmptyString(String text){
    return (text == null)?"":text;
  }
  
  /**
   * Check if input string is empty or null. If it's then returns true, otherwise false
   * @param s input string
   * @return true if input string is empty or null, otherwise false
   */
  public static boolean isNullOrEmpty(String s) {
    return s == null || s.length() == 0;
  }

}
