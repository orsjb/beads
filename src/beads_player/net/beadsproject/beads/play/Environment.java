package net.beadsproject.beads.play;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.BeadArray;
import net.beadsproject.beads.core.UGen;


// TODO: Auto-generated Javadoc
/**
 * The Class Environment.
 * 
 * @author ollie
 * 
 *         To replace Tools.
 */
public class Environment {
	
	public final Map<String, Object> elements;
	public final Map<String, BeadArray> pathways;
	public final Map<String, UGen> channels;
	public Random rng;
	public AudioContext ac;

	public Environment() {
		elements = new Hashtable<String, Object>();
		pathways = new Hashtable<String, BeadArray>();
		channels = new Hashtable<String, UGen>();
		rng = new Random();
	}
	
	public static Environment loadEnvironment(String environmentFactoryClassName, AudioContext ac) throws Exception {
		Class environmentFactoryClass = Class.forName(environmentFactoryClassName);
		Object environmentFactoryInstance = environmentFactoryClass.getConstructor(null).newInstance();
		Method initMethod = environmentFactoryClass.getMethod("createEnvironment", new Class[] {AudioContext.class});	
		Environment env = (Environment)initMethod.invoke(environmentFactoryInstance, new Object[] {ac});
		return env;
	}
	
}
