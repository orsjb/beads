/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */ 
package net.beadsproject.beads.ugens;


import java.util.ArrayList;
import java.util.List;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.core.UGen;

/**
 * An Envelope generates a sequence of timed transitions (segments) between 
 * values as an audio signal. New segments are added to a running Envelope 
 * using variations of the method <code>addSegment</code>. With the method 
 * {@link #addSegment(float, float, float, Bead)} a {@link Bead} 
 * can be provided which gets triggered when the segment has reached its destination.
 *  
 * At any point in time, the Envelope maintains a current value. 
 * New segments define transitions from that current value to a destination value 
 * over a given duration. Instead of a linear transition, a curved transition 
 * can be used. The curve is defined as the mapping of the range <code>[0,1]</code> 
 * from <code>y=x</code> to <code>y=x^p</code> with a given exponent <code>p</code>.    
 *
 * @beads.category control
 * @author ollie
 */
public class Envelope extends UGen {

    /** The queue of segments. */
	//TODO this should be a queue!
    private ArrayList<Segment> segments;
    
    /** The current start value. */
    private float currentStartValue;
    
    /** The current value. */
    private float currentValue;
    
    /** The current time in samples. */
    private int currentTime;
    
    /** The current segment. */
    private Segment currentSegment;
    
    /** Flag used to block the Envelope from further changes. */
    private boolean lock;
    
    /** Flag used to note whether the Envelope needs to move to a new segment. */
    private boolean unchanged;
    
    /** The real output buffer. */
    protected float[] myBufOut;
    
    /**
	 * The nested class Segment. Stores the duration, end value, curvature and trigger for the Segment.
	 */
    public class Segment {
        
        /** The end value. */
        float endValue; 
        
        /** The duration in samples. */
        long duration; 
        
        /** The curvature. */
        float curvature;  
        
        /** The trigger. */
        Bead trigger;
        
        /**
		 * Instantiates a new segment.
		 * 
		 * @param endValue
		 *            the end value
		 * @param duration
		 *            the duration
		 * @param curvature
		 *            the curvature
		 * @param trigger
		 *            the trigger
		 */
        public Segment(float endValue, float duration, float curvature, Bead trigger) {
            this.endValue = endValue;
            this.duration = (int)context.msToSamples(duration);
            this.curvature = Math.abs(curvature);
            this.trigger = trigger;
        }
        
    }
    
    /**
	 * Instantiates a new Envelope with start value 0.
	 * 
	 * @param context
	 *            the AudioContext.
	 */
    public Envelope(AudioContext context) {
        super(context, 1);
        segments = new ArrayList<Segment>();
        currentStartValue = 0;
        currentValue = 0;
        currentSegment = null;
        lock = false;
        unchanged = false;
        outputInitializationRegime = OutputInitializationRegime.RETAIN;
        outputPauseRegime = OutputPauseRegime.RETAIN;
        myBufOut = new float[bufferSize];
        bufOut[0] = myBufOut;
    }
    
    /**
	 * Instantiates a new Envelope with the specified start value.
	 * 
	 * @param context
	 *            the AudioContext.
	 * @param value
	 *            the start value.
	 */
    public Envelope(AudioContext context, float value) {
    	this(context);
    	setValue(value);
    }
    
    /**
	 * Locks/unlocks the Envelope.
	 */
    public Envelope lock(boolean lock) {
    	this.lock = lock;
    	return this;
    }
    
    /**
	 * Checks whether the Envelope is locked.
	 * 
	 * @return true if the Envelope is locked.
	 */
    public boolean isLocked() {
    	return lock;
    }
    
    /**
	 * Adds a new Segment.
	 * 
	 * @param endValue
	 *            the destination value.
	 * @param duration
	 *            the duration.
	 * @param curvature
	 *            the exponent of the curve.
	 */
    public synchronized Envelope addSegment(float endValue, float duration, float curvature) { //synchronized
        if(!lock) {
        	if(!Float.isNaN(endValue) && !Float.isInfinite(endValue)) {
        		segments.add(new Segment(endValue, duration, curvature, null));
                unchanged = false;
        	}
        }
        return this;
    }
    
