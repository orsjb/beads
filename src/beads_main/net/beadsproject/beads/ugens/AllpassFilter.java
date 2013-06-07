/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.*;
import net.beadsproject.beads.data.*;

/**
 * A simple all-pass filter with variable delay. Implements the following
 * formula: Y(n) = X(n-d) + g * (Y(n-d) - X(n)),
 * 
 * for delay time <em>d</em> and factor <em>g</em>.
 * 
 * @beads.category filter
 * @author Benito Crawford
 * @version 0.9.5
 */
public class AllpassFilter extends IIRFilter implements DataBeadReceiver {

	protected float g;
	protected int maxDelay = 1, delay = 1, ind = 0, bufLen;
	protected UGen delayUGen, gUGen;
	protected boolean isDelayStatic, isGStatic;
	protected float[] xn, yn;

	/**
	 * Constructor with delay and g specified by floats.
	 * 
	 * @param context
	 *            The AudioContext.
	 * @param maxdel
	 *            The maximum delay in samples; cannot be changed.
	 * @param idel
	 *            The initial delay in samples.
	 * @param ig
	 *            The initial g parameter.
	 */
	public AllpassFilter(AudioContext context, int maxdel, int idel, float ig) {
		this(context, maxdel);
		setDelay(idel).setG(ig);
	}

	/**
	 * Constructor with delay specified by a UGen and g specified by a float.
	 * 
	 * @param context
	 *            The AudioContext.
	 * @param maxdel
	 *            The maximum delay in samples; cannot be changed.
	 * @param idel
	 *            The delay UGen.
	 * @param ig
	 *            The initial g parameter.
	 */
	public AllpassFilter(AudioContext context, int maxdel, UGen idel, float ig) {
		this(context, maxdel);
		setDelay(idel).setG(ig);
	}

	/**
	 * Constructor with delay specified by a float and g specified by a UGen.
	 * 
	 * @param context
	 *            The AudioContext.
	 * @param maxdel
	 *            The maximum delay in samples; cannot be changed.
	 * @param idel
	 *            The initial delay in samples.
	 * @param ig
	 *            The g UGen.
	 */
	public AllpassFilter(AudioContext context, int maxdel, int idel, UGen ig) {
		this(context, maxdel);
		setDelay(idel).setG(ig);
	}

	/**
	 * Constructor with delay and g specified by UGens.
	 * 
	 * @param context
	 *            The AudioContext.
	 * @param maxdel
	 *            The maximum delay in samples; cannot be changed.
	 * @param idel
	 *            The delay UGen.
	 * @param ig
	 *            The g UGen.
	 */
	public AllpassFilter(AudioContext context, int maxdel, UGen idel, UGen ig) {
		this(context, maxdel);
		setDelay(idel).setG(ig);
	}

	private AllpassFilter(AudioContext context, int maxdel) {
		super(context, 1, 1);
		maxDelay = Math.max(maxdel, 1);
		bufLen = maxDelay + 1;
		xn = new float[bufLen];
		yn = new float[bufLen];

	}

	@Override
	public void calculateBuffer() {

		float[] bi = bufIn[0];
		float[] bo = bufOut[0];

		if (isDelayStatic && isGStatic) {

			int ind2 = (ind + bufLen - delay) % bufLen;
			for (int currsample = 0; currsample < bufferSize; currsample++) {
				bo[currsample] = yn[ind] = xn[ind2] + g
						* (yn[ind2] - (xn[ind] = bi[currsample]));
				ind2 = (ind2 + 1) % bufLen;
				ind = (ind + 1) % bufLen;
			}

		} else {

			gUGen.update();
			delayUGen.update();

			for (int currsample = 0; currsample < bufferSize; currsample++) {
				if ((delay = (int) gUGen.getValue(0, currsample)) < 1) {
					delay = 1;
				} else if (delay > maxDelay) {
					delay = maxDelay;
				}
				int ind2 = (ind + bufLen - delay) % bufLen;
				bo[currsample] = yn[ind] = xn[ind2]
						+ gUGen.getValue(0, currsample)
						* (yn[ind2] - (xn[ind] = bi[currsample]));

				ind = (ind + 1) % bufLen;
			}
			g = gUGen.getValue(0, bufferSize - 1);
		}

	}

	/**
	 * Gets the current g parameter.
	 * 
	 * @return The g parameter.
	 */
	public float getG() {
		return g;
	}

	/**
	 * Sets the g parameter. This clears the g UGen if there is one.
	 * 
	 * @param g
	 *            The g parameter.
	 * @return This filter instance.
	 */
	public AllpassFilter setG(float g) {
		this.g = g;
		if (isGStatic) {
			gUGen.setValue(g);
		} else {
			gUGen = new Static(context, g);
			isGStatic = true;
		}
		return this;
	}

