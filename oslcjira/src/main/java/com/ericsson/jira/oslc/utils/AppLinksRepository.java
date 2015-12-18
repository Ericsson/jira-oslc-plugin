package com.ericsson.jira.oslc.utils;

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
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import com.ericsson.jira.oslc.constants.JiraConstants;

/**
 * It represents the list of the links to remote systems
 *
 */
public class AppLinksRepository {
    
    ArrayList<AppLink> appLinks = new ArrayList<AppLink>();
    
    public ArrayList<Map<String, String>> GetAllAppLinks() {
        ArrayList<Map<String, String>> AppLinks_1 = new ArrayList<Map<String, String>>();
        // get the Enumeration object
        Enumeration<AppLink> e = Collections.enumeration(appLinks);
        // enumerate through the ArrayList elements
        while (e.hasMoreElements()) {
            AppLinks_1.add(((AppLink) e.nextElement()).getAppLinkMap());
        }
        return AppLinks_1;
    }
    
    /**
     * Add an application link to the application repository
     * @param Label the label of the link
     * @param URI the uri of the link
     * @param exclusive true - the link will be added only when there is no link with same URI in the repository,
     *  false - the link will be added either when the same link exists in the repository 
     */
    public void addAppLink(String Label, String URI, Boolean exclusive) {
        if (!exclusive) {
            appLinks.add(new AppLink(Label, URI));
        } else {
            
            Boolean uriFound = false;
            for (int n_link = 0; n_link < appLinks.size(); n_link++) {
                if (URI.equals((String) appLinks.get(n_link).getAppLinkMap().get("URI"))) {
                    uriFound = true;
                    break;
                }
            }
            
            if (!uriFound)
                appLinks.add(new AppLink(Label, URI));
        }
    }
    
    /**
     * Add an application link to the application repository
     * @param appLink Application link - the link to remote resource
     * @param exclusive true - the link will be added only when there is no link with same URI in the repository,
     *  false - the link will be added either when the same link exists in the repository 
     */
    public void addAppLink(AppLink appLink, Boolean exclusive) {
        if (appLink == null) {
            return;
        }
        String url = appLink.getAppLinkMap().get(JiraConstants.OSLC_CUSTOM_FIELD_URI);
        if (url == null) {
            return;
        }
        
        if (!exclusive) {
            appLinks.add(appLink);
        } else {
            
            Boolean uriFound = false;
            for (int i = 0; i < appLinks.size(); i++) {
                String currentUrl = appLinks.get(i).getAppLinkMap().get(JiraConstants.OSLC_CUSTOM_FIELD_URI);
                if (url.equals(currentUrl)) {
                    uriFound = true;
                    break;
                }
            }
            
            if (!uriFound)
                appLinks.add(appLink);
        }
    }
    
    /**
     * Remove the link from the repository
     * @param URI the uri of the link
     * @return the link has been removed successfully, otherwise false
     */
    public Boolean removeAppLink(String URI) {
        
        for (int n_link = 0; n_link < appLinks.size(); n_link++) {
            
            if (URI.equals((String) appLinks.get(n_link).getAppLinkMap().get("URI"))) {
                // URI founded
                // Remove one appLink from array list
                appLinks.remove(n_link);
                return true;
            }
        }
        // URI not found
        return false;
    }
    
    /**
     * Returns the list of the application links from the repository
     * @return  the list of the application links from the repository
     */
    public List<AppLink> getAppLinks() {
      return this.appLinks;
    }
}
