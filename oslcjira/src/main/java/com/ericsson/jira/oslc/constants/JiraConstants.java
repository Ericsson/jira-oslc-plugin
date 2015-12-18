package com.ericsson.jira.oslc.constants;

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

public interface JiraConstants {
  public static final String REST_URL="/rest/jirarestresource/1.0/";
  public static final String ACTIVATE_CONSUMER_URL="oauth/consumers/activate/";
  public static final String REMOVE_CONSUMER_URL="oauth/consumers/remove/";
  public static final String REMOVE_RS_LINK = "oauth/rootserviceslinks/removelink/";
  public static final String REMOVE_SP_CATALOG_LINK = "oauth/serviceprovidercatalogslinks/removelink/";
  public static final String REMOVE_SERVICEPROVIDER_LINK = "oauth/serviceproviderslinks/removelink/";
  public static final String OAUTH_CONSUMER_PAGE_URL="/plugins/servlet/jiraservlet/OAuthConsumerServlet";
  public static final String ROOTSEERVICES_MANAGEMENTT_PAGE = "/plugins/servlet/jiraservlet/RootServicesManagementServlet";
  public static final String CATALOGS_MANAGEMENT_PAGE = "/plugins/servlet/jiraservlet/ServiceProviderCatalogsManagementServlet";
  public static final String PROJECT_RELATIONSHIPS_PAGE = "/plugins/servlet/jiraservlet/ProjectRelationshipsServlet";
  public static final String ISSUE_TYPE_PATH = "issueTypes/";
  public static final String ISSUE_PRIORITY_PATH = "issuePriorities/";
  public static final String ISSUE_STATUS_PATH = "issueStates/";
  public static final String ISSUE_RESOLUTION_PATH = "issueResolutions/";
  public static final String CREATION_DIALOG_WIDTH="900px";
  public static final String CREATION_DIALOG_HEIGHT="600px";
  public static final String SELECTION_DIALOG_WIDTH="900px";
  public static final String SELECTION_DIALOG_HEIGHT="600px";
  public static final String CM_CHANGE_REQUEST= "http://open-services.net/ns/cm#ChangeRequest";
  public static final String CREATE_ISSUE = "/plugins/servlet/jiraservlet/createissue";
  public static final String SELECT_ISSUE = "/plugins/servlet/jiraservlet/selectissue";
  public static final String REMOVE_OSLC_LINK_FROM_REMOTE_APP = "oslc/links/removeFromRemoteApp/";
  public static final String REMOVE_OSLC_LINK_FROM_JIRA = "oslc/links/removeFromJira/";
  public static final String ADD_OSLC_LINK_DIALOG="/plugins/servlet/jiraservlet/addoslclinkdialog";
  public static final String ADD_OSLC_LINK_TO_REMOTE_APP = "oslc/links/addToRemoteApp/";
  public static final String ADD_OSLC_LINK_TO_JIRA = "oslc/links/addToJira/";
  public static final String OSLC_CUSTOM_FIELD_NAME="External Links";
  public static final String OSLC_CUSTOM_FIELD_LABEL="Label";
  public static final String OSLC_CUSTOM_FIELD_URI="URI";
  public static final String GET_OSLC_LINK_TYPES="oslc/links/types/";
  public static final String OSLC_RESPONSE_TYPE_1 = "#oslc-windowName-1.0";
  public static final String OSLC_RESPONSE_TYPE_2 = "#oslc-postMessage-1.0";
  public static final String OSLC_RESPONSE_TYPE_3 = "#oslc-core-windowName-1.0";
  public static final String OSLC_RESPONSE_TYPE_4 = "#oslc-core-postMessage-1.0";
  public static final String SESSION_OAUTHACCESSOR = "oAuthAccessor";
  public static final String SESSION_CURRENT_LINK = "currentOperationLink";
  public static final String OAUTH_CALLBACK_SERVICE_URL = REST_URL + "oauth/authorizationcallback";
  public static final String OAUTH_EXT_CALLBACK_SERVICE_URL = REST_URL + "oauth/authorizationexternalcallback";
  public static final String RELATED_CHANGE_REQUEST_URL_APPENDIX = "?oslc.properties=oslc_cm%3ArelatedChangeRequest&oslc.prefix=oslc_cm%3D%3Chttp%3A%2F%2Fopen-services.net%2Fns%2Fcm%23%3E";
  public static final String ISSUE_ICON = "/images/icons/favicon.png";
  public static final String REALM_NAME = "JIRA";
  public static final String SYNC_HEADER_NAME = "LeanSync";
  public static final int SINGLE_TEXT_LIMIT = 255;
  public static final String XSD_PATH = "/config/leanSyncConfig.xsd";
  

}
