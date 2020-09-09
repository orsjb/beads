/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.data.buffers.CosineWindow;

/**
 * GranularSamplePlayer plays back a {@link Sample} using granular synthesis. GranularSamplePlayer inherits its main behaviour from {@link SamplePlayer} but replaces the direct {@link Sample} lookup with a granular process. 
 * {@link UGen}s can be used to control playback rate, pitch, loop points, grain size, grain interval, grain randomness and position (this last case assumes that the playback rate is zero). 
 * 
 * @see SamplePlayer Sample
 * @author ollie
 */
public class FastGranularSamplePlayer extends SamplePlayer {

	/** The pitch envelope. */
	private UGen pitchEnvelope;

	/** The grain interval envelope. */
	private float grainIntervalEnvelope;

	/** The grain size envelope. */
	private float grainSizeEnvelope;

	/** The randomness envelope. */
	private float randomnessEnvelope;

	/** The time in milliseconds since the last grain was activated. */
	private float timeSinceLastGrain;

	private float loopStart;
	
	private float loopEnd;

	/** The length of one sample in milliseconds. */
	private double msPerSample;

	/** The pitch, bound to the pitch envelope. */
	protected float pitch;

	/** The list of current grains. */
	private LinkedList<Grain> grains;

	/** A list of free grains. */
	private LinkedList<Grain> freeGrains;

	/** A list of dead grains. */
	private LinkedList<Grain> deadGrains;

	/** The window used by grains. */
	private Buffer window;

	/** Flag to determine whether, looping occurs within individual grains. */
	private boolean loopInsideGrains;

	/**
	 * The nested class Grain. Stores information about the start time, current position, age, and grain size of the grain.
	 */
	private static class Grain {

		/** The position in millseconds. */
		double position;

		/** The age of the grain in milliseconds. */
		double age;

		/** The grain size of the grain. Fixed at instantiation. */
		double grainSize;
	}

	/**
	 * Instantiates a new GranularSamplePlayer.
	 * 
	 * @param context the AudioContext.
	 * @param outs the number of outputs.
	 */
	public FastGranularSamplePlayer(AudioContext context, int outs) {
		super(context, outs);
		grains = new LinkedList<Grain>();
		freeGrains = new LinkedList<Grain>();
		deadGrains = new LinkedList<Grain>();
		pitchEnvelope = new Static(context, 1f);
        setGrainInterval(new Static(context, 70.0f));
        setGrainSize(new Static(context, 100.0f));
        setRandomness(new Static(context, 0.0f));
		setWindow(new CosineWindow().getDefault());
		msPerSample = context.samplesToMs(1f);
		loopInsideGrains = false;
	}

	/**
	 * Instantiates a new GranularSamplePlayer.
	 *
	 * @param outs the number of outputs.
	 */
	public FastGranularSamplePlayer(int outs) {
		this(getDefaultContext(), outs);
	}

	/**
	 * Instantiates a new GranularSamplePlayer.
	 * 
	 * @param context the AudioContext.
	 * @param buffer the Sample played by the GranularSamplePlayer.
	 */
	public FastGranularSamplePlayer(AudioContext context, Sample buffer) {
		this(context, buffer.getNumChannels());
		setSample(buffer);
		loopStart = 0.0f;
		loopEnd = (float)buffer.getLength();
	}

	/**
	 * Instantiates a new GranularSamplePlayer.
	 *
	 * @param buffer the Sample played by the GranularSamplePlayer.
	 */
	public FastGranularSamplePlayer(Sample buffer) {
		this(getDefaultContext(), buffer);
	}

	   /**
     * Gets the rate envelope.
     * 
     * @deprecated use {@link #getRateUGen()} instead.
     * 
     * @return the rate envelope.
     */
    @Deprecated
    public UGen getRateEnvelope() {
        return new Static(rate);
    }

    /**
     * Gets the rate UGen.
     * 
     * @return the rate UGen.
     */
    public UGen getRateUGen() {
        return new Static(rate);
    }

    /**
     * Sets the rate envelope.
     * 
     * @deprecated use {@link #setRate(UGen)} instead.
     * 
     * @param rateEnvelope
     *            the new rate envelope.
     */
    @Deprecated
    public void setRateEnvelope(UGen rateEnvelope) {
        rateEnvelope.update();
        this.rate = rateEnvelope.getValue();
    }

