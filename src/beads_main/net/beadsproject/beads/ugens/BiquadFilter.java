/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.*;

/**
 * A simple implementation of a multi-channel biquad filter. It calculates
 * coefficients based on three parameters (frequency, Q, and gain - the latter
 * only relevant for EQ and shelving filters), each of which may be specified by
 * a static float or by the output of a UGen.
 * <p>
 * Filter parameters may be set with individual setter functions (
 * {@link #setFrequency(float) setFreq}, {@link #setQ(float) setQ}, and
 * {@link #setGain(float) setGain}), or by passing a DataBead with the
 * appropriate properties to {@link #setParams(DataBead) setParams}. (Messaging
 * the filter with a DataBead is equivalent to calling setParams.) Setter
 * methods return the instance, so they may be strung together:
 * <p>
 * <code>filt.setFreq(200).setQ(30).setGain(.4);</code>
 * <p>
 * BiquadFilterMulti can be used with pre-programmed algorithms that calculate
 * coefficients for various filter types. (See {@link #setType(int)} for a list
 * of available types.)
 * <p>
 * BiquadFilterMulti can also implement a user-defined filter algorithm by
 * calling {@link #setCustomType(CustomCoeffCalculator)}.
 * 
 * @beads.category filter
 * @author Benito Crawford
 * @version 0.9.6
 */
public class BiquadFilter extends IIRFilter implements DataBeadReceiver {

	/**
	 * Indicates a low-pass filter; coefficients are calculated from equations
	 * given in "Cookbook formulae for audio EQ biquad filter coefficients" by
	 * Robert Bristow-Johnson.
	 */
	public final static Type LP = Type.LP;

	/**
	 * Indicates a high-pass filter; coefficients are calculated from equations
	 * given in "Cookbook formulae for audio EQ biquad filter coefficients" by
	 * Robert Bristow-Johnson.
	 */
	public final static Type HP = Type.HP;

	/**
	 * Indicates a band-pass filter with constant skirt gain; coefficients are
	 * calculated from equations given in "Cookbook formulae for audio EQ biquad
	 * filter coefficients" by Robert Bristow-Johnson.
	 */
	public final static Type BP_SKIRT = Type.BP_SKIRT;

	/**
	 * Indicates a band-pass filter with constant peak gain; coefficients are
	 * calculated from equations given in "Cookbook formulae for audio EQ biquad
	 * filter coefficients" by Robert Bristow-Johnson.
	 */
	public final static Type BP_PEAK = Type.BP_PEAK;

	/**
	 * Indicates a notch (band-reject) filter; coefficients are calculated from
	 * equations given in
	 * "Cookbook formulae for audio EQ biquad filter coefficients" by Robert
	 * Bristow-Johnson.
	 */
	public final static Type NOTCH = Type.NOTCH;

	/**
	 * Indicates an all-pass filter; coefficients are calculated from equations
	 * given in "Cookbook formulae for audio EQ biquad filter coefficients" by
	 * Robert Bristow-Johnson.
	 */
	public final static Type AP = Type.AP;

	/**
	 * Indicates a peaking-EQ filter; coefficients are calculated from equations
	 * given in "Cookbook formulae for audio EQ biquad filter coefficients" by
	 * Robert Bristow-Johnson.
	 * 
	 * <em>untested!</em>
	 */
	public final static Type PEAKING_EQ = Type.PEAKING_EQ;

	/**
	 * Indicates a low-shelf filter; coefficients are calculated from equations
	 * given in "Cookbook formulae for audio EQ biquad filter coefficients" by
	 * Robert Bristow-Johnson.
	 */
	public final static Type LOW_SHELF = Type.LOW_SHELF;

	/**
	 * Indicates a high-shelf filter; coefficients are calculated from equations
	 * given in "Cookbook formulae for audio EQ biquad filter coefficients" by
	 * Robert Bristow-Johnson.
	 */
	public final static Type HIGH_SHELF = Type.HIGH_SHELF;

	/**
	 * Indicates a Butterworth low-pass filter; only the frequency parameter is
	 * relevant.
	 */
	public final static Type BUTTERWORTH_LP = Type.BUTTERWORTH_LP;

	/**
	 * Indicates a Butterworth high-pass filter; only the frequency parameter is
	 * relevant.
	 */
	public final static Type BUTTERWORTH_HP = Type.BUTTERWORTH_HP;

	/**
	 * Indicates a Bessel low-pass filter; only frequency is relevant.
	 */
	public final static Type BESSEL_LP = Type.BESSEL_LP;

	/**
	 * Indicates a Bessel high-pass filter; only frequency is relevant.
	 */
	public final static Type BESSEL_HP = Type.BESSEL_HP;

	/**
	 * Indicates a user-defined filter; see
	 * {@link #setCustomType(CustomCoeffCalculator) setCustomType}. This
	 * constant is not recognized by {@link #setType(int) setType}.
	 */
	public final static Type CUSTOM_FILTER = Type.CUSTOM_FILTER;

	public enum Type {
		LP, HP, BP_PEAK, BP_SKIRT, NOTCH, AP, PEAKING_EQ, LOW_SHELF, HIGH_SHELF, BUTTERWORTH_LP, BUTTERWORTH_HP, BESSEL_LP, BESSEL_HP, CUSTOM_FILTER
	}

	protected float a0 = 1;
	protected float a1 = 0;
	protected float a2 = 0;
	protected float b0 = 0;
	protected float b1 = 0;
	protected float b2 = 0;

	protected int channels = 1;
	protected float freq = 100, q = 1, gain = 0;
	protected Type type = null;
	protected float samplingfreq, two_pi_over_sf, pi_over_sf;
	public static final float SQRT2 = (float) Math.sqrt(2);

	// for analysis
	protected double w = 0, ampResponse = 0, phaseResponse = 0, phaseDelay = 0;
	protected double frReal = 0, frImag = 0;

