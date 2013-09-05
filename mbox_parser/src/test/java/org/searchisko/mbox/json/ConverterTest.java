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

    /**
     * Test bunch of mails from lucene-user mail archive.
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

    /**
     * Should fail because the "Date:" header contains invalid date format (probably mandatory "< >")
     */
    @Test
    public void shouldFailsParsingInvalidDate1() throws IOException, MimeException, MessageParseException {
        Message msg = getMessage("mbox/spam/jbossws-dev-01.mbox", mb);
        thrown.expect(NullPointerException.class);
        MessageParser.parse(msg);
    }

    /**
     * Should fail because the "Date:" header contains invalid date format (probably mandatory "< >")
     */
    @Test
    public void shouldFailsParsingInvalidDate2() throws IOException, MimeException, MessageParseException {
        Message msg = getMessage("mbox/spam/jbossws-dev-02.mbox", mb);
        thrown.expect(NullPointerException.class);
        MessageParser.parse(msg);
    }

    @Test
    public void shouldFixInvalidCharset() throws IOException, MimeException, MessageParseException {
        shouldEquals("mbox/encoding/invalid/simple.mbox", "json/encoding/invalid/simple.json");
        shouldEquals("mbox/encoding/invalid/jboss-l10n-na-01.mbox", "json/encoding/invalid/jboss-l10n-na-01.json");
        shouldEquals("mbox/encoding/invalid/jboss-cluster-dev-01.mbox", "json/encoding/invalid/jboss-cluster-dev-01.json");
    }

    @Test
    public void shouldParseMessageWithAttachments() throws MimeException, MessageParseException, IOException {
        shouldEquals("mbox/attachments/netty-dev-01.mbox", "json/attachments/netty-dev-01.json");
        shouldEquals("mbox/attachments/cdi-dev-01.mbox", "json/attachments/cdi-dev-01.json");
        shouldEquals("mbox/attachments/gatein-dev-01.mbox", "json/attachments/gatein-dev-01.json");
    }

    /**
     * Test nested multipart (alternate/related) messages.
     */
    @Test
    public void nestedMultipartShouldMatchAndNotFail() throws MimeException, MessageParseException, IOException {
        shouldEquals("mbox/multipart/jopr-dev-01.mbox", "json/multipart/jopr-dev-01.json");
        shouldEquals("mbox/multipart/weld-dev-01.mbox", "json/multipart/weld-dev-01.json");
        shouldEquals("mbox/multipart/wise-users-01.mbox", "json/multipart/wise-users-01.json");
    }

    /**
     * Test charset=gbk
     */
    @Test
    public void gbkCharsetShouldMatchAndNotFail() throws MimeException, MessageParseException, IOException {
        shouldEquals("mbox/encoding/esb-users-01.mbox","json/encoding/esb-users-01.json");
    }

    /**
     * Test charset=x-gbk
     */
    @Test
    public void xgbkCharsetShouldMatchAndNotFail() throws MimeException, MessageParseException, IOException {
        shouldEquals("mbox/encoding/jboss-as7-dev-01.mbox","json/encoding/jboss-as7-dev-01.json");
    }

    /**
     * UTF-7 encoding not supported by JDK out of the box. Third-party library is needed to make it work.
     */
    @Test
    public void utf7CharsetShouldMatchAndNotFail() throws MimeException, MessageParseException, IOException {
        shouldEquals("mbox/encoding/rules-dev-01.mbox","json/encoding/rules-dev-01.json");
    }

    @Test
    public void windows1252CharsetShouldMatchAndNotFail() throws MimeException, MessageParseException, IOException {
        // TODO these two mails look similar.
        shouldEquals("mbox/encoding/jbosstools-dev-01.mbox","json/encoding/jbosstools-dev-01.json");
        shouldEquals("mbox/encoding/jbosstools-dev-02.mbox","json/encoding/jbosstools-dev-02.json");
    }

    @Test
    public void iso88591CharsetShouldMatchAndNotFail() throws MimeException, MessageParseException, IOException {
        shouldEquals("mbox/encoding/rules-users-01.mbox","json/encoding/rules-users-01.json");
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
        Message msg = getMessage("mbox/attachments/gatein-dev-01.mbox", mb);
        String prettyJson = Converter.toJSON(MessageParser.parse(msg), true);
        System.out.println(prettyJson);
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
