package com.docuvantage.andhelper.hessian;


/*
 * DVClientHelper.java
 *
 * Created on October 12, 2007, 11:54 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

import android.os.RemoteException;

import com.dv.edm.api.bean.DUser;
import com.dv.edm.api.bean.DSessionID;
import com.dv.edm.api.intrface.EngineInterface;
import com.dv.edm.api.exception.DException;
import com.dv.edm.api.util.EngineProxyHelper;
import com.dv.edm.api.util.EngineProxyHelperListener;
import com.dv.util.base64.Base64;
import java.io.IOException;
import java.io.Serializable;
import java.net.CookieManager;
import java.net.URL;
import java.net.URLEncoder;
//import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * When fall back is implemented it should first try the
 * secure service, then fall back to the insecure service over HTTPS
 * @author Sarel
 */
public class DVClientHelper implements Serializable {
    static final long serialVersionUID = 1L;

    private static String DEFAULT_HESSIAN_URL = "https://www.docuvantageondemand.com:443/dvapi/2/h/";

    transient private EngineInterface engine;
    /** This engine proxy does not have a read timeout so it's suitable for use with the uploadFile API call() */
    transient private EngineInterface engineWithoutTimeout;
    private int connectTimeout = 5000; // 5 seconds
    /** The default is the encrypted DVStoreSSL service */
    private String rmiServiceUrl = "//www.docuvantageondemand.com:1100/DVStoreSSL";
    /** If we fall-back to HTTP then we want to use the non-encrypted service because  */
    private String httpCgiServiceUrl = "//localhost:1100/DVStore";
    /** The path to the CGI or servlet that will forward for us. */
    private String httpProxyUrl = "https://www.docuvantageondemand.com:443/cgi-bin/java-rmi.cgi";
    /** The URL for API calls via Hessian */
    private String hessianHttpUrl = DEFAULT_HESSIAN_URL;
    /** Use a local keystore for trusted certificates */
    private boolean useLocalTrustStore = false;
    /** The user we're logged in as */
    private DUser user;
    /** The sessionId for the engine on the server */
    private DSessionID sessionId;
    /** Set this to get callbacks when API call events occur */
    transient private EngineProxyHelperListener listener = null;
    /** In case of network error keep retrying until this number of tries have been reached */
    private int numTries = 4;
    /** After the first call delay the next retry using Thread.sleep() */
    private int delayAfterFirstTryInMilliseconds = 1000;
    /** After the second try multiply the delay by this number each time */
    private float multiplyDelayEachTimeAfterSecondTry = 2F;
    /** Make use of session cookies */
    private boolean useCookies = false;
    /** Make all HTTP calls go through the proxy server configured on this system. This just sets the system property java.net.useSystemProxies=true in connect() */
    private boolean useSystemProxies = true;
    /** If this request is being made on behalf of another system puts its IP here. This can be a comma and space separated list of multiple machines in a chain */
    private transient ThreadLocal<String> xForwardedFor = null;

    public static Log log = LogFactory.getLog(DVClientHelper.class);

    /** Creates a new instance of DVClientHelper */
    public DVClientHelper() {
    }
    
    /**
     * Connect to the service and login with login and password
     * This always tunnels over HTTPS.
     */
    public EngineInterface connect(String login, String password) throws Exception {
        return connect((DSessionID)null, login, password);
    }
    
    /**
     * Connect to the service and get an existing engine by SessionId.
     * Useful for when multiple programs need to connect using the same session.
     */
    public EngineInterface connect(DSessionID sessionId) throws Exception {
        return connect(sessionId, null, null);
    }
    
    /** */
    private EngineInterface connect(DSessionID mySessionId, String login, String password) throws RuntimeException {
        
        CookieManager.setDefault(null);
        
        try {
            log.debug("Timeout: " + getConnectTimeout());
            
            if (isUseSystemProxies()) {
                System.setProperty("java.net.useSystemProxies", "true");
            }

            log.debug("Using hessian: " + hessianHttpUrl);
            
            if (mySessionId != null) {
                this.sessionId = mySessionId;
            }
            
            EngineInterface realEngine;
            EngineInterface realEngineWithoutTimeout;
            if (this.sessionId == null) {
                // create new session and retrieve sessionId
                EngineInterface realTempEngine = MyProxyFactory.getProxy(EngineInterface.class, new URL(hessianHttpUrl), this.getClass().getClassLoader(), connectTimeout, useCookies, null, getxForwardedFor());

                // wrap engine in EngineProxyHelper which will retry
                EngineInterface tempEngine = wrapEngine(realTempEngine);

                // Can only make one call on this engine which is using an HTTP URL that has no sessionId.
                // Do not call again because it will have a different session on the server
                this.sessionId = tempEngine.getSessionID();
            }

            String hessianHttpUrlWithSessionId = hessianHttpUrl + ";jsessionid=" + new String(sessionId.key);

            // create proxy factory for URL with sessionId
            realEngine = MyProxyFactory.getProxy(EngineInterface.class, new URL(hessianHttpUrlWithSessionId), this.getClass().getClassLoader(), connectTimeout, useCookies, null, getxForwardedFor());
            realEngineWithoutTimeout = MyProxyFactory.getProxy(EngineInterface.class, new URL(hessianHttpUrlWithSessionId), this.getClass().getClassLoader(), 0, isUseCookies(), null, getxForwardedFor());
            
            // wrap engine in EngineProxyHelper which will retry
            engine = wrapEngine(realEngine);

            engineWithoutTimeout = wrapEngine(realEngineWithoutTimeout);

            // Call login() on this engine which is using a URL with sessionId in it
            // so the same session will be used for all calls to it
            if (login != null && password != null) {
                user = engine.login(login, password);
            }
            else
                user = engine.userGetMe();

            log.debug("Hessian connect done");

            return engine;
            
        } catch (RuntimeException ex) {
            log.debug(ex);
            log.info("Java home: " + System.getProperty("java.home"));
            throw ex;
        } catch (Throwable ex) {
                log.debug(ex);
            throw new RuntimeException(ex);
        }
    }

