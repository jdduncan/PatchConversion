
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
 * Describes a synth module (oscillator, filter, etc)
 *
 * @author Kenneth L. Martinez
 */

import java.util.*;

public class Module implements Cloneable {
	public static final String MODULE_USED[] = { "not_checked", "unused",
			"possible_modulator", "required" };
	private String name;
	private String type;
	private int number;
	private int used;
	private GenericPatch gp;
//	private boolean checked;
	private ArrayList parms; // fixed and variable parms
	private ArrayList inputJacks;
	private ArrayList outputJacks;

	Module(String pName, String pType, int pNumber) {
		name = pName;
		type = pType;
		number = pNumber;
		parms = new ArrayList();
		inputJacks = new ArrayList();
		outputJacks = new ArrayList();
	}

	Module(String xml) throws PatchDefinitionException {
		ModuleJack mj;
		parms = new ArrayList();
		inputJacks = new ArrayList();
		outputJacks = new ArrayList();
		XMLReader xr = new XMLReader(xml);
		String tag[];
		while ((tag = xr.getNextTag()) != null) {
			if (tag[0].equalsIgnoreCase("name")) {
				name = tag[1];
			} else if (tag[0].equalsIgnoreCase("type")) {
				type = tag[1];
			} else if (tag[0].equalsIgnoreCase("number")) {
				if (tag[1] == null) {
					number = 0;
				} else {
					number = new Integer(tag[1]).intValue();
				}
			} else if (tag[0].equalsIgnoreCase("parm")) {
				parms.add(new ModuleParm(tag[1]));
			} else if (tag[0].equalsIgnoreCase("input_jack")) {
				addInputJack(new ModuleInputJack(tag[1], this));
			} else if (tag[0].equalsIgnoreCase("output_jack")) {
				addOutputJack(new ModuleOutputJack(tag[1], this));
			}
		}
	}

	/**
	 * Creates deep copy of module.  Cloned jacks won't have any connections.
	 */
	public Object clone() {
		Module mod = new Module(getName(), getType(), getNumber());
		ModuleParm mp;
		ModuleInputJack mij, mij2;
		ModuleOutputJack moj;
		int i;
		for (i = 0; i < parms.size(); i++) {
			mod.addParm( (ModuleParm) ((ModuleParm)parms.get(i)).clone() );
		}
		for (i = 0; i < inputJacks.size(); i++) {
			mij = (ModuleInputJack)inputJacks.get(i);
			mij2 = new ModuleInputJack(mij.getName(), mij.getType());
			mp = mij.getAttenuator();
			if (mp == null) {
				mod.addInputJack(mij2);
			} else {
				mod.addInputJack(mij2, mod.findParm(mp.getName()));
			}
		}
		for (i = 0; i < outputJacks.size(); i++) {
			moj = (ModuleOutputJack)outputJacks.get(i);
			mod.addOutputJack(new ModuleOutputJack(moj.getName(), moj.getType(),
					moj.getPolarity()));
		}
		return mod;
	}

	public void setGp(GenericPatch pGp) {
		gp = pGp;
	}

	public GenericPatch getGp() {
		return gp;
	}

	public void initialize() {
//		checked = false;
		for (int i = 0; i < parms.size(); i++) {
			((ModuleParm)parms.get(i)).initialize();
		}
	}

	public void addParm(ModuleParm mp) {
		parms.add(mp);
		mp.setMod(this);
	}

	public void removeParm(ModuleParm mp) {
		parms.remove(mp);
		mp.setMod(null);
	}

	public void addInputJack(ModuleInputJack mj) {
		inputJacks.add(mj);
		mj.setMod(this);
	}

	public void addInputJack(ModuleInputJack mj, ModuleParm mp) {
		addInputJack(mj);
		mj.setAttenuator(mp);
		mp.setAttenuatedJack(mj);
	}

	public void addInputJackAndRenumber(ModuleInputJack mj) {
		String prefix;
		int i, num = 0;
		Module mod;
		ModuleInputJack mij;

		prefix = mj.getPrefix();
		for (i = 0; i < inputJacks.size(); i++) {
			mij = (ModuleInputJack)inputJacks.get(i);
			if (mij.getPrefix().equalsIgnoreCase(prefix) &&
					num < mij.getNumber()) {
				num = mij.getNumber();
			}
		}
		mj.setNumber(num + 1);
		addInputJack(mj);
	}

