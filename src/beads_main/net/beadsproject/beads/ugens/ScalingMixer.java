/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * ScalingMixer scales the gain of the signal at each input by the number of {@link UGen}s connected to that input, passing the scaled signal to the corresponding output.
 *
 * @beads.category effect
 * @author ollie
 */
public class ScalingMixer extends UGen {

	/**
	 * Instantiates a new ScalingMixer.
	 * 
	 * @param context
	 *            the AudioContext.
	 */
	public ScalingMixer(AudioContext context) {
        this(context, 1);
    }
	
    /**
	 * Instantiates a new ScalingMixer.
	 * 
	 * @param context
	 *            the AudioContext.
	 * @param inouts
	 *            the number of inputs (= the number of outputs).
	 */
    public ScalingMixer(AudioContext context, int inouts) {
        super(context, inouts, inouts);
    }
    
    /* (non-Javadoc)
     * @see com.olliebown.beads.core.UGen#calculateBuffer()
     */
    @Override
    public void calculateBuffer() {
        for(int i = 0; i < ins; i++) {
            int numInputs = getNumberOfConnectedUGens(i);
            if (numInputs > 0) numInputs = 1;            
	        for(int j = 0; j < bufferSize; j++) {
	            bufOut[i][j] = bufIn[i][j] / (float)numInputs;
	        }            
        }
    }

}
