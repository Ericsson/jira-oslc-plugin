User documentation
==================
This document describes how to use and set JIRA OSLC plugin.

General
-------
JIRA OSLC Provider plugin is Atlassian JIRA add-on. Main role of this plugin is extend JIRA issue
functionality to provide establishing and removing OSLC links between JIRA issue and external OSLC object.
Plugin enables handling OSLC links in N to M mode, one issue can have the links to M OSLC objects and N OSLC
objects can be linked to one JIRA issue.

Plugin doesn't duplicate OSLC links. It is not possible to create two same links from one specific JIRA
issue to one specific external OSLC system object.

Plugin is making use of two ways of authorization of external OSLC system, OAuth authorization
and simple credential authorization.

### OSLC URL strategy
Plugin provides several REST resources for communication with external system based on OSLC specification. This chapter states all relevant URLs to these resources.
URL has common "prefix" based on server, where JIRA instance is running.

#### Root services
Http GET only:

    <jira server>/rest/jirarestresource/1.0/rootservices

#### OAuth
Getting consumer key - http POST:

    <jira server>/rest/jirarestresource/1.0/oauth/requestKey

Getting request token - http POST:

    <jira server>/rest/jirarestresource/1.0/oauth/requestToken
    
User authorization - http GET:

    <jira server>/rest/jirarestresource/1.0/oauth/authorize
    
Getting access token - http POST:

    <jira server>/rest/jirarestresource/1.0/oauth/accessToken
    
#### Service providers catalog
Http GET only:

    <jira server>/rest/jirarestresource/1.0/catalog/singleton
    
Headers:
 * OSLC-Core-Version = 2.0
 * Accept = application/rdf+xml
 
#### Service provider(s)
Http GET only:

    <jira server>/rest/jirarestresource/1.0/serviceProviders/<project ID>

Headers:
 * OSLC-Core-Version = 2.0
 * Accept = application/rdf+xml
     
#### Issue types

List of types - http GET:

    <jira server>/rest/jirarestresource/1.0/issueTypes

Headers:
 * OSLC-Core-Version = 2.0
 * Accept = application/rdf+xml
      
Name of particular type - http GET:

    <jira server>/rest/jirarestresource/1.0/issueTypes/<type ID>

Headers:
 * OSLC-Core-Version = 2.0
 * Accept = application/rdf+xml
      
#### Issue priorities

List of priorities - http GET:

    <jira server>/rest/jirarestresource/1.0/issuePriorities
    
Headers:
 * OSLC-Core-Version = 2.0
 * Accept = application/rdf+xml
  
Name of particular priority - http GET:

    <jira server>/rest/jirarestresource/1.0/issuePriorities/<priority ID>

Headers:
 * OSLC-Core-Version = 2.0
 * Accept = application/rdf+xml
  
#### Issue states (workflow)

List of states for particular issue - http GET:

    <jira server>/rest/jirarestresource/1.0/issueStates/<issue key>
    
Headers:
 * OSLC-Core-Version = 2.0
 * Accept = application/rdf+xml
  
Name of particular state - http GET:

    <jira server>/rest/jirarestresource/1.0/issueStates/<issue key>/<state ID>

Headers:
 * OSLC-Core-Version = 2.0
 * Accept = application/rdf+xml
  
#### Issue resolutions

List of resolution - http GET:

    <jira server>/rest/jirarestresource/1.0/issueResolutions
    
Headers:
 * OSLC-Core-Version = 2.0
 * Accept = application/rdf+xml
      
Name of particular resolution - http GET:

    <jira server>/rest/jirarestresource/1.0/issueResolutions/<resolution ID>
    
Headers:
 * OSLC-Core-Version = 2.0
 * Accept = application/rdf+xml
  
#### Issues (change requests)
List of issue for particular project - http GET:

    <jira server>/rest/jirarestresource/1.0/<project ID>/changeRequests
    
Headers:
 * OSLC-Core-Version = 2.0
 * Accept = application/rdf+xml
  
