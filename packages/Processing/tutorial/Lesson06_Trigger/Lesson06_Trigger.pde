import beads.*;
import java.util.Arrays; 

AudioContext ac;

void setup() {
frameRate(200);
size(300,300);
ac = new AudioContext();
 /*
  * How do you trigger events to happen in the future? 
  *
  * Here's one example where an Envelope is used to trigger
  * the event. The event itself is a special class that
  * kills another object, in this case a master gain object.
  */
  
  /*
   * Here is the master gain object.
   */
  Gain masterGain = new Gain(ac, 1, 1);
  
  /*
   * Now two things. Firstly, a WavePlayer with an Envelope controlling
   * its frequency, connected to a Gain.
   */
  Envelope freqEnv = new Envelope(ac, 250);
  WavePlayer wp = new WavePlayer(ac, freqEnv, Buffer.SINE);
  Gain g1 = new Gain(ac, 1, 0.3);
  g1.addInput(wp);
  
  /*
   * Secondly, just another WavePlayer connected to a Gain (no Envelope).
   */
  WavePlayer wp2 = new WavePlayer(ac, 255, Buffer.SQUARE);
  Gain g2 = new Gain(ac, 1, 0.1);
  g2.addInput(wp2);
  
  /*
   * Connect them both to the master gain.
   */
  masterGain.addInput(g1);
  masterGain.addInput(g2); 
  
  /*
   * In this line we make the Gain envelope do something (fade to 
   * zero over 5 seconds), and then fire an event to a listener.
   * 
   * All we have to do is tell the Envelope what listener to trigger.
   * In this case, this will stop the second sound by killing g2.
   *
   * Note that "killing" any UGen causes it to be removed from the signal
   * chain, and causes all of the things "upstream" from it to be
   * removed as well. 
   */
  freqEnv.addSegment(500, 5000, new KillTrigger(g2));
  
  /*
   * Now play
   */
  ac.out.addInput(masterGain);
  ac.start();
  
  /*
   * Some other objects that can trigger events: 
   * Clock, DelayTrigger, PeakDetector.
   * 
   * Some other objects that can respond to events:
   * AudioContextStopTrigger, Pattern, PauseTrigger.
   * 
   * It is easy to make your own trigger, by subclassing Bead. 
   * The following event gets added an extra 1s after the previous one.
   */
   
   Bead myTrigger = new Bead() {
     public void messageReceived(Bead message) {
        System.out.println("I've been triggered!"); 
     }
   };
   freqEnv.addSegment(0, 1000, myTrigger);
   
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
