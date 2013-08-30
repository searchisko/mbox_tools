/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.ClassLoader.getSystemClassLoader;

/**
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
public abstract class BaseTestSupport {

    protected InputStream getInputStream(String path) throws FileNotFoundException {
        InputStream is = null;
        is = getSystemClassLoader().getResourceAsStream(path);
//        if (is == null) {
//            is = ClassLoader.getSystemResourceAsStream(path);
//        }
//        if (is == null) {
//            is = new FileInputStream(new File(path, "UTF-8"));
//        }
        return is;
    }

    protected String getFileContent(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream is = getInputStream(path);
        int c;
        while ((c = is.read()) != -1) {
            sb.append((char) c);
        }
        is.close();
        return sb.toString();
    }
}
