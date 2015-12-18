package com.ericsson.jira.oslc.resources;

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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lyo.oslc4j.core.annotation.OslcDescription;
import org.eclipse.lyo.oslc4j.core.annotation.OslcName;
import org.eclipse.lyo.oslc4j.core.annotation.OslcNamespace;
import org.eclipse.lyo.oslc4j.core.annotation.OslcOccurs;
import org.eclipse.lyo.oslc4j.core.annotation.OslcPropertyDefinition;
import org.eclipse.lyo.oslc4j.core.annotation.OslcResourceShape;
import org.eclipse.lyo.oslc4j.core.annotation.OslcTitle;
import org.eclipse.lyo.oslc4j.core.model.AbstractResource;
import org.eclipse.lyo.oslc4j.core.model.Occurs;

import com.atlassian.jira.issue.changehistory.ChangeHistory;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.ericsson.jira.oslc.Constants;

/**
 * It represents a History request. It servers for fetching a history from an issue
 * 
 */
@OslcNamespace(Constants.JIRA_NAMESPACE)
@OslcName("IssueHistory") 
@OslcResourceShape(title = "Jira issue history shape", describes = Constants.JIRA_TYPE_HISTORY)
public final class JiraHistoryRequest extends AbstractResource {

  private List<JiraIssueHistoryItem> history = null;
  
  //NOTE: default c'tor without parameters must be defined. If not, jena is not able to
  //create instance for further parsing in jersey.
  public JiraHistoryRequest() {
    this.history = new ArrayList<JiraIssueHistoryItem>();
  }
  
  public JiraHistoryRequest(List<ChangeHistory> hList) {
    this.history = new ArrayList<JiraIssueHistoryItem>();
    
    for (ChangeHistory h : hList) {
      List<ChangeItemBean> beans = h.getChangeItemBeans();
      for (ChangeItemBean bean : beans) {
        history.add(new JiraIssueHistoryItem(bean));
      }
    }
  }
  
  @OslcDescription("The Jira history item.")
  @OslcOccurs(Occurs.ZeroOrMany)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "issueHistoryItem")
  @OslcName("issueHistoryItem")
  @OslcTitle("History item")
  public List<JiraIssueHistoryItem> getIssueHistoryItems() {
    return this.history;
  }
  
  public void setIssueHistoryItems(List<JiraIssueHistoryItem> items) {
    this.history = items;
  }
}
