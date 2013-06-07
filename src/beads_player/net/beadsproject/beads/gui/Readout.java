package net.beadsproject.beads.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.sun.org.apache.xpath.internal.axes.SubContextList;

import net.beadsproject.beads.play.InterfaceElement;

public class Readout implements InterfaceElement {

	private JLabel label;
	private String text;
	private JComponent component;
	private int boxHeight;
	private int boxWidth;
	private int textVOffset;
	
	public Readout() {
		boxHeight = 10;
		boxWidth = 100;
		textVOffset = 1;
		label = new JLabel();
		label.setFont(new Font("Courier", Font.PLAIN, 10));
	}
	
	public Readout(String label, String text) {
		this();
		setLabel(label);
		setText(text);
	}
	
	public void setText(String text) {
		this.text = text;
		if(component != null) {
			component.repaint();
		}
	}
	
	public void setLabel(String label) {
		String tempLabel = label;
		while(tempLabel.length() < 7) {
			tempLabel = tempLabel + " ";
		}
		this.label.setText(tempLabel);
		if(component != null) {
			component.repaint();
		}
	}
	
	public String getText() {
		return text;
	}
	
	public String getLabel() {
		return label.getText();
	}
	
	public JComponent getComponent() {
		if(component == null) {
			JComponent subComponent = new JComponent() {
				private static final long serialVersionUID = 1L;
				public void paintComponent(Graphics g) {
					g.setColor(Color.white);
					g.fillRect(0, 0, getWidth(), getHeight());
					if(text != null) {
						g.setColor(Color.black);
						g.drawString(text, 0, boxHeight - textVOffset);
					}
				}
			};
			Dimension size = new Dimension(boxWidth, boxHeight);
			BeadsPanel bp = new BeadsPanel();
			bp.horizontalBox();
			bp.add(label);
			bp.add(subComponent);
			component = bp;
			component.setMinimumSize(size);
			component.setPreferredSize(size);
			component.setMaximumSize(size);
		}
		return component;
	}

}
