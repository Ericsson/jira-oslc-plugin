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

/**
 *
 * It represents OSLC service provider catalog. 
 *
 */
public class ServiceProviderDialog implements Comparable<ServiceProviderDialog> {
  private String type;
  private String link;
  private String width;
  private String height;
  
  public String getType() {
    return this.type;
  }
  
  public void setType(String type) {
    this.type = type;
  }
  
  public String getLink() {
    return this.link;
  }
  
  public void setLink(String link) {
    this.link = link;
  }
  
  public String getWidth() {
    return this.width;
  }
  
  public void setWidth(String w) {
    this.width = w;
  }
  
  public String getHeight() {
    return this.height;
  }
  
  public void setHeight(String h) {
    this.height = h;
  }

  @Override
  public int compareTo(ServiceProviderDialog compDlg) {
    if(this.type != null && compDlg != null){
      String compType = compDlg.getType();
      if(compType != null){
        return this.type.compareTo(compType);
      }
    }
    return 0;
  }
}
