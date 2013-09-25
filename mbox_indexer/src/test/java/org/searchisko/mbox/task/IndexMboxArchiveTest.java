/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.mbox.task;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.OutputStream;
import java.io.PrintStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

/**
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
@RunWith(JUnit4.class)
public class IndexMboxArchiveTest {

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(8089);

    @Test
    public void invalidArgsShouldPrintHelp() {

        final StringBuilder sb = new StringBuilder();

        class Interceptor extends PrintStream
        {
            public Interceptor(OutputStream out)
            {
                super(out, true);
            }
            @Override
            public void print(String s)
            {
                sb.append(s);
//                super.print(s);
            }
        }

        PrintStream origOut = System.out;
        PrintStream interceptor = new Interceptor(origOut);
        System.setOut(interceptor);

        IndexMboxArchive.main(new String[]{""});

        assertThat(sb.toString(), containsString("Invalid parameters!"));

        System.setOut(origOut);
    }

    /**
     * Test demonstrates that BlockingQueue works as expected and the log shows that
     * some tasks can be passed back to [main] thread if queue is full while rest of
     * the tasks are created and processed later.
     */
    @Test
    public void shouldPass() {

        stubFor(post(urlMatching("/service1/ct/[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(200) // simulate a small delay
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"foo\":\"bar\"}")));

        String path = "mboxArchive/simple6.mbox";
        int numberOfThreads = 2;
        String serviceHost = "http://localhost:8089";
        String servicePath = "/service1";
        String contentType = "ct";
        String username = "john.doe";
        String password = "not_defined";
        String mailListName = "aa";
        String mailListCategory = "bb";

        IndexMboxArchive.main(new String[]{path, Integer.toString(numberOfThreads),
                serviceHost, servicePath, contentType, username, password,
                mailListName, mailListCategory});

        verify(6, postRequestedFor(urlMatching("/service1/ct/[0-9]+")));
    }

    @Test
    public void hugeFileShouldPass449() {

        stubFor(post(urlMatching("/service2/ct/[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(10) // simulate a small delay
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"foo\":\"bar\"}")));

        String path = "mboxArchive/lucene-java-user-201301.mbox";
        int numberOfThreads = 10;
        String serviceHost = "http://localhost:8089";
        String servicePath = "/service2";
        String contentType = "ct";
        String username = "john.doe";
        String password = "not_defined";
        String mailListName = "aa";
        String mailListCategory = "bb";

        IndexMboxArchive.main(new String[]{path, Integer.toString(numberOfThreads),
                serviceHost, servicePath, contentType, username, password,
                mailListName, mailListCategory});

        // according to mailman stats there should be 449 mails in January 2013
        // http://mail-archives.apache.org/mod_mbox/lucene-java-user/201301.mbox/thread
        verify(449, postRequestedFor(urlMatching("/service2/ct/[0-9]+")));

    }

    @Test
    public void hugeFileShouldPass771() {

        stubFor(post(urlMatching("/service3/ct/[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(10) // simulate a small delay
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"foo\":\"bar\"}")));

        String path = "mboxArchive/lucene-java-user-200703.mbox";
        int numberOfThreads = 10;
        String serviceHost = "http://localhost:8089";
        String servicePath = "/service3";
        String contentType = "ct";
        String username = "john.doe";
        String password = "not_defined";
        String mailListName = "aa";
        String mailListCategory = "bb";

        IndexMboxArchive.main(new String[]{path, Integer.toString(numberOfThreads),
                serviceHost, servicePath, contentType, username, password,
                mailListName, mailListCategory});

        // according to mailman stats there should be 770 mails in March 2007
        // http://mail-archives.apache.org/mod_mbox/lucene-java-user/200703.mbox/thread
        // but we detect 771 !
        // however, until MIME4J-232 is fixed we parse successfully only 769 messages
        verify(769, postRequestedFor(urlMatching("/service3/ct/[0-9]+")));

    }
}
