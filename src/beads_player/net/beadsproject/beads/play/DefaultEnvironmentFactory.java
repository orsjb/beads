package net.beadsproject.beads.play;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.BeadArray;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.ugens.Clock;

public class DefaultEnvironmentFactory extends EnvironmentFactory {

	public Environment createEnvironment(AudioContext ac) {
		Environment e = new Environment();
		e.ac = ac;
		//set up clock as a pathway
		Clock c = new Clock(e.ac, 500f);
		c.setName("master clock");
		e.ac.out.addDependent(c);
		BeadArray clockListener = new BeadArray();
		c.addMessageListener(clockListener);
		e.pathways.put(c.getName(), clockListener);
		//and also as an object
		e.elements.put(c.getName(), c);
		//set up audio input as object in environment
		UGen in = e.ac.getAudioInput(new int[] {1, 2});
		e.elements.put("in", in);
		e.ac.out.addDependent(in);

		//set up in? and main out as object in channels
		e.channels.put("in", in);
		e.channels.put("out", e.ac.out);

		//try with delay
//		TapIn tin = new TapIn(e.ac, 10000);
//		TapOut tout = new TapOut(e.ac, tin, 50f);
//		e.ac.out.addInput(tout);
//		e.channels.put("out", tin);
		
		return e;
	}
}
