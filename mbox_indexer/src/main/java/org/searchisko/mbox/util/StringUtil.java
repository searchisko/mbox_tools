/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.mbox.util;

import org.apache.commons.codec.binary.Base64;

/**
 * Collection of utility methods for handling Base64 coding and parsing specific (i.e. mailman) URL string.
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
public class StringUtil {

    /**
     *  POJO to bear project name and mail list type both parsed from URL.
     */
    public static class URLInfo {

        // Does not allow to create instances of this class outside of parent class.
        private URLInfo() {};

        public String project;
        public String listType;

        public void setProject(String project) { this.project = project; }
        public void setListType(String listType) { this.listType = listType; }
        public String getProject() { return project; }
        public String getListType() { return listType; }
    }

    private static Base64 base64 = null;

    static {
        base64 = new Base64();
    }

    private static byte[] base64Decode(String encoded) {
        return base64.decode(encoded);
    }

    private static String base64Encode(String source) {
        return base64.encodeToString(source.getBytes());
    }

    private static String convertFilenameSafe(String source) {
        return source.replaceAll("_","/");
    }

    private static String[] splitURL(String source) {
        return source.split("\\/");
    }

    private static String join(String[] s, String delimiter) {
        StringBuilder buffer = new StringBuilder();
        boolean first = true;
        for (String item : s ) {
            if (first) { first = false; }
            else { buffer.append(delimiter); }
            buffer.append(item);
        }
        return buffer.toString();
    }

    public static String encodeFilenameSafe(String source) {
        return StringUtil.base64Encode(StringUtil.convertFilenameSafe(source));
    }

    public static String decodeFilenameSafe(String encoded) {
        return StringUtil.convertFilenameSafe(new String(StringUtil.base64Decode(encoded)));
    }

    public static URLInfo getInfo(String encoded) {

        String target = StringUtil.splitURL(StringUtil.convertFilenameSafe(new String(StringUtil.base64Decode(encoded))))[4];
        URLInfo info = new URLInfo();

        // TODO make exceptions like -l10n or -rpm configurable
        if (target.indexOf("-") > -1 && !target.endsWith("-l10n") && !target.endsWith("-rpm")) {
            String[] _tmp = target.split("-");
            String[] _tmp2 = new String[_tmp.length-1];
            System.arraycopy(_tmp, 0, _tmp2, 0, _tmp2.length);
            info.setProject(StringUtil.join(_tmp2, "-"));
            info.setListType(_tmp[_tmp.length-1]);
        } else {
            info.setProject(target);
            info.setListType(null);
        }

        return info;
    }
}
