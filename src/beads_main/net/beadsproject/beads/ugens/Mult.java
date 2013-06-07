/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * Takes an incoming signal (or signals in the multi-channel case) and
 * multiplies it with something (another signal or a float value).
 * 
 * @beads.category utilities
 * @author ollie
 * @author Benito Crawford
 */
public class Mult extends UGen {

	private float multiplier = 1;
	private UGen multiplierUGen;

	/**
	 * Constructor for a Mult object with a static multiplier value.
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 * @param multiplier
	 *            The multiplier value.
	 */
	public Mult(AudioContext context, int channels, float multiplier) {
		super(context, channels, channels);
		setMultiplier(multiplier);
	}

	/**
	 * Constructor for a Mult object with a UGen controlling the multiplier
	 * value.
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 * @param multiplierUGen
	 *            The UGen controlling the multiplier value.
	 */
	public Mult(AudioContext context, int channels, UGen multiplierUGen) {
		super(context, channels, channels);
		setMultiplier(multiplierUGen);
	}
	
	/**
	 * Constructor for a Mult object with a given UGen as input and another as multiplier.
	 * i.e., use this as quickest way to multiply two UGens together.
	 * 
	 * @param context the AudioContext.
	 * @param input the input UGen.
	 * @param multiplierUGen the multiplier UGen.
	 */
	public Mult(AudioContext context, UGen input, UGen multiplierUGen) {
		super(context, input.getOuts(), input.getOuts());
		setMultiplier(multiplierUGen);
		addInput(input);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.olliebown.beads.core.UGen#calculateBuffer()
	 */
	@Override
	public void calculateBuffer() {
		if (multiplierUGen == null) {
			for (int j = 0; j < outs; j++) {
				float[] bi = bufIn[j];
				float[] bo = bufOut[j];

				for (int i = 0; i < bufferSize; i++) {
					bo[i] = bi[i] * multiplier;
				}
			}
		} else {
			multiplierUGen.update();
			if (outs == 1) {
				float[] bi = bufIn[0];
				float[] bo = bufOut[0];
				for (int i = 0; i < bufferSize; i++) {
					multiplier = multiplierUGen.getValue(0, i);
					bo[i] = bi[i] * multiplier;

				}
			} else {
				for (int i = 0; i < bufferSize; i++) {
					for (int j = 0; j < outs; j++) {
						multiplier = multiplierUGen.getValue(0, i);
						bufOut[j][i] = bufIn[j][i] * multiplier;
					}
				}
			}
		}
	}

	/**
	 * Gets the current multiplier value.
	 * 
	 * @return The multiplier.
	 */
	public float getMultiplier() {
		return multiplier;
	}

	/**
	 * Sets the multiplier to a static float value.
	 * 
	 * @param multiplier
	 *            The new multiplier value.
	 * @return This Mult instance.
	 */
	public Mult setMultiplier(float multiplier) {
		this.multiplier = multiplier;
		multiplierUGen = null;
		return this;
	}

	/**
	 * Sets a UGen to control the multiplier value.
	 * 
	 * @param multiplierUGen
	 *            The multiplier UGen.
	 * @return This Mult instance.
	 */
	public Mult setMultiplier(UGen multiplierUGen) {
		if (multiplierUGen == null) {
			setMultiplier(multiplier);
		} else {
			this.multiplierUGen = multiplierUGen;
			multiplierUGen.update();
			multiplier = multiplierUGen.getValue();
		}
		return this;
	}

	/**
	 * Gets the multiplier UGen controller.
	 * 
	 * @return The multipler UGen controller.
	 */
	public UGen getMultiplierUGen() {
		return multiplierUGen;
	}

}
