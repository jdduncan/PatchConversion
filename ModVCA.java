
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
 * This module is a vca with a built-in control offset (which adjusts for
 * bipolar or negative-polarity modulation input).
 *
 * @author Kenneth L. Martinez
 */

//import java.util.*;

public class ModVCA extends Module {

	ModVCA(String pName, int pNumber, String initOffset) throws PatchDefinitionException {
		super(pName, "mod_vca", pNumber);
		ModuleParm mp;
		addParm(new ModuleParm("Mod Offset", "percent", new ParmValidatorRange(0, 100), initOffset, "linear"));
		mp = new ModuleParm("Level Amt1", "percent", new ParmValidatorRange(0, 100), "100", "linear");
		addParm(mp);
		addInputJack(new ModuleInputJack("Level In1", "control_input"), mp);
		mp = new ModuleParm("Level Amt2", "percent", new ParmValidatorRange(-100, 100), "0", "linear");
		addParm(mp);
		addInputJack(new ModuleInputJack("Level In2", "control_input"), mp);
		mp = new ModuleParm("Level Amt1 Mod Amt", "percent", new ParmValidatorRange(-100, 100), "0", "linear");
		addParm(mp);
		addInputJack(new ModuleInputJack("Level Amt1 Mod In", "control_input"), mp);
		addInputJack(new ModuleInputJack("VCA In", "audio_input"));
		addOutputJack(new ModuleOutputJack("VCA Out", "audio_output"));
	}

	ModVCA(String xml) throws PatchDefinitionException {
		super(xml);
		if (findInputJack("Level In1") == null) {
			throw new PatchDefinitionException("Error: ModAmount " + getName() +
					" missing required jack Level In1");
		}
		if (findParm("Level Amt1") == null) {
			throw new PatchDefinitionException("Error: ModAmount " + getName() +
					" missing required parm Level Amt1");
		}
		if (findParm("Mod Offset") == null) {
			throw new PatchDefinitionException("Error: ModAmount " + getName() +
					" missing required parm Mod Offset");
		}
	}

	public void seeIfParmsUsed() {
		super.seeIfParmsUsed();
//		ModuleParm mp;
////		if (getParms().size() == 0) {  // FIXME why was this here?
////			return;
////		}
//		for (int i = 0; i < getParms().size(); i++) {
//			mp = ((ModuleParm)getParms().get(i));
//			if (mp.isUsed()) {
//				return;
//			}
//		}
//		setUsed(1);
	}
}
