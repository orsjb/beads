/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data.buffers;

import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.BufferFactory;

/**
 * A filter used for smoothing data.
 * 
 * @author ben
 *
 */
public class TriangularWindow extends BufferFactory {

	@Override
	public Buffer generateBuffer(int bufferSize) {
		Buffer b = new Buffer(bufferSize);
		
		for (int i=0;i<bufferSize;i++) {
			b.buf[i] = tri((i + 0.5f) / bufferSize) / bufferSize; 
		}
		return b;
	}
	
	protected float tri(float x)
	{
		if (x < .5)
			return 4*x;		
		else		
			return 4*(1-x);	
	}

	@Override
	public String getName() {
		return "TriangularBuffer";
	}

}
