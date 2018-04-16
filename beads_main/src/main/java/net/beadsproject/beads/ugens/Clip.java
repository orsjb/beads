/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.DataBead;
import net.beadsproject.beads.data.DataBeadReceiver;

/**
 * Clip constrains a signal within a range. The range may be defined either by
 * static values, or by UGens. Use {@link RangeLimiter} to strictly (and more
 * efficiently) constrain a signal in the range [-1,1].
 * 
 * @beads.category lowlevel
 * @author Benito Crawford
 * @version 0.9.5
 * 
 */
public class Clip extends UGen implements DataBeadReceiver {

	private float min, max;
	private UGen minUGen, maxUGen;
	private boolean isMinStatic, isMaxStatic;

	/**
	 * Constructor for a one-channel Clip.
	 * 
	 * @param context
	 *            The audio context.
	 */
	public Clip(AudioContext context) {
		this(context, 1);
	}

	/**
	 * Constructor for a new Clip with the specified number of channels.
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 */
	public Clip(AudioContext context, int channels) {
		super(context, channels, channels);
		setRange(-1, 1);
	}

	@Override
	public void calculateBuffer() {
		if (isMinStatic && isMaxStatic) {
			for (int j = 0; j < ins; j++) {
				float[] bi = bufIn[j];
				float[] bo = bufOut[j];
				for (int i = 0; i < bufferSize; i++) {
					float y = bi[i];
					if (y < min) {
						bo[i] = min;
					} else if (y > max) {
						bo[i] = max;
					} else {
						bo[i] = y;
					}
				}
			}
		} else {
			minUGen.update();
			maxUGen.update();
			for (int i = 0; i < bufferSize; i++) {
				min = minUGen.getValue(0, i);
				max = maxUGen.getValue(0, i);
				for (int j = 0; j < ins; j++) {
					float y = bufIn[j][i];
					if (y < min) {
						bufOut[j][i] = min;
					} else if (y > max) {
						bufOut[j][i] = max;
					} else {
						bufOut[j][i] = y;
					}
				}
			}
		}
	}

	/**
	 * Sets the range.
	 * 
	 * @param minimum
	 *            The minimum value.
	 * @param maximum
	 *            The maximum value.
	 * @return This Clip instance.
	 */
	public Clip setRange(float minimum, float maximum) {
		setMinimum(minimum);
		setMaximum(maximum);
		return this;
	}

	/**
	 * Gets the current minimum value.
	 * 
	 * @return The minimum value.
	 */
	public float getMinimum() {
		return min;
	}

	/**
	 * Sets the minimum to a static value.
	 * 
	 * @param minimum
	 *            The new minimum value.
	 * @return This Clip instance.
	 */
	public Clip setMinimum(float minimum) {
		this.min = minimum;
		if (isMinStatic) {
			((Static) minUGen).setValue(minimum);
		} else {
			minUGen = new Static(context, minimum);
			isMinStatic = true;
		}
		return this;
	}

	/**
	 * Sets a UGen to control the minimum value.
	 * 
	 * @param minimumUGen
	 *            The minimum value controller UGen.
	 * @return This Clip instance.
	 */
	public Clip setMinimum(UGen minimumUGen) {
		if (minimumUGen == null) {
			setMinimum(min);
		} else {
			minUGen = minimumUGen;
			minUGen.update();
			min = minUGen.getValue();
			isMinStatic = false;
		}
		return this;
	}

	/**
	 * Gets the minimum value controller UGen, if there is one.
	 * 
	 * @return The minimum value controller UGen.
	 */
	public UGen getMinimumUGen() {
		if (isMinStatic) {
			return null;
		} else {
			return minUGen;
		}
	}

	/**
	 * Gets the current maximum value.
	 * 
	 * @return The maximum value.
	 */
	public float getMaximum() {
		return max;
	}

	/**
	 * Sets the maximum to a static value.
	 * 
	 * @param maximum
	 *            The new maximum value.
	 * @return This Clip instance.
	 */
	public Clip setMaximum(float maximum) {
		this.max = maximum;

		if (isMaxStatic) {
			((Static) maxUGen).setValue(maximum);
		} else {
			maxUGen = new Static(context, maximum);
			isMaxStatic = true;
		}
		return this;
	}

	/**
	 * Sets a UGen to control the maximum value.
	 * 
	 * @param maximumUGen
	 *            The maximum value controller UGen.
	 * @return This Clip instance.
	 */
	public Clip setMaximum(UGen maximumUGen) {
		if (maximumUGen == null) {
			setMaximum(max);
		} else {
			maxUGen = maximumUGen;
			maxUGen.update();
			max = maxUGen.getValue();
			isMaxStatic = false;
		}
		return this;
	}

	/**
	 * Gets the maximum value controller UGen, if there is one.
	 * 
	 * @return The maximum value controller UGen.
	 */
	public UGen getMaximumUGen() {
		if (isMaxStatic) {
			return null;
		} else {
			return maxUGen;
		}
	}

	/**
	 * Sets the Clip parameters according to the properties "maximum" and/or
	 * "minimum" in the specified DataBead.
	 * 
	 * @param db
	 *            The parameter DataBead.
	 * @return This DataBeadReceiver instance.
	 */
	public DataBeadReceiver sendData(DataBead db) {
		if (db != null) {
			UGen u = db.getUGen("maximum");
			if (u == null) {
				setMaximum(db.getFloat("maximum", max));
			} else {
				setMaximum(u);
			}

			u = db.getUGen("minimum");
			if (u == null) {
				setMinimum(db.getFloat("minimum", min));
			} else {
				setMinimum(u);
			}
		}
		return this;
	}

	/**
	 * Gets a new DataBead filled with the properties "minimum" and "maximum"
	 * set to the corresponding UGen controllers, if they exist, or to static
	 * values.
	 * 
	 * @return The new parameter DataBead.
	 */
	public DataBead getParams() {
		DataBead db = new DataBead();
		if (isMinStatic) {
			db.put("minimum", min);
		} else {
			db.put("minimum", minUGen);
		}

		if (isMaxStatic) {
			db.put("maximum", max);
		} else {
			db.put("maximum", maxUGen);
		}

		return db;
	}

	/**
	 * Gets a new DataBead filled with the properties "minimum" and "maximum"
	 * set to their current values.
	 * 
	 * @return The new DataBead.
	 */
	public DataBead getStaticParams() {
		DataBead db = new DataBead();
		db.put("minimum", min);
		db.put("maximum", max);
		return db;
	}

}
