/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.*;
import net.beadsproject.beads.data.*;

/**
 * A simple 2nd-order resonant low-pass filter optimized for single-channel
 * processing. Faster than a BiquadFilter because its algorithm is of the form:
 * <p>
 * <code>y(n) = b0 * x(n) + a1 * y(n-1) + a2 * y(n-2)</code>
 * <p>
 * so it doesn't compute unnecessary parts of the biquad formula.
 * <p>
 * Takes two parameters: cut-off frequency and resonance (0 for no resonance, 1
 * for maximum resonance). These parameters can be set using
 * {@link #setFrequency(float) setFreq()} and {@link #setRes(float) setRes()},
 * or by passing a DataBead with "frequency" and "resonance" properties to
 * {@link #setParams(DataBead)}. (Messaging this object with a DataBead achieves
 * the same.)
 * 
 * @beads.category filter
 * @author Benito Crawford
 * @version 0.9.5
 */
public class LPRezFilter extends IIRFilter implements DataBeadReceiver {

	protected float freq = 100;
	protected float res = .5f, _2pi_over_sr, cosw = 0;
	protected UGen freqUGen, resUGen;

	protected float a1, a2, b0;
	private float y1 = 0, y2 = 0;
	private float[] y1m, y2m;
	private int channels;

	protected boolean isFreqStatic, isResStatic;

	/**
	 * Constructor for a single-channel LPRezFilter with default values for
	 * frequency and resonance.
	 * 
	 * @param con
	 *            The audio context.
	 */
	public LPRezFilter(AudioContext con) {
		this(con, 1);
	}

	/**
	 * Constructor for a multi-channel LPRezFilter with default values for
	 * frequency and resonance.
	 * 
	 * @param con
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 */
	public LPRezFilter(AudioContext con, int channels) {
		super(con, channels, channels);
		_2pi_over_sr = (float) (2 * Math.PI / con.getSampleRate());
		this.channels = super.getOuts();
		y1m = new float[this.channels];
		y2m = new float[this.channels];
		setFrequency(freq).setRes(res);
	}

	/**
	 * Constructor for a single-channel LPRezFilter with frequency and resonance
	 * specified by floats.
	 * 
	 * @param con
	 *            The audio context.
	 * @param freq
	 *            The filter cut-off frequency.
	 * @param res
	 *            The resonance.
	 */
	public LPRezFilter(AudioContext con, float freq, float res) {
		this(con, 1, freq, res);
	}

	/**
	 * Constructor for a single-channel LPRezFilter with frequency specified by
	 * a UGen and resonance specified by a float.
	 * 
	 * @param con
	 *            The audio context.
	 * @param freq
	 *            The filter cut-off frequency UGen.
	 * @param res
	 *            The resonance.
	 */
	public LPRezFilter(AudioContext con, UGen freq, float res) {
		this(con, 1, freq, res);
	}

	/**
	 * Constructor for a single-channel LPRezFilter with frequency specified by
	 * a float and resonance specified by a UGen.
	 * 
	 * @param con
	 *            The audio context.
	 * @param freq
	 *            The filter cut-off frequency.
	 * @param res
	 *            The resonance UGen.
	 */
	public LPRezFilter(AudioContext con, float freq, UGen res) {
		this(con, 1, freq, res);
	}

	/**
	 * Constructor for a single-channel LPRezFilter with frequency and resonance
	 * specified by UGens.
	 * 
	 * @param con
	 *            The audio context.
	 * @param freq
	 *            The filter cut-off frequency UGen.
	 * @param res
	 *            The resonance UGen.
	 */
	public LPRezFilter(AudioContext con, UGen freq, UGen res) {
		this(con, 1, freq, res);
	}

	/**
	 * Constructor for a multi-channel LPRezFilter with frequency and resonance
	 * specified by floats.
	 * 
	 * @param con
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 * @param freq
	 *            The filter cut-off frequency.
	 * @param res
	 *            The resonance.
	 */
	public LPRezFilter(AudioContext con, int channels, float freq, float res) {
		this(con, channels);
		setFrequency(freq).setRes(res);
	}

