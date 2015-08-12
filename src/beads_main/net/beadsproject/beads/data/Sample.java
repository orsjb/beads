/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data;

import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.UnsupportedAudioFileException;

import net.beadsproject.beads.data.audiofile.AudioFileReader;
import net.beadsproject.beads.data.audiofile.AudioFileType;
import net.beadsproject.beads.data.audiofile.AudioFileWriter;

/**
 * A Sample encapsulates audio data, either loaded from an audio file (such as
 * an MP3) or written by a Recorder. <br />
 * The typical use of a Sample is through
 * {@link net.beadsproject.beads.data.SampleManager}. For example, to load an
 * mp3, you would do the following. <br />
 * <br />
 * <code>
 * Sample wicked = SampleManager.sample("wickedTrack.mp3");	
 * </code> <br />
 * <br />
 * 
 * <p>
 * Samples are usually played with a
 * {@link net.beadsproject.beads.ugens.SamplePlayer}. Sample data can also be
 * accessed through the methods: {@link #getFrame(int, float[]) getFrame},
 * {@link #getFrameLinear(double, float[]) getFrameLinear}, and
 * {@link #getFrames(int, float[][]) getFrames}. Sample data can be written
 * with: {@link #putFrame(int, float[]) putFrame} or
 * {@link #putFrames(int, float[][]) putFrames}.
 * 
 * @beads.category data
 * @see SampleManager
 * @see net.beadsproject.beads.ugens.RecordToSample
 * @author Beads Team
 */
@SuppressWarnings("unchecked")
public class Sample {

	private float sampleRate;
	private int nChannels;
	private long nFrames;
	private String simpleName;
	private String filename = null;
	private float[][] theSampleData; // theSampleData[0] first channel, theSampleData[1] second channel, etc..
	private float[] current, next;   // used as temp buffers whilst calculating interpolation

	// These are the classes that handle audio file IO
	private Class<? extends AudioFileReader> audioFileReaderClass = null;
	private Class<? extends AudioFileWriter> audioFileWriterClass = null;
	private static Class<? extends AudioFileReader> defaultAudioFileReaderClass;
	private static Class<? extends AudioFileWriter> defaultAudioFileWriterClass;
	
	/*
	 * Try to set the defaultAudioFileReaderClass to JavaSoundAudioFile if available, and if not, use WavFileReaderWriter which
	 * should always be available in beads_main.
	 */
	static {
		try {
			defaultAudioFileReaderClass = (Class<? extends AudioFileReader>) Class.forName("net.beadsproject.beads.data.audiofile.JavaSoundAudioFile");
		} catch (ClassNotFoundException e) {
			try {
				defaultAudioFileReaderClass = (Class<? extends AudioFileReader>) Class.forName("net.beadsproject.beads.data.audiofile.WavFileReaderWriter");
			} catch (ClassNotFoundException e2) {
				defaultAudioFileReaderClass = null;
			}
		}
	}
	
	/*
	 * Set the defaultAudioFileWriterClass to WavFileReaderWriter which should always be available in beads_main.
	 */
	static {
		try {
			defaultAudioFileWriterClass = (Class<? extends AudioFileWriter>) Class.forName("net.beadsproject.beads.data.audiofile.JavaSoundAudioFile");
		} catch (ClassNotFoundException e) {
			try {
				defaultAudioFileWriterClass = (Class<? extends AudioFileWriter>) Class.forName("net.beadsproject.beads.data.audiofile.WavFileReaderWriter");
			} catch (ClassNotFoundException e2) {
				defaultAudioFileReaderClass = null;
			}
		}
	}
	
	/**
	 * Instantiates a new writable sample with specified length and default
	 * audio format: 44.1KHz, float, stereo.
	 * 
	 * @param length
	 *            the length in ms.
	 */
	public Sample(double length) {
		this(length, 2, 44100f);
	}
	
	
	/**
	 * Instantiates a new writable sample with specified length and number of channels and default
	 * audio format: 44.1KHz, float.
	 * 
	 * @param length
	 *            the length in ms.
	 *            
	 * @param nChannels
	 * 			  the number of channels.
	 */
	public Sample(double length, int nChannels) {
		this(length, nChannels, 44100f);
	}

