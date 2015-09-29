/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.docuvantage.andhelper.hessian;

import com.caucho.hessian.client.HessianProxy;
import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.io.HessianRemoteObject;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Map;

/**
 * Custom Hessian ProxyFactory to allow us to use our own MyHessianProxy
 * @author Sarel
 */
public class MyProxyFactory extends HessianProxyFactory {

    private final boolean useCookies;
    private final Map<String, String> cookieStore;

    public MyProxyFactory(boolean useCookies, Map<String, String> cookieStore) {
        this.useCookies = useCookies;
        this.cookieStore = cookieStore;
    }

    public static <T> T getProxy(
            final Class<T> proxyClass,
            final URL url,
            final ClassLoader classLoader,
            final int readTimeoutInMs,
            final boolean useCookies,
            final Map<String, String> cookieStore,
            final ThreadLocal<String> xForwardedFor) {
        
        MyProxyFactory factory = new MyProxyFactory(useCookies, cookieStore);
        factory.setReadTimeout(readTimeoutInMs);
        factory.setChunkedPost(true);
        factory.setOverloadEnabled(true);
        factory.setHessian2Request(false);
        factory.setHessian2Reply(false);

        HessianProxy proxy = new MyHessianProxy(url, factory, useCookies, cookieStore, xForwardedFor);
        T o = (T) Proxy.newProxyInstance(
                classLoader,
                new Class[]{proxyClass, HessianRemoteObject.class},
                proxy);
        return o;
        
    }

}