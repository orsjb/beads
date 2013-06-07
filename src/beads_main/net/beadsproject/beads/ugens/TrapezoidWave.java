package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.*;

/**
 * <p>
 * Generates a pulse-wave with adjustable duty cycle and "attack"/"decay"
 * lengths. Duty cycle indicates the portion of the waveform that is greater
 * than 0; "attack" and "decay" indicate the lengths of linear ramps between -1
 * and 1.
 * </p>
 * <p>
 * Several common waveforms can be generated using the following values (though
 * it should be noted that if all you want are these common waveforms, you might
 * prefer {@link WavePlayer}).
 * </p>
 * <ul>
 * <li>Duty cycle: .5, attack: 0, decay: 0 - classic square wave.</li>
 * <li>Duty cycle: .5, attack: .5, decay: .5 - classic triangle wave.</li>
 * <li>Duty cycle: .5, attack: 1, decay: 0 - classic sawtooth wave.</li>
 * </ul>
 * 
 * @author Benito Crawford
 * @version 0.9.5
 */
public class TrapezoidWave extends UGen {

	protected float index;
	protected float a, b, c, abSlope, cdSlope;
	protected float freq, dutyCycle = .5f, attack = 0, decay = 0;
	protected float delta, iSampleRate;
	protected UGen freqUGen, dutyCycleUGen, attackUGen, decayUGen;

	/**
	 * Constructor.
	 * 
	 * @param con
	 *            The AudioContext.
	 */
	public TrapezoidWave(AudioContext con) {
		super(con, 0, 1);
		iSampleRate = 1 / con.getSampleRate();
		calcVals();
	}

	@Override
	public void calculateBuffer() {
		float[] bo = bufOut[0];

		if (freqUGen != null) {
			freqUGen.update();
		}
		if (dutyCycleUGen != null) {
			dutyCycleUGen.update();
		}
		if (attackUGen != null) {
			attackUGen.update();
		}
		if (decayUGen != null) {
			decayUGen.update();
		}

		for (int i = 0; i < bufferSize; i++) {
			boolean doCalc = false;
			if (dutyCycleUGen != null) {
				dutyCycle = dutyCycleUGen.getValue(0, i);
				if (dutyCycle < 0) {
					dutyCycle = 0;
				} else if (dutyCycle > 1) {
					dutyCycle = 1;
				}
				doCalc = true;
			}
			if (attackUGen != null) {
				attack = attackUGen.getValue(0, i);
				if (attack < 0) {
					attack = 0;
				} else if (attack > 1) {
					attack = 1;
				}
				doCalc = true;
			}
			if (decayUGen != null) {
				decay = decayUGen.getValue(0, i);
				if (decay < 0) {
					decay = 0;
				} else if (decay > 1) {
					decay = 1;
				}
				doCalc = true;
			}
			if (doCalc) {
				calcVals();
			}

			if (freqUGen != null) {
				freq = freqUGen.getValue(0, i);
				delta = freq * iSampleRate;
			}

			index = (index + delta) % 1;
			if (index < 0) {
				index = (index + 1) % 1;
			}

			if (index > c) {
				bo[i] = 1 - cdSlope * (index - c);
			} else if (index > b) {
				bo[i] = 1;
			} else if (index > a) {
				bo[i] = -1 + abSlope * (index - a);
			} else {
				bo[i] = -1;
			}
		}

	}

	protected void calcVals() {
		a = 1 - dutyCycle;
		if (a >= 1) {
			a = b = c = 1;
			abSlope = 0;
			cdSlope = 0;
		} else {
			float att, dec;
			float m = attack + decay;
			if (m > 0) {
				if (dutyCycle < .5) {
					m = dutyCycle / m;
				} else {
					m = (1 - dutyCycle) / m;
				}

				if (m >= .5) {
					att = attack;
					dec = decay;
				} else {
					att = attack * 2 * m;
					dec = decay * 2 * m;
				}

			} else {
				// no attack / decay
				att = 0;
				dec = 0;
			}

			c = 1 - dec;
			b = c - dutyCycle + (att + dec) * .5f;
			a = b - att;

			if (a == b) {
				abSlope = 0;
			} else {
				abSlope = 2 / (b - a);
			}

			if (c == 1) {
				cdSlope = 0;
			} else {
				cdSlope = 2 / (1 - c);
			}
		}
	}

	/**
	 * Sets the frequency to a float value.
	 * 
	 * @param freq
	 *            The frequency.
	 * @return This TrapezoidWave instance.
	 */
	public TrapezoidWave setFrequency(float freq) {
		this.freq = freq;
		freqUGen = null;
		delta = freq * iSampleRate;
		return this;
	}

