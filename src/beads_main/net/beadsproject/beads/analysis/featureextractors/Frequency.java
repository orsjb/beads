/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 * CREDIT: This class uses portions of code taken from MEAP. See readme/CREDITS.txt.
 */
package net.beadsproject.beads.analysis.featureextractors;

import net.beadsproject.beads.analysis.FeatureExtractor;
import net.beadsproject.beads.core.TimeStamp;

/**
 * Frequency processes spectral data forwarded to it by a {@link PowerSpectrum} 
 * to determine the best estimate for the frequency of the current signal.
 * 
 * @beads.category analysis
 */
public class Frequency extends FeatureExtractor<Float, float[]> {

	/** The Constant FIRSTBAND. */
	static final int FIRSTBAND = 3;
	
	/** The ratio bin2hz. */
	private float bin2hz;
	
	private int bufferSize;
	
	private float sampleRate;
	
	/**
	 * Instantiates a new Frequency.
	 * 
	 * @param sampleRate The sample rate of the audio context 
	 */
	public Frequency(float sampleRate) {
		bufferSize = -1;
		this.sampleRate = sampleRate;
		features = null;
	}
	
	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.PowerSpectrumListener#calculateFeatures(float[])
	 */
	public synchronized void process(TimeStamp startTime, TimeStamp endTime, float[] powerSpectrum) {
		if(bufferSize != powerSpectrum.length) {
			bufferSize = powerSpectrum.length;
			bin2hz = sampleRate / (2 * bufferSize);
		}
		features = null;
		// now pick best peak from linspec
		double pmax = -1;
		int maxbin = 0;
		for (int band = FIRSTBAND; band < powerSpectrum.length; band++) {			
			double pwr = powerSpectrum[band];
			if (pwr > pmax) {
				pmax = pwr;
				maxbin = band;
			}
		}
		// cubic interpolation
		double yz = powerSpectrum[maxbin];
		double ym = maxbin <= 0? powerSpectrum[maxbin] : powerSpectrum[maxbin - 1];
		double yp = maxbin < powerSpectrum.length - 1 ? powerSpectrum[maxbin + 1] : powerSpectrum[maxbin];
		double k = (yp + ym) / 2 - yz;
		double x0 = (ym - yp) / (4 * k);
		features = (float)(bin2hz * (maxbin + x0));
		
		forward(startTime,endTime);
	}

	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.FrameFeatureExtractor#getFeatureDescriptions()
	 */
	public String[] getFeatureDescriptions() {
		return new String[]{"frequency"};
	}


}
