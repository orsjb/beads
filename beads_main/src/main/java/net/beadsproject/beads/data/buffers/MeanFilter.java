/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data.buffers;

import java.util.Arrays;

import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.BufferFactory;

/**
 * Creates a {@link Buffer} of the constant 1/bufferSize over [0,1]. The convolution of the MeanFilter with data gives the mean.
 * 
 * @author ben
 *
 */
public class MeanFilter extends BufferFactory {

	@Override
	public Buffer generateBuffer(int bufferSize) {
		Buffer b = new Buffer(bufferSize);
		Arrays.fill(b.buf,1.f/bufferSize);
		return b;
	}

	@Override
	public String getName() {
		return "MeanFilter";
	}

}
