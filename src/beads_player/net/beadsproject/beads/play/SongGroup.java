package net.beadsproject.beads.play;

import java.util.ArrayList;
import java.util.Hashtable;


// TODO: Auto-generated Javadoc
/**
 * The Class SongPartGroup.
 */
public class SongGroup {
 
	/** The song parts. */
	private final ArrayList<SongPart> parts;
	private final Hashtable<SongPart,Boolean> partVisibility;
	private String name;
	private int flipQuantisation;
	
	/**
	 * Instantiates a new song part group.
	 * 
	 * @param context
	 *            the context
	 * @param inouts
	 *            the inouts
	 */
	public SongGroup(String name) {
		setName(name);
		parts = new ArrayList<SongPart>();
		partVisibility = new Hashtable<SongPart, Boolean>();
		flipQuantisation = 4;
	}

	public void add(SongPart sp) {
		parts.add(sp);
		partVisibility.put(sp, true);
	}
	
	public void remove(SongPart sp) {
		parts.remove(sp);
		partVisibility.remove(sp);
	}
	
	public boolean contains(SongPart sp) {
		return parts.contains(sp);
	}
	
	public ArrayList<SongPart> parts() {
		return parts;
	}

	public boolean getPartVisibility(SongPart sp) {
		return partVisibility.get(sp);
	}

	
	public String getName() {
		return name;
	}

	
	public void setName(String name) {
		this.name = name;
	}
	
	public final String toString() {
		return getName() + " (" + getClass().getSimpleName() + ")";
	}
	
	public void setFlipQuantisation(int flipQuantisation) {
		this.flipQuantisation = flipQuantisation;
	}

	public int getFlipQuantisation() {
		return flipQuantisation;
	}

}
