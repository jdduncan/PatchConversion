
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
 * Executes all JUnit tests for this project
 *
 * @author Kenneth L. Martinez
 */

//import java.io.*;
//import java.util.*;
import junit.framework.*;

public class TestAll extends TestCase {

	public TestAll(String name) {
		super(name);
	}

//	  public static void main(String[] args) {
//		  junit.textui.TestRunner.run(suite());
//	  }

	public static Test suite() {
		TestSuite suite = new TestSuite(UtilTest.class);
		suite.addTestSuite(SynthParmRangeTest.class);
		suite.addTestSuite(SynthParmTableTest.class);
		suite.addTestSuite(XMLReaderTest.class);
		suite.addTestSuite(GenericPatchTest.class);
		suite.addTestSuite(NordLead2ProgTest.class);
		suite.addTestSuite(AccessVirusProgTest.class);
		suite.addTestSuite(SupernovaIIProgTest.class);
		suite.addTestSuite(NordModularPatchTest.class);
		suite.addTestSuite(Prophet600ProgTest.class);
		return suite;
	}
}