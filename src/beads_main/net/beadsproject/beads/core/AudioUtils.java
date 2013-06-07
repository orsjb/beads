/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 * CREDIT: This class uses portions of code taken from JASS. See readme/CREDITS.txt.
 * 
 */
package net.beadsproject.beads.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Random;

/**
 * AudioUtils provides basic conversion of buffers of audio data between
 * different formats, interleaving, and some other miscellaneous audio functions.
 * 
 * @author ollie
 */
public final class AudioUtils {

	/**
	 * A handy random number generator.
	 */
	public static Random rng = new Random();
	
	/**
	 * Converts a buffer of shorts to a buffer of floats.
	 * 
	 * @param out
	 *            buffer of floats.
	 * @param in
	 *            buffer of shorts.
	 */
	static final public void shortToFloat(float[] out, short[] in) {
		for (int i = 0; i < in.length; i++) {
			out[i] = (float) (in[i] / 32768.);
		}
	}

	/**
	 * Converts a buffer of floats to a buffer of shorts.
	 * 
	 * @param out
	 *            buffer of shorts.
	 * @param in
	 *            buffer of floats.
	 */
	static final public void floatToShort(short[] out, float[] in) {
		for (int i = 0; i < in.length; i++) {
			out[i] = (short) (32767. * in[i]);
		}
	}

	/**
	 * Converts a buffer of floats to a buffer of bytes with a given byte order.
	 * 
	 * @param out
	 *            buffer of bytes.
	 * @param in
	 *            buffer of floats.
	 * @param bigEndian
	 *            true for big endian byte order, false otherwise.
	 */
	static final public void floatToByte(byte[] out, float[] in, boolean bigEndian) {
		floatToByte(out,0,in,0,in.length,bigEndian);
	}
	
	/**
	 * Converts a buffer of floats to a buffer of bytes with a given byte order.
	 * 
	 * @param out Output array
	 * @param outstart Start index of output 
	 * @param in Input array 
	 * @param instart Start index of input 
	 * @param inlength Number of floats to copy
	 * @param bigEndian Format
	 */
	static final public void floatToByte(byte[] out, int outstart, float[] in, int instart, int inlength, boolean bigEndian)
	{
		int bufsz = Math.min(inlength,in.length);
		int ib = outstart;
		if (bigEndian) {
			for (int i = 0; i < bufsz; ++i) {
				short y = (short) (32767. * Math.min(Math.max(in[i+instart], -1.0f),
						1.0f));
				out[ib++] = (byte) ((y >> 8) & 0xFF);
				out[ib++] = (byte) (y & 0xFF);
			}
		} else {
			for (int i = 0; i < bufsz; ++i) {
				short y = (short) (32767. * in[i+instart]);
				out[ib++] = (byte) (y & 0xFF);
				out[ib++] = (byte) ((y >> 8) & 0xFF);
			}
		}
	}

	/**
	 * Converts a buffer of bytes to a buffer of floats with a given byte order.
	 * 
	 * @param out
	 *            buffer of floats.
	 * @param in
	 *            buffer of bytes.
	 * @param bigEndian
	 *            true for big endian byte order, false otherwise.
	 */
	static final public void byteToFloat(float[] out, byte[] in, boolean bigEndian) {
		byteToFloat(out,in,bigEndian,out.length);
	}	

	/**
	 * Converts a buffer of bytes to a buffer of floats with a given byte order. Will copy numFloat floats to out.
	 * 
	 * @param out
	 *            buffer of floats.
	 * @param in
	 *            buffer of bytes.
	 * @param bigEndian
	 *            true for big endian byte order, false otherwise.
	 * @param numFloats
	 *            number of elements to copy into out
	 */
	static final public void byteToFloat(float[] out, byte[] in, boolean bigEndian, int numFloats) {
		byteToFloat(out,in,bigEndian,0,numFloats);
	}
	
	/**
	 * Converts a buffer of bytes to a buffer of floats with a given byte order. Will copy numFloat floats to out.
	 * 
	 * @param out
	 *            buffer of floats.
	 * @param in
	 *            buffer of bytes.
	 * @param bigEndian
	 *            true for big endian byte order, false otherwise.
	 * @param startIndexInByteArray
	 * 			  where to start copying from
	 * @param numFloats
	 *            number of elements to copy into out
	 */
	static final public void byteToFloat(float[] out, byte[] in, boolean bigEndian, int startIndexInByteArray, int numFloats) {
		byteToFloat(out,in,bigEndian,startIndexInByteArray,0,numFloats);
	}
	
