/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.DataBead;
import net.beadsproject.beads.data.DataBeadReceiver;

/**
 * A simple wave-shaper. WaveShaper takes an incoming signal, maps it onto a
 * stored wave shape, and outputs the result. For each sample it:
 * <p>
 * <ol>
 * <li>Multiplies by the <code>preGain</code>.</li>
 * <li>Clips between -1 and 1, and maps onto the stored wave shape, with linear
 * interpolation.</li>
 * <li>Multiplies by the <code>postGain</code>.</li>
 * <li>Limits the result between <code>-limit</code> and <code>limit</code>.</li>
 * <li>Mixes the result with the original signal according to
 * <code>wetMix</code>. (1 is none of the original signal; 0 is only the
 * original signal.)</li>
 * </ol>
 * <p>
 * This UGen is a {@link DataBeadReceiver}, so you can set its parameters with a
 * DataBead.
 * </p>
 * 
 * @beads.category synth
 * @author Benito Crawford
 * @version 0.9.5
 */
public class WaveShaper extends UGen implements DataBeadReceiver {

	protected float preGain = 2, postGain = 1, limit = 1, wetMix = 1;
	protected UGen preGainUGen, postGainUGen, limitUGen, wetMixUGen;
	protected boolean isPreGainStatic = false, isPostGainStatic = false,
			isLimitStatic = false, isWetMixStatic = false;

	protected float[] shape;
	protected int shapeLen;
	protected int channels = 1;

	/**
	 * Constructor for a mono-channel wave shaper that uses a default
	 * cosine-based wave shape.
	 * 
	 * @param context
	 *            The audio context.
	 */
	public WaveShaper(AudioContext context) {
		this(context, 1);
	}

	/**
	 * Constructor for a multi-channel wave shaper that uses a default
	 * cosine-based wave shape.
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 */
	public WaveShaper(AudioContext context, int channels) {
		super(context, channels, channels);
		this.channels = channels;
		setPreGain(2).setPostGain(1).setLimit(1).setWetMix(1);
		setShape(generateCosineShape(1025));
	}

	/**
	 * Constructor for a mono-channel wave shaper that uses the provided float
	 * array for its wave shape.
	 * 
	 * @param context
	 *            The audio context.
	 * @param shape
	 *            The float array.
	 */
	public WaveShaper(AudioContext context, float[] shape) {
		this(context);
		setShape(shape);
	}

	/**
	 * Constructor for a multi-channel wave shaper that uses the provided float
	 * array for its wave shape.
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 * @param shape
	 *            The float array.
	 */
	public WaveShaper(AudioContext context, int channels, float[] shape) {
		this(context, channels);
		setShape(shape);
	}

	/**
	 * Constructor for a mono-channel wave shaperthat uses the float array from
	 * a Buffer for its wave shape.
	 * 
	 * @param context
	 *            The audio context.
	 * @param shapeBuffer
	 *            The Buffer from which to get the wave shape.
	 */
	public WaveShaper(AudioContext context, Buffer shapeBuffer) {
		this(context);
		setShape(shapeBuffer.buf);
	}

	/**
	 * Constructor for a multi-channel wave shaper that uses the float array
	 * from a Buffer for its wave shape.
	 * 
	 * @param context
	 *            The audio context.
	 * @param channels
	 *            The number of channels.
	 * @param shapeBuffer
	 *            The Buffer from which to get the wave shape.
	 */
	public WaveShaper(AudioContext context, int channels, Buffer shapeBuffer) {
		this(context, channels);
		setShape(shapeBuffer.buf);
	}

	/**
	 * Generates a nice cosine-based waveform in a float array that will provide
	 * a little warmth when used for wave-shaping.
	 * 
	 * @param length
	 *            The length of the array.
	 * @return The generated array.
	 */
	public static float[] generateCosineShape(int length) {
		float[] ret = new float[length];
		int l = length - 1;
		for (int i = 1; i < length - 1; i++) {
			ret[i] = 0 - (float) Math.cos((float) i * Math.PI / l);
		}
		ret[0] = -1;
		ret[length - 1] = 1;
		if (length % 2 == 1) {
			ret[l / 2] = 0;
		}
		return ret;
	}

