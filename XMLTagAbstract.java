
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

public abstract class XMLTagAbstract implements XMLTag {
	protected String name;
	protected String attr;

	XMLTagAbstract(String pName) {
		name = pName;
	}

	XMLTagAbstract(String pName, String pAttr) {
		name = pName;
		attr = pAttr;
	}

	public boolean readXML(String xml) throws PatchDefinitionException {
		int start, end, valueStart;
		
		start = xml.indexOf("<" + name);
		if (start == -1) {
			System.out.println("begin tag <" + name + "> not found");
			return false;
		}
		if (attr == null) {
			valueStart = start + name.length() + 2;
		} else {
			start = xml.indexOf(attr + ">");
			if (start == -1) {
				System.out.println("attribute '" + attr + "' for begin tag <" +
						name + "> not found");
				return false;
			} else {
				valueStart = start + attr.length() + 1;
			}
		}
		end = xml.indexOf("</" + name + ">");
		if (end == -1) {
			System.out.println("end tag </" + name + "> not found");
			return false;
		}
		String s = xml.substring(valueStart, end);
		readValue(s);
		return true;
	}

	void readValue(String s) throws PatchDefinitionException {
		throw new PatchDefinitionException("This function of abstract class should never be used");
	}

	public String writeXML() throws PatchDefinitionException {
		StringBuffer sb = new StringBuffer("<" + name);
		if (attr == null) {
			sb.append(">");
		} else {
			sb.append(" " + attr + ">");
		}
		sb.append(writeValue());
		sb.append("</" + name + ">");
		return sb.toString();
	}

	String writeValue() throws PatchDefinitionException {
		throw new PatchDefinitionException("This function of abstract class should never be used");
	}
}
