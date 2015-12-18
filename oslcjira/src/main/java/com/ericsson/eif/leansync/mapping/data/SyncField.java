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
 * Data class containing a configuration for the synchronization between fields
 *
 */
public class SyncField extends SyncGeneralField{
  //unique ID of field which is used e,g, in the template
  private String id;
  //location of the field in the xml. The path is defined by XPATH
  private String xpath;
  //true - the tags from the value of the filed will not be removed
  private boolean keepTags;
  //true - encode HTML character e.g. '<' -> '!lt;' 
  private boolean encodeHtml;
  // input value of date will be converted to this format
  private String toDateFormat;
  //specifies the date format in input value of date
  private String fromDateFormat;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getXpath() {
    return xpath;
  }

  public void setXpath(String xpath) {
    this.xpath = xpath;
  }

  public boolean isKeepTags() {
    return keepTags;
  }

  public void setKeepTags(boolean keepTags) {
    this.keepTags = keepTags;
  }

  public boolean isEncodeHtml() {
    return encodeHtml;
  }

  public void setEncodeHtml(boolean encodeHtml) {
    this.encodeHtml = encodeHtml;
  }

  public String getToDateFormat() {
    return toDateFormat;
  }

  public void setToDateFormat(String toDateFormat) {
    this.toDateFormat = toDateFormat;
  }
  
  public String getFromDateFormat() {
    return fromDateFormat;
  }

  public void setFromDateFormat(String fromDateFormat) {
    this.fromDateFormat = fromDateFormat;
  }


}