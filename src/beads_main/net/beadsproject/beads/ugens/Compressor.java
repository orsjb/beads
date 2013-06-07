/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.*;
import net.beadsproject.beads.data.*;

/**
 * A simple multi-channel dynamic range compression UGen. Users can vary the
 * threshold (set in RMS power units), the compression ratio, the rate of
 * attack/decay, look-ahead time (delay), and the hardness/softness of the knee.
 * The amount of compression can also be controlled by an alternate side-chain.
 * <p>
 * The following are the default parameter values:
 * <p>
 * <ul>
 * <li>{@link #setThreshold(float) threshold} - .5</li>
 * <li>{@link #setAttack(float) attack} - 1</li>
 * <li>{@link #setDecay(float) decay} - .5</li>
 * <li>{@link #setKnee(float) knee} - .5</li>
 * <li>{@link #setRatio(float) ratio} - 2</li>
 * <li>{@link #setSideChain(UGen) side-chain} - the input audio</li>
 * </ul>
 * 
 * @beads.category dynamics
 * @author Benito Crawford
 * @version 0.9.5
 */
public class Compressor extends UGen implements DataBeadReceiver {
	private int channels;
	private int memSize, index = 0;
	private float[][] delayMem;
	private UGen powerUGen;
	private BiquadFilter pf;
	private float downstep = .9998f, upstep = 1.0002f, ratio = .5f,
			threshold = .5f, knee = 1;
	private float tok, kt, ikp1, ktrm1, tt1mr;

	private float attack, decay;
	private float currval = 1, target = 1, delay;
	private int delaySamps;
	private int rmsMemorySize = 500;
	private UGen myInputs;
	private float[][] myBufIn;

	/**
	 * Constructor for a 1-channel compressor with no look-ahead time and other
	 * parameters set to their default values.
	 * 
	 * @param context
	 *            The audio context.
	 */
	public Compressor(AudioContext context) {
		this(context, 1);
	}

	/**
	 * Constructor for a multi-channel compressor with no look-ahead time and
	 * other parameters set to their default values.
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 */
	public Compressor(AudioContext context, int channels) {
		this(context, channels, 0, null);
	}

	/**
	 * Constructor for a multi-channel compressor with the specified side-chain,
	 * no look-ahead time, and other parameters set to their default values.
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 * @param sideChain
	 *            The UGen to use as the side-chain.
	 */
	public Compressor(AudioContext context, int channels, UGen sideChain) {
		this(context, channels, 0, sideChain);
	}

	/**
	 * Constructor for a multi-channel compressor with the specified look-ahead
	 * time and other parameters set to their default values.
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 * @param lookAheadDelay
	 *            The look-ahead time in milliseconds.
	 */
	public Compressor(AudioContext context, int channels, float lookAheadDelay) {
		this(context, channels, lookAheadDelay, null);
	}

	/**
	 * Constructor for a multi-channel compressor with the specified look-ahead
	 * time and side-chain, and other parameters set to their default values.
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 * @param lookAheadDelay
	 *            The look-ahead time in milliseconds.
	 * @param sideChain
	 *            The UGen to use as the side-chain.
	 */
	public Compressor(AudioContext context, int channels, float lookAheadDelay,
			UGen sideChain) {
		super(context, channels, channels);
		this.channels = channels;
		delay = lookAheadDelay;
		delaySamps = (int) delay;
		memSize = (int) context.msToSamples(delay) + 1;
		delayMem = new float[channels][memSize];
		myBufIn = bufIn;

		class MyInputs extends UGen {
			MyInputs(AudioContext context, int channels) {
				super(context, 0, channels);
				bufOut = myBufIn;
				this.outputInitializationRegime = OutputInitializationRegime.RETAIN;
			}

			@Override
			public void calculateBuffer() {
			}
		}

		myInputs = new MyInputs(context, channels);

		setSideChain(sideChain).setAttack(1).setDecay(.5f).setRatio(2)
				.setThreshold(.5f).setKnee(.5f);
	}

	@Override
	public void calculateBuffer() {

		pf.update();

		if (channels == 1) {

			float[] bi = bufIn[0];
			float[] bo = bufOut[0];
			float[] dm = delayMem[0];

			for (int i = 0; i < bufferSize; i++) {
				float p = pf.getValue(0, i);
				if (p <= tok) {
					target = 1;
				} else if (p >= kt) {
					target = ((p - threshold) * ratio + threshold) / p;
				} else {
					float x1 = (p - tok) * ikp1 + tok;
					target = ((ktrm1 * x1 + tt1mr) * (p - x1)
							/ (x1 * (knee - 1)) + x1)
							/ p;
				}

				if (currval > target) {
					currval *= downstep;
					if (currval < target)
						currval = target;
				} else if (currval < target) {
					currval *= upstep;
					if (currval > target)
						currval = target;
				}

				dm[index] = bi[i];
				bo[i] = dm[(index + delaySamps) % memSize] * currval;
				index = (index + 1) % memSize;
			}
		} else {
			for (int i = 0; i < bufferSize; i++) {
				float p = pf.getValue(0, i);
				if (p <= tok) {
					target = 1;
				} else if (p >= kt) {
					target = ((p - threshold) * ratio + threshold) / p;
				} else {
					float x1 = (p - tok) * ikp1 + tok;
					target = (ktrm1 * x1 + tt1mr) * (p - x1)
							/ (x1 * (knee - 1)) + x1;
				}

				if (currval > target) {
					currval *= downstep;
					if (currval < target)
						currval = target;
				} else if (currval < target) {
					currval *= upstep;
					if (currval > target)
						currval = target;
				}

				int delIndex = (index + delaySamps) % memSize;
				for (int j = 0; j < channels; j++) {
					delayMem[j][index] = bufIn[j][i];
					bufOut[j][i] = delayMem[j][delIndex] * currval;
				}
				index = (index + 1) % memSize;
			}
		}

	}