	// filter memory
	protected float[] bo1m, bo2m, bi1m, bi2m;
	protected float bo1 = 0, bo2 = 0, bi1 = 0, bi2 = 0;
	protected boolean cuedInputMemory = false;
	protected boolean cuedOutputMemory = false;
	protected float[] cbo1m, cbo2m, cbi1m, cbi2m;
	protected float cbo1 = 0, cbo2 = 0, cbi1 = 0, cbi2 = 0;
	
	protected ValCalculator vc;
	protected UGen freqUGen, qUGen, gainUGen;
	protected boolean isFreqStatic, isQStatic, isGainStatic, areAllStatic;

	/**
	 * Constructor for a multi-channel low-pass biquad filter UGen with the
	 * specified number of channels.
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 */
	public BiquadFilter(AudioContext context, int channels) {
		this(context, channels, LP);
	}

	/**
	 * Constructor for a multi-channel biquad filter UGen of specified type with
	 * the specified number of channels. See {@link #setType(int) setType} for a
	 * list of supported filter types.
	 * 
	 * @param context
	 *            The AudioContext.
	 * @param channels
	 *            The number of channels.
	 * @param itype
	 *            The initial filter type, e.g. {@link #LP}, {@link #HP},
	 *            {@link #BP_SKIRT}, etc.
	 */
	public BiquadFilter(AudioContext context, int channels, Type itype) {
		super(context, channels, channels);
		this.channels = super.getOuts();
		bi1m = new float[this.channels];
		bi2m = new float[this.channels];
		bo1m = new float[this.channels];
		bo2m = new float[this.channels];
		samplingfreq = context.getSampleRate();
		two_pi_over_sf = (float) (Math.PI * 2 / samplingfreq);
		pi_over_sf = (float) (Math.PI / samplingfreq);
		setType(itype);
		setFrequency(freq).setQ(q).setGain(gain);
	}

	/**
	 * Constructor for a multi-channel biquad filter UGen with the specified
	 * number of channels and parameters specified by a DataBead.
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 * @param params
	 *            A DataBead specifying parameter values; see
	 *            {@link #setParams(DataBead)}.
	 */
	public BiquadFilter(AudioContext context, int channels, DataBead params) {
		this(context, channels, LP);
		setParams(params);
	}

	/**
	 * Constructor for a multi-channel biquad filter UGen of specified type,
	 * with the specified number of channels, and with parameters specified by a
	 * DataBead.
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 * @param itype
	 *            The initial filter type, e.g. {@link #LP}, {@link #HP},
	 *            {@link #BP_SKIRT}, etc.
	 * @param params
	 *            A DataBead specifying parameter values; see
	 *            {@link #setParams(DataBead)}.
	 */
	public BiquadFilter(AudioContext context, int channels, Type itype,
			DataBead params) {
		this(context, channels, itype);
		setParams(params);
	}

	/**
	 * Constructor for frequency and Q as floats. See {@link #setType(int)
	 * setType} for a list of supported filter types.
	 * 
	 * @param context
	 *            The AudioContext.
	 * @param itype
	 *            The initial filter type, e.g. {@link #LP}, {@link #HP},
	 *            {@link #BP_SKIRT}, etc.
	 * @param ifreq
	 *            The initial frequency.
	 * @param iqval
	 *            The initial Q-value.
	 */
	public BiquadFilter(AudioContext context, Type itype, float ifreq,
			float iqval) {
		this(context, 1, itype);
		setFrequency(ifreq).setQ(iqval);
	}

	/**
	 * Constructor for frequency as a UGen and Q as a float. See
	 * {@link #setType(int) setType} for a list of supported filter types.
	 * 
	 * @param context
	 *            The AudioContext.
	 * @param itype
	 *            The initial filter type, {@link #LP}, {@link #HP},
	 *            {@link #BP_SKIRT}, etc.
	 * @param ifreq
	 *            The frequency UGen.
	 * @param iqval
	 *            The initial Q-value.
	 */
	public BiquadFilter(AudioContext context, Type itype, UGen ifreq, float iqval) {
		this(context, 1, itype);
		setFrequency(ifreq).setQ(iqval);
	}

	/**
	 * Constructor for frequency as a float and Q as a UGen. See
	 * {@link #setType(int) setType} for a list of supported filter types.
	 * 
	 * @param context
	 *            The AudioContext.
	 * @param itype
	 *            The initial filter type, e.g. {@link #LP}, {@link #HP},
	 *            {@link #BP_SKIRT}, etc.
	 * @param ifreq
	 *            The initial frequency.
	 * @param iqval
	 *            The Q-value UGen.
	 */
	public BiquadFilter(AudioContext context, Type itype, float ifreq, UGen iqval) {
		this(context, 1, itype);
		setFrequency(ifreq).setQ(iqval);
	}

	/**
	 * Constructor for frequency and Q as UGens. See {@link #setType(int)
	 * setType} for a list of supported filter types.
	 * 
	 * @param context
	 *            The AudioContext.
	 * @param itype
	 *            The initial filter type, e.g. {@link #LP}, {@link #HP},
	 *            {@link #BP_SKIRT}, etc.
	 * @param ifreq
	 *            The frequency UGen.
	 * @param iqval
	 *            The Q-value UGen.
	 */
	public BiquadFilter(AudioContext context, Type itype, UGen ifreq, UGen iqval) {
		this(context, 1, itype);
		setFrequency(ifreq).setQ(iqval);
	}

	private void checkStaticStatus() {
		if (isFreqStatic && isQStatic && isGainStatic) {
			areAllStatic = true;
			vc.calcVals();
		} else {
			areAllStatic = false;
		}
	}

