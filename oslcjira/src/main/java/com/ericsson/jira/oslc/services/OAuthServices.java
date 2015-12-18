/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation.
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
package com.ericsson.jira.oslc.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import net.oauth.OAuth;
import net.oauth.OAuth.Parameter;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthValidator;
import net.oauth.server.OAuthServlet;

import org.apache.wink.json4j.JSON;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.eclipse.lyo.server.oauth.core.AuthenticationException;
import org.eclipse.lyo.server.oauth.core.OAuthConfiguration;
import org.eclipse.lyo.server.oauth.core.OAuthRequest;
import org.eclipse.lyo.server.oauth.core.consumer.ConsumerStore;
import org.eclipse.lyo.server.oauth.core.consumer.ConsumerStoreException;
import org.eclipse.lyo.server.oauth.core.consumer.LyoOAuthConsumer;
import org.eclipse.lyo.server.oauth.core.token.TokenStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.ericsson.jira.oslc.constants.JiraConstants;
import com.ericsson.jira.oslc.handlers.OAuthHandler;
import com.ericsson.jira.oslc.managers.PermissionManager;
import com.ericsson.jira.oslc.oslcclient.Client;
import com.ericsson.jira.oslc.resources.ao.AOManager;
import com.ericsson.jira.oslc.resources.ao.RootServicesEntity;
import com.ericsson.jira.oslc.resources.ao.ServiceProvEntity;
import com.ericsson.jira.oslc.servlet.CredentialsFilter;

/**
 * Issues OAuth request tokens, handles authentication, and then exchanges
 * request tokens for access tokens based on the OAuth configuration set in the
 * {@link OAuthConfiguration} singleton.
 * 
 * @author Samuel Padgett <spadgett@us.ibm.com>
 * @see <a href="http://tools.ietf.org/html/rfc5849">The OAuth 1.0 Protocol</a>
 */
@Path("/oauth")
@AnonymousAllowed
public class OAuthServices {
    
    private static final String CURRENT_CLASS = "OAuthServices";
    
    private static Logger logger = LoggerFactory.getLogger(OAuthServices.class);
    
    @Context
    protected HttpServletRequest httpRequest;
    
    @Context
    protected HttpServletResponse httpResponse;
    
    private TemplateRenderer templateRenderer;

    private static HashMap<String, OAuthAccessor> temporary_accessors = new HashMap<String, OAuthAccessor>();
    private static HashMap<String, String> temporary_domains = new HashMap<String, String>();
    private static HashMap<String, String> temporary_recalls = new HashMap<String, String>();
    private LoginUriProvider loginUriProvider = null;
    
    public OAuthServices() {
    }
    
    /**
     * Constructor
     * 
     * @param templateRenderer
     */
    public OAuthServices(TemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;
    }
    
    public OAuthServices(TemplateRenderer templateRenderer, LoginUriProvider loginUriProvider) {
        this.templateRenderer = templateRenderer;
        this.loginUriProvider = loginUriProvider;
    }
    
