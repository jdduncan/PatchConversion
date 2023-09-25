
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
 * Convert generic patch XML to Nord Modular v3 patch
 *
 * Notes:
 * - envelopes provide 0-64 unipolar units of modulation
 * - lfos provide -64 to +64 bipolar units of modulation
 * - keyboard "CV" is 0 at midi note 64 (E4), -64 at note 0, +63 at note 127
 * - Patch Settings/Octave Shift doesn't do anything on the Micro Modular
 *
 * Fix soon:
 * - 1.04 in progress.  Patch level is expo; converted - need to test
 * - Morph: if CV mixer used, need to separate morph so it's on CV mixer not
 *   original control
 * - Can I create NM patch cable "daisy-chain" routine?  All cables now are
 *   "home runs" from source to each dest, making the screen look cluttered.
 *   (Is there a down side to daisy-chaining?)
 * 
 * Version history:
 * 1.04 Patch gain moved from final VCA to output module.  Added gain morph.
 *      Changed max resonance without self-oscillating from 114 to 123.
 * 1.03 Convert separate mod jacks & attenuators to parm morph.
 *      Limit module name to 16 characters; longer name gives load error.
 * 1.02 Handle new module type mod_vca
 * 1.01 Increased NM3InputsMixer levels (50% was less than half volume).
 *      NMXFade mix setting was backwards 
 * 1.00 First major release of converted Nord Lead 2 sounds
 * 
 * @author Kenneth L. Martinez
 */

import java.io.*;
import java.util.*;
import java.text.*;

public class NordModularPatch /*extends SynthPatchAbstract*/ {
	private boolean valid;
	private String version = "1.01";
	private String synthGenericVersion;
//	protected GenericPatch genPatch;
	private ArrayList nmModules;
	private ArrayList nmCables;
	private ArrayList nmControls;
	private Module voiceParms;
	private int numVoices;
	private int portamento;
	private int fingeredPortamento;
	private int transpose;
	private int unison;
	private ArrayList nmMorphMap;
	private boolean keyVelocityMorph;
	private String inputFile;
	private String patchName;
	private String patchNumber;
	private String patchBank;
	private String patchComment;

	public static void main(String args[]) throws IOException, PatchDefinitionException {
		if (args.length == 3) {
			System.out.println("----------------------------------------");
			System.out.println("NordModularPatch " + args[0] + " " + args[1] +
					" " + args[2]);
			if (args[0].equalsIgnoreCase("toSysex")) {
				try {
					BufferedReader in = new BufferedReader(new FileReader(args[1]));
					String s;
					StringBuffer sb = new StringBuffer();
					while ((s = in.readLine()) != null) {
						sb.append(s + System.getProperty("line.separator"));
					}
					in.close();
					NordModularPatch nm = new NordModularPatch(args[1]);
					nm.fromXML(sb.toString());

					if (nm.isValid()) {
						PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(args[2])));
						nm.writePatchFile(out);
						out.close();
					} else {
						System.out.println("input file " + args[1] + " did not contain valid XML: "/* + nm.getInvalidMsg()*/);
					}
				} catch (FileNotFoundException e) {
					System.out.println("unable to open input file " + args[1]);
				}
				System.out.println("   done.");
				return;
			} else {
				System.out.println("invalid run type '" + args[0] + "'");
			}
		}

		System.out.println("please specify toSysex followed by input and output filenames");
	}
	
	NordModularPatch(String s) {
		valid = false;
//		genPatch = new GenericPatch();
		nmModules = new ArrayList();
		nmCables = new ArrayList();
		nmControls = new ArrayList();
		numVoices = 16;
		portamento = 0;
		fingeredPortamento = 0;
		transpose = 2;
		nmMorphMap = new ArrayList();
		keyVelocityMorph = false;
		inputFile = s;
	}

	public boolean isValid() {
		return valid;
	}

//	public ArrayList getNmMorphMap() {
//		return nmMorphMap;
//	}

	public void addMorph(String s) {
		if (nmMorphMap.size() < 25) {
			nmMorphMap.add(s);
		} else if (nmMorphMap.size() == 25) {
			System.out.println("Warning: Can't add more than 25 morphs - the rest will be ignored");
		}
	}

	public void fromXML(String xml) throws PatchDefinitionException {
		if (xml.indexOf("generic_patch") == -1) {
			System.out.println("input file has no generic patch");
			return;
		}

		MyGenericPatch mgp = new MyGenericPatch(this);
		if (mgp.readXML(xml)) {
			valid = true;
			patchName = mgp.getPatchName();
			patchNumber = mgp.getPatchNumber();
			patchBank = mgp.getPatchBank();
			patchComment = mgp.getPatchComment();
		}
	}

	void writePatchFile(PrintWriter out) throws PatchDefinitionException {
		String s, ls = System.getProperty("line.separator");
		NMModule nmMod;
		NMControl nmCon;
		int i;

		s = voiceParms.findParm("Transpose").getValue();
		transpose = new Integer(s).intValue() + 2;
		s = voiceParms.findParm("Voice Mode").getValue();
		if (s.equalsIgnoreCase("Poly")) {
			numVoices = 16;
		} else {
			numVoices = 1;
		}

		out.println("[Header]");
		out.println("Version=Nord Modular patch 3.0");
		out.println("0 127 0 127 2 " + portamento + " " + fingeredPortamento + " " +
				numVoices + " 600 " + transpose + " 1 1 1 1 1 1 1 1 1 1 1 1 1 ");
		out.println("[/Header]");
		out.println("[ModuleDump]");
		out.println("1 ");
		for (i = 0; i < nmModules.size(); i++) {
			nmMod = (NMModule)nmModules.get(i);
			out.println((i + 1) + " " + nmMod.writeModuleDump());
		}
		out.println("[/ModuleDump]");
		out.println("[ModuleDump]");
		out.println("0 ");
		out.println("[/ModuleDump]");
		out.println("[CurrentNoteDump]");
		out.println("64 0 0 64 0 0 ");
		out.println("[/CurrentNoteDump]");
		out.println("[CableDump]");
		out.println("1 ");
		for (i = 0; i < nmCables.size(); i++) {
			out.println((String)nmCables.get(i));
		}
		out.println("[/CableDump]");
		out.println("[CableDump]");
		out.println("0 ");
		out.println("[/CableDump]");
		out.println("[ParameterDump]");
		out.println("1 ");
		for (i = 0; i < nmModules.size(); i++) {
			nmMod = (NMModule)nmModules.get(i);
			s = nmMod.writeParameterDump();
			if (s != null) {
				out.println((i + 1) + " " + s);
			}
		}
		out.println("[/ParameterDump]");
		out.println("[ParameterDump]");
		out.println("0 ");
		out.println("[/ParameterDump]");
		if (nmMorphMap.size() > 0) {
			out.println("[MorphMapDump]");
			out.println("0 0 0 0 ");
			for (i = 0; i < nmMorphMap.size(); i++) {
				out.print((String)nmMorphMap.get(i));
			}
			out.println("");
			out.println("[/MorphMapDump]");
			if (keyVelocityMorph == true) {
				out.println("[KeyboardAssignment]");
				out.println("1 0 0 0 ");
				out.println("[/KeyboardAssignment]");
			}
		}
		if (nmControls.size() > 0) {
			out.println("[CtrlMapDump]");
			for (i = 0; i < nmControls.size(); i++) {
				nmCon = (NMControl)nmControls.get(i);
				out.println(nmCon.getSection() + " " + nmCon.getModIndex() + " " + nmCon.getParmIndex() +
						" " + nmCon.getCcNumber() + " ");
			}
			out.println("[/CtrlMapDump]");
		}
		out.println("[CustomDump]");
		out.println("1 ");
		for (i = 0; i < nmModules.size(); i++) {
			nmMod = (NMModule)nmModules.get(i);
			s = nmMod.writeCustomDump();
			if (s != null) {
				out.println((i + 1) + " " + s);
			}
		}
		out.println("[/CustomDump]");
		out.println("[CustomDump]");
		out.println("0 ");
		out.println("[/CustomDump]");
		out.println("[NameDump]");
		out.println("1 ");
		for (i = 0; i < nmModules.size(); i++) {
			nmMod = (NMModule)nmModules.get(i);
			out.println((i + 1) + " " + nmMod.writeNameDump());
		}
		out.println("[/NameDump]");
		out.println("[NameDump]");
		out.println("0 ");
		out.println("[/NameDump]");
		out.println("[Notes]");
		out.println("Produced by PatchConversion.NordModularPatch version 1.04");
		out.println("Converted from: " + inputFile);
		if (patchName != null) {
			out.println("Patch Name: " + patchName);
		}
		if (patchNumber != null) {
			out.println("Patch Number: " + patchNumber);
		}
		if (patchBank != null) {
			out.println("Patch Bank: " + patchBank);
		}
		if (patchComment != null) {
			out.println("Patch Comment: " + patchComment);
		}
		out.println("[/Notes]");
	}

	/**
	 * Find module, return its index in module table (first entry = 1)
	 */
	public int findNMModule(String name) {
		int i;
		NMModule m;

		// with few modules, a sequential search is fast enough
		for (i = 0; i < nmModules.size(); i++) {
			m = (NMModule)nmModules.get(i);
			if (m.getName().equalsIgnoreCase(name)) {
				return i + 1;
			}
		}
		return -1;
	}

class MyGenericPatch extends GenericPatch {
	private int mixerNum;
	private NordModularPatch nmp;

	MyGenericPatch(NordModularPatch pNmp) {
		super("0.07");
		nmp = pNmp;
		mixerNum = 0;
	}

