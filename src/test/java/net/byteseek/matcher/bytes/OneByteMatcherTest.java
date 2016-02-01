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

import net.byteseek.matcher.bytes.OneByteMatcher;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matt
 */
public class OneByteMatcherTest {

    /**
     * 
     */
    public OneByteMatcherTest() {
    }


    /**
     * Tests every possible byte value against every other non-matching
     * byte value.
     */
    @Test
    public void testMatcher() {
        for (int i = 0; i < 256; i++) {
            final byte theByte = (byte) i;
            final OneByteMatcher matcher = new OneByteMatcher(theByte);
            assertEquals("matches", true, matcher.matches(theByte));
            assertEquals("1 byte matches", 1, matcher.getNumberOfMatchingBytes());
            assertArrayEquals("matching bytes", new byte[] {theByte}, matcher.getMatchingBytes());
            final String regularExpression = String.format("%02x", theByte);
            assertEquals("regular expression", regularExpression, matcher.toRegularExpression(false));
            for (int x = 0; x < 256; x++) {
                if (x != i) {
                    final byte nomatch = (byte) x;
                    assertEquals("no match", false, matcher.matches(nomatch));
                }
            }
            if (i % 32 == 0) {
                String message = String.format("Matching byte %d", i);
                SimpleTimer.timeMatcher(message, matcher);
            }
        }

    }

}