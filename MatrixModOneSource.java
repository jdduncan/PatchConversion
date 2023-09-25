
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
 * Describes a matrix modulation.  This can affect one of several destinations,
 * even multiple input jacks per destination (all of the same type), choosing
 * from one of several sources.
 *
 * @author Kenneth L. Martinez
 */

//import java.util.*;

public class MatrixModOneSource implements MatrixMod {
	private SynthParmTable sourceParm;
	private SynthParmTable destParm;
	private int sourceParmMap[];
	private int destParmMap[];
	private GenericPatch gp;
	private ModuleOutputJack sourceJacks[];
	private ModuleInputJack destJacks[][];
	private ParmTranslator parmTranslators[][];
	private ParmTranslator morphTranslators[][];
	private int sourceIndex;
	private int destIndex;
	private int sourceGroup;

	MatrixModOneSource(SynthParmTable pSourceParm, int pSourceParmMap[],
			SynthParmTable pDestParm, int pDestParmMap[], ModuleOutputJack pSourceJacks[],
			ModuleInputJack pDestJacks[][], ParmTranslator pParmTranslators[][],
			ParmTranslator pMorphTranslators[][]) throws PatchDefinitionException {
		sourceJacks = pSourceJacks;
		destJacks = pDestJacks;
		parmTranslators = pParmTranslators;
		morphTranslators = pMorphTranslators;
		sourceParm = pSourceParm;
		destParm = pDestParm;
		sourceParmMap = pSourceParmMap;
		destParmMap = pDestParmMap;
		sourceIndex = -1;
		destIndex = -1;
		if ((parmTranslators != null && destJacks.length != parmTranslators.length) ||
				(morphTranslators != null && destJacks.length != morphTranslators.length)) {
			throw new PatchDefinitionException("Mods, jacks and parms must be same size for dest");
		}
	}

	// Simplified, to connect one input jack to one of several output jacks
	MatrixModOneSource(SynthParmTable pSourceParm, int pSourceParmMap[],
			ModuleOutputJack pSourceJacks[], ModuleInputJack pDestJacks[][]) throws PatchDefinitionException {
		this(pSourceParm, pSourceParmMap, null, new int[] { -1 },
				pSourceJacks, pDestJacks, null, null);
	}

	public void setGp(GenericPatch pGp) {
		gp = pGp;
	}

	public GenericPatch getGp() {
		return gp;
	}

	/**
	 * While trying to implement an input generic patch for a specific synth,
	 * the input process will try to see if all connections and jacks can be
	 * implemented by the target synth using hard-wired modulations.  If it
	 * cannot, it will use this routine to see if the target synth has a
	 * matrix mod feature which can be used to create this modulation type.
	 * Note that this routine only sees if matrix mod can work with the given
	 * type of input and output module; other processing will determine if
	 * there are unused matrix mods available and if they can connect to
	 * the specific modules requested.
	 */
	public boolean seeIfModTypeIsAllowed(String sourceModType, String sourceModJack,
			String destModType, String destModJackPrefix) {
		int i;
		for (i = 0; i < sourceJacks.length; i++) {
			if (sourceJacks[i].getMod().getType().equalsIgnoreCase(sourceModType) &&
					sourceJacks[i].getName().equalsIgnoreCase(sourceModJack)) {
				break;
			}
		}
	 	if (i >= sourceJacks.length) {
			return false;
		}
		for (i = 0; i < destJacks.length; i++) {
			if (destJacks[i][0].getMod().getType().equalsIgnoreCase(destModType) &&
					destJacks[i][0].getPrefix().equalsIgnoreCase(destModJackPrefix)) {
				break;
			}
		}
		if (i >= destJacks.length) {
			return false;
		}
		return true;
	}

