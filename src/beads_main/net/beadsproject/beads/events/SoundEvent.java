/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.events;

import java.util.Map;

import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.DataBead;

/**
 * A general purpose interface for defining sound events. A SoundEvent is created with
 * the method @link {@link #play(UGen, DataBead)} and can be passed a {@link DataBead}
 * which can contain additional classes for controlling the event after it has been 
 * triggered. The SoundEvent also gets passed a {@link UGen} which it should connect to.
 * It should also return the {@link UGen} which is its root, so that callers of the
 * SoundEvent can keep track of which SoundEvents are still alive.
 */
public interface SoundEvent {
	
	/**
	 * Cause a SoundEvent to play. The SoundEvent is responsible for connecting itself
	 * to the output arg. The SoundEvent should also return the {@link UGen} which is its 
	 * root, i.e., a {@link UGen} which, if dead, indicates that the SoundEvent is dead.
	 * 
	 * @param output the output.
	 * @param parameters additional data.
	 * @return a {@link UGen} which is the root of the SoundEvent.
	 */
	public UGen play(UGen output, Map<String, Object> parameters);
	
}
