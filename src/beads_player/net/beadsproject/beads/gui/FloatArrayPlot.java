package net.beadsproject.beads.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

import net.beadsproject.beads.analysis.FeatureExtractor;
import net.beadsproject.beads.analysis.SegmentListener;
import net.beadsproject.beads.core.TimeStamp;
import net.beadsproject.beads.play.InterfaceElement;

public class FloatArrayPlot implements InterfaceElement, SegmentListener {

	public static enum ViewMode {
		INSTANT, HISTORY
	}
	
	JComponent component;
	ViewMode viewMode;
	float[] instantData;
	float[][] historyData; //float[time][index]
	int historyLength;
	int historyIndex;
	float min, max, range;
	boolean adaptiveRange;
	FeatureExtractor<float[], ?> extractor;
	
	public FloatArrayPlot(FeatureExtractor<float[], ?> extractor, float min, float max) {
		this(min, max);
		listenTo(extractor);
	}

	public FloatArrayPlot(FeatureExtractor<float[], ?> extractor, boolean adaptive) {
		this(adaptive);
		listenTo(extractor);
	}
	
	public FloatArrayPlot(float min, float max) {
		this.min = min;
		this.max = max;
		range = max - min;
		adaptiveRange = false;
		viewMode = ViewMode.INSTANT;
	}
	
	public FloatArrayPlot(boolean adaptive) {
		min = 0;
		max = 0;
		range = 0;
		adaptiveRange = adaptive;
		viewMode = ViewMode.INSTANT;
	}
	
	public void setViewMode(ViewMode vm) {
		viewMode = vm;
		if(viewMode == ViewMode.INSTANT) {
			historyData = null;
		} else {
			historyLength = 4000;
			historyData = new float[historyLength][extractor.getNumberOfFeatures()];
			historyIndex = 0;
		}
	}
	
	public JComponent getComponent() {
		if(component == null) {
			component = new BeadsComponent() {
				private static final long serialVersionUID = 1L;
				public void paintComponent(Graphics g) {
					g.setColor(Color.white);
					g.fillRect(0, 0, getWidth(), getHeight());
					if(viewMode == ViewMode.INSTANT) {
						if(instantData != null && range != 0) {
							float blockWidth = (float)getWidth() / instantData.length;
							g.setColor(Color.lightGray);
							for(int i = 0; i < instantData.length; i++) {
								int height = (int)((instantData[i] - min) / range * getHeight());
								g.fillRect((int)(i * blockWidth), getHeight() - 1 - height, Math.max(3, (int)blockWidth), height);
							}
						}
					} else {
						for(int i = 0; i < getWidth(); i++) {	//lazy
							int index = (int)(i / (float)getWidth() * historyLength);
							index = (historyIndex + index) % historyLength;
							for(int j = 0; j < extractor.getNumberOfFeatures(); j++) {
								int y = getHeight() - (int)(j * getHeight() / extractor.getNumberOfFeatures());
								float darkness = 1f - Math.max(0f, Math.min(1f, (historyData[index][j] - min) / range));
								g.setColor(new Color(darkness, darkness, darkness));
								g.fillRect(i, y, 1, 1);
							}
						}
					}
					g.setColor(Color.black);
					g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
				}
			};
		}
		Dimension size = new Dimension(200, 100);
		component.setMinimumSize(size);
		component.setPreferredSize(size);
		component.setMaximumSize(size);
		return component;
	}
	
	public void listenTo(FeatureExtractor<float[], ?> extractor) {
		this.extractor = extractor;
	}

	public void newSegment(TimeStamp start, TimeStamp end) {
		if(component != null && extractor != null) {
			float[] f = extractor.getFeatures();
			if(f != null) {
				if(viewMode == ViewMode.INSTANT) {
					instantData = f;
				} else {
					historyData[historyIndex] = f.clone();
					historyIndex = (historyIndex + 1) % historyLength;
				}
				if(adaptiveRange) {
					for(int i = 0; i < f.length; i++) {
						if(min > instantData[i]) min = f[i];
						if(max < instantData[i]) max = f[i];
					}
					range = max - min;
				}
				component.repaint();
			}
		}
	}

}
