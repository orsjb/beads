/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.*;

/**
 * An abstract class that provides methods for analyzing infinite impulse
 * response (IIR) filters. IIR filters built on this class should implement
 * {@link #getFilterResponse(float)} appropriately.
 * 
 * @beads.category filter
 * @author Benito Crawford
 * @version 0.9.5
 */
public abstract class IIRFilter extends UGen {

	public IIRFilter(AudioContext context, int ins, int outs) {
		super(context, ins, outs);
	}

	public abstract IIRFilterAnalysis getFilterResponse(float freq);

	/**
	 * Gets the filter's amplitude response at the specified frequency.
	 * 
	 * @param freq
	 *            The frequency to evaluate.
	 * @return The amplitude response.
	 */
	public float getAmplitudeResponse(float freq) {
		return (float) getFilterResponse(freq).amplitudeResponse;
	}

	/**
	 * Gets the filter's phase response at the specified frequency.
	 * 
	 * @param freq
	 *            The frequency to evaluate.
	 * @return The phase response.
	 */
	public float getPhaseResponse(float freq) {
		return (float) getFilterResponse(freq).phaseResponse;
	}

	/**
	 * Gets the filter's phase delay at the specified frequency.
	 * 
	 * @param freq
	 *            The frequency to evaluate.
	 * @return The phase delay.
	 */
	public float getPhaseDelay(float freq) {
		return (float) getFilterResponse(freq).phaseDelay;
	}

	/**
	 * Gets an estimate of the filter's group delay at the specified frequency.
	 * 
	 * @param freq
	 *            The frequency to evaluate.
	 * @return The group delay.
	 */
	public float getGroupDelay(float freq) {
		return (float) getFilterResponse(freq).groupDelay;
	}

	/**
	 * Gets an IIRFilterAnalysis object filled with the filter response
	 * characteristics for the specified frequency: frequency response (real),
	 * frequency response (imaginary), amplitude response, phase response, phase
	 * delay, group delay.
	 * 
	 * @param bs
	 *            The b coefficients (corresponding to the x(n) terms of the
	 *            filter algorithm.
	 * @param as
	 *            The a coefficients (corresponding to the y(n) terms of the
	 *            filter algorithm.
	 * @param freq
	 *            The frequency to evaluate.
	 * @param samplingFreq
	 *            The sampling frequency.
	 * @return The IIRFilterAnalysis object.
	 */
	public static IIRFilterAnalysis calculateFilterResponse(float[] bs,
			float[] as, float freq, float samplingFreq) {

		IIRFilterAnalysis ret = analyzeFilter(bs, as, freq, samplingFreq);
		ret.groupDelay = calculateGroupDelay(bs, as, freq, samplingFreq);
		return ret;
	}

	protected static double calculateGroupDelay(float[] bs, float[] as,
			float freq, float samplingFreq) {
		IIRFilterAnalysis a = analyzeFilter(bs, as, freq - .01f, samplingFreq);
		IIRFilterAnalysis b = analyzeFilter(bs, as, freq + .01f, samplingFreq);
		return ((b.phaseResponse - a.phaseResponse) / (a.w - b.w));
	}

	/**
	 * Does our analysis at the specified frequency.
	 * 
	 * @param freq
	 *            The frequency to analyze.
	 */
	protected static IIRFilterAnalysis analyzeFilter(float[] bs, float[] as,
			float freq, float samplingFreq) {

		double w = -2 * freq * Math.PI / samplingFreq;

		double nr = 0, ni = 0, dr = 1, di = 0;

		if (bs.length > 0) {
			nr = bs[0];
			for (int i = 1; i < bs.length; i++) {
				nr += bs[i] * Math.cos(w * i);
				ni += bs[i] * Math.sin(w * i);
			}
		}
		if (as.length > 0) {
			dr = as[0];
			for (int i = 1; i < as.length; i++) {
				dr += as[i] * Math.cos(w * i);
				di += as[i] * Math.sin(w * i);
			}
		}

		double md2 = dr * dr + di * di;

		IIRFilterAnalysis ret = new IIRFilterAnalysis();

		ret.amplitudeResponse = Math.sqrt((nr * nr + ni * ni) / md2);
		ret.phaseResponse = (Math.atan2(ni, nr) - Math.atan2(di, dr));
		ret.phaseDelay = (ret.phaseResponse / Math.PI / -2.0 / freq);

		ret.frReal = (nr * dr + ni * di) / md2;
		ret.frImag = (ni * dr - nr * di) / md2;
		ret.w = w;

		return ret;
	}

	/**
	 * A holder class for the various filter analysis data.
	 * 
	 * @author Benito Crawford
	 * @version 0.9.5
	 */
	public static class IIRFilterAnalysis {
		public double frReal = 0, frImag = 0, amplitudeResponse = 0,
				phaseResponse = 0, phaseDelay = 0, groupDelay = 0, w = 0;
	}

}
