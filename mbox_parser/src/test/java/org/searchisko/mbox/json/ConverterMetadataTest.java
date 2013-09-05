/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.mbox.json;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Message;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.searchisko.mbox.parser.MessageParseException;
import org.searchisko.mbox.parser.MessageParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
@RunWith(JUnit4.class)
public class ConverterMetadataTest extends ConverterTestSupport {

    @Test
    public void shouldAddAndOverrideFromMetadata() throws IOException, MimeException, MessageParseException {

        Map<String, String> metadata = new HashMap<>();
        metadata.put("foo", "bar");
        metadata.put("author", "John Doe <john.doe@his.com>");

        Message msg = getMessage("mbox/encoding/invalid/simple.mbox", mb);

        JsonNode node = null;
        try {
            node = mapper.readValue(
                    new ByteArrayInputStream(
                            Converter.toJSON(
                                    MessageParser.parse(msg),
                                    metadata
                            ).getBytes()
                    ),
                    JsonNode.class
            );
        } catch (IOException e) {
            fail("Exception while parsing!: " + e);
        }

        assertTrue(node.has("foo"));
        assertTrue(node.has("author"));

        assertEquals("John Doe <john.doe@his.com>", node.get("author").getTextValue());

    }

}