	@Override
	public void calculateBuffer() {

		float[] bi, bo;

		if (channels == 1) {
			
			if(cuedInputMemory) {
				bi1 = cbi1;
				bi2 = cbi2;
				cuedInputMemory = false;
			}
			if(cuedOutputMemory) {
				bo1 = cbo1;
				bo2 = cbo2;
				cuedOutputMemory = false;
			}

			bi = bufIn[0];
			bo = bufOut[0];

			if (areAllStatic) {

				// first two samples
				bo[0] = (b0 * bi[0] + b1 * bi1 + b2 * bi2 - a1 * bo1 - a2 * bo2)
						/ a0;
				bo[1] = (b0 * bi[1] + b1 * bi[0] + b2 * bi1 - a1 * bo[0] - a2
						* bo1)
						/ a0;

				// main loop
				for (int currsamp = 2; currsamp < bufferSize; currsamp++) {
					bo[currsamp] = (b0 * bi[currsamp] + b1 * bi[currsamp - 1]
							+ b2 * bi[currsamp - 2] - a1 * bo[currsamp - 1] - a2
							* bo[currsamp - 2])
							/ a0;
				}

			} else {

				freqUGen.update();
				qUGen.update();
				gainUGen.update();

				// first two samples
				freq = freqUGen.getValue(0, 0);
				q = qUGen.getValue(0, 0);
				gain = gainUGen.getValue(0, 0);
				vc.calcVals();
				bo[0] = (b0 * bi[0] + b1 * bi1 + b2 * bi2 - a1 * bo1 - a2 * bo2)
						/ a0;

				freq = freqUGen.getValue(0, 1);
				q = qUGen.getValue(0, 1);
				gain = gainUGen.getValue(0, 1);
				vc.calcVals();
				bo[1] = (b0 * bi[1] + b1 * bi[0] + b2 * bi1 - a1 * bo[0] - a2
						* bo1)
						/ a0;

				// main loop
				for (int currsamp = 2; currsamp < bufferSize; currsamp++) {
					freq = freqUGen.getValue(0, currsamp);
					q = qUGen.getValue(0, currsamp);
					gain = gainUGen.getValue(0, currsamp);
					vc.calcVals();

					bo[currsamp] = (b0 * bi[currsamp] + b1 * bi[currsamp - 1]
							+ b2 * bi[currsamp - 2] - a1 * bo[currsamp - 1] - a2
							* bo[currsamp - 2])
							/ a0;
				}

			}

			// get 2 samples of "memory" between sample vectors
			bi1 = bi[bufferSize - 1];
			bi2 = bi[bufferSize - 2];
			bo1 = bo[bufferSize - 1];
			bo2 = bo[bufferSize - 2];

			// check to make sure filter didn't blow up
			if (Float.isNaN(bo1))
				reset();

		} else {
			// multi-channel version
			
			if(cuedInputMemory) {
				for(int i = 0; i < channels; i++) {
					bi1m[i] = cbi1m[i];
					bi2m[i] = cbi2m[i];
				}
				cuedInputMemory = false;
			}
			if(cuedOutputMemory) {
				for(int i = 0; i < channels; i++) {
					bo1m[i] = cbo1m[i];
					bo2m[i] = cbo2m[i];
				}
				cuedOutputMemory = false;
			}


			if (areAllStatic) {

				for (int i = 0; i < channels; i++) {
					bi = bufIn[i];
					bo = bufOut[i];

					// first two samples
					bo[0] = (b0 * bi[0] + b1 * bi1m[i] + b2 * bi2m[i] - a1
							* bo1m[i] - a2 * bo2m[i])
							/ a0;
					bo[1] = (b0 * bi[1] + b1 * bi[0] + b2 * bi1m[i] - a1
							* bo[0] - a2 * bo1m[i])
							/ a0;

					// main loop
					for (int currsamp = 2; currsamp < bufferSize; currsamp++) {

						bo[currsamp] = (b0 * bi[currsamp] + b1
								* bi[currsamp - 1] + b2 * bi[currsamp - 2] - a1
								* bo[currsamp - 1] - a2 * bo[currsamp - 2])
								/ a0;
					}

					// get 2 samples of "memory" between sample vectors
					bi2m[i] = bi[bufferSize - 2];
					bi1m[i] = bi[bufferSize - 1];
					bo2m[i] = bo[bufferSize - 2];

					// and check to make sure filter didn't blow up
					if (Float.isNaN(bo1m[i] = bo[bufferSize - 1]))
						reset();

				}

			} else {

				freqUGen.update();
				qUGen.update();
				gainUGen.update();

				// first two samples
				freq = freqUGen.getValue(0, 0);
				q = qUGen.getValue(0, 0);
				gain = gainUGen.getValue(0, 0);
				vc.calcVals();

				for (int i = 0; i < channels; i++) {
					bufOut[i][0] = (b0 * bufIn[i][0] + b1 * bi1m[i] + b2
							* bi2m[i] - a1 * bo1m[i] - a2 * bo2m[i])
							/ a0;
				}

				freq = freqUGen.getValue(0, 1);
				q = qUGen.getValue(0, 1);
				gain = gainUGen.getValue(0, 1);
				vc.calcVals();
				for (int i = 0; i < channels; i++) {
					bufOut[i][1] = (b0 * bufIn[i][1] + b1 * bufIn[i][0] + b2
							* bi1m[i] - a1 * bufOut[i][0] - a2 * bo1m[i])
							/ a0;
				}

				// main loop
				for (int currsamp = 2; currsamp < bufferSize; currsamp++) {
					freq = freqUGen.getValue(0, currsamp);
					q = qUGen.getValue(0, currsamp);
					gain = gainUGen.getValue(0, currsamp);
					vc.calcVals();

					for (int i = 0; i < channels; i++) {
						bufOut[i][currsamp] = (b0 * bufIn[i][currsamp] + b1
								* bufIn[i][currsamp - 1] + b2
								* bufIn[i][currsamp - 2] - a1
								* bufOut[i][currsamp - 1] - a2
								* bufOut[i][currsamp - 2])
								/ a0;
					}

				}

				for (int i = 0; i < channels; i++) {
					// get 2 samples of "memory" between sample vectors
					bi2m[i] = bufIn[i][bufferSize - 2];
					bi1m[i] = bufIn[i][bufferSize - 1];
					bo2m[i] = bufOut[i][bufferSize - 2];

					// and check to make sure filter didn't blow up
					if (Float.isNaN(bo1m[i] = bufOut[i][bufferSize - 1]))
						reset();
				}

			}
		}

	}

