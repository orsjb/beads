/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

/**
 * BiquadCustomCoeffCalculator provides a mechanism to define custom filter
 * coefficients for a {@link BiquadFilter} based on frequency and Q. Users can
 * create their own coefficient calculator classes by extending this class and
 * passing it to a BiquadFilter instance with {@link BiquadFilter#setType(int)}.
 * 
 * <p>
 * An instance of such a custom class should override
 * {@link #calcCoeffs(float, float, float)} to define the coefficient
 * calculation algorithm. The floats a0, a1, a2, b0, b1, and b2 should be set
 * according to the input parameters freq, q, and gain, as well as the useful
 * class variables {@link #sampFreq} and {@link #two_pi_over_sf}.
 * </p>
 * 
 * @beads.category filter
 * @author Benito Crawford
 * @version .9.1
 */
public class BiquadCustomCoeffCalculator {
	public float a0 = 1;
	public float a1 = 0;
	public float a2 = 0;
	public float b0 = 0;
	public float b1 = 0;
	public float b2 = 0;
	/**
	 * The sampling frequency.
	 */
	protected float sampFreq;
	/**
	 * Two * pi / sampling frequency.
	 */
	protected float two_pi_over_sf;

	/**
	 * Constructor for a given sampling frequency.
	 * 
	 * @param sf
	 *            The sampling frequency, in Hertz.
	 */
	BiquadCustomCoeffCalculator(float sf) {
		setSamplingFrequency(sf);
	}

	/**
	 * Constructor with default sampling frequency of 44100.
	 */
	BiquadCustomCoeffCalculator() {
		setSamplingFrequency(44100);
	}

	/**
	 * Sets the sampling frequency.
	 * 
	 * @param sf
	 *            The sampling frequency in Hertz.
	 */
	public void setSamplingFrequency(float sf) {
		sampFreq = sf;
		two_pi_over_sf = (float) (Math.PI * 2 / sf);
	}

	/**
	 * Override this function with code that sets a0, a1, etc.&nbsp;in terms of
	 * frequency, Q, and sampling frequency.
	 * 
	 * @param freq
	 *            The frequency of the filter in Hertz.
	 * @param q
	 *            The Q-value of the filter.
	 * @param gain
	 *            The gain of the filter.
	 */
	public void calcCoeffs(float freq, float q, float gain) {
		// override with coefficient calculations
	}
}
