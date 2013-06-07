/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.events;

import net.beadsproject.beads.core.Bead;

/**
 * Use KillTrigger to cause a specific {@link Bead} to be killed ({@link Bead#kill()}) in response to a specific event.
 */
public class KillTrigger extends Bead {

	/** The Bead that will be killed. */
	Bead receiver;
	
	/**
	 * Instantiates a new KillTrigger which will stop the given {@link Bead} when triggered.
	 * 
	 * @param receiver
	 *            the receiver.
	 */
	public KillTrigger(Bead receiver) {
		this.receiver = receiver;
	}
	
	/**
	 * Any incoming message will cause the specified {@link Bead} to be killed.
	 * @see #message(Bead)
	 */
	public void messageReceived(Bead message) {
		if(receiver != null) receiver.kill();
	}
}