    /**
     * Sets the rate to a UGen.
     * 
     * @param rateUGen
     *            the new rate UGen.
     */
    public void setRate(UGen rateUGen) {
        rateUGen.update();
        this.rate = rateUGen.getValue();
    }

	/**
	 * Gets the pitch envelope.
	 * 
	 * @deprecated use {@link #getPitchUGen()}.
	 * @return the pitch envelope.
	 */
	@Deprecated
	public UGen getPitchEnvelope() {
		return pitchEnvelope;
	}
	

	/**
	 * Gets the pitch UGen.
	 * 
	 * @return the pitch UGen.
	 */
	public UGen getPitchUGen() {
		return pitchEnvelope;
	}

	/**
	 * Sets the pitch envelope.
	 * 
	 * @deprecated Use {@link #setPitch(UGen)} instead.
	 * 
	 * @param pitchEnvelope
	 *            the new pitch envelope.
	 */
	@Deprecated
	public void setPitchEnvelope(UGen pitchEnvelope) {
	    pitchEnvelope.update();
		this.pitchEnvelope = pitchEnvelope;
	}
	
	/**
	 * Sets the pitch UGen.
	 * 
	 * @param pitchUGen
	 *            the new pitch Ugen.
	 */
	public void setPitch(UGen pitchUGen) {
	    pitchUGen.update();
		this.pitchEnvelope = pitchUGen;
	}

	   /**
     * Gets the loop end envelope.
     * 
     * @deprecated Use {@link #getLoopEndUGen()} instead.
     * 
     * @return the loop end envelope.
     */
    @Deprecated
    public UGen getLoopEndEnvelope() {
        return new Static(loopEnd);
    }

    /**
     * Gets the loop end UGen.
     * 
     * @return the loop end UGen.
     */
    public UGen getLoopEndUGen() {
        return new Static(loopEnd);
    }

    /**
     * Sets the loop end envelope.
     * 
     * @deprecated Use {@link #setLoopEnd(UGen)} instead.
     * 
     * @param loopEndEnvelope
     *            the new loop end envelope.
     */
    @Deprecated
    public void setLoopEndEnvelope(UGen loopEndEnvelope) {
        loopEndEnvelope.update();
        this.loopEnd = loopEndEnvelope.getValue();
    }

    /**
     * Sets the loop end UGen.
     * 
     * @param loopEndUGen
     *            the new loop end UGen.
     */
    public void setLoopEnd(UGen loopEndUGen) {
        loopEndUGen.update();
        this.loopEnd = loopEndUGen.getValue();
    }

    /**
     * Gets the loop start envelope.
     * 
     * @deprecated Use {@link #getLoopStartUGen()} instead.
     * @return the loop start envelope
     */
    @Deprecated
    public UGen getLoopStartEnvelope() {
        return new Static(loopStart);
    }

    /**
     * Gets the loop start UGen.
     * 
     * @return the loop start UGen
     */
    public UGen getLoopStartUGen() {
        return new Static(loopStart);
    }

    /**
     * Sets the loop start envelope.
     * 
     * @deprecated Use {@link #setLoopStart(UGen)} instead.
     * 
     * @param loopStartEnvelope
     *            the new loop start envelope.
     */
    @Deprecated
    public void setLoopStartEnvelope(UGen loopStartEnvelope) {
        loopStartEnvelope.update();
        this.loopStart = loopStartEnvelope.getValue();
    }

    /**
     * Sets the loop start UGen.
     * 
     * @param loopStartUGen
     *            the new loop start UGen.
     */
    public void setLoopStart(UGen loopStartUGen) {
        loopStartUGen.update();
        this.loopStart = loopStartUGen.getValue();
    }

    /**
     * Sets both loop points to static values as fractions of the Sample length,
     * overriding any UGens that were controlling the loop points.
     * 
     * @param start
     *            the start value, as fraction of the Sample length.
     * @param end
     *            the end value, as fraction of the Sample length.
     */
    public void setLoopPointsFraction(float start, float end) {
        loopStart = start * (float) sample.getLength();
        loopEnd = end * (float) sample.getLength();
    }
    