	/**
	 * Sets a UGen to determine the g value.
	 * 
	 * @param g
	 *            The g UGen.
	 * @return This filter instance.
	 */
	public AllpassFilter setG(UGen g) {
		if (g == null) {
			setG(this.g);
		} else {
			gUGen = g;
			g.update();
			this.g = g.getValue();
			isGStatic = false;
		}
		return this;
	}

	/**
	 * Gets the g UGen, if there is one.
	 * 
	 * @return The g UGen.
	 */
	public UGen getGUGen() {
		if (isGStatic) {
			return null;
		} else {
			return gUGen;
		}
	}

	/**
	 * Gets the current delay in samples.
	 * 
	 * @return The delay in samples.
	 */
	public int getDelay() {
		return delay;
	}

	/**
	 * Sets the delay.
	 * 
	 * @param del
	 *            The delay in samples. This will remove the delay UGen if there
	 *            is one.
	 * @return This filter instance.
	 */
	public AllpassFilter setDelay(int del) {
		if (del > maxDelay) {
			delay = maxDelay;
		} else if (del < 1) {
			delay = 1;
		} else {
			delay = del;
		}
		if (isDelayStatic) {
			delayUGen.setValue(delay);
		} else {
			delayUGen = new Static(context, delay);
			isDelayStatic = true;
		}
		return this;
	}

	/**
	 * Sets a UGen to determine the delay in samples. Delay times are converted
	 * to integers. Passing a null value freezes the delay at its current value.
	 * 
	 * @param del
	 *            The delay UGen.
	 * @return This filter instance.
	 */
	public AllpassFilter setDelay(UGen del) {
		if (del == null) {
			setDelay(delay);
		} else {
			delayUGen = del;
			del.update();
			if ((delay = (int) del.getValue()) < 0) {
				delay = 0;
			} else if (delay > maxDelay) {
				delay = maxDelay;
			}
			isDelayStatic = false;
		}
		return this;
	}

	/**
	 * Gets the delay UGen, if there is one.
	 * 
	 * @return The delay UGen.
	 */
	public UGen getDelayUGen() {
		if (isDelayStatic) {
			return null;
		} else {
			return delayUGen;
		}
	}

	/**
	 * Sets the filter parameters with a DataBead.
	 * <p>
	 * Use the following properties to specify filter parameters:
	 * </p>
	 * <ul>
	 * <li>"delay": (float or UGen)</li>
	 * <li>"g": (float or UGen)</li>
	 * </ul>
	 * 
	 * @param paramBead
	 *            The DataBead specifying parameters.
	 * @return This filter instance.
	 */
	public AllpassFilter setParams(DataBead paramBead) {
		if (paramBead != null) {
			Object o;

			if ((o = paramBead.get("delay")) != null) {
				if (o instanceof UGen) {
					setDelay((UGen) o);
				} else {
					setDelay((int) paramBead.getFloat("delay", delay));
				}
			}

			if ((o = paramBead.get("g")) != null) {
				if (o instanceof UGen) {
					setG((UGen) o);
				} else {
					setG(paramBead.getFloat("g", g));
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
	 * Gets a DataBead with properties "delay" and "g" set to the corresponding
	 * filter parameters.
	 * 
	 * @return The parameter DataBead.
	 */
	public DataBead getParams() {
		DataBead db = new DataBead();
		if (isDelayStatic) {
			db.put("delay", delay);
		} else {
			db.put("delay", delayUGen);
		}

		if (isGStatic) {
			db.put("g", g);
		} else {
			db.put("g", gUGen);
		}

		return db;
	}

	/**
	 * Gets a DataBead with properties "delay" and "g" set to static float
	 * values corresponding to the current filter parameters.
	 * 
	 * @return The static parameter DataBead.
	 */
	public DataBead getStaticParams() {
		DataBead db = new DataBead();
		db.put("delay", delay);
		db.put("g", g);
		return db;
	}

	/**
	 * Sets the filter's parameters with a DataBead.
	 * 
	 * @return This filter instance.
	 * @see #setParams(DataBead)
	 */
	public DataBeadReceiver sendData(DataBead db) {
		setParams(db);
		return this;
	}

	@Override
	public IIRFilterAnalysis getFilterResponse(float freq) {
		float[] as = new float[delay + 1], bs = new float[delay + 1];
		bs[0] = as[delay] = -g;
		as[0] = bs[delay] = 1;
		return calculateFilterResponse(bs, as, freq, context.getSampleRate());
	}

}