Partucular issue - http GET:

    <jira server>/rest/jirarestresource/1.0/<project ID>/changeRequests/<issue key>
    
Headers:
 * OSLC-Core-Version = 2.0
 * Accept = application/rdf+xml
  
Create new issue - http POST:

    <jira server>/rest/jirarestresource/1.0/<project ID>/changeRequests
    
Headers:
 * OSLC-Core-Version = 2.0
 * Accept = application/rdf+xml
 * Content-Type = application/rdf+xml 
  
Update issue - http PUT:

    <jira server>/rest/jirarestresource/1.0/<project ID>/changeRequests/<issue key>

Headers:
 * OSLC-Core-Version = 2.0
 * Accept = application/rdf+xml
 * Content-Type = application/rdf+xml   

Installation & Configuration
----------------------------
### Installation of JIRA OSLC Provider plugin

For installation JIRA plugin do following steps:
 1. Download JIRA OSLC Provider plugin installation "jar" file.
 2. Log in to JIRA as administrator.
 3. Open JIRA Administration - "Add-ons" -> "Manage add-ons" section.
 4. Upload add-on.
 5. Select JIRA OSLC Provider plugin installation jar file and select "Upload".
 6. Check proper installation in "User-installed add-ons" list.

### JIRA Admin settings
There is needed to create the custom fields for proper working of JIRA OSLC Provider plugin.
 - External Links - handles the links to the external systems
 - Snapshot - handles a snapshot of the external object
 - Error log - handles information about synchronization. It's divided to two sections - for inbound and outbound connection

#### Add new "External Links" JIRA custom field named "External Links"
New "External links" JIRA custom field type is provided by JIRA OSLC Provider plugin. Correct name of new custom field
is important for association the links with this specific custom field.

 1. Log in to JIRA as administrator.
 2. Open JIRA Administration - Issues - Custom Fields.
 3. Add Custom Field.
 4. Select External Links field type and name new custom field as "External Links" (mandatory!).
 5. Associate new custom field with "Default Screen". There is no need to associate new custom field with other issue screens.

#### Add JIRA custom field for handling the snapshot of external object 
The field contains the snapshot of external object. The list of fields which are contained in the snapshot is defined
in the LeanSync configuration.

 1. Log in to JIRA as administrator.
 2. Open JIRA Administration - Issues - Custom Fields.
 3. Add Custom Field.
 4. Select multi-line field type and name new custom field as "Snapshot". The name is of your choice and it has to be defined
 in the LeanSync configuration
 5. Associate new custom field with "Default Screen". There is no need to associate new custom field with other issue screens.

#### Add JIRA custom field for handling the error log
It contains the information about synchronization with external system. It's divided to two sections - for inbound
and outbound connection. The information are displayed only when LeanSync configuration is set up correctly.

 1. Log in to JIRA as administrator.
 2. Open JIRA Administration - Issues - Custom Fields.
 3. Add Custom Field.
 4. Select multi-line field type and name new custom field as "Error log". The name is of your choice and it has to be defined
 in the LeanSync configuration
 5. Associate new custom field with "Default Screen". There is no need to associate new custom field with other issue screens.

### OAuth admin settings
JIRA OSLC Provider authorization functionality is based on OAuth v 1.0 open protocol.

Following OAuth authorization steps need to be done for establishing proper connection between JIRA OSLC Provider plugin
and external OSLC system. For all steps the admin user rights is needed.
 
All settings described in this chapter are placed in JIRA - Administration - "Add-ons" - OSLC section.

Note: "OSLC" section is a part of plugin and will be accessible after plugin installation.

#### OAuth consumers administration
Admin JIRA user can register OAuth consumer in "Register consumer" section.

In this section user set the external OSLC system consumer name, consumer key and consumer secret. By this operation
JIRA OSLC Provider plugin registers this specific consumer key directly to "Authorized keys" list.