	/**
	 * Gets the grain interval envelope.
	 * 
	 * @deprecated Use {@link #getGrainIntervalUGen()} instead.
	 * 
	 * @return the grain interval envelope.
	 */
	@Deprecated
	public UGen getGrainIntervalEnvelope() {
		return new Static(grainIntervalEnvelope);
	}
	
	/**
	 * Gets the grain interval UGen.
	 * 
	 * @return the grain interval UGen.
	 */
	public UGen getGrainInterval() {
		return new Static(grainIntervalEnvelope);
	}

	/**
	 * Sets the grain interval envelope.
	 * 
	 * @deprecated Use {@link #setGrainInterval(UGen)} instead.
	 * 
	 * @param grainIntervalEnvelope
	 *            the new grain interval envelope.
	 */
	@Deprecated
	public void setGrainIntervalEnvelope(UGen grainIntervalEnvelope) {
	    grainIntervalEnvelope.update();
		this.grainIntervalEnvelope = grainIntervalEnvelope.getValue();
	}
	
	/**
	 * Sets the grain interval UGen.
	 * 
	 * @param grainIntervalUGen
	 *            the new grain interval UGen.
	 */
	public void setGrainInterval(UGen grainIntervalUGen) {
	    grainIntervalUGen.update();
		this.grainIntervalEnvelope = grainIntervalUGen.getValue();
	}

	/**
	 * Gets the grain size envelope.
	 * 
	 * @deprecated Use {@link #getGrainSizeUGen()} instead.
	 * 
	 * @return the grain size envelope.
	 */
	@Deprecated
	public UGen getGrainSizeEnvelope() {
		return new Static(grainSizeEnvelope);
	}
	

	/**
	 * Gets the grain size UGen.
	 * 
	 * @return the grain size UGen.
	 */
	public UGen getGrainSize() {
		return new Static(grainSizeEnvelope);
	}

	/**
	 * Sets the grain size envelope.
	 * 
	 * @deprecated Use {@link #setGrainSize(UGen)} instead.
	 * 
	 * @param grainSizeEnvelope the new grain size envelope.
	 */
	@Deprecated
	public void setGrainSizeEnvelope(UGen grainSizeEnvelope) {
	    grainSizeEnvelope.update();
		this.grainSizeEnvelope = grainSizeEnvelope.getValue();
	}
	
	/**
	 * Sets the grain size UGen.
	 * 
	 * @param grainSizeUGen the new grain size UGen.
	 */
	public void setGrainSize(UGen grainSizeUGen) {
	    grainSizeUGen.update();
		this.grainSizeEnvelope = grainSizeUGen.getValue();
	}
	
	public Buffer getWindow() {
		return window;
	}

	
	public void setWindow(Buffer window) {
		this.window = window;
	}

	/**
	 * Gets the randomness envelope.
	 * 
	 * @deprecated Use {@link #getRandomnessUGen()} instead.
	 * 
	 * @return the randomness envelope.
	 */
	@Deprecated
	public UGen getRandomnessEnvelope() {
		return new Static(randomnessEnvelope);
	}
	
	/**
	 * Gets the randomness UGen.
	 * 
	 * @return the randomness UGen.
	 */
	public UGen getRandomness() {
		return new Static(randomnessEnvelope);
	}

	/**
	 * Sets the randomness envelope.
	 * 
	 * @deprecated Use {@link #setRandomness(UGen)} instead.
	 * 
	 * @param randomnessEnvelope the new randomness envelope.
	 */
	@Deprecated
	public void setRandomnessEnvelope(UGen randomnessEnvelope) {
	    randomnessEnvelope.update();
		this.randomnessEnvelope = randomnessEnvelope.getValue();
	}
	
	/**
	 * Sets the randomness UGen.
	 * 
	 * @param randomnessUGen the new randomness UGen.
	 */
	public void setRandomness(UGen randomnessUGen) {
	    randomnessUGen.update();
		this.randomnessEnvelope = randomnessUGen.getValue();
	}

	/**
	 * @deprecated Use {@link #setSample(Sample)} instead.
	 */
	@Deprecated
	public synchronized void setBuffer(Sample buffer) {
		super.setSample(buffer);
		grains.clear();
		timeSinceLastGrain = 0f;
	}
	
