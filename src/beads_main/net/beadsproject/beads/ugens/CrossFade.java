package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * CrossFade is used to quickly crossfade between elements. Add it to the signal chain and
 * simply call {@link #fadeTo(UGen, float)} to fade to a new {@link UGen} over a given duration. 
 * Note that UGen has it's own simpler crossFade method.
 * 
 * TODO: non-linear fades.
 * 
 * @beads.category utilities
 * @author ollie
 *
 */
public class CrossFade extends UGen {

	private UGen incoming;
	private UGen outgoing;
	private double crossfadeTimeSamps;
	private long currentTimeSamps;
	private float incomingLevel, outgoingLevel;
	private boolean pauseAfterComplete;
	
	/**
	 * Create a new CrossFade with given number of channels. The CrossFade has no inputs
	 * since the input UGens are assigned in the method {@link #fadeTo(UGen, float)}.
	 * @param context the AudioContext.
	 * @param channels the number of output channels.
	 */
	public CrossFade(AudioContext context, int channels) {
		super(context, channels);
		incoming = new Static(context, 0f);
		pauseAfterComplete = false;
	}
	

	/**
	 * Create a new CrossFade with given start UGen.
	 * @param context the AudioContext.
	 * @param start the UGen to start on.
	 */
	public CrossFade(AudioContext context, UGen start) {
		super(context, start.getOuts());
		incoming = start;
		pauseAfterComplete = false;
	}
	
	/**
	 * Cross fades from the current UGen to the specified UGen the specified number of milliseconds.
	 * @param target the new target UGen.
	 * @param crossfadeTimeMS the cross fade time in milliseconds.
	 */
	public void fadeTo(UGen target, float crossfadeTimeMS) {
		if(incoming != target) {
			outgoing = incoming;
			incoming = target;
			if(incoming == null) incoming = new Static(context, 0f);
			crossfadeTimeSamps = context.msToSamples(crossfadeTimeMS);
			currentTimeSamps = 0;
		}
	}
	

	/**
	 * True if this CrossFade is set to pause outgoing UGens once they have been faded out.
	 * @return true if it is true, false otherwise.
	 */
	public boolean doesPauseAfterComplete() {
		return pauseAfterComplete;
	}

	/**
	 * Set whether this CrossFade is set to pause outgoing UGens once they have been faded out.
	 * @param pauseAfterComplete true means that outgoing UGens will be paused once they have been faded out.
	 */
	public void setPauseAfterComplete(boolean pauseAfterComplete) {
		this.pauseAfterComplete = pauseAfterComplete;
	}


	/* (non-Javadoc)
	 * @see net.beadsproject.beads.core.UGen#calculateBuffer()
	 */
	@Override
	public void calculateBuffer() {
		incoming.update();
		if(outgoing != null) outgoing.update();
		for(int j = 0; j < bufferSize; j++) {
			if(currentTimeSamps >= crossfadeTimeSamps) {
				for(int i = 0; i < outs; i++) {
					bufOut[i][j] = incoming.getValue(i, j);
				}	
			} else {
				incomingLevel = (float)(currentTimeSamps / crossfadeTimeSamps);
				outgoingLevel = 1f - incomingLevel;
				for(int i = 0; i < outs; i++) {
					bufOut[i][j] = incomingLevel * incoming.getValue(i, j) + 
								   outgoingLevel * outgoing.getValue(i, j);
				}
				currentTimeSamps++;
			}
		}
		if(currentTimeSamps >= crossfadeTimeSamps && pauseAfterComplete && outgoing != null) {
			outgoing.pause(true);
			outgoing = null;
		}
	}

}
