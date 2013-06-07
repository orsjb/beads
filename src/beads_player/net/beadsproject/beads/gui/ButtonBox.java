package net.beadsproject.beads.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.JComponent;
import net.beadsproject.beads.play.InterfaceElement;


public class ButtonBox implements InterfaceElement {

	public static enum SelectionMode {SINGLE_SELECTION, MULTIPLE_SELECTION};
	private static Color transparentOverlay = new Color(0.2f, 0.2f, 0.2f, 0.3f);
	
	private float boxWidth;
	private float boxHeight;
	private int columnHighlight;
	private int rowHighlight;
	protected boolean[][] buttons;
	private ButtonBoxListener listener;
	private int previousX = -1, previousY = -1;
	private SelectionMode selectionMode;
	private JComponent component;
	
	public ButtonBox(int width, int height) {
		this(width, height, SelectionMode.SINGLE_SELECTION);		
	}
	
	public ButtonBox(int width, int height, SelectionMode selectionMode) {
		resize(width, height);
		boxWidth = 10;
		boxHeight = 10;
		columnHighlight = -1;
		rowHighlight = -1;
		this.selectionMode = selectionMode;	
	}
	
	public void resize(int width, int height) {
		boolean[][] oldButtons = buttons;
		buttons = new boolean[width][height];
		if(oldButtons != null) {
			int minWidth = Math.min(oldButtons.length, buttons.length);
			int minHeight = Math.min(oldButtons[0].length, buttons[0].length);
			for(int i = 0; i < minWidth; i++) {
				for(int j = 0; j < minHeight; j++) {
					buttons[i][j] = oldButtons[i][j];
				}
			}
		}
	}
	
	public SelectionMode getSelectionMode() {
		return selectionMode;
	}

	public void setSelectionMode(SelectionMode selectionMode) {
		this.selectionMode = selectionMode;
	}

	public int getColumnHighlight() {
		return columnHighlight;
	}

	public void setColumnHighlight(int columnHighlight) {
		this.columnHighlight = columnHighlight;
		if(component != null) component.repaint();
	}
	
	public void clearColumnHighlight() {
		setColumnHighlight(-1);
	}

	public int getRowHighlight() {
		return rowHighlight;
	}

	public void setRowHighlight(int rowHighlight) {
		this.rowHighlight = rowHighlight;
		if(component != null) component.repaint();
	}
	
	public void clearRowHighlight() {
		setRowHighlight(-1);
	}

	public JComponent getComponent() {
		if(component == null) {
			component = new JComponent() {
				private static final long serialVersionUID = 1L;
				public void paintComponent(Graphics g) {
					//outer box
					g.setColor(Color.white);
					g.fillRect(0, 0, getWidth(), getHeight());
					for(int i = 0; i < buttons.length; i++) {
						for(int j = 0; j < buttons[i].length; j++) {
							if(buttons[i][j]) {
								g.setColor(Color.black);
								g.fillRect((int)(i * boxWidth), (int)(j * boxHeight), (int)boxWidth, (int)boxHeight);
							} else {
								g.setColor(Color.gray);
								g.drawRect((int)(i * boxWidth), (int)(j * boxHeight), (int)boxWidth, (int)boxHeight);
							}
						}
					}
					g.setColor(Color.gray);
					g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
					g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight() - 1);
					//do highlights
					if(columnHighlight >= 0) {
						g.setColor(transparentOverlay);
						g.fillRect((int)(columnHighlight * boxWidth), 0, (int)boxWidth, getHeight());
					}
					if(rowHighlight >= 0) {
						g.setColor(transparentOverlay);
						g.fillRect(0, (int)(rowHighlight * boxHeight), getWidth(), (int)boxHeight);
					}
				}
			};
			component.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					int i = (int)(e.getX() / boxWidth);
					int j = (int)(e.getY() / boxHeight);
					if(i >= 0 && i < buttons.length && j >= 0 && j < buttons[0].length) {
						makeSelection(i, j);
						component.repaint();
					}
				}
			});
			component.addMouseMotionListener(new MouseMotionListener() {
				public void mouseDragged(MouseEvent e) {
					int i = (int)(e.getX() / boxWidth);
					int j = (int)(e.getY() / boxHeight);
					if(i >= 0 && i < buttons.length && j >= 0 && j < buttons[0].length) {
						if(i != previousX || j != previousY) {
							makeSelection(i, j);
							component.repaint();
						}
					}
				}
				public void mouseMoved(MouseEvent e) {}
			});
			Dimension size = new Dimension((int)(buttons.length * boxWidth + 1), (int)(buttons[0].length * boxHeight + 1));
			component.setMinimumSize(size);
			component.setPreferredSize(size);
			component.setMaximumSize(size);
		}
		return component;
	}
	
	public void makeSelection(int i, int j) {
		switch(selectionMode) {
		case SINGLE_SELECTION:
			boolean currentValue = buttons[i][j];
			if(previousX != -1) {
				buttons[previousX][previousY] = false;
			}
			buttons[i][j] = !currentValue;
			if(listener != null) {
				if(buttons[i][j]) {
					if(previousX != i && previousY != j && previousX != -1) {
						listener.buttonOff(previousX, previousY);
					}
					listener.buttonOn(i, j);
				} else {
					listener.buttonOff(i, j);
				}
			}
			if(buttons[i][j] == false) {
				previousX = -1;
				previousY = -1;
			} else {
				previousX = i;
				previousY = j;
			}
			break;
		case MULTIPLE_SELECTION:
			buttons[i][j] = !buttons[i][j];
			if(listener != null) {
				if(buttons[i][j]) {
					listener.buttonOn(i, j);
				} else {
					listener.buttonOff(i, j);
				}
			}
			previousX = i;
			previousY = j;
			break;
		}
	}
	
	public void clear() {
		int width = buttons.length;
		int height = buttons[0].length;
		buttons = new boolean[width][height];
	}

	public static interface ButtonBoxListener {
		public void buttonOn(int i, int j);
		public void buttonOff(int i, int j);
	}

	public float getBoxWidth() {
		return boxWidth;
	}

	public void setBoxWidth(float boxWidth) {
		this.boxWidth = boxWidth;
	}
	
	public float getBoxHeight() {
		return boxHeight;
	}
	
	public void setBoxHeight(float boxHeight) {
		this.boxHeight = boxHeight;
	}

	public ButtonBoxListener getListener() {
		return listener;
	}

	public void setListener(ButtonBoxListener listener) {
		this.listener = listener;
	}

	public boolean[][] getButtons() {
		return buttons;
	}
	
	public int getWidth() {
		return buttons.length;
	}
	
	public int getHeight() {
		return buttons[0].length;
	}
	
}
