package net.beadsproject.beads.gui;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;

import javax.swing.JFrame;

public class BeadsWindow extends JFrame {

	private static final long serialVersionUID = 1L;
	final public BeadsPanel content;
	private boolean antialias;
	
	public BeadsWindow(String string) {
		super(string);
		content = new BeadsPanel();
		content.horizontalBox();
		content.emptyBorder();
		getContentPane().add(content);
		addKeyListener(BeadsKeys.singleton);
		setAntiAlias(false);
	}
	
	public Component add(Component c) {
		return content.add(c);
	}
	
	public void remove(Component c) {
		content.remove(c);
	}
	
	public void setAntiAlias(boolean aa) {
		antialias = aa;
	}
	
	public void setVisible(boolean vis) {
		super.setVisible(vis);
		if(vis) {
			if(antialias) {
				Graphics2D g2d = (Graphics2D)getGraphics();
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			}
		}
	}
}