	/**
	 * read XML into internal variables
	 */
	public boolean readXML(String xml) throws PatchDefinitionException {
		if (matchXML(xml) == false) {
			System.out.println("input file has no generic patch");
			return false;
		}

		// Read XML into temp patch structure of modules/jacks/connections
		GenericPatch tempGP = new GenericPatch("1.01");
		String tag[], sourceModule, sourceJack, targetModule, targetJack;
		Module mod, mod2;
		Connection conn;
		XMLReader xr = new XMLReader(XMLReader.getTagValue(xml, "generic_patch"));
		while ((tag = xr.getNextTag()) != null) {
			if (tag[0].equalsIgnoreCase("version")) {
				if (tag[1].equalsIgnoreCase(version) == false) {
					System.out.println("Warning: Expected generic patch version " +
							version + ", input is " + tag[1]);
				}
			} else if (tag[0].equalsIgnoreCase("synth_generic_version")) {
				// do anything with this?
			} else if (tag[0].equalsIgnoreCase("patch_name")) {
				setPatchName(tag[1]);
			} else if (tag[0].equalsIgnoreCase("patch_number")) {
				setPatchNumber(tag[1]);
			} else if (tag[0].equalsIgnoreCase("patch_bank")) {
				setPatchBank(tag[1]);
			} else if (tag[0].equalsIgnoreCase("patch_comment")) {
				setPatchComment(tag[1]);
			} else if (tag[0].equalsIgnoreCase("module")) {
				mod = new Module(tag[1]);
				mod.setUsed(3);
				tempGP.addModule(mod);
			} else {
				System.out.println("unknown tag " + tag[0]);
				return false;
			}
		}

		for (int i = 0; i < tempGP.getModules().size(); i++) {
			mod = (Module)tempGP.getModules().get(i);
			for (int j = 0; j < mod.getInputJacks().size(); j++) {
				ModuleInputJack mij = (ModuleInputJack)mod.getInputJacks().get(j);
				tempGP.addConnection(mij.buildConnection());
			}
		}

		// Make substitutions of some generic modules so they map better
		//  to NM modules.
		String s;
		String src, srcJack, dest, destJack;
		ModuleOutputJack moj, moj2;
		ModuleInputJack mij;
		ModuleParm mp, mp2;
		int i, j, y1 = 1, y2 = 1, y3 = 1, y4 = 1;

		// Convert mod_vca mod jack name if there's no base jack
		for (i = 0; i < tempGP.getModules().size(); i++) {
			mod = (Module)tempGP.getModules().get(i);
			if (mod.getType().equalsIgnoreCase("mod_vca") == false) {
				continue;
			}
			if (mod.findInputJack("Level In1") != null) {
				continue;
			}
			mij = mod.findInputJack("Level Amt1 Mod In");
			if (mij != null) {
				mij.setName("Level In1");
				mij.getAttenuator().setName("Level Amt1");
			}
		}

		convertModsToMorphs(tempGP);

		// Add portamento module if needed
		mod = tempGP.findModule("Voice Parms");
		mp = mod.findParm("Portamento");
		if (mp != null && (mp.getValue().equals("0") == false ||
				(mp.getMorph() != null && mp.getMorph().getValue().equals("0") == false))) {
			if (mod.findParm("Voice Mode").getValue().equalsIgnoreCase("Mono")) {
				if (mod.findParm("Fingered Portamento").getValue().equalsIgnoreCase("On")) {
					mod2 = new Module("Patch", "patch_parms", 0);
					i = tempGP.findModuleIndex("Voice Parms");
					tempGP.addModule(i, mod2); // insert before voice parms
					moj = new ModuleOutputJack("Gate Out", "control_output");
					mod2.addOutputJack(moj);
					mod2 = new Module("Portamento", "portamento_fingered", 0);
					i = tempGP.findModuleIndex("Voice Parms");
					tempGP.addModule(i, mod2); // insert before voice parms
					mij = new ModuleInputJack("Jmp In", "control_input");
					mod2.addInputJack(mij);
					tempGP.addConnection(new Connection(moj, mij));
				} else {
					mod2 = new Module("Portamento", "portamento_full", 0);
					i = tempGP.findModuleIndex("Voice Parms");
					tempGP.addModule(i, mod2); // insert before voice parms
				}
				mij = new ModuleInputJack("Note In", "control_input");
				mod2.addInputJack(mij);
				moj = new ModuleOutputJack("Note Out", "control_output");
				mod2.addOutputJack(moj);
				mp2 = new ModuleParm("Time", null, mp.getValue());
				mp2.setMorph(mp.getMorph());
				mod2.addParm(mp2);
				moj2 = new ModuleOutputJack("Note Out", "control_output");
				mod.addOutputJack(moj2);
				tempGP.addConnection(new Connection(moj2, mij));
				for (i = 0; i < tempGP.getModules().size(); i++) {
					mod2 = (Module)tempGP.getModules().get(i);
					if (mod2.getType().equalsIgnoreCase("osc")) {
						if (mod2.findParm("Key Track").getValue().equals("100") == false) {
							System.out.println("Warning: Module " + mod2.getName() +
									" Key Track is " + mod2.findParm("Key Track").getValue() +
									" - expected 100 for use with Portamento");
						}
						mod2.findParm("Key Track").setValue("0");
						mij = new ModuleInputJack("Expo FM In9", "audio_input");
						mp2 = new ModuleParm("Expo FM Amt9", null, "60.01");
						mod2.addParm(mp2);
						mod2.addInputJack(mij, mp2);
						tempGP.addConnection(new Connection(moj, mij));
					}
				}
			} else {
				System.out.println("Warning: Poly portamento not supported");
			}
		}

		// Convert mod_vca to vca plus constant
		for (i = 0; i < tempGP.getModules().size(); i++) {
			mod = (Module)tempGP.getModules().get(i);
			if (mod.getType().equalsIgnoreCase("mod_vca")) {
				mod.setType("vca");
				mij = new ModuleInputJack("Level In2", "control_input");
				mp = mod.findParm("Mod Offset");
				mp.setName("Level Amt2");
				mod.addInputJack(mij, mp);
				mod2 = new Module(mod.getName() + "Const", "constant", 0);
				i = tempGP.findModuleIndex(mod.getName());
				tempGP.addModule(i, mod2); // insert before vca
				moj = new ModuleOutputJack("Value Out", "control_output");
				mod2.addOutputJack(moj);
				tempGP.addConnection(new Connection(moj, mij));
			}
		}

		// Reduce numbers for modules, jacks and parms as needed, and add
		// CV mixers as needed.
		reduceModNumbers(tempGP);

		// Mod Wheel must be attached to Constant module
		mod = tempGP.findModule("Voice Parms");
		moj = mod.findOutputJack("Mod Wheel Out");
		// Make sure morph conversion didn't remove all connections
		if (moj != null && moj.getFirstConn() != null) {
			mod2 = new Module("Mod Wheel", "constant", 0);
			i = tempGP.findModuleIndex("Voice Parms");
			tempGP.addModule(i + 1, mod2); // insert after voice parms
			mod2.addParm(new ModuleParm("Value", null, "0"));
			moj.setName("Value Out");
			mod2.addOutputJack(moj);
			nmControls.add(new NMControl(i + 2, 0, 1));
		}

		// Merge final VCA into NM's Amp Envelope module (which incorporates
		//  a VCA); if that VCA is only controlled by the envelope, remove it.
		mod = tempGP.findModule("VCA");
		moj = mod.findOutputJack("VCA Out");
		if (moj != null &&
				tempGP.findConnectionFromSource(moj).getTargetJack().getMod().getName().equalsIgnoreCase("Audio Out") &&
				mod.findInputJack("Level In1") != null &&
				tempGP.findConnection("Amp Envelope", "Env Out", "VCA", "Level In1") != null) {
			// Not needed: the build-in VCA is hardwired to the envelope inside the module
			tempGP.removeConnection("Amp Envelope",	"Env Out", "VCA", "Level In1");
			mod2 = tempGP.findModule("Amp Envelope");
			// Find connections that went into & out of separate VCA, and
			//  redirect to Amp Envelope module
			conn = tempGP.findConnectionToTarget(mod.findInputJack("VCA In"));
			moj2 = conn.getSourceJack();
			tempGP.removeConnection(conn);
			mij = new ModuleInputJack("VCA In", "audio_input");
			mod2.addInputJack(mij);
			tempGP.addConnection(new Connection(moj2, mij));
			conn = tempGP.findConnectionFromSource(moj);
			moj2 = new ModuleOutputJack("VCA Out", "audio_output");
			mod2.addOutputJack(moj2);
			if (mod.findInputJack("Level In2") == null) {
				tempGP.removeConnection(conn);
				mij = conn.getTargetJack();
				tempGP.addConnection(new Connection(moj2, mij));
				// Remove separate VCA module from the patch
				tempGP.removeModule(mod);
			} else {
				mij = mod.findInputJack("VCA In");
				tempGP.addConnection(new Connection(moj2, mij));
				mod.removeInputJack(mod.findInputJack("Level In1"));
				reduceJackNumbers(mod, tempGP);
			}
		}

		// Audio Out module only has one input, but NM has two; add a second
		mod = tempGP.findModule("Audio Out");
		mij = new ModuleInputJack("Level In2", "audio_input");
		mod.addInputJack(mij);
		moj = tempGP.findModuleOutputJack("Amp Envelope", "VCA Out");
		if (moj == null) {
			moj = tempGP.findModuleOutputJack("VCA", "VCA Out");
		}
		tempGP.addConnection(new Connection(moj, mij));
		voiceParms = tempGP.findModule("Voice Parms");
		s = voiceParms.findParm("Unison").getValue();
		if (s.equalsIgnoreCase("On") == false) {
			unison = 0;
		} else {
			unison = 1;
			// Duplicate Oscs to make unison sound
			for (i = 0; i < tempGP.getModules().size(); i++) {
				mod = (Module)tempGP.getModules().get(i);
				if (mod.getType().equalsIgnoreCase("osc") &&
						mod.findParm("CloneOf") == null) {
					mod2 = (Module)mod.clone();
					mod2.setName(mod.getName() + "U");
					j = tempGP.findModuleIndex(mod.getName());
					tempGP.addModule(j + 1, mod2); // insert after osc
					mod2.addParm(new ModuleParm("CloneOf", null, mod.getName()));
					mp = mod2.findParm("Fine Tune");
					// default will be 17 cents for unison detune
					double d2 = 17, d = new Double(mp.getValue()).doubleValue();
					// look for Unison Detune value from Voice Parms
					mp2 = voiceParms.findParm("Unison Detune");
					if (mp2 != null) {
						d2 = new Double(mp2.getValue()).doubleValue();
						if (voiceParms.findParm("Unison Voices").getValue().equals("2") == false) {
							// If more than 2 voices stacked, first will have twice the detune
							d2 *= 2;
						}
					}
					mp.setValue(Double.toString(d + d2));
					if (mp.getMorph() != null) {
						d = new Double(mp.getMorph().getValue()).doubleValue();
						mp.getMorph().setValue(Double.toString(d + d2));
					}
					// Creating submixer for unison oscs, so that Xfade mixer can still be used
					Module modMix = new Module(mod.getName() + "UniMix", "mixer", 0);
					tempGP.addModule(j + 2, modMix); // insert after unison osc
					ModuleOutputJack mixMoj = new ModuleOutputJack("Mixer Out", "audio_output");
					modMix.addOutputJack(mixMoj);
					mp = new ModuleParm("Audio Amt1", "percent", null, "100");
					modMix.addParm(mp);
					ModuleInputJack mixMij1 = new ModuleInputJack("Audio In1", "audio_input");
					modMix.addInputJack(mixMij1, mp);
					mp = new ModuleParm("Audio Amt2", "percent", null, "100");
					modMix.addParm(mp);
					ModuleInputJack mixMij2 = new ModuleInputJack("Audio In2", "audio_input");
					modMix.addInputJack(mixMij2, mp);
					// Move mixer connection
					moj = mod.findOutputJack("Wave Out");
					Connection conns[] = moj.getConn();
					for (j = 0; j < conns.length; j++) {
						mij = conns[j].getTargetJack();
						if (mij.getPrefix().equalsIgnoreCase("Audio In") &&
								mij.getMod().getType().equalsIgnoreCase("mixer")) {
							tempGP.removeConnection(conns[j]);
							tempGP.addConnection(new Connection(mixMoj, mij));
							tempGP.addConnection(new Connection(moj, mixMij1));
							tempGP.addConnection(new Connection(mod2.findOutputJack("Wave Out"), mixMij2));
							break;
						}
					}
					// If more than 2 voices stacked, simulate with 3 osc stack
					mp = voiceParms.findParm("Unison Voices");
					if (mp.getValue().equals("2") == false) {
						mod2 = (Module)mod.clone();
						mod2.setName(mod.getName() + "V");
						j = tempGP.findModuleIndex(mod.getName() + "U");
						tempGP.addModule(j + 1, mod2); // insert after other unison osc
						mod2.addParm(new ModuleParm("CloneOf", null, mod.getName()));
						mp = mod2.findParm("Fine Tune");
						// default will be -9 cents for second unison osc
						d2 = -9;
						d = new Double(mp.getValue()).doubleValue();
						// look for Unison Detune value from Voice Parms
						mp2 = voiceParms.findParm("Unison Detune");
						if (mp2 != null) {
							d2 = new Double(mp2.getValue()).doubleValue() / -1;
						}
						mp.setValue(Double.toString(d + d2));
						if (mp.getMorph() != null) {
							d = new Double(mp.getMorph().getValue()).doubleValue();
							mp.getMorph().setValue(Double.toString(d + d2));
						}
						mp = new ModuleParm("Audio Amt3", "percent", null, "100");
						modMix.addParm(mp);
						ModuleInputJack mixMij3 = new ModuleInputJack("Audio In3", "audio_input");
						modMix.addInputJack(mixMij3, mp);
						tempGP.addConnection(new Connection(mod2.findOutputJack("Wave Out"), mixMij3));
					}
				}
			}
			for (i = 0; i < tempGP.getModules().size(); i++) {
				mod2 = (Module)tempGP.getModules().get(i);
				mp = mod2.findParm("CloneOf");
				if (mp != null) {
					// Loop thru each jack, adding corresponding connections.
					// Sync & FM connections must go to corresponding Uni osc
					Module mod3;
					mod = tempGP.findModule(mp.getValue());
					for (j = 0; j < mod.getInputJacks().size(); j++) {
						mij = (ModuleInputJack)mod.getInputJacks().get(j);
						conn = mij.getConn();
						moj = conn.getSourceJack();
						if (moj.getMod().getType().equalsIgnoreCase("osc")) {
							mod3 = tempGP.findModule(moj.getMod().getName() + "U");
							moj = mod3.findOutputJack(moj.getName());
						}
						tempGP.addConnection(new Connection(moj,
								mod2.findInputJack(mij.getName())));
					}
				}
			}
		}

		// LFOs, Mod Env may need polarity change
		for (i = 0; i < tempGP.getModules().size(); i++) {
			mod = (Module)tempGP.getModules().get(i);
			if (mod.getType().equalsIgnoreCase("lfo")) {
				moj = mod.findOutputJack("Wave Out");
				if (moj.getPolarity().equalsIgnoreCase("bipolar") == false) {
					mod2 = new Module("Unipolar" + mod.getNumber(), "level_shift", mod.getNumber());
					j = tempGP.findModuleIndex(mod.getName());
					tempGP.addModule(j + 1, mod2); // insert after lfo
					mod2.addParm(new ModuleParm("Invert", null, "Off"));
					mod2.addParm(new ModuleParm("Polarity", null, "negative"));
					mij = new ModuleInputJack("Audio In", "audio_input");
					mod2.addInputJack(mij);
					moj2 = new ModuleOutputJack("Audio Out", "audio_output", moj.getPolarity());
					mod2.addOutputJack(moj2);
					Connection conns[] = moj.getConn();
					for (j = 0; j < conns.length; j++) {
						tempGP.removeConnection(conns[j]);
						tempGP.addConnection(new Connection(moj2, conns[j].getTargetJack()));
					}
					tempGP.addConnection(new Connection(moj, mij));
				}
			} else if (mod.getType().equalsIgnoreCase("env_ar")) {
				moj = mod.findOutputJack("Env Out");
				// FIXME shouldn't just check first jack and assume all others are the same
				mij = moj.getFirstConn().getTargetJack();
				mp = mij.getAttenuator();
				double d = new Double(mp.getValue()).doubleValue();
				if (mij.getMod().getType().equalsIgnoreCase("cv_mixer") == false &&
						((moj.getPolarity().equalsIgnoreCase("negative") && d >= 0) ||
						(moj.getPolarity().equalsIgnoreCase("positive") && d <= 0))) {
					mod2 = new Module("Unipolar" + mod.getNumber(), "level_shift", mod.getNumber());
					j = tempGP.findModuleIndex(mod.getName());
					tempGP.addModule(j + 1, mod2); // insert after lfo
					mod2.addParm(new ModuleParm("Invert", null, "On"));
					mod2.addParm(new ModuleParm("Polarity", null, "bipolar"));
					mij = new ModuleInputJack("Audio In", "audio_input");
					mod2.addInputJack(mij);
					moj2 = new ModuleOutputJack("Audio Out", "audio_output", moj.getPolarity());
					mod2.addOutputJack(moj2);
					Connection conns[] = moj.getConn();
					for (j = 0; j < conns.length; j++) {
						tempGP.removeConnection(conns[j]);
						tempGP.addConnection(new Connection(moj2, conns[j].getTargetJack()));
						if (d < 0) {
							double val = new Double(conns[j].getTargetJack().getAttenuator().getValue()).doubleValue();
							conns[j].getTargetJack().getAttenuator().setValue(Double.toString(val * -1));
						}
					}
					tempGP.addConnection(new Connection(moj, mij));
				}
			} else if (mod.getType().equalsIgnoreCase("env_adsr")) {
				moj = mod.findOutputJack("Env Out");
				if (moj.getPolarity().equalsIgnoreCase("negative")) {
					mod.addParm(new ModuleParm("Invert", null, "On"));
				}
			}
		}
		// FIXME polarity change may apply to Osc (for P5-style positive-only expo FM) 

		// FIXME add gain control (attenuator) to VCAs with 1 level in

		// create NM patch from tempGP
		NMModule nmMod;
		String nmc;
		for (i = 0; i < tempGP.getModules().size(); i++) {
			mod = (Module)tempGP.getModules().get(i);
			if (mod.getType().equalsIgnoreCase("voice_parms")) {
				nmMod = new NMKeyboardVoice(mod.getName(), 3, y4, mod, nmp);
				y4 += 3;
				nmModules.add(nmMod);
			} else if (mod.getType().equalsIgnoreCase("patch_parms")) {
				nmMod = new NMKeyboardPatch(mod.getName(), 3, y4, mod, nmp);
				y4 += 4;
				nmModules.add(nmMod);
			} else if (mod.getType().equalsIgnoreCase("constant")) {
				nmMod = new NMConstant(mod.getName(), 2, y3, mod, nmp);
				y3 += 3;
				nmModules.add(nmMod);
			} else if (mod.getType().equalsIgnoreCase("osc")) {
				if (mod.findParm("CloneOf") == null) {
					nmMod = new NMOscA(mod.getName(), 1, y2, mod, nmp);
					y2 += 7;
				} else {
					nmMod = new NMOscA(mod.getName(), 1, y2 - 1, mod, nmp);
					y2 += 6;
				}
				nmModules.add(nmMod);
			} else if (mod.getType().equalsIgnoreCase("noise")) {
				nmMod = new NMNoise(mod.getName(), 1, y2, mod, nmp);
				y2 += 3;
				nmModules.add(nmMod);
			} else if (mod.getType().equalsIgnoreCase("filter")) {
				nmMod = new NMFilterE(mod.getName(), 2, y3, mod, nmp);
				y3 += 7;
				nmModules.add(nmMod);
			} else if (mod.getType().equalsIgnoreCase("cv_mixer")) {
				if (((ModuleOutputJack)mod.getOutputJacks().get(0)).getFirstConn().getTargetJack().getMod().getType().equalsIgnoreCase("vca")) {
					nmMod = new NMControlMixer(mod.getName(), 2, y3, mod, nmp);
					y3 += 3;
				} else {
					nmMod = new NMControlMixer(mod.getName(), 1, y2, mod, nmp);
					y2 += 3;
				}
				nmModules.add(nmMod);
			} else if (mod.getType().equalsIgnoreCase("mixer")) {
				// look for crossfade mixer first
				if (mod.getInputJacks().size() == 2 && mod.getParms().size() == 2 &&
						((ModuleParm)mod.getParms().get(1)).getMorph() != null /*&&
						((ModuleParm)mod.getParms().get(1)).getMorph().getSource().equals(mod.getName())*/) {
					nmMod = new NMXFade(mod.getName(), 2, y3, mod, nmp);
					y3 += 4;
				} else {
					// FIXME if more than 3 inputs, need 8InputMixer
					if (mod.getName().endsWith("UniMix")) {
						nmMod = new NM3InputsMixer(mod.getName(), 1, y2 - 1, mod, nmp);
						y2 += 2;
					} else {
						nmMod = new NM3InputsMixer(mod.getName(), 2, y3, mod, nmp);
						y3 += 3;
					}
				}
				nmModules.add(nmMod);
			} else if (mod.getType().equalsIgnoreCase("crossfade_mixer")) {
				nmMod = new NMXFade(mod.getName(), 2, y3, mod, nmp);
				y3 += 4;
				nmModules.add(nmMod);
			} else if (mod.getType().equalsIgnoreCase("env_adsr")) {
				nmMod = new NMADSREnvelope(mod.getName(), 3, y4, mod, nmp);
				y4 += 6;
				nmModules.add(nmMod);
				nmc = matchJacks("Voice Parms", "Gate Out", mod.getName(), "Gate In", 2);
				nmCables.add(nmc);
			} else if (mod.getType().equalsIgnoreCase("env_ar")) {
				nmMod = new NMADEnvelope(mod.getName(), 2, y3, mod, nmp);
				y3 += 4;
				nmModules.add(nmMod);
				nmc = matchJacks("Voice Parms", "Gate Out", mod.getName(), "Gate In", 2);
				nmCables.add(nmc);
			} else if (mod.getType().equalsIgnoreCase("lfo")) {
				s = mod.findParm("Waveform").getValue();
				if (s.equalsIgnoreCase("S&H")) {
					nmMod = new NMRndStepGen(mod.getName(), 0, y1, mod, nmp);
					y1 += 3;
				} else if (s.equalsIgnoreCase("S&G")) {
					nmMod = new NMRandomGen(mod.getName(), 0, y1, mod, nmp);
					y1 += 3;
				} else {
					nmMod = new NMLFOA(mod.getName(), 0, y1, mod, nmp);
					y1 += 6;
				}
				nmModules.add(nmMod);
			} else if (mod.getType().equalsIgnoreCase("vca")) {
				nmMod = new NMGainControl(mod.getName(), 2, y3, mod, nmp);
				y3 += 3;
				nmModules.add(nmMod);
			} else if (mod.getType().equalsIgnoreCase("portamento_full")) {
				nmMod = new NMPortamentoA(mod.getName(), 0, y1, mod, nmp);
				y1 += 3;
				nmModules.add(nmMod);
			} else if (mod.getType().equalsIgnoreCase("portamento_fingered")) {
				nmMod = new NMPortamentoB(mod.getName(), 0, y1, mod, nmp);
				y1 += 3;
				nmModules.add(nmMod);
			} else if (mod.getType().equalsIgnoreCase("stereo_chorus")) {
				nmMod = new NMStereoChorus(mod.getName(), 3, y4, mod, nmp);
				y4 += 4;
				nmModules.add(nmMod);
			} else if (mod.getType().equalsIgnoreCase("level_shift")) {
				if (mod.findInputJack("Audio In").getConn().getSourceJack().getMod().getType().equalsIgnoreCase("env_ar")) {
					nmMod = new NMInvLevShift(mod.getName(), 2, y3, mod, nmp);
					y3 += 3;
				} else {
					nmMod = new NMInvLevShift(mod.getName(), 1, y2, mod, nmp);
					y2 += 3;
				}
				nmModules.add(nmMod);
			} else if (mod.getType().equalsIgnoreCase("audio_out")) {
				nmMod = new NM2Outputs(mod.getName(), 3, y4, mod, nmp);
				y4 += 4;
				nmModules.add(nmMod);
			} else {
				System.out.println("can't handle module " + mod.getName() +
						" type " + mod.getType());
			}
		}

		// add connections
		for (i = 0; i < tempGP.getConnections().size(); i++) {
			conn = (Connection)tempGP.getConnections().get(i);
			src = conn.getSourceJack().getMod().getName();
			srcJack = conn.getSourceJack().getName();
			if (src.equalsIgnoreCase("Voice Parms") &&
					srcJack.equalsIgnoreCase("Voice Control Out")) {
				continue;
			}
			dest = conn.getTargetJack().getMod().getName();
			destJack = conn.getTargetJack().getName();
			if (conn.getSourceJack().getType().equalsIgnoreCase("control_output")) {
				nmc = matchJacks(src, srcJack, dest, destJack, 1);
			} else {
				nmc = matchJacks(src, srcJack, dest, destJack, 0);
			}
			if (nmc != null) {
				nmCables.add(nmc);
			} else {
				System.out.println("unable to create cable from " + src + "," +
						srcJack + " to " + dest + "," + destJack);
			}
		}

		if (xml.indexOf("Clavia") != -1 && xml.indexOf("Nord Lead 2 program") != -1 &&
				xml.indexOf("<source_patch>") != -1) {
			// Implement Nord Lead 2 morphing
			// FIXME this should be temporary, until morphing is
			// represented in generic patch
			addNordLead2Morph(xml, tempGP);
		}

		return true;
	}

