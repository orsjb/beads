package net.beadsproject.beads.core.io;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.ugens.Noise;

public class JSTest {

	public static void main(String[] args) {
		//test
		AudioContext ac = new AudioContext(new JavaSoundAudioIO());
		Noise n = new Noise(ac);
		ac.out.addInput(n);
		ac.start();
	}

}
