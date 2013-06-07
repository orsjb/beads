/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.analysis;

import java.util.Hashtable;
import java.util.List;

import net.beadsproject.beads.analysis.featureextractors.FFT;
import net.beadsproject.beads.analysis.featureextractors.Frequency;
import net.beadsproject.beads.analysis.featureextractors.MFCC;
import net.beadsproject.beads.analysis.featureextractors.MelSpectrum;
import net.beadsproject.beads.analysis.featureextractors.PeakDetector;
import net.beadsproject.beads.analysis.featureextractors.Power;
import net.beadsproject.beads.analysis.featureextractors.PowerSpectrum;
import net.beadsproject.beads.analysis.featureextractors.SpectralCentroid;
import net.beadsproject.beads.analysis.featureextractors.SpectralDifference;
import net.beadsproject.beads.analysis.featureextractors.SpectralPeaks;
import net.beadsproject.beads.analysis.featureextractors.SpectralDifference.DifferenceType;
import net.beadsproject.beads.analysis.segmenters.ShortFrameSegmenter;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * The Class Analyzer sets up a quick'n'easy audio analysis chain which can then be
 * plugged into a realtime or non-realtime audio stream. Tell the Analyzer what features
 * you want to extract using a list of classes, corresponding to the FeatureExtractors
 * you want. You can then add a SegmentListener to the Analyzer to monitor new features.
 * 
 * Analyzer is a work in progress.
 */
public class Analyzer implements SegmentMaker {

	/**
	 * The Class AnalysisSettings.
	 */
	public static class AnalysisSettings {
		
		/** The hop size. */
		int hopSize;
		
		/** The chunk size. */
		int chunkSize;

		public AnalysisSettings(int hopSize, int chunkSize) {
			super();
			this.hopSize = hopSize;
			this.chunkSize = chunkSize;
		}

		public AnalysisSettings() {
			super();
		}
		
		
	}
	
	/** The default settings. */
	private static AnalysisSettings defaultSettings;
	static {
		defaultSettings = new AnalysisSettings();
		defaultSettings.hopSize = 512;
		defaultSettings.chunkSize = 1024;
	}
	
	/** The sfs. */
	private ShortFrameSegmenter sfs;
	
	/** The results. */
	private FeatureSet results;
	
	/** The set of all extractor data. */
	private Hashtable<Class<?>, Object> extractorArrangement;
	
	/** The thing responsible for sending messages about beats. */
	private SegmentMaker beatSegmentMaker;
	
	/**
	 * Instantiates a new analyzer.
	 *
	 * @param ac the ac
	 * @param extractors the extractors
	 */
	public Analyzer(AudioContext ac, List<Class<? extends FeatureExtractor<?,?>>> extractors) {
		this(ac, extractors, defaultSettings);
	}
	
	/**
	 * Instantiates a new analyzer.
	 *
	 * @param ac the ac
	 * @param extractors the extractors
	 * @param settings the settings
	 */
	public Analyzer(AudioContext ac, List<Class<? extends FeatureExtractor<?,?>>> extractors, AnalysisSettings settings) {
		setup(ac, extractors, settings);
	}
	
	/* (non-Javadoc)
	 * @see net.beadsproject.beads.analysis.SegmentMaker#addSegmentListener(net.beadsproject.beads.analysis.SegmentListener)
	 */
	public void addSegmentListener(SegmentListener sl) {
		sfs.addSegmentListener(sl);
	}

	/* (non-Javadoc)
	 * @see net.beadsproject.beads.analysis.SegmentMaker#removeSegmentListener(net.beadsproject.beads.analysis.SegmentListener)
	 */
	public void removeSegmentListener(SegmentListener sl) {
		sfs.removeSegmentListener(sl);
	}
	
	/**
	 * Adds a {#link SegmentListener} which will listen to the beats detected by this Analyzer.
	 * @param sl a SegmentListener.
	 */
	public void addBeatListener(SegmentListener sl) {
		beatSegmentMaker.addSegmentListener(sl);
	}
	
	/**
	 * Removes the {#link SegmentListener} from listening to the beats detected by this Analyzer.
	 * @param sl a SegmentListener.
	 */
	public void removeBeatListener(SegmentListener sl) {
		beatSegmentMaker.removeSegmentListener(sl);
	}
	
	/**
	 * Listen to this input ugen.
	 *
	 * @param ugen the ugen
	 */
	public void listenTo(UGen ugen) {
		sfs.addInput(0, ugen, 0);
	}
	
	/**
	 * Update from this source ugen.
	 * 
	 * @param ugen
	 */
	public void updateFrom(UGen ugen) {
		ugen.addDependent(sfs);
	}
	
	/**
	 * Gets the last low level frame.
	 *
	 * @return the last low level frame
	 */
	public FeatureFrame getLastLowLevelFrame() {
		return results.get("Low Level").getLastFrame();
	}
	
