package net.beadsproject.beads.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.play.InterfaceElement;
import net.beadsproject.beads.ugens.Clock;


public class TimeGraph extends Bead implements InterfaceElement {
	
	private int boxWidth;
	private int range;
	private int currentTime;
	private JComponent component;
	
	public TimeGraph() {
		this(10);
	}
	
	public TimeGraph(int range) {
		this.range = range;
		boxWidth = 10;
		currentTime = 0;
		component = null;
	}
	
	public JComponent getComponent() {
		if(component == null) {
			component = new JComponent() {
				private static final long serialVersionUID = 1L;
				public void paintComponent(Graphics g) {
					//outer box
					g.setColor(Color.white);
					g.fillRect(0, 0, getWidth(), getHeight());
					for(int i = 0; i < range; i++) {
						for(int j = i; j < range; j++) {
							if(currentTime % (i + 1) == 0 && currentTime % (j + 1) == 0) {
								g.setColor(Color.black);
								g.fillRect(i * boxWidth, j * boxWidth, boxWidth, boxWidth);
							} else {
								g.setColor(Color.gray);
								g.drawRect(i * boxWidth, j * boxWidth, boxWidth, boxWidth);
							}
						}
					}
					g.setColor(Color.gray);
					g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
					g.setColor(Color.black);
					g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
				}
			};
			Dimension size = new Dimension(range * boxWidth + 1, range * boxWidth + 1);
			component.setMinimumSize(size);
			component.setPreferredSize(size);
			component.setMaximumSize(size);
		}
		return component;
	}
	
	public int getRange() {
		return range;
	}

	public void setRange(int range) {
		this.range = range;
	}
	
	public void messageReceived(Bead message) {
		Clock c = (Clock)message;
		if(c.isBeat()) {
			currentTime = c.getBeatCount();
			component.repaint();
		}
	}
	
}
