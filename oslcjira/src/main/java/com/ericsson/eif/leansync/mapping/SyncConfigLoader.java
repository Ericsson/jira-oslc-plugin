package com.ericsson.eif.leansync.mapping;

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

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.xpath.jaxp.XPathFactoryImpl;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import com.ericsson.eif.leansync.mapping.data.ActionType;
import com.ericsson.eif.leansync.mapping.data.SyncConfiguration;
import com.ericsson.eif.leansync.mapping.data.SyncField;
import com.ericsson.eif.leansync.mapping.data.SyncGeneralField;
import com.ericsson.eif.leansync.mapping.data.SyncMapping;
import com.ericsson.eif.leansync.mapping.data.SyncTemplate;
import com.ericsson.eif.leansync.mapping.data.SyncXmlFieldConfig;
import com.ericsson.eif.leansync.mapping.exceptions.SyncConfigurationException;

/**
 * A class which servers for loading LeanSync configuration from xml
 *
 */
public class SyncConfigLoader {
  private final String CONFIG_NODE_CONNECTION = "connection";
  private final String CONFIG_ATTR_NS = "ns";
  private final String CONFIG_ATTR_NAME= "name";
  private final String CONFIG_ATTR_ID = "id";
  private final String CONFIG_ATTR_XPATH = "xpath";
  private final String CONFIG_ATTR_FIELD_TYPE = "fieldType";
  private final String CONFIG_ATTR_CONTENT_TYPE = "contentType";
  private final String CONFIG_ATTR_MAP_TO = "mapTo";
  private final String CONFIG_ATTR_NOTIFY_CHANGE = "notifyChange";
  private final String CONFIG_ATTR_ACTION = "action";
  private final String CONFIG_ATTR_KEEP_TAGS = "keepTags";
  private final String CONFIG_ATTR_ENCODE_HTML = "encodeHtml";
  private final String CONFIG_ATTR_TO_DATE_FORMAT = "toDateFormat";
  private final String CONFIG_ATTR_FROM_DATE_FORMAT = "fromDateFormat";
  private final String CONFIG_ATTR_USERNAME = "username";
  private final String CONFIG_ATTR_PASSWORD = "password";
  private final String CONFIG_NODE_CONFIG_FULL = "//configurations/configuration";
  private final String CONFIG_NODE_PROJECT_REL = "projects/project";
  private final String CONFIG_NODE_HEADER_REL = "headers/header";
  private final String CONFIG_NODE_DOMAIN_REL = "domains/domain";
  private final String CONFIG_NODE_RDFTYPE_REL = "rdfTypes/rdfType";
  private final String CONFIG_ATTR_HEADER_VALUE = "value";
  private final String CONFIG_NODE_TYPE_REL = "issueTypes/issueType";
  private final String CONFIG_NODE_MAPPING_IN_REL = "mappings/mappingIn";
  private final String CONFIG_NODE_MAPPING_OUT_REL = "mappings/mappingOut";
  private final String CONFIG_NODE_XMLFIELDS = "xmlFields";
  private final String CONFIG_NODE_FIELDS = "fields";
  private final String CONFIG_NODE_FIELD = "field";
  private final String CONFIG_NODE_TEMPLATE = "template";
  private final String CONFIG_NODE_TEMPLATE_FIELD_REL = "templateFields/templateField";
  private final String CONFIG_ATTR_ID_PREFIX = "idPrefix";
  private final String CONFIG_ATTR_ID_SUFFIX = "idSuffix";
  private final String CONFIG_ATTR_ALWAYS_SAVE = "alwaysSave";
  private final String CONFIG_NODE_ERRORLOG = "errorLog";
  private final String CONFIG_NODE_MAP_REL = "maps/map";
  private final String CONFIG_ATTR_KEY = "key";
  private final String CONFIG_ATTR_VALUE = "value";
  private final String CONFIG_ATTR_DEFAULT= "default";
 
