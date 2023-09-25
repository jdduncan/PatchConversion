
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
 * Read a patch parameter that's stored as a bit field.  The bit
 * number is the right-most bit that will be retrieved, with zero
 * being the farthest right.  Width is the number of bits to be
 * retrieved.
 *
 * @author Kenneth L. Martinez
 */

class SysexParmBitField implements SysexParm {
	private int sysexIndex;
	private int bit;
	private int mask;

	SysexParmBitField(int pSysexIndex, int pBit, int pWidth) {
		sysexIndex = pSysexIndex;
		bit = pBit;
		mask = 0;
		for (int i = bit; i < bit + pWidth; i++) {
			mask |= 0x01 << i;
		}
	}

	public int getSysexParm(byte sysexData[]) {
		return (sysexData[sysexIndex] & mask) >>> bit;
	}

	public void setSysexParm(byte sysexData[], int i) {
		sysexData[sysexIndex] = (byte)((sysexData[sysexIndex] & ~mask) | (i << bit));
	}

	public boolean canBeStored(int i) {
		int maxValue = mask >>> bit;
		if (i < 0 || i > maxValue) {
			return false;
		} else {
			return true;
		}
	}
}
