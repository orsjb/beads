/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.Sample;

/**
 * RecordToSample records audio into a writeable {@link Sample}.
 * 
 * RecordToSample has three different modes, which dictate 
 * how it behaves when the end of the sample is reached:
 * <ul>
 * <li>FINITE (the default): The recorder kills itself.</li>
 * <li>LOOPING: The recorder loop back to the beginning of the sample.</li>
 * <li>INFINITE: The recorder increases the size of the sample.</li>
 * </ul>
 *
 * A recorder may not completely fill a sample. If you just want the recorded data
 * then be sure to {@link #clip()} the sample once done. Alternatively you can see
 * {@link #getNumFramesRecorded() how many frames were written}. 
 * 
 * <p>
 * <b>Tip: </b> Be sure to {@link #pause(boolean)} the recorder when using INFINITE mode, 
 * otherwise it will keep recording and you may quickly run out of memory.
 * </p>
 * 
 * <i>ADVANCED:</i> When resizing a sample in INFINITE mode, the recorder uses a set of parameters
 * that specify how it behaves. If necessary you can modify the parameters on a per-recorder basis. See
 * {@link #setResizingParameters(double, double)}.
 * 
 * @beads.category utilities
 *  
 */
public class RecordToSample extends UGen {

    /** The Sample to record into. */
    private Sample sample;
    
    /** The position in samples. */
    private long position;
    
    /** The number of frames of recorded data in the sample. 
     * 
     * A recorder may never write the entire length of the sample. Hence
     * this variable keeps track of the recorded section.
     * 
     * In particular, in INFINITE mode, framesWritten will always be less 
     * than sample.getNumFrames(). In this case, it is often necessary to resize
     * the sample once you have finished writing into it. This is where clip()
     * comes in.
     **/
    private long framesWritten;
    
    public enum Mode {
    	FINITE,
    	LOOPING,
    	INFINITE
    };
    
    /** Recording mode. */
    private Mode mode;
    
    /** Resizing parameters - in ms.*/
    private double doubleUpTime = 0;
	private double constantResizeLength = 10000;
	
	// computed from the above..
	private long doubleUpFrame; 
	private long constantResizeLengthInFrames; 
	
        
    /**
	 * Instantiates a new RecordToSample.
	 * 
	 * @param context
	 *            the AudioContext.
	 * @param sample
	 *            the Sample.
     * @throws Exception if sample is not writeable.
	 */
    public RecordToSample(AudioContext context, Sample sample) {
        this(context, sample, Mode.FINITE);
    }
    
    /**
	 * Instantiates a new RecordToSample.
	 * 
	 * @param context
	 *            the AudioContext.
	 * @param sample
	 *            the Sample.
	 * @param mode
	 *            the Recording Mode to use.
     * @throws Exception if sample is not writeable.
	 */
    public RecordToSample(AudioContext context, Sample sample, Mode mode) {
        this(context, sample.getNumChannels());
        this.mode = mode;
        setSample(sample);
    }
    
    public RecordToSample(AudioContext context, int numChannels) {
    	super(context, numChannels, 0);
    	mode = Mode.FINITE;
        sample = null;
    }

	/**
	 * Gets the Sample.
	 * 
	 * @return the Sample.
	 */
	public Sample getSample() {
        return sample;
    }

    /**
	 * Sets the Sample.
	 * 
	 * @param sample
	 *            the new Sample.
	 */
    public void setSample(Sample sample) {
        this.sample = sample;
        framesWritten = 0;
        position = 0;
        doubleUpFrame = (long) sample.msToSamples(doubleUpTime);
        constantResizeLengthInFrames = (long) sample.msToSamples(constantResizeLength); 
    }
    
    /**
	 * Resets the Recorder to record into the beginning of the Sample.
	 */
    public void reset() {
        position = 0;
    }
    
    /**
     * Once you have finished writing into a sample this method clips the sample
     * length to the recorded data. 
     */
    public void clip() {
		sample.resize(framesWritten);
    }
    
    /**
	 * Sets the position to record to in milliseconds.
	 * 
	 * @param positionMs
	 *            the new position in milliseconds.
	 */
    public void setPosition(double positionMs) {
        position = (long) sample.msToSamples(positionMs);
        framesWritten = position;
    }
    
