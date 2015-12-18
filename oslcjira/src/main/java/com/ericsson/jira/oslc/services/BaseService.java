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

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.jira.oslc.exceptions.GetIssueException;
import com.ericsson.jira.oslc.exceptions.IssueValidationException;
import com.ericsson.jira.oslc.exceptions.NoResourceException;
import com.ericsson.jira.oslc.exceptions.PermissionException;
import com.ericsson.jira.oslc.exceptions.PreconditionException;
import com.ericsson.jira.oslc.exceptions.StatusException;

/**
 * General service for other the services. It contains the method for exception handling.
 *
 */
public class BaseService {
  protected static Logger logger = LoggerFactory.getLogger(BaseService.class);

  /**
   * It returns the response with a code based on the exception
   * @param e Exception
   */
  protected Response handleException(Exception e){
    String errorMessage = (e.getMessage() != null)?e.getMessage():"";
    
    if(e instanceof IssueValidationException){
      logger.warn(e.getMessage());
      return Response.status(Response.Status.FORBIDDEN).entity(errorMessage).build();
    }
    
    if (e instanceof StatusException) {
      logger.warn(e.getMessage());
      if (errorMessage.isEmpty()) {
        errorMessage = "Error during generating status information";
      }
      return Response.status(Response.Status.FORBIDDEN).entity(errorMessage).build();
    }
    
    if(e instanceof GetIssueException){
        logger.warn(e.getMessage());
        return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
    }
    
    if(e instanceof PermissionException){
      logger.warn(e.getMessage());
      if(errorMessage == null || errorMessage.isEmpty()){
        errorMessage = "Permission denied.";
      }
      return Response.status(Response.Status.FORBIDDEN).entity(errorMessage).build();
    }
    
    if(e instanceof NoResourceException){
      logger.warn(e.getMessage());
      if(errorMessage == null || errorMessage.isEmpty()){
        errorMessage = "Resource not found.";
      }
      return Response.status(Response.Status.NOT_FOUND).entity(errorMessage).build();
    }
    
    if(e instanceof PreconditionException){
      logger.warn(e.getMessage());
      if(errorMessage == null || errorMessage.isEmpty()){
        errorMessage = "Precondition failed.";
      }
      return Response.status(Response.Status.PRECONDITION_FAILED).entity(errorMessage).build();
    }

    logger.error("Error: ", e);
    if(errorMessage.isEmpty()){
      errorMessage = "Unexpected error. Please contact your administrator";
    }
    
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorMessage).build();
  }
}