	/**
	 * Instantiates a new writeable Sample with the specified audio format and
	 * length;
	 * 
	 * The sample isn't initialised, so may contain junk. Use {@link #clear()}
	 * to clear it.
	 * 
	 * @param length
	 *            The length of the sample in ms.
	 * @param nChannels
	 * 			  The number of channels
	 * @param sampleRate
	 * 			  The sampleRate
	 */
	public Sample(double length, int nChannels, float sampleRate) {
		this.nChannels = nChannels;
		this.sampleRate = sampleRate;
		current = new float[nChannels];
		next = new float[nChannels];
		nFrames = (long) msToSamples(length);
		theSampleData = new float[nChannels][(int) nFrames];
		length = 1000f * nFrames / this.sampleRate;
	}

	/**
	 * Create a sample from a file. This constructor immediately loads the
	 * entire audio file into memory.
	 * 
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	public Sample(String filename) throws IOException {
		loadAudioFile(filename);
		this.filename = filename;
	}

	/**
	 * Gets the current AudioFileReaderClass. This is an instantiation of
	 * a class that implements {@link AudioFileReader} that Sample will use for file reading operations. 
	 * If unset or set to null Sample will use a default.
	 * 
	 * @return
	 */
	public Class<? extends AudioFileReader> getAudioFileReaderClass() {
		return audioFileReaderClass;
	}
	
	/**
	 * Gets the current AudioFileWriterClass. This is an instantiation of
	 * a class that implements {@link AudioFileWriter} that Sample will use for file writing operations. 
	 * If unset or set to null Sample will use a default.
	 * 
	 * @return
	 */
	public Class<? extends AudioFileWriter> getAudioFileWriterClass() {
		return audioFileWriterClass;
	}

	/**
	 * Set the audioFileReaderClass. This is an instantiation of
	 * a class that implements {@link AudioFileReader} that Sample will use for file reading operations. 
	 * If unset or set to null Sample use a default.
	 * 
	 * @param audioFileReaderClass
	 */
	public void setAudioFileReaderClass(Class<? extends AudioFileReader> audioFileReaderClass) {
		this.audioFileReaderClass = audioFileReaderClass;
	}
	
	/**
	 * Set the audioFileWriterClass. This is an instantiation of
	 * a class that implements {@link AudioFileWriter} that Sample will use for file writing operations. 
	 * If unset or set to null Sample use a default.
	 * 
	 * @param audioFileWriterClass
	 */
	public void setAudioFileWriterClass(Class<? extends AudioFileWriter> audioFileWriterClass) {
		this.audioFileWriterClass = audioFileWriterClass;
	}

	/**
	 * Return a single frame.
	 * 
	 * If the data is not readily available this doesn't do anything to
	 * frameData.
	 * 
	 * @param frame
	 *            Must be in range, else framedata is unchanged.
	 * @param frameData
	 * 
	 */
	public void getFrame(int frame, float[] frameData) {
		if (frame < 0 || frame >= nFrames) {
			return;
		}
		for (int i = 0; i < nChannels; i++) {
			frameData[i] = theSampleData[i][frame];
		}
	}

	/**
	 * Retrieves a frame of audio using no interpolation. If the frame is not in
	 * the sample range then zeros are returned.
	 * 
	 * @param posInMS
	 *            The frame to read -- will take the last frame before this one.
	 * @param result
	 *            The framedata to fill.
	 */
	public void getFrameNoInterp(double posInMS, float[] result) {
		double frame = msToSamples(posInMS);
		int frame_floor = (int) Math.floor(frame);
		getFrame(frame_floor, result);
	}

