/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.mbox.task;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.net.URI;

import static org.kohsuke.args4j.ExampleMode.ALL;

/**
 * Options of the IndexMboxArchive task started from the command line.
 *
 * @author Lukas Vlcek (lvlcek@redhat.com)
 */
public class IndexMboxArchiveOptions {

	public static final String MBOX_FILE_PATH = "-mboxFilePath";
	public static final String NUMBER_OF_THREADS = "-numberOfThreads";
	public static final String SERVICE_HOST = "-serviceHost";
	public static final String SERVICE_PATH = "-servicePath";
	public static final String CONTENT_TYPE = "-contentType";
	public static final String USERNAME = "-username";
	public static final String PASSWORD = "-password";
	public static final String MAIL_LIST_NAME = "-mailListName";
	public static final String MAIL_LIST_CATEGORY = "-mailListCategory";
	public static final String NUMBER_OFFSET = "-numberOffset";
	public static final String EXCLUDE_MESSAGE_ID_LIST_PATH = "-excludeMessageIdListPath";

	private CmdLineParser parser;

	private File mboxFilePath;

	@Option(name = MBOX_FILE_PATH, usage = "path to mbox file", metaVar = "<path>")
	public void setMboxFilePath(String input) throws CmdLineException {
		mboxFilePath = new File(input);
		if (!mboxFilePath.exists() || !mboxFilePath.canRead()) {
			throw new CmdLineException(
					this.parser,
					new Throwable("Invalid " + MBOX_FILE_PATH + " value: " +
							"file [" + input + "] does not exist or can not be read.")
			);
		}
	}

	@Option(name = NUMBER_OF_THREADS, usage = "max threads used for processing tasks")
	private Integer numberOfThreads;

	@Option(name = SERVICE_HOST, usage = "service host URL")
	private URI serviceHost;

	@Option(name = SERVICE_PATH, usage = "service path")
	private String servicePath;

	@Option(name = CONTENT_TYPE, usage = "Searchisko provider sys_content_type")
	private String contentType;

	@Option(name = USERNAME, usage = "Searchisko provider username (plaintext)")
	private String username;

	@Option(name = PASSWORD, usage = "Searchisko provider password (plaintext)")
	private String password;

	@Option(name = MAIL_LIST_NAME, usage = "name of mail_list, it is needed for document URL creation")
	private String mailListName;

	@Option(name = MAIL_LIST_CATEGORY, usage = "mail_list category [dev,users,announce,...etc]")
	private String mailListCategory;

	@Option(name = NUMBER_OFFSET, usage = "[optional] public URL numbering offset")
	private Integer numberOffset;

	private File excludeMessageIdListPath;

	@Option(name = EXCLUDE_MESSAGE_ID_LIST_PATH, usage = "[optional] path to properties file containing list of Message-Ids to skip", metaVar = "<path>")
	public void setExcludeMessageIdListPath(String input) throws CmdLineException {
		excludeMessageIdListPath = new File(input);
		if (!excludeMessageIdListPath.exists() || !excludeMessageIdListPath.canRead()) {
			throw new CmdLineException(
					this.parser,
					new Throwable("Invalid " + EXCLUDE_MESSAGE_ID_LIST_PATH + " value: " +
							"file [" + input + "] does not exist or can not be read.")
			);
		}
	}

	public File getMboxFilePath() {
		return this.mboxFilePath;
	}

	public Integer getNumberOfThreads() {
		return this.numberOfThreads;
	}

	public URI getServiceHost() {
		return this.serviceHost;
	}

	public String getServicePath() {
		return this.servicePath;
	}

	public String getContentType() {
		return this.contentType;
	}

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		return this.password;
	}

	public String getMailListName() {
		return this.mailListName;
	}

	public String getMailListCategory() {
		return this.mailListCategory;
	}

	public Integer getNumberOffset() {
		return this.numberOffset;
	}

	public File getExcludeMessageIdListPath() {
		return this.excludeMessageIdListPath;
	}

	public boolean isValid() {
		return (
			mboxFilePath != null && mboxFilePath.exists() && mboxFilePath.canRead() &&
			numberOfThreads != null && numberOfThreads > 0 &&
			serviceHost != null &&
			servicePath != null && !servicePath.isEmpty() &&
			contentType != null && !contentType.isEmpty() &&
			username != null && !username.trim().isEmpty() &&
			password != null && !password.isEmpty() &&
			mailListName != null && !mailListName.trim().isEmpty() &&
			mailListCategory != null && !mailListCategory.trim().isEmpty()
		);
	}

	public static void main(String[] args) {
		new IndexMboxArchiveOptions().parseArgs(args);
	}

	protected void parseArgs(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);
		this.parser = parser;

		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {

			System.err.println(e.getMessage());
			System.err.println("java application.jar [options...] arguments...");
			parser.printUsage(System.err);
			System.err.println();
			System.err.println("  Example: java application.jar " + parser.printExample(ALL));

//			return;
		}
	}
}
