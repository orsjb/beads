/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.analysis;

import java.util.ArrayList;

import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.core.TimeStamp;

/**
 * FeatureExtractor is an abstract base class for classes that perform some kind of analysis 
 * on incoming data. Both the incoming data (P) and the generated data (R) are generic types. 
 * Implementing classes use the method {@link #process} to process data. 
 * 
 * @author ollie
 */
public abstract class FeatureExtractor<R, P> extends Bead {
	
	/** The number of features. */
	protected int numFeatures;
	
	/** The current feature data. */
	protected R features = null;
	
	/** The name of the FeatureExtractor. */
	protected String name;
	
	/** An array of Strings providing descriptions of the feature data. */
	protected String[] featureDescriptions;
	
	/** A set of FeatureExtractors that this FeatureExtractor forwards its feature data to. */
	private ArrayList<FeatureExtractor<?, R>> featureExtractorListeners;
	
	/**
	 * Instantiates a new FeatureExtractor. This constructor names the FeatureExtractor with the name of the implementing class.
	 */
	public FeatureExtractor() {
		name = getClass().getSimpleName();
		featureExtractorListeners = new ArrayList<FeatureExtractor<?, R>>();
	}
	
	/**
	 * Process some data of type P (specified by the class def). This method must be overidden by implementing classes.
	 * 
	 * @param data the data.
	 */
	public abstract void process(TimeStamp startTime, TimeStamp endTime, P data);
	
	/**
	 * Subclasses should call this at end of their process() method to forward features to listeners.
	 */
	public void forward(TimeStamp startTime, TimeStamp endTime) {
		//forward to the feature extractor listeners
		for(FeatureExtractor<?, R> fe : featureExtractorListeners) {
			fe.process(startTime, endTime, features);
		}
	}
	
	/**
	 * Adds a FeatureExtractor to listen to this FeatureExtractor. 
	 * @param listener the FeatureExtractor that listens to this one.
	 */
	public void addListener(FeatureExtractor<?, R> listener) {
		featureExtractorListeners.add(listener);
	}
	
	/**
	 * Removes a FeatureExtractor from the list of listeners.
	 * @param listener the FeatureExtractor to remove.
	 */
	public void removeListener(FeatureExtractor<?, R> listener) {
		featureExtractorListeners.remove(listener);
	}
	
	/**
	 * Gets the current features of type R, specified in the class def.
	 * 
	 * @return the features.
	 */
	public R getFeatures() {
		return features;
	}
	
	/**
	 * Gets the number of features.
	 * 
	 * @return the number of features.
	 */
	public int getNumberOfFeatures() {
		return numFeatures;
	}
	
	/**
	 * Sets the number of features.
	 * 
	 * @param numFeatures the new number of features.
	 */
	public void setNumberOfFeatures(int numFeatures) {
		this.numFeatures = numFeatures;
	}
	
	/**
	 * Sets the name.
	 * 
	 * @param name the new name.
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Gets the name.
	 * 
	 * @return the name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the feature descriptions. Implementing classes should make sure that this array has meaningful content.
	 * 
	 * @return the feature descriptions.
	 */
	public String[] getFeatureDescriptions() {
		return featureDescriptions;
	}

}

