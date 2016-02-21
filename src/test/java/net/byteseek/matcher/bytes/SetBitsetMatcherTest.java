/*
 * Copyright Matt Palmer 2009-2016, All rights reserved.
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

import net.byteseek.bytes.ByteUtils;

import java.io.IOException;
import java.util.*;

import net.byteseek.io.reader.ByteArrayReader;
import net.byteseek.io.reader.WindowReader;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matt
 */
public class SetBitsetMatcherTest {

    Random randomGenerator = new Random();

    private static byte[] BYTE_VALUES; // an array where each position contains the byte value corresponding to it.

    static {
        BYTE_VALUES = new byte[256];
        for (int i = 0; i < 256; i++) {
            BYTE_VALUES[i] = (byte) i;
        }
    }

    /**
     *
     */
    public SetBitsetMatcherTest() {
    }


    /**
     *
     */
    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testNullBitSetMatcher() {
        new SetBitsetMatcher(null, false);
    }

       /**
     *
     */
    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testEmptyBitSetMatcher() {
        new SetBitsetMatcher(new LinkedHashSet<Byte>(), false);
    }

    /**
     * Test of matches method, of class SetBitsetMatcher.
     *
     * Can't build all possible subsets of a byte set = 2^256 possible sets,
     * so generates a large number of random byte sets and tests them.
     */
    @Test
    public void testByteSet() throws IOException {
        int numberOfTests = 100;
        for (int testnum = 0; testnum <= numberOfTests; testnum++) {
            Set<Byte> bytesToTest = buildRandomByteSet();
            writeTestDefinition(testnum, numberOfTests, bytesToTest);
            testSet(bytesToTest);
        }
    }

    @Test
    public void testRegularExpressions() {
        int numberOfTests = 5;
        for (int testnum = 0; testnum <= numberOfTests; testnum++) {
            Set<Byte> bytesToTest = buildRandomByteSet();
            writeTestDefinition(testnum, numberOfTests, bytesToTest);
            testRegularExpression(bytesToTest);
        }
    }

    @Test
    public void testSetToString() {
        Set<Byte> bytes = new HashSet<Byte>();
        bytes.add((byte) 0);
        bytes.add((byte) 255);
        ByteMatcher matcher = new SetBitsetMatcher(bytes, InvertibleMatcher.NOT_INVERTED);
        String toString = matcher.toString();
        assertTrue("Matcher contains class name", toString.contains(matcher.getClass().getSimpleName()));
        assertTrue("Matcher contains inversion", toString.contains("inverted"));
        assertTrue("Matcher contains byte 0", toString.contains("0"));
        assertTrue("Matcher contains byte 255", toString.contains("255"));
    }

    private void testRegularExpression(Set<Byte> bytesToTest) {

        SetBitsetMatcher matcherNotInverted = new SetBitsetMatcher(bytesToTest, InvertibleMatcher.NOT_INVERTED);
        testExpression("BitSetMatcher", matcherNotInverted, bytesToTest);

        SetBitsetMatcher matcherInverted = new SetBitsetMatcher(bytesToTest, InvertibleMatcher.INVERTED);
        testExpression("BitSetMatcher", matcherInverted, bytesToTest);
    }


    private void testSet(Set<Byte> testSet) throws IOException {
        Set<Byte> otherBytes = ByteUtils.invertedSet(testSet);

        SetBitsetMatcher matcherNotInverted = new SetBitsetMatcher(testSet, InvertibleMatcher.NOT_INVERTED);
        testMatcher("BitSetMatcher", matcherNotInverted, testSet, otherBytes);

        SetBitsetMatcher matcherInverted = new SetBitsetMatcher(testSet, InvertibleMatcher.INVERTED);
        testMatcher("BitSetMatcher", matcherInverted, otherBytes, testSet);
    }

