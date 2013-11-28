/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.http.client;

import org.apache.http.Consts;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Http Client is used to handle Http communication with REST service (Searchisko).
 * It is thread-safe and it is recommended to share a single instance for many threads.
 *
 * @author Lukáš Vlček
 */
public class Client {

    private static Logger log = LoggerFactory.getLogger(Client.class);

    public static class ClientConfig {
        // defaults
        private String serviceHost = "http://localhost:8089";
        private String servicePath = "/v1/rest/content";
        private String contentType = "jbossorg_mailing_list";
        private int connectionsPerRoute = 2;
        private String username = "john.doe";
        private String password = "not_defined";

        public ClientConfig serviceHost(String url) { this.serviceHost = url; return this; }
        public ClientConfig servicePath(String path) { this.servicePath = path; return this; }
        public ClientConfig contentType(String type) { this.contentType = type; return this; }
        public ClientConfig connectionsPerRoute(int num) { this.connectionsPerRoute = num; return this; }
        public ClientConfig username(String username) { this.username = username; return this; }
        public ClientConfig password(String password) { this.password = password; return this; }
    }

    public static ClientConfig getConfig() {
        return new ClientConfig();
    }

    private final ClientConfig config;
    private CloseableHttpClient httpClient;
    private ResponseHandler responseHandler = new BasicResponseHandler();
    private static final ThreadLocal<HttpClientContext> httpClientContent = new ThreadLocal<HttpClientContext>() {
        @Override
        protected HttpClientContext initialValue() {
            return new HttpClientContext();
        }
    };

    private boolean closed = false;

    public Client() {
        this(new ClientConfig());
    }

    public Client(ClientConfig config) {
        this.config = config;

        if (log.isTraceEnabled()) {
            log.trace("Using HttpClient with the following configuration:");
            log.trace("  serviceHost: '{}'", config.serviceHost);
            log.trace("  servicePath: '{}'", config.servicePath);
            log.trace("  contentType: '{}'", config.contentType);
            log.trace("  connectionsPerRoute: '{}'", config.connectionsPerRoute);
            log.trace("  user: '{}'", config.username);
            log.trace("  password not empty: '{}'", config.password.length() > 0);
        }

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(this.config.connectionsPerRoute);
        cm.setDefaultMaxPerRoute(this.config.connectionsPerRoute);

        CredentialsProvider cp = new BasicCredentialsProvider();
        cp.setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(config.username, config.password)
        );

        httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(cp)
                .setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
                .build();

    }

    /**
     * Call this method to release allocated resourced (connection pool, ...) once you are done this the Client.
     * @throws IOException
     */
    public synchronized void close() throws IOException {
        if (!closed) {
            this.httpClient.close();
            this.closed = true;
        }
    }

    public Object post(final String messageBody, final String id) throws IOException {

        String idURLEncoded = URLEncoder.encode(id, StandardCharsets.UTF_8.name());

        String uri = config.serviceHost + config.servicePath + "/" + config.contentType+ "/" + idURLEncoded;

        log.trace("making POST to '{}'", uri);

        HttpPost httpPost = new HttpPost(uri);
        httpPost.setEntity(EntityBuilder.create()
                .setContentEncoding(Consts.UTF_8.displayName())
                .setContentType(ContentType.APPLICATION_JSON)
                .setText(messageBody)
                .build());

        Object response = httpClient.execute(httpPost, responseHandler, httpClientContent.get());

        return response;
    }



}
