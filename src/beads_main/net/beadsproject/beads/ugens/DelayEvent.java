package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * An abstract UGen that waits for a specified amount of time before "triggering"
 * (executing some code). The {@link #trigger()} method is called when the delay
 * time has elapsed. DelayEvent is the base class for {@link DelayTrigger} (used to send
 * Bead messages) and {@link DelayData} (used to send DataBeads).
 * 
 * @author Benito Crawford
 * @version 0.9.5
 */
public abstract class DelayEvent extends UGen {

	/** The duration of the delay in samples. */
	private long sampleDelay;

	/** The current count in samples. */
	private long count;

	/**
	 * Whether to wait until after the frame in which the delay time has
	 * expired.
	 */
	private boolean triggerAfter = false;
	private long threshold;

	/**
	 * Instantiates a new DelayEvent with the specified millisecond delay and
	 * receiver. By default, the object will be triggered at the beginning of
	 * the frame in which the delay time expires.
	 * 
	 * @param context
	 *            The audio context.
	 * @param delay
	 *            The delay time in milliseconds.
	 * @param receiver
	 *            The receiver.
	 */
	public DelayEvent(AudioContext context, double delay) {
		this(context, delay, false);
	}

	/**
	 * Instantiates a new DelayEvent with the specified millisecond delay and
	 * receiver. <code>triggerAfter</code> indicates whether the object is
	 * triggered at the beginning of the frame in which the delay time elapses (
	 * <code>false</code>), or after (<code>true</code>).
	 * 
	 * @param context
	 *            The audio context.
	 * @param delay
	 *            The delay in milliseconds.
	 * @param receiver
	 *            The receiver.
	 * @param triggerAfter
	 *            Whether the object fires just before or just after the delay
	 *            time expires.
	 */
	public DelayEvent(AudioContext context, double delay, boolean triggerAfter) {
		super(context, 0, 0);
		// context.out.addDependent(this); //Ollie - I don't think this should
		// be automatic
		sampleDelay = (long) context.msToSamples(delay);
		reset();
		triggeredAfter(triggerAfter);
	}

	/**
	 * Reset timer to zero.
	 */
	public void reset() {
		count = 0;
	}

	@Override
	public void calculateBuffer() {
		if (sampleDelay - count > threshold) {
			count += bufferSize;
		} else {
			trigger();
		}
	}

	/**
	 * Called when the delay time has elapsed. Implement this method with code
	 * to be executed after the delay.
	 */
	public abstract void trigger();

	/**
	 * Gets the sample delay.
	 * 
	 * @return the sample delay in milliseconds.
	 */
	public double getSampleDelay() {
		return context.samplesToMs(sampleDelay);
	}

	/**
	 * Sets the sample delay; this may cause the DelayEvent to trigger
	 * immediately.
	 * 
	 * @param sampleDelay
	 *            The new sample delay in milliseconds.
	 * @return This DelayEvent instance.
	 */
	public DelayEvent setSampleDelay(float sampleDelay) {
		this.sampleDelay = (long) context.msToSamples(sampleDelay);
		return this;
	}

	/**
	 * Gets the current count.
	 * 
	 * @return The count in milliseconds.
	 */
	public double getCount() {
		return context.samplesToMs(count);
	}

	/**
	 * Returns <code>true</code> if the DelayEvent is triggered during the frame
	 * after the delay time expires; returns <code>false</code> if it is
	 * triggered during the frame in which the delay time expires.
	 * 
	 * @return True or false.
	 */
	public boolean isTriggeredAfter() {
		return triggerAfter;
	}

	/**
	 * Sets whether the Delay event fires during the frame in which the delay
	 * time expires (<code>false</code>, the default), or the frame after (
	 * <code>true</code>).
	 * 
	 * @param f
	 *            Whether to fire after the frame when the delay time expires.
	 * @return This DelayEvent instance.
	 */
	public DelayEvent triggeredAfter(boolean f) {
		triggerAfter = f;
		if (f) {
			threshold = 0;
		} else {
			threshold = bufferSize;
		}
		return this;
	}

}
