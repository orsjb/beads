/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.DataBead;
import net.beadsproject.beads.data.DataBeadReceiver;

/**
 * Gain modifies the gain of a multi-channel audio signal. The gain value can be
 * controlled by an audio signal.
 * 
 * @beads.category effect
 * @author ollie
 */
public class Gain extends UGen implements DataBeadReceiver {

	/** The gain envelope. */
	private UGen gainUGen;
	private float gain = 1;

	/**
	 * Instantiates a new Gain.
	 * 
	 * @param context
	 *            the AudioContext.
	 * @param inouts
	 *            the number of inputs (= number of outputs).
	 * @param gainEnvelope
	 *            the gain envelope.
	 */
	public Gain(AudioContext context, int inouts, UGen gainEnvelope) {
		super(context, inouts, inouts);
		setGain(gainEnvelope);
	}

	/**
	 * Instantiates a new Gain with a {@link Static} gain envelop with the given
	 * value.
	 * 
	 * @param context
	 *            the AudioContext.
	 * @param inouts
	 *            the number of inputs (= number of outputs).
	 * @param gain
	 *            the fixed gain level.
	 */
	public Gain(AudioContext context, int inouts, float gain) {
		super(context, inouts, inouts);
		setGain(gain);
	}

	/**
	 * Instantiates a new Gain with {@link Static} gain envelop set to 1.
	 * 
	 * @param context
	 *            the AudioContext.
	 * @param inouts
	 *            the number of inputs (= number of outputs).
	 */
	public Gain(AudioContext context, int inouts) {
		this(context, inouts, 1f);
	}

	/**
	 * Gets the gain envelope.
	 * 
	 * @return the gain envelope.
	 * @deprecated As of version 1.0, replaced by {@link #setGain(UGen)}.
	 */
	@Deprecated
	public UGen getGainEnvelope() {
		return gainUGen;
	}

	/**
	 * Sets the gain envelope.
	 * 
	 * @param gainEnvelope
	 *            the new gain envelope.
	 * @deprecated As of version 1.0, replaced by {@link #setGain(UGen)}.
	 */
	@Deprecated
	public void setGainEnvelope(UGen gainEnvelope) {
		this.gainUGen = gainEnvelope;
	}

	/**
	 * Gets the current gain value.
	 * 
	 * @return The gain value.
	 */
	public float getGain() {
		return gain;
	}

	/**
	 * Sets the gain to a static float value.
	 * 
	 * @param gain
	 *            The gain value.
	 * @return This gain instance.
	 */
	public Gain setGain(float gain) {
		this.gainUGen = null;
		this.gain = gain;
		return this;
	}

	/**
	 * Sets a UGen to control the gain amount.
	 * 
	 * @param gainUGen
	 *            The gain UGen.
	 * @return This gain instance.
	 */
	public Gain setGain(UGen gainUGen) {
		if (gainUGen == null) {
			setGain(gain);
		} else {
			this.gainUGen = gainUGen;
			gainUGen.update();
			gain = gainUGen.getValue();
		}
		return this;
	}

	/**
	 * Gets the gain UGen, if it exists.
	 * 
	 * @return The gain UGen.
	 */
	public UGen getGainUGen() {
		return gainUGen;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.olliebown.beads.core.UGen#calculateBuffer()
	 */
	@Override
	public void calculateBuffer() {
		if (gainUGen == null) {
			for (int channel = 0; channel < ins; channel++) {
				float[] bi = bufIn[channel];
				float[] bo = bufOut[channel];

				for (int i = 0; i < bufferSize; ++i) {
					bo[i] = gain * bi[i];
				}
			}
		} else {
			gainUGen.update();
			for (int i = 0; i < bufferSize; ++i) {
				gain = gainUGen.getValue(0, i);
				for (int channel = 0; channel < ins; channel++) {
					bufOut[channel][i] = gain * bufIn[channel][i];
				}
			}
		}
	}

	public DataBeadReceiver sendData(DataBead db) {
		if (db != null) {
			UGen u = db.getUGen("gain");
			if (u == null) {
				setGain(db.getFloat("gain", gain));
			} else {
				setGain(u);
			}
		}
		return this;
	}

}
