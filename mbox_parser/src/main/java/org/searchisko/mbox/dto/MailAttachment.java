/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.mbox.dto;

/**
 * Represents parsed message attachment.
 *
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
public class MailAttachment {

    private String contentType;
    private String fileName;
    private String content;

    public String getContentType() { return this.contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getFileName() { return this.fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContent() { return this.content; }
    public void setContent(String content) { this.content = content; }
}
