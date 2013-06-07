package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * Produces a cycling linear ramp from 0 to 1, like the Phasor object in
 * Max/MSP. More accurate than WavePlayer for slow speeds, as it calculates it's
 * value for each sample, rather than stepping through a buffer.
 * 
 * @author Benito Crawford
 * @version 0.9.5
 */
public class Phasor extends UGen {

	private UGen frequencyUGen;
	protected float frequency = 0;
	protected double phase = 0;
	protected double one_over_sr;

	/**
	 * Basic constructor.
	 * 
	 * @param con
	 *            The AudioContext.
	 */
	public Phasor(AudioContext con) {
		super(con, 0, 1);
		one_over_sr = 1d / con.getSampleRate();
	}

	/**
	 * Constructor that sets the initial frequency to a float value.
	 * 
	 * @param con
	 *            The AudioContext.
	 * @param frequency
	 *            The initial frequency.
	 */
	public Phasor(AudioContext con, float frequency) {
		this(con);
		setFrequency(frequency);
	}

	/**
	 * Constructor that sets a UGen to control the frequency.
	 * 
	 * @param con
	 *            The AudioContext.
	 * @param frequencyUGen
	 *            The frequency controller UGen.
	 */
	public Phasor(AudioContext con, UGen frequencyUGen) {
		this(con);
		setFrequency(frequencyUGen);
	}

	@Override
	public void calculateBuffer() {
		float[] bo = bufOut[0];

		if (frequencyUGen == null) {
			for (int i = 0; i < bufferSize; i++) {
				phase = (((phase + one_over_sr * frequency) % 1) + 1) % 1;
				bo[i] = (float) phase;
			}
		} else {
			frequencyUGen.update();
			for (int i = 0; i < bufferSize; i++) {
				frequency = frequencyUGen.getValue(0, i);
				phase = (((phase + one_over_sr * frequency) % 1) + 1) % 1;
				bo[i] = (float) phase;
			}
		}
	}

	/**
	 * Gets the UGen that controls the frequency.
	 * 
	 * @return The frequency controller UGen.
	 */
	public UGen getFrequencyUGen() {
		return frequencyUGen;
	}

	/**
	 * Gets the current frequency.
	 * 
	 * @return The current frequency.
	 */
	public float getFrequency() {
		return frequency;
	}

	/**
	 * Sets a UGen to control the frequency.
	 * 
	 * @param frequencyUGen
	 *            The new frequency controller.
	 * @return This Phasor instance.
	 */
	public Phasor setFrequency(UGen frequencyUGen) {
		if (frequencyUGen == null) {
			setFrequency(frequency);
		} else {
			this.frequencyUGen = frequencyUGen;
			frequencyUGen.update();
			frequency = frequencyUGen.getValue();
		}
		return this;
	}

	/**
	 * Sets the frequency to a static value.
	 * 
	 * @param frequency
	 *            The new frequency value.
	 * @return This Phasor instance.
	 */
	public Phasor setFrequency(float frequency) {
		this.frequency = frequency;
		frequencyUGen = null;
		return this;
	}

	/**
	 * Gets the current phase.
	 * 
	 * @return The current phase.
	 */
	public float getPhase() {
		return (float) phase;
	}

	/**
	 * Sets the phase.
	 * 
	 * @param phase
	 *            The new phase.
	 * @return This Phasor instance.
	 */
	public Phasor setPhase(float phase) {
		this.phase = phase;
		return this;
	}

}