	/**
	 * Gets the last beat frame.
	 *
	 * @return the last beat frame
	 */
	public FeatureFrame getLastBeatFrame() {
		return results.get("Beats").getLastFrame();
	}
	
	/**
	 * Gets the extractor or other element of the given class type.
	 * @param classID
	 * @return
	 */
	public Object getElement(Class<?> classID) {
		return extractorArrangement.get(classID);
	}
	
	/**
	 * Gets the results from the analysis, which is a {@link FeatureSet} containing feature
	 * tracks: "Low Level" for low level features and "Beat" for beat level features.
	 * @return the results set.
	 */
	public FeatureSet getResults() {
		return results;
	}
	
	/**
	 * Setup.
	 *
	 * @param ac the ac
	 * @param extractors the extractors
	 * @param settings the settings
	 */
	@SuppressWarnings("unchecked")
	private void setup(AudioContext ac, List<Class<? extends FeatureExtractor<?,?>>> extractors, AnalysisSettings settings) {
		results = new FeatureSet();
		FeatureTrack lowLevel = new FeatureTrack();
		FeatureTrack beats = new FeatureTrack();
		results.add("Low Level", lowLevel);
		results.add("Beats", beats);
		extractorArrangement = new Hashtable<Class<?>, Object>();
		extractorArrangement.put(AudioContext.class, ac);
		//set up call chain
		sfs = new ShortFrameSegmenter(ac);
		sfs.setChunkSize(settings.chunkSize);
		sfs.setHopSize(settings.hopSize);
		sfs.addSegmentListener(lowLevel);
		extractorArrangement.put(AudioSegmenter.class, sfs);
		if(extractors != null) {
			for(Class extractor : extractors) {
				if(extractor.equals(PowerSpectrum.class)) {
					powerSpectrum(extractorArrangement);
				} else if(extractor.equals(FFT.class)) {
					fft(extractorArrangement);
				} else if(extractor.equals(Frequency.class)) {
					frequency(extractorArrangement);
				} else if(extractor.equals(MelSpectrum.class)) {
					melSpectrum(extractorArrangement);
				} else if(extractor.equals(MFCC.class)) {
					mfcc(extractorArrangement);
				} else if(extractor.equals(SpectralPeaks.class)) {
					spectralPeaks(extractorArrangement);
				} else if(extractor.equals(Power.class)) {
					power(extractorArrangement);
				} else if(extractor.equals(SpectralCentroid.class)) {
					spectralCentroid(extractorArrangement);
				} else {
					System.err.println("Analyzer: unknown extractor class: " + extractor);
				}
			}
		}
		//inisit on spectral diff
		spectralDifference(extractorArrangement);
		//add low level stuff
		for(Class featureName : extractorArrangement.keySet()) {
			if(extractorArrangement.get(featureName) instanceof FeatureExtractor) {
				lowLevel.addFeatureExtractor((FeatureExtractor)extractorArrangement.get(featureName));
			}
		}
		//add beat stuff
		PeakDetector d = new PeakDetector();
		beatSegmentMaker = d;
		d.setThreshold(0.1f);
		d.setAlpha(0.9f);
		d.setResetDelay(200f);
		SpectralDifference sd = (SpectralDifference)extractorArrangement.get(SpectralDifference.class);
		sd.addListener(d);
		sd.setDifferenceType(DifferenceType.POSITIVEMEANDIFFERENCE);
		d.addSegmentListener(beats);
	}
	
	/**
	 * Sets the frame memory for FeatureTracks stored by this Analyzer. If unset, or
	 * set to -1, the number of frames stored is unlimited, which is likely to lead to
	 * intensive memory use. When set, only the specified number of most recent frames
	 * are stored. 
	 * 
	 * @param fm number of FeatureFrames stored, -1 for unlimited.
	 */
	public void setFrameMemory(int fm) {
		for(FeatureTrack ft : results.tracks().values()) {
			ft.setFrameMemory(fm);
		}
	}

	
	/**
	 * Spectral peaks.
	 *
	 * @param extractorArrangement the extractor arrangement
	 */
	private static void spectralPeaks(Hashtable<Class<?>, Object> extractorArrangement) {
		if(!extractorArrangement.containsKey(SpectralPeaks.class)) {
			powerSpectrum(extractorArrangement);
			AudioContext ac = (AudioContext)extractorArrangement.get(AudioContext.class);
			SpectralPeaks sp = new SpectralPeaks(ac, 10);
			PowerSpectrum ps = (PowerSpectrum)extractorArrangement.get(PowerSpectrum.class);
			ps.addListener(sp);
			extractorArrangement.put(SpectralPeaks.class, sp);
		}
	}
	
