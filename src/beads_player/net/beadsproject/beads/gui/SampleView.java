package net.beadsproject.beads.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JComponent;

import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.play.InterfaceElement;
import net.beadsproject.beads.ugens.SamplePlayer;

public class SampleView implements InterfaceElement {

	private static Color transparentOverlay = new Color(0.2f, 0.2f, 0.2f, 0.3f);
	private static Color veryTransparentOverlay = new Color(0.2f, 0.2f, 0.2f, 0.1f);
	private static Stroke lightStroke = new BasicStroke(0.1f);
	public static enum SelectMode {REGION, POSITION};
	public static enum SnapMode {GRID, FREE};
	
	private Sample sample;
	private int[] view;
	private int selectionStart;
	private int selectionEnd;
	private int tempSelectionMarker;
	private int height;
	private int width;
	private int chunkSize;
	private JComponent component;
	private SelectMode selectionMode;
	private SnapMode snapMode;
	private SampleViewListener listener;
	private TreeSet<Double> snapPoints; //could make this multilayered
	private double[] orderedSnapPoints;
	private SamplePlayer player;
	private BufferedImage waveForm;

	public SampleView() {
		this(null);
	}

	public SampleView(Sample sample) {
		this.sample = sample;
		height = 100;
		chunkSize = 200;
		setWidth(500);
		selectionMode = SelectMode.REGION;
		snapMode = SnapMode.FREE;
		snapPoints = new TreeSet<Double>();
		orderedSnapPoints = null;
		player = null;
	}
	
	public void bindToSamplePlayer(SamplePlayer sp) {
		player = sp;
	}
	
	public int getSelectionStartPixels() {
		return selectionStart;
	}
	
	public void setSelectionStartPixels(int selectionStart) {
		this.selectionStart = selectionStart;
	}
	
	public int getSelectionEndPixels() {
		return selectionEnd;
	}
	
	public void setSelectionEndPixels(int selectionEnd) {
		this.selectionEnd = selectionEnd;
	}
	
	public double getSelectionStartMS() {
		return pixelsToMS(selectionStart);
	}
	
	public void setSelectionStartMS(double selectionStart) {
		this.selectionStart = msToPixels(selectionStart);
	}
	
	public double getSelectionEndMS() {
		return pixelsToMS(selectionEnd);
	}
	
	public void setSelectionEndMS(double selectionEnd) {
		this.selectionEnd = msToPixels(selectionEnd);
	}
	
	public SelectMode getSelectionMode() {
		return selectionMode;
	}
	
	public void setSelectionMode(SelectMode mode) {
		this.selectionMode = mode;
	}

	public SnapMode getSnapMode() {
		return snapMode;
	}

	public void setSnapMode(SnapMode snapMode) {
		this.snapMode = snapMode;
	}
	
	public void addSnapPoint(double timeMS) {
		snapPoints.add(timeMS);
		orderedSnapPoints = null;
		waveForm = null;
	}
	
	public void clearSnapPoints() {
		snapPoints.clear();
	}
	
	public Set<Double> getSnapPoints() {
		return snapPoints;
	}
	
	//test me
	public double getSnapPointBefore(double d) {
		return snapPoints.headSet(d).last();
	}
	
	//test me
	public double getSnapPointAfter(double d) {
		SortedSet<Double> tailSet = snapPoints.tailSet(d);
		if(tailSet == null || tailSet.size() == 0) {
			return sample.getLength();
		} else {
			return tailSet.first();
		}
	}
	
	//test me
	public double getNearestSnapPoint(double d) {
		double before = getSnapPointBefore(d);
		double after = getSnapPointAfter(d);
		if(Math.abs(before - d) > Math.abs(after - d)) return after;
		else return before;
	}

	public void setSample(Sample sample) {
		this.sample = sample;
		clearSnapPoints();
		calculateOverview();
	}
	

	//TODO a nice feature would be to redraw a specified region rather than the whole thing
	public synchronized void redraw() {
		if(component != null) {
			calculateOverview();
			component.repaint();
		}
	}

	public Sample getSample() {
		return sample;
	}

	public void setWidth(int width) {
		this.width = width;
		view = new int[width];
		calculateOverview();
	}
	
	public SampleViewListener getListener() {
		return listener;
	}
	
	public void setListener(SampleViewListener listener) {
		this.listener = listener;
	}

	private void calculateOverview() {
		if(sample != null) {
			float[] frame = new float[sample.getNumChannels()];
			if(sample != null) {
				double hop = (double)sample.getNumFrames() / width;
				for(int i = 0; i < width; i++) {
					int index = (int)(i * hop);
					float average = 0;
					int maxJ = Math.min(chunkSize, (int)sample.getNumFrames() - index);
					for(int j = 0; j < maxJ; j++) {
						sample.getFrame(index + j, frame);
						average += Math.abs(frame[0]);
					}
					if(maxJ != 0) {
						average /= maxJ;
					}
					view[i] = (int)((average + 1f) * (float)height / 2f);
				}
			}
		}
		waveForm = null;
	}
	
