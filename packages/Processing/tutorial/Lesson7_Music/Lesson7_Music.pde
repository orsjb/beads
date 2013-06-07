import beads.*;

AudioContext ac;

void setup() {
frameRate(200);
size(300,300);
ac = new AudioContext();
 /*
  * In this example a Clock is used to trigger events. We do this
  * by adding a listener to the Clock (which is of type Bead).
  * 
  * The Bead is made on-the-fly. All we have to do is to
  * give the Bead a callback method to make notes.
  * 
  * This example is more sophisticated than the previous
  * ones. It uses nested code.
  */
 Clock clock = new Clock(ac, 700);
 clock.addMessageListener(
  //this is the on-the-fly bead
  new Bead() {
    //this is the method that we override to make the Bead do something
     public void messageReceived(Bead message) {
        Clock c = (Clock)message;
        if(c.isBeat()) {
          WavePlayer wp = new WavePlayer(ac, (float)Math.random() * 3000 + 100, Buffer.SINE);
          Gain g = new Gain(ac, 1, new Envelope(ac, 0.1));
          ((Envelope)g.getGainEnvelope()).addSegment(0, 1000, new KillTrigger(g));
          g.addInput(wp);
          ac.out.addInput(g);
       }
     }
   }
 );
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
    pixels[vOffset * height + i] = fore;
  }
  updatePixels();
}
