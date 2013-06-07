package net.beadsproject.beads.play;

import net.beadsproject.beads.core.AudioContext;

public abstract class EnvironmentFactory {
	
	public abstract Environment createEnvironment(AudioContext ac);
	
}