	public void addInputJackAndRenumber(ModuleInputJack mj, ModuleParm mp) {
		addInputJackAndRenumber(mj);
		mp.setNumber(mj.getNumber());
		mj.setAttenuator(mp);
		mp.setAttenuatedJack(mj);
	}

	public void removeInputJack(ModuleInputJack mj) {
		inputJacks.remove(mj);
		mj.setMod(null);
	}

	public void addOutputJack(ModuleOutputJack mj) {
		outputJacks.add(mj);
		mj.setMod(this);
	}

	public void removeOutputJack(ModuleOutputJack mj) {
		outputJacks.remove(mj);
		mj.setMod(null);
	}

	public String getName() {
		return name;
	}

	void setName(String s) {
		name = s;
	}

	public String getType() {
		return type;
	}

	void setType(String s) {
		type = s;
	}

	public int getNumber() {
		return number;
	}

	void setNumber(int i) {
		number = i;
	}

	public int getUsed() {
		return used;
	}

	public void setUsed(int i) {
		used = i;
	}

	public void seeIfUsed() {
		// FIXME need to perform process to see if module is either
		// in audio path or is a modulator in use (required), or if it
		// could be triggered as a modulator via some controller
		// (possible_modulator)
		setUsed(3);
	}

	public void seeIfParmsUsed() {
		// FIXME should do better check
		ModuleParm mp;
		ModuleInputJack mij;
		for (int i = 0; i < parms.size(); i++) {
			mp = (ModuleParm)parms.get(i);
			mij = mp.getAttenuatedJack();
			if (mij != null && mp.getValue().equals("0") &&
					(mp.getMorph() == null || mp.getMorph().getValue().equals("0"))) {
				mp.setUsed(false);
			} else {
				mp.setUsed(true);
			}
		}
	}

	public ArrayList getParms() {
		return parms;
	}

	public ArrayList getInputJacks() {
		return inputJacks;
	}

	public ArrayList getOutputJacks() {
		return outputJacks;
	}

	public ModuleInputJack findInputJack(String jack) {
		ModuleInputJack mij;

		// with few jacks, a sequential search is fast enough
		for (int i = 0; i < inputJacks.size(); i++) {
			mij = (ModuleInputJack)inputJacks.get(i);
			if (mij.getName().equalsIgnoreCase(jack)) {
				return mij;
			}
		}
		return null;
	}

	public ModuleOutputJack findOutputJack(String jack) {
		ModuleOutputJack moj;

		// with few jacks, a sequential search is fast enough
		for (int i = 0; i < outputJacks.size(); i++) {
			moj = (ModuleOutputJack)outputJacks.get(i);
			if (moj.getName().equalsIgnoreCase(jack)) {
				return moj;
			}
		}
		return null;
	}

	public ModuleParm findParm(String parm) {
		ModuleParm mp;

		// with few parms, a sequential search is fast enough
		for (int i = 0; i < parms.size(); i++) {
			mp = (ModuleParm)parms.get(i);
			if (mp.getName().equalsIgnoreCase(parm)) {
				return mp;
			}
		}
		return null;
	}

//	public boolean isChecked() {
//		return checked;
//	}
//
//	public void setChecked(boolean b) {
//		checked = b;
//	}

	public String writeXML() throws PatchDefinitionException {
		// FIXME later change so possible_modulator triggers print
		if (used < 3) {
			return "";
		}
		StringBuffer sb = new StringBuffer("<module>");
		sb.append("<name>" + name + "</name>");
		sb.append("<type>" + type + "</type>");
		if (number != 0) {
			sb.append("<number>" + number + "</number>");
		}
		sb.append("<used>" + MODULE_USED[used] + "</used>");
		sb.append(writeValue());
		sb.append("</module>");
		return sb.toString();
	}

	/**
	 * Write XML for all module parms and jacks
	 */
	String writeValue() throws PatchDefinitionException {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < parms.size(); i++) {
			sb.append(((ModuleParm)parms.get(i)).writeXML());
		}
		for (int i = 0; i < inputJacks.size(); i++) {
			sb.append(((ModuleInputJack)inputJacks.get(i)).writeXML());
		}
		for (int i = 0; i < outputJacks.size(); i++) {
			sb.append(((ModuleOutputJack)outputJacks.get(i)).writeXML());
		}
		return sb.toString();
	}
}
