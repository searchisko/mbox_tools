/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.mbox.parser;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.searchisko.mbox.MessageTestSupport;
import org.searchisko.mbox.dto.Mail;

import java.io.*;
import java.util.Arrays;

/**
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
@RunWith(JUnit4.class)
public class MessageHeaderParsingTest extends MessageTestSupport {

    private MessageBuilder mb;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws MimeException {
        mb = MessageParser.getMessageBuilder();
    }

    @Test
    public void shouldParseHeaders() throws IOException, MimeException, MessageParseException {

        Message msg = getMessage("mbox/encoding/invalid/simple.mbox", mb);
        Mail mail = MessageParser.parse(msg, "clientSuffix");

        assertEquals(mail.message_id_original(), "<7EC53B0B-B47C-45E5-A9E8-46B48FCE394E@redhat.com>");
        assertEquals(mail.message_id(), "<7EC53B0B-B47C-45E5-A9E8-46B48FCE394E@redhat.com>clientSuffix");

        assertEquals(mail.author(), "Galder Zamarreño <galder@redhat.com>");

        assertThat(mail.to().length, is(1));
        assertThat(Arrays.asList(mail.to()), hasItems("infinispan -Dev List <infinispan-dev@lists.jboss.org>"));

        assertEquals(mail.subject_original(), "Re: [infinispan-dev] Feedback from Mobicents Cluster Framework on        top of Infinispan 5.0 Alpha1");
        assertEquals(mail.subject(), "Feedback from Mobicents Cluster Framework on top of Infinispan 5.0 Alpha1");

        assertEquals(mail.dateUTC(), "2011-01-04T10:30:45.000Z");

        assertEquals(mail.in_reply_to(), "<AANLkTikEBijE6P2Lm75phaZd63P1gR6oRMDCK-Vy3hFC@mail.gmail.com>");

        assertThat(mail.references().length, is(3));
        assertThat(
                Arrays.asList(mail.references()),
                hasItems("<AANLkTikwB2V_tjm9PjfTCn4V9Byn9t7U9v9QzAcdafSm@mail.gmail.com>",
                         "<18D7FECE-F54D-4028-A16F-8471A938D602@redhat.com>",
                         "<AANLkTikEBijE6P2Lm75phaZd63P1gR6oRMDCK-Vy3hFC@mail.gmail.com>")
        );
    }

    @Test
    public void withoutMessageIdFiresException() throws IOException, MimeException, MessageParseException {

        Message msg = getMessage("mbox/headers/withoutMessageId.mbox", mb);

        thrown.expect(MessageParseException.class);
        thrown.expectMessage("Message-ID header not found.");
        MessageParser.parse(msg);
    }

    @Test
    public void emptyMessageIdFiresException() throws IOException, MimeException, MessageParseException {

        Message msg = getMessage("mbox/headers/emptyMessageId.mbox", mb);

        thrown.expect(MessageParseException.class);
        thrown.expectMessage("Message-ID header is null or empty.");
        MessageParser.parse(msg);
    }

}