    /**
     * Confirms OAuth callback
     * @param oAuthRequest OAuthRequest
     * @return true - it has been confirmed, otherwise false
     * @throws OAuthException
     */
    protected boolean confirmCallback(OAuthRequest oAuthRequest) throws OAuthException {
        boolean callbackConfirmed = OAuthConfiguration.getInstance().getTokenStrategy().getCallback(httpRequest, oAuthRequest.getAccessor().requestToken) != null;
        
        if (callbackConfirmed) {
            oAuthRequest.getConsumer().setOAuthVersion(LyoOAuthConsumer.OAuthVersion.OAUTH_1_0A);
        } else {
            if (!OAuthConfiguration.getInstance().isV1_0Allowed()) {
                throw new OAuthProblemException(OAuth.Problems.OAUTH_PARAMETERS_ABSENT);
            }
            
            oAuthRequest.getConsumer().setOAuthVersion(LyoOAuthConsumer.OAuthVersion.OAUTH_1_0);
        }
        
        return callbackConfirmed;
    }
    
    
    /**
     * Responds with a web page to log in.
     * 
     * @return the response
     * @throws IOException
     *             on I/O errors
     * @throws ServletException
     *             on internal errors validating the request
     * @throws OAuthException  
     *             OAuth exception
     */
    public Response authorize(HttpServletRequest httpReq, HttpServletResponse httpResp) throws ServletException, IOException, OAuthException {
        httpRequest = httpReq;
        httpResponse = httpResp;

        /*
         * Check that the request token is valid and determine what consumer
         * it's for. The OAuth spec does not require that consumers pass the
         * consumer key to the authorization page, so we must track this in
         * the TokenStrategy implementation.
         */
        OAuthMessage message = OAuthServlet.getMessage(httpRequest, null);
        OAuthConfiguration config = OAuthConfiguration.getInstance();
        String consumerKey = config.getTokenStrategy().validateRequestToken(httpRequest, message);
        LyoOAuthConsumer consumer = OAuthConfiguration.getInstance().getConsumerStore().getConsumer(consumerKey);
        // Pass some data to the vm.
        httpRequest.setAttribute("requestToken", message.getToken());
        httpRequest.setAttribute("consumerName", consumer.getName());
        httpRequest.setAttribute("callback", getCallbackURL(message, consumer));
        boolean callbackConfirmed = consumer.getOAuthVersion() == LyoOAuthConsumer.OAuthVersion.OAUTH_1_0A;
        httpRequest.setAttribute("callbackConfirmed", new Boolean(callbackConfirmed));
        
        // The application name is displayed on the OAuth login page.
        httpRequest.setAttribute("applicationName", config.getApplication().getName());
        
        ApplicationUser user = null;
        user = PermissionManager.getLoggedUser();
        
        if (user == null) {
            StringBuffer builder = httpRequest.getRequestURL();
            if (httpRequest.getQueryString() != null) {
                builder.append("?");
                builder.append(httpRequest.getQueryString());
            }
            httpResponse.sendRedirect(loginUriProvider.getLoginUri(URI.create(builder.toString())).toASCIIString());
            return null;
        }
        else{
                Map<String, Object> context = new HashMap<String, Object>();
                context.put("requestToken", message.getToken());
                context.put("consumerName", consumer.getName());
                context.put("applicationName", config.getApplication().getName());
                context.put("callback", getCallbackURL(message, consumer));
                context.put("sessionId", httpRequest.getSession().getId());
                templateRenderer.render("templates/oauth_authorize.vm", context, httpResponse.getWriter());
            return null;
        }
             
    }
    
    /**
     * Returns URL where the server serves callback requests
     * @param message OAuthMessage 
     * @param consumer OAuth consumer
     * @return URL where the server serves callback requests
     * @throws IOException
     * @throws OAuthException
     */
    private String getCallbackURL(OAuthMessage message, LyoOAuthConsumer consumer) throws IOException, OAuthException {
        String callback = null;
        switch (consumer.getOAuthVersion()) {
        case OAUTH_1_0:
            
            if (!OAuthConfiguration.getInstance().isV1_0Allowed()) {
                throw new OAuthProblemException(OAuth.Problems.VERSION_REJECTED);
            }
            
            // If this is OAuth 1.0, the callback should be a request parameter.
            callback = message.getParameter(OAuth.OAUTH_CALLBACK);
            break;
        
        case OAUTH_1_0A:
            // If this is OAuth 1.0a, the callback was passed when the consumer
            // asked for a request token.
            String requestToken = message.getToken();
            callback = OAuthConfiguration.getInstance().getTokenStrategy().getCallback(httpRequest, requestToken);
            break;
        }
        
        if (callback == null) {
            return null;
        }
        
        UriBuilder uriBuilder = UriBuilder.fromUri(callback).queryParam(OAuth.OAUTH_TOKEN, message.getToken());
        if (consumer.getOAuthVersion() == LyoOAuthConsumer.OAuthVersion.OAUTH_1_0A) {
            String verificationCode = OAuthConfiguration.getInstance().getTokenStrategy().generateVerificationCode(httpRequest, message.getToken());
            uriBuilder.queryParam(OAuth.OAUTH_VERIFIER, verificationCode);
        }
        
        return uriBuilder.build().toString();
    }
    
