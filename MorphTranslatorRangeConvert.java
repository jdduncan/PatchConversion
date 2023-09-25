
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
 * Translates a range parm morph to a generic range parm morph
 * or vice versa.
 *
 * @author Kenneth L. Martinez
 */

public class MorphTranslatorRangeConvert implements ParmTranslator {
	private SynthParmRange sp;
	private SynthParmRange bp;
	private ModuleParm mp;
	private ParmValidatorRange pv;
	private ParmMorph morph;

	MorphTranslatorRangeConvert(SynthParmRange pSp, SynthParmRange pBp, ModuleParm pMp) {
		sp = pSp;
		bp = pBp;
		sp.getClass(); // referencing, to give error if it's null
		bp.getClass(); // referencing, to give error if it's null
		mp = pMp;
		pv = (ParmValidatorRange)mp.getPv();
		morph = mp.getMorph();
	}

	public void toGeneric() {
		int i = sp.getIntValue();
		if (i == 0) {
			morph.setUsed(false);
		} else {
			morph.setUsed(true);
			i += bp.getIntValue();
			morph.setValue(Util.rangeConvert(i, bp.getLow(), bp.getHi(),
					pv.getLow(), pv.getHi()));
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
		double d = new Double(valm).doubleValue() - new Double(valp).doubleValue();
		String s = Util.rangeConvert(d, pv.getLow(),
				pv.getHi(), bp.getLow(), bp.getHi());
		d = new Double(s).doubleValue();
		if (d >= 0) {
			sp.setValue(new Double(d + 0.5).intValue());
		} else {
			sp.setValue(new Double(d - 0.5).intValue());
		}
	}
}
