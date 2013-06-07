/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data.buffers;

import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.BufferFactory;

/**
 * Creates a {@link Buffer} consisting of a sine wave in the range [-1,1].
 * 
 * @see Buffer BufferFactory
 * @author ollie
 */
public class SineBuffer extends BufferFactory {


    /* (non-Javadoc)
     * @see net.beadsproject.beads.data.BufferFactory#generateBuffer(int)
     */
    public Buffer generateBuffer(int bufferSize) {
    	Buffer b = new Buffer(bufferSize);
        for(int i = 0; i < bufferSize; i++) {
            b.buf[i] = (float)Math.sin(2.0 * Math.PI * (double)i / (double)bufferSize);
        }
    	return b;
    }

    /* (non-Javadoc)
     * @see net.beadsproject.beads.data.BufferFactory#getName()
     */
    public String getName() {
    	return "Sine";
    }
}
