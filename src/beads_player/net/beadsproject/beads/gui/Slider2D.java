package net.beadsproject.beads.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.data.buffers.SineBuffer;
import net.beadsproject.beads.play.InterfaceElement;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.WavePlayer;


public class Slider2D implements InterfaceElement {

	private final Slider sliderX;
	private final Slider sliderY;
	private JComponent component;
	private enum State {NORMAL, RECORD, PLAY};
	State state;
	Path path;
	
	private class Path {
		ArrayList<Float> xValues;
		ArrayList<Float> yValues;
		ArrayList<Float> times;
		double lastTime;
		
		Path(double time) {
			lastTime = time;
			xValues = new ArrayList<Float>();
			yValues = new ArrayList<Float>();
			times = new ArrayList<Float>();
		}

		public void record(double time, float xFract, float yFract) {
			float timeSinceLast = (float)(time - lastTime);
			lastTime = time;
			xValues.add(xFract);
			yValues.add(yFract);
			times.add(timeSinceLast);
		}
		
		public void play() {
			sliderX.clear();
			sliderY.clear();
			for(int i = 0; i < xValues.size(); i++) {
				float destX = sliderX.calculateValueFromFract(xValues.get(i));
				float destY = sliderY.calculateValueFromFract(yValues.get(i));
				float time = times.get(i);
				sliderX.addSegment(destX, time);
				sliderY.addSegment(destY, time, new Bead() {
					public void messageReceived(Bead message) {
						component.repaint();
					}
				});
				if(i == xValues.size() - 1) {
					sliderX.addSegment(destX, 0f, new Bead() {
						public void messageReceived(Bead message) {
							play();
						}
					});
				}
			}
		}
	}
	
	public Slider2D(Slider sliderX, Slider sliderY) {
		this.sliderX = sliderX;
		this.sliderY = sliderY;
		state = State.NORMAL;
	}

	public JComponent getComponent() {
		if(component == null) {
			component = new BeadsComponent() {
				private static final long serialVersionUID = 1L;
				public void paintComponent(Graphics g) {
					Graphics2D g2d = (Graphics2D)g;
					g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					//outer box
					g.setColor(Color.white);
					g.fillRect(0, 0, getWidth(), getHeight());
					g.setColor(Color.black);
					g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
					//path
					g.setColor(Color.lightGray);
					if(state != State.NORMAL) {
						for(int i = 0; i < path.xValues.size(); i++) {
							int xpos = (int)(sliderX.calculateFractFromValue(path.xValues.get(i)) * getWidth());
							int ypos = (int)((1f - sliderY.calculateFractFromValue(path.yValues.get(i))) * getHeight());
							g.fillOval(xpos - 3, ypos - 3, 6, 6);			
						}
					}
					
					//text
					g.setColor(Color.gray);
					g.drawString(sliderX.getName(), 2, 12);
					g.drawString("" + sliderX.getValue(), 2, 26);
					g.drawString(sliderY.getName(), 2, 40);
					g.drawString("" + sliderY.getValue(), 2, 54);
					//slider
					g.setColor(new Color(0f, 0f, 0f, 0.5f));
					int xpos = (int)(sliderX.getValueFract() * getWidth());
					int ypos = (int)((1f - sliderY.getValueFract()) * getHeight());
					g.fillOval(xpos - 5, ypos - 5, 10, 10);
					
				}
			};
			component.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					float xFract = (float)e.getX() / (float)component.getWidth();
					float yFract = 1f - (float)e.getY() / (float)component.getHeight();
					if((e.getModifiers() & MouseEvent.ALT_MASK) != 0) {
						if(state != State.RECORD) {
							path = new Path(System.currentTimeMillis());
							state = State.RECORD;
						}
					} else {
						if(state == State.RECORD) {
							state = State.PLAY;
							path.play();
						} else if(state == State.PLAY) {
							state = State.NORMAL;
						}
					}
					if(state != State.PLAY) {
						sliderX.setValueFract(xFract);
						sliderY.setValueFract(yFract);
					}
					if(state == State.RECORD) {
						path.record(System.currentTimeMillis(), xFract, yFract);
					}
					component.repaint();
				}
			});
			component.addMouseMotionListener(new MouseMotionListener() {
				public void mouseDragged(MouseEvent e) {
					float xFract = (float)e.getX() / (float)component.getWidth();
					float yFract = 1f - (float)e.getY() / (float)component.getHeight();
					sliderX.setValueFract(xFract);
					sliderY.setValueFract(yFract);
					if(state == State.RECORD) {
						path.record(System.currentTimeMillis(), xFract, yFract);
					}
					component.repaint();
				}
				public void mouseMoved(MouseEvent e) {
				}
			});
			component.setMinimumSize(new Dimension(100,100));
			component.setPreferredSize(new Dimension(100,100));
			component.setMaximumSize(new Dimension(100,100));
		}
		return component;
	}

	public Slider getSliderX() {
		return sliderX;
	}

	public Slider getSliderY() {
		return sliderY;
	}
	
	
}