	/**
	 * If this matrix mod is unused and it can implement the requested mod
	 * connection, set source & dest indexes and create connection
	 */
	public ModuleInputJack createSingleDestMod(String sourceMod, String sourceJack, String destMod,
			String destJackPrefix) throws PatchDefinitionException {
		ModuleInputJack mij = null;
		int i;  // , j;

		if (destIndex != -1) {
			return null;
		}
		for (i = 0; i < sourceJacks.length; i++) {
			if (sourceJacks[i].getMod().getName().equalsIgnoreCase(sourceMod) &&
					sourceJacks[i].getName().equalsIgnoreCase(sourceJack)) {
				sourceIndex = i;
				break;
			}
		}
		if (i >= sourceJacks.length) {
			return null;
		}
		for (i = 0; i < destJacks.length; i++) {
			if (destJacks[i].length > 1) {
				continue; // only looking for single-dest mods
			}
			if (destJacks[i][0].getMod().getName().equalsIgnoreCase(destMod) &&
					destJacks[i][0].getPrefix().equalsIgnoreCase(destJackPrefix)) {
				destIndex = i;
				mij = destJacks[i][0];
				break;
			}
		}
		if (i >= destJacks.length) {
			sourceIndex = -1;
			return null;
		}
		createModRoute();
		return mij;
	}

	/**
	 * If this matrix mod is unused and it can implement the requested mod
	 * connections, set source & dest indexes and create connections
	 */
	public ModuleInputJack[] createMultiDestMod(String sourceMod, String sourceJack, String destMod[],
			String destJackPrefix) throws PatchDefinitionException {
		ModuleInputJack mij[] = new ModuleInputJack[destMod.length];
		int i, j, k;

		if (destIndex != -1) {
			return null;
		}
		for (i = 0; i < sourceJacks.length; i++) {
			if (sourceJacks[i].getMod().getName().equalsIgnoreCase(sourceMod) &&
					sourceJacks[i].getName().equalsIgnoreCase(sourceJack)) {
				sourceIndex = i;
				break;
			}
		}
		if (i >= sourceJacks.length) {
			return null;
		}
		outer: for (i = 0; i < destJacks.length; i++) {
			if (destJacks[i].length != mij.length) {
				continue; 
			}
			for (j = 0; j < mij.length; j++) {
				for (k = 0; k < destJacks[i].length; k++) {
					if (destJacks[i][k].getMod().getName().equalsIgnoreCase(destMod[j]) &&
							destJacks[i][k].getPrefix().equalsIgnoreCase(destJackPrefix)) {
						mij[j] = destJacks[i][k];
						break;
					}
				}
				if (k >= destJacks[i].length) {
					continue outer;
				}
			}
			destIndex = i;
			break;
		}
		if (i >= destJacks.length) {
			sourceIndex = -1;
			return null;
		}
		createModRoute();
		return mij;
	}

	/**
	 * If matrix mod is in use, delete connection, jack(s) and attentuator(s)
	 * created.  This will be invoked if a trial module match fails and
	 * another must be attempted.
	 *
	 */
	public void removeCurrentMod() {
		if (sourceIndex != -1 && destIndex != -1) {
			for (int i = 0; i < destJacks[destIndex].length; i++) {
				// delete connection(s)
				gp.removeConnection(sourceJacks[sourceIndex].getMod().getName(),
						sourceJacks[sourceIndex].getName(),
						destJacks[destIndex][i].getMod().getName(),
						destJacks[destIndex][i].getName());
			}
		}
		sourceIndex = -1;
		destIndex = -1;
	}

	private void createModRoute() throws PatchDefinitionException {
		ModuleInputJack mij;
		ModuleParm mp;
		int i;

		for (i = 0; i < destJacks[destIndex].length; i++) {
			mij = destJacks[destIndex][i];
			// create connection from source to dest jack
			gp.addConnection(new Connection(sourceJacks[sourceIndex], mij));
		}
	}

