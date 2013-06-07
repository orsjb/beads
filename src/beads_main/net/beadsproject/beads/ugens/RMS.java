/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.*;

/**
 * Calculates and outputs the RMS (root-mean-squares) power factor for a signal
 * over a given time frame. The algorithm accounts for multi-channel input by
 * summing the squares of each channel and dividing by the square root of the
 * number of channels.
 * 
 * @beads.category lowlevel
 * @author Benito Crawford
 * @version 0.9.5
 */
public class RMS extends UGen {

	private float[] rmsMem;
	private float sum = 0, channelScale, memScale;
	private int channels, index = 0, memorySize;

	/**
	 * Constructor.
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 * @param memorySize
	 *            The number of samples over which to compute the RMS.
	 */
	public RMS(AudioContext context, int channels, int memorySize) {
		super(context, channels, 1);
		this.channels = channels;
		channelScale = 1f / channels;
		rmsMem = new float[memorySize];
		this.memorySize = memorySize;
		memScale = 1f / memorySize;
	}

	@Override
	public void calculateBuffer() {

		float[] bo = bufOut[0];

		for (int i = 0; i < bufferSize; i++) {

			float x, newMem = 0;

			for (int j = 0; j < channels; j++) {
				x = bufIn[j][i];
				newMem += x * x;
			}
			sum -= rmsMem[index];
			rmsMem[index] = newMem * channelScale;
			sum += rmsMem[index];
			if (sum < 0)
				sum = 0;
			index = (index + 1) % memorySize;

			bo[i] = (float) Math.sqrt(sum * memScale);

		}

		// System.out.println("cb: " + sum);
	}

	/**
	 * Gets the number of channels.
	 * 
	 * @return The number of channels.
	 */
	public int getChannels() {
		return channels;
	}

	/**
	 * Gets the number of samples over which the RMS is calculated.
	 * 
	 * @return The number of samples.
	 */
	public int getMemorySize() {
		return memorySize;
	}

}
