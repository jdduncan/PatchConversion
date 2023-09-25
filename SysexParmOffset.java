
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
 * Read a patch parameter that's stored in one byte of the sysex
 * string.  Add the value offset when reading from sysex; subtract
 * it when writing.  Make the offset zero if it's not needed.
 *
 * @author Kenneth L. Martinez
 */

class SysexParmOffset implements SysexParm {
	private int sysexIndex;
	private int valueOffset;

	SysexParmOffset(int pSysexIndex, int pValueOffset) {
		sysexIndex = pSysexIndex;
		valueOffset = pValueOffset;
	}

	public int getSysexParm(byte sysexData[]) {
		return sysexData[sysexIndex] - valueOffset;
	}

	public void setSysexParm(byte sysexData[], int i) {
		sysexData[sysexIndex] = (byte)(i + valueOffset);
	}

	public boolean canBeStored(int i) {
		int storedValue = i + valueOffset;
		if (i < 0 || i > 127) { // MIDI data byte must have high bit = zero
			return false;
		} else {
			return true;
		}
	}
}
