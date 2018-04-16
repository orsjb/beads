/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data;

import java.io.Serializable;
import java.util.Hashtable;

import net.beadsproject.beads.data.buffers.NoiseBuffer;
import net.beadsproject.beads.data.buffers.SawBuffer;
import net.beadsproject.beads.data.buffers.SineBuffer;
import net.beadsproject.beads.data.buffers.SquareBuffer;
import net.beadsproject.beads.data.buffers.TriangleBuffer;

/**
 * A Buffer stores a one-dimensional buffer of floats for use as a wavetable or a window. Buffer does not perform any interpolation in this version, you should just make sure your buffer 
 * is high-res enough for what you need. Could add interpolation easily if needed. 
 * 
 * @beads.category data 
 * @see Sample BufferFactory
 * @author ollie
 */
public class Buffer implements Serializable {
	
	/**
	 * Default serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;
	

	/** 
	 * A static storage area for common buffers, such as a sine wave. Used by {@link BufferFactory} to keep track of common buffers.
	 */
	public static Hashtable<String, Buffer> staticBufs = new Hashtable<String, Buffer>(); 

	// A collection of default buffers, initialised for your convenience.
	public static final Buffer SINE = new SineBuffer().getDefault();
	public static final Buffer SAW = new SawBuffer().getDefault();
	public static final Buffer SQUARE = new SquareBuffer().getDefault();
	public static final Buffer TRIANGLE = new TriangleBuffer().getDefault();
	public static final Buffer NOISE = new NoiseBuffer().getDefault();
	
	/** 
	 * The buffer data. 
	 */
	public final float[] buf;
	
	/**
	 * Instantiates a new buffer.
	 * 
	 * @param size the size of the buffer.
	 */
	public Buffer(int size) {
		buf = new float[size];
	}
	
	/**
	 * Returns the value of the buffer at the given fraction along its length (0 = start, 1 = end). Uses linear interpolation.
	 * 
	 * @param fraction the point along the buffer to inspect. 
	 * 
	 * @return the value at that point.
	 */
	public float getValueFraction(float fraction) {
		float posInBuf = fraction * buf.length;
		int lowerIndex = (int)posInBuf;
		float offset = posInBuf - lowerIndex;
		int upperIndex = (lowerIndex + 1) % buf.length;
		return (1 - offset) * getValueIndex(lowerIndex) + offset * getValueIndex(upperIndex);
	}
	
	/**
	 * Returns the value of the buffer at the given fraction along its length (0 = start, 1 = end). No interpolation.
	 * 
	 * @param fraction the point along the buffer to inspect.
	 * 
	 * @return the value at that point.
	 */
	public float getValueFractionNoInterp(float fraction) {
		return getValueIndex((int)(fraction * buf.length));
	}

	/**
	 * Returns the value of the buffer at a specific index. If index is outside the range, then it is clipped to the ends.
	 * 
	 * @param index the index to inspect.
	 * 
	 * @return the value at that point.
	 */
	public float getValueIndex(int index) {
		if(index < 0) index = 0;
		if(index >= buf.length) index = buf.length - 1;
    	return buf[index];
	}
	
	/**
	 * Returns the contents of the buffer as a String over one line.
	 * 
	 * @return Buffer as String.
	 */
	public String toString() {
		StringBuffer b = new StringBuffer();
		for(int i = 0; i < buf.length; i++) {
			b.append(buf[i] + " ");
		}
		return b.toString();
	}

}
