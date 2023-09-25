
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
 * Describes a mixer module
 *
 * @author Kenneth L. Martinez
 */

//import java.util.*;

public class ModuleMixer extends Module {

	ModuleMixer(String pName, String pType, int pNumber) {
		super(pName, pType, pNumber);
	}

	ModuleMixer(String xml) throws PatchDefinitionException {
		super(xml);
	}

	public void seeIfParmsUsed() {
		super.seeIfParmsUsed();
		ModuleParm mp;
//		if (getParms().size() == 0) {  // FIXME why was this here?
//			return;
//		}
		for (int i = 0; i < getParms().size(); i++) {
			mp = ((ModuleParm)getParms().get(i));
			if (mp.isUsed()) {
				return;
			}
		}
		setUsed(1);
	}
}
