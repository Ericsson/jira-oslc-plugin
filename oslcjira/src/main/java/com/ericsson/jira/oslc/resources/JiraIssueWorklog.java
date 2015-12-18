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

import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.user.ApplicationUser;
import com.ericsson.jira.oslc.Constants;

/**
 * It represents a work log. It's a part of JIRA Change Request
 *
 */

@OslcNamespace(Constants.JIRA_NAMESPACE)
@OslcName("IssueWorklog")
@OslcResourceShape(title = "Issue Worklog resource shape")
public class JiraIssueWorklog extends AbstractResource {
  
  private String author = null;
  private String comment = null;
  private Date created = null;
  private Date updated = null;
  private String updateAuthor = null;
  private Date startDate = null;
  private Long timeSpent = null;
  
  //NOTE: default c'tor without parameters must be defined. If not, jena is not able to
  // create instance for further parsing in jersey.
  public JiraIssueWorklog() {
  }
  
  public JiraIssueWorklog(Worklog w) {
    ApplicationUser appUser = w.getAuthorObject();
    this.author = appUser.getName();
    this.comment = w.getComment();
    this.created = w.getCreated();
    this.updated = w.getUpdated();
    ApplicationUser updUser = w.getUpdateAuthorObject();
    this.updateAuthor = updUser.getName();
    this.startDate = w.getStartDate();
    this.timeSpent = w.getTimeSpent();
  }
  
  @OslcDescription("Worklog comment.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "worklogComment")
  @OslcTitle("Worklog comment")
  public String getWorklogComment() {
    return this.comment;
  }

  public void setWorklogComment(String value) {
    this.comment = value;
  }
  
  @OslcDescription("Author of worklog.")
  @OslcOccurs(Occurs.ExactlyOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "worklogAuthor")
  @OslcTitle("Worklog author")
  public String getWorklogAuthor() {
    return this.author;
  }

  public void setWorklogAuthor(String name) {
    this.author = name;
  }
  
  @OslcDescription("Date of worklog creation.")
  @OslcOccurs(Occurs.ExactlyOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "worklogCreated")
  @OslcTitle("Worklog creation date")
  public Date getWorklogCreated() {
    return this.created;
  }

  public void setWorklogCreated(Date created) {
    this.created = created;
  }
  
  @OslcDescription("Date of worklog update.")
  @OslcOccurs(Occurs.ExactlyOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "worklogUpdated")
  @OslcTitle("Worklog update date")
  public Date getWorklogUpdated() {
    return this.updated;
  }

  public void setWorklogUpdated(Date updated) {
    this.updated = updated;
  }
  
  @OslcDescription("Author of worklog update.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "worklogUpdateAuthor")
  @OslcTitle("Worklog update author")
  public String getWorklogUpdateAuthor() {
    return this.updateAuthor;
  }

  public void setWorklogUpdateAuthor(String name) {
    this.updateAuthor = name;
  }
  
  @OslcDescription("Date of worklog start.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "worklogStart")
  @OslcTitle("Worklog start date")
  public Date getWorklogStart() {
    return this.startDate;
  }

  public void setWorklogStart(Date started) {
    this.startDate = started;
  }
  
  @OslcDescription("Worklog spent time.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "worklogTimeSpent")
  @OslcTitle("Worklog spent time")
  public Long getWorklogTimeSpent() {
    return this.timeSpent;
  }

  public void setWorklogTimeSpent(Long value) {
    this.timeSpent = value;
  }
}
