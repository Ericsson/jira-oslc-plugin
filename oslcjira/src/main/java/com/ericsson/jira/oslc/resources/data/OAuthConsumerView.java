package com.ericsson.jira.oslc.resources.data;

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
import java.util.Collection;
import java.util.List;

import org.eclipse.lyo.server.oauth.core.consumer.LyoOAuthConsumer;

/**
 * Data class which propagates the consumers to velocity template
 * 
 *
 */
public class OAuthConsumerView {
  List<LyoOAuthConsumer> provisionalConsumers;
  List<LyoOAuthConsumer> activeConsumers;
  
  public OAuthConsumerView(Collection<LyoOAuthConsumer> consumers){
    provisionalConsumers = new ArrayList<LyoOAuthConsumer>();
    activeConsumers = new ArrayList<LyoOAuthConsumer>();
    categorizeConsumers(consumers);
   
   }
  
  /**
   * It divides the consumers to two groups according to if the consumer is provisional or not
   * @param consumers
   */
  private void categorizeConsumers(Collection<LyoOAuthConsumer> consumers){
    if(consumers == null){
      return;
    }
    
    for (LyoOAuthConsumer consumer: consumers) {
      if(consumer.isProvisional()){
        provisionalConsumers.add(consumer);
      }else{
        activeConsumers.add(consumer);
      }
    }
  }

  /**
   * Returns the list of provisional consumer
   * @return the list of provisional consumer
   */
  public List<LyoOAuthConsumer> getProvisionalConsumers(){
    return provisionalConsumers;
  }
  
  /**
   * Returns the list of active consumer
   * @return the list of active consumer
   */
  public List<LyoOAuthConsumer> getActiveConsumers(){
    return activeConsumers;
  }
}
