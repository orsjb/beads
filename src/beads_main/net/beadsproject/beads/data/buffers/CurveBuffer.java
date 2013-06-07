/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data.buffers;

import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.BufferFactory;

/**
 * Creates a {@link Buffer} of the function x^e^(-c) where x is in the range [0,1] and c is a curviness factor.
 */
public class CurveBuffer extends BufferFactory {

	/** The curviness. */
	private final float curviness;
	
	/**
	 * Instantiates a new curve buffer.
	 * 
	 * @param curviness the curviness.
	 */
	public CurveBuffer(float curviness) {
		this.curviness = Math.min(1, Math.max(-1, curviness));
	}
	
	/* (non-Javadoc)
	 * @see net.beadsproject.beads.data.BufferFactory#generateBuffer(int)
	 */
	@Override
	public Buffer generateBuffer(int bufferSize) {
		Buffer b = new Buffer(bufferSize);
		double exponent = Math.exp(-curviness);
		for(int i = 0; i < bufferSize; i++) {
			b.buf[i] = (float)Math.pow((float)i / bufferSize, exponent);
		}
		return b;
	}

	/* (non-Javadoc)
	 * @see net.beadsproject.beads.data.BufferFactory#getName()
	 */
	@Override
	public String getName() {
		return "Curve " + curviness;
	}

}