	/**
	 * Retrieves a frame of audio using linear interpolation. If the frame is
	 * not in the sample range then zeros are returned.
	 * 
	 * @param posInMS
	 *            The frame to read -- can be fractional (e.g., 4.4).
	 * @param result
	 *            The framedata to fill.
	 */
	public void getFrameLinear(double posInMS, float[] result) {
		double frame = msToSamples(posInMS);
		int frame_floor = (int) Math.floor(frame);
		if (frame_floor > 0 && frame_floor < nFrames) {
			double frame_frac = frame - frame_floor;
			if (frame_floor == nFrames - 1) {
				getFrame(frame_floor, result);
			} else // lerp
			{
				getFrame(frame_floor, current);
				getFrame(frame_floor + 1, next);
				for (int i = 0; i < nChannels; i++) {
					result[i] = (float) ((1 - frame_frac) * current[i] + frame_frac * next[i]);
				}
			}
		} else {
			for (int i = 0; i < nChannels; i++) {
				result[i] = 0.0f;
			}
		}
	}

	/**
	 * Retrieves a frame of audio using cubic interpolation. If the frame is not
	 * in the sample range then zeros are returned.
	 * 
	 * @param posInMS
	 *            The frame to read -- can be fractional (e.g., 4.4).
	 * @param result
	 *            The framedata to fill.
	 */
	public void getFrameCubic(double posInMS, float[] result) {
		double frame = msToSamples(posInMS);
		float a0, a1, a2, a3, mu2;
		float ym1, y0, y1, y2;
		for (int i = 0; i < nChannels; i++) {
			int realCurrentSample = (int) Math.floor(frame);
			float fractionOffset = (float) (frame - realCurrentSample);

			if (realCurrentSample >= 0 && realCurrentSample < (nFrames - 1)) {
				realCurrentSample--;
				if (realCurrentSample < 0) {
					getFrame(0, current);
					ym1 = current[i];
					realCurrentSample = 0;
				} else {
					getFrame(realCurrentSample++, current);
					ym1 = current[i];
				}
				getFrame(realCurrentSample++, current);
				y0 = current[i];
				if (realCurrentSample >= nFrames) {
					getFrame((int) nFrames - 1, current);
					y1 = current[i]; // ??
				} else {
					getFrame(realCurrentSample++, current);
					y1 = current[i];
				}
				if (realCurrentSample >= nFrames) {
					getFrame((int) nFrames - 1, current);
					y2 = current[i]; // ??
				} else {
					getFrame(realCurrentSample++, current);
					y2 = current[i];
				}
				mu2 = fractionOffset * fractionOffset;
				a0 = y2 - y1 - ym1 + y0;
				a1 = ym1 - y0 - a0;
				a2 = y1 - ym1;
				a3 = y0;
				result[i] = a0 * fractionOffset * mu2 + a1 * mu2 + a2
						* fractionOffset + a3;
			} else {
				result[i] = 0.0f;
			}
		}
	}

	/**
	 * Get a series of frames. FrameData will only be filled with the available
	 * frames. It is the caller's responsibility to count how many frames are
	 * valid. <code>min(nFrames - frame, frameData[0].length)</code> frames in
	 * frameData are valid.
	 * 
	 * If the data is not readily available this doesn't do anything.
	 * 
	 * @param frame
	 *            The frame number (NOTE: This parameter is in frames, not in
	 *            ms!)
	 * @param frameData
	 */
	public void getFrames(int frame, float[][] frameData) {
		if (frame >= nFrames) {
			return;
		}
		int numFloats = Math.min(frameData[0].length, (int) (nFrames - frame));
		for (int i = 0; i < nChannels; i++) {
			System.arraycopy(theSampleData[i], frame, frameData[i], 0, numFloats);
		}
	}

	/**
	 * Clears the (writeable) sample.
	 */
	public void clear() {
		for (int i = 0; i < nChannels; i++) {
			Arrays.fill(theSampleData[i], 0f);
		}

	}

	/**
	 * Write a single frame into this sample. Takes care of format conversion.
	 * 
	 * This only makes sense if this.isWriteable() returns true. If
	 * isWriteable() is false, the behaviour is undefined/unstable.
	 * 
	 * @param frame
	 *            The frame to write into. Must be >=0 and <numFrames.
	 * @param frameData
	 *            The frame data to write.
	 */
	public void putFrame(int frame, float[] frameData) {
		for (int i = 0; i < nChannels; i++) {
			theSampleData[i][frame] = frameData[i];
		}
	}

