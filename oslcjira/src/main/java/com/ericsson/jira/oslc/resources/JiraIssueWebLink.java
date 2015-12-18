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

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.lyo.oslc4j.core.annotation.OslcDescription;
import org.eclipse.lyo.oslc4j.core.annotation.OslcName;
import org.eclipse.lyo.oslc4j.core.annotation.OslcNamespace;
import org.eclipse.lyo.oslc4j.core.annotation.OslcOccurs;
import org.eclipse.lyo.oslc4j.core.annotation.OslcPropertyDefinition;
import org.eclipse.lyo.oslc4j.core.annotation.OslcResourceShape;
import org.eclipse.lyo.oslc4j.core.annotation.OslcTitle;
import org.eclipse.lyo.oslc4j.core.model.AbstractResource;
import org.eclipse.lyo.oslc4j.core.model.Occurs;

import com.ericsson.jira.oslc.Constants;

/**
 * It represents a web link. It's a part of JIRA Change Request
 *
 */

@OslcNamespace(Constants.JIRA_NAMESPACE)
@OslcName("IssueWebLink")
@OslcResourceShape(title = "Issue web link resource shape")
public class JiraIssueWebLink extends AbstractResource {
  
  private URI url = null;
  private String name = null;
  
  //NOTE: default c'tor without parameters must be defined. If not, jena is not able to
  // create instance for further parsing in jersey.
  public JiraIssueWebLink() {
  }
  
  public JiraIssueWebLink(URI url, String name) {
    this.url = url;
    this.name = name;
  }
  
  public JiraIssueWebLink(String url, String name) {
    this.url = null;
    this.name = name;
    try {
      this.url = new URI(url);
    } 
    catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }
  
  @OslcDescription("The Jira web link url for this change request.")
  @OslcOccurs(Occurs.ZeroOrOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "webLinkUrl")
  @OslcTitle("WebLinkUrl")
  public URI getWebLinkUrl() {
    return this.url;
  }

  public void setWebLinkUrl(URI url) {
    this.url = url;
  }
  
  @OslcDescription("The Jira web link name for this change request.")
  @OslcOccurs(Occurs.ExactlyOne)
  @OslcPropertyDefinition(Constants.JIRA_NAMESPACE + "webLinkRelation")
  @OslcTitle("WebLinkRelation")
  public String getWebLinkRelation() {
    return this.name;
  }

  public void setWebLinkRelation(String value) {
    this.name = value;
  }
  
}
