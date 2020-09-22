

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.Pitch;
import net.beadsproject.beads.events.KillTrigger;
import net.beadsproject.beads.ugens.Clock;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.Noise;
import net.beadsproject.beads.ugens.Panner;
import net.beadsproject.beads.ugens.WavePlayer;

public class Lesson07_Music {

	public static void main(String[] args) {
        AudioContext ac = AudioContext.getDefaultContext();

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
		Clock clock = new Clock(700);
		clock.addMessageListener(
				  //this is the on-the-fly bead
				  new Bead() {
				    //this is the method that we override to make the Bead do something
				    int pitch;
				     public void messageReceived(Bead message) {
				        Clock c = (Clock)message;
				        if(c.isBeat()) {
				          //choose some nice frequencies
				          if(random(1) < 0.5) return;
				          pitch = Pitch.forceToScale((int)random(12), Pitch.dorian);
				          float freq = Pitch.mtof(pitch + (int)random(5) * 12 + 32);
				          WavePlayer wp = new WavePlayer(freq, Buffer.SINE);
				          Gain g = new Gain(1, new Envelope(0));
				          g.addInput(wp);
				          ac.out.addInput(g);
				          ((Envelope)g.getGainUGen()).addSegment(0.1f, random(200));
				          ((Envelope)g.getGainUGen()).addSegment(0, random(7000), new KillTrigger(g));
				       }
				       if(c.getCount() % 4 == 0) {
				           //choose some nice frequencies
				          int pitchAlt = pitch;
				          if(random(1) < 0.2) pitchAlt = Pitch.forceToScale((int)random(12), Pitch.dorian) + (int)random(2) * 12;
				          float freq = Pitch.mtof(pitchAlt + 32);
				          WavePlayer wp = new WavePlayer(freq, Buffer.SQUARE);
				          Gain g = new Gain(1, new Envelope(0));
				          g.addInput(wp);
				          Panner p = new Panner(random(1));
				          p.addInput(g);
				          ac.out.addInput(p);
				          ((Envelope)g.getGainUGen()).addSegment(random(0.1), random(50));
				          ((Envelope)g.getGainUGen()).addSegment(0, random(400), new KillTrigger(p));
				       }
				       if(c.getCount() % 4 == 0) {
				          Noise n = new Noise();
				          Gain g = new Gain(1, new Envelope(0.05f));
				          g.addInput(n);
				          Panner p = new Panner(random(0.5) + 0.5f);
				          p.addInput(g);
				          ac.out.addInput(p);
				          ((Envelope)g.getGainUGen()).addSegment(0, random(100), new KillTrigger(p));
				       }
				     }
				   }
				 );
		ac.out.addDependent(clock);
		ac.start();

	}
	
	public static float random(double x) {
		return (float)(Math.random() * x);
	}
}
