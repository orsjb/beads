import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.SamplePlayer;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;


public class Lesson04_SamplePlayer_Test {

	@Test
	public void Lesson04_SamplePlayer()
			throws IOException,
			InterruptedException {
		AudioContext ac;

		ac = new AudioContext();
		/*
		 * Here's how to play back a sample.
		 *
		 * The first line gives you a way to choose the audio file. The
		 * (commented, optional) second line allows you to stream the audio
		 * rather than loading it all at once. The third line creates a sample
		 * player and loads in the Sample. SampleManager is a utility which
		 * keeps track of loaded audio files according to their file names, so
		 * you don't have to load them again.
		 */
		// SampleManager.setBufferingRegime(Sample.Regime.newStreamingRegime(1000));
		Sample sample = SampleManager.sample(SampleManagerTest.audioFilePath);
		Assert.assertNotNull(sample);
		SamplePlayer player = new SamplePlayer(ac, sample);
		/*
		 * And as before...
		 */
		Gain g = new Gain(ac, 2, 0.2f);
		g.addInput(player);
		ac.out.addInput(g);
		ac.start();
		Thread.sleep(2000);
		ac.stop();
	}
}
