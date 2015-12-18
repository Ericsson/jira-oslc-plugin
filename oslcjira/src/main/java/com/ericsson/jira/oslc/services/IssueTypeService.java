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

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;

import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.ericsson.jira.oslc.Constants;
import com.ericsson.jira.oslc.managers.FieldManager;

/**
 * A service for issue type
 *
 */
@Path("/issueTypes")
@AnonymousAllowed
public class IssueTypeService {
   
   @Context private HttpServletRequest httpServletRequest;
   
   @GET
   @AnonymousAllowed
   public Response getIssueTypes() throws Exception {
      
      String jiraServiceBase = ServiceHelper.getOslcBaseUri(httpServletRequest);
      
      StringBuilder builder = new StringBuilder();
      
      builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
      builder.append("<ns1:Description ");
      builder.append("xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" ");
      builder.append("xmlns:ns2=\"http://www.w3.org/2000/01/rdf-schema#\" ");
      builder.append("ns1:about=\"" + jiraServiceBase + "/issueTypes\">\n");
      List<String> issueTypes = FieldManager.getFilteredIssueTypes();
      if (issueTypes != null) {
        for (String id : issueTypes) {
          builder.append("  <ns2:member ns1:resource=\"");
          builder.append(jiraServiceBase);
          builder.append("/issueTypes/");
          builder.append(id); // issue type id
          builder.append("\"/>\n");
        }
      }
      builder.append("</ns1:Description>");

      return Response.ok(builder.toString()).type(OslcMediaType.APPLICATION_RDF_XML).build();
   }
   
   @GET
   @AnonymousAllowed
   @Path("{issueTypeId}")
   public Response getIssueType(@PathParam("issueTypeId") String issueTypeId) throws IOException {
      
      String jiraServiceBase = ServiceHelper.getOslcBaseUri(httpServletRequest);
      
      StringBuilder builder = new StringBuilder();
      
      builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
      builder.append("<rdf:RDF ");
      builder.append("xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" ");
      builder.append("xmlns:ns1=\""+Constants.JIRA_NAMESPACE+"\" ");
      builder.append("xmlns:ns2=\"http://purl.org/dc/terms/\">\n");
      builder.append("  <ns1:IssueType rdf:about=\"" + jiraServiceBase + "/issueTypes/" + issueTypeId + "\">\n");
      String itn = FieldManager.getIssueType(issueTypeId);
      builder.append("    <ns2:title>");
      builder.append(itn); //issue type name
      builder.append("</ns2:title>\n");
      builder.append("  </ns1:IssueType>\n");
      builder.append("</rdf:RDF>");
      
      return Response.ok(builder.toString()).type(OslcMediaType.APPLICATION_RDF_XML).build();
   }

}
