package com.ericsson.jira.oslc.servlet;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.CreateValidationResult;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.OperationContext;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.screen.FieldScreenRenderLayoutItem;
import com.atlassian.jira.issue.fields.screen.FieldScreenRenderer;
import com.atlassian.jira.issue.fields.screen.FieldScreenRendererFactory;
import com.atlassian.jira.issue.operation.IssueOperation;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;

import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.JiraUtils;
import com.atlassian.jira.util.VelocityParamFactory;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.templaterenderer.RenderingException;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.ericsson.jira.oslc.exceptions.PermissionException;
import com.ericsson.jira.oslc.managers.PermissionManager;
import com.ericsson.jira.oslc.utils.OSLCUtils;
import com.ericsson.jira.oslc.utils.ServletUtils;

import webwork.action.Action;


/**
 * Inner class for creating operation context during creation issue
 *
 */
class CreateIssueOperationContext implements OperationContext {

  private Map<String, Object> values = new HashMap<String, Object>();
  
  public CreateIssueOperationContext(String key, String val) 
  {
    values.put(key, val);
  }
  
  @Override
  public Map<String, Object> getFieldValuesHolder() {
    return values;
  }

  @Override
  public IssueOperation getIssueOperation() {
    return IssueOperations.CREATE_ISSUE_OPERATION;
  }
}

/**
 * Servlet class, which is responsible for rendering and handling "Create issue dialog"
 * for OSLC delegation UI. 
 *
 */

// IMPORTANT NOTE:
// Delegation UI (dialogs) is displayed within 'iframe' element (done by external system).
// Because Jira doesn't support proper rendering it's pages in 'iframe', some field generated
// in reloadPage() method won't work properly (mainly due to not loaded javascripts). 
// In some (easier) cases, we are able to substitute generated field by 'hand-made' html.
// In some cases, it is not easy to substitute generated field, so it is simply ignored
// and it is not in resulting html (attachement, labels).
// Refer to:
// https://jira.atlassian.com/browse/JRA-20988
// https://jira.atlassian.com/browse/JRA-38635

public class CreateIssueServlet extends HttpServlet
{
    private final String FIELD_ORIGINAL_ESTIMATE = "timetracking_originalestimate";
    private final String FIELD_REMAINING_ESTIMATE = "timetracking_remainingestimate";
    
    private static final Logger log = LoggerFactory.getLogger(CreateIssueServlet.class);
    private final TemplateRenderer templateRenderer;

    public CreateIssueServlet(TemplateRenderer tr) {
      this.templateRenderer = tr;
    }

    /**
     * Get HTML code for displaying warning messages
     * @param fieldName the name of field where displays error message to
     * @param content the warning message
     * @return HTML code for displaying warning messages
     */
    private String warnningField(String fieldName, String content) {
      StringBuilder sb = new StringBuilder();
      
      String visibility = "hidden";
      if (content != null && content != "") {
        visibility = "visible";
      }
      
      sb.append("<tr><td></td><td>");
      sb.append("<div id=\"" + fieldName + "_w\" class=\"fwarning\" style=\"visibility: " + visibility + ";\">");
      sb.append(content);
      sb.append("</div>");
      sb.append("</td></tr>");
      
      return sb.toString();
    }
    
    /**
     * Prepare code for displaying Reporter field in the form
     * @param prj a project
     * @return HTML code which displays Reporter field
     */  
    private String reporterField() {
      StringBuilder sb = new StringBuilder();
      sb.append("<tr class=\"fieldArea\" id=\"reporterFieldArea\">");
      sb.append("<td class=\"fieldLabelArea\" >");
      sb.append("<label for=\"reporter\">");
      sb.append("<span class=\"required\" title=\"Required Field\"><sup>*</sup> Reporter:</span>");
      sb.append("</label>");
      sb.append("</td>");
      sb.append("<td class=\"fieldValueArea\">");
      sb.append("<select id=\"reporter\" name=\"reporter\" class=\"select\">");
      
      ApplicationUser aUser = ComponentAccessor.getJiraAuthenticationContext().getUser();
      sb.append("<option value=\"" + aUser.getKey() + "\">" + aUser.getDisplayName() + "</option>");
      
      sb.append("</select>");
      sb.append("</td>");
      sb.append("</tr>");
      
      sb.append(this.warnningField("reporter", ""));
      
      return sb.toString();
    }
    
