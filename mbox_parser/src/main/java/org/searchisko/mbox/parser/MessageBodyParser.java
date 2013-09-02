/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.mbox.parser;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.sun.xml.messaging.saaj.packaging.mime.MessagingException;
import com.sun.xml.messaging.saaj.packaging.mime.internet.MimeUtility;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.dom.*;
import org.apache.james.mime4j.message.BodyPart;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the content of parsed mail body (no headers).
 *
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
public class MessageBodyParser {

    /**
     * Supported message body subtypes.
     */
    public enum SupportedMultiPartType {
        ALTERNATIVE("alternative"), MIXED("mixed"), RELATED("related"), SIGNED("signed"),
        /*,TODO: APPLEDOUBLE("appledouble")*/
        UNKNOWN("");

        private final String value;

        private SupportedMultiPartType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public static SupportedMultiPartType getValue(String value) {
            try {
                return valueOf(value.replaceAll("-", "_").toUpperCase());
            } catch (Exception e) {
                return UNKNOWN;
            }
        }
    }


    /**
     * Represents parsed message attachment.
     */
    public class MailAttachment {

        private String contentType;
        private String fileName;
//            private String contentInputStream;

        public String getContentType() { return this.contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }

        public String getFileName() { return this.fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
    }

    /**
     * Represents parsed message body content.
     */
    public static class MailBodyContent {

        private String messageId;
        private String firstTextContent;
        private String firstTextContentWithoutQuotes;
        private String firstHtmlContent;
        private List<String> textMessages = new ArrayList<>();
        private List<String> htmlMessages = new ArrayList<>();
        private List<MailAttachment> attachments = new ArrayList<>();

        public void setMessageId(String id) { this.messageId = id; }
        public String getMessageId() { return this.messageId; }

        public void setFirstTextContent(String content) { this.firstTextContent = content; }
        public String getFirstTextContent() { return this.firstTextContent; }

        public void setFirstTextContentWithoutQuotes(String content) { this.firstTextContentWithoutQuotes = content; }
        public String getFirstTextContentWithoutQuotes() { return this.firstTextContentWithoutQuotes; }

        public void setFirstHtmlContent(String content) { this.firstHtmlContent = content; }
        public String getFirstHtmlContent() { return this.firstHtmlContent; }

        public void setTextMessages(List<String> textMessages) { this.textMessages = textMessages; }
        public List<String> getTextMessages() { return this.textMessages; }

        public void setHtmlMessages(List<String> htmlMessages) { this.htmlMessages = htmlMessages; }
        public List<String> getHtmlMessages() { return this.htmlMessages; }

        public List<MailAttachment> getAttachments() { return this.attachments; }
    }

    /**
     *
     * @param message
     * @return
     */
    public static MailBodyContent parse(Entity message) throws MessageParseException, IOException {

        MailBodyContent content = new MailBodyContent();
        return parse(content, message);
    }

    private static MailBodyContent parse(MailBodyContent content, Entity message) throws MessageParseException, IOException {

        Body body = message.getBody();
        String mimeType = message.getMimeType().toLowerCase();
        String contentTransferEncoding = message.getContentTransferEncoding();
        String charset = message.getCharset();
        String filename = message.getFilename();

        if ("x-gbk".equalsIgnoreCase(charset)) {
            // hardcoded fix for java.io.UnsupportedEncodingException: x-gbk
//            log.warn("Unsupported encoding found: 'x-gbk', using 'gbk' instead.");
            charset = "gbk";
        }
        /*
        if (log.isTraceEnabled()) {
            log.trace("parsing Entity, mimeType: {}, filename: {}", mimeType, filename);
            log.trace("contentTransferEncoding: {}", contentTransferEncoding);
            log.trace("charset: {}", charset);
        }
        */

        if (body instanceof Multipart) {
            parseMultipartBody(content, (Multipart)body);
        } else
        if (body instanceof TextBody) {
            parseTextBody(content, (TextBody)body, mimeType, contentTransferEncoding, charset, filename);
        } else
        if (body instanceof BinaryBody) {
            parseBinaryBody(content, (BinaryBody)body, mimeType, contentTransferEncoding, charset, filename);
        } else
        if (body instanceof Message) {
            parseMessage(content, (Message)body);
        } else {
            throw new MessageParseException("Message body of type [" + body.getClass().getSimpleName() + "] is not supported.");
        }

        return content;
    }

    private static MailBodyContent parseMultipartBody(MailBodyContent content, Multipart body) throws MessageParseException, IOException {

        String subType = body.getSubType().toLowerCase();
        switch(SupportedMultiPartType.getValue(subType)) {
            case UNKNOWN:
                throw new MessageParseException(subType + " is unsupported body multipart subtype.");
            case ALTERNATIVE:
                BodyPart thePart = null;
                for (Entity part : body.getBodyParts()) {
                    if (part.getMimeType().toLowerCase().equals("text/plain")) {
                        thePart = (BodyPart)part;
                    }
                }
                if (thePart == null) {
                    for (Entity part : body.getBodyParts()) {
                        if (part.getMimeType().toLowerCase().equals("text/html")) {
                            thePart = (BodyPart)part;
                        }
                    }
                }
                if (thePart != null)
                    return parseTextBody(content, (TextBody) thePart.getBody(), thePart.getMimeType(), thePart.getContentTransferEncoding(), thePart.getCharset(), thePart.getFilename());
                else {
                    for (Entity part : body.getBodyParts()) {
                        if (part.getBody() instanceof Entity) {
                            parse(content, (Entity)part.getBody());
                        } else
                        if (part.getBody() instanceof Multipart) {
                            parseMultipartBody(content, (Multipart)part.getBody());
                        } else {
//                            log.warn("Body of type [{}] not supported! Ignoring.", part.getBody().getClass().getCanonicalName());
                        }
                    }
                }
                break;
            default:
                for (Entity part : body.getBodyParts()) {
                    parse(content, part);
                }
                break;
        }

        return content;
    }

