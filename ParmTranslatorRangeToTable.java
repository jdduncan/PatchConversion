
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
 * Translates a range synth parm to a generic table parm
 * or vice versa, e.g. envelope attack time may be a range of
 * numbers on the synth but is converted to time values using
 * a table.  Generic parm table MUST contain numbers in ascending order.
 *
 * @author Kenneth L. Martinez
 */

public class ParmTranslatorRangeToTable implements ParmTranslator {
	private SynthParmRange sp;
	private Parm mp;
	private ParmValidatorNumTable pv;

	ParmTranslatorRangeToTable(SynthParmRange pSp, Parm pMp) {
		sp = pSp;
		sp.getClass(); // referencing, to give error if it's null
		mp = pMp;
		pv = (ParmValidatorNumTable)mp.getPv();
	}

	public void toGeneric() {
		mp.setValue(pv.getTbl()[sp.getIntValue() - sp.getLow()]);
	}

	public void fromGeneric() {
		// FIXME does this need to give a warning message if the value is approximated?
		sp.setValue(Util.matchToNumberTable(mp.getValue(), pv.getTbl()) + sp.getLow());

//		String s = mp.getValue();
//		String convTbl[] = pv.getTbl();
//		int i;
//
//		for (i = 0; i < convTbl.length; i++) {
//			if (convTbl[i].equalsIgnoreCase(s)) {
//				sp.setValue(i + sp.getLow());
//				return;
//			}
//		}
//		sp.setValue(s);
	}
}
