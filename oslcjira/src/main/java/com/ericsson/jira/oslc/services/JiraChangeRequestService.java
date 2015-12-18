/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation.
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
 *     Michael Fiedler     - initial API and implementation for Bugzilla adapter
 *     
 *******************************************************************************/
package com.ericsson.jira.oslc.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.lyo.core.query.ParseException;
import org.eclipse.lyo.core.query.Properties;
import org.eclipse.lyo.core.query.QueryUtils;
import org.eclipse.lyo.oslc4j.core.OSLC4JConstants;
import org.eclipse.lyo.oslc4j.core.annotation.OslcCreationFactory;
import org.eclipse.lyo.oslc4j.core.annotation.OslcNamespaceDefinition;
import org.eclipse.lyo.oslc4j.core.annotation.OslcQueryCapability;
import org.eclipse.lyo.oslc4j.core.annotation.OslcSchema;
import org.eclipse.lyo.oslc4j.core.annotation.OslcService;
import org.eclipse.lyo.oslc4j.core.model.Compact;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;
import org.eclipse.lyo.oslc4j.core.model.Preview;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.ericsson.jira.oslc.Constants;
import com.ericsson.jira.oslc.constants.JiraConstants;
import com.ericsson.jira.oslc.exceptions.NoResourceException;
import com.ericsson.jira.oslc.exceptions.PermissionException;
import com.ericsson.jira.oslc.managers.FieldManager;
import com.ericsson.jira.oslc.managers.JiraManager;
import com.ericsson.jira.oslc.provider.ResponseArrayWrapper;
import com.ericsson.jira.oslc.resources.ChangeRequest;
import com.ericsson.jira.oslc.resources.JiraChangeRequest;
import com.ericsson.jira.oslc.resources.JiraHistoryRequest;
import com.ericsson.jira.oslc.servlet.ServiceProviderCatalogSingleton;
import com.ericsson.jira.oslc.utils.LogUtils;

/**
 * A service for JIRA Change Request. The request represents JIRA issue
 *
 */
@OslcService(Constants.CHANGE_MANAGEMENT_DOMAIN)
@Path("{projectId}/changeRequests")
@AnonymousAllowed
public class JiraChangeRequestService extends BaseService{
   private static final String CURRENT_CLASS = "JiraChangeRequestService";
   private TemplateRenderer templateRenderer;

   @Context private HttpServletRequest httpServletRequest;
   @Context private HttpServletResponse httpServletResponse;
   @Context private UriInfo uriInfo;
	
   public JiraChangeRequestService(TemplateRenderer templateRenderer) {
     super();
      this.templateRenderer = templateRenderer;
   }

    @OslcQueryCapability
    (
        title = "Change Request Query Capability",
        label = "Change Request Catalog Query",
        resourceShape = OslcConstants.PATH_RESOURCE_SHAPES + "/" + Constants.PATH_CHANGE_REQUEST,
        resourceTypes = {Constants.TYPE_CHANGE_REQUEST},
        usages = {OslcConstants.OSLC_USAGE_DEFAULT}
    )
    