	/**
	 * Resets the filter in case it "explodes".
	 */
	public void reset() {
		for (int i = 0; i < channels; i++) {
			bi1m[i] = 0;
			bi2m[i] = 0;
			bo1m[i] = 0;
			bo2m[i] = 0;
		}
		bi1 = 0;
		bi2 = 0;
		bo1 = 0;
		bo2 = 0;
	}

	protected class ValCalculator {
		public void calcVals() {
		};
	}

	private class LPValCalculator extends ValCalculator {
		public void calcVals() {
			float w = two_pi_over_sf * freq;
			float cosw = (float) Math.cos(w);
			float a = (float) Math.sin(w) / q * .5f;
			b1 = 1 - cosw;
			b2 = b0 = b1 * .5f;
			a0 = 1 + a;
			a1 = -2 * cosw;
			a2 = 1 - a;
		}
	}

	private class HPValCalculator extends ValCalculator {
		public void calcVals() {
			float w = two_pi_over_sf * freq;
			float cosw = (float) Math.cos(w);
			float a = (float) Math.sin(w) / q * .5f;
			b1 = -1 - cosw;
			b2 = b0 = b1 * -.5f;
			a0 = 1 + a;
			a1 = -2 * cosw;
			a2 = 1 - a;
		}
	}

	private class BPSkirtValCalculator extends ValCalculator {
		public void calcVals() {
			float w = two_pi_over_sf * freq;
			float sinw = (float) Math.sin(w);
			float a = sinw / q * .5f;
			b1 = 0;
			b2 = 0 - (b0 = sinw * .5f);
			a0 = 1 + a;
			a1 = -2 * (float) Math.cos(w);
			a2 = 1 - a;
		}
	}

	private class BPPeakValCalculator extends ValCalculator {
		public void calcVals() {
			float w = two_pi_over_sf * freq;
			// float a = (float) Math.sin(w) / q * .5f;
			b1 = 0;
			b2 = 0 - (b0 = (float) Math.sin(w) / q * .5f);
			a0 = 1 + b0;
			a1 = -2 * (float) Math.cos(w);
			a2 = 1 - b0;
		}
	}

	private class NotchValCalculator extends ValCalculator {
		public void calcVals() {
			float w = two_pi_over_sf * freq;
			float a = (float) Math.sin(w) / q * .5f;
			b2 = b0 = 1;
			a1 = b1 = -2 * (float) Math.cos(w);
			a0 = 1 + a;
			a2 = 1 - a;
		}
	}

	private class APValCalculator extends ValCalculator {
		public void calcVals() {
			float w = two_pi_over_sf * freq;
			float a = (float) (Math.sin(w) / q * .5);
			a2 = b0 = 1 - a;
			a1 = b1 = (float) (-2 * Math.cos(w));
			a0 = b2 = 1 + a;
		}
	}

	private class PeakingEQValCalculator extends ValCalculator {
		public void calcVals() {
			float A = (float) Math.pow(10, gain * .025);
			float w = two_pi_over_sf * freq;
			// float cosw = (float) Math.cos(w);
			float a = (float) (Math.sin(w) / q * .5);
			b2 = 2 - (b0 = 1 + a * A);
			a1 = b1 = -2 * (float) Math.cos(w);
			a2 = 2 - (a0 = 1 + a / A);
			/*
			 * peakingEQ: H(s) = (s^2 + s*(A/Q) + 1) / (s^2 + s/(A*Q) + 1)
			 * 
			 * b0 = 1 + alpha*A b1 = -2*cos(w0) b2 = 1 - alpha*A a0 = 1 +
			 * alpha/A a1 = -2*cos(w0) a2 = 1 - alpha/A
			 */
		}
	}

	private class LowShelfValCalculator extends ValCalculator {
		public void calcVals() {
			float A = (float) Math.pow(10, gain * .025);
			float w = two_pi_over_sf * freq;
			float cosw = (float) Math.cos(w);
			float a = (float) (Math.sin(w) / q * .5);
			float b = 2 * a * (float) Math.sqrt(A);
			float c = (A - 1) * cosw;
			b0 = A * (A + 1 - c + b);
			b1 = 2 * A * ((A - 1) - (A + 1) * cosw);
			b2 = A * (A + 1 - c - b);
			a0 = A + 1 + c + b;
			a1 = -2 * ((A - 1) + (A + 1) * cosw);
			a2 = A + 1 + c - b;
			/*
			 * lowShelf: H(s) = A * (s^2 + (sqrt(A)/Q)*s + A)/(A*s^2 +
			 * (sqrt(A)/Q)*s + 1)
			 * 
			 * b0 = A*( (A+1) - (A-1)*cos(w0) + 2*sqrt(A)*alpha ) b1 = 2*A*(
			 * (A-1) - (A+1)*cos(w0) ) b2 = A*( (A+1) - (A-1)*cos(w0) -
			 * 2*sqrt(A)*alpha ) a0 = (A+1) + (A-1)*cos(w0) + 2*sqrt(A)*alpha a1
			 * = -2*( (A-1) + (A+1)*cos(w0) ) a2 = (A+1) + (A-1)*cos(w0) -
			 * 2*sqrt(A)*alpha
			 */
		}
	}

