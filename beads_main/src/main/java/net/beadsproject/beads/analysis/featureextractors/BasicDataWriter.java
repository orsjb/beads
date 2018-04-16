/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.analysis.featureextractors;

import java.io.FileOutputStream;
import java.io.PrintStream;

import net.beadsproject.beads.analysis.FeatureExtractor;
import net.beadsproject.beads.core.TimeStamp;

/**
 * BasicDataWriter grabs forwarded feature data and prints it to a file in a simple format.
 * Each line contains a new set of features.
 * Each individual feature is separated by whitespace.
 */
public class BasicDataWriter<T> extends FeatureExtractor<Object, T> {

	/** The print stream. */
	protected PrintStream ps;
	
	/**
	 * Instantiates a new BasicDataWriter with the given FileOutputStream.
	 * 
	 * @param fos the FileOutputStream.
	 */
	public BasicDataWriter(FileOutputStream fos) {
		ps = new PrintStream(fos);
	}
	
	/* (non-Javadoc)
	 * @see net.beadsproject.beads.analysis.FeatureExtractor#process(java.lang.Object)
	 */
	@Override
	public void process(TimeStamp startTime, TimeStamp endTime, T data) {
		if(data instanceof float[]) {
			float[] dataf = (float[])data;
			for(int i = 0; i < dataf.length; i++) {
				ps.print(dataf[i]);
				ps.print(" ");			
			}
			ps.println();	
		} else if(data instanceof Object[]) {
			Object[] dataf = (Object[])data;
			for(int i = 0; i < dataf.length; i++) {
				ps.print(dataf[i]);
				ps.print(" ");			
			}
			ps.println();		
		} else {
			ps.println(data);
		}
	}

}
