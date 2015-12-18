package com.ericsson.jira.oslc.utils;

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
import java.util.Date;
import java.util.List;

import com.ericsson.eif.leansync.mapping.SyncConstants;

/**
 * The ErrorSyncHandler class provides the methods for storing error state messages.
 * 
 */
public class ErrorSyncHandler {
  private List<String> errorMessages = new ArrayList<String>(); 
 
  /**
   * Append simple message without date time information.  
   * @param errorMessage Message to be appended
   */
  public void addMessageWithoutDateInfo(String errorMessage) {
    if (errorMessage != null) {
      this.errorMessages.add(errorMessage );
    }
  }
  
  /**
   * Append message with date time information. The date time format will be added automaticalyThe format of message is:
   * @param errorMessage Message to be appended
   */
  public void addMessage(String errorMessage) {
    if (errorMessage != null) {
      this.addMessage(errorMessage, new Date());
    }
  }
  
  /**
   * Append message with specific Date object.  
   * @param errorMessage Error message to be appended
   * @param actualDate Data object that contains date information which will be appended
   */
  public void addMessage(String errorMessage, Date actualDate) {
    if (errorMessage != null && actualDate != null) {
      this.errorMessages.add(errorMessage + " (" + actualDate.toString() + ")");
    }
  }
  
  /**
   * Append message and problematic object name. Automatically add actual date and time to the message.  
   * @param errorMessage Error message to be appended
   * @param fieldName Problematic object name
   */
  public void addMessage(String errorMessage, String fieldName) {
    if (errorMessage != null && fieldName != null) {
      this.addMessage("Field name: " + fieldName + ", " + "ErrorMsg:" + errorMessage, new Date()); 
    }
  }
  /**
   * Get all messages
   * @return List<String> of all messages 
   */
  public List<String> getMessages() {
    return this.errorMessages;
  }
  
   /**
    * Return all messages in one String. Each message is on new line
   * @return String value of all messages. Each message is on new line
   */
  public String getMessagesAsString() {
    StringBuilder sbResult = new StringBuilder();
    
    for (String message : errorMessages) {
      sbResult.append(message);
      sbResult.append(SyncConstants.END_OF_LINE);
    }
    return sbResult.toString();
  }

  /**
   * Check if ErrorSyncHandler contains some error messages
   * @return
   */
  public boolean isLogEmpty() {
    if (!errorMessages.isEmpty()) {
      return false;
    }
    return true;
  }
  
  
  /**
   * Clear all messages in ErrorSyncHandler 
   */
  public void clearLog() {
    this.errorMessages.clear();
  }
  
  /**
   * Get count of all messages
   * @return Return count of messages
   */
  public int getCount() {
    return this.errorMessages.size();
  }
}
