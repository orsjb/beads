/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;

import net.beadsproject.beads.core.AudioUtils;
import net.beadsproject.beads.data.audiofile.AudioFile;

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
public class Sample {

	// Sample stuff
	private AudioFile audioFile;
	private SampleAudioFormat audioFormat;
	private int nChannels;
	private long nFrames;
	private double length; // length in ms
	private String simpleName;

	// the class that handles audio file IO
	// if null then any read/write operations will attempt to use JavaSound via
	// reflection
	private Class<? extends AudioFile> audioFileHandlerClass = null;
	private static Class<? extends AudioFile> defaultAudioFileHandlerClass;
	static {
		try {
			defaultAudioFileHandlerClass = (Class<? extends AudioFile>) Class
					.forName("net.beadsproject.beads.data.audiofile.JavaSoundAudioFile");
		} catch (ClassNotFoundException e) {
			// snuff
			defaultAudioFileHandlerClass = null;
		}
	}

	private float[][] theSampleData; // f_sampleData[0] first channel,
										// f_sampleData[1] second channel, etc..

	private float[] current, next; // used as temp buffers whilst calculating
									// interpolation

	/**
	 * Instantiates a new writable sample with specified length and default
	 * audio format: 44.1KHz, 16 bit, stereo.
	 * 
	 * @param length
	 *            the length in ms.
	 */
	public Sample(double length) {
		this(new SampleAudioFormat(44100, 16, 2), length);
	}

	/**
	 * Instantiates a new writeable Sample with the specified audio format and
	 * length;
	 * 
	 * The sample isn't initialised, so may contain junk. Use {@link #clear()}
	 * to clear it.
	 * 
	 * @param audioFormat
	 *            the audio format.
	 * @param length
	 *            The length of the sample in ms.
	 */
	public Sample(SampleAudioFormat audioFormat, double length) {
		this.audioFormat = audioFormat;
		nChannels = audioFormat.channels;
		current = new float[nChannels];
		next = new float[nChannels];
		nFrames = (long) msToSamples(length);
		theSampleData = new float[nChannels][(int) nFrames];
		length = 1000f * nFrames / audioFormat.sampleRate;
	}

	/**
	 * Create a sample from a file. This constructor immediately loads the
	 * entire audio file into memory.
	 * 
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 * @throws AudioFileUnsupportedException
	 */
	public Sample(String filename) throws IOException {
		setFile(filename);
	}

	/**
	 * Create a sample from an input stream. This constructor immediately loads
	 * the entire audio file into memory.
	 * 
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 * @throws AudioFileUnsupportedException
	 */
	public Sample(InputStream is) throws IOException {
		setExistingFile(is);
	}

	/**
	 * Create a sample from an Audio File, using the default buffering scheme.
	 * 
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 * @throws AudioFileUnsupportedException
	 */
	public Sample(AudioFile af) throws IOException {
		setExistingFile(af);
	}

	/**
	 * Gets the current AudioFileHandlerClass. This is an instantiation of
	 * {@link AudioFile} that Sample will use for file IO operations. If unset
	 * or set to null Sample will attempt to find a default implementation:
	 * JavaSoundAudioFile.
	 * 
	 * @return
	 */
	public Class<? extends AudioFile> getAudioFileHandlerClass() {
		return audioFileHandlerClass;
	}