	/* (non-Javadoc)
	 * @see net.beadsproject.beads.ugens.SamplePlayer#setBuffer(net.beadsproject.beads.data.Sample)
	 */
	public synchronized void setSample(Sample buffer) {
		super.setSample(buffer);
		grains.clear();
		timeSinceLastGrain = 0f;
	}

	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.UGen#start()
	 */
	@Override
	public void start() {
		super.start();
		timeSinceLastGrain = 0;
	}

	/**
	 * Sets the given Grain to start immediately.
	 * 
	 * @param g
	 *            the g
	 * @param time
	 *            the time
	 */
	private void resetGrain(Grain g) {
		g.position = position + grainSizeEnvelope * randomnessEnvelope* (Math.random() * 2.0 - 1.0);
		g.age = 0f;
		g.grainSize = grainSizeEnvelope;
	}   

	@Override
	public void reset() {
		super.reset();
		firstGrain = true;
	}

	/** Flag to indicate special case for the first grain. */
	private boolean firstGrain = true;

	/** Special case method for playing first grain. */
	private void firstGrain() {
		if(firstGrain) {
			Grain g = new Grain();
			g.position = position;
			g.age = grainSizeEnvelope / 4f;
			g.grainSize = grainSizeEnvelope;
			grains.add(g);
			firstGrain = false;
			timeSinceLastGrain = grainIntervalEnvelope / 2f;
		}
	}

	/* (non-Javadoc)
	 * @see com.olliebown.beads.ugens.SamplePlayer#calculateBuffer()
	 */
	@Override
	public synchronized void calculateBuffer() {
		//special condition for first grain
		//update the various envelopes
		if(sample != null) {
			if(positionEnvelope != null) {
				positionEnvelope.update();
			} 
			pitchEnvelope.update();
			firstGrain();
			//now loop through the buffer
			for (int i = 0; i < bufferSize; i++) {
				//determine if we need a new grain
				if (timeSinceLastGrain > grainIntervalEnvelope) {
					Grain g = null;
					if(freeGrains.size() > 0) {
						g = freeGrains.pollFirst();
					} else {
						g = new Grain();
					}
					resetGrain(g);
					grains.add(g);
					timeSinceLastGrain = 0f;
				}
				//for mono channel, start by resetting current output frame
				bufOut[0][i] = 0.0f;
				
				//gather the output from each grain
				for(Grain g : grains) {
					//calculate value of grain window
					float windowScale = window.getValueFraction((float)(g.age / g.grainSize));
					//get position in sample for this grain
					//get the frame for this grain
					switch (interpolationType) {
					case ADAPTIVE: 
						if(pitch > ADAPTIVE_INTERP_HIGH_THRESH) {
							sample.getFrameNoInterp(g.position, frame);
						} else if(pitch > ADAPTIVE_INTERP_LOW_THRESH) {
							sample.getFrameLinear(g.position, frame);
						} else {
							sample.getFrameCubic(g.position, frame);
						}
						break;
					case LINEAR:
						sample.getFrameLinear(g.position, frame);
						break;
					case CUBIC:
						sample.getFrameCubic(g.position, frame);
						break;
					case NONE:
						sample.getFrameNoInterp(g.position, frame);
						break;
					}
					//add it to the current output frame
					bufOut[0][i] += windowScale * frame[0 % sample.getNumChannels()];
				}
				//increment time and stuff
				calculateNextPosition(i);
				pitch = Math.abs(pitchEnvelope.getValue(0, i));
				for(Grain g : grains) {
					calculateNextGrainPosition(g);
				}
				//increment timeSinceLastGrain
				timeSinceLastGrain += msPerSample;
				//finally, see if any grains are dead
				for(Grain g : grains) {
					if(g.age > g.grainSize) {
						freeGrains.add(g);
						deadGrains.add(g);
					}
				}
				grains.removeAll(deadGrains);
				deadGrains.clear();
			}
        }
    }
    
