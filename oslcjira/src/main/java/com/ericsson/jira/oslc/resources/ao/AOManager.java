package com.ericsson.jira.oslc.resources.ao;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.java.ao.RawEntity;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * 
 * A class offers methods for operations with Active objects
 */
public class AOManager {
  private static AOManager instance = null;
  private final ActiveObjects ao; 
  
  protected AOManager() {
    AOAccessor aoAccessor = ComponentAccessor.getOSGiComponentInstanceOfType(AOAccessor.class);
    this.ao = aoAccessor.getActiveObjects();
  }
  
  public static AOManager getInstance() {
     if(instance == null) {
        instance = new AOManager();
     }
     return instance;
  }
  
  /**
   * Saves rootservices to the db
   * @param title the title of rootservises
   * @param uri the uri of rootservises
   * @param consumerSecret the consumer secret of rootservises
   * @param consumerKey the cosumer key of rootservises
   */
  public synchronized void createRootServicesEntity(final String title, final String uri, 
      final String consumerSecret, final String consumerKey) {
    ao.executeInTransaction(
        new TransactionCallback<Void>() {

          @Override
          public Void doInTransaction() {
            final RootServicesEntity e = ao.create(RootServicesEntity.class);
            e.setTitle(title);
            e.setRootServicesURI(uri);
            e.setConsumerSecret(consumerSecret);
            e.setConsumerKey(consumerKey);
            e.setRootServices(true);
            e.save();
            return null;
          }
        }
    );
  }
  
  /**
   * Saves the OSLC service provider catalog to db
   * @param title the title of the provider
   * @param uri the uri of the provider
   * @param consumerSecret the consumer secret of the provider
   * @param consumerKey the consumer key of the provider
   * @param requestTokenURI the request token URI of the provider
   * @param userAuthURI the User Authorization URI of the provider
   * @param accessTokenURI the Access Token URI of the provider
   * @param oAuthDomain the domain of the provider
   */
  public synchronized void createServiceProviderCatalogEntity(final String title, final String uri, 
      final String consumerSecret, final String consumerKey, final String requestTokenURI, 
      final String userAuthURI, final String accessTokenURI, final String oAuthDomain) {
    ao.executeInTransaction(
        new TransactionCallback<Void>() {

          @Override
          public Void doInTransaction() {
            final RootServicesEntity e = ao.create(RootServicesEntity.class);
            e.setTitle(title);
            e.setRootServicesURI(uri);
            e.setConsumerSecret(consumerSecret);
            e.setConsumerKey(consumerKey);
            e.setAccessTokenURI(accessTokenURI);
            e.setOAuthDomain(oAuthDomain);
            e.setRequsetTokenURI(requestTokenURI);
            e.setUserAuthUri(userAuthURI);
            e.setRootServices(false);
            e.save();
            return null;
          }
        }
    );
  }
  
  /**
   * Saves a service provider do db
   * @param serverId the id of remote server
   * @param serverTitle the title of remote server
   * @param title title of service provider
   * @param uri the uri of service provider
   */
  public synchronized void createServiceproviderEntiry(final int serverId, final String serverTitle, 
      final String title, final String uri) {
    ao.executeInTransaction(
        new TransactionCallback<Void>() {

          @Override
          public Void doInTransaction() {
            final ServiceProvEntity e = ao.create(ServiceProvEntity.class);
            e.setServerId(serverId);
            e.setServerTitle(serverTitle);
            e.setTitle(title);
            e.setURI(uri);
            e.save();
            return null;
          }
        }
    );
  }

  /**
   * Saves OAuth consumer to db
   * @param key the key of consumer
   * @param secret OAuth secret
   * @param name the name of consumer
   * @param provisional - true -it's permanent consumer, false it's temporary consumer - waiting for approve 
   */
  public synchronized void createOAuthConsumerEntity(final String key, final String secret, 
      final String name, final boolean provisional) {
    ao.executeInTransaction(
        new TransactionCallback<Void>() {

          @Override
          public Void doInTransaction() {
            final OAuthConsmrEntity e = ao.create(OAuthConsmrEntity.class);
            e.setConsumerKey(key);
            e.setConsumerSecret(secret);
            e.setName(name);
            e.setProvisional(provisional);
            e.save();
            return null;
          }
        }
    );
  }

  /**
   * Updates OAuth consumer in db
   * @param key the key of consumer
   * @param secret OAuth secret
   * @param name the name of consumer
   * @param provisional - true -it's permanent consumer, false it's temporary consumer - waiting for approve 
   */
  public synchronized void updateOAuthConsumerEntity(final String key, final String secret, 
      final String name, final boolean provisional) {
    ao.executeInTransaction(
        new TransactionCallback<Void>() {

          @Override
          public Void doInTransaction() {
            final OAuthConsmrEntity [] e = 
                ao.find(OAuthConsmrEntity.class, "CONSUMER_KEY = ?", key);
            if (e.length > 0) {
              e[0].setConsumerSecret(secret);
              e[0].setName(name);
              e[0].setProvisional(provisional);
              e[0].save();
            }
            return null;
          }
        }
    );
  }
  
