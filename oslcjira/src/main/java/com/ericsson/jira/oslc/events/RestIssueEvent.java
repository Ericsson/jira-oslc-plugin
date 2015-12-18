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

import java.util.Map;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.event.AbstractEvent;
import com.atlassian.jira.issue.Issue;

/**
 * It represents custom event which are fired manually from the code e.g. when 
 * external link is added.
 *
 */
public class RestIssueEvent extends AbstractEvent {
  private Issue issue;
  private User user;
  private IssueEventType type;
  
  public RestIssueEvent(final Issue issue, final User user, IssueEventType type) {
    super();
    this.issue = issue;
    this.user = user;
    this.type = type;
  }
  
  public RestIssueEvent(final Issue issue, final User user, Map<String, Object> parameters) {
    super(parameters);
    this.issue = issue;
    this.user = user;
  }
  
  public Issue getIssue() {
      return issue;
  }

  public User getUser() {
      return user;
  }

  public IssueEventType getType() {
    return type;
  }

  public void setType(IssueEventType type) {
    this.type = type;
  }
  
  
}