	/**
	 * Constructor for a multi-channel LPRezFilter with frequency specified by a
	 * UGen and resonance specified by a float.
	 * 
	 * @param con
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 * @param freq
	 *            The filter cut-off frequency UGen.
	 * @param res
	 *            The resonance.
	 */
	public LPRezFilter(AudioContext con, int channels, UGen freq, float res) {
		this(con, channels);
		setFrequency(freq).setRes(res);
	}

	/**
	 * Constructor for a multi-channel LPRezFilter with frequency specified by a
	 * float and resonance specified by a UGen.
	 * 
	 * @param con
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 * @param freq
	 *            The filter cut-off frequency.
	 * @param res
	 *            The resonance UGen.
	 */
	public LPRezFilter(AudioContext con, int channels, float freq, UGen res) {
		this(con, channels);
		setFrequency(freq).setRes(res);
	}

	/**
	 * Constructor for a multi-channel LPRezFilter with frequency and resonance
	 * specified by UGens.
	 * 
	 * @param con
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 * @param freq
	 *            The filter cut-off frequency UGen.
	 * @param res
	 *            The resonance UGen.
	 */
	public LPRezFilter(AudioContext con, int channels, UGen freq, UGen res) {
		this(con, channels);
		setFrequency(freq).setRes(res);
	}

	protected void calcVals() {
		a1 = -2 * res * cosw;
		a2 = res * res;
		b0 = 1 + a1 + a2;
	}

	@Override
	public void calculateBuffer() {

		if (channels == 1) {

			float[] bi = bufIn[0];
			float[] bo = bufOut[0];

			if (isFreqStatic && isResStatic) {

				bo[0] = bi[0] * b0 - a1 * y1 - a2 * y2;
				bo[1] = bi[1] * b0 - a1 * bo[0] - a2 * y1;

				// main loop
				for (int currsamp = 2; currsamp < bufferSize; currsamp++) {
					bo[currsamp] = bi[currsamp] * b0 - a1 * bo[currsamp - 1]
							- a2 * bo[currsamp - 2];
				}

			} else {

				freqUGen.update();
				resUGen.update();

				cosw = (float) (Math.cos(_2pi_over_sr
						* (freq = freqUGen.getValue(0, 0))));
				if ((res = resUGen.getValue(0, 0)) > .999999f) {
					res = .999999f;
				} else if (res < 0) {
					res = 0;
				}
				calcVals();
				bo[0] = bi[0] * b0 - a1 * y1 - a2 * y2;

				cosw = (float) (Math.cos(_2pi_over_sr
						* (freq = freqUGen.getValue(0, 1))));
				if ((res = resUGen.getValue(0, 1)) > .999999f) {
					res = .999999f;
				} else if (res < 0) {
					res = 0;
				}
				calcVals();
				bo[1] = bi[1] * b0 - a1 * bo[0] - a2 * y1;

				// main loop
				for (int currsamp = 2; currsamp < bufferSize; currsamp++) {

					cosw = (float) (Math.cos(_2pi_over_sr
							* (freq = freqUGen.getValue(0, currsamp))));
					if ((res = resUGen.getValue(0, currsamp)) > .999999f) {
						res = .999999f;
					} else if (res < 0) {
						res = 0;
					}
					calcVals();

					bo[currsamp] = bi[currsamp] * b0 - a1 * bo[currsamp - 1]
							- a2 * bo[currsamp - 2];
				}

			}

			y2 = bo[bufferSize - 2];
			if (Float.isNaN(y1 = bo[bufferSize - 1])) {
				reset();
			}

		} else {

			// multi-channel case

			if (isFreqStatic && isResStatic) {
				for (int i = 0; i < channels; i++) {
					float[] bi = bufIn[i];
					float[] bo = bufOut[i];

					bo[0] = bi[0] * b0 - a1 * y1m[i] - a2 * y2m[i];
					bo[1] = bi[1] * b0 - a1 * bo[0] - a2 * y1m[i];

					// main loop
					for (int currsamp = 2; currsamp < bufferSize; currsamp++) {
						bo[currsamp] = bi[currsamp] * b0 - a1
								* bo[currsamp - 1] - a2 * bo[currsamp - 2];
					}

					y2m[i] = bo[bufferSize - 2];
					if (Float.isNaN(y1m[i] = bo[bufferSize - 1])) {
						reset();
					}
				}

			} else {

				freqUGen.update();
				resUGen.update();

				// first sample
				cosw = (float) (Math.cos(_2pi_over_sr
						* (freq = freqUGen.getValue(0, 0))));
				if ((res = resUGen.getValue(0, 0)) > .999999f) {
					res = .999999f;
				} else if (res < 0) {
					res = 0;
				}
				calcVals();
				for (int i = 0; i < channels; i++) {
					bufOut[i][0] = bufIn[i][0] * b0 - a1 * y1m[i] - a2 * y2m[i];
				}

				// second sample
				cosw = (float) (Math.cos(_2pi_over_sr
						* (freq = freqUGen.getValue(0, 1))));
				if ((res = resUGen.getValue(0, 1)) > .999999f) {
					res = .999999f;
				} else if (res < 0) {
					res = 0;
				}
				calcVals();
				for (int i = 0; i < channels; i++) {
					bufOut[i][1] = bufIn[i][1] * b0 - a1 * bufOut[i][0] - a2
							* y1m[i];
				}

				// main loop
				for (int currsamp = 2; currsamp < bufferSize; currsamp++) {

					cosw = (float) (Math.cos(_2pi_over_sr
							* (freq = freqUGen.getValue(0, currsamp))));
					if ((res = resUGen.getValue(0, currsamp)) > .999999f) {
						res = .999999f;
					} else if (res < 0) {
						res = 0;
					}
					calcVals();

					for (int i = 0; i < channels; i++) {
						bufOut[i][currsamp] = bufIn[i][currsamp] * b0 - a1
								* bufOut[i][currsamp - 1] - a2
								* bufOut[i][currsamp - 2];
					}
				}

				for (int i = 0; i < channels; i++) {
					y2m[i] = bufOut[i][bufferSize - 2];
					if (Float.isNaN(y1m[i] = bufOut[i][bufferSize - 1])) {
						reset();
					}
				}
			}

		}
	}

