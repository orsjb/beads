/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.*;

//import net.beadsproject.beads.ugens.Delta;

/**
 * Outputs 1 if the input signal is increasing; -1 if it is decreasing; 0 if it
 * is the same. Use {@link Delta} to find how much a signal is changing.
 * 
 * @beads.category lowlevel
 * @author Benito Crawford
 * @version 0.9.5
 */
public class Change extends UGen {

	private float lastX = 0;
	private int currentDirection = 0;

	/**
	 * Bare constructor.
	 * 
	 * @param context
	 *            The audio context.
	 */
	public Change(AudioContext context) {
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
	public Change(AudioContext context, UGen ugen) {
		super(context, 1, 1);
		addInput(0, ugen, 0);
	}

	@Override
	public void calculateBuffer() {

		float[] bi = bufIn[0];
		float[] bo = bufOut[0];
		float x;

		for (int i = 1; i < bufferSize; i++) {
			if ((x = bi[i]) > lastX) {
				bo[i] = 1;
				if (currentDirection != 1) {
					currentDirection = 1;
					directionChange(1);
				}
			} else if (x < lastX) {
				bo[i] = -1;
				if (currentDirection != -1) {
					currentDirection = -1;
					directionChange(-1);
				}
			} else {
				bo[i] = 0;
			}
			lastX = x;
		}
	}

	/**
	 * Called when the input signal changes direction. Does nothing by default;
	 * can be overridden to execute code when this happens.
	 * 
	 * @param newDirection
	 *            The new direction of the signal (1 or -1);
	 */
	public void directionChange(int newDirection) {
	}

}
