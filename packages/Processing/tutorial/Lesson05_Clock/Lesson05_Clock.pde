import beads.*;
import java.util.Arrays; 

AudioContext ac;

void setup() {
frameRate(200);
size(300,300);
ac = new AudioContext();
 /*
  * This is a boring example. See Lesson_07_Music for 
  * something more complex.
  * 
  * A Clock is an unusual UGen because it doesn't
  * have any outputs and because objects can listen
  * to it.
  * 
  * In this example, Clock just ticks. We can run a 
  * clock from an envelope which determines the rate
  * of ticking.
  * 
  * So we begin with the envelope as before.
  */
  Envelope intervalEnvelope = new Envelope(ac, 1000);
  intervalEnvelope.addSegment(600, 10000);
  intervalEnvelope.addSegment(1000, 10000);
  intervalEnvelope.addSegment(400, 10000);
  intervalEnvelope.addSegment(1000, 10000);
  /*
   * Then the clock, which gets initialised with the
   * envelope. 
   */
  Clock clock = new Clock(ac, intervalEnvelope);
  /*
   * Tell the clock to tick (you probably don't want
   * to do this except for debugging.
   */
  clock.setClick(true);
  /*
   * Now this is new, because the clock doesn't have
   * any outputs, we can't add it to the AudioContext
   * and that means it won't run. So we use the method
   * addDependent() instead.
   */
  ac.out.addDependent(clock);
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