	/**
	 * Resets the filter in case it "explodes".
	 */
	public void reset() {
		y1 = 0;
		y2 = 0;
		for (int i = 0; i < channels; i++) {
			y1m[i] = 0;
			y2m[i] = 0;
		}
	}

	/**
	 * Gets the current cut-off frequency.
	 * 
	 * @return The cut-off frequency.
	 */
	public float getFrequency() {
		return freq;
	}

	/**
	 * Sets the cut-off frequency to a float. Removes the frequency UGen, if
	 * there is one.
	 * 
	 * @param freq
	 *            The cut-off frequency.
	 * @return This filter instance.
	 */
	public LPRezFilter setFrequency(float freq) {
		this.freq = freq;
		if (isFreqStatic) {
			freqUGen.setValue(freq);
		} else {
			freqUGen = new Static(context, freq);
			isFreqStatic = true;
		}
		cosw = (float) (Math.cos(_2pi_over_sr * this.freq));
		calcVals();
		return this;
	}

	/**
	 * Sets a UGen to specify the cut-off frequency. Passing a null value
	 * freezes the parameter.
	 * 
	 * @param freqUGen
	 *            The frequency UGen.
	 * @return This filter instance.
	 */
	public LPRezFilter setFrequency(UGen freqUGen) {
		if (freqUGen == null) {
			setFrequency(freq);
		} else {
			this.freqUGen = freqUGen;
			freqUGen.update();
			freq = freqUGen.getValue();
			isFreqStatic = false;
		}
		return this;
	}

	/**
	 * Gets the frequency UGen, if it exists.
	 * 
	 * @return The frequency UGen.
	 */
	public UGen getFrequencyUGen() {
		if (isFreqStatic) {
			return null;
		} else {
			return freqUGen;
		}
	}

	/**
	 * Gets the current cut-off frequency.
	 * 
	 * @return The cut-off frequency.
	 * @deprecated Use {@link #getFrequency()}.
	 */
	@Deprecated
	public float getFreq() {
		return getFrequency();
	}

	/**
	 * Sets the cut-off frequency to a float. Removes the frequency UGen, if
	 * there is one.
	 * 
	 * @param freq
	 *            The cut-off frequency.
	 * @return This filter instance.
	 * @deprecated Use {@link #setFrequency(float)}.
	 */
	@Deprecated
	public LPRezFilter setFreq(float freq) {
		return setFrequency(freq);
	}

