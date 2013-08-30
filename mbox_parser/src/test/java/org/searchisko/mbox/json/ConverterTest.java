/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.mbox.json;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.searchisko.mbox.MessageTestSupport;
import org.searchisko.mbox.parser.MessageParseException;
import org.searchisko.mbox.parser.MessageParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
@RunWith(JUnit4.class)
public class ConverterTest extends MessageTestSupport {

    private MessageBuilder mb;
    private ObjectMapper mapper;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws MimeException {
        mb = MessageParser.getMessageBuilder();
        mapper = getMapper();
    }

    @Test
    public void shouldEquals() throws MimeException, MessageParseException, IOException {
        shouldEquals("mbox/simple/simple.mbox", "json/simple/simple.json");
    }

    /**
     * Test bunch of mail from lucene-user mail archive.
     */
    @Test
    public void shouldEqualsLuceneML() throws MimeException, MessageParseException, IOException {
        shouldEquals("mbox/lucene-user/lucene0087.mbox", "json/lucene-user/lucene0087.json");
        shouldEquals("mbox/lucene-user/lucene0089.mbox", "json/lucene-user/lucene0089.json");
    }

    @Test
    public void base64CodingTest() throws MimeException, MessageParseException, IOException {
        shouldEquals("mbox/encoding/hibernate-announce-01.mbox", "json/encoding/hibernate-announce-01.json");
        shouldEquals("mbox/encoding/jbpm-users-01.mbox", "json/encoding/jbpm-users-01.json");
    }

    public void shouldEquals(String sourceMBoxPath, String expectedFilePath) throws IOException, MimeException, MessageParseException {

        Message msg = getMessage(sourceMBoxPath, mb);

        JsonNode jsonFromMessage = jsonNodeFromMessage(msg, mapper);
        JsonNode jsonFromPath    = jsonNodeFromPath(expectedFilePath, mapper);

        assertTrue(jsonFromMessage.equals(jsonFromPath));

    }

    /**
     * Can be used to get pretty JSON from messages.
     * This method should be commented out.
     */
//    @Test
    public void shouldNotBeIncludedInTests() throws IOException, MimeException, MessageParseException {

        Message msg = getMessage("mbox/encoding/jbpm-users-01.mbox", mb);
        System.out.println(Converter.toJSON(MessageParser.parse(msg), true));

    }

    private ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    private JsonNode jsonNodeFromMessage(Message msg, ObjectMapper mapper) throws MessageParseException {
        JsonNode node = null;
        try {
            node = mapper.readValue(
                    new ByteArrayInputStream(
                            Converter.toJSON(
                                    MessageParser.parse(msg)
                            ).getBytes()
                    ),
                    JsonNode.class
            );
        } catch (IOException e) {
            fail("Exception while parsing!: " + e);
        }
        return node;
    }

    private JsonNode jsonNodeFromPath(String jsonPath, ObjectMapper mapper) {
        JsonNode node = null;
        try {
            node = mapper.readValue(getInputStream(jsonPath), JsonNode.class);
        } catch (IOException e) {
            fail("Exception while parsing!: " + e);
        }
        return node;
    }
}
