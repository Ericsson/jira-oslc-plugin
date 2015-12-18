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
 *     Michael Fiedler      - Bugzilla adapter implementation
 *******************************************************************************/
package com.ericsson.jira.oslc.servlet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.lyo.oslc4j.core.exception.OslcCoreApplicationException;
import org.eclipse.lyo.oslc4j.core.model.Dialog;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.oslc4j.core.model.Publisher;
import org.eclipse.lyo.oslc4j.core.model.Service;
import org.eclipse.lyo.oslc4j.core.model.ServiceProvider;
import org.eclipse.lyo.oslc4j.core.model.ServiceProviderFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.ericsson.jira.oslc.PluginConfig;
import com.ericsson.jira.oslc.constants.JiraConstants;
import com.ericsson.jira.oslc.managers.JiraManager;
import com.ericsson.jira.oslc.resources.ServiceProviderRef;
import com.ericsson.jira.oslc.services.JiraChangeRequestService;
import com.ericsson.jira.oslc.utils.ServiceProviderRegistryURIs;

/**
 * It's an OSLC provider factory which offers the methods for creating the services, the dialogs ...
 *
 */
public class JiraServiceProviderFactory
{
    private static Class<?>[] RESOURCE_CLASSES =
    {
        JiraChangeRequestService.class
    };

    private JiraServiceProviderFactory()
    {
        super();
    }

    /**
     * Create a new Jira OSLC change management service provider.
     * @param baseURI
     * @param project
     * @param parameterValueMap - a map containing the path replacement value for {productId}.  See ServiceProviderCatalogSingleton.initServiceProvidersFromProducts()
     * @return
     * @throws Exception 
     */
    public static ServiceProvider createServiceProvider(
        final String baseURI, final Project project, final Map<String,Object> parameterValueMap)
           throws Exception
    {

        final ServiceProvider serviceProvider = createServiceProvider(baseURI,
                                                                                             ServiceProviderRegistryURIs.getUIURI(),
                                                                                             project.getName(),
                                                                                             "Service provider for Jira projects: "+project.getName(),
                                                                                             new Publisher("JIRA - " + project.getName(), "urn:oslc:ServiceProvider:" + project.getId()),
                                                                                             RESOURCE_CLASSES,
                                                                                             parameterValueMap);
        URI detailsURIs[] = {new URI(baseURI + "/serviceProviders/"+project.getId()+"/details")};
        serviceProvider.setDetails(detailsURIs);
        
        Service[] services = null;
        if(serviceProvider instanceof ServiceProviderRef){
          services = ((ServiceProviderRef)serviceProvider).getOSLCServices();
        }else{
          serviceProvider.getServices();
        }
        
        if(services != null){
          for (Service service : services) {
            addCreationDialog(service, project);
            addSelectionialog(service, project);
          }
        }
        return serviceProvider;
    }

    /**
     * Add creation dialogs to a service
     * @param service the dialogs will be added to this service
     * @param project JIRA project - the types of issue are got from the project configuration
     * @throws Exception
     */
    protected static void addCreationDialog(Service service, Project project) throws Exception{
      List<IssueType> issueTypes = null;
      
      PluginConfig config = PluginConfig.getInstance();
      Set<Long> filteredTypes = config.getFilteredTypes();
      
      if(filteredTypes == null || filteredTypes.isEmpty()){
        Collection<IssueType> collIssueTypes = ComponentAccessor.getProjectManager().getProjectObj(project.getId()).getIssueTypes();
        issueTypes = new ArrayList<IssueType>(collIssueTypes);
      }else{
        issueTypes = new ArrayList<IssueType>();
        for (Long typeId : filteredTypes) {
          IssueType issueType = ComponentAccessor.getConstantsManager().getIssueTypeObject(typeId.toString());
          if(issueType != null){
            issueTypes.add(issueType);
          }
        }
      }
       
      for (IssueType issueType : issueTypes) {
        if(issueType.isSubTask()){
          continue;
        }
        Dialog dialog = new Dialog();
        dialog.setHintWidth(JiraConstants.CREATION_DIALOG_WIDTH);
        dialog.setHintHeight(JiraConstants.CREATION_DIALOG_HEIGHT);
        URI[] resourceTypes = new URI[]{new URI(JiraConstants.CM_CHANGE_REQUEST)};
        dialog.setResourceTypes(resourceTypes);
        URI[] usages = new URI[]{new URI(OslcConstants.OSLC_USAGE_DEFAULT) };
        dialog.setUsages(usages);
        dialog.setLabel(issueType.getName());
        dialog.setTitle(issueType.getName());

        dialog.setDialog(new URI(JiraManager.getBaseUrl() +JiraConstants.CREATE_ISSUE+"?projectId="+project.getId()+"&issueType=" + issueType.getId()));
        service.addCreationDialog(dialog);
      }
    }
    
    /**
     * Add selection dialogs to a service
     * @param service the dialogs will be added to this service
     * @param project JIRA project - the types of issue are got from the project configuration
     * @throws Exception
     */
    protected static void addSelectionialog(Service service, Project project) throws Exception{
      List<IssueType> issueTypes = null;
      
      PluginConfig config = PluginConfig.getInstance();
      Set<Long> filteredTypes = config.getFilteredTypes();
      
      if(filteredTypes == null || filteredTypes.isEmpty()){
        Collection<IssueType> collIssueTypes = ComponentAccessor.getProjectManager().getProjectObj(project.getId()).getIssueTypes();
        issueTypes = new ArrayList<IssueType>(collIssueTypes);
      }else{
        issueTypes = new ArrayList<IssueType>();
        for (Long typeId : filteredTypes) {
          IssueType issueType = ComponentAccessor.getConstantsManager().getIssueTypeObject(typeId.toString());
          if(issueType != null){
            issueTypes.add(issueType);
          }
        }
      }
      for (IssueType issueType : issueTypes) {
        Dialog dialog = new Dialog();
        dialog.setHintWidth(JiraConstants.SELECTION_DIALOG_WIDTH);
        dialog.setHintHeight(JiraConstants.SELECTION_DIALOG_HEIGHT);
        URI[] resourceTypes = new URI[]{new URI(JiraConstants.CM_CHANGE_REQUEST)};
        dialog.setResourceTypes(resourceTypes);
        URI[] usages = new URI[]{new URI(OslcConstants.OSLC_USAGE_DEFAULT) };
        dialog.setUsages(usages);
        dialog.setLabel(issueType.getName());
        dialog.setTitle(issueType.getName());
        URI uri = new URI(JiraManager.getBaseUrl() +JiraConstants.SELECT_ISSUE+"?projectId="+project.getId()+"&issueType=" + issueType.getId());
        dialog.setDialog(uri);
        service.addSelectionDialog(dialog);
      }
    }
    /**
     * Create OSLC service provider
     * @param baseURI base URI
     * @param genericBaseURI generic URI
     * @param title the title of service provider
     * @param description the description of service provider
     * @param publisher the publisher of service provider
     * @param resourceClasses it's class of Change Request
     * @param pathParameterValues the parameter in the path
     * @return created Service provider
     * @throws OslcCoreApplicationException
     * @throws URISyntaxException
     */
    public static ServiceProvider createServiceProvider(final String baseURI, final String genericBaseURI, final String title, final String description, final Publisher publisher, final Class<?>[] resourceClasses, final Map<String,Object> pathParameterValues) throws OslcCoreApplicationException, URISyntaxException {
      return ServiceProviderFactory.initServiceProvider(new ServiceProviderRef(), baseURI, genericBaseURI, title, description, publisher, resourceClasses, pathParameterValues);
    }
} 