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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.xerces.jaxp.validation.XMLSchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.ericsson.jira.oslc.constants.JiraConstants;
import com.ericsson.jira.oslc.managers.JiraManager;
import com.ericsson.jira.oslc.managers.PermissionManager;
import com.ericsson.jira.oslc.resources.ao.AOManager;
import com.ericsson.jira.oslc.sync.SyncConfig;
import com.ericsson.jira.oslc.utils.ServletUtils;

/**
 * A servlet which is responsible for loading saving the configurations from/to db.
 *
 */
public class SyncConfigServlet extends HttpServlet {

  private static final long serialVersionUID = -6468802507370592016L;
  private final UserManager userManager;
  private final LoginUriProvider loginUriProvider;
  private final TemplateRenderer renderer;
  private static Logger logger =  LoggerFactory.getLogger(PluginConfigurationServlet.class);

  public SyncConfigServlet(UserManager userManager, LoginUriProvider loginUriProvider, TemplateRenderer renderer) {
    this.userManager = userManager;
    this.loginUriProvider = loginUriProvider;
    this.renderer = renderer;
  }

  /**
   * Load the configurations from db and displays them on the page
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    reloadPage(request, response, "", null);
  }

  /**
   * Reload the configurations from db and displays them on the page
   * @param request a request
   * @param response a response
   * @param errorMessage an error message which will be displayed on the page
   * @param inputConfiguration the configuration to save
   * @throws IOException
   * @throws ServletException
   */
  private void reloadPage(HttpServletRequest request, HttpServletResponse response, String errorMessage, String inputConfiguration) throws IOException, ServletException {

    String username = userManager.getRemoteUsername(request);
    if (username == null || !userManager.isSystemAdmin(username)) {
      ServletUtils.redirectToLogin(request, response, loginUriProvider);
      return;
    }

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("errorMsg", errorMessage);
    String mappingConf = null;
    
    if(inputConfiguration == null){
      AOManager mngr = AOManager.getInstance();
      String syncConfig = mngr.getConfigClobValue(SyncConfig.SYNC_CONFIG_PROPERTY);
      if (syncConfig != null) {
        mappingConf = syncConfig;
      }
    }else{
      mappingConf = inputConfiguration;
    }

    if (mappingConf == null) {
      mappingConf = "";
    }

    String debug = request.getParameter("debug");

    if(debug != null){
      JAXBContext ctx;
      try {
        ctx = JAXBContext.newInstance(SyncConfig.class);
        Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        SyncConfig syncConfig = SyncConfig.getInstance();
        java.io.StringWriter sw = new StringWriter();
        marshaller.marshal(syncConfig, sw); 
        mappingConf = sw.toString();
        params.put("debug", true);
      } catch (Exception e) {
        logger.error("SyncConfigServlet", e);
        mappingConf = e.getMessage();
      }

    }else{
      params.put("debug", false);
    }

    params.put("mappingConfiguration", mappingConf);

    response.setContentType("text/html;charset=utf-8");
    renderer.render("templates/syncConfig.vm", params, response.getWriter());
  }

  /**
   * Save the configuration to db
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
    final String inputConfiguration = req.getParameter("mappingConfiguration");
    Boolean validateReq = ServletUtils.parseBoolean(req.getParameter("validate"));
    boolean allowValidation = (validateReq == null || validateReq);
    
    
    ApplicationUser user = PermissionManager.getLoggedUser();
    if (user == null) {
      reloadPage(req, response, "Can't continue.\\nMessage: User not defined!", inputConfiguration);
      return;
    }

    if (!PermissionManager.isSystemAdmin(userManager)) {
      reloadPage(req, response, "Can't continue.\\nMessage: User not system admin!", inputConfiguration);
      return;
    }
    
    
    

    SyncConfig config;
    try {
      if(allowValidation){
        validateConfig(inputConfiguration, JiraConstants.XSD_PATH);
      }
      
      config = SyncConfig.getInstance(false);

      config.loadConfiguration(inputConfiguration);
      
      AOManager mngr = AOManager.getInstance();
      mngr.saveConfigClobValue(SyncConfig.SYNC_CONFIG_PROPERTY, inputConfiguration);
      reloadPage(req, response, "", null);
    } catch (Exception e) {
      logger.error("Error", e);
      String msg = (e.getMessage() != null)? e.getMessage() : e.toString();
      msg = ServletUtils.encodeWhitespaces(msg);
      reloadPage(req, response, msg, inputConfiguration);
    } 
  }
  
  private static void validateConfig(String inputConfiguration, String pathToXsd) throws SAXException, IOException{
    if(inputConfiguration == null || pathToXsd == null){
      return;
    }
     
    InputStream is = new ByteArrayInputStream(inputConfiguration.getBytes(StandardCharsets.UTF_8));
    Source source = new StreamSource(is);
    Source xsd = new StreamSource(JiraManager.class.getResourceAsStream(pathToXsd));
    
    SchemaFactory schemaFacotry = new XMLSchemaFactory();
    Schema schema  = schemaFacotry.newSchema(xsd);
    Validator validator = schema.newValidator();

    validator.validate(source);
  }
}
