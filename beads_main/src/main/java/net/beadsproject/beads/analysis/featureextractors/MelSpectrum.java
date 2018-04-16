/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 * CREDIT: This class uses portions of code taken from MEAP. See readme/CREDITS.txt.
 */
package net.beadsproject.beads.analysis.featureextractors;

import net.beadsproject.beads.analysis.FeatureExtractor;
import net.beadsproject.beads.core.TimeStamp;

/**
 * MelSpectrum receives spectral data from a {@link PowerSpectrum} object and 
 * converts it to the mel frequency spectrum. To use MelSpectrum, make sure it 
 * is set as a listener to a {@link PowerSpectrum} object, not directly from an audio stream.
 * 
 * @beads.category analysis
 */
public class MelSpectrum extends FeatureExtractor<float[], float[]>  {

	//TODO be able to specify max in min frequencies
	//TODO work out a nice filter so that the spectrum is empty when zero sound comes in
	
	public static final float LOG10 = (float)Math.log(10.0);
	
	/** The sample rate in samples per second. */
	private final float sampleRate;
	
	/** The size of incoming power spectrum data. */
	private int bufferSize;
	
	/** Array of mel spectrum bin centres. */
	private double[] melCenter;

	/** Array of mel spectrum bin widths. */
	private double[] melWidth;

	/** The mel of lin. */
	private double[] melOfLin;

	/** Hard frequency maximum. */
	private double hardMax;

	/**
	 * Instantiates a new MelSpectrum.
	 * 
	 * @param sampleRate
	 *            the sample rate in samples per second.
	 * @param numCoeffs
	 *            the number of filters to use (number of features).
	 */
	public MelSpectrum(float sampleRate, int numCoeffs) {
		this.sampleRate = sampleRate;
		setNumberOfFeatures(numCoeffs);
		hardMax = 8000.0;
	}
	
	/**
	 * Builds the filterbank. Only needs to be called if the size of the input array or the number of features changes.
	 */
	private void setup() {
		int twiceBufferSize = bufferSize * 2;
		features = new float[numFeatures];
		// Calculate the locations of the bin centers on the mel scale and
		// as indices into the input vector
		melCenter = new double[numFeatures + 2];
		melWidth = new double[numFeatures + 2];
		double melMin = lin2mel(0);
		double melMax = lin2mel((hardMax < sampleRate / 2) ? hardMax : sampleRate / 2); // dpwe 2006-12-11 - hard maximum
		double hzPerBin = sampleRate / 2 / twiceBufferSize;
		for (int i = 0; i < numFeatures + 2; i++) {
			melCenter[i] = melMin + i * (melMax - melMin) / (numFeatures + 1);
		}
		for (int i = 0; i < numFeatures + 1; i++) {
			melWidth[i] = melCenter[i + 1] - melCenter[i];
			double linbinwidth = (mel2lin(melCenter[i + 1]) - mel2lin(melCenter[i]))
					/ hzPerBin;
			if (linbinwidth < 1) {
				melWidth[i] = lin2mel(mel2lin(melCenter[i]) + hzPerBin) - melCenter[i];
			}
			if(melWidth[i] == 0f) System.out.println("zero melwidth");
		}
		// precalculate mel translations of fft bin frequencies
		melOfLin = new double[twiceBufferSize];
		for (int i = 0; i < twiceBufferSize; i++) {
			melOfLin[i] = lin2mel(i * sampleRate / (2 * twiceBufferSize));
			if(Double.isInfinite(melOfLin[i])) System.out.println("infinte meloflin");
		}
		featureDescriptions = new String[numFeatures];
		for (int i = 0; i < numFeatures; i++) {
			if(i < 9) featureDescriptions[i] = "mel0" + (i + 1);
			else featureDescriptions[i] = "mel" + (i + 1);
		}
	}

	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.PowerSpectrumListener#calculateFeatures(float[])
	 */
	public void process(TimeStamp startTime, TimeStamp endTime, float[] powerSpectrum) {
		if(powerSpectrum.length != bufferSize) {
			bufferSize = powerSpectrum.length;
			setup();
		}
		float[] linSpec = new float[powerSpectrum.length];
		// convert log magnitude to linear magnitude for binning
		for (int band = 0; band < linSpec.length; band++) {
//			linSpec[band] = (float) Math.pow(10f, powerSpectrum[band] / 10f);
			linSpec[band] = powerSpectrum[band]; //mod by Ollie -- is the PowerSpectrum already linear?
		}
		// convert to mel scale
		for (int bin = 0; bin < features.length; bin++) {
			// initialize
			features[bin] = 0;
			for (int i = 0; i < linSpec.length; ++i) {
				double weight = 1.0 - (Math.abs(melOfLin[i] - melCenter[bin]) / melWidth[bin]);
				if (weight > 0) {
					features[bin] += weight * linSpec[i];
				}
			}
			// Take log
			features[bin] = Math.max(0f, (float)(10f * Math.log(features[bin]) / LOG10));
		}
		forward(startTime, endTime);
	}
	
	public void setNumberOfFeatures(int numFeatures) {
		super.setNumberOfFeatures(numFeatures);
		bufferSize = -1;
	}

	/**
	 * Converts from linear frequency to mel scale.
	 * 
	 * @param fq
	 *            the frequency in hz.
	 * 
	 * @return value on the mel scale.
	 */
	private static double lin2mel(double fq) {
		return 1127.0 * Math.log(1.0 + fq / 700.0);
	}

	/**
	 * Converts from mel scale to linear frequency.
	 * 
	 * @param mel
	 *            the mel scale value.
	 * 
	 * @return the frequency in hz.
	 */
	private static double mel2lin(double mel) {
		return 700.0 * (Math.exp(mel / 1127.0) - 1.0);
	}

	public double getFreqForBin(int bin) {
		if(melCenter != null) {
			return mel2lin(melCenter[bin]); 	//TODO proper test
		} else {
			return Double.NaN;
		}
	}
	
	public int getBinForFreq(double freq) {
		if(melCenter != null) {
			double mel = lin2mel(freq);
			int i;
			for(i = 0; i < melCenter.length; i++) {
				if(mel <= melCenter[i]) {
					break;
				}
			}
			if(i >= numFeatures) return numFeatures - 1;
			return i;
		} else {
			return 0;
		}
	}

}
