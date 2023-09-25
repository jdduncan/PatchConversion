
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
 * JUnit tests for GenericPatch
 *
 * @author Kenneth L. Martinez
 */

import java.io.*;
//import java.util.*;
import junit.framework.*;

public class GenericPatchTest extends TestCase {

	public GenericPatchTest(String name) {
		super(name);
	}

//	  public static void main(String[] args) {
//		  junit.textui.TestRunner.run(suite());
//	  }

	public static Test suite() {
		return new TestSuite(GenericPatchTest.class);
	}

	void buildTestPatch1(GenericPatch gp) throws PatchDefinitionException {
		Module mod;
		ModuleParm mp;

		mod = new Module("Osc1", "osc", 1);
		gp.addModule(mod);
		mod.addOutputJack(new ModuleOutputJack("Wave Out", "audio_output"));
		mod.addParm(new ModuleParm("Waveform", new ParmValidatorTable(new String[] { "Pulse", "Saw", "Tri", "Sine" }), "Pulse"));

		mod = new Module("Osc2", "osc", 2);
		gp.addModule(mod);
		mod.addOutputJack(new ModuleOutputJack("Wave Out", "audio_output"));
		mod.addParm(new ModuleParm("Waveform", new ParmValidatorTable(new String[] { "Pulse", "Saw", "Tri", "Sine" }), "Pulse"));
		mod.addParm(new ModuleParm("Coarse Tune", "semitones", new ParmValidatorRange(-60, 60), "0"));
		mod.addParm(new ModuleParm("Fine Tune", "cents", new ParmValidatorRange(-100, 100), "0"));
		mod.addParm(new ModuleParm("Pulse Width", new ParmValidatorRange(0, 127), "50"));

		mod = new Module("Osc3", "osc", 3);
		gp.addModule(mod);
		mod.addOutputJack(new ModuleOutputJack("Wave Out", "audio_output"));
		mod.addParm(new ModuleParm("Waveform", new ParmValidatorTable(new String[] { "Pulse", "Saw", "Tri", "Sine" }), "Saw"));
		mod.addParm(new ModuleParm("Coarse Tune", "semitones", new ParmValidatorRange(-60, 60), "0"));
		mod.addParm(new ModuleParm("Fine Tune", "cents", new ParmValidatorRange(-100, 100), "0"));
		mod.addParm(new ModuleParm("Pulse Width", new ParmValidatorRange(0, 127), "50"));

		mod = new Module("Mixer", "mixer", 0);
		gp.addModule(mod);
		mod.addParm(new ModuleParm("Audio Amt1", "percent", new ParmValidatorRange(0, 100), "50"));
		mod.addInputJack(new ModuleInputJack("Audio In1", "audio_input"));
		mod.addParm(new ModuleParm("Audio Amt2", "percent", new ParmValidatorRange(0, 100), "50"));
		mod.addInputJack(new ModuleInputJack("Audio In2", "audio_input"));
		mp = new ModuleParm("Audio Amt3", "percent", new ParmValidatorRange(0, 100), "0");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Audio In3", "audio_input"), mp);
		mod.addOutputJack(new ModuleOutputJack("Mixer Out", "audio_output"));

		mod = new Module("Audio Out", "audio_out", 0);
		gp.addModule(mod);
		mod.addInputJack(new ModuleInputJack("Voice In", "audio_input"));

		gp.addConnection(new Connection(gp.findModuleOutputJack("Osc1", "Wave Out")
				, gp.findModuleInputJack("Mixer", "Audio In1")));
		gp.addConnection(new Connection(gp.findModuleOutputJack("Osc2", "Wave Out")
				, gp.findModuleInputJack("Mixer", "Audio In2")));
		gp.addConnection(new Connection(gp.findModuleOutputJack("Osc3", "Wave Out")
				, gp.findModuleInputJack("Mixer", "Audio In3")));
		gp.addConnection(new Connection(gp.findModuleOutputJack("Mixer", "Mixer Out")
				, gp.findModuleInputJack("Audio Out", "Voice In")));

		// This one's not connected, so it shouldn't make it into the patch
		mod = new Module("LFO1", "lfo", 1);
		gp.addModule(mod);
		mod.addOutputJack(new ModuleOutputJack("Wave Out", "control_output"));
		mod.addParm(new ModuleParm("Waveform", new ParmValidatorTable(new String[] { "Pulse", "Saw", "Tri", "Sine" }), "Tri"));
	}

