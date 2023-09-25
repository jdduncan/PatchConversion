
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

public class XMLTagSysex implements XMLTag {
	private SynthPatch sp;

	XMLTagSysex(SynthPatch pSp) {
		sp = pSp;
	}

	public static String byteToHexStr(byte b) {
		String nibl[] = { "0", "1", "2", "3", "4", "5", "6", "7",
						"8", "9", "A", "B", "C", "D", "E", "F" };
		return nibl[(b & 0xF0) >>> 4] + nibl[b & 0x0F];
	}

	public boolean readXML(String xml) {
		return false;
	}

	public String writeXML() {
		int i, j;
		StringBuffer sb = new StringBuffer();

		sb.append("  <sysex length=\"" + sp.getSysex().length + "\">" + System.getProperty("line.separator"));
		for (i = 0, j = 1; i < sp.getSysex().length; i++, j++) {
			if (j == 1) {
				sb.append("    " + byteToHexStr(sp.getSysex()[i]) + " ");
			} else if (j == 5 || j == 15) {
				sb.append(byteToHexStr(sp.getSysex()[i]) + "   ");
			} else if (j == 10) {
				sb.append(byteToHexStr(sp.getSysex()[i]) + "    ");
			} else if (j < 20) {
				sb.append(byteToHexStr(sp.getSysex()[i]) + " ");
			} else {
				sb.append(byteToHexStr(sp.getSysex()[i]) + System.getProperty("line.separator"));
				j = 0;
			}
		}
		if (j > 2)
		sb.append(System.getProperty("line.separator"));
		sb.append("  </sysex>");
		return sb.toString();
	}
}
