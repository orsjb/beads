/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.core;

/**
 * AudioIO is the abstract base class for setting up interaction between {@link AudioContext} and the world. It is 
 * designed to be largely controlled by {@link AudioContext}. To be precise, AudioContext will prepare(), start(), and stop() the
 * AudioIO it is initialised with. However, certain AudioIO implementations may need to be set up before being passed to AudioContext(). By default, AudioContext creates a {@link JavaSoundAudioIO}.
 * 
 * @author ollie
 *
 */
public abstract class AudioIO {

	/** The context. */
	protected AudioContext context;
	
	/**
	 * Prepares the AudioIO. This method is called by {@link AudioContext}'s constructor. 
	 * This has default implementation as it will not be needed by most implementation.
	 * 
	 * @return true, if successful.
	 */
	protected boolean prepare() {
		return true;
	}
	
	/**
	 * Starts the AudioIO. When started, the AudioIO should repeatedly call {@link #update()}
	 * and then gather the output of {@link AudioContext#out}.
	 * 
	 * @return true, if successful
	 */
	protected abstract boolean start();
	
	/**
	 * Stops the AudioIO. Note this is not usually needed because the more usual way
	 * for the system to stop is simply to check {@link AudioContext#isRunning()} at
	 * each time step.
	 * 
	 * @return true, if successful.
	 */
	protected boolean stop() {return true;}
	
	/**
	 * Gets an audio input {@link UGen}. The argument specifies an array of channel numbers
	 * that this UGen should serve. For example, the array {0, 4, 2} should return a UGen with
	 * 3 outputs, corresponding to input channels 1, 5 and 3 respectively on the audio device.
	 * 
	 * @param channels an array indicating which channels to serve.
	 * @return the audio input.
	 */
	protected abstract UGen getAudioInput(int[] channels);
	
	/**
	 * Updates the {@link AudioContext}.
	 */
	protected void update() {
		context.update();
	}
	
	/**
	 * Gets the {@link AudioContext}.
	 * 
	 * @return the context.
	 */
	public AudioContext getContext() {
		return context;
	}

}
