package net.beadsproject.beads.data.audiofile;

import java.io.IOException;
import java.util.HashSet;

import net.beadsproject.beads.data.SampleAudioFormat;

/**
 * Implementing this interface indicates support for 'one-shot' reading of an audio file. 
 * That is, an entire audio file can be opened, read into a float[][] and then closed in a single blocking method.
 *   
 * @author aengus
 */
public interface AudioFileReader {
	
	/**
	 * Single method to read an entire audio file in one go.
	 * @param data     - upon return, this will point to a 2D array containing all the audio data. 
	 *                   The first dimension (data.length) is the number of channels. 
	 *                   The second dimension (data[0].length) is the number of frames.
	 * @param filename - the name of the file to be read
	 * @param saf      - upon return, this will point to a SampleAudioFormat object containing the details of the sample data that was read.
	 */
	public float[][] readAudioFile(String filename) throws IOException, OperationUnsupportedException, FileFormatException;
	
	/**
	 * After reading, the SampleAudioFormat can be obtained.
	 * @return the SampleAudioFormat object describing the sample data that has been read in.
	 */
	public SampleAudioFormat getSampleAudioFormat();

	/**
	 * Get the supported file types.
	 * @return - the supported file types.
	 */
	public HashSet<AudioFileType> getSupportedFileTypesForReading();
}
