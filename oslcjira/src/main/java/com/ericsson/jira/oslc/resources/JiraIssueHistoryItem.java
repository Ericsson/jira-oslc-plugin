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

import java.util.Date;

import org.eclipse.lyo.oslc4j.core.annotation.OslcDescription;
import org.eclipse.lyo.oslc4j.core.annotation.OslcName;
import org.eclipse.lyo.oslc4j.core.annotation.OslcNamespace;
import org.eclipse.lyo.oslc4j.core.annotation.OslcOccurs;
import org.eclipse.lyo.oslc4j.core.annotation.OslcPropertyDefinition;
import org.eclipse.lyo.oslc4j.core.annotation.OslcResourceShape;
import org.eclipse.lyo.oslc4j.core.annotation.OslcTitle;
import org.eclipse.lyo.oslc4j.core.model.AbstractResource;
import org.eclipse.lyo.oslc4j.core.model.Occurs;

import com.atlassian.jira.issue.history.ChangeItemBean;
import com.ericsson.jira.oslc.Constants;

/**
 * It represents an item of history. It's a part of JIRAIssueHIstory 
 *
 */

@OslcNamespace(Constants.JIRA_NAMESPACE)
@OslcName("IssueHistoryItem")
@OslcResourceShape(title = "Issue History Item resource shape", describes = Constants.JIRA_NAMESPACE + "IssueHistoryItem")
public class JiraIssueHistoryItem extends AbstractResource {
  
  private Date created = null;
  private String field = null;
  private String fieldType = null;
  private String from = null;
  private String fromString = null;
  private String to = null;
  private String toString = null;
  
  //NOTE: default c'tor without parameters must be defined. If not, jena is not able to
  //create instance for further parsing in jersey.
  public JiraIssueHistoryItem() {
  }
  
  public JiraIssueHistoryItem(ChangeItemBean bean) {
    this.created = bean.getCreated();
    this.field = bean.getField();
    this.fieldType = bean.getFieldType();
    this.from = bean.getFrom();
    this.fromString = bean.getFromString();
    this.to = bean.getTo();
    this.toString = bean.getToString();
  }

  @OslcDescription("Date of history item creation.")
  @OslcOccurs(Occurs.ExactlyOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "historyItemCreated")
  @OslcName("historyItemCreated")
  @OslcTitle("History item creation date")
  public Date getHistoryItemCreated() {
    return this.created;
  }

  public void setHistoryItemCreated(Date created) {
    this.created = created;
  }
 
  @OslcDescription("History item - field.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "historyItemField")
  @OslcName("historyItemField")
  @OslcTitle("Field")
  public String getHistoryItemField() {
    return this.field;
  }

  public void setHistoryItemField(String value) {
    this.field = value;
  }
  
  @OslcDescription("History item - field type.")
  @OslcOccurs(Occurs.ExactlyOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "historyItemFieldType")
  @OslcName("historyItemFieldType")
  @OslcTitle("FieldType")
  public String getHistoryItemFieldType() {
    return this.fieldType;
  }

  public void setHistoryItemFieldType(String value) {
    this.fieldType = value;
  }
  
  @OslcDescription("History item - from.")
  @OslcOccurs(Occurs.ExactlyOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "historyItemFrom")
  @OslcName("historyItemFrom")
  @OslcTitle("From")
  public String getHistoryItemFrom() {
    return this.from;
  }

  public void setHistoryItemFrom(String value) {
    this.from = value;
  }
  
  @OslcDescription("History item - from string.")
  @OslcOccurs(Occurs.ExactlyOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "historyItemFromString")
  @OslcName("historyItemFromString")
  @OslcTitle("FromString")
  public String getHistoryItemFromString() {
    return this.fromString;
  }

  public void setHistoryItemFromString(String value) {
    this.fromString = value;
  }
  
  @OslcDescription("History item - to.")
  @OslcOccurs(Occurs.ExactlyOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "historyItemTo")
  @OslcName("historyItemTo")
  @OslcTitle("To")
  public String getHistoryItemTo() {
    return this.to;
  }

  public void setHistoryItemTo(String value) {
    this.to = value;
  }
  
  @OslcDescription("History item - to string.")
  @OslcOccurs(Occurs.ExactlyOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "historyItemToString")
  @OslcName("historyItemToString")
  @OslcTitle("ToString")
  public String getHistoryItemToString() {
    return this.toString;
  }

  public void setHistoryItemToString(String value) {
    this.toString = value;
  }
}
