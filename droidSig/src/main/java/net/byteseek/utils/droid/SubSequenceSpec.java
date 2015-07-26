/*
 * Copyright Matt Palmer 2015, All rights reserved.
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

package net.byteseek.utils.droid;

import java.util.ArrayList;
import java.util.List;

public class SubSequenceSpec {
    public String mainExpression;
    public int minSeqOffset;
    public int maxSeqOffset;
    public List<FragmentSpec> leftFragments = new ArrayList<FragmentSpec>();
    public List<FragmentSpec> rightFragments = new ArrayList<FragmentSpec>();

    public String toDROIDXML(int position) {
        StringBuilder builder = new StringBuilder(2048);
        toDROIDXML(builder, position);
        return builder.toString();
    }

    public void toDROIDXML(StringBuilder builder, int position)  {
        builder.append("\t<SubSequence Position=\"").append(position).append("\" ");
        builder.append("SubSeqMaxOffset=\"").append(maxSeqOffset).append("\" ");
        builder.append("SubSeqMinOffset=\"").append(minSeqOffset).append("\">\n");
        builder.append("\t\t<Sequence>").append(StringUtils.escapeXml(mainExpression)).append("</Sequence>\n");
        for (FragmentSpec fragment : leftFragments) {
            fragment.toDROIDXML(builder, "LeftFragment");
        }
        for (FragmentSpec fragment : rightFragments) {
            fragment.toDROIDXML(builder, "RightFragment");
        }
        builder.append("\t</SubSequence>\n");
    }
}