    /**
	 * Adds a new Segment.
	 * 
	 * @param endValue
	 *            the destination value.
	 * @param duration
	 *            the duration.
	 * @param curvature
	 *            the exponent of the curve.
	 * @param trigger
	 *            the trigger.
	 */
    public synchronized Envelope addSegment(float endValue, float duration, float curvature, Bead trigger) { //synchronized
        if(!lock) {
        	if(!Float.isNaN(endValue) && !Float.isInfinite(endValue)) {
        		segments.add(new Segment(endValue, duration, curvature, trigger));
                unchanged = false;
        	}
        }
        return this;
    }
    
    /**
	 * Adds a new Segment.
	 * 
	 * @param endValue
	 *            the destination value.
	 * @param duration
	 *            the duration.
	 */
    public Envelope addSegment(float endValue, float duration) {
    	return addSegment(endValue, duration, 1.0f, null);
    }
    
    /**
	 * Adds a new Segment.
	 * 
	 * @param endValue
	 *            the destination value.
	 * @param duration
	 *            the duration.
	 * @param trigger
	 *            the trigger.
	 */
    public Envelope addSegment(float endValue, float duration, Bead trigger) {
    	return addSegment(endValue, duration, 1.0f, trigger);
    }
    
    /**
     * Adds the specified List of Segments.
     * @param segments the Segments.
     */
    public Envelope addSegments(List<Segment> segments) {
		if(!lock) {
			for(Segment s : segments) {
            	if(!Float.isNaN(s.endValue) && !Float.isInfinite(s.endValue)) {
            		segments.add(s);
                    unchanged = false;
            	}
            }
    	}
		return this;
    }
    
    /**
	 * Clears the list of Segments and sets the current value of the Envelope immediately.
     * @see #setValue(float)
     */
    public void setValue(float value) {
    	if(!lock) {
    		clear();
        	addSegment(value, 0.0f);
        	currentValue = value;
    	}
    }
    
    /**
	 * Clears the list of Segments.
	 */
    public synchronized Envelope clear() { //synchronized
    	if(!lock) {
    		segments = new ArrayList<Segment>();
        	currentSegment = null;
    	} 
    	return this;
    }
    
    /**
	 * Moves the Envelope to the next segment.
	 * 
	 * @return the next segment.
	 */
    private synchronized void getNextSegment() {
        if(currentSegment != null) {
            currentStartValue = currentSegment.endValue;
            currentValue = currentStartValue;
            segments.remove(currentSegment);
            if(currentSegment.trigger != null) {
            	currentSegment.trigger.message(this);
            }
        } else {
        	currentStartValue = currentValue;
        }
        if(segments.size() > 0) {
            currentSegment = segments.get(0);
        } else {
            currentSegment = null;
        }
        currentTime = 0;
    }
    
    /**
	 * Gets the current value.
	 * 
	 * @return the current value.
	 */
    public float getCurrentValue() {
        return currentValue;
    }

	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.UGen#calculateBuffer()
	 */
	@Override
	public synchronized void calculateBuffer() {
		if(!unchanged) {
			boolean iChanged = false;
			for(int i = 0; i < bufferSize; ++i) {
				if(currentSegment == null) {
					getNextSegment();
		        } else if(currentSegment.duration == 0) {
		            getNextSegment();
		            iChanged = true;
		        } else {
		        	iChanged = true;
		        	float ratio;
		        	// if(currentSegment.curvature != 1.0f) ratio = (float)AudioUtils.fastPow((double)currentTime / (double)currentSegment.duration, (double)currentSegment.curvature);
		        	// BP, fastPow doesn't like values > 1
		        	if(currentSegment.curvature != 1.0f) ratio = (float)Math.pow((double)currentTime / (double)currentSegment.duration, (double)currentSegment.curvature);
		        	else ratio = (float)currentTime / (float)currentSegment.duration;
		            currentValue = (1f - ratio) * currentStartValue + ratio * currentSegment.endValue;
		            currentTime++;
		            if(currentTime > currentSegment.duration) getNextSegment();
		        }
				myBufOut[i] = currentValue;
			}
			if(!iChanged) unchanged = true;
		} 
	}
	
	public float getValue(int i, int j) {
		if(unchanged) {
			return currentValue;
		} 
		return myBufOut[j];
	}
    
}
