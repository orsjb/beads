/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.events;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Bead;

/**
 * Use AudioContextStopTrigger to cause the {@link AudioContext} to stop in response to a given event.
 * 
 * For example, to cause the {@link AudioContext} to stop when a sample has finished playing:
 * <code>
 *         AudioContext context = new AudioContext();
 *         SamplePlayer samplePlayer = new SamplePlayer(SampleManager.sample(pathToAudioFile));
 *         context.out.addInput(samplePlayer);
 *        samplePlayer.setKillListener(new AudioContextStopTrigger(context));
 *        context.start();
 * </code>
 */
public class AudioContextStopTrigger extends Bead {

	/** The AudioContext. */
	AudioContext ac;
	
	/**
	 * Creates a new audio context stop trigger.
	 * 
	 * @param ac
	 *            the AudioContext.
	 */
	public AudioContextStopTrigger(AudioContext ac) {
		this.ac = ac;
	}
	
	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.Bead#message(com.olliebown.beads.core.Bead)
	 */
	public void messageReceived(Bead message) {
		kill();
	}
	
	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.Bead#stop()
	 */
	public void kill() {
		ac.stop();
    }
	
}
