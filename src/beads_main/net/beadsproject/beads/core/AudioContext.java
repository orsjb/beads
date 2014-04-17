/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 * CREDIT: This class uses portions of code taken from JASS. See readme/CREDITS.txt.
 * 
 */
package net.beadsproject.beads.core;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.beadsproject.beads.core.io.NonrealtimeIO;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.events.AudioContextStopTrigger;
import net.beadsproject.beads.ugens.DelayTrigger;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.RecordToSample;

/**
 * AudioContext provides the core audio set up for running audio in a Beads
 * project. An AudioContext determines the JavaSound {@link IOAudioFormat} used,
 * the IO device, the audio buffer size and the system IO buffer size. An
 * AudioContext also provides a {@link UGen} called {@link #out}, which is
 * the output point for networks of UGens in a Beads project.
 * 
 * @beads.category control
 * @author ollie
 */
public class AudioContext {

	public static final int DEFAULT_BUFFER_SIZE = 512;

	/** The audio IO device. */
	private AudioIO audioIO;

	/** The Beads audio format. */
	private IOAudioFormat audioFormat;

	/** The stop flag. */
	private boolean stopped;

	/** The root {@link UGen}. */
	public final Gain out;

	/** The current time step. */
	private long timeStep;

	/** Flag for logging time to System.out. */
	private boolean logTime;

	/** The buffer size in frames. */
	private int bufferSizeInFrames;

	/** Used for allocating buffers to UGens. */
	private int maxReserveBufs;
	private ArrayList<float[]> bufferStore;
	private int bufStoreIndex;
	private float[] zeroBuf;

	/** Used for testing for dropped frames. */
	@SuppressWarnings("unused")
	private long nanoLeap;
	@SuppressWarnings("unused")
	private boolean lastFrameGood;

	/** Used for concurrency-friendly method execution. */
	private final ConcurrentLinkedQueue<Bead> beforeFrameQueue = new ConcurrentLinkedQueue<Bead>();
	private final ConcurrentLinkedQueue<Bead> afterFrameQueue = new ConcurrentLinkedQueue<Bead>();
	private final ConcurrentLinkedQueue<Bead> beforeEveryFrameList = new ConcurrentLinkedQueue<Bead>();
	private final ConcurrentLinkedQueue<Bead> afterEveryFrameList = new ConcurrentLinkedQueue<Bead>();
	
	/**
	 * This constructor creates the default AudioContext, which means net.beadsproject.beads.core.io.JavaSoundAudioIO if it can find it, or net.beadsproject.beads.core.io.NonrealtimeIO otherwise.
	 * To get the former, link to the jaudiolibs-beads.jar file.
	 * 
	 * The libraries are decoupled like this so that the core beads library doesn't depend on JavaSound, which is not supported in various contexts, such as Android. At the moment there are in fact some
	 * JavaSound dependencies still to be removed before this process is complete. Pro-users should familiarise themselves with the different IO options, particularly Jack.
	 */
	public AudioContext() {
		this(DEFAULT_BUFFER_SIZE);
	}