Another way how to register the consumer key is requesting provisional OAuth key by external OSLC system from JIRA.
External system requests registration, provisional key is produced by JIRA and sent to external server. After accepting
of provisional key by JIRA admin user the authorized key is produced and registered directly to "Authorized keys" list.

#### Root services management
If the external system is providing Root service interface, JIRA OSLC Provider plugin can fetch information about services provided from external system:
 * URL to get consumer key
 * URL for service provider catalog
 * URLs for OAuth.
  
There is needed to fetch service providers from external server. For this reason JIRA OSLC Provider plugin is providing
the "Server friends" list.

#### Service provider catalogs management
Some OSLC servers don't provide root service interface. In that case information about service providers can be fetched
directly by service provider catalog interface. JIRA OSLC Provider plugin offers possibility of set such service provider
catalog and afterwards save obtained service provider catalog list.

 1. Log in to JIRA as administrator
 2. Open JIRA Administration - "Add-ons" - Service provider catalogs
 3. Fill in all mandatory fields.
    * Title - Your name of saved catalog
    * Service provider catalog URI - URI to external system service provider catalog
    * OAuth consumer key - OAuth consumer key provided by external system
    * OAuth consumer secret - OAuth consumer secret provided by external system
    * OAuth domain - OAuth domain of external system
    * OAuth request token URI - OAuth request token URI of external system
    * OAuth user authorization URI - OAuth user authorization URI of external system
    * OAuth access token URI - OAuth access token URI of external system
 4. Click on Request Access button. JIRA OSLC Provider plugin will save external system catalog to the "Service provider
    catalog" list.

#### Project relationships
External OSLC systems can define and provide own service providers.

JIRA OSLC Provider plugin enables selection and save service providers to service providers list in project management
section. List can contain various service providers from various OSLC external systems.

Selecting and saving service providers:
 1. Log into JIRA as administrator.
 2. Open JIRA Administration - "Add-ons" - Project relationships.
 3. Select one of already registered external OSLC servers.
 4. In Service providers select some providers with which JIRA OSLC Provider plugin will be working.
 5. Click to "Add" button. JIRA OSLC Provider plugin will add providers to its list.

Selecting and saving service providers in other OSLC system then JIRA:
As OSLC external systems are providing service providers also JIRA OSLC Provider plugin is providing own service providers.

Note: In plugin terminology, Service provider means JIRA project i.e. each project existing in JIRA instance represents
one service provider provided by plugin.

### LeanSync configuration
The configuration is handled in the xml file which is stored in Administration -> Add-ons -> LeanSync configuration.
The configuration is defined for projects and issue types. It's possible to have one or more configurations for different
projects. It's described in more detail in the following chapters.

The basic structure of configuration looks like that:

    <?xml version="1.0" encoding="UTF-8"?>
    <configurations>
      <configuration>
        <projects />
        <issueTypes />
        <domains />
        <mappings>
          <mappingIn>
            <xmlfields>
              <field />
              <field />
              .....
            </xmlfields>
            <fields>
              <field />
              <field />
            </fields>
            .....
            <templateFields>
              <templateField>
                <template />
              </templateField>
            </templateFields>
            .....
          </mappingIn>
          <mappingOut>
            <connection />
            <fields>
              <field />
              <field />
            </fields>
            .....
            <templateFields>
              <templateField>
                <template />
              </templateField>
            </templateFields>
            .....  
          </mappingOut>
        </mappings>
      </configuration>
    </configurations>

#### Projects
The 'projects' node contains the list of projects. Only the changes of the issues in these projects will be propagated
to the external system and the other way around (External system -> JIRA). The list contains the IDs of the projects and
not the name of the projects because only ID is unique and is never changed. The IDs of projects are not easily visible
in the JIRA administration.
 
The steps how to get the ID of project:

 1. Go to Administrations -> Projects -> View All Projects
 2. Move the mouse cursor on the Edit link and the ID is displayed in the status bar of the browser. It's stored in the
   attribute 'pid' of the URL.

