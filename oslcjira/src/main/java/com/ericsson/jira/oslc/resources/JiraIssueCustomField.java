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

import org.eclipse.lyo.oslc4j.core.annotation.OslcDescription;
import org.eclipse.lyo.oslc4j.core.annotation.OslcName;
import org.eclipse.lyo.oslc4j.core.annotation.OslcNamespace;
import org.eclipse.lyo.oslc4j.core.annotation.OslcOccurs;
import org.eclipse.lyo.oslc4j.core.annotation.OslcPropertyDefinition;
import org.eclipse.lyo.oslc4j.core.annotation.OslcReadOnly;
import org.eclipse.lyo.oslc4j.core.annotation.OslcResourceShape;
import org.eclipse.lyo.oslc4j.core.annotation.OslcTitle;
import org.eclipse.lyo.oslc4j.core.model.AbstractResource;
import org.eclipse.lyo.oslc4j.core.model.Occurs;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.ericsson.jira.oslc.Constants;

/**
 * It represents a Custom field. It's a part of JIRA Change Request
 *
 */

@OslcNamespace(Constants.JIRA_NAMESPACE)
@OslcName("CustomField")
@OslcResourceShape(title = "Issue custom field resource shape", describes = Constants.JIRA_NAMESPACE + "CustomField")
public class JiraIssueCustomField extends AbstractResource {
  private String id = null;
  private String name = null;
  private String value = null;
  private String typeName = null;
  private String typeKey = null;
  
  //NOTE: default c'tor without parameters must be defined. If not, jena is not able to
  // create instance for further parsing in jersey.
  public JiraIssueCustomField() {
  }

  public JiraIssueCustomField(CustomField cf, Issue issue) {
    this.id = cf.getId();
    this.name = cf.getName();
    Object obj = cf.getValue(issue);
    if (obj != null) {
      this.value = obj.toString();
    }
    this.typeKey = cf.getCustomFieldType().getKey();
    this.typeName = cf.getCustomFieldType().getName();
  }
  
  @OslcDescription("The id of custom field.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "id")
  @OslcReadOnly
  @OslcTitle("Custom field id")
  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }
  
  @OslcDescription("The name of custom field.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "name")
  @OslcReadOnly
  @OslcTitle("Custom field name")
  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }
  
  @OslcDescription("The value of custom field.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "value")
  @OslcTitle("Custom field value")
  public String getValue() {
    return this.value;
  }

  public void setValue(String value) {
    this.value = value;
  }
  
  @OslcDescription("The type key of custom field.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "typeKey")
  @OslcReadOnly
  @OslcTitle("Custom field type key")
  public String getTypeKey() {
    return this.typeKey;
  }

  public void setTypeKey(String typeKey) {
    this.typeKey = typeKey;
  }
  
  @OslcDescription("The type (name) of custom field.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "typeName")
  @OslcReadOnly
  @OslcTitle("Custom field type name")
  public String getTypeName() {
    return this.typeName;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }
}
