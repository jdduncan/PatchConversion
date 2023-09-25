
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
 * Controls one range parm from another parm, making the slave's value equal
 * the master's.
 *
 * @author Kenneth L. Martinez
 */

public class ParmLinkTranslatorDirect implements ParmTranslator {
	private ParmLink link;
	private ModuleParm master;
	private ModuleParm slave;

	ParmLinkTranslatorDirect(ParmLink pLink) {
		link = pLink;
		master = link.getMasterParm();
		slave = link.getSlaveParm();
	}

	public void toGeneric() {
		slave.setValue(master.getValue());
	}

	public void fromGeneric() {
		if (slave.isUsed() == false || slave.getMod().getUsed() < 3) {
			return;
		}
		String s = slave.getValue();
		if (master.isUsed() && master.getMod().getUsed() == 3) {
			if (master.getValue().equalsIgnoreCase(s) == false) {
				System.out.println("Warning: " + slave.getMod().getName() + " " +
						slave.getName() + " has value " + s + " - expected value was " + master.getValue() +
						" due to link to " + master.getMod().getName() + " " + master.getName());
			}
		} else {
			// Master is unused; copy slave value to master, so that master's
			// ParmTranslator will put that value into sysex.
			master.setValue(s);
		}
	}
}
