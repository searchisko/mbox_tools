/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.mbox.task;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.searchisko.http.client.Client;
import org.searchisko.mbox.dto.Mail;
import org.searchisko.mbox.json.Converter;
import org.searchisko.mbox.parser.MessageParser;
import org.searchisko.mbox.util.ContentType;
import org.searchisko.mbox.util.DirUtil;
import org.searchisko.mbox.util.StringUtil;
import org.searchisko.preprocessor.HTMLStripUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.searchisko.http.client.Client.getConfig;
import static org.searchisko.mbox.parser.MessageParser.getMessageBuilder;

/**
 * Given path to a folder <code>pathToDeltaArchive</code> we read list of files in it (no recursion).
 * We assume that files have name in special format (the name is base64 encoded and contains URL link of individual
 * entry in public Mailman archive).
 * <p/>
 * Next we filter list of files and exclude all that do not contain allowed project name in its name. List of allowed
 * projects is provided as a property file located at <code>activeMailListsConf<code/> path. All excluded files are
 * <b>deleted</b> from fs immediately.
 * <p/>
 * Next we process remaining files in parallel. We are using ThreadPoolExecutor with BlockingQueue to
 * throttle number of parallel tasks in order not to exhaust all system resources.
 * <p/>
 * Each thread is responsible for parsing one file, parsing it to a message and converting it to JSON and then sending
 * it to Searchisko for indexing via HttpClient. When HttpClient sends Http request it blocks the thread until the
 * response is received or until timeout.
 * <p/>
 * Client can specify number of parallel threads. Note the `main` thread is not included in that number but it can
 * be used to handle the task as well. Now, the underlying HttpClient is using connection pool which is configured
 * to allow for needed number of concurrent connections. In other words <code>numberOfThreads</code> of value `N` can
 * result up to `N+1` active parallel connections to target <code>host</code> (contrary, a typical HttpClient connection
 * pool does not allow for more then 2 parallel connection per <code>host</code>). So be sure your target service is
 * able to handle this number of incoming connections.
 * <p/>
 * Each remaining file is <b>deleted</b> immediately after it is processed successfully.
 *
 * @author Lukáš Vlček (lvlcek@redhat.com)
 *
 * @see StringUtil
 * @see ThreadPoolExecutor
 * @see ArrayBlockingQueue
 * @see {https://today.java.net/pub/a/today/2008/10/23/creating-a-notifying-blocking-thread-pool-executor.html}
 * @see {http://www.javacodegeeks.com/2011/12/using-threadpoolexecutor-to-parallelize.html}
 */
public class IndexDeltaFolder {

	private static Logger log = LoggerFactory.getLogger(IndexDeltaFolder.class);
	private static MessageBuilder mb;
	private static Client httpClient;

	private static Runnable prepareTask(final File file) {
		return new Runnable() {
			@Override
			public void run() {
				String mailURL = StringUtil.decodeFilenameSafe(file.getName());
				// Note: StringUtil.getInfo() can fire unchecked exception but as long as
				// #filter() is called before #index() we should not get file with invalid name
				StringUtil.URLInfo info = StringUtil.getInfo(file.getName());
				String messageId;
				try {

					Map<String, String> metadata = new HashMap<>();
					metadata.put("sys_view_url", mailURL);
					metadata.put("project", info.getProject());
					metadata.put("mail_list_category", info.getListType());

					Message message = mb.parseMessage(new FileInputStream(file));
					Mail mail = MessageParser.parse(message);
					messageId = mail.message_id(); // "sys_content_id"

					String sysContent = mail.first_text_message_without_quotes();
					String sysContentContentType = ContentType.TEXT_PLAIN;
					if (sysContent == null || sysContent.trim().isEmpty()) {
						sysContent = mail.first_text_message();
					}
					if (sysContent == null || sysContent.trim().isEmpty()) {
						sysContent = HTMLStripUtil.stripHTML(mail.first_html_message());
					}
					metadata.put("sys_content", sysContent);
					metadata.put("sys_content_content-type", sysContentContentType);

					metadata.put("sys_description", mail.message_snippet());
					String messageJSON = Converter.toJSON(mail, metadata);

					Object response = httpClient.post(messageJSON, messageId);

					log.trace("{}", response);

					if (!file.delete()) {
						log.error("Could not delete file after successful processing {}, does it exist?", file.getName(), file.exists());
					}

				} catch (Throwable e) {
					log.error("Error processing mail [{}]", mailURL);
					log.debug("Error details", e);
				}
			}
		};
	}

	/**
	 * Calls #read(deltaArchivePath, 2000)
	 *
	 * @param deltaArchivePath
	 * @return
	 * @see #read(File, long)
	 */
	public static File[] read(File deltaArchivePath) {
		return read(deltaArchivePath, 2000);
	}