	void addNordLead2Morph(String xml, GenericPatch gp) {
		int i, j;
		String s;

		if (XMLReader.getTagValue(xml, "Mod_Wheel_Dest").equalsIgnoreCase("Morph")) {
			nmControls.add(new NMControl(1, 0, 1, 2));
		} else {
			keyVelocityMorph = true;
		}
	}

	int getTagIntValue(String xml, String tag) {
		String s;
		s = XMLReader.getTagValue(xml, tag);
		if (s == null) {
			return 0;
		}
		return new Integer(s).intValue();
	}

	public String matchJacks(String src, String srcJack, String dest, String destJack, int color) {
		int sourceIdx = findNMModule(src);
		if (sourceIdx == -1) {
			return null;
		}
		NMModule modSource = (NMModule)nmModules.get(sourceIdx - 1);
		int sourceJackIdx = modSource.findJack(srcJack);
		if (sourceJackIdx == -1) {
			return null;
		}
		int targetIdx = findNMModule(dest);
		if (targetIdx == -1) {
			return null;
		}
		NMModule modTarget = (NMModule)nmModules.get(targetIdx - 1);
		int targetJackIdx = modTarget.findJack(destJack);
		if (targetJackIdx == -1) {
			return null;
		}
		return color + " " + targetIdx + " " + targetJackIdx + " 0 " +
				sourceIdx + " " + sourceJackIdx + " 1 ";
	}

	/**
	 * Reduce numbering for all module types in the generic patch.  First of a
	 * type should be 1, next is 2, etc.
	 * 
	 * @param gp GenericPatch to process
	 */
	public void reduceModNumbers(GenericPatch gp) throws PatchDefinitionException {
		int i, j, num;
		String s;
		Module mod, mod2;
		HashMap modTypesChecked = new HashMap();

		for (i = 0; i < gp.getModules().size(); i++) {
			mod = (Module)gp.getModules().get(i);
			// Have we processed this module type yet?
			if (modTypesChecked.containsKey(mod.getType())) {
				continue;
			}
			// No - add it to the list
			modTypesChecked.put(mod.getType(), null);
			// Find all modules of this type and reduce their numbers.
			// This works for the first module found also (although the
			// type compare is not necessary)
			for (j = i, num = 0; j < gp.getModules().size(); j++) {
				mod2 = (Module)gp.getModules().get(j);
				if (mod2.getType().equalsIgnoreCase(mod.getType())) {
					num++;
					if (mod2.getNumber() != num) {
						mod2.setNumber(num);
					}
					s = mod2.getName();
					if (Character.isDigit(s.charAt(s.length() - 1))) {
						mod2.setName(s.substring(0, s.length() - 1) + num);
					}
					reduceJackNumbers(mod2, gp);
				}
			}
		}
	}

