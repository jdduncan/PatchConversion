
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
 * Translates a range synth parm to a generic range parm
 * or vice versa, using a linear conversion to & from percent.
 *
 * @author Kenneth L. Martinez
 */

public class ParmTranslatorPct implements ParmTranslator {
	private SynthParmRange sp;
	private Parm mp;
	private ParmValidatorRange pv;

	ParmTranslatorPct(SynthParmRange pSp, Parm pMp) {
		sp = pSp;
		sp.getClass(); // referencing, to give error if it's null
		mp = pMp;
		pv = (ParmValidatorRange)mp.getPv();
	}

	public void toGeneric() {
		mp.setValue(Util.parmToPct(sp.getIntValue(), sp.getLow(), sp.getHi()));
	}

	public void fromGeneric() {
		try {
			int i = Util.pctToParm(mp.getValue(), sp.getLow(), sp.getHi());
			sp.setValue(i);
		} catch (NumberFormatException e) {
			sp.setValue(mp.getValue());
		}
	}
}