	/**
	 * Sets a UGen to control the frequency.
	 * 
	 * @param freqUGen
	 *            The frequency controller UGen.
	 * @return This TrapezoidWave instance.
	 */
	public TrapezoidWave setFrequency(UGen freqUGen) {
		if (freqUGen == null) {
			setFrequency(freq);
		} else {
			this.freqUGen = freqUGen;
			freqUGen.update();
			freq = freqUGen.getValue();
		}
		return this;
	}

	/**
	 * Gets the current frequency.
	 * 
	 * @return The frequency.
	 */
	public float getFrequency() {
		return freq;
	}

	/**
	 * Gets the UGen controlling the frequency, if there is one.
	 * 
	 * @return The frequency UGen.
	 */
	public UGen getFrequencyUGen() {
		return freqUGen;
	}

	/**
	 * Sets a UGen to control the duty cycle.
	 * 
	 * @param dutyCycleUGen
	 *            The duty cycle controller UGen.
	 * @return This TrapezoidWave instance.
	 */
	public TrapezoidWave setDutyCycle(UGen dutyCycleUGen) {
		if (dutyCycleUGen == null) {
			setDutyCycle(dutyCycle);
		} else {
			this.dutyCycleUGen = dutyCycleUGen;
			dutyCycleUGen.update();
			dutyCycle = dutyCycleUGen.getValue();
			calcVals();
		}
		return this;
	}

	/**
	 * Sets the duty cycle to a float value.
	 * 
	 * @param dutyCycle
	 *            The duty cycle.
	 * @return This TrapezoidWave instance.
	 */
	public TrapezoidWave setDutyCycle(float dutyCycle) {
		dutyCycleUGen = null;
		this.dutyCycle = dutyCycle;
		if (this.dutyCycle < 0) {
			this.dutyCycle = 0;
		} else if (this.dutyCycle > 1) {
			this.dutyCycle = 1;
		}
		calcVals();
		return this;
	}

	/**
	 * Gets the current duty cycle.
	 * 
	 * @return The duty cycle.
	 */
	public float getDutyCycle() {
		return dutyCycle;
	}

	/**
	 * Gets the UGen controlling the duty cycle, if there is one.
	 * 
	 * @return The duty cycle controller UGen.
	 */
	public UGen getDutyCycleUGen() {
		return dutyCycleUGen;
	}

	/**
	 * Sets a UGen to control the attack length.
	 * 
	 * @param attackUGen
	 *            The attack length controller UGen.
	 * @return This TrapezoidWave instance.
	 */
	public TrapezoidWave setAttack(UGen attackUGen) {
		if (attackUGen == null) {
			setAttack(attack);
		} else {
			this.attackUGen = attackUGen;
			attackUGen.update();
			attack = attackUGen.getValue();
			calcVals();
		}
		return this;
	}

	/**
	 * Sets the attack length to a float value.
	 * 
	 * @param attack
	 *            The attack length.
	 * @return This TrapezoidWave instance.
	 */
	public TrapezoidWave setAttack(float attack) {
		attackUGen = null;
		this.attack = attack;
		if (this.attack < 0) {
			this.attack = 0;
		} else if (this.attack > 1) {
			this.attack = 1;
		}
		calcVals();
		return this;
	}

	/**
	 * Gets the current attack length.
	 * 
	 * @return The attack length.
	 */
	public float getAttack() {
		return attack;
	}

	/**
	 * Gets the attack length controller UGen, if there is one.
	 * 
	 * @return The attack length controller UGen.
	 */
	public UGen getAttackUGen() {
		return attackUGen;
	}

	/**
	 * Sets a UGen to control the decay length.
	 * 
	 * @param decayUGen
	 *            The decay length controller UGen.
	 * @return This TrapezoidWave instance.
	 */
	public TrapezoidWave setDecay(UGen decayUGen) {
		if (decayUGen == null) {
			setDecay(decay);
		} else {
			this.decayUGen = decayUGen;
			decayUGen.update();
			decay = decayUGen.getValue();
			calcVals();
		}
		return this;
	}

	/**
	 * Sets the decay length to a float value.
	 * 
	 * @param decay
	 *            The decay length value.
	 * @return This TrapezoidWave instance.
	 */
	public TrapezoidWave setDecay(float decay) {
		decayUGen = null;
		this.decay = decay;
		if (decay < 0) {
			this.decay = 0;
		} else if (decay > 1) {
			this.decay = 1;
		}
		calcVals();
		return this;
	}

	/**
	 * Gets the current decay length.
	 * 
	 * @return The decay length.
	 */
	public float getDecay() {
		return decay;
	}

	/**
	 * Gets the UGen controlling the decay length, if there is one.
	 * 
	 * @return The decay controller UGen.
	 */
	public UGen getDecayUGen() {
		return decayUGen;
	}

}
