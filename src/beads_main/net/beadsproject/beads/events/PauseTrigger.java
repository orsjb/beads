/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.events;

import net.beadsproject.beads.core.Bead;

/**
 * Use PauseTrigger to cause a specific {@link Bead} to pause in response to a specific event.
 */
public class PauseTrigger extends Bead {

	/** The Bead that will be paused. */
	private Bead receiver;
	
	/**
	 * Instantiates a new PauseTrigger which will pause the given {@link Bead} when triggered.
	 * 
	 * @param receiver
	 *            the receiver.
	 */
	public PauseTrigger(Bead receiver) {
		this.receiver = receiver;
	}
	
	/** 
     * Any incoming message will cause the specified {@link Bead} to get paused.
	 * @see #message(Bead)
	 */
	public void messageReceived(Bead message) {
		if(receiver != null) receiver.pause(true);
	}
}
