package net.beadsproject.beads.play;

import java.beans.XMLDecoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.TreeMap;

public class Music {
	
	/**
	 * A set of named lists of SoundEvents.
	 */
	public final static Map<String, Kit> kits = new TreeMap<String, Kit>();
	
	/**
	 * A set of named Patterns.
	 */
	public final static Map<String, Pattern> patterns = new TreeMap<String, Pattern>();

	public static void loadKit(String name, String path) {
		try {
			FileInputStream fis = new FileInputStream(new File(path));
			XMLDecoder dec = new XMLDecoder(fis);
			kits.put(name, (Kit)dec.readObject());
			dec.close();
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void loadPattern(String name, String path) {
		try {
			FileInputStream fis = new FileInputStream(new File(path));
			ObjectInputStream oos = new ObjectInputStream(fis);
			patterns.put(name, (Pattern)oos.readObject());
			oos.close();
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

