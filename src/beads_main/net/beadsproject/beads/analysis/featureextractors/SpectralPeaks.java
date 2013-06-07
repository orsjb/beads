/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 * CREDIT: This class uses portions of code taken from MEAP. See readme/CREDITS.txt.
 */
package net.beadsproject.beads.analysis.featureextractors;

import net.beadsproject.beads.analysis.FeatureExtractor;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.TimeStamp;

/**
 * Peaks finds the strongest N peaks in a signal passed from a {@link PowerSpectrum}, 
 * where N is the specified number of features. Peaks must be set as a listener to 
 * a {@link PowerSpectrum} object to work properly.
 * 
 * Its output is an array of [frequency, power] tuples. 
 * 
 * @beads.category analysis
 */
public class SpectralPeaks extends FeatureExtractor<float[][], float[]>  {

	/** The Constant FIRSTBAND. */
	static final int FIRSTBAND = 3;
	
	/** internal constants */
	private float bin2hz;
	private float samplingRate;
	
	/**
	 * Instantiates a new Peaks.
	 * 
	 * @param context
	 *            the AudioContext.
	 */
	public SpectralPeaks(AudioContext context) {
		this(context, 10);
	}
	
	/**
	 * Instantiates a new Peaks with the given number of features.
	 * 
	 * @param context
	 *            the AudioContext.
	 * @param numFeatures
	 *            the number of features.
	 */
	public SpectralPeaks(AudioContext context, int numFeatures) {
		bin2hz = -1;		
		samplingRate = context.getSampleRate();
		setNumberOfFeatures(numFeatures);
	}
	
	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.PowerSpectrumListener#calculateFeatures(float[])
	 */
	public synchronized void process(TimeStamp startTime, TimeStamp endTime, float[] powerSpectrum) {
		if (bin2hz < 0)
		{
			bin2hz = samplingRate/(2*powerSpectrum.length-1);
		}
		
		// collect average linear spectrum
		double[] linSpec = new double[powerSpectrum.length];
		for (int band = 0; band < linSpec.length; band++) {
//			linSpec[band] = Math.pow(10, powerSpectrum[band] / 10);
			linSpec[band] = powerSpectrum[band];
		}
		// now pick best peaks from linspec
		for(int i = 0; i < features.length; i++) {
			double pmax = -1;
			int maxbin = 0;
			for (int band = FIRSTBAND; band < powerSpectrum.length; band++) {
				//double pwr = pitchWt[band]*linSpec[band];
				double pwr = linSpec[band];
				if (pwr > pmax) {
					pmax = pwr;
					maxbin = band;
				}
			}
			// cubic interpolation
			double yz = linSpec[maxbin];
			double ym = maxbin <= 0? linSpec[maxbin] : linSpec[maxbin - 1];
			double yp = maxbin < linSpec.length - 1 ? linSpec[maxbin + 1] : linSpec[maxbin];
			double k = (yp + ym) / 2 - yz;
			double x0 = (ym - yp) / (4 * k);
			//double c = yz - k * Math.pow(x0, 2);
			
			features[i][0] = (float)(bin2hz * (maxbin + x0));
			features[i][1] = (float)(pmax);
			features[i][1] = (float)Math.sqrt(features[i][1] / powerSpectrum.length);
			
			if(Float.isNaN(features[i][0]) || features[i][0] < 0f)
			{
				features[i][0] = 0f;
				features[i][1] = 0f;
			}
			
			linSpec[maxbin] = -1f;
		}
		forward(startTime, endTime);
	}
	
	public float[][] getFeatures() {
		float[][] original = (float[][])features;
		float[][] copy = new float[original.length][original[0].length];
		for(int i = 0; i < copy.length; i++) {
			for(int j = 0; j < copy[i].length; j++) {
				copy[i][j] = original[i][j];
			}
		}
		return copy;
	}

	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.FrameFeatureExtractor#setNumFeatures(int)
	 */
	public void setNumberOfFeatures(int numFeatures) {
		features = new float[numFeatures][2];
		
		featureDescriptions = new String[numFeatures];
		for(int i = 0; i < numFeatures; i++) {
			featureDescriptions[i] = "peak" + i;
		}
	}

}
