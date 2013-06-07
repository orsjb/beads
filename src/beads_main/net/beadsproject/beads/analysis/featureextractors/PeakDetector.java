/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.analysis.featureextractors;

import java.util.ArrayList;
import java.util.List;

import net.beadsproject.beads.analysis.FeatureExtractor;
import net.beadsproject.beads.analysis.SegmentListener;
import net.beadsproject.beads.analysis.SegmentMaker;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.core.BeadArray;
import net.beadsproject.beads.core.TimeStamp;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.buffers.MeanFilter;

/**
 * Detects peaks in a continuous stream of one element inputs. Attach to an
 * OnsetDetectionFunction (like SpectralDifference) to get Onsets. Use
 * addMessageListener to receive a message when an onset is detected.
 * 
 * The algorithm follows the one described in: <code>Dixon, S (2006)
 * "Onset Detection Revisited" Proc. of the 9th Int. Conference on Digital Audio
 * Effects (DAFx-06), Montreal, Canada, September 18-20, 2006</code>
 * 
 * @beads.category analysis
 * @author ben
 */

public class PeakDetector extends FeatureExtractor<Float, Float> implements SegmentMaker {
	
	private BeadArray listeners;
	private List<SegmentListener> segmentListeners;
	private TimeStamp lastStartTime;
	private double resetDelay; //milliseconds

	private float valueAtOnset = 0;
	private float threshold = 0;
	private float baseThreshold;

	private float lastValues[];
	private Buffer filter;

	/**
	 * size of window to search for local maxima (lag will then be floor(W/2)
	 * frames)
	 */
	private final int W = 3;
	private final int WM = 3; // multiplier
	private final int M = W + WM * W + 1;
	private float alpha = 0.9f; //Ollie - description of alpha would be handy, look like a kind of momentum value for changing threshold
	//The lower alpha, the more rapidly changeable?

	public PeakDetector() {
		super();
		listeners = new BeadArray();
		segmentListeners = new ArrayList<SegmentListener>();
		lastValues = new float[M];
		filter = new MeanFilter().generateBuffer(M);
		baseThreshold = 0.1f;
		valueAtOnset = 0;
		threshold = 0;
		resetDelay = 100;
	}
	
	public void setThreshold(float thresh) {
		baseThreshold = thresh;
	}

	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}
	
	public void setResetDelay(float resetDelay) {
		this.resetDelay = resetDelay;
	}

	/**
	 * @return the value at the last onset
	 */
	public float getLastOnsetValue() {
		return valueAtOnset;
	}

	/**
	 * @return The lag in frames between onsets occurring and actually being
	 *         detected
	 */
	public int getLagInFrames() {
		return W;
	}

	/**
	 * Get the correct BufferSize for the OnsetDetector 
	 */
	public int getBufferSize() {
		return M;
	}

	/**
	 * Sets the window for the local averaging.
	 * 
	 * @param b
	 *            Buffer must be of size == getBufferSize(), and integrates to
	 *            1.
	 */
	public void setFilter(Buffer b) {
		assert(b.buf.length == M);
		filter = b;
	}

	/**
	 * process: assumes input is a 1 element array
	 */
	@Override
	public void process(TimeStamp startTime, TimeStamp endTime, Float input) {
		//make sure lastStartTime isn't empty
		if (lastStartTime == null) {
			lastStartTime = startTime;
		}
		// cache the values
		for (int i = 1; i < M; i++) {
			lastValues[i - 1] = lastValues[i];
		}
		lastValues[M - 1] = input;
		// simple onset detection mechanism
		// from simon dixon paper...
		// Trigger a peak at (M-1-W) if
		// 1. lastMValues[M-1-W] > threshold (DONE)
		// 2. lastMValues[M-1-W] > lastMValues[M-1-..]..lastMValues[M-1]
		// 3. lastMValues[M-1-W] > average of lastMValues[M-1-.. .. M-1]
		float lastValue = lastValues[M - 1 - W];
		if (lastValue > threshold && endTime.since(lastStartTime) > resetDelay) {
			boolean passedTest2 = true;
			for (int i = M - 1 - 2 * W; i <= M - 1; i++) {
				if (i == M - 1 - W)
					continue;
				if (lastValue < lastValues[i]) {
					passedTest2 = false;
				}
			}
			if (passedTest2) {
				// apply the FIR filter
				float average = 0;
				for (int i = 1; i < M; i++) {
					average += lastValues[i] * filter.buf[i];
				}
				average += input * filter.buf[M - 1];
				if (lastValue > average + baseThreshold) {
					// All tests have passed, therefore we have detected a
					// peak->thus an onset
					valueAtOnset = lastValue;
					//notify the vast cohort of interested parties
					features = valueAtOnset;	//TODO what value best indicates the 'strength' of the onset?
					forward(startTime, endTime); 
					for (SegmentListener sl : segmentListeners) {
						sl.newSegment(lastStartTime, endTime);
					}
					listeners.message(this);
					lastStartTime = endTime;
				}
			}
		}
		// update the threshold function
		threshold = Math.max(lastValue, alpha * threshold + (1 - alpha) * lastValue);
	}

	public void addMessageListener(Bead b) {
		listeners.add(b);
	}

	public void removeMessageListener(Bead b) {
		listeners.remove(b);
	}

	public void addSegmentListener(SegmentListener sl) {
		segmentListeners.add(sl);
	}

	public void removeSegmentListener(SegmentListener sl) {
		segmentListeners.remove(sl);
	}

}
