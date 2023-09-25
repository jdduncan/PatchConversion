
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
 * Describes one parameter of a synth patch (oscillator pitch, filter
 * resonance, etc).  Implementor must read and write parm from sysex,
 * read and write value in source patch XML (using synth's specific units,
 * e.g. filter resonance between 0 and 127).
 *
 * @author Kenneth L. Martinez
 */

interface SynthParm {
	void getValueFromSysex(byte pSysexData[]);
	void putValueToSysex(byte pSysexData[]);
	boolean isValid();
	boolean isDefined();
	String getName();
	String getValue();
	void setValue(int i);
	void setValue(String s);
}