    /** Wrap an engine in an EngineProxyHelper and pass along some DVClientHelper attributes */
    public EngineInterface wrapEngine(final EngineInterface realEngine) {
        // wrap engine in EngineProxyHelper which will retry
        EngineProxyHelper e = new EngineProxyHelper() {
            protected EngineInterface getEngine() throws DException {
                return realEngine;
            }
        };
        if (listener != null) { ((EngineProxyHelper) e).setListener(listener); }
        e.setNumTries(this.getNumTries());
        e.setDelayAfterFirstTryInMilliseconds(this.getDelayAfterFirstTryInMilliseconds());
        e.setMultiplyDelayEachTimeAfterSecondTry(this.getMultiplyDelayEachTimeAfterSecondTry());

        return e;
    }

    public void disconnect() throws RemoteException {
        this.sessionId = null;
        if (engine != null) {
            engine.logout();
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
//        try {
//            connect(sessionId);
//        } catch (Exception ex) {
//            log.error("", ex);
//            throw new IOException(ex.getMessage());
//        }
    }

    public EngineInterface getEngine() {
        return engine;
    }
    
    public EngineInterface getEngineWithoutTimeout() {
        return engineWithoutTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    /** In ms */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
    
    public String getRmiServiceUrl() {
        return rmiServiceUrl;
    }
    
    public void setRmiServiceUrl(String rmiServiceUrl) {
        this.rmiServiceUrl = rmiServiceUrl;
    }
    
    public String getHttpProxyUrl() {
        return httpProxyUrl;
    }
    
    public void setHttpProxyUrl(String httpProxyUrl) {
        this.httpProxyUrl = httpProxyUrl;
    }
    
    public String getHttpCgiServiceUrl() {
        return httpCgiServiceUrl;
    }
    
    public void setHttpCgiServiceUrl(String httpCgiServiceUrl) {
        this.httpCgiServiceUrl = httpCgiServiceUrl;
    }
    
    public boolean isUseLocalTrustStore() {
        return useLocalTrustStore;
    }

    public void setUseLocalTrustStore(boolean useLocalTrustStore) {
        this.useLocalTrustStore = useLocalTrustStore;
    }

    public DUser getUser() {
        return user;
    }

    public DSessionID getSessionId() {
        return sessionId;
    }

    /**
     * @return the hessianHttpUrl
     */
    public String getHessianHttpUrl() {
        return hessianHttpUrl;
    }

    /**
     * @param hessianHttpUrl the hessianHttpUrl to set
     */
    public void setHessianHttpUrl(String hessianHttpUrl) {
        this.hessianHttpUrl = hessianHttpUrl;
    }

    /**
     * @return the listener
     */
    public EngineProxyHelperListener getListener() {
        return listener;
    }

    /**
     * @param listener the listener to set
     */
    public void setListener(EngineProxyHelperListener listener) {
        this.listener = listener;
    }

    /**
     * @return the numTries
     */
    public int getNumTries() {
        return numTries;
    }

    /**
     * @param numTries the numTries to set
     */
    public void setNumTries(int numTries) {
        this.numTries = numTries;
    }

    /**
     * @return the delayAfterFirstTryInMilliseconds
     */
    public int getDelayAfterFirstTryInMilliseconds() {
        return delayAfterFirstTryInMilliseconds;
    }

    /**
     * @param delayAfterFirstTryInMilliseconds the delayAfterFirstTryInMilliseconds to set
     */
    public void setDelayAfterFirstTryInMilliseconds(int delayAfterFirstTryInMilliseconds) {
        this.delayAfterFirstTryInMilliseconds = delayAfterFirstTryInMilliseconds;
    }

    /**
     * @return the multiplyDelayEachTimeAfterSecondTry
     */
    public float getMultiplyDelayEachTimeAfterSecondTry() {
        return multiplyDelayEachTimeAfterSecondTry;
    }

    /**
     * @param multiplyDelayEachTimeAfterSecondTry the multiplyDelayEachTimeAfterSecondTry to set
     */
    public void setMultiplyDelayEachTimeAfterSecondTry(float multiplyDelayEachTimeAfterSecondTry) {
        this.multiplyDelayEachTimeAfterSecondTry = multiplyDelayEachTimeAfterSecondTry;
    }

    /**
     * @return the useSystemProxies
     */
    public boolean isUseSystemProxies() {
        return useSystemProxies;
    }

    /**
     * @param useSystemProxies the useSystemProxies to set
     */
    public void setUseSystemProxies(boolean useSystemProxies) {
        this.useSystemProxies = useSystemProxies;
    }

    /**
     * @return the useCookies
     */
    public boolean isUseCookies() {
        return useCookies;
}

    /**
     * @param useCookies the useCookies to set
     */
//    public void setUseCookies(boolean useCookies) {
//        this.useCookies = useCookies;
//    }

    /**
     * @return the xForwardedFor
     */
    public ThreadLocal<String> getxForwardedFor() {
        return xForwardedFor;
    }

    /**
     * @param xForwardedFor the xForwardedFor to set
     */
    public void setxForwardedFor(ThreadLocal<String> xForwardedFor) {
        this.xForwardedFor = xForwardedFor;
    }

    /**
     * @param useCookies the useCookies to set
     */
    public void setUseCookies(boolean useCookies) {
        this.useCookies = useCookies;
    }
}
