
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
 * Describes a module input jack
 *
 * @author Kenneth L. Martinez
 */

//import java.util.*;

public class ModuleInputJack extends ModuleJack {
//	private ModuleJack sourceJack;
	private Connection sourceConn;  // FIXME s.b. temp until conn[] is removed
	private ModuleParm attenuator;
	private String sourceModule;
	private String sourceJack;

	ModuleInputJack(String pName, String pType) { 
		super(pName, pType);
		sourceConn = null;
		tag = "input_";
	}

	ModuleInputJack(String pName, String pType, ModuleParm pAttenuator) { 
		super(pName, pType);
		attenuator = pAttenuator;
		sourceConn = null;
		tag = "input_";
	}

	ModuleInputJack(String xml, Module pMod) throws PatchDefinitionException {
		super(xml, pMod);
		String s = XMLReader.getTagValue(xml, "attenuator");
		if (s != null) {
			attenuator = pMod.findParm(s);
		}
		sourceConn = null;
		tag = "input_";
		sourceModule = XMLReader.getTagValue(xml, "source_module");
		if (sourceModule == null) {
			s = "Error: source_module is missing for " + pMod.getName() + " " + getName();
			throw new PatchDefinitionException(s);
		}
		sourceJack = XMLReader.getTagValue(xml, "source_jack");
		if (sourceJack == null) {
			s = "Error: source_jack is missing for " + pMod.getName() + " " + getName();
			throw new PatchDefinitionException(s);
		}
	}

	public Connection buildConnection() throws PatchDefinitionException {
		GenericPatch gp = getMod().getGp();
		sourceConn = new Connection(gp.findModuleOutputJack(sourceModule, sourceJack),
				this);
		return sourceConn;
	}

	public ModuleParm getAttenuator() {
		return attenuator;
	}

	public void setAttenuator(ModuleParm mp) {
		attenuator = mp;
	}

//	public void setSourceJack(ModuleJack mj) {
//		sourceJack = mj;
//	}
//
//	public ModuleJack getSourceJack() {
//		return sourceJack;
//	}

	public void addConn(Connection pConn) throws PatchDefinitionException {
		if (sourceConn == null) {
			sourceConn = pConn;
			return; 
		}
		String s = "cannot add another connection to " + this.getMod().getName() +
				" " + this.getName();
		throw new PatchDefinitionException(s);
	}

	public void removeConn() {
		sourceConn = null;
	}

	public Connection getConn() {
		return sourceConn;
	}

	public boolean isConnectedToUsed() {
		if (sourceConn == null) {
			return false;
		}
		ModuleOutputJack mij = sourceConn.getSourceJack();
		if (mij.isUsed() && mij.getMod().getUsed() > 1) {
			return true;
		} else {
			return false;
		}
	}

	String writeValue() {
		StringBuffer sb = new StringBuffer();
		if (attenuator != null) {
			sb.append("<attenuator>" + attenuator.getName() + "</attenuator>");
		}
		sb.append("<source_module>" + sourceConn.getSourceJack().getMod().getName() + "</source_module>");
		sb.append("<source_jack>" + sourceConn.getSourceJack().getName() + "</source_jack>");
		return sb.toString();
	}
}
