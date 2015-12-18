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

import com.ericsson.eif.leansync.mapping.SyncConstants;

/**
 * Data class contains the template for the field mapping M:1 or the snapshot
 *
 */
public class SyncTemplate extends SyncGeneralField{
  //the template containing special marks for inserting the field values
  private String template;
  //prefix for id of the field in the template
  private String idPrefix;
  //suffix for id of the field in the template
  private String idSuffix;
  //true - the mapped value of the field will be save even when the error accurs
  private String alwaysSave;  
  
  public SyncTemplate(){
    idPrefix = SyncConstants.ID_PREFIX_DEFAULT_VALUE;
    idSuffix = SyncConstants.ID_SUFFIX_DEFAULT_VALUE;
  }

  public String getTemplate() {
    return template;
  }
  public void setTemplate(String template) {
    this.template = template;
  }
  public String getIdPrefix() {
    return idPrefix;
  }
  public void setIdPrefix(String idPrefix) {
    this.idPrefix = idPrefix;
  }
  public String getIdSuffix() {
    return idSuffix;
  }
  public void setIdSuffix(String idSuffix) {
    this.idSuffix = idSuffix;
  }

  public String getAlwaysSave() {
    return alwaysSave;
  }

  public void setAlwaysSave(String alwaysSave) {
    this.alwaysSave = alwaysSave;
  }

}
