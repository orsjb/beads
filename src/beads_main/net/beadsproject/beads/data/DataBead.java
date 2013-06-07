/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data;

import net.beadsproject.beads.core.*;

import java.lang.reflect.Method;
import java.util.*;

/**
 * A bead that stores properties as key/value pairs. Keys must be Strings, and
 * values may be any Object. Implements the Map interface.
 * 
 * @author Benito Crawford
 * @version 0.9.6
 */
public class DataBead extends Bead implements Map<String, Object> {
	public Map<String, Object> properties;

	/**
	 * Creates a DataBead instance with no defined properties. Properties may be
	 * added with {@link #put(String, Object) put()}.
	 */
	public DataBead() {
		properties = new Hashtable<String, Object>();
	}

	/**
	 * Creates a DataBead with one property defined by the specified key and
	 * value. Other properties may be added with {@link #put(String, Object)
	 * put()}.
	 * 
	 * @param key
	 *            The property name.
	 * @param val
	 *            The property value.
	 */
	public DataBead(String key, Object val) {
		properties = new Hashtable<String, Object>();
		if (key != null)
			properties.put(key, val);
	}

	/**
	 * Creates a DataBead instance with properties specified by a String array
	 * that are set to corresponding values specified by an Object array. Other
	 * properties may be added with {@link #put(String, Object) put()}.
	 * 
	 * @param proparr
	 *            The array of property names.
	 * @param valarr
	 *            The array of Object values.
	 * 
	 */
	public DataBead(String[] proparr, Object[] valarr) {
		properties = new Hashtable<String, Object>();
		if (proparr != null && valarr != null) {
			int s = Math.min(proparr.length, valarr.length);
			for (int i = 0; i < s; i++) {
				if (proparr[i] != null)
					properties.put(proparr[i], valarr[i]);
			}
		}
	}

	/**
	 * Creates a DataBead instance that uses a Map (a Hashtable, for example)
	 * for its properties. (This does not copy the input Map, so any changes to
	 * it will change the properties of the DataBead!) Other properties may be
	 * added with {@link #put(String, Object) put()}.
	 * 
	 * @param ht
	 *            The input Map.
	 */
	public DataBead(Map<String, Object> ht) {
		if (ht == null) {
			properties = new Hashtable<String, Object>();
		} else {
			properties = ht;
		}
	}

	/**
	 * Creates a new DataBead from an interleaved series of key-value pairs,
	 * which must be in the form (String, Object, String, Object...), etc.
	 * 
	 * @param objects
	 *            interleaved series of key-value pairs.
	 */
	public DataBead(Object... objects) {
		properties = new Hashtable<String, Object>();
		putAll(objects);
	}

	/**
	 * If the input message is a DataBead, this adds the properties from the
	 * message Bead to this one. (Equivalent to {@link #putAll(DataBead)} .)
	 */
	public void messageReceived(Bead message) {
		if (message instanceof DataBead) {
			putAll(((DataBead) message).properties);
		}
	}

	/**
	 * Adds the properties from the input DataBead to this one.
	 * 
	 * @param db
	 *            The input DataBead.
	 */
	public void putAll(DataBead db) {
		putAll(db.properties);
	}

	/**
	 * Adds an interleaved series of key-value pairs to the DataBead, which must
	 * be in the form (String, Object, String, Object...), etc.
	 * 
	 * @param objects
	 *            an interleaved series of key-value pairs.
	 */
	public void putAll(Object... objects) {
		for (int i = 0; i < objects.length; i += 2) {
			put((String) objects[i], objects[i + 1]);
		}
	}

	/**
	 * Uses the parameters stored by this DataBead, this method configures the
	 * given object by using reflection to discover appropriate setter methods.
	 * For example, if the object has a method <code>setX(float f)</code> then
	 * the key-value pair <String "x", float 0.5f> will be used to invoke this
	 * method. Errors are caught and printed (actually, not right now...).
	 * <p>
	 * Be aware that this may not work as expected with all objects. Use with
	 * care...
	 * 
	 * @param o
	 *            the Object to configure.
	 */
	public void configureObject(Object o) {
		if (o instanceof DataBeadReceiver) {
			((DataBeadReceiver) o).sendData(this);
		} else {
			for (String s : properties.keySet()) {
				// generate the correct method name
				String methodName = "set" + s.substring(0, 1).toUpperCase()
						+ s.substring(1);
				// get the arg object
				Object theArg = properties.get(s);
				try {
					// find the correct method, with appropriate argument type
					// (hope this works with primitives)
					Method m = o.getClass().getMethod(methodName,
							theArg.getClass());
					// set it
					m.invoke(o, theArg);
				} catch (Exception e) {
					// ignore exceptions
				}
			}
		}
	}

	/**
	 * Gets a float representation of the specified property; returns the
	 * specified default value if that property doesn't exist or cannot be cast
	 * as a float.
	 * <p>
	 * This method is a useful way to update <code>float</code> parameters in a
	 * class:
	 * <p>
	 * <code>float param = startval;<br>
	 * ...<br>
	 * <code>param = databead.getFloat("paramKey", param);</code>
	 * 
	 * @param key
	 *            The property key.
	 * @param defaultVal
	 *            The value to return if the property does not contain a
	 *            float-convertible value.
	 * @return The property value, or the default value if there is no float
	 *         representation of the property.
	 */
	public float getFloat(String key, float defaultVal) {
		Float ret;
		if ((ret = getFloatObject(key)) == null) {
			return defaultVal;
		} else
			return ret;
	}

	/**
	 * Gets a Float representation of the specified property; returns
	 * <code>null</code> if that property doesn't exist or cannot be cast as a
	 * Float.
	 * 
	 * @param key
	 *            The property key.
	 * @return The property value, or the default value if there is no float
	 *         representation of the property.
	 */

