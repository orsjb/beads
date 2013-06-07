package net.beadsproject.beads.play;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;


public class Kit implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public Map<Integer, Map<String, Object>> soundEvents;
	public Map<String, Object> globals;
	
	public Kit() {
		soundEvents = new Hashtable<Integer, Map<String,Object>>();
		globals = new Hashtable<String, Object>();
	}
	
	public Map<Integer, Map<String, Object>> getSoundEvents() {
		return soundEvents;
	}
	
	public void setSoundEvents(Map<Integer, Map<String, Object>> soundEvents) {
		this.soundEvents = soundEvents;
	}
	
	public Map<String, Object> getGlobals() {
		return globals;
	}
	
	public void setGlobals(Map<String, Object> globals) {
		this.globals = globals;
	}
	
	public Map<String, Object> getEvent(int i) {
		Map<String, Object> event = new Hashtable<String, Object>();
		event.putAll(globals);
		event.putAll(soundEvents.get(i % soundEvents.size()));
		return event;
	}
	
	
}
