
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
 * Describes a synth patch parm which has a continuous range of values,
 * e.g. filter resonance between 0 and 127.
 *
 * @author Kenneth L. Martinez
 */

public class SynthParmRange extends SynthParmAbstract {
	protected int low;

	SynthParmRange(String pName, int pLow, int pHi, SysexParm pSp) {
		super(pName, pHi, pSp);
		low = pLow;
	}

	public int getLow() {
		return low;
	}

	/**
	 * @return value
	 */
	public String getValue() {
		return Integer.toString(value);
	}

	public int getIntValue() {
		return value;
	}

	/**
	 * @param i
	 */
	public void setValue(int i) {
		value = i;
		if (value >= low && value <= hi) {
			defined = true;
			valid = true;
		} else {
			defined = false;
			System.out.println(name + ": value " + i + " is out of range " + low + " to " + hi);
			if (sp.canBeStored(i)) {
				valid = true;
			} else {
				valid = false;
				System.out.println(name + ": value " + i + " cannot be stored in sysex");
			}
		}
	}

	/**
	 * @param s string with numeric value
	 */
	public void setValue(String s) {
		try {
			int i = new Integer(s).intValue();
			setValue(i);
		} catch (NumberFormatException e) {
			defined = false;
			valid = false;
			System.out.println(name + ": value " + s + " is not a valid number");
		}
	}
}
