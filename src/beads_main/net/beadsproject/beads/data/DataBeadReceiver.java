/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data;

/**
 * Implementing this interface means that a class is equipped to receive and
 * parse DataBeads through the {@link #sendData(DataBead)} method. Example uses
 * include setting parameters for a UGen, triggering events, and storing data.
 * 
 * @author Benito Crawford
 * @version 0.9
 * 
 */
public interface DataBeadReceiver {

	/**
	 * The method through which a class receives and parses DataBeads. By
	 * convention, a class that implements this interface will return the
	 * DataBeadReceiver instance.
	 * <p>
	 * Example usage:
	 * <p>
	 * <code>public DataBeadReceiver sendData(DataBead db) {<br>
	 * &nbsp;&nbsp;&nbsp;someParameter = db.getFloat("someparameter", defaultValue);<br>
	 * &nbsp;&nbsp;&nbsp;someOtherParameter = db.getUGen("otherparameter");<br>
	 * &nbsp;&nbsp;&nbsp;// etc...<br>
	 * &nbsp;&nbsp;&nbsp;return this;
	 * }</code>
	 * 
	 * @param db
	 *            The DataBead message.
	 * @return Typically, the object instance: <code>this</code>.
	 */

	public DataBeadReceiver sendData(DataBead db);

}
