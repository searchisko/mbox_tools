/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.mbox.parser;

/**
 *
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
public class MessageParseException extends Exception {

    public MessageParseException(Exception e) {
        super(e);
    }

    public MessageParseException(String message) {
        super(message);
    }
}
