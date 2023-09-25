
/* Synth Patch Conversion for Clavia Nord Modular patches
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
 * JUnit tests for NordModularPatch
 *
 * @author Kenneth L. Martinez
 */

import java.io.*;
//import java.util.*;
import junit.framework.*;

public class NordModularPatchTest extends TestCase {

	public NordModularPatchTest(String name) {
		super(name);
	}

//	  public static void main(String[] args) {
//		  junit.textui.TestRunner.run(suite());
//	  }

	public static Test suite() {
		return new TestSuite(NordModularPatchTest.class);
	}

	public void testFromXML1() throws IOException, PatchDefinitionException {
		fileFromXML(1);
	}

	public void testFromXML2() throws IOException, PatchDefinitionException {
		fileFromXML(2);
	}

	public void testFromXML3() throws IOException, PatchDefinitionException {
		fileFromXML(3);
	}

	public void testFromXML4() throws IOException, PatchDefinitionException {
		fileFromXML(4);
	}

	public void testFromXML5() throws IOException, PatchDefinitionException {
		fileFromXML(5);
	}

	public void testFromXML6() throws IOException, PatchDefinitionException {
		fileFromXML(6);
	}

	public void testFromXML7() throws IOException, PatchDefinitionException {
		fileFromXML(7);
	}

	public void fileFromXML(int num) throws IOException, PatchDefinitionException {
		BufferedReader in = new BufferedReader(new FileReader("tst/testNMIn" + num + ".xml"));
		PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter("tst/testNMActual" + num + ".pch")));
		String s;
		StringBuffer sb = new StringBuffer();
		while ((s = in.readLine()) != null) {
			sb.append(s + "\n");
		}
		in.close();
		NordModularPatch nm = new NordModularPatch("tst/testNMIn" + num + ".xml");
		nm.fromXML(sb.toString());
		if (nm.isValid() == false) {
			System.out.println("input file " + num + " did not contain valid XML: " /*+ nl2.getInvalidMsg()*/);
			assertTrue(false);
		}
		nm.writePatchFile(out);
		out.close();

		String sActual, sExpected;
		int i = 0;
		BufferedReader actual = new BufferedReader(new FileReader("tst/testNMActual" + num + ".pch"));
		BufferedReader expected = new BufferedReader(new FileReader("tst/testNMExpected" + num + ".pch"));
		while ((sActual = actual.readLine()) != null) {
			i++;
//			System.out.println(sActual);
			if ((sExpected = expected.readLine()) == null || sActual.equals(sExpected) == false) {
				System.out.println("line " + i + " did not match");
				if (sExpected == null) {
					System.out.println("expected was null");
				} else {
					System.out.println("expected = /" + sExpected + "/");
				}
				System.out.println("actual   = /" + sActual + "/");
				assertTrue(false);
			}
		}
		actual.close();
		expected.close();
	}
}