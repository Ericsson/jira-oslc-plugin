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

import java.util.ArrayList;

/**
 * It represents OSLC service provider. 
 *
 */
public class ServiceProvider {
  private ArrayList<ServiceProviderDialog> creationDialogs;
  private ArrayList<ServiceProviderDialog> selectionDialogs;
  private String title;
  private String creationFactoryUri;
  
  public ServiceProvider() {
    this.creationDialogs = new ArrayList<ServiceProviderDialog>();
    this.selectionDialogs = new ArrayList<ServiceProviderDialog>();
  }
  
  public int getCreationDialogsCount() {
    return this.creationDialogs.size();
  }
  
  public int getSelectionDialogsCount() {
    return this.selectionDialogs.size();
  }
  
  public void addDialog(ServiceProviderDialog dlg, boolean isCreation) {
    if (isCreation == true) {
      this.creationDialogs.add(dlg);
    }
    else {
      this.selectionDialogs.add(dlg);
    }
  }
  
  public ServiceProviderDialog getCreationDialog(int idx) {
    if (idx >= 0 && idx < this.creationDialogs.size()) {
      return this.creationDialogs.get(idx);
    }
    return null;
  }
  
  public ServiceProviderDialog getSelectionDialog(int idx) {
    if (idx >= 0 && idx < this.selectionDialogs.size()) {
      return this.selectionDialogs.get(idx);
    }
    return null;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getCreationFactoryUri() {
    return creationFactoryUri;
  }

  public void setCreationFactory(String creationFactoryUri) {
    this.creationFactoryUri = creationFactoryUri;
  }

  public ArrayList<ServiceProviderDialog> getCreationDialogs() {
    return creationDialogs;
  }

  public ArrayList<ServiceProviderDialog> getSelectionDialogs() {
    return selectionDialogs;
  }
 
}
