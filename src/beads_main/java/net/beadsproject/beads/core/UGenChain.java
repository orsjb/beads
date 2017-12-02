/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.core;




/**
 * Organizes a series of connected UGens into one unit. It allows for users to
 * define a custom UGen purely from other UGens, without programming the
 * {@link UGen#calculateBuffer()} routine.
 * 
 * @author Benito Crawford
 * @version 0.9.5
 */
public class UGenChain extends UGen {

	private UGen chainIn, chainOut;

	public UGenChain(AudioContext context, int ins, int outs) {
		super(context, ins, outs);

		// This grabs the inputs from this Chain instance, so they can be used
		// by UGens in the chain.
		chainIn = new UGen(context, 0, ins) {
			@Override
			public void calculateBuffer() {
			}
		};
		chainIn.bufOut = bufIn;
		chainIn.outputInitializationRegime = OutputInitializationRegime.RETAIN;

		// This collects the output of the chain and lets this Chain instance
		// grab the data.
		chainOut = new UGen(context, outs, 0) {
			@Override
			public void calculateBuffer() {
			}
		};
		this.bufOut = chainOut.bufIn;
		this.outputInitializationRegime = OutputInitializationRegime.RETAIN;

	}

	/**
	 * Adds the Chain inputs to the target UGen's inputs.
	 * 
	 * @param targetUGen
	 *            The target UGen.
	 */
	public void drawFromChainInput(UGen targetUGen) {
		targetUGen.addInput(chainIn);
	}

	/**
	 * Adds the specified Chain input to all of a target UGen's inputs.
	 * 
	 * @param chainInputIndex
	 *            The index of the Chain input.
	 * @param targetUGen
	 *            The UGen to which to add the Chain input.
	 */
	public void drawFromChainInput(int chainInputIndex, UGen targetUGen) {
		for (int i = 0; i < targetUGen.ins; i++) {
			targetUGen.addInput(i, chainIn, chainInputIndex);
		}
	}

	/**
	 * 
	 * Adds the specified Chain input to a target UGen's input.
	 * 
	 * @param chainInputIndex
	 *            The index of the Chain input.
	 * @param targetUGen
	 *            The target UGen to which to add the Chain input.
	 * @param targetInputIndex
	 *            The input of the target UGen.
	 */
	public void drawFromChainInput(int chainInputIndex, UGen targetUGen,
			int targetInputIndex) {
		targetUGen.addInput(targetInputIndex, chainIn, chainInputIndex);
	}

	/**
	 * Adds the output of a source UGen to the Chain output.
	 * 
	 * @param sourceUGen
	 *            The source UGen.
	 */
	public void addToChainOutput(UGen sourceUGen) {
		chainOut.addInput(sourceUGen);
	}

	/**
	 * Adds all of the outputs of a source UGen to a Chain output.
	 * 
	 * @param chainOutputIndex
	 *            The Chain output.
	 * @param sourceUGen
	 *            The source UGen.
	 */
	public void addToChainOutput(int chainOutputIndex, UGen sourceUGen) {
		for (int i = 0; i < sourceUGen.outs; i++) {
			addToChainOutput(chainOutputIndex, sourceUGen, i);
		}
	}

	/**
	 * Adds an output from a source UGen to a Chain output.
	 * 
	 * @param chainOutputIndex
	 *            The Chain output.
	 * @param sourceUGen
	 *            The source UGen.
	 * @param sourceOutputIndex
	 *            The output of the source UGen to add to the Chain output.
	 */
	public void addToChainOutput(int chainOutputIndex, UGen sourceUGen,
			int sourceOutputIndex) {
		chainOut.addInput(chainOutputIndex, sourceUGen, sourceOutputIndex);
	}

	@Override
	public final void calculateBuffer() {
		preFrame();
		chainOut.update();
		postFrame();
	}

	/**
	 * Called before the signal chain is updated for this Chain. Does nothing by
	 * default; can be implemented with code as needed.
	 */
	protected void preFrame() {
	}

	/**
	 * Called after the signal chain is updated for this Chain. Does nothing by
	 * default; can be implemented with code as needed.
	 */
	protected void postFrame() {
	}

}
