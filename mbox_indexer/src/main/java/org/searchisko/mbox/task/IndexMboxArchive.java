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
import org.searchisko.mbox.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.searchisko.http.client.Client.getConfig;
import static org.searchisko.mbox.parser.MessageParser.getMessageBuilder;

/**
 * Given a single mbox archive file (can be huge) we read it line by line and every time a complete message is red we
 * pass that message for processing to parallel thread. Yet, we are using ThreadPoolExecutor with BlockingQueue to
 * throttle number of parallel tasks in order not to exhaust all system resources.
 * <p/>
 * Each thread is responsible for parsing the mail message, converting it to JSON and then sending it to Searchisko
 * for indexing via HttpClient. When HttpClient sends Http request it blocks the thread until the response is received
 * or until timeout.
 * <p/>
 * Client can specify number of parallel threads. Note the `main` thread is not included in that number but it can
 * be used to handle the task as well. Now, the underlying HttpClient is using connection pool which is configured
 * to allow for needed number of concurrent connections. In other words <code>numberOfThreads</code> of value `N` can
 * result up to `N+1` active parallel connections to target <code>host</code> (contrary, a typical HttpClient connection
 * pool does not allow for more then 2 parallel connection per <code>host</code>). So be sure your target service is
 * able to handle this number of incoming connections.
 *
 * @author Lukáš Vlček (lvlcek@redhat.com)
 *
 * @see ThreadPoolExecutor
 * @see ArrayBlockingQueue
 * @see {https://today.java.net/pub/a/today/2008/10/23/creating-a-notifying-blocking-thread-pool-executor.html}
 * @see {http://www.javacodegeeks.com/2011/12/using-threadpoolexecutor-to-parallelize.html}
 */
public class IndexMboxArchive {

	private static Logger log = LoggerFactory.getLogger(IndexMboxArchive.class);
	private static MessageBuilder mb;
	private static Client httpClient;
	private static AtomicLong taskCount = new AtomicLong();
	private static long messageCount = 0;

	/**
	 *
	 * @param message
	 * @param cnt order # of this message within the single cumulative mbox archive file
	 * @return
	 */
	private static Runnable prepareTask(final String message, final String mailListName, final String mailListCategory, final long cnt) {
		return new Runnable() {
			@Override
			public void run() {
				// 1. Convert mail to JSON representation with added metadata.
				// 2. Send mail to the server, using blocking operation.
				long taskId = taskCount.incrementAndGet();
				log.debug("starting task [{}]", taskId);
				String messageId = null;
				try {
					Message msg = mb.parseMessage(new ByteArrayInputStream(message.getBytes()));

					String document_url = getDocumentUrl(msg, mailListName, cnt);

					// add missing metadata
					Map<String, String> metadata = new HashMap<>();
					metadata.put("sys_url_view", document_url);
					metadata.put("project", StringUtil.getProjectName(mailListName, mailListCategory));
					metadata.put("mail_list_category", mailListCategory);

					Mail mail = MessageParser.parse(msg);
					messageId = mail.message_id(); // "sys_content_id"

					String sysContent = mail.first_text_message_without_quotes();
					String sysContentContentType = ContentType.TEXT_PLAIN;
					if (sysContent == null || sysContent.trim().isEmpty()) {
						sysContent = mail.first_text_message();
					}
					if (sysContent == null || sysContent.trim().isEmpty()) {
						sysContent = mail.first_html_message();
						sysContentContentType = ContentType.TEXT_HTML;
					}
					metadata.put("sys_content", sysContent);
					metadata.put("sys_content_content-type", sysContentContentType);

					metadata.put("sys_description", mail.message_snippet());
					String messageJSON = Converter.toJSON(mail, metadata);

					Object response = httpClient.post(messageJSON, messageId);

					log.trace("{}", response);

				} catch (Exception e) {
					log.warn("Error processing message {} in task [{}], caused: {}", new Object[]{messageId, taskId, e.getMessage()});
				}

			}
		};
	}

	/**
	 * Construct public URL for given message.
	 * TODO: this needs to be configurable going forward.
	 * @param message
	 * @param mailListName
	 * @param cnt order # of this message within mbox file (single cumulative file)
	 * @return
	 */
	protected static String getDocumentUrl(final Message message, final String mailListName, final long cnt) {
		// our Mailman is in specific times zone, this has impact on how it constructs URLs
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMMMM", Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("EST"));
		return "http://lists.jboss.org/pipermail/"+mailListName+"/"+sdf.format(message.getDate())+"/"+String.format("%06d",cnt)+".html";
	}

