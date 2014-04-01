/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.core.BeadArray;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.events.IntegerBead;

/**
 * A sample rate Clock. A Clock generates timing data at two levels: ticks and beats. It notifies an {@link BeadArray} of listeners at each tick. These listeners can query the Clock to find out the current tick count, if it is on a beat, and the current beat count. The rate of ticking of the Clock is controlled by an interval envelope.
 *
 * @beads.category control
 * @author ollie
 */
public class Clock extends UGen implements IntegerBead {

    /** The interval envelope. */
    private UGen intervalEnvelope;
    
    /** The current point in time of the Clock in ticks. */
    private double point;
    
    /** The current tick count of the clock. */
    private long count;

    /** The number of ticks per beat of the clock. */
    private int ticksPerBeat;
    
    /** The listeners. */
    private BeadArray listeners;
    
    /** Boolean to determine whether this clock makes an audible click on beats. */
    private boolean click;
    
    /** The strength (gain) of the audible click. */
    private float clickStrength;
    
    /** Used so that other objects can discover what */
    private double[] subticks;
    
    /**
     * Instantiates a new Clock with a static interval of 1000ms.
     * 
     * @param context the AudioContext.
     */
    public Clock(AudioContext context) {
        this(context, 1000.0f);
    }
    
    /**
     * Instantiates a new Clock with the given static interval in milliseconds.
     * 
     * @param context the AudioContext.
     * @param interval the interval in milliseconds.
     */
    public Clock(AudioContext context, float interval) {
        this(context, new Static(context, interval));
        ticksPerBeat = 16;
    }
    
    /**
     * Instantiates a new Clock with the given interval enveliope.
     * 
     * @param context the AudioContext.
     * @param env the interval envelope.
     */
    public Clock(AudioContext context, UGen env) {
        super(context, 0, 0);
        intervalEnvelope = env;
        listeners = new BeadArray();
        reset();
        ticksPerBeat = 16;
        clickStrength = 0.1f;
        subticks = new double[context.getBufferSize()];
    }
    
	/**
	 * Checks if the Clock is set to make an audible click.
	 * 
	 * @return true if clicking.
	 */
	public boolean isClicking() {
		return click;
	}
	
	/**
	 * Starts/stops the audible click.
	 * 
	 * @param click true for audible click.
	 */
	public void setClick(boolean click) {
		this.click = click;
	}

	/**
	 * Adds a new message listener.
	 * 
	 * @param newListener the new message listener.
	 */
	public void addMessageListener(Bead newListener) {
        listeners.add(newListener);
    }
    
    /**
     * Removes the given message listener.
     * 
     * @param newListener the listener being removed.
     */
    public void removeMessageListener(Bead newListener) {
        listeners.remove(newListener);
    }
    
    /**
     * Resets the Clock immediately.
     */
    public void reset() {
        point = 0.0f;
        count = -1;	//OLLIE - hack to get the first tick to be a beat
//        tick();	//OLLIE - this must be pointless, if we haven't connect the clock to anything
    }

    /**
     * Gets the tick count.
     * 
     * @return the tick count.
     */
    public long getCount() {
        return (int)Math.floor(point);
    }

    /**
     * Sets the interval envelope.
     * 
     * @param intervalEnvelope the new interval envelope.
     */
    public void setIntervalEnvelope(UGen intervalEnvelope) {
        this.intervalEnvelope = intervalEnvelope;
    }
    
    /**
     * Deprecated. Use {getIntervalUGen()} instead.
     * Gets the interval envelope.
     * 
     * @return the interval envelope.
     * 
     * @deprecated
     */
    public UGen getIntervalEnvelope() {
    	return intervalEnvelope;    	
    }
    
    /**
     * Gets the interval envelope.
     * 
     * @return the interval envelope.
     */
    public UGen getIntervalUGen() {
    	return intervalEnvelope;    	
    }
    
    public float getTempo() {
    	return 60000f / Math.abs(intervalEnvelope.getValue());
    }
    
    /* (non-Javadoc)
     * @see com.olliebown.beads.core.UGen#calculateBuffer()
     */
    @Override
    public void calculateBuffer() {
    	intervalEnvelope.update();
    	for(int i = 0; i < bufferSize; i++) {  
    		subticks[i] = point;
    		double interval = intervalEnvelope.getValueDouble(0, i);
    		double value = Math.max(1.0, Math.abs(interval) / ticksPerBeat);
    		boolean backwards = interval < 0;
    		if(backwards) value *= -1;
    		point += 1.0 / context.msToSamples(value); //OLLIE - TODO We don't get a BEAT on the first TICK
    		//what happens if we start going backwards?
    		while(!backwards && point >= count + 1) {// || point < -count) {
    			tick();
    			count += Math.signum(interval);
    		} 
    		while(backwards && point <= count) {
    			tick();
    			count += Math.signum(interval);
    		}
    	}
    }
    
    /**
     * Trigger a tick.
     */
    private void tick() {
    	if(click && isBeat()) context.out.addInput(new Clicker(context, clickStrength));
    	listeners.message(this);
    }

	/* (non-Javadoc)
	 * @see com.olliebown.beads.events.IntegerBead#getInt()
	 */
	public int getInt() {
		return (int)getCount();
	}

	/**
	 * Gets the ticks per beat.
	 * 
	 * @return the ticks per beat.
	 */
	public int getTicksPerBeat() {
		return ticksPerBeat;
	}

	/**
	 * Sets the ticks per beat.
	 * 
	 * @param ticksPerBeat the new ticks per beat.
	 */
	public void setTicksPerBeat(int ticksPerBeat) {
		this.ticksPerBeat = Math.max(1, ticksPerBeat);
	}
	
	/**
	 * Checks if the current tick is on a beat.
	 * 
	 * @return true if the current tick is a beat.
	 */
	public boolean isBeat() {
		return getCount() % ticksPerBeat == 0;
	}
	
	/**
	 * Checks if the Clock is on a beat at the given modulo level. 
	 * 
	 * @param mod the modulo.
	 * 
	 * @return true if the clock is on a beat, and the current beat count is a multiple of mod.
	 */
	public boolean isBeat(int mod) {
		return isBeat() && getBeatCount() % mod == 0;
	}
	
	/**
	 * Gets the current beat count.
	 * 
	 * @return the current beat count.
	 */
	public int getBeatCount() {
		return (int)(getCount() / ticksPerBeat);
	}
	
	public double getSubTickAtIndex(int i) {
		return subticks[i];
	}
	
	public double getSubTickNow() {
		return point;
	}
	
	public void clearMessageListeners() {
		listeners.clear();
	}

}