  /**
   * RDF/XML, XML and JSON representation of a change request collection
   * 
   * 
   * @param projectId
   * @param where
   * @param select
   * @param prefix
   * @param pageString
   * @param orderBy
   * @param searchTerms
   * @param paging
   * @param pageSize
   * @return
   * @throws IOException
   * @throws ServletException
   */
  @GET
  @Produces({ OslcMediaType.APPLICATION_RDF_XML, OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON })
  @AnonymousAllowed
  public Response getChangeRequests(@PathParam("projectId") final String projectId, @QueryParam("oslc.where") final String where, @QueryParam("oslc.select") final String select, @QueryParam("oslc.prefix") final String prefix, @QueryParam("page") final String pageString, @QueryParam("oslc.orderBy") final String orderBy, @QueryParam("oslc.searchTerms") final String searchTerms, @QueryParam("oslc.paging") final String paging, @QueryParam("oslc.properties") final String propertiesString, @QueryParam("oslc.pageSize") final String pageSize) throws IOException, ServletException, URISyntaxException {
    try {
      String currentMethod = "getChangeRequests";
      logger.debug(getClass().getName() + ":getChangeRequests");
      boolean isPaging = false;

      if (paging != null) {
        isPaging = Boolean.parseBoolean(paging);
      }

      int limit = 10;
      if (isPaging && pageSize != null) {
        limit = Integer.parseInt(pageSize);
      }

      Map<String, String> prefixMap = QueryUtils.parsePrefixes(prefix);
      addDefaultPrefixes(prefixMap);

      Properties filterProperties;
      if (propertiesString == null) {
        filterProperties = QueryUtils.WILDCARD_PROPERTY_LIST;
      } else {
        filterProperties = QueryUtils.parseSelect(propertiesString, prefixMap);
      }

      final List<JiraChangeRequest> results = JiraManager.getIssuesByProject(httpServletRequest, projectId);
      Object nextPageAttr = httpServletRequest.getAttribute(Constants.NEXT_PAGE);

      if (!isPaging && nextPageAttr != null) {
        String location = uriInfo.getBaseUri().toString() + uriInfo.getPath() + '?' + (where != null ? ("oslc.where=" + URLEncoder.encode(where, "UTF-8") + '&') : "") + (select != null ? ("oslc.select=" + URLEncoder.encode(select, "UTF-8") + '&') : "") + (prefix != null ? ("oslc.prefix=" + URLEncoder.encode(prefix, "UTF-8") + '&') : "") + (orderBy != null ? ("oslc.orderBy=" + URLEncoder.encode(orderBy, "UTF-8") + '&') : "") + (searchTerms != null ? ("oslc.searchTerms=" + URLEncoder.encode(searchTerms, "UTF-8") + '&') : "") + "oslc.paging=true&oslc.pageSize=" + limit;
        try {
          throw new WebApplicationException(Response.temporaryRedirect(new URI(location)).build());
        } catch (URISyntaxException e) {
          logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
          throw new IllegalStateException(e);
        }
      }

      Map<String, Object> filterPropertiesMap = QueryUtils.invertSelectedProperties(filterProperties);
      filterPropertiesMap = (filterPropertiesMap == null || filterPropertiesMap.isEmpty()) ? null : filterPropertiesMap;
      httpServletRequest.setAttribute(OSLC4JConstants.OSLC4J_SELECTED_PROPERTIES, filterPropertiesMap);

      if (nextPageAttr != null) {
        String location = uriInfo.getBaseUri().toString() + uriInfo.getPath() + '?' + (where != null ? ("oslc.where=" + URLEncoder.encode(where, "UTF-8") + '&') : "") + (select != null ? ("oslc.select=" + URLEncoder.encode(select, "UTF-8") + '&') : "") + (prefix != null ? ("oslc.prefix=" + URLEncoder.encode(prefix, "UTF-8") + '&') : "") + (orderBy != null ? ("oslc.orderBy=" + URLEncoder.encode(orderBy, "UTF-8") + '&') : "") + (searchTerms != null ? ("oslc.searchTerms=" + URLEncoder.encode(searchTerms, "UTF-8") + '&') : "") + "oslc.paging=true&oslc.pageSize=" + limit + "&page=" + nextPageAttr;
        httpServletRequest.setAttribute(OSLC4JConstants.OSLC4J_NEXT_PAGE, location);
      }

      JiraChangeRequest[] array = results.toArray(new JiraChangeRequest[results.size()]);
      ResponseArrayWrapper<JiraChangeRequest> wrapper = new ResponseArrayWrapper<JiraChangeRequest>();
      wrapper.setResource(array);
      return Response.ok(wrapper).header(Constants.HDR_OSLC_VERSION, Constants.OSLC_VERSION_V2).build();
    } catch (Exception e) {
      logger.error(CURRENT_CLASS + ". getChangeRequests - Exception: " + e.getMessage());
      return handleException(e);
    }
  }
    
