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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;

/**
 * A services for rootservices
 *
 */
@Path("/rootservices")
public class RootServicesService {
   
   @Context
   private HttpServletRequest httpServletRequest;
  
   @GET
   @AnonymousAllowed
   public Response rootservices() {

      String jiraServiceBase = ServiceHelper.getOslcBaseUri(httpServletRequest);

      StringBuilder builder = new StringBuilder();

      builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
      builder.append("<ns1:Description ");
      builder.append("xmlns:ns1=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" ");
      builder.append("xmlns:ns2=\"http://purl.org/dc/terms/\" ");
      builder.append("xmlns:ns8=\"http://open-services.net/xmlns/cm/1.0/\" ");
      builder.append("xmlns:ns9=\"http://jazz.net/xmlns/prod/jazz/jfs/1.0/\" ");
      builder.append("xmlns:ns10=\"http://jazz.net/ns/ui#\" ");
      builder.append("xmlns:ns11=\"http://jazz.net/xmlns/prod/jazz/calm/1.0/\" ");
      builder.append("xmlns:ns12=\"http://xmlns.com/foaf/0.1/\" ");
      builder.append("ns1:about=\"" + jiraServiceBase + "/rootservices\">\n");
      builder.append("    <ns2:title>OSLC Adapter/Jira Root Services</ns2:title>\n");
      builder.append("    <ns8:cmServiceProviders ns1:resource=\"" + jiraServiceBase + "/catalog/singleton\"/>\n");
      builder.append("    <ns9:oauthRequestTokenUrl ns1:resource=\"" + jiraServiceBase + "/oauth/requestToken\"/>\n");
      builder.append("    <ns9:oauthUserAuthorizationUrl ns1:resource=\"" + jiraServiceBase + "/oauth/authorize\"/>\n");
      builder.append("    <ns9:oauthAccessTokenUrl ns1:resource=\"" + jiraServiceBase + "/oauth/accessToken\"/>\n");
      builder.append("    <ns9:oauthRealmName>JIRA</ns9:oauthRealmName>\n");
      builder.append("    <ns9:oauthDomain>" + ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL) + "/</ns9:oauthDomain>\n");
      builder.append("    <ns9:oauthRequestConsumerKeyUrl ns1:resource=\"" + jiraServiceBase + "/oauth/requestKey\"/>\n");
      builder.append("</ns1:Description>");
      
      String responseBody = builder.toString();
      
      return Response.ok().entity(responseBody).header("max-age", 0).header("pragma", "no-cache")
          .header("Cache-Control", "no-cache").header("OSLC-Core-Version", 2.0)
          .header("Content-Length", responseBody.getBytes().length).type(OslcMediaType.APPLICATION_RDF_XML)
          .build();
   }
}