    /**
     * Prepare code for displaying Component field in the form
     * @param prj a project
     * @return HTML code which displays Component field
     */   
    private String componentsField(Project prj) {
      StringBuilder sb = new StringBuilder();
      sb.append("<tr class=\"fieldArea\" id=\"componentsFieldArea\">");
      sb.append("<td class=\"fieldLabelArea\" >");
      sb.append("<label for=\"components\">Component/s:</label>");
      sb.append("</td>");
      sb.append("<td class=\"fieldValueArea\">");
      sb.append("<select id=\"components\" name=\"components\" class=\"select\"> ");
      
      sb.append("<option value=\"-1\">Unknown</option>");
      
      Collection<ProjectComponent> components = prj.getProjectComponents();
      for (ProjectComponent c : components) {
        sb.append("<option value=\"" + c.getId() + "\">" + c.getName() + "</option>");
      }
      
      sb.append("</select>");
      sb.append("</td>");
      sb.append("</tr>");
      
      return sb.toString();
    }
    
    /**
     * Prepare code for displaying Affects Version field in the form
     * @param prj a project
     * @return HTML code which displays Affects Version field
     */
    private String affectsVersionField(Project prj) {
      StringBuilder sb = new StringBuilder();
      sb.append("<tr class=\"fieldArea\" id=\"versionsFieldArea\">");
      sb.append("<td class=\"fieldLabelArea\" >");
      sb.append("<label for=\"versions\">Affects Version/s:</label>");
      sb.append("</td>");
      sb.append("<td class=\"fieldValueArea\">");
      sb.append("<select id=\"versions\" name=\"versions\" class=\"select\"> ");
      
      sb.append("<option value=\"-1\">Unknown</option>");
      
      Collection<Version> versions = prj.getVersions();
      for (Version v : versions) {
        sb.append("<option value=\"" + v.getId() + "\">" + v.getName() + "</option>");
      }
      
      sb.append("</select>");
      sb.append("</td>");
      sb.append("</tr>");
      
      return sb.toString();
    }
    
    /**
     * Prepare code for displaying Fix Version field in the form
     * @param prj a project
     * @return HTML code which displays Fix Version field
     */
    private String fixVersionsField(Project prj) {
      StringBuilder sb = new StringBuilder();
      sb.append("<tr class=\"fieldArea\" id=\"fixVersionsFieldArea\">");
      sb.append("<td class=\"fieldLabelArea\" >");
      sb.append("<label for=\"fixVersions\">Fix Version/s:</label>");
      sb.append("</td>");
      sb.append("<td class=\"fieldValueArea\">");
      sb.append("<select id=\"fixVersions\" name=\"fixVersions\" class=\"select\"> ");
      
      sb.append("<option value=\"-1\">Unknown</option>");
      
      Collection<Version> versions = prj.getVersions();
      for (Version v : versions) {
        sb.append("<option value=\"" + v.getId() + "\">" + v.getName() + "</option>");
      }
      
      sb.append("</select>");
      sb.append("</td>");
      sb.append("</tr>");
      
      return sb.toString();
    }
    
    /**
     * Open the page with error information
     * @param resp HttpServletResponse
     * @param msg an error message which will be displayed
     */
    private void sendError(HttpServletResponse resp, String msg) {
      Map<String, Object> context = new HashMap<String, Object>();
      context.put("errorMessage", msg);
      try {
        templateRenderer.render("templates/error_page.vm", context, resp.getWriter());
      } 
      catch (RenderingException e) {
        log.error("CreateIssueServlet.sendError: " + e.getMessage());
        e.printStackTrace();
      } 
      catch (IOException e) {
        log.error("CreateIssueServlet.sendError: " + e.getMessage());
        e.printStackTrace();
      }
    }
    