	/**
	 * Generates an exponentially-based waveform in a float array. Negative
	 * input results in negative output.
	 * 
	 * @param length
	 *            The length of the array.
	 * @param exponent
	 *            The exponent.
	 * @return The generated array.
	 */
	public static float[] generateExponentialShape(int length, float exponent) {
		float[] ret = new float[length];

		int l = length - 1;
		for (int i = 1; i < length - 1; i++) {
			float x = (i / l) * 2 - 1;
			if (x < 0) {
				ret[i] = (float) -Math.pow(-x, exponent);
			} else if (x == 0) {
				ret[i] = 0;
			} else {
				ret[i] = (float) Math.pow(x, exponent);
			}
		}
		ret[0] = -1;
		ret[length - 1] = 1;
		if (length % 2 == 1) {
			ret[l / 2] = 0;
		}
		return ret;
	}

	public void calculateBuffer() {
		preGainUGen.update();
		postGainUGen.update();
		limitUGen.update();
		wetMixUGen.update();

		if (channels == 1) {
			float[] bi = bufIn[0];
			float[] bo = bufOut[0];

			for (int currsample = 0; currsample < bufferSize; currsample++) {
				preGain = preGainUGen.getValue(0, currsample);
				postGain = postGainUGen.getValue(0, currsample);
				limit = limitUGen.getValue(0, currsample);
				if (limit < 0)
					limit = 0;
				wetMix = wetMixUGen.getValue(0, currsample);

				float y, y2;
				float y1 = (((y = bi[currsample]) * preGain * .5f) + .5f)
						* shapeLen;

				if (y1 <= 0) {
					y2 = shape[0] * postGain;
				} else if (y1 >= shapeLen) {
					y2 = shape[shapeLen] * postGain;
				} else {
					int ind = (int) y1;
					float frac = y1 - ind;
					y2 = (shape[ind] * (1 - frac) + shape[ind + 1] * frac)
							* postGain;
				}
				// System.out.println("#1: " + y1 + ", " + y2);

				if (y2 > limit) {
					y2 = limit;
				} else if (y2 < -limit) {
					y2 = -limit;
				}

				bo[currsample] = y * (1 - wetMix) + y2 * wetMix;
			}

		} else {

			// multi-channel version

			for (int currsample = 0; currsample < bufferSize; currsample++) {
				preGain = preGainUGen.getValue(0, currsample);
				postGain = postGainUGen.getValue(0, currsample);
				limit = limitUGen.getValue(0, currsample);
				if (limit < 0)
					limit = 0;
				wetMix = wetMixUGen.getValue(0, currsample);

				float y, y2;
				for (int currchannel = 0; currchannel < channels; currchannel++) {
					float y1 = (((y = bufIn[currchannel][currsample]) * preGain * .5f) + .5f)
							* shapeLen;

					if (y1 <= 0) {
						y2 = shape[0] * postGain;
					} else if (y1 >= shapeLen) {
						y2 = shape[shapeLen] * postGain;
					} else {
						int ind = (int) y1;
						float frac = y1 - ind;
						y2 = (shape[ind] * (1 - frac) + shape[ind + 1] * frac)
								* postGain;
					}

					if (y2 > limit) {
						y2 = limit;
					} else if (y2 < -limit) {
						y2 = -limit;
					}

					bufOut[currchannel][currsample] = y * (1 - wetMix) + y2
							* wetMix;
				}
			}
		}

	}

	/**
	 * Gets the current pre-gain.
	 * 
	 * @return The pre-gain.
	 */
	public float getPreGain() {
		return preGain;
	}

	/**
	 * Sets the pre-gain to a static float value.
	 * 
	 * @param preGain
	 *            The pre-gain.
	 * @return This object instance.
	 */
	public WaveShaper setPreGain(float preGain) {
		this.preGain = preGain;
		if (isPreGainStatic) {
			preGainUGen.setValue(preGain);
		} else {
			preGainUGen = new Static(context, preGain);
			isPreGainStatic = true;
		}
		return this;
	}

