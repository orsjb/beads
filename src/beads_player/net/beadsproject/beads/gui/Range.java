package net.beadsproject.beads.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.text.NumberFormat;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.JFrame;

import net.beadsproject.beads.play.InterfaceElement;

public class Range implements InterfaceElement {

	private static Color transparentOverlay = new Color(0.2f, 0.2f, 0.2f, 0.3f);
	
	private JComponent component;
	private String name;
	private float min;
	private float max;
	private float rangeMin;
	private float rangeMax;
	private boolean dragMax;
	NumberFormat nf;
	
	public Range(String name, float min, float max, float rangeMin, float rangeMax) {
		this.name = name;
		this.min = min;
		this.max = max;
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
		dragMax = false;
		checkRange();
		nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(6);
	}

	private void checkRange() {
		if(rangeMin > rangeMax) {
			dragMax = !dragMax;
			float temp = rangeMin;
			rangeMin = rangeMax;
			rangeMax = temp;
		}
		if(rangeMin < min) rangeMin = min;
		if(rangeMax > max) rangeMax = max;
	}
	
	public JComponent getComponent() {
		if(component == null) {
			component = new BeadsComponent() {
				private static final long serialVersionUID = 1L;
				public void paintComponent(Graphics g) {
					Graphics2D g2d = (Graphics2D)g;
					//outer box
					g.setColor(Color.white);
					g.fillRect(0, 0, getWidth(), getHeight());
					g.setColor(Color.gray);
					g2d.rotate(-Math.PI / 2f);
					g.drawString(name, -getHeight() + 2, 12);
					g.drawString(nf.format(rangeMin), -getHeight() + 2, 26);
					g.drawString(nf.format(rangeMax), -getHeight() + 2 + getHeight() / 3, 26);
					g2d.rotate(Math.PI / 2f);
					g.setColor(Color.black);
					g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
					int minHeight = valueToPixel(rangeMin);
					int maxHeight = valueToPixel(rangeMax);
					g.setColor(transparentOverlay);
					g.fillRect(0, maxHeight, getWidth(), minHeight - maxHeight);
					g.setColor(Color.black);
					g.drawLine(0, minHeight, getWidth(), minHeight);
					g.drawLine(0, maxHeight, getWidth(), maxHeight);
				}
			};
			component.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {	
					float mouseVal = pixelToValue(e.getY());
					if(Math.abs(mouseVal - rangeMax) < Math.abs(mouseVal - rangeMin)) {
						dragMax = true;
						rangeMax = mouseVal;
					} else {
						dragMax = false;
						rangeMin = mouseVal;
					}
					checkRange();
					component.repaint();
				}
			});
			component.addMouseMotionListener(new MouseMotionListener() {
				public void mouseDragged(MouseEvent e) {
					if(dragMax) {
						rangeMax = pixelToValue(e.getY());
					} else {
						rangeMin = pixelToValue(e.getY());
					}
					checkRange();
					component.repaint();
				}
				public void mouseMoved(MouseEvent e) {
				}
			});
			Dimension size = new Dimension(30,100);
			component.setMinimumSize(size);
			component.setPreferredSize(size);
			component.setMaximumSize(size);
		}
		return component;
	}
	
	private int valueToPixel(float value) {
		if(component == null) return 0;
		return (int)((1f - (value - min) / (max - min)) * component.getHeight());
	}
	
	private float pixelToValue(int pixel) {
		if(component == null) return 0f;
		return (1f - (float)pixel / component.getHeight()) * (max - min) + min;
	}
	
	public float getRangeMin() {
		return rangeMin;
	}

	public void setRangeMin(float rangeMin) {
		this.rangeMin = rangeMin;
		checkRange();
		if(component != null) component.repaint();
	}

	public float getRangeMax() {
		return rangeMax;
	}
	
	public void setRangeMax(float rangeMax) {
		this.rangeMax = rangeMax;
		checkRange();
		if(component != null) component.repaint();
	}
	
	public float randomInRange(Random rng) {
		return rng.nextFloat() * (rangeMax - rangeMin) + rangeMin;
	}

	public static void main(String[] args) {
		BeadsWindow w = new BeadsWindow("test");
		Range r = new Range("Sample length", 0f, 1f, 0.1f, 0.2f);
		w.content.add(r.getComponent());
		w.pack();
		w.setVisible(true);
		
	}

}
