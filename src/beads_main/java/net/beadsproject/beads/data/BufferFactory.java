/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data;

import java.util.Hashtable;

/**
 * Abstract base class for factories that generate {@link Buffer}s. Create subclasses of BufferFactory to generate different types of {@link Buffer}.
 * 
 * @see Buffer
 * 
 * @author ollie
 */
public abstract class BufferFactory {
	
	/** The Constant DEFAULT_BUFFER_SIZE. */
	public static final int DEFAULT_BUFFER_SIZE = 4096;
	
	/**
	 * Subclasses should override this method to generate a {@link Buffer} of the specified size.
	 * 
	 * @param bufferSize the buffer size.
	 * 
	 * @return the buffer.
	 */
	public abstract Buffer generateBuffer(int bufferSize);
	
	/**
	 * Subclasses should override this method to generate a name. A default name should always be available for the case where {@link #getDefault()} is called.
	 * 
	 * @return the name of the buffer.
	 */
	public abstract String getName();
	
	/**
	 * Generates a buffer using {@link #DEFAULT_BUFFER_SIZE} and the BufferFactory's default name.
	 * 
	 * @return the default Buffer.
	 */
	public final Buffer getDefault() {
		if (Buffer.staticBufs==null)
		{
			Buffer.staticBufs = new Hashtable<String, Buffer>();
		}
		
		String name = getName();
    	if(!Buffer.staticBufs.containsKey(name)) {
        	Buffer.staticBufs.put(name, generateBuffer(DEFAULT_BUFFER_SIZE));
    	}
    	return Buffer.staticBufs.get(name);
	}

}
