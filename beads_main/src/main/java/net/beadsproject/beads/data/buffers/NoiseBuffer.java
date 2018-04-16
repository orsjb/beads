/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data.buffers;

import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.BufferFactory;

/**
 * Creates a {@link Buffer} of random floats.
 * 
 * @author ben
 *
 */
public class NoiseBuffer extends BufferFactory
{
  public Buffer generateBuffer(int bufferSize) {
    Buffer b = new Buffer(bufferSize);
    for(int i = 0; i < bufferSize; i++) {
      b.buf[i] = (float)(1.-2.*Math.random());
    }
    return b;
  }

  public String getName() {
    return "Noise";
  }

};