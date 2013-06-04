/*
 * Copyright Matt Palmer 2009-2012, All rights reserved.
 *
 * This code is licensed under a standard 3-clause BSD license:
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution.
 * 
 *  * The names of its contributors may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.byteseek.matcher.bytes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.byteseek.util.bytes.ByteUtilities;

/**
 * A fairly simple implementation of {@link ByteMatcherFactory}.  It attempts to build the
 * most efficient {@link ByteMatcher} which matches the set of bytes passed in (and whether or
 * not that set of bytes should be inverted).
 * <p>
 * Its heuristics are as follows:
 * <ul>
 * <li>Find the simple cases that only match 1, 2, 255 and 256 different values.
 *     Use either a {@link net.byteseek.matcher.bytes.OneByteMatcher}, a {@link net.byteseek.matcher.bytes.CaseInsensitiveByteMatcher}, a
 *     {@link net.byteseek.matcher.bytes.InvertedByteMatcher} or an {@link net.byteseek.matcher.bytes.AnyByteMatcher}.
 * <li>Do the set of bytes match a bitmask (all or any of the bits?)  Use either an 
 *     {@link net.byteseek.matcher.bytes.AnyBitmaskMatcher} or a {@link net.byteseek.matcher.bytes.AllBitmaskMatcher}.
 * <li>Do the set of bytes match a contiguous range of bytes?  Use a {@link net.byteseek.matcher.bytes.ByteRangeMatcher}
 * <li>For less than 16 byte values, use a {@link net.byteseek.matcher.bytes.SetBinarySearchMatcher}.
 * <li>Otherwise, fall back on a {@link net.byteseek.matcher.bytes.SetBitsetMatcher}.
 * </ul>
 * 
 * @author Matt Palmer
 */
public final class SetAnalysisByteMatcherFactory implements ByteMatcherFactory {

    private static final String ILLEGAL_ARGUMENTS = "Null or empty Byte set passed in to ByteSetMatcher.";
    private static final int BINARY_SEARCH_THRESHOLD = 16;

    
    /**
     * Creates an efficient {@link ByteMatcher} from a set of bytes passed in.
     * 
     * @param bytes A set of bytes which a ByteMatcher must match.
     * @return A ByteMatcher which matches that set of bytes.
     */
    @Override
    public ByteMatcher create(final Collection<Byte> bytes) {
        return create(bytes, ByteMatcherFactory.NOT_INVERTED);
    }
    
    
    /**
     * Creates an efficient {@link ByteMatcher} from a collection of bytes passed in (
     * and whether that the set of bytes in the collection should be inverted or not).
     * <p>
     * Duplicate values are permitted in the collection passed in.
     *
     * @param bytes  The collection of bytes to match (or their inverse).
     * @param matchInverse   Whether the set values are inverted or not
     * @return A ByteMatcher which is optimal for that set of bytes.
     */
    @Override
    public ByteMatcher create(final Collection<Byte> bytes, final boolean matchInverse) {
        if (bytes == null || bytes.isEmpty()) {
            throw new IllegalArgumentException(ILLEGAL_ARGUMENTS);
        }
        // Produce the (possibly inverted) unique set of bytes:
        Set<Byte> uniqueValues = new LinkedHashSet<Byte>(bytes);
        final  Set<Byte> values = matchInverse? ByteUtilities.invertedSet(uniqueValues) : uniqueValues;

        // See if some obvious byte matchers apply:
        ByteMatcher result = getSimpleCases(values);
        if (result == null) {
            
            // Now check to see if any of the invertible matchers 
            // match our set of bytes:
            result = getInvertibleCases(values, false);
            if (result == null) {

                // They didn't match the set of bytes, but since we have invertible
                // matchers, does the inverse set match any of them?
                final Set<Byte> invertedValues = matchInverse? uniqueValues : ByteUtilities.invertedSet(uniqueValues);
                result = getInvertibleCases(invertedValues, true);
                if (result == null) {

                    // Fall back on a standard set, defined as passed in.
                    result = new SetBitsetMatcher(uniqueValues, matchInverse);
                }
            }
        }
        return result;
    }

    
    private ByteMatcher getInvertibleCases(final Set<Byte> bytes, final boolean isInverted) {
        ByteMatcher result = getBitmaskMatchers(bytes, isInverted);
        if (result == null) {
            result = getRangeMatchers(bytes, isInverted);
            if (result == null) {
                 result = getBinarySearchMatcher(bytes, isInverted);
            }
        }
        return result;
    }


