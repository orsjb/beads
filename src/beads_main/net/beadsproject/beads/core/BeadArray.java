/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.core;

import java.util.ArrayList;

/**
 * BeadArray represents an array of Beads (and is itself a subclass of Bead). Its purpose is to forward messages to its array members. A BeadArray detects whether or not its members are deleted, and removes them if they are. For this reason it should be used in any situations where a Bead needs to be automatically disposed of. Note, however, that a BeadArray does not forward {@link Bead#kill()}, {@link Bead#start()} and {@link Bead#pause(boolean)} messages to its component Beads unless told to do so by setting {@link #setForwardKillCommand(boolean)} and {@link BeadArray#setForwardPauseCommand(boolean)} respectively.
 * 
 * @author ollie
 */
public class BeadArray extends Bead {

	/** The beads. */
	private ArrayList<Bead> beads;
	
	/** Flag to forward kill commands. */
	private boolean forwardKillCommand;
	
	/** Flag to forward pause commands. */
	private boolean forwardPauseCommand;

	/**
	 * Creates an empty BeadArray.
	 */
	public BeadArray() {
		beads = new ArrayList<Bead>();
		forwardKillCommand = false;
		forwardPauseCommand = false;
	}

	/**
	 * Adds a new Bead to the list of receivers.
	 * 
	 * @param bead Bead to add.
	 */
	public void add(Bead bead) {
		beads.add(bead);
	}

	/**
	 * Removes a Bead from the list of receivers.
	 * 
	 * @param bead Bead to remove.
	 */
	public void remove(Bead bead) {
		beads.remove(bead);
	}

	/**
	 * Gets the ith Bead from the list of receivers.
	 * 
	 * @param i index of Bead to retrieve.
	 * 
	 * @return the Bead at the ith index.
	 */
	public Bead get(int i) {
		return beads.get(i);
	}

	/**
	 * Clears the list of receivers.
	 */
	public void clear() {
		beads.clear();
	}

	/**
	 * Gets the size of the list of receivers.
	 * 
	 * @return size of list.
	 */
	public int size() {
		return beads.size();
	}
	
	/**
	 * Gets the contents of this BeadArrays as an ArrayList of Beads.
	 * 
	 * @return the beads.
	 */
	public ArrayList<Bead> getBeads() {
		return beads;
	}

	/**
	 * Forwards incoming message to all receivers.
	 * 
	 * @param message incoming message.
	 */
	public void messageReceived(Bead message) {
		BeadArray clone = clone();
		for (int i = 0; i < clone.size(); i++) {
			Bead bead = clone.get(i);
			if (bead.isDeleted()) {
				remove(bead);
			} else {
				bead.message(message);
			}
		}
	}

	/**
	 * Creates a shallow copy of itself.
	 * 
	 * @return shallow copy of this Bead.
	 */
	public BeadArray clone() {
		BeadArray clone = new BeadArray();
		for (int i = 0; i < beads.size(); i++) {
			clone.add(beads.get(i));
		}
		return clone;
	}
	
	/**
	 * Checks if this BeadArray forwards kill commands.
	 * 
	 * @return true if this BeadArray forwards kill commands.
	 */
	public boolean doesForwardKillCommand() {
		return forwardKillCommand;
	}
	
	/**
	 * Determines whether or not this BeadArray forwards kill commands.
	 * 
	 * @param forwardKillCommand true if this BeadArray forwards kill commands.
	 */
	public void setForwardKillCommand(boolean forwardKillCommand) {
		this.forwardKillCommand = forwardKillCommand;
	}

	/**
	 * Checks if this BeadArray forwards pause commands.
	 * 
	 * @return true if this BeadArray forwards pause commands.
	 */
	public boolean doesForwardPauseCommand() {
		return forwardPauseCommand;
	}

	/**
	 * Determines whether or not this BeadArray forwards pause commands.
	 * 
	 * @param forwardPauseCommand true if this BeadArray forwards pause commands.
	 */
	public void setForwardPauseCommand(boolean forwardPauseCommand) {
		this.forwardPauseCommand = forwardPauseCommand;
	}

	/* (non-Javadoc)
	 * @see net.beadsproject.beads.core.Bead#kill()
	 */
	@Override
	public void kill() {
		super.kill();
		if(forwardKillCommand) {
			BeadArray clone = clone();
			for(Bead bead : clone.beads) {
				if (bead.isDeleted()) {
					remove(bead);
				} else {
					bead.kill();
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see net.beadsproject.beads.core.Bead#pause(boolean)
	 */
	@Override
	public void pause(boolean paused) {
		super.pause(paused);
		if(forwardPauseCommand) {
			BeadArray clone = clone();
			for(Bead bead : clone.beads) {
				if (bead.isDeleted()) {
					remove(bead);
				} else {
					bead.pause(paused);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see net.beadsproject.beads.core.Bead#start()
	 */
	@Override
	public void start() {
		super.start();
		if(forwardPauseCommand) {
			BeadArray clone = clone();
			for(Bead bead : clone.beads) {
				if (bead.isDeleted()) {
					remove(bead);
				} else {
					bead.start();
				}
			}
		}
	}
	
	

}
