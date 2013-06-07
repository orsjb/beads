package net.beadsproject.beads.gui;

import javax.swing.JFrame;
import net.beadsproject.beads.data.buffers.SineBuffer;
import net.beadsproject.beads.events.PauseTrigger;
import net.beadsproject.beads.play.Environment;
import net.beadsproject.beads.play.SongPart;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.WavePlayer;




public class ExampleSongPart extends SongPart {
	
	public ExampleSongPart(String name, Environment environment) {
		super(name, environment);
		WavePlayer wp = new WavePlayer(context, (float)Math.random() * 5000f + 100f, new SineBuffer().getDefault());
		addInput(wp);
		Slider s = new Slider(getContext(), "gain", 0f, 1f, 0.5f);
		setGainEnvelope(s);
		JFrame frame = new JFrame();
		frame.add(s.getComponent());
		frame.pack();
		frame.setVisible(true);
	}

	@Override
	public void enter() {
		getGainEnvelope().setValue(0.1f);
		((Envelope)getGainEnvelope()).addSegment(0.2f, 1000f);
	}

	@Override
	public void exit() {
		((Envelope)getGainEnvelope()).addSegment(0f, 1000f, new PauseTrigger(this));
	}
	
	
}