    private ByteMatcher getSimpleCases(final Set<Byte> values) {
        ByteMatcher result = null;
        switch (values.size()) {
            case 0: {
                // TODO: review whether a match that matches nothing should be allowed...?
                // matches no bytes at all - AnyBitmaskMatcher with a mask of zero never matches anything.
                // Or: should throw exception - matcher can never match anything.
                result = new AnyBitmaskMatcher((byte) 0, false);
                break;
            }

            case 1: { // just one byte matches:
                final Iterator<Byte> byteValue = values.iterator();
                result = new OneByteMatcher(byteValue.next());
                break;
            }

            case 2: { // there is a slim possibility it might be case insensitive...
                result = getCaseInsensitiveMatcher(values);
                break;
            }

            case 255: { // all but one byte matches - find the one that doesn't match:
                for (int byteValue = 0; byteValue < 256; byteValue++) {
                    if (!values.contains((byte) byteValue)) {
                        result = new InvertedByteMatcher((byte) byteValue);
                        break;
                    }
                }
                break;
            }
            
            case 256: { // all the bytes match:
                result = new AnyByteMatcher();
                break;
            }
        }
       return result;
    }


    private ByteMatcher getBitmaskMatchers(final Set<Byte> values, final boolean isInverted) {
        ByteMatcher result = null;
        // Determine if the bytes in the set can be matched by a bitmask:
        Byte bitmask = ByteUtilities.getAllBitMaskForBytes(values);
        if (bitmask != null) {
             result = new AllBitmaskMatcher(bitmask, isInverted);
        } else {
             bitmask = ByteUtilities.getAnyBitMaskForBytes(values);
             if (bitmask != null) {
                result = new AnyBitmaskMatcher(bitmask, isInverted);
             }
        }
        return result;
    }


    private ByteMatcher getRangeMatchers(final Set<Byte> values, boolean isInverted) {
        ByteMatcher result = null;
        // Determine if all the values lie in a single range:
        List<Integer> byteValues = getSortedByteValues(values);
        int lastValuePosition = byteValues.size() - 1;
        int firstValue = byteValues.get(0);
        int lastValue = byteValues.get(lastValuePosition);
        if (lastValue - firstValue == lastValuePosition) {  // values lie in a contiguous range
            result = new ByteRangeMatcher(firstValue, lastValue, isInverted);
        }
        return result;
    }

    
    private ByteMatcher getBinarySearchMatcher(final Set<Byte> values, final boolean isInverted) {
        ByteMatcher result = null;
        // if there aren't very many values, use a BinarySearchMatcher:
        if (values.size() < BINARY_SEARCH_THRESHOLD) { // small number of bytes in set - use binary searcher:
            result = new SetBinarySearchMatcher(values, isInverted);
        }
        return result;
    }


    private ByteMatcher getCaseInsensitiveMatcher(final Set<Byte> values) {
        ByteMatcher result = null;
        Character caseChar = getCaseChar(values);
        if (caseChar != null) {
            result = new CaseInsensitiveByteMatcher(caseChar);
        }
        return result;
    }

    
    private Character getCaseChar(final Set<Byte> values) {
        Character result = null;
        final int size = values.size();
        if (size == 2) {
            Iterator<Byte> iterator = values.iterator();
            int val1 = iterator.next() & 0xFF;
            int val2 = iterator.next() & 0xFF;
            if (isSameCharDifferentCase(val1, val2)) {
                result = Character.valueOf((char) val1);
            }
        }
        return result;
    }

    
    private boolean isSameCharDifferentCase(final int val1, final int val2) {
        boolean result = false;
        if (isAlphaChar(val1) && isAlphaChar(val2)) {
            result = Math.abs(val1-val2) == 32;
        }
        return result;
    }

    
    private boolean isAlphaChar(final int val) {
        return ((val >= 65 && val <= 90) || (val >= 97 && val <= 122));
    }

    
    private static List<Integer> getSortedByteValues(final Set<Byte> byteSet) {
        final List<Integer> sortedByteValues = new ArrayList<Integer>();
        for (final Byte b : byteSet) {
            sortedByteValues.add(Integer.valueOf(b.byteValue() & 0xFF));
        }
        Collections.sort(sortedByteValues);
        return sortedByteValues;
    }

}