	private void recalculateBackgroundImage() {
		//now draw to the buffered image
		waveForm = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics g = waveForm.getGraphics();
		((Graphics2D)g).setStroke(lightStroke);
		g.setColor(Color.white);
		g.fillRect(0, 0, width, height);
		g.setColor(Color.black);
		g.drawRect(0, 0, width - 1, height - 1);
		//wave
		if(view != null) {
			for(int i = 1; i < view.length; i++) {
				g.drawLine(i - 1, view[i - 1], i, view[i]);
				g.drawLine(i - 1, height - view[i - 1], i, height - view[i]);
			}
		}
		//snap points
		g.setColor(transparentOverlay);
		if(snapPoints != null) {
			if(orderedSnapPoints == null) {
				orderedSnapPoints = new double[snapPoints.size()];
				int count = 0;
				for(Double d : snapPoints) {
					int x = (int)(d * width / sample.getLength());
					g.drawLine(x, 0, x, height);
					orderedSnapPoints[count++] = d;
				}
			} else {
				for(int i = 0; i < orderedSnapPoints.length; i++) {
					int x = (int)(orderedSnapPoints[i] * width / sample.getLength());
					g.drawLine(x, 0, x, height);
				}
			}
		}
		if(component != null) {
			component.getTopLevelAncestor().repaint();
		}
	}

	public JComponent getComponent() {
		if(component == null) {
			final JComponent subComponent = new BeadsComponent() {
				public void paintComponent(Graphics g) {
					Graphics2D g2d = (Graphics2D)g;
					g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					//outer box
					if(waveForm == null) recalculateBackgroundImage();
					g.drawImage(waveForm, 0, 0, null);
					//playback
					g.setColor(transparentOverlay);
					if(player != null) {
						int x = (int)(player.getPosition() / sample.getLength() * getWidth());
						g.drawLine(x, 0, x, getHeight());
					}
					//overlay
					g.setColor(veryTransparentOverlay);
					g.fillRect(Math.min(selectionStart, selectionEnd), 0, Math.abs(selectionEnd - selectionStart), height);
				}
			};
			subComponent.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if(snapMode == SnapMode.FREE) {
						selectionStart = e.getX();
						selectionEnd = selectionStart + 1;
					} else {
						tempSelectionMarker = e.getX();
						selectionStart = (int)(width / sample.getLength() * getSnapPointBefore((float)e.getX() / width * sample.getLength()));
						selectionEnd = (int)(width / sample.getLength() * getSnapPointAfter((float)e.getX() / width * sample.getLength()));
					}
					if(listener != null) {
						listener.selectionChanged(pixelsToMS(Math.min(selectionStart, selectionEnd)), pixelsToMS(Math.max(selectionStart, selectionEnd)));
					}
					subComponent.repaint();
				}
			});
			subComponent.addMouseMotionListener(new MouseMotionListener() {
				public void mouseDragged(MouseEvent e) {
					if(e.getX() > 0 && e.getX() < width && e.getY() > 0 && e.getY() < height) {
						switch(selectionMode) {
						case REGION:
							if(snapMode == SnapMode.FREE) {
								selectionStart = e.getX();
							} else {
								int min = (int)Math.min(tempSelectionMarker, e.getX());
								int max = (int)Math.max(tempSelectionMarker, e.getX());
								selectionStart = (int)(width / sample.getLength() * getSnapPointBefore((float)min / width * sample.getLength()));
								selectionEnd = (int)(width / sample.getLength() * getSnapPointAfter((float)max / width * sample.getLength()));
							}
							break;
						case POSITION:
							if(snapMode == SnapMode.FREE) {
								selectionStart = e.getX();
							} else {
								selectionStart = (int)(width / sample.getLength() * getSnapPointBefore((float)e.getX() / width * sample.getLength()));
							}
							selectionEnd = selectionStart + 1;
							break;
						}
						if(listener != null) {
							listener.selectionChanged(pixelsToMS(Math.min(selectionStart, selectionEnd)), pixelsToMS(Math.max(selectionStart, selectionEnd)));
						}
						subComponent.repaint();
					}
				}
				public void mouseMoved(MouseEvent e) {
				}
			});
			component = subComponent;
			Dimension size = new Dimension(width, height);
			subComponent.setMinimumSize(size);
			subComponent.setPreferredSize(size);
			subComponent.setMaximumSize(size);
		}
		return component;
	}
	
	public double pixelsToMS(int pixels) {
		return (double)pixels / (double)width * sample.getLength();
	}

	public int msToPixels(double ms) {
		return (int)(ms * (double)width / sample.getLength());
	}
	
	public interface SampleViewListener {
		public void selectionChanged(double startTimeMS, double endTimeMS);
	}


}
