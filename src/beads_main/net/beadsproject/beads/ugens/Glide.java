/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * 
 * Simple UGen that ramps between given values over a given duration (e.g., for portamento).
 * 
 * @beads.category control
 * @author ollie
 */
public class Glide extends UGen {

	private float currentValue;
	private float previousValue;
	private float targetValue;
	private int glideTime; //in samples
	private int countSinceGlide;
	private boolean gliding;
	private boolean nothingChanged;
	
	/**
	 * Creates a new Glide with the specified AudioContext, initial value and glide time in milliseconds.
	 * @param context 
	 * 				the AudioContext.
	 * @param currentValue 
	 * 				the initial value.
	 * @param glideTimeMS 
	 * 				the glide time in milliseconds.
	 */
	public Glide(AudioContext context, float currentValue, float glideTimeMS) {
		super(context, 1);
		this.currentValue = currentValue;
		countSinceGlide = 0;
		gliding = false;
		nothingChanged = false;
		outputInitializationRegime = OutputInitializationRegime.RETAIN;
		outputPauseRegime = OutputPauseRegime.RETAIN;
		bufOut[0] = new float[bufferSize];
		setGlideTime(glideTimeMS);
	}
	
	/**
	 * Creates a new Glide with the specified AudioContext, initial value. Uses the 
	 * default glide time of 100 milliseconds.
	 * @param context 
	 * 				the AudioContext.
	 * @param currentValue 
	 * 				the initial value.
	 */
	public Glide(AudioContext context, float currentValue) {
		this(context, currentValue, 100);
	}
	
	/**
	 * Creates a new Glide with the specified AudioContext. Uses the 
	 * default inital value of zero and glide time of 100 milliseconds.
	 * @param context 
	 * 				the AudioContext.
	 */
	public Glide(AudioContext context) {
		this(context, 0f);
	}

	/** 
	 * Sets the target glide value. From its current value Glide immediately interpolates
	 * its way to that value over the specified glideTime.
	 * @param targetValue the target value.
	 */
	public void setValue(float targetValue) {
		this.targetValue = targetValue;
		gliding = true;
		nothingChanged = false;
		countSinceGlide = 0;
		previousValue = currentValue;
	}
	
	/**
	 * Resets the Glide's current value to the specified value immediately.
	 * @param targetValue the target value.
	 */
	public void setValueImmediately(float targetValue) {
		currentValue = targetValue;
		gliding = false;
		nothingChanged = false;
		countSinceGlide = 0;
	}
	
	/**
	 * Sets the glide time in milliseconds immediately.
	 * @param msTime glide time in milliseconds.
	 */
	public void setGlideTime(float msTime) {
		glideTime = (int)context.msToSamples(msTime);
	}
	
	/**
	 * Gets the glide time in milliseconds.
	 * @return the glide time in milliseconds.
	 */
	public float getGlideTime() {
		return (float)context.samplesToMs(glideTime);
	}

	@Override
	public void calculateBuffer() {
		if(!nothingChanged) {
			nothingChanged = true;
			for(int i = 0; i < bufferSize; i++) {
				if(gliding) {
					if(glideTime <= 0f) {
						gliding = false;
						bufOut[0][i] = previousValue = currentValue = targetValue;
						nothingChanged = false;
					} else if(countSinceGlide > glideTime) {
						gliding = false;
						bufOut[0][i] = previousValue = targetValue;
					} else {
						float offset = ((float)countSinceGlide / glideTime);
						bufOut[0][i] = currentValue = offset * targetValue + (1f - offset) * previousValue;
						nothingChanged = false;
					}
					countSinceGlide++;
				} else {
					bufOut[0][i] = currentValue;
				}
			}
		}
	}

}
