/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * SampleManager provides a static repository for {@link Sample} data and provides methods to organise samples into groups.
 * 
 * @beads.category data
 */
public class SampleManager {
	
	/** List of all Samples, indexed by name. */
	private final static Map<String, Sample> samples = new TreeMap<String, Sample>();
	
	/** List of Sample groups, indexed by group name. */
	private final static Map<String, ArrayList<Sample>> groups = new TreeMap<String, ArrayList<Sample>>();
	
	/** List of group names mapped to group directories, groups only in this list if from same directory. */
	private final static Map<String, String> groupDirs = new TreeMap<String, String>();

	private final static Set<SampleGroupListener> listeners = new HashSet<SampleGroupListener>();
	
	private static boolean verbose = true;
	
	/**
	 * Returns a new Sample from the given filename. If the Sample has already
	 * been loaded, it will not be loaded again, but will simply be retrieved
	 * from the static repository.
	 * 
	 * @param fn the file path.
	 * 
	 * @return the sample.
	 */
	public static Sample sample(String fn) {
		return sample(fn, fn);
	}

	/**
	 * Adds a sample by name to the sample list. This lets you load samples with a different buffering regime.
	 * @param name
	 * @param sample
	 */
	public static void sample(String name, Sample sample) {
		if (samples.get(name) == null) {
			samples.put(name, sample);
			if(sample.getSimpleName() == null) {
				sample.setSimpleName(name);
			}
		}
	}	
	
	/**
	 * Like {@link SampleManager#sample(String)} but with the option to specify the name with which this {@link Sample} is indexed.
	 * 
	 * @param ref the name with which to index this Sample.
	 * @param fn the file path.
	 * 
	 * @return the sample.
	 */
	public static Sample sample(String ref, String fn) {
		Sample sample = samples.get(ref);
		if (sample == null) {
			try {
				sample = new Sample(fn);
				samples.put(ref, sample);
				if(verbose) System.out.println("Loaded " + fn);
			} catch (Exception e) {
				//swallow exception
			}
		}
		return sample;
	}

	/**
	 * Generates a new group with the given group name and list of Samples to be
	 * added to the group.
	 * 
	 * @param groupName the group name.
	 * @param sampleList the sample list.
	 */
	public static List<Sample> group(String groupName, Sample[] sampleList) {
		ArrayList<Sample> group;
		if (!groups.keySet().contains(groupName)) {
			group = new ArrayList<Sample>();
			groups.put(groupName, group);
		} else {
			group = groups.get(groupName);
		}
		for (int i = 0; i < sampleList.length; i++) {
			if (!group.contains(sampleList[i])) {
				group.add(sampleList[i]);
			}
		}
		for(SampleGroupListener l : listeners) {
			l.changed(groupName);
		}
		return group;
	}

	/**
	 * Generates a new group with the given group name and a string that
	 * specifies where to load samples to be added to the group. The string is interpreted firstly as a URL, and if that fails, as a folder path.
	 * 
	 * @param groupName the group name.
	 * @param folderName the folder address (URL or file path).
	 */
	public static List<Sample> group(String groupName, String folderName) {
		return group(groupName, folderName, Integer.MAX_VALUE);
	}
	
