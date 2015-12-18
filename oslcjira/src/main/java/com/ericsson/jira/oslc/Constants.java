/*******************************************************************************
 * Copyright (c) 2012 IBM, 2013 Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *  
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *
 *     Russell Boykin       - initial API and implementation
 *     Alberto Giammaria    - initial API and implementation
 *     Chris Peters         - initial API and implementation
 *     Gianluca Bernardini  - initial API and implementation
 *     Michael Fiedler      - Bugzilla adpater implementations
 *******************************************************************************/
package com.ericsson.jira.oslc;

import org.eclipse.lyo.oslc4j.core.model.OslcConstants;

public interface Constants
{
    public static String CHANGE_MANAGEMENT_DOMAIN                    = "http://open-services.net/ns/cm#";
    public static String CHANGE_MANAGEMENT_NAMESPACE                 = "http://open-services.net/ns/cm#";
    public static String CHANGE_MANAGEMENT_NAMESPACE_PREFIX          = "oslc_cm";
    public static String FOAF_NAMESPACE                              = "http://xmlns.com/foaf/0.1/";
    public static String FOAF_NAMESPACE_PREFIX                       = "foaf";
    public static String QUALITY_MANAGEMENT_NAMESPACE                = "http://open-services.net/ns/qm#";
    public static String QUALITY_MANAGEMENT_PREFIX                   = "oslc_qm";
    public static String REQUIREMENTS_MANAGEMENT_NAMESPACE           = "http://open-services.net/ns/rm#";
    public static String REQUIREMENTS_MANAGEMENT_PREFIX              = "oslc_rm";
    public static String SOFTWARE_CONFIGURATION_MANAGEMENT_NAMESPACE = "http://open-services.net/ns/scm#";
    public static String SOFTWARE_CONFIGURATION_MANAGEMENT_PREFIX    = "oslc_scm";
    public static String JIRA_DOMAIN                                 = "http://atlassian.com/ns/cm#"; 
    public static String JIRA_NAMESPACE                              = "http://atlassian.com/ns/cm#";
    public static String JIRA_NAMESPACE_PREFIX                       = "jira";
    
    public static String CHANGE_REQUEST              = "ChangeRequest";
    public static String TYPE_CHANGE_REQUEST         = CHANGE_MANAGEMENT_NAMESPACE + "ChangeRequest";
    public static String TYPE_RELATED_CHANGE_REQUEST = CHANGE_MANAGEMENT_NAMESPACE + "relatedChangeRequest";
    public static String TYPE_CHANGE_SET             = SOFTWARE_CONFIGURATION_MANAGEMENT_NAMESPACE + "ChangeSet";
    public static String TYPE_DISCUSSION             = OslcConstants.OSLC_CORE_NAMESPACE + "Discussion";
    public static String TYPE_PERSON                 = FOAF_NAMESPACE + "Person";
    public static String TYPE_REQUIREMENT            = REQUIREMENTS_MANAGEMENT_NAMESPACE + "Requirement";
    public static String TYPE_TEST_CASE              = QUALITY_MANAGEMENT_NAMESPACE + "TestCase";
    public static String TYPE_TEST_EXECUTION_RECORD  = QUALITY_MANAGEMENT_NAMESPACE + "TestExecutionRecord";
    public static String TYPE_TEST_PLAN              = QUALITY_MANAGEMENT_NAMESPACE + "TestPlan";
    public static String TYPE_TEST_RESULT            = QUALITY_MANAGEMENT_NAMESPACE + "TestResult";
    public static String TYPE_TEST_SCRIPT            = QUALITY_MANAGEMENT_NAMESPACE + "TestScript";
    
    public static String PATH_CHANGE_REQUEST = "changeRequest";
    
    public static String USAGE_LIST = CHANGE_MANAGEMENT_NAMESPACE + "list";
    
    public static final String HDR_OSLC_VERSION = "OSLC-Core-Version";
    public static final String OSLC_VERSION_V2 = "2.0";
    
    public static final String NEXT_PAGE = "jira.NextPage";
    
    //Jira issue - OSLC types
    public static final String JIRA_TYPE_ASIGNEE            = Constants.JIRA_NAMESPACE + "assignee";
    public static final String JIRA_TYPE_REPORTER           = Constants.JIRA_NAMESPACE + "reporter";
    public static final String JIRA_TYPE_DESCRIPTION        = OslcConstants.DCTERMS_NAMESPACE + "description";
    public static final String JIRA_TYPE_ENVIRONMNET        = Constants.JIRA_NAMESPACE + "environment";
    public static final String JIRA_TYPE_PRIORITY           = Constants.JIRA_NAMESPACE + "IssuePriority";
    public static final String JIRA_TYPE_PROJECT_ID         = Constants.JIRA_NAMESPACE + "projectId";
    public static final String JIRA_TYPE_STATUS             = Constants.JIRA_NAMESPACE + "IssueStatus";
    public static final String JIRA_TYPE_ISSUE_TYPE         = Constants.JIRA_NAMESPACE + "IssueType";
    public static final String JIRA_TYPE_COMPONENT          = Constants.JIRA_NAMESPACE + "component";
    public static final String JIRA_TYPE_AFFECTS_VERSION    = Constants.JIRA_NAMESPACE + "affectsVersion";
    public static final String JIRA_TYPE_FIX_VERSION        = Constants.JIRA_NAMESPACE + "fixVersion";
    public static final String JIRA_TYPE_ORIGINAL_ESTIMATE  = Constants.JIRA_NAMESPACE + "originalEstimate";
    public static final String JIRA_TYPE_REMAINING_ESTIMATE = Constants.JIRA_NAMESPACE + "remainingEstimate";
    public static final String JIRA_TYPE_TIME_SPENT         = Constants.JIRA_NAMESPACE + "timeSpent";
    public static final String JIRA_TYPE_RESOLUTION         = Constants.JIRA_NAMESPACE + "IssueResolution";
    public static final String JIRA_TYPE_LABEL              = Constants.JIRA_NAMESPACE + "label";
    public static final String JIRA_TYPE_VOTER              = Constants.JIRA_NAMESPACE + "voter";
    public static final String JIRA_TYPE_WATCHER            = Constants.JIRA_NAMESPACE + "watcher";
    public static final String JIRA_TYPE_CUSTOM_FIELD       = Constants.JIRA_NAMESPACE + "customField";
    public static final String JIRA_TYPE_HISTORY            = Constants.JIRA_NAMESPACE + "IssueHistory";
    
    //dcterms types
    public static final String DCTERMS_TITLE   = OslcConstants.DCTERMS_NAMESPACE + "title";
    public static final String DCTERMS_DESCRIPTION   = OslcConstants.DCTERMS_NAMESPACE + "description";
    public static final String DCTERMS_IDENTIFIER   = OslcConstants.DCTERMS_NAMESPACE + "identifier";
    public static final String DCTERMS_DUEDATE = OslcConstants.DCTERMS_NAMESPACE + "dueDate";

    //OSLC constant
    public static final String OSLC_CM_STATUS = "http://open-services.net/ns/cm#" + "status";
    
}
