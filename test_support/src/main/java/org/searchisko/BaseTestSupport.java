/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko;

import java.io.FileNotFoundException;
import java.io.InputStream;

import static java.lang.ClassLoader.getSystemClassLoader;

/**
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
public abstract class BaseTestSupport {

    protected InputStream getInputStream(String path) throws FileNotFoundException {
        return getSystemClassLoader().getResourceAsStream(path);
    }
}
