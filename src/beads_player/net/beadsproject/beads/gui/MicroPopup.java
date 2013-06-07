package net.beadsproject.beads.gui;

import java.awt.Color;
import java.awt.Frame;
import java.awt.MouseInfo;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;


public class MicroPopup extends JDialog {

	private JTextField tf;

	public MicroPopup(Frame owner, String name) {
		super(owner, true);
		setLocation(MouseInfo.getPointerInfo().getLocation());
		JPanel background = new JPanel();
		background.setBackground(Color.white);
		setContentPane(background);
		tf = new JTextField();
		tf.setText(name);
		tf.selectAll();
		tf.setEditable(true);
		tf.setColumns(20);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		background.add(tf);
		setUndecorated(true);
		setAlwaysOnTop(true);
		pack();
		setVisible(true);
	}

	
	public String getText() {
		return tf.getText();
	}


}
