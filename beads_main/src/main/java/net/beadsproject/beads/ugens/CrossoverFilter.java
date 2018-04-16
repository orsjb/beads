/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.DataBead;
import net.beadsproject.beads.data.DataBeadReceiver;

/**
 * A multi-channel 4th-order Linkwitz-Riley crossover filter. For each input
 * channel, the filter outputs both a low-pass and a high-pass channel. If the
 * filter has two inputs, for example, then it will output four channels:
 * <p>
 * <ul>
 * <li>0: Low-pass of input 0</li>
 * <li>1: High-pass of input 0</li>
 * <li>2: Low-pass of input 1</li>
 * <li>3: High-pass of input 1</li>
 * </ul>
 * <p>
 * A key feature of Linkwitz-Riley filters is that the low- and high-pass bands
 * added together produce a flat frequency response, making them particularly
 * useful as crossover filters. A 4th-order version is equivalent to cascading
 * two identical 2nd-order Butterworth filters.
 * 
 * @beads.category filter
 * @author Benito Crawford
 * @version 0.9.5
 * 
 */
public class CrossoverFilter extends UGen implements DataBeadReceiver {

	// filter coefficients
	private float a0, a1, a2, lb0, lb1; // lb2, hb0, hb1, hb2;
	// for calculating the filter coefficients
	private float k;
	// useful
	public static final float SQRT2 = (float) Math.sqrt(2);
	private float sr = 41000, freq = 120, pi_sr;
	private UGen freqUGen;

	// for single channel - memory
	private float x1 = 0, x2 = 0;
	private float ly0 = 0, ly1 = 0, ly2 = 0;
	private float lz1 = 0, lz2 = 0;
	private float hy0 = 0, hy1 = 0, hy2 = 0;
	private float hz1 = 0, hz2 = 0;

	// for multi-channel - memory
	private float[][] xms, lyms, lzms, hyms, hzms;

	private int channels;

	// for buggy 4th-order implementation
	// public float la3, la4, ha3, ha4, nik4, b3, b4;
	// public float ly3 = 0, ly4 = 0, hy3 = 0, hy4 = 0, x3 = 0, x4 = 0;
	// public float nr2k, nr2k3;

	/**
	 * Constructor for a one-channel crossover filter, initialized with a cutoff
	 * frequency of 800Hz.
	 * 
	 * @param context
	 *            The audio context.
	 */
	public CrossoverFilter(AudioContext context) {
		this(context, 1, 800);
	}

	/**
	 * Constructor for a crossover filter with the specified number of channels,
	 * initialized with a cutoff frequency of 800Hz.
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 */
	public CrossoverFilter(AudioContext context, int channels) {
		this(context, channels, 800);
	}

	/**
	 * Constructor for a crossover filter with the specified number of channels,
	 * set at the specified frequency.
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 * @param freq
	 *            The initial cutoff frequency.
	 */
	public CrossoverFilter(AudioContext context, int channels, float freq) {
		super(context, channels, channels * 2);
		this.channels = channels;
		if (channels > 1) {
			xms = new float[channels][3];
			lyms = new float[channels][3];
			lzms = new float[channels][3];
			hyms = new float[channels][3];
			hzms = new float[channels][3];
		}
		sr = context.getSampleRate();
		pi_sr = (float) (Math.PI / sr);
		setFrequency(freq);
	}

