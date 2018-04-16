/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * RangeLimiter forces a signal within the range [-1,1]. Use {@link Clip} for
 * constraining to other ranges.
 * 
 * @beads.category utilities
 * @author ollie
 * @author benito
 * @version 0.9.5
 */
public class RangeLimiter extends UGen {

	/**
	 * Instantiates a new RangeLimiter.
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 */
	public RangeLimiter(AudioContext context, int channels) {
		super(context, channels, channels);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.olliebown.beads.core.UGen#calculateBuffer()
	 */
	@Override
	public void calculateBuffer() {
		float y;

		for (int j = 0; j < ins; j++) {
			float[] bi = bufIn[j];
			float[] bo = bufOut[j];
			for (int i = 0; i < bufferSize; i++) {
				if ((y = bi[i]) > 1.0f) {
					bo[i] = 1f;
				} else if (y < -1f) {
					bo[i] = -1f;
				} else {
					bo[i] = y;
				}
			}
		}
	}

}
