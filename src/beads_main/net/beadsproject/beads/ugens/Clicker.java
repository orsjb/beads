/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * A very simple {@link UGen} that generates one click and then kills itself.
 *
 * @beads.category synth
 * @author ollie
 */
public class Clicker extends UGen {

	private boolean done;
	private float strength;
	
	/**
	 * Instantiates a new Clicker.
	 * 
	 * @param context
	 *            the AudioContext.
	 * @param strength the volume of the click (max = 1).
	 */
	public Clicker(AudioContext context, float strength) {
		super(context, 0, 1);
		this.strength = Math.min(1f, Math.abs(strength));
		done = false;
	}

	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.UGen#calculateBuffer()
	 */
	@Override
	public void calculateBuffer() {
		if(done) kill();
		else {
			bufOut[0][0] = strength;
			done = true;
		}
	}

}