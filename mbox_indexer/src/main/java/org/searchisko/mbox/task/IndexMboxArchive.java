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
import org.searchisko.preprocessor.HTMLStripUtil;
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
import static org.searchisko.mbox.parser.MessageParser.getMessageHeaders;

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
 * <p/>
 * The <code>numberOffset</code> is used if numbering of individual messages in the public archive does not start
 * from 0. This can be typically result of Mailman admin mistake during archive rebuilding or similar issue.
 * Note this is an optional parameter but we need to provide it if we need to provide parameters following this. In such
 * case we can use value 0.
 * <p/>
 * The <code>excludeMessageIdListPath</code> is used if we need to exclude specific messages from processing. This is
 * an optional parameter.
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
	 * @param messageString raw message as a string. Can be null.
	 * @param message parsed message. Can be null.
	 * @param mailListName
	 * @param mailListCategory
	 * @param cnt order # of this message within the single cumulative mbox archive file
	 * @return
	 */
	private static Runnable prepareTask(final String messageString, final Message message, final String mailListName, final String mailListCategory, final long cnt) {
		return new Runnable() {
			@Override
			public void run() {
				// 1. Convert mail to JSON representation with added metadata.
				// 2. Send mail to the server, using blocking operation.
				long taskId = taskCount.incrementAndGet();
				log.debug("starting task [{}]", taskId);
				if (messageString == null && message == null) {
					log.error("Missing message source. Either raw message string or parsed message must be provided. Exit task {}", taskId);
					return;
				}
				String messageId = null;
				try {
					Message msg;
					if (messageString != null) {
						msg = mb.parseMessage(new ByteArrayInputStream(messageString.getBytes()));
					} else {
					    msg = message;
					}

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
						sysContent = HTMLStripUtil.stripHTML(mail.first_html_message());
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
		// try to get from fs
		File file = new File(path);
		if (file.exists()) { return file; }
		// try to get from classpath
		URL url = IndexMboxArchive.class.getClassLoader().getResource(path);
		log.trace("file url: {}", url);
		String filesPathAndName = url.getPath();
		log.info("trying to get file {}", filesPathAndName);
		return new File(filesPathAndName);
	}

	public static InputStream getInputStream(String filePath) throws FileNotFoundException {
		InputStream is = null;
		// try to get from fs
		File f = new File(filePath);
		if (f.exists()) is = new FileInputStream(f);
		// try to get from classpath
		if (is == null) {
			is = IndexMboxArchive.class.getClassLoader().getResourceAsStream(filePath);
		}
		return is;
	}


	private static void processMessageBuffer(ThreadPoolExecutor executor, Properties excludeMessageIds, StringBuilder messageSB, String mailListName, String mailListCategory, int offset) throws IOException, MimeException {
		if (messageSB.length() > 0) {
			String messageString = messageSB.toString();
			Message message = null;
			boolean filterOut = false;
			if (excludeMessageIds != null && !excludeMessageIds.isEmpty()) {
				message = mb.parseMessage(new ByteArrayInputStream(messageString.getBytes()));
				String messageId = getMessageHeaders(message).get(MessageParser.MessageHeader.MESSAGE_ID.toString()).getBody();
				filterOut = excludeMessageIds.containsKey(messageId) ? true : false;
				if (filterOut) log.info("skipping message [{}]", messageId);
			}
			if (!filterOut) {
				executor.submit(prepareTask(messageString, message, mailListName, mailListCategory, messageCount+offset));
				messageCount++;
			}
			messageSB.setLength(0);
		}
	}

	/**
	 * @param args see Class JavaDoc
	 */
	public static void main(String[] args) {

		log.info("Job started.");

		if (args.length < 9) {
			StringBuilder sb = new StringBuilder();
			sb.append("Parameters: ");
			sb.append("mboxFilePath numberOfThreads serviceHost servicePath contentType username password mailListName mailListCategory [numberOffset] [excludeMessageIdListPath]\n\n");
			sb.append("mboxFilePath - path to mbox file\n");
			sb.append("numberOfThreads - max threads used for processing tasks\n");
			sb.append("serviceHost - service host URL\n");
			sb.append("servicePath - service path\n");
			sb.append("contentType - Searchisko provider sys_content_type\n");
			sb.append("username - Searchisko provider username (plaintext)\n");
			sb.append("password - Searchisko provider password (plaintext)\n");
			sb.append("mailListName - name of mail_list, it is needed for document URL creation\n");
			sb.append("mailListCategory - mail_list category [dev,users,announce,...etc]\n");
			sb.append("[numberOffset] - public URL numbering offset\n");
			sb.append("[excludeMessageIdListPath] - path to properties file containing list of Message-Ids to skip");
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
		String excludeMessageIdListPath = null;
		if (args.length > 10) {
			excludeMessageIdListPath = args[10].trim();
		}

		if (log.isDebugEnabled()) {
			log.debug("CL parameters:");
			log.debug("----------------------------------");
			log.debug("mboxFilePath: {}", mboxFilePath);
			log.debug("numberOfThreads: {} (avail_cores: {})", new Object[]{numberOfThreads, Runtime.getRuntime().availableProcessors()});
			log.debug("mailListName: {}", mailListName);
			log.debug("mailListCategory: {}", mailListCategory);
			log.debug("offset: {}", offset);
			log.debug("excludeMessageIdListPath: {}", excludeMessageIdListPath);
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

		FileReader mboxFileReader = null;
		FileReader excludedIdsFileReader = null;
		BufferedReader br = null;

		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				numberOfThreads,
				numberOfThreads,
				3, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(numberOfThreads, true),
				new ThreadPoolExecutor.CallerRunsPolicy());

		try {
			mb = getMessageBuilder();

			log.info("Processing file {}", mboxFilePath);
			mboxFileReader = new FileReader(getFile(mboxFilePath));
			Properties excludeMessageIds = new Properties();
			// Note that if there are any Message-Ids to be excluded then we have to parse all messages
			// in the main thread before they are handed to another thread for processing.
			if (excludeMessageIdListPath != null) {
				excludeMessageIds.load(getInputStream(excludeMessageIdListPath));
			}
			br = new BufferedReader(mboxFileReader);

			String line;
			StringBuilder messageSB = new StringBuilder();
			String separator = System.getProperty("line.separator");

			Date start = new Date();

			while ((line = br.readLine()) != null) {
				if (line.startsWith("From ")) {
					processMessageBuffer(executor, excludeMessageIds, messageSB, mailListName, mailListCategory, offset);
				}
				messageSB.append(line).append(separator);
			}
			// process last message
			processMessageBuffer(executor, excludeMessageIds, messageSB, mailListName, mailListCategory, offset);

			executor.shutdown();
			executor.awaitTermination(10L, TimeUnit.SECONDS);

			Date end = new Date();

			log.info("Processed {} mails in {} millis", messageCount, end.getTime() - start.getTime());
			log.debug("Tasks created: {}", taskCount.get());

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

			if (mboxFileReader != null) {
				try {
					mboxFileReader.close();
				} catch (IOException e) {
					e.printStackTrace();
					log.error("Error closing mboxFileReader", e);
				}
			}

			if (excludedIdsFileReader != null) {
				try {
					excludedIdsFileReader.close();
				} catch (IOException e) {
					e.printStackTrace();
					log.error("Error closing excludedIdsFileReader", e);
				}
			}

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
