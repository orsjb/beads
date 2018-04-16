/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * A mixer for a speaker config in any number of dimensions (we haven't tested D>3 yet). 
 * Add sources (UGens) and control their locations using other UGens. Locations are changed on a per-channel basis,
 * so that multichannel files can be located in the mixer independently. 
 * 
 * We follow the 'right-handed' ordering of the axes: 
 * http://en.wikipedia.org/wiki/Cartesian_coordinate_system#Orientation_and_handedness
 * 
 * The default speaker numbering is as follows: 
 * 
 * first layout speakers 1-4 on the ground in clockwise order (2D and 3D)
 * then layout speakers 5-8 so that they are above 1-4 respectively. Then according to the 
 * 'right-handed' ordering:
 * the x-axis follows the line joining 1 and 4
 * the y-axis follows the line joining 1 and 2
 * the z-axis follows the line joining 1 and 5
 * 
 * @beads.category utilities
 * @author ollie
 *
 */
public class Spatial extends UGen {

	/**
	 * The Class Location. Stores a source UGen and a set of UGens that control 
	 * the position of each channel of that UGen along each dimension.
	 */
	private class Location {
		
		/** The source UGen. */
		UGen source;
		
		/** The position controllers. */
		UGen[][] pos; 	//pos[chan][dim] - for each output channel of the source, 
						//the position is given by N Glides (p1, p2, p3...).
		/** Does this UGen own it's position? In which case the pos UGen's are Glides. */
		boolean ownsPosition;
		
		/**
		 * Instantiates a new location with the give source UGen.
		 * 
		 * @param source the source.
		 */
		Location(UGen source) {
			this.source = source;
			pos = new UGen[source.getOuts()][dimensions];
			for(int i = 0; i < pos.length; i++) {
				for(int j = 0; j < dimensions; j++) {
					pos[i][j] = new Glide(context, 100f, 5f); //the position is set to be far-far away
				}
			}	
			ownsPosition = true;
		}
		
		/**
		 * Instantiates a new location with the given source UGen and conrollers.
		 * 
		 * @param source the source.
		 * @param controllers the controllers.
		 */
		Location(UGen source, UGen[][] controllers) {
			this.source = source;
			this.pos = controllers;
			ownsPosition = false;
		}
		
		/**
		 * Move.
		 * 
		 * @param channel the channel
		 * @param newPos the new pos
		 */
		void move(int channel, float[] newPos) {
			if(!ownsPosition) return;
			for(int i = 0; i < pos[channel].length; i++) {
				pos[channel][i].setValue(newPos[i]);
			}
		}
		
		/**
		 * Move immediately.
		 * 
		 * @param channel the channel
		 * @param newPos the new pos
		 */
		void moveImmediately(int channel, float[] newPos) {
			if(!ownsPosition) return;
			for(int i = 0; i < pos[channel].length; i++) {
				((Glide)pos[channel][i]).setValueImmediately(newPos[i]);
			}
		}
		
		/**
		 * Mix in audio.
		 * 
		 * @param output the output
		 */
		void mixInAudio(float[][] output) {
			//update the source
			source.update();
			//for each channel
			for(int outputChannel = 0; outputChannel < pos.length; outputChannel++) {
				//first update the glides
				for(int dim = 0; dim < dimensions; dim++) {
					pos[outputChannel][dim].update();
				}
				//at each time step
				for(int time = 0; time < bufferSize; time++) {
					//get current position of this channel using pos[outputChannel][dim].getValue(0, time);
					float[] currentPos = new float[dimensions];
					for(int dim = 0; dim < dimensions; dim++) {
						currentPos[dim] = pos[outputChannel][dim].getValue(0, time);
					}	
					float[] speakerGains = new float[speakerPositions.length];
					//work out speaker gains given current pos (distance of pos from each speaker?)
					for(int speaker = 0; speaker < speakerPositions.length; speaker++) {
						float distance = distance(speakerPositions[speaker], currentPos);
						float linearGain = Math.max(0, 1f - distance / sphereDiameter);
						/*
						 * TODO I've removed the math pow because it was really slowing things down.
						 * the fastPow01 method doesn't help much either. Surprising this should make such
						 * a great difference.
						 */
						speakerGains[speaker] = linearGain;//(float)Math.pow(linearGain, curve);
//						speakerGains[speaker] = (float)AudioUtils.fastPow01(linearGain, curve);
					}
					//then mix that channel in
					for(int speaker = 0; speaker < speakerPositions.length; speaker++) {
						output[speaker][time] += speakerGains[speaker] * source.getValue(outputChannel, time);
					}
				}
			}
		}
	}
	
	/* 
	 * Default speaker numbering: first layout speakers 1-4 on the ground in clockwise order
	 * then layout speakers 5-8 so that they are above 1-4 respectively. Then:
	 * the x-axis follows the line joining 1 and 4
	 * the y-axis follows the line joining 1 and 2
	 * the z-axis follows the line joining 1 and 5
	 * This follows the 'right-handed' ordering of the axes: 
	 * http://en.wikipedia.org/wiki/Cartesian_coordinate_system#Orientation_and_handedness
	 * 
	 */
	/** The dimensions. */
	private int dimensions;
	
	/** The speaker positions. */
	private float[][] speakerPositions; //float[speakerNumber][dimension]
	
	/** The sphere diameter. */
	private float sphereDiameter;

	/** The sources. */
	private Map<UGen, Location> sources;
	
	/** The dead sources. */
	private List<UGen> deadSources;
	
	/** The curve. */
	private float curve; //values over 1 will focus the sound on individual speakers more
								//values below 1 will spread the sound, with zero being closer to an equal mix
								//1 is linear
								//speakers will not play a sound that is further than 1 diameter away from them
	
	/**
	 * Instantiates a new Spatial with given AudioContext and dimensions. The default speaker config for the dimensionality
	 * is used, and the default sphereDiameter (equal to Math.sqrt(dimensions)).
	 * 
	 * @param context the context
	 * @param dimensions the dimensions
	 */
	public Spatial(AudioContext context, int dimensions) {
		this(context, dimensions, (float)Math.sqrt(dimensions));
	}
	
	/**
	 * Instantiates a new Spatial with given AudioContext and sphereDiameter.
	 * 
	 * @param context the AudioContext.
	 * @param dimensions the number of dimensions, between 1 and 3.
	 * @param sphereDiameter the sphere diameter.
	 */
	public Spatial(AudioContext context, int dimensions, float sphereDiameter) {
		super(context, (int)Math.pow(2, dimensions));
		this.dimensions = dimensions;
		switch(dimensions) {
		case 1:
			setSpeakerPositions(new float[][] {
					{0}, 
					{1}
					});
			break;
		case 2:
			setSpeakerPositions(new float[][] {
					{0, 0}, 
					{0, 1}, 
					{1, 1}, 
					{1, 0}
					});
			break;
		case 3:
			setSpeakerPositions(new float[][] {
					{0,0,0},
					{0,1,0},
					{1,1,0},
					{1,0,0},
					{0,0,1},
					{0,1,1},
					{1,1,1},
					{1,0,1}
					});
			break;
		default:
			new IllegalArgumentException(
					"Error, that's a stupid number of dimensions: " + dimensions + "!"
					).printStackTrace();	
		}  
		setSphereDiameter(sphereDiameter);
		setup();
	}

	/**
	 * Instantiates a new Spatial with given AudioContext, dimensions and locations. The locations array
	 * is an array of the form float[speakerNumber][dimension].
	 * 
	 * @param context the context.
	 * @param dimensions the dimensions.
	 * @param locations the locations.
	 */
	public Spatial(AudioContext context, int dimensions, float[][] locations) {
		this(context, dimensions, locations, (float)Math.sqrt(dimensions));
	}
	
	/**
	 * Instantiates a new Spatial with the given AudioContext, dimensions, locations and sphereDiamater. The locations array
	 * is an array of the form float[speakerNumber][dimension].
	 * 
	 * @param context the context
	 * @param dimensions the dimensions
	 * @param locations the locations
	 * @param sphereDiameter the sphere diameter
	 */
	public Spatial(AudioContext context, int dimensions, float[][] locations, float sphereDiameter) {
		super(context, locations.length);
		this.dimensions = dimensions;
		setSpeakerPositions(locations);
		setSphereDiameter(sphereDiameter);
		setup();
	}
	
	/**
	 * Setup.
	 */
	private void setup() {
		outputInitializationRegime = OutputInitializationRegime.ZERO;
		sources = Collections.synchronizedMap(new Hashtable<UGen, Location>());
		deadSources = new ArrayList<UGen>();
		curve = 3f;
	}
	
	
	/**
	 * Sets the sphere diameter. This is the distance beyond which sound is attenuated to zero.
	 * 
	 * @param sd the new sphere diameter.
	 */
	public void setSphereDiameter(float sd) {
		sphereDiameter = sd;
	}
	
	/**
	 * Gets speaker positions from file. File data must correspond to given dimensions.
	 * 
	 * @param file the file.
	 * @param dimensions the dimensions.
	 * @return the speaker locations.
	 */
	public static float[][] speakerPositionsFromFile(String file, int dimensions) {
		try {
			FileInputStream fis = new FileInputStream(new File(file));
			Scanner scanner = new Scanner(fis);
			LinkedList<Float> coords = new LinkedList<Float>();
			while(scanner.hasNext()) {
				coords.add(scanner.nextFloat());
			}
			System.out.print("Spatial: Loaded speaker positions from " + file + " ");
			float[][] speakerPositions = new float[coords.size() / dimensions][dimensions];
			for(int i = 0; i < speakerPositions.length; i++) {
				System.out.print("[");
				for(int j = 0; j < dimensions; j++) {
					speakerPositions[i][j] = coords.poll();
					System.out.print(speakerPositions[i][j] + " ");
				}
				System.out.print("]");
			}
			System.out.println();
			return speakerPositions;
		} catch(Exception e) { 
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Sets the speaker positions. The locations array
	 * is an array of the form float[speakerNumber][dimension]. 
	 * 
	 * @param locations the new speaker positions.
	 */
	public void setSpeakerPositions(float[][] locations) {
		if(locations.length > 0 && locations[0].length != dimensions) {
			new IllegalArgumentException(
					"Error, location data does not correspond to dimensions: " + dimensions + "!"
					).printStackTrace();	
			return;
		}
		speakerPositions = new float[locations.length][dimensions];
		for(int i = 0; i < speakerPositions.length; i++) {
			for(int j = 0; j < dimensions; j++) {
				speakerPositions[i][j] = locations[i][j];
			}
		}
	}
	
	/**
	 * Gets the Euclidian distance between two positions.
	 * 
	 * @param a the a
	 * @param b the b
	 * @return the float
	 */
	public static float distance(float[] a, float[] b) {
		float distance = 0;
		for(int i = 0; i < a.length; i++) {
			distance += (a[i] - b[i]) * (a[i] - b[i]);
		}
		distance = (float)Math.sqrt(distance);
		return distance;
	}
	
	/** 
	 * This overrides {@link #addInput(UGen)} by adding a new 'source' sound to the spatialisation.
	 */
	public void addInput(UGen source) {
		Location location = new Location(source);
		sources.put(source, location);
	}
	
	/** 
	 * This overrides {@link #addInput(UGen)} by adding a new 'source' sound to the spatialisation. 
	 */
	public void addInput(int inputIndex, UGen source, int outputIndex) {
		addInput(source);
	}

	/**
	 * Adds a new source sound with the given UGen controllers for controlling its position. The array of controllers is of the form
	 * UGen[channel][dimension] where channel is the output channel of the source UGen, treated as a point source in the space
	 * and dimension is the axis along which the controller UGen is controlling position. 
	 * 
	 * @param source the source.
	 * @param controllers the controllers.
	 */
	public void addInput(UGen source, UGen[][] controllers) {
		Location location = new Location(source, controllers);
		sources.put(source, location);
	}
	
	/**
	 * Sets the location of a UGen at the give channel with glide. This assumes the UGen's position is not being
	 * controlled by other external UGens.
	 * 
	 * @param source the source
	 * @param channel the channel
	 * @param newPos the new pos
	 */
	public void setLocation(UGen source, int channel, float[] newPos) {
		Location l = sources.get(source);
		if(l != null) l.move(channel, newPos);
	}
	
//	/**
//	 * Sets the location of a UGen at the give channel immediately. This assumes the UGen's position is not being
//	 * controlled by other external UGens.
//	 * 
//	 * @param source the source
//	 * @param channel the channel
//	 * @param newPos the new pos
//	 */
//	public void setLocationImmediately(UGen source, int channel, float[] newPos) {
//		sources.get(source).moveImmediately(channel, newPos);
//	}
	
	/**
	 * Removes the source.
	 * 
	 * @param source the source
	 */
	public void removeSource(UGen source) {
		synchronized(sources) {
			sources.remove(source);
		}
	}
	

	/* (non-Javadoc)
	 * @see net.beadsproject.beads.core.UGen#clearInputConnections()
	 */
	@Override
	public synchronized void clearInputConnections() {
		super.clearInputConnections();
		sources.clear();
	}

	/* (non-Javadoc)
	 * @see net.beadsproject.beads.core.UGen#removeAllConnections(net.beadsproject.beads.core.UGen)
	 */
	@Override
	public synchronized void removeAllConnections(UGen sourceUGen) {
		super.removeAllConnections(sourceUGen);
		removeSource(sourceUGen);
	}

	
	/**
	 * Sets the curve. The curve defines the attenuation from each speaker from 1 when the source 
	 * is next to the speaker to zero when the source is at sphereDiamater away from the speaker or further. 
	 * The curve defines the exponent of the attenuation, such that numbers greater than 1 drop more rapidly
	 * near the speaker whilst numbers less than 1 (greater than zero please) drop more gently.
	 * 
	 * @param curve the new curve
	 */
	public void setCurve(float curve) {
		this.curve = curve;
	}

	/* (non-Javadoc)
	 * @see net.beadsproject.beads.core.UGen#calculateBuffer()
	 */
	@Override
	public void calculateBuffer() {
		synchronized(sources) {
			for(UGen source : sources.keySet()) {
				Location location = sources.get(source);
				location.mixInAudio(bufOut);
				if(source.isDeleted()) {
					deadSources.add(source);
				}
			}
			for(UGen source : deadSources) {
				sources.remove(source);
			}
			deadSources.clear();
		}
	}

	@Override
	public synchronized int getNumberOfConnectedUGens(int index) {
		return sources.size();
	}

	public synchronized int getNumberOfSources() {
		return sources.size();
	}
	

}
