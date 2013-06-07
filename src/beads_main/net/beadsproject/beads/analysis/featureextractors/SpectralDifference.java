/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.analysis.featureextractors;

import net.beadsproject.beads.analysis.FeatureExtractor;
import net.beadsproject.beads.core.TimeStamp;

/**
 * SpectralDifference calculates the spectral difference between one frame and the next.
 * 
 * @beads.category analysis
 */
public class SpectralDifference extends FeatureExtractor<Float, float[]> {

	public enum DifferenceType {
		POSITIVERMS, RMS, POSITIVEMEANDIFFERENCE, MEANDIFFERENCE
	};

	private float[] previousSpectrum;
	private float minFreq;
	private float maxFreq;
	private float sampleRate;
	private DifferenceType differenceType = DifferenceType.POSITIVEMEANDIFFERENCE;

	// cached size of last block
	private int lastBlockSize = 0;

	// derived: minBin, maxBin are cached each time the fft block size changes
	private int minBin;
	private int maxBin;

	/**
	 * Create a spectral difference feature extractor of the entire spectrum.
	 * 
	 * @param samplerate
	 *            The sample rate of the AudioContext
	 */
	public SpectralDifference(float samplerate) {
		this(samplerate, 0, samplerate);
	}

	/**
	 * Create a spectral difference feature extractor with a specific frequency
	 * window.
	 * 
	 * @param samplerate
	 *            The sample rate of the AudioContext
	 * @param minf
	 *            The lower frequency of the window
	 * @param maxf
	 *            The upper frequency of the window
	 */
	public SpectralDifference(float samplerate, float minf, float maxf) {
		minFreq = minf;
		maxFreq = maxf;
		sampleRate = samplerate;
	}

	/**
	 * Specify a window of the spectrum to analyse. By default the entire
	 * spectrum is analysed.
	 * 
	 * @param minf
	 *            The lower frequency
	 * @param maxf
	 *            The upper frequency
	 */
	public void setFreqWindow(float minf, float maxf) {
		minFreq = minf;
		maxFreq = maxf;
	}

	public void setDifferenceType(DifferenceType dt) {
		differenceType = dt;
	}

	@Override
	public void process(TimeStamp startTime, TimeStamp endTime, float[] spectrum) {
		// compare this spectrum with the last
		int numBins = maxBin - minBin;
		// 1. check to see if we need to create a new previousSpectrum cache
		if (lastBlockSize != spectrum.length) {
			// 2. calculate min and maxBin
			calcMaxAndMinBin(spectrum.length);

			// 3. create a new spectrum cache of the appropriate size
			numBins = maxBin - minBin;
			if (numBins > 0)
				previousSpectrum = new float[numBins];

			lastBlockSize = spectrum.length;
		}

		if (numBins > 0) {
			switch (differenceType) {
			case POSITIVERMS:
				features = positiveRms(spectrum, minBin, previousSpectrum, 0,
						numBins);
				break;
			case RMS:
				features = rms(spectrum, minBin, previousSpectrum, 0, numBins);
				break;
			case POSITIVEMEANDIFFERENCE:
				features = positiveMeanDifference(spectrum, minBin,
						previousSpectrum, 0, numBins);
				break;
			case MEANDIFFERENCE:
				features = meanDifference(spectrum, minBin, previousSpectrum,
						0, numBins);
				break;
			}

			// finally copy the current spectrum
			System.arraycopy(spectrum, minBin, previousSpectrum, 0, numBins);
		}
		forward(startTime, endTime);
	}

	// helper functions
	/**
	 * Computes the Root-Mean-Squared of two arrays
	 * 
	 * @param arr1
	 *            first array
	 * @param i1
	 *            index of first element
	 * @param arr2
	 *            second array
	 * @param i2
	 *            index of second element
	 * @param length
	 *            length of arrays
	 * @return
	 */
	private float rms(float arr1[], int i1, float arr2[], int i2, int length) {
		float value = 0f;
		for (int i = 0; i < length; i++) {
			float thisDiff = arr1[i1 + i] - arr2[i2 + i];
			value += thisDiff * thisDiff;
		}
		return (float) Math.sqrt(value / length);
	}

	/**
	 * Computes the Root-Mean-Squared of two arrays (only summing the positive
	 * differences)
	 * 
	 * @param arr1
	 *            first array
	 * @param i1
	 *            index of first element
	 * @param arr2
	 *            second array
	 * @param i2
	 *            index of second element
	 * @param length
	 *            length of arrays
	 * @return
	 */
	private float positiveRms(float arr1[], int i1, float arr2[], int i2,
			int length) {
		float value = 0f;
		for (int i = 0; i < length; i++) {
			float thisDiff = arr1[i1 + i] - arr2[i2 + i];
			if (thisDiff >= 0)
				value += thisDiff * thisDiff;
		}
		return (float) Math.sqrt(value / length);
	}

	/**
	 * Computes the mean difference of two arrays
	 * 
	 * @param arr1
	 *            first array
	 * @param i1
	 *            index of first element
	 * @param arr2
	 *            second array
	 * @param i2
	 *            index of second element
	 * @param length
	 *            length of arrays
	 * @return
	 */
	private float meanDifference(float arr1[], int i1, float arr2[], int i2,
			int length) {
		float value = 0f;
		for (int i = 0; i < length; i++) {
			value += arr1[i1 + i] - arr2[i2 + i];
		}
		return value / length;
	}

	/**
	 * Computes the mean difference of two arrays (only considering the positive
	 * differences)
	 * 
	 * @param arr1
	 *            first array
	 * @param i1
	 *            index of first element
	 * @param arr2
	 *            second array
	 * @param i2
	 *            index of second element
	 * @param length
	 *            length of arrays
	 * @return
	 */
	private float positiveMeanDifference(float arr1[], int i1, float arr2[],
			int i2, int length) {
		float value = 0f;
		for (int i = 0; i < length; i++) {
			if (arr1[i1 + i] > arr2[i2 + i])
				value += arr1[i1 + i] - arr2[i2 + i];
		}
		return value / length;
	}

	private void calcMaxAndMinBin(int blocksize) {
		minBin = Math.min(blocksize - 1, Math.max(0, Math.round(FFT.binNumber(
				sampleRate, blocksize, minFreq))));
		maxBin = Math.min(blocksize - 1, Math.max(0, Math.round(FFT.binNumber(
				sampleRate, blocksize, maxFreq))));
	}

}
