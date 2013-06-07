/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.core.io;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.AudioIO;
import net.beadsproject.beads.core.UGen;

/**
 * A dummy AudioIO class that is purely for non-realtime use; it does not
 * interface with any system audio.
 * 
 * @author Benito Crawford
 * @version 0.9.5
 * 
 */
public class NonrealtimeIO extends AudioIO {

	/* (non-Javadoc)
	 * @see net.beadsproject.beads.core.AudioIO#start()
	 */
	protected boolean start() {
		while(context.isRunning()) {
			update();
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see net.beadsproject.beads.core.AudioIO#getAudioInput(int[])
	 */
	protected UGen getAudioInput(int[] channels) {
		return new ThisIsNotAnInput(context, channels.length);
	}
	
	/**
	 * The Class ThisIsNotAnInput.
	 */
	private class ThisIsNotAnInput extends UGen {

		/**
		 * Instantiates a new this is not an input.
		 * 
		 * @param context the context
		 * @param outs the outs
		 */
		public ThisIsNotAnInput(AudioContext context, int outs) {
			super(context, outs);
			outputInitializationRegime = OutputInitializationRegime.ZERO;
			pause(true);
		}

		/* (non-Javadoc)
		 * @see net.beadsproject.beads.core.UGen#calculateBuffer()
		 */
		@Override
		public void calculateBuffer() {}
		
	}


}