	private class HighShelfValCalculator extends ValCalculator {
		public void calcVals() {
			float A = (float) Math.pow(10, gain * .025);
			float w = two_pi_over_sf * freq;
			float cosw = (float) Math.cos(w);
			float a = (float) (Math.sin(w) / q * .5);
			float b = 2 * a * (float) Math.sqrt(A);
			float c = (A - 1) * cosw;

			b0 = A * (A + 1 + c + b);
			b1 = -2 * A * (A - 1 + (A + 1) * cosw);
			b2 = A * (A + 1 + c - b);
			a0 = A + 1 - c + b;
			a1 = 2 * (A - 1 - (A + 1) * cosw);
			a2 = A + 1 - c - b;
			/*
			 * highShelf: H(s) = A * (A*s^2 + (sqrt(A)/Q)*s + 1)/(s^2 +
			 * (sqrt(A)/Q)*s + A)
			 * 
			 * b0 = A*( (A+1) + (A-1)*cos(w0) + 2*sqrt(A)*alpha ) b1 = *
			 * -2*A*((A-1) + (A+1)*cos(w0) ) b2 = A*( (A+1) + (A-1)*cos(w0) -
			 * 2*sqrt(A)*alpha ) a0 = (A+1) - (A-1)*cos(w0) + 2*sqrt(A)*alpha a1
			 * = 2*( (A-1) - (A+1)*cos(w0) ) a2 = (A+1) - (A-1)*cos(w0) -
			 * 2*sqrt(A)*alpha
			 */
		}
	}

	private class ButterworthLPValCalculator extends ValCalculator {
		public void calcVals() {
			float k = (float) Math.tan(freq * pi_over_sf);
			b0 = b2 = k * k;
			b1 = 2f * b0;
			a0 = b0 + (SQRT2 * k) + 1;
			a1 = 2f * (b0 - 1);
			a2 = b0 - (SQRT2 * k) + 1;
			// System.out.println(k + "^2 = " + k2);
		}
	}

	private class ButterworthHPValCalculator extends ValCalculator {
		public void calcVals() {
			float k = (float) Math.tan(freq * pi_over_sf);
			float k2p1 = k * k + 1;
			b0 = b2 = 1;
			b1 = -2;
			a0 = k2p1 + (SQRT2 * k);
			a1 = 2f * (k2p1 - 2);
			a2 = k2p1 - (SQRT2 * k);

		}
	}

	// same as BP_PEAK! but less efficient...
	@SuppressWarnings("unused")
	private class ButterworthBPValCalculator extends ValCalculator {
		public void calcVals() {
			float hbw = pi_over_sf * .5f * freq / q;
			float root = (float) Math.sqrt(1 + 4 * q * q);
			float k1 = (float) Math.tan(hbw * (root - 1));
			float k2 = (float) Math.tan(hbw * (root + 1));
			float mp1 = k1 * k2 + 1;
			b2 = -(b0 = k2 - k1);
			b1 = 0;
			a0 = mp1 + b0;
			a1 = 2 * (mp1 - 2);
			a2 = mp1 - b0;
		}
	}

	private class BesselLPValCalculator extends ValCalculator {
		public void calcVals() {
			float w = (float) Math.tan(pi_over_sf * freq);
			b2 = b0 = 3 * w * w;
			b1 = 2 * b0;
			a0 = 1 + 3 * w + b0;
			a1 = -2 + b1;
			a2 = 1 - 3 * w + b0;
		}
	}

	private class BesselHPValCalculator extends ValCalculator {
		public void calcVals() {
			float w = (float) Math.tan(pi_over_sf * freq);
			float w2 = w * w;
			b2 = b0 = 3;
			b1 = -6;
			a0 = w2 + 3 * w + 3;
			a1 = 2 * w2 - 6;
			a2 = w2 - 3 * w + 3;
		}
	}

	/**
	 * The coeffiecent calculator that interfaces with a
	 * {@link CustomCoeffCalculator} to allow user-defined filter algorithms.
	 * 
	 * @author benito
	 * @version .9
	 */
	private class CustomValCalculator extends ValCalculator {
		CustomCoeffCalculator ccc;

		CustomValCalculator(CustomCoeffCalculator iccc) {
			ccc = iccc;
		}

		public void calcVals() {
			ccc.calcCoeffs(freq, q, gain);
			a0 = ccc.a0;
			a1 = ccc.a1;
			a2 = ccc.a2;
			b0 = ccc.b0;
			b1 = ccc.b1;
			b2 = ccc.b2;
		}
	}

	/**
	 * Sets the filter parameters with a DataBead.
	 * <p>
	 * Use the following properties to specify filter parameters:
	 * </p>
	 * <ul>
	 * <li>"filterType": (int) The filter type.</li>
	 * <li>"frequency": (float or UGen) The filter frequency.</li>
	 * <li>"q": (float or UGen) The filter Q-value.</li>
	 * <li>"gain": (float or UGen) The filter gain.</li>
	 * </ul>
	 * 
	 * @param paramBead
	 *            The DataBead specifying parameters.
	 * @return This filter instance.
	 */
	public BiquadFilter setParams(DataBead paramBead) {
		if (paramBead != null) {
			Object o;

			o = paramBead.get("type");
			if (o instanceof Number) {
				setType(((Number) o).intValue());
			} else if (o instanceof Type) {
				setType((Type) o);
			}

			if ((o = paramBead.get("frequency")) != null) {
				if (o instanceof UGen) {
					setFrequency((UGen) o);
				} else {
					setFrequency(paramBead.getFloat("frequency", freq));
				}
			}

			if ((o = paramBead.get("q")) != null) {
				if (o instanceof UGen) {
					setQ((UGen) o);
				} else {
					setQ(paramBead.getFloat("q", q));
				}
			}

			if ((o = paramBead.get("gain")) != null) {
				if (o instanceof UGen) {
					setGain((UGen) o);
				} else {
					setGain(paramBead.getFloat("gain", gain));
				}
			}
		}
		return this;
	}

	public void messageReceived(Bead message) {
		if (message instanceof DataBead) {
			setParams((DataBead) message);
		}
	}