Here is the sample how to define the projects. It contains the list of projects 10000 and 10001.

    <projects>
      <project>10000</project>
      <project>10001</project>
    </projects>

#### Issue types
The 'issueTypes' node contains the list of issue types (Bug, Task , ...). Only the changes of the issue of these types
will be propagated to the external system and the other way around (External system -> JIRA). The list contains the IDs
of the issue types and not the name of the types because only ID of the issue type is unique and is never changed. The IDs
of issue types are not easily visible in the JIRA administration. 

The steps how to get the ID of issue type:

 1. Go to Administrations -> Issues -> Issue Types
 2. Move the mouse cursor on the Edit link and the ID is displayed in the status bar of the browser. It's stored in the
    attribute 'id'.

Here is the sample of issueTypes node. It contains the list of Issue Types 1 and 2.

    <issueTypes>
      <issueType>1</issueType>
      <issueType>2</issueType>
    </issueTypes>

#### Domains
It specifies for which domains the configuration is valid. The changes of JIRA issue will be propagated only to the
external objects (specified by the links in External Links field) which start with specified domain. 

    <domains>
      <domain>https://some.domain.com</domain>
    </domains>

#### Mapping
The 'mappings' node contains the 'mappingIn' node for inbound and the 'mappingOut' node for outbound mappings. The inbound
mapping is direction External system -> JIRA and the outbound mapping is JIRA -> External system.

Example:

    <mappings>
      <mappingIn>
        ....
      </mappingIn>
      <mappingOut>
        ....
      </mappingOut>
    </mappings>

Inbound mapping:

Inbound mapping serves for saving the values of the external object to the JIRA issue fields. The data representation
of external object is in xml.

Inbound mapping M:1:

It's possible to map one or more extenal object's fields to one JIRA field. It's used for saving Snapshot to the JIRA field to
display the summary information about Extenal object.

Inbound mapping 1:1:

For mapping one extenal object's field to one Jira field there is used attribute 'mapTo' which points to JIRA field.
The list of supported Jira field which can be mapped are specified in the chapter Mapping.

Sample:

    <field ns="http://purl.org/dc/terms/" name="identifier" mapTo="Extenal object ID" fieldType="custom" />

The value of the 'identifier' node will be put to the JIRA custom field called 'Extenal object ID'. The value 'custom'
means that the value will be saved to the custom field and not to general JIRA field. It's from reason that the name
of custom field can have the same name as the general field and we have to differentiate them. Also it's possible to map
the value to the general JIRA field like Summary or Description. In this case we skip the 'fieldType' attribute.

    <field ns="http://purl.org/dc/terms/" name="identifier" mapTo="Summary">
    
It's also possible to map the value at once to the template and to the JIRA field. It's enough to add 'id' attribute.

    <field ns="http://purl.org/dc/terms/" name="identifier" id="extObjectId" mapTo="Summary">

Inbound mapping 1:M:

For mapping one extenal object's field to many JIRA fields there is enough to define the 'field' node several times.

Sample:

    <field ns="http://purl.org/dc/terms/" name="identifier" mapTo="Summary">
    <field ns="http://purl.org/dc/terms/" name="identifier" mapTo="Description">

Inbound mapping - attributes:

