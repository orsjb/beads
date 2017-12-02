/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.analysis;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import net.beadsproject.beads.core.TimeStamp;

/**
 * Stores a set of features associated with a continuous period of audio data.
 * 
 * A FeatureTrack can hold different views on the data. Time-based features are stored 
 * in lists mapping segments to features.
 * 
 * @author ollie
 */
public class FeatureTrack implements Serializable, Iterable<FeatureFrame>, SegmentListener {
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;
	
	/** The list of FeatureFrames. */
	private SortedSet<FeatureFrame> frames;

	/** An alternative list which blocks together the feature frames. */
	private Map<Integer, SortedSet<FeatureFrame>> framesInBlocks;
	
	/** Interval in ms between regions of framesInBlocks. */
	private int skipMS;
	
	/** The list of FeatureExtractors used to extract data. */
	private transient List<FeatureExtractor<?, ?>> extractors;
	
	/** The memory of the FeatureTrack. Max number of frames allowed. -1 means unlimited. */
	private int frameMemory;
	
	/* TODO FeatureTrack should be able to clear parts of its memory, for example
	 * be able to only remember a certain period of time before last. Should also 
	 * have quick access option based on access by frames (assumes the user knows
	 * distance between frames or doesn't care), or iterartion backwards through
	 * frames from last.
	 */
	
	/**
	 * Instantiates a new FeatureTrack.
	 */
	public FeatureTrack() {
		frames = new TreeSet<FeatureFrame>();
		framesInBlocks = new Hashtable<Integer, SortedSet<FeatureFrame>>();
		skipMS = 1000;
		extractors = new ArrayList<FeatureExtractor<?,?>>();
		frameMemory = -1;
	}
	
	/**
	 * Adds the specified FeatureFrame. Note that this will not add the frame
	 * if there is already a frame with the same start time. Use removeRange to
	 * make space available.
	 * 
	 * @param ff the FeatureFrame.
	 */
	public void add(FeatureFrame ff) {
		if(frameMemory != 0) {
			//add to frames
			frames.add(ff);
			//and to framesInBlocks
			int startIndex = (int)(ff.getStartTimeMS() / skipMS);
			int endIndex = (int)(ff.getEndTimeMS() / skipMS);
			for(int i = startIndex; i <= endIndex; i++) {
				SortedSet<FeatureFrame> frameSet;
				if(framesInBlocks.containsKey(i)) {
					frameSet = framesInBlocks.get(i);
				} else {
					frameSet = new TreeSet<FeatureFrame>();
					framesInBlocks.put(i, frameSet);
				}
				frameSet.add(ff);
			}
			frameMemoryCheck();
		}
	}
	
	/**
	 * Removes the specified FeatureFrame.
	 * @param ff the FeatureFrame to remove.
	 */
	public void remove(FeatureFrame ff) {
		//remove from frames
		frames.remove(ff);
		//remove from framesInBlocks
		int startIndex = (int)(ff.getStartTimeMS() / skipMS);
		int endIndex = (int)(ff.getEndTimeMS() / skipMS);
		for(int i = startIndex; i <= endIndex; i++) {
			SortedSet<FeatureFrame> frameSet;
			if(framesInBlocks.containsKey(i)) {
				frameSet = framesInBlocks.get(i);
				frameSet.remove(ff);
			}
		}
	}
	
	/**
	 * Remove all feature frames in the given range.
	 * @param startRangeMS the beginning of the range.
	 * @param endRangeMS the end of the range.
	 */
	public void removeRange(double startRangeMS, double endRangeMS) {
		ArrayList<FeatureFrame> toRemove = new ArrayList<FeatureFrame>();
		FeatureFrame startFrame = getFrameAt(startRangeMS);
		if(startFrame == null) return;
		toRemove.add(startFrame);
		for(FeatureFrame ff : frames.tailSet(startFrame)) {
			if(ff.getStartTimeMS() < endRangeMS) {
				toRemove.add(ff);
			} else {
				break;
			}
		}
		for(FeatureFrame ff : toRemove) {
			remove(ff);
		}
	}
	
	/**
	 * Gets the FeatureFrame at the given index.
	 * 
	 * @param index the index.
	 * 
	 * @return the FeatureFrame.
	 */
	public FeatureFrame get(int index) {
		if(index >= frames.size()) {
			return null;
		}
		int count = 0;
		FeatureFrame result = null;
		for(FeatureFrame ff : frames) {
			result = ff;
			if(count == index) break;
			count++;
		}
		return result;
	}