	/**
	 * Calculate next position for the given Grain.
	 * 
	 * @param g the Grain.
	 */
	private void calculateNextGrainPosition(Grain g) {
		int direction = rate >= 0 ? 1 : -1;	//this is a bit odd in the case when controlling grain from positionEnvelope
		g.age += msPerSample;
		if(loopInsideGrains) {
			switch(loopType) {
			case NO_LOOP_FORWARDS:
				g.position += direction * positionIncrement * pitch;
				break;
			case NO_LOOP_BACKWARDS:
				g.position -= direction * positionIncrement * pitch;
				break;
			case LOOP_FORWARDS:
				g.position += direction * positionIncrement * pitch;
				if(rate > 0 && g.position > Math.max(loopStart, loopEnd)) {
					g.position = Math.min(loopStart, loopEnd);
				} else if(rate < 0 && g.position < Math.min(loopStart, loopEnd)) {
					g.position = Math.max(loopStart, loopEnd);
				}
				break;
			case LOOP_BACKWARDS:
				g.position -= direction * positionIncrement * pitch;
				if(rate > 0 && g.position < Math.min(loopStart, loopEnd)) {
					g.position = Math.max(loopStart, loopEnd);
				} else if(rate < 0 && g.position > Math.max(loopStart, loopEnd)) {
					g.position = Math.min(loopStart, loopEnd);
				}
				break;
			case LOOP_ALTERNATING:
				g.position += direction * (forwards ? positionIncrement * pitch : -positionIncrement * pitch);
				if(forwards ^ (rate < 0)) {
					if(g.position > Math.max(loopStart, loopEnd)) {
						g.position = 2 * Math.max(loopStart, loopEnd) - g.position;
					}
				} else if(g.position < Math.min(loopStart, loopEnd)) {
					g.position = 2 * Math.min(loopStart, loopEnd) - g.position;
				}
				break;
			}   
		} else {
			g.position += direction * positionIncrement * pitch;
		}
	}

	   /**
     * Used at each sample in the perform routine to determine the next playback
     * position.
     * 
     * @param i
     *            the index within the buffer loop.
     */
	@Override
    protected void calculateNextPosition(int i) {
        if (positionEnvelope != null) {
            position = positionEnvelope.getValueDouble(0, i);
        } else {
            switch (loopType) {
            case NO_LOOP_FORWARDS:
                position += positionIncrement * rate;
                if (position > sample.getLength() || position < 0)
                    atEnd();
                break;
            case NO_LOOP_BACKWARDS:
                position -= positionIncrement * rate;
                if (position > sample.getLength() || position < 0)
                    atEnd();
                break;
            case LOOP_FORWARDS:
                position += positionIncrement * rate;
                if (rate > 0 && position > Math.max(loopStart, loopEnd)) {
                    position = Math.min(loopStart, loopEnd);
                } else if (rate < 0 && position < Math.min(loopStart, loopEnd)) {
                    position = Math.max(loopStart, loopEnd);
                }
                break;
            case LOOP_BACKWARDS:
                position -= positionIncrement * rate;
                if (rate > 0 && position < Math.min(loopStart, loopEnd)) {
                    position = Math.max(loopStart, loopEnd);
                } else if (rate < 0 && position > Math.max(loopStart, loopEnd)) {
                    position = Math.min(loopStart, loopEnd);
                }
                break;
            case LOOP_ALTERNATING:
                position += forwards ? positionIncrement * rate
                        : -positionIncrement * rate;
                if (forwards ^ (rate < 0)) {
                    if (position > Math.max(loopStart, loopEnd)) {
                        forwards = (rate < 0);
                        position = 2 * Math.max(loopStart, loopEnd) - position;
                    }
                } else if (position < Math.min(loopStart, loopEnd)) {
                    forwards = (rate > 0);
                    position = 2 * Math.min(loopStart, loopEnd) - position;
                }
                break;
            }
        }
    }
	/**
	 * Calculates the average number of Grains given the current grain size and grain interval.
	 * @return the average number of Grains.
	 */
	public float getAverageNumberOfGrains() {
		return grainSizeEnvelope / grainIntervalEnvelope;
	}


//	public static void main(String[] args) {
//		AudioContext ac = new AudioContext();
//		ac.start();
//		//clock
//		Clock c = new Clock(ac, 500);
//		ac.out.addDependent(c);
//		GranularSamplePlayer gsp = new GranularSamplePlayer(ac, SampleManager.sample("/Users/ollie/git/HappyBrackets/HappyBrackets/data/audio/guit.wav"));
//		gsp.getRateUGen().setValue(0.1f);
//		ac.out.addInput(gsp);
//		c.addMessageListener(new Bead() {
//			@Override
//			protected void messageReceived(Bead bead) {
//				if (c.getCount() % 32 == 0) {
//					gsp.reset();
//				}
//			}
//		});
//	}


}