	/**
	 * Write multiple frames into the sample.
	 * 
	 * This only makes sense if this.isWriteable() returns true. If
	 * isWriteable() is false, the behaviour is undefined/unstable.
	 * 
	 * @param frame
	 *            The frame to write into.
	 * @param frameData
	 *            The frames to write.
	 */
	public void putFrames(int frame, float[][] frameData) {
		int numFrames = Math.min(frameData[0].length, (int) (nFrames - frame));
		if (frame < 0) {
			return;
		}
		// TODO in loop record this falls over
		for (int i = 0; i < nChannels; i++) {
			System.arraycopy(frameData[i], 0, theSampleData[i], frame, numFrames);
		}
	}

	/**
	 * Write multiple frames into the sample.
	 * 
	 * This only makes sense if this.isWriteable() returns true. If
	 * isWriteable() is false, the behaviour is undefined/unstable.
	 * 
	 * @param frame
	 *            The frame to write into.
	 * @param frameData
	 *            The frames to write.
	 * @param offset
	 *            The offset into frameData
	 * @param numFrames
	 *            The number of frames from frameData to write
	 */
	public void putFrames(int frame, float[][] frameData, int offset,
			int numFrames) {
		if (numFrames <= 0) {
			return;
		}
		// clip numFrames
		numFrames = Math.min(numFrames, (int) (nFrames - frame));
		for (int i = 0; i < nChannels; i++) {
			System.arraycopy(frameData[i], offset, theSampleData[i], frame,
					numFrames);
		}
	}

	/**
	 * This records the sample to a WAV format audio file. It is BLOCKING.
	 * 
	 * @param fn
	 *            The filename (should have the .aif extension).
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void write(String fn) throws IOException {
		write(fn, AudioFileType.WAV);
	}
	
	/**
	 * This records the sample to a file with the specified
	 * AudioFile.Type. It is BLOCKING.
	 * 
	 * @param fn
	 *            The filename.
	 * @param type
	 *            The type (AIFF, WAVE, etc.)
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void write(String fn, AudioFileType type) throws IOException {
		write(fn, type, new SampleAudioFormat(this.sampleRate, 16, this.nChannels));
	}

	/**
	 * This records the sample to a file with the specified
	 * AudioFile.Type. It is BLOCKING.
	 * 
	 * @param fn
	 *            The filename.
	 * @param type
	 *            The type (AIFF, WAVE, etc.)
	 * @param saf
	 * 			  The SampleAudioFormat
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void write(String fn, AudioFileType type, SampleAudioFormat saf) throws IOException {	
		Class<? extends AudioFileWriter> theRealAudioFileWriterClass = audioFileWriterClass == null ? defaultAudioFileWriterClass : audioFileWriterClass;
		//JavaSound can only write 16-bit, but we can use WavFileReaderWriter for >16-bit wavs, hence always write wavs this way
		if(type == AudioFileType.WAV) {
			try {
				theRealAudioFileWriterClass = (Class<? extends AudioFileWriter>) Class.forName("net.beadsproject.beads.data.audiofile.WavFileReaderWriter");
			} catch (ClassNotFoundException e) {
				//worth continuing in case the default manages it.
			}
		}
		if(theRealAudioFileWriterClass == null) {
			throw new IOException("Sample: No AudioFile Class has been set and the default JavaSoundAudioFile Class cannot be found. Aborting write(). You may need to link to beads-io.jar.");
		}
		try {
			AudioFileWriter audioFileWriter = theRealAudioFileWriterClass.getConstructor().newInstance();
			audioFileWriter.writeAudioFile(theSampleData, fn, type, saf);
		} catch(Exception e) {
			throw new IOException("Sample: Unable to create or use the AudioFileWriter class.", e);
		}
	}

	/**
	 * <b>Advanced</b>
	 * 
	 * Change the number of frames in the (writeable) sample. This is slow and
	 * so should be used sparingly.
	 * 
	 * The new frames may contain garbage, but see
	 * {@link #resizeWithZeros(long)}.
	 * 
	 * @param frames
	 *            The total number of frames the sample should have.
	 */
	public void resize(long frames) {
		int framesToCopy = (int) Math.min(frames, nFrames);
		float[][] olddata = theSampleData;
		theSampleData = new float[nChannels][(int) frames];
		for (int i = 0; i < nChannels; i++)
			System.arraycopy(olddata[i], 0, theSampleData[i], 0,
					framesToCopy);
		nFrames = frames;
	}

