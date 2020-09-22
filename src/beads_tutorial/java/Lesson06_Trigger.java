

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.events.KillTrigger;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.WavePlayer;

public class Lesson06_Trigger {

	public static void main(String[] args) {
          AudioContext ac = AudioContext.getDefaultContext();

		 /*
		  * How do you trigger events to happen in the future? 
		  *
		  * Here's one example where an Envelope is used to trigger
		  * the event. The event itself is a special class that
		  * kills another object, in this case a master gain object.
		  */
		  
		  /*
		   * Here is the master gain object.
		   */
		  Gain masterGain = new Gain(1, 1);
		  
		  /*
		   * Now two things. Firstly, a WavePlayer with an Envelope controlling
		   * its frequency, connected to a Gain.
		   */
		  Envelope freqEnv = new Envelope(250);
		  WavePlayer wp = new WavePlayer(freqEnv, Buffer.SINE);
		  Gain g1 = new Gain(1, 0.3f);
		  g1.addInput(wp);
		  
		  /*
		   * Secondly, just another WavePlayer connected to a Gain (no Envelope).
		   */
		  WavePlayer wp2 = new WavePlayer(255, Buffer.SQUARE);
		  Gain g2 = new Gain(1, 0.1f);
		  g2.addInput(wp2);
		  
		  /*
		   * Connect them both to the master gain.
		   */
		  masterGain.addInput(g1);
		  masterGain.addInput(g2); 
		  
		  /*
		   * In this line we make the Gain envelope do something (fade to 
		   * zero over 5 seconds), and then fire an event to a listener.
		   * 
		   * All we have to do is tell the Envelope what listener to trigger.
		   * In this case, this will stop the second sound by killing g2.
		   *
		   * Note that "killing" any UGen causes it to be removed from the signal
		   * chain, and causes all of the things "upstream" from it to be
		   * removed as well. 
		   */
		  freqEnv.addSegment(500, 5000, new KillTrigger(g2));
		  
		  /*
		   * Now play
		   */
		  ac.out.addInput(masterGain);
		  ac.start();
		  
		  /*
		   * Some other objects that can trigger events: 
		   * Clock, DelayTrigger, PeakDetector.
		   * 
		   * Some other objects that can respond to events:
		   * AudioContextStopTrigger, Pattern, PauseTrigger.
		   * 
		   * It is easy to make your own trigger, by subclassing Bead. 
		   * The following event gets added an extra 1s after the previous one.
		   */
		   
		   Bead myTrigger = new Bead() {
		     public void messageReceived(Bead message) {
		        System.out.println("I've been triggered!"); 
		     }
		   };
		   freqEnv.addSegment(0, 1000, myTrigger);
		   

	}
}