	private void calcVals() {
		tok = threshold / knee;
		kt = knee * threshold;
		ikp1 = 1 / (knee + 1);
		ktrm1 = knee * ratio - 1;
		tt1mr = threshold * (1 - ratio);
	}

	/**
	 * Sets the side chain. The power of the side chain (measured by an
	 * {@link RMS} object) determines how much the compressor scales the input;
	 * by default (and if this method is passed <code>null</code>), the side
	 * chain is the same as the input.
	 * 
	 * @param sideChain
	 *            The side chain signal.
	 * @return This compressor instance.
	 */
	public Compressor setSideChain(UGen sideChain) {
		pf = (new BiquadFilter(context, 1, BiquadFilter.BUTTERWORTH_LP))
				.setFrequency(31);
		if (sideChain == null) {
			powerUGen = new RMS(context, channels, rmsMemorySize);
			powerUGen.addInput(myInputs);
			pf.addInput(powerUGen);
		} else {
			powerUGen = new RMS(context, sideChain.getOuts(), rmsMemorySize);
			powerUGen.addInput(sideChain);
			pf.addInput(powerUGen);
		}
		return this;
	}

	/**
	 * Gets the attack factor.
	 * 
	 * @return The attack factor in decibels per millisecond.
	 */
	public float getAttack() {
		return attack;
	}

	/**
	 * Sets the attack factor.
	 * 
	 * @param attack
	 *            The attack factor in decibels per millisecond.
	 * @return This compressor instance.
	 */
	public Compressor setAttack(float attack) {
		if (attack < .0001f) {
			attack = .0001f;
		}

		this.attack = attack;
		this.downstep = (float) Math.pow(Math.pow(10, attack / 20f), -1000f
				/ context.getSampleRate());

		return this;
	}

	/**
	 * Gets the decay factor.
	 * 
	 * @return The decay factor in decibels per millisecond.
	 */
	public float getDecay() {
		return decay;
	}

	/**
	 * Sets the decay factor.
	 * 
	 * @param decay
	 *            The decay factor in decibels per millisecond.
	 * @return This compressor instance.
	 */
	public Compressor setDecay(float decay) {
		if (decay < .0001f) {
			decay = .0001f;
		}
		this.decay = decay;

		this.upstep = (float) Math.pow(Math.pow(10, decay / 20f),
				1000f / context.getSampleRate());
		return this;
	}

	/**
	 * Gets the compression ratio.
	 * 
	 * @return The compression ratio.
	 */
	public float getRatio() {
		return 1 / ratio;
	}

	/**
	 * Sets the compression ratio. (For example, a value of 2.4 yields a 2.4:1
	 * compression ratio over the threshold. Values less than 1 will yield
	 * expansion rather than compression.)
	 * 
	 * @param ratio
	 *            The compression ratio.
	 * @return This compressor instance.
	 */
	public Compressor setRatio(float ratio) {
		if (ratio <= 0)
			ratio = .01f;
		this.ratio = 1 / ratio;
		calcVals();
		return this;
	}

	/**
	 * Gets the threshold value.
	 * 
	 * @return The threshold.
	 */
	public float getThreshold() {
		return threshold;
	}

	/**
	 * Sets the threshold value.
	 * 
	 * @param threshold
	 *            The threshold.
	 * @return This compressor instance.
	 */
	public Compressor setThreshold(float threshold) {
		this.threshold = threshold;
		calcVals();
		return this;
	}

	/**
	 * Gets the knee softness.
	 * 
	 * @return The knee softness.
	 */
	public float getKnee() {
		return knee - 1;
	}

	/**
	 * Sets the knee softness. 0 results in a strictly hard knee; the higher the
	 * value, the more curvature to the knee.
	 * 
	 * @param knee
	 *            The knee softness.
	 * @return This compressor instance.
	 */
	public Compressor setKnee(float knee) {
		this.knee = knee + 1;
		calcVals();
		return this;
	}

	/**
	 * Sets the compressor parameters with a DataBead. Use the following
	 * property names: "threshold", "ratio", "attack", "decay", "knee", and
	 * "sidechain".
	 * 
	 * @param db
	 *            The parameter DataBead.
	 * @return This compressor instance.
	 */
	public DataBeadReceiver sendData(DataBead db) {
		if (db != null) {
			setThreshold(db.getFloat("threshold", threshold));
			setRatio(db.getFloat("ratio", getRatio()));
			setAttack(db.getFloat("attack", getAttack()));
			setDecay(db.getFloat("decay", getDecay()));
			setKnee(db.getFloat("knee", getKnee()));
			setSideChain(db.getUGen("sidechain"));
		}
		return this;
	}

	/**
	 * Gets a DataBead filled with properties set to corresponding compressor
	 * parameters: "threshold", "ratio", "attack", "decay", "knee".
	 * 
	 * @return The
	 */
	public DataBead getParams() {
		DataBead db = new DataBead();
		db.put("threshold", getThreshold());
		db.put("ratio", getRatio());
		db.put("attack", getAttack());
		db.put("decay", getDecay());
		db.put("knee", getKnee());
		return db;
	}

	/**
	 * Gets the current scaling factor - the factor which scales the incoming
	 * signal, determined by the side chain.
	 * 
	 * @return The compression factor.
	 */
	public float getCurrentCompression() {
		return currval;
	}
}
