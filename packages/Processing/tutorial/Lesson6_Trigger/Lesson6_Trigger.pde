import beads.*;

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
  * kills the AudioContext.
  */
  WavePlayer wp = new WavePlayer(ac, 500, Buffer.SINE);
  Gain g = new Gain(ac, 1, new Envelope(ac, 0.1));
  /*
   * In this line we make the Gain envelope do something (fade to 
   * zero over 5 seconds), and then fire an event to a listener.
   * 
   * All we have to do is tell the Envelope what listener to trigger.
   * In this case, this will kill the audio.
   */
  ((Envelope)g.getGainEnvelope()).addSegment(0, 5000, new AudioContextStopTrigger(ac));
  g.addInput(wp);
  ac.out.addInput(g);
  /*
   * The clock is just here to prove the point. Notice that the clock
   * stops ticking.
   */
  Clock c = new Clock(ac, 1000);
  c.setClick(true);
  ac.out.addDependent(c);
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
    pixels[vOffset * height + i] = fore;
  }
  updatePixels();
}
