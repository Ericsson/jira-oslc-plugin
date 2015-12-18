package com.ericsson.jira.oslc.sync;

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

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import com.ericsson.eif.leansync.mapping.SyncConfigLoader;
import com.ericsson.eif.leansync.mapping.data.SyncConfiguration;
import com.ericsson.jira.oslc.resources.ao.AOManager;

/**
 * Sync configuration -  handles LeanSync configuration including loading from DB
 */
@XmlRootElement
public class SyncConfig {
  private static SyncConfig instance;
  private Map<Object, SyncConfiguration> configurationMap;
  public static final String SYNC_CONFIG_PROPERTY = "syncConfig";

  /**
   * Get a instance of the configuration - singleton
   * @return the configuration
   * @throws Exception
   */
  public static SyncConfig getInstance() throws Exception {
    if (instance == null) {
      instance = new SyncConfig(true);
    }
    return instance;
  }
  
  private SyncConfig() throws Exception {
  }
  
  /**
   * Get a instance of the sync configuration - singleton
   * @param loadConfig - true - the sync configuration will be loaded from DB, false - it creates empty configuration
   * @return the sync configuration
   * @throws Exception
   */
  public static SyncConfig getInstance(boolean loadConfig) throws Exception {
    if (instance == null) {
      instance = new SyncConfig(loadConfig);
    }
    return instance;
  }
  
  /**
   * It creates empty sync configuration and if the argument loadConfig is set on 'true' then the configuration
   * is loaded from DB
   * @param loadConfig the sync configuration will be loaded from DB, false - it creates empty configuration
   * @throws Exception
   */
  private SyncConfig(boolean loadConfig) throws Exception {
    configurationMap = new HashMap<Object, SyncConfiguration>();
    if(loadConfig){
      loadConfiguration();
    }
  }

  /**
   * It loads sync configuration from DB
   * @throws Exception
   */
  public void loadConfiguration() throws Exception {

    AOManager mngr = AOManager.getInstance();
    String syncConfig = mngr.getConfigClobValue(SYNC_CONFIG_PROPERTY);
    if (syncConfig != null) {
      loadConfiguration(syncConfig);
    }else{
      configurationMap.clear();
    }
  }
  
  /**
   * It creates sync configuration from defined configuration which is stored in the argument inputConfiguration
   * @param inputConfiguration the sync configuration which will be loaded
   * @throws Exception
   */
  public void loadConfiguration(String inputConfiguration) throws Exception {
    if (inputConfiguration != null && !inputConfiguration.isEmpty()) {
      SyncConfigLoader loader = new SyncConfigLoader();
      configurationMap = loader.loadConfiguration(inputConfiguration);
    }else{
      configurationMap.clear();
    }
  }
  
  /**
   * Get the sync configuration
   * @return the sync configuration
   */
  public Map<Object, SyncConfiguration> getConfigurationMap() {
    return configurationMap;
  }

  /**
   * Set the sync configuration
   * @param map the sync configuration
   */
  public void setConfigurationMap(Map<Object, SyncConfiguration> map) {
    configurationMap = map;
  }
  
}
