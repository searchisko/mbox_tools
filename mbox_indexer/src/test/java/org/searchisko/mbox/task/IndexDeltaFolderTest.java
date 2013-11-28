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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

/**
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
@RunWith(JUnit4.class)
public class IndexDeltaFolderTest {

	@ClassRule
	public static WireMockClassRule wireMockRule = new WireMockClassRule(8089);

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

}
