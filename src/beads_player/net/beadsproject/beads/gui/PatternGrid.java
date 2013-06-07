package net.beadsproject.beads.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.AudioUtils;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.Pitch;
import net.beadsproject.beads.events.KillTrigger;
import net.beadsproject.beads.gui.SingleButton.Mode;
import net.beadsproject.beads.play.Pattern;
import net.beadsproject.beads.play.PatternPlayer;
import net.beadsproject.beads.play.PatternPlayer.ContinuousPlayMode;
import net.beadsproject.beads.ugens.Clock;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.WavePlayer;

/**
 * Interface for a Pattern object. Can save and load.
 */
public class PatternGrid extends ButtonBox {

	private Pattern pattern;
	private PatternPlayer patternPlayer;
	private JComponent component;
	private int numNotesVisible;
	private Chooser loopChooser;
	
	public PatternGrid(int width, int height) {
		super(width, height, SelectionMode.MULTIPLE_SELECTION);
		pattern = new Pattern();
		patternPlayer = new PatternPlayer(pattern);
		numNotesVisible = 20;
		patternPlayer.setLoop(width);
		patternPlayer.setContinuousPlayMode(ContinuousPlayMode.INTERNAL);
		setListener(new ButtonBoxListener() {
			public void buttonOff(int i, int j) {
				pattern.removeEvent(i, j);
			}
			public void buttonOn(int i, int j) {
				pattern.addEvent(i, j);
			}
		});
	}
	
	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
		patternPlayer.setPattern(pattern);
		if(loopChooser != null) loopChooser.setChoice(patternPlayer.getLoop() - 1);
		resize(patternPlayer.getLoop(), numNotesVisible);
		setBoxWidth(200f / patternPlayer.getLoop());	//TODO unhardwire
		setBoxHeight(100f / numNotesVisible);	//TODO unhardwire
		setBBFromPattern();
		if(component != null) component.repaint();
	}
	
	public void setLoop(int loop) {
		patternPlayer.setLoop(loop);
		resize(loop, numNotesVisible);
		setBoxWidth(200f / patternPlayer.getLoop());
		if(component != null) component.repaint();
		if(super.getComponent() != null) super.getComponent().repaint();
	}
	
	private void setBBFromPattern() {
		clear();
		for(Integer i : pattern.getEvents()) {
			if(i < buttons.length) {
				ArrayList<Integer> sounds = pattern.getEventAtIndex(i);
				if(sounds != null) {
					for(Integer j : sounds) {
						if(j < buttons[i].length) {
							buttons[i][j] = true;
						}
					}
				}
			}
		}
	}
	
	public ArrayList<Integer> goToStep(int index) {
		//work out the column to highlight
		ArrayList<Integer> event = patternPlayer.getEventAtStep(index);
		setColumnHighlight(patternPlayer.getLastIndex());	//does this break if the continuous update mode changes?
		//the return the data
		return event;
	}
	
	public JComponent getComponent() {
		if(component == null) {
			JComponent bb = super.getComponent();
			final BeadsPanel mainPanel = new BeadsPanel();
			mainPanel.add(bb);
			final BeadsPanel buttonPanel = new BeadsPanel();
			buttonPanel.verticalBox();
			mainPanel.add(buttonPanel);
			SingleButton sb = new SingleButton("Read", Mode.ONESHOT);
			sb.setListener(new SingleButton.SingleButtonListener() {
				public void buttonPressed(boolean newState) {
					JFileChooser chooser = new JFileChooser();
					int returnVal = chooser.showOpenDialog(mainPanel);
			        if (returnVal == JFileChooser.APPROVE_OPTION) {
						try {
							read(chooser.getSelectedFile().getAbsolutePath());
						} catch (Exception e1) {
							e1.printStackTrace();
						}
			        } 
				}
			});
			buttonPanel.add(sb.getComponent());
			sb = new SingleButton("Write", Mode.ONESHOT);
			sb.setListener(new SingleButton.SingleButtonListener() {
				public void buttonPressed(boolean newState) {
					JFileChooser chooser = new JFileChooser();
					int returnVal = chooser.showSaveDialog(mainPanel);
			        if (returnVal == JFileChooser.APPROVE_OPTION) {
						try {
							write(chooser.getSelectedFile().getAbsolutePath());
						} catch (Exception e1) {
							e1.printStackTrace();
						}
			        }
				}
			});
			buttonPanel.add(sb.getComponent());
			loopChooser = new Chooser("Loop");
			for(int i = 1; i < 33; i++) {
				loopChooser.add("" + i);
			}
			loopChooser.setListener(new Chooser.ChooserListener() {
				public void choice(String s) {
					setLoop(Integer.parseInt(s));
				}
			});
			loopChooser.setChoice(patternPlayer.getLoop() - 1);
			buttonPanel.add(loopChooser.getComponent());
			component = mainPanel;
		}
		return component;
	}
	
	public Pattern getPattern() {
		return pattern;
	}
	
	public PatternPlayer getPatternPlayer() {
		return patternPlayer;
	}
	
	public void read(String filename) {
		URL url = AudioUtils.urlFromString(filename);
		if(url == null) {
			return;
		}
		try {
			ObjectInputStream ois = new ObjectInputStream(url.openStream());
			pattern = (Pattern)ois.readObject();
			patternPlayer.setPattern(pattern);
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		setBBFromPattern();
		if(component != null) component.repaint();
	}
	
	public void write(String filename) {
		File f = new File(filename);
		pattern.setSuggestedLoop(patternPlayer.getLoop());
		try {
			FileOutputStream fos = new FileOutputStream(f);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(pattern);
			oos.close();
			fos.close();
		} catch(Exception e) {
			//suppress
		}
	}
	
}
