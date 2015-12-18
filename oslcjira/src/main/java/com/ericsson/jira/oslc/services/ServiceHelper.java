package com.ericsson.jira.oslc.services;

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

import javax.servlet.http.HttpServletRequest;

/**
 * Utility class for services
 *
 */
public class ServiceHelper {

   public static String getOslcBaseUri(HttpServletRequest request) {
      return getJiraBaseUri(request) + "/rest/jirarestresource/1.0";
   }
   
   /**
    * Gets base URI of JIRA server
    * @param request HttpServletRequest
    * @return ase URI of JIRA server
    */
   public static String getJiraBaseUri(HttpServletRequest request) {
      StringBuilder builder = new StringBuilder();

      builder.append(request.getScheme());
      builder.append("://");
      builder.append(request.getServerName());

      if (request.getServerPort() != 80 && request.getServerPort() != 443) {
         builder.append(":");
         builder.append(request.getServerPort());
      }

      builder.append(request.getContextPath());
      return builder.toString();
   }

   private ServiceHelper() {
      // Prevent instantiation
   }

}
