/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.mbox;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.searchisko.BaseTestSupport;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
public class MessageTestSupport extends BaseTestSupport {

    protected Message getMessage(String path, MessageBuilder mb) throws IOException, MimeException {
        InputStream is = getInputStream(path);
        Message message = mb.parseMessage(is);
        is.close();
        return message;
    }
}
