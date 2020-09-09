

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.FastGranularSamplePlayer;
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
		FastGranularSamplePlayer player = new FastGranularSamplePlayer(
				SampleManager.sample(audioFile));
		/*
		 * Have some fun with the controls.
		 */
		// loop the sample at its end points
		player.setLoopType(SamplePlayer.LoopType.LOOP_ALTERNATING);
		player.setLoopStart(new Envelope(0));
		player.setLoopEnd(new Envelope((float)SampleManager.sample(audioFile).getLength()));
		
		// control the rate of grain firing
		Envelope grainIntervalEnvelope = new Envelope(100);
		//player.setGrainInterval(grainIntervalEnvelope);
		player.setGrainInterval(grainIntervalEnvelope);
		
		// control the playback rate
		Envelope rateEnvelope = new Envelope(1);
		player.setRate(rateEnvelope);
		
		// a bit of noise can be nice
		//player.setRandomness(new Envelope(0.01f));
		player.setRandomness(new Envelope(0.01f));

		/*
		 * And as before...
		 */
		Gain g = new Gain(2, 0.2f);
		g.addInput(player);
		ac.out.addInput(g);
		ac.start();
		
		ac.logTime(true);

		while (ac.getTimeStep() < 10000) {
		    try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
		}

	    ac.stop();
	}

}
