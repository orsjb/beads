import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.GranularSamplePlayer;
import net.beadsproject.beads.ugens.SamplePlayer;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class Lesson08_Granulation_Test {

	@Test
	public void Lesson08_Granulation()
			throws IOException, InterruptedException {
		AudioContext ac;

		ac = new AudioContext();
		/*
		 * In lesson 4 we played back samples. This example is almost the same
		 * but uses GranularSamplePlayer instead of SamplePlayer. See some of
		 * the controls below.
		 */
		Sample sample = SampleManager.sample(SampleManagerTest.audioFilePath);
		Assert.assertNotNull(sample);
		GranularSamplePlayer player = new GranularSamplePlayer(ac,
				sample);
		/*
		 * Have some fun with the controls.
		 */
		// loop the sample at its end points
		player.setLoopType(SamplePlayer.LoopType.LOOP_ALTERNATING);
		player.getLoopStartUGen().setValue(0);
		player.getLoopEndUGen().setValue(
				(float) sample.getLength());
		// control the rate of grain firing
		Envelope grainIntervalEnvelope = new Envelope(ac, 100);
		grainIntervalEnvelope.addSegment(20, 10000);
		player.setGrainInterval(grainIntervalEnvelope);
		// control the playback rate
		Envelope rateEnvelope = new Envelope(ac, 1);
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
		Gain g = new Gain(ac, 2, 0.2f);
		g.addInput(player);
		ac.out.addInput(g);
		ac.start();
		Thread.sleep(10500);
		ac.stop();
	}
}
