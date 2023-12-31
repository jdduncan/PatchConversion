
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

import java.text.*;

public class Util {

	/**
	 * Convert value between range low-hi into percentage.  Note that value
	 * and/or range can be bipolar.
	 */
	static String parmToPct(int parm, int low, int hi) {
		double d;
		DecimalFormat df = new DecimalFormat("#.####");
		if (low < 0 && parm < 0) {
			d = parm * -100.0 / low;
		} else {
			d = parm * 100.0 / hi;
		}
		return df.format(d);
	}

	/**
	 * Convert percentage into value between range low-hi.  Note that value
	 * and/or range can be bipolar.
	 */
	static int pctToParm(String pct, int low, int hi) {
		double d;
		try {
			d = new Double(pct).doubleValue();
			return pctToParm(d, low, hi);
		} catch (NumberFormatException e) {
			return 0; // FIXME shouldn't this return an error?
		}
	}

	/**
	 * Convert percentage into value between range low-hi.  Note that value
	 * and/or range can be bipolar.
	 */
	static int pctToParm(double d, int low, int hi) {
		if (low < 0 && d < 0) {
			d = (d * low + 50) / -100;
		} else {
			d = (d * hi + 50) / 100;
		}
		return new Double(d).intValue();
	}

	/**
	 * Convert value from one linear range to another.  Note that value
	 * and/or range can be bipolar, although bipolar range is expected to
	 * be more or less symmetrical (e.g. -50 to 50, or -64 to 63).
	 */
	static String rangeConvert(String value, double fromLow, double fromHi,
			double toLow, double toHi) {
		double d;
		try {
			d = new Double(value).doubleValue();
			return rangeConvert(d, fromLow, fromHi, toLow, toHi);
		} catch (NumberFormatException e) {
			return "0"; // FIXME shouldn't this return an error?
		}
	}

	/**
	 * Convert value from one linear range to another.  Note that value
	 * and/or range can be bipolar, although bipolar range is expected to
	 * be more or less symmetrical (e.g. -50 to 50, or -64 to 63).
	 */
	static String rangeConvert(double d, double fromLow, double fromHi,
			double toLow, double toHi) {
		DecimalFormat df = new DecimalFormat("#.####");
		double convValue, pct;

		if ((fromLow <= 0 && fromHi <= 0) || (fromLow >= 0 && fromHi >= 0)) {
			if ((toLow <= 0 && toHi <= 0) || (toLow >= 0 && toHi >= 0)) {
				convValue = ((d - fromLow) * (toHi - toLow) / (fromHi - fromLow)) + toLow;
				return df.format(convValue);
			} else {
				System.out.println("Error: Util.rangeConvert can't handle unipolar to bipolar conversion");
				int i = 1 / 0; // abort - can't handle unipolar to bipolar
			}
		} else if ((toLow <= 0 && toHi <= 0) || (toLow >= 0 && toHi >= 0)) {
			System.out.println("Error: Util.rangeConvert can't handle bipolar to unipolar conversion");
			int i = 1 / 0; // abort - can't handle bipolar to unipolar
		}

		if (fromLow < 0) {
			if (d < 0) {
				pct = d / fromLow * -1;
			} else {
				pct = d / fromHi;
			}
		} else if (fromHi < 0) { // for ranges like 100 to -100
			if (d < 0) {
				pct = d / fromHi * -1;
			} else {
				pct = d / fromLow;
			}
		} else {
			pct = (d - fromLow) / (fromHi - fromLow);
		}
		if (toLow < 0) {
			if (pct < 0) {
				convValue = pct * toLow * -1;
			} else {
				convValue = pct * toHi;
			}
		} else if (toHi < 0) { // for ranges like 100 to -100
			if (pct < 0) {
				convValue = pct * toHi * -1;
			} else {
				convValue = pct * toLow;
			}
		} else {
			convValue = (pct * (toHi - toLow)) + toLow;
		}
		if (convValue != 0 && ((fromLow < 0 && toLow > 0) || (fromLow > 0 && toLow < 0))) {
			convValue *= -1;
		}
		return df.format(convValue);

// This didn't handle ranges like -64 to 63 properly
//		double fromDiff = fromHi - fromLow;
//		double toDiff = toHi - toLow;
//		double convValue = ((d - fromLow) * toDiff / fromDiff) + toLow;
//		DecimalFormat df = new DecimalFormat("#.####");
//		return df.format(convValue);
	}

	/**
	 * Find the number (passed as a string) in the table and return that entry's
	 * index, or return the index of the entry whose value is closest.
	 * 
	 * Performs a binary search, so table must be in ascending order to give
	 * correct results.
	 */
	static int matchToNumberTable(String s, String convTbl[]) {
		double dTbl, dTbl2, dVal = new Double(s).doubleValue();
		int i;
		int step = convTbl.length / 2;
//		int cont = convTbl.length + 1;

		// binary search
		i = step;
//		while (cont != 0) {
		while (true) {
			dTbl = new Double(convTbl[i]).doubleValue();
			if (dVal == dTbl) {
				return i;
			}
			step /= 2;
			if (step == 0) {
				step = 1;
			}
			if (dVal > dTbl) {
				if (i == convTbl.length - 1) { // greater than highest entry
					return i;
				}
				dTbl2 = new Double(convTbl[i + 1]).doubleValue();
				if (dVal < dTbl2) { // in between this entry and the higher one
					if (dTbl2 - dVal < dVal - dTbl) {
						return i + 1; // value is closer to the higher one
					} else {
						return i;
					}
				}
				i += step;
			} else {
				if (i == 0) { // less than lowest entry
					return i;
				}
				dTbl2 = new Double(convTbl[i - 1]).doubleValue();
				if (dVal > dTbl2) { // in between this entry and the lower one
					if (dTbl - dVal < dVal - dTbl2) {
						return i; // value is closer to the higher one
					} else {
						return i - 1;
					}
				}
				i -= step;
			}
//			cont /= 2;
		}

//		for (int i = 0; i < convTbl.length; i++) {
//			if (convTbl[i].equalsIgnoreCase(s)) {
//				return i;
//			}
//		}
//		return -1;
	}

	/**
	 * Receive string with unformatted XML and produce formatted
	 * XML having line feeds and indentation
	 */
	static String formatXML(String xml, int indentLevel) {
		StringBuffer sb = new StringBuffer();
		String tag[], subTag[], startTag;
		int i, start, end;
		String ls = System.getProperty("line.separator");
		StringBuffer sbi = new StringBuffer("");
		for (i = 0; i < indentLevel; i++) {
			sbi.append("  ");
		}

		start = xml.indexOf("<?xml");
		if (start != -1) {
			end = xml.indexOf("?>");
			sb.append(xml.substring(start, end + 2) + ls);
			sb.append(formatXML(xml.substring(end + 2), indentLevel));
		} else {
			XMLReader xr = new XMLReader(xml);
			while ((tag = xr.getNextTag()) != null) {
				if (tag[2] == null) {
					startTag = tag[0];
				} else {
					startTag = tag[0] + " " + tag[2];
				}
				if (new XMLReader(tag[1]).getNextTag() == null) {
					sb.append(sbi + "<" + startTag + ">" + XMLReader.convertOutputEscapedChars(tag[1]) +
							"</" + tag[0] + ">" + ls);
				} else {
					// print nested group of tags
					sb.append(sbi + "<" + startTag + ">" + ls);
					sb.append(formatXML(tag[1], indentLevel + 1));
					sb.append(sbi + "</" + tag[0] + ">" + ls);
				}
			}
		}

		return sb.toString();
	}
}
