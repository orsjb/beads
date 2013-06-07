package net.beadsproject.beads.ugens;

import java.util.Iterator;
import java.util.LinkedList;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * Drain mixes grains of audio. Inspired by FTM's Gabor Drain. Send chunks of windowed audio data
 * and it will play them, mix them and dispose of them when done. Drain may not be very efficient
 * in the sense that you may be allocating large arrays of floats. This is left to the user
 * to work out. Call {@link #add(float[][])} to add a new bit of audio data. Beware that the float
 * will then be in use for a while as the audio data gets accessed and spat out.
 */
public class Drain extends UGen {
	
	/**
	 * The Class Grain.
	 */
	private class Grain {
		
		/** The audio data. */
		float[][] audioData;	//assumes audio data has already been windowed
		
		/** The position. */
		int position;
	}
	
	/** The grains. */
	LinkedList<Grain> grains;

	/**
	 * Instantiates a new Drain.
	 *
	 * @param context the AudioContext.
	 * @param outs the number of output channels.
	 */
	public Drain(AudioContext context, int outs) {
		super(context, outs);
		grains = new LinkedList<Grain>();
	}
	
	/**
	 * Adds a new grain of audio data.
	 *
	 * @param audioData the audio data.
	 */
	public synchronized void add(float[][] audioData) {
		Grain g = new Grain();
		g.audioData = audioData;
		g.position = 0;
		grains.add(g);
	}

	/* (non-Javadoc)
	 * @see net.beadsproject.beads.core.UGen#calculateBuffer()
	 */
	@Override
	public void calculateBuffer() {
		for(int i = 0; i < bufferSize; i++) {
			Iterator<Grain> grainIterator = grains.iterator();
			while(grainIterator.hasNext()) {
				Grain g = grainIterator.next();
				for(int j = 0; j < outs; j++) {
					bufOut[j][i] += g.audioData[j % g.audioData.length][g.position];
				}
				g.position++;
				if(g.position > g.audioData[0].length) {
					grainIterator.remove();
				}
			}
		}
	}
	
	
	
	
	
}