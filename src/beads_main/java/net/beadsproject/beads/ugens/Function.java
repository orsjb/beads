/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.UGen;

/**
 * Function is an abstract class which can be quickly subclassed to make a custom {@link UGen} on the fly. Subclasses of Function must implement {@link #calculate()}, getting data from the array {@link #x}, and returning the result. {@link #x} provides access to the array of {@link UGen}s that are passed to Function's constructor. 
 *
 * @beads.category utilities
 * @author ollie
 */
public abstract class Function extends UGen {

	// TODO curious challenge: 
	//can we get Function to not bother updating if its inputs are not updating?
	//could UGen have a method 'hasChanged()'?
	
	/** An array representing the current values from the array of input UGens. */
	protected float[] x;
	
	/** The inputs. */
	private UGen[] inputs;
	
	/**
	 * Instantiates a new function.
	 * 
	 * @param input the input
	 */
	public Function(UGen input) {
		this(new UGen[] {input});
	}
	
	/**
	 * Instantiates a new function.
	 * 
	 * @param inputs the set of input UGens.
	 */
	public Function(UGen... inputs) {
		super(inputs[0].getContext(), 1);
		this.inputs = inputs;
		x = new float[inputs.length];
	}
	
	/* (non-Javadoc)
	 * @see net.beadsproject.beads.core.UGen#calculateBuffer()
	 */
	public final void calculateBuffer() {
		for(int i = 0; i < inputs.length; i++) {
			inputs[i].update();
		}
		for(int i = 0; i < bufferSize; i++) {
			for(int j = 0; j < inputs.length; j++) {
				x[j] = inputs[j].getValue(0, i);
			}
			bufOut[0][i] = calculate();
		}
	}
	
	/**
	 * Override this to calculate what to do.
	 * Use x[] to get the values from the input {@link UGen}s.
	 * 
	 * @return the result of the calculation.
	 */
	public abstract float calculate();

}
