
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
 * JUnit tests for SynthParmTable
 *
 * @author Kenneth L. Martinez
 */

//import java.util.*;
import junit.framework.*;

public class SynthParmTableTest extends TestCase {

	public SynthParmTableTest(String name) {
		super(name);
	}

//	  public static void main(String[] args) {
//		  junit.textui.TestRunner.run(suite());
//	  }

	public static Test suite() {
		return new TestSuite(SynthParmTableTest.class);
	}

	public void testGetValue() {
		String s;
		SynthParmTable spt = new SynthParmTable("spt1", new String[] { "Off", "On"},
				new SysexParmOffset(0, 0));
		spt.setValue(1);
		assertTrue(spt.isValid());
		assertTrue(spt.isDefined());
		s = spt.getValue();
		assertTrue(s.equals("On"));
		spt.setValue(2);
		assertTrue(spt.isValid());
		assertFalse(spt.isDefined());
		s = spt.getValue();
		assertTrue(s.equals("2"));
		spt.setValue(128);
		assertFalse(spt.isValid());
		assertFalse(spt.isDefined());
	}

	public void testSetValue() {
		int i;
		SynthParmTable spt = new SynthParmTable("spt2", new String[] { "Off", "On"},
				new SysexParmOffset(0, 0));
		spt.setValue("On");
		assertTrue(spt.isValid());
		assertTrue(spt.isDefined());
		i = spt.getIntValue();
		assertTrue(i == 1);
		spt.setValue("5");
		assertTrue(spt.isValid());
		assertFalse(spt.isDefined());
		i = spt.getIntValue();
		assertTrue(i == 5);
		spt.setValue("128");
		assertFalse(spt.isValid());
		assertFalse(spt.isDefined());
		i = spt.getIntValue();
		assertTrue(i == 128);
	}
}
