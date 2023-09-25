
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
 * Implements several common features required by SynthPatch interface
 *
 * @author Kenneth L. Martinez
 */

//import java.io.*;
import java.util.*;

public abstract class SynthPatchAbstract implements SynthPatch {
	protected String manufacturerName;
	protected String itemName;
	protected String invalidMsg;

	protected int programType; // 0=stored, 1=edit buffer
	protected ArrayList sourceTags; // XML tags which make up the source patch
	protected ArrayList sourceTagsEdit; // XML tags for edit buffer source patch
	protected ArrayList pgmParms; // program parameters
	protected ArrayList hdrParms; // global parameters (patch number, etc)
	protected ArrayList hdrParmsEdit; // global parameters for edit buffer
	protected GenericPatch genPatch;
	protected ArrayList parmTranslators;
	protected ArrayList parmLinks;

	protected byte sysex[]; // program sysex data
	protected byte initSysex[]; // init stored program sysex
	protected byte initSysexEdit[]; // init edit buffer sysex

	SynthPatchAbstract(String pManufacturerName, String pItemName, byte pInitSysex[],
			byte pInitSysexEdit[], String pVersion) {
		programType = 0;
		sourceTags = new ArrayList();
		sourceTagsEdit = new ArrayList();
		pgmParms = new ArrayList();
		hdrParms = new ArrayList();
		hdrParmsEdit = new ArrayList();
		genPatch = new GenericPatch(pVersion);
		parmTranslators = new ArrayList();
		parmLinks = new ArrayList();
		manufacturerName = pManufacturerName;
		itemName = pItemName;
		initSysex = pInitSysex;
		initSysexEdit = pInitSysexEdit;
	}

	/**
	 * see if all voice and global parameters have values within their valid ranges
	 * @return valid
	 */
	public boolean isValid() {
		int i;
		try {
			if (programType == 0) {
				for (i = 0; i < hdrParms.size(); i++) {
					if (((SynthParm)hdrParms.get(i)).isValid() == false) {
						invalidMsg = "parm " + ((SynthParm)hdrParms.get(i)).getName() + " was invalid";
						return false;
					}
				}
			} else {
				for (i = 0; i < hdrParmsEdit.size(); i++) {
					if (((SynthParm)hdrParmsEdit.get(i)).isValid() == false) {
						invalidMsg = "parm " + ((SynthParm)hdrParmsEdit.get(i)).getName() + " was invalid";
						return false;
					}
				}
			}
			for (i = 0; i < pgmParms.size(); i++) {
				if (((SynthParm)pgmParms.get(i)).isValid() == false) {
					invalidMsg = "parm " + ((SynthParm)pgmParms.get(i)).getName() + " was invalid";
					return false;
				}
			}
		} catch (NullPointerException e) {
			invalidMsg = "error reading parm list";
			return false;
		}
		return true;
	}

	/**
	 * see if all voice and global parameters have values within their defined ranges
	 * @return valid
	 */
	public boolean isDefined() {
		int i;
		try {
			if (programType == 0) {
				for (i = 0; i < hdrParms.size(); i++) {
					if (((SynthParm)hdrParms.get(i)).isDefined() == false) {
						invalidMsg = "parm " + ((SynthParm)hdrParms.get(i)).getName() + " was invalid";
						return false;
					}
				}
			} else {
				for (i = 0; i < hdrParmsEdit.size(); i++) {
					if (((SynthParm)hdrParmsEdit.get(i)).isDefined() == false) {
						invalidMsg = "parm " + ((SynthParm)hdrParmsEdit.get(i)).getName() + " was invalid";
						return false;
					}
				}
			}
			for (i = 0; i < pgmParms.size(); i++) {
				if (((SynthParm)pgmParms.get(i)).isDefined() == false) {
					invalidMsg = "parm " + ((SynthParm)pgmParms.get(i)).getName() + " was invalid";
					return false;
				}
			}
		} catch (NullPointerException e) {
			invalidMsg = "error reading parm list";
			return false;
		}
		return true;
	}

	public String getInvalidMsg() {
		return invalidMsg;
	}

	public GenericPatch getGenPatch() {
		return genPatch;
	}

	void addPgmParm(SynthParmAbstract spa, XMLTagGroup xtg) {
		pgmParms.add(spa);
		xtg.add(new XMLTagValue(spa.getName(), spa));
	}

	/**
	 * see if input XML contains this type of patch
	 */
	public String matchXMLTop(String xml) {
		int i;
		String tag[];

		i = xml.indexOf("<synth_patch"); // skipping <?xml ... ?>
		if (i == -1) {
			return null;
		}
		XMLReader xr = new XMLReader(xml.substring(i));
		tag = xr.getNextTag();
		if (tag[0].equalsIgnoreCase("synth_patch") == false) {
			return null;
		}
		xr = new XMLReader(tag[1]);
		tag = xr.getNextTag();
		if (tag == null || tag[0].equalsIgnoreCase("source_synth_manufacturer") == false ||
				tag[1].equalsIgnoreCase(manufacturerName) == false) {
			return null;
		}
		tag = xr.getNextTag();
		if (tag == null || tag[0].equalsIgnoreCase("source_synth_name") == false ||
				tag[1].equalsIgnoreCase(itemName) == false) {
			return null;
		}
		tag = xr.getNextTag();
		if (tag == null || tag[0].equalsIgnoreCase("source_patch") == false) {
			return null;
		}
		return tag[1];
	}

