/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.mbox.parser;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.searchisko.mbox.MessageTestSupport;
import org.searchisko.mbox.dto.Mail;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
@RunWith(JUnit4.class)
public class MessageBodyParsingTest extends MessageTestSupport {

    private MessageBuilder mb;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws MimeException {
        mb = MessageParser.getMessageBuilder();
    }

    @Test
    public void shouldParseMessage() throws IOException, MimeException, MessageParseException {

        Message msg = getMessage("mbox/simple/simple.mbox", mb);
        Mail mail = MessageParser.parse(msg);

        assertEquals(mail.message_snippet(),
                "See comments inline: On Jan 3, 2011, at 3:44 PM, Eduardo Martins wrote: I can't say 100% for sure, " +
                "Paul Ferraro or Scott Marlow will be able to clarify this further, but AFAIK, this is not being done " +
                "yet. -- Galder ZamarreÃ±o Sr. Software Engineer In");

        assertEquals(mail.first_text_message_without_quotes(),
                "See comments inline: On Jan 3, 2011, at 3:44 PM, Eduardo Martins wrote: I can't say 100% for sure, " +
                "Paul Ferraro or Scott Marlow will be able to clarify this further, but AFAIK, this is not being done " +
                "yet. -- Galder ZamarreÃ±o Sr. Software Engineer Infinispan, JBoss Cache"
        );
    }

}
