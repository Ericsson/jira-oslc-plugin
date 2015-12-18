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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The enumeration contains the names of the fields which are used in LeanSync configuration for mapping  to conctrete JIRA field
 *
 */
public enum JiraObjectMapping {
  SUMMARY("Summary"), DESCRIPTION("Description"), ASSIGNEE_ID("AssigneeId"),
  ISSUE_KEY("IssueKey"), ISSUE_TYPE_NAME("IssueTypeName"), RESOLUTION_NAME("ResolutionName"),
  REPORTER_ID("ReporterId"), CREATOR_ID("CreatorId"), PROJECT_KEY("ProjectKey"),
  PROJECT_NAME("ProjectName"), STATUS_NAME("StatusName"), STATUS_ID("StatusId"),
  COMPONENT_NAMES("ComponentNames"), AFFECTED_VERSION_NAMES("AffectedVersionNames"), FIX_VERSION_NAMES("FixVersionNames"),
  DUE_DATE("DueDate"), ORIGINAL_ESTIMATE("OriginalEstimate"), ESTIMATE("Estimate"),
  TIME_SPENT("TimeSpent"), LABEL_NAMES("LabelNames"), SUBTASK_NAMES("SubtaskNames"),
  COMMENTS("Comments"), WORK_LOG("WorkLog"), HISTORY("History"),
  RESOLUTION_DATE("ResolutionDate"), VOTER_NAMES("VoterNames"), WATCHER_NAMES("WatcherNames"),
  INWARD_LINKS("InwardLinks"), OUTWARD_LINKS("OutwardLinks"), OUTSIDE_LINKS("OutsideLinks"), 
  CREATED_DATE("CreatedDate"), UPDATED_DATE("UpdatedDate"), PRIORITY_NAME("PriorityName"),
  RESOLUTION_ID("ResolutionId"), ISSUE_TYPE_ID("IssueTypeId"), PRIORITY_ID("PriorityId"),
  ENVIRONMENT("Environment"), COMMENT("Comment");

  
  static {
    Map<String, String> result = new HashMap<String, String>();
    result.put("summary", SUMMARY.getName());
    result.put("description", DESCRIPTION.getName());
    result.put("assignee", ASSIGNEE_ID.getName());
    result.put("Key", ISSUE_KEY.getName());
    result.put("issuetype", ISSUE_TYPE_NAME.getName());
    result.put("resolution", RESOLUTION_NAME.getName());
    result.put("created", CREATOR_ID.getName());
    result.put("reporter", REPORTER_ID.getName());
    result.put("project", PROJECT_NAME.getName());
    result.put("status", STATUS_NAME.getName());
    result.put("Component", COMPONENT_NAMES.getName());
    result.put("Version", AFFECTED_VERSION_NAMES.getName());
    result.put("Fix Version", FIX_VERSION_NAMES.getName());
    result.put("duedate", DUE_DATE.getName());
    result.put("timeoriginalestimate", ORIGINAL_ESTIMATE.getName());
    result.put("timeestimate", ESTIMATE.getName());
    result.put("timespent", TIME_SPENT.getName());
    result.put("labels", LABEL_NAMES.getName());
    result.put("priority", PRIORITY_NAME.getName());
    result.put("environment", ENVIRONMENT.getName());
    
    fieldIdsToLabels = Collections.unmodifiableMap(result);
  }
  
  private static final Map<String, String> fieldIdsToLabels;
  
  public static Map<String, String> getFieldIdsToLabels() {
    return fieldIdsToLabels;
  }
  
  private String name;
  JiraObjectMapping(String name){
    this.name = name;
  }
  
  
  /**
   * Get the name of mapped field
   * @return the name of mapped field
   */
  public String getName() {
    return name;
  }
  
  /**
   * Compare the names 
   * @param name the name which will be compared
   * @return true - the names are the same, otherwise false
   */
  public boolean compare(String name) {
    if(this.name != null){
      return this.name.equals(name);
    }else if (this.name == null && name == null){
      return true;
    }
    return false;
  }

}
