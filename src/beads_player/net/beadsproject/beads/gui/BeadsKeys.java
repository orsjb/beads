package net.beadsproject.beads.gui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

public class BeadsKeys implements KeyListener {

	
	private final static ArrayList<KeyboardListener> listeners = new ArrayList<KeyboardListener>();
	public final static boolean[] keysDown = new boolean[500]; //how many key codes are there?
	public final static BeadsKeys singleton = new BeadsKeys();

	//keyListener methods
	public synchronized void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		keysDown[keyCode] = true;
		for(KeyboardListener listener: listeners) {
			listener.keyPressed(keyCode);
		}
//		System.out.println("key pressed, code = " + keyCode);
	}

	public synchronized void keyReleased(KeyEvent e) {
		int keyCode = e.getKeyCode();
		keysDown[keyCode] = false;
		for(KeyboardListener listener: listeners) {
			listener.keyReleased(keyCode);
		}
//		System.out.println("key released, code = " + keyCode);
	}

	public void keyTyped(KeyEvent e) {/*Do nothing.*/}
	//////////////////////////
	
	public static synchronized void addListener(KeyboardListener listener) {
		listeners.add(listener);
	}
	
	public static synchronized void removeListener(KeyboardListener listener) {
		listeners.remove(listener);
	}
	
	public static synchronized void clearListeners() {
		listeners.clear();
	}
	
	public static boolean keyDown(int keyCode) {
		return keysDown[keyCode];
	}
	
	public static void printKeysDown() {
		for(int i = 0; i < keysDown.length; i++) {
			if(keysDown[i]) System.out.println(i);
		}
	}
	
	public static interface KeyboardListener {
		
		public void keyPressed(int keyCode);
		public void keyReleased(int keyCode);
		
	}
	
	public static void main(String[] args) {
		BeadsWindow bw = new BeadsWindow("hello world");
//		bw.addKeyListener(BeadsKeys.singleton);
		bw.setVisible(true);
	}
	
}
