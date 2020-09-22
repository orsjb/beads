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
	 * @param filename - the name of the file to be read
	 * @return the samples as arrays
	 */
	float[][] readAudioFile(String filename) throws IOException, OperationUnsupportedException, FileFormatException;
	
	/**
	 * After reading, the SampleAudioFormat can be obtained.
	 * @return the SampleAudioFormat object describing the sample data that has been read in.
	 */
	SampleAudioFormat getSampleAudioFormat();

	/**
	 * Get the supported file types.
	 * @return - the supported file types.
	 */
	HashSet<AudioFileType> getSupportedFileTypesForReading();
}
