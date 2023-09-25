
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
 * Convert Nord Lead 2 program sysex to XML or vice versa
 * 
 * Fix soon:
 * - n2lib1b1-06: XML has Tremolo even though it's not used >>> Fixed with
 *   code in toGeneric(); is there a cleaner way?
 * - Notch+LP filter: make into 2 filters in series >>> IN PROGRESS
 *   Resonance: remove from notch, reduce amount for LP?
 * - add parm for PB range (global - how to handle?)
 * - add parms noise "color", "sync"; filt. distortion; ring mod; add/fix
 *   mod wheel dests; arpeggiator
 * - Unison detune: 1=2.5 cents, 2=5, 3=10, 4=13, 5=17, 6=20, 7=23, 8=25, 9=28
 * 
 * Source Patch version history:
 * 1.01 Handle undefined values in sysex.  Added <edit_buffer>
 * 1.00 First release
 * 
 * Generic Patch version history:
 * 1.10 Moved program gain (and its morph) from final VCA to Audio Out.
 * 1.09 Converted parm morphs to separate parms and jacks.
 *      If Mod Env amount is negative, allow attenuator parm to have negative
 *      value rather than inverting Mod Env polarity.  (Polarity change made it
 *      easier for NordModularPatch to implement, but that converter should be
 *      made to handle negative amounts instead.)
 * 1.08 Changed Tremolo from vca to mod_vca (and eliminated paired Constant)
 * 1.07 First release
 *
 * @author Kenneth L. Martinez
 */

import java.io.*;
import java.text.*;

