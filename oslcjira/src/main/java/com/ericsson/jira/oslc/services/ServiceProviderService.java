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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;

import org.eclipse.lyo.oslc4j.core.annotation.OslcDialog;
import org.eclipse.lyo.oslc4j.core.annotation.OslcService;
import org.eclipse.lyo.oslc4j.core.model.Compact;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;
import org.eclipse.lyo.oslc4j.core.model.ServiceProvider;

import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.ericsson.jira.oslc.resources.ServiceProviderRef;
import com.ericsson.jira.oslc.servlet.ServiceProviderCatalogSingleton;

/**
 * A service for Service provider
 *
 */
@OslcService(OslcConstants.OSLC_CORE_DOMAIN)
@Path("serviceProviders")
@AnonymousAllowed
public class ServiceProviderService {

   @Context private HttpServletRequest httpServletRequest;
   @Context private HttpServletResponse httpServletResponse;

   /**
    * RDF/XML, XML and JSON representations of an OSLC Service Provider
    * collection
    * 
    * @return
    */
   @OslcDialog(
         title = "Service Provider Selection Dialog",
         label = "Service Provider Selection Dialog",
         uri = "",
         hintWidth = "1000px",
         hintHeight = "600px",
         resourceTypes = { OslcConstants.TYPE_SERVICE_PROVIDER },
         usages = { OslcConstants.OSLC_USAGE_DEFAULT }
   )

   @GET
   @Produces({ OslcMediaType.APPLICATION_RDF_XML, OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON })
   @AnonymousAllowed
   public ServiceProvider[] getServiceProviders() {
      httpServletResponse.addHeader("Oslc-Core-Version", "2.0");
      return ServiceProviderCatalogSingleton.getServiceProviders(httpServletRequest);
   }

   /**
    * RDF/XML, XML and JSON representations of a single OSLC Service Provider
    * 
    * @param serviceProviderId
    * @return
    */
   @GET
   @Path("{serviceProviderId}")
   @Produces({ OslcMediaType.APPLICATION_RDF_XML, OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON })
   @AnonymousAllowed
   public ServiceProvider getServiceProvider(@PathParam("serviceProviderId") final String serviceProviderId) {
      httpServletResponse.addHeader("Oslc-Core-Version", "2.0");
      ServiceProvider serviceProvider = ServiceProviderCatalogSingleton.getServiceProvider(httpServletRequest, serviceProviderId);
      if(serviceProvider instanceof ServiceProviderRef){
        ((ServiceProviderRef)serviceProvider).setReduced(false);
      }
      return serviceProvider;
   }
   
   /**
    * Get the details about Service provider like OSLC creation and selection dialogs ...
    * @param serviceProviderId
    * @return
    */
   @GET
   @Path("{serviceProviderId}/details")
   @Produces({ OslcMediaType.APPLICATION_RDF_XML, OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON })
   @AnonymousAllowed
   public ServiceProvider getServiceProviderDetails(@PathParam("serviceProviderId") final String serviceProviderId) {
      httpServletResponse.addHeader("Oslc-Core-Version", "2.0");
      ServiceProvider serviceProvider = ServiceProviderCatalogSingleton.getServiceProvider(httpServletRequest, serviceProviderId);
      if(serviceProvider instanceof ServiceProviderRef){
        ((ServiceProviderRef)serviceProvider).setReduced(false);
      }
      return serviceProvider;
   }

   /**
    * OSLC compact XML representation of a single OSLC Service Provider
    * 
    * @param serviceProviderId
    * @return
    */
   @GET
   @Path("{serviceProviderId}")
   @Produces({ OslcMediaType.APPLICATION_X_OSLC_COMPACT_XML, OslcMediaType.APPLICATION_X_OSLC_COMPACT_JSON })
   @AnonymousAllowed
   public Compact getCompact(@PathParam("serviceProviderId") final String serviceProviderId) {
      final ServiceProvider serviceProvider = ServiceProviderCatalogSingleton.getServiceProvider(httpServletRequest,
            serviceProviderId);
      if (serviceProvider != null) {
         final Compact compact = new Compact();
         compact.setAbout(serviceProvider.getAbout());
         compact.setShortTitle(serviceProvider.getTitle());
         compact.setTitle(serviceProvider.getTitle());
         httpServletResponse.addHeader("Oslc-Core-Version", "2.0");
         return compact;
      }
      throw new WebApplicationException(Status.NOT_FOUND);
   }

}
