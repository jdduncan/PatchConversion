
/* Synth Patch Conversion
 * Copyright (C) 2003-4,  Kenneth L. Martinez (kmartin@users.sourceforge.net)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

package PatchConversion;

/**
 * Read one element at a time from input string containing XML
 *
 * @author Kenneth L. Martinez
 */

//import java.util.*;

public class XMLReader {
	private String xml;
	private int indx;

	XMLReader(String pXml, boolean convertEscapedChars) {
		if (convertEscapedChars) {
			xml = convertInputEscapedChars(pXml);
		} else {
			xml = pXml;
		}
		indx = 0;
	}

	XMLReader(String pXml) {
		this(pXml, true);
	}

	/**
	 * Returns a string array containing element name, value, and then attribute
	 * 
	 * @return String[3]
	 */
	String[] getNextTag() {
		int start, endBlank, end, valueStart, valueEnd;
		String tag[] = new String[3];
		
		start = xml.indexOf("<", indx);
		if (start == -1) {
//			System.out.println("begin tag \"<\" not found");
			return null;
		}
		end = xml.indexOf(">", start);
		if (end == -1) {
//			System.out.println("begin tag \">\" not found");
			return null;
		}
		endBlank = xml.indexOf(" ", start);
		if (endBlank != -1 && endBlank < end) {
			// attribute found after element name
			tag[0] = xml.substring(start + 1, endBlank);
			tag[2] = xml.substring(endBlank + 1, end);
		} else {
			tag[0] = xml.substring(start + 1, end);
			tag[2] = null;
		}
		valueStart = end + 1;
		valueEnd = xml.indexOf("</" + tag[0] + ">", valueStart);
		if (valueEnd == -1) {
//			System.out.println("end tag </" + tag[0] + "> not found");
			return null;
		}
		tag[1] = xml.substring(valueStart, valueEnd);
		indx = valueEnd + tag[0].length() + 3;
		return tag;
	}

	static String getTagValue(String xml, String name) {
		int start, end, valueStart, valueEnd;
		
		start = xml.indexOf("<" + name);
		if (start == -1) {
//			System.out.println("begin tag <" + name + "> not found");
			return null;
		}
		valueStart = start + name.length() + 2;
		end = xml.indexOf("</" + name + ">", valueStart);
		if (end == -1) {
//			System.out.println("end tag </" + name + "> not found");
			return null;
		}
		return xml.substring(valueStart, end);
	}

	/**
	 * Convert "&amp;" to "&", etc
	 */
	static String convertInputEscapedChars(String xml) {
		String s;
		StringBuffer sb = new StringBuffer();
		int i = 0, j;

		while (i < xml.length()) {
			if ((j = xml.indexOf("&", i)) != -1) {
				s = xml.substring(j);
				if (s.length() >= 5 && s.substring(0, 5).equals("&amp;")) {
					sb.append(xml.substring(i, j));
					sb.append("&");
					i = j + 5;
					continue;
				}
				if (s.length() >= 4 && s.substring(0, 4).equals("&lt;")) {
					sb.append(xml.substring(i, j));
					sb.append("<");
					i = j + 4;
					continue;
				}
				if (s.length() >= 4 && s.substring(0, 4).equals("&gt;")) {
					sb.append(xml.substring(i, j));
					sb.append(">");
					i = j + 4;
					continue;
				}
//	FIXME failing silently, because when this is called to initially write out XML,
//   conversion hasn't yet occurred... is this a problem?
//				System.out.println("convertInputEscapedChars: unexpected ampersand in '" + xml + "'");
			}
			sb.append(xml.substring(i));
			break;
		}
		return sb.toString();
	}

	/**
	 * Convert "&" to "&amp;", etc
	 */
	static String convertOutputEscapedChars(String xml) {
		StringBuffer sb = new StringBuffer();
		int i = 0, j;
		char ch;

		for (j = 0; j < xml.length(); j++) {
			ch = xml.charAt(j);
			if (ch == '&') {
				sb.append("&amp;");
				i = j + 1;
				continue;
			}
			if (ch == '<') {
				sb.append("&lt;");
				i = j + 1;
				continue;
			}
			if (ch == '>') {
				sb.append("&gt;");
				i = j + 1;
				continue;
			}
			sb.append(ch);
		}
		return sb.toString();
	}
}
