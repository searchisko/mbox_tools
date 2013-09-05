/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.mbox.json;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import org.searchisko.mbox.dto.Mail;
import org.searchisko.mbox.dto.MailAttachment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Convert {@link Mail} to JSON.
 *
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
public class Converter {

    /**
     * Convert mail to JSON. It is the same as calling #toJSON(mail, false).
     * @param mail
     * @return
     */
    public static String toJSON(Mail mail) {
        return toJSON(mail, false);
    }

    /**
     * Convert mail to JSON. It is the same as calling #toJSON(mail, metadata, false).
     * @param mail
     * @param metadata
     * @return
     */
    public static String toJSON(Mail mail, Map<String, String> metadata) {
        return toJSON(mail, metadata, false);
    }

    /**
     * Convert mail to JSON. It is the same as calling #toJSON(mail, null, false).
     * @param mail
     * @param prettyPrint
     * @return
     */
    private static String toJSON(Mail mail, boolean prettyPrint) {
        return toJSON(mail, null, prettyPrint);
    }

    /**
     * Convert mail to JSON. JSON output encoding is hardcoded to UTF-*.
     * Whatever is in metadata is added to or override result JSON.
     * @param mail
     * @param metadata
     * @param prettyPrint
     * @return
     */
    public static String toJSON(Mail mail, Map<String,String> metadata, boolean prettyPrint) {

        String json = null;

        OutputStream os = new ByteArrayOutputStream();
        JsonGenerator generator = null;

        try {

            generator = new JsonFactory().createJsonGenerator(os, JsonEncoding.UTF8);
            generator.writeStartObject();

            if (prettyPrint == true) {
                generator.useDefaultPrettyPrinter();
            }

            if (mail.author() != null && !hasKey(metadata, "author")) generator.writeStringField("author", mail.author());

            if (mail.to() != null && !hasKey(metadata, "to")) {
                generator.writeArrayFieldStart("to");
                for (String to : mail.to()) {
                    generator.writeString(to);
                }
                generator.writeEndArray();
            }

            if (mail.subject_original() != null && !hasKey(metadata, "subject_original")) generator.writeStringField("subject_original", mail.subject_original());
            if (mail.subject() != null && !hasKey(metadata, "subject")) generator.writeStringField("subject", mail.subject());
            if (mail.dateUTC() != null && !hasKey(metadata, "date")) generator.writeStringField("date", mail.dateUTC());
            if (mail.message_id_original() != null && !hasKey(metadata, "message_id_original")) generator.writeStringField("message_id_original", mail.message_id_original());
            if (mail.message_id() != null && !hasKey(metadata, "message_id")) generator.writeStringField("message_id", mail.message_id());

            if (mail.references() != null && !hasKey(metadata, "references")) {
                generator.writeArrayFieldStart("references");
                for (String ref : mail.references()) {
                    generator.writeString(ref);
                }
                generator.writeEndArray();
            }

            if (mail.in_reply_to() != null && !hasKey(metadata, "in_reply_to")) generator.writeStringField("in_reply_to", mail.in_reply_to());

            if (mail.message_snippet() != null && !hasKey(metadata, "message_snippet")) generator.writeStringField("message_snippet", mail.message_snippet());

            if (mail.first_text_message() != null && !hasKey(metadata, "first_text_message")) generator.writeStringField("first_text_message", mail.first_text_message());
            if (mail.first_text_message_without_quotes() != null && !hasKey(metadata, "first_text_message_without_quotes")) generator.writeStringField("first_text_message_without_quotes", mail.first_text_message_without_quotes());
            if (mail.first_html_message() != null && !hasKey(metadata, "first_html_message")) generator.writeStringField("first_html_message", mail.first_html_message());

            if (mail.text_messages() != null && mail.text_messages().length > 0 && !hasKey(metadata, "text_messages")) {
                generator.writeArrayFieldStart("text_messages");
                for (String part : mail.text_messages())
                {
                    generator.writeString(part);
                }
                generator.writeEndArray();
            }
            if (mail.text_messages_cnt() != null && !hasKey(metadata, "text_messages_cnt")) generator.writeNumberField("text_messages_cnt", mail.text_messages_cnt());

            if (mail.html_messages() != null && mail.html_messages().length > 0 && !hasKey(metadata, "html_messages")) {
                generator.writeArrayFieldStart("html_messages");
                for (String part : mail.html_messages())
                {
                    generator.writeString(part);
                }
                generator.writeEndArray();
            }
            if (mail.html_messages_cnt() != null && !hasKey(metadata, "html_messages_cnt")) generator.writeNumberField("html_messages_cnt", mail.html_messages_cnt());

            if (mail.message_attachments() != null && mail.message_attachments().length > 0 && !hasKey(metadata, "message_attachments")) {
                generator.writeArrayFieldStart("message_attachments");
                for (MailAttachment atchm : mail.message_attachments()) {
                    generator.writeStartObject();
                        generator.writeStringField("content_type",atchm.getContentType());
                        generator.writeStringField("filename", atchm.getFileName());
                        generator.writeStringField("content", atchm.getContent());
                    generator.writeEndObject();
                }
                generator.writeEndArray();
            }
            if (mail.message_attachments_cnt() != null && !hasKey(metadata, "message_attachments_cnt")) generator.writeNumberField("message_attachments_cnt", mail.message_attachments_cnt());

            if (metadata != null && !metadata.isEmpty()) {
                for (String key : metadata.keySet()) {
                    if (key.trim().length() > 0) {
                        generator.writeStringField(key, metadata.get(key));
                    }
                }
            }

            generator.writeEndObject();
            generator.close();

            json = os.toString();

        } catch (IOException e) {
            // TODO

        } finally {
            try {
                if (os != null) os.close();
            } catch (IOException e) {
                // ignore
            }
        }

        return json;
    }

    private static boolean hasKey(Map<String, String> metadata, String key) {
        if (metadata != null) {
            return metadata.containsKey(key);
        } else {
            return false;
        }
    }
}
