
/* Synth Patch Conversion for Clavia Nord Lead 2 programs
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
 * JUnit tests for NordLead2Prog
 *
 * @author Kenneth L. Martinez
 */

import java.io.*;
import java.util.*;
import junit.framework.*;

public class NordLead2ProgTest extends TestCase {

    public NordLead2ProgTest(String name) {
        super(name);
    }

//    public static void main(String[] args) {
//        junit.textui.TestRunner.run(suite());
//    }

    public static Test suite() {
        return new TestSuite(NordLead2ProgTest.class);
    }

    public void testSysexToData() {
        byte dat1[] = { (byte)0, (byte)0x0A, (byte)0x0B };
        byte expected1[] = { (byte)0xBA };
        byte actual1[] = NordLead2Prog.sysexToData(dat1, 1, 2);
        assertTrue(Arrays.equals(actual1, expected1));
    }

	public void testDataToSysex() {
		byte dat1[] = { (byte)0xBA, (byte)0x12 };
		byte hdr1[] = { (byte)0xF0, (byte)0x33, (byte)0x0F, (byte)0x04 };
		byte expected1[] = { (byte)0xF0, (byte)0x33, (byte)0x0F, (byte)0x04,
				(byte)0x0A, (byte)0x0B, (byte)2, (byte)1, (byte)0xF7 };
		byte actual1[] = NordLead2Prog.dataToSysex(dat1, hdr1, 4);
		assertTrue(Arrays.equals(actual1, expected1));
	}

	public void testToSysex() throws IOException, PatchDefinitionException  {
		File inputFile = new File("tst/testNL2In1.syx");
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(inputFile));
		int fileLen = (int)inputFile.length();
		byte sysex[] = new byte[fileLen];
		in.read(sysex, 0, fileLen);
		in.close();
		NordLead2Prog nl2 = new NordLead2Prog();
		nl2.fromSysex(sysex);
		nl2.toSysex();
		assertTrue(Arrays.equals(nl2.getSysex(), sysex));
	}

    public void testToXML1() throws IOException, PatchDefinitionException {
    	fileToXML(1);
	}

	public void testFromXML1() throws IOException, PatchDefinitionException {
		fileFromXML(1);
    }

	public void testToXML2() throws IOException, PatchDefinitionException {
		fileToXML(2);
	}

	public void testFromXML2() throws IOException, PatchDefinitionException {
		fileFromXML(2);
	}

	public void testToXML3() throws IOException, PatchDefinitionException {
		fileToXML(3);
	}

	public void testFromXML3() throws IOException, PatchDefinitionException {
		fileFromXML(3);
	}

	public void testToXML4() throws IOException, PatchDefinitionException {
		fileToXML(4);
	}

	public void testFromXML4() throws IOException, PatchDefinitionException {
		fileFromXML(4);
	}

	public void testToXML5() throws IOException, PatchDefinitionException {
		fileToXML(5);
	}

	public void testFromXML5() throws IOException, PatchDefinitionException {
		fileFromXML(5);
	}

	public void testToXML6() throws IOException, PatchDefinitionException {
		fileToXML(6);
	}

	public void testFromXML6() throws IOException, PatchDefinitionException {
		fileFromXML(6);
	}

	public void testFromXMLGeneric1() throws IOException, PatchDefinitionException {
		fileFromXMLGeneric(1);
	}

	public void testFromXMLGeneric2() throws IOException, PatchDefinitionException {
		fileFromXMLGeneric(2);
	}

	public void testFromXMLGeneric3() throws IOException, PatchDefinitionException {
		fileFromXMLGeneric(3);
	}

	public void testFromXMLGeneric4() throws IOException, PatchDefinitionException {
		fileFromXMLGeneric(4);
	}

	public void testFromXMLGeneric6() throws IOException, PatchDefinitionException {
		fileFromXMLGeneric(6);
	}

	public void fileFromXMLGeneric(int num) throws IOException, PatchDefinitionException  {
		BufferedReader in = new BufferedReader(new FileReader("tst/testNL2GenericIn" + num + ".xml"));
		PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter("tst/testNL2GenericActual" + num + ".xml")));
		String s;
		StringBuffer sb = new StringBuffer();
		while ((s = in.readLine()) != null) {
			sb.append(s + "\n");
		}
		in.close();
		NordLead2Prog nl2 = new NordLead2Prog();
		nl2.fromXML(sb.toString());
		if (nl2.isValid() == false) {
			System.out.println("input file did not contain valid XML: " + nl2.getInvalidMsg());
			assertTrue(false);
		}
		out.print(Util.formatXML(nl2.toXML(), -1));
		out.close();

		String sActual, sExpected;
		int i = 0;
		BufferedReader actual = new BufferedReader(new FileReader("tst/testNL2GenericActual" + num + ".xml"));
		BufferedReader expected = new BufferedReader(new FileReader("tst/testNL2GenericExpected" + num + ".xml"));
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

	public void fileToXML(int num) throws IOException, PatchDefinitionException  {
		File inputFile = new File("tst/testNL2In" + num + ".syx");
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(inputFile));
		int fileLen = (int)inputFile.length();
		PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter("tst/testNL2Actual" + num + ".xml")));
		NordLead2Prog nl2 = new NordLead2Prog();
		byte sysex[] = new byte[fileLen];
		in.read(sysex, 0, fileLen);

		if (nl2.matchSysex(sysex)) {
			nl2.fromSysex(sysex);
			try {
				BufferedReader info = new BufferedReader(new FileReader(("tst/testNL2In" + num + ".syx.info")));
				String s;
				StringBuffer sb = new StringBuffer();
				while ((s = info.readLine()) != null) {
					sb.append(s + System.getProperty("line.separator"));
				}
				nl2.getGenPatch().readInfoXML(sb.toString());
				info.close();
			} catch (FileNotFoundException e) {
				// This is not an error
			};
			out.print(Util.formatXML(nl2.toXML(), -1));
			in.close();
			out.close();
			String sActual, sExpected;
			int i = 0;
			BufferedReader actual = new BufferedReader(new FileReader("tst/testNL2Actual" + num + ".xml"));
			BufferedReader expected = new BufferedReader(new FileReader("tst/testNL2Expected" + num + ".xml"));
			while ((sActual = actual.readLine()) != null) {
				i++;
//				System.out.println(sActual);
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
		} else {
			in.close();
			out.close();
			assertTrue(false);
		}
	}

	public void fileFromXML(int num) throws IOException, PatchDefinitionException {
		BufferedReader in = new BufferedReader(new FileReader("tst/testNL2Expected" + num + ".xml"));
		PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter("tst/testNL2Actual" + num + "b.xml")));
		String s;
		StringBuffer sb = new StringBuffer();
		while ((s = in.readLine()) != null) {
			sb.append(s + "\n");
		}
		in.close();
		NordLead2Prog nl2 = new NordLead2Prog();
		nl2.fromXML(sb.toString());
		out.print(Util.formatXML(nl2.toXML(), -1));
		out.close();
//		FileOutputStream out2 = new FileOutputStream("tst/testNL2Actual" + num + ".syx");
//		out2.write(nl2.getSysex());
//		out2.close();

		String sActual, sExpected;
		int i = 0;
		BufferedReader actual = new BufferedReader(new FileReader("tst/testNL2Actual" + num + "b.xml"));
		BufferedReader expected = new BufferedReader(new FileReader("tst/testNL2Expected" + num + ".xml"));
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