    private void testMatcher(String description, ByteMatcher matcher, Set<Byte> bytesMatched, Set<Byte> bytesNotMatched) throws IOException {
        // test of length
        assertEquals("length is one", 1, matcher.length());

        // test of getNumberOfMatchingBYtes() method
        int numberOfMatchingBytes = matcher.getNumberOfMatchingBytes();
        assertEquals("Matches correct number of bytes", bytesMatched.size(), numberOfMatchingBytes);

        // test of getMatchingBytes() method
        byte[] matchingBytes = matcher.getMatchingBytes();
        for (byte b : matchingBytes) {
            assertTrue("Contains byte " + b, bytesMatched.contains(b));
        }

        // test of matches(byte) method
        for (Byte byteShouldMatch : bytesMatched) {
            assertEquals(String.format("%s: Byte %02x should match:", description, byteShouldMatch), true, matcher.matches(byteShouldMatch));
        }
        for (Byte byteShouldNotMatch : bytesNotMatched) {
            assertEquals(String.format("%s: Byte %02x should not match:", description, byteShouldNotMatch), false, matcher.matches(byteShouldNotMatch));
        }

        // test of matches(WindowReader) method:
        WindowReader reader = new ByteArrayReader(BYTE_VALUES);
        assertFalse(matcher.matches(reader, -1L));
        assertFalse(matcher.matches(reader, 256L));
        for (Byte byteShouldMatch : bytesMatched) {
            long bytePosition = byteShouldMatch.byteValue() & 0xff;
            assertEquals(String.format("%s: Byte %02x should match:", description, byteShouldMatch), true, matcher.matches(reader, bytePosition));
        }
        for (Byte byteShouldNotMatch : bytesNotMatched) {
            long bytePosition = byteShouldNotMatch.byteValue() & 0xff;
            assertEquals(String.format("%s: Byte %02x should not match:", description, byteShouldNotMatch), false, matcher.matches(reader, bytePosition));
        }

        // test of matches(byte[]) method
        assertFalse(matcher.matches(BYTE_VALUES, -1));
        assertFalse(matcher.matches(BYTE_VALUES, 256));
        for (Byte byteShouldMatch : bytesMatched) {
            int bytePosition = byteShouldMatch.byteValue() & 0xff;
            assertEquals(String.format("%s: Byte %02x should match:", description, byteShouldMatch), true, matcher.matches(BYTE_VALUES, bytePosition));
        }
        for (Byte byteShouldNotMatch : bytesNotMatched) {
            int bytePosition = byteShouldNotMatch.byteValue() & 0xff;
            assertEquals(String.format("%s: Byte %02x should not match:", description, byteShouldNotMatch), false, matcher.matches(BYTE_VALUES, bytePosition));
        }

        // test of matchesNoBoundsCheck method
        try {
            assertFalse(matcher.matchesNoBoundsCheck(BYTE_VALUES, -1));
            fail("Expected an ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException expectedIgnore) {}

        try {
            assertFalse(matcher.matchesNoBoundsCheck(BYTE_VALUES, 256));
            fail("Expected an ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException expectedIgnore) {}

        for (Byte byteShouldMatch : bytesMatched) {
            int bytePosition = byteShouldMatch.byteValue() & 0xff;
            assertEquals(String.format("%s: Byte %02x should match:", description, byteShouldMatch), true, matcher.matchesNoBoundsCheck(BYTE_VALUES, bytePosition));
        }
        for (Byte byteShouldNotMatch : bytesNotMatched) {
            int bytePosition = byteShouldNotMatch.byteValue() & 0xff;
            assertEquals(String.format("%s: Byte %02x should not match:", description, byteShouldNotMatch), false, matcher.matchesNoBoundsCheck(BYTE_VALUES, bytePosition));
        }

    }

    private void testExpression(String description, InvertibleMatcher matcher, Set<Byte> bytesMatched) {
        String expression = matcher.toRegularExpression(false);
        assertEquals("Inversion of expression correct.", matcher.isInverted(), expression.startsWith("^"));
        for (Byte byteMatched : bytesMatched) {
            String value = Integer.toString(byteMatched & 0xFF, 16);
            assertTrue(expression.contains(value));
        }

        expression = matcher.toRegularExpression(true);
        assertEquals("Inversion of expression correct.", matcher.isInverted(), expression.startsWith("^"));
    }

    private void writeTestDefinition(int testnum, int totalTests, Set<Byte> bytesToTest) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("Test %d of %d\t{", testnum, totalTests));
        for (Byte b : bytesToTest) {
            builder.append(String.format("%02x ", b));
        }
        builder.append("}");
        System.out.println(builder.toString());
    }

    Set<Byte> buildRandomByteSet() {
        int numberOfElements = randomGenerator.nextInt(255) + 1;
        Set<Byte> randomSet = new TreeSet<Byte>();
        for (int i = 0; i < numberOfElements; i++) {
            int value = randomGenerator.nextInt(255);
            randomSet.add((byte) value);
        }
        return randomSet;
    }

}