package com.app.security;
import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

public class SslConfiguration {
    @Value("${http.client.ssl.trust-store}")
    private Resource keyStore;
    @Value("${http.client.ssl.trust-store-password}")
    private String keyStorePassword;

    @Bean
    CloseableHttpClient restTemplate() throws Exception {
        SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(
                        keyStore.getURL(),
                        keyStorePassword.toCharArray()
                ).build();
        SSLConnectionSocketFactory socketFactory = 
        		new SSLConnectionSocketFactory(
        		       sslContext);
        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(socketFactory)
                .build();
        return HttpClients.custom()
                .setConnectionManager(cm)
                .evictExpiredConnections()
                .build();
    }
}