	public Float getFloatObject(String key) {
		Object o = get(key);
		if (o instanceof Number) {
			return ((Number) o).floatValue();
		} else if (o instanceof String) {
			try {
				Float r = Float.parseFloat((String) o);
				return r;
			} catch (Exception e) {
			}
		} else if (o instanceof Boolean) {
			if ((Boolean) o == true) {
				return 1f;
			} else {
				return 0f;
			}
		}
		return null;
	}

	/**
	 * Returns the UGen value for the specified key. If the value stored at the
	 * key is not a UGen, it returns <code>null</code>.
	 * 
	 * @param key
	 *            The key.
	 * @return The UGen if it exists.
	 */
	public UGen getUGen(String key) {
		Object o = get(key);
		if (o instanceof UGen) {
			return (UGen) o;
		} else {
			return null;
		}
	}

	/**
	 * Gets a float array from the value stored with the specified key. If the
	 * stored value is actually of type <code>float[]</code>, the method returns
	 * that object. In the event that the stored value is an array of numbers of
	 * some other type, the method will return a new float array filled with
	 * values converted to float; an array of doubles, for instance, will be
	 * recast as floats. Single numbers will be returned as a one-element float
	 * array. If no array can be formed from the stored value, or if there is no
	 * stored value, the method returns <code>null</code>.
	 * 
	 * @param key
	 *            The key.
	 * @return The derived float array.
	 */
	public float[] getFloatArray(String key) {
		Object o = get(key);
		float[] ret;
		if (o instanceof Number[]) {
			Number[] n = (Number[]) o;
			ret = new float[n.length];
			for (int i = 0; i < n.length; i++) {
				ret[i] = n[i].floatValue();
			}
		} else if (o instanceof float[]) {
			ret = (float[]) o;
		} else if (o instanceof double[]) {
			double[] p = (double[]) o;
			ret = new float[p.length];
			for (int i = 0; i < p.length; i++) {
				ret[i] = (float) p[i];
			}
		} else if (o instanceof int[]) {
			int[] p = (int[]) o;
			ret = new float[p.length];
			for (int i = 0; i < p.length; i++) {
				ret[i] = (float) p[i];
			}
		} else if (o instanceof long[]) {
			long[] p = (long[]) o;
			ret = new float[p.length];
			for (int i = 0; i < p.length; i++) {
				ret[i] = (float) p[i];
			}
		} else if (o instanceof char[]) {
			char[] p = (char[]) o;
			ret = new float[p.length];
			for (int i = 0; i < p.length; i++) {
				ret[i] = (float) p[i];
			}
		} else if (o instanceof byte[]) {
			byte[] p = (byte[]) o;
			ret = new float[p.length];
			for (int i = 0; i < p.length; i++) {
				ret[i] = (float) p[i];
			}
		} else if (o instanceof short[]) {
			short[] p = (short[]) o;
			ret = new float[p.length];
			for (int i = 0; i < p.length; i++) {
				ret[i] = (float) p[i];
			}
		} else if (o instanceof boolean[]) {
			boolean[] p = (boolean[]) o;
			ret = new float[p.length];
			for (int i = 0; i < p.length; i++) {
				if (p[i] == true) {
					ret[i] = 1;
				} else {
					ret[i] = 0;
				}
			}
		} else if (o instanceof Number) {
			ret = new float[] { ((Number) o).floatValue() };
		} else {
			ret = null;
		}

		return ret;
	}

	/**
	 * Gets an array of UGens from the value stored with the specified key. If
	 * the value is a UGen object (not an array), the method returns a new
	 * one-element UGen array with the value stored in it. If the value is
	 * empty, or not a UGen array or UGen, the method returns <code>null</code>.
	 * 
	 * @param key
	 *            The key.
	 * @return The UGen array.
	 */
	public UGen[] getUGenArray(String key) {
		Object o = get(key);
		if (o instanceof UGen[]) {
			return (UGen[]) o;
		} else if (o instanceof UGen) {
			return new UGen[] { (UGen) o };
		} else {
			return null;
		}
	}

	/**
	 * Returns a new DataBead with a shallow copy of the the original DataBead's
	 * properties.
	 */
	@Override
	public DataBead clone() {
		DataBead ret = new DataBead();
		ret.setName(getName());
		ret.putAll(properties);
		return ret;
	}

	/**
	 * Creates a new DataBead that combines properties from both input
	 * DataBeads. If the same key exists in both, the value from the first one
	 * is used.
	 * 
	 * @param a
	 *            The first input DataBead.
	 * @param b
	 *            The second input DataBead.
	 * @return The new DataBead.
	 */
	public static DataBead combine(DataBead a, DataBead b) {
		DataBead c = new DataBead();
		c.putAll(b);
		c.putAll(a);
		return c;
	}

	@Override
	public String toString() {
		return super.toString() + ":\n" + properties.toString();
	}

	/*
	 * These implement the Map interface methods.
	 */

	public boolean containsKey(Object key) {
		return properties.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return properties.containsValue(value);
	}

	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		return properties.entrySet();
	}

	public Object get(Object key) {
		return properties.get(key);
	}

	public boolean isEmpty() {
		return properties.isEmpty();
	}

	public Set<String> keySet() {
		return properties.keySet();
	}

	public Object put(String key, Object value) {
		return properties.put(key, value);
	}

	public void putAll(Map<? extends String, ? extends Object> m) {
		properties.putAll(m);
	}

	public Object remove(Object key) {
		return properties.remove(key);
	}

	public int size() {
		return properties.size();
	}

	public Collection<Object> values() {
		return properties.values();
	}

	public void clear() {
		properties.clear();
	}

}