	/**
	 * Reduce numbering for jacks (first of a type should be 1, next is 2, etc)
	 * and also reduce numbers of corresponding parms when they exist.
	 * 
	 * @param mod Module to process
	 */
	public void reduceJackNumbers(Module mod, GenericPatch gp) throws PatchDefinitionException {
		int i, j, num = 0, oldNum;
		String s, prefix, prefix2;
		Module mod2;
		ModuleJack mj, mj2;
		ModuleParm mp;
		HashMap jackTypesChecked = new HashMap();

		for (i = 0; i < mod.getInputJacks().size(); i++) {
			mj = (ModuleJack)mod.getInputJacks().get(i);
			s = mj.getName();
			if (Character.isDigit(s.charAt(s.length() - 1))) {
				prefix = s.substring(0, s.length() - 1);
				// Have we processed this jack type yet?
				if (jackTypesChecked.containsKey(prefix)) {
					continue;
				}
				// No - add it to the list
				jackTypesChecked.put(prefix, null);
				// Find all jacks of this type and reduce their numbers
				// This works for the first jack found also (although the
				// prefix compare is not necessary)
				for (j = i, num = 0; j < mod.getInputJacks().size(); j++) {
					mj2 = (ModuleJack)mod.getInputJacks().get(j);
					s = mj2.getName();
					if (Character.isDigit(s.charAt(s.length() - 1))) {
						prefix2 = s.substring(0, s.length() - 1);
						if (prefix2.equalsIgnoreCase(prefix)) {
							num++;
							oldNum = new Integer(s.substring(s.length() - 1, s.length())).intValue();
							mj2.setName(prefix + num);
							mp = mod.findParm(prefix.substring(0, prefix.length() - 2) + "Amt" + oldNum);
							if (mp != null) {
								mp.setName(prefix.substring(0, prefix.length() - 2) + "Amt" + num);
							}
						}
					}
				}
				if (mod.getType().equalsIgnoreCase("osc")) {
					if (prefix.equalsIgnoreCase("Expo FM In") && num > 2) {
						addMixer(2, num, gp, mod, "Expo FM");
					} else if (prefix.equalsIgnoreCase("Linear FM In") && num > 1) {
						addMixer(1, num, gp, mod, "Linear FM");
					} else if (prefix.equalsIgnoreCase("PWM In") && num > 1) {
						addMixer(1, num, gp, mod, "PWM");
					}
				} else if (mod.getType().equalsIgnoreCase("filter") &&
						prefix.equalsIgnoreCase("Expo FM In") && num > 2) {
					addMixer(2, num, gp, mod, "Expo FM");
				} else if (mod.getType().equalsIgnoreCase("vca") &&
						prefix.equalsIgnoreCase("Level In") && num > 1) {
					addMixer(1, num, gp, mod, "Level");
				}
			}
		}
	}

	public void addMixer(int allowedNum, int actualNum, GenericPatch gp,
			Module mod, String prefix) throws PatchDefinitionException {
		String cvMix;
		int j;
		Module mod2;
		ModuleOutputJack moj;
		ModuleInputJack mjold1, mjold2, mjcv1, mjcv2;
		ModuleParm mpold1, mpold2, mpcv1, mpcv2;
		Connection conn;
		double d1, d2, max;

		if (allowedNum + 1 == actualNum) {
			// create control mixer after module
			mixerNum++;
			mjold1 = mod.findInputJack(prefix + " In" + allowedNum);
			mjold2 = mod.findInputJack(prefix + " In" + (allowedNum + 1));
			mpold1 = mod.findParm(prefix + " Amt" + allowedNum);
			mpold2 = mod.findParm(prefix + " Amt" + (allowedNum + 1));
			cvMix = prefix + " Mixer" + mixerNum;
			mod2 = new Module(cvMix, "cv_mixer", 0);
			j = gp.findModuleIndex(mod.getName());
			gp.addModule(j + 1, mod2);
			mod2.addOutputJack(new ModuleOutputJack("CV Out", "control_output"));
			mjcv1 = new ModuleInputJack("CV In1", "control_input");
			mpcv1 = new ModuleParm("CV Amt1",
					new ParmValidatorRange(0, 100), "0");
			mod2.addParm(mpcv1);
			mod2.addInputJack(mjcv1, mpcv1);
			mjcv2 = new ModuleInputJack("CV In2", "control_input");
			mpcv2 = new ModuleParm("CV Amt2",
					new ParmValidatorRange(0, 100), "0");
			mod2.addParm(mpcv2);
			mod2.addInputJack(mjcv2, mpcv2);
			// move connections to control mixer's jacks
			conn = gp.findConnectionToTarget(mjold1);
			moj = conn.getSourceJack();
			gp.removeConnection(moj.getMod().getName(), moj.getName(),
					mod.getName(), prefix + " In" + allowedNum);
			gp.addConnection(new Connection(moj, mjcv1));
			conn = gp.findConnectionToTarget(mjold2);
			moj = conn.getSourceJack();
			gp.removeConnection(moj.getMod().getName(), moj.getName(),
					mod.getName(), prefix + " In" + (allowedNum + 1));
			gp.addConnection(new Connection(moj, mjcv2));
			// remove input which module can't accomodate
			mod.removeInputJack(mjold2);
			mod.removeParm(mpold2);
			// connect mixer to one original input
			gp.addConnectionIfNotFound(cvMix, "CV Out",
					mod.getName(), prefix + " In" + allowedNum);
			// Find higher value between parms & set mixer amts as percentage of that
			d1 = new Double(mpold1.getValue()).doubleValue();
			if (d1 < 0) {
				max = d1 * -1;
			} else {
				max = d1;
			}
			d2 = new Double(mpold2.getValue()).doubleValue();
			if (d2 < 0) {
				if (d1 < d2 * -1) {
					max = d2 * -1;
					mpold1.setValue(Double.toString(max));
				}
			} else {
				if (d1 < d2) {
					max = d2;
					mpold1.setValue(Double.toString(max));
				}
			}
			mpcv1.setValue(Double.toString(d1 / max * 100));
			mpcv2.setValue(Double.toString(d2 / max * 100));
		}
	}

	/**
	 * Try to convert modulation inputs (with attenuators) into morphs.  These
	 * count as 0% of the DSP usage, so they are preferred.
	 */
	public void convertModsToMorphs(GenericPatch tempGP) throws PatchDefinitionException {
		int i, j;
		Module mod;
		ModuleParm mp;
		ModuleInputJack mij;
		String source;

		// Copying modules into separate array, because some modules will be
		// removed during this process so regular iteration can be thrown off
		Module modList[] = (Module[])tempGP.getModules().toArray(new Module[tempGP.getModules().size()]);
		for (i = 0; i < modList.length; i++) {
			mod = modList[i];
			if (tempGP.getModules().contains(mod) == false) {
				continue; // Current module was removed, so don't process it
			}
			// FIXME kludge to deal with current practive of having morph for
			// Pulse Width named PWM Amt (while other morphs are named after
			// the original parm but with a suffix of Mod Amt)
			if (mod.getType().equalsIgnoreCase("osc")) {
				mp = mod.findParm("Pulse Width");
				if (mp != null) {
					for (j = 0; j < mod.getInputJacks().size(); j++) {
						mij = (ModuleInputJack)mod.getInputJacks().get(j);
						if (mij.getPrefix().equalsIgnoreCase("PWM In")) {
							source = mij.getConn().getSourceJack().getName();
							// FIXME there are other midi controllers that could be converted
							if (source.equalsIgnoreCase("Velocity Out") ||
									source.equalsIgnoreCase("Mod Wheel Out")) {
								mij.setName("Pulse Width Mod In");
								mij.getAttenuator().setName("Pulse Width Mod Amt");
								break;
							}
						}
					}
				}
			}
			// FIXME kludge to deal with current practive of having morph for
			// Frequency named Expo FM Amt (while other morphs are named after
			// the original parm but with a suffix of Mod Amt)
			if (mod.getType().equalsIgnoreCase("filter")) {
				mp = mod.findParm("Frequency");
				for (j = 0; j < mod.getInputJacks().size(); j++) {
					mij = (ModuleInputJack)mod.getInputJacks().get(j);
					if (mij.getPrefix().equalsIgnoreCase("Expo FM In")) {
						source = mij.getConn().getSourceJack().getName();
						// FIXME there are other midi controllers that could be converted
						// FIXME avoiding NL2 mod wheel -> filter routing; must be better way
						if (source.equalsIgnoreCase("Velocity Out") ||
								(source.equalsIgnoreCase("Mod Wheel Out") &&
								mij.getAttenuator().getValue().equals("127") == false)) {
							mij.setName("Frequency Mod In");
							mij.getAttenuator().setName("Frequency Mod Amt");
							break;
						}
					}
				}
			}
			convertModType1ToMorph(tempGP, mod);
			convertModType2ToMorph(tempGP, mod);
		}
	}

	/**
	 * Look for jacks which essentially modulate another parm via MIDI control,
	 * and convert these into MIDI parm morphs
	 */
	public void convertModType1ToMorph(GenericPatch tempGP, Module mod) {
		ModuleParm mp, mp2;
		ModuleInputJack mij;
		Connection conn;
		ParmMorph morph;
		String source;
		int i;
		double d, d2;
		DecimalFormat df = new DecimalFormat("#.####");

		for (i = 0; i < mod.getParms().size(); i++) {
			mp = (ModuleParm)mod.getParms().get(i);
			mij = mod.findInputJack(mp.getName() + " Mod In");
			try {
				conn = mij.getConn();
				source = conn.getSourceJack().getName();
				// FIXME there are other midi controllers that could be converted
				if (source.equalsIgnoreCase("Velocity Out") ||
						source.equalsIgnoreCase("Mod Wheel Out")) { 
					morph = new ParmMorph("MIDI", source.substring(0, source.length() - 4), null, mp);
					mp.setMorph(morph);
					d = new Double(mp.getValue()).doubleValue();
					mp2 = mij.getAttenuator();
					d2 = new Double(mp2.getValue()).doubleValue();
					morph.setValue(df.format(d + d2));
					tempGP.removeConnection(conn);
					mod.removeParm(mp2);
					mod.removeInputJack(mij);
				}
			} catch (NullPointerException e) {
				// This means morph wasn't found; this is not an error
			}
		}
	}

	/**
	 * Look for jacks whose source is a modVCA controlled via MIDI control,
	 * and convert these into MIDI parm morphs
	 */
	public void convertModType2ToMorph(GenericPatch tempGP, Module mod)
			throws PatchDefinitionException {
		Module mod2;
		ModuleParm mp, mp2;
		ModuleInputJack mij, mij2;
		ModuleOutputJack moj;
		Connection conn, conn2;
		ParmMorph morph;
		String source;
		int i, j;
		double d, d2;
		DecimalFormat df = new DecimalFormat("#.####");

		outer: for (i = 0; i < mod.getInputJacks().size(); i++) {
			// Find jack's source; then look for another jack with the same
			// prefix and which receives from the same source but passing through
			// a modVCA controlled by MIDI.  This can be converted into a morph.
			mij = (ModuleInputJack)mod.getInputJacks().get(i);
			moj = mij.getConn().getSourceJack();
			for (j = i + 1; j < mod.getInputJacks().size(); j++) {
				mij2 = (ModuleInputJack)mod.getInputJacks().get(j);
				if (mij2.getPrefix().equalsIgnoreCase(mij.getPrefix()) == false) {
					continue;
				}
				conn = mij2.getConn();
				mod2 = conn.getSourceJack().getMod();
				if (mod2.getType().equalsIgnoreCase("mod_vca") == false) {
					continue;
				}
				source = mod2.findInputJack("Level In1").getConn().getSourceJack().getName();
				if (source.equalsIgnoreCase("Velocity Out") == false &&
						source.equalsIgnoreCase("Mod Wheel Out") == false) {
					continue;
				}
				conn2 = mod2.findInputJack("VCA In").getConn();
				if (conn2.getSourceJack() != moj) {
					continue;
				}
				mp = mij.getAttenuator();
				morph = new ParmMorph("MIDI", source.substring(0, source.length() - 4), null, mp);
				mp.setMorph(morph);
				d = new Double(mp.getValue()).doubleValue();
				mp2 = mij2.getAttenuator();
				d2 = new Double(mp2.getValue()).doubleValue();
				morph.setValue(df.format(d + d2));
				tempGP.removeConnection(conn);
				mod.removeParm(mp2);
				mod.removeInputJack(mij2);
				tempGP.removeConnection(conn2);
				tempGP.removeConnection(mod2.findInputJack("Level In1").getConn());
				tempGP.removeModule(mod2);
				continue outer;
			}
		}

		for (i = 0; i < mod.getInputJacks().size(); i++) {
			// Look for jack which wasn't matched by the process above, but
			// which receives from a modVCA controlled by MIDI.  This can be
			// converted into a morph (with base parm amount of zero).
			mij = (ModuleInputJack)mod.getInputJacks().get(i);
			conn = mij.getConn();
			mod2 = conn.getSourceJack().getMod();
			if (mod2.getType().equalsIgnoreCase("mod_vca") == false) {
				continue;
			}
			source = mod2.findInputJack("Level In1").getConn().getSourceJack().getName();
			if (source.equalsIgnoreCase("Velocity Out") == false &&
					source.equalsIgnoreCase("Mod Wheel Out") == false) {
				continue;
			}
			conn2 = mod2.findInputJack("VCA In").getConn();
			moj = conn2.getSourceJack();
			mp = mij.getAttenuator();
			morph = new ParmMorph("MIDI", source.substring(0, source.length() - 4), null, mp);
			mp.setMorph(morph);
			morph.setValue(mp.getValue());
			mp.setValue("0");
			tempGP.removeConnection(conn);
			tempGP.removeConnection(conn2);
			tempGP.removeConnection(mod2.findInputJack("Level In1").getConn());
			tempGP.removeModule(mod2);
			tempGP.addConnection(new Connection(moj, mij));
		}
	}

//	public void convertModType1ToMorph(GenericPatch tempGP, String parm, String parmMod,
//			Module mod) {
//		ModuleParm mp, mp2;
//		ModuleInputJack mij, mij2;
//		ModuleOutputJack moj;
//		Connection conn;
//		ParmMorph morph;
//		String source;
//		double d, d2;
//		DecimalFormat df = new DecimalFormat("#.####");
//
//		mp = mod.findParm(parm);
//		mij2 = mod.findInputJack(parmMod);
//		try {
//			conn = mij2.getConn();
//			moj = conn.getSourceJack();
//			source = moj.getName();
//			// FIXME there are other midi controllers that could be converted
//			if (source.equalsIgnoreCase("Velocity Out") ||
//					source.equalsIgnoreCase("Mod Wheel Out")) { 
//				morph = new ParmMorph("MIDI", source.substring(0, source.length() - 4), null, mp);
//				mp.setMorph(morph);
//				d = new Double(mp.getValue()).doubleValue();
//				mp2 = mij2.getAttenuator();
//				d2 = new Double(mp2.getValue()).doubleValue();
//				morph.setValue(df.format(d + d2));
//				tempGP.removeConnection(conn);
//				mod.removeParm(mp2);
//				mod.removeInputJack(mij2);
//			}
//		} catch (NullPointerException e) {
//			// This means mod wasn't found; this is not an error
//		}
//	}
}

}

