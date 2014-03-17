/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import java.util.ArrayList;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.buffers.CosineWindow;

/**
 * GranularSamplePlayer plays back a {@link Sample} using granular synthesis. GranularSamplePlayer inherits its main behaviour from {@link SamplePlayer} but replaces the direct {@link Sample} lookup with a granular process. 
 * {@link UGen}s can be used to control playback rate, pitch, loop points, grain size, grain interval, grain randomness and position (this last case assumes that the playback rate is zero). 
 * 
 * @see SamplePlayer Sample
 * @beads.category sample players
 * @author ollie
 */
public class GranularSamplePlayer extends SamplePlayer {

	/** The pitch envelope. */
	private UGen pitchEnvelope;

	/** The grain interval envelope. */
	private UGen grainIntervalEnvelope;

	/** The grain size envelope. */
	private UGen grainSizeEnvelope;

	/** The randomness envelope. */
	private UGen randomnessEnvelope;
	
	/** The random pan envelope. */
	private UGen randomPanEnvelope;

	/** The time in milliseconds since the last grain was activated. */
	private float timeSinceLastGrain;

	/** The length of one sample in milliseconds. */
	private double msPerSample;

	/** The pitch, bound to the pitch envelope. */
	protected float pitch;

	/** The list of current grains. */
	private ArrayList<Grain> grains;

	/** A list of free grains. */
	private ArrayList<Grain> freeGrains;

	/** A list of dead grains. */
	private ArrayList<Grain> deadGrains;

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
		
