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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import org.apache.http.Header;
import org.eclipse.lyo.oslc4j.provider.jena.OslcRdfXmlProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.jira.oslc.resources.JiraChangeRequest;

/**
 * Utility class for logging. It can create readable representaion of the incoming Change request
 * 
 */
public class LogUtils {
  private static final Logger log = LoggerFactory.getLogger(LogUtils.class);
  
  /**
   * Convert MHChangeRequest model to string format
   * @param req MH Change request model
   * @return MH Change request in string format
   * @throws IOException 
   * @throws WebApplicationException 
   * @throws MHTechnicalException
   */
  public static String createLogForModel(JiraChangeRequest req) {
    try {
      return convertModelToString(req);
    } catch (WebApplicationException e) {
      log.debug("logModel", e);
    } catch (IOException e) {
      log.debug("logModel", e);
    }
    return "";
  }
  
  
  /**
   * Convert MHChangeRequest model to string format
   * @param req MH Change request model
   * @return MH Change request in string format
   * @throws IOException 
   * @throws WebApplicationException 
   * @throws MHTechnicalException
   */
  public static String convertModelToString(JiraChangeRequest req) throws WebApplicationException, IOException {
    if(req == null){
      return "";
    }
    
    OutputStream outputStream = null;
    String rdfData = "";
    
    outputStream = new ByteArrayOutputStream();
    OslcRdfXmlProvider provider = new OslcRdfXmlProvider();
    provider.writeTo(req, JiraChangeRequest.class, JiraChangeRequest.class, null, null, null, outputStream);
    outputStream.flush();
    rdfData = outputStream.toString();

    return rdfData;
  }
  
  /**
   * It converts Change request to text representation. It also add the information about project.
   * It's serves for POST requests.
   * @param httpServletRequest HttpServletRequest
   * @param projectId ID of projects
   * @param changeRequest Change request which will be converted to text form
   * @return text representation of Change request
   */
  public  static String createPostChangeRequest(final HttpServletRequest httpServletRequest, final String projectId, final JiraChangeRequest changeRequest){
    StringBuilder sb = new StringBuilder();
    
    sb.append("Project id = ");
    sb.append(projectId);
    sb.append("\n");
    
    sb.append(createHeaders(httpServletRequest));
    
    String model = createLogForModel(changeRequest);
    sb.append(model);
    sb.append("\n");
 
    return sb.toString();
  }
  
  /**
   * It converts the header of request to text representation.
   * @param httpServletRequest HttpServletRequest
   * @return text representation of the header
   */
  public static String createHeaders(final HttpServletRequest httpServletRequest){
    StringBuilder sb = new StringBuilder();
    Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
    sb.append("Headers \n");
    
    if (headerNames != null) {
      while (headerNames.hasMoreElements()) {
        String name = headerNames.nextElement();
        String value = httpServletRequest.getHeader(name);
        sb.append("Name = ");
        sb.append(name);
        sb.append(", ");
        sb.append("Value = ");
        sb.append(value);
        sb.append("\n");
      }
    }
    return sb.toString();
  }
  
  /**
   * It converts Change request to text representation. It also add information about project.
   * It's serves for PUT requests.  It also add the information about a project, ID of updated issue and the properties
   * @param httpServletRequest HttpServletRequest
   * @param projectId ID of projects
   * @param changeRequestId ID of Change request
   * @param changeRequest Change request which will be converted to text form
   * @param propertiesString the OSLC properties
   * @param propertiesString the prefix for OSLC properties
   * @return text representation of Change request
   */
  public  static String createPutChangeRequest(HttpServletRequest httpServletRequest, final String projectId, final String changeRequestId, final JiraChangeRequest changeRequest, final String propertiesString, final String prefix){
    StringBuilder sb = new StringBuilder();
    
    sb.append("Id = ");
    sb.append(changeRequestId);
    sb.append("\n");
    
    sb.append("Project id = ");
    sb.append(projectId);
    sb.append("\n");
    
    sb.append("Properties = ");
    sb.append(propertiesString);
    sb.append("\n");
    
    sb.append("prefix= ");
    sb.append(prefix);
    sb.append("\n");
    
    sb.append(createHeaders(httpServletRequest));
    
    String model = createLogForModel(changeRequest);
    sb.append(model);
    sb.append("\n");
 
    return sb.toString();
  }
  
  /**
   * It converts Change request to text representation. It also add information about project.
   * It's serves for GET requests.  It also add the information about a project, ID of updated issue and the properties
   * @param httpServletRequest HttpServletRequest
   * @param projectId ID of projects
   * @param changeRequestId ID of Change request
   * @param changeRequest Change request which will be converted to text form
   * @param propertiesString the OSLC properties
   * @param propertiesString the prefix for OSLC properties
   * @return text representation of Change request
   */
  public  static String createGetChangeRequest(HttpServletRequest httpServletRequest, final String projectId, final String changeRequestId, final String propertiesString, final String prefix){
    StringBuilder sb = new StringBuilder();
    
    sb.append("Id = ");
    sb.append(changeRequestId);
    sb.append("\n");
    
    sb.append("Project id = ");
    sb.append(projectId);
    sb.append("\n");
    
    sb.append("Properties = ");
    sb.append(propertiesString);
    sb.append("\n");
    
    sb.append("prefix= ");
    sb.append(prefix);
    sb.append("\n");
    
    sb.append(createHeaders(httpServletRequest));
    
 
    return sb.toString();
  }
  
  /**
   * Creates the text containing information about HTTP request like URI, headers, body 
   * @param uri the URI which will be logged
   * @param body the body which will be logged
   * @param headers the headers which will be logged
   * @return the text containing information about the request like URI, headers, body  
   */
  public static String createLogMessage(String uri, String body, Header[] headers, String action){
    StringBuilder sb = new StringBuilder();
    sb.append("Action = " + action + "\n");
    sb.append("URI = " + uri + "\n");
    
    if(headers != null){
      for(Header header:headers){
        if(!"Authorization".equalsIgnoreCase(header.getName())){
          sb.append("Header name = " + header.getName() + ", value = " + header.getValue()+"\n");
        }
      }
    }

    if(body != null){
      sb.append("Body = " + body + "\n");
    }

    return sb.toString();
  }
}
