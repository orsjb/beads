import beads.*;
import java.util.Arrays; 

AudioContext ac;

void setup() {
  size(300,300);
  ac = new AudioContext();
  /*
   * In the last example, we used an Envelope to
   * control the frequency of a WavePlayer.
   * 
   * In this example, we'll use another WavePlayer.
   * This is called FM synthesis.
   * 
   * Here's the modulating WavePlayer. It has a low 
   * frequency.
   */
  WavePlayer freqModulator = new WavePlayer(ac, 50, Buffer.SINE);
  /*
   * The next line might look outrageous if you're not
   * experienced in Java. Basically we're defining a 
   * Function on the fly which takes the freqModulator
   * and maps it to a sensible range. Since the input
   * to the function is a sine wave (freqModulator), the
   * output will be a sine wave that goes from 500 to 700,
   * 50 times a second.
   */
  Function function = new Function(freqModulator) {
    public float calculate() {
      return x[0] * 100.0 + 600.0;
    }
  };
  /*
   * Here's the WavePlayer that will actually play.
   * Now we plug in the function. Compare this to the previous
   * example, where we plugged in an envelope.
   */
  WavePlayer wp = new WavePlayer(ac, function, Buffer.SINE);
  /*
   * Connect it all together as before.
   */
  Gain g = new Gain(ac, 1, 0.1);
  g.addInput(wp);
  ac.out.addInput(g);
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