class NMControl {
	private int modIndex;
	private int parmIndex;
	private int ccNumber;
	private int section;

	NMControl(int pModIndex, int pParmIndex, int pCcNumber) {
		this(pModIndex, pParmIndex, pCcNumber, 1);
	}

	NMControl(int pModIndex, int pParmIndex, int pCcNumber, int pSection) {
		modIndex = pModIndex;
		parmIndex = pParmIndex;
		ccNumber = pCcNumber;
		section = pSection;
	}

	public int getModIndex() {
		return modIndex;
	}

	public int getParmIndex() {
		return parmIndex;
	}

	public int getCcNumber() {
		return ccNumber;
	}

	public int getSection() {
		return section;
	}
}

interface NMModule {
	public String getName();
	public String writeModuleDump();
	public String writeParameterDump() throws PatchDefinitionException;
	public String writeCustomDump() throws PatchDefinitionException;
	public String writeNameDump();
	public int findJack(String jack);
}

class NMModuleAbstract implements NMModule {
	private String name;
	protected int type;
	protected Module mod;
	protected NordModularPatch nmp;
	private int x;
	private int y;

	NMModuleAbstract(String pName, int pType, int pX, int pY, Module pMod,
			NordModularPatch pNmp) {
		name = pName;
		type = pType;
		mod = pMod;
		nmp = pNmp;
		x = pX;
		y = pY;
	}

	public String getName() {
		return name;
	}

	public String writeModuleDump() {
		return type + " " + x + " " + y + " ";
	}

	public String writeParameterDump() throws PatchDefinitionException {
		throw new PatchDefinitionException("This function of abstract class should never be used");
	}

	public String writeCustomDump() throws PatchDefinitionException {
		throw new PatchDefinitionException("This function of abstract class should never be used");
	}

	public String writeNameDump() {
		if (name.length() <= 16) {
			return name;
		} else {
			return name.substring(0, 16);
		}
	}

	public int findJack(String jack) {
		System.out.println("NMModuleAbstract findJack oops");
		return -1;
	}
}

class NMKeyboardVoice extends NMModuleAbstract {

	NMKeyboardVoice(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 1, pX, pY, pMod, pNmp);
	}

	public String writeParameterDump() {
		return null;
	}

	public String writeCustomDump() {
		return null;
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("Note Out")) {
			return 0;
		} else if (jack.equalsIgnoreCase("Gate Out")) {
			return 1;
		} else if (jack.equalsIgnoreCase("Velocity Out")) {
			return 2;
		} else {
			return -1;
		}
	}
}

class NMKeyboardPatch extends NMModuleAbstract {

	NMKeyboardPatch(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 63, pX, pY, pMod, pNmp);
	}

	public String writeParameterDump() {
		return null;
	}

	public String writeCustomDump() {
		return null;
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("Gate Out")) {
			return 1;
		} else {
			return -1;
		}
	}
}

class NMOscA extends NMModuleAbstract {
	private int freqCoarse;
	private int freqFine;
	private int keyTrack;
	private int pulseWidth = 64;
	private int waveform;
	private int pitchMod1;
	private int pitchMod2;
	private int fmaMod = 0;
	private int pwMod;

	static final String LFO_AMT_OSC[] = {
		//Sampling of values:
		// 0=0, 19=0.125, 26=0.25, 36=0.5, 43=1, 52=2, 63=4, 74=8
		// 82=12, 87=15, 94=21, 100=26.5, 108=35.5, 117=48, 125=60, 127=60.01
		"0", "0.0066", "0.0132", "0.0197", "0.0263", "0.0329", "0.0395", "0.0461",
		"0.0526", "0.0592", "0.0658", "0.0724", "0.0789", "0.0855", "0.0921", "0.0987",
		"0.1053", "0.1118", "0.1184", "0.125", "0.1429", "0.1607", "0.1786", "0.1964",
		"0.2143", "0.2321", "0.25", "0.275", "0.3", "0.325", "0.35", "0.375",
		"0.4", "0.425", "0.45", "0.475", "0.5", "0.5714", "0.6429", "0.7143",
		"0.7857", "0.8571", "0.9286", "1", "1.1111", "1.2222", "1.3333", "1.4444",
		"1.5556", "1.6667", "1.7778", "1.8889", "2", "2.1818", "2.3636", "2.5455",
		"2.7273", "2.9091", "3.0909", "3.2727", "3.4545", "3.6364", "3.8182", "4",

		"4.3636", "4.7273", "5.0909", "5.4545", "5.8182", "6.1818", "6.5455", "6.9091",
		"7.2727", "7.6364", "8", "8.5", "9", "9.5", "10", "10.5",
		"11", "11.5", "12", "12.6", "13.2", "13.8", "14.4", "15",
		"15.8571", "16.7143", "17.5714", "18.4286", "19.2857", "20.1429", "21", "21.9167",
		"22.8333", "23.75", "24.6667", "25.5833", "26.5", "27.625", "28.75", "29.875",
		"31", "32.125", "33.25", "34.375", "35.5", "36.8889", "38.2778", "39.6667",
		"41.0556", "42.4444", "43.8333", "45.2222", "46.6111", "48", "49.5", "51",
		"52.5", "54", "55.5", "57", "58.5", "60", "60.005", "60.01",
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

	NMOscA(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 7, pX, pY, pMod, pNmp);
		String s;
		int i, j;
		ModuleParm mp, mp2;

		mp = mod.findParm("Coarse Tune");
		s = mp.getValue();
		freqCoarse = new Integer(s).intValue() + 64;
		if (freqCoarse < 0) {
			System.out.println(mod.getName() + " pitch " + s + " out of range -64 to 63");
			freqCoarse = 0;
		}
		if (freqCoarse > 127) {
			System.out.println(mod.getName() + " pitch " + s + " out of range -64 to 63");
			freqCoarse = 127;
		}

		mp2 = mod.findParm("Fine Tune");
		s = mp2.getValue();
		// range is +/-50 cents; this will convert properly
		freqFine = Util.pctToParm(s, -128, 126) + 64;
		if (freqFine < 0) {
			System.out.println(mod.getName() + " fine " + s + " out of range -50 to 50 cents");
			freqFine = 0;
		}
		if (freqFine > 127) {
			System.out.println(mod.getName() + " fine " + s + " out of range -50 to 50 cents");
			freqFine = 127;
		}

		if (mp.getMorph() != null || mp2.getMorph() != null) {
			j = mod.getGp().findModuleIndex(mod.getName()) + 1;
			if (mp.getMorph() != null) {
				i = new Integer(mp.getMorph().getValue()).intValue() - freqCoarse + 64;
			} else {
				i = 0;
			}
			s = "1 " + j + " 0 0 " + i + " ";
//			System.out.println("Pitch morph=" + s);
			nmp.addMorph(s);
			if (mp2.getMorph() != null) {
				i = Util.pctToParm(mp2.getMorph().getValue(), -128, 126) - freqFine + 64;
			} else {
				i = 0;
			}
			s = "1 " + j + " 1 0 " + i + " ";
//			System.out.println("Fine morph=" + s);
			nmp.addMorph(s);
		}

		s = mod.findParm("Key Track").getValue();
		keyTrack = Util.pctToParm(s, 0, 64);
		if (keyTrack < 0) {
			System.out.println(mod.getName() + " key_track " + s + " out of range 0 to 100 percent");
			keyTrack = 0;
		}
		if (keyTrack > 64) {
			System.out.println(mod.getName() + " key_track " + s + " out of range 0 to 100 percent");
			keyTrack = 64;
		}

		// Pulse width maxes out at 1% and 99% - cannot be driven to cutoff
		mp = mod.findParm("Pulse Width");
		if (mp != null) {
			s = mp.getValue();
			pulseWidth = Util.pctToParm(s, 0, 127);
			if (pulseWidth < 0) {
				System.out.println(mod.getName() + " pulse width " + s + " out of range 1 to 99 percent");
				pulseWidth = 0;
			}
			if (pulseWidth > 127) {
				System.out.println(mod.getName() + " pulse width " + s + " out of range 1 to 99 percent");
				pulseWidth = 127;
			}
			if (mp.getMorph() != null) {
				i = Util.pctToParm(mp.getMorph().getValue(), 0, 127) - pulseWidth;
				j = mod.getGp().findModuleIndex(mod.getName()) + 1;
				s = "1 " + j + " 3 0 " + i + " ";
//				System.out.println("PW morph=" + s);
				nmp.addMorph(s);
			}
		}

		// PWM range is +/-100% (amplitude is 200%)
		mp = mod.findParm("PWM Amt1");
		if (mp != null) {
			s = mp.getValue();
			pwMod = Util.pctToParm(s, 0, 64);
			if (pwMod < 0) {
				System.out.println(mod.getName() + " pwm amt1 " + s + " out of range 0 to 100 percent");
				pwMod = 0;
			}
			if (pwMod > 127) {
				System.out.println(mod.getName() + " pwm amt1 " + s + " out of range 0 to 100 percent");
				pwMod = 127;
			}
			if (mp.getMorph() != null) {
				i = Util.pctToParm(mp.getMorph().getValue(), 0, 64) -
						pwMod;
				j = mod.getGp().findModuleIndex(mod.getName()) + 1;
				s = "1 " + j + " 8 0 " + i + " ";
//				System.out.println("PWM morph=" + s);
				nmp.addMorph(s);
			}
		}

		s = mod.findParm("Waveform").getValue();
		if (s.equalsIgnoreCase("Sine")) {
			waveform = 0;
		} else if (s.equalsIgnoreCase("Tri")) {
			waveform = 1;
		} else if (s.equalsIgnoreCase("Saw")) {
			waveform = 2;
		} else if (s.equalsIgnoreCase("Pulse")) {
			waveform = 3;
		} else {
			System.out.println(mod.getName() + " waveform " + s + " not supported");
			waveform = 2;
		}

		mp = mod.findParm("Expo FM Amt1");
		if (mp != null) {
			s = mp.getValue();
			pitchMod1 = Util.matchToNumberTable(s, LFO_AMT_OSC);
			if (mp.getMorph() != null) {
				i = Util.matchToNumberTable(mp.getMorph().getValue(), LFO_AMT_OSC) -
						pitchMod1;
				j = mod.getGp().findModuleIndex(mod.getName()) + 1;
				s = "1 " + j + " 5 0 " + i + " ";
//				System.out.println("Expo FM1 morph=" + s);
				nmp.addMorph(s);
			}
		} else {
			pitchMod1 = 0;
		}

		mp = mod.findParm("Expo FM Amt2");
		if (mp != null) {
			s = mp.getValue();
			pitchMod2 = Util.matchToNumberTable(s, LFO_AMT_OSC);
			if (mp.getMorph() != null) {
				i = Util.matchToNumberTable(mp.getMorph().getValue(), LFO_AMT_OSC) -
						pitchMod2;
				j = mod.getGp().findModuleIndex(mod.getName()) + 1;
				s = "1 " + j + " 6 0 " + i + " ";
//				System.out.println("Expo FM2 morph=" + s);
				nmp.addMorph(s);
			}
		} else {
			pitchMod2 = 0;
		}

		mp = mod.findParm("Linear FM Amt1");
		if (mp != null) {
			s = mp.getValue();
//			fmaMod = new Integer(s).intValue();
//			if (fmaMod < 0 || fmaMod > 127) {
//				System.out.println(mod.getName() + " linear fm amt1 " + s + " out of range 0 to 127");
//				fmaMod = 0;
//			}
			fmaMod = Util.matchToNumberTable(s, LINEAR_FM_AMT);
			if (mp.getMorph() != null) {
				i = Util.matchToNumberTable(mp.getMorph().getValue(), LINEAR_FM_AMT) -
						fmaMod;
				j = mod.getGp().findModuleIndex(mod.getName()) + 1;
				s = "1 " + j + " 7 0 " + i + " ";
//				System.out.println("Osc FM morph=" + s);
				nmp.addMorph(s);
			}
		} else {
			fmaMod = 0;
		}
	}