  /**
   * Load LeanSync configuration from xml
   * @param inputConfiguration the xml containing LeanSync configuration
   * @return LeanSync configuration for each project
   * @throws Exception
   */
  public Map<Object, SyncConfiguration> loadConfiguration(String inputConfiguration) throws Exception {
    Map<Object, SyncConfiguration> confMap = new HashMap<Object, SyncConfiguration>();
    // initialize dom and xpath factory
    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
    domFactory.setNamespaceAware(true);
    DocumentBuilder builder = domFactory.newDocumentBuilder();
    ByteArrayInputStream is = new ByteArrayInputStream(inputConfiguration.getBytes());
    Document doc = builder.parse(is);

    XPathFactory xpathFactory = new XPathFactoryImpl();
    XPath xpath = xpathFactory.newXPath();

    // locate all property nodes:
    NodeList configurationList = (NodeList) xpath.evaluate(CONFIG_NODE_CONFIG_FULL, doc, XPathConstants.NODESET);

    if (configurationList != null) {
      for (int i = 0; i < configurationList.getLength(); i++) {
        Node configuration = configurationList.item(i);
        SyncConfiguration fieldConfiguration = processConfiguration(configuration, xpath);
        for (String project : fieldConfiguration.getProjects()) {
          confMap.put(project, fieldConfiguration);
        }
      }
    }
    return confMap;
  }

  /**
   * Load the configuration section from LeanSync configuration
   * @param configurationNode the configuration section in xml
   * @param xpath the path to the configuration section in xml
   * @return the configuration for LeanSync
   * @throws Exception
   */
  private SyncConfiguration processConfiguration(Node configurationNode, XPath xpath ) throws Exception {
    SyncConfiguration configuration = new SyncConfiguration();
    
    processErrorLog(configurationNode, configuration, xpath);
    processProjects(configurationNode, configuration, xpath);
    processIssueTypes(configurationNode, configuration, xpath);
    processDomains(configurationNode, configuration, xpath);
    processMappings(configurationNode, configuration, xpath);
   
    return configuration;
  }
  
  /**
   * It validates if the IDs in the templates are defined in the field configuration
   * @param mapping the mapping configuration
   * @param name information text for validation. It specifies the type of mapping
   * @return the validation message, it's empty if the configuration is valid
   */
  private static String validateMapping(SyncMapping mapping, String name){
    List<SyncTemplate> templates = mapping.getTemplates();
    StringBuilder validation = new StringBuilder();
    if (templates != null) {
      for (SyncTemplate template : templates) {
        String validateTemplate = validateTemplate(mapping, template, name);
        if (validateTemplate != null) {
          validation.append(validateTemplate);
        }
      }
    }
    if(validation.length() == 0){
      return null;
    }else{
      return validation.toString();
    }
 }
  
  /**
   * It validates if the IDs in the template are defined in the field configuration
   * @param mapping the mapping configuration
   * @param temlate the template containing IDs which are checked if there are defined in the field configuration
   * @param name information text for validation. It specifies the type of mapping
   * @return the validation message, it's empty if the configuration is valid
   */
  private static String validateTemplate(SyncMapping mapping, SyncTemplate template, String name){
     if(template.getTemplate() == null || mapping.getFields() == null){
       return null;
     }
     StringBuilder result = new StringBuilder();
    
     List<String> templateItem = new ArrayList<String>();
     Pattern p = Pattern.compile(template.getIdPrefix() + ".+?" + template.getIdSuffix());
     Matcher m = p.matcher(template.getTemplate());
     
     Set<String> declaredFelds = new HashSet<String>();
     List<SyncField> fields = mapping.getFields();
     for (SyncField leanSyncField : fields) {
       declaredFelds.add(template.getIdPrefix() + leanSyncField.getId() + template.getIdSuffix());
     }
     
     List<SyncXmlFieldConfig> xmlFieldConfigsList = mapping.getXmlFieldConfigs();
     for (SyncXmlFieldConfig xmlFieldConfig : xmlFieldConfigsList) {
       List<SyncField> fieldList = xmlFieldConfig.getFields();
       for (SyncField leanSyncField : fieldList) {
         declaredFelds.add(template.getIdPrefix() + leanSyncField.getId() + template.getIdSuffix());
       }
     }
    
     while(m.find()) {
       templateItem.add(m.group());
     } 
     for (String item : templateItem) {
       if(!declaredFelds.contains(item)){
         if(result.length() != 0){
           result.append(", ");
         }
         result.append(item);
       }
     } 
    
     if(result.length() > 0){
       return name + " mapping - following items do not contain field declaration: " + result.toString() +". ";
     }
    
    return null;
  }
  
