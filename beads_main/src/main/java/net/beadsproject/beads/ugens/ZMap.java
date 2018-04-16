/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.DataBead;
import net.beadsproject.beads.data.DataBeadReceiver;

/**
 * Performs a simple linear map from one range of values to another. Can be
 * controlled by specifying the ranges from and to which to map, or by
 * specifying a multiplier and shift (addition) value. Optionally, the signal
 * can be clipped to the specified range.
 * 
 * @beads.category lowlevel
 * @author Benito Crawford
 * @version 0.9.5
 */
public class ZMap extends UGen implements DataBeadReceiver {

	private int channels;
	private float a = 1, b = 0;
	private float o1 = 0, o2 = 1, n1 = 0, n2 = 1;
	private boolean clip = false;
	private boolean flipped = false;

	/**
	 * Constructor for a 1-channel mapping object with default parameters
	 * (mapping [0,1] to [0,1] with no clipping, or multiplying by 1 and adding
	 * 0).
	 * 
	 * @param context
	 *            The audio context.
	 */
	public ZMap(AudioContext context) {
		this(context, 1);
	}

	/**
	 * Constructor for a mapping object with the specified number of channels
	 * and the default parameters (mapping [0,1] to [0,1] with no clipping, or
	 * multiplying by 1 and adding 0).
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 */
	public ZMap(AudioContext context, int channels) {
		super(context, channels, channels);
		this.channels = channels;
		clear();
	}

	@Override
	public void calculateBuffer() {
		for (int j = 0; j < channels; j++) {
			float[] bi = bufIn[j];
			float[] bo = bufOut[j];

			if (clip) {
				float y;
				if (flipped) {
					for (int i = 0; i < bufferSize; i++) {
						y = bi[i];
						if (y < o2) {
							y = o2;
						} else if (y > o1) {
							y = o1;
						}
						bo[i] = a * y + b;
					}
				} else {
					for (int i = 0; i < bufferSize; i++) {
						y = bi[i];
						if (y > o2) {
							y = o2;
						} else if (y < o1) {
							y = o1;
							bo[i] = a * y + b;
						}
					}

				}
			} else {
				for (int i = 0; i < bufferSize; i++) {
					bo[i] = a * bi[i] + b;
				}
			}

		}
	}

	/**
	 * Gets the "minimum" expected value for the incoming signal.
	 * 
	 * @return The "minimum" expected value.
	 */
	public float getSourceMinimum() {
		return o1;
	}

	/**
	 * Sets the "minimum" expected value for the incoming signal.
	 * 
	 * @param sourceMin
	 *            The "minimum" expected value.
	 * @return This ZMap instance.
	 */
	public ZMap setSourceMinimum(float sourceMin) {
		if (sourceMin == o2) {
			o2 = sourceMin + .0000000001f;
		}
		return setRanges(sourceMin, o2, n1, n2);
	}

	/**
	 * Gets the "maximum" expected value for the incoming signal.
	 * 
	 * @return The "maximum" expected value.
	 */
	public float getSourceMaximum() {
		return o2;
	}

	/**
	 * Sets the "maximum" expected value for the incoming signal.
	 * 
	 * @param sourceMax
	 *            The "maximum" expected value.
	 * @return This ZMap instance.
	 */
	public ZMap setSourceMaximun(float sourceMax) {
		return setRanges(o1, sourceMax, n1, n2);
	}

	/**
	 * Gets the "minimum" target value for the outgoing signal.
	 * 
	 * @return The "minimum" target value.
	 */
	public float getTargetMinimum() {
		return n1;
	}

	/**
	 * Sets the "minimum" value for the output signal.
	 * 
	 * @param targetMin
	 *            The "minimum" output value.
	 * @return This ZMap instance.
	 */
	public ZMap setTargetMinimum(float targetMin) {
		return setRanges(o1, o2, targetMin, n2);
	}

	/**
	 * Gets the "maximum" target value for the outgoing signal.
	 * 
	 * @return The "maximum" target value.
	 */
	public float getTargetMaximum() {
		return n2;
	}

	/**
	 * Sets the "maximum" value for the output signal.
	 * 
	 * @param targetMax
	 *            The "maximum" output value.
	 * @return This ZMap instance.
	 */

	public ZMap setTargetMaximum(float targetMax) {
		return setRanges(o1, o2, n1, targetMax);
	}

	/**
	 * Sets the source and target ranges for the signal mapping.
	 * 
	 * @param sourceMin
	 *            The "minimum" incoming value.
	 * @param sourceMax
	 *            The "maximum" incoming value.
	 * @param targetMin
	 *            The "minimum" outgoing value.
	 * @param targetMax
	 *            The "maximum" outgoing value.
	 * @return This ZMap instance.
	 */
	public ZMap setRanges(float sourceMin, float sourceMax, float targetMin,
			float targetMax) {
		if (sourceMin == sourceMax) {
			sourceMin = sourceMax - .0000000001f;
		}

		o1 = sourceMin;
		o2 = sourceMax;
		n1 = targetMin;
		n2 = targetMax;

		if (o1 > o2) {
			flipped = true;
		} else {
			flipped = false;
		}

		a = (targetMax - targetMin) / (sourceMin - sourceMax);
		b = targetMin - a * sourceMin;

		return this;
	}

