/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.play;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;

import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.core.BeadArray;
import net.beadsproject.beads.events.IntegerBead;

/**
 * A Pattern is a {@link Bead} that responds to integer events by 
 * generating other integer events and forwarding them to a {@link BeadArray} 
 * of listeners. Typically, Patterns are used with {@link net.beadsproject.beads.ugens.Clock Clocks}. 
 * 
 * <p/>Patterns contain a list of events specified as key/value pairs of integers. A Pattern keeps an internal counter which is incremented internally. When the counter is incremented, if its new value is listed as a key, Pattern forwards the corresponding value to its listeners. 
 * 
 * Pattern responds to {@link Bead} messages that implement {@link IntegerBead}. An incoming integer causes Pattern's internal counter to increment if it is a multiple of {@link #hop}. If the internal counter reaches {@link Pattern#loop}, it returns to zero. In this 
 * way, Pattern can be quicly maniuplated to play back at different speeds and loop lengths in 
 * response to a regular {@link net.beadsproject.beads.ugens.Clock Clock}. 
 */
public class Pattern implements Serializable {
	
	private static final long serialVersionUID = 1L;

	/** A list of events. */
    private final Hashtable<Integer, ArrayList<Integer>> events;
    private int suggestedLoop;
    
    
    /**
     * Instantiates a new empty pattern.
     */
    public Pattern() {
        events = new Hashtable<Integer, ArrayList<Integer>>();
    }
    
	/**
     * Adds an event consisting of a integer key and an integer value.
     * 
     * @param key the key.
     * @param value the value.
     */
    public synchronized void addEvent(int key, int value) {
    	ArrayList<Integer> eventSet = events.get(key);
    	if(eventSet == null) {
    		eventSet = new ArrayList<Integer>();
    		events.put(key, eventSet);
    	}
        eventSet.add(value);
    }
    
    /**
     * Removes the event with the given integer key and value.
     * 
     * @param key the key.
     */
    public synchronized void removeEvent(int key, int value) {
    	ArrayList<Integer> eventSet = events.get(key);
    	if(eventSet != null) {
    		eventSet.remove(new Integer(value));
    	}
    }
    
    /**
     * Removes the events at the given integer key (step).
     * 
     * @param key the key.
     */
    public void clearEventsAtStep(int key) {
    	events.remove(key);
    }
    
    /**
     * Clears the pattern data. Does not reset the Pattern.
     */
    public void clear() {
    	events.clear();
    }

    
    
    public ArrayList<Integer> getQuantizedEvent(int index, int quant, int loop) {
    	if(quant == 1) return events.get(index);
    	ArrayList<Integer> collection = new ArrayList<Integer>();
    	//go from half before index to half after index
    	for(int i = - quant / 2; i < quant / 2; i++) {
    		int theRealIndex = index + i;
    		while(theRealIndex < 0) theRealIndex += loop;
    		while(theRealIndex >= loop) theRealIndex -= loop;
        	ArrayList<Integer> moreEvents = events.get(theRealIndex);	
        	if(moreEvents != null) collection.addAll(moreEvents);
    	}
    	if(collection.size() == 0) return null;
    	return collection;
    }
    
    public  ArrayList<Integer> getEventAtIndex(int index) {
    	return events.get(index);
    }
    
    
    public Set<Integer> getEvents() {
    	return events.keySet();
    }

	public int getSuggestedLoop() {
		return suggestedLoop;
	}

	public void setSuggestedLoop(int suggestedLoop) {
		this.suggestedLoop = suggestedLoop;
	}
    
    
    
}
