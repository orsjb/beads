/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * An easy way to monitor a signal; useful for debugging signal chains.
 * 
 * @beads.category lowlevel
 * @author Benito Crawford
 * @version 0.9.3
 */
public class SignalReporter extends UGen {
	private long interval = 44100, index = 44100, count = 0;
	private long total = 0;
	private float flInt;
	protected String name = "";

	/**
	 * Constructor for a SignalReporter that calls {@link #notify()} at the
	 * specified interval, with the specified name.
	 * 
	 * @param context
	 *            The audio context.
	 * @param reportInterval
	 *            The interval between reports, in milliseconds.
	 * @param name
	 *            The SignalReporter name (used in reports).
	 */
	public SignalReporter(AudioContext context, float reportInterval,
			String name) {
		super(context, 1, 0);
		setInterval(reportInterval);
		index = interval;
		this.name = name;

		context.out.addDependent(this);
	}

	/**
	 * Sets the report interval.
	 * 
	 * @param reportInterval
	 *            The report interval in milliseconds.
	 */
	public void setInterval(float reportInterval) {
		if (reportInterval <= 0)
			reportInterval = 1000;
		flInt = reportInterval;
		interval = (long) context.msToSamples(reportInterval);
	}

	/**
	 * Gets the report interval.
	 * 
	 * @return The report interval in milliseconds.
	 */
	public float getInterval() {
		return flInt;
	}

	public void calculateBuffer() {
		if (index >= interval) {
			notify(count, bufIn[0][0]);
			count++;
			index -= interval;
		}
		total += bufferSize;
		index += bufferSize;
	}

	/**
	 * Called regularly according to the interval length; by default, it outputs
	 * a report (<code>System.out.println</code>)that includes the name, report
	 * #, and first value of the current input signal frame. This can be
	 * overridden to provide other functionality.
	 * 
	 * @param count
	 *            The current report #.
	 * @param value
	 *            The first value in the input signal frame.
	 */
	public void notify(long count, float value) {
		System.out.println("SignalReporter " + name + ", report #" + count
				+ ": " + value);
	}

	/**
	 * Resets the current report # to 0.
	 */
	public void resetCount() {
		count = 0;
	}
}