Field and temlateField node - common attributes
 * mapTo - the name of JIRA field where the value of extenal object's field will be mapped. If it's custom field
   the fieldType with value 'custom' has to be specified.
   
        <field ns="http://purl.org/dc/terms/" name="identifier" mapTo="Summary">
   
 * fieldType - it determines if the mapped field is custom (value is "custom") or not (default value).

        <templateField mapTo="Snapshot" fieldType="custom" contentType="html" notifyChanged="false" alwaysSave="true"> 

 * notifyChange - it determines if the change of the value will have the impact for propagating the change to the external
   system
      
   * true - if the value is changed then the changes are propagated to external system. It's default value.
   * false - if the value is changed then the change will not have the impact on decision if the changes of JIRA issue
     will be propagated to the external system. For example, when only the fields with set this attribute as 'false' are
     changed the changes are not propagated to the external system. It's recommended to set this attribute to 'false' for
     fields which handles the snapshots of external system.
  
          <templateField mapTo="Snapshot" fieldType="custom" idPrefix="%" idSuffix="%" contentType="html" notifyChanged="false" alwaysSave="true"> 

 * encodeHtml - if it's set on 'true' it escapes html content of  the value during mapping process. It serves for correct
   displaying html tags in the field e.g. '<' is replaced for '& lt;' during mapping process but it's displayed correctly as '<' in the web page.
   The default value is 'false'.
   
       <field ns="http://purl.org/dc/terms/" name="identifier" mapTo="Summary" encodeHtml="true">

 * action - It determines for which action the field mapping is valid. By default the mapping is valid for
   both actions - create and update.
   * create - the mapping is valid only when the JIRA issue is creating
   * update - the mapping is valid only when the JIRA issue is updating
   
         <field ns="http://purl.org/dc/terms/" name="identifier" mapTo="Summary" action="update">

Field node
 * ns (xmlField) - the name of namespace where the field belongs.

       <field ns="http://purl.org/dc/terms/" name="identifier" mapTo="Summary">
       
 * name (field) - the name of the node in the xml which contains External object data. The value of the node will be put to the template
   or map to the field.
    
       <field ns="http://purl.org/dc/terms/" name="identifier" mapTo="Summary">
       
 * id - the id of the field. The id of the field and id (with prefix and suffix, described bottom) in the template
   have to be matched. The id serves for referencing the field in the template.
   
       <field ns="http://purl.org/dc/terms/" name="identifier" id="summary">
       
 * xpath (xmlField) - it's selector for selecting the node in the xml. This attribute is valid only when the parent
   of the 'field' node is 'xmlField' node.
   
       <field xpath="/extObject/comment" id="comment" />
       
 * keepTags -  The html tags are removed from mapped value by default. If it's set on 'true' the html tags are not removed.
   This attribute is valid only when the parent of the 'field' node is 'xmlField' node.
   
       <field xpath="/extObject/comment" id="comment" keepTags="true" />
       
 * fromDateFormat - it specifies datetime patpern in external object for conversion date/datetime field. If this attribute
   is specified then the toDateFormat has to be specified too.
   
 * toDateFormat - it specifies datetime patpern for conversion date/datetime field in JIRA. If this attribute is specified
   then the fromDateFormat has to be specified too.
   
       <field xpath="/extObject/custtargdatetime " mapTo="ExtObject datetime" fieldType="custom" fromDateFormat="dd/MMM/yy h:mm toDateFormat="dd/MMM/yy"/>

TemplateField node
 * contentType - It offers only one value 'html'. If this attribute is set it says that the ends of lines will be replaced
   by tags < br/> during mapping process from extenal object's field to JIRA field.
   
       <templateField mapTo="Snapshot" fieldType="custom" idPrefix="%" idSuffix="%" contentType="html" notifyChanged="false">
       
 * idPrefix and idSuffix - the attributes specify the prefix and suffix which will be add to the ID of the declared
   field (in the 'id' attribute of 'field' node) and the value of the field replaces the string (id with prefix and suffix)
   in the template.
   
       <templateField mapTo="Snapshot" fieldType="custom" idPrefix="%" idSuffix="%" contentType="html" notifyChanged="false">
       
 * alwaysSave - if the attribute is set on 'true' then the value will be saved even when updating of JIRA issue failed.
   Default value is 'false' and it's recommended to set it on 'true' for the field which handles the snapshot. It's
   possible to set the attribute only for JIRA custom field.
   
       <templateField mapTo="Snapshot" fieldType="custom" idPrefix="%" idSuffix="%" contentType="html" notifyChanged="false" alwaysSave="true">