	/**
	 * Sets a UGen to specify the cut-off frequency. Passing a null value
	 * freezes the parameter.
	 * 
	 * @param freqUGen
	 *            The frequency UGen.
	 * @return This filter instance.
	 * @deprecated Use {@link #setFrequency(UGen)}.
	 */
	@Deprecated
	public LPRezFilter setFreq(UGen freqUGen) {
		return setFrequency(freqUGen);
	}

	/**
	 * Gets the frequency UGen, if it exists.
	 * 
	 * @return The frequency UGen.
	 * @deprecated Use {@link #getFrequencyUGen()}.
	 */
	@Deprecated
	public UGen getFreqUGen() {
		return getFrequencyUGen();
	}

	/**
	 * Gets the current resonance value.
	 * 
	 * @return The resonance.
	 */
	public float getRes() {
		return res;
	}

	/**
	 * Sets the filter resonance to a float value. This removes the resonance
	 * UGen, if it exists. (Should be >= 0 and < 1.)
	 * 
	 * @param r
	 *            The resonance.
	 * @return This filter instance.
	 */
	public LPRezFilter setRes(float r) {
		if (r > .999999f) {
			res = .999999f;
		} else if (r < 0) {
			res = 0;
		} else {
			res = r;
		}
		if (isResStatic) {
			resUGen.setValue(res);
		} else {
			resUGen = new Static(context, res);
			isResStatic = true;
		}
		calcVals();
		return this;
	}

	/**
	 * Sets a UGen to specify the filter resonance. Passing a null value freezes
	 * the parameter.
	 * 
	 * @param r
	 *            The resonance UGen.
	 * @return This filter instance.
	 */
	public LPRezFilter setRes(UGen r) {
		if (r == null) {
			setRes(res);
		} else {
			resUGen = r;
			r.update();
			res = r.getValue();
			isResStatic = false;
		}
		return this;
	}

	/**
	 * Gets the resonance UGen, if it exists.
	 * 
	 * @return The resonance UGen.
	 */
	public UGen getResUGen() {
		if (isResStatic) {
			return null;
		} else {
			return resUGen;
		}
	}

	/**
	 * Sets the filter parameters with a DataBead.
	 * <p>
	 * Use the following properties to specify filter parameters:
	 * </p>
	 * <ul>
	 * <li>"frequency": (float or UGen)</li>
	 * <li>"resonance": (float or UGen)</li>
	 * </ul>
	 * 
	 * @param paramBead
	 *            The DataBead specifying parameters.
	 * @return This filter instance.
	 */
	public LPRezFilter setParams(DataBead paramBead) {
		if (paramBead != null) {
			Object o;

			if ((o = paramBead.get("frequency")) != null) {
				if (o instanceof UGen) {
					setFrequency((UGen) o);
				} else {
					setFrequency(paramBead.getFloat("frequency", freq));
				}
			}

			if ((o = paramBead.get("resonance")) != null) {
				if (o instanceof UGen) {
					setRes((UGen) o);
				} else {
					setRes(paramBead.getFloat("resonance", res));
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
	 * Gets a DataBead with properties "frequency" and "resonance" set to the
	 * corresponding filter parameters.
	 * 
	 * @return The parameter DataBead.
	 */
	public DataBead getParams() {
		DataBead db = new DataBead();
		if (isFreqStatic) {
			db.put("frequency", freq);
		} else {
			db.put("frequency", freqUGen);
		}

		if (isResStatic) {
			db.put("resonance", res);
		} else {
			db.put("resonance", resUGen);
		}

		return db;
	}

	/**
	 * Gets a DataBead with properties "frequency" and "resonance" set to static
	 * float values corresponding to the current filter parameters.
	 * 
	 * @return The static parameter DataBead.
	 */
	public DataBead getStaticParams() {
		DataBead db = new DataBead();
		db.put("frequency", freq);
		db.put("resonance", res);
		return db;
	}

	/**
	 * Sets the filter's parameters with properties from a DataBead.
	 * 
	 * @see #setParams(DataBead)
	 */
	public DataBeadReceiver sendData(DataBead db) {
		setParams(db);
		return this;
	}

	@Override
	public IIRFilterAnalysis getFilterResponse(float freq) {
		return calculateFilterResponse(new float[] { b0 }, new float[] { 1, a1,
				a2 }, freq, context.getSampleRate());
	}

}
