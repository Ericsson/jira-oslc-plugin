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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.auth.LoginUriProvider;

/**
 * A utility class for the servlets
 *
 */
public class ServletUtils {
  private static Logger logger = LoggerFactory.getLogger(ServletUtils.class);
  
  /**
   * In case of unauthorized access redirect a user to login page
   * @param request a request
   * @param response a response
   * @throws IOException
   */
  public static void redirectToLogin(HttpServletRequest request, HttpServletResponse response,
      LoginUriProvider loginUriProvider ) throws IOException {
    response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
  }

  /**
   * Get uri from a request
   * @param request a request
   * @return uri from a request
   */
  public static URI getUri(HttpServletRequest request) {
    StringBuffer builder = request.getRequestURL();
    if (request.getQueryString() != null) {
      builder.append("?");
      builder.append(request.getQueryString());
    }
    return URI.create(builder.toString());
  }
  
  /**
   * Convert InputStream to String
   * @param is the InputStream which will be converted to the String
   * @return converted InsputStream
   */
  public static String getStringFromInputStream(InputStream is) {

    BufferedReader br = null;
    StringBuilder sb = new StringBuilder();

    String line;
    try {

      br = new BufferedReader(new InputStreamReader(is));
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }

    } catch (IOException e) {
      logger.error("GetStringFromInputStream", e);
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          logger.error("GetStringFromInputStream", e);
        }
      }
    }

    return sb.toString();
  }
  
  /**
   * Convert string value to boolean value
   * @param value the string value which will be converted to boolean
   * @return the boolean value
   */
  public static Boolean parseBoolean(String value) {
    if(value == null){
      return null;
    } else if("TRUE".equals(value.toUpperCase())){
      return true;
    } else if("FALSE".equals(value.toUpperCase())){
      return false;
    } else{
      return null;
    }
  }
  
  /**
   * Convert white spaces like \n, \r, \t to \\n, \\r, \\t 
   * @param value the string value which will be encoded
   * @return encoded string
   */
  public static String encodeWhitespaces(String value) {
    if(value != null){
      value=value.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
    return value;
  }
  
}
