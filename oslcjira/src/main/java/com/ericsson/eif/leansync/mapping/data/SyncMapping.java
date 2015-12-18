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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Data class for mapping between the fields. There can be two mapping for incoming and outgoing connections  
 *
 */
public class SyncMapping {
  //the http headers which are put to the header of outgoing request
  private Map<String, String> headers;
  //it specifies rdf types of the outgoing content
  private Set<String> rdfTypes;
  //it's used for basic auth to the remote system
  private String username;
  //it's used for basic auth to the remote system
  private String password;
  //sync configuration for field mapping  
  private List<SyncField> fields;
  //sync configuration for field mapping - the fields are saved in xml
  private List<SyncXmlFieldConfig> xmlFieldConfigs;
  //for mapping M:1
  private List<SyncTemplate> templates;
  
  public SyncMapping(){
    headers = new HashMap<String, String>();
    rdfTypes = new HashSet<String>();
    fields = new ArrayList<SyncField>();
    xmlFieldConfigs = new ArrayList<SyncXmlFieldConfig>();
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setTemplates(List<SyncTemplate> templates) {
    this.templates = templates;
  }

  public Set<String> getRdfTypes() {
    return rdfTypes;
  }

  public void setRdfTypes(Set<String> rdfTypes) {
    this.rdfTypes = rdfTypes;
  }
  
  public void addTemplate(SyncXmlFieldConfig config){
    if(xmlFieldConfigs == null){
      xmlFieldConfigs = new ArrayList<SyncXmlFieldConfig>();
    }
    xmlFieldConfigs.add(config);
  }
  
  public void addField(SyncField field){
    if(fields == null){
      fields = new ArrayList<SyncField>();
    }
    fields.add(field);
  }
  
  public void addTemplate(SyncTemplate template){
    if(templates == null){
      templates = new ArrayList<SyncTemplate>();
    }
    templates.add(template);
  }
  
  public void addXmlFieldConfig(SyncXmlFieldConfig xmlFieldConfig){
    if(xmlFieldConfigs == null){
      xmlFieldConfigs = new ArrayList<SyncXmlFieldConfig>();
    }
    xmlFieldConfigs.add(xmlFieldConfig);
  }
  
  public void addHeader(String name, String value){
    if(headers == null){
      headers = new HashMap<String, String>();
    }
    headers.put(name, value);
  }
  
  public void addRdfType(String value){
    if(rdfTypes == null){
      rdfTypes = new HashSet<String>();
    }
    rdfTypes.add(value);
  }

  public List<SyncField> getFields() {
    return fields;
  }

  public void setFields(List<SyncField> fields) {
    this.fields = fields;
  }

  public List<SyncXmlFieldConfig> getXmlFieldConfigs() {
    return xmlFieldConfigs;
  }

  public void setXmlFieldConfigs(List<SyncXmlFieldConfig> xmlFieldConfigs) {
    this.xmlFieldConfigs = xmlFieldConfigs;
  }

  public List<SyncTemplate> getTemplates() {
    return templates;
  }
}
