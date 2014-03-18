import beads.*;
import java.util.Arrays; 

/*
 * Lesson 1: Make some noise! Note, if you don't know Processing, you'd
 * be well advised to follow some of the Processing tutorials first.
 */
 
AudioContext ac;

void setup() {
  size(300,300);
  /*
   * Make an AudioContext. This class is always the starting point for 
   * any Beads project. You need it to define various things to do with 
   * audio processing. It also connects the the JavaSound system and
   * provides you with an output device.
   */
   ac = new AudioContext();
  /* 
   * Make a noise-making object. Noise is a type of Class known as a
   * UGen. UGens have some number of audio inputs and audio outputs
   * and do some kind of audio processing or generation. Notice that
   * UGens always get initialised with the AudioContext.
   */
  Noise n = new Noise(ac);
  /* 
   * Make a gain control object. This is another UGen. This has a few
   * more arguments in its constructor: the second argument gives the
   * number of channels, and the third argument can be used to initialise
   * the gain level.
   */
  Gain g = new Gain(ac, 1, 0.1);
  /*
   * Now things get interesting. You can plug UGens into other UGens, 
   * making chains of audio processing units. Here we're just going to
   * plug the Noise object into the Gain object, and the Gain object
   * into the main audio output (ac.out). In this case, the Noise object
   * has one output, the Gain object has one input and one output, and
   * the ac.out object has two inputs. The method addInput() does its
   * best to work out what to do. For example, when connecting the Gain
   * to the out object, the output of the Gain object gets connected to
   * both channels of the output object.
   */
  g.addInput(n);
  ac.out.addInput(g);
  /*
   * Finally, start things running.
   */
  ac.start();
}

/*
 * Here's the code to draw a scatterplot waveform.
 * The code draws the current buffer of audio across the
 * width of the window. To find out what a buffer of audio
 * is, read on.
 * 
 * Start with some spunky colors.
 */
color fore = color(255, 102, 204);
color back = color(0,0,0);

/*
 * Just do the work straight into Processing's draw() method.
 */
void draw() {
  loadPixels();
  //set the background
  Arrays.fill(pixels, back);
  //scan across the pixels
  for(int i = 0; i < width; i++) {
    //for each pixel work out where in the current audio buffer we are
    int buffIndex = i * ac.getBufferSize() / width;
    //then work out the pixel height of the audio data at that point
    int vOffset = (int)((1 + ac.out.getValue(0, buffIndex)) * height / 2);
    //draw into Processing's convenient 1-D array of pixels
    vOffset = min(vOffset, height);
    pixels[vOffset * height + i] = fore;
  }
  updatePixels();
}
