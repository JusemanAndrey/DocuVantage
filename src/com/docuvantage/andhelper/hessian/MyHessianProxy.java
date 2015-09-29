/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.docuvantage.andhelper.hessian;

import com.caucho.hessian.client.HessianProxy;
import com.caucho.hessian.client.HessianProxyFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom HessianProxy to allow us to have simple cookie-handling
 * @author Sarel
 */
public class MyHessianProxy extends HessianProxy {

    Logger log = LoggerFactory.getLogger(this.getClass());
    Map<String, String> cookieStore;
    private final boolean useCookies;
    private final ThreadLocal<String> xForwardedFor;
    
    public MyHessianProxy(URL url, HessianProxyFactory factory, boolean useCookies, Map<String, String> cookieStore, ThreadLocal<String> xForwardedFor) {
        super(url, factory);
        this.useCookies = useCookies;
        this.cookieStore = cookieStore;
        this.xForwardedFor = xForwardedFor;
    }

    @Override
    protected void addRequestHeaders(URLConnection conn) {
        super.addRequestHeaders(conn);
        addXff(conn);
        if (useCookies) {
            setCookies(conn);
        }
    }

    public void addXff(URLConnection conn) {
        if (xForwardedFor!=null) {
            String xff = xForwardedFor.get();
            if (xff != null) {
                log.debug("Setting XFF to {}", xff);
                conn.setRequestProperty("X-Forwarded-For", xff);
            }
        }
    }

    private static final String SET_COOKIE_SEPARATOR="; ";
    private static final String COOKIE = "Cookie";

    public void setCookies(URLConnection conn) {
        if (cookieStore.size() == 0) {
            return;
        }

        log.debug("Sending cookies with request: {}", cookieStore);
	StringBuilder cookieString = new StringBuilder();
        Iterator<Entry<String, String>> cookies = cookieStore.entrySet().iterator();
        while (cookies.hasNext()) {
            Entry<String, String> cookie = cookies.next();
            cookieString.append(cookie.getKey());
            cookieString.append("=");
            cookieString.append(cookie.getValue());
            if (cookies.hasNext()) cookieString.append(SET_COOKIE_SEPARATOR);
	}
        conn.setRequestProperty(COOKIE, cookieString.toString());
    }
    
    /**
     * Save all cookies from server
     */
    @Override
    protected void parseResponseHeaders(URLConnection conn) {
        super.parseResponseHeaders(conn);
        if (useCookies) {
            Map<String, String> cookies = getCookies(conn);
            cookieStore.putAll(cookies);
            log.debug("Saved cookies from response: {}", cookies);
        }
    }

    private Map<String, String> getCookies(URLConnection conn) {
        Map<String, String> cookies = new HashMap();
        String headerName = null;
        for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
            if (headerName.equalsIgnoreCase(SET_COOKIE)) {
                StringTokenizer st = new StringTokenizer(conn.getHeaderField(i), COOKIE_VALUE_DELIMITER);

                // the specification dictates that the first name/value pair
                // in the string is the cookie name and value, so let's handle
                // them as a special case:
                if (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    String name = token.substring(0, token.indexOf(NAME_VALUE_SEPARATOR));
                    String value = token.substring(token.indexOf(NAME_VALUE_SEPARATOR) + 1, token.length());
                    // remove double quotes
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    // tihs is a simple implementation, we just ignore path and domain attributes, we always send cookies
                    if (!name.equalsIgnoreCase("path") && !name.equalsIgnoreCase("domain")) {
                        log.debug("Saved cookie from server: " + name + "=" + value);
                        cookies.put(name, value);
                    }
                }

                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    int startValue = token.indexOf(NAME_VALUE_SEPARATOR);
                    if (startValue > -1) {
                        String name = token.substring(0, startValue).trim().toLowerCase();
                        String value = token.substring(startValue + 1, token.length());
                        if (!name.equalsIgnoreCase("path") && !name.equalsIgnoreCase("domain")) {
                            log.debug("Saved cookie from server: " + name + "=" + value);
                            cookies.put(name, value);
                        }
                    }
                }
            }
        }
        return cookies;
    }

    private static final String COOKIE_VALUE_DELIMITER = ";";
    private static final String SET_COOKIE = "Set-Cookie";
    private static final char NAME_VALUE_SEPARATOR = '=';

}
