/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data.buffers;

import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.BufferFactory;

/**
 * Creates a {@link Buffer} of the range zero to Pi of the cosine function, used for windowing.
 */
public class CosineWindow extends BufferFactory {

	/* (non-Javadoc)
	 * @see net.beadsproject.beads.data.BufferFactory#generateBuffer(int)
	 */
	@Override
	public Buffer generateBuffer(int bufferSize) {
		Buffer b = new Buffer(bufferSize);
		for(int i = 0; i < bufferSize; i++) {
			b.buf[i] = (float)Math.sin((double)i / bufferSize * Math.PI);
		}
		return b;
	}

	/* (non-Javadoc)
	 * @see net.beadsproject.beads.data.BufferFactory#getName()
	 */
	@Override
	public String getName() {
		return "Cosine";
	}

}
