/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *
 *     Russell Boykin       - initial API and implementation
 *     Alberto Giammaria    - initial API and implementation
 *     Chris Peters         - initial API and implementation
 *     Gianluca Bernardini  - initial API and implementation
 *     Michael Fiedler      - implementation for Bugzilla adapter
 *******************************************************************************/
package com.ericsson.jira.oslc.services;


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.lyo.oslc4j.core.annotation.OslcDialog;
import org.eclipse.lyo.oslc4j.core.annotation.OslcQueryCapability;
import org.eclipse.lyo.oslc4j.core.annotation.OslcService;
import org.eclipse.lyo.oslc4j.core.model.Compact;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;
import org.eclipse.lyo.oslc4j.core.model.ServiceProviderCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.ericsson.jira.oslc.Constants;
import com.ericsson.jira.oslc.servlet.ServiceProviderCatalogSingleton;

/**
 * A service for Service Provider Catalog
 *
 */
@OslcService(OslcConstants.OSLC_CORE_DOMAIN)
@Path("catalog")
@AnonymousAllowed
public class ServiceProviderCatalogService {

   @Context
   private HttpServletRequest httpServletRequest;
   @Context
   private HttpServletResponse httpServletResponse;
   @Context
   private UriInfo uriInfo;
   
   private static Logger logger = LoggerFactory.getLogger(ServiceProviderCatalogService.class);

   /**
    * Get the Service Provider Catalog
    * 
    * @return OSLC Service Provider Catalog
    * @throws IOException
    *            If an I/O error occurred
    * @throws URISyntaxException
    *            If an URI syntax error occurred
    */
   @GET
   @AnonymousAllowed
   @OslcDialog (
      title = "Service Provider Catalog Selection Dialog",
      label = "Service Provider Catalog Selection Dialog",
      uri = "/catalog",
      hintWidth = "1000px",
      hintHeight = "600px",
      resourceTypes = {OslcConstants.TYPE_SERVICE_PROVIDER_CATALOG},
      usages = {OslcConstants.OSLC_USAGE_DEFAULT}
   )
   @OslcQueryCapability (
      title = "Service Provider Catalog Query Capability",
      label = "Service Provider Catalog Query",
      resourceShape = OslcConstants.PATH_RESOURCE_SHAPES + "/" + OslcConstants.PATH_SERVICE_PROVIDER_CATALOG,
      resourceTypes = {OslcConstants.TYPE_SERVICE_PROVIDER_CATALOG},
      usages = {OslcConstants.OSLC_USAGE_DEFAULT}
   )
   @Produces({ OslcMediaType.APPLICATION_RDF_XML, OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON })
   public Response getServiceProviderCatalogs() throws IOException, URISyntaxException {
      httpServletResponse.addHeader("Oslc-Core-Version", "2.0");
      String forwardUri = ServiceHelper.getOslcBaseUri(httpServletRequest) + "/catalog/singleton";
      httpServletResponse.sendRedirect(forwardUri);
      return Response.seeOther(new URI(forwardUri)).build();
   }

   /**
    * Return the OSLC service provider catalog as RDF/XML, XML or JSON
    * 
    * @return A service provider catalog
    * @throws URISyntaxException
    */
   @GET
   @Path("{serviceProviderCatalogId}")
   @Produces({ OslcMediaType.APPLICATION_RDF_XML, OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON })
   @AnonymousAllowed
   public ServiceProviderCatalog getServiceProviderCatalog() throws URISyntaxException {
      
      long start = System.currentTimeMillis();
      
      ServiceProviderCatalog catalog = ServiceProviderCatalogSingleton.getServiceProviderCatalog(httpServletRequest);
      if (catalog != null) {
         httpServletResponse.addHeader(Constants.HDR_OSLC_VERSION, "2.0");
         
         logger.debug("Catalog: " + (System.currentTimeMillis() - start) + "ms");
         return catalog;
      }
      throw new WebApplicationException(Status.NOT_FOUND);
   }

   /**
    * Return the catalog singleton as OSLC compact XML
    * 
    * @return
    */
   @GET
   @Path("{serviceProviderCatalogId}")
   @Produces({ OslcMediaType.APPLICATION_X_OSLC_COMPACT_XML, OslcMediaType.APPLICATION_X_OSLC_COMPACT_JSON })
   @AnonymousAllowed
   public Compact getCompact() {
      ServiceProviderCatalog serviceProviderCatalog = ServiceProviderCatalogSingleton
            .getServiceProviderCatalog(httpServletRequest);
      if (serviceProviderCatalog != null) {
         Compact compact = new Compact();
         compact.setAbout(serviceProviderCatalog.getAbout());
         compact.setShortTitle(serviceProviderCatalog.getTitle());
         compact.setTitle(serviceProviderCatalog.getTitle());
         httpServletResponse.addHeader("Oslc-Core-Version", "2.0");
         return compact;
      }
      throw new WebApplicationException(Status.NOT_FOUND);
   }
  
}