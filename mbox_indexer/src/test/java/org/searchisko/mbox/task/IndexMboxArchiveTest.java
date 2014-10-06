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

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

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

        PrintStream origOut = System.err;
        PrintStream interceptor = new Interceptor(origOut);
        System.setErr(interceptor);

		// no args
        IndexMboxArchive.main(new String[]{""});
        assertThat(sb.toString(), containsString("Example: "));

		// not enough args
		IndexMboxArchive.main(new String[]{"1","2","3","4","5","6","7","8"});
		assertThat(sb.toString(), containsString("Example: "));

        System.setErr(origOut);
    }

    /**
     * Test demonstrates that BlockingQueue works as expected and the log shows that
     * some tasks can be passed back to [main] thread if queue is full while rest of
     * the tasks are created and processed later.
     */
    @Test
    public void shouldPass() throws URISyntaxException {

        stubFor(post(urlMatching("/service1/ct/[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(200) // simulate a small delay
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"foo\":\"bar\"}")));

        String path = "mboxArchive"+File.separator+"simple6.mbox";
		String fileClassPath = getClass().getClassLoader().getResource(path).getFile();
        int numberOfThreads = 2;
        String serviceHost = "http://localhost:8089";
        String servicePath = "/service1";
        String contentType = "ct";
        String username = "john.doe";
        String password = "not_defined";
        String mailListName = "aa";
        String mailListCategory = "bb";

        IndexMboxArchive.main(new String[]{
				IndexMboxArchiveOptions.MBOX_FILE_PATH, fileClassPath,
				IndexMboxArchiveOptions.NUMBER_OF_THREADS, Integer.toString(numberOfThreads),
				IndexMboxArchiveOptions.SERVICE_HOST, serviceHost,
				IndexMboxArchiveOptions.SERVICE_PATH, servicePath,
				IndexMboxArchiveOptions.CONTENT_TYPE, contentType,
				IndexMboxArchiveOptions.USERNAME, username,
				IndexMboxArchiveOptions.PASSWORD, password,
				IndexMboxArchiveOptions.MAIL_LIST_NAME, mailListName,
				IndexMboxArchiveOptions.MAIL_LIST_CATEGORY, mailListCategory});

        verify(6, postRequestedFor(urlMatching("/service1/ct/[0-9]+")));
    }

    @Test
    public void hugeFileShouldPass449() {

        stubFor(post(urlMatching("/service2/ct/.+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(10) // simulate a small delay
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"foo\":\"bar\"}")));

        String path = "mboxArchive"+File.separator+"lucene-java-user-201301.mbox";
		String fileClassPath = getClass().getClassLoader().getResource(path).getFile();
        int numberOfThreads = 10;
        String serviceHost = "http://localhost:8089";
        String servicePath = "/service2";
        String contentType = "ct";
        String username = "john.doe";
        String password = "not_defined";
        String mailListName = "aa";
        String mailListCategory = "bb";

        IndexMboxArchive.main(new String[]{
				IndexMboxArchiveOptions.MBOX_FILE_PATH, fileClassPath,
				IndexMboxArchiveOptions.NUMBER_OF_THREADS, Integer.toString(numberOfThreads),
				IndexMboxArchiveOptions.SERVICE_HOST, serviceHost,
				IndexMboxArchiveOptions.SERVICE_PATH, servicePath,
				IndexMboxArchiveOptions.CONTENT_TYPE, contentType,
				IndexMboxArchiveOptions.USERNAME, username,
				IndexMboxArchiveOptions.PASSWORD, password,
				IndexMboxArchiveOptions.MAIL_LIST_NAME, mailListName,
				IndexMboxArchiveOptions.MAIL_LIST_CATEGORY, mailListCategory});

        // according to mailman stats there should be 449 mails in January 2013
        // http://mail-archives.apache.org/mod_mbox/lucene-java-user/201301.mbox/thread
        verify(449, postRequestedFor(urlMatching("/service2/ct/.+")));

    }

    @Test
    public void hugeFileShouldPass771() {

        stubFor(post(urlMatching("/service3/ct/.+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(10) // simulate a small delay
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"foo\":\"bar\"}")));

        String path = "mboxArchive"+File.separator+"lucene-java-user-200703.mbox";
		String fileClassPath = getClass().getClassLoader().getResource(path).getFile();
        int numberOfThreads = 10;
        String serviceHost = "http://localhost:8089";
        String servicePath = "/service3";
        String contentType = "ct";
        String username = "john.doe";
        String password = "not_defined";
        String mailListName = "aa";
        String mailListCategory = "bb";

        IndexMboxArchive.main(new String[]{
				IndexMboxArchiveOptions.MBOX_FILE_PATH, fileClassPath,
				IndexMboxArchiveOptions.NUMBER_OF_THREADS, Integer.toString(numberOfThreads),
				IndexMboxArchiveOptions.SERVICE_HOST, serviceHost,
				IndexMboxArchiveOptions.SERVICE_PATH, servicePath,
				IndexMboxArchiveOptions.CONTENT_TYPE, contentType,
				IndexMboxArchiveOptions.USERNAME, username,
				IndexMboxArchiveOptions.PASSWORD, password,
				IndexMboxArchiveOptions.MAIL_LIST_NAME, mailListName,
				IndexMboxArchiveOptions.MAIL_LIST_CATEGORY, mailListCategory});

        // according to mailman stats there should be 770 mails in March 2007
        // http://mail-archives.apache.org/mod_mbox/lucene-java-user/200703.mbox/thread
        // but we detect 771 !
        // however, until MIME4J-232 is fixed we parse successfully only 769 messages
        verify(769, postRequestedFor(urlMatching("/service3/ct/.+")));

    }

	/**
	 * Parsing three messages but only one is indexed. The rest is filtered out.
	 */
	@Test
	public void shouldFilterOutMessageIds() {

		stubFor(post(urlMatching("/service4/ct/.+"))
				.willReturn(aResponse()
						.withStatus(200)
						.withFixedDelay(200) // simulate a small delay
						.withHeader("Content-Type", "application/json")
						.withBody("{\"foo\":\"bar\"}")));

		String path = "mboxArchive"+File.separator+"simpleFilter.mbox";
		String fileClassPath = getClass().getClassLoader().getResource(path).getFile();
		String filteredIdPath = "mboxArchive"+File.separator+"filteredMessageId.properties";
		String filteredIdClassPath = getClass().getClassLoader().getResource(filteredIdPath).getFile();
		int numberOfThreads = 2;
		String serviceHost = "http://localhost:8089";
		String servicePath = "/service4";
		String contentType = "ct";
		String username = "john.doe";
		String password = "not_defined";
		String mailListName = "aa";
		String mailListCategory = "bb";

		IndexMboxArchive.main(new String[]{
				IndexMboxArchiveOptions.MBOX_FILE_PATH, fileClassPath,
				IndexMboxArchiveOptions.NUMBER_OF_THREADS, Integer.toString(numberOfThreads),
				IndexMboxArchiveOptions.SERVICE_HOST, serviceHost,
				IndexMboxArchiveOptions.SERVICE_PATH, servicePath,
				IndexMboxArchiveOptions.CONTENT_TYPE, contentType,
				IndexMboxArchiveOptions.USERNAME, username,
				IndexMboxArchiveOptions.PASSWORD, password,
				IndexMboxArchiveOptions.MAIL_LIST_NAME, mailListName,
				IndexMboxArchiveOptions.MAIL_LIST_CATEGORY, mailListCategory,
				IndexMboxArchiveOptions.NUMBER_OFFSET, "0",
				IndexMboxArchiveOptions.EXCLUDE_MESSAGE_ID_LIST_PATH, filteredIdClassPath});

		verify(1, postRequestedFor(urlMatching("/service4/ct/.+")));
	}
}