	@Override
	public void calculateBuffer() {

		float[] bi, lo, hi;

		if (freqUGen != null) {
			freqUGen.update();
			freq = freqUGen.getValue();
			calcVals();
		}

		if (channels > 1) {
			for (int chan = 0; chan < channels; chan++) {
				bi = bufIn[chan];
				lo = bufOut[chan * 2];
				hi = bufOut[chan * 2 + 1];

				float[] ly = lyms[chan];
				float[] lz = lzms[chan];
				float[] hy = hyms[chan];
				float[] hz = hzms[chan];
				float[] x = xms[chan];

				for (int i = 0; i < bufferSize; i++) {

					// low-pass part - two identical butterworth filters
					ly[0] = (lb0 * (bi[i] + x[2]) + lb1 * x[1] - a1 * ly[1] - a2
							* ly[2])
							/ a0;
					lo[i] = (lb0 * (ly[0] + ly[2]) + lb1 * ly[1] - a1 * lz[1] - a2
							* lz[2])
							/ a0;

					// optimized high-pass part - two identical butterworth
					// filters
					hy[0] = (bi[i] - 2f * x[1] + x[2] - a1 * hy[1] - a2 * hy[2])
							/ a0;
					hi[i] = (hy[0] - 2f * hy[1] + hy[2] - a1 * hz[1] - a2
							* hz[2])
							/ a0;

					// remember everything
					x[2] = x[1];
					x[1] = bi[i];

					ly[2] = ly[1];
					ly[1] = ly[0];
					lz[2] = lz[1];
					lz[1] = lo[i];

					hy[2] = hy[1];
					hy[1] = hy[0];
					hz[2] = hz[1];
					hz[1] = hi[i];

				}
			}
		} else {
			// single channel means fewer arrays to deal with
			bi = bufIn[0];
			lo = bufOut[0];
			hi = bufOut[1];

			for (int i = 0; i < bufferSize; i++) {

				// low-pass part - two identical butterworth filters
				ly0 = (lb0 * (bi[i] + x2) + lb1 * x1 - a1 * ly1 - a2 * ly2)
						/ a0;
				lo[i] = (lb0 * (ly0 + ly2) + lb1 * ly1 - a1 * lz1 - a2 * lz2)
						/ a0;

				// optimized high-pass part - two identical butterworth
				// filters
				hy0 = (bi[i] - 2 * x1 + x2 - a1 * hy1 - a2 * hy2) / a0;
				hi[i] = (hy0 - 2 * hy1 + hy2 - a1 * hz1 - a2 * hz2) / a0;

				// remember everything
				x2 = x1;
				x1 = bi[i];

				ly2 = ly1;
				ly1 = ly0;
				lz2 = lz1;
				lz1 = lo[i];

				hy2 = hy1;
				hy1 = hy0;
				hz2 = hz1;
				hz1 = hi[i];

			}

		}
	}

	private final void calcVals() {

		k = (float) Math.tan(freq * pi_sr);
		lb0 = k * k;
		lb1 = 2f * lb0;
		// lb2 = lb0;
		// hb0 = hb2 = 1;
		// hb1 = -2;
		a0 = lb0 + (SQRT2 * k) + 1;
		a1 = 2f * (lb0 - 1);
		a2 = lb0 - (SQRT2 * k) + 1;

	}

	/**
	 * Resets the filter in case it "explodes".
	 */
	public void reset() {
		lz2 = lz1 = ly2 = ly1 = hz2 = hz1 = hy2 = hy1 = 0;
	}

	/**
	 * Gets the current cutoff frequency.
	 * 
	 * @return The cutoff frequency.
	 */
	public float getFrequency() {
		return freq;
	}

	/**
	 * Sets the cutoff frequency to a static float value.
	 * 
	 * @param freq
	 *            The cutoff frequency in Hertz.
	 * @return This filter instance.
	 */
	public CrossoverFilter setFrequency(float freq) {
		this.freq = freq;
		freqUGen = null;
		calcVals();
		return this;
	}

	/**
	 * Sets a UGen to control the cutoff frequency. Note that the frequency is
	 * only updated every frame, not every sample.
	 * 
	 * @param freqUGen
	 *            The UGen to control the cutoff frequency.
	 * @return This filter instance.
	 */
	public CrossoverFilter setFrequency(UGen freqUGen) {
		if (freqUGen == null) {
			setFrequency(freq);
		} else {
			this.freqUGen = freqUGen;
			freqUGen.update();
			freq = freqUGen.getValue();
			calcVals();
		}
		return this;
	}

	/**
	 * Gets the UGen controlling the cutoff frequency, if there is one.
	 * 
	 * @return The UGen controlling the cutoff frequency.
	 */
	public UGen getFrequencyUGen() {
		return freqUGen;
	}

	/**
	 * Gets the current cutoff frequency.
	 * 
	 * @return The cutoff frequency.
	 * @deprecated Use {@link #getFrequency()}.
	 */
	@Deprecated
	public float getFreq() {
		return getFrequency();
	}

	/**
	 * Sets the cutoff frequency to a static float value.
	 * 
	 * @param freq
	 *            The cutoff frequency in Hertz.
	 * @return This filter instance.
	 * @deprecated Use {@link #setFrequency(float)}.
	 */
	@Deprecated
	public CrossoverFilter setFreq(float freq) {
		return setFrequency(freq);
	}

