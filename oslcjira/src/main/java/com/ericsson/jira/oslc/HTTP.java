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

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.jira.oslc.utils.LogUtils;


/**
 * A simple HTTP client with support for authentication.
 */
public class HTTP {
  private static final String CURRENT_CLASS = "HTTP";
  
  public static enum OAuthPhases {
    /**
     * OAuth dance phase 1 - getting request token
     */
    OAUTH_PHASE_1,
    /**
     * OAuth dance phase 2 - user authorization and getting access token
     */
    OAUTH_PHASE_2,
    /**
     * OAuth authorization (using access token)
     */
    OAUTH_PHASE_3
  };
  
  private static final Logger logger = LoggerFactory.getLogger(HTTP.class);
  
  /**
   * It wraps the DefaultHttpClient to avoid to use ssl connection.
   * @param base default http client
   * @return
   */
  private static DefaultHttpClient wrapClient(DefaultHttpClient base) {
    String currentMethod = "wrapClient";
    try {
      SSLContext ctx = SSLContext.getInstance("TLS");
      X509TrustManager tm = new X509TrustManager() {

        public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
          return (X509Certificate[]) null;
        }
      };
      ctx.init(null, new TrustManager[] { tm }, null);
      SSLSocketFactory ssf = new SSLSocketFactory(ctx);
      ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
      ClientConnectionManager ccm = base.getConnectionManager();
      SchemeRegistry sr = ccm.getSchemeRegistry();
      sr.register(new Scheme("https", ssf, 443));
      return new DefaultHttpClient(ccm, base.getParams());
    } 
    catch (Exception ex) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + ex.getMessage());
      return null;
    }
  }
  
  /**
   * Creates OAuth header which is different for individual oauth phases
   * @param url the url where the request will be sent
   * @param method http method - POST, PUT, GET ...
   * @param accessor OAuth accessor
   * @param verifier verification code
   * @param phase oauth phase
   * @return OAuth header
   */
  private static String getOAuthAuthorizationHeader(
      String url, String method, OAuthAccessor accessor, String verifier, OAuthPhases phase) {
    String currentMethod = "getOAuthAuthorization";
    
    String oAuthHeader = null;
    
    List<OAuth.Parameter> params = new ArrayList<OAuth.Parameter>();
    switch (phase) {
      case OAUTH_PHASE_1:
        params.add(new OAuth.Parameter("oauth_callback", accessor.consumer.callbackURL));
        break;
      case OAUTH_PHASE_2:
        params.add(new OAuth.Parameter("oauth_token", accessor.requestToken));
        if (verifier != null) {
          params.add(new OAuth.Parameter("oauth_verifier", verifier));
        }
        break;
      case OAUTH_PHASE_3:
        params.add(new OAuth.Parameter("oauth_token", accessor.accessToken));
        break;
    }
    
    OAuthMessage request;
    try {
      request = accessor.newRequestMessage(method, url, params);
      oAuthHeader = request.getAuthorizationHeader("JIRA");
    }
    catch (OAuthException e) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
    } 
    catch (IOException e) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
    } 
    catch (URISyntaxException e) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
    }
    
    return oAuthHeader;
  }
  
  /**
   * Get resource on defined url
   * @param url the location of the resource
   * @param accept string with accept content definition (e.g. "text/html")
   * @param user user login used in basic authorization
   * @param password user password used in basic authorization
   * @param headers the headers which will be added to the request
   * @return http response from successful request, otherwise return null
   * @throws ClientProtocolException
   * @throws IOException
   */
  public static HttpResponse get(String url, String accept, String user, String password, Map<String, String> headers) 
      throws ClientProtocolException, IOException {
    return get(url, accept, null, user, password, null, null, null, headers);
  }
  
  /**
   * Get resource on defined url
   * @param url the location of the resource
   * @param accept string with accept content definition (e.g. "text/html")
   * @param content content-type - the type of the content in the body of the request (e.g. "text/html")
   * @param user user login used in basic authorization
   * @param password user password used in basic authorization
   * @param headers the headers which will be added to the request
   * @return http response from successful request, otherwise return null
   * @throws ClientProtocolException
   * @throws IOException
   */
  public static HttpResponse get(String url, String accept, String content, String user, String password, Map<String, String> headers) 
      throws ClientProtocolException, IOException {
    return get(url, accept, content, user, password, null, null, OAuthPhases.OAUTH_PHASE_3, headers);
  }
  
  /**
   * Methods create and send http GET request to given url. 
   * @param url link, where request is sent
   * @param accept string with accept content definition (e.g. "text/html")
   * @param user user login used in basic authorization
   * @param password user password used in basic authorization
   * @param accessor used in OAuth authorization, contains consumer information and tokens
   * @param phase OAuth authorization phase
   * @param headers the headers which will be added to the request
   * @return http response from successful request, otherwise return null
   * @throws IOException 
   * @throws ClientProtocolException 
   */
  public static HttpResponse get(String url, String accept, String content, 
      String user, String password, OAuthAccessor accessor, String verifier, OAuthPhases phase, Map<String, String> headers) 
          throws ClientProtocolException, IOException {
    String currentMethod = "get";
    
    DefaultHttpClient client = new DefaultHttpClient();
    client = wrapClient(client);
    
    HttpGet httpget = new HttpGet(url);
    

    
    httpget.addHeader("OSLC-Core-Version", "2.0");
    if (accept != null)
      httpget.addHeader("Accept", accept);
    if (content != null)
      httpget.addHeader("Content-Type", content);
    
    if (accessor != null) {
      httpget.addHeader(
          "Authorization", getOAuthAuthorizationHeader(url, "GET", accessor, verifier, phase));
    }
    else if (user != null && password != null) {
      StringBuilder builder = new StringBuilder();
      httpget.addHeader("Authorization", "Basic " + 
          javax.xml.bind.DatatypeConverter.printBase64Binary(
              builder.append(user).append(':').append(password).toString().getBytes()));
    }
    
    if(headers != null){
      Set<Entry<String, String>> entrySet = headers.entrySet();
      for (Entry<String, String> entry : entrySet) {
        httpget.addHeader(entry.getKey(), entry.getValue());
      }
    }
    
    try {
      String logMessage =LogUtils.createLogMessage(url, null, httpget.getAllHeaders(),"GET");
      logger.debug(logMessage);
      HttpResponse response = client.execute(httpget);
      logResponse(response, url, "GET");
      return response;
      
    } 
    catch (ClientProtocolException e) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      throw e;
    } 
    catch (IOException e) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      throw e;
    }
  }
  
  /**
   * Methods creates and send http POST request to given url. 
   * @param url link, where request is sent
   * @param body body of request, which will be sent
   * @param accept accept string with accept content definition (e.g. "text/html")
   * @param content content string with body format definition (e.g. "text/html")
   * @param user user login used in basic authorization
   * @param password user password used in basic authorization
   * @return http response from successful request, otherwise returns null
   * @throws IOException 
   * @throws ClientProtocolException 
   */
  public static HttpResponse post(String url, String body, String accept, String content, 
      String user, String password) throws ClientProtocolException, IOException {
    return post(url, body, accept, content, user, password, null, null, OAuthPhases.OAUTH_PHASE_3);
  }
  
  /**
   * Methods creates and send http POST request to given url. 
   * @param url link, where request is sent
   * @param body body of request, which will be sent
   * @param accept accept string with accept content definition (e.g. "text/html")
   * @param content content string with body format definition (e.g. "text/html")
   * @param user user login used in basic authorization
   * @param password user password used in basic authorization
   * @param accessor used in OAuth authorization, contains consumer information and tokens
   * @param phase OAuth authorization phase
   * @return http response from successful request, otherwise returns null
   * @throws IOException 
   * @throws ClientProtocolException 
   */
  public static HttpResponse post(String url, String body, String accept, String content, 
      String user, String password, OAuthAccessor accessor, String verifier, OAuthPhases phase) 
          throws ClientProtocolException, IOException {
    String currentMethod = "post";
    DefaultHttpClient client = new DefaultHttpClient();
    client = wrapClient(client);
    
    HttpPost httpPost = new HttpPost(url);
    
    httpPost.addHeader("OSLC-Core-Version", "2.0");
    if (accept != null)
      httpPost.addHeader("Accept", accept);
    if (content != null)
      httpPost.addHeader("Content-Type", content);
    
    if (accessor != null) {
      httpPost.addHeader(
          "Authorization", getOAuthAuthorizationHeader(url, "POST", accessor, verifier, phase));
    }
    else if (user != null && password != null) {
      StringBuilder builder = new StringBuilder();
      httpPost.addHeader("Authorization", "Basic " + 
        javax.xml.bind.DatatypeConverter.printBase64Binary(
            builder.append(user).append(':').append(password).toString().getBytes()));
    }

    try {
      httpPost.setEntity(new StringEntity(body, "UTF-8"));
      
      String logMessage =LogUtils.createLogMessage(url, null, httpPost.getAllHeaders(),"POST");
      logger.debug(logMessage);
      HttpResponse response = client.execute(httpPost);
      logResponse(response, url, "POST");
      return response;
      
    } 
    catch (ClientProtocolException e) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      throw e;
    } 
    catch (IOException e) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      throw e;
    }
  }
  
  /**
   * Methods creates and send http PUT request to given url. 
   * @param url link, where request is sent
   * @param body body of request, which will be sent
   * @param accept accept string with accept content definition (e.g. "text/html")
   * @param content content string with body format definition (e.g. "text/html")
   * @param user user login used in basic authorization
   * @param password user password used in basic authorization
   * @param accessor used in OAuth authorization, contains consumer information and tokens
   * @param phase OAuth authorization phase
   * @return http response from successful request, otherwise returns null
   * @throws IOException 
   * @throws ClientProtocolException 
   */
  public static HttpResponse put(String url, String body, String accept, String content, 
      String user, String password, OAuthAccessor accessor, String appendix, OAuthPhases phase) 
          throws ClientProtocolException, IOException {
    return put(url, body, accept, content, 
        user, password, accessor, appendix, phase, null);
}
  
  /**
   * Methods creates and send http PUT request to given url. 
   * @param url link, where request is sent
   * @param body body of request, which will be sent
   * @param accept accept string with accept content definition (e.g. "text/html")
   * @param content content string with body format definition (e.g. "text/html")
   * @param user user login used in basic authorization
   * @param password user password used in basic authorization
   * @param accessor used in OAuth authorization, contains consumer information and tokens
   * @param phase OAuth authorization phase
   * @param headers http headers
   * @return http response from successful request, otherwise returns null
   * @throws IOException 
   * @throws ClientProtocolException 
   */
  public static HttpResponse put(String url, String body, String accept, String content, 
      String user, String password, OAuthAccessor accessor, String appendix, OAuthPhases phase, Map<String, String> headers) 
          throws ClientProtocolException, IOException {
    String currentMethod = "put";
    
    DefaultHttpClient client = new DefaultHttpClient();
    client = wrapClient(client);
    
    String targetUrl = url;
    if (appendix != null)
      targetUrl += appendix;
    
    HttpPut httpput = new HttpPut(targetUrl);
    
    httpput.addHeader("OSLC-Core-Version", "2.0");
    if (accept != null)
      httpput.addHeader("Accept", accept);
    if (content != null)
      httpput.addHeader("Content-Type", content);
    
    
    if(headers != null){
      Set<Entry<String, String>> entrySet = headers.entrySet();
      for (Entry<String, String> entry : entrySet) {
        httpput.addHeader(entry.getKey(), entry.getValue());
      }
    }
    
    if (accessor != null) {
      httpput.addHeader(
          "Authorization", "OAuth " + getOAuthAuthorizationHeader(targetUrl, "PUT", accessor, null, phase));
    }
    else if (user != null && password != null) {
      StringBuilder builder = new StringBuilder();
      httpput.addHeader("Authorization", "Basic " + 
        javax.xml.bind.DatatypeConverter.printBase64Binary(
            builder.append(user).append(':').append(password).toString().getBytes()));
    }

    try {
      httpput.setEntity(new StringEntity(body, "UTF-8"));
      String logMessage =LogUtils.createLogMessage(url, body, httpput.getAllHeaders(),"PUT");
      logger.debug(logMessage);
      
      HttpResponse response = client.execute(httpput);
      logResponse(response, url, "PUT");
      
      return response;
      
    } 
    catch (ClientProtocolException e) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      throw e;
    } 
    catch (IOException e) {
      logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
      throw e;
    }
  }
  
  private static void logResponse(HttpResponse response, String url, String action){
    if (response != null && response.getStatusLine() != null) {
      logger.debug("Response of " + action + " request for uri = " + url + ", responseCode = " + response.getStatusLine().getStatusCode() + ", responsePhrase = " + response.getStatusLine().getReasonPhrase());
    } else {
      logger.error("Response of " + action + " request for uri  = " + url + " is null");
    }
  }
  

}
