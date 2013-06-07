/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * Static represents a {@link UGen} with a fixed value. Since the value is fixed, Static doesn't actually calculate anything, and overrides the methods {@link #getValue()} and {@link #getValue(int, int)} to return its fixed value.
 *
 * @beads.category utilities
 * @author ollie
 */
public class Static extends UGen {

	/** The stored value. */
	public float x;
	
	/**
	 * Instantiates a new Static with the given value.
	 * 
	 * @param context
	 *            the AudioContext.
	 * @param x
	 *            the value.
	 */
	public Static(AudioContext context, float x) {
		super(context, 1);
		this.x = x;
		outputInitializationRegime = OutputInitializationRegime.NULL;
		outputPauseRegime = OutputPauseRegime.NULL;
		pause(true); //might as well be muted
	}

	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.UGen#calculateBuffer()
	 */
	@Override
	public void calculateBuffer() {
		// Do nothing
	}
	
	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.UGen#setValue(float)
	 */
	public void setValue(float value) {
		x = value;
	}
	
	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.UGen#getValue(int, int)
	 */
	public float getValue(int a, int b) {
		return x;	//whatever happens return x
	}
	
	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.UGen#getValue()
	 */
	public float getValue() {
		return x;
	}
	
	
	
}
