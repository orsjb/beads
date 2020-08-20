import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.core.UGen.UGenStorageType;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.ugens.Clock;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.WavePlayer;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

public class BeadsTestCases {
	
	private static long totalDur = 0;
	private static long minDur = 0;
	private static long maxDur = 0;
	
	private static int N = 5000;
	private static int M = 100;
	private static int O = -1;


    /*
     * Raw test of CPU.
     */
    public static void manyNodes(UGenStorageType ugen) {
        //see how many nodes you can add before audio starts to break up
        for (int v = 0; v <= (int) M / N; v++) {
            Gain gain = new Gain(1, 0.1f / N);
            gain.getDefaultContext().out.addInput(gain);
            gain.setStorageType(ugen);
            
	        for(int i = 0; i < N; i++) {
	        	long sTime = System.nanoTime();
	        	
	            gain.addInput(new WavePlayer((float)Math.random() * 500f + 500f, Buffer.SINE));
	            
		        gain.update();
		        
		        System.out.println("add: " + gain.getNumberOfConnectedUGens(0) + ", " + i);
		        
		        long myDur = System.nanoTime() - sTime;
		        if (i == 0 && v == 0) {
		        	totalDur = 0;
		        	minDur = myDur;
		        	maxDur = myDur;
		        } else {
		        	minDur = (myDur < minDur) ? myDur : minDur;
		        	maxDur = (myDur > maxDur) ? myDur : maxDur;	        	
		        }
		        totalDur += myDur;            
	        }
        }
    }

    /*
     * Addition and removal but avoiding GC overhead and any other confusing use of data structures
     */
    public static void addAndRemoveNodes(UGenStorageType ugen) {
        //see how many nodes you can add before audio starts to break up, choose speed too
        int clockInterval = 50;     //fast interval
        int K = N - 1; //number of connected, K < N
        assert(K < N);
        //we will create N elements, with K elements connected at any time
        UGen[] allElements = new UGen[N];
        Gain gain = new Gain(1, 0.1f / N);
        gain.setStorageType(ugen);
        for(int i = 0; i < N; i++) {
            WavePlayer wp = new WavePlayer((float)Math.random() * 500f + 500f, Buffer.SINE);
            allElements[i] = wp;
        }
        //connect K elements
        for(int i = 0; i < K; i++) {
            gain.addInput(allElements[i]);
        }
        
        //set up the clock that will make this happen
        for (int i = 0; i < M; i++) {
        	long sTime = System.nanoTime();
            //choose one to remove
            int randomNumber;
            
            randomNumber = (O != -1) ? O : (int)(N * Math.random());
            gain.removeConnectionAtIndex(0, randomNumber, 0);
            //gain.removeConnection(0, allElements[randomNumber], 0);
            
            while (gain.getNumberOfConnectedUGens(0) != N - 2) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }               
            }
            
            long myDur = System.nanoTime() - sTime;
            if (i == 0) {
                minDur = myDur;
                maxDur = myDur;
            } else {
                minDur = (myDur < minDur) ? myDur : minDur;
                maxDur = (myDur > maxDur) ? myDur : maxDur;             
            }
            totalDur += myDur;
            
            //choose one to add
            randomNumber = (int)(N * Math.random());
            gain.addInput(allElements[randomNumber]);
            
	        gain.update();
	        
	        System.out.println("ar: " + gain.getNumberOfConnectedUGens(0) + ", " + i);
        }

        //connections
        AudioContext ac = UGen.getDefaultContext();
        ac.out.addInput(gain);
    }

    /*
     * Addition and removal involving automatic removal of "killed" UGens and also use of GC.
     */
    public static void addAndRemoveNodesWithGC(UGenStorageType ugen) {
        //see how many nodes you can add before audio starts to break up, choose speed too
        int clockInterval = 50;
        Gain gain = new Gain(1, 0.1f / N);
        gain.setStorageType(ugen);
        //we will create N elements
        //connect K elements
        for(int i = 0; i < N; i++) {
            gain.addInput(new WavePlayer((float)Math.random() * 500f + 500f, Buffer.SINE));
        }

        for (int i = 0; i < M; i++) {
        	long sTime = System.nanoTime();
	        //choose one to remove
	        Set<UGen> inputs = gain.getConnectedInputs();
	        int randomNumber = (O != -1) ? O : (int)(N * Math.random());
	        UGen anElement = inputs.iterator().next();      //is this always the first element?
	        anElement.kill(); //kill triggers automatic removal and hence add to GC pile
	        //choose one to add
	        gain.addInput(new WavePlayer((float)Math.random() * 500f + 500f, Buffer.SINE));
	        
	        gain.update();
	        
	        System.out.println("argc: " + gain.getNumberOfConnectedUGens(0) + ", " + i);
	        
	        long myDur = System.nanoTime() - sTime;
	        if (i == 0) {
	        	totalDur = 0;
	        	minDur = myDur;
	        	maxDur = myDur;
	        } else {
	        	minDur = (myDur < minDur) ? myDur : minDur;
	        	maxDur = (myDur > maxDur) ? myDur : maxDur;	        	
	        }
	        totalDur += myDur;
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //connections
        AudioContext ac = UGen.getDefaultContext();
        ac.out.addInput(gain);
    }

	public static void main(String[] args) {
//		int loopLength = 1;
//		long loopStart;
//		long loopMin1 = 0, loopMax1 = 0;
		
//		long startTime = System.nanoTime();
//		for (int i = 0; i < loopLength; i++) {
//			loopStart = System.nanoTime();
			
			// Actual Function Here
			
			AudioContext ac = new AudioContext();
			UGen.setDefaultContext(ac);
			ac.start();
			
			N = 100000;
			M = 10;
			O = 50000;
			addAndRemoveNodes(UGenStorageType.LINKEDLIST);

			// End Actual Function
			
//			long loopDur = System.nanoTime() - loopStart;
//			if (i == 0) {
//				loopMin1 = loopDur;
//				loopMax1 = loopDur;
//			} else {
//				loopMin1 = (loopDur < loopMin1) ? loopDur : loopMin1;
//				loopMax1 = (loopDur > loopMax1) ? loopDur : loopMax1;
//			}
			
//		}
			
//		long stopTime = System.nanoTime();
//		long elapsedTime1 = stopTime - startTime;
//		
//		System.out.println("Time - Total: " + elapsedTime1 + ", Average: " + (elapsedTime1 / loopLength) 
//				+ ", Min: " + loopMin1 + ", Max: " + loopMax1);
		System.out.println("Per Entry - Total: " + totalDur + ", Average: " + (totalDur / M)
				+ ", Min: " + minDur + ", Max: " + maxDur);
	}
}
