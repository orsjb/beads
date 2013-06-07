package net.beadsproject.beads.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import net.beadsproject.beads.play.InterfaceElement;

public class BeadsPanel extends JPanel implements InterfaceElement {
	
	private static final long serialVersionUID = 1L;
	private int spacing = 1;
	
	public BeadsPanel() {	
		setAlignmentX(LEFT_ALIGNMENT);
		setAlignmentY(TOP_ALIGNMENT);
		setFont(new Font("Courier", Font.PLAIN, 10));
	}

	public Component add(Component c) {
		//nasty but necessary -- try to ensure that all elements are non-focusable and have BeadsKeys
		//watch out - this could have terrible consequences
		defocusAndAddKeys(c);
		return super.add(c);
	}
	
	private void defocusAndAddKeys(Component c) {
		if(!(c instanceof JComboBox)) {
			c.setFocusable(false);
			c.addKeyListener(BeadsKeys.singleton);
			if(c instanceof Container) {
				Container cont = (Container)c;
				for(Component next : cont.getComponents()) {
					defocusAndAddKeys(next);
				}
			}
		}
		
	}
	
	public void horizontalBox() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
	}
	
	public void verticalBox() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	}
	
	public void lineBorder() {
		setBorder(BorderFactory.createLineBorder(Color.black, 1));
	}
	
	public void coloredLineBorder(Color c, int thickness) {
		if(c == null) c = Color.white;
		setBorder(BorderFactory.createLineBorder(c, thickness));
	}
	
	public void emptyBorder() {
		setBorder(BorderFactory.createEmptyBorder(spacing, spacing, spacing, spacing));
	}
	
	public void lineEmptyBorder() {
		Border line = BorderFactory.createLineBorder(Color.black, 1);
		Border empty = BorderFactory.createEmptyBorder(spacing, spacing, spacing, spacing);
		Border compound = BorderFactory.createCompoundBorder(
				line, empty);
		setBorder(compound);
	}
	
	public void titledBorder(String title) {
		Border empty = BorderFactory.createEmptyBorder(spacing, spacing, spacing, spacing);
		Border titleBorder = BorderFactory.createTitledBorder(empty, title, 
				TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION, 
				new Font("Courier", Font.BOLD, 11));
		setBorder(titleBorder);
	}
	
	public void highlight() {
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.red), getBorder()));
	}
	
	public void addSpace() {
		Component space = Box.createRigidArea(new Dimension(spacing, spacing));
		space.setBackground(Color.white);
		add(space);
	}
	
	public void addVerticalSeparator() {
		addSpace();
		add(new JSeparator(SwingConstants.VERTICAL));
		addSpace();
	}
	
	public void addHorizontalSeparator() {
		addSpace();
		add(new JSeparator(SwingConstants.HORIZONTAL));
		addSpace();
	}

	public void fixSize(int i, int j) {
		fixSize(new Dimension(i, j));
		
	}
	
	public void fixSize(Dimension d) {
		setPreferredSize(d);
		setMinimumSize(d);
		setMaximumSize(d);
	}
	
	public Dimension getSize() {
		return getPreferredSize();
	}

	public JComponent getComponent() {
		return this;
	}
	
}