  /**
   * Adds default prefixes to the map 
   * @param prefixMap the of prefixes
   */
  private static void addDefaultPrefixes(final Map<String, String> prefixMap) {
    recursivelyCollectNamespaceMappings(prefixMap, JiraChangeRequest.class);
  } 

  /**
   * Collects namespaces and puts them to the map
   * @param prefixMap the map of prefixes
   * @param resourceClass resource
   */
  private static void recursivelyCollectNamespaceMappings(final Map<String, String> prefixMap, final Class<? extends Object> resourceClass) {
    final OslcSchema oslcSchemaAnnotation = resourceClass.getPackage().getAnnotation(OslcSchema.class);

    if (oslcSchemaAnnotation != null) {
      final OslcNamespaceDefinition[] oslcNamespaceDefinitionAnnotations = oslcSchemaAnnotation.value();

      for (final OslcNamespaceDefinition oslcNamespaceDefinitionAnnotation : oslcNamespaceDefinitionAnnotations) {
        final String prefix = oslcNamespaceDefinitionAnnotation.prefix();
        final String namespaceURI = oslcNamespaceDefinitionAnnotation.namespaceURI();

        prefixMap.put(prefix, namespaceURI);
      }
    }

    final Class<?> superClass = resourceClass.getSuperclass();
    if (superClass != null) {
      recursivelyCollectNamespaceMappings(prefixMap, superClass);
    }

    final Class<?>[] interfaces = resourceClass.getInterfaces();

    if (interfaces != null) {
      for (final Class<?> interfac : interfaces) {
        recursivelyCollectNamespaceMappings(prefixMap, interfac);
      }
    }
  }
    


