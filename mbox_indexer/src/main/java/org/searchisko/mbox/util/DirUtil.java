/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.mbox.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;

/**
 * Collection of file and directory utilities.
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
public class DirUtil {

    /**
     * Directory is valid if it exists, does not represent a file, and can be read.
     */
    public static File validateDir(File dir) throws FileNotFoundException {
        if (dir == null) {
            throw new IllegalArgumentException("Directory should not be null.");
        }
        if (!dir.exists()) {
            throw new FileNotFoundException("Directory does not exist: " + dir);
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Is not a directory: " + dir);
        }
        if (!dir.canRead()) {
            throw new IllegalArgumentException("Directory cannot be read: " + dir);
        }
        return dir;
    }

    /**
     * Returns all readable subdirs.
     * @param root
     * @return
     * @throws java.io.FileNotFoundException
     */
    public static File[] getDirs(File root) throws FileNotFoundException {
        return validateDir(root).listFiles(new FileFilter(){
            public boolean accept(File file) {
                return (file.isDirectory() && file.canRead()) ? true : false;
            }
        });
    }

    /**
     * Returns all readable files for given dir with specific extension.
     * @param dir
     * @param extension if null then it is ignored
     * @return
     * @throws java.io.FileNotFoundException
     */
    public static File[] listFiles(File dir, final String extension) throws FileNotFoundException {
        return validateDir(dir).listFiles(new FileFilter(){
            public boolean accept(File file) {
                if (extension == null) {
                    return (file.isFile() && file.canRead()) ? true : false;
                } else {
                    return (file.isFile() && file.canRead() && file.getAbsolutePath().endsWith(extension)) ? true : false;
                }
            }
        });
    }

    /**
     * Calls {#listFiles(dir, null)}
     * @param dir
     * @return
     * @throws java.io.FileNotFoundException
     */
    public static File[] listFiles(File dir) throws FileNotFoundException {
        return listFiles(dir, null);
    }

    public static String getParentFolderName(File dir) throws FileNotFoundException {
        String[] path = validateDir(dir).getParent().split(File.separator);
        return path[path.length-1];
    }

}
