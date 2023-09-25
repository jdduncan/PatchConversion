
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
 * Translates a range parm morph to a generic table parm morph
 * or vice versa, e.g. envelope attack time may be a range of
 * numbers on the synth but is converted to time values using
 * a table.  Generic parm table MUST contain numbers in ascending order.
 *
 * @author Kenneth L. Martinez
 */

public class MorphTranslatorRangeToTable implements ParmTranslator {
	private SynthParmRange sp;
	private SynthParmRange bp;
	private ModuleParm mp;
	private ParmValidatorNumTable pv;
	private ParmMorph morph;

	MorphTranslatorRangeToTable(SynthParmRange pSp, SynthParmRange pBp, ModuleParm pMp) {
		sp = pSp;
		bp = pBp;
		sp.getClass(); // referencing, to give error if it's null
		bp.getClass(); // referencing, to give error if it's null
		mp = pMp;
		pv = (ParmValidatorNumTable)mp.getPv();
		morph = mp.getMorph();
	}

	public void toGeneric() {
		int i = sp.getIntValue();
		if (i == 0) {
			morph.setUsed(false);
		} else {
			morph.setUsed(true);
			i += bp.getIntValue() - bp.getLow();
			if (i < 0) {
				morph.setValue(pv.getTbl()[0]);
				System.out.println("parm " + bp.getName() + " morph resulting index " +
						i + " out of range - using index 0 value " + pv.getTbl()[0]);
			} else if (i > pv.getTbl().length) {
				morph.setValue(pv.getTbl()[pv.getTbl().length - 1]);
				System.out.println("parm " + bp.getName() + " morph resulting index " +
						i + " out of range - using index " + (pv.getTbl().length - 1) +
						" value " + pv.getTbl()[pv.getTbl().length - 1]);
			} else {
				morph.setValue(pv.getTbl()[i]);
			}
		}
	}

	public void fromGeneric() {
		if (morph.isUsed() == false) {
			sp.setValue(0);
			return;
		}
		String valp, valm;
		valp = mp.getValue();
		valm = morph.getValue();
		if (valp.equals(valm)) {
			sp.setValue(0);
			return;
		}
		int i = Util.matchToNumberTable(valm, pv.getTbl()) -
				Util.matchToNumberTable(valp, pv.getTbl()) + bp.getLow();
		// FIXME does this need to give a warning message if the value is approximated?
		sp.setValue(i);
	}
}
