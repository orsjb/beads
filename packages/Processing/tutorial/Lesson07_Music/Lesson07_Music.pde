import beads.*;
import java.util.Arrays; 

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
    int pitch;
     public void messageReceived(Bead message) {
        Clock c = (Clock)message;
        if(c.isBeat()) {
          //choose some nice frequencies
          if(random(1) < 0.5) return;
          pitch = Pitch.forceToScale((int)random(12), Pitch.dorian);
          float freq = Pitch.mtof(pitch + (int)random(5) * 12 + 32);
          WavePlayer wp = new WavePlayer(ac, freq, Buffer.SINE);
          Gain g = new Gain(ac, 1, new Envelope(ac, 0));
          g.addInput(wp);
          ac.out.addInput(g);
          ((Envelope)g.getGainEnvelope()).addSegment(0.1, random(200));
          ((Envelope)g.getGainEnvelope()).addSegment(0, random(7000), new KillTrigger(g));
       }
       if(c.getCount() % 4 == 0) {
           //choose some nice frequencies
          int pitchAlt = pitch;
          if(random(1) < 0.2) pitchAlt = Pitch.forceToScale((int)random(12), Pitch.dorian) + (int)random(2) * 12;
          float freq = Pitch.mtof(pitchAlt + 32);
          WavePlayer wp = new WavePlayer(ac, freq, Buffer.SQUARE);
          Gain g = new Gain(ac, 1, new Envelope(ac, 0));
          g.addInput(wp);
          Panner p = new Panner(ac, random(1));
          p.addInput(g);
          ac.out.addInput(p);
          ((Envelope)g.getGainEnvelope()).addSegment(random(0.1), random(50));
          ((Envelope)g.getGainEnvelope()).addSegment(0, random(400), new KillTrigger(p));
       }
       if(c.getCount() % 8 == 0) {
          Noise n = new Noise(ac);
          Gain g = new Gain(ac, 1, new Envelope(ac, 0.05));
          g.addInput(n);
          Panner p = new Panner(ac, random(0.5, 1));
          p.addInput(g);
          ac.out.addInput(p);
          ((Envelope)g.getGainEnvelope()).addSegment(0, random(100), new KillTrigger(p));
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
    vOffset = min(vOffset, height);
    pixels[vOffset * height + i] = fore;
  }
  updatePixels();
}
