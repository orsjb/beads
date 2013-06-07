package net.beadsproject.beads.gui;

import java.awt.Frame;
import java.awt.HeadlessException;

import javax.swing.JDialog;

public class BeadsDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	public BeadsDialog(Frame owner) throws HeadlessException {
		super(owner);
		addKeyListener(BeadsKeys.singleton);
	}

	
	
}