	/**
	 * Equivalent to {@link #setParams(DataBead)}.
	 * 
	 * @return This filter instance.
	 */
	public DataBeadReceiver sendData(DataBead db) {
		setParams(db);
		return this;
	}

	/**
	 * Gets a DataBead with the filter's parameters (whether float or UGen),
	 * stored in the keys "frequency", "q", "gain", and "filterType".
	 * 
	 * @return The DataBead with stored parameters.
	 */
	public DataBead getParams() {
		DataBead db = new DataBead();

		if (isFreqStatic) {
			db.put("frequency", freq);
		} else {
			db.put("frequency", freqUGen);
		}

		if (isQStatic) {
			db.put("q", q);
		} else {
			db.put("q", qUGen);
		}

		if (isGainStatic) {
			db.put("gain", gain);
		} else {
			db.put("gain", gainUGen);
		}

		db.put("type", type);

		return db;
	}

	/**
	 * Gets a DataBead with properties "frequency", "q", and "gain" set to their
	 * current float values and "type" set appropriately.
	 * 
	 * @return The DataBead with static float parameter values.
	 */
	public DataBead getStaticParams() {
		DataBead db = new DataBead();
		db.put("frequency", freq);
		db.put("q", q);
		db.put("gain", gain);
		db.put("type", type);
		return db;
	}

	/**
	 * Sets the type of filter. To set a custom type, use
	 * {@link #setCustomType(CustomCoeffCalculator) setCustomType}. The
	 * following types are recognized:
	 * <ul>
	 * <li>{@link #LP} - Low-pass filter.</li>
	 * <li>{@link #HP} - High-pass filter.</li>
	 * <li>{@link #BP_SKIRT} - Band-pass filter with constant skirt gain.</li>
	 * <li>{@link #BP_PEAK} - Band-pass filter with constant peak gain.</li>
	 * <li>{@link #NOTCH} - Notch (band-reject) filter.</li>
	 * <li>{@link #AP} - All-pass filter.</li>
	 * <li>{@link #PEAKING_EQ} - Peaking-EQ filter.</li>
	 * <li>{@link #LOW_SHELF} - Low-shelf filter.</li>
	 * <li>{@link #HIGH_SHELF} - High-shelf filter.</li>
	 * <li>{@link #BUTTERWORTH_LP} - Butterworth low-pass filter.</li>
	 * <li>{@link #BUTTERWORTH_HP} - Butterworth high-pass filter.</li>
	 * <li>{@link #BESSEL_LP} - Bessel low-pass filter.</li>
	 * <li>{@link #BESSEL_HP} - Bessel high-pass filter.</li>
	 * </ul>
	 * 
	 * @param ntype
	 *            The type of filter.
	 */
	public BiquadFilter setType(Type ntype) {
		if (ntype != type || vc == null) {
			Type t = type;
			type = ntype;
			switch (type) {
			case LP:
				vc = new LPValCalculator();
				break;
			case HP:
				vc = new HPValCalculator();
				break;
			case BP_SKIRT:
				vc = new BPSkirtValCalculator();
				break;
			case BP_PEAK:
				vc = new BPPeakValCalculator();
				break;
			case NOTCH:
				vc = new NotchValCalculator();
				break;
			case AP:
				vc = new APValCalculator();
				break;
			case PEAKING_EQ:
				vc = new PeakingEQValCalculator();
				break;
			case LOW_SHELF:
				vc = new LowShelfValCalculator();
				break;
			case HIGH_SHELF:
				vc = new HighShelfValCalculator();
				break;
			case BUTTERWORTH_LP:
				vc = new ButterworthLPValCalculator();
				break;
			case BUTTERWORTH_HP:
				vc = new ButterworthHPValCalculator();
				break;
			case BESSEL_LP:
				vc = new BesselLPValCalculator();
				break;
			case BESSEL_HP:
				vc = new BesselHPValCalculator();
				break;
			default:
				type = t;
				break;
			}
			vc.calcVals();
		}
		return this;
	}

	/**
	 * Sets the type of filter with an integer. This method is deprecated and
	 * has been kept for backwards-compatibility reasons only. {
	 * {@link #setType(Type)} should be used.
	 * <ul>
	 * <li>0 - Low-pass filter.</li>
	 * <li>1 - High-pass filter.</li>
	 * <li>2 - Band-pass filter with constant skirt gain.</li>
	 * <li>3 - Band-pass filter with constant peak gain.</li>
	 * <li>4 - Notch (band-reject) filter.</li>
	 * <li>5 - All-pass filter.</li>
	 * <li>6 - Peaking-EQ filter.</li>
	 * <li>7 - Low-shelf filter.</li>
	 * <li>8 - High-shelf filter.</li>
	 * <li>9 - Butterworth low-pass filter.</li>
	 * <li>10 - Butterworth high-pass filter.</li>
	 * <li>11 - Bessel low-pass filter.</li>
	 * <li>12 - Bessel high-pass filter.</li>
	 * </ul>
	 * 
	 * @param ntype
	 *            The type of filter.
	 * @deprecated Use {@link #setType(Type)}.
	 */
	@Deprecated
	public BiquadFilter setType(int ntype) {
		Type n = null;
		switch (ntype) {
		case 0:
			n = LP;
			break;
		case 1:
			n = HP;
			break;
		case 2:
			n = BP_SKIRT;
			break;
		case 3:
			n = BP_PEAK;
			break;
		case 4:
			n = NOTCH;
			break;
		case 5:
			n = AP;
			break;
		case 6:
			n = PEAKING_EQ;
			break;
		case 7:
			n = LOW_SHELF;
			break;
		case 8:
			n = HIGH_SHELF;
			break;
		case 9:
			n = BUTTERWORTH_LP;
			break;
		case 10:
			n = BUTTERWORTH_HP;
			break;
		case 11:
			n = BESSEL_LP;
			break;
		case 12:
			n = BESSEL_HP;
			break;
		case 100:
			n = CUSTOM_FILTER;
			break;
		}

		if (n != type || vc == null) {
			Type t = type;
			type = n;
			switch (type) {
			case LP:
				vc = new LPValCalculator();
				break;
			case HP:
				vc = new HPValCalculator();
				break;
			case BP_SKIRT:
				vc = new BPSkirtValCalculator();
				break;
			case BP_PEAK:
				vc = new BPPeakValCalculator();
				break;
			case NOTCH:
				vc = new NotchValCalculator();
				break;
			case AP:
				vc = new APValCalculator();
				break;
			case PEAKING_EQ:
				vc = new PeakingEQValCalculator();
				break;
			case LOW_SHELF:
				vc = new LowShelfValCalculator();
				break;
			case HIGH_SHELF:
				vc = new HighShelfValCalculator();
				break;
			case BUTTERWORTH_LP:
				vc = new ButterworthLPValCalculator();
				break;
			case BUTTERWORTH_HP:
				vc = new ButterworthHPValCalculator();
				break;
			case BESSEL_LP:
				vc = new BesselLPValCalculator();
				break;
			case BESSEL_HP:
				vc = new BesselHPValCalculator();
				break;
			default:
				type = t;
				break;
			}
			vc.calcVals();
		}
		return this;
	}

