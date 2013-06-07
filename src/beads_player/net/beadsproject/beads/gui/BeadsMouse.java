package net.beadsproject.beads.gui;

import java.awt.MouseInfo;
import java.awt.Toolkit;

public class BeadsMouse {

	public static float getXFract() {
		return (float)MouseInfo.getPointerInfo().getLocation().x / Toolkit.getDefaultToolkit().getScreenSize().width;
	}

	public static float getYFract() {
		return (float)MouseInfo.getPointerInfo().getLocation().y / Toolkit.getDefaultToolkit().getScreenSize().height;
	}
}
