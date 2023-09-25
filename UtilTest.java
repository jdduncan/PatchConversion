
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
 * JUnit tests for Util
 *
 * @author Kenneth L. Martinez
 */

//import java.util.*;
import junit.framework.*;

public class UtilTest extends TestCase {

	public UtilTest(String name) {
		super(name);
	}

//	  public static void main(String[] args) {
//		  junit.textui.TestRunner.run(suite());
//	  }

	public static Test suite() {
		return new TestSuite(UtilTest.class);
	}

	public void testParmToPct() {
		String actual;
		actual = Util.parmToPct(0, 0, 127);
		assertTrue(actual.equals("0"));
		actual = Util.parmToPct(127, 0, 127);
		assertTrue(actual.equals("100"));
		actual = Util.parmToPct(64, 0, 127);
		assertTrue(actual.equals("50.3937"));
		actual = Util.parmToPct(10, -64, 63);
		assertTrue(actual.equals("15.873"));
		actual = Util.parmToPct(-32, -64, 63);
		assertTrue(actual.equals("-50"));
	}

	public void testPctToParm() {
		int actual;
		actual = Util.pctToParm("0", 0, 127);
		assertTrue(actual == 0);
		actual = Util.pctToParm("100", 0, 127);
		assertTrue(actual == 127);
		actual = Util.pctToParm("50.3937", 0, 127);
		assertTrue(actual == 64);
		actual = Util.pctToParm("15.873", -64, 63);
		assertTrue(actual == 10);
		actual = Util.pctToParm("-50", -64, 63);
		assertTrue(actual == -32);
	}

	public void testRangeConvert() {
		String s;
		s = Util.rangeConvert("0", 0, 127, 50, 99);
		assertTrue(s.equals("50"));
		s = Util.rangeConvert("127", 0, 127, 50, 99);
		assertTrue(s.equals("99"));
		s = Util.rangeConvert("50", 50, 99, 0, 127);
		assertTrue(s.equals("0"));
		s = Util.rangeConvert("99", 50, 99, 0, 127);
		assertTrue(s.equals("127"));
		s = Util.rangeConvert("64", 0, 127, 50, 99);
		assertTrue(s.equals("74.6929"));
		s = Util.rangeConvert("74.6929", 50, 99, 0, 127);
		assertTrue(s.equals("64"));
		s = Util.rangeConvert(25, 0, 100, 100, 0);
		assertTrue(s.equals("75"));
		s = Util.rangeConvert(75, 0, 100, 100, 0);
		assertTrue(s.equals("25"));
		s = Util.rangeConvert(20, -100, 100, -50, 50);
		assertTrue(s.equals("10"));
		s = Util.rangeConvert(10, -50, 50, -100, 100);
		assertTrue(s.equals("20"));
		s = Util.rangeConvert(25, -100, 100, 100, -100);
		assertTrue(s.equals("-25"));
		s = Util.rangeConvert(-25, 100, -100, -100, 100);
		assertTrue(s.equals("25"));
		s = Util.rangeConvert(50, -100, 100, -128, 127);
		assertTrue(s.equals("63.5"));
		s = Util.rangeConvert(-50, -100, 100, -128, 127);
		assertTrue(s.equals("-64"));
		s = Util.rangeConvert(63.5, -128, 127, -100, 100);
		assertTrue(s.equals("50"));
		s = Util.rangeConvert(-64, -128, 127, -100, 100);
		assertTrue(s.equals("-50"));
	}

	public void testMatchToNumberTable() {
		int i;

		String tbl1[] = {
			"0.5", "0.6", "0.8", "1.0",   "1.3", "1.8", "2.2", "3.0"
		};
		i = Util.matchToNumberTable("0.5", tbl1);
		assertTrue(i == 0);
		i = Util.matchToNumberTable("0.6", tbl1);
		assertTrue(i == 1);
		i = Util.matchToNumberTable("1.0", tbl1);
		assertTrue(i == 3);
		i = Util.matchToNumberTable("1.8", tbl1);
		assertTrue(i == 5);
		i = Util.matchToNumberTable("3.0", tbl1);
		assertTrue(i == 7);
		i = Util.matchToNumberTable("0.4", tbl1);
		assertTrue(i == 0);
		i = Util.matchToNumberTable("0.51", tbl1);
		assertTrue(i == 0);
		i = Util.matchToNumberTable("0.55", tbl1);
		assertTrue(i == 1);
		i = Util.matchToNumberTable("0.69", tbl1);
		assertTrue(i == 1);
		i = Util.matchToNumberTable("1.1", tbl1);
		assertTrue(i == 3);
		i = Util.matchToNumberTable("2.61", tbl1);
		assertTrue(i == 7);
		i = Util.matchToNumberTable("4.0", tbl1);
		assertTrue(i == 7);

		String tbl2[] = {
			"0.5", "0.6", "0.8", "1.0",   "1.3", "1.8", "3.0"
		};
		i = Util.matchToNumberTable("0.5", tbl2);
		assertTrue(i == 0);
		i = Util.matchToNumberTable("0.6", tbl2);
		assertTrue(i == 1);
		i = Util.matchToNumberTable("1.0", tbl1);
		assertTrue(i == 3);
		i = Util.matchToNumberTable("1.8", tbl2);
		assertTrue(i == 5);
		i = Util.matchToNumberTable("3.0", tbl2);
//		System.out.println(i);
		assertTrue(i == 6);
		i = Util.matchToNumberTable("0.4", tbl2);
		assertTrue(i == 0);
		i = Util.matchToNumberTable("0.51", tbl2);
		assertTrue(i == 0);
		i = Util.matchToNumberTable("0.55", tbl2);
		assertTrue(i == 1);
		i = Util.matchToNumberTable("0.69", tbl2);
		assertTrue(i == 1);
		i = Util.matchToNumberTable("1.1", tbl2);
		assertTrue(i == 3);
		i = Util.matchToNumberTable("2.41", tbl2);
		assertTrue(i == 6);
		i = Util.matchToNumberTable("4.0", tbl2);
		assertTrue(i == 6);
	}

	public void testFormatXML() {
		String ls = System.getProperty("line.separator");
		String in1 = "<t1>";
		String actual1 = Util.formatXML(in1, 0);
		assertTrue(actual1.equals(""));
		String in2 = "<t1 attr=\"yes\"><t2>hi</t2></t1>";
		String actual2 = Util.formatXML(in2, 0);
		assertTrue(actual2.equals("<t1 attr=\"yes\">" + ls + "  <t2>hi</t2>" + ls + "</t1>" + ls));
		String in3 = "<t1><t2><t3a>hi</t3a><t3b>there</t3b></t2></t1>";
		String actual3 = Util.formatXML(in3, -1);
		assertTrue(actual3.equals("<t1>" + ls + "<t2>" + ls + "  <t3a>hi</t3a>" + ls +
				"  <t3b>there</t3b>" + ls + "</t2>" + ls + "</t1>" + ls));
		String in4 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><t1><t2><t3a>hello</t3a><t3b>there</t3b></t2></t1>";
		String actual4 = Util.formatXML(in4, 0);
		assertTrue(actual4.equals("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + ls +
				"<t1>" + ls + "  <t2>" + ls + "    <t3a>hello</t3a>" + ls +
				"    <t3b>there</t3b>" + ls + "  </t2>" + ls + "</t1>" + ls));
	}
}