/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.mbox.parser;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.FieldParser;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.field.*;
import org.apache.james.mime4j.field.AddressListFieldImpl;
import org.apache.james.mime4j.field.DateTimeFieldLenientImpl;
import org.apache.james.mime4j.field.LenientFieldParser;
import org.apache.james.mime4j.field.MailboxListFieldImpl;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeConfig;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.searchisko.mbox.dto.Mail;
import org.searchisko.mbox.dto.MailAttachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Provides various static methods for parsing Messages.
 *
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
public class MessageParser {

    private static Logger log = LoggerFactory.getLogger(MessageParser.class);

    /**
     * We are interested in parsing only the following message header fields
     */
    public enum MessageHeader {
        FROM("from"), TO("to"), SUBJECT("subject"), DATE("date"), MESSAGE_ID("message-id"),
        REFERENCES("references"), IN_REPLY_TO("in-reply-to"), IGNORE("");

        private final String value;

        private MessageHeader(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public static MessageHeader getValue(String value) {
            try {
                return valueOf(value.replaceAll("-", "_").toUpperCase());
            } catch (Exception e) {
                return IGNORE;
            }
        }
    }

    public final static DateTimeFormatter defaultDatePrinter = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

    private static MessageBuilder messageBuilder;

    private MessageParser() {};

    /**
     * Lazy initialize MessageBuilder instance.
     * @return MessageBuilder instance
     * @throws MimeException
     */
    public static MessageBuilder getMessageBuilder() throws MimeException {
        MessageBuilder mb = messageBuilder;
        if (mb == null) {
            synchronized(MessageParser.class) {
                mb = messageBuilder;
                if (mb == null) {

                    MimeConfig config = new MimeConfig();
                    config.setMaxLineLen(10000);

                    FieldParser<MailboxListField> fromMailboxListParser = MailboxListFieldImpl.PARSER;
                    FieldParser<AddressListField> toAddressListParser = AddressListFieldImpl.PARSER;
                    FieldParser<DateTimeField> dateParser = DateTimeFieldLenientImpl.PARSER;

                    LenientFieldParser fieldParser = new LenientFieldParser();
                    fieldParser.setFieldParser(FieldName.TO, toAddressListParser);
                    fieldParser.setFieldParser(FieldName.REPLY_TO, toAddressListParser);
                    fieldParser.setFieldParser(FieldName.FROM, fromMailboxListParser);
                    fieldParser.setFieldParser(FieldName.RESENT_FROM, fromMailboxListParser);
                    fieldParser.setFieldParser(FieldName.DATE, dateParser);
                    fieldParser.setFieldParser(FieldName.RESENT_DATE, dateParser);

                    DefaultMessageBuilder _mb = new DefaultMessageBuilder();
                    _mb.setMimeEntityConfig(config);
                    _mb.setFieldParser(fieldParser);

                    mb = _mb;

                    messageBuilder = mb;
                }
            }
        }
        return mb;
    }

    /**
     * It is the same as calling #parse(message, null).
     *
     * @param message
     * @return
     */
    public static Mail parse(Message message) throws MessageParseException {
        return parse(message, null);
    }

