/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.AudioUtils;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.Sample;

/**
 * SamplePlayer plays back a {@link Sample}. Playback rate and loop points can
 * be controlled by {@link UGen}s. The playback point in the {@link Sample} can
 * also be directly controlled from {@link UGen} to perform scrubbing. The
 * player can be set to a number of different loop modes. If constructed with a
 * {@link Sample} argument, the number of outputs of SamplePlayer is determined
 * by the number of channels of the {@link Sample}. {@link Sample} playback can
 * use either linear or cubic interpolation.
 * 
 * TODO: Loop cross-fading has not been implemented yet.
 * 
 * @beads.category sample players
 * @author ollie
 */
public class SamplePlayer extends UGen {

	public static final float ADAPTIVE_INTERP_LOW_THRESH = 0.5f;
	public static final float ADAPTIVE_INTERP_HIGH_THRESH = 2.5f;

	/**
	 * Used to determine what kind of interpolation is used when access samples.
	 */
	public static enum InterpolationType {

		/** Use no interpolation. */
		NONE,

		/** Use linear interpolation. */
		LINEAR,

		/** Use cubic interpolation. */
		CUBIC,

		/**
		 * Use context dependent interpolation: NONE above 2x, CUBIC below 0.5x,
		 * LINEAR otherwise.
		 */
		ADAPTIVE
	};

	/**
	 * Used to determine which kind of loop the sample player will use.
	 */
	public static enum LoopType {

		/** Play forwards without looping. */
		NO_LOOP_FORWARDS,

		/** Play backwards without looping. */
		NO_LOOP_BACKWARDS,

		/** Play forwards with loop. */
		LOOP_FORWARDS,

		/** Play backwards with loop. */
		LOOP_BACKWARDS,

		/** Loop alternately forwards and backwards. */
		LOOP_ALTERNATING

	};

	/**
	 * Used to determine whether the SamplePlayer updates control values at
	 * sample rate ({@link EnvelopeType#FINE}) or just every frame (
	 * {@link EnvelopeType#COARSE}).
	 * 
	 * @author ollie
	 * 
	 */
	public static enum EnvelopeType {
		/**
		 * Sample the controlling envelopes every buffer. Faster but only
		 * approximate.
		 */
		COARSE,

		/**
		 * Sample the controlling envelopes every frame. Better for more
		 * accurate work.
		 */
		FINE
	}

	/** The Sample. */
	protected Sample sample;

	/** The position in milliseconds. */
	protected double position;

	/** The position envelope. */
	protected UGen positionEnvelope;

	/** The rate envelope. */
	protected UGen rateEnvelope;

	/**
	 * The millisecond position increment per sample. Calculated from the ratio
	 * of the {@link AudioContext}'s sample rate and the {@link Sample}'s sample
	 * rate.
	 */
	protected double positionIncrement;

	/**
	 * Flag for alternating loop mode to determine if playback is in forward or
	 * reverse phase.
	 */
	protected boolean forwards;

	protected EnvelopeType envelopeType;

	/** The interpolation type. */
	protected InterpolationType interpolationType;

	/** The loop start envelope. */
	protected UGen loopStartEnvelope;

	/** The loop end envelope. */
	protected UGen loopEndEnvelope;

	/** The loop type. */
	protected LoopType loopType;

	/** The loop cross fade in milliseconds. */
	protected float loopCrossFade; // TODO loop crossfade behaviour

	/**
	 * Flag to determine whether playback starts at the beginning of the sample
	 * or at the beginning of the loop.
	 */
	protected boolean startLoop;

	/**
	 * Flag to determine whether the SamplePlayer should kill itself when it
	 * gets to the end of the Sample.
	 */
	protected boolean killOnEnd;

	/** The rate. Calculated and used internally from the rate envelope. */
	protected float rate;

	/**
	 * The loop start. Calculated and used internally from the loop start
	 * envelope.
	 */
	protected float loopStart;

	/** The loop end. Calculated and used internally from the loop end envelope. */
	protected float loopEnd;

	/** Array for temp storage. */
	protected float[] frame;

	/** Bead responding to sample at end (only applies when not in loop mode). */
	private Bead endListener;

	/**
	 * Instantiates a new SamplePlayer with given number of outputs.
	 * 
	 * @param context
	 *            the AudioContext.
	 * @param outs
	 *            the number of outputs.
	 */
	public SamplePlayer(AudioContext context, int outs) {
		super(context, outs);
		rateEnvelope = new Static(context, 1.0f);
		positionEnvelope = null;
		envelopeType = EnvelopeType.FINE;
		interpolationType = InterpolationType.ADAPTIVE;
		loopType = LoopType.NO_LOOP_FORWARDS;
		forwards = true;
		killOnEnd = true;
		loopStartEnvelope = new Static(context, 0.0f);
		loopEndEnvelope = new Static(context, 0.0f);
		positionIncrement = context.samplesToMs(1);
	}