	/**
	 * Converts a buffer of bytes to a buffer of floats with a given byte order. Will copy numFloat floats to out.
	 * 
	 * @param out
	 *            buffer of floats.
	 * @param in
	 *            buffer of bytes.
	 * @param bigEndian
	 *            true for big endian byte order, false otherwise.
	 * @param startIndexInByteArray
	 * 			  where to start copying from
	 * @param startIndexInFloatArray
	 * 			  where to start copying to
	 * @param numFloats
	 *            number of elements to copy into out
	 */
	static final public void byteToFloat(float[] out, byte[] in, boolean bigEndian, int startIndexInByteArray, int startIndexInFloatArray, int numFloats) {
		if (bigEndian) {
			int ib = startIndexInByteArray;
			int min = Math.min(out.length,startIndexInFloatArray+numFloats);
			for (int i = startIndexInFloatArray; i < min; ++i) {
				float sample = ((in[ib + 0] << 8) | (in[ib + 1] & 0xFF)) / 32768.0F;
				
				// BP: Could you explain the above code better?
				// How do we generalise to N-bytes per float?
				
				ib += 2;
				out[i] = sample;
			}
		} else {
			int ib = startIndexInByteArray;
			int min = Math.min(out.length,startIndexInFloatArray+numFloats);
			for (int i = startIndexInFloatArray; i < min; ++i) {
				float sample = ((in[ib] & 0xFF) | (in[ib + 1] << 8)) / 32768.0F;
				ib += 2;
				out[i] = sample;
			}
		}
	}
	
	/**
	 * Converts a buffer of bytes to a buffer of floats with a given byte order. 
	 * Will copy numFloat floats to out.
	 * 
	 * @param out
	 *            buffer of floats.
	 * @param in
	 *            buffer of bytes.
	 * @param nBytesPerFloat
	 * 			  number of bytes per float, maximum of 4           
	 * @param bigEndian
	 *            true for big endian byte order, false otherwise.
	 * @param startIndexInByteArray
	 * 			  where to start copying from
	 * @param startIndexInFloatArray
	 * 			  where to start copying to
	 * @param numFloats
	 *            number of elements to copy into out
	 */
	/*
	static final public void byteToFloatWithNBytesPerFloat(float[] out, byte[] in, int nBytesPerFloat, boolean bigEndian, int startIndexInByteArray, int startIndexInFloatArray, int numFloats) {
		float maxvalue = (float)(1 << (nBytesPerFloat*8));
		System.out.println(maxvalue);
		
		if (bigEndian) {
			int ib = startIndexInByteArray;
			int min = Math.min(out.length,startIndexInFloatArray+numFloats);
			for (int i = startIndexInFloatArray; i < min; ++i) {
				// float sample = ((in[ib + 0] << 8) | (in[ib + 1] & 0xFF)) / maxvalue;
				
				long l = 0;
				for(int k=0;k<nBytesPerFloat;k++)
					l |= (in[ib+k]&0xFF) << (8*((nBytesPerFloat-1)-k));
				float sample = l / maxvalue;
				ib += nBytesPerFloat;
				out[i] = sample;
			}
		} else {
			int ib = startIndexInByteArray;
			int min = Math.min(out.length,startIndexInFloatArray+numFloats);
			for (int i = startIndexInFloatArray; i < min; ++i) {
				// float sample = ((in[ib] & 0xFF) | (in[ib + 1] << 8)) / maxvalue;
				
				long l = 0;
				for(int k=0;k<nBytesPerFloat;k++)
					l |= (in[ib+k]&0xFF) << (8*k);
				float sample = l / maxvalue;
				
				ib += nBytesPerFloat;
				out[i] = sample;
			}
		}
	}	
	*/
	
	/**
	 * De-interleave an interleaved buffer of floats to form a 2D array of
	 * floats of size nChannels x nFrames.
	 * 
	 * @param source
	 *            interleaved buffer of floats.
	 * @param nChannels
	 *            first dimension of resulting 2D array.
	 * @param nFrames
	 *            second dimension of resulting 2D array.
	 * @param result
	 *            the result
	 */
	static final public void deinterleave(float[] source, int nChannels, int nFrames, float[][] result) {
		for (int i = 0, count = 0; i < nFrames; ++i) {
			for (int j = 0; j < nChannels; ++j) {
				result[j][i] = source[count++];
			}
		}
	}

	/**
	 * Interleave a 2D array of floats of size nChannels x nFrames to form a
	 * single interleaved buffer of floats.
	 * 
	 * @param source
	 *            2D array of floats.
	 * @param nChannels
	 *            first dimension of input 2D array.
	 * @param nFrames
	 *            second dimension of input 2D array.
	 * @param result
	 *            the result
	 */
	static final public void interleave(float[][] source, int nChannels,
			int nFrames, float[] result) {
		for (int i = 0, counter = 0; counter<result.length && i < nFrames; ++i) {
			for (int j = 0; j < nChannels; ++j) {
				result[counter++] = source[j][i];
			}
		}
	}
	
