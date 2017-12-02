/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * A simple UGen that just forwards its inputs to its outputs. Can be used to
 * isolate 1 or more channels from a multi-channel UGen's output, or to collect
 * several signals together.
 * 
 * @author Benito Crawford
 * @version 0.9.5
 * 
 */
public class Plug extends UGen {

	/**
	 * Constructor for a one-channel Plug using the specified audio
	 * context.
	 * 
	 * @param context
	 *            The audio context.
	 */
	public Plug(AudioContext context) {
		this(context, 1);
	}

	/**
	 * Constructor for a mono Plug that takes input from the specified source
	 * UGen.
	 * 
	 * @param context
	 *            The audio context.
	 * @param souceUGen
	 *            The source UGen.
	 */
	public Plug(AudioContext context, UGen sourceUGen) {
		this(context, 1);
		this.addInput(sourceUGen);
	}

	/**
	 * Constructor for a mono Plug that takes input from the specified output
	 * channel of a source UGen.
	 * 
	 * @param context
	 *            The audio context.
	 * @param souceUGen
	 *            The source UGen.
	 * @param sourceOutputChannel
	 *            The channel from the source UGen to take as input.
	 */
	public Plug(AudioContext context, UGen sourceUGen, int sourceOutputChannel) {
		this(context, 1);
		this.addInput(0, sourceUGen, sourceOutputChannel);
	}

	/**
	 * Constructor for a Plug with the specified number of channels, using
	 * the specified audio context.
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 */
	public Plug(AudioContext context, int channels) {
		super(context, channels, channels);
		this.outputInitializationRegime = OutputInitializationRegime.RETAIN;
		bufOut = bufIn;
	}

	@Override
	public void calculateBuffer() {
	}

}
