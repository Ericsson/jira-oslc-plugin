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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

import com.ericsson.eif.leansync.mapping.data.SyncGeneralField;


public class SyncHelper {
  
  /**
   * Encode all non alphanumeric chars are replaced with hex code
   * and surrounded by "_" characters.
   * @param name the name of tag 
   * @return encoded name
   */
  public static String encodeTagName(String name) {
    if(name == null){
      return null;
    }
    
    final int len = name.length();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len; i++) {
      Character ch = name.charAt(i);
      if (!Character.isDigit(ch) && !Character.isLetter(ch)) {
        // Replace with _hex value of ascii_
        sb.append('_');
        sb.append(Integer.toHexString((int) ch));
        sb.append('_');
      } else {
        sb.append(ch);
      }
    }
    
    return sb.toString();
  }
  
  /**
   * It creates the error information for inbound and outbound connection.
   * Inbound information are at first and then the outbound information is.
   * The section for inbound and outbound connection are
   * separated by constants INBOUND_SYNC_STATUS_MARK and OUTBOUND_SYNC_STATUS_MARK
   * If inbound information is added the current outbound information must be kept and vice-versia
   * Set status as null to clear information for defined direction (in/out)
   * @param content the current value of status
   * @param status the information whichewill be added
   * @param isInbound choice if added information is for in/out connection
   * @return sync status
   */
  public static String createSyncStatus(String content, String status,
      boolean isInbound) {

    if (content == null) {
      content = "";
    }
    StringBuilder result = new StringBuilder();

    int idxInboundMark = content.indexOf(SyncConstants.INBOUND_SYNC_STATUS_MARK);
    int idxOutboundMark = content.indexOf(SyncConstants.OUTBOUND_SYNC_STATUS_MARK);

    if (isInbound) {
      // Set inbound status
      
      if (status != null) {
        result.append(SyncConstants.INBOUND_SYNC_STATUS_MARK);
        result.append(SyncConstants.END_OF_LINE);
        result.append(status.trim());
      }
      if (idxOutboundMark >= 0) {
        result.append(SyncConstants.END_OF_LINE);
        String outBoundText = content.substring(idxOutboundMark);
        result.append(outBoundText.trim());
      }
    } else {
      // Set Outbound status
      if (idxInboundMark >= 0) {
        String inboundText = null;
        if (idxOutboundMark >= 0) {
          inboundText = content.substring(idxInboundMark,
              idxOutboundMark);
        } else {
          inboundText = content.substring(idxInboundMark);
        }
        result.append(inboundText.trim());
        result.append(SyncConstants.END_OF_LINE);
      }
      if (status != null) {
        result.append(SyncConstants.OUTBOUND_SYNC_STATUS_MARK);
        result.append(SyncConstants.END_OF_LINE);
        result.append(status.trim());
      }
    }
    return result.toString().trim();
  }
  
  /**
   * Add text to another text
   * @param text current text
   * @param textToAdd text which will be added to current text
   * @return result text
   */
  public static String appendText(String text, String textToAdd ){
    if(text == null){
      text = textToAdd;
    }else if(textToAdd == null){
      text = textToAdd;
    }else{
      text+= textToAdd;
    }
   return text;
  }

  /**
   * It maps the value to the defined value according to the configuration.
   * When the value mapping doesn't match corresponding value the default value is set if it's defined 
   * If the mapping doesn't matches the value and default value is not defined the input value is returned
   * @param value the value which will be mapped
   * @param field the configuration of field which contains value mapping and defined default value
   * @return mapped value
   */
  public static String mapToValue(String value, SyncGeneralField field){
    Map<String, String> valueMapping = field.getValueMapping();
    String defaultValue = field.getDefaultValue();
    
    if(valueMapping != null && valueMapping.containsKey(value)){
      return valueMapping.get(value);
    }else if(defaultValue != null){
      return defaultValue;
    }
    return value;
  }
  

  /**
   * Convert the list of the string to the one string where each items are separated by the end of the line
   * @param list the list of the string which will be put to one string
   * @return converted the list of string to the one string
   */
  public static String convertStringListToText(List<String> list){
    if(list == null){
      return null;
    }
    
    StringBuilder sb = new StringBuilder();
    for (String str : list) {
      sb.append(str);
      sb.append(SyncConstants.END_OF_LINE);
    }
    return sb.toString();
  }
  
  /**
   * Replace \n  for the </BR>
   */
  public static String convertToHTML(String text){
    if(text == null){
      return null;
    }
    return text.replaceAll(SyncConstants.END_OF_LINE, SyncConstants.HTML_END_OF_LINE);
  }
  
  /**
   * Escape html text 
   */
  public static String encodeHTML(String text){
    if(text == null){
      return null;
    }
    return StringEscapeUtils.escapeHtml(text);
  }
}
