
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
 * Describes a synth patch name stored in consecutive bytes in a sysex string.
 *
 * @author Kenneth L. Martinez
 */

public class SynthParmName implements SynthParm {
	private boolean valid;
	private boolean defined;
	private String name;
	private int offset;
	private int length;
	private String value;

	SynthParmName(String pName, int pOffset, int pLength) {
		valid = false;
		defined = false;
		name = pName;
		offset = pOffset;
		length = pLength;
	}

	public boolean isValid() {
		return valid;
	}

	public boolean isDefined() {
		return defined;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(int i) {
		System.out.println("SynthParmName THIS SHOULD NEVER PRINT");
		int j = 1 / 0; // abort
	}

	public void setValue(String s) {
		value = s;
		valid = true;
		defined = true; // FIXME may not be true if input contains chars synth can't use
	}

	public void getValueFromSysex(byte pSysexData[]) {
		char c[] = new char[length];
		for (int i = 0; i < length; i++) {
			c[i] = (char)pSysexData[offset + i];
		}
		setValue(new String(c));
	}

	public void putValueToSysex(byte pSysexData[]) {
		int i;
		// pad with blanks if not full length
		for (i = value.length(); i < length; i++) {
			value = value + " "; 
		}
		byte nameBytes[] = value.getBytes();
		for (i = 0; i < length; i++) {
			pSysexData[offset + i] = nameBytes[i];
		}
	}
}
