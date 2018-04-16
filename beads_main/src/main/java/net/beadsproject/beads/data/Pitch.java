/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data;

/**
 * A set of static fields and utility methods associated with pitch.
 * 
 * @beads.category data
 * @author ollie
 */
public abstract class Pitch {
	
	/** The constant log(2) = 0.6931472. */
	public final static float LOG2 = 0.6931472f;
    
	/**
	 * Convert frequency to MIDI note number.
	 * 
	 * @param frequency
	 *            the required frequency.
	 * 
	 * @return the resulting MIDI note number.
	 */
	public static final float ftom(float frequency) {
        return Math.max(0f, (float)Math.log(frequency / 440.0f) / LOG2 * 12f + 69f);
    }
    
	/**
	 * Convert MIDI note number to frequency.
	 * 
	 * @param midi
	 *            the required MIDI note number.
	 * 
	 * @return the resulting frequency.
	 */
	public static final float mtof(float midi) {
        return 440.0f * (float)Math.pow(2.0f, (midi - 69f) / 12.0f);
    }
	
	/**
	 * Takes a pitch and returns that pitch adjusted downwards to the nearest pitch in the given scale.
	 * 
	 * @param pitch the pitch to modify.
	 * @param scale the scale to use.
	 * @param notesPerOctave how many notes in your octave (12 if you're not sure).
	 * @return adjusted pitch.
	 */
	public static final int forceToScale(int pitch, int[] scale, int notesPerOctave) {
		int pitchClass = pitch % notesPerOctave;
		int register = pitch / notesPerOctave;
		int newPitchClass = -1;
		for(int i = scale.length - 1; i >= 0; i--) {
			if(pitchClass >= scale[i]) {
				newPitchClass = scale[i];
				break;
			}
		}
		if(newPitchClass == -1) {
			newPitchClass = pitchClass;
		}
		return register * notesPerOctave + newPitchClass;
	}
	
	/**
	 * Takes a pitch and returns that pitch adjusted downwards to the nearest pitch in the given scale. Assumes 12 pitches per octave.
	 * 
	 * @param pitch the pitch to modify.
	 * @param scale the scale to use.
	 * @return adjusted pitch.
	 */
	public static final int forceToScale(int pitch, int[] scale) {
		return forceToScale(pitch, scale, 12);
	}

	public static final float forceFrequencyToScale(float freq, int[] scale) {
		return mtof(forceToScale((int)ftom(freq), scale));
	}
	
	/** Pitch names for scale starting at C. */
	public static final String[] pitchNames = new String[]{"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"};
	
	/** The dorian scale relative to root. */
	public static final int[] dorian = {0, 2, 3, 5, 7, 9, 10};

	/** The major scale relative to root. */
	public static final int[] major = {0, 2, 4, 5, 7, 9, 11};
	
	/** The minor scale relative to root. */
	public static final int[] minor = {0, 2, 3, 5, 7, 8, 10};
    
    /** The circle of fifths relative to root. */
    public static final int[] circleOfFifths = {0, 5, 10, 3, 8, 1, 6, 11, 4, 9, 2, 7};

    /** Pentatonic. */
    public static final int[] pentatonic = {0, 2, 4, 7, 9};

}
