/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.analysis.featureextractors;

import net.beadsproject.beads.analysis.FeatureExtractor;
import net.beadsproject.beads.core.TimeStamp;

/**
 * PowerSpectrum calculates the power spectrum from the output of {@link FFT}. 
 * PowerSpectrum forwards the full power spectrum data to its listeners.
 * 
 * @beads.category analysis
 */
public class PowerSpectrum extends FeatureExtractor<float[], float[][]> {
	
	/**
	 * Instantiates a new PowerSpectrum.
	 */
	public PowerSpectrum() {
	}
	
	/* (non-Javadoc)
	 * @see com.olliebown.beads.analysis.FFT#calculateBuffer()
	 */
	public void process(TimeStamp startTime, TimeStamp endTime, float[][] data) {
		if(features == null || features.length != data[0].length / 2) {
			features = new float[data[0].length / 2];
		}
		for(int i = 0; i < features.length; i++) {		
			features[i] = (float)(data[0][i] * data[0][i] + data[1][i] * data[1][i]);
		}
		//update the listeners
		forward(startTime, endTime);
	}

}
