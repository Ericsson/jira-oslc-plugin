package com.ericsson.jira.oslc.handlers;

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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;

import org.eclipse.lyo.server.oauth.core.OAuthConfiguration;
import org.eclipse.lyo.server.oauth.core.consumer.ConsumerStore;
import org.eclipse.lyo.server.oauth.core.consumer.ConsumerStoreException;
import org.eclipse.lyo.server.oauth.core.consumer.LyoOAuthConsumer;
import org.eclipse.lyo.server.oauth.core.token.LRUCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.jira.oslc.constants.JiraConstants;
import com.ericsson.jira.oslc.oslcclient.Client;
import com.ericsson.jira.oslc.resources.RootServices;
import com.ericsson.jira.oslc.resources.ao.AOConsumerStore;
import com.ericsson.jira.oslc.resources.ao.AOManager;
import com.ericsson.jira.oslc.resources.ao.RootServicesEntity;
import com.ericsson.jira.oslc.utils.OSLCUtils;

/**
 * A class contains the methods for work with OAuth consumer store, Consumer, Accessor and other OAuth stuff.
 *
 */
public class OAuthHandler {
  private static Logger logger = LoggerFactory.getLogger(OAuthHandler.class);
  private static final String CURRENT_CLASS = "OAuthHandler";
  private static LRUCache<String, RootServices> rootServicesCache = new LRUCache<String, RootServices> (10);

  /**
   * It loads the consumer store and saves it to OAuth configuration
   * @param config OAuth configuration where the cosnumer store will be put to
   * @throws ConsumerStoreException
   * @throws ClassNotFoundException
   * @throws SQLException
   */
  public static void loadOAuthConsumers(OAuthConfiguration config) throws ConsumerStoreException, ClassNotFoundException, SQLException {
    config.setConsumerStore(loadConsumerStore());
    logger.info("OAuth consumers added");
  }
  
  /**
   * Loads the consumer store from DB
   * @return the consumer store 
   * @throws ConsumerStoreException
   * @throws ClassNotFoundException
   * @throws SQLException
   */
  private static ConsumerStore loadConsumerStore() throws ConsumerStoreException, ClassNotFoundException, SQLException{
    AOConsumerStore aoConsumerStore = new AOConsumerStore();
    return aoConsumerStore;
  }

  /**
   * Adds a consumer to the consumer store
   * @param consumer the consumer containing its name, if it's provisional
   * @return consumer store containing all the consumers including added consumer
   * @throws ConsumerStoreException
   * @throws ClassNotFoundException
   * @throws SQLException
   */
  public static ConsumerStore addConsumer(LyoOAuthConsumer consumer) throws ConsumerStoreException, ClassNotFoundException, SQLException{
    String currentMethod = "addConsumer";
    if(consumer == null){
      logger.debug(CURRENT_CLASS + "." + currentMethod + " Consumer is null");
      return loadConsumerStore();
    }
    
    ConsumerStore consumerStore = loadConsumerStore();
    consumerStore.addConsumer(consumer);
    
    logger.debug(CURRENT_CLASS + "." + currentMethod + " consumer added");
    return consumerStore;
  }
  
  /**
   * Removes a consumer defined by consumerKey.
   * @param consumerKey the key of consumer
   * @return consumer store containing all the consumers without removed consumer
   * @throws ClassNotFoundException
   * @throws SQLException
   * @throws ConsumerStoreException
   */
  public static ConsumerStore removeConsumer(String consumerKey) throws ClassNotFoundException, SQLException, ConsumerStoreException {
    String currentMethod = "removeConsumer";
    logger.debug(CURRENT_CLASS + "." + currentMethod + " Consumer key: " + consumerKey);
    
    ConsumerStore consumerStore = loadConsumerStore();
    if (consumerKey != null) {
      consumerStore.removeConsumer(consumerKey);
    }
   
    logger.debug(CURRENT_CLASS + "." + currentMethod + " consumer removed");
    return consumerStore;

 }
  
  /**
   * Updates a consumer defined by consumerKey. It's needed to specify if the consumer is provisional or not
   * @param consumerKey the key of consumer
   * @param provisional specifies if the consumer is provisional or
   * @return consumer store containing all the consumers including updated the consumer
   * @throws ClassNotFoundException
   * @throws SQLException
   * @throws ConsumerStoreException
   */
  public static ConsumerStore updateConsumer(String consumerKey, boolean provisional) throws ClassNotFoundException, SQLException, ConsumerStoreException {
    String currentMethod = "updateConsumer";
    logger.debug(CURRENT_CLASS + "." + currentMethod + " Consumer key: " + consumerKey + ", provisional = " + provisional);
    
    ConsumerStore consumerStore = loadConsumerStore();
    if (consumerKey != null) {
      LyoOAuthConsumer consumer = consumerStore.getConsumer(consumerKey);
      consumer.setProvisional(provisional);
      consumerStore.updateConsumer(consumer);
    }
   
    logger.debug(CURRENT_CLASS + "." + currentMethod + " consumer updated");
    return consumerStore;
 }
  
