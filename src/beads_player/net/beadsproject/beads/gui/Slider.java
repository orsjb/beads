package net.beadsproject.beads.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JWindow;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.buffers.Exp01Buffer;
import net.beadsproject.beads.data.buffers.Log01Buffer;
import net.beadsproject.beads.data.buffers.SineBuffer;
import net.beadsproject.beads.play.InterfaceElement;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.WavePlayer;



// TODO: Auto-generated Javadoc
/**
 * The Class Slider.
 * 
 * @author ollie
 * 
 *         A UGen which provides a GUI slider for audio control. incomplete
 */
public class Slider extends Envelope implements InterfaceElement {

	/** The min. */
	private float min;
	
	/** The max. */
	private float max;
	
	/** The value. */
	private float value;
	
	/** The component. */
	private JComponent component;
	
	private Vector<Float> storedValues;
	
	private float smoothnessInterval;
	
	private String name;
	
	private Buffer logBuffer;
	private Buffer expBuffer;
	
	/**
	 * Instantiates a new slider.
	 * 
	 * @param context
	 *            the context
	 * @param name
	 *            the name
	 * @param min
	 *            the min
	 * @param max
	 *            the max
	 * @param value
	 *            the value
	 */
	public Slider(AudioContext context, String nam, float min, float max, float val) {
		this(context, nam, min, max, val, false);
	}
	
	public Slider(AudioContext context, String nam, float min, float max, float val, boolean log) {
		super(context);
		storedValues = new Vector<Float>();
		name = nam;
		setMin(Math.min(min, max));
		setMax(Math.max(min, max));
		setValue(val);
		smoothnessInterval = 20;
		useLogBuffer(log);
	}
	
	public float calculateValueFromFract(float fract) {
		if(logBuffer != null) fract = expBuffer.getValueFraction(fract);
		return Math.max(min, Math.min(max, fract * (max - min) + min));
	}
	
	public void setValueFract(float fract) { 
		if(logBuffer != null) fract = expBuffer.getValueFraction(fract);
		setValue(fract * (max - min) + min);
	}
	
	public float getValueFract() {
		float fract = (value - min) / (max - min);
		if(logBuffer != null) fract = logBuffer.getValueFraction(fract);
		return fract;
	}
	
	public float calculateFractFromValue(float value) {
		float fract = (value - min) / (max - min);
		if(logBuffer != null) fract = logBuffer.getValueFraction(fract);
		return fract;
	}
	
	public String getName() {
		return name;
	}
	
	public void setValue(float val) {
		this.value = Math.max(min, Math.min(max, val));
		clear();
		addSegment(value, smoothnessInterval);
		if(component != null) component.repaint();
	}
	
	/**
	 * Gets the min.
	 * 
	 * @return the min
	 */
	public float getMin() {
		return min;
	}
	
	/**
	 * Sets the min.
	 * 
	 * @param min
	 *            the new min
	 */
	public void setMin(float min) {
		this.min = min;
	}
	
	/**
	 * Gets the max.
	 * 
	 * @return the max
	 */
	public float getMax() {
		return max;
	}
	
	/**
	 * Sets the max.
	 * 
	 * @param max
	 *            the new max
	 */
	public void setMax(float max) {
		this.max = max;
	}
	
	

	public float getSmoothnessInterval() {
		return smoothnessInterval;
	}

	public void setSmoothnessInterval(float smoothnessInterval) {
		this.smoothnessInterval = smoothnessInterval;
	}

	public void calculateBuffer() {
		super.calculateBuffer();
		value = myBufOut[0];
		if(component != null && context.getTimeStep() % 4 == 0) component.repaint();
	}
	
