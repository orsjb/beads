/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.analysis;

import java.util.ArrayList;
import java.util.List;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.TimeStamp;
import net.beadsproject.beads.core.UGen;

/**
 * An AudioSegmenter slices incoming audio data into chunks, as implemented by subclasses, 
 * usually in their calculateBuffer method, and then notifies any FeatureExtractors or
 * other SegmentListeners that are listening. 
 * 
 * @author ollie
 *
 */
public abstract class AudioSegmenter extends UGen implements SegmentMaker {
	
	/** The set of FeatureExtractors that respond to this Segmenter. */
	private ArrayList<FeatureExtractor<?, float[]>> listeners;
	
	/** The set of SegmentListener who are triggered by this Segmenter. */
	private List<SegmentListener> segmentListeners;
	
	/**
	 * Instantiates a new Segmenter.
	 * 
	 * @param context the AudioContext.
	 */
	public AudioSegmenter(AudioContext context) {
		super(context, 1, 0);		
		listeners = new ArrayList<FeatureExtractor<?, float[]>>();
		segmentListeners = new ArrayList<SegmentListener>();
	}
	
	/**
	 * Adds a FeatureExtractor as a responder to this Segmenter.
	 * 
	 * @param fe the FeatureExtractor.
	 */
	public void addListener(FeatureExtractor<?, float[]> fe) {
		listeners.add(fe);
	}
	
	/**
	 * Adds a SegmentListener as a listener to this Segmenter.
	 * 
	 * @param sl the SegmentListener.
	 */
	public void addSegmentListener(SegmentListener sl) {
		segmentListeners.add(sl);
	}

	/**
	 * Removes a SegmentListerner as a listener to this Segmenter.
	 * 
	 * @param sl the SegmentListerner.
	 */
	public void removeSegmentListener(SegmentListener sl) {
		segmentListeners.add(sl);
	}

	/**
	 * Called by instantiations of Segmenter, to indicate that a new segment has been created. 
	 * 
	 * @param startTime double indicating the start time of the data chunk in milliseconds.
	 * @param endTime double indicating the end time of the data chunk in milliseconds.
	 * @param data the audio data.
	 */
	protected void segment(TimeStamp startTime, TimeStamp endTime, float[] data) {
		if(data != null) {
			for(FeatureExtractor<?, float[]> fe : listeners) {
				fe.process(startTime, endTime, data);
			}
		}
		for(SegmentListener recorder : segmentListeners) {
			recorder.newSegment(startTime, endTime);
		}
	}
	

	/**
	 * Reset both beginning and last time stamps to zero.
	 */
	public abstract void resetTimeStamp();	
	
	/** Set the TimeStamp of this AudioSegmenter when the AudioContext is at t=0. 
	 * @param ts
	 */
	public abstract void setBeginningTimeStamp(TimeStamp ts);
	
	/** Set the last TimeStamp of this AudioSegmenter. The next time a segment is 
	 * produced, the start time of the segment will be this value. The counter is
	 * reset with this operation.
	 * @param ts
	 */
	public abstract void setLastTimeStamp(TimeStamp ts);
	
	/* (non-Javadoc)
	 * @see net.beadsproject.beads.core.Bead#toString()
	 */
	public String toString() {
		String result = "Segmenter: " + getClass().getSimpleName();
		for(FeatureExtractor<?, float[]> fe : listeners) {
			result += "\n    " + fe.getName();
		}
		return result;
	}
	
}
