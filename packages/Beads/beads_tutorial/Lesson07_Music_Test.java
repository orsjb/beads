import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.Pitch;
import net.beadsproject.beads.events.KillTrigger;
import net.beadsproject.beads.ugens.*;
import org.junit.Test;

public class Lesson07_Music_Test {

	private final AudioContext ac = new AudioContext();

	private static float random(double x) {
		return (float) (Math.random() * x);
	}

	@Test
	public void Lesson07_Music()
			throws InterruptedException {
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
		//this is the on-the-fly bead
		MusicBead musicBead = new MusicBead();
		clock.addMessageListener(musicBead);
		ac.out.addDependent(clock);
		ac.start();
		Thread.sleep(10000);
		ac.stop();
	}

	class MusicBead extends Bead {
		int pitch;

		//this is the method that we override to make the Bead do something
		public void messageReceived(Bead message) {
			Clock c = (Clock) message;
			if (c.isBeat()) {
				addBeatWavePlayer();
			}
			if (c.getCount() % 5 == 0) {
				addNoteWavePlayer(pitch);
			}
		}

		private void addBeatWavePlayer() {
			if (random(1) < 0.5) return;
			//choose some nice frequencies
			pitch = Pitch.forceToScale((int) random(12), Pitch.dorian)+ (int) random(4) * 12 * (int) random(1) * -1;
			float freq = Pitch.mtof(pitch + (int) random(5) * 12 );
			WavePlayer wp = new WavePlayer(ac, freq, Buffer.SINE);
			Gain g = new Gain(ac, 1, new Envelope(ac, 0));
			g.addInput(wp);
			ac.out.addInput(g);
			float endValue = 0.1f;
			float value = random(200);
			((Envelope) g.getGainUGen()).addSegment(endValue, value);
			float killDuration = random(7000);
			((Envelope) g.getGainUGen()).addSegment(0, killDuration, new KillTrigger(g));

			System.out.printf("%nBeat freq=%s endValue=%s value=%s killDuration=%s%n%n",
					freq,
					value,
					endValue,
					killDuration);
		}

		private void addNoteWavePlayer(int pitch) {
			//choose some nice frequencies
			if (random(1) < 0.2)
				this.pitch = Pitch.forceToScale((int) random(12), Pitch.dorian) + (int) random(4) * 12 * (int) random(1) * -1;
			float freq = Pitch.mtof(pitch +  (int) random(5) * 12);
			float ipos = random(1);
			float endValue = random(0.1);
			float value = random(50);
			float killDuration = random(400);
			Buffer buffer = Buffer.SQUARE;
			String bufferName = "SQUARE";
			float bufferType = random(5);
			if (bufferType < 1) {
				buffer = Buffer.SQUARE;
				bufferName = "SQUARE";
			} else if (bufferType < 2) {
				buffer = Buffer.SINE;
				bufferName = "SINE";
			} else if (bufferType < 3) {
				buffer = Buffer.SAW;
				bufferName = "SAW";
			} else if (bufferType < 4) {
				buffer = Buffer.TRIANGLE;
				bufferName = "TRIANGLE";
			} else if (bufferType < 5) {
				buffer = Buffer.NOISE;
				bufferName = "NOISE";
			}

			addNoteWavePlayer(freq,
					ipos,
					endValue,
					value,
					killDuration,
					bufferType,
					bufferName,
					buffer);

			float noiseIpos = random(0.5) + 0.5f;
			float noiseKillDuration = random(100);
			float envelopeValue = 0.05f;
			addNoise(envelopeValue,
					noiseIpos,
					noiseKillDuration);
		}

		private void addNoteWavePlayer(float freq,
		                               float ipos,
		                               float endValue,
		                               float value,
		                               float killDuration,
		                               float bufferType,
		                               String bufferName,
		                               Buffer buffer) {
			WavePlayer wp = new WavePlayer(ac,
					freq,
					buffer);
			Gain g = new Gain(ac, 1, new Envelope(ac, 0));
			g.addInput(wp);
			Panner p = new Panner(ac, ipos);
			p.addInput(g);
			ac.out.addInput(p);
			((Envelope) g.getGainUGen()).addSegment(endValue, value);
			((Envelope) g.getGainUGen()).addSegment(0, killDuration, new KillTrigger(p));
			System.out.printf("Note freq=%s bufferName=%s bufferType=%s ipos=%s endValue=%s value=%s killDuration=%s%n",
					freq,
					bufferName,
					bufferType,
					ipos,
					value,
					endValue,
					killDuration);
		}

		private void addNoise(float envelopeValue,
		                      float ipos,
		                      float killDuration) {
			Noise n = new Noise(ac);
			Gain gn = new Gain(ac, 1, new Envelope(ac, envelopeValue));
			gn.addInput(n);
			Panner np = new Panner(ac, ipos);
			np.addInput(gn);
			ac.out.addInput(np);
			((Envelope) gn.getGainUGen()).addSegment(0, killDuration, new KillTrigger(np));
			System.out.printf("Noise ipos=%s killDuration=%s%n%n",
					ipos,
					killDuration);
		}
	}
}