
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
 * JUnit tests for XMLReader
 *
 * @author Kenneth L. Martinez
 */

//import java.util.*;
import junit.framework.*;

public class XMLReaderTest extends TestCase {

	public XMLReaderTest(String name) {
		super(name);
	}

//	  public static void main(String[] args) {
//		  junit.textui.TestRunner.run(suite());
//	  }

	public static Test suite() {
		return new TestSuite(XMLReaderTest.class);
	}

	public void testConvertInputEscapedChars() {
		String s;
		s = XMLReader.convertInputEscapedChars("");
		assertTrue(s.equals(""));
		s = XMLReader.convertInputEscapedChars("hi there");
		assertTrue(s.equals("hi there"));
		s = XMLReader.convertInputEscapedChars("a&amp;b");
		assertTrue(s.equals("a&b"));
		s = XMLReader.convertInputEscapedChars("&amp; &amp;");
		assertTrue(s.equals("& &"));
		s = XMLReader.convertInputEscapedChars("&gt;&lt;");
		assertTrue(s.equals("><"));
	}

	public void testConvertOutputEscapedChars() {
		String s;
		s = XMLReader.convertOutputEscapedChars("");
		assertTrue(s.equals(""));
		s = XMLReader.convertOutputEscapedChars("hi there");
		assertTrue(s.equals("hi there"));
		s = XMLReader.convertOutputEscapedChars("a&b");
		assertTrue(s.equals("a&amp;b"));
		s = XMLReader.convertOutputEscapedChars("& &");
		assertTrue(s.equals("&amp; &amp;"));
		s = XMLReader.convertOutputEscapedChars("><");
		assertTrue(s.equals("&gt;&lt;"));
	}
}