  /**
   * Load the error log section
   * @param configurationNode the configuration section
   * @param configuration the configuration data
   * @param xpath the path to the error log section
   * @throws XPathExpressionException
   */
  private void processErrorLog(Node configurationNode, SyncConfiguration configuration, XPath xpath) throws XPathExpressionException {
    Node errorLogNode = (Node) xpath.evaluate(CONFIG_NODE_ERRORLOG, configurationNode, XPathConstants.NODE);

    if (errorLogNode == null) {
      return;
    }
    
    NamedNodeMap attributes = errorLogNode.getAttributes();
    Node nameNode = attributes.getNamedItem(CONFIG_ATTR_NAME);
    
    if (nameNode != null) {
      configuration.setErrorLog(nameNode.getNodeValue());
    }
    
  }

  /**
   * Load the projects section with the list of the projects which the configuration is valid for
   * @param configurationNode the configuration section
   * @param configuration the configuration data
   * @param xpath the path to projects section
   * @throws XPathExpressionException
   */
  private void processProjects(Node configurationNode, SyncConfiguration configuration, XPath xpath) throws XPathExpressionException {
    Set<String> values = processStringNodeListAsSet(configurationNode, xpath, CONFIG_NODE_PROJECT_REL);
    if(values != null){
      configuration.setProjects(values);
    }
  }
  
  /**
   * Load the domains section with the list of the domains which the configuration is valid for
   * @param configurationNode the configuration section
   * @param configuration the configuration data 
   * @param xpath the xpath instance
   * @throws XPathExpressionException
   */
  private void processDomains(Node configurationNode, SyncConfiguration configuration, XPath xpath) throws XPathExpressionException {
    Set<String> values = processStringNodeListAsSet(configurationNode, xpath, CONFIG_NODE_DOMAIN_REL);
    if(values != null){
      configuration.setDomains(values);
    }
  }
  
