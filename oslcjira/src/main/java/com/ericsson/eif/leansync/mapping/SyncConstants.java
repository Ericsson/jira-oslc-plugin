package com.ericsson.eif.leansync.mapping;

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
 * The constants for LeanSync
 *
 */
public interface SyncConstants {
  public static final String CONFIG_CONTENT_TYPE_HTML = "html";
  public static final String CONFIG_FIELD_TYPE_CUSTOM = "custom";
  public static final String ID_PREFIX_DEFAULT_VALUE = "%";
  public static final String ID_SUFFIX_DEFAULT_VALUE = "%";
  public static final String BOOLEAN_FALSE = "false";
  public static final String BOOLEAN_TRUE = "true";
  public static final String INBOUND_SYNC_STATUS_MARK = "---Inbound sync---";
  public static final String OUTBOUND_SYNC_STATUS_MARK = "---Outbound sync---";
  public static final String END_OF_LINE = "\n";
  public static final String HTML_END_OF_LINE = "<BR/>";
}
