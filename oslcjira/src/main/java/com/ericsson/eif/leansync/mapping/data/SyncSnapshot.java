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

/**
 * Data class contains the value of template and configuration of template field
 * It serves for saving a snapshot and for field mapping M:1
 *
 */
public class SyncSnapshot {
  //the configuration for template field
  private SyncTemplate templateConfig;
  //the value of the field which are mapped by the template
  private String value;

  public SyncSnapshot(SyncTemplate templateConfig) {
    this.templateConfig = templateConfig;
  }
  public SyncSnapshot(SyncTemplate templateConfig, String value) {
    this.templateConfig = templateConfig;
    this.value = value;
  }
  public SyncTemplate getTemplateConfig() {
    return templateConfig;
  }
  public void setTemplateConfig(SyncTemplate templateConfig) {
    this.templateConfig = templateConfig;
  }
  public String getValue() {
    return value;
  }
  public void setValue(String value) {
    this.value = value;
  }

}
