
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
 * Describes one parameter of a synth module (oscillator pitch,
 * filter resonance, etc)
 *
 * @author Kenneth L. Martinez
 */

public class ModuleParm implements Parm, Cloneable {
	private String name;
	private String unit;
	private String responseType;
	private boolean used;
	private Module mod;
	private String value;
	private String initValue;
	private ParmValidator pv;
	private ModuleInputJack attenuatedJack;
	// FIXME maybe more than one will be needed?
	private ParmMorph morph;
	private ParmLink link;

	ModuleParm(String pName, ParmValidator pPv, String pInitValue) {
		this(pName, null, pPv, pInitValue, null, null);
	}

	ModuleParm(String pName, String pUnit, ParmValidator pPv, String pInitValue) {
		this(pName, pUnit, pPv, pInitValue, null, null);
	}

	ModuleParm(String pName, String pUnit, ParmValidator pPv, String pInitValue,
			String pResponseType) {
		this(pName, pUnit, pPv, pInitValue, pResponseType, null);
	}

	ModuleParm(String pName, String pUnit, ParmValidator pPv, String pInitValue,
			String pResponseType, ParmMorph pMorph) {
		name = pName;
		unit = pUnit;
		responseType = pResponseType;
		pv = pPv;
		initValue = pInitValue;
		value = initValue;
		morph = pMorph;
	}

	ModuleParm(String xml) {
		name = XMLReader.getTagValue(xml, "name");
		unit = XMLReader.getTagValue(xml, "unit");
		responseType = XMLReader.getTagValue(xml, "response_type");
		used = true;
		value = XMLReader.getTagValue(xml, "value");
		String s = XMLReader.getTagValue(xml, "morph");
		if (s != null) {
			morph = new ParmMorph(s, this);
		}
	}

	public Object clone() {
		ModuleParm mp;
		Object o = null;
		try {
			o = super.clone();
		} catch(CloneNotSupportedException e) {
			e.printStackTrace(System.err);
		}
		mp = (ModuleParm)o;
		mp.setAttenuatedJack(null);
		if (morph == null) {
			mp.setMorph(null);
		} else {
			mp.setMorph(new ParmMorph(morph.getSource(), morph.getControl(),
					morph.getPv(), morph.getBaseParm()));
			mp.getMorph().setValue(morph.getValue());
		}
		mp.setLink(null);
		return o;
	}

	public void setMod(Module pMod) {
		mod = pMod;
	}

	public Module getMod() {
		return mod;
	}

	public void initialize() {
		value = initValue;
	}

	public String getName() {
		return name;
	}

	void setName(String s) {
		name = s;
	}

	public ParmMorph getMorph() {
		return morph;
	}

	public void setMorph(ParmMorph pMorph) {
		morph = pMorph;
		if (morph != null) {
			morph.setBaseParm(this);
		}
	}

	public ParmLink getLink() {
		return link;
	}

	public void setLink(ParmLink pLink) {
		link = pLink;
		if (link != null) {
			link.setSlaveParm(this);
		}
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

	public boolean isUsed() {
		return used;
	}

	public void setUsed(boolean b) {
		used = b;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String s) {
		value = s;
	}

	public ParmValidator getPv() {
		return pv;
	}

	public void setAttenuatedJack(ModuleInputJack mj) {
		attenuatedJack = mj;
	}

	public ModuleInputJack getAttenuatedJack() {
		return attenuatedJack;
	}

	public String writeXML() {
		if (used == false) {
			return "";
		}
		if (attenuatedJack != null && (attenuatedJack.isUsed() == false ||
				attenuatedJack.isConnectedToUsed() == false)) {
			return "";
		}
		StringBuffer sb = new StringBuffer("<parm>");
		sb.append("<name>" + name + "</name>");
		if (unit != null) {
			sb.append("<unit>" + unit + "</unit>");
		}
		if (responseType != null) {
			sb.append("<response_type>" + responseType + "</response_type>");
		}
		sb.append("<value>" + value + "</value>");
		if (morph != null) {
			sb.append(morph.writeXML());
		}
		sb.append("</parm>");
		return sb.toString();
	}
}
