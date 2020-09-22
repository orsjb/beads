package net.beadsproject.beads.core;

import java.io.IOException;

public abstract class JackRemote {

	public static void connect(String port1, String port2) throws IOException {
		Runtime.getRuntime().exec(new String[] {"/usr/local/bin/jack_connect", port1, port2});
		System.out.println("connected " + port1 + " " + port2);
	}
	
}
