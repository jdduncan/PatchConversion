
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
 * Describes the validation of one parameter of a generic synth patch
 * (oscillator pitch, filter resonance, etc).
 *
 * @author Kenneth L. Martinez
 */

public class ParmValidatorNumTable implements ParmValidator {
	private String tbl[];

	ParmValidatorNumTable(String pTbl[]) {
		tbl = pTbl;
	}

	public String[] getTbl() {
		return tbl;
	}

	public boolean validateParm(String value) {
		return true;  // FIXME we're going to match any value to SOMETHING in the table; is this OK?
//		for (int i = 0; i < tbl.length; i++) {
//			if (tbl[i].equalsIgnoreCase(value)) {
//				return true;
//			}
//		}
//		return false;
	}
}
