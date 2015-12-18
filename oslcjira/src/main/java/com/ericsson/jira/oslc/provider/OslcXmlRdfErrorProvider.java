package com.ericsson.jira.oslc.provider;

/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 which
 * accompanies this distribution. The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html and the Eclipse Distribution
 * License is available at http://www.eclipse.org/org/documents/edl-v10.php.
 * Contributors: Russell Boykin - initial API and implementation Alberto
 * Giammaria - initial API and implementation Chris Peters - initial API and
 * implementation Gianluca Bernardini - initial API and implementation Steve
 * Pitschke - Add support for FilteredResource and ResponseInfo
 *******************************************************************************/

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.eclipse.lyo.oslc4j.core.model.Error;
import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;
import org.eclipse.lyo.oslc4j.core.model.ResponseInfo;
import org.eclipse.lyo.oslc4j.provider.jena.AbstractOslcRdfXmlProvider;

@Provider
@Produces({ OslcMediaType.APPLICATION_RDF_XML, OslcMediaType.APPLICATION_XML, OslcMediaType.TEXT_XML })
@Consumes({ OslcMediaType.APPLICATION_RDF_XML, OslcMediaType.APPLICATION_XML, OslcMediaType.TEXT_XML })
public class OslcXmlRdfErrorProvider extends AbstractOslcRdfXmlProvider implements MessageBodyWriter<Error> {
  public OslcXmlRdfErrorProvider() {
    super();
  }

  @Override
  public long getSize(final Error object, final Class<?> type, final Type genericType, final Annotation[] annotation, final MediaType mediaType) {
    return -1;
  }

  @Override
  public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    if (Error.class.isAssignableFrom(type)) {
      return isWriteable(type, annotations, mediaType, OslcMediaType.APPLICATION_RDF_XML_TYPE, OslcMediaType.APPLICATION_XML_TYPE, OslcMediaType.TEXT_XML_TYPE, OslcMediaType.TEXT_TURTLE_TYPE);
    }
    return false;
  }

  @Override
  public void writeTo(final Error object, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String, Object> map, final OutputStream outputStream) throws IOException, WebApplicationException {
    Object[] objects;
    Map<String, Object> properties = null;
    String descriptionURI = null;
    String responseInfoURI = null;
    ResponseInfo<?> responseInfo = null;
    objects = new Object[] { object };

    writeTo(objects, mediaType, map, outputStream, properties, descriptionURI, responseInfoURI, responseInfo);
  }

}