	/**
	 * Set the audioFileHandlerClass. This is an instantiation of
	 * {@link AudioFile} that Sample will use for file IO operations. If unset
	 * or set to null Sample will attempt to find a default implementation:
	 * JavaSoundAudioFile.
	 * 
	 * @param audioFileHandlerClass
	 */
	public void setAudioFileHandlerClass(
			Class<? extends AudioFile> audioFileHandlerClass) {
		this.audioFileHandlerClass = audioFileHandlerClass;
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
					result[i] = (float) ((1 - frame_frac) * current[i] + frame_frac
							* next[i]);
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
	public void write(String fn) throws Exception {
		write(fn, AudioFile.Type.WAV);
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
	public void write(String fn, AudioFile.Type type) throws Exception {		
		if(audioFile == null) {
			Class<? extends AudioFile> theRealHandler = audioFileHandlerClass == null ? defaultAudioFileHandlerClass
					: audioFileHandlerClass;
			if(theRealHandler == null) {
				throw new IOException(
						"Sample: No AudioFile Class has been set and the default JavaSoundAudioFile Class cannot be found. Aborting write(). You may need to link to beads-io.jar.");
			}
			audioFile = (AudioFile)theRealHandler.getConstructor(SampleAudioFormat.class).newInstance(audioFormat);
		}
		audioFile.write(fn, type, theSampleData);
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
	 * @throws Exception
	 *             Thrown if the sample isn't writeable.
	 */
	public void resize(long frames) {
		int framesToCopy = (int) Math.min(frames, nFrames);
		float[][] olddata = theSampleData;
		theSampleData = new float[nChannels][(int) frames];
		for (int i = 0; i < nChannels; i++)
			System.arraycopy(olddata[i], 0, theSampleData[i], 0,
					framesToCopy);
		nFrames = frames;
		length = (float) samplesToMs(nFrames);
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
		length = (float) samplesToMs(nFrames);
	}

	/**
	 * Prints audio format info to System.out.
	 */
	public void printAudioFormatInfo() {
		System.out.println("Sample Rate: " + audioFormat.sampleRate);
		System.out.println("Channels: " + nChannels);
		System.out.println("Big Endian: " + audioFormat.bigEndian);
		System.out.println("Signed: " + audioFormat.signed);
	}

	/**
	 * Converts from milliseconds to samples based on the sample rate specified
	 * by {@link #audioFormat}.
	 * 
	 * @param msTime
	 *            the time in milliseconds.
	 * 
	 * @return the time in samples.
	 */
	public double msToSamples(double msTime) {
		return msTime * audioFormat.sampleRate / 1000.0f;
	}

	/**
	 * Converts from samples to milliseconds based on the sample rate specified
	 * by {@link #audioFormat}.
	 * 
	 * @param sampleTime
	 *            the time in samples.
	 * 
	 * @return the time in milliseconds.
	 */
	public double samplesToMs(double sampleTime) {
		return sampleTime / audioFormat.sampleRate * 1000.0f;
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
		if (audioFile == null)
			return null;
		return audioFile.getName();
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

	public AudioFile getAudioFile() {
		return audioFile;
	}

	public SampleAudioFormat getAudioFormat() {
		return audioFormat;
	}

	public int getNumChannels() {
		return nChannels;
	}

	public long getNumFrames() {
		return nFrames;
	}

	/**
	 * @return The number of bytes this sample uses to store each sample. May be
	 *         different than audioFile.audioFormat.
	 */
	public int getBytesPerSample() {
		return Float.SIZE / 8;
	}

	public double getLength() {
		return length;
	}

	public float getSampleRate() {
		return audioFormat.sampleRate;
	}



	/**
	 * Specify an audio file that the Sample reads from.
	 * 
	 * If BufferedRegime is TOTAL, this will block until the sample is loaded.
	 * 
	 * @throws AudioFileUnsupportedException
	 * 
	 */
	private void setFile(String file) throws IOException {

		Class<? extends AudioFile> theRealHandler = audioFileHandlerClass == null ? defaultAudioFileHandlerClass
				: audioFileHandlerClass;
		try {
			audioFile = (AudioFile) theRealHandler.getConstructor(String.class)
					.newInstance(file);
		} catch (Exception e) {
//			System.out.println("problem loading file " + file);
//			if(e instanceof IOException) {
//				throw (IOException)e;
//			} else {
//				e.printStackTrace();
//			}
			throw new IOException("Problem loading file " + file);
//			throw new IOException(
//					"Sample: No AudioFile Class has been set and the default JavaSoundAudioFile Class cannot be found. Aborting setFile(). You may need to link to beads-io.jar");
		}
		setExistingFile(audioFile);
	}

	/**
	 * Specify an audio file that the Sample reads from.
	 * 
	 * This will block until the sample is loaded.
	 * 
	 * @throws AudioFileUnsupportedException
	 * 
	 */
	private void setExistingFile(InputStream is) throws IOException {
		Class<? extends AudioFile> theRealHandler = audioFileHandlerClass == null ? defaultAudioFileHandlerClass
				: audioFileHandlerClass;
		try {
			audioFile = (AudioFile) theRealHandler.getConstructor(
					InputStream.class).newInstance(is);
		} catch (Exception e) {
			throw new IOException(
					"Sample: No AudioFile Class has been set and the default JavaSoundAudioFile Class cannot be found. Aborting setFile(). You may need to link to beads-io.jar");
		}
		setExistingFile(audioFile);
	}

	/**
	 * Specify an explicit AudioFile that the Sample reads from. NOTE: Only one
	 * sample should reference a particular AudioFile.
	 * 
	 * This will block until the sample is loaded.
	 * 
	 * @throws IOException.
	 * 
	 */
	private void setExistingFile(AudioFile af) throws IOException {
		audioFile = af;
		audioFile.open();
		audioFormat = audioFile.getFormat();
		nFrames = audioFile.getNumFrames();
		nChannels = audioFile.getNumChannels();
		current = new float[nChannels];
		next = new float[nChannels];
		length = audioFile.getLength();
		loadEntireSample();
	}

	// a helper function, loads the entire sample into sampleData
	private void loadEntireSample() throws IOException {
		final int BUFFERSIZE = 4096;
		byte[] audioBytes = new byte[BUFFERSIZE];

		int sampleBufferSize = 4096;
		byte[] data = new byte[sampleBufferSize];

		int bytesRead;
		int totalBytesRead = 0;
		while ((bytesRead = audioFile.read(audioBytes)) != -1) {
			// resize buf if necessary
			if (bytesRead > (sampleBufferSize - totalBytesRead)) {
				sampleBufferSize = Math.max(sampleBufferSize * 2,
						sampleBufferSize + bytesRead);
				// resize buffer
				byte[] newBuf = new byte[sampleBufferSize];
				System.arraycopy(data, 0, newBuf, 0, data.length);
				data = newBuf;
			}
			System.arraycopy(audioBytes, 0, data, totalBytesRead, bytesRead);
			totalBytesRead += bytesRead;
		}
		// resize buf to proper length
		// resize buf if necessary
		if (sampleBufferSize > totalBytesRead) {
			sampleBufferSize = totalBytesRead;
			// resize buffer
			byte[] newBuf = new byte[sampleBufferSize];
			System.arraycopy(data, 0, newBuf, 0, sampleBufferSize);
			data = newBuf;
		}
		this.nFrames = sampleBufferSize / (2 * nChannels);
		this.length = 1000f * nFrames / audioFormat.sampleRate;
		// copy and deinterleave entire data
		theSampleData = new float[nChannels][(int) nFrames];
		float[] interleaved = new float[(int) (nChannels * nFrames)];
		AudioUtils.byteToFloat(interleaved, data, audioFormat.bigEndian);
		AudioUtils.deinterleave(interleaved, nChannels, (int) nFrames,
				theSampleData);
		audioFile.close();
	}

}
