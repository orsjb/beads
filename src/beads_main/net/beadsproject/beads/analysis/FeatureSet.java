/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.analysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Hashtable;
import java.util.Map;
import net.beadsproject.beads.data.Sample;

/**
 * FeatureSet is a set of named {@link FeatureTrack}s associated with some audio data. FeatureSet provides methods for
 * reading and writing feature data to/from files.
 */
public class FeatureSet {

	/** The tracks. */
	private Hashtable<String, FeatureTrack> tracks;
	
	/** The list of Global Features. */
	private Hashtable<String, Object> globalFeatures;
	
	/** The file. */
	private File file;

	/**
	 * Tries to locate the FeatureSet for the given {@link Sample}. Assumes that the features are stored in a file next to the Sample's file
	 * but with the ending ".features". 
	 * 
	 * @param s the Sample
	 * 
	 * @return the FeatureSet or null if unsuccessful.
	 */
	public static FeatureSet forSample(Sample s) {
		String sampleFilePath = s.getFileName();
		FeatureSet fs = null;
		if(sampleFilePath != null) {
			File featureFile = new File(sampleFilePath + ".features");
			if(featureFile.exists()) {
//				try {
					fs = new FeatureSet(featureFile);
//				} catch(Exception e) {
//					fs = null;
//				}
			}
		}
		return fs;
	}

	/**
	 * Instantiates a new empty FeatureSet.
	 */
	public FeatureSet() {
		tracks = new Hashtable<String, FeatureTrack>();
		globalFeatures = new Hashtable<String, Object>();
	}
	
	/**
	 * Instantiates a new FeatureSet from the given file.
	 * 
	 * @param file the File.
	 */
	public FeatureSet(File file) {
		this();
		read(file);
	}
	
	/**
	 * Gets the {@link FeatureTrack} with the given name.
	 * 
	 * @param trackName the track name.
	 * 
	 * @return the FeatureTrack, or null if unsuccessful.
	 */
	public FeatureTrack get(String trackName) {
		return tracks.get(trackName);
	}
	
	/**
	 * Adds the given {@link FeatureTrack} with the given name, writing over a previously stored {@link FeatureTrack} with the same name.
	 * 
	 * @param trackName the track name.
	 * @param track the track.
	 */
	public void add(String trackName, FeatureTrack track) {
		tracks.put(trackName, track);
	}
	
	/**
	 * Returns true if this FeatureSet stores a track with the given name.
	 * 
	 * @param trackName name to check.
	 * @return true if track name is found.
	 */
	public boolean contains(String trackName) {
		return tracks.containsKey(trackName);
	}

	
	/**
	 * Adds a set of features with the given name to the global features.
	 * 
	 * @param s the name used to identify the feature set.
	 * @param f the features.
	 */
	public void addGlobal(String s, Object f) {
		globalFeatures.put(s, f);
	}
	
	/**
	 * Gets the global features for the given name.
	 * 
	 * @param s the name.
	 * 
	 * @return the features.
	 */
	public Object getGlobal(String s) {
		return globalFeatures.get(s);
	}
	
	/**
	 * Returns true if this FeatureSet stores a global feature with the given name.
	 * 
	 * @param feature name to check.
	 * @return true if feature name is found.
	 */
	public boolean containsGlobal(String s) {
		return globalFeatures.containsKey(s);
	}
	
	/**
	 * Writes to a file. Assumes file has already been specified by {@link write(File)} or {@link new FeatureSet(File)}.
	 */
	public void write() {
		try {
			FileOutputStream fos = new FileOutputStream(file);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(tracks);
			oos.writeObject(globalFeatures);
			oos.close();
			fos.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Reads data from the given file. Retains file ref for future use.
	 * 
	 * @param file the file
	 */
	private void read(File file) {
		this.file = file;
		read();
	}
	
	/**
	 * Reads data from given file. Assumes file ref already exists.
	 */
	@SuppressWarnings("unchecked")
	private void read() {
		if(file.exists()) {
			try {
				FileInputStream fis = new FileInputStream(file);
				ObjectInputStream ois = new ObjectInputStream(fis);
				tracks = (Hashtable<String, FeatureTrack>)ois.readObject();
				globalFeatures = (Hashtable<String, Object>)ois.readObject();
				ois.close();
				fis.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Rereads the data from the stored file. Assumes the file has been specified in the constructor.
	 */
	public void refresh() {
		read();
	}
	
	/**
	 * Writes the data to the given {@link File} and keeps the file ref for future use.
	 * 
	 * @param file the file.
	 */
	public void write(File file) {
		this.file = file;
		write();
	}
	
	/**
	 * Writes the data to the named file, and keeps a file ref for future use.
	 * 
	 * @param fn the file name.
	 */
	public void write(String fn) {
		write(new File(fn));
	}
	
	/**
	 * Returns the tracks.
	 * @return A Map<String, FeatureTrack> structure.
	 */
	public Map<String, FeatureTrack> tracks() {
		return tracks;
	}

	public void printGlobalFeatures() {
		System.out.println("Features for " + this + ":");
		for(String s : globalFeatures.keySet()) {
			System.out.println("- " + s);
		}
	}
	
}
