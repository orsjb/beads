/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data;

/**
 * Encapsulates data about audio format for Samples. 
 * 
 * We have elected to use our own AudioFormat instead of 
 * javax.sound.sampled.AudioFormat as javasound is not supported everywhere.
 * 
 * @author ben
 */
public class SampleAudioFormat {
	
	public final int channels, bitDepth;
	public final float sampleRate;
	public final boolean bigEndian, signed;
	
	public SampleAudioFormat(float sampleRate, int bitDepth, int channels, boolean signed, boolean bigEndian) {
		this.sampleRate = sampleRate;
		this.bitDepth = bitDepth;
		this.signed = signed;
		this.bigEndian = bigEndian;
		this.channels = channels;
	}

	public SampleAudioFormat(float sampleRate, int bitDepth, int channels) {
		this(sampleRate, bitDepth, channels, true, true);
	}
	
}