	public String writeParameterDump() {
		return type + " 10 " + freqCoarse + " " + freqFine + " " + keyTrack +
				" " + pulseWidth + " " + waveform + " " + pitchMod1 + " " + pitchMod2 +
				" " + fmaMod + " " + pwMod + " 0 ";
	}

	public String writeCustomDump() {
		return "1 1 ";
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("Sync In")) {
			return 0;
		} else if (jack.equalsIgnoreCase("Linear FM In1")) {
			return 1;
		} else if (jack.equalsIgnoreCase("Expo FM In1")) {
			return 2;
		} else if (jack.equalsIgnoreCase("Expo FM In2")) {
			return 3;
		} else if (jack.equalsIgnoreCase("PWM In1")) {
			return 4;
		} else if (jack.equalsIgnoreCase("Wave Out")) {
			return 0;
		} else {
			return -1;
		}
	}
}

class NMNoise extends NMModuleAbstract {

	NMNoise(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 31, pX, pY, pMod, pNmp);
	}

	public String writeParameterDump() {
		return type + " 1 0 ";
	}

	public String writeCustomDump() {
		return null;
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("Wave Out")) {
			return 0;
		} else {
			return -1;
		}
	}
}

class NMFilterE extends NMModuleAbstract {
	private int filterType;
	private int filterSlope;
	private int frequency;
	private int kbt;
	private int resonance;
	private int frequencyModAmt1 = 0;
	private int frequencyModAmt2 = 0;

	static final String FILTERE_RESONANCE[] = {
		//Sampling of values:
		// 0=0, 86=25, 112=50, 121=75, 127=100
		"0", "0.2907", "0.5814", "0.8721", "1.1628", "1.4535", "1.7442", "2.0349",
		"2.3256", "2.6163", "2.907", "3.1977", "3.4884", "3.7791", "4.0698", "4.3605",
		"4.6512", "4.9419", "5.2326", "5.5233", "5.814", "6.1047", "6.3953", "6.686",
		"6.9767", "7.2674", "7.5581", "7.8488", "8.1395", "8.4302", "8.7209", "9.0116",
		"9.3023", "9.593", "9.8837", "10.1744", "10.4651", "10.7558", "11.0465", "11.3372",
		"11.6279", "11.9186", "12.2093", "12.5", "12.7907", "13.0814", "13.3721", "13.6628",
		"13.9535", "14.2442", "14.5349", "14.8256", "15.1163", "15.407", "15.6977", "15.9884",
		"16.2791", "16.5698", "16.8605", "17.1512", "17.4419", "17.7326", "18.0233", "18.314",
		"18.6047", "18.8953", "19.186", "19.4767", "19.7674", "20.0581", "20.3488", "20.6395",
		"20.9302", "21.2209", "21.5116", "21.8023", "22.093", "22.3837", "22.6744", "22.9651",
		"23.2558", "23.5465", "23.8372", "24.1279", "24.4186", "24.7093", "25", "25.9615",
		"26.9231", "27.8846", "28.8462", "29.8077", "30.7692", "31.7308", "32.6923", "33.6538",
		"34.6154", "35.5769", "36.5385", "37.5", "38.4615", "39.4231", "40.3846", "41.3462",
		"42.3077", "43.2692", "44.2308", "45.1923", "46.1538", "47.1154", "48.0769", "49.0385",
		"50", "52.7778", "55.5556", "58.3333", "61.1111", "63.8889", "66.6667", "69.4444",
		"72.2222", "75", "79.1667", "83.3333", "87.5", "91.6667", "95.8333", "100",
	};
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

	NMFilterE(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 51, pX, pY, pMod, pNmp);
		String s;
		ModuleParm mp, mp2;
		double keyTrack, d;
		int i, j;

		s = mod.findParm("Filter Type").getValue();
		if (s.equalsIgnoreCase("LP")) {
			filterType = 0;
		} else if (s.equalsIgnoreCase("BP")) {
			filterType = 1;
		} else if (s.equalsIgnoreCase("HP")) {
			filterType = 2;
		} else if (s.equalsIgnoreCase("Notch")) {
			filterType = 3;
		} else {
			System.out.println(mod.getName() + " filter_type " + s + " not supported");
			filterType = 0;
		}

		s = mod.findParm("Filter Slope").getValue();
		if (s.equalsIgnoreCase("12db")) {
			filterSlope = 0;
		} else {
			filterSlope = 1; // 24db
		}

		// key_track range is +/-100% (amplitude is 200%)
		s = mod.findParm("Key Track").getValue();
		kbt = Util.pctToParm(s, 0, 64);
		if (kbt < 0) {
			System.out.println(mod.getName() + " key track " + s + " out of range 0 to 100 percent");
			kbt = 0;
		}
		if (kbt > 127) {
			System.out.println(mod.getName() + " key track " + s + " out of range 0 to 100 percent");
			kbt = 127;
		}
		keyTrack = new Double(s).doubleValue();

		mp = mod.findParm("Frequency");
		s = mp.getValue();
		frequency = new Integer(s).intValue() + 60; // FIXME if base key = 64, why is this 60?
		if (frequency < 0) {
			System.out.println(mod.getName() + " frequency " + s + " out of range -60 to 67");
			frequency = 0;
		}
		if (frequency > 127) {
			System.out.println(mod.getName() + " frequency " + s + " out of range -60 to 67");
			frequency = 127;
		}
		// adjust for differing base key (NM is E4, midi note 64)
		if (keyTrack < 100) {
			s = mod.findParm("Base Key").getValue();
			frequency -= (64 - new Integer(s).intValue()) * (100 - keyTrack) / 100;
			if (frequency < 0) {
				frequency = 0;
			}
			if (frequency > 127) {
				frequency = 127;
			}
		}
		if (mp.getMorph() != null) {
			i = new Integer(mp.getMorph().getValue()).intValue() -
					new Integer(mp.getValue()).intValue();
			j = mod.getGp().findModuleIndex(mod.getName()) + 1;
			s = "1 " + j + " 3 0 " + i + " ";
//			System.out.println("Freq morph=" + s);
			nmp.addMorph(s);
		}

		mp = mod.findParm("Resonance");
		s = mp.getValue();
		mp2 = mod.findParm("Self-Oscillate Percent");
		if (mp2.getValue().equals("101")) { // input patch filter doesn't self-oscillate
			if (new Double(s).doubleValue() > 85.4165) {
				s = "83.3333"; // reduce value so NM filter doesn't either
			}
			if (mp.getMorph() != null &&
					new Double(mp.getMorph().getValue()).doubleValue() > 85.4165) {
				mp.getMorph().setValue("83.3333"); // do the same for morph
			}
		}
		resonance = Util.matchToNumberTable(s, FILTERE_RESONANCE);
		if (mp.getMorph() != null) {
			i = Util.matchToNumberTable(mp.getMorph().getValue(), FILTERE_RESONANCE) -
					resonance;
			j = mod.getGp().findModuleIndex(mod.getName()) + 1;
			s = "1 " + j + " 6 0 " + i + " ";
//			System.out.println("Res morph=" + s);
			nmp.addMorph(s);
		}

		// mod amount: each integer value adds one half-step
		mp = mod.findParm("Expo FM Amt1");
		if (mp != null) {
//			s = mp.getValue();
//			frequencyModAmt1 = new Integer(s).intValue();
			d = new Double(mp.getValue()).doubleValue();
			frequencyModAmt1 = new Double(d + .5).intValue();
			if (frequencyModAmt1 < 0) {
				System.out.println(mod.getName() + " expo fm amt1 " + s + " out of range 0-127");
				frequencyModAmt1 = 0;
			}
			if (frequencyModAmt1 > 127) {
				System.out.println(mod.getName() + " expo fm amt1 " + s + " out of range 0-127");
				frequencyModAmt1 = 127;
			}
			if (mp.getMorph() != null) {
				i = new Double(new Double(mp.getMorph().getValue()).doubleValue() -
						frequencyModAmt1 + .5).intValue();
				j = mod.getGp().findModuleIndex(mod.getName()) + 1;
				s = "1 " + j + " 2 0 " + i + " ";
//				System.out.println("Filt Env Amt morph=" + s);
				nmp.addMorph(s);
			}
		}

		mp = mod.findParm("Expo FM Amt2");
		if (mp != null) {
//			s = mp.getValue();
//			frequencyModAmt2 = Util.matchToNumberTable(s, LFO_AMT_OSC);
			d = new Double(mp.getValue()).doubleValue();
			frequencyModAmt2 = new Double(d + .5).intValue();
			if (frequencyModAmt2 < 0) {
				System.out.println(mod.getName() + " expo fm amt2 " + s + " out of range 0-127");
				frequencyModAmt2 = 0;
			}
			if (frequencyModAmt2 > 127) {
				System.out.println(mod.getName() + " expo fm amt2 " + s + " out of range 0-127");
				frequencyModAmt2 = 127;
			}
			if (mp.getMorph() != null) {
				i = new Double(new Double(mp.getMorph().getValue()).doubleValue() -
						frequencyModAmt2 + .5).intValue();
				j = mod.getGp().findModuleIndex(mod.getName()) + 1;
				s = "1 " + j + " 8 0 " + i + " ";
//				System.out.println("Filt Env Amt2 morph=" + s);
				nmp.addMorph(s);
			}
		}
	}

	public String writeParameterDump() {
		return type + " 10 " + filterType + " 1 " + frequencyModAmt1 + " " + frequency +
				" " + kbt + " 0 " + resonance + " " + filterSlope + " " + frequencyModAmt2 + " 0 ";
	}

	public String writeCustomDump() {
		return "1 1 ";
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("Filter In")) {
			return 2;
		} else if (jack.equalsIgnoreCase("Expo FM In1")) {
			return 0;
		} else if (jack.equalsIgnoreCase("Expo FM In2")) {
			return 3;
		} else if (jack.equalsIgnoreCase("Filter Out")) {
			return 0;
		} else {
			return -1;
		}
	}
}

class NM2Outputs extends NMModuleAbstract {
	private int level;

	NM2Outputs(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 4, pX, pY, pMod, pNmp);
		String s;
		int i, j;
		ModuleParm mp;

		mp = mod.findParm("Level Amt1");
		s = mp.getValue();
		level = Util.matchToNumberTable(s, NM3InputsMixer.MIXER_AMT); // FIXME right table?
//		level = Util.pctToParm(s, 0, 127);
//		if (level < 0) {
//			System.out.println(mod.getName() + " Patch level " + s + " out of range 0-100");
//			level = 0;
//		}
//		if (level > 127) {
//			System.out.println(mod.getName() + " Patch level " + s + " out of range 0-100");
//			level = 127;
//		}
		if (mp.getMorph() != null) {
//			i = Util.pctToParm(mp.getMorph().getValue(), 0, 127) - level;
			i = Util.matchToNumberTable(mp.getMorph().getValue(), NM3InputsMixer.MIXER_AMT) - level; // FIXME right table?
			j = mod.getGp().findModuleIndex(mod.getName()) + 1;
			s = "1 " + j + " 0 0 " + i + " ";
//			System.out.println("Patch level morph=" + s);
			nmp.addMorph(s);
		}
	}

	public String writeParameterDump() {
		return type + " 3 " + level + " 0 0 ";
	}

	public String writeCustomDump() {
		return null;
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("Level In1")) {
			return 0;
		} else if (jack.equalsIgnoreCase("Level In2")) {
			return 1;
		} else {
			return -1;
		}
	}
}

class NMXFade extends NMModuleAbstract {
	private int mix;

	NMXFade(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 18, pX, pY, pMod, pNmp);
		String s;
		int i, j;
		ModuleParm mp;

