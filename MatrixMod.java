
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
 * Describes a matrix modulation, which can create one or more connections.
 * It has one or more allowable sources (only one allowed at a time) which
 * affects one of multiple destinations.  It may also add a jack and its
 * attenuator parm to the destination module.
 * 
 * Several matrix mods may be part of a Source Group, which means that all
 * of them must have the same source selected.  An example of its use is
 * for the Access Virus' second Modulation Matrix, which allows one source
 * to affect two independent destinations; this can be modeled by two
 * matrix mods in one source group.
 *
 * @author Kenneth L. Martinez
 */

interface MatrixMod extends ParmTranslator{
	public void setGp(GenericPatch pGp);
	public GenericPatch getGp();
	public boolean seeIfModTypeIsAllowed(String sourceModType, String sourceModJack,
			String destModType, String destModJackPrefix);
	public ModuleInputJack createSingleDestMod(String sourceMod, String sourceJack, String destMod,
			String destJackPrefix) throws PatchDefinitionException;
	public ModuleInputJack[] createMultiDestMod(String sourceMod, String sourceJack, String destMod[],
			String destJackPrefix) throws PatchDefinitionException;
	public void removeCurrentMod();
	public boolean seeIfJackCanBeAdded(String sourceModType, String sourceJack,
			String destMod, String destModJackPrefix);
	public boolean seeIfParmCanBeAdded(String parmPrefix);
	public int getSourceGroup();
	public void setSourceIndex(int pSourceIndex);
}
