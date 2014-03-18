

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.SamplePlayer;


public class Lesson04_SamplePlayer {

	public static void main(String[] args) {

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
		String audioFile = "audio/1234.aif";
		// SampleManager.setBufferingRegime(Sample.Regime.newStreamingRegime(1000));
		SamplePlayer player = new SamplePlayer(ac, SampleManager
				.sample(audioFile));
		/*
		 * And as before...
		 */
		Gain g = new Gain(ac, 2, 0.2f);
		g.addInput(player);
		ac.out.addInput(g);
		ac.start();
		

	}
}
