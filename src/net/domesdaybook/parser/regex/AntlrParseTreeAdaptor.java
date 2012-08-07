/*
 * Copyright Matt Palmer 2012, All rights reserved.
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
package net.domesdaybook.parser.regex;

import java.util.List;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeAdaptor;

import net.domesdaybook.parser.ParseException;
import net.domesdaybook.parser.tree.ParseTree;
import net.domesdaybook.parser.tree.ParseTreeType;

/**
 * @author matt
 *
 */
public class AntlrParseTreeAdaptor extends CommonTreeAdaptor {

	@Override
	public Object create(Token payload) {
		switch (payload.getType()) {

		case AntlrRegexParser.BYTE: {
			return new HexByteAdaptor(payload, ParseTreeType.BYTE);
		}

		case AntlrRegexParser.ALL_BITMASK: {
			return new ChildHexByteAdaptor(payload, ParseTreeType.ALL_BITMASK);
		}

		case AntlrRegexParser.ANY_BITMASK: {
			return new ChildHexByteAdaptor(payload, ParseTreeType.ANY_BITMASK);
		}

		case AntlrRegexParser.CASE_INSENSITIVE_STRING: {
			return new QuotedTextAdaptor(payload, ParseTreeType.CASE_INSENSITIVE_STRING);
		}

		case AntlrRegexParser.CASE_SENSITIVE_STRING: {
			return new QuotedTextAdaptor(payload, ParseTreeType.CASE_SENSITIVE_STRING);
		}

		case AntlrRegexParser.ANY: {
			return new InvertibleNode(payload, ParseTreeType.ANY);
		}		
		
		case AntlrRegexParser.SET: {
			return new InvertibleNode(payload, ParseTreeType.SET, false);
		}

		case AntlrRegexParser.INVERTED_SET: {
			return new InvertibleNode(payload, ParseTreeType.SET, true);
		}

		case AntlrRegexParser.RANGE: {
			return new InvertibleNode(payload, ParseTreeType.RANGE);
		}

		case AntlrRegexParser.NUMBER: {
			return new IntAdaptor(payload, ParseTreeType.INTEGER);
		}

		default:
			//TODO: should we throw an exception here instead of creating a node with no type...?
			return new StructuralNode(payload, null);
		}
	}

	private static class StructuralNode extends CommonTree implements ParseTree {

		private final ParseTreeType	nodeType;

		private StructuralNode(Token payload, ParseTreeType nodeType) {
			super(payload);
			this.nodeType = nodeType;
		}

		@Override
		public final ParseTreeType getParseTreeType() {
			return nodeType;
		}

		@Override
		public byte getByteValue() throws ParseException {
			throw new ParseException("No byte value is available.");
		}

		@Override
		public int getIntValue() throws ParseException {
			throw new ParseException("No int value is available.");
		}

		@Override
		public String getTextValue() throws ParseException {
			throw new ParseException("No text value is available.");
		}

		@Override
		public boolean isValueInverted() throws ParseException {
			throw new ParseException("No value exists which can be inverted.");
		}

		@SuppressWarnings("unchecked")
		// We know that all tree nodes are ParseTree implementations.
		@Override
		public List<ParseTree> getChildren() {
			return super.getChildren();
		}

		protected final byte parseHexByte(final String hexByte) throws ParseException {
			try {
				return Byte.parseByte(hexByte, 16);
			} catch (NumberFormatException nfe) {
				throw new ParseException("Not a hex byte", nfe);
			}
		}

	}
	
	private static class InvertibleNode extends StructuralNode {
		
		private final boolean inverted;
		
		public InvertibleNode(Token payload, ParseTreeType type) {
			super(payload, type);
			inverted = false;
		}
		
		public InvertibleNode(Token payload, ParseTreeType type, boolean isInverted) {
			super(payload, type);
			inverted = isInverted;
		}
		
		@Override
		public final boolean isValueInverted() throws ParseException {
			return inverted;
		}
	}

	private final static class HexByteAdaptor extends InvertibleNode {

		public HexByteAdaptor(Token payload, ParseTreeType type) {
			super(payload, type);
		}

		@Override
		public byte getByteValue() throws ParseException {
			return parseHexByte(getText());
		}

	}

	private final static class ChildHexByteAdaptor extends InvertibleNode {

		public ChildHexByteAdaptor(Token payload, ParseTreeType type) {
			super(payload, type);
		}

		@Override
		public byte getByteValue() throws ParseException {
			//TODO: what is the type of the node under bitmask?  Is it a byte node?
			return parseHexByte(getChild(0).getText());
		}

	}
	
	private final static class QuotedTextAdaptor extends StructuralNode {

		public QuotedTextAdaptor(Token payload, ParseTreeType type) {
			super(payload, type);
		}

		@Override
		public String getTextValue() throws ParseException {
			return unquoteString(getText());
		}

