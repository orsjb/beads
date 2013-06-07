/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data.audiofile;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import net.beadsproject.beads.core.AudioUtils;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleAudioFormat;

import org.tritonus.share.sampled.file.TAudioFileFormat;

/**
 * Uses javasound to load audio files located on disk or via a URL.
 * 
 * KNOWN ISSUE: Certain .wav files will be not be able to be loaded. This is due
 * to mp3spi recognizing them incorrectly as mp3s. This will hopefull be fixed
 * in the future, but until then resaving your .wavs with a different audio util
 * may help.
 * 
 * @author ben
 */
public class JavaSoundAudioFile extends AudioFile {

	// AudioFile encapsulates one of the following.
	// The one it encapsulates is none-null, the others are null.
	protected File file;
	protected URL url;
	protected AudioInputStream audioInputStream;

	// the mp3 info if its an mp3 file
	protected AudioFileFormat audioFileFormat;

	private long nTotalFramesRead = 0; // also a pointer into the current pos
	private boolean finished = false;

	// stream-specific stuff
	private long lengthInBytes; // length of file in bytes
	private javax.sound.sampled.AudioFormat encodedFormat;
	private javax.sound.sampled.AudioFormat decodedFormat;
	private AudioInputStream encodedStream;
	private AudioInputStream decodedStream;
	private int sampleSizeInBits = 16;
	private boolean isEncoded = false; // is the audio file encoded
	private int bufferSize;

	/**
	 * Load an audio file from disk. The audiofile needs to be open()'ed before
	 * it's data can be read. Note: AudioFile provides low-level access to audio
	 * files -- If you just want to access the data of a sound file use a
	 * Sample.
	 * 
	 * @see Sample
	 * 
	 * @param filename
	 *            The name of the file to open.
	 * 
	 * @throws IOException
	 *             If the file cannot be found or opened.
	 * @throws UnsupportedAudioFileException
	 *             If the file is of an unsupported audio type.
	 */
	public JavaSoundAudioFile(String filename) throws IOException,
			UnsupportedAudioFileException {
		this(filename, -1);
	}

	/**
	 * Advanced: Create an input stream from a file, but don't keep more than
	 * numBytes of data in memory.
	 * 
	 * @param filename
	 * @param bufferSize
	 *            The maximum number of bytes the AudioFile can keep in memory.
	 *            If it is <0 then the length of the audio file is used.
	 * 
	 * @throws IOException
	 *             If the file cannot be found or opened.
	 * @throws UnsupportedAudioFileException
	 *             If the file is of an unsupported audio type.
	 * @throws javax.sou
	 */
	public JavaSoundAudioFile(String filename, int bufferSize)
			throws IOException, UnsupportedAudioFileException {
		// first try to interpret string as URL, then as local file
		try {
			url = new URL(filename);
			file = null;
			name = url.getFile();
		} catch (Exception e) {
			file = new File(filename);
			url = null;
			name = file.getAbsolutePath();
		}
		audioInputStream = null;
		// Sometimes non-mp3 files get detected as mp3s so we eradicate them
		if (!name.endsWith(".mp3")) {
			if ((url != null && AudioSystem.getAudioFileFormat(url) instanceof TAudioFileFormat)
					|| (file != null && AudioSystem.getAudioFileFormat(file) instanceof TAudioFileFormat)) {
				throw (new UnsupportedAudioFileException(
						"Cannot read \""
								+ name
								+ "\". "
								+ "If it is a .wav then try re-converting it in a different audio program."));
			}
		}

		if (url != null)
			audioFileFormat = AudioSystem.getAudioFileFormat(url);
		else if (file != null)
			audioFileFormat = AudioSystem.getAudioFileFormat(file);
		// common init
		init(bufferSize);
	}

	public JavaSoundAudioFile(InputStream stream) throws IOException,
			UnsupportedAudioFileException {
		this(stream, -1);
	}

