package net.beadsproject.beads.gui;

import java.awt.Color;

public class BeadsColors {

	public static Color nextColor() {
		return new Color((int)(Math.random() * 256), (int)(Math.random() * 256), (int)(Math.random() * 256));
	}
	
}
