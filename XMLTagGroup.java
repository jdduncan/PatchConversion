
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

import java.util.*;

public class XMLTagGroup extends XMLTagAbstract {
	private ArrayList children;

	XMLTagGroup(String pName) {
		super(pName);
		children = new ArrayList();
	}

	XMLTagGroup(String pName, String pAttr) {
		super(pName, pAttr);
		children = new ArrayList();
	}

	XMLTagGroup(String pName, ArrayList al) {
		super(pName);
		children = al;
	}

	XMLTagGroup(String pName, String pAttr, ArrayList al) {
		super(pName, pAttr);
		children = al;
	}

	public void add(Object o) {
		children.add(o);
	}

	void readValue(String s) throws PatchDefinitionException {
		for (int i = 0; i < children.size(); i++) {
			((XMLTag)children.get(i)).readXML(s);
		}
	}

	String writeValue() throws PatchDefinitionException {
		String s;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < children.size(); i++) {
			s = ((XMLTag)children.get(i)).writeXML();
			if (s.equals("") == false) {
				sb.append(System.getProperty("line.separator") + s);
			}
		}
		sb.append(System.getProperty("line.separator"));
		return sb.toString();
	}
}
