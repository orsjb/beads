

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.events.KillTrigger;
import net.beadsproject.beads.ugens.Clock;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.WavePlayer;

public class Lesson07_Music {

	public static void main(String[] args) {

		final AudioContext ac;

		ac = new AudioContext();
		/*
		 * In this example a Clock is used to trigger events. We do this by
		 * adding a listener to the Clock (which is of type Bead).
		 * 
		 * The Bead is made on-the-fly. All we have to do is to give the Bead a
		 * callback method to make notes.
		 * 
		 * This example is more sophisticated than the previous ones. It uses
		 * nested code.
		 */
		Clock clock = new Clock(ac, 700);
		clock.addMessageListener(
		// this is the on-the-fly bead
				new Bead() {

					// this is the method that we override to make the Bead do
					// something
					public void messageReceived(Bead message) {
						Clock c = (Clock) message;
						if (c.isBeat()) {
							WavePlayer wp = new WavePlayer(ac, (float) Math
									.random() * 3000 + 100, Buffer.SINE);
							Gain g = new Gain(ac, 1, new Envelope(ac, 0.1f));
							((Envelope) g.getGainUGen()).addSegment(0,
									1000, new KillTrigger(g));
							g.addInput(wp);
							ac.out.addInput(g);
						}
					}
				});
		ac.out.addDependent(clock);
		ac.start();

	}
}