	/**
	 * Sets the pre-gain to be controlled by a UGen.
	 * 
	 * @param preGainUGen
	 *            The pre-gain UGen.
	 * @return This object instance.
	 */
	public WaveShaper setPreGain(UGen preGainUGen) {
		if (preGainUGen == null) {
			setPreGain(preGain);
		} else {
			this.preGainUGen = preGainUGen;
			preGainUGen.update();
			preGain = preGainUGen.getValue();
			isPreGainStatic = false;
		}
		return this;
	}

	/**
	 * Gets the pre-gain UGen, if there is one.
	 * 
	 * @return The pre-gain UGen.
	 */
	public UGen getPreGainUGen() {
		if (isPreGainStatic) {
			return null;
		} else {
			return preGainUGen;
		}
	}

	/**
	 * Gets the current post-gain.
	 * 
	 * @return The post-gain.
	 */
	public float getPostGain() {
		return postGain;
	}

	/**
	 * Sets the post-gain to a static float value.
	 * 
	 * @param postGain
	 *            The post-gain.
	 * @return This object instance.
	 */
	public WaveShaper setPostGain(float postGain) {
		this.postGain = postGain;
		if (isPostGainStatic) {
			postGainUGen.setValue(postGain);
		} else {
			postGainUGen = new Static(context, postGain);
			isPostGainStatic = true;
		}
		return this;
	}

	/**
	 * Sets the post-gain to be controlled by a UGen.
	 * 
	 * @param postGainUGen
	 *            The post-gain UGen.
	 * @return This object instance.
	 */
	public WaveShaper setPostGain(UGen postGainUGen) {
		if (postGainUGen == null) {
			setPostGain(postGain);
		} else {
			this.postGainUGen = postGainUGen;
			postGainUGen.update();
			postGain = postGainUGen.getValue();
			isPostGainStatic = false;
		}
		return this;
	}

	/**
	 * Gets the post-gain UGen, if there is one.
	 * 
	 * @return The post-gain UGen.
	 */
	public UGen getPostGainUGen() {
		if (isPostGainStatic) {
			return null;
		} else {
			return postGainUGen;
		}
	}

	/**
	 * Gets the current limit.
	 * 
	 * @return The limit.
	 */
	public float getLimit() {
		return limit;
	}

	/**
	 * Sets the limit to a static float value.
	 * 
	 * @param limit
	 *            The limit.
	 * @return This object instance.
	 */
	public WaveShaper setLimit(float limit) {
		if (limit < 0)
			limit = 0;
		this.limit = limit;
		if (isLimitStatic) {
			limitUGen.setValue(limit);
		} else {
			limitUGen = new Static(context, limit);
			isLimitStatic = true;
		}
		return this;
	}

	/**
	 * Sets the limit to be controlled by a UGen.
	 * 
	 * @param limitUGen
	 *            The limit UGen.
	 * @return This object instance.
	 */
	public WaveShaper setLimit(UGen limitUGen) {
		if (limitUGen == null) {
			setLimit(limit);
		} else {
			this.limitUGen = limitUGen;
			limitUGen.update();
			limit = limitUGen.getValue();
			isLimitStatic = false;
		}
		return this;
	}

	/**
	 * Gets the limit UGen, if there is one.
	 * 
	 * @return The limit UGen.
	 */
	public UGen getLimitUGen() {
		if (isLimitStatic) {
			return null;
		} else {
			return limitUGen;
		}
	}

	/**
	 * Gets the current wet-mix.
	 * 
	 * @return The wet-mix.
	 */
	public float getWetMix() {
		return wetMix;
	}

	/**
	 * Sets the wet-mix to a static float value.
	 * 
	 * @param wetMix
	 *            The wetMix.
	 * @return This object instance.
	 */
	public WaveShaper setWetMix(float wetMix) {
		this.wetMix = wetMix;
		if (isWetMixStatic) {
			wetMixUGen.setValue(wetMix);
		} else {
			wetMixUGen = new Static(context, wetMix);
			isWetMixStatic = true;
		}
		return this;
	}

