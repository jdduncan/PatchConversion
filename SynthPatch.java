
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
 * Describes a synth patch (a.k.a. preset, program, etc).  Implementor
 * must be able to store all patch parameters in its attributes, read and
 * write patch sysex, and read and write synth-specific representation
 * of patch as XML.
 *
 * @author Kenneth L. Martinez
 */

//import java.io.*;

interface SynthPatch {
	boolean isValid();
	boolean matchSysex(byte sysex[]);
	void toSysex();
	void fromSysex(byte syx[]);
	boolean matchXMLStored(String xml);
	boolean matchXMLEdit(String xml);
	String toXML() throws PatchDefinitionException;
	void fromXML(String xml) throws PatchDefinitionException;
	public byte[] getSysex();
}
