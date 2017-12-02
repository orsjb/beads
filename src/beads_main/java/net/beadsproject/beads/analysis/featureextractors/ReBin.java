/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.analysis.featureextractors;

import net.beadsproject.beads.analysis.FeatureExtractor;
import net.beadsproject.beads.core.TimeStamp;

/**
 * ReBin takes an array of float data and places the data into a smaller array, the size of which is specified by the number of features.
 */
public class ReBin extends FeatureExtractor<float[], float[]> {
	
	/**
	 * Instantiates a new ReBin.
	 * 
	 * @param numFeatures the number of features.
	 */
	public ReBin(int numFeatures) {
		setNumberOfFeatures(numFeatures);	
	}
	
	@Override
	public void setNumberOfFeatures(int nf) {
		super.setNumberOfFeatures(nf);
		features = new float[numFeatures];
	}	
	
	/* (non-Javadoc)
	 * @see net.beadsproject.beads.analysis.FeatureExtractor#process(java.lang.Object)
	 */
	@Override
	public void process(TimeStamp startTime, TimeStamp endTime, float[] original) {
		//features = new float[numFeatures];
		if(original != null) {
			float scale = (float)original.length / (float)features.length;
			for(int i = 0; i < original.length; i++) {
				features[(int)(i / scale)] += original[i];
			}
			for(int i = 0; i < features.length; i++) {
				features[i] /= scale;
				if(Float.isNaN(features[i])) features[i] = 0f;
			}
		}
		forward(startTime, endTime);
	}

}