  /**
   * Returns the consumer for entered resource. It goes through all registered
   * rootservices and tries to match oAuthDomain of rootservices with URI of
   * entered resource
   * 
   * @param resourceURI uri of Resource
   * @return consumer for entered resource
   */
  public static OAuthConsumer getConsumer(String resourceURI) {
    AOManager mngr = AOManager.getInstance();
    List<RootServicesEntity> rootServicesList = mngr.all(RootServicesEntity.class);
    List<RootServicesEntity> addToCache = new ArrayList<RootServicesEntity>();

    for (RootServicesEntity rootServicesEntity : rootServicesList) {
      RootServices rootServices = rootServicesCache.get(rootServicesEntity.getRootServicesURI());
      if (rootServices != null) {
        if (matchAuthDomain(resourceURI, rootServices.getOAuthDomain())) {
          return getConsumer(rootServices, rootServicesEntity);
        }
      } else {
        addToCache.add(rootServicesEntity);
      }
    }

    for (RootServicesEntity rootServicesEntity : addToCache) {

      Client client = new Client();
      
      //If entity is root services, then call to Client.getRootServicesDetails. 
      //If entity is catalog, 1st try get values from entity. If values are not set in catalog,
      //then new Client function will be called, which fetch OAuth values from catalog.
      RootServices rootServices = null;
      if (rootServicesEntity.isRootServices() == true) {
        rootServices = client.getRootServicesDetails(rootServicesEntity.getRootServicesURI());
      }
      else {
        if (OSLCUtils.isNullOrEmpty(rootServicesEntity.getConsumerKey()) == false &&
            OSLCUtils.isNullOrEmpty(rootServicesEntity.getConsumerSecret()) == false &&
            OSLCUtils.isNullOrEmpty(rootServicesEntity.getOAuthDomain()) == false &&
            OSLCUtils.isNullOrEmpty(rootServicesEntity.getRequestTokenURI()) == false &&
            OSLCUtils.isNullOrEmpty(rootServicesEntity.getAccessTokenURI()) == false &&
            OSLCUtils.isNullOrEmpty(rootServicesEntity.getUserAuthURI()) == false) {
          
          rootServices = new RootServices();
          rootServices.setOAuthAccessTokenURL(rootServicesEntity.getAccessTokenURI());
          rootServices.setOAuthDomain(rootServicesEntity.getOAuthDomain());
          rootServices.setOAuthRequestTokenURL(rootServicesEntity.getRequestTokenURI());
          rootServices.setOAuthUserAuthorizationURL(rootServicesEntity.getUserAuthURI());
        }
        else {
          rootServices = client.getRootServicesDetailsFromCatalog(rootServicesEntity.getRootServicesURI());
        }
      }
      
      if (rootServices != null) {
        rootServicesCache.put(rootServicesEntity.getRootServicesURI(), rootServices);
        if (matchAuthDomain(resourceURI, rootServices.getOAuthDomain())) {
          return getConsumer(rootServices, rootServicesEntity);
        }
      }
    }
    
    return null;
    
  }
  
  /**
   * Matches entered URI with oAuth domain. If OAuth domain is a part of URI then return true, else false 
   * @param uri uri of resource
   * @param oAuthDomain OAthDomain of rootservices
   * @return true if OAuth domain is part of URI, else false
   */
  public static boolean matchAuthDomain(String uri, String oAuthDomain) {
    return (uri != null && !uri.isEmpty() && oAuthDomain != null && !oAuthDomain.isEmpty() && uri.startsWith(oAuthDomain));
  }
  
  /**
   * Creates and returns the consumer based on input parameters
   * @param rootServices - rootservices which has been fetched from friend's side
   * @param rootServicesEntity - rootservices of registered friend
   * @return consumer based on input parameters
   */
  public static OAuthConsumer getConsumer(RootServices rootServices, RootServicesEntity rootServicesEntity){
    OAuthServiceProvider provider = new OAuthServiceProvider(rootServices.getOAuthRequestTokenURL(), rootServices.getOAuthUserAuthorizationURL(), rootServices.getOAuthAccessTokenURL());
    OAuthConsumer oauthConsumer = new OAuthConsumer("", rootServicesEntity.getConsumerKey(), rootServicesEntity.getConsumerSecret(), provider);
    return oauthConsumer;
  }  
  
  /**
   * Returns a consumer according to defined a consumer key
   * @param consumerKey a key of consumer which is returned
   * @return a consumer according to defined a consumer key
   * @throws ClassNotFoundException
   * @throws SQLException
   * @throws ConsumerStoreException
   */
  public static LyoOAuthConsumer getConsumerByKey(String consumerKey) throws ClassNotFoundException, SQLException, ConsumerStoreException {
    String currentMethod = "getConsumerByKey";
    logger.debug(CURRENT_CLASS + "." + currentMethod + " Consumer key: " + consumerKey);
    
    ConsumerStore consumerStore = loadConsumerStore();
    if (consumerKey != null) {
      return consumerStore.getConsumer(consumerKey);
    }
    return null;
  }
  
  public static OAuthAccessor getAccessorFromSession(HttpSession session, String consumerKey, String url) {
    OAuthAccessor ac = null;
    try {
      String oAuthAccessorSession = JiraConstants.SESSION_OAUTHACCESSOR + consumerKey;
      ac = (OAuthAccessor) session.getAttribute(oAuthAccessorSession);
    } 
    catch (ClassCastException e) {
      logger.debug(CURRENT_CLASS + ".getAccessorFormSession Exception: " + e.getMessage());
      e.printStackTrace();
    }
    return ac;
  }
  
  /**
   * It saves an accessor to the session. The accessor are saved to the session according to the consumer key
   * @param session the session which contains the accessors
   * @param ac accessor to save
   * @param consumerKey a key consumers 
   */
  public static void saveAccessorToSession(HttpSession session, OAuthAccessor ac, String consumerKey) {
    String oAuthAccessorSession = JiraConstants.SESSION_OAUTHACCESSOR + consumerKey;
    session.setAttribute(oAuthAccessorSession, ac);
  }
  
  /**
   * Clear the cache of rootservices.
   */
  public static void clearRootServicesCache(){
    if(rootServicesCache != null){
      rootServicesCache.clear();
    }

  }
}
