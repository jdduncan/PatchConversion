
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
 * Describes a synth patch (a.k.a. preset, program, etc).  Implementor
 * must be able to store all patch parameters in its attributes, read and
 * write patch sysex, and read and write synth-specific representation
 * of patch as XML.
 * 
 * Version history:
 * 1.01 New module type mod_vca
 * 1.00 First release
 *
 * @author Kenneth L. Martinez
 */

import java.util.*;

public class GenericPatch {
	private String patchName = "No Name";
	private String patchNumber;
	private String patchBank;
	private String patchComment;
	private String version = "1.01";
	private String synthGenericVersion;
	private ArrayList modules; // oscillators, filters, etc which make up a patch
	private ArrayList connections; // all connections between modules
	private ArrayList matrixMods; // allowable matrix modulations (if applicable)
	private GenericPatch inputGP;

	GenericPatch(String s) {
		synthGenericVersion = s;
		modules = new ArrayList();
		connections = new ArrayList();
		matrixMods = new ArrayList();
	}

	/**
	 * Set all parameter values to initial settings - mixer inputs at
	 * zero, modulation amounts to zero, etc
	 */
//	public void initializeModules() {
//		for (int i = 0; i < modules.size(); i++) {
//			((Module)modules.get(i)).initialize();
//		}
//	}

	public String getPatchName() {
		return patchName;
	}

	public void setPatchName(String s) {
		patchName = s;
	}

	public String getPatchNumber() {
		return patchNumber;
	}

	public void setPatchNumber(String s) {
		patchNumber = s;
	}

	public String getPatchBank() {
		return patchBank;
	}

	public void setPatchBank(String s) {
		patchBank = s;
	}

	public String getPatchComment() {
		return patchComment;
	}

	public void setPatchComment(String s) {
		patchComment = s;
	}

	/**
	 * see if all modules have values within their valid ranges
	 * @return valid
	 */
	public boolean isValid() {
		int i;
		try {
			for (i = 0; i < modules.size(); i++) {
				if (((SynthParm)modules.get(i)).isValid() == false) {
					return false;
				}
			}
		} catch (NullPointerException e) {
			return false;
		}
		return true;
	}

	public void readInfoXML(String xml) {
		String s = XMLReader.getTagValue(xml, "patch_name");
		if (s != null) {
			setPatchName(s);
		}
		setPatchNumber(XMLReader.getTagValue(xml, "patch_number"));
		setPatchBank(XMLReader.getTagValue(xml, "patch_bank"));
		setPatchComment(XMLReader.getTagValue(xml, "patch_comment"));
	}

	public ArrayList getModules() {
		return modules;
	}

	public ArrayList getConnections() {
		return connections;
	}

	public ArrayList getMatrixMods() {
		return matrixMods;
	}

	public GenericPatch getInputGP() {
		return inputGP;
	}

	public void addModule(Module mod) {
		modules.add(mod);
		mod.setGp(this);
	}

	public void addModule(int i, Module mod) {
		modules.add(i, mod);
		mod.setGp(this);
	}

	public void removeModule(Module mod) {
		modules.remove(mod);
		mod.setGp(null);
	}

	public void addConnection(Connection conn) {
		// This is now done in ModuleInputJack.addConn()
//		if (findConnectionToTarget(conn.getTargetJack()) != null) {
//			System.out.println("Error: can't add connection from " +
//					conn.getSourceJack().getMod().getName() + " " +
//					conn.getSourceJack().getName() + " to " +
//					conn.getTargetJack().getMod().getName() + " " +
//					conn.getTargetJack().getName() + " - target already connected");
//			int i = 1 / 0; // abort
//		}
		connections.add(conn);
		conn.setGp(this);
	}

	public void removeConnection(String sourceModName, String sourceMjName,
			String targetModName, String targetMjName) {
		Connection conn = findConnection(sourceModName, sourceMjName,
				targetModName, targetMjName);
		removeConnection(conn);
	}

	public void removeConnection(Connection conn) {
		conn.getSourceJack().removeConn(conn);
		conn.getTargetJack().removeConn();
		connections.remove(conn);
		conn.setGp(null);
	}

	public void addMatrixMod(MatrixMod mm) {
		matrixMods.add(mm);
		mm.setGp(this);
	}

	public boolean addConnectionIfNotFound(String sourceModName, String sourceMjName,
			String targetModName, String targetMjName) throws PatchDefinitionException {
		Connection conn;
		ModuleOutputJack mjSource;
		ModuleInputJack mjTarget;

		if (sourceModName.equalsIgnoreCase(targetModName) &&
				sourceMjName.equalsIgnoreCase(targetMjName)) {
					return true;
		}
		conn = findConnection(sourceModName, sourceMjName,
				targetModName, targetMjName);
		if (conn == null) {
//			System.out.println("addConnection " + sourceModName + ", " + sourceMjName + ", " +
//					targetModName + ", " + targetMjName);
			mjSource = findModuleOutputJack(sourceModName, sourceMjName);
			if (mjSource == null) {
				System.out.println("Error: Can't find source jack - can't duplicate connection " +
						sourceModName + "," + sourceMjName + " to " + targetModName + "," + targetMjName);
				return false;
			}
			mjTarget = findModuleInputJack(targetModName, targetMjName);
			if (mjTarget == null) {
				System.out.println("Error: Can't find target jack - can't duplicate connection " +
						sourceModName + "," + sourceMjName + " to " + targetModName + "," + targetMjName);
				return false;
			}
			conn = new Connection(mjSource, mjTarget);
			addConnection(conn);
			conn.setGp(this);
		}
		return true;
	}

