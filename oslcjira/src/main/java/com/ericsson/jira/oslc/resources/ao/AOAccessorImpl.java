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

import com.atlassian.activeobjects.external.ActiveObjects;

/**
 * This class serves as getter of ActiveObjects engine instance.
 * 
 * It seems that only way, how to get ActiveObjects instance, is injection. 
 * It is not problem, when using it in components and servlets (because 
 * those are defined in plugin descriptor and injection happens automatically).
 * However if ActiveObjects is needed somewhere else (e.g. AOConsumerStore),
 * where injection can't be used, ActiveObjects must be provided in some other
 * way. 
 * 
 * AOAccessor (resp. AOAccessorImpl) enables to get to ActiveObjects from
 * "somewhere else". AOAccessor is component registered in plugin descriptor,
 * so it is injected with ActiveObjects instance and provide API do get it.
 * 
 * Refer to: http://www.j-tricks.com/tutorials/active-objects-injection
 *
 */
public class AOAccessorImpl implements AOAccessor {

  private final ActiveObjects ao;
  
  public AOAccessorImpl(ActiveObjects ao) {
    this.ao = ao;
  }
  
  @Override
  public ActiveObjects getActiveObjects() {
    return this.ao;
  }
}