	/**
	 * Sets the wetMix to be controlled by a UGen.
	 * 
	 * @param wetMixUGen
	 *            The wet-mix UGen.
	 * @return This object instance.
	 */
	public WaveShaper setWetMix(UGen wetMixUGen) {
		if (wetMixUGen == null) {
			setWetMix(wetMix);
		} else {
			this.wetMixUGen = wetMixUGen;
			wetMixUGen.update();
			wetMix = wetMixUGen.getValue();
			isWetMixStatic = false;
		}
		return this;
	}

	/**
	 * Gets the wet-mix UGen, if there is one.
	 * 
	 * @return The wet-mix UGen.
	 */
	public UGen getWetMixUGen() {
		if (isWetMixStatic) {
			return null;
		} else {
			return wetMixUGen;
		}
	}

	/**
	 * Gets the array of floats that represents the wave used for shaping.
	 * 
	 * @return The wave shape.
	 */
	public float[] getShape() {
		return shape;
	}

	/**
	 * Sets the array of floats to be used for WaveShaping.
	 * 
	 * @param shape
	 *            The array of floats.
	 * @return This object instance.
	 */
	public WaveShaper setShape(float[] shape) {
		if (shape != null && shape.length > 1) {
			this.shape = shape;
			shapeLen = shape.length - 1;
		}
		return this;
	}

	/**
	 * Sets parameters with a DataBead. If the DataBead any of the properties
	 * "preGain", "postGain", "limit", "wetMix", or "shape", it sets the
	 * corresponding parameters accordingly. Each property (except "shape",
	 * which should be a float array) can be specified by either a float or a
	 * controller UGen.
	 */
	public DataBeadReceiver sendData(DataBead db) {
		UGen u;
		if (db != null) {
			if ((u = db.getUGen("preGain")) != null) {
				setPreGain(u);
			} else {
				setPreGain(db.getFloat("preGain", preGain));
			}

			if ((u = db.getUGen("postGain")) != null) {
				setPostGain(u);
			} else {
				setPostGain(db.getFloat("postGain", postGain));
			}

			if ((u = db.getUGen("limit")) != null) {
				setLimit(u);
			} else {
				setLimit(db.getFloat("limit", limit));
			}

			if ((u = db.getUGen("wetMix")) != null) {
				setWetMix(u);
			} else {
				setWetMix(db.getFloat("wetMix", wetMix));
			}
			setShape(db.getFloatArray("shape"));
		}
		return this;
	}

	/**
	 * Gets a new DataBead filled with the various parameter values stored in
	 * the properties "preGain", "postGain", "limit", "wetMix", and "shape".
	 * Stores UGens if the parameter is not static.
	 * 
	 * @return The new DataBead.
	 */
	public DataBead getParams() {
		DataBead db = new DataBead();

		if (isPreGainStatic) {
			db.put("preGain", preGain);
		} else {
			db.put("preGain", preGainUGen);
		}

		if (isPostGainStatic) {
			db.put("postGain", postGain);
		} else {
			db.put("postGain", postGainUGen);
		}

		if (isLimitStatic) {
			db.put("limit", limit);
		} else {
			db.put("limit", limitUGen);
		}

		if (isWetMixStatic) {
			db.put("wetMix", wetMix);
		} else {
			db.put("wetMix", wetMixUGen);
		}

		db.put("shape", shape);

		return db;
	}

	/**
	 * Gets a new DataBead filled with the various parameter values stored in
	 * the properties "preGain", "postGain", "limit", "wetMix", and "shape".
	 * Stores current values as floats only, even if the parameter is controlled
	 * by a UGen.
	 * 
	 * @return The new DataBead.
	 */
	public DataBead getStaticParams() {

		DataBead db = new DataBead();
		db.put("preGain", preGain);
		db.put("postGain", postGain);
		db.put("limit", limit);
		db.put("wetMix", wetMix);
		db.put("shape", shape);
		return db;
	}
	// end of class
}
