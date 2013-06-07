/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
//Much code taken from MEAP

package net.beadsproject.beads.analysis.featureextractors;

import net.beadsproject.beads.analysis.FeatureExtractor;
import net.beadsproject.beads.core.TimeStamp;

/**
 * SpectralCentroid calculates the spectral centroid of a signal. It should be set up to listen to a {@link PowerSpectrum} object.
 * 
 * @beads.category analysis
 */
public class SpectralCentroid extends FeatureExtractor<Float, float[]>  {

	/** The sample rate in samples per second. */
	private float sampleRate;
	
	/**
	 * Instantiates a new SpectralCentroid.
	 * 
	 * @param sampleRate
	 *            the sample rate in samples per second.
	 */
	public SpectralCentroid(float sampleRate) {
		this.sampleRate = sampleRate;
	}

	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.PowerSpectrumListener#calculateFeatures(float[])
	 */
	public void process(TimeStamp startTime, TimeStamp endTime, float[] powerSpectrum) {
		double num = 0;
		double den = 0;
		num = 0;
		den = 0;
		for (int band = 0; band < powerSpectrum.length; band++) {
			double freqCenter = band * (sampleRate / 2)
					/ (powerSpectrum.length - 1);
			// convert back to linear power
			double p = Math.pow(10, powerSpectrum[band] / 10);
			num += freqCenter * p;
			den += p;
		}
		features = (float) (num / den);
		forward(startTime,endTime);
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

}
