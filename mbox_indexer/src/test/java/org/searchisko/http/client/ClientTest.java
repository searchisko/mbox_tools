/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.http.client;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.searchisko.http.client.Client.getConfig;

/**
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
@RunWith(JUnit4.class)
public class ClientTest {

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(8089);

    @Test
    public void shouldNotFail() throws IOException, URISyntaxException {

        stubFor(post(urlMatching("/service/ct/[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"foo\":\"bar\"}")));

        Client client = new Client(getConfig().serviceHost(new URI("http://localhost:8089")).servicePath("/service").contentType("ct"));

        client.post("{\"foo\":\"1\"}", "1");
        client.post("{\"foo\":\"2\"}", "2");
        client.post("{\"foo\":\"3\"}", "3");
        client.post("{\"foo\":\"4\"}", "4");
        client.post("{\"foo\":\"5\"}", "5");

        client.close();
    }
}
