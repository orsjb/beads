

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.Noise;

public class Lesson01_AudioContext {

	public static void main(String[] args) {

		/*
		 * Lesson 1: Make some noise! Note, if you don't know Java, you'd
		 * be well advised to follow some of the Java tutorials first.
		 */
		AudioContext ac;

		/*
		 * Make an AudioContext. This class is always the starting point for any
		 * Beads project. You need it to define various things to do with audio
		 * processing. It also connects the the JavaSound system and provides
		 * you with an output device.
		 * 
		 * UPDATE: As of Beads (v3.2), Default Audio Contexts (defaultcontext)
		 * can now be used. Please see below for more details.
		 */
		ac = new AudioContext();
		/*
		 * Make a noise-making object. Noise is a type of Class known as a UGen.
		 * UGens have some number of audio inputs and audio outputs and do some
		 * kind of audio processing or generation. Notice that UGens always get
		 * initialised with the AudioContext.
		 */
		Noise n = new Noise(ac);
		/*
		 * Make a gain control object. This is another UGen. This has a few more
		 * arguments in its constructor: the second argument gives the number of
		 * channels, and the third argument can be used to initialise the gain
		 * level.
		 */
		Gain g = new Gain(ac, 1, 0.1f);
		/*
		 * Now things get interesting. You can plug UGens into other UGens,
		 * making chains of audio processing units. Here we're just going to
		 * plug the Noise object into the Gain object, and the Gain object into
		 * the main audio output (ac.out). In this case, the Noise object has
		 * one output, the Gain object has one input and one output, and the
		 * ac.out object has two inputs. The method addInput() does its best to
		 * work out what to do. For example, when connecting the Gain to the out
		 * object, the output of the Gain object gets connected to both channels
		 * of the output object.
		 */
		g.addInput(n);
		ac.out.addInput(g);
		/*
		 * Finally, start things running.
		 */
		ac.start();
		
		/*
		 * UPDATE: As of Beads v3.2, all UGens have a shared default Audio
		 * Context in them that can be accessed through AudioContext.getDefaultContext().
		 * You will no longer need to explicitly create AudioContext ac and insert this
		 * when making new UGen objects. This defaultcontext will provide the same effects
		 * as creating a new AudioContext without any information. If you want to specify
		 * particular information such as the device or AudioIO you want to use, you will
		 * need to make your own AudioContext object for that, as shown in this lesson.
		 * 
		 * Lesson02_EnvelopeAndWavePlayer onwards will instead be using the 
		 * defaultcontext.  
		 */
	}

}
