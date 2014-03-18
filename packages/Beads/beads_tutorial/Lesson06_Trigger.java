

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.events.AudioContextStopTrigger;
import net.beadsproject.beads.ugens.Clock;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.WavePlayer;

public class Lesson06_Trigger {

	public static void main(String[] args) {

		AudioContext ac;

		ac = new AudioContext();
		/*
		 * How do you trigger events to happen in the future?
		 * 
		 * Here's one example where an Envelope is used to trigger the event.
		 * The event itself is a special class that kills the AudioContext.
		 */
		WavePlayer wp = new WavePlayer(ac, 500, Buffer.SINE);
		Gain g = new Gain(ac, 1, new Envelope(ac, 0.1f));
		/*
		 * In this line we make the Gain envelope do something (fade to zero
		 * over 5 seconds), and then fire an event to a listener.
		 * 
		 * All we have to do is tell the Envelope what listener to trigger. In
		 * this case, this will kill the audio.
		 */
		((Envelope) g.getGainUGen()).addSegment(0, 5000,
				new AudioContextStopTrigger(ac));
		g.addInput(wp);
		ac.out.addInput(g);
		/*
		 * The clock is just here to prove the point. Notice that the clock
		 * stops ticking.
		 */
		Clock c = new Clock(ac, 1000);
		c.setClick(true);
		ac.out.addDependent(c);
		ac.start();

	}
}
