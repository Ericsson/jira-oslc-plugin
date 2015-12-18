package com.ericsson.eif.leansync.mapping.data;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;

/**
 * Data class representing the configuration. There can be one or more configurations for different projects
 *
 */
public class SyncConfiguration {

  private Set<String> projects;
  private Set<String> issueTypes;
  private Set<String> domains;
  @XmlElement(name="inMappings")
  private List<SyncMapping> inMappingList;
  @XmlElement(name="outMappings")
  private List<SyncMapping> outMappingList;
  private String errorLog;
  
  public SyncConfiguration(){
    projects = new HashSet<String>();
    issueTypes = new HashSet<String>();
    domains = new HashSet<String>();
    inMappingList = new ArrayList<SyncMapping>(); 
    outMappingList = new ArrayList<SyncMapping>(); 
  }

  public Set<String> getProjects() {
    return projects;
  }

  public void setProjects(Set<String> projects) {
    this.projects = projects;
  }
  
  public void addProject(String project) {
    this.projects.add(project);
  }

  public Set<String> getIssueTypes() {
    return issueTypes;
  }

  public void setIssueTypes(Set<String> issueTypes) {
    this.issueTypes = issueTypes;
  }

  public void addIssueType(String issueType) {
    this.issueTypes.add(issueType);
  }
  
  public boolean containsIssueType(String issueType) {
    return (this.issueTypes != null && this.issueTypes.contains(issueType));
  }

  public List<SyncMapping> getInMappings() {
    return inMappingList;
  }

  public void setInMapping(List<SyncMapping> inMappings) {
    this.inMappingList = inMappings;
  }

  public List<SyncMapping> getOutMappings() {
    return outMappingList;
  }

  public void setOutMappings(List<SyncMapping> outMappings) {
    this.outMappingList = outMappings;
  }
  
  public void addOutMapping(SyncMapping mappings) {
    this.outMappingList.add(mappings);
  }
  public void addInMapping(SyncMapping mappings) {
    this.inMappingList.add(mappings);
  }
  
  public SyncMapping getFirstInMapping() {
    if(inMappingList != null && inMappingList.size() > 0){
      return inMappingList.get(0);
    }
    return null;
  }
  
  public SyncMapping getFirstOutMapping() {
    if(outMappingList != null && outMappingList.size() > 0){
      return outMappingList.get(0);
    }
    return null;
  }

  public String getErrorLog() {
    return errorLog;
  }

  public void setErrorLog(String errorLog) {
    this.errorLog = errorLog;
  }

  public Set<String> getDomains() {
    return domains;
  }

  public void setDomains(Set<String> domains) {
    this.domains = domains;
  }

}