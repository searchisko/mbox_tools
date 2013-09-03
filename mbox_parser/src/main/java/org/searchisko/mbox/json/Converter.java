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
     * Convert mail to JSON.
     * @param mail
     * @param prettyPrint
     * @return
     */
    public static String toJSON(Mail mail, boolean prettyPrint) {

        String json = null;

        OutputStream os = new ByteArrayOutputStream();
        JsonGenerator generator = null;

        try {

            generator = new JsonFactory().createJsonGenerator(os, JsonEncoding.UTF8);
            generator.writeStartObject();

            if (prettyPrint == true) {
                generator.useDefaultPrettyPrinter();
            }

            if (mail.author() != null) generator.writeStringField("author", mail.author());

            if (mail.to() != null) {
                generator.writeArrayFieldStart("to");
                for (String to : mail.to()) {
                    generator.writeString(to);
                }
                generator.writeEndArray();
            }

            if (mail.subject_original() != null) generator.writeStringField("subject_original", mail.subject_original());
            if (mail.subject() != null) generator.writeStringField("subject", mail.subject());
            if (mail.dateUTC() != null) generator.writeStringField("date", mail.dateUTC());
            /*if (mail.message_id_original() != null)*/ generator.writeStringField("message_id_original", mail.message_id_original());
            /*if (mail.messageId() != null)*/ generator.writeStringField("message_id", mail.message_id());

            generator.writeArrayFieldStart("references");
            if (mail.references() != null) {
                for (String ref : mail.references()) {
                    generator.writeString(ref);
                }
            }
            generator.writeEndArray();

            if (mail.in_reply_to() != null) generator.writeStringField("in_reply_to", mail.in_reply_to());

            if (mail.message_snippet() != null) generator.writeStringField("message_snippet", mail.message_snippet());

            if (mail.first_text_message() != null) generator.writeStringField("first_text_message", mail.first_text_message());
            if (mail.first_text_message_without_quotes() != null) generator.writeStringField("first_text_message_without_quotes", mail.first_text_message_without_quotes());
            if (mail.first_html_message() != null) generator.writeStringField("first_html_message", mail.first_html_message());

            if (mail.text_messages() != null) {
                generator.writeArrayFieldStart("text_messages");
                for (String part : mail.text_messages())
                {
                    generator.writeString(part);
                }
                generator.writeEndArray();
            }

            if (mail.text_messages_cnt() != null) generator.writeNumberField("text_messages_cnt", mail.text_messages_cnt());

            if (mail.html_messages() != null) {
                generator.writeArrayFieldStart("html_messages");
                for (String part : mail.html_messages())
                {
                    generator.writeString(part);
                }
                generator.writeEndArray();
                generator.writeNumberField("html_messages_cnt", mail.html_messages_cnt());
            }

            if (mail.message_attachments_cnt() > 0) {
                generator.writeArrayFieldStart("message_attachments");
                for (MailAttachment atchm : mail.message_attachments()) {
                    generator.writeStartObject();
                    generator.writeStringField("content_type",atchm.getContentType());
                    generator.writeStringField("filename", atchm.getFileName());
                    generator.writeStringField("content", atchm.getContent());
                    generator.writeEndObject();
                }
                generator.writeEndArray();
                generator.writeNumberField("message_attachments_cnt", mail.message_attachments_cnt());
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
}