	/**
	 * Reader files found at given path. It ignores all files that have been "lastModified" before 2 seconds or less.
	 *
	 * @param deltaArchivePath
	 * @param fileAge
	 * @return
	 */
	public static File[] read(File deltaArchivePath, long fileAge) {

		List<File> filesToProcess = new ArrayList<>();
		log.info("Reading folder {}", deltaArchivePath);

		try {
			File[] files = DirUtil.listFiles(deltaArchivePath);
			log.info("Checking {} files", files.length);
			for (File file : files) {
				// If file can not be deleted then do not process it,
				// that would lead to repetitive processing of the same file.
				if (file.canWrite()) {
					// Process only files older then 2 seconds. This is to make sure that we do not process files that
					// are currently being created and thus are incomplete. (Not sure if Python file creation operation
					// is atomic).
					if (System.currentTimeMillis() - file.lastModified() > fileAge) {
						filesToProcess.add(file);
					}
				}
			}
		} catch (FileNotFoundException e) {
			log.error("Could not read resource: {}", deltaArchivePath, e);
		}
		return filesToProcess.toArray(new File[filesToProcess.size()]);
	}

	/**
	 * Filter out all files that do not belong to any of provided mail list collection.
	 * Files that are filtered out are also immediately deleted from the filesystem.
	 *
	 * @param filesToProcess
	 * @param activeMailLists
	 * @return
	 */
	public static File[] filter(File[] filesToProcess, Collection<String> activeMailLists) {

		List<File> filesFiltered = new ArrayList<>();
		int countOfOriginalFiles = filesToProcess.length;

		for (File file : filesToProcess) {
			// decode
			StringUtil.URLInfo info = null;
			try {
				info = StringUtil.getInfo(file.getName());
			} catch (Throwable e) {
				log.error("Can not extract info from file name [{}]. Skipping this file", file.getName());
			}

			if (info != null && info.getProject() != null) {
				// get lookup key
				String key = info.getProject();
				if (info.getListType() != null) {
					key += "-" + info.getListType();
				}

				// if found among active projects
				if (activeMailLists.contains(key)) {
					filesFiltered.add(file);
				} else {
					// just delete it
					if (!file.delete()) {
						// may be the file has been already deleted by some other process...
						log.error("Could not delete file {}, does it exist? {}", file.getName(), file.exists());
					}
				}
			} else {
				// this should probably not happen
				log.error("Could not parse project name from file name [{}]. Skipping this file.", file.getName());
			}
		}
		log.info("Filtered {} files out in total", countOfOriginalFiles - filesFiltered.size());
		return filesFiltered.toArray(new File[filesFiltered.size()]);
	}

	/**
	 * This method is not thread safe.
	 *
	 * @param filesToProcess
	 * @param executor
	 */
	public static void index(File[] filesToProcess, ThreadPoolExecutor executor) {
		log.info("Starting to index {} files", filesToProcess.length);
		if (filesToProcess.length > 0) {
			try {
				mb = getMessageBuilder(); // not thread safe
			} catch (MimeException e) {
				log.error("Could not get MessageBuilder", e);
				throw new RuntimeException(e);
			}

			for (File file : filesToProcess) {
				executor.submit(prepareTask(file));
			}
		}
		log.info("Done.");
	}

	public static void main(String[] args) {

		log.info("Job started.");

		IndexDeltaFolderOptions options = new IndexDeltaFolderOptions();
		options.parseArgs(args);
		if (options.isValid()) {

			if (log.isDebugEnabled()) {
				log.debug("CL parameters:");
				log.debug("----------------------------------");
				log.debug("pathToDeltaArchive: {}", options.getPathToDeltaArchive());
				log.debug("numberOfThreads: {} (avail_cores: {})", new Object[]{options.getNumberOfThreads(), Runtime.getRuntime().availableProcessors()});
				log.debug("activeMailListsConf: {}", options.getActiveMailListsConf());
				log.debug("----------------------------------");
			}

			if (options.getNumberOfThreads() < 1) {
				throw new IllegalArgumentException("numberOfThreads must be at least 1");
			}

			httpClient = new Client(getConfig()
					.connectionsPerRoute(options.getNumberOfThreads() + 1) // because task can be executed in the `main` thread as well
					.serviceHost(options.getServiceHost())
					.servicePath(options.getServicePath())
					.contentType(options.getContentType())
					.username(options.getUsername())
					.password(options.getPassword())
			);

			ThreadPoolExecutor executor = new ThreadPoolExecutor(
					options.getNumberOfThreads(),
					options.getNumberOfThreads(),
					3, TimeUnit.SECONDS,
					new ArrayBlockingQueue<Runnable>(options.getNumberOfThreads(), true),
					new ThreadPoolExecutor.CallerRunsPolicy());

			// load properties conf
			Properties prop = new Properties();

			try {

				prop.load(new FileInputStream(options.getActiveMailListsConf()));
				Collection<String> activeMailLists = prop.stringPropertyNames();

				File[] files = read(options.getPathToDeltaArchive());
				files = filter(files, activeMailLists);
				index(files, executor);

				executor.shutdown();
				executor.awaitTermination(10L, TimeUnit.SECONDS);

			} catch (IOException e) {
				log.error("Error occurred", e);
			} catch (InterruptedException e) {
				log.error("Unexpected exception", e);
			} finally {
				// try to force executor termination if needed
				if (!executor.isTerminated()) {
					log.warn("Executor not terminated, forcing termination.");
					executor.shutdownNow();
					Thread.currentThread().interrupt();
				}

				log.info("Job finished.");
			}
		}
	}
}