Inbound mapping - value mapping

The values of external system can be mapped to another values and saved to Jira issue field.

For example:

The priority of external object contains values A, B and C. The priority of JIRA issue contains values 1, 2, 3. It's
possible to map A -> 1, B->2, C->3.  

The mapping will define like:

    <field ns="http://some.domain.com/rdf#" name="priority" mapTo="Priority" />    
      <default value="1"/>
      <maps>
        <map key="A" value="1"/>
        <map key="B" value="2"/>
        <map key="C" value="3"/>
      </maps>
    </field>
    
You can also define the default value which is mapped when the value doesn't match defined mapping. The value mapping
is possible to define for templateField too.

Outbound mapping

Outbound mapping serves for saving the values of the JIRA object to the fields which will be saved to the external object
in the external system.

Here is the sample how to configure outbound mapping:

    <mappingOut>
      <connection username="johndoe" password="*****">
        <headers>
          <header name="header_name" value="header_value"/>
        </headers>
        <rdfTypes>
          <rdfType value="http://some.domain.com/rdf#ChangeRequest"/>
        </rdfTypes>
      </connection>
      <fields>
        <field name="Summary" id="summary" />
        <field name="Answer" fieldType="custom" id="answer"/>
      </fields>
      <templateFields>
        <templateField>
          <template ns="http://some.domain.com/rdf/cf#" name="JIRA issue" idPrefix="%" idSuffix="%" contentType="html">
          Summary:  %summary%
          Answer: %answer%
          </template>
        </templateField>
      </templateFields>
    </mappingOut>

The 'connection' node specifies the configuration which is need for a connection with external system.
 * username - the name of user for basic authentication. The external object will be updated in behalf of this user.
 * password - the password of user for basic authentication.
 
The 'header' node contains the list of headers which will be sent in update request. The header values will be provided
by support team of external system.

The 'rdfTypes' node specifies the type of object which will be sent to external object, e.g. EnterpriseChangeRequest. 
The type values will be provided by support team of external system.

To mapping JIRA field values to the snapshot it's need to add the 'templateFiled' node which has the following attributes:
 * ns - specify the namespaces of the node in outbound xml  where the snapshot will be stored. The value is 
   always 'http://some.domain.com/rdf/cf'.
 * name - specify the name of the node in outbound xml  where the snapshot will be stored.
 * idPrefix - the prefix string which will be added to the ID of the field.
 * idSuffix - the suffix string which will be added to the ID of the field.
 * contentType - specifies the type of the field in the external object where the snapshot will be put. When you specify
   the 'html' value, the special handling will be used e.g. the end of line will be replaced with '< br/>' tag.
   The default value is empty and in this case the snapshot is put to the field as it is.

Example:

    <fields>
      <field name="Summary" id="summary" />
      <field name="Answer" id="answer" fieldType="custom"/>
    </fields>
    
Note: For mapping custom field we specify the 'fieldType' attribute with value 'custom'. The general JIRA fields are
mapped according to the chapter Mapping which contains the list of JIRA field which can be mapped.

Let's JIRA issue has these values:
 * General JIRA field - Summary = The error in software H005
 * Custom field - Answer = It's not fault.
 
When we use mapping from sample above, the following snapshot will be put to the 'JIRA issue' field in the external object.
    
    Summary: The error in software H005
    Answer: It's not fault.

Template

As it's mentioned in the previous chapter, the template serves for specifying how the snapshot will look. We have several
types of template:
 1.	The template can be as plain text

        <template>
          Summary = %summary%
          Answer = %answer%
        </template>
        
 2. The template can contain wiki marks

        <template>
          *Summary* = %summary%
          *Answer* = %answer%
        </template>
    More information about wiki marks is here: https://jira.atlassian.com/secure/WikiRendererHelpAction.jspa?section=all

 3. The template can contain html tags

        <template>
          {html}
          <b>Summary</b> = %summary% </br>
          <b>Answer</b> = %answer%
          {html}
        </template>
    We are able to save the snapshot of external object to JIRA issue with wiki marks or with html tags. We have to turn
    wiki rendering on for the JIRA field where the snapshot will be saved. How to enable wiki rendering is described in
    the chapter Wiki rendering.
       
