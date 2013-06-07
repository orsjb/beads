package net.beadsproject.beads.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.play.InterfaceElement;

public class SampleManagerPanel implements InterfaceElement {

	private static final String newText = "New...";
	private int textVOffset;
	private JComponent component;
	private int boxHeight;
	private String hoveredBox;
	private ArrayList<String> chosenBoxes;
	private File rootDirectoryForAudio;
	private int maxSamples;

	public SampleManagerPanel() {
		this(null);
	}

	public SampleManagerPanel(String s) {
		this(s, Integer.MAX_VALUE);
	}

	public SampleManagerPanel(int x) {
		this(null, x);
	}

	public SampleManagerPanel(String s, int maxSamples) {
		boxHeight = 10;
		textVOffset = 2;
		chosenBoxes = new ArrayList<String>();
		hoveredBox = null;
		this.maxSamples = maxSamples;
		if (s != null)
			rootDirectoryForAudio = new File(s);
	}

	public void setMaxSamples(int x) {
		maxSamples = x;
	}

	public void setRootDir(String s) {
		if (s == null) {
			rootDirectoryForAudio = null;
		} else {
			rootDirectoryForAudio = new File(s);
		}
	}

	public JComponent getComponent() {
		if (component == null) {
			component = new JComponent() {
				private static final long serialVersionUID = 1L;

				public void paintComponent(Graphics g) {
					g.setColor(Color.white);
					g.fillRect(0, 0, getWidth(), getHeight());
					if (hoveredBox == newText) {
						g.setColor(Color.lightGray);
						g.fillRect(0, 0, getWidth(), boxHeight);
					}
					g.setColor(Color.black);
					g.drawString(newText, 2, boxHeight - textVOffset);
					int i = 1;
					for (String s : SampleManager.groups()) {
						if (chosenBoxes.contains(s)) {
							g.setColor(Color.darkGray);
							g.fillRect(0, i * boxHeight, getWidth(), boxHeight);
							g.setColor(Color.white);
							g.drawString(s, 2, (i + 1) * boxHeight
									- textVOffset);
						} else if (hoveredBox == s) {
							g.setColor(Color.lightGray);
							g.fillRect(0, i * boxHeight, getWidth(), boxHeight);
							g.setColor(Color.black);
							g.drawString(s, 2, (i + 1) * boxHeight
									- textVOffset);
						} else {
							g.setColor(Color.black);
							g.drawString(s, 2, (i + 1) * boxHeight
									- textVOffset);
						}
						i++;
					}
				}
			};
			component.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					List<String> groupList = SampleManager.groupsAsList();
					int index = e.getY() / boxHeight;
					if (index == 0) {
						final JFileChooser chooser = new JFileChooser(rootDirectoryForAudio);
						chooser.setMultiSelectionEnabled(true);
						chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
						int returnVal = chooser.showOpenDialog(component);
						if (returnVal == JFileChooser.APPROVE_OPTION) {
							Thread t = new Thread(new Runnable() {
								public void run() {
									File[] files = chooser.getSelectedFiles();
									for (File f : files) {
										SampleManager.group(f.getName(), f.getAbsolutePath(), maxSamples);
									}
									component.repaint();
								}
							});
							t.setPriority(Thread.MIN_PRIORITY);
							t.start();
						}
					} else {
						index--;
						if (index < groupList.size()) {
							String s = groupList.get(index);
							if ((e.getModifiers() & MouseEvent.SHIFT_MASK) == 0) {
								if (!chosenBoxes.contains(s)) {
									chosenBoxes.clear();
									chosenBoxes.add(s);
								} else {
									chosenBoxes.clear();
								}
							} else if (!component.hasFocus()) {
								if (chosenBoxes.contains(s)) {
									chosenBoxes.remove(s);
								} else {
									chosenBoxes.add(s);
								}
							}
						}
					}
					component.repaint();
				}

				public void mouseExited(MouseEvent e) {
					hoveredBox = null;
				}
			});
			component.addMouseMotionListener(new MouseMotionListener() {
				public void mouseMoved(MouseEvent e) {
					List<String> groupList = SampleManager.groupsAsList();
					int index = e.getY() / boxHeight;
					if (index == 0) {
						hoveredBox = newText;
					} else {
						index--;
						if (index < groupList.size()) {
							hoveredBox = groupList.get(index);
						}
					}
					component.repaint();
				}

				public void mouseDragged(MouseEvent e) {
				}
			});
			component.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent k) {
					if (k.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
						for (String s : chosenBoxes) {
							SampleManager.destroyGroup(s);
						}
						component.repaint();
					}
				}
			});
			Dimension d = new Dimension(100, 500);
			component.setMinimumSize(d);
			component.setPreferredSize(d);
			component.setMaximumSize(d);
		}
		return component;
	}

	public static void main(String[] args) {
		BeadsWindow b = new BeadsWindow("test sample window");
		b.content.add(new SampleManagerPanel().getComponent());
		SampleManager.group("audio", "audio");
		SampleManager.group("other", "/Users/ollie/Music/Audio/classic breaks");
		b.pack();
		b.setVisible(true);
	}

}
