package net.beadsproject.beads.data.audiofile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import net.beadsproject.beads.core.AudioUtils;
import net.beadsproject.beads.data.SampleAudioFormat;

import org.tritonus.share.sampled.file.TAudioFileFormat;

/**
 * Read and write audio files using the Javasound library. Only 'one-shot' reading and writing is supported (i.e. no streaming).
 * See {@link net.beadsproject.beads.data.audiofile.AudioFileWriter} and {@link net.beadsproject.beads.data.audiofile.AudioFileReader}.
 * @author aengus
 */
public class JavaSoundAudioFile implements AudioFileReader, AudioFileWriter {

	private File file;
	private URL url;
	private AudioInputStream audioInputStream;
	private String name;
	private boolean finished = false;
	private javax.sound.sampled.AudioFormat encodedFormat;
	private javax.sound.sampled.AudioFormat decodedFormat;
	private AudioInputStream encodedStream = null;
	private AudioInputStream decodedStream = null;
	private boolean isEncoded = false;
	private SampleAudioFormat audioFormat;
	private float[][] sampleData;
	private WavFileReaderWriter wavBackup = new WavFileReaderWriter();	

	public JavaSoundAudioFile() {
	}

	/**
	 * Convert a Javasound AudioFormat object to a Beads SampleAudioFormat object.
	 * @param af
	 * @return
	 */
	static public SampleAudioFormat convertJavasoundAudioFormatToBeadsAudioFormat(
			javax.sound.sampled.AudioFormat af) {
		boolean signed = af.getEncoding() == AudioFormat.Encoding.PCM_SIGNED; // ?
		SampleAudioFormat newaf = new SampleAudioFormat(af.getSampleRate(),
				af.getSampleSizeInBits(), af.getChannels(), signed,
				af.isBigEndian());
		return newaf;
	}

	/**
	 * Convert a Beads SampleAudioFormat object to a Javasound AudioFormat object.
	 * @param saf
	 * @return
	 */
	static public AudioFormat convertBeadsAudioFormatToJavasoundAudioFormat(SampleAudioFormat saf) {
		AudioFormat af = new AudioFormat(saf.sampleRate, saf.bitDepth, saf.channels, saf.signed, saf.bigEndian);
		return af;
	}