		mp = mod.findParm("Mix");
		if (mp == null) {
			mp = mod.findParm("Audio Amt2");
		}
		s = mp.getValue();
		mix = Util.pctToParm(s, 0, 127);
		if (mix < 0) {
			System.out.println(mod.getName() + " Mix " + s + " out of range 0-100");
			mix = 0;
		}
		if (mix > 127) {
			System.out.println(mod.getName() + " Mix " + s + " out of range 0-100");
			mix = 127;
		}
		if (mp.getMorph() != null) {
			i = Util.pctToParm(mp.getMorph().getValue(), 0, 127) - mix;
			j = mod.getGp().findModuleIndex(mod.getName()) + 1;
			s = "1 " + j + " 1 0 " + i + " ";
//			System.out.println("Mix morph=" + s);
			nmp.addMorph(s);
		}
	}

	public String writeParameterDump() {
		return type + " 2 0 " + mix + " ";
	}

	public String writeCustomDump() {
		return null;
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("Audio In1")) {
			return 0;
		} else if (jack.equalsIgnoreCase("Audio In2")) {
			return 1;
		} else if (jack.equalsIgnoreCase("Mixer Out")) {
			return 0;
		} else {
			return -1;
		}
	}
}

class NM3InputsMixer extends NMModuleAbstract {
	private int amt1 = 0;
	private int amt2 = 0;
	private int amt3 = 0;

	static final String MIXER_AMT[] = {
		//Sampling of values:
		// 0=0, 80=25, 108=50, 120=75, 127=100
		"0", "0.3125", "0.625", "0.9375", "1.25", "1.5625", "1.875", "2.1875",
		"2.5", "2.8125", "3.125", "3.4375", "3.75", "4.0625", "4.375", "4.6875",
		"5", "5.3125", "5.625", "5.9375", "6.25", "6.5625", "6.875", "7.1875",
		"7.5", "7.8125", "8.125", "8.4375", "8.75", "9.0625", "9.375", "9.6875",
		"10", "10.3125", "10.625", "10.9375", "11.25", "11.5625", "11.875", "12.1875",
		"12.5", "12.8125", "13.125", "13.4375", "13.75", "14.0625", "14.375", "14.6875",
		"15", "15.3125", "15.625", "15.9375", "16.25", "16.5625", "16.875", "17.1875",
		"17.5", "17.8125", "18.125", "18.4375", "18.75", "19.0625", "19.375", "19.6875",

		"20", "20.3125", "20.625", "20.9375", "21.25", "21.5625", "21.875", "22.1875",
		"22.5", "22.8125", "23.125", "23.4375", "23.75", "24.0625", "24.375", "24.6875",
		"25", "25.8929", "26.7857", "27.6786", "28.5714", "29.4643", "30.3571", "31.25",
		"32.1429", "33.0357", "33.9286", "34.8214", "35.7143", "36.6071", "37.5", "38.3929",
		"39.2857", "40.1786", "41.0714", "41.9643", "42.8571", "43.75", "44.6429", "45.5357",
		"46.4286", "47.3214", "48.2143", "49.1071", "50", "52.0833", "54.1667", "56.25",
		"58.3333", "60.4167", "62.5", "64.5833", "66.6667", "68.75", "70.8333", "72.9167",
		"75", "78.5714", "82.1429", "85.7143", "89.2857", "92.8571", "96.4286", "100",
	};

	NM3InputsMixer(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 19, pX, pY, pMod, pNmp);
		String s;
		ModuleParm mp;

		s = mod.findParm("Audio Amt1").getValue();
		amt1 = Util.matchToNumberTable(s, MIXER_AMT);
//		amt1 = Util.pctToParm(s, 0, 127);
//		if (amt1 < 0 || amt1 > 127) {
//			System.out.println(mod.getName() + " Audio Amt1 " + s + " out of range 0-100");
//			amt1 = 0;
//		}

		mp = mod.findParm("Audio Amt2");
		if (mp != null) {
			s = mp.getValue();
			amt2 = Util.matchToNumberTable(s, MIXER_AMT);
//			amt2 = Util.pctToParm(s, 0, 127);
//			if (amt2 < 0 || amt2 > 127) {
//				System.out.println(mod.getName() + " Audio Amt2 " + s + " out of range 0-100");
//				amt2 = 0;
//			}
		}

		mp = mod.findParm("Audio Amt3");
		if (mp != null) {
			s = mp.getValue();
			amt3 = Util.matchToNumberTable(s, MIXER_AMT);
//			amt3 = Util.pctToParm(s, 0, 127);
//			if (amt3 < 0 || amt3 > 127) {
//				System.out.println(mod.getName() + " Audio Amt3 " + s + " out of range 0-100");
//				amt3 = 0;
//			}
		}
	}

	public String writeParameterDump() {
		return type + " 3 " + amt1 + " " + amt2 + " " + amt3 + " ";
	}

	public String writeCustomDump() {
		return null;
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("Audio In1")) {
			return 0;
		} else if (jack.equalsIgnoreCase("Audio In2")) {
			return 1;
		} else if (jack.equalsIgnoreCase("Audio In3")) {
			return 2;
		} else if (jack.equalsIgnoreCase("Mixer Out")) {
			return 0;
		} else {
			return -1;
		}
	}
}

class NMControlMixer extends NMModuleAbstract {
	private int amt1 = 0;
	private int inv1 = 0;
	private int amt2 = 0;
	private int inv2 = 0;

	NMControlMixer(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 66, pX, pY, pMod, pNmp);
		String s;
		ModuleParm mp;

		s = mod.findParm("CV Amt1").getValue();
		amt1 = Util.pctToParm(s, 0, 127);
		if (amt1 < 0) {
			inv1 = 1;
			amt1 *= -1;
		}
		if (amt1 > 127) {
			System.out.println(mod.getName() + " CV Amt1 " + s + " out of range -100 to 100 percent");
			amt1 = 0;
		}

		mp = mod.findParm("CV Amt2");
		if (mp != null) {
			s = mp.getValue();
			amt2 = Util.pctToParm(s, 0, 127);
			if (amt2 < 0) {
				inv2 = 1;
				amt2 *= -1;
			}
			if (amt2 > 127) {
				System.out.println(mod.getName() + " CV Amt2 " + s + " out of range -100 to 100 percent");
				amt2 = 0;
			}
		}
	}

	public String writeParameterDump() {
		return type + " 5 " + inv1 + " " + amt1 + " " + inv2 + " " + amt2 + " 1 ";
	}

	public String writeCustomDump() {
		return null;
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("CV In1")) {
			return 0;
		} else if (jack.equalsIgnoreCase("CV In2")) {
			return 1;
		} else if (jack.equalsIgnoreCase("CV Out")) {
			return 0;
		} else {
			return -1;
		}
	}
}

class NMADSREnvelope extends NMModuleAbstract {
	private int attack;
	private int decay;
	private int sustain;
	private int release;
	private int invert;

	static final String ATTACK[] = {
		"0.0005", "0.0007", "0.001", "0.0013",   "0.0015", "0.0018", "0.0021", "0.0023",
		"0.0026", "0.0029", "0.0032", "0.0035",   "0.0039", "0.0042", "0.0046", "0.0049",
		"0.0053", "0.0057", "0.0061", "0.0066",   "0.007", "0.0075", "0.008", "0.0085",
		"0.0091", "0.0097", "0.01", "0.011",   "0.012", "0.013", "0.0135", "0.014",
		"0.015", "0.016", "0.017", "0.019",   "0.02", "0.021", "0.023", "0.024",
		"0.026", "0.028", "0.03", "0.032",   "0.035", "0.037", "0.04", "0.043",
		"0.047", "0.051", "0.055", "0.059",   "0.064", "0.069", "0.075", "0.081",
		"0.088", "0.095", "0.103", "0.112",   "0.122", "0.132", "0.143", "0.156",

		"0.17", "0.185", "0.201", "0.219",   "0.238", "0.26", "0.283", "0.308",
		"0.336", "0.367", "0.4", "0.436",   "0.476", "0.52", "0.567", "0.619",
		"0.676", "0.738", "0.806", "0.881",   "0.962", "1.1", "1.2", "1.3",
		"1.4", "1.5", "1.6", "1.8",   "2", "2.1", "2.3", "2.6",
		"2.8", "3.1", "3.3", "3.7",   "4", "4.4", "4.8", "5.2",
		"5.7", "6.3", "6.8", "7.5",   "8.2", "9", "9.8", "10.7",
		"11.7", "12.8", "14", "15.3",   "16.8", "18.3", "20.1", "21.9",
		"24", "26.3", "28.7", "31.4",   "34.4", "37.6", "41.1", "45"
	};

	NMADSREnvelope(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 20, pX, pY, pMod, pNmp);
		String s;
		int i, j;
		ModuleParm mp;

		mp = mod.findParm("Attack");
		s = mp.getValue();
		attack = Util.matchToNumberTable(s, ATTACK);
		if (mp.getMorph() != null) {
			i = Util.matchToNumberTable(mp.getMorph().getValue(), ATTACK) -
					attack;
			j = mod.getGp().findModuleIndex(mod.getName()) + 1;
			s = "1 " + j + " 1 0 " + i + " ";
//			System.out.println("Attack morph=" + s);
			nmp.addMorph(s);
		}

		mp = mod.findParm("Decay");
		s = mp.getValue();
		decay = Util.matchToNumberTable(s, ATTACK);
		if (mp.getMorph() != null) {
			i = Util.matchToNumberTable(mp.getMorph().getValue(), ATTACK) -
					decay;
			j = mod.getGp().findModuleIndex(mod.getName()) + 1;
			s = "1 " + j + " 2 0 " + i + " ";
//			System.out.println("Decay morph=" + s);
			nmp.addMorph(s);
		}

		mp = mod.findParm("Sustain");
		s = mp.getValue();
		sustain = Util.pctToParm(s, 0, 127);
		if (sustain < 0) {
			System.out.println(mod.getName() + " sustain " + s + " out of range 0-100");
			sustain = 0;
		}
		if (sustain > 127) {
			System.out.println(mod.getName() + " sustain " + s + " out of range 0-100");
			sustain = 127;
		}
		if (mp.getMorph() != null) {
			i = Util.pctToParm(mp.getMorph().getValue(), 0, 127) - sustain;
			j = mod.getGp().findModuleIndex(mod.getName()) + 1;
			s = "1 " + j + " 3 0 " + i + " ";
//			System.out.println("Sustain morph=" + s);
			nmp.addMorph(s);
		}

		mp = mod.findParm("Release");
		s = mp.getValue();
		release = Util.matchToNumberTable(s, ATTACK);
		if (mp.getMorph() != null) {
			i = Util.matchToNumberTable(mp.getMorph().getValue(), ATTACK) -
					release;
			j = mod.getGp().findModuleIndex(mod.getName()) + 1;
			s = "1 " + j + " 4 0 " + i + " ";
//			System.out.println("Release morph=" + s);
			nmp.addMorph(s);
		}

		mp = mod.findParm("Invert");
		if (mp != null && mp.getValue().equalsIgnoreCase("On")) {
			invert = 1;
		} else {
			invert = 0;
		}
	}

	public String writeParameterDump() {
		return type + " 6 0 " + attack + " " + decay + " " + sustain + " " + release + " " + invert + " ";
	}

	public String writeCustomDump() {
		return null;
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("VCA In")) {
			return 0;
		} else if (jack.equalsIgnoreCase("Gate In")) {
			return 1;
		} else if (jack.equalsIgnoreCase("Env Out")) {
			return 0;
		} else if (jack.equalsIgnoreCase("VCA Out")) {
			return 1;
		} else {
			return -1;
		}
	}
}

class NMADEnvelope extends NMModuleAbstract {
	private int attack;
	private int decay;

	NMADEnvelope(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 84, pX, pY, pMod, pNmp);
		String s;
		int i, j;
		ModuleParm mp;

		mp = mod.findParm("Attack");
		s = mp.getValue();
		attack = Util.matchToNumberTable(s, NMADSREnvelope.ATTACK);
		if (mp.getMorph() != null) {
			i = Util.matchToNumberTable(mp.getMorph().getValue(), NMADSREnvelope.ATTACK) -
					attack;
			j = mod.getGp().findModuleIndex(mod.getName()) + 1;
			s = "1 " + j + " 0 0 " + i + " ";
//			System.out.println("Attack morph=" + s);
			nmp.addMorph(s);
		}

		mp = mod.findParm("Decay");
		s = mp.getValue();
		decay = Util.matchToNumberTable(s, NMADSREnvelope.ATTACK);
		if (mp.getMorph() != null) {
			i = Util.matchToNumberTable(mp.getMorph().getValue(), NMADSREnvelope.ATTACK) -
					decay;
			j = mod.getGp().findModuleIndex(mod.getName()) + 1;
			s = "1 " + j + " 1 0 " + i + " ";
//			System.out.println("Decay morph=" + s);
			nmp.addMorph(s);
		}
	}

	public String writeParameterDump() {
		return type + " 3 " + attack + " " + decay + " 0 ";
	}

	public String writeCustomDump() {
		return null;
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("Env Out")) {
			return 0;
		} else if (jack.equalsIgnoreCase("Gate In")) {
			return 0;
		} else {
			return -1;
		}
	}
}

