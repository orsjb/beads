

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.WavePlayer;


public class Lesson02_EnvelopeAndWavePlayer {

	public static void main(String[] args) {
            
	      /*
	       * As we are using the defaultcontext, there is no longer
           * a need to create an AudioContext object. For simplicity's sake,
           * we'll just give the defaultcontext a local name, and access it
           * it using 'ac'.
	       */
          AudioContext ac = AudioContext.getDefaultContext();
	    
		  /*
		   * This is an Envelope. It can be used to modify
		   * the behaviour of other UGen object. We need to
		   * do this to get precise control of certain parameters
		   * at an audio rate.
           *
           * When using defaultcontexts, we no longer need to pass 
           * in an explicit AudioContext when constructing new UGens. 
		   * Note that if you use a custom AudioContext, you will need
           * pass it into the constructor of each UGen.
		   */
		  Envelope freqEnv = new Envelope(500);
		  /*
		   * This is a WavePlayer. Here we've set it up using 
		   * the above Envelope, and a SineBuffer. We'll use
		   * the Envelope to modify the freqency below.
		   */
		  WavePlayer wp = new WavePlayer(freqEnv, Buffer.SINE);
		  /*
		   * So now that the WavePlayer is set up with the 
		   * frequency Envelope, do stuff with the frequency
		   * envelope. This command tells the Envelope to change
		   * to 1000 in 1 second. Note that when we made the Envelope
		   * it was set to 500, so the transition goes from 500 to
		   * 1000. These control the frequency of the WavePlayer
		   * in Hz.
		   */
		  freqEnv.addSegment(1000, 1000);
		  /*
		   * Connect it all together as before.
		   */
		  Gain g = new Gain(1, 0.1f);
		  g.addInput(wp);
		  
		  /*
		   * We will still need to attach all the UGens to the output
		   * of the defaultcontext and make it start running in order
		   * for audio to play. We can do this the same way as before.
		   */
		  ac.out.addInput(g);
		  ac.start();
		
	}
}
