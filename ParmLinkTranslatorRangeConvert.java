
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
 * Controls one range parm from another parm, e.g. on a crossfade mixer,
 * input two (the slave) is at minimum when input one (the master) is at maximum.
 *
 * @author Kenneth L. Martinez
 */

public class ParmLinkTranslatorRangeConvert implements ParmTranslator {
	private ParmLink link;
	private ModuleParm master;
	private ParmValidatorRange pvm;
	private ModuleParm slave;
	private ParmValidatorRange pvs;
	private double low;
	private double hi;

	ParmLinkTranslatorRangeConvert(ParmLink pLink, double pLow, double pHi) {
		link = pLink;
		master = link.getMasterParm();
		slave = link.getSlaveParm();
		low = pLow;
		hi = pHi;
		pvm = (ParmValidatorRange)master.getPv();
		pvs = (ParmValidatorRange)slave.getPv();
	}

	public void toGeneric() {
		slave.setValue(Util.rangeConvert(master.getValue(),
				pvm.getLow(), pvm.getHi(), low, hi));
	}

	public void fromGeneric() {
		if (slave.isUsed() == false || slave.getMod().getUsed() < 3) {
			return;
		}
		String s = Util.rangeConvert(slave.getValue(),
				low, hi, pvm.getLow(), pvm.getHi());
		if (master.isUsed() && master.getMod().getUsed() == 3) {
			if (master.getValue().equalsIgnoreCase(s) == false) {
				String s2 = Util.rangeConvert(master.getValue(),
						pvm.getLow(), pvm.getHi(), low, hi);
				System.out.println("Warning: " + slave.getMod().getName() + " " +
						slave.getName() + " has value " + s + " - expected value was " + s2 +
						" due to link to " + master.getMod().getName() + " " + master.getName());
			}
		} else {
			// Master is unused; copy slave value to master, so that master's
			// ParmTranslator will put that value into sysex.
			master.setValue(s);
		}
	}
}
