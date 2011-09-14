/*
 * Copyright Matt Palmer 2011, All rights reserved.
 *
 */

package net.domesdaybook.reader;

import java.io.File;
import java.io.FileNotFoundException;

/**
 *
 * @author matt
 */
public class Utilities {
    
    
    public static FileReader getFileByteReader(final String path) {
        return getFileByteReader(new File(path));
    }
    
    
    public static FileReader getFileByteReader(final File file) {
        try {
            return new FileReader(file);
        } catch (FileNotFoundException ex) {
            return null;
        }
    }
    
    
    public static FileReader getFileArrayProvider(final File file) {
        try {
            return new FileReader(file);
        } catch (FileNotFoundException ex) {
            return null;
        }
    }

    
    public static byte[] getByteArray(String path) {
        return getByteArray(new File(path));
    }
                
    
    public static byte[] getByteArray(File file) {
        try {
            final Reader reader = new FileReader(file);
            final Window array = reader.getWindow(0);
            return array.getArray();
        } catch (FileNotFoundException ex) {
            return null;
        }
    }
    
    
}