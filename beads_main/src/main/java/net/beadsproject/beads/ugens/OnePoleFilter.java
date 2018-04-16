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
 * A simple one-pole filter implementation. Cut-off frequency can be specified
 * either by UGen or a float.
 * <p>
 * It uses the formula: y(n) = a * x(n) + (1 - a) * y(n - 1)
 * 
 * @beads.category filter
 * @author Benito Crawford
 * @version 0.9
 */
public class OnePoleFilter extends IIRFilter implements DataBeadReceiver {
	private float freq, b0, a1, y1 = 0;
	private UGen freqUGen;
	protected float samplingfreq, two_pi_over_sf;
	protected boolean isFreqStatic;

	/**
	 * Constructor for cut-off frequency specified by a static float.
	 * 
	 * @param con
	 *            The audio context.
	 * @param freq
	 *            The cut-off frequency.
	 */
	public OnePoleFilter(AudioContext con, float freq) {
		super(con, 1, 1);
		samplingfreq = con.getSampleRate();
		two_pi_over_sf = (float) (2 * Math.PI / samplingfreq);

		setFrequency(freq);
	}

	/**
	 * Constructor for cut-off frequency specified by a UGen.
	 * 
	 * @param con
	 *            The audio context.
	 * @param freq
	 *            The cut-off frequency UGen.
	 */
	public OnePoleFilter(AudioContext con, UGen freq) {
		super(con, 1, 1);
		samplingfreq = con.getSampleRate();
		two_pi_over_sf = (float) (2 * Math.PI / samplingfreq);

		setFrequency(freq);
	}

	protected void calcVals() {
		a1 = (b0 = (float) Math.sin(two_pi_over_sf * freq)) - 1;
	}

	@Override
	public void calculateBuffer() {
		float[] bi = bufIn[0];
		float[] bo = bufOut[0];

		if (isFreqStatic) {

			for (int currsamp = 0; currsamp < bufferSize; currsamp++) {
				bo[currsamp] = y1 = b0 * bi[currsamp] - a1 * y1;
			}

		} else {

			freqUGen.update();

			for (int currsamp = 0; currsamp < bufferSize; currsamp++) {
				a1 = (b0 = (float) Math.sin(two_pi_over_sf
						* freqUGen.getValue(0, currsamp))) - 1;
				bo[currsamp] = y1 = b0 * bi[currsamp] - a1 * y1;
			}
			freq = freqUGen.getValue(0, bufferSize - 1);

		}

		// check to see if it blew up
		if (Float.isNaN(y1))
			y1 = 0;
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
	 * Sets the cut-off frequency to a static float.
	 * 
	 * @param freq
	 *            The cut-off frequency.
	 * @return This filter instance.
	 */
	public OnePoleFilter setFrequency(float freq) {
		this.freq = freq;
		a1 = (b0 = (float) Math.sin(two_pi_over_sf * freq)) - 1;
		isFreqStatic = true;
		return this;
	}

	/**
	 * Sets a UGen to specify the cut-off frequency; passing null freezes the
	 * frequency at its current value.
	 * 
	 * @param freqUGen
	 *            The cut-off frequency UGen.
	 * @return This filter instance.
	 */
	public OnePoleFilter setFrequency(UGen freqUGen) {
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
	 * Gets the cut-off frequency UGen; returns null if frequency is static.
	 * 
	 * @return The cut-off frequency UGen.
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
	 * Sets the cut-off frequency to a static float.
	 * 
	 * @param freq
	 *            The cut-off frequency.
	 * @return This filter instance.
	 * @deprecated Use {@link #setFrequency(float)}.
	 */
	@Deprecated
	public OnePoleFilter setFreq(float freq) {
		return setFrequency(freq);
	}

	/**
	 * Sets a UGen to specify the cut-off frequency; passing null freezes the
	 * frequency at its current value.
	 * 
	 * @param freqUGen
	 *            The cut-off frequency UGen.
	 * @return This filter instance.
	 * @deprecated Use {@link #setFrequency(UGen)}.
	 */
	@Deprecated
	public OnePoleFilter setFreq(UGen freqUGen) {
		return setFrequency(freqUGen);
	}

	/**
	 * Gets the cut-off frequency UGen; returns null if frequency is static.
	 * 
	 * @return The cut-off frequency UGen.
	 * @deprecated Use {@link #getFrequencyUGen()}.
	 */
	@Deprecated
	public UGen getFreqUGen() {
		return getFrequencyUGen();
	}

	/**
	 * Sets the filter parameters with a DataBead.
	 * <p>
	 * Use the "frequency" properties to specify filter frequency.
	 * 
	 * @param paramBead
	 *            The DataBead specifying parameters.
	 * @return This filter instance.
	 */
	public OnePoleFilter setParams(DataBead paramBead) {
		if (paramBead != null) {
			Object o;

			if ((o = paramBead.get("frequency")) != null) {
				if (o instanceof UGen) {
					setFrequency((UGen) o);
				} else {
					setFrequency(paramBead.getFloat("frequency", freq));
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
	 * Gets a DataBead with the filter frequency (whether float or UGen), stored
	 * in the key "frequency".
	 * 
	 * @return The DataBead with the stored parameter.
	 */
	public DataBead getParams() {
		DataBead db = new DataBead();

		if (isFreqStatic) {
			db.put("frequency", freq);
		} else {
			db.put("frequency", freqUGen);
		}

		return db;
	}

	/**
	 * Gets a DataBead with property "frequency" set to its current float value.
	 * 
	 * @return The DataBead with the static float parameter value.
	 */
	public DataBead getStaticParams() {
		DataBead db = new DataBead();
		db.put("frequency", freq);
		return db;
	}

	/**
	 * Sets the filter frequency with a DataBead.
	 * 
	 * @see #setParams(DataBead)
	 * @return This filter instance.
	 */
	public DataBeadReceiver sendData(DataBead db) {
		setParams(db);
		return this;
	}

	@Override
	public IIRFilterAnalysis getFilterResponse(float freq) {
		return calculateFilterResponse(new float[] { b0 },
				new float[] { 1, a1 }, freq, context.getSampleRate());
	}

}
