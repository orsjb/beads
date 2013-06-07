/**
 * Onset Detection demo
 * BP 170309
 *
 * This demonstrates how to use the OnsetDetector.
 * 1. Run the sketch and choose a sound file. The two parameters of the OnsetDetector are available as sliders.
 * 2. Higher alpha and threshold will result in less onsets detected.
 * 3. The display shows two visualisations of the onsets.
 */

import beads.*;
import controlP5.*;

ControlP5 controlP5;
AudioContext ac;
PeakDetector od;

class OnsetGraph extends Bead
{
  private float[] lastNOnsetStrengths;
  private float[] lastNOnsetTimes;
  private int n;
  private AudioContext ac;
  private float temporalWidth;
  private float strengthScale = 1;
  
  public OnsetGraph(AudioContext ac, int n, float timeWidth) // timeWidth in ms
  {
    this.ac = ac;
    this.n = n;
    temporalWidth = timeWidth;
    lastNOnsetStrengths = new float[n];
    lastNOnsetTimes = new float[n];
  }
  
  protected void messageReceived(Bead b)
  {  
    strengthScale -= (1+lastNOnsetStrengths[0])/n;
    
     for(int i=1;i<n;i++)
     {
       lastNOnsetTimes[i-1] = lastNOnsetTimes[i];
       lastNOnsetStrengths[i-1] = lastNOnsetStrengths[i];
     }
     lastNOnsetTimes[n-1] = (float)ac.getTime();
     lastNOnsetStrengths[n-1] = ((PeakDetector)b).getLastOnsetValue();     
     strengthScale += (1+lastNOnsetStrengths[n-1])/n;
  }
  
  public void draw(int x, int y, int w, int h)
  {
    // scale temporalWidth for the whole width
    // scale 1 strength unit for the whole height
    fill(0);
    noStroke();
    rect(x,y,w,h);
    
    noFill();
    stroke(255);
    rect(x,y,w,h);
    
    float time = (float)ac.getTime();
    
    // for each onset, draw a thin white line
    for(int i=n-1;i>=0;i--)
    {
      float msBefore = time - lastNOnsetTimes[i];
      if (msBefore > temporalWidth) break;
      else
      {
        int xpos = x+(int)(w*(1-msBefore/temporalWidth));
        float lineh = h*lastNOnsetStrengths[i]/strengthScale;
        if (lineh > h)
        {
          stroke(255,0,0);
          lineh = h;
        }
        else
        {
           stroke(255); 
        }
          
        line(xpos,y+h,xpos,y+h-lineh);        
      }
    }
  }  
}

class Flasher extends Bead
{
  private boolean flash;
  private float strength = 1;
  
  protected void messageReceived(Bead b)
  {
    flash = true;    
    strength = ((PeakDetector)b).getLastOnsetValue();
  }

  public void draw()
  {
    if (flash)
    {
      noFill();
      stroke(255);
      strokeWeight((int)strength);
      ellipse(width/2,height/2,width/2,height/2);
      strokeWeight(1);
      flash = false;
    }
  }
}

Flasher f = new Flasher();
OnsetGraph og;

void setup()
{
  size(400,400);
  frameRate(60);

  controlP5 = new ControlP5(this);
  controlP5.addSlider("threshold",0.0f,1.0f,0.2f,width-50,height-90,10,70);
  controlP5.addSlider("alpha",0.0f,1.0f,0.9f,width-80,height-90,10,70);
  
  ac = new AudioContext();
  //sample
  String audioFile = selectInput();
  SamplePlayer sp = new SamplePlayer(ac, SampleManager.sample(audioFile));
  sp.setLoopType(SamplePlayer.LoopType.LOOP_FORWARDS);
  ac.out.addInput(sp);

  //set up the chopper upper
  ShortFrameSegmenter sfs = new ShortFrameSegmenter(ac);
  sfs.addInput(ac.out);
  ac.out.addDependent(sfs);
  /*
  int chunkSize = 512;
  sfs.setChunkSize(chunkSize);
  sfs.setHopSize(chunkSize/2);
  */
  // following dixon's paper again...
  int chunkSize = 2048;
  int hopSize = 441;
  sfs.setChunkSize(chunkSize);
  sfs.setHopSize(hopSize);

  //set up the fft
  FFT fft = new FFT();
  sfs.addListener(fft);
  PowerSpectrum ps = new PowerSpectrum();
  fft.addListener(ps);
  SpectralDifference sd = new SpectralDifference(ac.getSampleRate());
  //sd.setFreqWindow(80.f,1100.f);
  ps.addListener(sd);
  od = new PeakDetector();
  sd.addListener(od);
  od.setThreshold(0.2f);
  od.setAlpha(.9f);
  od.addMessageListener(f);  
  og = new OnsetGraph(ac,100,3000);
  od.addMessageListener(og);
  ac.start();  
}

void draw()
{
  fill(0,0,0,25);
  rect(0,0,width,height);
  
  f.draw();
  og.draw(10,height-height/16,width/2,height/16);
}

public void controlEvent(ControlEvent theEvent) {
  if (theEvent!=null && theEvent.isController())
  {
    Controller c = theEvent.controller();
    if (c.name()=="threshold")
    {
      od.setThreshold(c.value());
    }
    else if (c.name()=="alpha")
    {
      od.setAlpha(c.value());      
    }
    
  }
}