	/**
	 * Just like {@link #resize(long)} but initialises the new frames with
	 * zeros.
	 * 
	 * @param frames
	 *            The total number of frames the sample should have.
	 */
	public void resizeWithZeros(long frames) {
		nFrames = frames;
	}

	/**
	 * Converts from milliseconds to samples based on the sample rate.
	 * 
	 * @param msTime
	 *            the time in milliseconds.
	 * 
	 * @return the time in samples.
	 */
	public double msToSamples(double msTime) {
		return msTime * this.sampleRate / 1000.0f;
	}

	/**
	 * Converts from samples to milliseconds based on the sample rate.
	 * 
	 * @param sampleTime
	 *            the time in samples.
	 * 
	 * @return the time in milliseconds.
	 */
	public double samplesToMs(double sampleTime) {
		return sampleTime / this.sampleRate * 1000.0f;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getFileName();
	}

	/**
	 * Gets the full file path.
	 * 
	 * @return the file path.
	 */
	public String getFileName() {
		if (filename == null)
			return null;
		return filename;
	}

	/**
	 * Gets the simple name.
	 * 
	 * @return the name.
	 */
	public String getSimpleName() {
		if (simpleName != null)
			return simpleName;
		String fileName = getFileName();
		if (fileName == null)
			return null;
		String[] nameParts = fileName.split("/");
		return nameParts[nameParts.length - 1];
	}

	/**
	 * Sets the simple name.
	 * 
	 * @param simpleName
	 *            the name.
	 */
	public void setSimpleName(String simpleName) {
		this.simpleName = simpleName;
	}

	/**
	 * Get the sample rate
	 * @return
	 */
	public float getSampleRate() {
		return this.sampleRate;
	}

	/**
	 * Get the number of channels
	 * @return
	 */
	public int getNumChannels() {
		return nChannels;
	}

	/**
	 * Get the number of frames
	 * @return
	 */
	public long getNumFrames() {
		return nFrames;
	}

	/**
	 * Return length of sample in ms
	 * @return
	 */
	public double getLength() {
		return 1000f * this.nFrames / this.sampleRate;
	}

	/**
	 * Specify an audio file that the Sample reads from.
	 * 
	 * If BufferedRegime is TOTAL, this will block until the sample is loaded.
	 * 
	 * @throws IOException
	 * 
	 */
	private void loadAudioFile(String file) throws IOException {
		//we have to deal with a bug in Tritonus: JavaSound doesn't accept 24-bit wav but strangely Tritonus 
		//interprets 24-bit wavs as mp3s. So we intercept all wavs and send them to the WavFileReaderWriter.
		//In the first instance we can only use the file suffix as a clue to this, not the header.		
		Class<? extends AudioFileReader> theRealAudioFileReaderClass = audioFileReaderClass == null ? defaultAudioFileReaderClass : audioFileReaderClass;
		if(file.endsWith(".wav") || file.endsWith(".WAV")) {
			try {
				theRealAudioFileReaderClass = (Class<? extends AudioFileReader>) Class.forName("net.beadsproject.beads.data.audiofile.WavFileReaderWriter");
			} catch (ClassNotFoundException e) {
				//worth continuing in case the default manages it.
			}
		}
		AudioFileReader audioFileReader;
		try {
			audioFileReader = theRealAudioFileReaderClass.getConstructor().newInstance();
		} catch (Exception e1) {
			throw new IOException("Sample: No AudioFileReader Class has been set and the default JavaSoundAudioFile Class cannot be found. Aborting write(). You may need to link to beads-io.jar.");
		} 
		try {
			this.theSampleData = audioFileReader.readAudioFile(file);
		} catch (Exception e) {
			throw new IOException(e);
		}
		this.sampleRate = audioFileReader.getSampleAudioFormat().sampleRate;
		this.nChannels = theSampleData.length;
		this.nFrames = theSampleData[0].length;
		this.current = new float[nChannels];
        this.next = new float[nChannels];
	}
}
