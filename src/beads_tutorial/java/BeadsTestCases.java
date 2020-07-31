import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.ugens.Clock;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.WavePlayer;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

public class BeadsTestCases {


    /*
     * Raw test of CPU.
     */
    public static void manyNodes(UGen.UGenStorageType ugenType) {
        //see how many nodes you can add before audio starts to break up
        int N = 100;
        Gain gain = new Gain(1, 0.1f / N);
        gain.setStorageType(ugenType);
        gain.getDefaultContext().out.addInput(gain);
        for(int i = 0; i < N; i++) {
            gain.addInput(new WavePlayer((float)Math.random() * 500f + 500f, Buffer.SINE));
        }
        System.out.println(gain.getStorageType());
    }

    /*
     * Addition and removal but avoiding GC overhead and any other confusing use of data structures
     */
    public static void addAndRemoveNodes(UGen.UGenStorageType ugenType) {
        //see how many nodes you can add before audio starts to break up, choose speed too
        int N = 100;
        int clockInterval = 50;     //fast interval
        int K = 99; //number of connected, K < N
        assert(K < N);
        //we will create N elements, with K elements connected at any time
        UGen[] allElements = new UGen[N];
        Gain gain = new Gain(1, 0.1f / N);
        gain.setStorageType(ugenType);
        for(int i = 0; i < N; i++) {
            WavePlayer wp = new WavePlayer((float)Math.random() * 500f + 500f, Buffer.SINE);
            allElements[i] = wp;
        }
        //connect K elements
        for(int i = 0; i < K; i++) {
            gain.addInput(allElements[i]);
        }
        //set up the clock that will make this happen
        Clock c = new Clock(clockInterval) {
            @Override
            protected void messageReceived(Bead message) {
                //choose one to remove
                int randomNumber = (int)(N * Math.random());
                gain.removeConnection(0, allElements[randomNumber], 0);
                //choose one to add
                randomNumber = (int)(N * Math.random());
                gain.addInput(allElements[randomNumber]);
            }
        };
        //connections
        AudioContext ac = UGen.getDefaultContext();
        ac.out.addInput(gain);
        ac.out.addDependent(c);
    }

    /*
     * Addition and removal involving automatic removal of "killed" UGens and also use of GC.
     */
    public static void addAndRemoveNodesWithGC(UGen.UGenStorageType ugenType) {
        //see how many nodes you can add before audio starts to break up, choose speed too
        int N = 100;
        int clockInterval = 50;
        Gain gain = new Gain(1, 0.1f / N);
        gain.setStorageType(ugenType);
        //we will create N elements
        //connect K elements
        for(int i = 0; i < N; i++) {
            gain.addInput(new WavePlayer((float)Math.random() * 500f + 500f, Buffer.SINE));
        }
        //set up the clock that will make this happen
        Clock c = new Clock(clockInterval) {
            @Override
            protected void messageReceived(Bead message) {
                //choose one to remove
                Set<UGen> inputs = gain.getConnectedInputs();
                int randomNumber = (int)(N * Math.random());
                UGen anElement = inputs.iterator().next();      //is this always the first element?
                anElement.kill(); //kill triggers automatic removal and hence add to GC pile
                //choose one to add
                gain.addInput(new WavePlayer((float)Math.random() * 500f + 500f, Buffer.SINE));
            }
        };
        //connections
        AudioContext ac = UGen.getDefaultContext();
        ac.out.addInput(gain);
        ac.out.addDependent(c);
    }

	public static void main(String[] args) {
		int loopLength = 1000000;
		
		long startTime = System.nanoTime();
		for (int i = 0; i < loopLength; i++) {
			// Actual Function Here
			
			AudioContext ac = new AudioContext();
			UGen.setDefaultContext(ac);
			addAndRemoveNodes(UGen.UGenStorageType.ARRAYLIST);
			
			// End Actual Function
		}
			
		long stopTime = System.nanoTime();
		long elapsedTime1 = stopTime - startTime;
		
		//----
		
		startTime = System.nanoTime();
		for (int i = 0; i < loopLength; i++) {
			// Actual Function Here
			
			AudioContext ac = new AudioContext();
			UGen.setDefaultContext(ac);
			addAndRemoveNodes(UGen.UGenStorageType.LINKEDLIST);
			
			// End Actual Function
		}
			
		stopTime = System.nanoTime();
		long elapsedTime2 = stopTime - startTime;
		
		//----
		
		System.out.println("Array - Total: " + elapsedTime1 + ", Average: " + (elapsedTime1 / loopLength));
		System.out.println("Linked - Total: " + elapsedTime2 + ", Average: " + (elapsedTime2 / loopLength));
	}
}
