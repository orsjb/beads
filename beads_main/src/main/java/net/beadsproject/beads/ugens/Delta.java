/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * Outputs the change in the input signal from the previous sample to the current one.
 * 
 * @beads.category lowlevel
 * @author Benito Crawford
 * @version 0.9
 */
public class Delta extends UGen {
	
	private float lastX = 0;
	
	/**
	 * Bare constructor.
	 * 
	 * @param context
	 *            The audio context.
	 */
	public Delta(AudioContext context) {
		super(context, 1, 1);
	}

	/**
	 * Constructor for a given input UGen.
	 * 
	 * @param context
	 *            The audio context.
	 * @param ugen
	 *            The input UGen.
	 */
	public Delta(AudioContext context, UGen ugen) {
		super(context, 1, 1);
		addInput(0, ugen, 0);
	}

	@Override
	public void calculateBuffer() {
		
		float[] bi = bufIn[0];
		float[] bo = bufOut[0];
		int i;
		
		bo[0] = bi[0] - lastX;
		
		for(i = 1; i < bufferSize; i++) {
			bo[i] = bi[i] - bi[i - 1];
		}
		
		lastX = bi[i - 1];
	}

}
