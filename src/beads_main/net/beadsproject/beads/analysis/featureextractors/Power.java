/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.analysis.featureextractors;

import net.beadsproject.beads.analysis.FeatureExtractor;
import net.beadsproject.beads.core.TimeStamp;

/**
 * Power calculates the RMS power over a frame directly from an audio signal.
 * 
 * @beads.category analysis
 */
public class Power extends FeatureExtractor<Float, float[]>  {

	/**
	 * Instantiates a new Power.
	 */
	public Power() {
	}

	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.FrameFeatureExtractor#getFeatureDescriptions()
	 */
	public String[] getFeatureDescriptions() {
		return new String[] {"Power"};
	}

	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.FrameFeatureExtractor#getNumFeatures()
	 */
	public int getNumberOfFeatures() {
		return 1;
	}

	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.FrameFeatureExtractor#setNumFeatures(int)
	 */
	public void setNumberOfFeatures(int numFeatures) {
		//not allowed
	}

	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.UGen#calculateBuffer()
	 */
	@Override
	public void process(TimeStamp startTime, TimeStamp endTime, float[] audioData) {
		features = 0.0f;
		for(int i = 0; i < audioData.length; i++) {
			features = features + audioData[i] * audioData[i];
		}
		features = (float)Math.sqrt(features / (float)audioData.length);
	}

}