public class NordLead2Prog extends SynthPatchAbstract {
	// byte 3 = global channel, 5 = bank, 6 = program
	static final byte[] SYSEX_HDR = { (byte)0xF0, (byte)0x33, (byte)0x0F, (byte)0x04, 0, 0 };
	static final byte[] INIT_SYSEX = {
		(byte)0xF0, (byte)0x33, (byte)0x0F, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x0C, (byte)0x03,
		(byte)0x03, (byte)0x04, (byte)0x09, (byte)0x03, (byte)0x0D, (byte)0x03, (byte)0x00, (byte)0x00,
		(byte)0x0A, (byte)0x03, (byte)0x08, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0C, (byte)0x00,
		(byte)0x0E, (byte)0x04, (byte)0x02, (byte)0x01, (byte)0x00, (byte)0x07, (byte)0x02, (byte)0x01,
		(byte)0x00, (byte)0x01, (byte)0x0E, (byte)0x07, (byte)0x08, (byte)0x00, (byte)0x00, (byte)0x00,
		(byte)0x0D, (byte)0x03, (byte)0x01, (byte)0x01, (byte)0x08, (byte)0x04, (byte)0x00, (byte)0x04,
		(byte)0x08, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x0F, (byte)0x02, (byte)0x00, (byte)0x00,
		(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
		(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
		(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
		(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
		(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
		(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
		(byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,
		(byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x00,
		(byte)0x02, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
		(byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x00,
		(byte)0x04, (byte)0x00, (byte)0xF7
	};

    private int dataLen = 132;
	private String morphControl = "Key Velocity";

	static final String ENV_ATTACK[] = {
		//Sampling of values:
		// 0=0.0005, 32=0.0053, 41=0.015, 49=0.047, 64=0.17, 84=0.676, 102=2.8, 120=11.7
		// 127=16.8
		"0.0005", "0.0006", "0.0008", "0.001", "0.0011", "0.0012", "0.0014", "0.0016",
		"0.0017", "0.0018", "0.002", "0.0022", "0.0023", "0.0024", "0.0026", "0.0028",
		"0.0029", "0.003", "0.0032", "0.0034", "0.0035", "0.0037", "0.0038", "0.004",
		"0.0041", "0.0042", "0.0044", "0.0046", "0.0047", "0.0048", "0.005", "0.0052",
		"0.0053", "0.0064", "0.0075", "0.0085", "0.0096", "0.0107", "0.0118", "0.0128",
		"0.0139", "0.015", "0.019", "0.023", "0.027", "0.031", "0.035", "0.039",
		"0.043", "0.047", "0.0552", "0.0634", "0.0716", "0.0798", "0.088", "0.0962",
		"0.1044", "0.1126", "0.1208", "0.129", "0.1372", "0.1454", "0.1536", "0.1618",
		"0.17", "0.1953", "0.2206", "0.2459", "0.2712", "0.2965", "0.3218", "0.3471",
		"0.3724", "0.3977", "0.423", "0.4483", "0.4736", "0.4989", "0.5242", "0.5495",
		"0.5748", "0.6001", "0.6254", "0.6507", "0.676", "0.794", "0.912", "1.03",
		"1.148", "1.266", "1.384", "1.502", "1.62", "1.738", "1.856", "1.974",
		"2.092", "2.21", "2.328", "2.446", "2.564", "2.682", "2.8", "3.2944",
		"3.7889", "4.2833", "4.7778", "5.2722", "5.7667", "6.2611", "6.7556", "7.25",
		"7.7444", "8.2389", "8.7333", "9.2278", "9.7222", "10.2167", "10.7111", "11.2056",
		"11.7", "12.4286", "13.1571", "13.8857", "14.6143", "15.3429", "16.0714", "16.8",
	};
	static final String ENV_DECAY[] = {
		//Sampling of values:
		// 0=0.0005, 24=0.047, 36=0.088, 48=0.17, 56=0.336, 64=0.676, 76=1.4, 88=2.8
		// 96=5.7, 108=11.7, 116=24, 127=45
		"0.0005", "0.0024", "0.0044", "0.0063", "0.0082", "0.0102", "0.0121", "0.0141",
		"0.016", "0.0179", "0.0199", "0.0218", "0.0238", "0.0257", "0.0276", "0.0296",
		"0.0315", "0.0334", "0.0354", "0.0373", "0.0392", "0.0412", "0.0431", "0.0451",
		"0.047", "0.0504", "0.0538", "0.0572", "0.0607", "0.0641", "0.0675", "0.0709",
		"0.0743", "0.0778", "0.0812", "0.0846", "0.088", "0.0948", "0.1017", "0.1085",
		"0.1153", "0.1222", "0.129", "0.1358", "0.1427", "0.1495", "0.1563", "0.1632",
		"0.17", "0.1908", "0.2115", "0.2322", "0.253", "0.2738", "0.2945", "0.3153",
		"0.336", "0.3785", "0.421", "0.4635", "0.506", "0.5485", "0.591", "0.6335",

		"0.676", "0.7363", "0.7967", "0.857", "0.9173", "0.9777", "1.038", "1.0983",
		"1.1587", "1.219", "1.2793", "1.3397", "1.4", "1.5167", "1.6333", "1.75",
		"1.8667", "1.9833", "2.1", "2.2167", "2.3333", "2.45", "2.5667", "2.6833",
		"2.8", "3.1625", "3.525", "3.8875", "4.25", "4.6125", "4.975", "5.3375",
		"5.7", "6.2", "6.7", "7.2", "7.7", "8.2", "8.7", "9.2",
		"9.7", "10.2", "10.7", "11.2", "11.7", "13.2375", "14.775", "16.3125",
		"17.85", "19.3875", "20.925", "22.4625", "24", "25.9091", "27.8182", "29.7273",
		"31.6364", "33.5455", "35.4545", "37.3636", "39.2727", "41.1818", "43.0909", "45",
	};
	// FIXME mod env max decay time is shorter with negative amt than positive (!)
	static final String MOD_ENV_AMT_OSC2[] = { // semitones (fraction is cents)
		// All values checked by ear using semitone & fine tune knobs.  (Values
		// at the extreme ranges were difficult to test.)
		"-130", "-126", "-122", "-118", "-114", "-110", "-106", "-102",
		"-98", "-94.5", "-91.125", "-87.75", "-84.5", "-81.25", "-78.125", "-75",
		"-72", "-69", "-66", "-63.25", "-60.5", "-57.75", "-55", "-52.5",
		"-50", "-47.5", "-45", "-42.75", "-40.5", "-38.25", "-36", "-34",
		"-32", "-30", "-28.1", "-26.25", "-24.5", "-22.75", "-21.25", "-19.5",
		"-18", "-16.5", "-15.2", "-13.75", "-12.5", "-11", "-10", "-9",
		"-8", "-7", "-6.125", "-5.25", "-4.5", "-3.875", "-3.125", "-2.5",
		"-2", "-1.5", "-1.125", "-.75", "-.5", "-.25", "-.125", "-.03125",
		"0", // 64=no mod
		".03125", ".125", ".25", ".5", ".75", "1.125", "1.5", "2",
		"2.5", "3.125", "3.875", "4.5", "5.25", "6.125", "7", "8",
		"9", "10", "11", "12.5", "13.75", "15.2", "16.5", "18",
		"19.5", "21.25", "22.75", "24.5", "26.25", "28.1", "30", "32",
		"34", "36", "38.25", "40.5", "42.75", "45", "47.5", "50",
		"52.5", "55", "57.75", "60.5", "63.25", "66", "69", "72",
		"75", "78.125", "81.25", "84.5", "87.75", "91.125", "94.5", "98",
		"102", "106", "110", "114", "118", "122", "126"
	};
	// FIXME convert to absolute value?  Other bipolar amts measured in real units,
	//  but this one shown in symmetrical half-amt.  (Note: NM is the same.)
	static final String LFO_AMT_OSC[] = {
		// Sampling of values: 16= +/-0.25; 26= +/-0.5; 32= +/-1 
		// 40= +/-2; 51= +/-4; 64= +/-8; 73= +/-12; 79= +/-15; 88= +/-21
		// 96= +/-27; 104= about 34; 112= 43; 120= about 53; 127= about 62
		"0", "0.0156", "0.0312", "0.0469", "0.0625", "0.0781", "0.0938", "0.1094",
		"0.125", "0.1406", "0.1562", "0.1719", "0.1875", "0.2031", "0.2188", "0.2344",
		"0.25", "0.275", "0.3", "0.325", "0.35", "0.375", "0.4", "0.425",
		"0.45", "0.475", "0.5", "0.5833", "0.6667", "0.75", "0.8333", "0.9167",
		"1", "1.125", "1.25", "1.375", "1.5", "1.625", "1.75", "1.875",
		"2", "2.1818", "2.3636", "2.5455", "2.7273", "2.9091", "3.0909", "3.2727",
		"3.4545", "3.6364", "3.8182", "4", "4.3077", "4.6154", "4.9231", "5.2308",
		"5.5385", "5.8462", "6.1538", "6.4615", "6.7692", "7.0769", "7.3846", "7.6923",

		"8", "8.4444", "8.8889", "9.3333", "9.7778", "10.2222", "10.6667", "11.1111",
		"11.5556", "12", "12.5", "13", "13.5", "14", "14.5", "15",
		"15.6667", "16.3333", "17", "17.6667", "18.3333", "19", "19.6667", "20.3333",
		"21", "21.75", "22.5", "23.25", "24", "24.75", "25.5", "26.25",
		"27", "27.875", "28.75", "29.625", "30.5", "31.375", "32.25", "33.125",
		"34", "35.125", "36.25", "37.375", "38.5", "39.625", "40.75", "41.875",
		"43", "44.25", "45.5", "46.75", "48", "49.25", "50.5", "51.75",
		"53", "54.2857", "55.5714", "56.8571", "58.1429", "59.4286", "60.7143", "62",
	};
	// LFO2 units seem to match LFO1 units
	// Filter by LFO units seem to match Osc units
	// pw by lfo1: max amt is 50% swing. Amt seems expo: 64=10%(?), 96=25%, 127=50%.
	//  Refuses to swing PW below 50%, so unless manual PW is properly set it will
	//  "max out" at 50% during part of the PWM cycle.
	// pw by mod env: positive amt adds linear mod between 0 and +50%.
	//  Negative actually seems to move the "limit" below 50%, e.g. pw by mod env amt
	//  of half (-25% actual mod) plus manual PW at 75% seems to have moved the hard
	//  limit down to 25%, so that bipolar LFO at full mod (50% actual) seems to
	//  now provide PWM between 25% and 75%
	// FM amount in general: low part of mod seems to "wrap around" to positive
	// amt.  This is obvious with square wave, where after 40% mod the lowest
	// osc1 freq goes up and sound ends up being a trill a couple octaves above
	// osc1 base freq.  How to model this?
	// FIXME fm amt by mod env: range is almost as much as fixed knob; maybe not
	//  quite as much.
	//  Negative provides same range as positive, and does provide a negative signal
	//  amt (which can zero out when applied against an equal amount of positive mod
	//  from the fixed FM knob and/or the LFO).
	// FIXME amp by lfo2: subtracts from volume in even amounts: 64=half- to full-volume;
	//  127=off to full.  (So, does not act like regular bipolar modulator.)
	static final String LFO_RATE[] = {
		//Sampling of values:
		// 0=0.0769, 4=0.1, 8=0.125, 16=0.19, 32=0.5, 48=1.28, 64=3.23, 80=8.14
		// 96=18.3, 112=31, 120=43.7, 124=56, 127=65.4
		"0.0769", "0.0827", "0.0884", "0.0942", "0.1", "0.1063", "0.1125", "0.1188",
		"0.125", "0.1331", "0.1412", "0.1494", "0.1575", "0.1656", "0.1738", "0.1819",
		"0.19", "0.2094", "0.2288", "0.2481", "0.2675", "0.2869", "0.3062", "0.3256",
		"0.345", "0.3644", "0.3838", "0.4031", "0.4225", "0.4419", "0.4612", "0.4806",
		"0.5", "0.5488", "0.5975", "0.6462", "0.695", "0.7438", "0.7925", "0.8412",
		"0.89", "0.9388", "0.9875", "1.0362", "1.085", "1.1338", "1.1825", "1.2313",
		"1.28", "1.4019", "1.5238", "1.6456", "1.7675", "1.8894", "2.0112", "2.1331",
		"2.255", "2.3769", "2.4988", "2.6206", "2.7425", "2.8644", "2.9862", "3.1081",

		"3.23", "3.5369", "3.8438", "4.1506", "4.4575", "4.7644", "5.0712", "5.3781",
		"5.685", "5.9919", "6.2988", "6.6056", "6.9125", "7.2194", "7.5263", "7.8331",
		"8.14", "8.775", "9.41", "10.045", "10.68", "11.315", "11.95", "12.585",
		"13.22", "13.855", "14.49", "15.125", "15.76", "16.395", "17.03", "17.665",
		"18.3", "19.0938", "19.8875", "20.6812", "21.475", "22.2688", "23.0625", "23.8562",
		"24.65", "25.4438", "26.2375", "27.0312", "27.825", "28.6188", "29.4125", "30.2062",
		"31", "32.5875", "34.175", "35.7625", "37.35", "38.9375", "40.525", "42.1125",
		"43.7", "46.775", "49.85", "52.925", "56", "59.1333", "62.2667", "65.4",
	};
	static final String LINEAR_FM_AMT[] = {
		// had to guess after 72 semitones
		//Sampling of values:
		// 0=0, 5=1, 11=2, 21=4, 29=7, 39=12, 57=24, 72=36
		// 85=48, 98=60, 105=72, 111=84, 116=96, 120=108, 124=120, 127=127
		"0", "0.2", "0.4", "0.6", "0.8", "1", "1.1667", "1.3333",
		"1.5", "1.6667", "1.8333", "2", "2.2", "2.4", "2.6", "2.8",
		"3", "3.2", "3.4", "3.6", "3.8", "4", "4.375", "4.75",
		"5.125", "5.5", "5.875", "6.25", "6.625", "7", "7.5", "8",
		"8.5", "9", "9.5", "10", "10.5", "11", "11.5", "12",
		"12.6667", "13.3333", "14", "14.6667", "15.3333", "16", "16.6667", "17.3333",
		"18", "18.6667", "19.3333", "20", "20.6667", "21.3333", "22", "22.6667",
		"23.3333", "24", "24.8", "25.6", "26.4", "27.2", "28", "28.8",

		"29.6", "30.4", "31.2", "32", "32.8", "33.6", "34.4", "35.2",
		"36", "36.9231", "37.8462", "38.7692", "39.6923", "40.6154", "41.5385", "42.4615",
		"43.3846", "44.3077", "45.2308", "46.1538", "47.0769", "48", "48.9231", "49.8462",
		"50.7692", "51.6923", "52.6154", "53.5385", "54.4615", "55.3846", "56.3077", "57.2308",
		"58.1538", "59.0769", "60", "61.7143", "63.4286", "65.1429", "66.8571", "68.5714",
		"70.2857", "72", "74", "76", "78", "80", "82", "84",
		"86.4", "88.8", "91.2", "93.6", "96", "99", "102", "105",
		"108", "111", "114", "117", "120", "122.3333", "124.6667", "127",
	};
	static final String FM_AMT_LFO1[] = {
		// had to guess after 72 semitones
		//Sampling of values:
		// 0=0, 12=1, 14=2, 18=4, 23=7, 29=12, 41=24, 55=36
		// 71=48, 90=60, 103=72, 110=84, 115=96, 119=108, 123=120, 127=127
		"0", "0.0833", "0.1667", "0.25", "0.3333", "0.4167", "0.5", "0.5833",
		"0.6667", "0.75", "0.8333", "0.9167", "1", "1.5", "2", "2.5",
		"3", "3.5", "4", "4.6", "5.2", "5.8", "6.4", "7",
		"7.8333", "8.6667", "9.5", "10.3333", "11.1667", "12", "13", "14",
		"15", "16", "17", "18", "19", "20", "21", "22",
		"23", "24", "24.8571", "25.7143", "26.5714", "27.4286", "28.2857", "29.1429",
		"30", "30.8571", "31.7143", "32.5714", "33.4286", "34.2857", "35.1429", "36",
		"36.75", "37.5", "38.25", "39", "39.75", "40.5", "41.25", "42",

		"42.75", "43.5", "44.25", "45", "45.75", "46.5", "47.25", "48",
		"48.6316", "49.2632", "49.8947", "50.5263", "51.1579", "51.7895", "52.4211", "53.0526",
		"53.6842", "54.3158", "54.9474", "55.5789", "56.2105", "56.8421", "57.4737", "58.1053",
		"58.7368", "59.3684", "60", "60.9231", "61.8462", "62.7692", "63.6923", "64.6154",
		"65.5385", "66.4615", "67.3846", "68.3077", "69.2308", "70.1538", "71.0769", "72",
		"73.7143", "75.4286", "77.1429", "78.8571", "80.5714", "82.2857", "84", "86.4",
		"88.8", "91.2", "93.6", "96", "99", "102", "105", "108",
		"111", "114", "117", "120", "121.75", "123.5", "125.25", "127",
	};
	static final String PORTAMENTO[] = {
		//Sampling of values:
		// 0=0, 32=0.08, 64=0.3333, 96=1.3, 127=1.8
		"0", "0.0025", "0.005", "0.0075", "0.01", "0.0125", "0.015", "0.0175",
		"0.02", "0.0225", "0.025", "0.0275", "0.03", "0.0325", "0.035", "0.0375",
		"0.04", "0.0425", "0.045", "0.0475", "0.05", "0.0525", "0.055", "0.0575",
		"0.06", "0.0625", "0.065", "0.0675", "0.07", "0.0725", "0.075", "0.0775",
		"0.08", "0.0879", "0.0958", "0.1037", "0.1117", "0.1196", "0.1275", "0.1354",
		"0.1433", "0.1512", "0.1592", "0.1671", "0.175", "0.1829", "0.1908", "0.1987",
		"0.2066", "0.2146", "0.2225", "0.2304", "0.2383", "0.2462", "0.2541", "0.2621",
		"0.27", "0.2779", "0.2858", "0.2937", "0.3016", "0.3096", "0.3175", "0.3254",

		"0.3333", "0.3635", "0.3937", "0.4239", "0.4541", "0.4843", "0.5146", "0.5448",
		"0.575", "0.6052", "0.6354", "0.6656", "0.6958", "0.726", "0.7562", "0.7864",
		"0.8167", "0.8469", "0.8771", "0.9073", "0.9375", "0.9677", "0.9979", "1.0281",
		"1.0583", "1.0885", "1.1187", "1.149", "1.1792", "1.2094", "1.2396", "1.2698",
		"1.3", "1.3161", "1.3323", "1.3484", "1.3645", "1.3806", "1.3968", "1.4129",
		"1.429", "1.4452", "1.4613", "1.4774", "1.4935", "1.5097", "1.5258", "1.5419",
		"1.5581", "1.5742", "1.5903", "1.6065", "1.6226", "1.6387", "1.6548", "1.671",
		"1.6871", "1.7032", "1.7194", "1.7355", "1.7516", "1.7677", "1.7839", "1.8",
	};

	/**
	 * Convert Nord Lead 2 program sysex to XML or vice versa
	 * @param toXML|toSysex|desc input-file output-file
	 * @throws IOException
	 */
	public static void main(String args[]) throws IOException, PatchDefinitionException  {
		if (args.length == 3) {
			System.out.println("----------------------------------------");
			System.out.println("NordLead2Prog " + args[0] + " " + args[1] +
					" " + args[2]);
			if (args[0].equalsIgnoreCase("toXML")) {
				try {
					File inputFile = new File(args[1]);
					BufferedInputStream in = new BufferedInputStream(new FileInputStream(inputFile));
					int fileLen = (int)inputFile.length();
					byte sysex[] = new byte[fileLen];
					in.read(sysex, 0, fileLen);
					in.close();
					NordLead2Prog nl2 = new NordLead2Prog();
					nl2.fromSysex(sysex);

					if (nl2.isValid()) {
						try {
							BufferedReader info = new BufferedReader(new FileReader((args[1] + ".info")));
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
						PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(args[2])));
						out.println(Util.formatXML(nl2.toXML(), -1));
						out.close();
					} else {
						System.out.println("input file " + args[1] + " did not contain valid sysex");
					}
				} catch (FileNotFoundException e) {
					System.out.println("unable to open input file " + args[1]);
				}
				System.out.println("   done.");
				return;
			} else if (args[0].equalsIgnoreCase("toSysex")) {
				try {
					BufferedReader in = new BufferedReader(new FileReader(args[1]));
					String s;
					StringBuffer sb = new StringBuffer();
					while ((s = in.readLine()) != null) {
						sb.append(s + System.getProperty("line.separator"));
					}
					in.close();
					NordLead2Prog nl2 = new NordLead2Prog();
					nl2.fromXML(sb.toString());

					if (nl2.isValid()) {
						FileOutputStream out = new FileOutputStream(args[2]);
						nl2.toSysex();
						out.write(nl2.getSysex());
						out.close();
					} else {
						System.out.println("input file " + args[1] + " did not contain valid XML: " + nl2.getInvalidMsg());
					}
				} catch (FileNotFoundException e) {
					System.out.println("unable to open input file " + args[1]);
				}
				System.out.println("   done.");
				return;
			} else {
				System.out.println("invalid run type '" + args[0] + "'");
			}
		} /* else if (args.length == 2) {
			if (args[0].equalsIgnoreCase("desc")) {
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(args[2])));
				NordLead2Prog nl2 = new NordLead2Prog();
				nl2.writeGenericDesc(out);
				out.close();
			} else {
				System.out.println("invalid run type '" + args[0] + "'");
			}
		}
*/

		System.out.println("please specify toXML|toSysex followed by input and output filenames");
	}
	
	NordLead2Prog() throws PatchDefinitionException {
		super("Clavia", "Nord Lead 2 program", INIT_SYSEX, INIT_SYSEX, "1.10");
		buildNL2Patch();
		buildGenericPatch();
		buildGenericPatchLinkage();
	}

	void buildNL2Patch() {
		XMLTagGroup xtSource, xtSourceEdit, xtParam, xt, xtg;
		SynthParmAbstract spa;
		XMLTag xt2;
		int i;

		xtSource = new XMLTagGroup("source_patch");
		sourceTags.add(xtSource);
		xtSourceEdit = new XMLTagGroup("source_patch");
		sourceTagsEdit.add(xtSourceEdit);
		xt2 = new XMLTagFixed("version", "1.01");
		xtSource.add(xt2);
		xtSourceEdit.add(xt2);

		spa = new SynthParmRange("global_channel", 1, 16, new SysexParmOffset(2, -1));
		hdrParms.add(spa);
		hdrParmsEdit.add(spa);
		xt2 = new XMLTagValue(spa.getName(), spa);
		xtSource.add(xt2);
		xtSourceEdit.add(xt2);
		spa = new SynthParmRange("bank_number", 1, 4, new SysexParmOffset(4, 0));
		hdrParms.add(spa);
		xtSource.add(new XMLTagValue(spa.getName(), spa));
		spa = new SynthParmRange("patch_number", 0, 98, new SysexParmOffset(5, 0));
		hdrParms.add(spa);
		xtSource.add(new XMLTagValue(spa.getName(), spa));
		spa = new SynthParmRange("edit_buffer", 0, 3, new SysexParmOffset(5, 0));
		hdrParmsEdit.add(spa);
		xtSourceEdit.add(new XMLTagValue(spa.getName(), spa));
		xtParam = new XMLTagGroup("parameters");
		xtSource.add(xtParam);
		xtSourceEdit.add(xtParam);

		xt = new XMLTagGroup("Voice_Parms");
		xtParam.add(xt);
		addPgmParm(new SynthParmRange("Oct_Shift", -2, 2,
				new SysexParmOffset(64, 2)), xt);
		addPgmParm(new SynthParmTable("Mod_Wheel_Dest", new String[] { "Filter", "FM", "OSC2", "LFO1", "Morph" },
				new SysexParmOffset(59, 0)), xt);
		addPgmParm(new SynthParmTable("Unison", new String[] { "Off", "On" },
				new SysexParmOffset(60, 0)), xt);
		addPgmParm(new SynthParmTable("Voice_Mode", new String[] { "Mono", "Legato", "Poly" },
				new SysexParmOffset(58, 0)), xt);
		addPgmParm(new SynthParmRange("Portamento", 0, 127,
				new SysexParmOffset(16, 0)), xt);
		addPgmParm(new SynthParmTable("Auto", new String[] { "Off", "On" },
				new SysexParmOffset(62, 0)), xt);

		xt = new XMLTagGroup("Oscillators");
		xtParam.add(xt);
		addPgmParm(new SynthParmTable("OSC1_Waveform", new String[] { "Pulse", "Saw", "Tri", "Sine" },
				new SysexParmOffset(50, 0)), xt);
		addPgmParm(new SynthParmRange("OSC1_FM_Amount", 0, 127,
				new SysexParmOffset(7, 0)), xt);

		addPgmParm(new SynthParmTable("OSC2_Waveform", new String[] { "Pulse", "Saw", "Tri", "Noise" },
				new SysexParmOffset(51, 0)), xt);
		addPgmParm(new SynthParmRange("OSC2_Pitch", -60, 60,
				new SysexParmOffset(0, 60)), xt);
		addPgmParm(new SynthParmRange("OSC2_Fine_Tune", -64, 63,
				new SysexParmOffset(1, 64)), xt);
		addPgmParm(new SynthParmTable("OSC2_Kbd_Track", new String[] { "Off", "On" },
				new SysexParmOffset(54, 0)), xt);
		addPgmParm(new SynthParmRange("Pulse_Width", 0, 127,
				new SysexParmOffset(6, 0)), xt);
		addPgmParm(new SynthParmTable("Sync", new String[] { "Off", "On" },
				new SysexParmBitField(52, 0, 1)), xt);
		addPgmParm(new SynthParmTable("Ring_Mod", new String[] { "Off", "On" },
				new SysexParmBitField(52, 1, 1)), xt);

		addPgmParm(new SynthParmRange("OSC_1_2_Mix", 0, 127,
				new SysexParmOffset(2, 0)), xt);

		xt = new XMLTagGroup("Amplifier");
		xtParam.add(xt);
		addPgmParm(new SynthParmRange("Amp_Env_Attack", 0, 127,
				new SysexParmOffset(12, 0)), xt);
		addPgmParm(new SynthParmRange("Amp_Env_Decay", 0, 127,
				new SysexParmOffset(13, 0)), xt);
		addPgmParm(new SynthParmRange("Amp_Env_Sustain", 0, 127,
				new SysexParmOffset(14, 0)), xt);
		addPgmParm(new SynthParmRange("Amp_Env_Release", 0, 127,
				new SysexParmOffset(15, 0)), xt);
		addPgmParm(new SynthParmRange("Gain", 0, 127,
				new SysexParmOffset(17, 0)), xt);

		xt = new XMLTagGroup("Filter");
		xtParam.add(xt);
		addPgmParm(new SynthParmRange("Filter_Env_Attack", 0, 127,
				new SysexParmOffset(8, 0)), xt);
		addPgmParm(new SynthParmRange("Filter_Env_Decay", 0, 127,
				new SysexParmOffset(9, 0)), xt);
		addPgmParm(new SynthParmRange("Filter_Env_Sustain", 0, 127,
				new SysexParmOffset(10, 0)), xt);
		addPgmParm(new SynthParmRange("Filter_Env_Release", 0, 127,
				new SysexParmOffset(11, 0)), xt);
		addPgmParm(new SynthParmTable("Filter_Type", new String[] { "LP 12db", "LP 24db", "HP 24db", "BP 12db", "Notch + LP 12db" },
				new SysexParmOffset(53, 0)), xt);
		addPgmParm(new SynthParmRange("Frequency", 0, 127,
				new SysexParmOffset(3, 0)), xt);
		addPgmParm(new SynthParmRange("Resonance", 0, 127,
				new SysexParmOffset(4, 0)), xt);
		addPgmParm(new SynthParmRange("Filter_Env_Amount", 0, 127,
				new SysexParmOffset(5, 0)), xt);
		addPgmParm(new SynthParmTable("Filter_Velocity", new String[] { "Off", "On" },
				new SysexParmOffset(63, 0)), xt);
		addPgmParm(new SynthParmTable("Filter_Kbd_Track", new String[] { "Off", "1/3", "2/3", "Full" },
				new SysexParmOffset(55, 0)), xt);
		addPgmParm(new SynthParmTable("Distortion", new String[] { "Off", "On" },
				new SysexParmBitField(52, 4, 1)), xt);

		xt = new XMLTagGroup("LFO1");
		xtParam.add(xt);
		addPgmParm(new SynthParmRange("LFO1_Rate", 0, 127,
				new SysexParmOffset(21, 0)), xt);
		addPgmParm(new SynthParmTable("LFO1_Wave", new String[] { "Random", "Saw", "Tri", "Square", "Soft Random" },
				new SysexParmOffset(56, 0)), xt);
		addPgmParm(new SynthParmTable("LFO1_Dest", new String[] { "PW", "Filter", "OSC2", "OSC1+2", "FM" },
				new SysexParmOffset(57, 0)), xt);
		addPgmParm(new SynthParmRange("LFO1_Amount", 0, 127,
				new SysexParmOffset(22, 0)), xt);

		xt = new XMLTagGroup("LFO2");
		xtParam.add(xt);
		addPgmParm(new SynthParmRange("LFO2_Rate", 0, 127,
				new SysexParmOffset(23, 0)), xt);
		addPgmParm(new SynthParmTable("LFO2_Dest", new String[] { "Arp Down", "Arp Up", "Arp Up&Down", "Amp", "OSC1+2", "Arp Random", "Echo", "Filter", "Off" },
				new SysexParmOffset(65, 0)), xt);
		addPgmParm(new SynthParmRange("LFO2_Amount_Arp_Range", 0, 127,
				new SysexParmOffset(24, 0)), xt);

		xt = new XMLTagGroup("Mod_Env");
		xtParam.add(xt);
		addPgmParm(new SynthParmRange("Mod_Env_Attack", 0, 127,
				new SysexParmOffset(18, 0)), xt);
		addPgmParm(new SynthParmRange("Mod_Env_Decay", 0, 127,
				new SysexParmOffset(19, 0)), xt);
		addPgmParm(new SynthParmTable("Mod_Env_Dest", new String[] { "OSC2", "FM", "PW", "Off" },
				new SysexParmOffset(61, 0)), xt);
		addPgmParm(new SynthParmRange("Mod_Env_Amount", -64, 63,
				new SysexParmOffset(20, 64)), xt);

		xt = new XMLTagGroup("Velocity_Morph");
		xtParam.add(xt);
		addPgmParm(new SynthParmRange("Portamento_Morph", -128, 127,
				new SysexParmOffset(41, 0)), xt);
		addPgmParm(new SynthParmRange("OSC1_FM_Amount_Morph", -128, 127,
				new SysexParmOffset(32, 0)), xt);
		addPgmParm(new SynthParmRange("OSC2_Pitch_Morph", -128, 127,
				new SysexParmOffset(25, 0)), xt);
		addPgmParm(new SynthParmRange("OSC2_Fine_Tune_Morph", -128, 127,
				new SysexParmOffset(26, 0)), xt);
		addPgmParm(new SynthParmRange("Pulse_Width_Morph", -128, 127,
				new SysexParmOffset(31, 0)), xt);
		addPgmParm(new SynthParmRange("OSC_1_2_Mix_Morph", -128, 127,
				new SysexParmOffset(27, 0)), xt);
		addPgmParm(new SynthParmRange("Amp_Env_Attack_Morph", -128, 127,
				new SysexParmOffset(37, 0)), xt);
		addPgmParm(new SynthParmRange("Amp_Env_Decay_Morph", -128, 127,
				new SysexParmOffset(38, 0)), xt);
		addPgmParm(new SynthParmRange("Amp_Env_Sustain_Morph", -128, 127,
				new SysexParmOffset(39, 0)), xt);
		addPgmParm(new SynthParmRange("Amp_Env_Release_Morph", -128, 127,
				new SysexParmOffset(40, 0)), xt);
		addPgmParm(new SynthParmRange("Gain_Morph", -128, 127,
				new SysexParmOffset(42, 0)), xt);
		addPgmParm(new SynthParmRange("Filter_Env_Attack_Morph", -128, 127,
				new SysexParmOffset(33, 0)), xt);
		addPgmParm(new SynthParmRange("Filter_Env_Decay_Morph", -128, 127,
				new SysexParmOffset(34, 0)), xt);
		addPgmParm(new SynthParmRange("Filter_Env_Sustain_Morph", -128, 127,
				new SysexParmOffset(35, 0)), xt);
		addPgmParm(new SynthParmRange("Filter_Env_Release_Morph", -128, 127,
				new SysexParmOffset(36, 0)), xt);
		addPgmParm(new SynthParmRange("Frequency_Morph", -128, 127,
				new SysexParmOffset(28, 0)), xt);
		addPgmParm(new SynthParmRange("Resonance_Morph", -128, 127,
				new SysexParmOffset(29, 0)), xt);
		addPgmParm(new SynthParmRange("Filter_Env_Amount_Morph", -128, 127,
				new SysexParmOffset(30, 0)), xt);
		addPgmParm(new SynthParmRange("LFO1_Rate_Morph", -128, 127,
				new SysexParmOffset(46, 0)), xt);
		addPgmParm(new SynthParmRange("LFO1_Amount_Morph", -128, 127,
				new SysexParmOffset(47, 0)), xt);
		addPgmParm(new SynthParmRange("LFO2_Rate_Morph", -128, 127,
				new SysexParmOffset(48, 0)), xt);
		addPgmParm(new SynthParmRange("LFO2_Amount_Arp_Range_Morph", -128, 127,
				new SysexParmOffset(49, 0)), xt);
		addPgmParm(new SynthParmRange("Mod_Env_Attack_Morph", -128, 127,
				new SysexParmOffset(43, 0)), xt);
		addPgmParm(new SynthParmRange("Mod_Env_Decay_Morph", -128, 127,
				new SysexParmOffset(44, 0)), xt);
		addPgmParm(new SynthParmRange("Mod_Env_Amount_Morph", -128, 127,
				new SysexParmOffset(45, 0)), xt);

		xt2 = new XMLTagSysex(this);
		xtSource.add(xt2);
		xtSourceEdit.add(xt2);
	}

	void buildGenericPatch() throws PatchDefinitionException {
		Module mod, modOsc1, modOsc2, modFilter, modVCA, modLFO1, modLFO2, modLFO1FMAmt,
				modEnvFMAmt, modModEnv, modFilterEnv, modFiltEnvVel, modVoiceParms,
				modNoise, modMixer, modTrem, modTremLevelAmt1Mod, modFilter2, modAmpEnv,
				modOsc1PWMAmt1Mod, modOsc2PWMAmt1Mod, modOsc1PWMAmt2Mod, modOsc2PWMAmt2Mod,
				modOsc1ExpoFMAmt1Mod, modOsc2ExpoFMAmt1Mod, modOsc1ExpoFMAmt2Mod, modOsc2ExpoFMAmt2Mod,
				modOsc2ExpoFMAmt3Mod, modOsc1LinearFMAmt1Mod, modOsc1LinearFMAmt2Mod, modOsc1LinearFMAmt3Mod,
				modFilterExpoFMAmt1Mod, modFilterExpoFMAmt2Mod, modFilterExpoFMAmt3Mod,
				modFilter2ExpoFMAmt1Mod, modFilter2ExpoFMAmt2Mod, modFilter2ExpoFMAmt3Mod,
				modMixerAudioAmt1Mod, modMixerAudioAmt2Mod, modAudioOutLevelAmt1Mod;
		ModuleParm mp, mp2;

		mod = new Module("Voice Parms", "voice_parms", 0);
		modVoiceParms = mod;
		genPatch.addModule(mod);
		mod.addOutputJack(new ModuleOutputJack("Voice Control Out", "control_output"));
		mod.addOutputJack(new ModuleOutputJack("Velocity Out", "control_output", "positive"));
		mod.addOutputJack(new ModuleOutputJack("Mod Wheel Out", "control_output", "positive"));
		mod.addParm(new ModuleParm("Transpose", "octave", new ParmValidatorRange(-2, 2), "0"));
		mod.addParm(new ModuleParm("Unison", new ParmValidatorTable(new String[] { "Off", "On" }), "Off"));
		mod.addParm(new ModuleParm("Unison Voices", new ParmValidatorRange(2, 4), "2"));
		mod.addParm(new ModuleParm("Unison Detune", new ParmValidatorRange(2.5, 28), "17"));
		// Mono key priority = last note played; where to record this?
		mod.addParm(new ModuleParm("Voice Mode", new ParmValidatorTable(new String[] { "Mono", "Poly" }), "Poly"));
		mp = new ModuleParm("Portamento", new ParmValidatorNumTable(PORTAMENTO), "0");
		mod.addParm(mp);
		mp = new ModuleParm("Portamento Mod Amt", "seconds", new ParmValidatorRange(-1.8, 1.8), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Portamento Mod In", "control_input"), mp);
		mod.addParm(new ModuleParm("Portamento Type", new ParmValidatorTable(new String[] { "Exponential" }), "Exponential"));
		mod.addParm(new ModuleParm("Fingered Portamento", new ParmValidatorTable(new String[] { "Off", "On" }), "Off"));

		mod = new ModuleOsc("Osc1", "osc", 1);
		modOsc1 = mod;
		genPatch.addModule(mod);
		mod.addParm(new ModuleParm("Waveform", new ParmValidatorTable(new String[] { "Pulse", "Saw", "Tri", "Sine" }), "Saw"));
		mod.addParm(new ModuleParm("Coarse Tune", "semitones", new ParmValidatorRange(0, 0), "0", "linear"));
		mod.addParm(new ModuleParm("Fine Tune", "cents", new ParmValidatorRange(0, 0), "0", "linear"));
		mod.addParm(new ModuleParm("Key Track", "percent", new ParmValidatorRange(100, 100), "100", "linear"));
		mod.addParm(new ModuleParm("Base Key", "midi note", new ParmValidatorRange(56, 56), "56"));
		mp = new ModuleParm("Pulse Width", "percent", new ParmValidatorRange(50, 99), "50", "linear");
		mod.addParm(mp); // pulse width affects osc1 and osc2
		mod.addParm(new ModuleParm("Pulse Width Range", null, new ParmValidatorTable(new String[] { "50-99" }), "50-99"));
		mp = new ModuleParm("PWM Amt1", "percent", new ParmValidatorRange(0, 50), "0", "linear"); // pwm affects osc1 and osc2
		mod.addParm(mp);
		// receives LFO1
		mod.addInputJack(new ModuleInputJack("PWM In1", "control_input"), mp);
		mp = new ModuleParm("PWM Amt2", "percent", new ParmValidatorRange(-50, 50), "0", "linear"); // pwm affects osc1 and osc2
		mod.addParm(mp);
		// receives Mod Env
		mod.addInputJack(new ModuleInputJack("PWM In2", "control_input"), mp);
		mp = new ModuleParm("PWM Amt3", "percent", new ParmValidatorRange(-49, 49), "0", "linear"); // pwm affects osc1 and osc2
		mod.addParm(mp);
		// for morph
		mod.addInputJack(new ModuleInputJack("PWM In3", "control_input"), mp);
		mp = new ModuleParm("PWM Amt4", "percent", new ParmValidatorRange(-49, 49), "0", "linear");
		mod.addParm(mp);
		// for PWM Amt1 Mod
		mod.addInputJack(new ModuleInputJack("PWM In4", "control_input"), mp);
		mp = new ModuleParm("PWM Amt5", "percent", new ParmValidatorRange(-49, 49), "0", "linear");
		mod.addParm(mp);
		// for PWM Amt2 Mod
		mod.addInputJack(new ModuleInputJack("PWM In5", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt1", "semitones", new ParmValidatorNumTable(LFO_AMT_OSC), "0", "expo");
		mod.addParm(mp);
		// receives LFO1
		mod.addInputJack(new ModuleInputJack("Expo FM In1", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt2", "semitones", new ParmValidatorNumTable(LFO_AMT_OSC), "0", "expo");
		mod.addParm(mp);
		// receives LFO2
		mod.addInputJack(new ModuleInputJack("Expo FM In2", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt4", "semitones", new ParmValidatorRange(-62, 62), "0", "expo");
		mod.addParm(mp);
		// for Expo FM Amt1 Mod
		mod.addInputJack(new ModuleInputJack("Expo FM In4", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt5", "semitones", new ParmValidatorRange(-62, 62), "0", "expo");
		mod.addParm(mp);
		// for Expo FM Amt2 Mod
		mod.addInputJack(new ModuleInputJack("Expo FM In5", "control_input"), mp);
		mp = new ModuleParm("Linear FM Amt1", "semitones", new ParmValidatorNumTable(LINEAR_FM_AMT), "0", "expo");
		mod.addParm(mp);
		// Osc2/Noise FM controlled by front panel knob
		mod.addInputJack(new ModuleInputJack("Linear FM In1", "control_input"), mp);
		mp = new ModuleParm("Linear FM Amt2", "semitones", new ParmValidatorNumTable(LINEAR_FM_AMT), "0", "expo"); // FIXME right table?
		mod.addParm(mp);
		// Osc2/Noise FM amount controlled by LFO1
		mod.addInputJack(new ModuleInputJack("Linear FM In2", "control_input"), mp);
		mp = new ModuleParm("Linear FM Amt3", "semitones", new ParmValidatorNumTable(MOD_ENV_AMT_OSC2), "0", "expo"); // FIXME right table?
		mod.addParm(mp);
		// Osc2/Noise FM amount controlled by Mod Env
		mod.addInputJack(new ModuleInputJack("Linear FM In3", "control_input"), mp);
		// FIXME unused, but will be needed for mod wheel FM dest
//		mp = new ModuleParm("Linear FM Amt4", "semitones", new ParmValidatorRange(0, 127), "0", "expo");
//		mp.setMorph(new ParmMorph("MIDI", "Mod Wheel", null, mp));
//		mod.addParm(mp);
//		mod.addInputJack(new ModuleInputJack("Linear FM In4", "control_input"), mp);
		mp = new ModuleParm("Linear FM Amt5", "semitones", new ParmValidatorRange(-127, 127), "0", "expo");
		mod.addParm(mp);
		// for Linear FM Amt1 Mod
		mod.addInputJack(new ModuleInputJack("Linear FM In5", "control_input"), mp);
		mp = new ModuleParm("Linear FM Amt6", "semitones", new ParmValidatorRange(-127, 127), "0", "expo");
		mod.addParm(mp);
		// for Linear FM Amt2 Mod
		mod.addInputJack(new ModuleInputJack("Linear FM In6", "control_input"), mp);
		mp = new ModuleParm("Linear FM Amt7", "semitones", new ParmValidatorRange(-127, 127), "0", "expo");
		mod.addParm(mp);
		// for Linear FM Amt3 Mod
		mod.addInputJack(new ModuleInputJack("Linear FM In7", "control_input"), mp);
		mod.addInputJack(new ModuleInputJack("Voice Control In", "control_input"));
		mod.addOutputJack(new ModuleOutputJack("Wave Out", "audio_output"));

		mod = new ModVCA("Osc1 PWM Amt1 Mod", 1, "0");
		modOsc1PWMAmt1Mod = mod;
		genPatch.addModule(mod);

		mod = new ModVCA("Osc1 PWM Amt2 Mod", 1, "0");
		modOsc1PWMAmt2Mod = mod;
		genPatch.addModule(mod);

		mod = new ModVCA("Osc1 Expo FM Amt1 Mod", 1, "0");
		modOsc1ExpoFMAmt1Mod = mod;
		genPatch.addModule(mod);

		mod = new ModVCA("Osc1 Expo FM Amt2 Mod", 1, "0");
		modOsc1ExpoFMAmt2Mod = mod;
		genPatch.addModule(mod);

		mod = new ModVCA("Osc1 Linear FM Amt1 Mod", 1, "0");
		modOsc1LinearFMAmt1Mod = mod;
		genPatch.addModule(mod);

		mod = new ModVCA("Osc1 Linear FM Amt2 Mod", 1, "0");
		modOsc1LinearFMAmt2Mod = mod;
		genPatch.addModule(mod);

		mod = new ModVCA("Osc1 Linear FM Amt3 Mod", 1, "0");
		modOsc1LinearFMAmt3Mod = mod;
		genPatch.addModule(mod);

		mod = new NL2ModuleOsc2("Osc2", "osc", 2);
		modOsc2 = mod;
		genPatch.addModule(mod);
		mod.addParm(new ModuleParm("Waveform", new ParmValidatorTable(new String[] { "Pulse", "Saw", "Tri" }), "Saw"));
		mp = new ModuleParm("Coarse Tune", "semitones", new ParmValidatorRange(-60, 60), "0", "linear");
		mod.addParm(mp);
		mp = new ModuleParm("Coarse Tune Mod Amt", "semitones", new ParmValidatorRange(-60, 60), "0", "linear");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Coarse Tune Mod In", "control_input"), mp);
		mp = new ModuleParm("Fine Tune", "cents", new ParmValidatorRange(-50, 50), "0", "linear");
		mod.addParm(mp);
		mp = new ModuleParm("Fine Tune Mod Amt", "semitones", new ParmValidatorRange(-50, 50), "0", "linear");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Fine Tune Mod In", "control_input"), mp);
		mod.addParm(new ModuleParm("Key Track", "percent", new ParmValidatorTable(new String[] { "0", "100" }), "100", "linear"));
		mod.addParm(new ModuleParm("Base Key", "midi note", new ParmValidatorRange(56, 56), "56"));
		mp = new ModuleParm("Pulse Width", "percent", new ParmValidatorRange(50, 99), "50", "linear");
		mod.addParm(mp); // pulse width affects osc1 and osc2
		mod.addParm(new ModuleParm("Pulse Width Range", null, new ParmValidatorTable(new String[] { "50-99" }), "50-99"));
		mp = new ModuleParm("PWM Amt1", "percent", new ParmValidatorRange(0, 50), "0", "linear"); // pwm affects osc1 and osc2
		mod.addParm(mp);
		// receives LFO1
		mod.addInputJack(new ModuleInputJack("PWM In1", "control_input"), mp);
		mp = new ModuleParm("PWM Amt2", "percent", new ParmValidatorRange(-50, 50), "0", "linear"); // pwm affects osc1 and osc2
		mod.addParm(mp);
		// receives Mod Env
		mod.addInputJack(new ModuleInputJack("PWM In2", "control_input"), mp);
		mp = new ModuleParm("PWM Amt3", "percent", new ParmValidatorRange(-49, 49), "0", "linear"); // pwm affects osc1 and osc2
		mod.addParm(mp);
		// for morph
		mod.addInputJack(new ModuleInputJack("PWM In3", "control_input"), mp);
		mp = new ModuleParm("PWM Amt4", "percent", new ParmValidatorRange(-49, 49), "0", "linear");
		mod.addParm(mp);
		// for PWM Amt1 Mod
		mod.addInputJack(new ModuleInputJack("PWM In4", "control_input"), mp);
		mp = new ModuleParm("PWM Amt5", "percent", new ParmValidatorRange(-49, 49), "0", "linear");
		mod.addParm(mp);
		// for PWM Amt2 Mod
		mod.addInputJack(new ModuleInputJack("PWM In5", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt1", "semitones", new ParmValidatorNumTable(LFO_AMT_OSC), "0", "expo");
		mod.addParm(mp);
		// receives LFO1
		mod.addInputJack(new ModuleInputJack("Expo FM In1", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt2", "semitones", new ParmValidatorNumTable(LFO_AMT_OSC), "0", "expo");
		mod.addParm(mp);
		// receives LFO2
		mod.addInputJack(new ModuleInputJack("Expo FM In2", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt3", "semitones", new ParmValidatorNumTable(MOD_ENV_AMT_OSC2), "0", "expo");
		mod.addParm(mp);
		// receives Mod Env
		mod.addInputJack(new ModuleInputJack("Expo FM In3", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt4", "semitones", new ParmValidatorRange(-62, 62), "0", "expo");
		mod.addParm(mp);
		// for Expo FM Amt1 Mod
		mod.addInputJack(new ModuleInputJack("Expo FM In4", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt5", "semitones", new ParmValidatorRange(-62, 62), "0", "expo");
		mod.addParm(mp);
		// for Expo FM Amt2 Mod
		mod.addInputJack(new ModuleInputJack("Expo FM In5", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt6", "semitones", new ParmValidatorRange(-130, 126), "0", "expo");
		mod.addParm(mp);
		// for Expo FM Amt3 Mod
		mod.addInputJack(new ModuleInputJack("Expo FM In6", "control_input"), mp);
		mod.addInputJack(new ModuleInputJack("Voice Control In", "control_input"));
		mp = new ModuleParm("Sync", "hard", new ParmValidatorTable(new String[] { "Off", "On" }), "Off");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Sync In", "control_input"), mp);
		mod.addOutputJack(new ModuleOutputJack("Wave Out", "audio_output"));
		mod.addParm(new ModuleParm("Ring Mod", new ParmValidatorTable(new String[] { "Off", "On" }), "Off"));

		mod = new ModVCA("Osc2 PWM Amt1 Mod", 1, "0");
		modOsc2PWMAmt1Mod = mod;
		genPatch.addModule(mod);

		mod = new ModVCA("Osc2 PWM Amt2 Mod", 1, "0");
		modOsc2PWMAmt2Mod = mod;
		genPatch.addModule(mod);

		mod = new ModVCA("Osc2 Expo FM Amt1 Mod", 1, "0");
		modOsc2ExpoFMAmt1Mod = mod;
		genPatch.addModule(mod);

		mod = new ModVCA("Osc2 Expo FM Amt2 Mod", 1, "0");
		modOsc2ExpoFMAmt2Mod = mod;
		genPatch.addModule(mod);

		mod = new ModVCA("Osc2 Expo FM Amt3 Mod", 1, "0");
		modOsc2ExpoFMAmt3Mod = mod;
		genPatch.addModule(mod);

		mod = new Module("Noise", "noise", 1);
		modNoise = mod;
		genPatch.addModule(mod);
		// FIXME add parms noise "color", "sync"
		mod.addOutputJack(new ModuleOutputJack("Wave Out", "audio_output"));

		mod = new ModuleMixer("LFO1 FM Amt", "vca", 2);
		modLFO1FMAmt = mod;
		genPatch.addModule(mod);
		mp = new ModuleParm("Level Amt1", "percent", new ParmValidatorRange(0, 100), "100", "linear");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Level In1", "control_input"), mp);
		mod.addInputJack(new ModuleInputJack("VCA In", "audio_input"));
		mod.addOutputJack(new ModuleOutputJack("VCA Out", "audio_output"));

		mod = new ModuleMixer("Mod Env FM Amt", "vca", 3);
		modEnvFMAmt = mod;
		genPatch.addModule(mod);
		mp = new ModuleParm("Level Amt1", "percent", new ParmValidatorRange(0, 100), "100", "linear");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Level In1", "control_input"), mp);
		mod.addInputJack(new ModuleInputJack("VCA In", "audio_input"));
		mod.addOutputJack(new ModuleOutputJack("VCA Out", "audio_output"));

		mod = new ModuleMixer("Mixer", "mixer", 0);
		modMixer = mod;
		genPatch.addModule(mod);
		mp = new ModuleParm("Audio Amt1", "percent", new ParmValidatorRange(0, 100), "100", "linear");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Audio In1", "audio_input"), mp);
		mp2 = new ModuleParm("Audio Amt2", "percent", new ParmValidatorRange(0, 100), "0", "linear");
		mod.addParm(mp2);
		mod.addInputJack(new ModuleInputJack("Audio In2", "audio_input"), mp2);
		mp.setLink(new ParmLink(mp2, null));
		mp = new ModuleParm("Audio Amt3", "seconds", new ParmValidatorRange(-100, 100), "0", "linear");
		mod.addParm(mp);
		// for Audio Amt1 Mod
		mod.addInputJack(new ModuleInputJack("Audio In3", "control_input"), mp);
		mp2 = new ModuleParm("Audio Amt4", "seconds", new ParmValidatorRange(-100, 100), "0", "linear");
		mod.addParm(mp2);
		// for Audio Amt2 Mod
		mod.addInputJack(new ModuleInputJack("Audio In4", "control_input"), mp2);
		mp.setLink(new ParmLink(mp2, null));
		mod.addOutputJack(new ModuleOutputJack("Mixer Out", "audio_output"));

		mod = new ModVCA("Mixer Audio Amt1 Mod", 1, "0");
		modMixerAudioAmt1Mod = mod;
		genPatch.addModule(mod);

		mod = new ModVCA("Mixer Audio Amt2 Mod", 1, "0");
		modMixerAudioAmt2Mod = mod;
		genPatch.addModule(mod);

		mod = new Module("Filter", "filter", 1);
		modFilter = mod;
//		genPatch.addModule(mod);
		mod.addParm(new ModuleParm("Filter Type", new ParmValidatorTable(new String[] { "LP", "HP", "BP" }), "LP"));
		// FIXME slopes go with certain types; how to represent in generic patch??
		mod.addParm(new ModuleParm("Filter Slope", new ParmValidatorTable(new String[] { "12db", "24db" }), "24db"));
		mp = new ModuleParm("Frequency", "semitones", new ParmValidatorRange(-22, 105), "24", "linear");
		mod.addParm(mp);
		mp = new ModuleParm("Resonance", "percent", new ParmValidatorRange(0, 100), "0", "linear");
		mod.addParm(mp);
		mp = new ModuleParm("Resonance Mod Amt", "seconds", new ParmValidatorRange(-100, 100), "0", "linear");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Resonance Mod In", "control_input"), mp);
		mod.addParm(new ModuleParm("Self-Oscillate Percent", "percent", new ParmValidatorRange(101, 101), "101")); // can't self-oscillate
		mp = new ModuleParm("Expo FM Amt1", "semitones", new ParmValidatorRange(0, 127), "0", "linear");
		mod.addParm(mp);
		// receives Filter Envelope
		mod.addInputJack(new ModuleInputJack("Expo FM In1", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt2", "semitones", new ParmValidatorNumTable(LFO_AMT_OSC), "0", "expo");
		mod.addParm(mp);
		// receives LFO1
		mod.addInputJack(new ModuleInputJack("Expo FM In2", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt3", "semitones", new ParmValidatorNumTable(LFO_AMT_OSC), "0", "expo");
		mod.addParm(mp);
		// receives LFO2
		mod.addInputJack(new ModuleInputJack("Expo FM In3", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt4", "semitones", new ParmValidatorNumTable(LFO_AMT_OSC), "0", "expo");
		mod.addParm(mp);
		// receives Mod Wheel
		mod.addInputJack(new ModuleInputJack("Expo FM In4", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt5", "semitones", new ParmValidatorRange(-127, 127), "0", "linear");
		mod.addParm(mp);
		// for Frequency Mod
		mod.addInputJack(new ModuleInputJack("Expo FM In5", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt6", "semitones", new ParmValidatorRange(-127, 127), "0", "linear");
		mod.addParm(mp);
		// for Expo FM Amt1 Mod
		mod.addInputJack(new ModuleInputJack("Expo FM In6", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt7", "semitones", new ParmValidatorRange(-62, 62), "0", "expo");
		mod.addParm(mp);
		// for Expo FM Amt2 Mod
		mod.addInputJack(new ModuleInputJack("Expo FM In7", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt8", "semitones", new ParmValidatorRange(-62, 62), "0", "expo");
		mod.addParm(mp);
		// for Expo FM Amt3 Mod
		mod.addInputJack(new ModuleInputJack("Expo FM In8", "control_input"), mp);
		mod.addInputJack(new ModuleInputJack("Voice Control In", "control_input"));
		mod.addInputJack(new ModuleInputJack("Filter In", "audio_input"));
		mod.addOutputJack(new ModuleOutputJack("Filter Out", "audio_output"));
		mod.addParm(new ModuleParm("Key Track", "percent", new ParmValidatorTable(new String[] { "0", "33.3333", "66.6667", "100" }), "100"));
		mod.addParm(new ModuleParm("Base Key", "midi note", new ParmValidatorRange(36, 36), "36"));

		mod = new ModVCA("Filter Expo FM Amt1 Mod", 1, "0");
		modFilterExpoFMAmt1Mod = mod;
		genPatch.addModule(mod);

		mod = new ModVCA("Filter Expo FM Amt2 Mod", 1, "0");
		modFilterExpoFMAmt2Mod = mod;
		genPatch.addModule(mod);

		mod = new ModVCA("Filter Expo FM Amt3 Mod", 1, "0");
		modFilterExpoFMAmt3Mod = mod;
		genPatch.addModule(mod);

		mod = new Module("Notch Filter", "filter", 2);
		modFilter2 = mod;
		genPatch.addModule(mod);
		genPatch.addModule(modFilter);
		mod.addParm(new ModuleParm("Filter Type", new ParmValidatorTable(new String[] { "Notch" }), "Notch"));
		// FIXME slopes go with certain types; how to represent in generic patch??
		mod.addParm(new ModuleParm("Filter Slope", new ParmValidatorTable(new String[] { "12db" }), "12db"));
		mp = new ModuleParm("Frequency", "semitones", new ParmValidatorRange(-28, 99), "24", "linear");
		mp.setLink(new ParmLink(modFilter.findParm("Frequency"), null));
		mod.addParm(mp);
		mp = new ModuleParm("Resonance", "percent", new ParmValidatorRange(0, 100), "0", "linear");
		mp.setLink(new ParmLink(modFilter.findParm("Resonance"), null));
		mod.addParm(mp);
		mod.addParm(new ModuleParm("Self-Oscillate Percent", "percent", new ParmValidatorRange(101, 101), "101")); // can't self-oscillate
		mp = new ModuleParm("Expo FM Amt1", "semitones", new ParmValidatorRange(0, 127), "0", "linear");
		mp.setLink(new ParmLink(modFilter.findParm("Expo FM Amt1"), null));
		mod.addParm(mp);
		// receives Filter Envelope
		mod.addInputJack(new ModuleInputJack("Expo FM In1", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt2", "semitones", new ParmValidatorNumTable(LFO_AMT_OSC), "0", "expo");
		mp.setLink(new ParmLink(modFilter.findParm("Expo FM Amt2"), null));
		mod.addParm(mp);
		// receives LFO1
		mod.addInputJack(new ModuleInputJack("Expo FM In2", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt3", "semitones", new ParmValidatorNumTable(LFO_AMT_OSC), "0", "expo");
		mp.setLink(new ParmLink(modFilter.findParm("Expo FM Amt3"), null));
		mod.addParm(mp);
		// receives LFO2
		mod.addInputJack(new ModuleInputJack("Expo FM In3", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt4", "semitones", new ParmValidatorNumTable(LFO_AMT_OSC), "0", "expo");
		mp.setLink(new ParmLink(modFilter.findParm("Expo FM Amt4"), null));
		mod.addParm(mp);
		// receives Mod Wheel
		mod.addInputJack(new ModuleInputJack("Expo FM In4", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt6", "semitones", new ParmValidatorRange(-127, 127), "0", "linear");
		mp.setLink(new ParmLink(modFilter.findParm("Expo FM Amt6"), null));
		mod.addParm(mp);
		// for Expo FM Amt1 Mod
		mod.addInputJack(new ModuleInputJack("Expo FM In6", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt7", "semitones", new ParmValidatorRange(-62, 62), "0", "expo");
		mp.setLink(new ParmLink(modFilter.findParm("Expo FM Amt7"), null));
		mod.addParm(mp);
		// for Expo FM Amt2 Mod
		mod.addInputJack(new ModuleInputJack("Expo FM In7", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt8", "semitones", new ParmValidatorRange(-62, 62), "0", "expo");
		mp.setLink(new ParmLink(modFilter.findParm("Expo FM Amt8"), null));
		mod.addParm(mp);
		// for Expo FM Amt3 Mod
		mod.addInputJack(new ModuleInputJack("Expo FM In8", "control_input"), mp);
		mod.addInputJack(new ModuleInputJack("Voice Control In", "control_input"));
		mod.addInputJack(new ModuleInputJack("Filter In", "audio_input"));
		mod.addOutputJack(new ModuleOutputJack("Filter Out", "audio_output"));
		mp = new ModuleParm("Key Track", "percent", new ParmValidatorTable(new String[] { "0", "33.3333", "66.6667", "100" }), "100");
		mp.setLink(new ParmLink(modFilter.findParm("Key Track"), null));
		mod.addParm(mp);
		mod.addParm(new ModuleParm("Base Key", "midi note", new ParmValidatorRange(36, 36), "36"));

		mod = new ModVCA("Filter2 Expo FM Amt1 Mod", 1, "0");
		modFilter2ExpoFMAmt1Mod = mod;
		genPatch.addModule(mod);

		mod = new ModVCA("Filter2 Expo FM Amt2 Mod", 1, "0");
		modFilter2ExpoFMAmt2Mod = mod;
		genPatch.addModule(mod);

		mod = new ModVCA("Filter2 Expo FM Amt3 Mod", 1, "0");
		modFilter2ExpoFMAmt3Mod = mod;
		genPatch.addModule(mod);

		mod = new ModuleMixer("Filt Env Vel", "vca", 3);
		modFiltEnvVel = mod;
		genPatch.addModule(mod);
		mp = new ModuleParm("Level Amt1", "percent", new ParmValidatorRange(0, 100), "100", "linear");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Level In1", "control_input"), mp);
		mod.addInputJack(new ModuleInputJack("VCA In", "audio_input"));
		mod.addOutputJack(new ModuleOutputJack("VCA Out", "audio_output"));

		mod = new Module("Filter Envelope", "env_adsr", 1);
		modFilterEnv = mod;
		genPatch.addModule(mod);
		mod.addOutputJack(new ModuleOutputJack("Env Out", "control_output", "positive"));
		mp = new ModuleParm("Attack", "seconds", new ParmValidatorNumTable(ENV_ATTACK), "0.0005", "expo");
		mod.addParm(mp);
		mp = new ModuleParm("Attack Mod Amt", "seconds", new ParmValidatorRange(-16.8, 16.8), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Attack Mod In", "control_input"), mp);
		mp = new ModuleParm("Decay", "seconds", new ParmValidatorNumTable(ENV_DECAY), "0.5", "expo");
		mod.addParm(mp);
		mp = new ModuleParm("Decay Mod Amt", "seconds", new ParmValidatorRange(-45, 45), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Decay Mod In", "control_input"), mp);
		mp = new ModuleParm("Sustain", "percent", new ParmValidatorRange(0, 100), "0", "linear");
		mod.addParm(mp);
		mp = new ModuleParm("Sustain Mod Amt", "percent", new ParmValidatorRange(-100, 100), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Sustain Mod In", "control_input"), mp);
		mp = new ModuleParm("Release", "seconds", new ParmValidatorNumTable(ENV_DECAY), "0.5", "expo");
		mod.addParm(mp);
		mp = new ModuleParm("Release Mod Amt", "seconds", new ParmValidatorRange(-45, 45), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Release Mod In", "control_input"), mp);
		mod.addParm(new ModuleParm("Trigger", new ParmValidatorTable(new String[] { "Single", "Multi" }), "Multi"));
		mod.addParm(new ModuleParm("Slope", new ParmValidatorTable(new String[] { "Expo" }), "Expo"));

		mod = new Module("Amp Envelope", "env_adsr", 2);
		modAmpEnv = mod;
		genPatch.addModule(mod);
		mod.addOutputJack(new ModuleOutputJack("Env Out", "control_output", "positive"));
		mp = new ModuleParm("Attack", "seconds", new ParmValidatorNumTable(ENV_ATTACK), "0.0005", "expo");
		mod.addParm(mp);
		mp = new ModuleParm("Attack Mod Amt", "seconds", new ParmValidatorRange(-16.8, 16.8), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Attack Mod In", "control_input"), mp);
		mp = new ModuleParm("Decay", "seconds", new ParmValidatorNumTable(ENV_DECAY), "0.5", "expo");
		mod.addParm(mp);
		mp = new ModuleParm("Decay Mod Amt", "seconds", new ParmValidatorRange(-45, 45), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Decay Mod In", "control_input"), mp);
		mp = new ModuleParm("Sustain", "percent", new ParmValidatorRange(0, 100), "100", "linear");
		mod.addParm(mp);
		mp = new ModuleParm("Sustain Mod Amt", "percent", new ParmValidatorRange(-100, 100), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Sustain Mod In", "control_input"), mp);
		mp = new ModuleParm("Release", "seconds", new ParmValidatorNumTable(ENV_DECAY), "0.5", "expo");
		mod.addParm(mp);
		mp = new ModuleParm("Release Mod Amt", "seconds", new ParmValidatorRange(-45, 45), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Release Mod In", "control_input"), mp);
		mod.addParm(new ModuleParm("Trigger", new ParmValidatorTable(new String[] { "Single", "Multi" }), "Multi"));
		mod.addParm(new ModuleParm("Slope", new ParmValidatorTable(new String[] { "Expo" }), "Expo"));

		mod = new ModVCA("Tremolo", 1, "100");
		modTrem = mod;
		genPatch.addModule(mod);

		mod = new ModVCA("Tremolo Level Amt1 Mod", 1, "0");
		modTremLevelAmt1Mod = mod;
		genPatch.addModule(mod);

		mod = new Module("VCA", "vca", 2);
		modVCA = mod;
		genPatch.addModule(mod);
		mp = new ModuleParm("Level Amt1", "percent", new ParmValidatorRange(0, 100), "100", "linear");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Level In1", "control_input"), mp);
		mod.addInputJack(new ModuleInputJack("VCA In", "audio_input"));
		mod.addOutputJack(new ModuleOutputJack("VCA Out", "audio_output"));

		mod = new Module("LFO1", "lfo", 1);
		modLFO1 = mod;
		genPatch.addModule(mod);
		mod.addOutputJack(new ModuleOutputJack("Wave Out", "control_output"));
		mp = new ModuleParm("Rate", "Hz", new ParmValidatorNumTable(LFO_RATE), ".5");
		mod.addParm(mp);
		mp = new ModuleParm("Rate Mod Amt", "Hz", new ParmValidatorRange(-65.4, 65.4), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Rate Mod In", "control_input"), mp);
		mod.addParm(new ModuleParm("Waveform", new ParmValidatorTable(new String[] { "S&H", "Saw", "Tri", "Square", "S&G" }), "Tri"));

		mod = new Module("LFO2", "lfo", 2);
		modLFO2 = mod;
		genPatch.addModule(mod);
		mod.addOutputJack(new ModuleOutputJack("Wave Out", "control_output"));
		mp = new ModuleParm("Rate", "Hz", new ParmValidatorNumTable(LFO_RATE), ".5");
		mod.addParm(mp);
		mp = new ModuleParm("Rate Mod Amt", "Hz", new ParmValidatorRange(-65.4, 65.4), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Rate Mod In", "control_input"), mp);
		mod.addParm(new ModuleParm("Waveform", new ParmValidatorTable(new String[] { "Tri" }), "Tri"));

		mod = new Module("Mod Envelope", "env_ar", 0);
		modModEnv = mod;
		genPatch.addModule(mod);
		mod.addOutputJack(new ModuleOutputJack("Env Out", "control_output", "positive"));
		mp = new ModuleParm("Attack", "seconds", new ParmValidatorNumTable(ENV_ATTACK), "0.0005", "expo");
		mod.addParm(mp);
		mp = new ModuleParm("Attack Mod Amt", "seconds", new ParmValidatorRange(-16.8, 16.8), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Attack Mod In", "control_input"), mp);
		mp = new ModuleParm("Decay", "seconds", new ParmValidatorNumTable(ENV_DECAY), "0.5", "expo");
		mod.addParm(mp);
		mp = new ModuleParm("Decay Mod Amt", "seconds", new ParmValidatorRange(-45, 45), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Decay Mod In", "control_input"), mp);
		mod.addParm(new ModuleParm("Trigger", new ParmValidatorTable(new String[] { "Multi" }), "Multi"));
		mod.addParm(new ModuleParm("Slope", new ParmValidatorTable(new String[] { "Expo" }), "Expo"));

		mod = new Module("Audio Out", "audio_out", 0);
		genPatch.addModule(mod);
		mp = new ModuleParm("Level Amt1", "percent", new ParmValidatorRange(0, 100), "50", "linear");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Level In1", "audio_input"), mp);
		mp = new ModuleParm("Level Amt2", "percent", new ParmValidatorRange(-100, 100), "0", "linear");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Level In2", "audio_input"), mp);

		mod = new ModVCA("Audio Out Level Amt1 Mod", 1, "0");
		modAudioOutLevelAmt1Mod = mod;
		genPatch.addModule(mod);

		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Voice Parms", "Voice Control Out")
				, genPatch.findModuleInputJack("Osc1", "Voice Control In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Voice Parms", "Voice Control Out")
				, genPatch.findModuleInputJack("Osc2", "Voice Control In")) );
//		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Osc2", "Wave Out")
//				, genPatch.findModuleInputJack("Osc1", "Linear FM In1")) );
//		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Osc2", "Wave Out")
//				, genPatch.findModuleInputJack("LFO1 FM Amt", "VCA In")) );
//		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Osc2", "Wave Out")
//				, genPatch.findModuleInputJack("Mod Env FM Amt", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Osc1", "Wave Out")
				, genPatch.findModuleInputJack("Osc2", "Sync In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Osc1", "Wave Out")
				, genPatch.findModuleInputJack("Mixer", "Audio In1")) );
//		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Osc2", "Wave Out")
//				, genPatch.findModuleInputJack("Mixer", "Audio In2")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Voice Parms", "Velocity Out")
				, genPatch.findModuleInputJack("Filt Env Vel", "Level In1")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Filter Envelope", "Env Out")
				, genPatch.findModuleInputJack("Filt Env Vel", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Voice Parms", "Voice Control Out")
				, genPatch.findModuleInputJack("Filter", "Voice Control In")) );
//		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Mixer", "Mixer Out")
//				, genPatch.findModuleInputJack("Filter", "Filter In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Voice Parms", "Voice Control Out")
				, genPatch.findModuleInputJack("Notch Filter", "Voice Control In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Mixer", "Mixer Out")
				, genPatch.findModuleInputJack("Notch Filter", "Filter In")) );
//		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Constant", "Value Out")
//				, genPatch.findModuleInputJack("Tremolo", "Level In1")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Amp Envelope", "Env Out")
				, genPatch.findModuleInputJack("VCA", "Level In1")) );
//		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Filter", "Filter Out")
//				, genPatch.findModuleInputJack("VCA", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Filter", "Filter Out")
				, genPatch.findModuleInputJack("Tremolo", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("VCA", "VCA Out")
				, genPatch.findModuleInputJack("Audio Out", "Level In1")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("LFO1", "Wave Out")
				, genPatch.findModuleInputJack("LFO1 FM Amt", "Level In1")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Mod Envelope", "Env Out")
				, genPatch.findModuleInputJack("Mod Env FM Amt", "Level In1")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("LFO1", "Wave Out")
				, genPatch.findModuleInputJack("Osc1 PWM Amt1 Mod", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Osc1 PWM Amt1 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Osc1", "PWM In4")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Mod Envelope", "Env Out")
				, genPatch.findModuleInputJack("Osc1 PWM Amt2 Mod", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Osc1 PWM Amt2 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Osc1", "PWM In5")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("LFO1", "Wave Out")
				, genPatch.findModuleInputJack("Osc1 Expo FM Amt1 Mod", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Osc1 Expo FM Amt1 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Osc1", "Expo FM In4")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("LFO2", "Wave Out")
				, genPatch.findModuleInputJack("Osc1 Expo FM Amt2 Mod", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Osc1 Expo FM Amt2 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Osc1", "Expo FM In5")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Osc1 Linear FM Amt1 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Osc1", "Linear FM In5")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("LFO1 FM Amt", "VCA Out")
				, genPatch.findModuleInputJack("Osc1 Linear FM Amt2 Mod", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Mod Env FM Amt", "VCA Out")
				, genPatch.findModuleInputJack("Osc1 Linear FM Amt3 Mod", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("LFO1", "Wave Out")
				, genPatch.findModuleInputJack("Osc2 PWM Amt1 Mod", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Osc2 PWM Amt1 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Osc2", "PWM In4")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Mod Envelope", "Env Out")
				, genPatch.findModuleInputJack("Osc2 PWM Amt2 Mod", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Osc2 PWM Amt2 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Osc2", "PWM In5")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("LFO1", "Wave Out")
				, genPatch.findModuleInputJack("Osc2 Expo FM Amt1 Mod", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Osc2 Expo FM Amt1 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Osc2", "Expo FM In4")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("LFO2", "Wave Out")
				, genPatch.findModuleInputJack("Osc2 Expo FM Amt2 Mod", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Osc2 Expo FM Amt2 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Osc2", "Expo FM In5")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Mod Envelope", "Env Out")
				, genPatch.findModuleInputJack("Osc2 Expo FM Amt3 Mod", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Osc2 Expo FM Amt3 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Osc2", "Expo FM In6")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Filter Expo FM Amt1 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Filter", "Expo FM In6")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("LFO1", "Wave Out")
				, genPatch.findModuleInputJack("Filter Expo FM Amt2 Mod", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Filter Expo FM Amt2 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Filter", "Expo FM In7")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("LFO2", "Wave Out")
				, genPatch.findModuleInputJack("Filter Expo FM Amt3 Mod", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Filter Expo FM Amt3 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Filter", "Expo FM In8")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Filter2 Expo FM Amt1 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Notch Filter", "Expo FM In6")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("LFO1", "Wave Out")
				, genPatch.findModuleInputJack("Filter2 Expo FM Amt2 Mod", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Filter2 Expo FM Amt2 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Notch Filter", "Expo FM In7")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Osc1", "Wave Out")
				, genPatch.findModuleInputJack("Mixer Audio Amt1 Mod", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Mixer Audio Amt1 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Mixer", "Audio In3")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Mixer Audio Amt2 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Mixer", "Audio In4")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("LFO2", "Wave Out")
				, genPatch.findModuleInputJack("Tremolo Level Amt1 Mod", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Tremolo Level Amt1 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Tremolo", "Level In2")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("VCA", "VCA Out")
				, genPatch.findModuleInputJack("Audio Out Level Amt1 Mod", "VCA In")) );
		genPatch.addConnection(new Connection(genPatch.findModuleOutputJack("Audio Out Level Amt1 Mod", "VCA Out")
				, genPatch.findModuleInputJack("Audio Out", "Level In2")) );

		SynthParmRange spr, spr2;
		spr = (SynthParmRange)findPgmParm("LFO1_Amount");
		spr2 = (SynthParmRange)findPgmParm("LFO1_Amount_Morph");
		// Had to split into 2 matrix mods since there are 2 possible source jacks,
		// and dest jacks can only connect to one of those source jacks.
		// (Can't list both source jacks - that allows any dest to use either source.)
		genPatch.addMatrixMod(new MatrixModOneSource(null,
				new int[] { -1 },
				(SynthParmTable)findPgmParm("LFO1_Dest"),
				new int[] { 0, 1, 2, 3, -1 },
				new ModuleOutputJack[] { modLFO1.findOutputJack("Wave Out") },
				new ModuleInputJack[][] {
					{ modOsc1.findInputJack("PWM In1"),
						modOsc2.findInputJack("PWM In1") },
					{ modFilter.findInputJack("Expo FM In2"),
						modFilter2.findInputJack("Expo FM In2") },
					{ modOsc2.findInputJack("Expo FM In1") },
					{ modOsc1.findInputJack("Expo FM In1"),
						modOsc2.findInputJack("Expo FM In1") } },
				new ParmTranslator[][] {
					{ new ParmTranslatorRangeConvert(spr, modOsc1.findParm("PWM Amt1")),
						new ParmTranslatorRangeConvert(spr, modOsc2.findParm("PWM Amt1")) },
					{ new ParmTranslatorRangeToTable(spr, modFilter.findParm("Expo FM Amt2")),
						new ParmTranslatorRangeToTable(spr, modFilter2.findParm("Expo FM Amt2")) },
					{ new ParmTranslatorRangeToTable(spr, modOsc2.findParm("Expo FM Amt1")) },
					{ new ParmTranslatorRangeToTable(spr, modOsc1.findParm("Expo FM Amt1")),
						new ParmTranslatorRangeToTable(spr, modOsc2.findParm("Expo FM Amt1")) } },
				new ParmTranslator[][] {
					{ new NL2ModTranslatorRangeConvert(spr2, spr, modOsc1.findParm("PWM Amt4"), modOsc1.findParm("PWM Amt1")),
						new NL2ModTranslatorRangeConvert(spr2, spr, modOsc2.findParm("PWM Amt4"), modOsc2.findParm("PWM Amt1")) },
					{ new NL2ModTranslatorRangeToTable(spr2, spr, modFilter.findParm("Expo FM Amt7"), modFilter.findParm("Expo FM Amt2")),
						new NL2ModTranslatorRangeToTable(spr2, spr, modFilter2.findParm("Expo FM Amt7"), modFilter2.findParm("Expo FM Amt2")) },
					{ new NL2ModTranslatorRangeToTable(spr2, spr, modOsc2.findParm("Expo FM Amt4"), modOsc2.findParm("Expo FM Amt1")) },
					{ new NL2ModTranslatorRangeToTable(spr2, spr, modOsc1.findParm("Expo FM Amt4"), modOsc1.findParm("Expo FM Amt1")),
						new NL2ModTranslatorRangeToTable(spr2, spr, modOsc2.findParm("Expo FM Amt4"), modOsc2.findParm("Expo FM Amt1")) } } ));
		genPatch.addMatrixMod(new MatrixModOneSource(null,
				new int[] { -1 },
				(SynthParmTable)findPgmParm("LFO1_Dest"),
				new int[] { -1, -1, -1, -1, 0 },
				new ModuleOutputJack[] { modLFO1FMAmt.findOutputJack("VCA Out") },
				new ModuleInputJack[][] {
					{ modOsc1.findInputJack("Linear FM In2") } },
				new ParmTranslator[][] {
					{ new ParmTranslatorRangeToTable(spr, modOsc1.findParm("Linear FM Amt2")) } },
				null ));
		genPatch.addMatrixMod(new MatrixModOneSource(null,
				new int[] { -1 },
				(SynthParmTable)findPgmParm("LFO1_Dest"),
				new int[] { -1, -1, -1, -1, 0 },
				new ModuleOutputJack[] { modOsc1LinearFMAmt2Mod.findOutputJack("VCA Out") },
				new ModuleInputJack[][] {
					{ modOsc1.findInputJack("Linear FM In6") } },
				new ParmTranslator[][] {
					{ new NL2ModTranslatorRangeToTable(spr2, spr, modOsc1.findParm("Linear FM Amt6"), modOsc1.findParm("Linear FM Amt2")) } },
				null ));
		spr = (SynthParmRange)findPgmParm("LFO2_Amount_Arp_Range");
		spr2 = (SynthParmRange)findPgmParm("LFO2_Amount_Arp_Range_Morph");
		// Must do this one first, as its value setting won't be correct in fromGeneric()
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("LFO2_Dest"),
				new int[] { 1, 1, 1, 0, 1, 1, 1, 1, 1 },
				new ModuleOutputJack[] { modTrem.findOutputJack("VCA Out"),
					modFilter.findOutputJack("Filter Out") },
				new ModuleInputJack[][] { { modVCA.findInputJack("VCA In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource(null,
				new int[] { -1 },
				(SynthParmTable)findPgmParm("LFO2_Dest"),
				new int[] { -1, -1, -1, 0, 1, -1, -1, 2, -1 },
				new ModuleOutputJack[] { modLFO2.findOutputJack("Wave Out") },
				new ModuleInputJack[][] {
					{ modTrem.findInputJack("Level In1") },
					{ modOsc1.findInputJack("Expo FM In2"),
						modOsc2.findInputJack("Expo FM In2") },
					{ modFilter.findInputJack("Expo FM In3"),
						modFilter2.findInputJack("Expo FM In3") } },
				new ParmTranslator[][] {
					{ new ParmTranslatorPct(spr, modTrem.findParm("Level Amt1")) },
					{ new ParmTranslatorRangeToTable(spr, modOsc1.findParm("Expo FM Amt2")),
						new ParmTranslatorRangeToTable(spr, modOsc2.findParm("Expo FM Amt2")) },
					{ new ParmTranslatorRangeToTable(spr, modFilter.findParm("Expo FM Amt3")),
						new ParmTranslatorRangeToTable(spr, modFilter2.findParm("Expo FM Amt3")) } },
				new ParmTranslator[][] {
					{ new NL2ModTranslatorPct(spr2, spr, modTrem.findParm("Level Amt2"), modTrem.findParm("Level Amt1")) },
					{ new NL2ModTranslatorRangeToTable(spr2, spr, modOsc1.findParm("Expo FM Amt5"), modOsc1.findParm("Expo FM Amt2")),
						new NL2ModTranslatorRangeToTable(spr2, spr, modOsc2.findParm("Expo FM Amt5"), modOsc2.findParm("Expo FM Amt2")) },
					{ new NL2ModTranslatorRangeToTable(spr2, spr, modFilter.findParm("Expo FM Amt8") , modFilter.findParm("Expo FM Amt3")),
						new NL2ModTranslatorRangeToTable(spr2, spr, modFilter2.findParm("Expo FM Amt8"), modFilter2.findParm("Expo FM Amt3")) } } ));
		spr = (SynthParmRange)findPgmParm("Mod_Env_Amount");
		spr2 = (SynthParmRange)findPgmParm("Mod_Env_Amount_Morph");
		// Had to split into 2 matrix mods since there are 2 possible source jacks,
		// and dest jacks can only connect to one of those source jacks.
		// (Can't list both source jacks - that allows any dest to use either source.)
		genPatch.addMatrixMod(new MatrixModOneSource(null,
				new int[] { -1 },
				(SynthParmTable)findPgmParm("Mod_Env_Dest"),
				new int[] { 0, -1, 1, -1, -1 },
				new ModuleOutputJack[] { modModEnv.findOutputJack("Env Out") },
				new ModuleInputJack[][] {
					{ modOsc2.findInputJack("Expo FM In3") },
					{ modOsc1.findInputJack("PWM In2"),
						modOsc2.findInputJack("PWM In2") } },
				new ParmTranslator[][] {
					{ new ParmTranslatorRangeToTable(spr, modOsc2.findParm("Expo FM Amt3")) },
					{ new NL2ParmTranslatorPW(spr, modOsc1.findParm("PWM Amt2")),
						new NL2ParmTranslatorPW(spr, modOsc2.findParm("PWM Amt2")) } },
				new ParmTranslator[][] {
					{ new NL2ModTranslatorRangeToTable(spr2, spr, modOsc2.findParm("Expo FM Amt6"), modOsc2.findParm("Expo FM Amt3")) },
					{ new NL2ModTranslatorRangeConvert(spr2, spr, modOsc1.findParm("PWM Amt5"), modOsc1.findParm("PWM Amt2")),
						new NL2ModTranslatorRangeConvert(spr2, spr, modOsc2.findParm("PWM Amt5"), modOsc2.findParm("PWM Amt2")) } } ));
		genPatch.addMatrixMod(new MatrixModOneSource(null,
				new int[] { -1 },
				(SynthParmTable)findPgmParm("Mod_Env_Dest"),
				new int[] { -1, 0, -1, -1, -1 },
				new ModuleOutputJack[] { modEnvFMAmt.findOutputJack("VCA Out") },
				new ModuleInputJack[][] {
					{ modOsc1.findInputJack("Linear FM In3") } },
				new ParmTranslator[][] {
					{ new ParmTranslatorRangeToTable(spr, modOsc1.findParm("Linear FM Amt3")) } },
				null ));
		genPatch.addMatrixMod(new MatrixModOneSource(null,
				new int[] { -1 },
				(SynthParmTable)findPgmParm("Mod_Env_Dest"),
				new int[] { -1, 0, -1, -1, -1 },
				new ModuleOutputJack[] { modOsc1LinearFMAmt3Mod.findOutputJack("VCA Out") },
				new ModuleInputJack[][] {
					{ modOsc1.findInputJack("Linear FM In7") } },
				new ParmTranslator[][] {
					{ new NL2ModTranslatorRangeToTable(spr2, spr, modOsc1.findParm("Linear FM Amt7"), modOsc1.findParm("Linear FM Amt3")) } },
				null ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Filter_Velocity"),
				new int[] { 0, 1 },
				new ModuleOutputJack[] { modFilterEnv.findOutputJack("Env Out"),
					modFiltEnvVel.findOutputJack("VCA Out") },
				new ModuleInputJack[][] { { modFilter.findInputJack("Expo FM In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Filter_Velocity"),
				new int[] { 0, 1 },
				new ModuleOutputJack[] { modFilterEnv.findOutputJack("Env Out"),
					modFiltEnvVel.findOutputJack("VCA Out") },
				new ModuleInputJack[][] { { modFilterExpoFMAmt1Mod.findInputJack("VCA In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Filter_Velocity"),
				new int[] { 0, 1 },
				new ModuleOutputJack[] { modFilterEnv.findOutputJack("Env Out"),
					modFiltEnvVel.findOutputJack("VCA Out") },
				new ModuleInputJack[][] { { modFilter2.findInputJack("Expo FM In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Filter_Velocity"),
				new int[] { 0, 1 },
				new ModuleOutputJack[] { modFilterEnv.findOutputJack("Env Out"),
					modFiltEnvVel.findOutputJack("VCA Out") },
				new ModuleInputJack[][] { { modFilter2ExpoFMAmt1Mod.findInputJack("VCA In") } } ));

		// FIXME need to add 2=Osc2, 3=LFO1 (which is a morph), 4=morph (do nothing for this?)
		// FIXME find out if ModWheel->Osc2 is like morph (knob + mod wheel can't
		// go past knob max of 60 semitones) or regular mod (adds to knob value,
		// even if knob = +60)
		// FIXME temporarily removed 1=FM dest: must morph Osc1's Linear FM Amt4
		// FIXME see project notes for new understanding of how mod wheel
		// affects the various destinations
		genPatch.addMatrixMod(new MatrixModOneSource(null,
				new int[] { -1 },
				(SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
//				new int[] { 0, 1, -1, -1, -1 },
				new int[] { 0, -1, -1, -1, -1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] {
					{ modFilter.findInputJack("Expo FM In4") }/*,
					{ modFMWheel.findInputJack("Level In1") }*/ },
				new ParmTranslator[][] {
//					{ null },
					{ null } },
				new ParmTranslator[][] {
//					{ null },
					{ null } } ));
		genPatch.addMatrixMod(new MatrixModOneSource(null,
				new int[] { -1 },
				(SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
//				new int[] { 0, 1, -1, -1, -1 },
				new int[] { 0, -1, -1, -1, -1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] {
					{ modFilter2.findInputJack("Expo FM In4") }/*,
					{ modFMWheel.findInputJack("Level In1") }*/ },
				new ParmTranslator[][] {
//					{ null },
					{ null } },
				new ParmTranslator[][] {
//					{ null },
					{ null } } ));

		// Morph comes from either key velocity or mod wheel
		// FIXME these should really all be set to same source, but they can't be
		//  combined because MatrixModOneSource can't handle input jacks of
		//  differing types
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modVoiceParms.findInputJack("Portamento Mod In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modOsc1.findInputJack("PWM In3") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modOsc2.findInputJack("Coarse Tune Mod In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modOsc2.findInputJack("Fine Tune Mod In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modOsc2.findInputJack("PWM In3") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modFilter.findInputJack("Expo FM In5") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modFilter.findInputJack("Resonance Mod In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modAmpEnv.findInputJack("Attack Mod In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modAmpEnv.findInputJack("Decay Mod In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modAmpEnv.findInputJack("Sustain Mod In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modAmpEnv.findInputJack("Release Mod In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modFilterEnv.findInputJack("Attack Mod In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modFilterEnv.findInputJack("Decay Mod In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modFilterEnv.findInputJack("Release Mod In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modFilterEnv.findInputJack("Sustain Mod In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modModEnv.findInputJack("Attack Mod In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modModEnv.findInputJack("Decay Mod In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modTremLevelAmt1Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modLFO1.findInputJack("Rate Mod In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modLFO2.findInputJack("Rate Mod In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modOsc1PWMAmt1Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modOsc1PWMAmt2Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modOsc1ExpoFMAmt1Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modOsc1ExpoFMAmt2Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modOsc1LinearFMAmt1Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modOsc1LinearFMAmt2Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modOsc1LinearFMAmt3Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modOsc2PWMAmt1Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modOsc2PWMAmt2Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modOsc2ExpoFMAmt1Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modOsc2ExpoFMAmt2Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modOsc2ExpoFMAmt3Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modFilterExpoFMAmt1Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modFilterExpoFMAmt2Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modFilterExpoFMAmt3Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modFilter2ExpoFMAmt1Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modFilter2ExpoFMAmt2Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modFilter2ExpoFMAmt3Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modMixerAudioAmt1Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modMixerAudioAmt2Mod.findInputJack("Level In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Mod_Wheel_Dest"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modVoiceParms.findOutputJack("Velocity Out"),
					modVoiceParms.findOutputJack("Mod Wheel Out") },
				new ModuleInputJack[][] { { modAudioOutLevelAmt1Mod.findInputJack("Level In1") } } ));

		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("OSC2_Waveform"),
				new int[] { 0, 0, 0, 1 },
				new ModuleOutputJack[] { modOsc2.findOutputJack("Wave Out"),
					modNoise.findOutputJack("Wave Out") },
				new ModuleInputJack[][] { { modMixer.findInputJack("Audio In2") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("OSC2_Waveform"),
				new int[] { 0, 0, 0, 1 },
				new ModuleOutputJack[] { modOsc2.findOutputJack("Wave Out"),
					modNoise.findOutputJack("Wave Out") },
				new ModuleInputJack[][] { { modMixerAudioAmt2Mod.findInputJack("VCA In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("OSC2_Waveform"),
				new int[] { 0, 0, 0, 1 },
				new ModuleOutputJack[] { modOsc2.findOutputJack("Wave Out"),
					modNoise.findOutputJack("Wave Out") },
				new ModuleInputJack[][] { { modOsc1.findInputJack("Linear FM In1") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("OSC2_Waveform"),
				new int[] { 0, 0, 0, 1 },
				new ModuleOutputJack[] { modOsc2.findOutputJack("Wave Out"),
					modNoise.findOutputJack("Wave Out") },
				new ModuleInputJack[][] { { modOsc1LinearFMAmt1Mod.findInputJack("VCA In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("OSC2_Waveform"),
				new int[] { 0, 0, 0, 1 },
				new ModuleOutputJack[] { modOsc2.findOutputJack("Wave Out"),
					modNoise.findOutputJack("Wave Out") },
				new ModuleInputJack[][] { { modLFO1FMAmt.findInputJack("VCA In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("OSC2_Waveform"),
				new int[] { 0, 0, 0, 1 },
				new ModuleOutputJack[] { modOsc2.findOutputJack("Wave Out"),
					modNoise.findOutputJack("Wave Out") },
				new ModuleInputJack[][] { { modEnvFMAmt.findInputJack("VCA In") } } ));
		genPatch.addMatrixMod(new MatrixModOneSource((SynthParmTable)findPgmParm("Filter_Type"),
				new int[] { 0, 0, 0, 0, 1 },
				new ModuleOutputJack[] { modMixer.findOutputJack("Mixer Out"),
					modFilter2.findOutputJack("Filter Out") },
				new ModuleInputJack[][] { { modFilter.findInputJack("Filter In") } } ));

		// FIXME need to add arp feature of LFO2, and ring mod's
		//  use of FM knob for pitch adjustment
		// FIXME midi control not mapped: pitch bend range
	}

	void buildGenericPatchLinkage() {
		Module mod;
		ModuleParm mp;

		parmTranslators.add(new ParmTranslatorDirect(findPgmParm("Oct_Shift"),
				genPatch.findModuleParm("Voice Parms", "Transpose")) );
		parmTranslators.add(new ParmTranslatorDirect(findPgmParm("Unison"),
				genPatch.findModuleParm("Voice Parms", "Unison")) );
		mp = genPatch.findModuleParm("Voice Parms", "Portamento");
		parmTranslators.add(new ParmTranslatorRangeToTable((SynthParmRange)findPgmParm("Portamento"),
				mp) );
//		parmTranslators.add(new MorphTranslatorRangeToTable((SynthParmRange)findPgmParm("Portamento_Morph"),
//				(SynthParmRange)findPgmParm("Portamento"), mp) );
		parmTranslators.add(new NL2ModTranslatorRangeToTable((SynthParmRange)findPgmParm("Portamento_Morph"),
				(SynthParmRange)findPgmParm("Portamento"),
				genPatch.findModuleParm("Voice Parms", "Portamento Mod Amt"), mp) );
		parmTranslators.add(new ParmTranslatorDirect(findPgmParm("Auto"),
				genPatch.findModuleParm("Voice Parms", "Fingered Portamento")) );

		parmTranslators.add(new ParmTranslatorDirect(findPgmParm("OSC1_Waveform"),
				genPatch.findModuleParm("Osc1", "Waveform")) );
		mp = genPatch.findModuleParm("Osc1", "Linear FM Amt1");
		parmTranslators.add(new ParmTranslatorRangeToTable((SynthParmRange)findPgmParm("OSC1_FM_Amount"),
				mp) );
		parmTranslators.add(new NL2ModTranslatorRangeToTable((SynthParmRange)findPgmParm("OSC1_FM_Amount_Morph"),
				(SynthParmRange)findPgmParm("OSC1_FM_Amount"),
				genPatch.findModuleParm("Osc1", "Linear FM Amt5"), mp) );

//		parmTranslators.add(new ParmTranslatorDirect(findPgmParm("OSC2_Waveform"),
//				genPatch.findModuleParm("Osc2", "Waveform")) );
		mp = genPatch.findModuleParm("Osc2", "Coarse Tune");
		parmTranslators.add(new ParmTranslatorDirect(findPgmParm("OSC2_Pitch"),
				mp) );
//		parmTranslators.add(new MorphTranslatorDirect((SynthParmRange)findPgmParm("OSC2_Pitch_Morph"),
//				(SynthParmRange)findPgmParm("OSC2_Pitch"), mp) );
		parmTranslators.add(new NL2ModTranslatorDirect((SynthParmRange)findPgmParm("OSC2_Pitch_Morph"),
				genPatch.findModuleParm("Osc2", "Coarse Tune Mod Amt")) );
		parmTranslators.add(new ParmTranslatorTable((SynthParmTable)findPgmParm("OSC2_Kbd_Track"),
				genPatch.findModuleParm("Osc2", "Key Track")) );
		parmTranslators.add(new ParmTranslatorDirect(findPgmParm("Sync"),
				genPatch.findModuleParm("Osc2", "Sync")) );
		parmTranslators.add(new ParmTranslatorDirect(findPgmParm("Ring_Mod"),
				genPatch.findModuleParm("Osc2", "Ring Mod")) );

		mp = genPatch.findModuleParm("Mixer", "Audio Amt2");
		parmTranslators.add(new ParmTranslatorPct((SynthParmRange)findPgmParm("OSC_1_2_Mix"),
				mp) );
//		parmTranslators.add(new MorphTranslatorPct((SynthParmRange)findPgmParm("OSC_1_2_Mix_Morph"),
//				(SynthParmRange)findPgmParm("OSC_1_2_Mix"), mp) );
		parmTranslators.add(new NL2ModTranslatorPct((SynthParmRange)findPgmParm("OSC_1_2_Mix_Morph"),
				(SynthParmRange)findPgmParm("OSC_1_2_Mix"),
				genPatch.findModuleParm("Mixer", "Audio Amt4"), mp) );
		parmLinks.add(new ParmLinkTranslatorRangeConvert(genPatch.findModuleParm("Mixer", "Audio Amt1").getLink(), 100, 0));
		parmLinks.add(new ParmLinkTranslatorRangeConvert(genPatch.findModuleParm("Mixer", "Audio Amt3").getLink(), 100, -100));

		mp = genPatch.findModuleParm("Amp Envelope", "Attack");
		parmTranslators.add(new ParmTranslatorRangeToTable((SynthParmRange)findPgmParm("Amp_Env_Attack"),
				mp) );
//		parmTranslators.add(new MorphTranslatorRangeToTable((SynthParmRange)findPgmParm("Amp_Env_Attack_Morph"),
//				(SynthParmRange)findPgmParm("Amp_Env_Attack"), mp) );
		parmTranslators.add(new NL2ModTranslatorRangeToTable((SynthParmRange)findPgmParm("Amp_Env_Attack_Morph"),
				(SynthParmRange)findPgmParm("Amp_Env_Attack"),
				genPatch.findModuleParm("Amp Envelope", "Attack Mod Amt"), mp) );
		mp = genPatch.findModuleParm("Amp Envelope", "Decay");
		parmTranslators.add(new ParmTranslatorRangeToTable((SynthParmRange)findPgmParm("Amp_Env_Decay"),
				mp) );
//		parmTranslators.add(new MorphTranslatorRangeToTable((SynthParmRange)findPgmParm("Amp_Env_Decay_Morph"),
//				(SynthParmRange)findPgmParm("Amp_Env_Decay"), mp) );
		parmTranslators.add(new NL2ModTranslatorRangeToTable((SynthParmRange)findPgmParm("Amp_Env_Decay_Morph"),
				(SynthParmRange)findPgmParm("Amp_Env_Decay"),
				genPatch.findModuleParm("Amp Envelope", "Decay Mod Amt"), mp) );
		mp = genPatch.findModuleParm("Amp Envelope", "Sustain");
		parmTranslators.add(new ParmTranslatorPct((SynthParmRange)findPgmParm("Amp_Env_Sustain"),
				mp) );
//		parmTranslators.add(new MorphTranslatorPct((SynthParmRange)findPgmParm("Amp_Env_Sustain_Morph"),
//				(SynthParmRange)findPgmParm("Amp_Env_Sustain"), mp) );
		parmTranslators.add(new NL2ModTranslatorPct((SynthParmRange)findPgmParm("Amp_Env_Sustain_Morph"),
				(SynthParmRange)findPgmParm("Amp_Env_Sustain"),
				genPatch.findModuleParm("Amp Envelope", "Sustain Mod Amt"), mp) );
		mp = genPatch.findModuleParm("Amp Envelope", "Release");
		parmTranslators.add(new ParmTranslatorRangeToTable((SynthParmRange)findPgmParm("Amp_Env_Release"),
				mp) );
//		parmTranslators.add(new MorphTranslatorRangeToTable((SynthParmRange)findPgmParm("Amp_Env_Release_Morph"),
//				(SynthParmRange)findPgmParm("Amp_Env_Release"), mp) );
		parmTranslators.add(new NL2ModTranslatorRangeToTable((SynthParmRange)findPgmParm("Amp_Env_Release_Morph"),
				(SynthParmRange)findPgmParm("Amp_Env_Release"),
				genPatch.findModuleParm("Amp Envelope", "Release Mod Amt"), mp) );
		parmTranslators.add(new ParmTranslatorPct((SynthParmRange)findPgmParm("Gain"),
				genPatch.findModuleParm("Audio Out", "Level Amt1")) );
		parmTranslators.add(new NL2ModTranslatorPct((SynthParmRange)findPgmParm("Gain_Morph"),
				(SynthParmRange)findPgmParm("Gain"),
				genPatch.findModuleParm("Audio Out", "Level Amt2"),
				genPatch.findModuleParm("Audio Out", "Level Amt1")) );

		mp = genPatch.findModuleParm("Filter Envelope", "Attack");
		parmTranslators.add(new ParmTranslatorRangeToTable((SynthParmRange)findPgmParm("Filter_Env_Attack"),
				mp) );
//		parmTranslators.add(new MorphTranslatorRangeToTable((SynthParmRange)findPgmParm("Filter_Env_Attack_Morph"),
//				(SynthParmRange)findPgmParm("Filter_Env_Attack"), mp) );
		parmTranslators.add(new NL2ModTranslatorRangeToTable((SynthParmRange)findPgmParm("Filter_Env_Attack_Morph"),
				(SynthParmRange)findPgmParm("Filter_Env_Attack"),
				genPatch.findModuleParm("Filter Envelope", "Attack Mod Amt"), mp) );
		mp = genPatch.findModuleParm("Filter Envelope", "Decay");
		parmTranslators.add(new ParmTranslatorRangeToTable((SynthParmRange)findPgmParm("Filter_Env_Decay"),
				mp) );
//		parmTranslators.add(new MorphTranslatorRangeToTable((SynthParmRange)findPgmParm("Filter_Env_Decay_Morph"),
//				(SynthParmRange)findPgmParm("Filter_Env_Decay"), mp) );
		parmTranslators.add(new NL2ModTranslatorRangeToTable((SynthParmRange)findPgmParm("Filter_Env_Decay_Morph"),
				(SynthParmRange)findPgmParm("Filter_Env_Decay"),
				genPatch.findModuleParm("Filter Envelope", "Decay Mod Amt"), mp) );
		mp = genPatch.findModuleParm("Filter Envelope", "Sustain");
		parmTranslators.add(new ParmTranslatorPct((SynthParmRange)findPgmParm("Filter_Env_Sustain"),
				mp) );
//		parmTranslators.add(new MorphTranslatorPct((SynthParmRange)findPgmParm("Filter_Env_Sustain_Morph"),
//				(SynthParmRange)findPgmParm("Filter_Env_Sustain"), mp) );
		parmTranslators.add(new NL2ModTranslatorPct((SynthParmRange)findPgmParm("Filter_Env_Sustain_Morph"),
				(SynthParmRange)findPgmParm("Filter_Env_Sustain"),
				genPatch.findModuleParm("Filter Envelope", "Sustain Mod Amt"), mp) );
		mp = genPatch.findModuleParm("Filter Envelope", "Release");
		parmTranslators.add(new ParmTranslatorRangeToTable((SynthParmRange)findPgmParm("Filter_Env_Release"),
				mp) );
//		parmTranslators.add(new MorphTranslatorRangeToTable((SynthParmRange)findPgmParm("Filter_Env_Release_Morph"),
//				(SynthParmRange)findPgmParm("Filter_Env_Release"), mp) );
		parmTranslators.add(new NL2ModTranslatorRangeToTable((SynthParmRange)findPgmParm("Filter_Env_Release_Morph"),
				(SynthParmRange)findPgmParm("Filter_Env_Release"),
				genPatch.findModuleParm("Filter Envelope", "Release Mod Amt"), mp) );
		mp = genPatch.findModuleParm("Filter", "Resonance");
		parmTranslators.add(new ParmTranslatorPct((SynthParmRange)findPgmParm("Resonance"),
				mp) ); // FIXME seems like Notch+LP resonance is about 66% of max for regular LP
//		parmTranslators.add(new MorphTranslatorPct((SynthParmRange)findPgmParm("Resonance_Morph"),
//				(SynthParmRange)findPgmParm("Resonance"), mp) );
		parmTranslators.add(new NL2ModTranslatorPct((SynthParmRange)findPgmParm("Resonance_Morph"),
				(SynthParmRange)findPgmParm("Resonance"),
				genPatch.findModuleParm("Filter", "Resonance Mod Amt"), mp) );
		parmLinks.add(new ParmLinkTranslatorDirect(genPatch.findModuleParm("Notch Filter", "Resonance").getLink()));
		mp = genPatch.findModuleParm("Filter", "Expo FM Amt1");
		parmTranslators.add(new ParmTranslatorDirect(findPgmParm("Filter_Env_Amount"),
				mp) );
		parmTranslators.add(new NL2ModTranslatorDirect((SynthParmRange)findPgmParm("Filter_Env_Amount_Morph"),
				genPatch.findModuleParm("Filter", "Expo FM Amt6")) );
		parmLinks.add(new ParmLinkTranslatorDirect(genPatch.findModuleParm("Notch Filter", "Expo FM Amt1").getLink()));
		parmLinks.add(new ParmLinkTranslatorDirect(genPatch.findModuleParm("Notch Filter", "Expo FM Amt2").getLink()));
		parmLinks.add(new ParmLinkTranslatorDirect(genPatch.findModuleParm("Notch Filter", "Expo FM Amt3").getLink()));
		parmLinks.add(new ParmLinkTranslatorDirect(genPatch.findModuleParm("Notch Filter", "Expo FM Amt4").getLink()));
		parmLinks.add(new ParmLinkTranslatorDirect(genPatch.findModuleParm("Notch Filter", "Expo FM Amt6").getLink()));
		parmLinks.add(new ParmLinkTranslatorDirect(genPatch.findModuleParm("Notch Filter", "Expo FM Amt7").getLink()));
		parmTranslators.add(new ParmTranslatorTable((SynthParmTable)findPgmParm("Filter_Kbd_Track"),
				genPatch.findModuleParm("Filter", "Key Track")) );
		parmLinks.add(new ParmLinkTranslatorDirect(genPatch.findModuleParm("Notch Filter", "Key Track").getLink()));
		// FIXME Distortion ignored


		mp = genPatch.findModuleParm("LFO1", "Rate");
		parmTranslators.add(new ParmTranslatorRangeToTable((SynthParmRange)findPgmParm("LFO1_Rate"),
				mp) );
//		parmTranslators.add(new MorphTranslatorRangeToTable((SynthParmRange)findPgmParm("LFO1_Rate_Morph"),
//				(SynthParmRange)findPgmParm("LFO1_Rate"), mp) );
		parmTranslators.add(new NL2ModTranslatorRangeToTable((SynthParmRange)findPgmParm("LFO1_Rate_Morph"),
				(SynthParmRange)findPgmParm("LFO1_Rate"),
				genPatch.findModuleParm("LFO1", "Rate Mod Amt"), mp) );
		parmTranslators.add(new ParmTranslatorTable((SynthParmTable)findPgmParm("LFO1_Wave"),
				genPatch.findModuleParm("LFO1", "Waveform")) );

		mp = genPatch.findModuleParm("LFO2", "Rate");
		parmTranslators.add(new ParmTranslatorRangeToTable((SynthParmRange)findPgmParm("LFO2_Rate"),
				mp) );
//		parmTranslators.add(new MorphTranslatorRangeToTable((SynthParmRange)findPgmParm("LFO2_Rate_Morph"),
//				(SynthParmRange)findPgmParm("LFO2_Rate"), mp) );
		parmTranslators.add(new NL2ModTranslatorRangeToTable((SynthParmRange)findPgmParm("LFO2_Rate_Morph"),
				(SynthParmRange)findPgmParm("LFO2_Rate"),
				genPatch.findModuleParm("LFO2", "Rate Mod Amt"), mp) );

		mp = genPatch.findModuleParm("Mod Envelope", "Attack");
		parmTranslators.add(new ParmTranslatorRangeToTable((SynthParmRange)findPgmParm("Mod_Env_Attack"),
				mp) );
//		parmTranslators.add(new MorphTranslatorRangeToTable((SynthParmRange)findPgmParm("Mod_Env_Attack_Morph"),
//				(SynthParmRange)findPgmParm("Mod_Env_Attack"), mp) );
		parmTranslators.add(new NL2ModTranslatorRangeToTable((SynthParmRange)findPgmParm("Mod_Env_Attack_Morph"),
				(SynthParmRange)findPgmParm("Mod_Env_Attack"),
				genPatch.findModuleParm("Mod Envelope", "Attack Mod Amt"), mp) );
		mp = genPatch.findModuleParm("Mod Envelope", "Decay");
		parmTranslators.add(new ParmTranslatorRangeToTable((SynthParmRange)findPgmParm("Mod_Env_Decay"),
				mp) );
//		parmTranslators.add(new MorphTranslatorRangeToTable((SynthParmRange)findPgmParm("Mod_Env_Decay_Morph"),
//				(SynthParmRange)findPgmParm("Mod_Env_Decay"), mp) );
		parmTranslators.add(new NL2ModTranslatorRangeToTable((SynthParmRange)findPgmParm("Mod_Env_Decay_Morph"),
				(SynthParmRange)findPgmParm("Mod_Env_Decay"),
				genPatch.findModuleParm("Mod Envelope", "Decay Mod Amt"), mp) );
	}

	/**
	 * look for tags for a stored patch
	 */
	public boolean matchXMLStored(String xml) {
		String tag[], s;

		s = matchXMLTop(xml);
		if (s == null) {
			return false;
		}
		XMLReader xr = new XMLReader(s);
		tag = xr.getNextTag();
		if (tag == null || tag[0].equalsIgnoreCase("version") == false) {
			return false;
		}
		tag = xr.getNextTag();
		if (tag == null || tag[0].equalsIgnoreCase("global_channel") == false) {
			return false;
		}
		tag = xr.getNextTag();
		if (tag == null || tag[0].equalsIgnoreCase("bank_number") == false) {
			return false;
		}
		tag = xr.getNextTag();
		if (tag == null || tag[0].equalsIgnoreCase("patch_number") == false) {
			return false;
		}
		return true;
	}

	/**
	 * look for tags for an edit buffer patch
	 */
	public boolean matchXMLEdit(String xml) {
		String tag[], s;
	
		s = matchXMLTop(xml);
		if (s == null) {
			return false;
		}
		XMLReader xr = new XMLReader(s);
		tag = xr.getNextTag();
		if (tag == null || tag[0].equalsIgnoreCase("version") == false) {
			return false;
		}
		tag = xr.getNextTag();
		if (tag == null || tag[0].equalsIgnoreCase("global_channel") == false) {
			return false;
		}
		tag = xr.getNextTag();
		if (tag == null || tag[0].equalsIgnoreCase("edit_buffer") == false) {
			return false;
		}
		return true;
	}

	void translateFromGeneric() {
		// FIXME cheating by initializing all parms, then copying select known parms
		//  from the generic patch
		fromSysex(initSysex);
		super.translateFromGeneric();

		String pwOsc1 = null, pwOsc1Morph = null, pwmOsc1 = null;
		SynthParm sp;
		SynthParmRange spr, spr2;
		Module mod;
		ModuleParm mp, mp2;
		ModuleJack mj, mjTarget;
		int i, j;

		mod = genPatch.findModule("Voice Parms");
		mp = mod.findParm("Voice Mode");
		sp = findPgmParm("Voice_Mode");
		if (mp.getValue().equalsIgnoreCase("Poly")) {
			sp.setValue("Poly");
		} else {
			// FIXME should make sure both envs set to same trigger mode
			mod = genPatch.findModule("Filter Envelope");
			mp = mod.findParm("Trigger");
			if (mp.getValue().equalsIgnoreCase("Single")) {
				sp.setValue("Legato");
			} else {
				sp.setValue("Mono");
			}
		}
//		// need to recompute mod wheel dest from connections
//		SynthParmTable spt;
//		spt = (SynthParmTable)findPgmParm("Mod_Wheel_Dest");
//		if (genPatch.findConnection("Voice Parms", "Mod Wheel Out",
//				"Filter", "Expo FM In4") != null) {
//			spt.setValue("Filter");
//		} else if (genPatch.findConnection("Voice Parms", "Mod Wheel Out",
//					"FM Wheel", "Level In1") != null) {
//				spt.setValue("FM");
//		}

		mod = genPatch.findModule("Osc1");
		mp = mod.findParm("Waveform");
		if (mp.getValue().equalsIgnoreCase("Pulse")) {
			mp = mod.findParm("Pulse Width");
			pwOsc1 = mp.getValue();
			spr = (SynthParmRange)findPgmParm("Pulse_Width");
			new ParmTranslatorRangeConvert(spr, mp).fromGeneric();
//			spr2 = (SynthParmRange)findPgmParm("Pulse_Width_Morph");
//			new MorphTranslatorRangeConvert(spr2, spr, mp).toGeneric();
			new NL2ModTranslatorRangeConvert((SynthParmRange)findPgmParm("Pulse_Width_Morph"),
					(SynthParmRange)findPgmParm("Pulse_Width"),
					genPatch.findModuleParm("Osc1", "PWM Amt3"),
					genPatch.findModuleParm("Osc1", "Pulse Width")).fromGeneric();
		}
		mod = genPatch.findModule("Osc2");
		mp = mod.findParm("Waveform");
		if (mp.getValue().equalsIgnoreCase("Pulse")) {
			mp = mod.findParm("Pulse Width");
			if (pwOsc1 != null && mp.getValue().equals(pwOsc1) == false) {
				System.out.println("Warning - pulse width for Osc1=" + pwOsc1 +
						" not equal Osc2=" + mp.getValue() + "; using Osc1 value");
			} else {
				spr = (SynthParmRange)findPgmParm("Pulse_Width");
				new ParmTranslatorRangeConvert(spr, mp).fromGeneric();
				// FIXME get morph for Osc2, check against Osc1
			}
		}
		mp = mod.findParm("Fine Tune");
		spr = (SynthParmRange)findPgmParm("OSC2_Fine_Tune");
		// range is +/-50 cents; this will convert properly
		spr.setValue(Util.pctToParm(mp.getValue(), spr.getLow() * 2, spr.getHi() * 2));
		mp = mod.findParm("Fine Tune Mod Amt");
		spr = (SynthParmRange)findPgmParm("OSC2_Fine_Tune_Morph");
		// range is +/-50 cents; this will convert properly
		spr.setValue(Util.pctToParm(mp.getValue(), spr.getLow(), spr.getHi()));

		for (i = 0; i < genPatch.getMatrixMods().size(); i++) {
			((MatrixMod)genPatch.getMatrixMods().get(i)).fromGeneric();
		}

		sp = findPgmParm("Frequency");
		spr = (SynthParmRange)findPgmParm("Frequency_Morph");
		mod = genPatch.findModule("Filter");
		mp = mod.findParm("Frequency");
		mp2 = mod.findParm("Expo FM Amt5");
		i = new Integer(mp.getValue()).intValue();
		sp.setValue(i + 22);
		spr.setValue(mp2.getValue());
//		if (mp.getMorph() != null && mp.getMorph().isUsed()) {
//			j = new Integer(mp.getMorph().getValue()).intValue();
//			spr.setValue(j - i);
//		}
		// FIXME is something like this needed?
//		new ParmLinkTranslatorDirect(genPatch.findModuleParm("Notch Filter", "Frequency").getLink()).fromGeneric();
		// Must set after matrix mod, since it'll give wrong Filter_Type
		mp = mod.findParm("Filter Type");
		mp2 = mod.findParm("Filter Slope");
		sp = findPgmParm("Filter_Type");
		if (mp.getValue().equalsIgnoreCase("LP")) {
			if (mp2.getValue().equalsIgnoreCase("12db")) {
				sp.setValue("LP 12db");
			} else {
				sp.setValue("LP 24db");
			}
		} else if (mp.getValue().equalsIgnoreCase("HP") &&
				mp2.getValue().equalsIgnoreCase("24db")) {
			sp.setValue("HP 24db");
		} else if (mp.getValue().equalsIgnoreCase("BP") &&
				mp2.getValue().equalsIgnoreCase("12db")) {
			sp.setValue("BP 12db");
		// FIXME need new version, checking for filters in series
//		} else if (mp.getValue().equalsIgnoreCase("Notch + LP") &&
//				mp2.getValue().equalsIgnoreCase("12db")) {
//			sp.setValue("Notch + LP 12db");
		} // FIXME what to do if type not supported?

		// Matrix mod will connect Osc2 or Noise to mixer, but won't
		// properly set Osc2 waveform.  Must do it here.
		new ParmTranslatorDirect(findPgmParm("OSC2_Waveform"),
				genPatch.findModuleParm("Osc2", "Waveform")).fromGeneric();
	}

	void translateToGeneric() throws PatchDefinitionException {
//		super.translateToGeneric();

		SynthParm sp, spAmt;
		SynthParmRange spr, spr2;
		Module mod, modOsc1, modOsc2;
		ModuleParm mp, mp2;
		ModuleJack mj;
		int i;

		genPatch.findModuleParm("Voice Parms", "Unison Detune").initialize();
		genPatch.findModuleParm("Osc1", "Base Key").initialize();
		genPatch.findModuleParm("Osc1", "Pulse Width Range").initialize();
		genPatch.findModuleParm("Osc2", "Base Key").initialize();
		genPatch.findModuleParm("Osc2", "Pulse Width Range").initialize();
		genPatch.findModuleParm("Filter", "Base Key").initialize();
		genPatch.findModuleParm("VCA", "Level Amt1").initialize();

		for (i = 0; i < parmTranslators.size(); i++) {
			((ParmTranslator)parmTranslators.get(i)).toGeneric();
		}
		for (i = 0; i < parmLinks.size(); i++) {
			((ParmTranslator)parmLinks.get(i)).toGeneric();
		}

		mod = genPatch.findModule("Voice Parms");
		mp = mod.findParm("Voice Mode");
		mp2 = mod.findParm("Unison Voices");
		sp = findPgmParm("Voice_Mode");
		if (sp.getValue().equalsIgnoreCase("Poly")) {
			mp.setValue("Poly");
			mp2.setValue("2");
		} else {
			mp.setValue("Mono");
			mp2.setValue("4");
			genPatch.findModuleParm("Voice Parms", "Unison Detune").setValue("8.5");
		}
		sp = findPgmParm("Mod_Wheel_Dest");
		if (sp.getValue().equalsIgnoreCase("Morph")) {
			morphControl = "Mod Wheel";
		}

		modOsc1 = genPatch.findModule("Osc1");
		// FIXME s.b. osc1 used if osc_1_2_mix < 127 or fm is used 
		// FIXME not using yet; I think rule should be -
		//  used if ring mod off && (FM amt > 0 || mod_env mods amt || lfo1 mods amt)
		// Actually: FM can still be invoked by LFO1 or Mod Wheel when ring mod is used
		mp = modOsc1.findParm("Pulse Width");
		spr = (SynthParmRange)findPgmParm("Pulse_Width");
		new ParmTranslatorRangeConvert(spr, mp).toGeneric();
//		spr2 = (SynthParmRange)findPgmParm("Pulse_Width_Morph");
//		new MorphTranslatorRangeConvert(spr2, spr, mp).toGeneric();
		new NL2ModTranslatorRangeConvert((SynthParmRange)findPgmParm("Pulse_Width_Morph"),
				(SynthParmRange)findPgmParm("Pulse_Width"),
				genPatch.findModuleParm("Osc1", "PWM Amt3"),
				genPatch.findModuleParm("Osc1", "Pulse Width")).toGeneric();

		modOsc2 = genPatch.findModule("Osc2");
		// FIXME s.b. osc2 used if osc_1_2_mix > 0 
		// FIXME not using yet; I think rule should be -
		//  used if ring mod off && (FM amt > 0 || mod_env mods amt || lfo1 mods amt) 
		mp = modOsc2.findParm("Pulse Width");
		spr = (SynthParmRange)findPgmParm("Pulse_Width");
		new ParmTranslatorRangeConvert(spr, mp).toGeneric();
//		spr2 = (SynthParmRange)findPgmParm("Pulse_Width_Morph");
//		new MorphTranslatorRangeConvert(spr2, spr, mp).toGeneric();
		new NL2ModTranslatorRangeConvert((SynthParmRange)findPgmParm("Pulse_Width_Morph"),
				(SynthParmRange)findPgmParm("Pulse_Width"),
				genPatch.findModuleParm("Osc2", "PWM Amt3"),
				genPatch.findModuleParm("Osc2", "Pulse Width")).toGeneric();
		mp = modOsc2.findParm("Fine Tune");
		spr = (SynthParmRange)findPgmParm("OSC2_Fine_Tune");
		// range is +/-50 cents; this will convert properly
		i = spr.getIntValue();
		mp.setValue(Util.parmToPct(i, spr.getLow() * 2, spr.getHi() * 2));
//		spr2 = (SynthParmRange)findPgmParm("OSC2_Fine_Tune_Morph");
//		if (spr2.getIntValue() != 0) {
//			i += spr2.getIntValue();
//			mp.getMorph().setUsed(true);
//			mp.getMorph().setValue(Util.parmToPct(i, spr.getLow() * 2, spr.getHi() * 2));
//		}
		mp = modOsc2.findParm("Fine Tune Mod Amt");
		spr = (SynthParmRange)findPgmParm("OSC2_Fine_Tune_Morph");
		// range is +/-50 cents; this will convert properly
		i = spr.getIntValue();
		mp.setValue(Util.parmToPct(i, spr.getLow(), spr.getHi()));

		// FIXME redo this using Mod Amt's ?
//		mod = genPatch.findModule("Mixer");
//		mp = mod.findParm("Audio Amt1");
//		mp2 = mod.findParm("Audio Amt2");
//		if (mp2.getMorph().isUsed()) {
//			mp.getMorph().setUsed(true);
//			double d, d2;
//			d = new Double(mp2.getMorph().getValue()).doubleValue() -
//					new Double(mp2.getValue()).doubleValue();
//			d2 = new Double(mp.getValue()).doubleValue() - d;
//			mp.getMorph().setValue(Double.toString(d2));
//		}

		spr = (SynthParmRange)findPgmParm("Frequency");
		mod = genPatch.findModule("Filter");
		mp = mod.findParm("Frequency");
		mp.setValue(new Integer(spr.getIntValue() - 22).toString());
		mp2 = genPatch.findModuleParm("Notch Filter", "Frequency");
		mp2.setValue(new Integer(spr.getIntValue() - 28).toString());
//		new ParmLinkTranslatorRangeConvert(genPatch.findModuleParm("Notch Filter", "Frequency").getLink(), -28, 99).toGeneric(); // FIXME what range to use?
		spr = (SynthParmRange)findPgmParm("Frequency_Morph");
		mp2 = mod.findParm("Expo FM Amt5");
		mp2.setValue(spr.getValue());
//		if (spr.getIntValue() != 0) {
//			i += spr.getIntValue();
//			mp.getMorph().setUsed(true);
//			mp.getMorph().setValue(Integer.toString(i));
//		}

		MatrixMod mm;
		for (i = 0; i < genPatch.getMatrixMods().size(); i++) {
			mm = (MatrixMod)genPatch.getMatrixMods().get(i);
			mm.toGeneric();
		}

		Connection conn = genPatch.findConnection("Tremolo", "VCA Out", "VCA", "VCA In");
		if (conn != null) {
			spr = (SynthParmRange)findPgmParm("LFO2_Amount_Arp_Range");
			spr2 = (SynthParmRange)findPgmParm("LFO2_Amount_Arp_Range_Morph");
			if (spr.getIntValue() == 0 && spr2.getIntValue() == 0) {
				// No need to have Tremolo if it's not being used (mod amount is zero)
				genPatch.removeConnection(conn);
				genPatch.addConnectionIfNotFound("Filter", "Filter Out", "VCA", "VCA In");
			}
		}

		sp = findPgmParm("Mod_Wheel_Dest");
		if (sp.getValue().equalsIgnoreCase("Filter")) {
			// works like frequency knob - moves thru its full range
			genPatch.findModuleParm("Filter", "Expo FM Amt4").setValue("127");
//		} else if (sp.getValue().equalsIgnoreCase("FM")) {
//			// separate FM amount, works just like the knob
//			genPatch.findModuleParm("FM Wheel", "Level Amt1").setValue("127");
		} else if (sp.getValue().equalsIgnoreCase("OSC2")) {
			// FIXME not done - increases pitch up to 4 oct 11 semi; expo curve
		} else if (sp.getValue().equalsIgnoreCase("LFO1")) {
			// FIXME not done - LFO1 amount, works just like the knob
		} // else Morph

		mod = genPatch.findModule("Filter");
		mp = mod.findParm("Filter Type");
		mp2 = mod.findParm("Filter Slope");
		sp = findPgmParm("Filter_Type");
		if (sp.getValue().equalsIgnoreCase("LP 12db")) {
			mp.setValue("LP");
			mp2.setValue("12db");
		} else if (sp.getValue().equalsIgnoreCase("LP 24db")) {
			mp.setValue("LP");
			mp2.setValue("24db");
		} else if (sp.getValue().equalsIgnoreCase("HP 24db")) {
			mp.setValue("HP");
			mp2.setValue("24db");
		} else if (sp.getValue().equalsIgnoreCase("BP 12db")) {
			mp.setValue("BP");
			mp2.setValue("12db");
		} else {
//			mp.setValue("Notch + LP");
			mp.setValue("LP");
			mp2.setValue("12db");
			// Notch filter already has correct values (only one choice for each)
		}

		mod = genPatch.findModule("Filter Envelope");
		mp = mod.findParm("Trigger");
		sp = findPgmParm("Voice_Mode");
		if (sp.getValue().equalsIgnoreCase("Legato")) {
			mp.setValue("Single");
		} else {
			mp.setValue("Multi");
		}

		mod = genPatch.findModule("Amp Envelope");
		mp = mod.findParm("Trigger");
		sp = findPgmParm("Voice_Mode");
		if (sp.getValue().equalsIgnoreCase("Legato")) {
			mp.setValue("Single");
		} else {
			mp.setValue("Multi");
		}

		// "Noise" is not a generic osc waveform; only copy other values
		sp = findPgmParm("OSC2_Waveform");
		mp = genPatch.findModuleParm("Osc2", "Waveform");
		if (sp.getValue().equalsIgnoreCase("Pulse") ||
				sp.getValue().equalsIgnoreCase("Saw") ||
				sp.getValue().equalsIgnoreCase("Tri")) {
			new ParmTranslatorDirect(findPgmParm("OSC2_Waveform"),
					genPatch.findModuleParm("Osc2", "Waveform")).toGeneric();
		}

		// amp by lfo2: subtracts from volume in even amounts: 64=half- to full-volume;
		// 127=off to full.  So, acts like a negative-only modulator.
		if (findPgmParm("LFO2_Dest").getValue().equalsIgnoreCase("Amp")) {
			genPatch.findModuleOutputJack("LFO2", "Wave Out").setPolarity("negative");
		}

		/*
		 * Start at Synth Audio Out, work backwards across connections
		 * to find all modules that are either in the audio chain or
		 * are active modulators of those modules
		 */
		mj = genPatch.findModuleInputJack("Audio Out", "Level In1");
		genPatch.findJacksAndModulesUsed(mj);
	}

    /**
     * extract program data from sysex, and convert data from
     * sequence of nibbles into bytes
     */
    static byte[] sysexToData(byte sysex[], int start, int len) {
        byte data[] = new byte[len / 2];
        for (int i = start, j = 0; i < start + len; i+=2, j++) {
            data[j] = (byte)((sysex[i] & 0x0F) | (sysex[i + 1] << 4));
        }
        return data;
    }

	/**
	 * convert data to sequence of nibbles and make into complete
	 * sysex array of bytes
	 */
	static byte[] dataToSysex(byte data[], byte hdr[], int dataLen) {
		int len = hdr.length + dataLen + 1;
		byte sysex[] = new byte[len];
		System.arraycopy(hdr, 0, sysex, 0, hdr.length);
		for (int i = 0, j = hdr.length; i < (dataLen / 2); i++) {
			sysex[j++] = (byte)(data[i] & 0x0F);
			sysex[j++] = (byte)((data[i] & 0xF0) >>> 4);
		}
		sysex[len - 1] = (byte)0xF7;
		return sysex;
	}

	/**
	 * see if input sysex is a Nord Lead 2 program dump
	 * @param sysex
	 * @return valid
	 */
    public boolean matchSysex(byte sysex[]) {
    	if (sysex.length == INIT_SYSEX.length &&
    			sysex[0] == SYSEX_HDR[0] &&
				sysex[1] == SYSEX_HDR[1] &&
				sysex[3] == SYSEX_HDR[3]) {
			return true;
    	} else {
    		return false;
    	}
    }

	/**
	 * create NL2 sysex dump from internal variables
	 * @return sysex in byte array
	 */
	public void toSysex() {
		int i;
		byte data[] = new byte[dataLen / 2];
		for (i = 0; i < pgmParms.size(); i++) {
			((SynthParm)pgmParms.get(i)).putValueToSysex(data);
		}
		byte hdr[] = new byte[SYSEX_HDR.length];
		System.arraycopy(SYSEX_HDR, 0, hdr, 0, SYSEX_HDR.length);
		if (programType == 0) {
			for (i = 0; i < hdrParms.size(); i++) {
				((SynthParm)hdrParms.get(i)).putValueToSysex(hdr);
			}
		} else {
			for (i = 0; i < hdrParmsEdit.size(); i++) {
				((SynthParm)hdrParmsEdit.get(i)).putValueToSysex(hdr);
			}
		}
		byte syx[] = dataToSysex(data, hdr, dataLen); 
		System.arraycopy(syx, 0, sysex, 0, INIT_SYSEX.length);
	}

	/**
	 * read NL2 sysex dump into internal variables
	 */
	public void fromSysex(byte syx[]) {
		int i;
		if (matchSysex(syx) == false) {
			System.out.println("input file does not contain a " + manufacturerName +
					" " + itemName);
			return;
		}
		sysex = new byte[syx.length];
		System.arraycopy(syx, 0, sysex, 0, syx.length);
		byte data[] = sysexToData(syx, SYSEX_HDR.length, dataLen);
		if (syx[4] == 0) {
			programType = 1;
			for (i = 0; i < hdrParmsEdit.size(); i++) {
				((SynthParm)hdrParmsEdit.get(i)).getValueFromSysex(sysex);
			}
		} else {
			programType = 0;
			for (i = 0; i < hdrParms.size(); i++) {
				((SynthParm)hdrParms.get(i)).getValueFromSysex(sysex);
			}
		}
		for (i = 0; i < pgmParms.size(); i++) {
			((SynthParm)pgmParms.get(i)).getValueFromSysex(data);
		}
	}
}

class NL2ModuleOsc2 extends ModuleOsc {

	NL2ModuleOsc2(String pName, String pType, int pNumber) {
		super(pName, pType, pNumber);
	}

	NL2ModuleOsc2(String xml) throws PatchDefinitionException {
		super(xml);
	}

	public void seeIfParmsUsed() {
		super.seeIfParmsUsed();
		ModuleParm mp;
		mp = super.findParm("Sync");
		if (mp.getValue().equalsIgnoreCase("On") == false) {
			mp.setUsed(false);
		}
		mp = super.findParm("Ring Mod");
		if (mp.getValue().equalsIgnoreCase("On") == false) {
			mp.setUsed(false);
		}
	}
}

/**
 * For translating Mod Env PW mod amount
 */
class NL2ParmTranslatorPW implements ParmTranslator {
	private SynthParmRange sp;
	private ModuleParm mp;
	private ParmValidatorRange pv;

	NL2ParmTranslatorPW(SynthParmRange pSp, ModuleParm pMp) {
		sp = pSp;
		sp.getClass(); // referencing, to give error if it's null
		mp = pMp;
		pv = (ParmValidatorRange)mp.getPv();
	}

	public void toGeneric() {
		// range is +/-50%; this will convert properly
		mp.setValue(Util.parmToPct(sp.getIntValue(), sp.getLow() * 2, sp.getHi() * 2));
	}

	public void fromGeneric() {
		try {
			// range is +/-50%; this will convert properly
			int i = Util.pctToParm(mp.getValue(), sp.getLow() * 2, sp.getHi() * 2);
			sp.setValue(i);
		} catch (NumberFormatException e) {
			sp.setValue(mp.getValue());
		}
	}
}

/**
 * For translating Mod Env PW mod amount morph
 */
class NL2MorphTranslatorPW implements ParmTranslator {
	private SynthParmRange sp;
	private SynthParmRange bp;
	private ModuleParm mp;
	private ParmValidatorRange pv;
	private ParmMorph morph;

	NL2MorphTranslatorPW(SynthParmRange pSp, SynthParmRange pBp, ModuleParm pMp) {
		sp = pSp;
		bp = pBp;
		sp.getClass(); // referencing, to give error if it's null
		mp = pMp;
		pv = (ParmValidatorRange)mp.getPv();
		morph = mp.getMorph();
	}

	public void toGeneric() {
		int i = sp.getIntValue();
		if (i == 0) {
			morph.setUsed(false);
		} else {
			morph.setUsed(true);
			i += bp.getIntValue();
			// range is +/-50%; this will convert properly
			morph.setValue(Util.parmToPct(i, bp.getLow() * 2, bp.getHi() * 2));
		}
	}

	public void fromGeneric() {
		if (morph.isUsed() == false) {
			sp.setValue(0);
			return;
		}
		String valp, valm;
		valp = mp.getValue();
		valm = morph.getValue();
		if (valp.equals(valm)) {
			sp.setValue(0);
			return;
		}
		double d = new Double(valm).doubleValue() - new Double(valp).doubleValue();
		try {
			// range is +/-50%; this will convert properly
			int i = Util.pctToParm(Double.toString(d), bp.getLow() * 2, bp.getHi() * 2);
			sp.setValue(i);
		} catch (NumberFormatException e) {
			sp.setValue(0);
		}
	}
}

class NL2ModTranslatorDirect implements ParmTranslator {
	private SynthParmRange sp;
	private ModuleParm mp;
	private ParmValidatorRange pv;

	NL2ModTranslatorDirect(SynthParmRange pSp, ModuleParm pMp) {
		sp = pSp;
		sp.getClass(); // referencing, to give error if it's null
		mp = pMp;
		pv = (ParmValidatorRange)mp.getPv();
	}

	public void toGeneric() {
		// FIXME when presented with overly large morph amount, NL2 maxes out
		// partway thru morph range.  Will this to do the same?
		mp.setValue(sp.getValue());
	}

	public void fromGeneric() {
		sp.setValue(mp.getValue());
	}
}

class NL2ModTranslatorPct implements ParmTranslator {
	private SynthParmRange sp;
	private SynthParmRange bp;
	private ModuleParm mp;
	private ParmValidatorRange pv;
	private ModuleParm bmp;

	NL2ModTranslatorPct(SynthParmRange pSp, SynthParmRange pBp, ModuleParm pMp, ModuleParm pBmp) {
		sp = pSp;
		bp = pBp;
		sp.getClass(); // referencing, to give error if it's null
		bp.getClass(); // referencing, to give error if it's null
		mp = pMp;
		pv = (ParmValidatorRange)mp.getPv();
		bmp = pBmp;
	}

	public void toGeneric() {
		int i = sp.getIntValue();
		if (i == 0) {
			mp.setValue("0");
			return;
		}
		double base, morph;
		DecimalFormat df = new DecimalFormat("#.####");
		base = new Double(Util.parmToPct(bp.getIntValue(), bp.getLow(), bp.getHi())).doubleValue();
		i += bp.getIntValue();
		// FIXME when presented with overly large morph amount, NL2 maxes out
		// partway thru morph range.  Will this to do the same?
		morph = new Double(Util.parmToPct(i, bp.getLow(), bp.getHi())).doubleValue() - base;
		mp.setValue(df.format(morph));
	}

	public void fromGeneric() {
		if (mp.getValue().equals("0")) {
			sp.setValue(0);
			return;
		}
		int i, j;
		double base, morph;
		i = Util.pctToParm(bmp.getValue(), bp.getLow(), bp.getHi());
		base = new Double(bmp.getValue()).doubleValue();
		morph = new Double(mp.getValue()).doubleValue() + base;
		j = Util.pctToParm(morph, bp.getLow(), bp.getHi());
		sp.setValue(j - i);
	}
}

class NL2ModTranslatorRangeConvert implements ParmTranslator {
	private SynthParmRange sp;
	private SynthParmRange bp;
	private ModuleParm mp;
	private ParmValidatorRange pv;
	private ModuleParm bmp;
	private ParmValidatorRange bpv;

	NL2ModTranslatorRangeConvert(SynthParmRange pSp, SynthParmRange pBp, ModuleParm pMp, ModuleParm pBmp) {
		sp = pSp;
		bp = pBp;
		sp.getClass(); // referencing, to give error if it's null
		bp.getClass(); // referencing, to give error if it's null
		mp = pMp;
		pv = (ParmValidatorRange)mp.getPv();
		bmp = pBmp;
		bpv = (ParmValidatorRange)bmp.getPv();
	}

	public void toGeneric() {
		int i = sp.getIntValue();
		if (i == 0) {
			mp.setValue("0");
			return;
		}
		double base, morph;
		DecimalFormat df = new DecimalFormat("#.####");
		base = new Double(Util.rangeConvert(bp.getIntValue(),
				bp.getLow(), bp.getHi(), bpv.getLow(), bpv.getHi())).doubleValue();
		i += bp.getIntValue();
		// FIXME when presented with overly large morph amount, NL2 maxes out
		// partway thru morph range.  Will this to do the same?
		morph = new Double(Util.rangeConvert(i, bp.getLow(),
				bp.getHi(), bpv.getLow(), bpv.getHi())).doubleValue() - base;
		mp.setValue(df.format(morph));
	}

	public void fromGeneric() {
		if (mp.getValue().equals("0")) {
			sp.setValue(0);
			return;
		}
		int i, j;
		double d, base, morph;
		d = new Double(Util.rangeConvert(bmp.getValue(), bpv.getLow(), bpv.getHi(),
				bp.getLow(), bp.getHi())).doubleValue();
		if (d >= 0) {
			i = new Double(d + 0.5).intValue();
		} else {
			i = new Double(d - 0.5).intValue();
		}
		base = new Double(bmp.getValue()).doubleValue();
		morph = new Double(mp.getValue()).doubleValue() + base;
		d = new Double(Util.rangeConvert(morph, bpv.getLow(), bpv.getHi(),
				bp.getLow(), bp.getHi())).doubleValue();
		if (d >= 0) {
			j = new Double(d + 0.5).intValue();
		} else {
			j = new Double(d - 0.5).intValue();
		}
		sp.setValue(j - i);
	}
}

class NL2ModTranslatorRangeToTable implements ParmTranslator {
	private SynthParmRange sp;
	private SynthParmRange bp;
	private ModuleParm mp;
	private ParmValidatorRange pv;
	private ModuleParm bmp;
	private ParmValidatorNumTable bpv;

	NL2ModTranslatorRangeToTable(SynthParmRange pSp, SynthParmRange pBp, ModuleParm pMp, ModuleParm pBmp) {
		sp = pSp;
		bp = pBp;
		sp.getClass(); // referencing, to give error if it's null
		bp.getClass(); // referencing, to give error if it's null
		mp = pMp;
		pv = (ParmValidatorRange)mp.getPv();
		bmp = pBmp;
		bpv = (ParmValidatorNumTable)bmp.getPv();
	}

	public void toGeneric() {
		int i = sp.getIntValue();
		if (i == 0) {
			mp.setValue("0");
			return;
		}
		double base, morph;
		DecimalFormat df = new DecimalFormat("#.####");
		base = new Double(bpv.getTbl()[bp.getIntValue() - bp.getLow()]).doubleValue();
		i += bp.getIntValue() - bp.getLow();
		// FIXME when presented with overly large morph amount, NL2 maxes out
		// partway thru morph range.  Need to fix this to do the same.
		if (i < 0) {
			mp.setValue("-" + df.format(base));
			System.out.println("parm " + bp.getName() + " morph resulting index " +
					i + " out of range - using value " + mp.getValue());
		} else if (i > bpv.getTbl().length) {
			morph = new Double(bpv.getTbl()[bpv.getTbl().length - 1]).doubleValue() -
					base;
			mp.setValue(df.format(morph));
			System.out.println("parm " + bp.getName() + " morph resulting index " +
					i + " out of range - using value " + mp.getValue());
		} else {
			morph = new Double(bpv.getTbl()[i]).doubleValue() - base;
			mp.setValue(df.format(morph));
		}
	}

	public void fromGeneric() {
		if (mp.getValue().equals("0")) {
			sp.setValue(0);
			return;
		}
		int i, j;
		double base, morph;
		i = Util.matchToNumberTable(bmp.getValue(), bpv.getTbl());
		base = new Double(bmp.getValue()).doubleValue();
		morph = new Double(mp.getValue()).doubleValue() + base;
		j = Util.matchToNumberTable(Double.toString(morph), bpv.getTbl());
		sp.setValue(j - i);
	}
}