	/**
	 * Gets the type of the filter.
	 * 
	 * @return The filter type.
	 * @see #setType(Type)
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Gets the current filter frequency.
	 * 
	 * @return The filter frequency.
	 */
	public float getFrequency() {
		return freq;
	}

	/**
	 * Sets the filter frequency to a float value. This will remove the
	 * frequency UGen, if there is one.
	 * 
	 * @param freq
	 *            The frequency.
	 */
	public BiquadFilter setFrequency(float freq) {
		this.freq = freq;
		if (isFreqStatic) {
			freqUGen.setValue(freq);
		} else {
			freqUGen = new Static(context, freq);
			isFreqStatic = true;
			checkStaticStatus();
		}
		vc.calcVals();
		return this;
	}

	/**
	 * Sets a UGen to determine the filter frequency.
	 * 
	 * @param freqUGen
	 *            The frequency UGen.
	 */
	public BiquadFilter setFrequency(UGen freqUGen) {
		if (freqUGen == null) {
			setFrequency(freq);
		} else {
			this.freqUGen = freqUGen;
			freqUGen.update();
			freq = freqUGen.getValue();
			isFreqStatic = false;
			areAllStatic = false;
		}
		vc.calcVals();
		return this;
	}

	/**
	 * Gets the frequency UGen, if there is one.
	 * 
	 * @return The frequency UGen.
	 */
	public UGen getFrequencyUGen() {
		if (isFreqStatic == true) {
			return null;
		} else {
			return freqUGen;
		}
	}

	/**
	 * Gets the current filter frequency.
	 * 
	 * @return The filter frequency.
	 * @deprecated Use {@link #getFrequency()}.
	 */
	@Deprecated
	public float getFreq() {
		return getFrequency();
	}

	/**
	 * Sets the filter frequency to a float value. This will remove the
	 * frequency UGen, if there is one.
	 * 
	 * @param freq
	 *            The frequency.
	 * @deprecated Use {@link #setFrequency(float)}.
	 */
	@Deprecated
	public BiquadFilter setFreq(float freq) {
		return setFrequency(freq);
	}

	/**
	 * Sets a UGen to determine the filter frequency.
	 * 
	 * @param freqUGen
	 *            The frequency UGen.
	 * @deprecated Use {@link #setFrequency(UGen)}.
	 */
	@Deprecated
	public BiquadFilter setFreq(UGen freqUGen) {
		return setFrequency(freqUGen);
	}

	/**
	 * Gets the frequency UGen, if there is one.
	 * 
	 * @return The frequency UGen.
	 * @deprecated Use {@link #getFrequencyUGen()}.
	 */
	@Deprecated
	public UGen getFreqUGen() {
		return getFrequencyUGen();
	}

	/**
	 * Sets the filter Q-value to a float. This will remove the Q UGen if there
	 * is one.
	 * 
	 * @param nqval
	 *            The Q-value.
	 */
	public BiquadFilter setQ(float nqval) {
		q = nqval;
		if (isQStatic) {
			qUGen.setValue(nqval);
		} else {
			qUGen = new Static(context, nqval);
			isQStatic = true;
			checkStaticStatus();
		}
		vc.calcVals();
		return this;
	}

	/**
	 * Sets a UGen to determine the filter Q-value.
	 * 
	 * @param nqval
	 *            The Q-value UGen.
	 * @return This BiquadFilter instance.
	 */
	public BiquadFilter setQ(UGen nqval) {
		if (nqval == null) {
			setQ(q);
		} else {
			qUGen = nqval;
			qUGen.update();
			q = freqUGen.getValue();
			isQStatic = false;
			areAllStatic = false;
		}
		vc.calcVals();
		return this;
	}

	/**
	 * Gets the current Q-value for the filter.
	 * 
	 * @return The current Q-value.
	 */
	public float getQ() {
		return q;
	}

	/**
	 * Gets the Q UGen, if there is one.
	 * 
	 * @return The Q UGen.
	 */
	public UGen getQUGen() {
		if (isQStatic) {
			return null;
		} else {
			return qUGen;
		}
	}

	/**
	 * Sets the filter gain to a float. This will remove the gain UGen if there
	 * is one. (Only relevant for {@link #PEAKING_EQ}, {@link #LOW_SHELF}, and
	 * {@link #HIGH_SHELF} types.)
	 * 
	 * @param ngain
	 *            The gain in decibels (0 means no gain).
	 */
	public BiquadFilter setGain(float ngain) {
		gain = ngain;
		if (isGainStatic) {
			gainUGen.setValue(ngain);
		} else {
			gainUGen = new Static(context, ngain);
			isGainStatic = true;
			checkStaticStatus();
		}
		vc.calcVals();
		return this;
	}

