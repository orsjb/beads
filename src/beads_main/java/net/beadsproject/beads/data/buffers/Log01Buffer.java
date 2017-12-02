/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data.buffers;

import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.BufferFactory;

/**
 * Creates a {@link Buffer} of the function 1 / (1 - log(x)) over [0,1].
 */
public class Log01Buffer extends BufferFactory {

	/* (non-Javadoc)
	 * @see net.beadsproject.beads.data.BufferFactory#generateBuffer(int)
	 */
	@Override
	public Buffer generateBuffer(int bufferSize) {
		Buffer b = new Buffer(bufferSize);
		for(int i = 0; i < bufferSize; i++) {
			float fract = (float)i / (float)(bufferSize - 1);
			b.buf[i] = 1f / (1f - (float)Math.log(fract));
		}
		return b;
	}

	/* (non-Javadoc)
	 * @see net.beadsproject.beads.data.BufferFactory#getName()
	 */
	@Override
	public String getName() {
		return "Log01";
	}

}