class NMLFOA extends NMModuleAbstract {
	private int rate;
	private int range;
	private int waveform;

	static final String RATE_HI[] = {
		"0.26", "0.27", "0.29", "0.3", "0.32", "0.34", "0.36", "0.38",
		"0.41", "0.43", "0.46", "0.48", "0.51", "0.54", "0.57", "0.61",
		"0.64", "0.68", "0.72", "0.77", "0.81", "0.86", "0.91", "0.96",
		"1.02", "1.08", "1.15", "1.22", "1.29", "1.36", "1.45", "1.53",
		"1.62", "1.72", "1.82", "1.93", "2.04", "2.17", "2.29", "2.43",
		"2.58", "2.73", "2.89", "3.06", "3.24", "3.44", "3.64", "3.86",
		"4.09", "4.33", "4.59", "4.86", "5.15", "5.46", "5.78", "6.13",
		"6.49", "6.88", "7.28", "7.72", "8.18", "8.66", "9.18", "9.72",

		"10.3", "10.9", "11.6", "12.3", "13.0", "13.8", "14.6", "15.4",
		"16.4", "17.3", "18.4", "19.4", "20.6", "21.8", "23.1", "24.5",
		"26", "27.5", "29.1", "30.9", "32.7", "34.6", "36.7", "38.9",
		"41.2", "43.7", "46.3", "49", "51.9", "55", "58.3", "61.7",
		"65.4", "69.3", "73.4", "77.8", "82.4", "87.3", "92.5", "98",
		"104", "110", "117", "123", "131", "139", "147", "156",
		"165", "175", "185", "196", "208", "220", "233", "247",
		"262", "277", "294", "311", "330", "349", "370", "392"
	};
	static final String RATE_LO[] = {
		"0.0159", "0.0168", "0.0179", "0.0189", "0.02", "0.0212", "0.0225", "0.0238",
		"0.0253", "0.0267", "0.0283", "0.03", "0.0318", "0.0337", "0.0357", "0.0379",
		"0.04", "0.0424", "0.045", "0.0476", "0.0505", "0.0535", "0.0568", "0.0599",
		"0.0637", "0.0676", "0.0714", "0.0758", "0.08", "0.0847", "0.0901", "0.0952",
		"0.1", "0.11", "0.115", "0.12", "0.13", "0.135", "0.14", "0.15",
		"0.16", "0.17", "0.18", "0.19", "0.2", "0.21", "0.23", "0.24",
		"0.25", "0.27", "0.29", "0.3", "0.32", "0.34", "0.36", "0.38",
		"0.4", "0.43", "0.45", "0.48", "0.51", "0.54", "0.57", "0.61",

		"0.64", "0.68", "0.72", "0.76", "0.81", "0.86", "0.91", "0.96",
		"1.02", "1.08", "1.14", "1.21", "1.28", "1.36", "1.44", "1.52",
		"1.62", "1.71", "1.81", "1.92", "2.04", "2.16", "2.28", "2.42",
		"2.56", "2.72", "2.88", "3.05", "3.23", "3.42", "3.63", "3.84",
		"4.07", "4.31", "4.57", "4.84", "5.13", "5.43", "5.76", "6.1",
		"6.46", "6.85", "7.25", "7.68", "8.14", "8.62", "9.14", "9.68",
		"10.3", "10.9", "11.5", "12.2", "12.9", "13.7", "14.5", "15.4",
		"16.3", "17.2", "18.3", "19.4", "20.5", "21.7", "23", "24.4"
	};

	NMLFOA(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 24, pX, pY, pMod, pNmp);
		String s, rateList[];
		int i, j;
		ModuleParm mp;

		mp = mod.findParm("Rate");
		s = mp.getValue();
		double d = new Double(s).doubleValue();
		if (d > 24.4) {
			range = 2;
			rateList = RATE_HI;
		} else {
			range = 1;
			rateList = RATE_LO;
		}
		rate = Util.matchToNumberTable(s, rateList);
		if (mp.getMorph() != null) {
			i = Util.matchToNumberTable(mp.getMorph().getValue(), rateList) -
					rate;
			j = mod.getGp().findModuleIndex(mod.getName()) + 1;
			s = "1 " + j + " 0 0 " + i + " ";
//			System.out.println("Rate morph=" + s);
			nmp.addMorph(s);
		}

		s = mod.findParm("Waveform").getValue();
		if (s.equalsIgnoreCase("Sine")) {
			waveform = 0;
		} else if (s.equalsIgnoreCase("Tri")) {
			waveform = 1;
		} else if (s.equalsIgnoreCase("Saw")) {
			waveform = 2;
		} else if (s.equalsIgnoreCase("Square")) {
			waveform = 4;
		} else {
			System.out.println(mod.getName() + " waveform " + s + " not supported");
			waveform = 1;
		}
	}

	public String writeParameterDump() {
		return type + " 8 " + rate + " " + range + " " + waveform + " 0 0 0 64 0 ";
	}

	public String writeCustomDump() {
		return null;
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("Wave Out")) {
			return 1;
		} else {
			return -1;
		}
	}
}

class NMStereoChorus extends NMModuleAbstract {
	private int detune = 72;
	private int amount = 127;

	NMStereoChorus(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 94, pX, pY, pMod, pNmp);
		String s;
	}

	public String writeParameterDump() {
		return type + " 3 " + detune + " " + amount + " 0 ";
	}

	public String writeCustomDump() {
		return null;
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("Audio In")) {
			return 0;
		} else if (jack.equalsIgnoreCase("Audio Out L")) {
			return 0;
		} else if (jack.equalsIgnoreCase("Audio Out R")) {
			return 1;
		} else {
			return -1;
		}
	}
}

class NMRndStepGen extends NMModuleAbstract {
	private int rate = 64;

	NMRndStepGen(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 34, pX, pY, pMod, pNmp);
		String s;

		s = mod.findParm("Rate").getValue();
		rate = Util.matchToNumberTable(s, NMLFOA.RATE_LO);
	}

	public String writeParameterDump() {
		return type + " 1 " + rate + " ";
	}

	public String writeCustomDump() {
		return "1 1 ";
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("Wave Out")) {
			return 0;
		} else {
			return -1;
		}
	}
}

class NMRandomGen extends NMModuleAbstract {
	private int rate = 64;

	NMRandomGen(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 110, pX, pY, pMod, pNmp);
		String s;

		s = mod.findParm("Rate").getValue();
		rate = Util.matchToNumberTable(s, NMLFOA.RATE_LO);
	}

	public String writeParameterDump() {
		return type + " 1 " + rate + " ";
	}

	public String writeCustomDump() {
		return "1 1 ";
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("Wave Out")) {
			return 0;
		} else {
			return -1;
		}
	}
}

class NMGainControl extends NMModuleAbstract {
	private int shift = 0;

	NMGainControl(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 44, pX, pY, pMod, pNmp);
	}

	public String writeParameterDump() {
		return type + " 1 " + shift + " ";
	}

	public String writeCustomDump() {
		return null;
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("Level In1")) {
			return 0;
		} else if (jack.equalsIgnoreCase("VCA In")) {
			return 1;
		} else if (jack.equalsIgnoreCase("VCA Out")) {
			return 0;
		} else {
			return -1;
		}
	}
}

class NMConstant extends NMModuleAbstract {
	private int value;
	private int unipolar = 1;

	NMConstant(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 43, pX, pY, pMod, pNmp);
		ModuleParm mp = pMod.findParm("Value");
		if (mp != null) {
			value = new Integer(mp.getValue()).intValue();
		} else {
			value = 127;
		}
	}

	public String writeParameterDump() {
		return type + " 2 " + value + " " + unipolar + " ";
	}

	public String writeCustomDump() {
		return null;
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("Value Out")) {
			return 0;
		} else {
			return -1;
		}
	}
}

class NMInvLevShift extends NMModuleAbstract {
	private int polarity;
	private int invert;

	NMInvLevShift(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 57, pX, pY, pMod, pNmp);
		ModuleParm mp;
		mp = pMod.findParm("Polarity");
		if (mp.getValue().equalsIgnoreCase("bipolar")) {
			polarity = 0;
		} else if (mp.getValue().equalsIgnoreCase("negative")) {
			polarity = 1;
		} else {
			polarity = 2;
		}
		mp = pMod.findParm("Invert");
		if (mp.getValue().equalsIgnoreCase("On")) {
			invert = 1;
		} else {
			invert = 0;
		}
	}

	public String writeParameterDump() {
		return type + " 2 " + polarity + " " + invert + " ";
	}

	public String writeCustomDump() {
		return null;
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("Audio In")) {
			return 0;
		} else if (jack.equalsIgnoreCase("Audio Out")) {
			return 0;
		} else {
			return -1;
		}
	}
}

class NMPortamentoA extends NMModuleAbstract {
	private int time = 64;

	static final String PORTAMENTO[] = {
		"0.0005", "0.0007", "0.001", "0.0013", "0.0015", "0.0018", "0.0021", "0.0023",
		"0.0026", "0.0029", "0.0032", "0.0035", "0.0039", "0.0042", "0.0046", "0.0049", 
		"0.0053", "0.0057", "0.0061", "0.0066", "0.007", "0.0075", "0.008", "0.0085", 
		"0.0091", "0.0097", "0.01", "0.011", "0.012", "0.013", "0.0135", "0.014", 
		"0.015", "0.016", "0.017", "0.019", "0.02", "0.021", "0.023", "0.024", 
		"0.026", "0.028", "0.03", "0.032", "0.035", "0.037", "0.04", "0.043", 
		"0.047", "0.051", "0.055", "0.059", "0.064", "0.069", "0.075", "0.081", 
		"0.088", "0.095", "0.103", "0.112", "0.122", "0.132", "0.143", "0.156", 

		"0.170", "0.185", "0.201", "0.219", "0.238", "0.26", "0.283", "0.308", 
		"0.336", "0.367", "0.4", "0.436", "0.476", "0.52", "0.567", "0.619", 
		"0.676", "0.738", "0.806", "0.881", "0.962", "1.1", "1.2", "1.3", 
		"1.4", "1.5", "1.6", "1.8", "2", "2.1", "2.3", "2.6", 
		"2.8", "3.1", "3.3", "3.7", "4", "4.4", "4.8", "5.2", 
		"5.7", "6.3", "6.8", "7.5", "8.2", "9", "9.8", "10.7", 
		"11.7", "12.8", "14", "15.3", "16.8", "18.3", "20.1", "21.9", 
		"24", "26.3", "28.7", "31.4", "34.4", "37.6", "41.1", "45"
	};

	NMPortamentoA(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 48, pX, pY, pMod, pNmp);
		String s;
		int i, j;
		ModuleParm mp;

		mp = mod.findParm("Time");
		time = Util.matchToNumberTable(mp.getValue(), PORTAMENTO);
		if (mp.getMorph() != null) {
			i = Util.matchToNumberTable(mp.getMorph().getValue(), PORTAMENTO) -
					time;
			j = mod.getGp().findModuleIndex(mod.getName()) + 1;
			s = "1 " + j + " 0 0 " + i + " ";
//			System.out.println("Portamento morph=" + s);
			nmp.addMorph(s);
		}
	}

	public String writeParameterDump() {
		return type + " 1 " + time + " ";
	}

	public String writeCustomDump() {
		return null;
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("Note In")) {
			return 0;
		} else if (jack.equalsIgnoreCase("Note Out")) {
			return 0;
		} else {
			return -1;
		}
	}
}

class NMPortamentoB extends NMModuleAbstract {
	private int time = 64;

	NMPortamentoB(String pName, int pX, int pY, Module pMod, NordModularPatch pNmp) {
		super(pName, 16, pX, pY, pMod, pNmp);
		String s;
		int i, j;
		ModuleParm mp;

		mp = mod.findParm("Time");
		time = Util.matchToNumberTable(mp.getValue(), NMPortamentoA.PORTAMENTO);
		if (mp.getMorph() != null) {
			i = Util.matchToNumberTable(mp.getMorph().getValue(), NMPortamentoA.PORTAMENTO) -
					time;
			j = mod.getGp().findModuleIndex(mod.getName()) + 1;
			s = "1 " + j + " 0 0 " + i + " ";
//			System.out.println("Portamento morph=" + s);
			nmp.addMorph(s);
		}
	}

	public String writeParameterDump() {
		return type + " 1 " + time + " ";
	}

	public String writeCustomDump() {
		return null;
	}

	public int findJack(String jack) {
		if (jack.equalsIgnoreCase("Note In")) {
			return 0;
		} else if (jack.equalsIgnoreCase("Jmp In")) {
			return 1;
		} else if (jack.equalsIgnoreCase("Note Out")) {
			return 0;
		} else {
			return -1;
		}
	}
}
