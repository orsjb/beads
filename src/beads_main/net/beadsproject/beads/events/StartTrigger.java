/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.events;

import net.beadsproject.beads.core.Bead;

/**
 * Use StartTrigger to cause a specific {@link Bead} to start (unpause) in response to a specific event.
 */
public class StartTrigger extends Bead {

	/** The Bead that will be started. */
	Bead receiver;
	
	/**
	 * Instantiates a new StartTrigger which will start the given {@link Bead} when triggered.
	 * 
	 * @param receiver
	 *            the receiver.
	 */
	public StartTrigger(Bead receiver) {
		this.receiver = receiver;
	}
	
	/**
	 * Any incoming message will cause the specified {@link Bead} to start.
	 * @see #message(Bead)
	 */
	public void messageReceived(Bead message) {
		receiver.start();
		
	}
}