		/** The pan level for each channel. Currently only 2 channel is supported. */
		float[] pan;
	}

	/**
	 * Instantiates a new GranularSamplePlayer.
	 * 
	 * @param context the AudioContext.
	 * @param outs the number of outputs.
	 */
	public GranularSamplePlayer(AudioContext context, int outs) {
		super(context, outs);
		grains = new ArrayList<Grain>();
		freeGrains = new ArrayList<Grain>();
		deadGrains = new ArrayList<Grain>();
		pitchEnvelope = new Static(context, 1f);
		setGrainInterval(new Static(context, 70.0f));
		setGrainSize(new Static(context, 100.0f));
		setRandomness(new Static(context, 0.0f));
		setRandomPan(new Static(context, 0.0f));
		setWindow(new CosineWindow().getDefault());
		msPerSample = context.samplesToMs(1f);
		loopInsideGrains = false;
	}

	/**
	 * Instantiates a new GranularSamplePlayer.
	 * 
	 * @param context the AudioContext.
	 * @param buffer the Sample played by the GranularSamplePlayer.
	 */
	public GranularSamplePlayer(AudioContext context, Sample buffer) {
		this(context, buffer.getNumChannels());
		setSample(buffer);
		loopStartEnvelope = new Static(context, 0.0f);
		loopEndEnvelope = new Static(context, (float)buffer.getLength());
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
		this.pitchEnvelope = pitchEnvelope;
	}
	
	/**
	 * Sets the pitch UGen.
	 * 
	 * @param pitchUGen
	 *            the new pitch Ugen.
	 */
	public void setPitch(UGen pitchUGen) {
		this.pitchEnvelope = pitchUGen;
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
		return grainIntervalEnvelope;
	}
	
	/**
	 * Gets the grain interval UGen.
	 * 
	 * @return the grain interval UGen.
	 */
	public UGen getGrainIntervalUGen() {
		return grainIntervalEnvelope;
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
		this.grainIntervalEnvelope = grainIntervalEnvelope;
	}
	
	/**
	 * Sets the grain interval UGen.
	 * 
	 * @param grainIntervalUGen
	 *            the new grain interval UGen.
	 */
	public void setGrainInterval(UGen grainIntervalUGen) {
		this.grainIntervalEnvelope = grainIntervalUGen;
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
		return grainSizeEnvelope;
	}
	

	/**
	 * Gets the grain size UGen.
	 * 
	 * @return the grain size UGen.
	 */
	public UGen getGrainSizeUGen() {
		return grainSizeEnvelope;
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
		this.grainSizeEnvelope = grainSizeEnvelope;
	}
	
	/**
	 * Sets the grain size UGen.
	 * 
	 * @param grainSizeUGen the new grain size UGen.
	 */
	public void setGrainSize(UGen grainSizeUGen) {
		this.grainSizeEnvelope = grainSizeUGen;
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
		return randomnessEnvelope;
	}
	
	/**
	 * Gets the randomness UGen.
	 * 
	 * @return the randomness UGen.
	 */
	public UGen getRandomnessUGen() {
		return randomnessEnvelope;
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
		this.randomnessEnvelope = randomnessEnvelope;
	}
	
	/**
	 * Sets the randomness UGen.
	 * 
	 * @param randomnessUGen the new randomness UGen.
	 */
	public void setRandomness(UGen randomnessUGen) {
		this.randomnessEnvelope = randomnessUGen;
	}
	
	/**
	 * @deprecated Use {@link #getRandomPanUGen()} instead.
	 * @return the random pan envelope.
	 */
	@Deprecated
	public UGen getRandomPanEnvelope() {
		return randomPanEnvelope;
	}

	/**
	 * Gets the random pan UGen.
	 * @return the random pan Ugen.
	 */
	public UGen getRandomPanUGen() {
		return randomPanEnvelope;
	}
	
	/**
	 * @deprecated Use {@link #setRandomPan(UGen)} instead.
	 * @param randomPanEnvelope
	 */
	@Deprecated
	public void setRandomPanEnvelope(UGen randomPanEnvelope) {
		this.randomPanEnvelope = randomPanEnvelope;
	}

	public void setRandomPan(UGen randomPanEnvelope) {
		this.randomPanEnvelope = randomPanEnvelope;
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
	private void resetGrain(Grain g, int time) {
		g.position = position + (grainSizeEnvelope.getValue(0, time) * randomnessEnvelope.getValue(0, time) * (Math.random() * 2.0 - 1.0));
		g.age = 0f;
		g.grainSize = grainSizeEnvelope.getValue(0, time);
	}   
	
	private void setGrainPan(Grain g, float panRandomness) {
		g.pan = new float[outs];
		if(outs == 2) {
			float pan = (float)Math.random() * Math.min(1, Math.max(0, panRandomness)) * 0.5f;
			pan = Math.random() < 0.5f ? 0.5f + pan : 0.5f - pan;
			g.pan[0] = pan > 0.5f ? 1f : 2f * pan;
			g.pan[1] = pan < 0.5f ? 1f : 2f * (1 - pan);
		} else {
			for(int i = 0; i < outs; i++) {
				g.pan[i] = 1f;
			}
		}
	}

	/** Flag to indicate special case for the first grain. */
	private boolean firstGrain = true;

	/** Special case method for playing first grain. */
	private void firstGrain() {
		if(firstGrain) {
			Grain g = new Grain();
			g.position = position;
			g.age = grainSizeEnvelope.getValue() / 4f;
			
			grains.add(g);
			firstGrain = false;
			timeSinceLastGrain = grainIntervalEnvelope.getValue() / 2f;
			setGrainPan(g, randomPanEnvelope.getValue(0, 0));
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
			rateEnvelope.update();
			if(positionEnvelope != null) {
				positionEnvelope.update();
			} 
			loopStartEnvelope.update();
			loopEndEnvelope.update();
			pitchEnvelope.update();
			grainIntervalEnvelope.update();
			grainSizeEnvelope.update();
			randomnessEnvelope.update();
			randomPanEnvelope.update();
			firstGrain();
			//now loop through the buffer
			for (int i = 0; i < bufferSize; i++) {
				//determine if we need a new grain
				if (timeSinceLastGrain > grainIntervalEnvelope.getValue(0, i)) {
					Grain g = null;
					if(freeGrains.size() > 0) {
						g = freeGrains.get(0);
						freeGrains.remove(0);
					} else {
						g = new Grain();
					}
					resetGrain(g, i);
					setGrainPan(g, randomPanEnvelope.getValue(0, i));
					grains.add(g);
					timeSinceLastGrain = 0f;
				}
				//for each channel, start by resetting current output frame
				for (int j = 0; j < outs; j++) {
					bufOut[j][i] = 0.0f;
				}
				//gather the output from each grain
				for(int gi = 0; gi < grains.size(); gi++) {
					Grain g = grains.get(gi);
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
					for (int j = 0; j < outs; j++) {
						bufOut[j][i] += g.pan[j] * windowScale * frame[j % sample.getNumChannels()];
					}
				}
				//increment time and stuff
				calculateNextPosition(i);
				pitch = Math.abs(pitchEnvelope.getValue(0, i));
				for(int gi = 0; gi < grains.size(); gi++) {
					Grain g = grains.get(gi);
					calculateNextGrainPosition(g);
				}
				//increment timeSinceLastGrain
				timeSinceLastGrain += msPerSample;
				//finally, see if any grains are dead
				for(int gi = 0; gi < grains.size(); gi++) {
					Grain g = grains.get(gi);
					if(g.age > g.grainSize) {
						freeGrains.add(g);
						deadGrains.add(g);
					}
				}
				for(int gi = 0; gi < deadGrains.size(); gi++) {
					Grain g = deadGrains.get(gi);
					grains.remove(g);
				}
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
	 * Calculates the average number of Grains given the current grain size and grain interval.
	 * @return the average number of Grains.
	 */
	public float getAverageNumberOfGrains() {
		return grainSizeEnvelope.getValue() / grainIntervalEnvelope.getValue();
	}

}