	/**
	 * Interleave a 2D array of floats of size nChannels x nFrames to form a
	 * single interleaved buffer of floats.
	 * 
	 * @param source
	 *            2D array of floats.
	 * @param nChannels
	 *            first dimension of input 2D array.
	 * @param nFrames
	 *            second dimension of input 2D array.
	 * @param offset
	 * 			  the number of frames offset
	 * @param result
	 *            the result
	 */
	static final public void interleave(float[][] source, int nChannels,
			int nFrames, int offset, float[] result) {
		for (int i = offset, counter = 0; counter<result.length && i < nFrames; ++i) {
			for (int j = 0; j < nChannels; ++j) {
				result[counter++] = source[j][i];
			}
		}
	}
	
	/**
	 * "Stretches" source into dest. Linearly interpolates.
	 * 
	 * @param source
	 * @param dest
	 */
	static final public void stretchBuffer(float[][] source, float[][] dest)
	{
		int numChannels = Math.min(source.length, dest.length);
		// this many steps in source for one step in dest
		double segStep = (source[0].length-1.0) / (dest[0].length-1.0);
		
		for (int ch=0; ch<numChannels; ch++)
		{
			float[] src = source[ch];
			float[] dst = dest[ch];
			
			// trivial case
			if (src.length==1 || dst.length==1)
			{
				dst[0] = src[0];
				continue;
			}			
			
			// each segment of src is considered a linear region that is interpolated to map onto dst,
			// for each segment, only need to calculate the gradient once
			// the first and last elements of src are mapped into the first and last elements of dst
			dst[0] = src[0];
			dst[dst.length-1] = src[src.length-1];
			
			/* algorithm:
			 * step in dst
			 * foreach element of dst figure out whether we are in a new segment of src
			 * if so, recalculate the lerp data for that segment
			 */
			
			// current segment data
			int segment = -1; // current segment, initialised to nothing
			double segstart = 0, segend = 0, seggrad = 0; // start, end, and gradient of current segment
			
			double fractionalSegment = 0;
			
			for(int i=1;i<dst.length-1;i++)		
			{
				// compute segment		
				fractionalSegment += segStep;
				int currentSegment = (int)Math.floor(fractionalSegment); 
				if (currentSegment!=segment)
				{
					// new segment, so calculate lerp data
					segment = currentSegment;
					segstart = src[segment];
					segend = src[segment+1];
					seggrad = segend - segstart;
				}
				
				// compute value from segstart, end, and grad
				dst[i] = (float) (segstart + seggrad*(fractionalSegment - currentSegment));				
			}						
		}
	}
	
	/**
	 * Reverse the data in each channel.
	 * @param buffer
	 */
	static final public void reverseBuffer(float[][] buffer)
	{
		for (float[] b: buffer)
		{
			for (int left=0, right=b.length-1; left<right; left++, right--) {
			    // exchange the first and last
			    float temp = b[left]; 
			    b[left]  = b[right]; 
			    b[right] = temp;
			}
		}
	}
	

	/**
	 * Does a freaky shortcut for calculating pow (limited to base with range 0-1), faster but less accurate than regular Math.pow(). 
	 * 
	 * <p/>CREDIT: this method is copied directly from <a href="http://martin.ankerl.com/2007/10/04/optimized-pow-approximation-for-java-and-c-c/">http://martin.ankerl.com/2007/10/04/optimized-pow-approximation-for-java-and-c-c/</a>
	 * 
	 * @param a the base.
	 * @param b the exponent.
	 * 
	 * @return the result
	 */
	public static double fastPow01(final double a, final double b) {  
		double realA = Math.max(0, Math.min(1, a));
		final int x = (int) (Double.doubleToLongBits(realA) >> 32);  
		final int y = (int) (b * (x - 1072632447) + 1072632447);  
		return Math.max(0, Math.min(1, Double.longBitsToDouble(((long) y) << 32)));  
	}  
	
	/**
	 * Attempts to determine a URL given a String. Firstly the String is interpreted as a System Resource. Failing that, it is interpreted as a proper URL. Failing that it is interpreted as
	 * a file path. All Exceptions are suppressed but the method returns null.
	 * @param s String to interpret as System Resource, URL or file path.
	 * @return a URL if successful, null otherwise.
	 */
	public static URL urlFromString(String s) {
		URL url = null;
		url = ClassLoader.getSystemResource(s);
		if(url != null) {
			return url;
		}
		try {
			url = new URL(s);
		} catch(Exception e) {
			File f = new File(s);
			try {
				url = f. toURI().toURL();
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			}
		}
		return url;
	}
	
	/**
	 * Attempts to get a File from a URL, suppressing warnings. Assumes UTF-8 encoding.
	 * @param url to get File from.
	 * @return a File if successful, null otherwise.
	 */
	public static File fileFromURL(URL url) {
		File theDirectory = null;
		try {
			theDirectory = new File(URLDecoder.decode(url.getPath(), "UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return theDirectory;
	}
	
	/**
	 * Equivalent to fileFromURL(urlFromString(s)). See these methods.
	 * @param s the String to interpret as System Resource, URL or file path.
	 * @return a File if successful, null otherwise.
	 */
	public static File fileFromString(String s) {
		return fileFromURL(urlFromString(s));
	}
 
}
