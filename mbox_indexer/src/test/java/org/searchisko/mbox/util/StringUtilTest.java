/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.mbox.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
@RunWith(JUnit4.class)
public class StringUtilTest {

    @Test
    public void shouldPassCoupleOfBasicTests() {

        String originalURL;
        String encoded;
        StringUtil.URLInfo info;

        // http://lists.jboss.org/pipermail/hibernate-announce/2010-April/000077.html
        encoded = "aHR0cDovL2xpc3RzLmpib3NzLm9yZy9waXBlcm1haWwvaGliZXJuYXRlLWFubm91bmNlLzIwMTAtQXByaWwvMDAwMDc3Lmh0bWw=";
        originalURL = StringUtil.decodeFilenameSafe(encoded);
        assertEquals(originalURL, "http://lists.jboss.org/pipermail/hibernate-announce/2010-April/000077.html");
        info = StringUtil.getInfo(encoded);
        assertEquals(info.getProject(), "hibernate");
        assertEquals(info.getListType(), "announce");

        // no list type info
        // http://lists.jboss.org/pipermail/scribbling/2010-August/000002.html
        encoded = "aHR0cDovL2xpc3RzLmpib3NzLm9yZy9waXBlcm1haWwvc2NyaWJibGluZy8yMDEwLUF1Z3VzdC8wMDAwMDIuaHRtbA==";
        originalURL = StringUtil.decodeFilenameSafe(encoded);
        assertEquals(originalURL, "http://lists.jboss.org/pipermail/scribbling/2010-August/000002.html");
        info = StringUtil.getInfo(encoded);
        assertEquals(info.getProject(), "scribbling");
        assertEquals(info.getListType(), null);

        // project name contains '-'
        // http://lists.jboss.org/pipermail/jboss-osgi-dev/2009-March/000007.html
        encoded = "aHR0cDovL2xpc3RzLmpib3NzLm9yZy9waXBlcm1haWwvamJvc3Mtb3NnaS1kZXYvMjAwOS1NYXJjaC8wMDAwMDcuaHRtbA==";
        originalURL = StringUtil.decodeFilenameSafe(encoded);
        assertEquals(originalURL, "http://lists.jboss.org/pipermail/jboss-osgi-dev/2009-March/000007.html");
        info = StringUtil.getInfo(encoded);
        assertEquals(info.getProject(), "jboss-osgi");
        assertEquals(info.getListType(), "dev");

    }

    @Test
    public void shouldYieldEmptyListTypeFor_jboss_l10n() {

        // http://lists.jboss.org/pipermail/jboss-l10n/2009-March/000007.html
        String madeUpUrlForTest = "aHR0cDovL2xpc3RzLmpib3NzLm9yZy9waXBlcm1haWwvamJvc3MtbDEwbi8yMDA5LU1hcmNoLzAwMDAwNy5odG1s";
        StringUtil.URLInfo info = StringUtil.getInfo(madeUpUrlForTest);
        assertEquals(info.getProject(), "jboss-l10n");
        assertEquals(info.getListType(), null);

    }

    @Test
    public void shouldYieldEmptyListTypeFor_jboss_rpm() {

        // http://lists.jboss.org/pipermail/jboss-rpm/2012-June/000090.html
        String madeUpUrlForTest = "aHR0cDovL2xpc3RzLmpib3NzLm9yZy9waXBlcm1haWwvamJvc3MtcnBtLzIwMTItSnVuZS8wMDAwOTAuaHRtbA==";
        StringUtil.URLInfo info = StringUtil.getInfo(madeUpUrlForTest);
        assertEquals(info.getProject(), "jboss-rpm");
        assertEquals(info.getListType(), null);

    }

    @Test
    public void shouldFailOnIncorrectFileName() {

        try {
            String madeUpUrlForTest = ".xx";
            StringUtil.URLInfo info = StringUtil.getInfo(madeUpUrlForTest);

            fail("java.lang.ArrayIndexOutOfBoundsException should have been thrown");

        } catch (ArrayIndexOutOfBoundsException e) {
            // ok
        }
    }
}