	/**
	 * Spectral difference.
	 *
	 * @param extractorArrangement the extractor arrangement
	 */
	private static void spectralDifference(Hashtable<Class<?>, Object> extractorArrangement) {
		if(!extractorArrangement.containsKey(SpectralDifference.class)) {
			powerSpectrum(extractorArrangement);
			AudioContext ac = (AudioContext)extractorArrangement.get(AudioContext.class);
			SpectralDifference sd = new SpectralDifference(ac.getSampleRate());
			sd.setDifferenceType(DifferenceType.POSITIVERMS);
			PowerSpectrum ps = (PowerSpectrum)extractorArrangement.get(PowerSpectrum.class);
			ps.addListener(sd);
			extractorArrangement.put(SpectralDifference.class, sd);
		}
	}
	
	/**
	 * Mfcc.
	 *
	 * @param extractorArrangement the extractor arrangement
	 */
	private static void mfcc(Hashtable<Class<?>, Object> extractorArrangement) {
		if(!extractorArrangement.containsKey(MFCC.class)) {
			melSpectrum(extractorArrangement);
			MFCC mfcc = new MFCC(20);
			MelSpectrum ms = (MelSpectrum)extractorArrangement.get(MelSpectrum.class);
			ms.addListener(mfcc);
			extractorArrangement.put(MFCC.class, mfcc);
		}
	}
	
	/**
	 * Mel spectrum.
	 *
	 * @param extractorArrangement the extractor arrangement
	 */
	private static void melSpectrum(Hashtable<Class<?>, Object> extractorArrangement) {
		if(!extractorArrangement.containsKey(MelSpectrum.class)) {
			powerSpectrum(extractorArrangement);
			AudioContext ac = (AudioContext)extractorArrangement.get(AudioContext.class);
			MelSpectrum ms = new MelSpectrum(ac.getSampleRate(), 200);
			PowerSpectrum ps = (PowerSpectrum)extractorArrangement.get(PowerSpectrum.class);
			ps.addListener(ms);
			extractorArrangement.put(MelSpectrum.class, ms);
		}
	}
	
	/**
	 * Frequency.
	 *
	 * @param extractorArrangement the extractor arrangement
	 */
	private static void frequency(Hashtable<Class<?>, Object> extractorArrangement) {
		if(!extractorArrangement.containsKey(Frequency.class)) {
			powerSpectrum(extractorArrangement);
			AudioContext ac = (AudioContext)extractorArrangement.get(AudioContext.class);
			Frequency f = new Frequency(ac.getSampleRate());
			PowerSpectrum ps = (PowerSpectrum)extractorArrangement.get(PowerSpectrum.class);
			ps.addListener(f);
			extractorArrangement.put(Frequency.class, f);
		}
	}
	
	/**
	 * SpectralCentroid.
	 *
	 * @param extractorArrangement the extractor arrangement
	 */
	private static void spectralCentroid(Hashtable<Class<?>, Object> extractorArrangement) {
		if(!extractorArrangement.containsKey(SpectralCentroid.class)) {
			powerSpectrum(extractorArrangement);
			AudioContext ac = (AudioContext)extractorArrangement.get(AudioContext.class);
			SpectralCentroid sc = new SpectralCentroid(ac.getSampleRate());
			PowerSpectrum ps = (PowerSpectrum)extractorArrangement.get(PowerSpectrum.class);
			ps.addListener(sc);
			extractorArrangement.put(SpectralCentroid.class, sc);
		}
	}
	
	/**
	 * Power spectrum.
	 *
	 * @param extractorArrangement the extractor arrangement
	 */
	private static void powerSpectrum(Hashtable<Class<?>, Object> extractorArrangement) {
		if(!extractorArrangement.containsKey(PowerSpectrum.class)) {
			fft(extractorArrangement);
			PowerSpectrum ps = new PowerSpectrum();
			FFT fft = (FFT)extractorArrangement.get(FFT.class);
			fft.addListener(ps);
			extractorArrangement.put(PowerSpectrum.class, ps);
		}
	}
	
	/**
	 * Fft.
	 *
	 * @param extractorArrangement the extractor arrangement
	 */
	private static void fft(Hashtable<Class<?>, Object> extractorArrangement) {
		if(!extractorArrangement.containsKey(FFT.class)) {
			FFT fft = new FFT();
			AudioSegmenter as = (AudioSegmenter)extractorArrangement.get(AudioSegmenter.class);
			as.addListener(fft);
			extractorArrangement.put(FFT.class, fft);
		}
	}
	
	/**
	 * Power.
	 *
	 * @param extractorArrangement the extractor arrangement
	 */
	private static void power(Hashtable<Class<?>, Object> extractorArrangement) {
		if(!extractorArrangement.containsKey(Power.class)) {
			Power p = new Power();
			AudioSegmenter as = (AudioSegmenter)extractorArrangement.get(AudioSegmenter.class);
			as.addListener(p);
			extractorArrangement.put(Power.class, p);
		}
	}
}
