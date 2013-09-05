/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.mbox.json;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.searchisko.mbox.MessageTestSupport;
import org.searchisko.mbox.parser.MessageParser;

/**
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
public abstract class ConverterTestSupport extends MessageTestSupport {

    MessageBuilder mb;
    ObjectMapper mapper;

    @Before
    public void setUp() throws MimeException {
        mb = MessageParser.getMessageBuilder();
        mapper = getMapper();
    }

    private ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

}
