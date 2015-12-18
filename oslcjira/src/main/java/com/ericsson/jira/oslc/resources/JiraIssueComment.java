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

import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.user.ApplicationUser;
import com.ericsson.jira.oslc.Constants;

/**
 * It represents Issue comment. It's a part of JIRA Change Request
 *
 */

@OslcNamespace(Constants.JIRA_NAMESPACE)
@OslcName("IssueComment")
@OslcResourceShape(title = "Issue comment resource shape", describes = Constants.JIRA_NAMESPACE + "IssueComment")
public class JiraIssueComment extends AbstractResource {
  
  private String author = null;
  private String body = null;
  private Date created = null;
  private Date updated = null;
  
  //NOTE: default c'tor without parameters must be defined. If not, jena is not able to
  // create instance for further parsing in jersey.
  public JiraIssueComment() {
  }
  
  public JiraIssueComment(Comment comment) {
    ApplicationUser appUser = comment.getAuthorApplicationUser();
    this.author = appUser.getName();
    this.body = comment.getBody();
    this.created = comment.getCreated();
    this.updated = comment.getUpdated();
  }
  
  @OslcDescription("The Jira comment text for this change request.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "commentBody")
  @OslcTitle("Comment body")
  public String getCommentBody() {
    return this.body;
  }

  public void setCommentBody(String body) {
    this.body = body;
  }
  
  @OslcDescription("Author of comment.")
  @OslcOccurs(Occurs.ExactlyOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "commentAuthor")
  @OslcTitle("Comment author")
  public String getCommentAuthor() {
    return this.author;
  }

  public void setCommentAuthor(String name) {
    this.author = name;
  }

  @OslcDescription("Date of comment creation.")
  @OslcOccurs(Occurs.ExactlyOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "commentCreated")
  @OslcTitle("Comment creation date")
  public Date getCommentCreated() {
    return this.created;
  }

  public void setCommentCreated(Date created) {
    this.created = created;
  }
  
  @OslcDescription("Date of comment update.")
  @OslcOccurs(Occurs.ExactlyOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "commentUpdated")
  @OslcTitle("Comment update date")
  public Date getCommentUpdated() {
    return this.updated;
  }

  public void setCommentUpdated(Date updated) {
    this.updated = updated;
  }
}