	/**
	 * Sets a UGen to determine the filter Q-value. (Only relevant for
	 * {@link #PEAKING_EQ}, {@link #LOW_SHELF}, and {@link #HIGH_SHELF} types.)
	 * 
	 * @param ngain
	 *            The gain UGen, specifying the gain in decibels.
	 */
	public BiquadFilter setGain(UGen ngain) {
		if (ngain == null) {
			setGain(gain);
		} else {
			gainUGen = ngain;
			gainUGen.update();
			gain = freqUGen.getValue();
			isGainStatic = false;
			areAllStatic = false;
		}
		vc.calcVals();
		return this;
	}

	/**
	 * Gets the current gain in decibels for the filter. (Only relevant for
	 * {@link #PEAKING_EQ}, {@link #LOW_SHELF}, and {@link #HIGH_SHELF} types.)
	 * 
	 * @return The current gain.
	 */
	public float getGain() {
		return gain;
	}

	/**
	 * Gets the gain UGen, if there is one.
	 * 
	 * @return The gain UGen.
	 */
	public UGen getGainUGen() {
		if (isGainStatic) {
			return null;
		} else {
			return gainUGen;
		}
	}
		
	public BiquadFilter loadMemory(float xm1, float xm2, float ym1, float ym2) {
		loadInputMemory(xm1, xm2);
		loadOutputMemory(ym1, ym2);
		return this;
	}
	
	public BiquadFilter loadMemory(float[] xm1, float[] xm2, float[] ym1, float[] ym2) {
		loadInputMemory(xm1, xm2);
		loadOutputMemory(ym1, ym2);
		return this;
	}
	
	public BiquadFilter loadInputMemory(float xm1, float xm2) {
		if(channels == 1) {
			bi1 = xm1;
			bi2 = xm2;
		} else {
			for(int i = 0; i < channels; i++) {
				bi1m[i] = xm1;
				bi2m[i] = xm2;
			}
		}
		cuedInputMemory = true;
		return this;
	}
	
	public BiquadFilter loadInputMemory(float[] xm1, float[] xm2) {
		int min = Math.min(xm1.length, xm2.length);
		if(channels == 1 && min > 0) {
			bi1 = xm1[0];
			bi2 = xm2[0];
			cuedInputMemory = true;
		} else {
			for(int i = 0; i < Math.min(channels, min); i++) {
				cbi1m[i] = xm1[i];
				cbi2m[i] = xm2[i];
				cuedInputMemory = true;
			}
		}
		return this;
	}
	
	public BiquadFilter loadOutputMemory(float ym1, float ym2) {
		if(channels == 1) {
			cbo1 = ym1;
			cbo2 = ym2;
		} else {
			for(int i = 0; i < channels; i++) {
				cbo1m[i] = ym1;
				cbo2m[i] = ym2;
			}
		}
		cuedOutputMemory = true;
		return this;
	}

	public BiquadFilter loadOutputMemory(float[] ym1, float[] ym2) {
		int min = Math.min(ym1.length, ym2.length);
		if(channels == 1 && min > 0) {
			bo1 = ym1[0];
			bo2 = ym2[0];
			cuedOutputMemory = true;
		} else {
			for(int i = 0; i < Math.min(channels, min); i++) {
				bo1m[i] = ym1[i];
				bo2m[i] = ym2[i];
				cuedOutputMemory = true;
			}
		}
		return this;
	}

	
	/**
	 * Gets an array of the current filter coefficients: {a0, a1, a2, b0, b1,
	 * b2}.
	 * 
	 * @return The coefficient array.
	 */
	public float[] getCoefficients() {
		return new float[] { a0, a1, a2, b0, b1, b2 };
	}

	/**
	 * Gets an array filled with the filter response characteristics: {frequency
	 * response (real), frequency response (imaginary), amplitude response,
	 * phase response, phase delay, group delay}.
	 * 
	 * @param freq
	 *            The frequency to test.
	 * @return The array.
	 */
	public IIRFilterAnalysis getFilterResponse(float freq) {
		return calculateFilterResponse(new float[] { b0, b1, b2 }, new float[] {
				a0, a1, a2 }, freq, samplingfreq);
	}

	/**
	 * Sets a user-defined coefficient calculation algorithm. The algorithm is
	 * defined in a user-defined class that extends
	 * {@link CustomCoeffCalculator}.
	 * 
	 * @param cc
	 *            The custom coefficient calculator.
	 */

	public BiquadFilter setCustomType(CustomCoeffCalculator cc) {
		vc = new CustomValCalculator(cc);
		vc.calcVals();
		return this;
	}

	/**
	 * CustomCoeffCalculator provides a mechanism to define custom filter
	 * coefficients for a biquad filter based on frequency and Q. Users can
	 * create their own coefficient calculator classes by extending this class
	 * and passing it to a BiquadFilterMulti instance with
	 * {@link BiquadFilter#setCustomType(CustomCoeffCalculator) setCustomType}.
	 * 
	 * <p>
	 * An instance of such a custom class should override
	 * {@link #calcCoeffs(float, float, float)} to define the coefficient
	 * calculation algorithm. The floats a0, a1, a2, b0, b1, and b2 should be
	 * set according to the input parameters freq, q, and gain, as well as the
	 * useful class variables {@link #sampFreq} and {@link #two_pi_over_sf}.
	 * </p>
	 * 
	 * @author Benito Crawford
	 * @version .9.1
	 */
	public class CustomCoeffCalculator {
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
		CustomCoeffCalculator(float sf) {
			setSamplingFrequency(sf);
		}

		/**
		 * Constructor with default sampling frequency of 44100.
		 */
		CustomCoeffCalculator() {
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
		 * Override this function with code that sets a0, a1, etc.&nbsp;in terms
		 * of frequency, Q, and sampling frequency.
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

}