	public JavaSoundAudioFile(InputStream stream, int bufferSize)
			throws IOException, UnsupportedAudioFileException {
		BufferedInputStream bis = new BufferedInputStream(stream);
		audioFileFormat = AudioSystem.getAudioFileFormat(bis);
		audioInputStream = AudioSystem.getAudioInputStream(bis);
		url = null;
		file = null;
		name = stream.toString();
		init(bufferSize);
	}
	
	public JavaSoundAudioFile(SampleAudioFormat audioFormat) {
		super(audioFormat);
	}

	private void init(int bufferSize) throws UnsupportedAudioFileException,
			IOException {
		nFrames = audioFileFormat.getFrameLength();

		if (audioFileFormat instanceof TAudioFileFormat && bufferSize < 0) {
			this.bufferSize = audioFileFormat.getByteLength() + 1024; // plus a
																		// little
																		// bit
																		// in
																		// case
																		// length
																		// is
																		// off...
			// 1024*1024*10;
		} else if (bufferSize < 0) {
			this.bufferSize = 0;
		} else {
			this.bufferSize = bufferSize;
		}
		nTotalFramesRead = 0;
		encodedStream = null;
		decodedStream = null;
	}

	/**
	 * Reset the audio input stream.
	 * 
	 * For some audio formats, this may involve re-opening the associated file.
	 */
	public void reset() {
		if (trace)
			System.err.printf("AudioFile \"%s\" reset\n", name);

		try {
			if (encodedStream.markSupported()) {
				try {
					encodedStream.reset();
					if (finished)
						reopen();
					nTotalFramesRead = 0;
					finished = false;
				} catch (IOException e) {
					reopen();
					nTotalFramesRead = 0;
					finished = false;
				}
			} else {
				close();
				reopen();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Skips a number of frames. Note: this function skips frames, not bytes.
	 * Doesn't work for vbr!
	 * 
	 * Known issue: MP3 seeking is not precise. Why?
	 * 
	 * @param frames
	 *            Number of frames to skip
	 */
	public void skip(long frames) {
		if (frames <= 0)
			return;
		if (trace)
			System.err.printf("AudioFile skip %d frames\n", frames);

		try {
			if (isEncoded && nFrames != AudioSystem.NOT_SPECIFIED) {
				if (!audioInfo.containsKey("mp3.vbr")
						|| (Boolean) audioInfo.get("mp3.vbr")) {
					System.out
							.println("Beads does not currently support seeking on variable bit rate mp3s.");
				}

				/* test method, _read_ n frames */
				// byte[] foo = new byte[(int) (frames*nChannels*2)];
				// read(foo);

				// skip by a proportion of the file
				// this technique is used in jlGui
				double rate = 1.0 * frames / nFrames;
				long skipBytes = (long) Math.round(lengthInBytes * rate);
				long totalSkipped = 0;
				while (totalSkipped < skipBytes) {
					long skipped = encodedStream.skip(skipBytes - totalSkipped);
					totalSkipped += skipped;
					if (skipped == 0)
						break;
				}
				// System.out.printf("skip want: %db, got %db\n",skipBytes,totalSkipped);
			} else {
				@SuppressWarnings("unused")
				long skipped = decodedStream.skip(this.getFrameSize() * frames);
				// System.out.printf("skip want: %db, got %db\n",nChannels*numBytes*frames,skipped);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		nTotalFramesRead += frames;
	}

	/**
	 * Seek to a specific frame number. Note that seeking is slower than
	 * skipping forward.
	 * 
	 * @param frame
	 *            The frame number, relative to the start of the audio data.
	 */
	public void seek(int frame) {
		if (frame >= nTotalFramesRead) {
			skip(frame - nTotalFramesRead);
		} else {
			reset();
			skip(frame);
		}
	}

	/**
	 * Opens the audio file, ready for data access.
	 * 
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	public void open() throws IOException {
		if (trace)
			System.err.printf("AudioFile \"%s\" open start\n", name);
		finished = false;
		nTotalFramesRead = 0;

		try {
			encodedStream = getStream();
		} catch (UnsupportedAudioFileException e) {
			throw (new IOException(e.getMessage())); // converts
														// UnsupportedAudioFileException,
														// which is JavaSound
														// specific, to more
														// generic IOException.
		}
		encodedFormat = encodedStream.getFormat();

		int bitDepth = 16;

		decodedFormat = new javax.sound.sampled.AudioFormat(
				javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
				encodedFormat.getSampleRate(), bitDepth,
				encodedFormat.getChannels(), encodedFormat.getChannels()
						* (bitDepth / 8), // 2*8 = 16-bits per sample per
											// channel
				44100, encodedFormat.isBigEndian());

		audioInfo = decodedFormat.properties();

		if (AudioSystem.isConversionSupported(decodedFormat, encodedFormat)) {
			isEncoded = true;
			decodedStream = AudioSystem.getAudioInputStream(decodedFormat,
					encodedStream);
			nFrames = -1;
			lengthInBytes = audioFileFormat.getByteLength();
			audioInfo = audioFileFormat.properties();
			setLength(getTimeLengthEstimation(audioInfo));

			if (getLength() < 0) // A Zero Length is okay.
			{
				nFrames = -1;
			} else {
				nFrames = (long) (decodedFormat.getSampleRate() * (getLength() / 1000.));
			}
		} else {
			// try to use the undecoded format
			isEncoded = false;
			decodedFormat = encodedFormat;
			decodedStream = encodedStream;

			if (decodedFormat.getFrameSize() != 2 * decodedFormat.getChannels()) {
				close();
				String s = "Tried to load "
						+ (8 * decodedFormat.getFrameSize() / decodedFormat
								.getChannels())
						+ "-bit file, but couldn't convert to 16-bit.";
				throw (new IOException(s));
			}

			nFrames = (int) (decodedStream.getFrameLength());
			setLength(1000.f * decodedStream.getFrameLength()
					/ decodedFormat.getSampleRate());
		}

		sampleSizeInBits = decodedFormat.getSampleSizeInBits();
		audioFormat = convertJavasoundAudioFormatToBeadsAudioFormat(decodedFormat);

		if (sampleSizeInBits * getNumChannels() != (8 * getFrameSize())) {
			System.err.println("Error with frame size calculation.");
		}

		if (file != null && encodedStream.markSupported()) {
			encodedStream.mark(Math.min(bufferSize, (int) file.length()));
		}

		if (trace)
			System.err.printf("AudioFile \"%s\" open end\n", name);
	}

	// / re-opens a file, resetting the file pointers, etc..
	// / note that this will not recalculate length, etc.
	private void reopen() throws UnsupportedAudioFileException, IOException {
		if (trace)
			System.err.printf("AudioFile %s reopen\n", name);
		finished = false;
		nTotalFramesRead = 0;

		// if (file.exists())
		encodedStream = getStream();
		if (isEncoded)
			decodedStream = AudioSystem.getAudioInputStream(decodedFormat,
					encodedStream);
		else
			decodedStream = encodedStream;

		if (file != null && encodedStream.markSupported())
			encodedStream.mark(Math.min(bufferSize, (int) file.length()));
	}

	private AudioInputStream getStream() throws UnsupportedAudioFileException,
			IOException {
		if (file != null)
			return AudioSystem.getAudioInputStream(file);
		else if (url != null)
			return AudioSystem.getAudioInputStream(url);
		else
			return audioInputStream;
	}

	public String getInformation() {
		String s = super.getInformation();
		if (isOpen()) {
			s += "Audio File Format: " + audioFileFormat.toString() + "\n";
			if (isEncoded) {
				s += "Encoded Audio Format:" + encodedFormat.toString();
			}
		}
		return s;
	}

	/**
	 * Close the audio file. Can be re-opened.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		if (trace)
			System.err.printf("AudioFile \"%s\" closing\n", name);

		if (isEncoded)
			decodedStream.close();

		if (encodedStream != null) {
			encodedStream.close();
			encodedStream = null;
		}

		decodedStream = null;
	}

	/**
	 * Is the file stream closed?
	 */
	public boolean isClosed() {
		return encodedStream == null;
	}

	public javax.sound.sampled.AudioFormat getDecodedFormat() {
		return decodedFormat;
	}

	public javax.sound.sampled.AudioFormat getEncodedFormat() {
		return encodedFormat;
	}

	/**
	 * Read bytes directly from the decoded audiofile. The bytes will be in an
	 * interleaved format. It is the responsibility of the caller to interpret
	 * this data correctly.
	 * 
	 * The number of bytes read is equal to the size of the byte buffer. If that
	 * many bytes aren't available the buffer will only be partially filled.
	 * 
	 * @param buffer
	 *            A buffer to fill.
	 * 
	 * @return The number of bytes read. A value of -1 indicates the file has no
	 *         data left.
	 */
	public int read(byte[] buffer) {
		if (finished) {
			if (trace)
				System.out.println("AudioFile finished!");
			return -1;
		}

		// read the next bufferSize frames from the input stream
		int actualBytesRead = -1;
		try {
			// loop while reading data in
			int totalBytesRead = 0;
			while (totalBytesRead < buffer.length) {
				actualBytesRead = decodedStream.read(buffer, totalBytesRead,
						buffer.length - totalBytesRead);
				if (actualBytesRead == -1) {
					finished = true;
					if (totalBytesRead > 0)
						actualBytesRead = totalBytesRead;
					break;
				} else
					totalBytesRead += actualBytesRead;
			}
			if (totalBytesRead == buffer.length)
				actualBytesRead = totalBytesRead;

		} catch (IOException e) {
			finished = true;
		}

		if (finished || actualBytesRead == -1) {
			finished = true;
			return actualBytesRead;
		}

		nTotalFramesRead += actualBytesRead / getFrameSize();
		return actualBytesRead;
	}

	/**
	 * Read decoded audio data in a non-interleaved, Beads-friendly format.
	 * 
	 * Note: This function is <b>extremely inefficient</b> if the buffer size is
	 * constant. Use Sample, it is very efficient!
	 * 
	 * @param buffer
	 *            The buffer to fill. After execution buffer[i][j] will contain
	 *            the sample in channel i, frame j. Buffer has size
	 *            (numChannels,numFramesRequested).
	 * 
	 * @return The number of <u>frames</u> read.
	 */
	public int read(float[][] buffer) {
		if (buffer.length != getNumChannels() || buffer[0].length == 0)
			return 0;
		else if (finished)
			return 0;
		// else, read the data

		// read the next bufferSize frames from the input stream
		byte[] byteBuffer = new byte[(buffer[0].length * getNumChannels() * sampleSizeInBits) / 8];
		int actualBytesRead = -1;
		try {
			actualBytesRead = decodedStream.read(byteBuffer, 0,
					byteBuffer.length);
		} catch (IOException e) {
			finished = true;
		}

		if (finished || actualBytesRead == -1) {
			finished = true;
			return 0;
		}

		int numFramesJustRead = actualBytesRead / getFrameSize();
		nTotalFramesRead += numFramesJustRead;

		float[] floatbuf = new float[buffer[0].length * getNumChannels()
				* numFramesJustRead];
		AudioUtils.byteToFloat(floatbuf, byteBuffer,
				decodedFormat.isBigEndian(), numFramesJustRead
						* getNumChannels());
		AudioUtils.deinterleave(floatbuf, getNumChannels(), numFramesJustRead,
				buffer);

		return numFramesJustRead;
	}

	public Map<String, Object> getProperties() {
		Map<String, Object> props = super.getProperties();
		if (props != null) {
			Map<String, Object> prop = new HashMap<String, Object>();
			prop.putAll(audioFileFormat.properties());
			prop.putAll(props);
			return prop;
		} else
			return audioFileFormat.properties();
	}

	/**
	 * THIS CODE IS FROM jlGui PlayerUI.java. jlGui can be obtained at:
	 * http://www.javazoom.net/jlgui/jlgui.html
	 */
	@SuppressWarnings("rawtypes")
	public long getTimeLengthEstimation(Map properties) {
		long milliseconds = -1;
		int byteslength = -1;
		if (properties != null) {
			if (properties.containsKey("audio.length.bytes")) {
				byteslength = ((Integer) properties.get("audio.length.bytes"))
						.intValue();
			}
			if (properties.containsKey("duration")) {
				milliseconds = (((Long) properties.get("duration")).longValue()) / 1000;
			} else {
				// Try to compute duration
				int bitspersample = -1;
				int channels = -1;
				float samplerate = -1.0f;
				int framesize = -1;
				if (properties.containsKey("audio.samplesize.bits")) {
					bitspersample = ((Integer) properties
							.get("audio.samplesize.bits")).intValue();
				}
				if (properties.containsKey("audio.channels")) {
					channels = ((Integer) properties.get("audio.channels"))
							.intValue();
				}
				if (properties.containsKey("audio.samplerate.hz")) {
					samplerate = ((Float) properties.get("audio.samplerate.hz"))
							.floatValue();
				}
				if (properties.containsKey("audio.framesize.bytes")) {
					framesize = ((Integer) properties
							.get("audio.framesize.bytes")).intValue();
				}
				if (bitspersample > 0) {
					milliseconds = (int) (1000.0f * byteslength / (samplerate
							* channels * (bitspersample / 8)));
				} else {
					milliseconds = (int) (1000.0f * byteslength / (samplerate * framesize));
				}
			}
		}

		return milliseconds;
	}

	static public SampleAudioFormat convertJavasoundAudioFormatToBeadsAudioFormat(
			javax.sound.sampled.AudioFormat af) {
		boolean signed = af.getEncoding() == AudioFormat.Encoding.PCM_SIGNED; // ?
		SampleAudioFormat newaf = new SampleAudioFormat(af.getSampleRate(),
				af.getSampleSizeInBits(), af.getChannels(), signed,
				af.isBigEndian());
		return newaf;
	}

	static public AudioFormat convertBeadsAudioFormatToJavasoundAudioFormat(SampleAudioFormat saf) {
		AudioFormat af = new AudioFormat(saf.sampleRate, saf.bitDepth, saf.channels, saf.signed, saf.bigEndian);
		return af;
	}
	
	//the following write classes implement writing behaviour. However, they do not necessarily (i.e., in this case) update the frame length or error check to make sure
	//that the data being sent has the right number of channels.
	
	@Override
	public void write(String fn, Type type, byte[] audioDataBytes)
			throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(audioDataBytes);
		AudioFormat jsaf = new AudioFormat(audioFormat.sampleRate,
				audioFormat.bitDepth, audioFormat.channels, audioFormat.signed,
				audioFormat.bigEndian);
		int frames = audioDataBytes.length / (audioFormat.bitDepth / 8) / audioFormat.channels;
		AudioInputStream aos = new AudioInputStream(bais, jsaf, frames);
		AudioFileFormat.Type jsType = AudioFileFormat.Type.WAVE;
		if (type == Type.AIFF) {
			jsType = AudioFileFormat.Type.AIFF;
		}
		AudioSystem.write(aos, jsType, new File(fn));
	}

	@Override
	public void write(String fn, Type type, float[][] audioDataFloat)
			throws IOException {
		// convert the sample data into bytes then write it out as above
		// this is inefficient!
		int chans = audioDataFloat.length;
		int frames = audioDataFloat[0].length;
		// convert de-interleaved data to interleaved bytes...
		float interleaved[] = new float[chans * frames];
		AudioUtils.interleave(audioDataFloat, chans, frames, interleaved);
		byte bytes[] = new byte[chans*frames*audioFormat.bitDepth/8];
		AudioUtils.floatToByte(bytes, interleaved, audioFormat.bigEndian);
		AudioFormat jsaf = new AudioFormat(audioFormat.sampleRate, audioFormat.bitDepth, audioFormat.channels, audioFormat.signed, audioFormat.bigEndian);
		AudioFileFormat.Type jsType = AudioFileFormat.Type.WAVE;
		if (type == Type.AIFF) {
			jsType = AudioFileFormat.Type.AIFF;
		}
		AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(bytes),jsaf,frames), jsType, new File(fn));
	}
}
