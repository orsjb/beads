package net.beadsproject.beads.gui;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.beadsproject.beads.analysis.Analyzer;
import net.beadsproject.beads.analysis.FeatureExtractor;
import net.beadsproject.beads.analysis.featureextractors.MelSpectrum;
import net.beadsproject.beads.analysis.featureextractors.PowerSpectrum;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.Throughput;

public class AudioInputGUI extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	AudioContext ac;
	Analyzer anal;
	UGen analysisMix;
	
	public AudioInputGUI(AudioContext ac) {
		this(ac, new ArrayList<Class<? extends FeatureExtractor<?,?>>>());
		
	}
	
	public AudioInputGUI(AudioContext ac, List<Class<? extends FeatureExtractor<?,?>>> extractors) {
		this.ac = ac;
		if(!extractors.contains(MelSpectrum.class)) extractors.add(MelSpectrum.class);
		anal = new Analyzer(ac, extractors);
		anal.setFrameMemory(1);
		analysisMix = new Throughput(ac, 1);
		Throughput monitorMix = new Throughput(ac, 1);
		anal.listenTo(analysisMix); 
		anal.updateFrom(analysisMix);
		ac.out.addDependent(analysisMix);
		
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		UGen input = ac.getAudioInput();
		System.out.println("Number of inputs: " + input.getOuts());

		add(Box.createHorizontalStrut(10));
		
		Slider[] analysisGains = new Slider[input.getOuts()];
		for(int i = 0; i < analysisGains.length; i++) {
			analysisGains[i] = new Slider(ac, "Analysis " + i, 0, 2, ((i == 0) ? 1 : 0));
			Gain g = new Gain(ac, 1, analysisGains[i]);
			g.addInput(0, input, i);
			LevelMeter meter = new LevelMeter(g);
			analysisMix.addInput(g);
			add(analysisGains[i].getComponent());
			add(meter.getComponent());
		}
		
		add(Box.createHorizontalStrut(10));
		
		Slider[] monitorGains = new Slider[input.getOuts()];
		for(int i = 0; i < monitorGains.length; i++) {
			monitorGains[i] = new Slider(ac, "Monitor " + i, 0, 2, 0);
			Gain g = new Gain(ac, 1, monitorGains[i]);
			g.addInput(0, input, i);
			LevelMeter meter = new LevelMeter(g);
			monitorMix.addInput(g);
			add(monitorGains[i].getComponent());
			add(meter.getComponent());
		}
		
		add(Box.createHorizontalStrut(10));
		
		FloatArrayPlot fap = new FloatArrayPlot((FeatureExtractor<float[], ?>)anal.getElement(MelSpectrum.class), 0, 100f);
		fap.setViewMode(FloatArrayPlot.ViewMode.INSTANT);
		anal.addSegmentListener(fap);
		add(fap.getComponent());

		add(Box.createHorizontalStrut(10));
		
		OnsetView ov = new OnsetView(ac);
		ac.out.addDependent(ov);
		anal.addBeatListener(ov);
		add(ov.getComponent());
		
		add(Box.createHorizontalStrut(10));
		
		Slider gainSlider = new Slider(ac, "Master Gain", 0, 2, 1);
		ac.out.setGain(gainSlider);
		add(gainSlider.getComponent());
		LevelMeter meter = new LevelMeter(ac.out);
		add(meter.getComponent());
		
		add(Box.createHorizontalStrut(10));
		
		ac.out.addInput(monitorMix);
	}
	
	public UGen getAnalysisMix() {
		return analysisMix;
	}
	
	public static AudioInputGUI createAndShow(AudioContext ac, List<Class<? extends FeatureExtractor<?,?>>> extractors) {
		AudioInputGUI newGui = new AudioInputGUI(ac, extractors);
		JFrame f = new JFrame();
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.add(Box.createVerticalStrut(10));
		p.add(newGui);
		p.add(Box.createVerticalStrut(10));
		f.setContentPane(p);
		f.pack();
		f.setResizable(false);
		f.setVisible(true);
		return newGui;
	}

	public Analyzer getAnalyzer() {
		return anal;
	}
	

}
