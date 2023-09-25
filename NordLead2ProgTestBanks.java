
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
 * JUnit tests for NordLead2Prog, processing large groups of sysex: read in
 * sysex, convert to XML, convert back to sysex and compare to the original sysex
 *
 * @author Kenneth L. Martinez
 */

import java.io.*;
import java.util.*;
import junit.framework.*;

public class NordLead2ProgTestBanks extends TestCase {

	public NordLead2ProgTestBanks(String name) {
		super(name);
	}

//	  public static void main(String[] args) {
//		  junit.textui.TestRunner.run(suite());
//	  }

	public static Test suite() {
		return new TestSuite(NordLead2ProgTestBanks.class);
	}

	public void testLib3Bank1Generic() throws IOException, PatchDefinitionException {
		filesToXML("data/NL2/n2lib3b1", 99, "generic", "n2lib3b1-");
	}

	public void testInternal() throws IOException, PatchDefinitionException {
		filesToXML("data/NL2/nl2internal", 40, "source", "pgm");
	}

	public void testUser() throws IOException, PatchDefinitionException {
		filesToXML("data/NL2/nl2user", 99, "source", "pgm");
	}

	public void testLib1Bank1() throws IOException, PatchDefinitionException {
		filesToXML("data/NL2/n2lib1b1", 99, "source", "n2lib1b1-");
	}

	public void testLib1Bank2() throws IOException, PatchDefinitionException {
		filesToXML("data/NL2/n2lib1b2", 99, "source", "n2lib1b2-");
	}

	public void testLib1Bank3() throws IOException, PatchDefinitionException {
		filesToXML("data/NL2/n2lib1b3", 99, "source", "n2lib1b3-");
	}

	public void testLib2Bank1() throws IOException, PatchDefinitionException {
		filesToXML("data/NL2/n2lib2b1", 99, "source", "n2lib2b1-");
	}

	public void testLib2Bank2() throws IOException, PatchDefinitionException {
		filesToXML("data/NL2/n2lib2b2", 99, "source", "n2lib2b2-");
	}

	public void testLib2Bank3() throws IOException, PatchDefinitionException {
		filesToXML("data/NL2/n2lib2b3", 99, "source", "n2lib2b3-");
	}

	public void testLib3Bank1() throws IOException, PatchDefinitionException {
		filesToXML("data/NL2/n2lib3b1", 99, "source", "n2lib3b1-");
	}

	public void testLib3Bank2() throws IOException, PatchDefinitionException {
		filesToXML("data/NL2/n2lib3b2", 99, "source", "n2lib3b2-");
	}

	public void testLib3Bank3() throws IOException, PatchDefinitionException {
		filesToXML("data/NL2/n2lib3b3", 99, "source", "n2lib3b3-");
	}

	public void testLib4Bank1() throws IOException, PatchDefinitionException {
		filesToXML("data/NL2/n2lib4b1", 99, "source", "n2lib4b1-");
	}

	public void testLib4Bank2() throws IOException, PatchDefinitionException {
		filesToXML("data/NL2/n2lib4b2", 99, "source", "n2lib4b2-");
	}

	public void testLib4Bank3() throws IOException, PatchDefinitionException {
		filesToXML("data/NL2/n2lib4b3", 99, "source", "n2lib4b3-");
	}

	public void filesToXML(String path, int fileCnt, String type, String prefix) throws IOException, PatchDefinitionException {
		File inputFile;
		BufferedInputStream in;
		PrintWriter out;
		int i, fileLen;
		byte sysex[], sysexConv[];
		NordLead2Prog nl2;
		BufferedReader xmlIn;
		String s, sGen;
		StringBuffer sb;
		boolean skip = false, genFailed = false;

		System.out.println("---- Processing " + path + " " + type + " -----------------------");
		for (i = 1; i <= fileCnt; i++) {
			if (i < 10) {
				inputFile = new File(path + "/" + prefix + "0" + i + ".syx");
			} else {
				inputFile = new File(path + "/" + prefix + i + ".syx");
			}
			in = new BufferedInputStream(new FileInputStream(inputFile));
			fileLen = (int)inputFile.length();
			nl2 = new NordLead2Prog();
			sysex = new byte[fileLen];
			in.read(sysex, 0, fileLen);
			in.close();

			nl2.fromSysex(sysex);
			if (nl2.isValid()) {
				if (i < 10) {
					out = new PrintWriter(new BufferedWriter(
							new FileWriter(path + "/" + prefix + "0" + i + ".xml")));
				} else {
					out = new PrintWriter(new BufferedWriter(
							new FileWriter(path + "/" + prefix + i + ".xml")));
				}
				out.print(Util.formatXML(nl2.toXML(), -1));
				sGen = nl2.genPatch.writeXML();
				out.close();

				if (i < 10) {
					xmlIn = new BufferedReader(new FileReader(path + "/" + prefix + "0" + i + ".xml"));
				} else {
					xmlIn = new BufferedReader(new FileReader(path + "/" + prefix + i + ".xml"));
				}
				sb = new StringBuffer();
				while ((s = xmlIn.readLine()) != null) {
					if (type.equalsIgnoreCase("generic")) {
						if (skip) {
							if (s.equalsIgnoreCase("</source_patch>")) {
								skip = false;
							}
						} else if (s.equalsIgnoreCase("<source_patch>")) {
							skip = true;
						} else {
							sb.append(s + "\n");
						}
					} else {
						sb.append(s + "\n");
					}
				}
				in.close();
				nl2 = new NordLead2Prog();
				nl2.fromXML(sb.toString());
//				out = new PrintWriter(new BufferedWriter(
//						new FileWriter(path + "/converted.xml")));
				if (i < 10) {
					out = new PrintWriter(new BufferedWriter(
							new FileWriter(path + "/" + prefix + "0" + i + "conv.xml")));
				} else {
					out = new PrintWriter(new BufferedWriter(
							new FileWriter(path + "/" + prefix + i + "conv.xml")));
				}
				out.print(Util.formatXML(nl2.toXML(), -1));
				out.close();
				if (type.equalsIgnoreCase("generic")) {
//					sGen = XMLReader.getTagValue(sb.toString(), "generic_patch");
					// FIXME had to execute toXML() so last connections would be built;
					//  seems like a kludge?
					s = nl2.genPatch.writeXML();
					if (sGen.equals(s) == false) {
						System.out.println("program #" + i + " did not match");
//						out = new PrintWriter(new BufferedWriter(
//								new FileWriter(path + "/converted.xml")));
//						nl2.toXML(out);
//						out.close();
//						assertTrue(false);
						genFailed = true;
					}
				} else {
					sysexConv = nl2.getSysex();
					if (Arrays.equals(sysexConv, sysex) == false) {
						System.out.println("program #" + i + " did not match");
						out = new PrintWriter(new BufferedWriter(
								new FileWriter(path + "/converted.xml")));
						out.print(Util.formatXML(nl2.toXML(), -1));
						out.close();
						assertTrue(false);
					}
				}
				// FIXME this compare is invalid: generic program won't contain unused
				//  modules and so can't mirror their original values; need to compare
				//  to original's generic XML instead
			} else {
				System.out.println("program #" + i + " did not contain valid sysex");
			}
		}
		if (genFailed) {
			assertTrue(false);
		}
	}
}