	/**
	 * Instantiates a new SamplePlayer with given Sample. Number of outputs is
	 * determined by number of channels in Sample.
	 * 
	 * @param context
	 *            the AudioContext.
	 * @param buffer
	 *            the Sample.
	 */
	public SamplePlayer(AudioContext context, Sample buffer) {
		this(context, buffer.getNumChannels());
		setSample(buffer);
		loopEndEnvelope.setValue((float) buffer.getLength());
	}

	/**
	 * Sets the Sample.
	 */
	public void setSample(Sample sample) {
		this.sample = sample;
		// sampleRate = sample.getSampleRate();
		frame = new float[sample.getNumChannels()];
	}

	/**
	 * @deprecated
	 */
	public void setBuffer(Sample s) {
		setSample(s);
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
	 * @deprecated
	 */
	public Sample getBuffer() {
		return sample;
	}

	/**
	 * Sets the playback position to the end of the Sample.
	 */
	public void setToEnd() {
		position = sample.getLength();
	}

	/**
	 * Determines whether the playback position is within the loop points.
	 * 
	 * @return true if the playback position is within the loop points.
	 */
	public boolean inLoop() {
		return position < Math.max(loopStart, loopEnd)
				&& position > Math.min(loopStart, loopEnd);
	}

	/**
	 * Sets the playback position to the loop start point.
	 */
	public void setToLoopStart() {
		position = Math.min(loopStart, loopEnd);
		forwards = (rate > 0);
	}

	/**
	 * Starts the sample at the given position.
	 * 
	 * @param msPosition
	 *            the position in milliseconds.
	 */
	public void start(float msPosition) {
		position = msPosition;
		start();
	}

	/**
	 * Resets the position to the start of the Sample.
	 */
	public void reset() {
		position = 0f;
		forwards = true;
	}

	/**
	 * Gets the playback position.
	 * 
	 * @return the position in milliseconds.
	 */
	public double getPosition() {
		return position;
	}

	/**
	 * Sets the playback position. This will not work if the position envelope
	 * is not null.
	 * 
	 * @param position
	 *            the new position in milliseconds.
	 */
	public void setPosition(double position) {
		this.position = position;
	}

	/**
	 * Gets the position envelope.
	 * 
	 * @deprecated Use {@link #getRateUGen()} instead.
	 * 
	 * @return the position envelope.
	 */
	@Deprecated
	public UGen getPositionEnvelope() {
		return positionEnvelope;
	}

	/**
	 * Gets the position UGen.
	 * 
	 * @return the position UGen.
	 */
	public UGen getPositionUGen() {
		return positionEnvelope;
	}

	/**
	 * Sets the position envelope. Setting the position envelope means that the
	 * position is then controlled by this envelope. If the envelope is null the
	 * position continues to be modified by the SamplePlayer's internal playback
	 * or by calls to change the position.
	 * 
	 * @deprecated Use {@link #setPosition(UGen)} instead.
	 * 
	 * @param positionEnvelope
	 *            the new position envelope.
	 */
	@Deprecated
	public void setPositionEnvelope(UGen positionEnvelope) {
		this.positionEnvelope = positionEnvelope;
	}

	/**
	 * Sets the position as a UGen. Setting the position envelope means that the
	 * position is then controlled by this UGen. If the UGen is null the
	 * position continues to be modified by the SamplePlayer's internal playback
	 * or by calls to change the position.
	 * 
	 * @param positionUGen
	 *            the new position UGen.
	 */
	public void setPosition(UGen positionUGen) {
		this.positionEnvelope = positionUGen;
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
		return rateEnvelope;
	}

	/**
	 * Gets the rate UGen.
	 * 
	 * @return the rate UGen.
	 */
	public UGen getRateUGen() {
		return rateEnvelope;
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
		this.rateEnvelope = rateEnvelope;
	}

	/**
	 * Sets the rate to a UGen.
	 * 
	 * @param rateUGen
	 *            the new rate UGen.
	 */
	public void setRate(UGen rateUGen) {
		this.rateEnvelope = rateUGen;
	}

	/**
	 * Gets the rate envelope (this method is provided so that SamplePlayer and
	 * GranularSamplePlayer can be used interchangeably).
	 * 
	 * @deprecated use {@link #getPitchUGen()} instead.
	 * 
	 * @return the rate envelope.
	 */
	@Deprecated
	public UGen getPitchEnvelope() {
		return rateEnvelope;
	}

	/**
	 * Gets the rate UGen (this method is provided so that SamplePlayer and
	 * GranularSamplePlayer can be used interchangeably).
	 * 
	 * @return the rate envelope.
	 */
	public UGen getPitchUGen() {
		return rateEnvelope;
	}

	/**
	 * Sets the rate envelope (this method is provided so that SamplePlayer and
	 * GranularSamplePlayer can be used interchangeably).
	 * 
	 * @deprecated use {@link #setPitch(UGen)} instead.
	 * 
	 * @param rateEnvelope
	 *            the new rate envelope.
	 */
	@Deprecated
	public void setPitchEnvelope(UGen rateEnvelope) {
		this.rateEnvelope = rateEnvelope;
	}

	/**
	 * Sets the rate UGen (this method is provided so that SamplePlayer and
	 * GranularSamplePlayer can be used interchangeably).
	 * 
	 * @param rateUGen
	 *            the new rate UGen.
	 */
	public void setPitch(UGen rateUGen) {
		this.rateEnvelope = rateUGen;
	}

	/**
	 * Gets the {@link EnvelopeType}, either {@link EnvelopeType#COARSE} or
	 * {@link EnvelopeType#FINE}.
	 * 
	 * @return the {@link EnvelopeType}.
	 */
	public EnvelopeType getEnvelopeType() {
		return envelopeType;
	}

	/**
	 * Sets the {@link EnvelopeType}. The value {@link EnvelopeType#COARSE}
	 * means that elements controlled by UGens will not be modified every
	 * sample, but every sample frame. The value {@link EnvelopeType#FINE} means
	 * that values will be updated every sample. Use COARSE to save processing
	 * resources, but use FINE if you want clean and sample accurate control of
	 * values.
	 * 
	 * @param et
	 *            the {@link EnvelopeType}.
	 */
	public void setEnvelopeType(EnvelopeType et) {
		envelopeType = et;
	}

	/**
	 * Gets the {@link InterpolationType} used for accessing samples.
	 * 
	 * @return the interpolation type.
	 */
	public InterpolationType getInterpolationType() {
		return interpolationType;
	}

	/**
	 * Sets the interpolation type.
	 * 
	 * @param interpolationType
	 *            the new interpolation type.
	 */
	public void setInterpolationType(InterpolationType interpolationType) {
		this.interpolationType = interpolationType;
	}

	/**
	 * Gets the loop cross fade.
	 * 
	 * @return the loop cross fade in milliseconds.
	 */
	public float getLoopCrossFade() {
		return loopCrossFade;
	}

	/**
	 * Sets the loop cross fade.
	 * 
	 * @param loopCrossFade
	 *            the new loop cross fade in milliseconds.
	 */
	public void setLoopCrossFade(float loopCrossFade) {
		this.loopCrossFade = loopCrossFade;
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
		return loopEndEnvelope;
	}

	/**
	 * Gets the loop end UGen.
	 * 
	 * @return the loop end UGen.
	 */
	public UGen getLoopEndUGen() {
		return loopEndEnvelope;
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
		this.loopEndEnvelope = loopEndEnvelope;
	}

	/**
	 * Sets the loop end UGen.
	 * 
	 * @param loopEndUGen
	 *            the new loop end UGen.
	 */
	public void setLoopEnd(UGen loopEndUGen) {
		this.loopEndEnvelope = loopEndUGen;
	}

	/**
	 * Gets the loop start envelope.
	 * 
	 * @deprecated Use {@link #getLoopStartUGen()} instead.
	 * @return the loop start envelope
	 */
	@Deprecated
	public UGen getLoopStartEnvelope() {
		return loopStartEnvelope;
	}

	/**
	 * Gets the loop start UGen.
	 * 
	 * @return the loop start UGen
	 */
	public UGen getLoopStartUGen() {
		return loopStartEnvelope;
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
		this.loopStartEnvelope = loopStartEnvelope;
	}

	/**
	 * Sets the loop start UGen.
	 * 
	 * @param loopStartUGen
	 *            the new loop start UGen.
	 */
	public void setLoopStart(UGen loopStartUGen) {
		this.loopStartEnvelope = loopStartUGen;
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
		loopStartEnvelope = new Static(context, start
				* (float) sample.getLength());
		loopEndEnvelope = new Static(context, end * (float) sample.getLength());
	}

	/**
	 * Gets the loop type.
	 * 
	 * @return the loop type.
	 */
	public LoopType getLoopType() {
		return loopType;
	}

	/**
	 * Sets the loop type.
	 * 
	 * @param loopType
	 *            the new loop type.
	 */
	public void setLoopType(LoopType loopType) {
		this.loopType = loopType;
		if (loopType != LoopType.LOOP_ALTERNATING) {
			if (loopType == LoopType.LOOP_FORWARDS
					|| loopType == LoopType.NO_LOOP_FORWARDS) {
				forwards = true;
			} else {
				forwards = false;
			}
		}
	}

	/**
	 * Gets the sample rate.
	 * 
	 * @return the sample rate, in samples per second.
	 */
	public float getSampleRate() {
		return sample.getSampleRate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.olliebown.beads.core.UGen#calculateBuffer()
	 */
	@Override
	public void calculateBuffer() {
		if (sample != null) {
			if (positionEnvelope != null) {
				positionEnvelope.update();
			} else {
				// major speed up possible here if these envelopes are all
				// either null or paused
				// (can we pause Envelope when it is not doing anything?).
				// if this holds true we can tell buffer to just grab the whole
				// frame at the given rate
				// and then update the position all at once.
				rateEnvelope.update();
				loopStartEnvelope.update();
				loopEndEnvelope.update();
			}

			// depending on the envelope type, we either copy whole chunks of
			// data (COARSE), or step per frame (FINE)
			if (envelopeType == EnvelopeType.COARSE) {
				// from the envelopes (if they exist)
				// compute the position and rate
				// and number of frames to get from Sample

				if (positionEnvelope != null) {
					// use the first and last values in the current envelope
					// and provide a linear interpolation between them
					float startPosition = positionEnvelope.getValue(0, 0);
					float endPosition = positionEnvelope.getValue(0,
							bufferSize - 1);

					long startPosInSamples = (long) (sample
							.msToSamples((float) startPosition));
					long endPosInSamples = (long) (sample
							.msToSamples((float) endPosition));

					long numSamples = 1 + Math.abs(endPosInSamples
							- startPosInSamples);

					if (endPosInSamples >= startPosInSamples) {
						float[][] samples = new float[getOuts()][(int) numSamples];
						sample.getFrames((int) startPosInSamples, samples);
						AudioUtils.stretchBuffer(samples, bufOut);
					} else // endPosInSamples < startPosInSamples (i.e., rate is
							// negative)
					{
						float[][] samples = new float[getOuts()][(int) numSamples];
						sample.getFrames((int) endPosInSamples, samples);
						AudioUtils.reverseBuffer(samples);
						AudioUtils.stretchBuffer(samples, bufOut);
					}

					position = endPosition;
				} else // use the position variable and rate envelopes
				{
					// coarsely sample the rate envelope
					rate = rateEnvelope.getValue(0, 0);

					switch (loopType) {
					case NO_LOOP_FORWARDS:
					case NO_LOOP_BACKWARDS: {
						double normalisedRate = (loopType == LoopType.NO_LOOP_FORWARDS) ? rate
								: -rate;
						long numSamples = (long) (Math.abs(rate) * bufferSize);
						double numMs = sample.samplesToMs(numSamples);

						boolean isPlayingForwards;
						if (normalisedRate >= 0) // we are playing forwards
						{
							isPlayingForwards = true;
							if (numMs + position > sample.getLength()) {
								numSamples = (long) sample.msToSamples(sample
										.getLength() - position);
							}
						} else // playing backwards
						{
							isPlayingForwards = false;
							if (position - numMs < 0) {
								numSamples = (long) sample
										.msToSamples(position);
							}
						}

						if (numSamples <= 0)
							return;
						float[][] frames = new float[outs][(int) numSamples];

						if (isPlayingForwards) {
							sample.getFrames(
									(int) sample.msToSamples(position), frames);
							position += numMs;
						} else {
							sample.getFrames(
									(int) (sample.msToSamples(position) - numSamples),
									frames);
							AudioUtils.reverseBuffer(frames);
							position -= numMs;
						}

						AudioUtils.stretchBuffer(frames, bufOut);

						if (position > sample.getLength() || position < 0)
							atEnd();
						break;
					}
					default: {
						System.out
								.println("COARSE looping is not implemented yet. Killing SamplePlayer...");
						kill();
					}
					}

				}
			} else // envelopeType==EnvelopeType.FINE
			{
				for (int i = 0; i < bufferSize; i++) {
					// calculate the samples
					switch (interpolationType) {
					case ADAPTIVE:
						if (rate > ADAPTIVE_INTERP_HIGH_THRESH) {
							sample.getFrameNoInterp(position, frame);
						} else if (rate > ADAPTIVE_INTERP_LOW_THRESH) {
							sample.getFrameLinear(position, frame);
						} else {
							sample.getFrameCubic(position, frame);
						}
						break;
					case LINEAR:
						sample.getFrameLinear(position, frame);
						break;
					case CUBIC:
						sample.getFrameCubic(position, frame);
						break;
					case NONE:
						sample.getFrameNoInterp(position, frame);
						break;
					}
					for (int j = 0; j < outs; j++) {
						bufOut[j][i] = frame[j % sample.getNumChannels()];
					}
					// update the position, loop state, direction
					calculateNextPosition(i);
				}
			}
		}
	}

	/**
	 * Sets/unsets option for SamplePlayer to kill itself when it reaches the
	 * end of the Sample it is playing. True by default.
	 * 
	 * @param killOnEnd
	 *            true to kill on end.
	 */
	public void setKillOnEnd(boolean killOnEnd) {
		this.killOnEnd = killOnEnd;
	}

	/**
	 * Determines whether this SamplePlayer will kill itself at the end of the
	 * Sample it is playing.
	 * 
	 * @return true of SamplePlayer will kill itself at the end of the Sample it
	 *         is playing.
	 */
	public boolean getKillOnEnd() {
		return (killOnEnd);
	}

	/**
	 * Called when at the end of the Sample, assuming the loop mode is
	 * non-looping, or beginning, if the SamplePlayer is playing backwards..
	 */
	private void atEnd() {
		if (endListener != null) {
			endListener.message(this);
		}
		if (killOnEnd) {
			kill();
		}
	}

	/**
	 * Sets a {@link Bead} that will be triggered when this SamplePlayer gets to
	 * the end. This occurs when the SamplePlayer's position reaches then end
	 * when playing forwards in a non-looping mode, or reaches the the beginning
	 * when playing backwards in a non-looping mode. It is never triggered in a
	 * looping mode. As an alternative, you can use the method {@link
	 * Bead.#setKillListener(Bead)} as long as {@link #setKillOnEnd(boolean)} is
	 * set to true. In other words, you set this SamplePlayer to kill itself
	 * when it reaches the end of the sample, and then use the functionality of
	 * {@link Bead}, which allows you to create a trigger whenever a Bead is
	 * killed. Set to null to remove the current listener.
	 * 
	 * @param endListener
	 *            the {@link Bead} that responds to this SamplePlayer reaching
	 *            its end.
	 */
	public void setEndListener(Bead endListener) {
		this.endListener = endListener;
	}

	/**
	 * Gets the current endListener.
	 * 
	 * @see {#setEndListener(Bead)}.
	 * @return the current endListener.
	 */
	public Bead getEndListener() {
		return endListener;
	}

	/**
	 * Re trigger the SamplePlayer from the beginning.
	 */
	public void reTrigger() {
		reset();
		this.pause(false);
	}

	/**
	 * Used at each sample in the perform routine to determine the next playback
	 * position.
	 * 
	 * @param i
	 *            the index within the buffer loop.
	 */
	protected void calculateNextPosition(int i) {
		if (positionEnvelope != null) {
			position = positionEnvelope.getValueDouble(0, i);
		} else {
			rate = rateEnvelope.getValue(0, i);
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
				loopStart = loopStartEnvelope.getValue(0, i);
				loopEnd = loopEndEnvelope.getValue(0, i);
				position += positionIncrement * rate;
				if (rate > 0 && position > Math.max(loopStart, loopEnd)) {
					position = Math.min(loopStart, loopEnd);
				} else if (rate < 0 && position < Math.min(loopStart, loopEnd)) {
					position = Math.max(loopStart, loopEnd);
				}
				break;
			case LOOP_BACKWARDS:
				loopStart = loopStartEnvelope.getValue(0, i);
				loopEnd = loopEndEnvelope.getValue(0, i);
				position -= positionIncrement * rate;
				if (rate > 0 && position < Math.min(loopStart, loopEnd)) {
					position = Math.max(loopStart, loopEnd);
				} else if (rate < 0 && position > Math.max(loopStart, loopEnd)) {
					position = Math.min(loopStart, loopEnd);
				}
				break;
			case LOOP_ALTERNATING:
				loopStart = loopStartEnvelope.getValue(0, i);
				loopEnd = loopEndEnvelope.getValue(0, i);
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

}
