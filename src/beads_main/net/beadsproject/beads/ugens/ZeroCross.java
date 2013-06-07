/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.*;

/**
 * Counts and outputs as a signal the number of zero crossings in its input
 * signal over a specified time frame.
 * 
 * @beads.category lowlevel
 * @author Benito Crawford
 * @version 0.9.5
 */
public class ZeroCross extends UGen {

	private boolean above = false;
	private boolean[] cross;
	private int sum = 0, index = 0, memSize;

	/**
	 * Constructor. The specified memory size indicates the time frame over
	 * which zero crossings are counted.
	 * 
	 * @param context
	 *            The audio context.
	 * @param memSizeInMS
	 *            The time frame in milliseconds.
	 */
	public ZeroCross(AudioContext context, float memSizeInMS) {
		super(context, 1, 1);
		memSize = (int) (context.msToSamples(memSizeInMS) + 1);
		cross = new boolean[memSize];
	}

	@Override
	public void calculateBuffer() {

		float[] bi = bufIn[0];
		float[] bo = bufOut[0];

		for (int i = 0; i < bufferSize; i++) {

			if (cross[index]) {
				sum--;
				cross[index] = false;
			}

			if (bi[i] < 0) {
				if (above) {
					cross[index] = true;
					sum++;
					above = false;
				}
			} else {
				if (!above) {
					cross[index] = true;
					sum++;
					above = true;
				}
			}

			bo[i] = sum;
			index = (index + 1) % memSize;
		}
	}

	/**
	 * Gets the memory size.
	 * 
	 * @return The memory size in milliseconds.
	 */
	public float getMemorySize() {
		return (float) context.samplesToMs(memSize);
	}

}
