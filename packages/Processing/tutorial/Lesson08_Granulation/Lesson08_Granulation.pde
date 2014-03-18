import beads.*;
import java.util.Arrays; 

AudioContext ac;

void setup() {
  size(300,300);
  ac = new AudioContext();
  selectInput("Select an audio file:", "fileSelected");
}

/*
 * This code is used by the selectInput() method to get the filepath.
 */
void fileSelected(File selection) {
  /*
   * In lesson 4 we played back samples. This example
   * is almost the same but uses GranularSamplePlayer
   * instead of SamplePlayer. See some of the controls below.
   */
  String audioFileName = selection.getAbsolutePath();
  Sample sample = SampleManager.sample(audioFileName);
  GranularSamplePlayer player = new GranularSamplePlayer(ac, sample);
  /*
   * Have some fun with the controls.
   */
   //loop the sample at its end points
   player.setLoopType(SamplePlayer.LoopType.LOOP_ALTERNATING);
   player.getLoopStartEnvelope().setValue(0);
   player.getLoopEndEnvelope().setValue((float)sample.getLength());
   //control the rate of grain firing
   Envelope grainIntervalEnvelope = new Envelope(ac, 30);
   grainIntervalEnvelope.addSegment(20, 10000);
   player.setGrainIntervalEnvelope(grainIntervalEnvelope);
   //control the playback rate
   Envelope rateEnvelope = new Envelope(ac, 1);
   rateEnvelope.addSegment(1, 5000);
   rateEnvelope.addSegment(0, 5000);
   rateEnvelope.addSegment(0, 2000);
   rateEnvelope.addSegment(-0.1, 2000);
   player.setRateEnvelope(rateEnvelope);
   //a bit of noise can be nice
   player.getRandomnessEnvelope().setValue(0.02);
   /*
   * And as before...
   */
  Gain g = new Gain(ac, 2, 0.2);
  g.addInput(player);
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