	public void toGeneric() throws PatchDefinitionException {
		ModuleJack mj;
		ModuleParm mp;
		int i;

		if (destIndex == -1) {
			if (sourceParm == null) { // If there's no parm, there's only one source
				sourceIndex = 0;
			} else {
				sourceIndex = sourceParmMap[sourceParm.getIntValue()];
				if (sourceIndex == -1) {
					return;
				}
			}
			if (destParm == null) { // If there's no parm, there's only one dest
				destIndex = 0;
			} else {
				destIndex = destParmMap[destParm.getIntValue()];
			}
			if (destIndex == -1) {
				return;
			}
			createModRoute();
		}

		for (i = 0; i < destJacks[destIndex].length; i++) {
			mj = destJacks[destIndex][i];
			if (parmTranslators != null && parmTranslators[destIndex][i] != null) {
				// set attenuator value
				parmTranslators[destIndex][i].toGeneric();
			}
			if (morphTranslators != null && morphTranslators[destIndex][i] != null) {
				morphTranslators[destIndex][i].toGeneric();
			}
		}
	}

	public void fromGeneric() {
		int i;
		if (destIndex == -1) {
			return;
		}
		if (sourceParm != null) {
			sourceParm.setValue(sourceIndex);
			for (i = 0; i < sourceParmMap.length; i++) {
				if (sourceIndex == sourceParmMap[i]) {
					sourceParm.setValue(i);
					break;
				}
			}
			if (i >= sourceParmMap.length) {
				System.out.println("Error: MatrixMod could not match source index " + sourceIndex);
				return;
			}
		}
		if (destParm != null) {
			for (i = 0; i < destParmMap.length; i++) {
				if (destIndex == destParmMap[i]) {
					destParm.setValue(i);
					break;
				}
			}
			if (i >= destParmMap.length) {
				System.out.println("Error: MatrixMod could not match dest index " + destIndex);
				return;
			}
		}
		if (parmTranslators == null || parmTranslators[destIndex][0] == null) {
			return; // If no parm, should be no morph either
		}
		parmTranslators[destIndex][0].fromGeneric();
		// If more than one parm, see if all have same value
		for (i = 1; i < destJacks[destIndex].length; i++) {
			if (destJacks[destIndex][i].getAttenuator().getValue().equalsIgnoreCase(
					destJacks[destIndex][0].getAttenuator().getValue()) == false) {
				System.out.println("Warning - value " + destJacks[destIndex][i].getAttenuator().getValue() +
						" not equal to first value " + destJacks[destIndex][0].getAttenuator().getValue() +
						"; first value used");
			}
		}
		if (morphTranslators == null || morphTranslators[destIndex][0] == null) {
			return;
		}
		morphTranslators[destIndex][0].fromGeneric();
		// If more than one parm morph, see if all have same value
		for (i = 1; i < destJacks[destIndex].length; i++) {
			ParmMorph morph = destJacks[destIndex][i].getAttenuator().getMorph();
			if (morph != null) { // used for morph translators
				if (destJacks[destIndex][i].getAttenuator().getMorph().getValue().equalsIgnoreCase(
						destJacks[destIndex][0].getAttenuator().getMorph().getValue()) == false) {
					System.out.println("Warning - value " + destJacks[destIndex][i].getAttenuator().getMorph().getValue() +
							" not equal to first value " + destJacks[destIndex][0].getAttenuator().getMorph().getValue() +
							"; first value used");
				}
			} else { // used for regular parm translators
				if (destJacks[destIndex][i].getAttenuator().getValue().equalsIgnoreCase(
						destJacks[destIndex][0].getAttenuator().getValue()) == false) {
					System.out.println("Warning - value " + destJacks[destIndex][i].getAttenuator().getValue() +
							" not equal to first value " + destJacks[destIndex][0].getAttenuator().getValue() +
							"; first value used");
				}
			}
		}
	}

	public boolean seeIfJackCanBeAdded(String sourceModType, String sourceJack,
			String destMod, String destModJackPrefix) {
		return false;
	}

	public boolean seeIfParmCanBeAdded(String parmPrefix) {
		return false;
	}

	public int getSourceGroup() {
		return sourceGroup;
	}

	public void setSourceIndex(int pSourceIndex) {
		System.out.println("MatrixModOneSource can't use sourceGroups");
		int i = 1 / 0; // abort
	}
}
