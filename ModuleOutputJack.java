
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
 * Describes a module output jack
 *
 * @author Kenneth L. Martinez
 */

import java.util.*;

public class ModuleOutputJack extends ModuleJack {
	public static final String POLARITIES[] = { "bipolar", "positive", 
			"negative" };
	private String polarity;
	private ArrayList conn;

	ModuleOutputJack(String pName, String pType, String pPolarity) { 
		super(pName, pType);
		setPolarity(pPolarity);
		conn = new ArrayList();
		tag = "output_";
	}

	ModuleOutputJack(String pName, String pType) { 
		this(pName, pType, "bipolar");
	}

	ModuleOutputJack(String xml, Module pMod) {
		super(xml, pMod);
		String s = XMLReader.getTagValue(xml, "polarity");
		if (s == null) {
			polarity = "bipolar";
		} else {
			setPolarity(s);
		}
		conn = new ArrayList();
		tag = "output_";
	}

	public String getPolarity() {
		return polarity;
	}

	public void setPolarity(String s) {
		int i;
		for (i = 0; i < POLARITIES.length; i++) {
			if (s.equalsIgnoreCase(POLARITIES[i])) {
				break;
			}
		}
		polarity = POLARITIES[i]; // will get exception on invalid value
	}

	public void addConn(Connection pConn) {
		conn.add(pConn);
	}

	public void removeConn(Connection pConn) {
		conn.remove(pConn);
	}

	public Connection[] getConn() {
		if (conn.size() == 0) {
			return null;
		} else {
			return (Connection[])conn.toArray(new Connection[conn.size()]);
		}
	}

	public Connection getFirstConn() {
		if (conn.size() == 0) {
			return null;
		} else {
			return (Connection)conn.get(0);
		}
	}

	public boolean isConnectedToUsed() {
		ModuleInputJack mj;
		for (int j = 0; j < conn.size(); j++) {
			mj = ((Connection)conn.get(j)).getTargetJack();
			if (mj.isUsed() && mj.getMod().getUsed() > 1) {
				return true;
			}
		}
		return false;
	}

	String writeValue() {
		return "<polarity>" + polarity + "</polarity>";
	}
}
