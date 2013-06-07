package net.beadsproject.beads.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.play.InterfaceElement;

public class LevelMeter implements InterfaceElement {

	private UGen focus;
	
	/** The component. */
	private JComponent component;
	
	private int currentLevel;
	private int numLevels;
	private int range;
	private int outputIndex;
	
	public LevelMeter(UGen focus) {
		this(focus, 0);
	}

	public LevelMeter(UGen focus, int outputIndex) {
		setFocus(focus, outputIndex);
		setNumLevels(10);
		range = 50;
		currentLevel = 0;
	}
	
	public UGen getFocus() {
		return focus;
	}

	public void setFocus(UGen focus) {
		setFocus(focus, 0);
	}
	
	public void setFocus(UGen focus, int outputIndex) {
		this.focus = focus;
		this.outputIndex = outputIndex;
	}
	
	public int getNumLevels() {
		return numLevels;
	}

	public void setNumLevels(int numLevels) {
		this.numLevels = Math.max(0, numLevels);
	}

	public JComponent getComponent() {
		if(component == null) {
			component = new BeadsComponent() {
				private static final long serialVersionUID = 1L;
				public void paintComponent(Graphics g) {
					int levelBoxHeight = getHeight() / numLevels;
					getCurrentLevel();
					//outer box
					g.setColor(Color.white);
					g.fillRect(0, 0, getWidth(), getHeight());
					g.setColor(Color.black);
					g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
					//level
					for(int i = 0; i < currentLevel; i++) {
						if(i < numLevels / 2) {
							g.setColor(Color.green);
						} else if(i < 3 * numLevels / 4f) {
							g.setColor(Color.orange);
						} else {
							g.setColor(Color.red);
						}
						g.fillRect(1, getHeight() - (i + 1) * levelBoxHeight, getWidth() - 2, levelBoxHeight - 1);
					}
				}
			};
			Dimension size = new Dimension(10,100);
			component.setMinimumSize(size);
			component.setPreferredSize(size);
			component.setMaximumSize(size);
			new Thread() {
				public void run() {
					while(isAlive()) {
						component.repaint();
						try {
							sleep(100);
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
			}.start();			//TODO bad bad bad, this can get thrown away and still be running
		}
		return component;
	}
	
	private void getCurrentLevel() {
		if(focus != null && !focus.isPaused() && !focus.isDeleted()) {
			float max = 0f;
			range = Math.min(range, focus.getContext().getBufferSize());
			for(int i = 0; i < range; i++) {
				float val = Math.abs(focus.getValue(outputIndex, i));
				if(val > max) max = val;
			}
			currentLevel = (int)(max * numLevels);
		} else {
			currentLevel = 0;
		}
	}
	
	
}