	/**
	 * see if input XML contains this type of patch
	 */
	public boolean matchXML(String xml) {
		if (xml.indexOf("generic_patch") != -1) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * convert patch internal variables to XML
	 */
	public String writeXML() throws PatchDefinitionException {
		int i;
		StringBuffer sb = new StringBuffer();

		reduceModNumbers();

		sb.append("<generic_patch>");
		sb.append("<version>" + version + "</version>");
		sb.append("<synth_generic_version>" + synthGenericVersion + "</synth_generic_version>");
		sb.append("<patch_name>" + patchName + "</patch_name>");
		if (patchNumber != null) {
			sb.append("<patch_number>" + patchNumber + "</patch_number>");
		}
		if (patchBank != null) {
			sb.append("<patch_bank>" + patchBank + "</patch_bank>");
		}
		if (patchComment != null) {
			sb.append("<patch_comment>" + patchComment + "</patch_comment>");
		}
		for (i = 0; i < modules.size(); i++) {
			sb.append(((Module)modules.get(i)).writeXML());
		}
		sb.append("</generic_patch>");

		return sb.toString();
	}

	/**
	 * Reduce numbering for all module types in the generic patch.  First of a
	 * type should be 1, next is 2, etc.
	 * 
	 */
	public void reduceModNumbers() {
		int i, j, num;
		String s;
		Module mod, mod2;
		HashMap modTypesChecked = new HashMap();

		for (i = 0; i < modules.size(); i++) {
			mod = (Module)modules.get(i);
			if (mod.getUsed() < 3) {
				continue;
			}
			// Have we processed this module type yet?
			if (modTypesChecked.containsKey(mod.getType())) {
				continue;
			}
			// No - add it to the list
			modTypesChecked.put(mod.getType(), null);
			// Find all modules of this type and reduce their numbers.
			// This works for the first module found also (although the
			// type compare is not necessary)
			for (j = i, num = 0; j < modules.size(); j++) {
				mod2 = (Module)modules.get(j);
				if (mod2.getUsed() == 3 && mod2.getType().equalsIgnoreCase(mod.getType())) {
					num++;
					if (mod2.getNumber() != num) {
						mod2.setNumber(num);
					}
					s = mod2.getName();
					if (Character.isDigit(s.charAt(s.length() - 1))) {
						mod2.setName(s.substring(0, s.length() - 1) + num);
					}
					reduceJackNumbers(mod2);
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
	public void reduceJackNumbers(Module mod) {
		int i, j, num = 0, oldNum;
		String prefix;
		Module mod2;
		ModuleInputJack mij, mij2;
		ModuleParm mp;
		HashMap jackTypesChecked = new HashMap();

		for (i = 0; i < mod.getInputJacks().size(); i++) {
			mij = (ModuleInputJack)mod.getInputJacks().get(i);
			if (mij.getNumber() > 0) {
				prefix = mij.getPrefix();
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
					mij2 = (ModuleInputJack)mod.getInputJacks().get(j);
					if (mij2.isUsed() && mij2.getNumber() > 0 &&
							mij2.getPrefix().equalsIgnoreCase(prefix)) {
						num++;
						oldNum = mij2.getNumber();
						mij2.setNumber(num);
						mp = mij2.getAttenuator();
						if (mp != null) {
							mp.setNumber(num);
						}
					}
				}
			}
		}
	}

	/**
	 * read XML into internal variables
	 */
	public boolean readXML(String xml) throws PatchDefinitionException {
		if (matchXML(xml) == false) {
			System.out.println("Error: input file has no generic patch");
			return false;
		}

		// Read XML into temp patch structure of modules/jacks/connections
		inputGP = new GenericPatch(synthGenericVersion);
		String tag[], sourceModule, sourceJack, targetModule, targetJack;
		Module mod;
		Connection conn;
		XMLReader xr = new XMLReader(XMLReader.getTagValue(xml, "generic_patch"));
		while ((tag = xr.getNextTag()) != null) {
			if (tag[0].equalsIgnoreCase("version")) {
				if (tag[1].equalsIgnoreCase(version) == false) {
					System.out.println("Warning: Expected generic patch version " +
							version + ", input is " + tag[1]);
				}
			} else if (tag[0].equalsIgnoreCase("synth_generic_version")) {
				// FIXME can't test for this, can we?  differs by synth
//				if (tag[1].equalsIgnoreCase(synthGenericVersion) == false) {
//					System.out.println("Warning: Expected synth generic version " +
//							synthGenericVersion + ", input is " + tag[1]);
//				}
			} else if (tag[0].equalsIgnoreCase("patch_name")) {
				patchName = tag[1];
			} else if (tag[0].equalsIgnoreCase("patch_number")) {
				patchNumber = tag[1];
			} else if (tag[0].equalsIgnoreCase("patch_bank")) {
				patchBank = tag[1];
			} else if (tag[0].equalsIgnoreCase("patch_comment")) {
				patchComment = tag[1];
			} else if (tag[0].equalsIgnoreCase("module")) {
				mod = new Module(tag[1]);
				mod.setUsed(3);
				inputGP.addModule(mod);
			} else {
				System.out.println("Error: unknown tag " + tag[0]);
				return false;
			}
		}

		for (int i = 0; i < inputGP.getModules().size(); i++) {
			mod = (Module)inputGP.getModules().get(i);
			for (int j = 0; j < mod.getInputJacks().size(); j++) {
				ModuleInputJack mij = (ModuleInputJack)mod.getInputJacks().get(j);
				inputGP.addConnection(mij.buildConnection());
			}
		}

		return true;
	}

	/**
	 * convert input XML into our generic patch
	 */
	public boolean convertXML() throws PatchDefinitionException {

		// Compare module by module, to see if all input modules can
		// be duplicated by this synth
		HashMap modMatchLists = initialModuleMatch();
		if (modMatchLists.size() == 0) {
			return false;
		}

		ModMatchData md;
		Module mod, ourMod;
		ModuleParm mp, ourMp;
		ModuleInputJack mij, ourMij;
		ModuleOutputJack moj, ourMoj;
		ArrayList modMatches, a;
		Iterator e;
		int i, j, k, num;
		int debug = 0;
		if (debug == 1) {
			System.out.println("=================================================");
			e = inputGP.moduleIterator();
			while (e.hasNext()) {
				mod = (Module)e.next();
				modMatches = (ArrayList)modMatchLists.get(mod);
				for (k = 0; k < modMatches.size(); k++) {
					md = (ModMatchData)modMatches.get(k);
					ourMod = md.getMod();
					System.out.println(mod.getName() + " possible match " + ourMod.getName());
					for (i = 0; i < mod.getParms().size(); i++) {
						mp = (ModuleParm)mod.getParms().get(i);
						a = (ArrayList)md.getParmMatches().get(i);
						for (j = 0; j < a.size(); j++) {
							ourMp = (ModuleParm)a.get(j);
							System.out.println("  " + mp.getName() + " possible match " + ourMp.getName());
						}
					}
					for (i = 0; i < mod.getInputJacks().size(); i++) {
						mij = (ModuleInputJack)mod.getInputJacks().get(i);
						a = (ArrayList)md.getInputJackMatches().get(i);
						for (j = 0; j < a.size(); j++) {
							ourMij = (ModuleInputJack)a.get(j);
							System.out.println("  " + mij.getName() + " possible match " + ourMij.getName());
						}
					}
					for (i = 0; i < mod.getOutputJacks().size(); i++) {
						moj = (ModuleOutputJack)mod.getOutputJacks().get(i);
						ourMoj = (ModuleOutputJack)md.getOutputJackMatches().get(i);
						System.out.println("  " + moj.getName() + " possible match " + ourMoj.getName());
					}
				}
			}
		}

		// Make final module matches, selecting a single module to match each
		// input module if it has the same connections.  Also select matches
		// for all parms and jacks of matched modules.
		// FIXME should probably add way to override: maybe allow to try
		// favored combination first (which may not be in numeric order),
		// or allow combination to be specified which is known to work,
		// if automatic routine fails or gives undesired result
		HashMap finalMatch = finalModuleMatch(modMatchLists);
		if (finalMatch.size() == 0) {
			System.out.println("Error: cannot implement the input generic patch");
			return false;
		}

		// Definite matches have been selected, so copy parm values from input
		// to our generic patch and mark our matching jacks and parms used.
		boolean found;
		e = inputGP.moduleIterator();
		while (e.hasNext()) {
			mod = (Module)e.next();
			md = (ModMatchData)finalMatch.get(mod);
			ourMod = md.getMod();
			// FIXME temporary kludge copy process
			ourMod.setUsed(3);
			for (i = 0; i < mod.getParms().size(); i++) {
				mp = (ModuleParm)mod.getParms().get(i);
				ourMp = (ModuleParm)md.getParmMatches().get(i);
				if (ourMp.getPv().validateParm(mp.getValue()) == false) {
					System.out.println("warning: Module " + ourMod.getName() + " parm " +
							ourMp.getName() + " cannot accept value " + mp.getValue());
				} else {
					ourMp.setValue(mp.getValue());
				}
				ourMp.setUsed(true);
				if (mp.getMorph() != null && mp.getMorph().isUsed()) {
					if (ourMp.getMorph() == null) {
						System.out.println("warning: Can't create morph for module " +
								ourMod.getName() + " parm " + ourMp.getName());
					} else {
						ourMp.getMorph().setValue(mp.getMorph().getValue());
						ourMp.getMorph().setUsed(true);
					}
				}
			}
			for (i = 0; i < mod.getInputJacks().size(); i++) {
				mij = (ModuleInputJack)mod.getInputJacks().get(i);
				ourMij = (ModuleInputJack)md.getInputJackMatches().get(i);
				ourMij.setUsed(true);
			}
			for (i = 0; i < mod.getOutputJacks().size(); i++) {
				moj = (ModuleOutputJack)mod.getOutputJacks().get(i);
				ourMoj = (ModuleOutputJack)md.getOutputJackMatches().get(i);
				ourMoj.setUsed(true);
			}
		}

		setPatchName(inputGP.getPatchName());
		setPatchNumber(inputGP.getPatchNumber());
		setPatchBank(inputGP.getPatchBank());
		setPatchComment(inputGP.getPatchComment());

		return true;
	}

	/**
	 * Prepare lists of our potential matching modules for each of the
	 * modules in the input generic patch
	 */
	HashMap initialModuleMatch() {
		Module mod;
		HashMap modMatchLists = new HashMap();
		ArrayList modMatches;
		int i, j, num;
		boolean failed = false;

		// Process all modules of each type in a group
		ModuleTypeIterator mte = inputGP.moduleTypeIterator();
		String typ;
		while (mte.hasNext()) {
			typ = mte.nextType();
			// Find all input modules of this type
			mte.setToFirstMod();
			num = 0;
			while ((mod = mte.nextMod()) != null) {
				num++;
				modMatches = initialMatchToOurModules(mod);
				if (modMatches.size() == 0) {
					System.out.println("Error: unable to match input module " + mod.getName());
					failed = true;
				}
				modMatchLists.put(mod, modMatches);
			}
		}
		if (failed) {
			System.out.println("Error: cannot implement the input generic patch");
			return new HashMap();
		}
		return modMatchLists;
	}

	/**
	 * Returns a list of modules in our generic patch which are potential
	 * matches for the given module from the input generic patch.  "Match"
	 * means it has at least the same number of each type of jack and parm.
	 * Also makes lists of potential matches for each jack and parm.
	 * 
	 * @param mod
	 * @return
	 */
	ArrayList initialMatchToOurModules(Module mod) {
		ArrayList modMatches = new ArrayList(), errorList, modErrorList = new ArrayList();
		Module ourMod;
		ModuleParm mp, ourMp;
		ModuleOutputJack moj, ourMoj, matchJack;
		ModuleInputJack mij, ourMij;
		Connection conn;
		MatrixMod mm;
		ArrayList parmMatches, inputJackMatches, outputJackMatches;
		ArrayList parms, inputJacks, outputJacks;
		boolean found;
		int i, j;
		Iterator e, e2;
		GenericPatch inputGP = mod.getGp();

		e = moduleIterator();
		while (e.hasNext()) {
			ourMod = (Module)e.next();
			if (ourMod.getType().equalsIgnoreCase(mod.getType()) == false) {
				continue;
			}
			// FIXME added to prevent counter-intuitive match; is there a better way?
			if (mod.getName().equals("Amp Envelope") &&
					ourMod.getName().equals("Amp Envelope") == false) {
				continue;
			}
			errorList = new ArrayList();

			// compare each output jack
			outputJackMatches = new ArrayList();
			for (i = 0; i < mod.getOutputJacks().size(); i++) {
				moj = (ModuleOutputJack)mod.getOutputJacks().get(i);
				matchJack = null;
				ourMoj = ourMod.findOutputJack(moj.getName());
				if (ourMoj == null) {
					errorList.add("Module " + mod.getName() + " unable to match output jack " +
							moj.getName());
				} else {
					// only match if our module's output is connected to
					// same type of module and input jack with same prefix
					found = true;
					e2 = inputGP.connectionTargetIterator(moj);
					while (e2.hasNext()) {
						conn = (Connection)e2.next();
						if (findConnectionToSameType(ourMoj.getMod().getName(),
								ourMoj.getName(),
								conn.getTargetJack().getMod().getType(),
								conn.getTargetJack().getPrefix()) == false) {
							found = false;
							errorList.add("Module " + mod.getName() + " unable to match connection of output jack " +
									moj.getName() + " to " + conn.getTargetJack().getMod().getType() +
									" " + conn.getTargetJack().getPrefix());
						}
					}
					if (found) {
						matchJack = ourMoj;
					}
				}
				outputJackMatches.add(matchJack);
			}

			// compare each input jack
			inputJackMatches = new ArrayList();
			for (i = 0; i < mod.getInputJacks().size(); i++) {
				mij = (ModuleInputJack)mod.getInputJacks().get(i);
				conn = inputGP.findConnectionToTarget(mij);
				inputJacks = new ArrayList();
				for (j = 0; j < ourMod.getInputJacks().size(); j++) {
					ourMij = (ModuleInputJack)ourMod.getInputJacks().get(j);
					if (ourMij.getPrefix().equalsIgnoreCase(mij.getPrefix())) {
						if ((mij.getAttenuator() != null && ourMij.getAttenuator() != null) ||
								(mij.getAttenuator() == null && ourMij.getAttenuator() == null)) {
							if (findConnectionFromSameType(conn.getSourceJack().getMod().getType(),
									conn.getSourceJack().getPrefix(),
									ourMij.getMod().getName(),
									ourMij.getName())) {
								inputJacks.add(ourMij);
							}
						}
					}
				}
				if (inputJacks.size() == 0) {
					for (j = 0; j < matrixMods.size(); j++) {
						mm = (MatrixMod)matrixMods.get(j);
						if (mm.seeIfJackCanBeAdded(conn.getSourceJack().getMod().getType(),
								conn.getSourceJack().getPrefix(),
								ourMod.getName(),
								mij.getPrefix())) {
							break;
						}
					}
					if (j >= matrixMods.size()) {
						errorList.add("Module " + mod.getName() + " unable to match input jack " +
								mij.getName());
					}
				}
				inputJackMatches.add(inputJacks);
			}

			// compare each input parm
			parmMatches = new ArrayList();
			for (i = 0; i < mod.getParms().size(); i++) {
				mp = (ModuleParm)mod.getParms().get(i);
				parms = new ArrayList();
				for (j = 0; j < ourMod.getParms().size(); j++) {
					ourMp = (ModuleParm)ourMod.getParms().get(j);
					// FIXME compare parm range here too?  If so, range match
					// would be required.  As it is, match can occur even if
					// value is out of range.
					if (ourMp.getPrefix().equalsIgnoreCase(mp.getPrefix())) {
						parms.add(ourMp);
					}
				}
				if (parms.size() == 0) {
					for (j = 0; j < matrixMods.size(); j++) {
						mm = (MatrixMod)matrixMods.get(j);
						if (mm.seeIfParmCanBeAdded(mp.getPrefix())) {
							break;
						}
					}
					if (j >= matrixMods.size()) {
						errorList.add("Module " + mod.getName() + " unable to match parm " +
								mp.getName());
					}
				}
				parmMatches.add(parms);
			}
			if (errorList.size() == 0) {
				modMatches.add(new ModMatchData(ourMod, parmMatches, inputJackMatches,
						outputJackMatches));
			} else {
				modErrorList.add("Could not match input module " + mod.getName() +
						" to our module " + ourMod.getName());
				modErrorList.addAll(errorList);
			}
		}
		if (modMatches.size() == 0) {
			System.out.println(modErrorList);
		}
		return modMatches;
	}

	/**
	 * Iterates through groupings of potential matches, stopping at the
	 * first one which can implement the generic patch.
	 *  
	 * @param modMatchLists
	 * @return
	 */
	HashMap finalModuleMatch(HashMap modMatchLists) throws PatchDefinitionException {
		Module mod, mod2, ourMod;
		ModuleParm mp, ourMp;
		ModuleOutputJack moj, ourMoj = null;
		ModuleInputJack mij, ourMij;
		MatrixMod mm;
		ArrayList modMatches, trialMatch, a, errorList = new ArrayList(), unmatchedConns;
		ModMatchData md, md2, mdNew;
		Connection conn;
		Object o;
		Iterator e;
		int i, j, k, indx, cnt = 0;
		boolean found;
		StringBuffer sb;

		// For each permutation of matching modules, see if all connections
		// are possible.  Return the first complete match.
		e = new ModulePermutationIterator(inputGP, modMatchLists);
		while (e.hasNext()) {
			trialMatch = (ArrayList)e.next();
			cnt++;
			sb = new StringBuffer("Trial match " + cnt + " ");
			for (i = 0; i < trialMatch.size(); i++) {
				md = (ModMatchData)trialMatch.get(i);
				sb.append("/" + md.getMod().getName());
			}
			sb.append("/");
			errorList.add(sb.toString());

			for (i = 0; i < matrixMods.size(); i++) {
				((MatrixMod)matrixMods.get(i)).removeCurrentMod();
			}

			HashMap finalMatch = new HashMap();
			HashMap alreadyMatchedList = new HashMap();

			// Try to use all hard-wired connections before resorting to using
			// the limited number of matrix mods
			unmatchedConns = finalMatchHardWired(trialMatch, finalMatch, alreadyMatchedList, errorList);

// FIXME not ready yet
//			finalMatchCheckMMSourceGroups(finalMatch, alreadyMatchedList, unmatchedConns);

			finalMatchMultiDestMods(trialMatch, finalMatch, alreadyMatchedList, errorList);

			found = finalMatchSingleDestModsAndParms(trialMatch, finalMatch, alreadyMatchedList, errorList);

			// FIXME - may still use multi-dest mod route as single route
			//  if its extra connection is to unused jack (e.g. PWM when
			//  osc waveform is saw)

			if (found) {
				return finalMatch;
			}
		}

		if (cnt == 0) {
			System.out.println("Error: cannot match one or more input modules");
		} else {
			System.out.println(errorList);
		}
		return new HashMap(); // No match found, return empty match list
	}

	/**
	 * Match input jacks which have hard-wired connections
	 */
	public ArrayList finalMatchHardWired(ArrayList trialMatch, HashMap finalMatch,
			HashMap alreadyMatchedList, ArrayList errorList) {
		Module mod, mod2, ourMod;
		ModuleParm mp, ourMp;
		ModuleInputJack mij, ourMij;
		ArrayList a;
		ArrayList unmatchedConns = new ArrayList();
		ModMatchData md, md2, mdNew;
		Connection conn;
		Object o;
		int i, j, k, indx;
		HashMap alreadyMatched;

		for (i = 0; i < inputGP.getModules().size(); i++) {
			mod = (Module)inputGP.getModules().get(i);
			md = (ModMatchData)trialMatch.get(i);
			alreadyMatched = new HashMap();
			mdNew = new ModMatchData(md.getMod(),
					(ArrayList)md.getParmMatches().clone(),
					(ArrayList)md.getInputJackMatches().clone(),
					(ArrayList)md.getOutputJackMatches().clone());

			for (j = 0; j < mod.getInputJacks().size(); j++) {
				mij = (ModuleInputJack)mod.getInputJacks().get(j);
				o = mdNew.getInputJackMatches().get(j);
				a = (ArrayList)o;
				// Match by prefix and same source module
				for (k = 0; k < a.size(); k++) {
					ourMij = (ModuleInputJack)a.get(k);
					if (alreadyMatched.containsKey(ourMij)) {
						continue;
					}
					if (ourMij.getPrefix().equalsIgnoreCase(mij.getPrefix())) {
						// Find source module which is connected to input patch's input jack
						conn = inputGP.findConnectionToTarget(mij);
						mod2 = conn.getSourceJack().getMod();
						indx = inputGP.getModules().indexOf(mod2);
						md2 = (ModMatchData)trialMatch.get(indx);
						ourMod = md2.getMod(); // get our matching source module
						conn = findConnectionToTarget(ourMij);
						// If this jack is connected to our matching source module, it's a match
						if (conn != null && ourMod == conn.getSourceJack().getMod()) {
							mdNew.getInputJackMatches().set(j, ourMij);
							alreadyMatched.put(ourMij, mij);
							// If there's an attenuator, match it to our attenuator
							mp = mij.getAttenuator();
							if (mp != null) {
								ourMp = ourMij.getAttenuator();
								indx = mod.getParms().indexOf(mp);
								mdNew.getParmMatches().set(indx, ourMp);
							}
							break;
						}
					}
				}
				if (j >= mod.getInputJacks().size()) {
					unmatchedConns.add(mij.getConn());
				}
			}

			finalMatch.put(mod, mdNew);
			alreadyMatchedList.put(mod, alreadyMatched);
		}
		return unmatchedConns;
	}

	/**
	 * All matrix mods in a given source group must have the same source jack.
	 * Look for groups of matrix mods; if found, try to assign the largest
	 * groups first.  E.g. if key velocity affects 2 dest jacks in a patch, first
	 * try to use a source group containing exactly 2 matrix mods; if key
	 * velocity is the source for a group with more than 2 matrix mods, the
	 * additional matrix mods can't be used in this patch.  
	 */
	public void finalMatchCheckMMSourceGroups(HashMap finalMatch,
			HashMap alreadyMatchedList, ArrayList unmatchedConns) {
		MatrixMod mm;
		ArrayList a, sourceGroups = new ArrayList();
		HashMap h = new HashMap();
		int i, grp;

		// Look for matrix mods with group numbers
		for (i = 0; i < matrixMods.size(); i++) {
			mm = (MatrixMod)matrixMods.get(i);
			grp = mm.getSourceGroup();
			if (grp != 0) {
				a = (ArrayList)h.get(new Integer(grp));
				if (a == null) {
					a = new ArrayList();
					h.put(new Integer(grp), a);
				}
				a.add(mm);
			}
		}

		// Create list of groups, ordered from largest to smallest
		Iterator e = h.values().iterator();
		while (e.hasNext()) {
			a = (ArrayList)e.next();
			if (a.size() == 1) { // Groups need 2 or more
				continue;
			}
			for (i = 0; i < sourceGroups.size(); i++) {
				if (a.size() > ((ArrayList)sourceGroups.get(i)).size()) {
					sourceGroups.add(i, a);
					break;
				}
			}
			if (i >= sourceGroups.size()) {
				sourceGroups.add(a);
			}
		}

		Module mod;
		ModuleInputJack mij;
		Object o;
		HashMap alreadyMatched;
		ModMatchData mdNew;
		int j;

		// Group unmatched connections by source jack, largest group first
		// Now try to match largest source groups with the source jacks
		// having the most unmatched connections
	}

	/**
	 * Try to implement matrix mods which have multiple dest jacks (e.g. an
	 * LFO affects both Osc1's and Osc2's Pulse Width).
	 */
	public void finalMatchMultiDestMods(ArrayList trialMatch, HashMap finalMatch,
			HashMap alreadyMatchedList, ArrayList errorList) throws PatchDefinitionException {
		Module mod, mod2, ourMod;
		ModuleParm mp, ourMp;
		ModuleOutputJack moj, ourMoj = null;
		MatrixMod mm;
		ModMatchData md, md2, mdNew;
		Connection conn;
		int i, j, k, indx;
		HashMap alreadyMatched;
		// Loop thru each input module's output jacks
		//  Find jacks with connections to more than one jack w/same prefix,
		//      not yet matched, owned by same module type, whose attenuators
		//      have the same value.
		//   If not found, skip to next output jack & then next input module
		//  Feed output jack & input jacks to mm to match
		for (i = 0; i < inputGP.getModules().size(); i++) {
			mod = (Module)inputGP.getModules().get(i);
			alreadyMatched = (HashMap)alreadyMatchedList.get(mod);
			mdNew = (ModMatchData)finalMatch.get(mod);

			// Try to implement matrix mods which have multiple dest jacks
			 outer: for (j = 0; j < mod.getOutputJacks().size(); j++) {
				moj = (ModuleOutputJack)mod.getOutputJacks().get(j);
				HashMap h, targetJacks = new HashMap();
				ModuleInputJack mij2, inTargetMj[] = new ModuleInputJack[2];
				for (k = 0; k < inputGP.getConnections().size(); k++) {
					conn = (Connection)inputGP.getConnections().get(k);
					// Find connections from this output jack
					if (conn.getSourceJack() != moj) {
						continue;
					}
					// See if target jack has already been matched
					mij2 = conn.getTargetJack();
					mod2 = mij2.getMod();
					h = (HashMap)alreadyMatchedList.get(mod2);
					if (h.containsValue(mij2)) {
						continue;
					}
					// FIXME this only works for one dual on each output jack
					if (targetJacks.containsKey(mij2.getPrefix())) {
						inTargetMj[0] = (ModuleInputJack)targetJacks.get(mij2.getPrefix());
						inTargetMj[1] = mij2;
						// found - now try to get mm w/corresponding conns
						String ourModNames[] = new String[2];
						md2 = (ModMatchData)finalMatch.get(inTargetMj[0].getMod());
						ourModNames[0] = md2.getMod().getName(); // get our matching source module
						md2 = (ModMatchData)finalMatch.get(inTargetMj[1].getMod());
						ourModNames[1] = md2.getMod().getName(); // get our matching source module
						ourMod = mdNew.getMod();
						// FIXME get our source mod & jack; get our dest mods
						ModuleInputJack ourMijs[];
						for (k = 0; k < matrixMods.size(); k++) {
							mm = (MatrixMod)matrixMods.get(k);
							ourMijs = mm.createMultiDestMod(ourMod.getName(), conn.getSourceJack().getName(),
									ourModNames, mij2.getPrefix());
							if (ourMijs != null) {
								for (int k2 = 0; k2 < ourMijs.length; k2++) {
									indx = inTargetMj[k2].getMod().getInputJacks().indexOf(inTargetMj[k2]);
									md2 = (ModMatchData)finalMatch.get(inTargetMj[k2].getMod());
									md2.getInputJackMatches().set(indx, ourMijs[k2]);
									alreadyMatched.put(ourMijs[k2], inTargetMj[k2]);
									// If there's an attenuator, match it to our attenuator
									mp = inTargetMj[k2].getAttenuator();
									if (mp != null) {
										ourMp = ourMijs[k2].getAttenuator();
										indx = inTargetMj[k2].getMod().getParms().indexOf(mp);
										md2.getParmMatches().set(indx, ourMp);
									}
								}
								break;
							}
						}
						if (k >= matrixMods.size()) {
							// FIXME is this really an error, or just a warning?
							// Seems it's OK if two single dest mods can be used
							errorList.add("finalModuleMatch could not final match dual mod route from " + moj.getMod().getName() + " " + moj.getName() + " to " + mij2.getMod().getName() + " " + mij2.getPrefix());
						}
						break outer;
					} else {
						targetJacks.put(mij2.getPrefix(), mij2);
					}
				}
			}
		}
	}

	/**
	 * Implement single-dest-jack matrix mods, and then match all parms
	 * for each module.
	 */
	public boolean finalMatchSingleDestModsAndParms(ArrayList trialMatch, HashMap finalMatch,
			HashMap alreadyMatchedList, ArrayList errorList) throws PatchDefinitionException {
		Module mod, mod2, ourMod;
		ModuleParm mp, ourMp;
		ModuleOutputJack moj, ourMoj = null;
		ModuleInputJack mij, ourMij;
		MatrixMod mm;
		ModMatchData md, md2, mdNew;
		Connection conn;
		ArrayList a;
		Object o;
		int i, j, k, indx;
		HashMap alreadyMatched;
		boolean found;

		found = true;
		for (i = 0; i < inputGP.getModules().size() && found; i++) {
			mod = (Module)inputGP.getModules().get(i);
			alreadyMatched = (HashMap)alreadyMatchedList.get(mod);
			mdNew = (ModMatchData)finalMatch.get(mod);

			// If hard-wired jack match not found, try matrix mod
			for (j = 0; j < mod.getInputJacks().size(); j++) {
				mij = (ModuleInputJack)mod.getInputJacks().get(j);
				o = mdNew.getInputJackMatches().get(j);
				// If already matched, go to the next
				if (o.getClass() == ModuleInputJack.class) {
					continue;
				}
				a = (ArrayList)o;
				// Find source module which is connected to input patch's input jack
				conn = inputGP.findConnectionToTarget(mij);
				mod2 = conn.getSourceJack().getMod();
				indx = inputGP.getModules().indexOf(mod2);
				md2 = (ModMatchData)trialMatch.get(indx);
				ourMod = md2.getMod(); // get our matching source module
				for (k = 0; k < matrixMods.size(); k++) {
					mm = (MatrixMod)matrixMods.get(k);
					ourMij = mm.createSingleDestMod(ourMod.getName(), conn.getSourceJack().getName(),
							mdNew.getMod().getName(), mij.getPrefix());
					if (ourMij != null) {
						mdNew.getInputJackMatches().set(j, ourMij);
						alreadyMatched.put(ourMij, null);
						// If there's an attenuator, match it to our attenuator
						mp = mij.getAttenuator();
						if (mp != null) {
							ourMp = ourMij.getAttenuator();
							indx = mod.getParms().indexOf(mp);
							mdNew.getParmMatches().set(indx, ourMp);
						}
						break;
					}
				}
				if (k >= matrixMods.size()) {
					found = false;
					errorList.add("finalModuleMatch could not final match input jack " + mij.getName());
					break;
				}
			}

			// Match parms
			for (j = 0; j < mod.getParms().size(); j++) {
				mp = (ModuleParm)mod.getParms().get(j);
				o = mdNew.getParmMatches().get(j);
				// Attenuators were already matched during input jack matching
				if (o.getClass() == ModuleParm.class) {
					continue;
				}
				a = (ArrayList)o;
				// FIXME matching to same name OK?  All numbered parms matched
				// above as attenuators (?)
				for (k = 0; k < a.size(); k++) {
					ourMp = (ModuleParm)a.get(k);
					if (ourMp.getName().equalsIgnoreCase(mp.getName())) {
						mdNew.getParmMatches().set(j, ourMp);
						break;
					}
				}
				if (k >= a.size()) {
					found = false;
					errorList.add("finalModuleMatch could not final match parm " + mp.getName());
					break;
				}
			}

			if (found) {
				finalMatch.put(mod, mdNew);
			} else {
				errorList.add("finalModuleMatch test code failed to final match " + mod.getName());
			}
		}

		return found;
	}

	public class ModulePermutationIterator implements Iterator {
		private ArrayList trialMatchChoices;
		private int multipleChoices[];
		private int numChoices;
		private int curChoice[];
		private int indx;
		private ArrayList trialMatch;

		public ModulePermutationIterator(GenericPatch inputGP, HashMap modMatchLists) {
			Module mod, ourMod;
			ArrayList modMatches;
			ModMatchData md;
			int i, j;

			// Build list of possible choices.  For input modules which only have
			// a single matching module, store that match; for modules with two
			// or more possible matches, store the list of matches.
			trialMatchChoices = new ArrayList();
			multipleChoices = new int[inputGP.getModules().size()];
			for (i = 0, numChoices = 0; i < inputGP.getModules().size(); i++) {
				mod = (Module)inputGP.getModules().get(i);
				modMatches = (ArrayList)modMatchLists.get(mod);
				if (modMatches.size() == 1) {
					trialMatchChoices.add(modMatches.get(0));
				} else {
					trialMatchChoices.add(modMatches);
					multipleChoices[numChoices++] = i;
				}
			}
			if (numChoices > 0) {
				curChoice = new int[numChoices];
				curChoice[0] = -1;
			}
			indx = 0;
			nextTrialMatch();
		}

		public boolean hasNext() {
			if (trialMatch.size() > 0) {
				return true;
			} else {
				return false;
			}
		}

		public Object next() {
			if (trialMatch.size() > 0) {
				ArrayList a = trialMatch;
				nextTrialMatch();
				return a;
			} else {
				throw new NoSuchElementException();
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		private void nextTrialMatch() {
			// Step thru combinations, returning next usable permutation
			ModMatchData md;
			Module mod;
			HashMap modsChosen;
			Object o;
			int i, j;
			modsChosen = new HashMap();
			trialMatch = new ArrayList();
			// Build list of modules for which there is only one possible match
			// (no need to iterate through a list for them).
			for (i = 0; i < trialMatchChoices.size(); i++) {
				o = trialMatchChoices.get(i);
				if (o.getClass() == ModMatchData.class) {
					md = (ModMatchData)o;
					if (modsChosen.containsKey(md.getMod())) {
						// If one of our modules has been matched to more than
						// one input module, this is not a valid match
						System.out.println("Warning: attempted to match our module " +
								md.getMod().getName() + " to two different input modules at once");
						trialMatch = new ArrayList();
						return;
					}
					modsChosen.put(md.getMod(), null);
					trialMatch.add(o);
				} else {
					trialMatch.add(null); // placeholder value
				}
			}
			// If there's only one possible match for all modules, return that
			// match the first time and then indicate there are no more permutations
			if (numChoices == 0) {
				if (indx == 0) {
					indx = 1;
				} else {
					trialMatch = new ArrayList();
				}
				return;
			}
			outer: while (true) {
				modsChosen = new HashMap();
				for (i = 0; i < indx; i++) {
					md = (ModMatchData)((ArrayList)trialMatchChoices.get(multipleChoices[i])).get(curChoice[i]);
					modsChosen.put(md.getMod(), null);
				}
				for (i = 0; i < trialMatch.size(); i++) {
					o = trialMatch.get(i);
					if (o != null) {
						modsChosen.put(((ModMatchData)o).getMod(), null);
					}
				}
				inner: while (true) {
					curChoice[indx]++;
					if (curChoice[indx] >=
							((ArrayList)trialMatchChoices.get(multipleChoices[indx])).size()) {
						if (indx == 0) {
							trialMatch = new ArrayList();
							return;
						} else {
							indx--;
							continue outer;
						}
					}
					md = (ModMatchData)((ArrayList)trialMatchChoices.get(multipleChoices[indx])).get(curChoice[indx]);
					mod = md.getMod();
					if (modsChosen.containsKey(mod)) {
						continue inner;
					}
					modsChosen.put(mod, null);
					if (indx == numChoices - 1) {
						for (i = 0; i < numChoices; i++) {
							trialMatch.set(multipleChoices[i],
									((ArrayList)trialMatchChoices.get(multipleChoices[i])).get(curChoice[i]));
						}
						return;
					}
					curChoice[++indx] = -1;
				}
			}
		}
	}

	/**
	 * Find all output jacks which are connected to this module's input jacks (find
	 * sources of control or audio input).
	 * 
	 * @param mj
	 */
	void findJacksAndModulesUsed(ModuleJack mj) throws PatchDefinitionException {
		Module mod;
		ModuleParm mp;
		ModuleOutputJack mjSource;
		ModuleInputJack mjTarget;
		Connection conn;

		if (mj == null) {
			return;
		}
		mj.setUsed(true);
		mod = mj.getMod();
		if (mod.getUsed() > 0) {
			return; // module has been checked; don't do again, to avoid endless cycle
		}
		mod.seeIfUsed();
		mod.seeIfParmsUsed();
		for (int i = 0; i < mod.getInputJacks().size(); i++) {
			mjTarget = (ModuleInputJack)mod.getInputJacks().get(i);
			mp = mjTarget.getAttenuator();
			// If attenuator was marked as unused, jack is too
			if (mp != null && mp.isUsed() == false) {
				continue;
			}
			// See if this input jack has an attenuator whose value is zero
			if (mp != null && mp.getValue().equals("0") == true &&
					(mp.getMorph() == null || mp.getMorph().getValue().equals("0"))) {
				// jack and attenuator aren't being used
				mp.setUsed(false);
				continue;
			}
			conn = mjTarget.getConn();
			if (conn == null) {
				continue;
			}
			// FIXME should do better check
			mjTarget.setUsed(true);
			mjSource = conn.getSourceJack();
			if (mjSource == null) {
				throw new PatchDefinitionException("Connection source is null");
			} else {
				findJacksAndModulesUsed(findModuleOutputJack(mjSource.getMod().getName(),
						mjSource.getName()));
			}
		}
	}

	public Module findModule(String name) {
		int i;
		Module m;

		// with few modules, a sequential search is fast enough
		for (i = 0; i < modules.size(); i++) {
			m = (Module)modules.get(i);
			if (m.getName().equalsIgnoreCase(name)) {
				return m;
			}
		}
		return null;
	}

	public int findModuleIndex(String name) {
		int i;
		Module m;

		// with few modules, a sequential search is fast enough
		for (i = 0; i < modules.size(); i++) {
			m = (Module)modules.get(i);
			if (m.getName().equalsIgnoreCase(name)) {
				return i;
			}
		}
		return -1;
	}

	public ModuleInputJack findModuleInputJack(String name, String jack) {
		Module mod;

		mod = findModule(name);
		if (mod == null) {
			return null;
		}
		return mod.findInputJack(jack);
	}

	public ModuleOutputJack findModuleOutputJack(String name, String jack) {
		Module mod;

		mod = findModule(name);
		if (mod == null) {
			return null;
		}
		return mod.findOutputJack(jack);
	}

	public ModuleParm findModuleParm(String name, String parm) {
		Module mod;
		ModuleParm mp;

		mod = findModule(name);
		if (mod == null) {
			return null;
		}
		return mod.findParm(parm);
	}

	public Connection findConnection(String sourceModName, String sourceMjName,
			String targetModName, String targetMjName) {
		int i;
		ModuleOutputJack sourceMj;
		ModuleInputJack targetMj;
		Connection conn;

		sourceMj = findModuleOutputJack(sourceModName, sourceMjName);
		targetMj = findModuleInputJack(targetModName, targetMjName);
		conn = targetMj.getConn();
		if (conn != null && conn.getSourceJack() == sourceMj) {
			return conn;
		}
		return null;
	}

	// FIXME maybe use ModuleInputJack.getConn() directly?
	public Connection findConnectionToTarget(ModuleInputJack targetMj) {
		return targetMj.getConn();
	}

	// FIXME maybe use ModuleOutputJack.getFirstConn() directly?
	public Connection findConnectionFromSource(ModuleOutputJack sourceMj) {
		return sourceMj.getFirstConn();
	}

	public boolean findConnectionToSameType(String sourceModName, String sourceMjName,
			String modType, String jackType) {
		int i;
		ModuleOutputJack sourceMj;
		Connection conn;
		MatrixMod mm;

		sourceMj = findModuleOutputJack(sourceModName, sourceMjName);
		if (sourceMj.getConn() != null) {
			for (i = 0; i < sourceMj.getConn().length; i++) {
				conn = sourceMj.getConn()[i];
//				if (conn == null) {
//					continue;
//				}
				if (conn.getSourceJack() == sourceMj &&
						conn.getTargetJack().getMod().getType().equalsIgnoreCase(modType) &&
						conn.getTargetJack().getPrefix().equalsIgnoreCase(jackType)) {
					return true;
				}
			}
		}
		// If there's no hard-wired connection, maybe matrix mod can be used?
		for (i = 0; i < matrixMods.size(); i++) {
			mm = (MatrixMod)matrixMods.get(i);
			if (mm.seeIfModTypeIsAllowed(sourceMj.getMod().getType(),
					sourceMj.getPrefix(), modType, jackType)) {
				return true;
			}
		}
		return false;
	}

	public boolean findConnectionFromSameType(String modType, String jackType,
			String targetModName, String targetMjName) {
		int i;
		ModuleInputJack targetMj;
		Connection conn;
		MatrixMod mm;

		targetMj = findModuleInputJack(targetModName, targetMjName);
		conn = targetMj.getConn();
		if (conn != null &&
				conn.getSourceJack().getMod().getType().equalsIgnoreCase(modType) &&
				conn.getSourceJack().getPrefix().equalsIgnoreCase(jackType)) {
			return true;
		}
		// If there's no hard-wired connection, maybe matrix mod can be used?
		for (i = 0; i < matrixMods.size(); i++) {
			mm = (MatrixMod)matrixMods.get(i);
			if (mm.seeIfModTypeIsAllowed(modType, jackType,
					targetMj.getMod().getType(), targetMj.getPrefix())) {
				return true;
			}
		}
		return false;
	}

	public void updateGroupSources(MatrixMod pMm, int sourceIndex) {
		MatrixMod mm;
		int i, grp;

		if (pMm.getSourceGroup() == 0) {
			return;
		}
		for (i = 0; i < matrixMods.size(); i++) {
			mm = (MatrixMod)matrixMods.get(i);
			if (mm == pMm) {
				continue;
			}
			grp = mm.getSourceGroup();
			if (grp == pMm.getSourceGroup()) {
				mm.setSourceIndex(sourceIndex);
			}
		}
	}

	public Iterator moduleIterator() {
		return new ModuleIterator();
	}

	private class ModuleIterator implements Iterator {
		private int i;

		public ModuleIterator() {
			i = 0;
		}

		public boolean hasNext() {
			if (i < modules.size()) {
				return true;
			} else {
				return false;
			}
		}

		public Object next() {
			if (i < modules.size()) {
				return modules.get(i++);
			} else {
				throw new NoSuchElementException();
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public ModuleTypeIterator moduleTypeIterator() {
		return new ModuleTypeIterator();
	}

	class ModuleTypeIterator implements Iterator {
		private ArrayList modTypes;
		private ArrayList modLists;
		private int indxType;
		private ArrayList curModList;
		private int indxMod;

		public ModuleTypeIterator() {
			int j;
			String s;
			Module mod;
			ArrayList a;
			modTypes = new ArrayList();
			modLists = new ArrayList();
			Iterator e = moduleIterator();
			while (e.hasNext()) {
				mod = (Module)e.next();
				s = mod.getType();
				j = modTypes.indexOf(s);
				if (j != -1) {
					a = (ArrayList)modLists.get(j);
				} else {
					modTypes.add(s);
					a = new ArrayList();
					modLists.add(a);
				}
				a.add(mod);
			}
			indxType = 0;
		}

		public boolean hasNext() {
			return indxType < modTypes.size();
		}

		public Object next() {
			curModList = (ArrayList)modLists.get(indxType);
			return modTypes.get(indxType++);
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		public String nextType() {
			return (String)next();
		}

		public void setToFirstMod() {
			indxMod = 0;
		}

		public Module nextMod() {
			if (indxMod < curModList.size()) {
				return (Module)curModList.get(indxMod++);
			} else {
				return null;
			}
		}
	}

	public Iterator connectionTargetIterator(ModuleOutputJack sourceMj) {
		return new ConnectionTargetIterator(sourceMj);
	}

	private class ConnectionTargetIterator implements Iterator {
//		private ModuleOutputJack sourceMj;
		private Connection[] conn;
		private int i;

		public ConnectionTargetIterator(ModuleOutputJack mj) {
//			sourceMj = mj;
			i = 0;
			conn = mj.getConn();
//			getNext();
		}

		public boolean hasNext() {
//			if (i < connections.size()) {
			if (conn != null && i < conn.length) {
				return true;
			} else {
				return false;
			}
		}

		public Object next() {
//			if (i < connections.size()) {
//				Connection conn = (Connection)connections.get(i++);
//				getNext();
//				return conn;
			if (i < conn.length) {
				return conn[i++];
			} else {
				throw new NoSuchElementException();
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

//		private void getNext() {
//			Connection conn;
//			while (i < connections.size()) {
//				conn = (Connection)connections.get(i);
//				if (conn.getSourceJack() == sourceMj) {
//					return;
//				}
//				i++;
//			}
//		}
	}
}

class ModMatchData {
	private Module mod;
	private ArrayList parmMatches;
	private ArrayList inputJackMatches;
	private ArrayList outputJackMatches;

	ModMatchData(Module m, ArrayList p, ArrayList ij, ArrayList oj) {
		mod = m;
		parmMatches = p;
		inputJackMatches = ij;
		outputJackMatches = oj;
	}

	public Module getMod() {
		return mod;
	}

	public ArrayList getParmMatches() {
		return parmMatches;
	}

	public ArrayList getInputJackMatches() {
		return inputJackMatches;
	}

	public ArrayList getOutputJackMatches() {
		return outputJackMatches;
	}
}