	/**
	 * Generates a new group with the given group name and a string that
	 * specifies where to load samples to be added to the group, and also limits the number of items loaded from the folder to maxItems. The string is interpreted firstly as a URL, and if that fails, as a folder path.
	 * 
	 * @param groupName the group name.
	 * @param folderName the folder address (URL or file path).
	 * @param maxItems number of items to limit to.
	 */
	public static List<Sample> group(String groupName, String folderName, int maxItems) {
		//first try interpreting the folderName as a system resource
		File theDirectory = null;
		try {
			URL url = ClassLoader.getSystemResource(folderName);
			if(url != null) {
				theDirectory = new File(URLDecoder.decode(url.getPath(), "UTF-8"));
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		//failing that, try it as a plain file path
		if(theDirectory == null || !theDirectory.exists()) {
			theDirectory = new File(folderName);
		}
		groupDirs.put(groupName, theDirectory.getAbsolutePath());
		String[] fileNameList = theDirectory.list();
		for (int i = 0; i < fileNameList.length; i++) {
			String absFileName = theDirectory.getAbsolutePath() + "/" + fileNameList[i];
			if(new File(absFileName).exists()) {
				fileNameList[i] = absFileName;
			}
			
		}
		return group(groupName, fileNameList, maxItems);
	}

	/**
	 * Generates a new group with the given group name and a list of file names
	 * to be added to the group.
	 * 
	 * @param groupName the group name.
	 * @param fileNameList the file name list.
	 */
	public static List<Sample> group(String groupName, String[] fileNameList) {
		return group(groupName, fileNameList, Integer.MAX_VALUE);
	}
	
	/**
	 * Generates a new group with the given group name and a list of file names
	 * to be added to the group, with number of elements loaded limited to maxItems.
	 * 
	 * @param groupName the group name.
	 * @param fileNameList the file name list.
	 * @param maxItems number of items to limit to.
	 */
	public static List<Sample> group(String groupName, String[] fileNameList, int maxItems) {
		ArrayList<Sample> group;
		if (!groups.keySet().contains(groupName)) {
			group = new ArrayList<Sample>();
			groups.put(groupName, group);
		} else
			group = groups.get(groupName);
		int count = 0;
		for (int i = 0; i < fileNameList.length; i++) {
			String simpleName = fileNameList[i];
			try {
				Sample sample = sample(simpleName, fileNameList[i]);
				if (!group.contains(simpleName) && sample != null) {
					if(count++ >= maxItems) break;
					group.add(sample);
				}
			} catch(Exception e) {
				//snuff the exception
			}
		}
		for(SampleGroupListener l : listeners) {
			l.changed(groupName);
		}
		return group;
	}
	
	/**
	 * Add a new Sample to a group. Create the group if it doesn't exist.
	 * @param group the group to add to.
	 * @param sample the Sample to add.
	 */
	public static void addToGroup(String group, Sample sample) {
		ArrayList<Sample> samples;
		if(!groups.containsKey(group)) {
			samples = new ArrayList<Sample>();
			groups.put(group, samples);
		} else {
			samples = groups.get(group);
		}
		if(!samples.contains(sample)) {
			samples.add(sample);
		}
		for(SampleGroupListener l : listeners) {
			l.changed(group);
		}
	}
	

	/**
	 * Add a new list of Samples to the specified group. Create the group if it doesn't exist.
	 * @param group the group to add to.
	 * @param newSamples the list of Samples to add.
	 */
	public static void addToGroup(String group, List<Sample> newSamples) {
		if(newSamples == null) return;
		ArrayList<Sample> samples;
		if(!groups.containsKey(group)) {
			samples = new ArrayList<Sample>();
			groups.put(group, samples);
		} else {
			samples = groups.get(group);
		}
		for(Sample sample : newSamples) {
			if(!samples.contains(sample)) {
				samples.add(sample);
			}
		}
		for(SampleGroupListener l : listeners) {
			l.changed(group);
		}
	}
	
	/**
	 * Gets the set of group names.
	 * @return Set of Strings representing group names.
	 */
	public static Set<String> groups() {
		return groups.keySet();
	}
	
	/**
	 * List the groups by name as a list of Strings.
	 * @return a List of Strings.
	 */
	public static List<String> groupsAsList() {
		return new ArrayList<String>(groups.keySet());
	}

	/**
	 * Gets the specified group in the form ArrayList&lt;Sample&gt;.
	 * 
	 * @param groupName the group name.
	 * 
	 * @return the group.
	 */
	public static ArrayList<Sample> getGroup(String groupName) {
		return groups.get(groupName);
	}
	
	/**
	 * Gets the directory path of the group.
	 * @param groupName
	 * @return directory path.
	 */
	public static String getGroupDir(String groupName) {
		return groupDirs.get(groupName);
	}
	
	/**
	 * Gets a random sample from the specified group.
	 * 
	 * @param groupName the group.
	 * 
	 * @return a random Sample.
	 */
	public static Sample randomFromGroup(String groupName) {
		ArrayList<Sample> group = groups.get(groupName);
		return group.get((int)(Math.random() * group.size()));
	}

	/**
	 * Gets the Sample at the specified index from the specified group. If index is greater than the size of the group
	 * then the value index % sizeOfGroup is used.
	 * 
	 * @param groupName the group name.
	 * @param index the index.
	 * 
	 * @return the Sample.
	 */
	public static Sample fromGroup(String groupName, int index) {
		ArrayList<Sample> group = groups.get(groupName);
		if(group == null || group.size() == 0) {
			return null;
		}
		return group.get(index % group.size());
	}
	
	/**
	 * Removes the named {@link Sample}.
	 * 
	 * @param sampleName the sample name.
	 */
	public static void removeSample(String sampleName) {
		if(samples.containsKey(sampleName)) {
			samples.remove(sampleName);
		}
	}

	/**
	 * Removes the {@link Sample}.
	 * 
	 * @param sample the Sample.
	 */
	public static void removeSample(Sample sample) {
		for (String str : samples.keySet()) {
			if (samples.get(str).equals(sample)) {
				removeSample(str);
				break;
			}
		}
	}

	/**
	 * Removes the specified group, without removing the samples.
	 * 
	 * @param groupName the group name.
	 */
	public static void removeGroup(String groupName) {
		groups.remove(groupName);
		groupDirs.remove(groupName);
		for(SampleGroupListener l : listeners) {
			l.changed(groupName);
		}
	}

	/**
	 * Removes the specified group, and removes all of the samples found in the
	 * group from the sample repository.
	 * 
	 * @param groupName the group name.
	 */
	public static void destroyGroup(String groupName) {
		ArrayList<Sample> group = groups.get(groupName);
		for (int i = 0; i < group.size(); i++) {
			removeSample(group.get(i));
		}
		removeGroup(groupName);
	}

	public static void addGroupListener(SampleGroupListener l) {
		listeners.add(l);
	}
	
	public static void removeGroupListener(SampleGroupListener l) {
		listeners.remove(l);
	}
	
	/**
	 * Prints a list of all {@link Sample}s to System.out.
	 */
	public static void printSampleList() {
		for(String s : samples.keySet()) {
			System.out.println(s + " " + samples.get(s));
		}
	}
	
	/**
	 * Returns an ArrayList containing all of the Sample names.
	 * 
	 * @return ArrayList of Sample names.
	 */
	public static List<String> getSampleNameList() {
		return new ArrayList<String>(samples.keySet());
	}

	/**
	 * Determines if SampleManager is being verbose.
	 * 
	 * @return true if verbose.
	 */
	public static boolean isVerbose() {
		return verbose;
	}

	/**
	 * Tells SampleManager to produce verbose output.
	 * 
	 * @param verbose true for verbose output.
	 */
	public static void setVerbose(boolean verbose) {
		SampleManager.verbose = verbose;
	}
	
	/**
	 * Creates a text file at the specified path containing a list of all of the 
	 * file names of {@link Sample}s loaded so far. Useful if you need to gather 
	 * all of your sample data into one place using a script. Puts all file names in
	 * double quotes.
	 * 
	 * @param toFile destination of the file.
	 */
	public static void logSamplePaths(String toFile) {
		try {
			File f = new File(toFile);
			PrintWriter out = new PrintWriter(f);
			for(Sample s : samples.values()) {
				if(s.getFileName() != null && !s.getFileName().equals("")) {
					out.println("\"" + s.getFileName() + "\"");
				}
			}
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Takes all of the {@link Sample}s currently stored and attempts to relocate
	 * them so that their position relative to destRootDir duplicates their position
	 * relative to sourceRootDir. This only applies to {@link Sample}s that were loaded
	 * from a position below sourceRootDir.
	 * @param sourceRootDir root that you want to transfer from
	 * @param destRootDir place you want to transfer to
	 * @param force set to true to force overwrite existing files - take care!
	 */
	public static void transferSamples(String sourceRootDir, String destRootDir, boolean force) {
		File srd = new File(sourceRootDir);
		if(!srd.exists()) return;
		sourceRootDir = srd.getAbsolutePath();
		File drd = new File(destRootDir);
		if(!drd.exists()) drd.mkdir();
		destRootDir = drd.getAbsolutePath();
		for(Sample s : samples.values()) {
			if(s.getFileName() != null && !s.getFileName().equals("")) {
				String absFileName = s.getFileName();
				if(absFileName.startsWith(sourceRootDir)) {
					String destAbsFileName = absFileName.replace(sourceRootDir, destRootDir);
					if(force || !new File(destAbsFileName).exists()) {
						try {
							File parent = new File(destAbsFileName).getParentFile();
							if(!parent.exists()) parent.mkdirs();
							s.write(destAbsFileName);
							System.out.println("Copied file \"" + absFileName + "\" to \"" + destAbsFileName + "\"");
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	/**
	 * Interface for notificaiton of changes to a group. Add yourself to listen to 
	 * group changes using {@link SampleManager#addGroupListener(SampleGroupListener)}.
	 * 
	 * @author ollie
	 *
	 */
	public static interface SampleGroupListener {
		
		/**
		 * Called when {@link SampleManager} makes changes to a group.
		 * @param group the name of the affected group.
		 */
		public void changed(String group);
	}
	
	
}
