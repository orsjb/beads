/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;

/**
 * A MouseResponder is a way of getting mouse input to control audio rate data. The mouse doesn't generate audio rate data, but it is interpolated.
 * 
 * @beads.category utilities
 * @beads.category control
 */
public class MouseResponder extends UGen {

	/** The current mouse point. */
	private Point point;
	
	/** The current x value. */
	private float x;
	private float prevX;
	
	/** The current y value. */
	private float y;
	private float prevY;
	
	/** The screen width. */
	private int width;
	
	/** The screen height. */
	private int height;
	
	/**
	 * Instantiates a new MouseResponder.
	 * 
	 * @param context
	 *            the AudioContext.
	 */
	public MouseResponder(AudioContext context) {
		super(context, 2);
		width = Toolkit.getDefaultToolkit().getScreenSize().width;
		height = Toolkit.getDefaultToolkit().getScreenSize().height;
		prevX = 0;
		prevY = 0;
	}

	/**
	 * Gets the current point.
	 * 
	 * @return the point.
	 */
	public Point getPoint() {
		return point;
	}

	/* (non-Javadoc)
	 * @see com.olliebown.beads.core.UGen#calculateBuffer()
	 */
	@Override
	public void calculateBuffer() {
		point = MouseInfo.getPointerInfo().getLocation();
		x = (float)point.x / (float)width;
		y = (float)point.y / (float)height;
		for(int i = 0; i < bufferSize; i++) {
			float f = (float)i / bufferSize;
			bufOut[0][i] = f * x + (1f - f) * prevX;
			bufOut[1][i] = f * y + (1f - f) * prevY;
		}
		prevX = x;
		prevY = y;
	}

}
