

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.ugens.Clock;
import net.beadsproject.beads.ugens.Envelope;


public class Lesson05_Clock {

	public static void main(String[] args) {
          AudioContext ac = AudioContext.getDefaultContext();

		 /*
		  * A Clock is an unusual UGen because it doesn't
		  * have any outputs and because objects can listen
		  * to it.
		  * 
		  * In this example, Clock just ticks. We can run a 
		  * clock from an envelope which determines the rate
		  * of ticking.
		  * 
		  * So we begin with the envelope as before.
		  */
		  Envelope intervalEnvelope = new Envelope(1000);
		  intervalEnvelope.addSegment(600, 10000);
		  intervalEnvelope.addSegment(1000, 10000);
		  intervalEnvelope.addSegment(400, 10000);
		  intervalEnvelope.addSegment(1000, 10000);
		  /*
		   * Then the clock, which gets initialised with the
		   * envelope. 
		   */
		  Clock clock = new Clock(intervalEnvelope);
		  /*
		   * Tell the clock to tick (you probably don't want
		   * to do this except for debugging.
		   */
		  clock.setClick(true);
		  /*
		   * Now this is new, because the clock doesn't have
		   * any outputs, we can't add it to the AudioContext
		   * and that means it won't run. So we use the method
		   * addDependent() instead.
		   */
		  ac.out.addDependent(clock);
		  ac.start();
		}
	
}
