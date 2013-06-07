package net.beadsproject.beads.gui;

import java.awt.Font;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

public class BeadsList extends JList {
	
	private static final long serialVersionUID = 1L;
	DefaultListModel dm;
	
	public BeadsList() {
		dm = new DefaultListModel();
		setModel(dm);
		setFont(new Font("Courier", Font.PLAIN, 10));
		setAlignmentX(LEFT_ALIGNMENT);
		setAlignmentY(TOP_ALIGNMENT);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}
	
	public void addElement(Object element) {
		dm.addElement(element);
	}

	public Object getLastSelected() {
		Object[] selected = getSelectedValues();
		if(selected.length == 0) {
			return null;
		} else {
			return selected[selected.length - 1];
		}
	}

}