    /**
     * Validates the ID and password on the authorization form. This is intended
     * to be invoked by an XHR on the login page.
     * 
     * @return the response, 409 if login failed or 204 if successful
     * @throws URISyntaxException
     */
    @POST
    @Path("/login")
    @AnonymousAllowed
    public Response login(@FormParam("id") String id, @FormParam("password") String password, @FormParam("requestToken") String requestToken, @FormParam("callback") String callback) throws URISyntaxException {
        String currentMethod = "login";
        try {
            httpRequest.setAttribute("requestToken", requestToken);
            OAuthConfiguration.getInstance().getApplication().login(httpRequest, id, password);
            
        } catch (OAuthException e) {
            logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
            return Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (AuthenticationException e) {
            logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: Incorrect username or password.");
            return Response.status(Status.UNAUTHORIZED).entity("Incorrect username or password.").type(MediaType.TEXT_PLAIN).build();
        }
        
        try {
            OAuthConfiguration.getInstance().getTokenStrategy().markRequestTokenAuthorized(httpRequest, requestToken);
        } catch (OAuthException e) {
            logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
            return Response.status(Status.CONFLICT).entity("Request token invalid.").type(MediaType.TEXT_PLAIN).build();
        }
        
        if (callback == null) {
            return Response.ok().build();
        }
        
        return Response.status(302).location(new URI(callback)).build();
    }
    
    /**
     * Approves OAuth token
     * @param requestToken a request token
     * @return a response containing the result of the approving
     */
    @POST
    @Path("/internal/approveToken")
    @AnonymousAllowed
    public Response authorize(@FormParam("requestToken") String requestToken) {
        String currentMethod = "authorize";
        try {
            OAuthConfiguration.getInstance().getTokenStrategy().markRequestTokenAuthorized(httpRequest, requestToken);
            
            ApplicationUser user = null;
            user = PermissionManager.getLoggedUser();
            
            if (user != null) {
                CredentialsFilter.tokenToUserNameMap.put(requestToken, user.getUsername());
            }
        } catch (OAuthException e) {
            logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
        }
        String callback = httpRequest.getParameter("callback");
        if (callback == null) {
            return Response.ok().build();
        }
        
        try {
            return Response.status(302).location(new URI(callback)).build();
        } catch (URISyntaxException e) {
            logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
        }
        
        return Response.noContent().build();
    }
    
    /**
     * Generates a provisional consumer key. This request must be later approved
     * by an administrator.
     * 
     * @return a JSON response with the provisional key
     * @throws IOException
     * @throws NullPointerException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @see <a
     *      href="https://jazz.net/wiki/bin/view/Main/RootServicesSpecAddendum2">Jazz
     *      Root Services Spec Addendum2</a>
     */
    @POST
    @Path("/requestKey")
    @AnonymousAllowed
    // Some consumers do not set an appropriate Content-Type header.
    public Response provisionalKey() throws NullPointerException, IOException, ClassNotFoundException, SQLException {
        String currentMethod = "provisionalKey";
        
        try {
            logger.debug(CURRENT_CLASS + "." + currentMethod);
            
            // Create the consumer from the request.
            JSONObject request = (JSONObject) JSON.parse(httpRequest.getInputStream());
            
            String name = null;
            if (request.has("name") && request.get("name") != null) {
                name = request.getString("name");
            }
            
            if (name == null || name.trim().equals("")) {
                name = getRemoteHost();
            }
            
            String secret = request.getString("secret");
            
            boolean trusted = false;
            if (request.has("trusted")) {
                trusted = "true".equals(request.getString("trusted"));
            }
            
            String key = UUID.randomUUID().toString();
            LyoOAuthConsumer consumer = new LyoOAuthConsumer(key, secret);
            consumer.setName(name);
            consumer.setProvisional(true);
            consumer.setTrusted(trusted);
            
            OAuthConfiguration config = OAuthConfiguration.getInstance();
            ConsumerStore consumerStore = OAuthHandler.addConsumer(consumer);
            if (consumerStore != null) {
                config.setConsumerStore(consumerStore);
            }
            
            // Respond with the consumer key.
            JSONObject response = new JSONObject();
            response.put("key", key);
            logger.debug(CURRENT_CLASS + "." + currentMethod + " Provisioanl key generated : " + key);
            
            return Response.ok(response.write()).build();
        } catch (JSONException e) {
            logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
            return Response.status(Status.BAD_REQUEST).build();
        } catch (ConsumerStoreException e) {
            logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
            return Response.status(Status.SERVICE_UNAVAILABLE).type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();
        }
    }
    
    /**
     * Validates this is a known consumer and the request is valid using
     * {@link OAuthValidator#validateMessage(net.oauth.OAuthMessage, OAuthAccessor)}
     * . Does <b>not</b> check for any tokens.
     * 
     * @return an OAuthRequest
     * @throws OAuthException
     *             if the request fails validation
     * @throws IOException
     *             on I/O errors
     */
    protected OAuthRequest validateRequest() throws OAuthException, IOException {
        String currentMethod = "validateRequest";
        OAuthRequest oAuthRequest = new OAuthRequest(httpRequest);
        try {
            OAuthValidator validator = OAuthConfiguration.getInstance().getValidator();
            validator.validateMessage(oAuthRequest.getMessage(), oAuthRequest.getAccessor());
        } catch (URISyntaxException e) {
            logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
        return oAuthRequest;
    }
    
    
    protected Response respondWithOAuthProblem(OAuthException e) throws IOException, ServletException {
        String currentMethod = "respondWithOAuthProblem";
        try {
            OAuthServlet.handleException(httpResponse, e, OAuthConfiguration.getInstance().getApplication().getRealm(httpRequest));
        } catch (OAuthProblemException serviceUnavailableException) {
            logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
            return Response.status(Status.SERVICE_UNAVAILABLE).build();
        }
        
        return Response.status(Status.UNAUTHORIZED).build();
    }
    
    /**
     * Gets remote host
     * @return remote host
     */
    private String getRemoteHost() {
        String currentMethod = "getRemoteHost";
        try {
            // Try to get the hostname of the consumer.
            return InetAddress.getByName(httpRequest.getRemoteHost()).getCanonicalHostName();
        } catch (Exception e) {
            /*
             * Not fatal, and we shouldn't fail here. Fall back to returning
             * ServletRequest.getRemoveHost(). It might be the IP address, but
             * that's better than nothing.
             */
            logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
            return httpRequest.getRemoteHost();
        }
    }
    
    // Since Atlassian Jira has its own OAuth provider we can't simply pass the
    // OAuth headers
    // and expect our plugin endpoints to handle them since Jira will consume
    // the headers
    // and try to authenticate them itself. Instead we create our own filter
    // that calls
    // requestToken and accessToken methods in this class
    public void requestToken(HttpServletRequest request, HttpServletResponse response) {
        String currentMethod = "requestToken";
        try {
            logger.debug("requestToken");
            
            OAuthRequest oAuthRequest = validateRequest(request);
            
            logger.debug("requestToken request validated");
            
            // Generate the token.
            OAuthConfiguration.getInstance().getTokenStrategy().generateRequestToken(oAuthRequest);
            boolean callbackConfirmed = confirmCallback(oAuthRequest);
            
            logger.debug("requestToken callback confirmed: " + callbackConfirmed);
            
            // Respond to the consumer.
            OAuthAccessor accessor = oAuthRequest.getAccessor();
            tokenResponse(response, accessor.requestToken, accessor.tokenSecret, callbackConfirmed);
        } catch (Exception e) {
            logger.error(CURRENT_CLASS + "." + currentMethod + "Exception: " + e.getMessage());
            errorResponse(response, e.getMessage());
        }
    }
    
    /**
     * Accesses AAuth token
     * @param request a request
     * @param response a response
     */
    public void accessToken(HttpServletRequest request, HttpServletResponse response) {
        String currentMethod = "accessToken";
        try {
            logger.debug("accessToken");
            
            // Validate the request is signed and check that the request token is valid.
            OAuthRequest oAuthRequest = validateRequest(request);
            
            logger.debug("accessToken request validated");
            
            OAuthConfiguration config = OAuthConfiguration.getInstance();
            TokenStrategy strategy = config.getTokenStrategy();
            
            strategy.validateRequestToken(httpRequest, oAuthRequest.getMessage());
            
            // The verification code MUST be passed in the request if this is
            // OAuth 1.0a.
            if (!config.isV1_0Allowed() || oAuthRequest.getConsumer().getOAuthVersion() == LyoOAuthConsumer.OAuthVersion.OAUTH_1_0A) {
                logger.debug("accessToken OAuth 1.0A");
                
                strategy.validateVerificationCode(oAuthRequest);
                
                logger.debug("accessToken verification code validated");
            } else {
                logger.debug("accessToken OAuth 1.0");
            }
            
            // Generate a new access token for this accessor.
            strategy.generateAccessToken(oAuthRequest);
            OAuthAccessor accessor = oAuthRequest.getAccessor();
            
            logger.debug("accessToken token  = " + accessor.accessToken);
            logger.debug("accessToken secret = " + accessor.tokenSecret);
            
            // Send the new token and secret back to the consumer.
            tokenResponse(response, accessor.accessToken, accessor.tokenSecret, false);
        } catch (Exception e) {
            logger.error(CURRENT_CLASS + "." + currentMethod + "Exception: " + e.getMessage());
            errorResponse(response, e.getMessage());
        }
    }
    
    /**
     * Responses on OAuth token request
     * @param response HttpServletResponse
     * @param token OAuth token 
     * @param secret OAuth secret 
     * @param callbackConfirmed if callback is confirmed
     * @throws IOException
     */
    private void tokenResponse(HttpServletResponse response, String token, String secret, boolean callbackConfirmed) throws IOException {
        List<Parameter> oAuthParameters = OAuth.newList(OAuth.OAUTH_TOKEN, token, OAuth.OAUTH_TOKEN_SECRET, secret);
        if (callbackConfirmed) {
            oAuthParameters.add(new Parameter(OAuth.OAUTH_CALLBACK_CONFIRMED, "true"));
        }
        String encoded = OAuth.formEncode(oAuthParameters);
        response.setContentType(MediaType.TEXT_PLAIN);
        PrintWriter writer = response.getWriter();
        writer.print(encoded);
        writer.flush();
    }
    
    /**
     * Validates received OAuth reuqest
     * @param request HttpServletRequest 
     * @return OAuth Request
     * @throws OAuthException
     * @throws IOException
     */
    private OAuthRequest validateRequest(HttpServletRequest request) throws OAuthException, IOException {
        String currentMethod = "validateRequest";
        OAuthRequest oAuthRequest = new OAuthRequest(request);
        try {
            OAuthValidator validator = OAuthConfiguration.getInstance().getValidator();
            validator.validateMessage(oAuthRequest.getMessage(), oAuthRequest.getAccessor());
        } catch (URISyntaxException e) {
            logger.error(CURRENT_CLASS + "." + currentMethod + "Exception: " + e.getMessage());
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
        
        return oAuthRequest;
    }
    
    /**
     * Response with error code 500
     * @param response HttpServletResponse
     * @param message the error message
     */
    private void errorResponse(HttpServletResponse response, String message) {
        String currentMethod = "errorResponse";
        try {
            response.sendError(500, message);
        } catch (IOException e) {
            // Ignore
            logger.error(CURRENT_CLASS + "." + currentMethod + "Exception: " + e.getMessage());
        }
    }
    
    /**
     * Removes a cosumer from consumer store
     * @param consumerKey tke key of the consumer
     * @return a response of a request
     */
    @DELETE
    @AnonymousAllowed
    @Path("/consumers/remove/{consumerKey}")
    public Response removeConsumer(@PathParam("consumerKey") String consumerKey) {
        String currentMethod = "removeConsumer";
        logger.debug(CURRENT_CLASS + "." + currentMethod + " Consumer key: " + consumerKey);
        
        ApplicationUser user = PermissionManager.getLoggedUser();
        
        if (user == null) {
            return Response.status(Status.UNAUTHORIZED).build();
        }
        try {
            OAuthConfiguration config = OAuthConfiguration.getInstance();
            ConsumerStore consumerStore = OAuthHandler.removeConsumer(consumerKey);
            if (consumerStore != null) {
                config.setConsumerStore(consumerStore);
            }
        } catch (Exception e) {
            logger.error(CURRENT_CLASS + "." + currentMethod + "Exception: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        
        return Response.ok().build();
    }
    
    /**
     * Activates a consumer. Sets provisional key to false
     * @param consumerKey the key of consumer
     * @return a response of a request
     */
    @PUT
    @AnonymousAllowed
    @Path("/consumers/activate/{consumerKey}")
    public Response activateConsumer(@PathParam("consumerKey") String consumerKey) {
        String currentMethod = "activateConsumer";
        logger.debug(CURRENT_CLASS + "." + currentMethod + " Consumer key: " + consumerKey);
        
        ApplicationUser user = PermissionManager.getLoggedUser();
        
        if (user == null) {
            return Response.status(Status.UNAUTHORIZED).build();
        }
        try {
            OAuthConfiguration config = OAuthConfiguration.getInstance();
            ConsumerStore consumerStore = OAuthHandler.updateConsumer(consumerKey, false);
            if (consumerStore != null) {
                config.setConsumerStore(consumerStore);
            }
        } catch (Exception e) {
            logger.error(CURRENT_CLASS + "." + currentMethod + "Exception: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        
        return Response.ok().build();
    }
    
    /**
     * Removes a rootservices
     * @param unique id of the rootservices
     * @return a response of a request
     */
    @DELETE
    @AnonymousAllowed
    @Path("/rootserviceslinks/removelink/{entityID}")
    public Response removeRootServicesEntity(@PathParam("entityID") String entityID) {
        final int id = Integer.parseInt(entityID);
        
        AOManager mngr = AOManager.getInstance();
        mngr.remove(RootServicesEntity.class, id);
        
        return Response.ok().build();
    }
    
    /**
     * Removes OSLC service provider catalog
     * @param entityID unique id of the catalog
     * @return a response of a request
     */
    @DELETE
    @AnonymousAllowed
    @Path("/serviceprovidercatalogslinks/removelink/{entityID}")
    public Response removeServiceProviderCatalogEntity(@PathParam("entityID") String entityID) {
        final int id = Integer.parseInt(entityID);
        
        AOManager mngr = AOManager.getInstance();
        mngr.remove(RootServicesEntity.class, id);
        
        return Response.ok().build();
    }
    
    /**
     * Removes OSLC service provider
     * @param entityID unique id of service provider
     * @return a response of a request
     */
    @DELETE
    @AnonymousAllowed
    @Path("/serviceproviderslinks/removelink/{entityID}")
    public Response removeServiceProviderEntity(@PathParam("entityID") String entityID) {
        final int id = Integer.parseInt(entityID);
        
        AOManager mngr = AOManager.getInstance();
        mngr.remove(ServiceProvEntity.class, id);
        
        return Response.ok().build();
    }
    
    /**
     * Method continues in 1st phase of OAuth dance. Previously obtained request
     * token should be authorized by user. Method prepares authorization url,
     * which is forwarded to javascript as 401 response. Javascript then
     * displays authorization url in new window - authorization window.
     * 
     * Note: authorization url carries also callback url with "re-call" argument,
     * which is name of javascript function. This function will be called, when
     * new authorization window is closed.
     * 
     * @return response back to javascript
     */
    @GET
    @AnonymousAllowed
    @Path("/authorizationcallback")
    public Response authorizationcallbackDialog() {
        String currentMethod = "authorizationcallbackDialog";
        
        // message came from view-oslclinks.vm
        String targeturl = httpRequest.getParameter("targeturl");
        String recall = httpRequest.getParameter("recall");
        
        if (targeturl != null && recall != null) {
            
            // 1st phase of OAuth dance
            
            OAuthConsumer consumer = OAuthHandler.getConsumer(targeturl);
            if (consumer == null) {
                return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).entity("Fatal error!\\nGetting consumer failed (possible incorrect OAuth domain)!").build();
            }
            
            
            String key = UUID.randomUUID().toString();
            
            String callback = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL) + JiraConstants.OAUTH_EXT_CALLBACK_SERVICE_URL + "?key=" + key;
            
            Client client = new Client();
            
            // get request token
            OAuthAccessor ac = client.oAuthDancePhase1(consumer, callback);
            
            // save all needed values to temporary store
            temporary_accessors.put(key, ac);
            temporary_domains.put(key, targeturl);
            temporary_recalls.put(key, recall);
            
            String userAuthURL = consumer.serviceProvider.userAuthorizationURL + "?oauth_token=" + ac.requestToken + "&oauth_callback=" + OAuth.percentEncode(callback);
            
            // authorize request token
            try {
                httpResponse.sendRedirect(userAuthURL);
                return Response.seeOther(new URI(userAuthURL)).build();
            } catch (IOException e) {
                logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
            } catch (URISyntaxException e) {
                logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
            }
        }
        
        return Response.ok().build();
    }
    
    /**
     * Method finishes 1st phase of OAuth dance. It gets response on user
     * authorization and invokes 2nd phase of OAuth dance (@see
     * com.ericsson.jira.oslc.oslcclient.Client#oAuthDancePhase2).
     * 
     * Note: this happens in authorization window.
     * 
     * @param httpRequest HttpServletRequest
     * @return httpResponse back to authorization window
     */
    public void oAuthAuthorizationCallback(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String currentMethod = "oAuthAuthorizationCallback";
        
        // message came from remote app as response to authorization
        String verifier = httpRequest.getParameter("oauth_verifier");
        String key = httpRequest.getParameter("key");
        
        // 2nd phase of OAuth dance
        
        HttpSession session = httpRequest.getSession();
        String rl = temporary_domains.remove(key);
        
        OAuthConsumer consumer = OAuthHandler.getConsumer(rl);
        OAuthAccessor ac = temporary_accessors.remove(key);
        String recallF = temporary_recalls.remove(key);
        String closeWindowScript = "";
        
        if (consumer != null) {
            Client client = new Client();
            client.oAuthDancePhase2(consumer, ac, verifier);
            
            // save accessor to session
            OAuthHandler.saveAccessorToSession(session, ac, consumer.consumerKey);
            
            closeWindowScript = "<html><head><script>opener." + recallF // +
                                                                        // "()"
                    + ";window.close();</script></head><body></body></html>";
        } else {
            logger.error(CURRENT_CLASS + "." + currentMethod + " Error: consumer = null");
            closeWindowScript = "<html><head></head><body>" + "Fatal error!<br/>Getting consumer failed (possible incorrect OAuth domain)!" + "</body></html>";
        }
        
        try {
            httpResponse.getWriter().write(closeWindowScript);
        } catch (IOException e) {
            logger.error(CURRENT_CLASS + "." + currentMethod + " Exception: " + e.getMessage());
        }
    }
}