  /**
   * General method for removing objects from db
   * @param type class type
   * @param key unique id of object in db
   */
  public synchronized <T extends RawEntity<K>, K> void remove(final Class<T> type, final K key) {
    ao.executeInTransaction(
        new TransactionCallback<Void>() {

          @Override
          public Void doInTransaction() {
            final T e = ao.get(type, key);
            ao.delete(e);
            return null;
          }
        }
    );
  }
  
  /**
   * General method for removing objects from db. The removing object is found in db by search query
   * @param type class type
   * @param query removing object is found in db by search query
   * @param params the columns which will be returned
   */
  public synchronized <T extends RawEntity<K>, K> void remove(final Class<T> type, 
      final String query, final Object ... params) {
    ao.executeInTransaction(
        new TransactionCallback<Void>() {

          @Override
          public Void doInTransaction() {
            final T [] e = ao.find(type, query, params);
            if (e.length > 0) {
              ao.delete(e[0]);
            }
            return null;
          }
        }
    );
  }
  
  /**
   * Check if the object exist in db. The object is searched in db by search query
   * @param type class type
   * @param query the object is found in db by search query
   * @param params the columns which will be returned
   * @return true - object is found, otherwise false
   */
  public synchronized <T extends RawEntity<K>, K> boolean exist(final Class<T> type, 
      final String query, final Object ... params) {
    boolean result = ao.executeInTransaction(
        new TransactionCallback<Boolean>() {

          @Override
          public Boolean doInTransaction() {
            if (ao.count(type, query, params) == 1)
              return true;
            return false;
          }
        }
    );
    return result;
  }
  
  /**
   * Returns all objects defined by class type. The class type = name of table in db
   * @param type class type which specifies the returned objects
   * @return all object defined by class type
   */
  public synchronized <T extends RawEntity<K>, K> List<T> all(final Class<T> type) {
    final List<T> result = new ArrayList<T>();
    
    ao.executeInTransaction(
        new TransactionCallback<Void>() {

          @Override
          public Void doInTransaction() {
            for (T e : ao.find(type))
              result.add(e);
            return null;
          }
          
        }
    );
    
    return result;
  }
  
  /**
   * Returns the configuration from ConfigClobEntity. The table contains the configuration e.g. represented by xml
   * @param key the unique if of configuration
   * @return the configuration
   */
  public synchronized String getConfigClobValue(final String key) {
    final List<String> values = new ArrayList<String>();
    ao.executeInTransaction(
        new TransactionCallback<Void>() {

          @Override
          public Void doInTransaction() {
            final ConfigClobEntity [] entities = 
                ao.find(ConfigClobEntity.class, "KEY = ?", key);

            if (entities.length > 0) {
              values.add(entities[0].getValue());
            }
            return null;
          }
        }
    );
    
    return (values.size() > 0)?values.get(0):null;
  }
  
  /**
   * Returns all configurations from ConfigClobEntity. The table contains the configurations e.g. represented by xml
   * @return all configurations
   */
  public synchronized Map<String,String> getConfigValues() {
    final Map<String,String> values = new HashMap<String,String>();
    ao.executeInTransaction(
        new TransactionCallback<Void>() {

          @Override
          public Void doInTransaction() {
            final ConfigEntity [] entities = 
                ao.find(ConfigEntity.class);
            
            for (ConfigEntity entity : entities) {
              values.put(entity.getKey(), entity.getValue());
            }

            return null;
          }
        }
    );
    return values;
  }
  
  /**
   * Saves the configurations to db
   * @param values the configuration to save
   */
  public synchronized void saveConfigValues(final Map<String,String> values) {
    ao.executeInTransaction(
        new TransactionCallback<Void>() {

          @Override
          public Void doInTransaction() {
            Set<Entry<String, String>> entrySet = values.entrySet();
            for (Entry<String, String> entry : entrySet) {
              if(entry.getKey() == null){
                continue;
              }
              final ConfigEntity [] entities = 
                  ao.find(ConfigEntity.class, "KEY = ?", entry.getKey());
              
              ConfigEntity entity;
              if (entities.length == 0) {
                entity = ao.create(ConfigEntity.class);
                entity.setKey(entry.getKey());
              }else{
                entity = entities[0];
              }
              
              entity.setValue(entry.getValue());
              entity.save();
            }

            return null;
          }
        }
    );
}
  
  /**
   * Save the large configuration (e.g. represented by xml) to db 
   * @param key the unique id of configuration
   * @param value the configuration
   */
  public synchronized void saveConfigClobValue(final String key, final String value) {
    if(key == null){
      return;
    }

    ao.executeInTransaction(
        new TransactionCallback<Void>() {

          @Override
          public Void doInTransaction() {
            final ConfigClobEntity [] entities = 
                ao.find(ConfigClobEntity.class, "KEY = ?", key);
            
            ConfigClobEntity entity;
            if (entities.length == 0) {
              entity = ao.create(ConfigClobEntity.class);
              entity.setKey(key);
            }else{
              entity = entities[0];
            }
            
            entity.setValue(value);
            entity.save();

            return null;
          }
        }
    );
  }
}
