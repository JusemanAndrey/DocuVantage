/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.docuvantage.andhelper.hessian;

import com.dv.edm.api.intrface.WebApi;
import java.net.CookieManager;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Sarel
 */
public class DVWebApiClient {

    private static String DEFAULT_WEBAPI_URL = "https://www.docuvantageondemand.com:443/dvwebapi/h/";
    private static int DEFAULT_CONNECT_TIMEOUT = 5;

    private String serviceUrl = DEFAULT_WEBAPI_URL;
    private int connectTimeoutInSeconds = DEFAULT_CONNECT_TIMEOUT;
    private String sessionId;

    private WebApi webApiProxy;
    
    /** If this request is being made on behalf of another system puts its IP here. This can be a comma and space separated list of multiple machines in a chain */
    private ThreadLocal<String> xForwardedFor = null;
    

    public DVWebApiClient() {
        this(null, true);
    }

    public DVWebApiClient(boolean useSystemProxies) {
        this(null, useSystemProxies);
    }

    public DVWebApiClient(String sessionId) {
        this(sessionId, true);
    }

    public DVWebApiClient(String sessionId, boolean useSystemProxies) {
        this(DEFAULT_WEBAPI_URL, sessionId, useSystemProxies, DEFAULT_CONNECT_TIMEOUT);
    }

    public DVWebApiClient(String serviceUrl, String sessionId, boolean useSystemProxies, int connectTimeoutInSeconds) {
        try {
            this.serviceUrl = serviceUrl;
            this.sessionId = sessionId;
            this.connectTimeoutInSeconds = connectTimeoutInSeconds;

            if (useSystemProxies) {
//                System.setProperty("java.net.useSystemProxies", "true");
            }

            // remove the java applet/javaws CookieManager because it interferes with the JSESSIONID cookie we try to set which came from the browser
            CookieManager.setDefault(null);

            Map<String, String> cookies = new HashMap();
            // if session does not exist create it and set it
            if (sessionId == null || sessionId.length() == 0) {
                WebApi tempProxy = (WebApi) MyProxyFactory.getProxy(WebApi.class, new URL(serviceUrl), this.getClass().getClassLoader(), connectTimeoutInSeconds * 1000, true, cookies, xForwardedFor);
                sessionId = tempProxy.getWebSessionId();
            }
            cookies.put("JSESSIONID", sessionId);
            webApiProxy = (WebApi) MyProxyFactory.getProxy(WebApi.class, new URL(serviceUrl), this.getClass().getClassLoader(), connectTimeoutInSeconds * 1000, true, cookies, xForwardedFor);
        }
        catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @return the connectTimeoutInSeconds
     */
    public int getConnectTimeoutInSeconds() {
        return connectTimeoutInSeconds;
    }

    /**
     * @param connectTimeoutInSeconds the connectTimeoutInSeconds to set
     */
    public void setConnectTimeoutInSeconds(int connectTimeoutInSeconds) {
        this.connectTimeoutInSeconds = connectTimeoutInSeconds;
    }

    /**
     * @return the webApiProxy
     */
    public WebApi getWebApiProxy() {
        return webApiProxy;
    }

    /**
     * @param webApiProxy the webApiProxy to set
     */
    public void setWebApiProxy(WebApi webApiProxy) {
        this.webApiProxy = webApiProxy;
    }

}
