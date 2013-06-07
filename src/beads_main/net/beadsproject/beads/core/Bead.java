/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.core;

 
/**
 * Bead is an abstract class which defines basic behaviour such as starting and stopping, pausing and handling messages. 
 * 
 * <p/>Messages: A Bead can send a message to another Bead using the {@link #message(Bead)} method. Implementations of Bead handle these messages by subclassing the {@link #messageReceived(Bead)} method. {@link BeadArray} can be used to gather Beads into groups, which is useful for defining message channels in a system. 
 * 
 * <p/>Pausing: the method {@link #pause(boolean)} toggles the pause mode of a Bead. Beads are unpaused by default. A paused Bead will no longer respond to incoming messages. 
 * 
 * <p/>Deleting: The method {@link #kill()} deletes a Bead. Deleted Beads are automatically removed from {@link BeadArray}s. The method {@link Bead#setKillListener(Bead)} allows you to specify another Bead that gets notified when this Bead is killed. 
 * 
 * <p/>UGens: An important subclass of Bead is {@link UGen}. When a UGen is paused, it does not calculate audio. When it is deleted, it is automatically removed from any signal chains. 
 * 
 * @author ollie
 */
public abstract class Bead {

	/** True if the Bead is paused. */
	private boolean paused;
	
	/** True if the Bead is marked for deletion. */
	private boolean deleted;
	
	/** A Bead that gets informed when this Bead gets killed. */
	private Bead killListener;
	
	/** The name. */
	private String name;
	
	/**
	 * Instantiates a new bead.
	 */
	public Bead() {
		 paused = false;
		 deleted = false;
	}
	
	/**
	 * Gets the Bead's name.
	 * 
	 * @return the name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the Bead's name.
	 * 
	 * @param name the new name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns a String specifying the Bead's class and it's name.
	 * 
	 * @return String describing the Bead.
	 */
	@Override
	public String toString() {
		if(name != null) {
			return getClass().toString() + " name=" + name;
		} else {
			return getClass().toString();
		}
	}

	/**
	 * Send this Bead a message. Typically if another Bead was sending the message, it would send itself as the argument.
	 * 
	 * @param message the Bead is the message.
	 */
	public final void message(Bead message) {
		if(!paused) messageReceived(message);
	}
	
	/**
	 * Responds to an incoming message. Subclasses can override this in order to handle incoming messages. Typically a Bead would send a message to another Bead with itself as the arugment.
	 * 
	 * @param message the message
	 */
	protected void messageReceived(Bead message) {
		/*
		 * To be subclassed, but not compulsory.
		 */
	}
	
	/**
	 * Shortcut for pause(false).
	 */
	public void start() {
		paused = false;
	}

	/**
	 * Stops this Bead, and flags it as deleted. This means that the Bead will automatically be removed from any {@link BeadArray}s. Calling this method for the first time
	 * also causes the killListener to be notified.
	 */
	public void kill() {
		if(!deleted) {
			deleted = true;
			if(killListener != null) {
				killListener.message(this);
			}
		}
	}
	
	/**
	 * Checks if this Bead is paused.
	 * 
	 * @return true if paused
	 */
	public boolean isPaused() {
		return paused;
	}
	
	/**
	 * Toggle the paused state of the Bead.
	 * 
	 * @param paused true to pause Bead.
	 */
	public void pause(boolean paused) {
		this.paused = paused;
	}
	
	/**
	 * Sets this Bead's kill listener. The kill listener will receive a message containing this Bead as an argument when this Bead is killed.
	 * 
	 * @param killListener the new kill listener. 
	 */
	public void setKillListener(Bead killListener) {
		this.killListener = killListener;
	}
	
    /**
     * Gets this Bead's kill listener.
     * 
     * @return the kill listener.
     */
    public Bead getKillListener() {
    	return killListener;
    }


	/**
	 * Determines if this Bead is deleted.
	 * 
	 * @return true if this Bead's state is deleted, false otherwise.
	 */
	public boolean isDeleted() {
		return deleted;
	}
	

}