	void buildTestPatch2(GenericPatch gp) throws PatchDefinitionException {
		Module mod;
		ModuleParm mp;

		mod = new ModuleOsc("Osc1", "osc", 1);
		gp.addModule(mod);
		mod.addParm(new ModuleParm("Waveform", new ParmValidatorTable(new String[] { "Pulse", "Saw", "Tri", "Sine" }), "Saw"));
		mod.addParm(new ModuleParm("Coarse Tune", "semitones", new ParmValidatorRange(0, 0), "0", "linear"));
		mod.addParm(new ModuleParm("Fine Tune", "cents", new ParmValidatorRange(0, 0), "0", "linear"));
		mod.addParm(new ModuleParm("Key Track", "percent", new ParmValidatorRange(100, 100), "100", "linear"));
		mod.addParm(new ModuleParm("Base Key", "midi note", new ParmValidatorRange(56, 56), "56"));
		mod.addParm(new ModuleParm("Pulse Width", "percent", new ParmValidatorRange(50, 99), "50", "linear"));
		mod.addParm(new ModuleParm("Pulse Width Range", null, new ParmValidatorTable(new String[] { "50-99" }), "50-99"));
		mp = new ModuleParm("PWM Amt1", "percent", new ParmValidatorRange(0, 50), "0", "linear");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("PWM In1", "control_input"), mp);
		mp = new ModuleParm("PWM Amt2", "percent", new ParmValidatorRange(-100, 100), "0", "linear");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("PWM In2", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt1", "semitones", new ParmValidatorRange(0, 63), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Expo FM In1", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt2", "semitones", new ParmValidatorRange(0, 63), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Expo FM In2", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt3", "semitones", new ParmValidatorRange(0, 63), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Expo FM In3", "control_input"), mp);
		mod.addOutputJack(new ModuleOutputJack("Wave Out", "audio_output"));
		// FIXME can't we just use Wave Out?
		mod.addOutputJack(new ModuleOutputJack("Sync Out", "audio_output"));

		mod = new ModuleOsc("Osc2", "osc", 2);
		gp.addModule(mod);
		mod.addParm(new ModuleParm("Waveform", new ParmValidatorTable(new String[] { "Pulse", "Saw", "Tri", "Sine" }), "Saw"));
		mod.addParm(new ModuleParm("Coarse Tune", "semitones", new ParmValidatorRange(-36, 36), "0", "linear"));
		mod.addParm(new ModuleParm("Fine Tune", "cents", new ParmValidatorRange(-50, 50), "0", "linear"));
		mod.addParm(new ModuleParm("Key Track", "percent", new ParmValidatorRange(0, 100), "100", "linear"));
		mod.addParm(new ModuleParm("Base Key", "midi note", new ParmValidatorRange(56, 56), "56"));
		mod.addParm(new ModuleParm("Pulse Width", "percent", new ParmValidatorRange(50, 100), "50", "linear"));
		mod.addParm(new ModuleParm("Pulse Width Range", null, new ParmValidatorTable(new String[] { "50-100" }), "50-100"));
		mp = new ModuleParm("PWM Amt1", "percent", new ParmValidatorRange(0, 50), "0", "linear");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("PWM In1", "control_input"), mp);
		mp = new ModuleParm("PWM Amt2", "percent", new ParmValidatorRange(-100, 100), "0", "linear");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("PWM In2", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt1", "semitones", new ParmValidatorRange(0, 63), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Expo FM In1", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt2", "semitones", new ParmValidatorRange(0, 63), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Expo FM In2", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt3", "semitones", new ParmValidatorRange(0, 63), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Expo FM In3", "control_input"), mp);
		mod.addOutputJack(new ModuleOutputJack("Wave Out", "audio_output"));
		mp = new ModuleParm("Sync", "hard", new ParmValidatorTable(new String[] { "Off", "On" }), "Off");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Sync In", "control_input"), mp);

		mod = new ModuleMixer("Mixer", "mixer", 0);
		gp.addModule(mod);
		mp = new ModuleParm("Audio Amt1", "percent", new ParmValidatorRange(0, 100), "0", "linear");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Audio In1", "audio_input"), mp);
		mp = new ModuleParm("Audio Amt2", "percent", new ParmValidatorRange(0, 100), "0", "linear");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Audio In2", "audio_input"), mp);
		mod.addOutputJack(new ModuleOutputJack("Mixer Out", "audio_output"));

		mod = new Module("Filter", "filter", 0);
		gp.addModule(mod);
		mod.addParm(new ModuleParm("Filter Type", new ParmValidatorTable(new String[] { "LP", "HP", "BP" }), "LP"));
		mod.addParm(new ModuleParm("Filter Slope", new ParmValidatorTable(new String[] { "12db", "24db" }), "24db"));
		mod.addParm(new ModuleParm("Frequency", "semitones", new ParmValidatorRange(-22, 105), "24", "linear"));
		mod.addParm(new ModuleParm("Resonance", "percent", new ParmValidatorRange(0, 100), "0", "linear"));
		mod.addParm(new ModuleParm("Self-Oscillate Percent", "percent", new ParmValidatorRange(1, 101), "100"));
		mp = new ModuleParm("Expo FM Amt1", "semitones", new ParmValidatorRange(0, 127), "0", "linear");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Expo FM In1", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt2", "semitones", new ParmValidatorRange(0, 127), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Expo FM In2", "control_input"), mp);
		mp = new ModuleParm("Expo FM Amt3", "semitones", new ParmValidatorRange(0, 127), "0", "expo");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Expo FM In3", "control_input"), mp);
		mod.addInputJack(new ModuleInputJack("Filter In", "audio_input"));
		mod.addOutputJack(new ModuleOutputJack("Filter Out", "audio_output"));
		mod.addParm(new ModuleParm("Key Track", "percent", new ParmValidatorTable(new String[] { "0", "33.3333", "66.6667", "100" }), "100"));
		mod.addParm(new ModuleParm("Base Key", "midi note", new ParmValidatorRange(36, 36), "36"));

		mod = new Module("Filter Envelope", "env_adsr", 1);
		gp.addModule(mod);
		mod.addOutputJack(new ModuleOutputJack("Env Out", "control_output", "positive"));
		mod.addParm(new ModuleParm("Attack", "seconds", new ParmValidatorRange(.0005, 50), "0.0005", "expo"));
		mod.addParm(new ModuleParm("Decay", "seconds", new ParmValidatorRange(.0005, 50), "0.5", "expo"));
		mod.addParm(new ModuleParm("Sustain", "percent", new ParmValidatorRange(0, 100), "0", "linear"));
		mod.addParm(new ModuleParm("Release", "seconds", new ParmValidatorRange(.0005, 50), "0.5", "expo"));
		mod.addParm(new ModuleParm("Trigger", new ParmValidatorTable(new String[] { "Single", "Multi" }), "Multi"));
		mod.addParm(new ModuleParm("Slope", new ParmValidatorTable(new String[] { "Expo" }), "Expo"));

		mod = new Module("Amp Envelope", "env_adsr", 2);
		gp.addModule(mod);
		mod.addOutputJack(new ModuleOutputJack("Env Out", "control_output", "positive"));
		mod.addParm(new ModuleParm("Attack", "seconds", new ParmValidatorRange(.0005, 50), "0.0005", "expo"));
		mod.addParm(new ModuleParm("Decay", "seconds", new ParmValidatorRange(.0005, 50), "0.5", "expo"));
		mod.addParm(new ModuleParm("Sustain", "percent", new ParmValidatorRange(0, 100), "100", "linear"));
		mod.addParm(new ModuleParm("Release", "seconds", new ParmValidatorRange(.0005, 50), "0.5", "expo"));
		mod.addParm(new ModuleParm("Trigger", new ParmValidatorTable(new String[] { "Single", "Multi" }), "Multi"));
		mod.addParm(new ModuleParm("Slope", new ParmValidatorTable(new String[] { "Expo" }), "Expo"));

		mod = new Module("VCA", "vca", 1);
		gp.addModule(mod);
		mp = new ModuleParm("Level Amt1", "percent", new ParmValidatorRange(0, 100), "50", "linear");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Level In1", "control_input"), mp);
		mp = new ModuleParm("Level Amt2", "percent", new ParmValidatorRange(0, 100), "0");
		mod.addParm(mp);
		mod.addInputJack(new ModuleInputJack("Level In2", "control_input"), mp);
		mod.addInputJack(new ModuleInputJack("VCA In", "audio_input"));
		mod.addOutputJack(new ModuleOutputJack("VCA Out", "audio_output"));

		mod = new Module("LFO1", "lfo", 1);
		gp.addModule(mod);
		mod.addOutputJack(new ModuleOutputJack("Wave Out", "control_output"));
		mod.addParm(new ModuleParm("Rate", "Hz", new ParmValidatorRange(.0005, 50), ".5"));
		mod.addParm(new ModuleParm("Waveform", new ParmValidatorTable(new String[] { "S&H", "Saw", "Tri", "Square", "S&G" }), "Tri"));

		mod = new Module("LFO2", "lfo", 2);
		gp.addModule(mod);
		mod.addOutputJack(new ModuleOutputJack("Wave Out", "control_output"));
		mod.addParm(new ModuleParm("Rate", "Hz", new ParmValidatorRange(.0005, 50), ".5"));
		mod.addParm(new ModuleParm("Waveform", new ParmValidatorTable(new String[] { "Tri" }), "Tri"));

		mod = new Module("Mod Envelope", "env_ar", 0);
		gp.addModule(mod);
		mod.addOutputJack(new ModuleOutputJack("Env Out", "control_output", "positive"));
		mod.addParm(new ModuleParm("Attack", "seconds", new ParmValidatorRange(.0005, 50), "0.0005", "expo"));
		mod.addParm(new ModuleParm("Decay", "seconds", new ParmValidatorRange(.0005, 50), "0.5", "expo"));
		mod.addParm(new ModuleParm("Slope", new ParmValidatorTable(new String[] { "Expo" }), "Expo"));

		mod = new Module("Audio Out", "audio_out", 0);
		gp.addModule(mod);
		mod.addInputJack(new ModuleInputJack("Voice In", "audio_input"));

		gp.addConnection(new Connection(gp.findModuleOutputJack("Osc1", "Wave Out")
				, gp.findModuleInputJack("Mixer", "Audio In1")) );
		gp.addConnection(new Connection(gp.findModuleOutputJack("Osc2", "Wave Out")
				, gp.findModuleInputJack("Mixer", "Audio In2")) );
		gp.addConnection(new Connection(gp.findModuleOutputJack("Mixer", "Mixer Out")
				, gp.findModuleInputJack("Filter", "Filter In")) );
		gp.addConnection(new Connection(gp.findModuleOutputJack("Filter Envelope", "Env Out")
				, gp.findModuleInputJack("Filter", "Expo FM In1")) );
		gp.addConnection(new Connection(gp.findModuleOutputJack("Amp Envelope", "Env Out")
				, gp.findModuleInputJack("VCA", "Level In1")) );
		gp.addConnection(new Connection(gp.findModuleOutputJack("Filter", "Filter Out")
				, gp.findModuleInputJack("VCA", "VCA In")) );
		gp.addConnection(new Connection(gp.findModuleOutputJack("VCA", "VCA Out")
				, gp.findModuleInputJack("Audio Out", "Voice In")) );
		gp.addConnection(new Connection(gp.findModuleOutputJack("LFO1", "Wave Out")
				, gp.findModuleInputJack("Osc1", "PWM In1")) );
		gp.addConnection(new Connection(gp.findModuleOutputJack("LFO1", "Wave Out")
				, gp.findModuleInputJack("Osc1", "Expo FM In1")) );
		gp.addConnection(new Connection(gp.findModuleOutputJack("LFO1", "Wave Out")
				, gp.findModuleInputJack("Osc2", "PWM In1")) );
		gp.addConnection(new Connection(gp.findModuleOutputJack("LFO1", "Wave Out")
				, gp.findModuleInputJack("Osc2", "Expo FM In1")) );
		gp.addConnection(new Connection(gp.findModuleOutputJack("LFO1", "Wave Out")
				, gp.findModuleInputJack("Filter", "Expo FM In2")) );
		gp.addConnection(new Connection(gp.findModuleOutputJack("LFO2", "Wave Out")
				, gp.findModuleInputJack("Osc1", "Expo FM In3")) );
		gp.addConnection(new Connection(gp.findModuleOutputJack("LFO2", "Wave Out")
				, gp.findModuleInputJack("Osc2", "Expo FM In3")) );
		gp.addConnection(new Connection(gp.findModuleOutputJack("LFO2", "Wave Out")
				, gp.findModuleInputJack("Filter", "Expo FM In3")) );
		gp.addConnection(new Connection(gp.findModuleOutputJack("Mod Envelope", "Env Out")
				, gp.findModuleInputJack("Osc1", "PWM In2")) );
		gp.addConnection(new Connection(gp.findModuleOutputJack("Mod Envelope", "Env Out")
				, gp.findModuleInputJack("Osc1", "Expo FM In2")) );
		gp.addConnection(new Connection(gp.findModuleOutputJack("Mod Envelope", "Env Out")
				, gp.findModuleInputJack("Osc2", "PWM In2")) );
		gp.addConnection(new Connection(gp.findModuleOutputJack("Mod Envelope", "Env Out")
				, gp.findModuleInputJack("Osc2", "Expo FM In2")) );
	}

	public void testWriteXML() throws IOException, PatchDefinitionException {
		GenericPatch gp = new GenericPatch("0.07");
		buildTestPatch1(gp);
		gp.findJacksAndModulesUsed(gp.findModuleInputJack("Audio Out", "Voice In"));

		String s = gp.writeXML();
		PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter("tst/testGenericActual1.xml")));
		out.print(Util.formatXML(s, 0));
		out.close();

		String sActual, sExpected;
		int i = 0;
		BufferedReader actual = new BufferedReader(new FileReader("tst/testGenericActual1.xml"));
		BufferedReader expected = new BufferedReader(new FileReader("tst/testGenericExpected1.xml"));
		while ((sActual = actual.readLine()) != null) {
			i++;
//			System.out.println(sActual);
			if ((sExpected = expected.readLine()) == null || sActual.equals(sExpected) == false) {
				System.out.println("line " + i + " did not match");
				if (sExpected == null) {
					System.out.println("expected was null");
				} else {
					System.out.println("expected = /" + sExpected + "/");
				}
				System.out.println("actual   = /" + sActual + "/");
				assertTrue(false);
			}
		}
		actual.close();
		expected.close();
	}

	public void testReadXML2() throws IOException, PatchDefinitionException  {
		GenericPatch gp = new GenericPatch("0.07");
		buildTestPatch1(gp);
		fileReadXML(2, gp);
	}


	public void testReadXML3() throws IOException, PatchDefinitionException  {
		GenericPatch gp = new GenericPatch("0.07");
		buildTestPatch2(gp);
		fileReadXML(3, gp);
	}

	public void testReadXML4() throws IOException, PatchDefinitionException  {
		GenericPatch gp = new GenericPatch("0.07");
		buildTestPatch2(gp);
		fileReadXML(4, gp);
	}

	public void fileReadXML(int num, GenericPatch gp) throws IOException, PatchDefinitionException  {
		BufferedReader in = new BufferedReader(new FileReader("tst/testGenericIn" + num + ".xml"));
		PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter("tst/testGenericActual" + num + ".xml")));
		String s;
		StringBuffer sb = new StringBuffer();
		while ((s = in.readLine()) != null) {
			sb.append(s + "\n");
		}
		in.close();
		gp.readXML(sb.toString());
		gp.convertXML();
		String xml = gp.writeXML();
		out.print(Util.formatXML(xml, 0));
		out.close();

		String sActual, sExpected;
		int i = 0;
		BufferedReader actual = new BufferedReader(new FileReader("tst/testGenericActual" + num + ".xml"));
		BufferedReader expected = new BufferedReader(new FileReader("tst/testGenericExpected" + num + ".xml"));
		while ((sActual = actual.readLine()) != null) {
			i++;
//			System.out.println(sActual);
			if ((sExpected = expected.readLine()) == null || sActual.equals(sExpected) == false) {
				System.out.println("line " + i + " did not match");
				if (sExpected == null) {
					System.out.println("expected was null");
				} else {
					System.out.println("expected = /" + sExpected + "/");
				}
				System.out.println("actual   = /" + sActual + "/");
				assertTrue(false);
			}
		}
		actual.close();
		expected.close();
	}
}
