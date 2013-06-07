/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.analysis.segmenters;

import net.beadsproject.beads.analysis.AudioSegmenter;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.TimeStamp;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.buffers.HanningWindow;

/**
 * A ShortFrameSegmenter slices audio data in short regular overlapping chunks.
 * 
 * @see AudioSegmenter
 * 
 * @author ollie
 */
public class ShortFrameSegmenter extends AudioSegmenter {

	/** The chunk size. */
	private int chunkSize;
	
	/** The hop size. */
	private int hopSize;
	
	/** The current chunks being recorded at the moment. */
	private float[][] chunks;
	
	/** Equal to hopSize * chunks.length. This is a single record cycle. */
	private int cycleLen;
	
	/** The time in samples. */
	private int count;
	
	/** The previous TimeStamp. */
	private TimeStamp lastTimeStamp;
	
	/** The TimeStamp of this AudioSegmenter when the AudioContext is at t=0. */
	private TimeStamp beginningTimeStamp;
	
	/** The window function used to scale the chunks. */
	private Buffer window;
	
	/**
	 * Instantiates a new ShortFrameSegmenter.
	 * 
	 * @param context the AudioContext.
	 */
	public ShortFrameSegmenter(AudioContext context) {
		super(context);
		chunkSize = context.getBufferSize();
		hopSize = chunkSize;
		window = new HanningWindow().getDefault();
		count = 0;
		setupBuffers();
	}
	
	/**
	 * Gets the chunk size.
	 * 
	 * @return the chunk size.
	 */
	public int getChunkSize() {
		return chunkSize;
	}
	
	/**
	 * Sets the chunk size.
	 * 
	 * @param chunkSize the new chunk size.
	 */
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
		setupBuffers();
	}

	/**
	 * Gets the hop size.
	 * 
	 * @return the hop size.
	 */
	public int getHopSize() {
		return hopSize;
	}

	/**
	 * Sets the hop size.
	 * 
	 * @param hopSize the new hop size.
	 */
	public void setHopSize(int hopSize) {
		this.hopSize = hopSize;
		setupBuffers();
	}
	
	/**
	 * Sets the window Buffer.
	 * 
	 * @param window the new window Buffer.
	 */
	public void setWindow(Buffer window) {
		this.window = window;
	}
	
	/**
	 * Resets the chunks array and count when anything affecting the chunk array gets changed.
	 */
	private void setupBuffers() {
		int requiredBuffers = (int)Math.ceil((float)chunkSize / (float)hopSize);
		chunks = new float[requiredBuffers][chunkSize];
		cycleLen = requiredBuffers * hopSize;
	}

	/* (non-Javadoc)
	 * @see net.beadsproject.beads.core.UGen#calculateBuffer()
	 */
	@Override
	public void calculateBuffer() {
		if(beginningTimeStamp == null) {
			resetTimeStamp();
		}
		for(int i = 0; i < bufferSize; i++) {
			for(int j = 0; j < chunks.length; j++) {
				int pos = count - j * hopSize;
				if(pos < 0) pos += cycleLen;
				if(pos < chunkSize) {
					chunks[j][pos] = bufIn[0][i] * window.getValueFraction((float)pos / (float)(chunkSize - 1));
				}
			}
			count++;
			if(count % hopSize == 0) {
				TimeStamp nextTimeStamp = TimeStamp.add(context, context.generateTimeStamp(i), beginningTimeStamp);
				int chunkIndex = count / hopSize - 1;
				segment(lastTimeStamp, 
						nextTimeStamp, 
						chunks[chunkIndex]);
				lastTimeStamp = nextTimeStamp;
			}
			count %= cycleLen;
		}
	}
	
	public void resetTimeStamp() {
		lastTimeStamp = context.generateTimeStamp(0);
		beginningTimeStamp = context.generateTimeStamp(0);
		count = 0;
	}
	
	public void setLastTimeStamp(TimeStamp ts) {
		lastTimeStamp = ts;
		count = 0;
	}

	public void setBeginningTimeStamp(TimeStamp ts) {
		beginningTimeStamp = ts;
		count = 0;
	}
	
//	/* (non-Javadoc)
//	 * @see net.beadsproject.beads.core.UGen#calculateBuffer()
//	 */
//	@Override
//	public void calculateBuffer() {
//		for(int i = 0; i < bufferSize; i++) {
//			for(int j = 0; j < chunks.length; j++) {
//				int pos = (count + j * hopSize) % chunkSize;
//				chunks[j][pos] = bufIn[0][i] * window.getValueFraction((float)pos / (float)chunkSize);
//			}
//			count = (count + 1) % chunkSize;
//			if(count % hopSize == 0) {
//				TimeStamp nextTimeStamp = context.generateTimeStamp(i);
//				segment(lastTimeStamp, nextTimeStamp, chunks[count / hopSize]);
//				lastTimeStamp = nextTimeStamp;
//			}
//		}
//	}
	

}
