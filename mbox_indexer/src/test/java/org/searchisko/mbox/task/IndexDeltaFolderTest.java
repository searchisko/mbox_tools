/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.mbox.task;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
@RunWith(JUnit4.class)
public class IndexDeltaFolderTest {

	private static Logger log = LoggerFactory.getLogger(IndexDeltaFolderTest.class);
	private static final String path = "deltaTask";
	private static final String tmpDir = "folder_copy";

	@ClassRule
	public static WireMockClassRule wireMockRule = new WireMockClassRule(8089);

	@Before
	public void prepareMasterCopy() {
		assertTrue("Preparation of tmp files failed!", prepareTmpContent(path, "folder_master", tmpDir));
	}

	@After
	public void deleteMasterCopy() throws URISyntaxException {
		File copy = new File(ClassLoader.getSystemResource(path + File.separator + tmpDir).toURI());
		if (copy.exists()) {
			assertTrue("Cleanup failed!", FileUtils.deleteQuietly(copy));
		}
	}

	@Test
	public void invalidArgsShouldPrintHelp() throws IOException {

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

		// no args
		IndexDeltaFolder.main(new String[]{""});
		assertThat(sb.toString(), containsString("Parameters: "));

		// not enough args
		IndexDeltaFolder.main(new String[]{"1","2","3","4","5","6","7"});
		assertThat(sb.toString(), containsString("Parameters: "));

		System.setOut(origOut);
	}

//	@Test
	public void shouldPass() {

		stubFor(post(urlMatching("/service1/ct/.+"))
				.willReturn(aResponse()
						.withStatus(200)
						.withFixedDelay(200) // simulate a small delay
						.withHeader("Content-Type", "application/json")
						.withBody("{\"foo\":\"bar\"}")));

		String tmpPath = path+File.separator+tmpDir;
		int numberOfThreads = 2;
		String serviceHost = "http://localhost:8089";
		String servicePath = "/service1";
		String contentType = "ct";
		String username = "john.doe";
		String password = "not_defined";
		String activeMailListsConf = "deltaTask"+File.separator+"allowedLists.properties";

		IndexDeltaFolder.main(new String[]{tmpPath, Integer.toString(numberOfThreads),
				serviceHost, servicePath, contentType, username, password,
				activeMailListsConf
		});

//		verify(0, postRequestedFor(urlMatching("/service1/ct/.+")));

	}

	/**
	 * Prepare temporary directory for test. The directory will be deleted on JVM exit.
	 * The idea is to have some "golden" master ad make a copy of it for tests because tests will modify and delete it.
	 *
	 * @param path
	 * @param masterFolder
	 * @param copyFolder
	 * @return
	 */
	private boolean prepareTmpContent(String path, String masterFolder, String copyFolder) {
		try {
			File p = new File(ClassLoader.getSystemResource(path).toURI());
			if (!p.exists()) { throw new IOException("path " + path + " not found"); }

			File masterDir = new File(p, masterFolder);
			// if master does not exists we have nothing to test (considered fail)
			if (!masterDir.exists()) { throw new IOException("masterDir " + masterDir + " not found"); }

			File copyDir = new File(p, copyFolder);
			// delete copy if exists
			if (copyDir.exists()) {
				if (!FileUtils.deleteQuietly(copyDir)) {
					throw new IOException("can not delete copyDir " + copyDir);
				}
			}
			// create
			if (!copyDir.mkdir()) { throw new IOException("can not create copyDir " + copyDir); }

			// copy from maser to copy
			FileUtils.copyDirectory(masterDir, copyDir);

		} catch (Exception e) {
			log.error("Unexpected exception:", e);
			return false;
		}
		return true;
	}

}