    /**
     * Parse given Message into Mail.
     *
     * @param message
     * @param idsuffix  This value gets appended to the message-id.
     * @return
     */
    public static Mail parse(Message message, /*Map<String, String> data,*/ String idsuffix) throws MessageParseException {

        String author = null;
        String[] to = null;
        String subject_original = null;
        String subject = null;
        String date = null;
        String message_id_original = null;
        String message_id = null;
        String[] references = null;
        String in_reply_to = null;
        String message_snippet = null;
        String first_text_message = null;
        String first_text_message_without_quotes = null;
        String first_html_message = null;
        String[] text_messages = null;
        Integer text_messages_cnt = null;
        String[] html_messages = null;
        Integer html_messages_cnt = null;
        MailAttachment[] message_attachments = null;
        Integer message_attachments_cnt = null;

        Map<String, Field> headers = getMessageHeaders(message);

        boolean messageIdPresent = false;
        for (String fieldName : headers.keySet()) {

            Field f = headers.get(fieldName);
            switch (MessageHeader.getValue(f.getName())) {
                case FROM:
                    author = extractValue((MailboxListField)f);
                    break;
                case TO:
                    List<String> tos = new ArrayList<>();
                    for (String recipient : extractValue((AddressListField)f)) { tos.add(recipient); }
                    to = tos.toArray(new String[tos.size()]);
                    break;
                case SUBJECT:
                    subject_original = extractValue((UnstructuredField)f);
                    subject = normalizeSubject(subject_original);
                    break;
                case DATE:
                    Date d = extractValue((DateTimeField)f);
                    if (d != null) {
                        date = defaultDatePrinter.print(d.getTime());
                    } else {
                        String mid = headers.get(MessageHeader.MESSAGE_ID.toString()).getBody();
                        log.warn("Unable to parse header field '{}' for message-id: '{}'", f, mid);
                        throw new MessageParseException("Unable to parsed a date field. Skipping message ["+mid+"]");
                    }
                    break;
                case MESSAGE_ID:
                    String id = extractValue((UnstructuredField)f);
                    if (isNullOrEmpty(id)) {
                        throw new MessageParseException("Message-ID header is null or empty.");
                    }
                    message_id_original = id;
                    message_id = id;
                    if (!isNullOrEmpty(idsuffix)) {
                        message_id += idsuffix;
                    }
                    messageIdPresent = true;
                    break;
                case REFERENCES:
                    List<String> _references = new ArrayList<>();
                    for (String value : extractValue((UnstructuredField)f).trim().split("\\s+")) {
                        _references.add(value);
                    }
                    references = _references.toArray(new String[_references.size()]);
                    break;
                case IN_REPLY_TO:
                    in_reply_to = extractValue((UnstructuredField)f);
                    break;
            }

        }

        if (!messageIdPresent) throw new MessageParseException("Message-ID header not found.");

        MessageBodyParser.MailBodyContent content;
        try {
            content = MessageBodyParser.parse(message);
        } catch (IOException e) {
            throw new MessageParseException(e);
        }

        String snippet = "";
        if (content.getFirstTextContentWithoutQuotes() != null) {
            snippet = content.getFirstTextContentWithoutQuotes();
        } else if (content.getFirstTextContent() != null) {
            snippet = content.getFirstTextContent();
        } else if (content.getFirstHtmlContent() != null) {
            snippet =  Jsoup.parse(
                    Jsoup.clean(content.getFirstHtmlContent(), Whitelist.relaxed())
            ).text();
        } else {
            // TODO get text snippet from other fields
        }
        snippet = snippet.substring(0,(snippet.length() > 250 ? 250 : (snippet.length() > 0 ? snippet.length()-1 : 0))) // index can be -1 if length = 0 !!!
//                .replaceAll(">*", "")
                .replaceAll("^>From", "From")
                .replaceAll("\\s+", " ")
                .trim();
        message_snippet = snippet;

        first_text_message = content.getFirstTextContent();
        first_text_message_without_quotes = content.getFirstTextContentWithoutQuotes();
        first_html_message = content.getFirstHtmlContent();

        List<String> testMessages = new ArrayList<>();
        for (String part : content.getTextMessages())
        {
            testMessages.add(part);
        }
        text_messages = testMessages.toArray(new String[testMessages.size()]);
        text_messages_cnt = content.getTextMessages().size();

        List<String> htmlMessages = new ArrayList<>();
        for (String part : content.getHtmlMessages())
        {
            htmlMessages.add(part);
        }
        html_messages = htmlMessages.toArray(new String[htmlMessages.size()]);
        html_messages_cnt = content.getHtmlMessages().size();

        if (content.getAttachments().size() > 0) {
            message_attachments_cnt = content.getAttachments().size();
            message_attachments = content.getAttachments().toArray(new MailAttachment[message_attachments_cnt]);
        } else {
            message_attachments_cnt = 0;
        }

        return new Mail(
                message_id,
                message_id_original,
                to,
                subject_original,
                subject,
                author,
                date,
                in_reply_to,
                references,
                message_snippet,
                first_text_message,
                first_text_message_without_quotes,
                first_html_message,
                text_messages,
                text_messages_cnt,
                html_messages,
                html_messages_cnt,
                message_attachments,
                message_attachments_cnt
        );
    }

    /**
     * Extract only those header fields that are listed in MBoxParserUtil#headers
     *
     * @param message
     * @return
     */
    public static Map<String, Field> getMessageHeaders(Message message) {
        Map<String, Field> h = new HashMap<String, Field>();
        for (Field f : message.getHeader().getFields()) {
            if (MessageHeader.IGNORE.equals(MessageHeader.getValue(f.getName().toLowerCase()))) {
                continue;
            }
            h.put(f.getName().toLowerCase(), f);
        }
        return h;
    }

    /**
     *
     * @param field
     * @return
     */
    public static String extractValue(MailboxListField field) {
        if (field.getMailboxList() == null) {
            return field.getBody();
        }
        String name = field.getMailboxList().get(0).getName();
        String address = field.getMailboxList().get(0).getAddress();
        return prepareAddress(name, address);
    }

    /**
     *
     * @param field
     * @return
     */
    public static List<String> extractValue(AddressListField field) {
        List<String> items = new ArrayList<String>();
        for (Mailbox mb : field.getAddressList().flatten()) {
            items.add(prepareAddress(mb.getName(), mb.getAddress()));
        }
        return items;
    }

    /**
     *
     * @param field
     * @return
     */
    public static Date extractValue(DateTimeField field) {
        if (field.isValidField()) {
            return field.getDate();
        }
        if (field.getParseException() != null) {
            log.warn("The date field [{}] can not be parsed: {}", field, field.getParseException());
        }
        return null;
    }

    /**
     *
     * @param field
     * @return
     */
    public static String extractValue(UnstructuredField field) {
        return field.getValue();
    }

    /**
     * Normalize Subject value.
     *
     * @param subject
     * @return
     */
    public static String normalizeSubject(String subject) {
        String s = subject;
        if (s != null) {
            s = s.replaceAll("\\[.*?\\][^$]","")  // remove any brackets at the beginning except the one having end of line after the "]"
                    .replaceAll("^\\s*(-*\\s*[a-zA-Z]{2,3}:\\s*)*","") // remove all Re: Fw: Aw: ... etc including leading dash
                    .replaceAll("\\s+[a-zA-Z]{2,3}:","") // remove any additional Re: having white space prefix
                    .replaceAll("^\\s*-\\s*","") // remove any left dashes at the beginning
                    .replaceAll("\\s+"," ") // finally replace multi-white space with one white space
                    .trim();
        }
        return s;
    }


    private static String prepareAddress(String name, String address) {
        return name == null ? address : name + " <" + address + ">";
    }

    private static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().length() == 0;
    }

}
