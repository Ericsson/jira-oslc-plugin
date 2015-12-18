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

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.project.Project;

/**
 * The data class which holds the IssueInput parameters, the list of error and warning messages and the project
 * in which the issue is created/updated
 *
 */
public class JiraIssueInputParameters {
  private IssueInputParameters issueInputParams;
  private ErrorSyncHandler errorSyncHandler = new ErrorSyncHandler();
  private ErrorSyncHandler warnSyncHandler = new ErrorSyncHandler();
  private Project project;

  public JiraIssueInputParameters() {
    IssueService issueService = ComponentAccessor.getIssueService();
    issueInputParams = issueService.newIssueInputParameters();
  }
  
  public JiraIssueInputParameters(IssueInputParameters inputParameters) {
    issueInputParams = inputParameters;
  }
  
  public IssueInputParameters getIssueInputParameters() {
    return issueInputParams;
  }
  
  public ErrorSyncHandler getErrorSyncHandler() {
    return errorSyncHandler;
  }

  public ErrorSyncHandler getWarnSyncHandler() {
    return warnSyncHandler;
  }

  public void setWarnSyncHandler(ErrorSyncHandler warnSyncHandler) {
    this.warnSyncHandler = warnSyncHandler;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

}
