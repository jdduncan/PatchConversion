
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
 * Describes how a parameter's value is controlled via MIDI or another
 * parameter's value.  When the morph controller is at zero, the synth
 * uses the parameter value.  When the controller is at its max, the
 * synth uses the morph "max" value (which may be above or below the original
 * parameter value).  When the controller is somewhere in between, the
 * parameter value is proportionately between the two values.
 *
 * @author Kenneth L. Martinez
 */

/*
FIXME haven't thought this through completely... need:
- type: NL2 morph (which adjusts base parm max, using its linear or expo
  range, as if the parm knob was turned) or ??? used by other synths
- way to know if it's in use
- is ParmValidator needed?  not being used now
- should baseParm be set by ModuleParm.setMorph() instead of constructor?
*/

public class ParmMorph implements Parm {
	private boolean used;
	private String max;
	private String source;
	private String control;
	private ParmValidator pv;
	private ModuleParm baseParm;

	ParmMorph(String pSource, String pControl, ParmValidator pPv,
			ModuleParm pBaseParm) {
		max = "0";
		source = pSource;
		control = pControl;
		pv = pPv;
		baseParm = pBaseParm;
	}

	ParmMorph(String xml, ModuleParm pBaseParm) {
		used = true;
		baseParm = pBaseParm;
		max = XMLReader.getTagValue(xml, "max");
		source = XMLReader.getTagValue(xml, "source");
		control = XMLReader.getTagValue(xml, "control");
	}

	public boolean isUsed() {
		return used;
	}

	public void setUsed(boolean b) {
		used = b;
	}

	public String getValue() {
		return max;
	}

	public void setValue(String s) {
		max = s;
	}

	public String getSource() {
		return source;
	}

	public String getControl() {
		return control;
	}

	public ParmValidator getPv() {
		return pv;
	}

	public ModuleParm getBaseParm() {
		return baseParm;
	}

	public void setBaseParm(ModuleParm mp) {
		baseParm = mp;
	}

	public String writeXML() {
		if (used == false) {
			return "";
		}
		StringBuffer sb = new StringBuffer("<morph>");
		sb.append("<max>" + max + "</max>");
		sb.append("<source>" + source + "</source>");
		sb.append("<control>" + control + "</control>");
		sb.append("</morph>");
		return sb.toString();
	}
}