  /**
   * Put the values of the nodes to the set 
   * @param parentNode the parent node of the nodes with the values
   * @param xpath the xpath instance
   * @param path the path to the nodes with the values
   * @return the loaded set of the values
   * @throws XPathExpressionException
   */
  private Set<String> processStringNodeListAsSet(Node parentNode, XPath xpath, String path) throws XPathExpressionException {
    NodeList nodeList = (NodeList) xpath.evaluate(path, parentNode, XPathConstants.NODESET);
    if(nodeList == null){
      return null;
    }
    
    Set<String> values = new HashSet<String>();
    
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node != null) {
        String value = node.getTextContent();
        if (value != null && !value.isEmpty()) {
          values.add(value);
        }
      }
    }
    return values;
    
  }
  
  /**
   * Load the issue types section with the list of the issue types which the configuration is valid for
   * @param configurationNode the configuration section
   * @param configuration the configuration data
   * @param xpath the xpath instance
   * @throws XPathExpressionException
   */
  private void processIssueTypes(Node configurationNode, SyncConfiguration configuration, XPath xpath) throws XPathExpressionException {
    Set<String> values = processStringNodeListAsSet(configurationNode, xpath, CONFIG_NODE_TYPE_REL);
    if(values != null){
      configuration.setIssueTypes(values);
    }
  }
  
  /**
   * Load the mappings section with the list of the mapping configurations.
   * The mapping configurations are added to the configuration data (configuration)
   * @param configurationNode the configuration section
   * @param configuration the configuration data
   * @param xpath the xpath instance
   * @throws XPathExpressionException
   */
  private void processMappings(Node configurationNode, SyncConfiguration configuration, XPath xpath) throws Exception {
    
    String validationResult = null;
    String validationInResult = processMappingList(CONFIG_NODE_MAPPING_IN_REL, configurationNode, configuration, xpath);
    if (validationInResult != null) {
      validationResult = (validationResult == null) ? new String(validationInResult) : validationResult + validationInResult;
    }
    
    String validationOutResult = processMappingList(CONFIG_NODE_MAPPING_OUT_REL, configurationNode, configuration, xpath);
    if (validationOutResult != null) {
      validationResult = (validationResult == null) ? new String(validationOutResult) : validationResult + validationOutResult;
    }
    
    if (validationResult != null) {
      throw new SyncConfigurationException(validationResult);
    }
  }
  
  private String processMappingList(String mappingExpression, Node configurationNode, SyncConfiguration configuration, XPath xpath) throws Exception {
    NodeList mappingList = (NodeList) xpath.evaluate(mappingExpression, configurationNode, XPathConstants.NODESET);
    
    String validationResult = null;
    
    if (mappingList != null) {
      for (int i = 0; i < mappingList.getLength(); i++) {
        Node mappingNode = mappingList.item(i);
        SyncMapping mapping = processMapping(mappingNode, configuration, xpath);
        String result = null;
        if (CONFIG_NODE_MAPPING_IN_REL.equals(mappingExpression)) {
          configuration.addInMapping(mapping);
          result = validateMapping(mapping, "Inbound");
        } else if (CONFIG_NODE_MAPPING_OUT_REL.equals(mappingExpression)) {
          configuration.addOutMapping(mapping);
          result = validateMapping(mapping, "Outbound");
        }
        if (result != null) {
          validationResult = (validationResult == null) ? new String(result) : validationResult + result;
        }
      }
    }
    
    return validationResult;
  }

  /**
   * Load the mapping section.
   * @param mappingNode the mapping section
   * @param config the configuration data
   * @param xpath the xpath instance
   * @return the mapping data
   * @throws XPathExpressionException
   * @throws SyncConfigurationException
   */
  private SyncMapping processMapping(Node mappingNode, SyncConfiguration config, XPath xpath) throws XPathExpressionException, SyncConfigurationException {
    SyncMapping leanSyncMapping = new SyncMapping();
    
    processConnection(mappingNode, leanSyncMapping, xpath);
    processFieldConfig(mappingNode, leanSyncMapping, xpath);

    return leanSyncMapping;
  }
  
  
  /**
   * Load the templates section. The templates are added to the mapping data (mapping)
   * @param mappingNode the mapping section
   * @param mapping the mapping data
   * @param xpath the xpath instance
   * @throws XPathExpressionException
   * @throws SyncConfigurationException
   */
  private void processTemplates(Node mappingNode, SyncMapping mapping, XPath xpath) throws XPathExpressionException, SyncConfigurationException {
    NodeList templateList = (NodeList) xpath.evaluate(CONFIG_NODE_TEMPLATE_FIELD_REL, mappingNode, XPathConstants.NODESET);
    if(templateList == null){
      return;
    }

    for (int i = 0; i < templateList.getLength(); i++) {
      Node templateNode = templateList.item(i);
      SyncTemplate template = processTemplate(templateNode, xpath);
      if (template != null) {
        mapping.addTemplate(template);
      }
    }
  }
  
  /**
   * Load the template section.
   * @param templateFieldNode the templates section
   * @param xpath the xpath instance
   * @return the template data 
   * @throws XPathExpressionException
   * @throws SyncConfigurationException
   */
  private SyncTemplate processTemplate(Node templateFieldNode, XPath xpath) throws XPathExpressionException, SyncConfigurationException {
    SyncTemplate template = new SyncTemplate();
    processGeneralField(template, templateFieldNode, xpath);
        
    NamedNodeMap attrs = templateFieldNode.getAttributes();
    Node prefixNode = attrs.getNamedItem(CONFIG_ATTR_ID_PREFIX);
    Node suffixNode = attrs.getNamedItem(CONFIG_ATTR_ID_SUFFIX);
    Node alwaysSave = attrs.getNamedItem(CONFIG_ATTR_ALWAYS_SAVE);
    Node templateNode = (Node) xpath.evaluate(CONFIG_NODE_TEMPLATE, templateFieldNode, XPathConstants.NODE);
    
    if (prefixNode != null) { 
      String prefix = prefixNode.getNodeValue();
      template.setIdPrefix(prefix);
    }
    
    if (suffixNode != null) {
      String suffix = suffixNode.getNodeValue();
      template.setIdSuffix(suffix);
    }
    
    if (alwaysSave != null) {
      template.setAlwaysSave(alwaysSave.getNodeValue());
    }
    
    if(templateNode != null){
      String templateText = loadXMLNodeValue(templateNode);
      template.setTemplate(templateText);
    }
    
    return template;
  }
  
  /**
   * Load the parameters which are common for all the types of the fields
   * @param field the field configuration
   * @param node the field section
   * @param xpath xpath instance
   * @throws XPathExpressionException
   * @throws SyncConfigurationException
   */
  private void processGeneralField(SyncGeneralField field, Node node, XPath xpath) throws XPathExpressionException, SyncConfigurationException {
    NamedNodeMap attrs = node.getAttributes();
    Node nsNode = attrs.getNamedItem(CONFIG_ATTR_NS);
    Node nameNode = attrs.getNamedItem(CONFIG_ATTR_NAME);
    Node fieldTypeNode = attrs.getNamedItem(CONFIG_ATTR_FIELD_TYPE);
    Node contentTypeNode = attrs.getNamedItem(CONFIG_ATTR_CONTENT_TYPE);
    Node mapToNode = attrs.getNamedItem(CONFIG_ATTR_MAP_TO);
    Node notifyChangeNode = attrs.getNamedItem(CONFIG_ATTR_NOTIFY_CHANGE);
    Node actionNode = attrs.getNamedItem(CONFIG_ATTR_ACTION);
    
    if (nsNode != null) {
      field.setNs(nsNode.getNodeValue());
    }
    if (nameNode != null) {
      field.setName(nameNode.getNodeValue());
    }
    if (fieldTypeNode != null) {
      field.setFieldType(fieldTypeNode.getNodeValue());
    }
    if (contentTypeNode != null) {
      field.setContentType(contentTypeNode.getNodeValue());
    }
    if (mapToNode != null) {
      field.setMapTo(mapToNode.getNodeValue());
    }
    if (notifyChangeNode != null) {
      String notifyChangeVal = notifyChangeNode.getNodeValue();
      if(SyncConstants.BOOLEAN_FALSE.equalsIgnoreCase(notifyChangeVal)) {
        field.setNotifyChange(false);
      }
    }
    if (actionNode != null) {
      String nodeValue = actionNode.getNodeValue();
      ActionType actionType = ActionType.getActionType(nodeValue);
      field.setAction(actionType);
    }
    
    Map<String, String> valueMapping = processValueMapping(node, xpath);
    field.setValueMapping(valueMapping);
    
    String defaultValue = processDefaultValue(node, xpath);
    field.setDefaultValue(defaultValue);
  }
  

  /**
   * Load the fields section.
   * @param fieldsNode the fields section
   * @param xpath xpath instance
   * @return loaded the list of the field configuration
   * @throws XPathExpressionException
   * @throws SyncConfigurationException
   */
  private List<SyncField> processFields(Node fieldsNode, XPath xpath) throws XPathExpressionException, SyncConfigurationException {
    NodeList fieldNodeList = (NodeList) xpath.evaluate(CONFIG_NODE_FIELD, fieldsNode, XPathConstants.NODESET);
    if(fieldNodeList == null){
      return null;
    }
    List<SyncField> fieldList = new ArrayList<SyncField>();
    
    for (int i = 0; i < fieldNodeList.getLength(); i++) {
      Node fieldNode = fieldNodeList.item(i);
      if (fieldNode != null) {
        SyncField field = processField(fieldNode, xpath);
        if(field != null){
          fieldList.add(field);
        }
      }
    }
    return fieldList;
  }
  
  /**
   * Load the field section.
   * @param fieldNode the field section
   * @param xpath xpath instance
   * @return loaded the field configuration
   * @throws XPathExpressionException
   * @throws SyncConfigurationException
   */
  private SyncField processField(Node fieldNode, XPath xpath) throws XPathExpressionException, SyncConfigurationException {
    SyncField field = new SyncField();
    processGeneralField(field, fieldNode, xpath);
    
    NamedNodeMap attributes = fieldNode.getAttributes();

    Node idNode = attributes.getNamedItem(CONFIG_ATTR_ID);
    Node xpathNode = attributes.getNamedItem(CONFIG_ATTR_XPATH);
    Node keepTags = attributes.getNamedItem(CONFIG_ATTR_KEEP_TAGS);
    Node encodeHTML = attributes.getNamedItem(CONFIG_ATTR_ENCODE_HTML);
    Node toDateFormat = attributes.getNamedItem(CONFIG_ATTR_TO_DATE_FORMAT);
    Node fromDateFormat = attributes.getNamedItem(CONFIG_ATTR_FROM_DATE_FORMAT);

    if (idNode != null) {
      field.setId(idNode.getNodeValue());
    }
    if (xpathNode != null) {
      field.setXpath(xpathNode.getNodeValue());
    }
    if (keepTags != null) {
      String keepTagsVal = keepTags.getNodeValue();
      if(SyncConstants.BOOLEAN_TRUE.equalsIgnoreCase(keepTagsVal)) {
        field.setKeepTags(true);
      }
    }
    if (encodeHTML != null) {
      String encodeHTMLVal = encodeHTML.getNodeValue();
      if(SyncConstants.BOOLEAN_TRUE.equalsIgnoreCase(encodeHTMLVal)) {
        field.setEncodeHtml(true);
      }
    }
    if (toDateFormat != null) {
      field.setToDateFormat(toDateFormat.getNodeValue());
    }
    if (fromDateFormat != null) {
      field.setFromDateFormat(fromDateFormat.getNodeValue());
    }
    
    Map<String, String> valueMapping = processValueMapping(fieldNode, xpath);
    field.setValueMapping(valueMapping);
    
    return field;
  }

  /**
   * Load the connection section.
   * The connection configuration is added to the mapping data
   * @param mappingNode the mapping section
   * @param xpath xpath instance
   * @throws XPathExpressionException
   * @throws SyncConfigurationException
   */
  private void processConnection(Node mappingNode, SyncMapping mapping, XPath xpath) throws XPathExpressionException {
    NodeList connNodeList = (NodeList) xpath.evaluate(CONFIG_NODE_CONNECTION, mappingNode, XPathConstants.NODESET);
    
    //username attribute
    if (connNodeList != null && connNodeList.getLength() > 0) {
      Node connNode = connNodeList.item(0);
      NamedNodeMap connAttributes = connNode.getAttributes();
      Node usernameNode = connAttributes.getNamedItem(CONFIG_ATTR_USERNAME);
      if (usernameNode != null) {
        String username = usernameNode.getNodeValue();
        mapping.setUsername(username);
      }

      //password attribute
      Node passwordNode = connAttributes.getNamedItem(CONFIG_ATTR_PASSWORD);
      if (passwordNode != null) {
        String password = passwordNode.getNodeValue();
        mapping.setPassword(password);
      }

      //Headers
      NodeList headderList = (NodeList) xpath.evaluate(CONFIG_NODE_HEADER_REL, connNode, XPathConstants.NODESET);
      if (headderList != null) {
        HashMap<String, String> headers = new HashMap<String, String>();

        for (int i = 0; i < headderList.getLength(); i++) {
          Node headerNode = headderList.item(i);
          NamedNodeMap attributes = headerNode.getAttributes();
          if (attributes != null) {
            Node nameNode = attributes.getNamedItem(CONFIG_ATTR_NAME);
            Node valueNode = attributes.getNamedItem(CONFIG_ATTR_HEADER_VALUE);
            if (nameNode != null && nameNode.getNodeValue() != null && !nameNode.getNodeValue().isEmpty() && valueNode != null && valueNode.getNodeValue() != null) {
              headers.put(nameNode.getNodeValue(), valueNode.getNodeValue());
            }
          }
        }

        if (!headers.isEmpty()) {
          mapping.setHeaders(headers);
        }
      }
      
      //RDF types
      NodeList rdfTypeList = (NodeList) xpath.evaluate(CONFIG_NODE_RDFTYPE_REL, connNode, XPathConstants.NODESET);
      if (rdfTypeList != null) {
        Set<String> rdfTypes = new HashSet<String>();

        for (int i = 0; i < rdfTypeList.getLength(); i++) {
          Node rdfTypeNode = rdfTypeList.item(i);
          NamedNodeMap attributes = rdfTypeNode.getAttributes();
          if (attributes != null) {
            Node valueNode = attributes.getNamedItem(CONFIG_ATTR_VALUE);
            if (valueNode != null && valueNode.getNodeValue() != null && valueNode.getNodeValue() != null && !valueNode.getNodeValue().isEmpty() ) {
              rdfTypes.add(valueNode.getNodeValue());
            }
          }
        }

        if (!rdfTypes.isEmpty()) {
          mapping.setRdfTypes(rdfTypes);
        }
      }
      
    }
  }
  
  /**
   * Load the field configurations section.
   * The field configurations are added to the mapping data (mapping)
   * @param mappingNode the section containing field mapping configuration
   * @param mapping the mapping data
   * @param xpath xpath instance
   * @throws XPathExpressionException
   * @throws SyncConfigurationException
   */
  private void processFieldConfig(Node mappingNode, SyncMapping mapping, XPath xpath) throws XPathExpressionException, SyncConfigurationException {
    NodeList fieldsList = (NodeList) xpath.evaluate(CONFIG_NODE_FIELDS, mappingNode, XPathConstants.NODESET);
    
    if (fieldsList != null) {
      for (int i = 0; i < fieldsList.getLength(); i++) {
        Node fieldsNode = fieldsList.item(i);
        List<SyncField> fieldList = processFields(fieldsNode, xpath);
        if(fieldList != null) {
          mapping.setFields(fieldList);
        }
      }
    }
    processTemplates(mappingNode, mapping, xpath);
    processXmlFieldConfigs(mappingNode, mapping, xpath);

  }
  
  /**
   * Load the field configurations section in xml content of the field.
   * The field configurations are added to the mapping data (mapping)
   * @param mappingNode the section containing the field mapping configurations
   * @param mapping the mapping data
   * @param xpath xpath instance
   * @throws XPathExpressionException
   * @throws SyncConfigurationException
   */
  private void processXmlFieldConfigs(Node mappingNode, SyncMapping mapping, XPath xpath) throws XPathExpressionException, SyncConfigurationException {
    
    NodeList xmlFieldsConfigList = (NodeList) xpath.evaluate(CONFIG_NODE_XMLFIELDS, mappingNode, XPathConstants.NODESET);
    if(xmlFieldsConfigList == null){
      return;
    }
    
    for (int i = 0; i < xmlFieldsConfigList.getLength(); i++) {
      Node xmlFieldsConfigNode = xmlFieldsConfigList.item(i);
      if (xmlFieldsConfigNode != null) {
        SyncXmlFieldConfig xmlFieldConfig = processXmlFieldConfig(xmlFieldsConfigNode, xpath);
        if(xmlFieldConfig != null){
          mapping.addXmlFieldConfig(xmlFieldConfig);
        }
      }
    }
  }
  
  /**
   * Load the field configuration section in xml content of the field.
   * @param xmlFieldsConfigNode the section containing the field mapping configurations
   * @param xpath the xpath instance
   * @return loaded the field configuration section 
   * @throws XPathExpressionException
   * @throws SyncConfigurationException
   */
  private SyncXmlFieldConfig processXmlFieldConfig(Node xmlFieldsConfigNode, XPath xpath) throws XPathExpressionException, SyncConfigurationException {
    SyncXmlFieldConfig xmlFieldConfig = new SyncXmlFieldConfig();
    
    processXmlFieldsConfigProperties(xmlFieldsConfigNode, xmlFieldConfig);
    List<SyncField> fields = processFields(xmlFieldsConfigNode, xpath);
    if(fields != null){
      xmlFieldConfig.setFields(fields);
    }
  
    return xmlFieldConfig;
  }
  
  /**
   * Load the properties of the xml field configuration 
   * @param xmlFieldsNode the field configuration containing the properties for mapping
   * @param xmlFieldsConfig field mapping configuration data
   * @throws XPathExpressionException
   */
  private void processXmlFieldsConfigProperties(Node xmlFieldsNode, SyncXmlFieldConfig xmlFieldsConfig) throws XPathExpressionException {
    NamedNodeMap attributes = xmlFieldsNode.getAttributes();
    
    Node nsNode = attributes.getNamedItem(CONFIG_ATTR_NS);
    Node nameNode = attributes.getNamedItem(CONFIG_ATTR_NAME);

    if (nsNode != null) {
      xmlFieldsConfig.setNs(nsNode.getNodeValue());
    }
    if (nameNode != null) {
      xmlFieldsConfig.setName(nameNode.getNodeValue());
    }
  }
  
  /**
   * Load the section the value mapping
   * @param node the value mapping section
   * @param xpath xpath instance
   * @return the value mapping table
   * @throws XPathExpressionException
   */
  private Map<String, String> processValueMapping(Node node, XPath xpath) throws XPathExpressionException {
    NodeList mapList = (NodeList) xpath.evaluate(CONFIG_NODE_MAP_REL, node, XPathConstants.NODESET);
    
    Map<String, String> valueMapper = null;
    if (mapList != null && mapList.getLength() > 0) {
      valueMapper = new HashMap<String, String>();
      for (int i = 0; i < mapList.getLength(); i++) {
        Node mapNode = mapList.item(i);
        
        NamedNodeMap attrs = mapNode.getAttributes();
        Node keyNode = attrs.getNamedItem(CONFIG_ATTR_KEY);
        Node valueNode = attrs.getNamedItem(CONFIG_ATTR_VALUE);
        
        if (keyNode != null && keyNode.getNodeValue() != null && valueNode != null && valueNode.getNodeValue() != null) {
          valueMapper.put(keyNode.getNodeValue(), valueNode.getNodeValue());
        }
      }
    }
    return valueMapper;
 }
  
  /**
   * Load default value for the value mapping
   * @param node  the section with default value configuration
   * @param xpath xpath instance
   * @return the default value for the value mapping
   * @throws XPathExpressionException
   */
  private String processDefaultValue(Node node, XPath xpath) throws XPathExpressionException {
    Node defaultNode = (Node) xpath.evaluate(CONFIG_ATTR_DEFAULT, node, XPathConstants.NODE);
    if(defaultNode != null){
      NamedNodeMap attrsDefault = defaultNode.getAttributes();
      Node defaultValueNode = attrsDefault.getNamedItem(CONFIG_ATTR_VALUE);
      if(defaultValueNode != null){
        return defaultValueNode.getNodeValue();
      }
    }
    return null;
 }

  /**
   * Load xml value from the node
   * @param node the node containing xml content
   * @return xml value of the node
   */
  public static String loadXMLNodeValue(Node node){
    if(node == null){
      return null;
    }
    
    Document document = node.getOwnerDocument();
    DOMImplementationLS domImplLS = (DOMImplementationLS) document
        .getImplementation();
    LSSerializer serializer = domImplLS.createLSSerializer();

    LSOutput output=domImplLS.createLSOutput();
    output.setEncoding(document.getInputEncoding());
    StringWriter writer=new StringWriter();
    output.setCharacterStream(writer);
    serializer.getDomConfig().setParameter("xml-declaration", false);
    serializer.write(node,output);
    String value =  writer.toString();
    if(value != null){
      value = value.replaceFirst("<"+node.getNodeName()+".*?>\n?", "");
      int idx = value.indexOf("</"+node.getNodeName()+">");
      if(idx >= 0){
        value = value.substring(0, idx);
      }
    }
    return value;
  }
}
