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

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.Accessor;
import net.java.ao.Mutator;


/**
 * Class used to store root services or service provider catalogs in database.
 * 
 * NOTE: Class name should not be too long, 
 *   because database table name must not exceed 30 chars!
 * NOTE: Class must be registered in ActiveObject engine:
 *   remember to add definition to <ao key="ao-module"> in atlasian-plugin.xml! 
 */
@Preload
public interface RootServicesEntity extends Entity {
  
  String getTitle();
  void setTitle(String title);
  
  @Accessor("rsuri")
  String getRootServicesURI();
  @Mutator("rsuri")
  void setRootServicesURI(String rootServicesURI);
  
  String getConsumerSecret();
  void setConsumerSecret(String oAuthSecret);
  
  String getConsumerKey();
  void setConsumerKey(String consumerKey);
  
  //OAuth mandatory values:
  
  @Accessor("requesttokenuri")
  String getRequestTokenURI();
  @Mutator("requesttokenuri")
  void setRequsetTokenURI(String uri);
  
  @Accessor("userauthuri")
  String getUserAuthURI();
  @Mutator("userauthuri")
  void setUserAuthUri(String uri);
  
  @Accessor("accesstokenuri")
  String getAccessTokenURI();
  @Mutator("accesstokenuri")
  void setAccessTokenURI(String uri);
  
  @Accessor("oauthdomain")
  String getOAuthDomain();
  @Mutator("oauthdomain")
  void setOAuthDomain(String uri);
  
  //distinguish between root services or service provider catalog
  boolean isRootServices();
  void setRootServices(boolean value);
}