	/**
	 * See {@link net.beadsproject.beads.data.audiofile.AudioFileWriter#writeAudioFile}
	 */
	public void writeAudioFile(float[][] data, String filename, AudioFileType type, SampleAudioFormat saf) throws IOException, OperationUnsupportedException {
		
		if(!this.getSupportedFileTypesForWriting().contains(type)) {
			throw new OperationUnsupportedException("Unsupported file type for writing: " + type);
		}
		if (saf.bitDepth > 16) {
			throw new OperationUnsupportedException("Unsupported bit depth. Javasound cannot write WAV or AIFF files with bit depth > 16.");
		}

		int chans = data.length;
		int frames = data[0].length;

		// Convert de-interleaved data to interleaved bytes...
		float interleaved[] = new float[chans * frames];
		AudioUtils.interleave(data, chans, frames, interleaved);
		byte bytes[] = new byte[chans*frames*saf.bitDepth/8];
		AudioUtils.floatToByte(bytes, interleaved, saf.bigEndian);

		// Write the file
		AudioFormat jsaf = new AudioFormat(saf.sampleRate, saf.bitDepth, saf.channels, saf.signed, saf.bigEndian);
		AudioFileFormat.Type jsType = AudioFileFormat.Type.WAVE;
		if (type == AudioFileType.AIFF) {
			jsType = AudioFileFormat.Type.AIFF;
		}
		AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(bytes),jsaf,frames), jsType, new File(filename));
	}

	/**
	 * See {@link net.beadsproject.beads.data.audiofile.AudioFileWriter#getSupportedFileTypesForWriting}
	 */
	public HashSet<AudioFileType> getSupportedFileTypesForWriting() {
		HashSet<AudioFileType> types = new HashSet<AudioFileType>();
		types.add(AudioFileType.WAV);
		types.add(AudioFileType.AIFF);
		return types;
	}

	/**
	 * See {@link net.beadsproject.beads.data.audiofile.AudioFileReader#readAudioFile}
	 */
	public float[][] readAudioFile(String name) throws IOException {
		//First try to interpret string as URL, then as local file
		try {
			url = new URL(name);
			file = null;
			this.name = url.getFile();
		} catch (Exception e) {
			file = new File(name);
			url = null;
			this.name = file.getAbsolutePath();
		}
		audioInputStream = null;
		try {
			prepareForReading();
			readEntireFile();
		} catch (Exception e) {
			throw new IOException("Could not read audio file: " + this.name);
		}
		this.close();
		return sampleData;
	}

	/**
	 * See {@link net.beadsproject.beads.data.audiofile.AudioFileReader#getSupportedFileTypesForReading}
	 */
	public HashSet<AudioFileType> getSupportedFileTypesForReading() {
		HashSet<AudioFileType> types = new HashSet<AudioFileType>();
		types.add(AudioFileType.WAV);
		types.add(AudioFileType.AIFF);
		types.add(AudioFileType.MP3);
		return types;
	}

	/**
	 * Opens the audio file, ready for data access.
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	private void prepareForReading() throws IOException {
		
		finished = false;

		try {
			encodedStream = getStream();
		} catch (UnsupportedAudioFileException e) {
			throw (new IOException(e.getMessage())); // converts UnsupportedAudioFileException, which is JavaSound specific, to more generic IOException.
		}
		encodedFormat = encodedStream.getFormat();

		int bitDepth = 16;

		decodedFormat = new javax.sound.sampled.AudioFormat(
				javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
				encodedFormat.getSampleRate(), 
				bitDepth,
				encodedFormat.getChannels(), 
				encodedFormat.getChannels() * (bitDepth / 8), // 2*8 = 16-bits per sample per channel
				44100, 
				encodedFormat.isBigEndian());

		if (AudioSystem.isConversionSupported(decodedFormat, encodedFormat)) {
			isEncoded = true;
			decodedStream = AudioSystem.getAudioInputStream(decodedFormat, encodedStream);
		} else { // try to use the undecoded format
			isEncoded = false;
			decodedFormat = encodedFormat;
			decodedStream = encodedStream;

			if (decodedFormat.getFrameSize() != 2 * decodedFormat.getChannels()) {
				close();
				String s = "Tried to load " + (8 * decodedFormat.getFrameSize() / decodedFormat.getChannels()) + "-bit file, but couldn't convert to 16-bit.";
				throw new IOException(s);
			}
		}
		audioFormat = convertJavasoundAudioFormatToBeadsAudioFormat(decodedFormat);
	}

	/**
	 * Reads the entire file into a float[][].
	 * @throws IOException
	 */
	private void readEntireFile() throws IOException {
		
		final int BUFFERSIZE = 4096;
		byte[] audioBytes = new byte[BUFFERSIZE];
		int sampleBufferSize = 4096;
		byte[] data = new byte[sampleBufferSize];
		int bytesRead;
		int totalBytesRead = 0;
		
		while ((bytesRead = this.read(audioBytes)) != -1) {
			// resize buf if necessary
			if (bytesRead > (sampleBufferSize - totalBytesRead)) {
				sampleBufferSize = Math.max(sampleBufferSize * 2, sampleBufferSize + bytesRead);
				byte[] newBuf = new byte[sampleBufferSize];
				System.arraycopy(data, 0, newBuf, 0, data.length);
				data = newBuf;
			}
			System.arraycopy(audioBytes, 0, data, totalBytesRead, bytesRead);
			totalBytesRead += bytesRead;
		}

		// resize buf to proper length if necessary
		if (sampleBufferSize > totalBytesRead) {
			sampleBufferSize = totalBytesRead;
			byte[] newBuf = new byte[sampleBufferSize];
			System.arraycopy(data, 0, newBuf, 0, sampleBufferSize);
			data = newBuf;
		}
		int nFrames = sampleBufferSize / (2 * audioFormat.channels);

		// Copy and de-interleave entire data
		sampleData = new float[audioFormat.channels][(int) nFrames];
		float[] interleaved = new float[(int) (audioFormat.channels * nFrames)];
		AudioUtils.byteToFloat(interleaved, data, audioFormat.bigEndian);
		AudioUtils.deinterleave(interleaved, audioFormat.channels, (int) nFrames, sampleData);
	}

	/**
	 * Read bytes directly from the decoded audiofile. The bytes will be in an
	 * interleaved format. It is the responsibility of the caller to interpret
	 * this data correctly.
	 * 
	 * The number of bytes read is equal to the size of the byte buffer. If that
	 * many bytes aren't available the buffer will only be partially filled.
	 * 
	 * @param buffer A buffer to fill.
	 * 
	 * @return The number of bytes read. A value of -1 indicates the file has no data left.
	 */
	private int read(byte[] buffer) {
		if (finished) {
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
		return actualBytesRead;
	}

	/**
	 * Gets the AudioInputStream for reading.
	 * @return
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	private AudioInputStream getStream() throws UnsupportedAudioFileException, IOException {
		if (file != null)
			return AudioSystem.getAudioInputStream(file);
		else if (url != null)
			return AudioSystem.getAudioInputStream(url);
		else
			return audioInputStream;
	}

	/**
	 * Close the AudioInputStream objects as necessary.
	 * @throws IOException
	 */
	private void close() throws IOException {

		if (isEncoded)
			decodedStream.close();

		if (encodedStream != null) {
			encodedStream.close();
			encodedStream = null;
		}
		decodedStream = null;
	}

	/**
	 * Get the SampleAudioFormat read from a file.
	 * @return the SampleAudioFormat that has been read in.
	 */
	public SampleAudioFormat getSampleAudioFormat() {
		return audioFormat;
	}
}