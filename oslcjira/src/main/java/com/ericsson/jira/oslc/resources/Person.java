/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *  
 *  The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *  and the Eclipse Distribution License is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 *  
 *  Contributors:
 *  
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ericsson.jira.oslc.resources;

import java.net.URI;

import org.eclipse.lyo.oslc4j.core.annotation.OslcDescription;
import org.eclipse.lyo.oslc4j.core.annotation.OslcNamespace;
import org.eclipse.lyo.oslc4j.core.annotation.OslcPropertyDefinition;
import org.eclipse.lyo.oslc4j.core.annotation.OslcReadOnly;
import org.eclipse.lyo.oslc4j.core.annotation.OslcResourceShape;
import org.eclipse.lyo.oslc4j.core.annotation.OslcTitle;
import org.eclipse.lyo.oslc4j.core.model.AbstractResource;

import com.ericsson.jira.oslc.Constants;


/**
 * A FOAF Person.
 * 
 * @author Samuel Padgett <spadgett@us.ibm.com>
 * @see <a href="http://xmlns.com/foaf/spec/">FOAF Vocabulary Specification</a>
 */
@OslcNamespace(Constants.FOAF_NAMESPACE)
@OslcResourceShape(title = "FOAF Person Resource Shape", describes = Constants.TYPE_PERSON)
public class Person extends AbstractResource {
  private URI uri = null;
  private String name = null;
  private String mbox = null;

  public URI getUri() {
    return uri;
  }

  public void setUri(URI uri) {
    this.uri = uri;
  }

  @OslcDescription("A FOAF name ")
  @OslcPropertyDefinition(Constants.FOAF_NAMESPACE + "name")
  @OslcReadOnly
  @OslcTitle("Name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
  @OslcDescription("A FOAF Email address ")
  @OslcPropertyDefinition(Constants.FOAF_NAMESPACE + "mbox")
  @OslcReadOnly
  @OslcTitle("Email Address")
  public String getMbox() {
    return mbox;
  }

  public void setMbox(String mbox) {
    this.mbox = mbox;
  }
}