	/**
	 * Gets the panel.
	 * 
	 * @return the panel
	 */
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
					g.drawString("" + value, -getHeight() + 2, 26);
					g2d.rotate(Math.PI / 2f);
					g.setColor(Color.black);
					g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
					//value fract
					float fract = getValueFract();
					int sliderHeight = (int)(getHeight() * (1f - fract));
					if(isLocked()) {
						g.setColor(Color.gray);
					}
					g.drawLine(0, sliderHeight, getWidth(), sliderHeight);
				}
			};
			component.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					if((e.getModifiers() & MouseEvent.CTRL_MASK) != 0) {
						runEnvelopeDrawPanel();
					} else if((e.getModifiers() & MouseEvent.ALT_MASK) != 0) {
						runTextSelectPanel();
					} else {
						if(!isLocked()) {
							setValueFract(1f - (float)e.getY() / (float)component.getHeight());
						}
					}
				}
			});
			component.addMouseMotionListener(new MouseMotionListener() {
				public void mouseDragged(MouseEvent e) {
					if(!isLocked()) {
						float valueFract = 1f - (float)e.getY() / (float)component.getHeight();
						valueFract = Math.min(Math.max(valueFract, 0), 1);
						setValueFract(valueFract);
					}
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

	private void runEnvelopeDrawPanel() {
		final int[] x = new int[2];
		x[0] = x[1] = 0;
		final int[] y = new int[2];
		y[0] = y[1] = (int)((1f - getValueFract()) * component.getHeight());
		final JPanel drawPanel = new BeadsPanel() {
			private static final long serialVersionUID = 1L;
			public void paintComponent(Graphics g) {
				Graphics2D g2d = (Graphics2D)g;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(Color.white);
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setColor(Color.black);
				g.drawLine(x[0], y[0], x[1], y[1]);
			}
		};
		final BeadsDialog drawWindow = new BeadsDialog((Frame)component.getTopLevelAncestor());
		drawWindow.addMouseMotionListener(new MouseMotionListener() {
			public void mouseDragged(MouseEvent e) {
			}
			public void mouseMoved(MouseEvent e) {
				x[1] = e.getX();
				y[1] = e.getY();
				drawPanel.repaint();
			}
		});
		drawWindow.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				drawWindow.dispose();
				setValue(calculateValueFromFract(1f - y[0] / (float)component.getHeight()));
				addSegment(calculateValueFromFract(1f - y[1] / (float)component.getHeight()), x[1] / (float)drawPanel.getWidth() * 10000f);
			}
		});
		drawWindow.addWindowFocusListener(new WindowFocusListener() {
			public void windowGainedFocus(WindowEvent e) {
			}
			public void windowLostFocus(WindowEvent e) {
				drawWindow.dispose();
			}
		});
		drawWindow.setContentPane(drawPanel);
		drawWindow.setUndecorated(true);
		drawWindow.setSize(new Dimension(component.getWidth() + 200, component.getHeight()));
		drawWindow.setLocation(new Point(component.getLocationOnScreen().x + component.getWidth(), component.getLocationOnScreen().y));
		drawWindow.setVisible(true);
	}
	
	public void storeValue(float f) {
		if(!storedValues.contains(f)) {
			storedValues.add(f);
		}
	}
	
	public void runTextSelectPanel() {
		final BeadsDialog drawWindow = new BeadsDialog((Frame)component.getTopLevelAncestor());
		final JPanel drawPanel = new BeadsPanel();
		final JComboBox selector = new JComboBox(storedValues);
		selector.setEditable(true);
		selector.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object selection = selector.getSelectedItem();
				try {
					float f = Float.parseFloat(selection.toString());
					storeValue(f);
					Slider.this.setValue(f);
					drawWindow.dispose();
				} catch(Exception ex) {
					//do nothing
				}
			}
		});
		selector.setSize(new Dimension(100,0));
		drawPanel.add(selector);
		drawPanel.setSize(new Dimension(100,0));
		drawWindow.addWindowFocusListener(new WindowFocusListener() {
			public void windowGainedFocus(WindowEvent e) {
			}
			public void windowLostFocus(WindowEvent e) {
				drawWindow.dispose();
			}
		});
		drawWindow.setContentPane(drawPanel);
		drawWindow.setUndecorated(true);
		drawWindow.pack();
		drawWindow.setLocation(new Point(MouseInfo.getPointerInfo().getLocation().x, MouseInfo.getPointerInfo().getLocation().y));
		drawWindow.setVisible(true);
	}
	
	public void useLogBuffer(boolean useLogBuffer) {
		if(useLogBuffer) {
			logBuffer = new Log01Buffer().getDefault();
			expBuffer = new Exp01Buffer().getDefault();
		} else {
			logBuffer = null;
			expBuffer = null;
		}
	}

	
}
