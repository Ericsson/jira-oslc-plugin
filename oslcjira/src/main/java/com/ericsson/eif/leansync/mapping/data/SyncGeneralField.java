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

import java.util.Map;

/**
 * Data class contains the common parameters for the fields of configuration 
 *
 */
public abstract class SyncGeneralField {
  //the namespace of the field which will be mapped
  private String ns;
  //the name of the field whihc will be mapped
  private String name;
  //the type of field e.g. custom -> specifies the the field is custom and not general field of the system
  private String fieldType;
  //the type of field e.g. html - the characters "\n" will be replaced by </BR> in outgoing data
  private String contentType;
  //The name of the field where the value will be put
  private String mapTo;
  //The type of action with resource - create or update
  private ActionType action;
  //the value can be mapped to another value e.g. for priority: A->1. B->2, C->3
  private Map<String, String> valueMapping;
  //true - when the field is change then the change is propagated to a remote system 
  private boolean notifyChange = true;
  //when the value mapping doesn't match the value then the default value is used
  private String defaultValue;
  
  public String getNs() {
    return ns;
  }
  public void setNs(String ns) {
    this.ns = ns;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public String getFieldType() {
    return fieldType;
  }
  public void setFieldType(String fieldType) {
    this.fieldType = fieldType;
  }
  public String getContentType() {
    return contentType;
  }
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }
  public String getMapTo() {
    return mapTo;
  }
  public void setMapTo(String mapTo) {
    this.mapTo = mapTo;
  }
  public Map<String, String> getValueMapping() {
    return valueMapping;
  }
  public void setValueMapping(Map<String, String> valueMapping) {
    this.valueMapping = valueMapping;
  }
  public ActionType getAction() {
    return action;
  }
  public void setAction(ActionType action) {
    this.action = action;
  }
  public boolean isNotifyChange() {
    return notifyChange;
  }
  public void setNotifyChange(boolean notifyChange) {
    this.notifyChange = notifyChange;
  }
  public String getDefaultValue() {
    return defaultValue;
  }
  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }
  
}
