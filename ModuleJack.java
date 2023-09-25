
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
 * Describes common features of module input and output jacks
 *
 * @author Kenneth L. Martinez
 */

//import java.util.*;

public abstract class ModuleJack {
	public static final String JACK_TYPE[] = { "control_input", "control_output", 
			"audio_input", "audio_output" };
	protected String tag = "bogus";
	private String name;
	private String type;
	private boolean used;
	private Module mod;

	ModuleJack(String pName, String pType) {
		name = pName;
		setType(pType);
	}

	ModuleJack(String xml, Module pMod) {
		name = XMLReader.getTagValue(xml, "name");
		setType(XMLReader.getTagValue(xml, "type"));
		used = true;
		mod = pMod;
	}

	public void setMod(Module pMod) {
		mod = pMod;
	}

	public Module getMod() {
		return mod;
	}

	public String getName() {
		return name;
	}

	void setName(String s) {
		name = s;
	}

	public String getPrefix() {
		if (Character.isDigit(name.charAt(name.length() - 1))) {
			return name.substring(0, name.length() - 1);
		} else {
			return name;
		}
	}

	public int getNumber() {
		if (Character.isDigit(name.charAt(name.length() - 1))) {
			return new Integer(name.substring(name.length() - 1)).intValue();
		} else {
			return 0;
		}
	}

	public void setNumber(int i) {
		if (Character.isDigit(name.charAt(name.length() - 1))) {
			name = name.substring(0, name.length() - 1) + i;
		} else {
			name = name + i;
		}
	}

	public void setType(String s) {
		int i;
		for (i = 0; i < JACK_TYPE.length; i++) {
			if (s.equalsIgnoreCase(JACK_TYPE[i])) {
				break;
			}
		}
		type = JACK_TYPE[i]; // will get exception on invalid value
	}

	public String getType() {
		return type;
	}

	public boolean isUsed() {
		return used;
	}

	public void setUsed(boolean b) {
		used = b;
	}

	public boolean isConnectedToUsed() throws PatchDefinitionException {
		throw new PatchDefinitionException("This function from abstract class should never be used");
	}

	public String writeXML() throws PatchDefinitionException {
		// Don't write out unused jack
		if (used == false) {
			return "";
		}
		// If the jack is connected to an unused jack, or a jack on an unused
		// module, then this one's unused
		if (isConnectedToUsed() == false) {
			return "";
		}
		StringBuffer sb = new StringBuffer("<" + tag + "jack>");
		sb.append("<name>" + name + "</name>");
		sb.append("<type>" + type + "</type>");
		sb.append(writeValue());
		sb.append("</" + tag + "jack>");
		return sb.toString();
	}

	String writeValue() {
		return "";
	}
}
