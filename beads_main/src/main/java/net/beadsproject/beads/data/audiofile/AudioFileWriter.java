package net.beadsproject.beads.data.audiofile;

import java.io.IOException;
import java.util.HashSet;

import net.beadsproject.beads.data.SampleAudioFormat;

/**
 * Implementing this interface indicates support for 'one-shot' writing of an audio file. 
 * That is, an entire audio file can be opened, written from a float[][] and then closed in a single blocking method.
 *   
 * @author aengus
 */
public interface AudioFileWriter {

	/**
	 * Single method to write an entire audio file in one go.
	 * @param data - the data to be written
	 * @param filename - the name of the file to be written
	 * @param type - the type of audio file to be written (mp3, wav, etc.)
	 * @param saf - a SampleAudioFormat object specifying the attributes of the sample data to be written. Can be NULL, in which case default values will be used.
	 */
	public void writeAudioFile(float[][] data, String filename, AudioFileType type, SampleAudioFormat saf) throws IOException, OperationUnsupportedException, FileFormatException;

	/**
	 * Get the supported file types.
	 * @return - the supported file types.
	 */
	public HashSet<AudioFileType> getSupportedFileTypesForWriting();
}