    /**
     * Reloads the page for creation issue
     * @param req a request
     * @param resp a response
     * @param errs the map of errors
     * @throws IOException
     */
    private void reloadPage(HttpServletRequest req, HttpServletResponse resp, Map<String, String> errs) 
        throws IOException {
      //get project object based on incoming project id (project id is part of request)
      ProjectManager pm = ComponentAccessor.getProjectManager();
      long pid = Long.parseLong(req.getParameter("projectId"));
      Project prj = pm.getProjectObj(pid);
      if (prj == null) {
        String msg = "Project with id " + pid + " does not exist!";
        log.error(msg);
        sendError(resp, msg);
        return;
      }
      
      ApplicationUser user = PermissionManager.getLoggedUser();
      try {
        PermissionManager.checkPermission(user, prj, Permissions.CREATE_ISSUE);
      } 
      catch (PermissionException e) {
        String msg = "User " + user.getName() + " doesn't have permission to create issues!";
        log.error(msg);
        sendError(resp, msg);
        return;
      }
      
      VelocityParamFactory vpf = ComponentAccessor.getVelocityParamFactory();
      JiraAuthenticationContext jac = ComponentAccessor.getJiraAuthenticationContext();
      Map<String, Object> context = vpf.getDefaultVelocityParams(jac);
      
      //create new empty issue, object will be used to construct issue creation form (html)
      IssueFactory issueFactory = ComponentAccessor.getIssueFactory();
      MutableIssue issueObject = issueFactory.getIssue();
      
      //bind new (empty) issue to (existing) project
      String issueType = req.getParameter("issueType");
      issueObject.setProjectObject(prj);
      //set issue type (value is part of request)
      issueObject.setIssueTypeId(issueType);
      
      //Put project id and issue type from request to form as hidden fields.
      //Values will be passed to doPost on form submit.
      context.put("currentProjectId", pid);
      context.put("currentIssueType", issueType); 
      
      //prepare list of items, which should be rendered - list will be based on previous settings (issue, project)
      FieldScreenRendererFactory factory = ComponentAccessor.getFieldScreenRendererFactory();
      FieldScreenRenderer fsRenderer = factory.getFieldScreenRenderer(issueObject, IssueOperations.CREATE_ISSUE_OPERATION);
      List<FieldScreenRenderLayoutItem> list = fsRenderer.getAllScreenRenderItems(); 
      
      //list will hold html elements for resulting form
      List<String> htmlFields = new ArrayList<String>();

      String vHTML;

      OrderableField type = ComponentAccessor.getFieldManager().getOrderableField(IssueFieldConstants.ISSUE_TYPE);
      vHTML = type.getViewHtml(null, 
            (Action)JiraUtils.loadComponent(com.atlassian.jira.web.action.issue.CreateIssue.class),
            issueObject, context);
      if (vHTML != null)
      {
        //vHTML = cleanHTML(contextPath, vHTML);
        context.put("typeView", vHTML);
      }

      String issuetype_w_content = "";
      if (errs.containsKey("issuetype")) {
        issuetype_w_content = errs.get("issuetype");
      }
      htmlFields.add(this.warnningField(IssueFieldConstants.SUMMARY, issuetype_w_content));
      
      //go through all rendering elements and transform them to html elements
      for (Iterator<FieldScreenRenderLayoutItem> iterator = list.iterator(); iterator.hasNext();) 
      {
        FieldScreenRenderLayoutItem fieldScreenRenderLayoutItem = iterator.next();
        String key = fieldScreenRenderLayoutItem.getOrderableField().getId();
        
        if(skipField(key)){
          continue;
        }

        //ignore (it is pre-set)
        if (IssueFieldConstants.ISSUE_TYPE.equals(key))
          continue;
        
        //ignore - missing js, so it won't work
        if (IssueFieldConstants.ATTACHMENT.equals(key))
          continue;
        if (IssueFieldConstants.LABELS.equals(key))
          continue;
        
        //substitute to fill currently logged user by default (due to missing js, it won't work)
        if (IssueFieldConstants.REPORTER.equals(key)) {
          htmlFields.add(reporterField());
          continue;
        }
        
        //substitute (due to missing js, it won't work)
        if (IssueFieldConstants.COMPONENTS.equals(key)) {
          htmlFields.add(componentsField(prj));
          continue;
        }
        if (IssueFieldConstants.AFFECTED_VERSIONS.equals(key)) {
          htmlFields.add(affectsVersionField(prj));
          continue;
        }
        if (IssueFieldConstants.FIX_FOR_VERSIONS.equals(key)) {
          htmlFields.add(fixVersionsField(prj));
          continue;
        }
        
        vHTML = fieldScreenRenderLayoutItem.getCreateHtml(
            (Action)JiraUtils.loadComponent(com.atlassian.jira.web.action.issue.CreateIssue.class),
            new CreateIssueOperationContext(key, ""), issueObject, context);

        if (vHTML != null)
        {
          if (IssueFieldConstants.TIMETRACKING.equals(key)) {
            //add warning
            
            String content = "";
            if (errs.containsKey(FIELD_ORIGINAL_ESTIMATE)) {
              content = errs.get(FIELD_ORIGINAL_ESTIMATE);
            }
            
            //insert warning for orig. estimate between table rows
            int idx = vHTML.indexOf("</tr>"); //1st occurrence of </tr>
            String newHTML = vHTML.substring(0, idx+5) + 
                this.warnningField(FIELD_ORIGINAL_ESTIMATE, content)
                + vHTML.substring(idx+5, vHTML.length()-1);
            
            content = "";
            if (errs.containsKey(FIELD_REMAINING_ESTIMATE)) {
              content = errs.get(FIELD_REMAINING_ESTIMATE);
            }
            
            //append warning for remaining estimate after last rows
            newHTML += this.warnningField(FIELD_REMAINING_ESTIMATE, content);
            
            vHTML = newHTML;
          }
          
          //vHTML = cleanHTML(contextPath, vHTML);
          htmlFields.add(vHTML);
          
          if (IssueFieldConstants.SUMMARY.equals(key)) {
            //add warning
            htmlFields.add(this.warnningField(IssueFieldConstants.SUMMARY, ""));
          }
          
          if (IssueFieldConstants.DUE_DATE.equals(key)) {
            //add warning
            String content = "";
            if (errs.containsKey(IssueFieldConstants.DUE_DATE)) {
              content = errs.get(IssueFieldConstants.DUE_DATE);
            }
            htmlFields.add(this.warnningField("duedate", content));
          }
        }
      }

      //list with html elements will be passed to velocity template through context map
      context.put("fields", htmlFields);
      context.put("hasErrors", !errs.isEmpty());
      
      templateRenderer.render("templates/create_issue.vm", context, resp.getWriter());
    }
    
