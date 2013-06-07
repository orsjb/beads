package net.beadsproject.beads.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import net.beadsproject.beads.play.InterfaceElement;

public class SingleButton implements InterfaceElement {

	public static enum Mode {
		TOGGLE, ONESHOT, HOLD
	};
	
	private JComponent component;
	private int boxWidth;
	private int textWidth;
	private int textVOffset;
	private Mode mode;
	private boolean state;
	private String label;
	private Font font;
	SingleButtonListener listener;
	
	public SingleButton(String name) {
		this(name, Mode.TOGGLE);
	}
	
	public SingleButton(String name, Mode mode) {
		this.label = name;
		listener = null;
		this.mode = mode;
		boxWidth = 10;
		textWidth = 100;
		textVOffset = 1;
		font = new Font("Courier", Font.PLAIN, 10);
		state = false;
	}
	
	public boolean state() {
		return state;
	}
	
	public void setState(boolean state) {
		if(mode == Mode.TOGGLE) {
			this.state = state;
			if(component != null) component.repaint();
		}
		if(listener != null) listener.buttonPressed(state);
	}

	public JComponent getComponent() {
		component = new JComponent() {
			private static final long serialVersionUID = 1L;
			public void paintComponent(Graphics g) {
				g.setColor(Color.white);
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setColor(Color.black);
				if(state) {
					g.fillRect(0, 0, boxWidth, boxWidth);
				} else {
					g.drawRect(0, 0, boxWidth, boxWidth);
				}
				g.setFont(font);
				g.drawString(label, boxWidth + 4, boxWidth - textVOffset);
			}
		};
		component.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
					switch(mode) {
					case TOGGLE:
						state = !state;
						break;
					case ONESHOT:
//						state = true;
						break;
					case HOLD:
						state = true;
						break;
					}
					if(listener != null) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								listener.buttonPressed(state);
							}
						});
					}
					component.repaint();
			}
			public void mouseReleased(MouseEvent e) {
				if(mode == Mode.ONESHOT && state) {
					state = false;
					component.repaint();
				} else if(mode == Mode.HOLD && state) {
					state = false;
					component.repaint();
					if(listener != null) listener.buttonPressed(state);
				}
 			}
		});
		Dimension size = new Dimension(boxWidth + 1 + textWidth, boxWidth + 1);
		component.setMinimumSize(size);
		component.setPreferredSize(size);
		component.setMaximumSize(size);
		return component;
	}
	
	public void setListener(SingleButtonListener l) {
		listener = l;
	}
	
	public static interface SingleButtonListener {
		
		public void buttonPressed(boolean newState);
		
	}
	
	public static void main(String[] args) {
		BeadsWindow f = new BeadsWindow("buttons");
		SingleButton b1 = new SingleButton("button 1", SingleButton.Mode.TOGGLE);
		b1.setListener(new SingleButtonListener() {
			public void buttonPressed(boolean newState) {
				System.out.println("B1 pressed, state = " + newState);
			}
		});
		f.add(b1.getComponent());
		SingleButton b2 = new SingleButton("button 2", SingleButton.Mode.ONESHOT);
		b2.setListener(new SingleButtonListener() {
			public void buttonPressed(boolean newState) {
				System.out.println("B2 pressed, state = " + newState);
			}
		});
		f.add(b2.getComponent());
		SingleButton b3 = new SingleButton("button 2", SingleButton.Mode.HOLD);
		b3.setListener(new SingleButtonListener() {
			public void buttonPressed(boolean newState) {
				System.out.println("B3 pressed, state = " + newState);
			}
		});
		f.add(b3.getComponent());
		f.pack();
		f.setVisible(true);
	}

	
}