	/**
	 * Sets a UGen to control the cutoff frequency. Note that the frequency is
	 * only updated every frame, not every sample.
	 * 
	 * @param freqUGen
	 *            The UGen to control the cutoff frequency.
	 * @return This filter instance.
	 * @deprecated Use {@link #setFrequency(UGen)}.
	 */
	@Deprecated
	public CrossoverFilter setFreq(UGen freqUGen) {
		return setFrequency(freqUGen);
	}
	
	/**
	 * Gets the UGen controlling the cutoff frequency, if there is one.
	 * 
	 * @return The UGen controlling the cutoff frequency.
	 * @deprecated Use {@link #getFrequencyUGen()}.
	 */
	@Deprecated
	public UGen getFreqUGen() {
		return getFrequencyUGen();
	}

	/**
	 * Gets the number of channels.
	 * 
	 * @return The number of channels.
	 */
	public int getChannels() {
		return channels;
	}

	/**
	 * Sets the crossover frequency with a DataBead. If the value of the
	 * property "frequency" is either a UGen or can be interpreted as a float,
	 * the cutoff frequency will be set appropriately.
	 */
	public DataBeadReceiver sendData(DataBead db) {
		UGen u = db.getUGen("frequency");
		if (u == null) {
			setFrequency(db.getFloat("frequency", freq));
		} else {
			setFrequency(u);
		}
		return this;
	}

	@Override
	public void messageReceived(Bead message) {
		if (message instanceof DataBead) {
			sendData((DataBead) message);
		}
	}

	public CrossoverFilter drawFromLowOutput(UGen target) {
		for (int i = 0; i < target.getIns(); i++) {
			if (i >= channels) {
				break;
			}
			target.addInput(i, this, i * 2);
		}
		return this;
	}

	public CrossoverFilter drawFromLowOutput(int channel, UGen target,
			int targetInputIndex) {
		target.addInput(targetInputIndex, this, channel * 2);
		return this;
	}

	public CrossoverFilter drawFromHighOutput(UGen target) {
		for (int i = 0; i < target.getIns(); i++) {
			if (i >= channels) {
				break;
			}
			target.addInput(i, this, i * 2 + 1);
		}
		return this;
	}

	public CrossoverFilter drawFromHighOutput(int channel, UGen target,
			int targetInputIndex) {
		target.addInput(targetInputIndex, this, channel * 2 + 1);
		return this;
	}

	// Attempted implementation of true 4th-order filter, but there's a bug...
	/*
	 * public void calculateBuffer4() { float[] bi = bufIn[0]; float[] lo =
	 * bufOut[0]; float[] hi = bufOut[1];
	 * 
	 * for (int i = 0; i < bufferSize; i++) { lo[i] = (la0 * bi[i] + la1 * x1 +
	 * la2 * x2 + la3 * x3 + la4 * x4 - b1 * ly1 - b2 * ly2 - b3 * ly3 - b4 *
	 * ly4) / b0; hi[i] = (ha0 * bi[i] + ha1 * x1 + ha2 * x2 + ha3 * x3 + ha4 *
	 * x4 - b1 * hy1 - b2 * hy2 - b3 * hy3 - b4 * hy4) / b0; x4 = x3; x3 = x2;
	 * x2 = x1; x1 = bi[i];
	 * 
	 * ly4 = ly3; ly3 = ly2; ly2 = ly1; ly1 = lo[i];
	 * 
	 * hy4 = hy3; hy3 = hy2; hy2 = hy1; hy1 = hi[i];
	 * 
	 * } }
	 * 
	 * private final void calcVals4() {
	 * 
	 * ok = (float) Math.tan(pi_sr * freq); ok2 = ok * ok;
	 * 
	 * nik4 = 1 / ok2; nr2k = ok * SQRT2; nr2k3 = SQRT2 / ok;
	 * 
	 * b0 = 4 + nr2k + nik4 + nr2k3 + ok2; b1 = 4 * (ok2 + nr2k - nik4 - nr2k3);
	 * b2 = (6 * (ok2 + nik4)) - 8; b3 = 4 * (ok2 - nr2k + nr2k3 - nik4); b4 =
	 * nik4 - (2 * (nr2k + nr2k3)) + ok2 + 4;
	 * 
	 * // ================================================ // low-pass //
	 * ================================================ la4 = la0 = ok2; la3 =
	 * la1 = 4 * ok2; la2 = 6 * ok2;
	 * 
	 * // ===================================================== // high-pass //
	 * ===================================================== ha4 = ha0 = nik4;
	 * ha3 = ha1 = -4 * ha0; ha2 = 6 * ha0; }
	 */

}
