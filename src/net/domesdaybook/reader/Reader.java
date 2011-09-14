/*
 * Copyright Matt Palmer 2009-2011, All rights reserved.
 * 
 */


package net.domesdaybook.reader;


/**
 * An interface for classes which can read bytes at a given position.
 * <p/>
 * Design issues: 
 * <p/>
 * 1. Should ReaderException be a checked or unchecked exception?  
 *    It is currently a RuntimeException, but it can encapsulate 
 *    sources which may throw a checked IOException (e.g. RandomAccessFile).
 *    It can also encapsulate sources which only throw RuntimExceptions,
 *     e.g. byte arrays.  
 *    Require behaviour to be consistent across all implementations for client code,
 * 
 * 
 * @author Matt Palmer
 */
public interface Reader {

    /**
     * Read a byte from a given position.
     *
     * @param position The position of the byte to read.
     * @return byte The byte at the position given.
     */
    public byte readByte(final long position) throws ReaderException;

    /**
     * 
     * @param position The position of the byte to read in the underlying data.
     * @return Window an Window containing a byte array, and a startPos which gives
     *         the position of the byte in the byte array.
     */
    Window getWindow(final long position) throws ReaderException;
    
    
    /**
     * @return long the length of the byte source accessed by the reader.
     */
    public long length();
    
    
    /**
     * Clears any cache associated with this Reader.
     */
    public void clearCache();
    
    
    /**
     * Close any underlying resources the reader has open.
     */
    public void close();
    
}