		private String unquoteString(final String str) {
			return str.substring(1, str.length() - 1);
		}
	}

	/*
	public static class RangeAdaptor extends ParseTreeAdaptor {

		public RangeAdaptor(Token payload, ParseTreeType type) {
			super(payload, type);
		}

		
		@Override
		public Collection<Byte> getByteSetValue() throws ParseException {
			final ParseTree firstChild = (ParseTree) getChild(0);
			final ParseTree secondChild = (ParseTree) getChild(1);
			int minValue, maxValue;
			if (firstChild.getParseTreeType() == ParseTreeType.BYTE) {
				minValue = firstChild.getByteValue() & 0xFF;
				maxValue = secondChild.getByteValue() & 0xFF;
			} else if (firstChild.getParseTreeType() == ParseTreeType.CASE_SENSITIVE_STRING) {
				final String firstTextValue = firstChild.getTextValue();
				if (firstTextValue.length() != 1) {
					throw new ParseException("Only a single character is allowed for range values:"
							+ firstTextValue);
				}
				final String secondTextValue = secondChild.getTextValue();
				if (secondTextValue.length() != 1) {
					throw new ParseException("Only a single character is allowed for range values:"
							+ secondTextValue);
				}
				minValue = firstTextValue.charAt(0);
				maxValue = secondTextValue.charAt(0);
			} else {
				throw new ParseException(
						"Only bytes and case sensitive strings are allowed for ranges.");
			}
			if (minValue > maxValue) {
				final int tempSwap = minValue;
				minValue = maxValue;
				maxValue = tempSwap;
			}
			if (minValue < 0 || maxValue > 255) {
				throw new ParseException("Only range values from 0 to 255 are allowed.");
			}
			return buildRange(minValue, maxValue);
		}
		

		private List<Byte> buildRange(final int from, final int to) {
			final int rangeLength = from - to + 1;
			final List<Byte> byteRange = new ArrayList<Byte>(rangeLength);
			for (int rangeValue = from; rangeValue <= to; rangeValue++) {
				byteRange.add((byte) rangeValue);
			}
			return byteRange;
		}
		

	}
*/
	/*
	public static class SetAdaptor extends ParseTreeAdaptor {

		public SetAdaptor(Token payload, ParseTreeType type, boolean isInverted) {
			super(payload, type);
		}

		/*
		@Override
		public Set<Byte> getByteSetValue() throws ParseException {
		    final Set<Byte> setValues = new LinkedHashSet<Byte>(320);
		    for (final ParseTree child : getChildren()) {
		      switch (child.getParseTreeType()) {
	
	          case BYTE: {
	  	        setValues.add(child.getByteValue());
	  	        break;
	  	      }
	  	      
	  	      case RANGE:
	  	      case ALL_BITMASK:
	  	      case ANY_BITMASK:
	  	      case SET:
	  	      {
	  	    	if (child.isValueInverted()) {
	  	    		setValues.addAll(ByteUtilities.invertedSet(child.getByteSetValue()))
	  	    	} else {
	  	    		setValues.addAll(child.getByteSetValue());
	  	    	}
	  	        break;
	  	      }     
	  
	  	      case CASE_SENSITIVE_STRING: {
	  	        try {
	  	          final byte[] utf8Value = child.getTextValue().getBytes("US-ASCII");
	  	          ByteUtilities.addAll(utf8Value, setValues);
	  	        } catch (UnsupportedEncodingException e) {
	  	          throw new ParseException(e);
	  	        }
	  	        break;
	  	      }
	  
	  	      case CASE_INSENSITIVE_STRING: {
	  	        final String stringValue = child.getTextValue();
	  	        for (int position = 0; position < stringValue.length(); position++) {
	  	          final char charAtPos = stringValue.charAt(position);
	  	          if (charAtPos >= 'a' && charAtPos <= 'z') {
	  	            setValues.add((byte) Character.toUpperCase(charAtPos));
	  
	  	          } else if (charAtPos >= 'A' && charAtPos <= 'A') {
	  	            setValues.add((byte) Character.toLowerCase(charAtPos));
	  	          }
	  	          setValues.add((byte) charAtPos);
	  	        }
	  	        break;
	  	      }
	  
	  	      default: {
	  	        final ParseTreeType type = child.getParseTreeType();
	  	        final String message = String.format(TYPE_ERROR, type, type.getDescription());
	  	        throw new ParseException(message);
	  	      }
		      }
		    }
		    return setValues;
	    }
	    
	}
*/

	private final static class IntAdaptor extends StructuralNode {

		public IntAdaptor(Token payload, ParseTreeType type) {
			super(payload, type);
		}

		@Override
		public int getIntValue() throws ParseException {
			final String textValue = getText();
			try {
				return Integer.parseInt(textValue);
			} catch (NumberFormatException nfe) {
				throw new ParseException("Could not parse value into an integer: " + textValue, nfe);
			}
		}
	}

}