    private static MailBodyContent parseMessage(MailBodyContent content, Message message) throws MessageParseException, IOException {

        String mimeType = message.getMimeType().toLowerCase();
        String contentTransferEncoding = message.getContentTransferEncoding();
        String charset = message.getCharset();
        String filename = message.getFilename();

        Body body = message.getBody();
        if (body instanceof Multipart) {
            parseMultipartBody(content, (Multipart)body);
        } else
        if (body instanceof TextBody) {
            parseTextBody(content, (TextBody)body, mimeType, contentTransferEncoding, charset, filename);
        } else
        if (body instanceof BinaryBody) {
            parseBinaryBody(content, (BinaryBody)body, mimeType, contentTransferEncoding, charset, filename);
        } else
        if (body instanceof Message) {
            parseMessage(content, (Message)body);
        } else {
            throw new MessageParseException("Body of type [" + body.getClass().getSimpleName() + "] is not supported.");
        }
        return content;
    }

    private static MailBodyContent parseTextBody(MailBodyContent bodyContent, TextBody body, String mimeType, String contentTransferEncoding, String charset, String filename) throws IOException {

        /* if (log.isTraceEnabled()) {
            log.trace("parsing text body, mimeType: {}, contentTransferEncoding: {}, charset: {}, filename: {}",
                    new Object[]{mimeType, contentTransferEncoding, charset, filename});
        }*/

        if (filename != null) {
//            addAttachment(bodyContent, body, mimeType, filename);
        } else {

            String content = null;
            InputStream output = null;

            if (contentTransferEncoding != null && contentTransferEncoding.length() > 0) {
                /* if (log.isTraceEnabled()) {
                    log.trace("decoding: {}", contentTransferEncoding);
                    log.trace("charset: {}", charset);
                }*/
                try {
                    // com.sun.xml.messaging.saaj.packaging.mime.util.BASE64DecoderStream.decode() seems to be buggy
                    if ("base64".equalsIgnoreCase(contentTransferEncoding)) {
                        output = body.getInputStream();
                    } else {
                        output = MimeUtility.decode(body.getInputStream(), contentTransferEncoding.toLowerCase());
                    }

                    if (charset.toUpperCase().startsWith("ISO-8859") || charset.toUpperCase().startsWith("ISO8859")) {
                        CharsetMatch detectedCharset = detectCharset(output);
                        if (detectedCharset != null) {
                            int conf = detectedCharset.getConfidence();
                            if (conf >= 80) {
                                charset = detectedCharset.getName();
                            }
//                            log this !!!
//                            System.out.println("--- detected charset ---");
//                            System.out.println(charset + ", conf = " + conf);
                        }
                    }

                    StringWriter writer = new StringWriter();
                    IOUtils.copy(output, writer, charset);
                    content = writer.toString();
                } catch (MessagingException e) {
//                    log.error("Error decoding transfer coding.", e);
                    content = getTextBodyContent(body.getReader()).replaceAll("=\n","");
                }
            } else {
                content = getTextBodyContent(body.getReader()).replaceAll("=\n", "");
            }

            if (mimeType.equals("text/plain")) {
                content = content
//                        .replaceAll(">","&gt;")
                        .replaceAll("<", "&lt;")
                        .replaceAll("^>From","From");
                if (bodyContent.getFirstTextContent() == null && bodyContent.getFirstHtmlContent() == null) {
                    bodyContent.setFirstTextContentWithoutQuotes(filterOutQuotedContent(content));
                    if (bodyContent.getFirstTextContentWithoutQuotes().length() > 0) {
                        bodyContent.setFirstTextContentWithoutQuotes(bodyContent.getFirstTextContentWithoutQuotes().replaceAll(">","&gt;"));
                    }
                    bodyContent.setFirstTextContent(content.replaceAll(">","&gt;"));

                } else {
                    bodyContent.getTextMessages().add(content.replaceAll(">","&gt;"));
                }
            } else
            if (mimeType.equals("text/html")) {
                // TODO clean possible html tags?
                if (bodyContent.getFirstTextContent() == null && bodyContent.getFirstHtmlContent() == null) {
                    bodyContent.setFirstHtmlContent(content);
                } else {
                    bodyContent.getHtmlMessages().add(content);
                }
            } else {
                // TODO just in case we are missing something (?)
                // text/richtext, text/xml, text/x-vhdl, text/x-vcard, text/x-patch, text/x-log, text/css, text/java, text/rtf, text/x-diff, text/x-java
                bodyContent.getTextMessages().add(content);
            }

        }
        return bodyContent;
    }

    private static CharsetMatch detectCharset(InputStream inputStream) throws IOException {
        CharsetDetector cd = new CharsetDetector();
        cd.setText(inputStream);
        cd.enableInputFilter(true);
        return cd.detect();
    }

    private static MailBodyContent parseBinaryBody(MailBodyContent content, BinaryBody body, String mimeType, String contentTransferEncoding, String charset, String filename) {
        return null;
    }

    private static String filterOutQuotedContent(String content) {
        StringBuilder noQuotes = new StringBuilder();
        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.length() > 0 && !line.startsWith(">")) {
                noQuotes.append(line).append(" ");
            }
        }
        String result = noQuotes.toString().trim();
        return result;
    }

    private static String getTextBodyContent(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = reader.read()) != -1) {
            sb.append((char) c);
        }
        reader.close();
        return sb.toString();
    }

}
