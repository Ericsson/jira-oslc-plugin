package com.ericsson.jira.oslc.oslcclient;

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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.ericsson.jira.oslc.HTTP;
import com.ericsson.jira.oslc.HTTP.OAuthPhases;
import com.ericsson.jira.oslc.constants.JiraConstants;
import com.ericsson.jira.oslc.resources.RootServices;
import com.ericsson.jira.oslc.resources.ServiceProvider;
import com.ericsson.jira.oslc.resources.ServiceProviderDialog;
import com.ericsson.jira.oslc.utils.LogUtils;
import com.ericsson.jira.oslc.utils.OSLCUtils;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * A class for operations with HTTP requests/reponse 
 *
 */
public class Client {
  private static final String CURRENT_CLASS = "Client";
  private static final Logger log = LoggerFactory.getLogger(Client.class);
  
  //last status code from http response
  private int responseCode;
  //last phrase from http response
  private String responsePhrase;
  
  public int getLastResponseCode() {
    return responseCode;
  }
  
  public String getLastResponsePhrase() {
    return responsePhrase;
  }
  
  /** 
   * Method extracts body from http response to String instance
   * 
   * @param is input stream (should be accessible from http response)
   * @return string instance with response body
   */
  private String getResponseBody(InputStream is) {
    String currentMethod = "getResponseBody";
    InputStreamReader isr = new InputStreamReader(is);
    BufferedReader br = new BufferedReader(isr);
    String line = "";
    StringBuilder sb = new StringBuilder();
    try {
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
    } 
    catch (IOException e) {
      log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      e.printStackTrace();
    }
    return sb.toString();
  }
  
  /**
   * Method gets oAuth details from rootservices url.
   * @param rootServicesURI
   * @return instance of class with detailed data about rootservices
   */
  public RootServices getRootServicesDetails(String rootServicesURI) {
    String currentMethod = "getRootServicesDetails";
    
    try {
      //no authentication needed
      HttpResponse resp = HTTP.get(rootServicesURI, "application/rdf+xml", null, null, null);
      
      if (resp != null) {
        responseCode = resp.getStatusLine().getStatusCode();
        responsePhrase = resp.getStatusLine().getReasonPhrase();
      
        if (responseCode == 200) {
          try {
            InputStream is = resp.getEntity().getContent();
            Model rdfModel = null;
            rdfModel = ModelFactory.createDefaultModel();
            rdfModel.read(is, null);
            
            RootServices rs = new RootServices();
            final String ns = "http://jazz.net/xmlns/prod/jazz/jfs/1.0/";
            rs.setOAuthAccessTokenURL(OSLCUtils.getProperty(rdfModel, ns, "oauthAccessTokenUrl"));
            rs.setOAuthDomain(OSLCUtils.getProperty(rdfModel, ns, "oauthDomain"));
            rs.setOAuthRequestConsumerKeyURL(OSLCUtils.getProperty(rdfModel, ns, "oauthRequestConsumerKeyUrl"));
            rs.setOAuthRequestTokenURL(OSLCUtils.getProperty(rdfModel, ns, "oauthRequestTokenUrl"));
            rs.setOAuthUserAuthorizationURL(OSLCUtils.getProperty(rdfModel, ns, "oauthUserAuthorizationUrl"));
            
            return rs;
          }
          catch (Exception ex) {
            log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + ex.getMessage());
            ex.printStackTrace();
            responseCode = 500;
            responsePhrase = "Error in parsing response data!\\nSource isn't root services or malformed!";
          }
        }
      }
    }
    catch (ClientProtocolException e) {
      log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      e.printStackTrace();
      responseCode = 500;
      responsePhrase = e.getMessage();
    }
    catch (IOException e) {
      log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      e.printStackTrace();
      responseCode = 500;
      responsePhrase = e.getMessage();
    }
    
