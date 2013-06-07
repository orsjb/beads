package net.beadsproject.beads.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Random;

import javax.swing.JComponent;
import net.beadsproject.beads.gui.Chooser.ChooserListener;
import net.beadsproject.beads.play.InterfaceElement;


public class Histogram implements InterfaceElement {
	
	private String name;
	private int boxWidth;
	private int boxHeight;
	private float[] sliders;
	private HistogramListener listener;
	private JComponent component;
	
	public Histogram(String name, int numSliders) {
		this.name = name;
		sliders = new float[numSliders];
		boxWidth = 10;
		boxHeight = 100;
	}
	
	public JComponent getComponent() {
		if(component == null) {
			component = new JComponent() {
				public void paintComponent(Graphics g) {
					//outer box
					g.setColor(Color.white);
					g.fillRect(0, 0, getWidth(), getHeight());
					g.setColor(Color.black);
					for(int i = 0; i < sliders.length; i++) {
						g.fillRect(i * boxWidth, (int)((1f - sliders[i]) * getHeight()), boxWidth, getHeight());
					}
					g.setColor(Color.gray);
					for(int i = 0; i < sliders.length; i++) {
						g.drawRect(i * boxWidth, 0, boxWidth, getHeight());
					}
					g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
					//name
					g.drawString(Histogram.this.name, 2, 12);
				}
			};
			component.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					int i = e.getX() / boxWidth;
					float j = 1f - (float)e.getY() / boxHeight;
					if(i >= 0 && i < sliders.length && j >= 0 && j < 1) {
						makeSelection(i, j);
						component.repaint();
					}
				}
			});
			component.addMouseMotionListener(new MouseMotionListener() {
				public void mouseDragged(MouseEvent e) {
					int i = e.getX() / boxWidth;
					float j = 1f - (float)e.getY() / boxHeight;
					if(i >= 0 && i < sliders.length && j >= 0 && j < 1) {
						makeSelection(i, j);
						component.repaint();
					}
				}
				public void mouseMoved(MouseEvent e) {}
			});
			Dimension size = new Dimension(sliders.length * boxWidth + 1, 100);
			component.setMinimumSize(size);
			component.setPreferredSize(size);
			component.setMaximumSize(size);
		}
		return component;
	}
	
	private void makeSelection(int index, float value) {
		setValue(index, value);
		if(listener != null) {
			listener.valueChanged(index, value);
		}
	}
	
	public void setValue(int index, float value) {
		if(index < sliders.length) {
			sliders[index] = value;
		}
		if(component != null) component.repaint();
	}
	
	public float getValue(int index) {
		return sliders[index];
	}
	
	public void setListener(HistogramListener listener) {
		this.listener = listener;
	}
	
	public static interface HistogramListener {
		public void valueChanged(int index, float value);
	}

	public float nextIndexByProb(Random rng) {
		float sum = 0f;
		int i = 0;
		for(i = 0; i < sliders.length; i++) {
			sum += sliders[i];
		}
		float result = rng.nextFloat() * sum;
		sum = 0f;
		for(i = 0; i < sliders.length; i++) {
			sum += sliders[i];
			if(result < sum) {
				break;
			}
		}
		return i;
	}
	
	
}
