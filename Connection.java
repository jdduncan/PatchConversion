
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
 * Describes a connection between module jacks (like a patch cord for
 * a modular synth)
 *
 * @author Kenneth L. Martinez
 */

//import java.util.*;

public class Connection {
	private GenericPatch gp;
	private ModuleOutputJack sourceJack;
	private ModuleInputJack targetJack;

	Connection(ModuleOutputJack pSourceJack, ModuleInputJack pTargetJack) throws PatchDefinitionException {
		sourceJack = pSourceJack;
		if (sourceJack == null) {
			throw new PatchDefinitionException("Cannot create connection with null source jack");
		}
		sourceJack.addConn(this);
		targetJack = pTargetJack;
		if (targetJack == null) {
			throw new PatchDefinitionException("Cannot create connection with null target jack");
		}
		targetJack.addConn(this);
	}

	public void setGp(GenericPatch pGp) {
		gp = pGp;
	}

	public GenericPatch getGp() {
		return gp;
	}

	public ModuleOutputJack getSourceJack() {
		return sourceJack;
	}

	public ModuleInputJack getTargetJack() {
		return targetJack;
	}

	public String writeXML() {
		if (sourceJack == null || sourceJack.getMod().getUsed() < 3 ||
				sourceJack.isUsed() == false) {
			return "";
		}
		if (targetJack == null || targetJack.getMod().getUsed() < 3 ||
				targetJack.isUsed() == false) {
			return "";
		}

		StringBuffer sb = new StringBuffer("<connection>");
		sb.append("<source_module>" + sourceJack.getMod().getName() + "</source_module>");
		sb.append("<source_jack>" + sourceJack.getName() + "</source_jack>");
		sb.append("<target_module>" + targetJack.getMod().getName() + "</target_module>");
		sb.append("<target_jack>" + targetJack.getName() + "</target_jack>");
		sb.append("</connection>");
		return sb.toString();
	}
}
