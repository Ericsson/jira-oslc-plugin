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

import java.util.List;

import org.eclipse.lyo.server.oauth.core.consumer.AbstractConsumerStore;
import org.eclipse.lyo.server.oauth.core.consumer.ConsumerStoreException;
import org.eclipse.lyo.server.oauth.core.consumer.LyoOAuthConsumer;


/**
 * This class implements OAuth consumer store with data persistence using Active Objects.
 * 
 * Each LyoOAuthCosumer instance is stored as active object (OAuthConsmrEntity).
 * 
 * Consumers (LyoOAuthCosumer) are cached for get activities.
 * 
 * NOTES:
 * Class inherits from AbstractConsumerStore, which uses caching consumers in internal Map.
 * Legend:
 *  - cache ... Map
 *  - db ...... ActiveObjects
 * 
 */
public class AOConsumerStore extends AbstractConsumerStore {
  
  /**
   * c'tor - loads all consumers from db
   */
  public AOConsumerStore() {
    this.loadAll();
  }

  /**
   * loads all consumers from db
   */
  private void loadAll() {
    //load all saved consumers from db
    AOManager mngr = AOManager.getInstance();
    final List<OAuthConsmrEntity> entities = mngr.all(OAuthConsmrEntity.class);
        
    //transfer loaded entities to cache
    for (OAuthConsmrEntity e : entities) {
      LyoOAuthConsumer consumer = new LyoOAuthConsumer(e.getConsumerKey(), e.getConsumerSecret());
      consumer.setName(e.getName());
      consumer.setProvisional(e.isProvisional());
      //put to cache
      this.add(consumer);
    }
  }
  
  /**
   * Saves a consumer to db
   * @param consumer the consumer which will be saved
   */
  private void saveActiveObject(final LyoOAuthConsumer consumer) {
    AOManager mngr = AOManager.getInstance();
    mngr.createOAuthConsumerEntity(
            consumer.consumerKey,
            consumer.consumerSecret,
            consumer.getName(),
            consumer.isProvisional());
  }
  
  /**
   * Removes a consumer from db
   * @param consumerKey identifier of consumer
   */
  private void removeActiveObject(final String consumerKey) {
    AOManager mngr = AOManager.getInstance();
    mngr.remove(OAuthConsmrEntity.class, "CONSUMER_KEY = ?", consumerKey);
  }
  
  /**
   * Updates the parameters of a consumers
   * @param consumer the parameters of the cosumer
   */
  private void updateActiveObject(final LyoOAuthConsumer consumer) {
    AOManager mngr = AOManager.getInstance();
    mngr.updateOAuthConsumerEntity(
            consumer.consumerKey,
            consumer.consumerSecret,
            consumer.getName(),
            consumer.isProvisional());
  }
  
  /**
   * Saves a consumer to db and adds to the cache
   * @param consumer the consumer which will be saved
   */
  @Override
  public LyoOAuthConsumer addConsumer(LyoOAuthConsumer consumer) throws ConsumerStoreException {
    //store consumer to db
    this.saveActiveObject(consumer);
    //store consumer to cache and return
    return this.add(consumer);
  }

  /**
   * Removes a consumer from db and from th cache
   * @param consumerKey identifier of consumer
   */
  @Override
  public LyoOAuthConsumer removeConsumer(String consumerKey) throws ConsumerStoreException {
    //remove consumer from db
    this.removeActiveObject(consumerKey);
    //remove consumer from cache and return
    return this.remove(consumerKey);
  }

  /**
   * Updates the parameters of a consumers. The changes are propagated to db and to the cache
   * @param consumer the parameters of the consumer
   */
  @Override
  public LyoOAuthConsumer updateConsumer(LyoOAuthConsumer consumer) throws ConsumerStoreException {
    //update consumer in db
    this.updateActiveObject(consumer);
    //update consumer in cache (putting data to map with the same key) and return
    return this.add(consumer);
  }

  /**
   * Do nothing, because it is not need.
   */
  @Override
  public void closeConsumerStore() {
    //Empty, because it is not need.
  }
}
