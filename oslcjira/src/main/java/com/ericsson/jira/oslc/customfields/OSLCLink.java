package com.ericsson.jira.oslc.customfields;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.impl.GenericTextCFType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.ericsson.jira.oslc.constants.JiraConstants;
import com.ericsson.jira.oslc.exceptions.PermissionException;
import com.ericsson.jira.oslc.managers.JiraManager;
import com.ericsson.jira.oslc.managers.PermissionManager;
import com.ericsson.jira.oslc.utils.AppLinksRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * It represents the link to remote resource. 
 *
 */
public class OSLCLink extends GenericTextCFType {
	private static final String CURRENT_CLASS = "OSLCLink";
	private static final Logger logger = LoggerFactory.getLogger(OSLCLink.class);

	protected OSLCLink(CustomFieldValuePersister customFieldValuePersister, GenericConfigManager genericConfigManager) {
		super(customFieldValuePersister, genericConfigManager);
	}

	@Override
	public Map<String, Object> getVelocityParameters(final Issue issue, final CustomField field, final FieldLayoutItem fieldLayoutItem) {
		final Map<String, Object> map = super.getVelocityParameters(issue, field, fieldLayoutItem);

		CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();

		
		if (issue == null || issue.getId() == null) {
			return map;
		}

		CustomField customField = customFieldManager.getCustomFieldObjectByName(JiraConstants.OSLC_CUSTOM_FIELD_NAME);

		if (customField == null) {
			return map;
		}
		
		String ApplicationLinks = (String) customField.getValue(issue);

		AppLinksRepository appLinkList = new AppLinksRepository();
		try {
		GsonBuilder gsonBuilder = new GsonBuilder();
			Gson gson = gsonBuilder.create();
			appLinkList = gson.fromJson(ApplicationLinks, AppLinksRepository.class);

		} catch (com.google.gson.JsonSyntaxException e) {
			logger.error(CURRENT_CLASS, e);
		}

		if (appLinkList == null) {
			appLinkList = new AppLinksRepository();
		}
		
		ApplicationUser user = PermissionManager.getLoggedUser();
    boolean editable = false;
    if (user != null){ 
        try {
          PermissionManager.checkPermissionWithUser(user, issue, Permissions.EDIT_ISSUE);
            editable = true;
        } catch (PermissionException e) {
            editable = false;
        }
    }
    map.put("editable", editable);
		map.put("restURL", JiraManager.getRestUrl());
		map.put("baseURL", JiraManager.getBaseUrl());
		map.put("link_addoslclinkdialog", JiraManager.getBaseUrl() + JiraConstants.ADD_OSLC_LINK_DIALOG + "?issuekey=" + issue.getKey());
		map.put("removeOslcLinkURLFromRemoteApp", JiraConstants.REMOVE_OSLC_LINK_FROM_REMOTE_APP);
		map.put("removeOslcLinkURLFromJira", JiraConstants.REMOVE_OSLC_LINK_FROM_JIRA);
		map.put("issueID", issue.getId().toString());
		map.put("appLinkList", appLinkList.GetAllAppLinks());
		map.put("oauthcallback", JiraManager.getBaseUrl() + JiraConstants.OAUTH_CALLBACK_SERVICE_URL);

		return map;
	}
}