	/**
	 * Gets the frame for the given offset, in milliseconds, into the FeatureLayer.
	 * 
	 * @param timeMS the millisecond offset.
	 * 
	 * @return the FeatureFrame at this time.
	 */
	public FeatureFrame getFrameAt(double timeMS) {
		int startTimeIndex = (int)(timeMS / skipMS);
		SortedSet<FeatureFrame> localSet = framesInBlocks.get(startTimeIndex);
		if(localSet != null) {
			for(FeatureFrame ff : localSet) {	//TODO replace with binary search.
				if(ff.containsTime(timeMS)) {
					return ff;
				}
			}
		} 
		return null;
	}
	
	/**
	 * Gets the frame for the given offset, in milliseconds, into the FeatureLayer, or the last frame before then.
	 * 
	 * @param timeMS the millisecond offset.
	 * 
	 * @return the FeatureFrame at this time.
	 */
	public FeatureFrame getFrameBefore(double timeMS) {
		FeatureFrame ff = getFrameAt(timeMS);
		if(ff == null) ff = frames.last();
		return ff;
	}
	
	/**
	 * Gets the last FeatureFrame in this FeatureTrack. 
	 * 
	 * @return the last FeatureFrame in this FeatureTrack.
	 */
	public FeatureFrame getLastFrame() {
		if(frames.size() > 0) return frames.last();
		return null;
	}
	
	/**
	 * Adds a new {@link FeatureExtractor}. When {@link newSegment()} is called, the FeatureTrack creates a new {@link FeatureFrame}
	 * with the given start and end times and adds the data from all of its {@link FeatureExtractor}s to the {@link FeatureFrame}.
	 * 
	 * @param e the FeatureExtractor.
	 */
	public void addFeatureExtractor(FeatureExtractor<?, ?> e) {
		extractors.add(e);
	}
	
	/**
	 * Removes the specified FeatureExtractor.
	 * 
	 * @param e the FeatureExtractor.
	 */
	public void removeFeatureExtractor(FeatureExtractor<?, ?> e) {
		extractors.remove(e);
	}
	
	/**
	 * Tells this FeatureTrack to log a new {@link FeatureFrame}, with the given startTime and endTime. The FeatureTrack
	 * will gather features from its various {@link FeatureExtractor}s at this point.
	 * @throws CloneNotSupportedException 
	 */
	public void newSegment(TimeStamp startTime, TimeStamp endTime) {
		FeatureFrame ff = new FeatureFrame(startTime.getTimeMS(), endTime.getTimeMS());
		for(FeatureExtractor<?, ?> e : extractors) {
			Object features = e.getFeatures();
			try {
				Method cloneMethod = features.getClass().getMethod("clone", new Class[] {});
				ff.add(e.getName(), cloneMethod.invoke(features, new Object[] {}));
			} catch (Exception e1) {
				//is this ugly or what? Any better ideas?
				if(features instanceof float[]) {
					ff.add(e.getName(), ((float[])features).clone());
				} else if(features instanceof int[]) {
					ff.add(e.getName(), ((int[])features).clone());
				} else if(features instanceof double[]) {
					ff.add(e.getName(), ((double[])features).clone());
				} else if(features instanceof byte[]) {
					ff.add(e.getName(), ((byte[])features).clone());
				} else if(features instanceof short[]) {
					ff.add(e.getName(), ((short[])features).clone());
				} else if(features instanceof long[]) {
					ff.add(e.getName(), ((long[])features).clone());
				} else if(features instanceof Object[]) {
					ff.add(e.getName(), ((Object[])features).clone());
				}  else if(features instanceof boolean[]) {
					ff.add(e.getName(), ((boolean[])features).clone());
				} else {
					//TODO ultimately - get rid of primitives in the whole feature extraction system
//					new CloneNotSupportedException("Must implement clone handling in FeatureTrack for Class " + features.getClass()).printStackTrace();
					ff.add(e.getName(), features);
				}
				//how about ff.add(..., features.getClass().cast(features).clone())? - ben
				//doesn't work, since clone is not an available method - ollie
			} 
		}
		add(ff);
	}

	/**
	 * Returns an iterator over the {@link FeatureFrame}s.
	 */
	public Iterator<FeatureFrame> iterator() {
		return frames.iterator();
	}

	public int getFrameMemory() {
		return frameMemory;
	}

	public void setFrameMemory(int frameMemory) {
		this.frameMemory = frameMemory;
		frameMemoryCheck();
	}
	
	/**
	 * Ensures that number of stored frames is <= frameMemory.
	 */
	private void frameMemoryCheck() {
		if(frameMemory > 0) {
			while(frames.size() > frameMemory) {
				FeatureFrame ff = frames.first();
				remove(ff);	
			}
		}
	}

	/**
	 * Returns the number of {@link FeatureFrame}s stored in this FeatureTrack.
	 * 
	 * @return number of FeatureFrames.
	 */
	public int getNumberOfFrames() {
		return frames.size();
	}

	/**
	 * Clears all memory of feature frames.
	 */
	public void clear() {
		frames.clear();
		framesInBlocks.clear();
	}


}