#### Wiki rendering
When we want to use wiki marks in the JIRA fields we have to turn wiki rendering for the specific field. More information
about wiki rendering is here https://confluence.atlassian.com/display/JIRA/Configuring+Renderers.

Steps how to setup wiki rendering:
 1. Go to Administration->Issues->Field configuration.
 2. Click on Configure for Default Field Configuration.
 3. Click on Renderers for the field which will handle the snapshot of external object. It's 'Snapshot' in our sample.
 4. Select Wiki style renderer and press Update.
    If you want to enable html tags for the JIRA fields you have to enable it in Add-ons configuration.
 5. Go to Administations - > Add-ons -> Manage add-ons.
 6. Select System instead of User installed in drop-down menu.
 7. Click on Wiki Renderer Macros Plugin.
 8. Click on the plus in the right side and enable html module.
       
### Project and Issue type visibility
When external system want to get information about the projects in JIRA, the all information about projects including
all issue types are sent to the external system. In JIRA production there are usually about hundreds projects
and issue types. The sending information has about 5MB and it can take a long time. For this case we are able to specify
the list of projects and issue types which will be visible to external system.

Steps how to setup project and issue visibility:
 1. Administration -> Add-ons -> Plugin configuration.
 2. Fill in:
    * Project IDs - specifies the project IDs separated by comma which you want to have visible for external systems. How to
      find out the IDs of projects is described in the chapter Project.
    * Issue types - specify the issue types separated by comma which you want to have visible for external system. How to
      find out the ID of issue types is described in the chapter Issue types.
    
Note: If you don't specify the IDs of projects then all projects will be visible for external system. The same is for issue
types.


Mapping
-------
### JIRA fields
Here is the list of JIRA fields which can be mapped.

| Field name           | Inbound (ExtSys -> JIRA) | Outbound (JIRA -> ExtSys) |
|----------------------|--------------------------|---------------------------|
| AffectedVersionNames |            X*            |             X             |
| AssigneeId           |            X             |             X             |
| Comments             |                          |             X             |
| ComponentNames       |            X*            |             X             |
| CreatedDate          |                          |             X             |
| CreatorId            |                          |             X             |
| Custom field***      |            X             |             X             |
| Description          |            X             |             X             |
| DueDate              |            X             |             X             |
| Environment          |            X             |             X             |
| Estimate             |            X**           |             X             |
| FixVersionNames      |            X*            |             X             |
| IssueKey             |                          |             X             |
| IssueTypeId          |            X             |                           |
| IssueTypeName        |                          |             X             |
| LabelNames           |                          |             X             |
| OriginalEstimate     |            X**           |             X             |
| OutsideLinks         |                          |             X             |
| OutwardLinks         |                          |             X             |
| PriorityId           |            X             |                           |
| PriorityName         |                          |             X             |
| ProjectKey           |                          |             X             |
| ProjectName          |                          |             X             |
| ReporterId           |            X             |             X             |
| ResolutionDate       |                          |             X             |
| ResolutionId         |            X             |                           |
| ResolutionName       |                          |             X             |
| StatusName           |                          |             X             |
| SubtaskNames         |                          |             X             |
| Summary              |            X             |             X             |
| TimeSpent            |                          |             X             |
| UpdatedDate          |                          |             X             |
| VoterNames           |                          |             X             |
| WatcherNames         |                          |             X             |
| WorkLog              |                          |             X             |

'*' It's possible to set only one value.

'**' You can set only Estimate or Original Estimate, but not both value in one update.

'***' You have to specify fieldType attribute with the values 'custom' to recognize that it's not general JIRA field but 
JIRA custom field. Only single line and multiline custom fields are supported.





     
      