	/**
	 * convert patch internal variables to XML
	 */
	public String toXML() throws PatchDefinitionException {
		int i;
		StringBuffer sb = new StringBuffer();

		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
		sb.append("<synth_patch version=\"0.1\">");
		sb.append("<source_synth_manufacturer>" + manufacturerName + "</source_synth_manufacturer>");
		sb.append("<source_synth_name>" + itemName + "</source_synth_name>");

		if (programType == 0) {
			for (i = 0; i < sourceTags.size(); i++) {
				sb.append(((XMLTag)sourceTags.get(i)).writeXML());
			}
		} else {
			for (i = 0; i < sourceTagsEdit.size(); i++) {
				sb.append(((XMLTag)sourceTagsEdit.get(i)).writeXML());
			}
		}

		if (isDefined()) {
			translateToGeneric();
			sb.append(genPatch.writeXML());
		} else {
			System.out.println("input parameter(s) undefined - cannot create generic patch");
		}

		sb.append("</synth_patch>");
		return sb.toString();
	}

	/**
	 * read XML into internal variables
	 */
	public void fromXML(String xml) throws PatchDefinitionException {
		xml = XMLReader.convertInputEscapedChars(xml);
		if (matchXMLStored(xml) == true) {
			// Using this to initialize any unmapped parms
			fromSysex(initSysex);
			programType = 0;
			((XMLTag)sourceTags.get(0)).readXML(xml);
			genPatch.readInfoXML(XMLReader.getTagValue(xml, "generic_patch"));
			toSysex();
		} else if (matchXMLEdit(xml) == true) {
			// Using this to initialize any unmapped parms
			fromSysex(initSysexEdit);
			programType = 1;
			((XMLTag)sourceTagsEdit.get(0)).readXML(xml);
			genPatch.readInfoXML(XMLReader.getTagValue(xml, "generic_patch"));
			toSysex();
		} else if (xml.indexOf("generic_patch") != -1) {
			System.out.println("input file does not contain a " + manufacturerName +
					" " + itemName + ", converting from generic patch");
			if (genPatch.readXML(xml)) {
				adjustFromGeneric();
				if (genPatch.convertXML()) {
					translateFromGeneric();
					toSysex();
				}
			}
		} else {
			System.out.println("input file does not contain a " + manufacturerName +
					" " + itemName + " and has no generic patch");
		}
	}

	void adjustFromGeneric() throws PatchDefinitionException {
		// Each synth may need to make special adjustments to a generic patch
		// to be able to convert it, e.g. may need to simulate an AR envelope
		// with an ADSR by setting sustain to zero and decay=release
	}

	void translateFromGeneric() {
		for (int i = 0; i < parmLinks.size(); i++) {
			((ParmTranslator)parmLinks.get(i)).fromGeneric();
		}
		for (int i = 0; i < parmTranslators.size(); i++) {
			((ParmTranslator)parmTranslators.get(i)).fromGeneric();
		}
	}

	void translateToGeneric() throws PatchDefinitionException {
		for (int i = 0; i < parmTranslators.size(); i++) {
			((ParmTranslator)parmTranslators.get(i)).toGeneric();
		}
		for (int i = 0; i < parmLinks.size(); i++) {
			((ParmTranslator)parmLinks.get(i)).toGeneric();
		}

		/*
		 * Start at Synth Audio Out, work backwards across connections
		 * to find all modules that are either in the audio chain or
		 * are active modulators of those modules
		 */
		ModuleJack mj = genPatch.findModuleInputJack("Audio Out", "Voice In");
		genPatch.findJacksAndModulesUsed(mj);
	}

	/**
	 * @return sysex
	 */
	public byte[] getSysex() {
		return sysex;
	}

	public SynthParm findPgmParm(String name) {
		int i;
		SynthParm sp;

		// with few parms, a sequential search is fast enough
		for (i = 0; i < pgmParms.size(); i++) {
			sp = (SynthParm)pgmParms.get(i);
			if (sp.getName().equalsIgnoreCase(name)) {
				return sp;
			}
		}
		return null;
	}

	public SynthParm findHdrParm(String name) {
		int i;
		SynthParm sp;

		// with few parms, a sequential search is fast enough
		for (i = 0; i < hdrParms.size(); i++) {
			sp = (SynthParm)hdrParms.get(i);
			if (sp.getName().equalsIgnoreCase(name)) {
				return sp;
			}
		}
		return null;
	}
}
