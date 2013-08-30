/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.mbox.dto;

/**
 * DTO object representing parsed mail.
 * It is supposed to be converted to JSON down the road before indexing.
 *
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
public class Mail {

    private final String message_id;
    private final String message_id_original;

    private final String[] to;
    private final String subject_original;
    private final String subject;
    private final String author;
    private final String date;

    private final String in_reply_to;
    private final String[] references;

    private final String message_snippet;
    private final String first_text_message;
    private final String first_text_message_without_quotes;
    private final String first_html_message;

    private final String[] text_messages;
    private final Integer text_messages_cnt;

    private final String[] html_messages;
    private final Integer html_messages_cnt;

    private final String[] message_attachments;
    private final Integer message_attachments_cnt;

    public Mail(final String messageId, final String message_id_original, final String[] to, final String subject_original,
                    final String subject, final String author, final String date, final String in_reply_to,
                    final String[] references, final String message_snippet, final String first_text_message,
                    final String first_text_message_without_quotes, final String first_html_message, final String[] text_messages,
                    final Integer text_messages_cnt, final String[] html_messages, final Integer html_messages_cnt,
                    final String[] message_attachments, final Integer message_attachments_cnt) {

        this.message_id = messageId;
        this.message_id_original = message_id_original;
        this.to = to;
        this.subject_original = subject_original;
        this.subject = subject;
        this.author = author;
        this.date = date;
        this.in_reply_to = in_reply_to;
        this.references = references;
        this.message_snippet = message_snippet;
        this.first_text_message = first_text_message;
        this.first_text_message_without_quotes = first_text_message_without_quotes;
        this.first_html_message = first_html_message;
        this.text_messages = text_messages;
        this.text_messages_cnt = text_messages_cnt;
        this.html_messages = html_messages;
        this.html_messages_cnt = html_messages_cnt;
        this.message_attachments = message_attachments;
        this.message_attachments_cnt = message_attachments_cnt;
    }

    public String message_id() { return message_id; }
    public String message_id_original() { return message_id_original; }

    public String[] to() { return to; }
    public String subject_original() { return subject_original; }
    public String subject() { return subject; }
    public String author() { return author; }
    public String dateUTC() { return date; }
    public String in_reply_to() { return in_reply_to; }
    public String[] references() { return references; }
    public String message_snippet() { return message_snippet; }
    public String first_text_message() { return first_text_message; }
    public String first_text_message_without_quotes() { return first_text_message_without_quotes; }
    public String first_html_message() { return first_html_message; }
    public String[] text_messages() { return text_messages; }
    public Integer text_messages_cnt() { return text_messages_cnt; }
    public String[] html_messages() { return html_messages; }
    public Integer html_messages_cnt() { return html_messages_cnt; }
    public String[] message_attachments() { return message_attachments; }
    public Integer message_attachments_cnt() { return message_attachments_cnt; }

}
