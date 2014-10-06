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
 * Options of the IndexDeltaFolder task started from the command line.
 *
 * @author Lukas Vlcek (lvlcek@redhat.com)
 */
public class IndexDeltaFolderOptions {

	public static final String PATH_TO_DELTA_ARCHIVE = "-pathToDeltaArchive";
	public static final String NUMBER_OF_THREADS = "-numberOfThreads";
	public static final String SERVICE_HOST = "-serviceHost";
	public static final String SERVICE_PATH = "-servicePath";
	public static final String CONTENT_TYPE = "-contentType";
	public static final String USERNAME = "-username";
	public static final String PASSWORD = "-password";
	public static final String ACTIVE_MAIL_LISTS_CONF = "-activeMailListsConf";

	private CmdLineParser parser;

	private File pathToDeltaArchive;

	@Option(name = PATH_TO_DELTA_ARCHIVE, usage = "path to folder with delta mbox files")
	public void setPathToDeltaArchive(String input) throws CmdLineException {
		pathToDeltaArchive = new File(input);
		if (!pathToDeltaArchive.exists() || !pathToDeltaArchive.canRead()) {
			throw new CmdLineException(
					this.parser,
					new Throwable("Invalid " + PATH_TO_DELTA_ARCHIVE + " value: " +
							"file [" + input + "] does not exist or can not be read.")
			);
		}
	}

	@Option(name = "-delta", hidden = true)
	private String delta;

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

	private File activeMailListsConf;

	@Option(name = ACTIVE_MAIL_LISTS_CONF, usage = "conf file with list of mail lists to include into delta indexing (other files are still deleted!)")
	public void setActiveMailListsConf(String input) throws CmdLineException {
		activeMailListsConf = new File(input);
		if (!activeMailListsConf.exists() || !activeMailListsConf.canRead()) {
			throw new CmdLineException(
					this.parser,
					new Throwable("Invalid " + ACTIVE_MAIL_LISTS_CONF + " value: " +
							"file [" + input + "] does not exist or can not be read.")
			);
		}
	}

	public File getPathToDeltaArchive() {
		return this.pathToDeltaArchive;
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

	public File getActiveMailListsConf() {
		return this.activeMailListsConf;
	}

	public boolean isValid() {
		return (
			pathToDeltaArchive != null && pathToDeltaArchive.exists() && pathToDeltaArchive.canRead() &&
			numberOfThreads != null && numberOfThreads > 0 &&
			serviceHost != null &&
			servicePath != null && !servicePath.isEmpty() &&
			contentType != null && !contentType.isEmpty() &&
			username != null && !username.trim().isEmpty() &&
			password != null && !password.isEmpty() &&
			activeMailListsConf != null && activeMailListsConf.exists() && activeMailListsConf.canRead()
		);
	}

	public static void main(String[] args) {
		new IndexDeltaFolderOptions().parseArgs(args);
	}

	protected void parseArgs(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);
		this.parser = parser;

		try {
			parser.parseArgument(args);
		} catch( CmdLineException e ) {

			System.err.println(e.getMessage());
			System.err.println("java application.jar [options...] arguments...");
			parser.printUsage(System.err);
			System.err.println();
			System.err.println("  Example: java application.jar "+parser.printExample(ALL));

//			return;
		}
	}
}