	/**
	 * Returns ZMap to its default setting: multiply by 1, add 0 (mapping [0,1]
	 * to [0,1] with no clipping).
	 * 
	 * @return This ZMap instance.
	 */
	public ZMap clear() {
		o1 = 0;
		o2 = 1;
		n1 = 0;
		n2 = 1;
		a = 1;
		b = 0;
		flipped = false;
		clip = false;
		return this;
	}

	/**
	 * Sets the multiplier and the shift.
	 * 
	 * @param multiplier
	 *            The value to multiply by the incoming signal.
	 * @param shift
	 *            The value to then add to the result.
	 * @return This ZMap instance.
	 */
	public ZMap multiplyThenAdd(float multiplier, float shift) {

		a = multiplier;
		b = shift;

		n1 = a * o1 + b;
		n2 = a * o2 + b;

		return this;
	}

	/**
	 * A convenience method; specifies a linear map by adding first, then
	 * multiplying.
	 * 
	 * @param preshift
	 *            The value to first add to the signal.
	 * @param multiplier
	 *            The value then multiplied by the result.
	 * @return This ZMap instance.
	 */
	public ZMap addThenMultiply(float preshift, float multiplier) {
		return multiplyThenAdd(multiplier, multiplier * preshift);
	}

	/**
	 * Gets the value that is multiplied by the signal.
	 * 
	 * @return The multiplier.
	 */
	public float getMultiplier() {
		return a;
	}

	/**
	 * Sets the value that is multiplied by the signal.
	 * 
	 * @param multiplier
	 *            The multiplier
	 * @return This ZMap instance.
	 */
	public ZMap setMultiplier(float multiplier) {
		return multiplyThenAdd(multiplier, b);
	}

	/**
	 * Gets the value that is added to the signal after it has been multiplied
	 * by the multiplier.
	 * 
	 * @return The shift value.
	 */
	public float getShift() {
		return b;
	}

	/**
	 * Sets the value to add to the signal after it has been multiplied by the
	 * multiplier.
	 * 
	 * @param shift
	 *            The amount to add.
	 * @return This ZMap instance.
	 */
	public ZMap setShift(float shift) {
		return multiplyThenAdd(a, shift);
	}

	/**
	 * Gets whether ZMap clips the incoming values to lie within the specified
	 * range.
	 * 
	 * @return Whether ZMap clips.
	 */
	public boolean getClipping() {
		return clip;
	}

	/**
	 * Specifies whether ZMap clips the incoming values to lie within the
	 * specified range.
	 * 
	 * @param clip
	 *            Whether to clip.
	 * @return This ZMap instance.
	 */
	public ZMap setClipping(boolean clip) {
		this.clip = clip;
		return this;
	}

	/**
	 * Gets the number of channels for this ZMap instance.
	 * 
	 * @return The number of channels.
	 */
	public int getChannels() {
		return channels;
	}

	/**
	 * Sets the ZMap parameters with a DataBead, using the following properties:
	 * "sourceMinimum", "sourceMaximum", "targetMinimum", "targetMaximum",
	 * "multiplier", "shift", "clipping".
	 * 
	 * @param db 
	 * 			The parameter DataBead.
	 * @return This ZMap instance.
	 */
	public DataBeadReceiver sendData(DataBead db) {
		if (db != null) {
			setRanges(db.getFloat("sourceMinimum", o1), db.getFloat(
					"sourceMaximum", o2), db.getFloat("targetMinimum", n1), db
					.getFloat("targetMaximum", n2));
			multiplyThenAdd(db.getFloat("multiplier", a), db.getFloat("shift",
					b));
			Object o = db.get("clipping");
			if (o instanceof Boolean) {
				setClipping((Boolean) o);
			}
		}
		return this;
	}

	/**
	 * Gets a new DataBead filled with current parameter values.
	 * 
	 * @return The new parameter DataBead.
	 */
	public DataBead getParams() {
		return getStaticParams();
	}

	/**
	 * Gets a new DataBead filled with current parameter values.
	 * 
	 * @return The new parameter DataBead.
	 */
	public DataBead getStaticParams() {
		DataBead db = new DataBead();
		db.put("sourceMinimum", o1);
		db.put("sourceMaximum", o2);
		db.put("targetMinimum", n1);
		db.put("targetMaximum", n2);
		db.put("multiplier", a);
		db.put("shift", b);
		db.put("clipping", clip);

		return db;
	}

}
