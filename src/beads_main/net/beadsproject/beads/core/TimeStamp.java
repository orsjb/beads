/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.core;

/**
 * TimeStamps store time with respect to the current {@link AudioContext}. Specifically, the TimeStamp stores a time step and an index into a buffer. 
 */
public class TimeStamp {

	/** The context. */
	public final AudioContext context;
	
	/** The time step (AudioContext's count of sample frames). */
	public final long timeStep;
	
	/** The index into the sample frame. */
	public final int index;
	
	/** The time ms. */
	private double timeMs;
	
	/** The time samples. */
	private long timeSamples;
	
	/**
	 * Instantiates a new TimeStamp with the given time step, context and buffer index. Use {@link AudioContext#generateTimeStamp(int)} to generate a
	 * TimeStamp for the current time.
	 * 
	 * @param context the AudioContext.
	 * @param timeStep the time step.
	 * @param index the index.
	 */
	public TimeStamp(AudioContext context, long timeStep, int index) {
		this.context = context;
		this.timeStep = timeStep;
		this.index = index;
	}
	
	/**
	 * Instantiates a new TimeStamp with the given time step, context and buffer index. Use {@link AudioContext#generateTimeStamp(int)} to generate a
	 * TimeStamp for the current time.
	 * 
	 * @param context the AudioContext.
	 * @param timeStep the time step.
	 * @param index the index.
	 */
	public TimeStamp(AudioContext context, long timeInSamples) {
		this.context = context;
		timeStep = timeInSamples / context.getBufferSize();
		index = (int)(timeInSamples % context.getBufferSize());
	}

	/**
	 * Gets the time of the TimeStamp in milliseconds.
	 * 
	 * @return the time in milliseconds.
	 */
	public double getTimeMS() {
		timeMs = context.samplesToMs(getTimeSamples());
		return timeMs;
	}

	/**
	 * Gets the time in samples.
	 * 
	 * @return the time in samples.
	 */
	public long getTimeSamples() {
		timeSamples = timeStep * context.getBufferSize() + index;
		return timeSamples;
	}

	public double since(TimeStamp oldest) {
		return getTimeMS() - oldest.getTimeMS();
	}

	public boolean isBefore(TimeStamp other) {
		if(timeStep < other.timeStep) {
			return true;
		} else {
			if(timeStep == other.timeStep && timeSamples < other.timeSamples) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isAfter(TimeStamp other) {
		if(timeStep > other.timeStep) {
			return true;
		} else {
			if(timeStep == other.timeStep && timeSamples > other.timeSamples) {
				return true;
			}
		}
		return false;
	}
	
	public static TimeStamp subtract(AudioContext ac, TimeStamp a, TimeStamp b) {
		return new TimeStamp(ac, a.getTimeSamples() - b.getTimeSamples());
	}
	
	public static TimeStamp add(AudioContext ac, TimeStamp a, TimeStamp b) {
		return new TimeStamp(ac, a.getTimeSamples() + b.getTimeSamples());
	}

}
