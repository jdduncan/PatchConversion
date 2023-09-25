
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
 * JUnit tests for SynthParmRange
 *
 * @author Kenneth L. Martinez
 */

//import java.util.*;
import junit.framework.*;

public class SynthParmRangeTest extends TestCase {

	public SynthParmRangeTest(String name) {
		super(name);
	}

//	  public static void main(String[] args) {
//		  junit.textui.TestRunner.run(suite());
//	  }

	public static Test suite() {
		return new TestSuite(SynthParmRangeTest.class);
	}

	public void testGetValue() {
		int i;
		SynthParmRange spr = new SynthParmRange("spr1", 0, 1,
				new SysexParmOffset(0, 0));
		spr.setValue(1);
		assertTrue(spr.isValid());
		assertTrue(spr.isDefined());
		i = spr.getIntValue();
		assertTrue(i == 1);
		spr.setValue(2);
		assertTrue(spr.isValid());
		assertFalse(spr.isDefined());
		i = spr.getIntValue();
		assertTrue(i == 2);
		spr.setValue(128);
		assertFalse(spr.isValid());
		assertFalse(spr.isDefined());
	}

	public void testSetValue() {
		int i;
		SynthParmRange spr = new SynthParmRange("spr2", 0, 1,
				new SysexParmOffset(0, 0));
		spr.setValue("1");
		assertTrue(spr.isValid());
		assertTrue(spr.isDefined());
		i = spr.getIntValue();
		assertTrue(i == 1);
		spr.setValue("5");
		assertTrue(spr.isValid());
		assertFalse(spr.isDefined());
		i = spr.getIntValue();
		assertTrue(i == 5);
		spr.setValue("128");
		assertFalse(spr.isValid());
		assertFalse(spr.isDefined());
		i = spr.getIntValue();
		assertTrue(i == 128);
	}
}
