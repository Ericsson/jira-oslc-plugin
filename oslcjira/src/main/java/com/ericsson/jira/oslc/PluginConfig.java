package com.ericsson.jira.oslc;

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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ericsson.jira.oslc.resources.ao.AOManager;

/**
 *
 * The configuration for the plugin
 * It;s possible to set the list of projects which will be visible from outside (e.g. OSLC catalog contain only projects
 * in the list). If no project is set then all projects
 * will be visible from outside. The list contains the IDs of projects, not the names because the name can be changed
 * It's also possible to set the list of issue types. The rule is the same as for the projects.
 *
 *
 */
public class PluginConfig {
  private Set<Long> filteredProjects;
  private Set<Long> filteredTypes;
  private static PluginConfig instance;
  public static final String FILTERED_PROJECTS = "filteredProjects";
  public static final String FILTERED_TYPES = "filteredTypes";

  public static PluginConfig getInstance() throws Exception {
    if (instance == null) {
      instance = new PluginConfig(true);
    }
    return instance;
  }
  
  public static PluginConfig getInstance(boolean loadConfig) throws Exception {
    if (instance == null) {
      instance = new PluginConfig(loadConfig);
    }
    return instance;
  }
  
  /**
   * It creates empty configuration and if the argument loadConfig is set on 'true' then the configuration
   * is loaded from DB
   * @param loadConfig the sync configuration will be loaded from DB, false - it creates empty configuration
   * @throws Exception
   */
  private PluginConfig(boolean loadConfig) throws Exception {
    filteredProjects = new HashSet<Long>();
    filteredTypes = new HashSet<Long>();
     if(loadConfig){
       loadConfiguration();
     }
  }

  /**
   * It loads plugin configuration from DB
   * @throws Exception
   */
  private void loadConfiguration() throws Exception {

    AOManager mngr = AOManager.getInstance();
    Map<String, String> configValues = mngr.getConfigValues();
    if (configValues != null) {
      loadFilter(filteredProjects, configValues.get(FILTERED_PROJECTS));
      loadFilter(filteredTypes, configValues.get(FILTERED_TYPES));
    }else{
      filteredProjects.clear();
      filteredTypes.clear();
    }
  }
  
  /**
   * It creates sync configuration from defined configuration which is stored in the argument inputConfiguration
   * @param inputFilteredProjects the list of ids of projects separated by comma
   * @param inputFilteredTypes the list of ids of issue types separated by comma
   * @throws Exception
   */
  public void loadConfiguration(String inputFilteredProjects, String inputFilteredTypes) throws Exception {
    if (inputFilteredProjects != null && !inputFilteredProjects.isEmpty()) {
       loadFilter(filteredProjects, inputFilteredProjects);
    }else{
      filteredProjects.clear();
    }
    
    if (inputFilteredTypes != null && !inputFilteredTypes.isEmpty()) {
      loadFilter(filteredTypes, inputFilteredTypes);
   }else{
     filteredTypes.clear();
   }
  }
  
  /**
   * Parses the value of argument input containing the list of projects/issue types separated by comma.
   * The set of project/issue types saves to argument filter
   * @param filter the set of projects/issue types extracted from input
   * @param input the list of projects/issue types separated by comma
   * @throws Exception
   */
  public void loadFilter(Set<Long>filter, String input) throws Exception {
    filter.clear();
    if (input != null && !input.isEmpty()) {
      String[] splited = input.trim().split(",");
      for (String str : splited) {
        str= str.trim();
        if(!str.isEmpty()){
          filter.add(new Long(str));
        }
      }
    }
  }

  public Set<Long> getFilteredProjects() {
    return filteredProjects;
  }

  public void setFilteredProjects(Set<Long> filteredProjects) {
    this.filteredProjects = filteredProjects;
  }

  public Set<Long> getFilteredTypes() {
    return filteredTypes;
  }

  public void setFilteredTypes(Set<Long> filteredTypes) {
    this.filteredTypes = filteredTypes;
  }
  
  
}