	/**
	 * RDF/XML, XML and JSON representation of a single change request
	 * 
	 * @param projectId
	 * @param changeRequestId
	 * @param propertiesString
	 * @param prefix
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 * @throws URISyntaxException
	 */
    @GET
    @Path("{changeRequestId}")
    @Produces({OslcMediaType.APPLICATION_RDF_XML, OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON})
    @AnonymousAllowed
  public Response getChangeRequest(@PathParam("projectId") final String projectId, @PathParam("changeRequestId") final String changeRequestId, @QueryParam("oslc.properties") final String propertiesString, @QueryParam("oslc.prefix") final String prefix) throws IOException, ServletException, URISyntaxException {
    String currentMethod = "getChangeRequest";
    logger.info(CURRENT_CLASS + "." + currentMethod);
    
    String logMsg = LogUtils.createGetChangeRequest(httpServletRequest,projectId, changeRequestId, propertiesString, prefix);
    logger.debug(CURRENT_CLASS + "." + currentMethod + logMsg);

    Map<String, String> prefixMap;
    try {

      prefixMap = QueryUtils.parsePrefixes(prefix);
      addDefaultPrefixes(prefixMap);
      Properties properties;

      if (propertiesString == null) {
        properties = QueryUtils.WILDCARD_PROPERTY_LIST;
      } else {
        properties = QueryUtils.parseSelect(propertiesString, prefixMap);
      }

      JiraChangeRequest changeRequest = JiraManager.getIssueById(httpServletRequest, changeRequestId);

      changeRequest.setServiceProvider(ServiceProviderCatalogSingleton.getServiceProvider(httpServletRequest, projectId).getAbout());
      changeRequest.setAbout(getAboutURI(projectId + "/changeRequests/" + changeRequest.getIdentifier()));
      setETagHeader(getETagFromChangeRequest(changeRequest), httpServletResponse);

      Map<String, Object> filterProperties = QueryUtils.invertSelectedProperties(properties);
      filterProperties = (filterProperties == null || filterProperties.isEmpty()) ? null : filterProperties;
      httpServletRequest.setAttribute(OSLC4JConstants.OSLC4J_SELECTED_PROPERTIES, filterProperties);

      logMsg = LogUtils.createLogForModel(changeRequest);
      logger.debug(CURRENT_CLASS + "." + currentMethod + " - Response: " + logMsg);
      
      return Response.ok(changeRequest).header(Constants.HDR_OSLC_VERSION, Constants.OSLC_VERSION_V2).build();
    } catch (Exception e) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      return handleException(e);
    }
  }

   /**
    * OSLC Compact representation of a single change request
    * 
    * Contains a reference to the smallPreview method in this class for the
    * preview document
    * 
    * @param projectId
    * @param changeRequestId
    * @return
    * @throws URISyntaxException
    * @throws IOException
    * @throws ServletException
   * @throws PermissionException 
   * @throws NoResourceException 
    */
   @GET
   @Path("{changeRequestId}")
   @Produces({ OslcMediaType.APPLICATION_X_OSLC_COMPACT_XML })
   @AnonymousAllowed
   public Response getCompact(@PathParam("projectId") final String projectId,
         @PathParam("changeRequestId") final String changeRequestId) throws URISyntaxException, IOException,
         ServletException, PermissionException, NoResourceException {
     String currentMethod = "getCompact";
     try{
         JiraChangeRequest changeRequest = JiraManager.getIssueById(httpServletRequest, changeRequestId);
 
         final Compact compact = new Compact();
         compact.setAbout(getAboutURI(projectId + "/changeRequests/" + changeRequest.getIdentifier()));
         compact.setTitle(changeRequest.getTitle());
         String iconUri = ServiceHelper.getJiraBaseUri(httpServletRequest) + JiraConstants.ISSUE_ICON;
         compact.setIcon(new URI(iconUri));
         // Create and set attributes for OSLC preview resource
         final Preview smallPreview = new Preview();
         smallPreview.setHintHeight("11em");
         smallPreview.setHintWidth("45em");
         smallPreview.setDocument(new URI(compact.getAbout().toString() + "/smallPreview"));
         compact.setSmallPreview(smallPreview);
         // Use the HTML representation of a change request as the large preview
         // as well
         final Preview largePreview = new Preview();
         largePreview.setHintHeight("11em");
         largePreview.setHintWidth("45em");
         //now the large preview is the same as small preview
         largePreview.setDocument(new URI(compact.getAbout().toString() + "/smallPreview"));
         compact.setLargePreview(largePreview);
         return Response.ok(compact).header(Constants.HDR_OSLC_VERSION, Constants.OSLC_VERSION_V2).build();
     }catch (Exception e) {
       logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
       return handleException(e);
     }
   }

  /**
  * OSLC small preview for a single change request
  * 
  * @param projectId
  * @param changeRequestId
  * @throws ServletException
  * @throws IOException
  * @throws URISyntaxException
  */
  @GET
  @Path("{changeRequestId}/smallPreview")
  @Produces({ MediaType.TEXT_HTML })
  @AnonymousAllowed
  public Response smallPreview(@PathParam("projectId") final String projectId,
                               @PathParam("changeRequestId") final String changeRequestId) 
                               throws ServletException, IOException, URISyntaxException {
    
    final IssueManager issueManager = ComponentAccessor.getIssueManager();
    final MutableIssue issue = issueManager.getIssueObject(changeRequestId); 
    
    if (issue != null) {
      SimpleDateFormat format = new SimpleDateFormat();
      Map<String, Object> context = new HashMap<String, Object>();
      
      // Put field values in velocity template context
      if(issue.getStatusObject() != null && issue.getStatusObject().getName() != null){
        context.put("status", issue.getStatusObject().getName());
      }else{
        context.put("status", "");
      }
      
      if(issue.getProjectObject() != null && issue.getProjectObject().getName()!= null){
        context.put("project", issue.getProjectObject().getName());
      }else{
        context.put("project", "");
      }
      if(issue.getAssignee() != null && issue.getAssignee().getDisplayName() != null){
        context.put("assignee", issue.getAssignee().getDisplayName());
      }else{
        context.put("assignee", "");
      }
      if(issue.getPriorityObject() != null && issue.getPriorityObject().getName() != null){
        context.put("priority", issue.getPriorityObject().getName());
      }else{
        context.put("priority", "");
      }
      if(issue.getCreated() != null){
        context.put("reported", format.format(issue.getCreated()));
      }else{
        context.put("reported", "");
      }
      if(issue.getUpdated() != null){
        context.put("modified", format.format(issue.getUpdated()));
      }else{
        context.put("modified", "");
      }
      if(issue.getResolutionObject() != null && issue.getResolutionObject().getName() != null){
        context.put("resolution", issue.getResolutionObject().getName());
      }else{
        context.put("resolution", "");
      }
      if(issue.getCreatorId() != null){
        context.put("creator", issue.getCreatorId());
      }else{
        context.put("creator", "");
      }

      templateRenderer.render("templates/small_preview.vm", context, httpServletResponse.getWriter());
    }
    return Response.ok().build();
  }

	/**
	 * Create a single JiraChangeRequest via RDF/XML, XML or JSON POST
	 * @param projectId
	 * @param changeRequest
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 */
    @OslcCreationFactory
    (
         title = "Change Request Creation Factory",
         label = "Change Request Creation",
         resourceShapes = {OslcConstants.PATH_RESOURCE_SHAPES + "/" + Constants.PATH_CHANGE_REQUEST},
         resourceTypes = {Constants.TYPE_CHANGE_REQUEST},
         usages = {OslcConstants.OSLC_USAGE_DEFAULT}
    )
    @POST
    @Consumes({OslcMediaType.APPLICATION_RDF_XML, OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON})
    @Produces({OslcMediaType.APPLICATION_RDF_XML, OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON})
    @AnonymousAllowed
    public Response addChangeRequest(@PathParam("projectId") final String projectId,
                                                             final JiraChangeRequest changeRequest) 
                                     throws IOException, ServletException, URISyntaxException {
      String currentMethod = "addChangeRequest";
      logger.info(CURRENT_CLASS + "." + currentMethod);
      
      String logMsg = LogUtils.createPostChangeRequest(httpServletRequest, projectId, changeRequest);
      logger.debug(CURRENT_CLASS + "." + currentMethod + logMsg);
      
    	//Create a new Bug from the incoming change request, retrieve the bug and then convert to a JiraChangeRequest
      try {
        String syncType = httpServletRequest.getHeader(JiraConstants.SYNC_HEADER_NAME);
        Long newIssueId = JiraManager.createIssue(httpServletRequest, changeRequest, projectId, syncType);

        JiraChangeRequest newChangeRequest = JiraManager.getIssueById(httpServletRequest, newIssueId);
        URI about = getAboutURI(projectId + "/changeRequests/" + newChangeRequest.getIdentifier());
        newChangeRequest.setServiceProvider(ServiceProviderCatalogSingleton.getServiceProvider(httpServletRequest, projectId).getAbout());
        newChangeRequest.setAbout(about);
        setETagHeader(getETagFromChangeRequest(newChangeRequest), httpServletResponse);
        
        logMsg = LogUtils.createLogForModel(changeRequest);
        logger.debug(CURRENT_CLASS + "." + currentMethod + " - Response: " + logMsg);
        
        return Response.created(about).entity(newChangeRequest).header(Constants.HDR_OSLC_VERSION, Constants.OSLC_VERSION_V2).build();
      } catch (Exception e) {
    	logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
        return handleException(e);
      }

        
    }

    /**
     * Backend creator for the OSLC delegated creation dialog. 
     * 
     * Accepts the input in FormParams and returns a small JSON response
     * 
     * @param projectId
     * @param component
     * @param version
     * @param summary
     * @param op_sys
     * @param platform
     * @param description
     */
    @POST
    @Path("creator") 
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED})
    @AnonymousAllowed
    public void createHtmlChangeRequest(    @PathParam("projectId")   final String projectId,     
                						    @FormParam("component")   final String component,
                						    @FormParam("version")     final String version,
                						    @FormParam("summary")     final String summary,
                						    @FormParam("op_sys")      final String op_sys,
                						    @FormParam("platform")    final String platform,
                						    @FormParam("description") final String description) {
    	String currentMethod = "createHtmlChangeRequest";
    	try {
    		JiraChangeRequest changeRequest = new JiraChangeRequest(); 

    		changeRequest.setTitle(summary);
    		changeRequest.setDescription(description);
    	
    		String syncType = httpServletRequest.getHeader(JiraConstants.SYNC_HEADER_NAME);
    		final Long newBugId = JiraManager.createIssue(httpServletRequest, changeRequest, projectId, syncType);
    		
    		final JiraChangeRequest newChangeRequest = JiraManager.getIssueById(httpServletRequest, newBugId);
    		URI about = getAboutURI(projectId + "/changeRequests/" + newBugId);
            newChangeRequest.setAbout(about);
    		
    		httpServletRequest.setAttribute("changeRequest", newChangeRequest);
    		httpServletRequest.setAttribute("changeRequestUri", newChangeRequest.getAbout().toString());
    		
    		// Send back to the form a small JSON response
    		httpServletResponse.setContentType("application/json");
    		httpServletResponse.setStatus(Status.CREATED.getStatusCode());
    		httpServletResponse.addHeader("Location", newChangeRequest.getAbout().toString());
    		PrintWriter out = httpServletResponse.getWriter();
    		out.print("{\"title\": \"Bug " + newBugId + ": " + summary + "\"," +
            "\"resource\" : \"" + about + "\"}"); 
    		out.close();

    	} catch (Exception e) {
    		e.printStackTrace();
    		logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
    		throw new WebApplicationException(e);
    	}

    }

  /**
   * Updates JIra issue  
   * @param projectId the id of project
   * @param changeRequestId the id of Change Request
   * @param changeRequest JIRA Change Request
   * @param propertiesString OSLC properties
   * @param prefix the prefixes of parameters
   * @return the response of the request
   * @throws Exception
   */
  @PUT
  @Path("{changeRequestId}")
  @Consumes({ OslcMediaType.APPLICATION_RDF_XML, OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON })
  @Produces({ OslcMediaType.APPLICATION_RDF_XML, OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON })
  @AnonymousAllowed
  public Response updateChangeRequest(@PathParam("projectId") final String projectId, @PathParam("changeRequestId") final String changeRequestId, final JiraChangeRequest changeRequest, @QueryParam("oslc.properties") final String propertiesString, @QueryParam("oslc.prefix") final String prefix) throws Exception {
	String currentMethod = "updateChangeRequest";
	logger.info(CURRENT_CLASS + "." + currentMethod);
	
  String logMsg = LogUtils.createPutChangeRequest(httpServletRequest,projectId, changeRequestId,changeRequest, propertiesString, prefix);
  logger.debug(CURRENT_CLASS + "." + currentMethod + logMsg);
  
    try {
      Map<String, String> prefixMap;

      try {
        prefixMap = QueryUtils.parsePrefixes(prefix);
      } catch (ParseException e) {
        throw new IOException(e);
      }
      addDefaultPrefixes(prefixMap);
      
      Properties properties;
      if (propertiesString == null) {
        properties = QueryUtils.WILDCARD_PROPERTY_LIST;
      } else {
        try {
          properties = QueryUtils.parseSelect(propertiesString, prefixMap);
        } catch (ParseException e) {
        logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
          throw new IOException(e);
        }
      }
      Map<String, Object> selectedProperties = QueryUtils.invertSelectedProperties(properties);
      
      String syncType = httpServletRequest.getHeader(JiraConstants.SYNC_HEADER_NAME);
 
      JiraManager.updateIssue(httpServletRequest, changeRequest, changeRequestId, selectedProperties, syncType);
      JiraChangeRequest updatedChangeRequest = JiraManager.getIssueById(httpServletRequest, changeRequestId);

      updatedChangeRequest.setServiceProvider(ServiceProviderCatalogSingleton.getServiceProvider(httpServletRequest, projectId).getAbout());
      updatedChangeRequest.setAbout(getAboutURI(projectId + "/changeRequests/" + updatedChangeRequest.getIdentifier()));
      setETagHeader(getETagFromChangeRequest(updatedChangeRequest), httpServletResponse);
      
      selectedProperties = (selectedProperties == null || selectedProperties.isEmpty()) ? null : selectedProperties;
      httpServletRequest.setAttribute(OSLC4JConstants.OSLC4J_SELECTED_PROPERTIES, selectedProperties);
      
      logMsg = LogUtils.createLogForModel(changeRequest);
      logger.debug(CURRENT_CLASS + "." + currentMethod + " - Response: " + logMsg);
      
      return Response.ok(updatedChangeRequest).header(Constants.HDR_OSLC_VERSION, Constants.OSLC_VERSION_V2).build();

    } catch (Exception e) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());	
      return handleException(e);
    }
  }  

  /**
   * Sest Etag to the header
   * @param eTagFromChangeRequest eTag which is located in Change Request
   * @param httpServletResponse HttpServletResponse
   */
  private static void setETagHeader(final String eTagFromChangeRequest,
                                      final HttpServletResponse httpServletResponse)  {
   	httpServletResponse.setHeader("ETag", eTagFromChangeRequest);
	}
    
   /**
    * Gets ETag from Change Request. Its based on modification date or creation date
    * @param changeRequest Change Request
    * @return ETag of Change Request
    */
   private static String getETagFromChangeRequest(final ChangeRequest changeRequest)	{
   	Long eTag = null;
    	
    	if (changeRequest.getModified() != null) {
    		eTag = changeRequest.getModified().getTime();
    	} else if (changeRequest.getCreated() != null) {
    		eTag = changeRequest.getCreated().getTime();
    	} else {
    		eTag = new Long(0);
    	}
    	
		return eTag.toString();
	}
    

   /**
    * Gets About URI of resource 
    * @param fragment the path of resource
    * @return OSLC about URI
    */
   protected URI getAboutURI(final String fragment) {
	  String currentMethod = "getAboutURI";
      URI about;
      try {
         about = new URI(ServiceHelper.getOslcBaseUri(httpServletRequest) + "/" + fragment);
      } catch (URISyntaxException e) {
    	 logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage()); 
         throw new WebApplicationException(e);
      }
      return about;
   }
   
   /**
    * HTML representation for a issue - redirect the request
    * directly to Jira browse
    * 
    * @param productId
    * @param changeRequestId
    * @throws ServletException
    * @throws IOException
    * @throws URISyntaxException
    */
   @GET
   @Path("{changeRequestId}")
   @Produces({ MediaType.TEXT_HTML})
   public Response getHtmlChangeRequest(@PathParam("changeRequestId") final String changeRequestId) throws ServletException, IOException, URISyntaxException {
     logger.info(getClass().getName() + ":getChangeRequestsHTML");
     
     String forwardUri = "../../../../../browse/" + changeRequestId;
     logger.info(getClass().getName() + ":Forward to " + forwardUri);

     httpServletResponse.sendRedirect(forwardUri);
     return Response.seeOther(new URI(forwardUri)).build();
   }
   
   /**
    * List of history items for given issue.
    * @param changeRequestId issue key
    * @return response with history or warning in case of exception
    */
   @GET
   @Path("{changeRequestId}/history")
   @Produces({OslcMediaType.APPLICATION_RDF_XML, OslcMediaType.APPLICATION_XML, OslcMediaType.APPLICATION_JSON})
   @AnonymousAllowed
   public Response getIssueHistory(@PathParam("changeRequestId") final String changeRequestId) {
     
     try {
      JiraHistoryRequest jhr = FieldManager.getHistoryOfIssue(httpServletRequest, changeRequestId);
      return Response.ok(jhr).header(Constants.HDR_OSLC_VERSION, Constants.OSLC_VERSION_V2).build();
     } 
     catch (PermissionException e) {
       logger.error(CURRENT_CLASS + ".getIssueHistory Exception: " + e.getMessage());
       return handleException(e);
     } 
     catch (NoResourceException e) {
       logger.error(CURRENT_CLASS + ".getIssueHistory Exception: " + e.getMessage());
       return handleException(e);
     }
   }
}
