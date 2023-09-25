
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
 * Describes a synth patch parm whose values are described as a list,
 * e.g. waveform is sine | triangle | saw | pulse.
 *
 * @author Kenneth L. Martinez
 */

public class SynthParmTable extends SynthParmAbstract {
	private String tbl[];

	SynthParmTable(String pName, String pTbl[], SysexParm pSp) {
		super(pName, pTbl.length, pSp);
		tbl = pTbl;
	}

	/**
	 * @return description from table for value
	 */
	public String getValue() {
		if (defined) {
			return tbl[value];
		} else {
			return Integer.toString(value);
		}
	}

	/**
	 * @return index in table of the value
	 */
	public int getIntValue() {
		return value;
	}

	/**
	 * @param i
	 */
	public void setValue(int i) {
		value = i;
		if (value >= 0 && value < hi) {
			defined = true;
			valid = true;
		} else {
			defined = false;
			System.out.println(name + ": value " + i + " is out of range 0-" + (hi - 1));
			if (sp.canBeStored(i)) {
				valid = true;
			} else {
				valid = false;
				System.out.println(name + ": value " + i + " cannot be stored in sysex");
			}
		}
	}

	/**
	 * @param s string value, to be compared against table of values
	 */
	public void setValue(String s) {
		int i;
		value = -1;
		for (i = 0; i < tbl.length; i++) {
			if (tbl[i].equalsIgnoreCase(s)) {
				value = i;
				break;
			}
		}
		if (value == -1) {
			defined = false;
			System.out.print(name + ": value " + s + " is not in list " + tbl[0]);
			for (i = 1; i < tbl.length; i++) {
				System.out.print("," + tbl[i]);
			}
			System.out.println("");
			try {
				i = new Integer(s).intValue();
				setValue(i);
			} catch (NumberFormatException e) {
				valid = false;
				System.out.println(name + ": value " + s + " is not a valid number");
			}
		} else {
			defined = true;
			valid = true;
		}
	}
}