	/**
	 * This constructor creates the default AudioContext, which means net.beadsproject.beads.core.io.JavaSoundAudioIO if it can find it, or net.beadsproject.beads.core.io.NonrealtimeIO otherwise.
	 * To get the former, link to the jaudiolibs-beads.jar file.
	 * 
	 * The libraries are decoupled like this so that the core beads library doesn't depend on JavaSound, which is not supported in various contexts, such as Android. At the moment there are in fact some
	 * JavaSound dependencies still to be removed before this process is complete. Pro-users should familiarise themselves with the different IO options, particularly Jack.
	 * 
	 * @param bufferSize the number of samples calculated during one frame of audio processing. Higher numbers mean more latency and more stability. Typically use powers of 2. Default is 512.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public AudioContext(int bufferSize) {
		//attempt to find the default (JavaSound) AudioIO by reflection
		AudioIO ioSystem = null;
		try {
			Class javaSoundAudioIOClass = Class.forName("net.beadsproject.beads.core.io.JavaSoundAudioIO");		//alt choice is org.jaudiolibs.beads.AudioServerIO$JavaSound.
			Constructor noArgsConstructor = javaSoundAudioIOClass.getConstructor();
			ioSystem = (AudioIO)noArgsConstructor.newInstance();
			System.out.println("AudioContext : no AudioIO specified, using default => " + javaSoundAudioIOClass.getName() + ".");
		} catch (Exception e) {
			//if fail, print warning and revert to NonrealtimeIO.
			System.out.println("AudioContext : warning : unable to find default (JavaSound) AudioIO.");
			System.out.println("AudioContext : warning : reverting to NonrealtimeIO. You can still process audio but don't expect to hear anything.");
			ioSystem = new NonrealtimeIO();
			}
		//default audio format
		IOAudioFormat audioFormat = defaultAudioFormat(2, 2);
		// bind to AudioIO
		this.audioIO = ioSystem;
		this.audioIO.context = this;
		// set audio format
		this.audioFormat = audioFormat;
		// set buffer size
		setBufferSize(bufferSize);
		// set up basic stuff
		logTime = false;
		maxReserveBufs = 50;
		stopped = true;
		// set up the default root UGen
		out = new Gain(this, audioFormat.outputs);
		this.audioIO.prepare();
		
		
	}
	
	/**
	 * Creates a new AudioContext with default audio format and 
	 * buffer size and the specified {@link AudioIO}. The default audio format is 44.1Khz,
	 * 16 bit, stereo, signed, bigEndian.
	 * 
	 * @param ioSystem the AudioIO system.
	 */
	public AudioContext(AudioIO ioSystem) {
		this(ioSystem, DEFAULT_BUFFER_SIZE, defaultAudioFormat(2, 2));
	}
	
	/**
	 * Creates a new AudioContext with default audio format and the specified
	 * buffer size and {@link AudioIO}. The default audio format is 44.1Khz,
	 * 16 bit, stereo, signed, bigEndian.
	 * 
	 * @param bufferSizeInFrames
	 *            the buffer size in samples.
	 * @param ioSystem the AudioIO system.
	 */
	public AudioContext(AudioIO ioSystem, int bufferSizeInFrames) {
		// use almost entirely default settings
		this(ioSystem, bufferSizeInFrames, defaultAudioFormat(2, 2));
	}

	/**
	 * Creates a new AudioContext with the specified buffer size, AudioIO and audio format.
	 * 
	 * @param bufferSizeInFrames
	 *            the buffer size in samples.
	 * @param ioSystem the AudioIO system.
	 * @param audioFormat
	 *            the audio format, which specifies sample rate, bit depth,
	 *            number of channels, signedness and byte order.
	 */
	public AudioContext(AudioIO ioSystem, int bufferSizeInFrames, IOAudioFormat audioFormat) {
		// bind to AudioIO
		this.audioIO = ioSystem;
		this.audioIO.context = this;
		// set audio format
		this.audioFormat = audioFormat;
		// set buffer size
		setBufferSize(bufferSizeInFrames);
		// set up basic stuff
		logTime = false;
		maxReserveBufs = 50;
		stopped = true;
		// set up the default root UGen
		out = new Gain(this, audioFormat.outputs);
		this.audioIO.prepare();
	}

	/**
	 * Returns a UGen which can be used to grab audio from the audio input, as
	 * specified by the AudioIO.
	 * 
	 * @param channels
	 *            an array of ints indicating which channels are required.
	 * @return a UGen which can be used to access audio input.
	 */
	public UGen getAudioInput(int[] channels) {
		return audioIO.getAudioInput(channels);
	}

	/**
	 * Returns a UGen which can be used to grab audio from the audio input, as
	 * specified by the AudioIO. This method returns a UGen with one out for
	 * each input channel of the audio input device. For access to specific channels
	 * see {@link #getAudioInput(int[])}.
	 * 
	 * @return a UGen which can be used to access audio input.
	 */
	public UGen getAudioInput() {
		int[] chans = new int[audioFormat.inputs];
		for(int i = 0; i < chans.length; i++) {
			chans[i] = i + 1;
		}
		return audioIO.getAudioInput(chans);
	}

	/**
	 * Sets up the reserve of buffers.
	 */
	private void setupBufs() {
		bufferStore = new ArrayList<float[]>();
		while (bufferStore.size() < maxReserveBufs) {
			bufferStore.add(new float[bufferSizeInFrames]);
		}
		zeroBuf = new float[bufferSizeInFrames];
	}

