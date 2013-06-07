/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.analysis;

import net.beadsproject.beads.core.TimeStamp;

/**
 * SegmentListeners get notified with a {@link newSegment()} event by a {@link SegmentMaker}.
 */
public interface SegmentListener {

	/**
	 * newSegment even, called by any {@link SegmentMaker}s that this SegmentListener is listening to.
	 * 
	 * @param start the start time.
	 * @param end the end time.
	 */
	public void newSegment(TimeStamp start, TimeStamp end);
}
