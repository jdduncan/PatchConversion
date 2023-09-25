
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
 * Describes how a parameter's value is controlled via another
 * parameter's value.  When the morph controller is at zero, the synth
 * uses the parameter value.  When the controller is at its max, the
 * synth uses the morph "max" value (which may be above or below the original
 * parameter value).  When the controller is somewhere in between, the
 * parameter value is proportionately between the two values.
 *
 * Fix soon:
 * - is ParmValidator needed?  not being used now
 * 
 * @author Kenneth L. Martinez
 */

public class ParmLink implements Parm {
	private String max;
	private ModuleParm slaveParm;
	private ModuleParm masterParm;
	private ParmValidator pv;

	ParmLink(ModuleParm pMasterParm, ParmValidator pPv) {
		max = "0";
		masterParm = pMasterParm;
		pv = pPv;
	}

	// FIXME needed?
//	public boolean isUsed() {
//		return masterParm.isUsed();
//	}

	public String getValue() {
		return max;
	}

	public void setValue(String s) {
		max = s;
	}

	public ParmValidator getPv() {
		return pv;
	}

	public ModuleParm getSlaveParm() {
		return slaveParm;
	}

	public void setSlaveParm(ModuleParm mp) {
		slaveParm = mp;
	}

	public ModuleParm getMasterParm() {
		return masterParm;
	}
}