	/** callback from AudioIO. */
	protected void update() {
		try {
			bufStoreIndex = 0;
			Arrays.fill(zeroBuf, 0f);
			sendBeforeFrameMessages();
			out.update(); // this will propagate all of the updates
			sendAfterFrameMessages();
			timeStep++;
			if (Thread.interrupted()) {
				System.out.println("Thread interrupted");
			}
			if (logTime && timeStep % 100 == 0) {
				System.out.println(samplesToMs(timeStep * bufferSizeInFrames)
						/ 1000f + " (seconds)");
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets a buffer from the buffer reserve. This buffer will be owned by you
	 * until the next time step, and you shouldn't attempt to use it outside of
	 * the current time step. The length of the buffer is bufferSize, but there
	 * is no guarantee as to its contents.
	 * 
	 * @return buffer of size bufSize, unknown contents.
	 */
	public float[] getBuf() {
		if (bufStoreIndex < bufferStore.size()) {
			return bufferStore.get(bufStoreIndex++);
		} else {
			float[] buf = new float[bufferSizeInFrames];
			bufferStore.add(buf);
			bufStoreIndex++;
			return buf;
		}
	}

	/**
	 * Gets a zero initialised buffer from the buffer reserve. This buffer will
	 * be owned by you until the next time step, and you shouldn't attempt to
	 * use it outside of the current time step. The length of the buffer is
	 * bufferSize, and the buffer is full of zeros.
	 * 
	 * @return buffer of size bufSize, all zeros.
	 */
	public float[] getCleanBuf() {
		float[] buf = getBuf();
		Arrays.fill(buf, 0f);
		return buf;
	}

	/**
	 * Gets a pointer to a buffer of length bufferSize, full of zeros. Changing
	 * the contents of this buffer would be completely disastrous. If you want a
	 * buffer of zeros that you can actually do something with, use {@link
	 * #getCleanBuf()}.
	 * 
	 * @return buffer of size bufSize, all zeros.
	 */
	public float[] getZeroBuf() {
		return zeroBuf;
	}

	/**
	 * Starts the AudioContext running in non-realtime. This occurs in the
	 * current Thread.
	 */
	public void runNonRealTime() {
		if (stopped) {
			stopped = false;
			reset();
			while (out != null && !stopped) {
				bufStoreIndex = 0;
				Arrays.fill(zeroBuf, 0f);
				if (!out.isPaused())
					out.update();
				timeStep++;
				if (logTime && timeStep % 100 == 0) {
					System.out.println(samplesToMs(timeStep
							* bufferSizeInFrames)
							/ 1000f + " (seconds)");
				}
			}
		}
	}

	/**
	 * Runs the AudioContext in non-realtime for n milliseconds (that's n
	 * non-realtime milliseconds).
	 * 
	 * @param n
	 *            number of milliseconds.
	 */
	public void runForNMillisecondsNonRealTime(double n) {
		// time the playback to n seconds
		DelayTrigger dt = new DelayTrigger(this, n,
				new AudioContextStopTrigger(this));
		out.addDependent(dt);
		runNonRealTime();
	}

	/**
	 * Sets the buffer size.
	 * 
	 * @param bufferSize
	 *            the new buffer size.
	 */
	private void setBufferSize(int bufferSize) {
		bufferSizeInFrames = bufferSize;
		setupBufs();
	}

	/**
	 * Gets the buffer size for this AudioContext.
	 * 
	 * @return Buffer size in samples.
	 */
	public int getBufferSize() {
		return bufferSizeInFrames;
	}

	/**
	 * Gets the sample rate for this AudioContext.
	 * 
	 * @return sample rate in samples per second.
	 */
	public float getSampleRate() {
		return audioFormat.sampleRate;
	}

	/**
	 * Gets the AudioFormat for this AudioContext.
	 * 
	 * @return AudioFormat used by this AudioContext.
	 */
	public IOAudioFormat getAudioFormat() {
		return audioFormat;
	}
	
	/**
	 * Generates a new AudioFormat with the same everything as the
	 * AudioContext's AudioFormat except for the number of channels.
	 * 
	 * @param numChannels
	 *            the number of channels.
	 * @return a new AudioFormat with the given number of channels, all other
	 *         properties coming from the original AudioFormat.
	 */
	public IOAudioFormat getAudioFormat(int inputs, int outputs) {
		IOAudioFormat newFormat = new IOAudioFormat(audioFormat.sampleRate, audioFormat.bitDepth, inputs, outputs);
		return newFormat;
	}

	/**
	 * Generates the default {@link IOAudioFormat} for AudioContext, with the
	 * given number of channels. The default values are: sampleRate=44100,
	 * sampleSizeInBits=16, signed=true, bigEndian=true.
	 * 
	 * @param numChannels
	 *            the number of channels to use.
	 * @return the generated AudioFormat.
	 */
	public static IOAudioFormat defaultAudioFormat(int inputs, int outputs) {
		return new IOAudioFormat(44100, 16, inputs, outputs, true, true);
	}

	/**
	 * Prints AudioFormat information to System.out.
	 */
	public void postAudioFormatInfo() {
		System.out.println("Sample Rate: " + audioFormat.sampleRate);
		System.out.println("Inputs: " + audioFormat.inputs);
		System.out.println("Outputs: " + audioFormat.outputs);
		System.out.println("Bit Depth: " + audioFormat.bitDepth);
		System.out.println("Big Endian: " + audioFormat.bigEndian);
		System.out.println("Signed: " + audioFormat.signed);
	}

	/**
	 * Prints a representation of the audio signal chain stemming upwards from
	 * the specified UGen to System.out, indented by the specified depth.
	 * 
	 * @param current
	 *            UGen to start from.
	 * @param depth
	 *            depth by which to indent.
	 */
	public static void printCallChain(UGen current, int depth) {
		Set<UGen> children = current.getConnectedInputs();
		for (int i = 0; i < depth; i++) {
			System.out.print("  ");
		}
		System.out.println("- " + current);
		for (UGen child : children) {
			printCallChain(child, depth + 1);
		}
	}

	/**
	 * Prints the entire call chain to System.out (equivalent to
	 * AudioContext.printCallChain(this.out, 0);)
	 */
	public void printCallChain() {
		AudioContext.printCallChain(out, 0);
	}

	/**
	 * Converts samples to milliseconds at the current sample rate.
	 * 
	 * @param msTime
	 *            duration in milliseconds.
	 * 
	 * @return number of samples.
	 */
	public double msToSamples(double msTime) {
		return msTime * (audioFormat.sampleRate / 1000.0);
	}

	/**
	 * Converts milliseconds to samples at the current sample rate.
	 * 
	 * @param sampleTime
	 *            number of samples.
	 * 
	 * @return duration in milliseconds.
	 */
	public double samplesToMs(double sampleTime) {
		return (sampleTime / audioFormat.sampleRate) * 1000.0;
	}

	/**
	 * Gets the current time step of this AudioContext. The time step begins at
	 * zero when the AudioContext is started and is incremented by 1 for each
	 * update of the audio buffer.
	 * 
	 * @return current time step.
	 */
	public long getTimeStep() {
		return timeStep;
	}

	/**
	 * Generates a TimeStamp with the current time step and the given index into
	 * the time step.
	 * 
	 * @param index
	 *            the index into the current time step.
	 * @return a TimeStamp.
	 */
	public TimeStamp generateTimeStamp(int index) {
		return new TimeStamp(this, timeStep, index);
	}

	/**
	 * Get the runtime (in ms) since starting.
	 */
	public double getTime() {
		return samplesToMs(getTimeStep() * getBufferSize());
	}

	/**
	 * Switch on/off logging of time when running in realtime. The time is
	 * printed to System.out every 100 time steps.
	 * 
	 * @param logTime
	 *            set true to log time.
	 */
	public void logTime(boolean logTime) {
		this.logTime = logTime;
	}

	/**
	 * Tells the AudioContext to record all output for the given millisecond
	 * duration, kill the AudioContext, and save the recording to the given file
	 * path. This is a convenient way to make quick recordings, but may not suit
	 * every circumstance.
	 * 
	 * @param timeMS
	 *            the time in milliseconds to record for.
	 * @param filename
	 *            the filename to save the recording to.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * 
	 * @see RecordToSample recorder
	 * @see Sample sample
	 **/
	public void record(double timeMS, String filename) throws Exception {
		Sample s = new Sample(timeMS, audioFormat.outputs, audioFormat.sampleRate); 
		RecordToSample r;
		try {
			r = new RecordToSample(this, s);
			r.addInput(out);
			out.addDependent(r);
			r.start();
			r.setKillListener(new AudioContextStopTrigger(this));
		} catch (Exception e) { /* won't happen */
		}
		while (isRunning()) {
		}
		s.write(filename);
	}

	/**
	 * Convenience method to quickly audition a {@link UGen}.
	 * 
	 * @param ugen
	 *            the {@link UGen} to audition.
	 */
	public void quickie(UGen ugen) {
		out.addInput(ugen);
		start();
	}

	/**
	 * Starts the AudioContext running in realtime. Only happens if not already
	 * running. Resets time.
	 */
	public void start() {
		if (stopped) {
			// calibration test stuff
			nanoLeap = (long) (1000000000 * ((float) bufferSizeInFrames / audioFormat.sampleRate));
			lastFrameGood = true;
			// reset time step
			reset();
			stopped = false;
			// the AudioIO is where the thread actually runs.
			audioIO.start();
		}
	}
	
	/**
	 * Simply resets the timeStep to zero.
	 */
	public void reset() {
		timeStep = 0;
	}

	/**
	 * Stops the AudioContext if running either in realtime or non-realtime.
	 */
	public void stop() {
		stopped = true;
		audioIO.stop();
	}

	/**
	 * Checks if this AudioContext is running.
	 * 
	 * @return true if running.
	 */
	public boolean isRunning() {
		return !stopped;
	}

	/**
	 * @return The AudioIO used by this context.
	 */
	public AudioIO getAudioIO() {
		return audioIO;
	}

	/**
	 * Queues the specified Bead to be messaged upon the next audio frame
	 * completion. The Bead will be messaged only once.
	 * 
	 * @param target
	 *            The Bead to message.
	 * @return This AudioContext.
	 */
	public AudioContext invokeAfterFrame(Bead target) {
		afterFrameQueue.offer(target);
		return this;
	}

	/**
	 * Queues the specified Bead to be messaged after every audio frame.
	 * 
	 * @param target
	 *            The Bead to message.
	 * @return This AudioContext.
	 */
	public AudioContext invokeAfterEveryFrame(Bead target) {
		afterEveryFrameList.offer(target);
		return this;
	}

	/**
	 * Removes the specified Bead from the list of Beads that are messaged after
	 * every audio frame.
	 * 
	 * @param target
	 *            The Bead to stop messaging.
	 * @return Whether the Bead was being messaged.
	 */
	public boolean stopInvokingAfterEveryFrame(Bead target) {
		return afterEveryFrameList.remove(target);
	}

	/**
	 * Queues the specified bead to be messaged before the next audio frame. The
	 * Bead will be messaged only once.
	 * 
	 * @param target
	 *            The Bead to message.
	 * @return This AudioContext.
	 */
	public AudioContext invokeBeforeFrame(Bead target) {
		beforeFrameQueue.add(target);
		return this;
	}

	/**
	 * Queues the specified Bead to be messaged before every audio frame.
	 * 
	 * @param target
	 *            The Bead to message.
	 * @return This AudioContext.
	 */
	public AudioContext invokeBeforeEveryFrame(Bead target) {
		beforeEveryFrameList.offer(target);
		return this;
	}

	/**
	 * Removes the specified Bead from the list of Beads that are messaged
	 * before every audio frame.
	 * 
	 * @param target
	 *            The Bead to stop messaging.
	 * @return Whether the Bead was being messaged.
	 */
	public boolean stopInvokingBeforeEveryFrame(Bead target) {
		return beforeEveryFrameList.remove(target);
	}

	/**
	 * Used to send messages before the audio frame is done.
	 */
	private void sendBeforeFrameMessages() {
		Bead target;
		while((target = beforeFrameQueue.poll()) != null) {
			target.message(null);
		}
		for (Bead bead: beforeEveryFrameList) {
			bead.message(null);
		}
	}

	/**
	 * Used to send messages after the audio frame is done.
	 */
	private void sendAfterFrameMessages() {
		Bead target;
		while ((target = afterFrameQueue.poll()) != null) {
			target.message(null);
		}
		for (Bead bead : afterEveryFrameList) {
			bead.message(null);
		}
	}
}