    /**
     * Display the content of creation dialog
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
      reloadPage(req, resp, new HashMap<String, String>());
    }
    

    
    /**
     * When an issue is created an response has to be sent to remote server
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
    {
      //find project according to given project id
      Long prjid = Long.parseLong(req.getParameter("projectId"));
      ProjectManager projectManager = ComponentAccessor.getProjectManager();
      Project p = projectManager.getProjectObj(prjid);
      if (p == null) {
        String msg = "Project with id " + prjid + " does not exist!";
        log.error(msg);
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        return;
      }
      
      ApplicationUser aUser = PermissionManager.getLoggedUser();
      try {
        PermissionManager.checkPermission(aUser, p, Permissions.CREATE_ISSUE);
      } 
      catch (PermissionException e) {
        String msg = "User " + aUser.getName() + " doesn't have permission to create issues!";
        log.error(msg);
        sendError(resp, msg);
        return;
      }
      
      //retrieve all issue parameters from request
      String summary = req.getParameter("summary"); //MANDATORY
      String priority = req.getParameter("priority");
      String issueType = req.getParameter("issueType");
      String duedate = req.getParameter("duedate");
      String component = req.getParameter("components");
      String version = req.getParameter("versions");
      String fix_version = req.getParameter("fixVersions");
      String asignee = req.getParameter("assigne");
      String reporter = req.getParameter("reporter"); //MANDATORY
      String environment = req.getParameter("environment");
      String description = req.getParameter("description");
      String timetracking_originalestimate = req.getParameter("timetracking_originalestimate");
      String timetracking_remainingestimate = req.getParameter("timetracking_remainingestimate");
      
      //check if reporter filed was set
      //Note: it seems that for user, who are not admins, reporter field is not provided. In this case
      //      reporter field is missing in creation form. We assume, if user is able to display
      //      creation dialog in delegation UI, it is logged in. So here, if reporter is not set,
      //      we assume it is non-admin and it is logged. Then we set this logged-in user as reporter.
      if (reporter == null || reporter.isEmpty() == true) {
        if (aUser == null) {
          LoginUriProvider loginUriProvider = ComponentAccessor.getComponent(LoginUriProvider.class);
          ServletUtils.redirectToLogin(req, resp, loginUriProvider);
          return;
        }
        else {
          reporter = aUser.getName();
        }
      }
      
      //check validity
      //Note: for the same reason as in case of reporter, some fields are not present in form.
      //      For String fields we can keep null, but for number fields, which are parsed, we
      //      must set default value.
      if (component == null || component.isEmpty() == true) {
        component = "-1";
      }
      if (version == null || version.isEmpty() == true) {
        version = "-1";
      }
      if (fix_version == null || fix_version.isEmpty() == true) {
        fix_version = "-1";
      }
      

      
      JiraAuthenticationContext authenticationContext = ComponentAccessor.getJiraAuthenticationContext();
      authenticationContext.setLoggedInUser(aUser);
      
      IssueService issueService = ComponentAccessor.getIssueService();
      //construct new issue from values from request
      IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
      issueInputParameters.setProjectId(p.getId());
      issueInputParameters.setIssueTypeId(issueType);
      issueInputParameters.setPriorityId(priority);
      issueInputParameters.setSummary(summary);
      issueInputParameters.setDueDate(duedate);
      issueInputParameters.setComponentIds(Long.parseLong(component));
      issueInputParameters.setAffectedVersionIds(Long.parseLong(version));
      issueInputParameters.setFixVersionIds(Long.parseLong(fix_version));
      issueInputParameters.setReporterId(reporter);
      issueInputParameters.setAssigneeId(asignee);
      issueInputParameters.setEnvironment(environment);
      issueInputParameters.setDescription(description);
      issueInputParameters.setOriginalAndRemainingEstimate(timetracking_originalestimate, timetracking_remainingestimate);
      
      CreateValidationResult createValidationResult = issueService.validateCreate(aUser, issueInputParameters);
      //validate issue parameters
      if (!createValidationResult.isValid()) {
        ErrorCollection ec = createValidationResult.getErrorCollection();
        Map<String, String> em = ec.getErrors();
        reloadPage(req, resp, em);
        return;
      }
      
      //create issue from validated parameters
      IssueResult createResult = issueService.create(aUser, createValidationResult);
      if (!createResult.isValid()) {
        StringBuilder sb = new StringBuilder();
        sb.append("Create issue failed!\n");
        for (String msg : createResult.getErrorCollection().getErrorMessages())
          sb.append(msg + "\n");
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, sb.toString());
        return;
      }
      
      // Create issue passed -> Send new response with {id,link} back to remote app. Sending
      // happends in javascript included in create_issue_response.vm.
      
      VelocityParamFactory vpf = ComponentAccessor.getVelocityParamFactory();
      JiraAuthenticationContext jac = ComponentAccessor.getJiraAuthenticationContext();
      Map<String, Object> context = vpf.getDefaultVelocityParams(jac);
      
      long issueId = createResult.getIssue().getId();
      context.put("responseMessage", OSLCUtils.getOSLCResponseMessage(issueId, true));
      context.put("responseCoreMessage", OSLCUtils.getOSLCResponseCoreMessage(issueId, true));
      context.put("responseCommonMessage", OSLCUtils.getOSLCResponseCommonMessage(issueId, true));
      
      templateRenderer.render("templates/create_issue_response.vm", context, resp.getWriter());
    }
    
    /**
     * Skips the fields which will not be in the form. Currently custom fields will not be added to the form
     * @param key the key of the field
     * @return true - the field will not be added to the form
     */
    private boolean skipField(String key){
      if(key.startsWith("customfield_")){
        return true;
      }
      return false;
    }
}