    return null;
  }
  
  /**
   * Gets in-bound consumer key from remote application.
   * @param url consumer key request url
   * @param consumerName consumer name
   * @param secret consumer secret
   * @param userID user id 
   * @return in-bound consumer key
   */
  public String getConsumerKey(String url, String consumerName, String secret, String userID) {
    String key = null;
    try {
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append("\"name\": \"" + consumerName + "\","); 
      sb.append("\"secretType\": \"string\",");
      sb.append("\"secret\": \"" + secret + "\",");  
      sb.append("\"trusted\": false,"); 
      sb.append("\"userId\": null");
      sb.append("}");

      HttpResponse resp = null;
      
      //no authentication needed
      resp = HTTP.post(url, sb.toString(), null, "application/json", null, null);
      if (resp != null) {
        responseCode = resp.getStatusLine().getStatusCode();
        responsePhrase = resp.getStatusLine().getReasonPhrase();
        
        //no authentication needed
        if (responseCode != 200) {
          resp = HTTP.post(url, sb.toString(), null, "text/json", null, null);
          if (resp != null) {
            responseCode = resp.getStatusLine().getStatusCode();
            responsePhrase = resp.getStatusLine().getReasonPhrase();
          }
        }
        
        if (responseCode == 200) {
          InputStream is = resp.getEntity().getContent();
          String body = getResponseBody(is);
          
          org.apache.wink.json4j.JSONObject j;
          try {
            j = (org.apache.wink.json4j.JSONObject) JSON.parse(body);
            key = j.getString("key");
          } 
          catch (NullPointerException e) {
            log.error(CURRENT_CLASS + ".getConsumerKey Exception: " + e.getMessage());
            e.printStackTrace();
            responseCode = 500;
            responsePhrase = "Error in parsing response data!\\nSource isn't consumer key data or malformed json data!";
          } 
          catch (JSONException e) {
            log.error(CURRENT_CLASS + ".getConsumerKey Exception: " + e.getMessage());
            e.printStackTrace();
            responseCode = 500;
            responsePhrase = "Error in parsing response data!\\nSource isn't consumer key data or malformed json data!";
          }
        }
      }
    }
    catch (ClientProtocolException e) {
      log.error(CURRENT_CLASS + ".getConsumerKey Exception: " + e.getMessage());
      e.printStackTrace();
    }
    catch (IOException e) {
      log.error(CURRENT_CLASS + ".getConsumerKey Exception: " + e.getMessage());
      e.printStackTrace();
    }
    
    return key;
  }
  
  /**
   * Method extracts service provider catalog uri from root services.
   * @param rootServicesURI link to root services
   * @return uri to service provider catalog
   */
  public String getServicesProviderCatalogURI(String rootServicesURI) {
    String currentMethod = "getServicesProviderCatalogURI";
    String result = null;
    
    try {
      //no authentication needed
      HttpResponse resp = HTTP.get(rootServicesURI, "application/rdf+xml", null, null, null);
      
      if (resp != null) {
        responseCode = resp.getStatusLine().getStatusCode();
        responsePhrase = resp.getStatusLine().getReasonPhrase();
        
        if (responseCode == 200) {
          try {
            InputStream is = resp.getEntity().getContent();
            Model rdfModel = null;
            rdfModel = ModelFactory.createDefaultModel();
            rdfModel.read(is, null);
            
            result = OSLCUtils.getProperty(rdfModel, 
              "http://open-services.net/xmlns/cm/1.0/", "cmServiceProviders");
          }
          catch (Exception ex) {
            log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + ex.getMessage());
            ex.printStackTrace();
            responseCode = 500;
            responsePhrase = "Error in parsing response data!\\nSource isn't root services or malformed!";
          }
        }
      }
    }
    catch (ClientProtocolException e) {
      log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      e.printStackTrace();
      responseCode = 500;
      responsePhrase = e.getMessage();
    }
    catch (IOException e) {
      log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      e.printStackTrace();
      responseCode = 500;
      responsePhrase = e.getMessage();
    }
    
    return result;
  }
  
  public RootServices getRootServicesDetailsFromCatalog(String catalogURI) {
    return null;
  }
  
  /**
   * Method gets service providers from catalog.
   * @param catalogURI
   * @return collection of string objects, which represents service providers in format:
   *          "title1","link1","title2","link2"..."titleN","linkN"
   */
  public ArrayList<String> getServiceProviders(String catalogURI, OAuthAccessor accessor) {
    String currentMethod = "getServiceProviders";
    ArrayList<String> serviceProviders = new ArrayList<String>();
    
    try {
      //OAuth needed
      HttpResponse resp = HTTP.get(
          catalogURI, "application/rdf+xml", null, null, null, 
          accessor, null, OAuthPhases.OAUTH_PHASE_3, null);
      
      if (resp != null) {
        responseCode = resp.getStatusLine().getStatusCode();
        responsePhrase = resp.getStatusLine().getReasonPhrase();
        
        if (responseCode == 200) {
          InputStream is = resp.getEntity().getContent();
          Model rdfModel = null;
          rdfModel = ModelFactory.createDefaultModel();
          rdfModel.read(is, null);
          
          Property prop = rdfModel.getProperty("http://open-services.net/ns/core#" + "serviceProvider");
          StmtIterator statements = rdfModel.listStatements((Resource)null, prop, (RDFNode)null);
          
          while (statements.hasNext()) {
            Statement st = statements.next();
            String providerURI = st.getObject().toString();
            
            Property titleProp = rdfModel.createProperty("http://purl.org/dc/terms/title");
            Statement statement = rdfModel.getProperty((Resource)st.getObject(), titleProp);
            String title = "<NOT FOUND>";
            if (statement != null && statement.getObject() != null)
               title = statement.getObject().toString();
            
            int idx = title.indexOf("^^");
            if (idx > -1)
              title = title.substring(0, idx);
            
            serviceProviders.add(title);
            serviceProviders.add(providerURI);
          }
        }
      }
    }
    catch (ClientProtocolException e) {
      log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      e.printStackTrace();
      responseCode = 500;
      responsePhrase = e.getMessage();
    }
    catch (IOException e) {
      log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      e.printStackTrace();
      responseCode = 500;
      responsePhrase = e.getMessage();
    }
    
    return serviceProviders;
  }
  
  public void extractDialogs(Model rdfModel, ServiceProvider sp, boolean isCreation) {
    String dlgType = "";
    if (isCreation == true)
      dlgType = "creationDialog";
    else
      dlgType = "selectionDialog";
    
    Property prop = rdfModel.createProperty("http://open-services.net/ns/core#", dlgType);
    
    NodeIterator nodes = rdfModel.listObjectsOfProperty(prop);
    while (nodes.hasNext()) {
      ServiceProviderDialog dlg = new ServiceProviderDialog();
      
      Resource res = (Resource)nodes.next();
      
      Property titleProp = rdfModel.createProperty("http://purl.org/dc/terms/title");
      String title = OSLCUtils.getResourceLiteralValue(res, titleProp);
      dlg.setType(title);
      
      Property createDialogURLProperty = rdfModel.getProperty("http://open-services.net/ns/core#" + "dialog");
      String uri = res.getPropertyResourceValue(createDialogURLProperty).getURI();
      dlg.setLink(uri);
      
      Property cDlgWProp = rdfModel.getProperty("http://open-services.net/ns/core#" + "hintWidth");
      Property cDlgHProp = rdfModel.getProperty("http://open-services.net/ns/core#" + "hintHeight");
      
      Statement st1 = res.getProperty(cDlgWProp);
      if (st1 != null && st1.getObject() != null)
        dlg.setWidth(st1.getObject().toString());
      
      Statement st2 = res.getProperty(cDlgHProp);
      if (st2 != null && st2.getObject() != null)
        dlg.setHeight(st2.getObject().toString());
      
      sp.addDialog(dlg, isCreation);
    }
  }
  
  /**
   * Method loads details for given service provider:
   * - creation dialogs (type, link, width, height)
   * - selection dialogs (type, link, width, height)
   * @param spURL link to service provider
   * @return service provider detailed data
   */
  public ServiceProvider getServiceProviderDetails(String spURL, OAuthAccessor accessor) {
    String currentMethod = "getServiceProviderDetails";
    ServiceProvider sp = null;
    
    try {
      //OAuth needed
      HttpResponse resp = HTTP.get(
          spURL, "application/rdf+xml", null, null, null, 
          accessor, null, OAuthPhases.OAUTH_PHASE_3, null);
      
      if (resp != null) {
        responseCode = resp.getStatusLine().getStatusCode();
        responsePhrase = resp.getStatusLine().getReasonPhrase();
        
        if (responseCode == 200) {
          try {
            InputStream is = resp.getEntity().getContent();
            Model rdfModel = null;
            rdfModel = ModelFactory.createDefaultModel();
            rdfModel.read(is, null);
            
            sp = new ServiceProvider();
            
            extractDialogs(rdfModel, sp, true); //get creation dialogs
            extractDialogs(rdfModel, sp, false); //get selection dialogs
          }
          catch (Exception ex) {
            log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + ex.getMessage());
            ex.printStackTrace();
            responseCode = 500;
            responsePhrase = "Error in parsing response data!\\nSource isn't service provider or malformed!";
          }
        }
      }
    } 
    catch (ClientProtocolException e) {
      log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      e.printStackTrace();
      responseCode = 500;
      responsePhrase = e.getMessage();
    }
    catch (IOException e) {
      log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      e.printStackTrace();
      responseCode = 500;
      responsePhrase = e.getMessage();
    }
    
    return sp;
  }
  
  /**
   * Add link to Jira issue to remote application.
   * 
   * @param issueKey issue key in format PROJECT-#
   * @param projectId id of project
   * @param url link to remote application, where issue link will be sent to
   */
  public void addLinkToRemoteURL(String issueKey, Long projectId, String projectName, String url, OAuthAccessor accessor) {
    String currentMethod = "addLinkToRemoteURL";
    
    // Construct link to issue. The very same link should be also in object in remote
    // application.
    String issueLinkToAdd = 
        ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL) + 
        "/rest/jirarestresource/1.0/" + projectId.toString() + "/changeRequests/" + issueKey;
    
    Model rdfModel = null;
    boolean found = false;
    
    try {
      // First get object from remote application. Response is expected to be in rdf+xml format
      // and should contains list of jira issue links (relateChangeRequest).
      
      //OAuth needed
      HttpResponse resp = HTTP.get(
          url, "application/rdf+xml", null, null, null, 
          accessor, null, OAuthPhases.OAUTH_PHASE_3, null);
    
      if (resp != null) {
        responseCode = resp.getStatusLine().getStatusCode();
        responsePhrase = resp.getStatusLine().getReasonPhrase();
        
        if (responseCode == 200) {
          try {
            //get response body
            InputStream is = resp.getEntity().getContent();
            String body = getResponseBody(is);
            
            //initiate rdf model from response body
            rdfModel = ModelFactory.createDefaultModel();
            rdfModel.read(new ByteArrayInputStream(body.getBytes()), null);
            
            //find all occurences of "relatedChangeRequest" witin the response, resp. rdf model
            Property prop = rdfModel.createProperty("http://open-services.net/ns/cm#", "relatedChangeRequest");
            NodeIterator it = rdfModel.listObjectsOfProperty(prop);
            
            //go through the list
            while (it.hasNext()) {
              RDFNode node = it.next();
              Resource res = (Resource)node;
              String s = res.toString();
              // If there is a issue link in response, which corresponds to given issue link,
              // skip adding current link (found == true).
              if (s.compareTo(issueLinkToAdd) == 0) {
                found = true;
                break;
              }
            }
          }
          catch (Exception ex) {
            log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + ex.getMessage());
            ex.printStackTrace();
            responseCode = 500;
            responsePhrase = "Error in parsin object from remote application!";
            return;
          }
        }
        else {
          //getting remote app object failed
          return;
        }
      }
    }
    catch (ClientProtocolException e) {
      log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      e.printStackTrace();
      responseCode = 500;
      responsePhrase = e.getMessage();
    }
    catch (IOException e) {
      log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      e.printStackTrace();
      responseCode = 500;
      responsePhrase = e.getMessage();
    }

    if (found == false && rdfModel != null) {
      //given link was NOT found, so add it to model
      try {
        //add link to model - single property to existing main resource
        Resource res = rdfModel.getResource(url);
        Property addProp = rdfModel.createProperty("http://open-services.net/ns/cm#", "relatedChangeRequest");
        Resource link = rdfModel.createResource(issueLinkToAdd);
        
        Statement statement = rdfModel.createStatement(res, addProp, link);
        rdfModel.add(statement);
        
        
        if (projectName != null) {
          Property extProjectProp = rdfModel.createProperty("http://open-services.net#", "extProject");
          res.addProperty(extProjectProp, projectName);
        }
        
        //add link to model - details
        Resource res2 = rdfModel.createResource();
        
        Property subject = rdfModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "subject");
        Resource subjectRes = rdfModel.createResource(url);
        
        Property predicate = rdfModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "predicate");
        Resource predicateRes = rdfModel.createResource("http://open-services.net/ns/cm#relatedChangeRequest");
        
        Property object = rdfModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "object");
        Resource objectRes = rdfModel.createResource(issueLinkToAdd);
        
        Property type = rdfModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "type");
        Resource typeRes = rdfModel.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#Statement");
        
        Property title = rdfModel.createProperty("http://purl.org/dc/terms/title");
        Literal titleLit = rdfModel.createLiteral(issueKey);
        
        res2.addProperty(subject, subjectRes);
        res2.addProperty(predicate, predicateRes);
        res2.addProperty(object, objectRes);
        res2.addProperty(type, typeRes);
        res2.addProperty(title, titleLit);
        
        //write model back
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        rdfModel.write(os);
        os.flush();
        
        //send updated model back to remote application (same link)
        HttpResponse resp = HTTP.put(url, os.toString(), "application/rdf+xml", "application/rdf+xml", 
            null, null, accessor, JiraConstants.RELATED_CHANGE_REQUEST_URL_APPENDIX, 
            OAuthPhases.OAUTH_PHASE_3);
        
        if (resp != null) {
          responseCode = resp.getStatusLine().getStatusCode();
          responsePhrase = resp.getStatusLine().getReasonPhrase();
        }
      }
      catch (ClientProtocolException e) {
        log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
        e.printStackTrace();
        responseCode = 500;
        responsePhrase = e.getMessage();
      }
      catch (IOException e) {
        log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
        e.printStackTrace();
        responseCode = 500;
        responsePhrase = e.getMessage();
      }
    }
    else {
      //given link was found, so it is (probably) already linked in remote application
      responseCode = 404;
      responsePhrase = "Link to add already exists in remote application!";
    }
  }
  
  /**
   * Method removes link to Jira issue from remote application.
   * 
   * @param issueKey issue key (project-id), which represent issue
   * @param projectId project id, to which issue belongs
   * @param url link to object in remote application, where issue link should be stored
   */
  public void removeLinkFromRemoteURL(String issueKey, Long projectId, String url, OAuthAccessor accessor) {
    String currentMethod = "removeLinkFromRemoteURL";
    
    // Construct link to issue. The very same link should be also in object in remote
    // application.
    String issueLinkToRemove = 
        ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL) + 
        "/rest/jirarestresource/1.0/" + projectId.toString() + "/changeRequests/" + issueKey;

    Model rdfModel = null;
    boolean found = false;
    
    try {
      // First get object from remote application. Response is expected to be in rdf+xml format
      // and should contains list of jira issue links (relateChangeRequest).
      
      //OAuth needed
      HttpResponse resp = HTTP.get(
          url, "application/rdf+xml", null, null, null, 
          accessor, null, OAuthPhases.OAUTH_PHASE_3, null);
    
      responseCode = resp.getStatusLine().getStatusCode();
      responsePhrase = resp.getStatusLine().getReasonPhrase();
      
      if (responseCode == 200) {
        try {
          //get response body
          InputStream is = resp.getEntity().getContent();
          String body = getResponseBody(is);
          
          //initiate rdf model from response body
          rdfModel = ModelFactory.createDefaultModel();
          rdfModel.read(new ByteArrayInputStream(body.getBytes()), null);
          
          //find all occurences of "relatedChangeRequest" witin the response, resp. rdf model
          Property prop = rdfModel.createProperty("http://open-services.net/ns/cm#", "relatedChangeRequest");
          NodeIterator it = rdfModel.listObjectsOfProperty(prop);
          
          //go through the list
          while (it.hasNext()) {
            RDFNode node = it.next();
            Resource res = (Resource)node;
            String s = res.toString();
            // If there is a issue link in response, which corresponds to given issue link,
            // it is removed from model.
            if (s.compareTo(issueLinkToRemove) == 0) {
              rdfModel.removeAll(null, prop, node);
              found = true;
              break;
            }
          }
        }
        catch (Exception ex) {
          log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + ex.getMessage());
          ex.printStackTrace();
          responseCode = 500;
          responsePhrase = "Error in parsing object from remote application!";
          return;
        }
      }
      else
        return; //http failed
    }
    catch (ClientProtocolException e) {
      log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      e.printStackTrace();
      responseCode = 500;
      responsePhrase = e.getMessage();
    }
    catch (IOException e) {
      log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      e.printStackTrace(); 
      responseCode = 500;
      //responsePhrase = "Connection to remote application failed!";
      responsePhrase = e.getMessage();
      return;
    }

    if (found == true && rdfModel != null) {
      //given link was found and removed from model
      try {
        //write model back
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        rdfModel.write(os);
        os.flush();
        
        //send updated model back to remote application (same link)
        HttpResponse resp = HTTP.put(url, os.toString(), "application/rdf+xml", "application/rdf+xml", 
            null, null, accessor, JiraConstants.RELATED_CHANGE_REQUEST_URL_APPENDIX,
            OAuthPhases.OAUTH_PHASE_3);
        
        responseCode = resp.getStatusLine().getStatusCode();
        responsePhrase = resp.getStatusLine().getReasonPhrase();
      }
      catch (IOException e) {
        log.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
        e.printStackTrace();
        
        responseCode = 500;
        responsePhrase = "Connection to remote application failed!";
        return;
      }
    }
    else {
      //given link wasn't found, so it (probably) doesn't exist in object in remote application
      responseCode = 404;
      responsePhrase = "Link to remove was not found!";
    }
  }
  

  /**
   * Get a OAuth request token from remote server defined by requestTokenURL
   * @param requestTokenURL the URL for getting OAuth request token
   * @param accessor OAuth accessor
   * @return OAuth request token
   */
  private String [] oAuthGetRequestToken(String requestTokenURL, OAuthAccessor accessor) {
    
    try {
      HttpResponse response = HTTP.post(
          requestTokenURL, "", null, null, null, null, accessor, null, OAuthPhases.OAUTH_PHASE_1);
      
      if (response != null) {
        responseCode = response.getStatusLine().getStatusCode();
        responsePhrase = response.getStatusLine().getReasonPhrase();
        
        if (responseCode == 200) {
          String body = getResponseBody(response.getEntity().getContent());
        
          String [] result = new String[2];
          String [] elements = body.split("&");
          int idx = elements[0].indexOf("=");  
          result[0] = elements[0].substring(idx+1, elements[0].length());
          idx = elements[1].indexOf("=");  
          result[1] = elements[1].substring(idx+1, elements[1].length());
          return result;
        }
      }
    } 
    catch (ClientProtocolException e) {
      log.error(CURRENT_CLASS + ".oAuthGetRequestToken Exception: " + e.getMessage());
      e.printStackTrace();
      return null;
    } 
    catch (IOException e) {
      log.error(CURRENT_CLASS + ".oAuthGetRequestToken Exception: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
    
    return null;
  }
  
  /**
   * Get a OAuth access token from remote server defined by accessTokenURL
   * @param accessTokenURL the URL for getting OAuth access token
   * @param accessor OAuth accessor
   * @param verifier verification code
   * @return OAuth access  token
   */
  private String [] oAuthGetAccessToken(String accessTokenURL, OAuthAccessor accessor, String verifier) {
    try {
      HttpResponse response = HTTP.get(
          accessTokenURL, null, "text/xml", null, null, accessor, verifier, OAuthPhases.OAUTH_PHASE_2, null);

      if (response != null) {
        responseCode = response.getStatusLine().getStatusCode();
        responsePhrase = response.getStatusLine().getReasonPhrase();
        
        if (responseCode == 200) {
          String body = getResponseBody(response.getEntity().getContent());
          
          String [] result = new String[2];
          String [] elements = body.split("&");
          int idx = elements[0].indexOf("=");  
          result[0] = elements[0].substring(idx+1, elements[0].length());
          idx = elements[1].indexOf("=");  
          result[1] = elements[1].substring(idx+1, elements[1].length());
          return result;
        }
      }
    } 
    catch (ClientProtocolException e) {
      log.error(CURRENT_CLASS + ".oAuthGetAccessToken Exception: " + e.getMessage());
      e.printStackTrace();
    } 
    catch (IOException e) {
      log.error(CURRENT_CLASS + ".oAuthGetAccessToken Exception: " + e.getMessage());
      e.printStackTrace();
    }
    
    return null;
  }
  
  /**
   * Methods starts 1st phase of OAuth dance - getting request token from provider.
   * Note: After method gets request token back in response, OAuth dance continue on callback url
   * (@see com.ericsson.jira.oslc.services.OAuthServices#authorizationcallbackDialog).
   * @param consumer object with consumer information (consumer key, consumer secret, ...)
   * @param callbackURL link, where provider responses after request token is sent
   * @return accessor object with request token (used in next phase)
   */
  public OAuthAccessor oAuthDancePhase1(OAuthConsumer consumer, String callbackURL) {
    OAuthServiceProvider provider = new OAuthServiceProvider(null, null, null);
    OAuthConsumer c = new OAuthConsumer(callbackURL, consumer.consumerKey, consumer.consumerSecret, provider);
    OAuthAccessor a = new OAuthAccessor(c);
    
    String [] request = this.oAuthGetRequestToken(consumer.serviceProvider.requestTokenURL, a);
        
    if (request != null) {
      a.requestToken = request[0];
      a.tokenSecret = request[1];
    }
    
    return a;
  }
  
  /**
   * Methods performs 2nd phase of OAuth dance - getting access token.
   * Access token is stored to accessor.
   * 
   * @param consumer consumer object with consumer information (consumer key, consumer secret, ...)
   * @param accessor accessor object with request token (from previous phase)
   */
  public void oAuthDancePhase2(OAuthConsumer consumer, OAuthAccessor accessor, String verifier) {
    String [] access = this.oAuthGetAccessToken(
        consumer.serviceProvider.accessTokenURL, accessor, verifier);
    
    if (access != null) {
      //put access key to accessor
      
      accessor.accessToken = access[0];
      accessor.tokenSecret = access[1];
    }
  }
  
  /**
   * Updates the remote resource
   * @param uri the URI of remote resource
   * @param body the body of PUT request.
   * @param username the user name of the user who will update the remote resource
   * @param password the password of the user who will update the remote resource
   * @param headers HTTP header of PUT request
   * @return the response of the request
   * @throws ClientProtocolException
   * @throws IOException
   */
  public HttpResponse updateRemoteResource(String uri, String body, String username, String password, Map<String, String> headers) throws ClientProtocolException, IOException {
    HttpResponse resp = null;
    log.debug("Client.updateRemoteResource");

    resp = HTTP.put(uri, body, "application/rdf+xml", "application/rdf+xml", username, password, null, null, null, headers);

    return resp;
  }
  
  public HttpResponse getRemoteResource(String uri, String username, String password, Map<String, String> headers ) throws ClientProtocolException, IOException {
    HttpResponse resp = null;
    log.debug("Client.getRemoteResource for uri: " + uri);
    resp = HTTP.get(uri, "application/rdf+xml",  username, password, headers);

    return resp;
  }

}
