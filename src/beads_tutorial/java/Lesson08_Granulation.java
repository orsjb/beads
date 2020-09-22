import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.GranularSamplePlayer;
import net.beadsproject.beads.ugens.SamplePlayer;

public class Lesson08_Granulation {

	public static void main(String[] args) {
        AudioContext ac = AudioContext.getDefaultContext();

		/*
		 * In lesson 4 we played back samples. This example is almost the same
		 * but uses GranularSamplePlayer instead of SamplePlayer. See some of
		 * the controls below.
		 */
		String audioFile = "audio/kick_back.wav";
		GranularSamplePlayer player = new GranularSamplePlayer(
				SampleManager.sample(audioFile));
		/*
		 * Have some fun with the controls.
		 */
		// loop the sample at its end points
		player.setLoopType(SamplePlayer.LoopType.LOOP_ALTERNATING);
		player.getLoopStartUGen().setValue(0);
		player.getLoopEndUGen().setValue(
				(float)SampleManager.sample(audioFile).getLength());
		// control the rate of grain firing
		Envelope grainIntervalEnvelope = new Envelope(100);
		grainIntervalEnvelope.addSegment(20, 10000);
		player.setGrainInterval(grainIntervalEnvelope);
		// control the playback rate
		Envelope rateEnvelope = new Envelope(1);
		rateEnvelope.addSegment(1, 5000);
		rateEnvelope.addSegment(0, 5000);
		rateEnvelope.addSegment(0, 2000);
		rateEnvelope.addSegment(-0.1f, 2000);
		player.setRate(rateEnvelope);
		// a bit of noise can be nice
		player.getRandomnessUGen().setValue(0.01f);
        
		/*
		 * And as before...
		 */
		Gain g = new Gain(2, 0.2f);
		g.addInput(player);
		ac.out.addInput(g);
		ac.start();

	}

}