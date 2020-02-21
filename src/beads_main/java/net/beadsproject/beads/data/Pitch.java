/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data;

/**
 * A set of static fields and utility methods associated with pitch.
 *
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

	/**
	 * Calculate the MIDI note that would be required given the tonic of a scale, the numbers
	 * in the scale relative to the tonic, and the index using zero based reference
	 * For example, with Midi Note C3 (60), using the Major scale, and scaleIndex of 8 (being major 9th)
	 * we would get 60 + 12 + 2 =  74 (D4)
	 * @param scaleTonic The note number that would be the zero index of the scale
	 * @param scaleData An array of notes for which to define our scale
	 * @param scaleIndex The index within the scale to find the value to add
	 * @return The scaleTonic + scaleIndex in the index. If scaleIndex is outside the range of array
	 * eg (scaleIndex &lt; 0 || scaleIndex &gt;= scaleData.length) then modulo mathematics will be used to
	 * return note from next or previous register
	 */
    public static int getRelativeMidiNote (int scaleTonic, int[] scaleData, int scaleIndex){
		int ret =  scaleTonic; // this will be if a scaleIndex is zero

		// Note our comment text will assume scaleData is Picth.major for ease of understanding
		if (scaleIndex > 0){
			// get the scale degree from the scale
			// eg, if scaleIndex is 9, scale_degree = 9 % 7 = 2
			int scale_degree = scaleIndex % scaleData.length;

			// If our scale pitch is 2,Pitch.major[2] = 4 (major third)
			int scale_pitch = scaleData[scale_degree];

			// Now get the register of our note
			// eg, if scaleIndex is 9, note_register = 9 / 7 = 1
			int note_register = scaleIndex / scaleData.length;

			// we multiply our register x 12 because that is an octave in MIDI
			// if scaleIndex is 9 then 1 x 12 + 4 = 16
			int note_pitch = note_register * 12 + scale_pitch;

			// add the number to our base tonic to get the note based on key. Assume our scaleTonic is C2 (48)
			// if scaleIndex is 9 then 48 + 16 = 64. This is E3 in MIDI
			ret = scaleTonic + note_pitch;

		}
		else if (scaleIndex < 0){
			int negative_index = scaleIndex * -1;

			// get the scale degree from the scale we need to reduce by
			// eg, if scaleIndex is -9, scale_degree = 9 % 7 = 2 (we got absoly=te value of scaleIndex)
			// We need to get index below this figure = eg, 2 array index points from end
			int scale_degree = negative_index % scaleData.length;

			// If our scale scale_degree is 2, Pitch.major[7-2] = 9 (major sixth)
			int scale_pitch = scaleData[(scaleData.length - scale_degree) % scaleData.length];

			// Now get the register of our note
			// eg, if scaleIndex is -9, note_register = -9 / 7 = -1
			int note_register = scaleIndex / scaleData.length;

			// if it is not exactly an octave, we need to calculate our range from and extra register below
			if (scale_pitch != 0){
				// if scaleIndex is -9 then note_register =  -2
				note_register -= 1;
			}
			// we multiply our register x 12 because that is an octave in MIDI
			// if scaleIndex is -9 then -2 x 12 + 9 = -15
			int note_pitch = note_register * 12 + scale_pitch;

			// add the number to our base tonic to get the note based on key. Assume our Base Note C3 (60)
			// if scaleIndex is -9 then 60 - 15 = 45. This is A1 in MIDI
			ret = scaleTonic + note_pitch;
		}

		return ret;
	}

	/**
	 * Shift the Pitch based on the number of semitones
	 * @param frequency The frequency we want shifted
	 * @param midiDegree the number of semitones we want to shift
	 * @return the frequency value shifted by the number of semitones
	 */
	public static double shiftPitch (float frequency, int midiDegree){
		final double SEMITONE_CONSTANT =  Math.pow(2 , 1f/12f);
		return Math.pow(SEMITONE_CONSTANT, midiDegree) * frequency;

	}
}