	public static File getFile(String path) {
		URL url = IndexMboxArchive.class.getClassLoader().getResource(path);
		log.trace("file url: {}", url);
		String filesPathAndName = url.getPath();
		log.info("trying to get file {}", filesPathAndName);
		return new File(filesPathAndName);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if (args.length < 9) {
			StringBuilder sb = new StringBuilder();
			sb.append("Parameters: ");
			sb.append("mboxFilePath numberOfThreads serviceHost servicePath contentType username password mailListName mailListCategory [numberOffset]\n\n");
			sb.append("mboxFilePath - path to mbox file\n");
			sb.append("numberOfThreads - max threads used for processing tasks\n");
			sb.append("serviceHost - service host URL\n");
			sb.append("servicePath - service path\n");
			sb.append("contentType - Searchisko provider sys_content_type\n");
			sb.append("username - Searchisko provider username (plaintext)\n");
			sb.append("password - Searchisko provider password (plaintext)\n");
			sb.append("mailListName - name of mail_list, it is needed for document URL creation\n");
			sb.append("mailListCategory - mail_list category [dev,users,announce,...etc]\n");
			sb.append("numberOffset - public URL numbering offset\n");
			System.out.println(sb.toString());
			return;
		}

		String mboxFilePath = args[0].trim();
		int numberOfThreads = Integer.parseInt(args[1].trim());

		String serviceHost = args[2].trim();
		String servicePath = args[3].trim();
		String contentType = args[4].trim();
		String username = args[5].trim();
		String password = args[6].trim();

		String mailListName = args[7].trim();
		String mailListCategory = args[8].trim();
		int offset = 0;
		if (args.length > 9) {
			offset = Integer.parseInt(args[9].trim());
		}

		if (log.isDebugEnabled()) {
			log.debug("CL parameters:");
			log.debug("----------------------------------");
			log.debug("mboxFilePath: {}", mboxFilePath);
			log.debug("numberOfThreads: {} (avail_cores: {})", new Object[]{numberOfThreads, Runtime.getRuntime().availableProcessors()});
			log.debug("mailListName: {}", mailListName);
			log.debug("mailListCategory: {}", mailListCategory);
			log.debug("offset: {}", offset);
			log.debug("----------------------------------");
		}

		if (numberOfThreads < 1) {
			throw new IllegalArgumentException("numberOfThreads must be at least 1");
		}

		httpClient = new Client(getConfig()
				.connectionsPerRoute(numberOfThreads + 1) // because task can be executed in the `main` thread as well
				.serviceHost(serviceHost)
				.servicePath(servicePath)
				.contentType(contentType)
				.username(username)
				.password(password)
		);

		FileReader fr = null;
		BufferedReader br = null;

		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				numberOfThreads,
				numberOfThreads,
				3, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(numberOfThreads, true),
				new ThreadPoolExecutor.CallerRunsPolicy());

		try {
			mb = getMessageBuilder();

			fr = new FileReader(getFile(mboxFilePath));
			br = new BufferedReader(fr);

			String line;
			StringBuilder messageSB = new StringBuilder();
			String separator = System.getProperty("line.separator");

			Date start = new Date();

			while ((line = br.readLine()) != null) {
				if (line.startsWith("From ")) {
					if (messageSB.length() > 0) {
						executor.submit(prepareTask(messageSB.toString(), mailListName, mailListCategory, messageCount+offset));
						messageCount++;
						messageSB.setLength(0);
					}
				}
				messageSB.append(line).append(separator);
			}
			// process last message
			if (messageSB.length() > 0) {
				executor.submit(prepareTask(messageSB.toString(), mailListName, mailListCategory, messageCount+offset));
				messageCount++;
				messageSB.setLength(0);
			}

			executor.shutdown();
			executor.awaitTermination(10L, TimeUnit.SECONDS);

			Date end = new Date();

			if (log.isInfoEnabled()) {
				log.info("Processed {} mails in {} millis", messageCount, end.getTime() - start.getTime());
				log.info("Tasks created: {}", taskCount.get());
			}

		} catch (IOException e) {
			log.error("Error occurred", e);
		} catch (MimeException e) {
			log.error("Unable to instantiate MessageBuilder", e);
		} catch (/*InterruptedException | */ Throwable e) {
			log.error("Unexpected exception", e);
		} finally {

			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
					log.error("Error closing BufferedReader", e);
				}
			}

			if (fr != null) {
				try {
					fr.close();
				} catch (IOException e) {
					e.printStackTrace();
					log.error("Error closing FileReader", e);
				}
			}

			// try to force executor termination if needed
			if (!executor.isTerminated()) {
				log.warn("Executor not terminated, forcing termination.");
				executor.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}

	}
}