    /* (non-Javadoc)
     * @see com.olliebown.beads.core.UGen#calculateBuffer()
     */
    @Override
    public void calculateBuffer() { 
    	if(sample != null) {
	    	long nFrames = sample.getNumFrames();
	    	if ((position + bufferSize) >= nFrames)
	    	{
	    		switch (mode)
	    		{
	    			case FINITE: 
	    			{
	    				sample.putFrames((int)position, bufIn, 0, (int)(nFrames-position));
	    				framesWritten = Math.max(framesWritten,nFrames);
	    				kill();
	    				break;
	    			}
	    			
	    			case LOOPING:
	    			{
	    				int framesToEnd = (int)(nFrames - position);
	    				int numframesleft = bufferSize - framesToEnd;
	    				
	    				/*                  nFrames 
	    				 *                     V
	    				 * [      sample       ]
	    				 *                 [ bufIn ]
	    				 *                 A<--bs->
	    				 *                 |
	    				 *                pos
	    				 * 
	    				 * first chunk
	    				 *                 [f2e]
	    				 * second chunk
	    				 * [    ]
	    				 */
	    				
	    				sample.putFrames((int)position, bufIn, 0, framesToEnd);
	    				sample.putFrames(0, bufIn, framesToEnd, numframesleft);
		        		
	    				position += bufferSize;
		        		position %= nFrames;
		    			framesWritten = Math.max(framesWritten,nFrames);
		        		break;
	    			}
	    			
	    			case INFINITE:
	    			{
	    				// adjust the size of the sample
	    				// for now, we double the size of the sample.
	    				try {
	    					if (position < doubleUpFrame)
	    					{
	    						sample.resize(nFrames*2);
	    					}
	    					else
	    					{
	    						sample.resize(nFrames + constantResizeLengthInFrames);
	    					}							
						} catch (Exception e) { /* won't happen */ }
						
						sample.putFrames((int)position, bufIn);    		
			    		position += bufferSize;
			    		framesWritten = Math.max(framesWritten,position);
			    		break;
	    			}
	    		}	    		
	    	}
	    	else // general case
	    	{
	    		sample.putFrames((int)position, bufIn);    		
	    		position += bufferSize;
	    		framesWritten = Math.max(framesWritten,position);
	    	}   
    	}
    }
    
    /**
	 * Gets the position.
	 * 
	 * @return the position
	 */
    public double getPosition() {
    	return context.samplesToMs(position);
    }
    
    /**
     * @return The number of frames recorded into the sample.
     */
    public long getNumFramesRecorded()
    {
    	return framesWritten;
    }

    /**
     * @return The mode this recorder is operating in.
     */
    public Mode getMode()
    {
    	return mode;
    }
    
    /**
     * @param mode Change the mode of this recorder. Can be changed while running.
     */
    public void setMode(Mode mode)
    {
    	this.mode = mode;
    }
    
    /**
     * <i>Advanced:</i> Change the parameters used when resizing samples in INFINITE recorder mode.
     * 
     * In INFINITE mode the recorder resizes the sample when it needs to write more data into it. Initially
     * the recorder doubles the length of the sample, up to a particular length. Once the sample size 
     * is past this length the recorder stops doubling the length, and simply resizes the sample by a 
     * constant amount.  
     * 
     * @param doubleUpTime The time (in ms) up to which the sample size should be doubled.
     * @param constantResizeLength The length (in ms) of the extra space appended to Sample. 
     */
    public void setResizingParameters(double doubleUpTime, double constantResizeLength)
    {
    	this.doubleUpTime = doubleUpTime;
    	this.constantResizeLength = constantResizeLength;
    	
    	this.doubleUpFrame = (long) sample.msToSamples(this.doubleUpTime);
    	this.constantResizeLengthInFrames = (long) sample.msToSamples(this.constantResizeLength);
    }
    
	/**
	 * Checks if loop record mode is enabled.
	 * 
	 * @return true if loop record mode is enabled.
	 * @deprecated Use {@link #getMode()} instead.
	 */
	public boolean isLoopRecord() {
		return mode==Mode.LOOPING;
	}

	/**
	 * Starts/stops loop record mode.
	 * 
	 * @param loopRecord true to enable loop record mode.
	 * @deprecated Use {@link #setMode(Mode)} instead.
	 */
	public void setLoopRecord(boolean loopRecord) {
		mode = Mode.LOOPING;
	}
	
}
