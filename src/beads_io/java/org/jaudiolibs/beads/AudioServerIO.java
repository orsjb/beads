
package org.jaudiolibs.beads;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.AudioIO;
import net.beadsproject.beads.core.UGen;

import org.jaudiolibs.audioservers.AudioClient;
import org.jaudiolibs.audioservers.AudioConfiguration;
import org.jaudiolibs.audioservers.AudioServer;
import org.jaudiolibs.audioservers.AudioServerProvider;
import org.jaudiolibs.audioservers.ext.ClientID;
import org.jaudiolibs.audioservers.ext.Connections;
import org.jaudiolibs.audioservers.ext.Device;
import org.jaudiolibs.audioservers.javasound.JSTimingMode;

/**
 *
 * @author Neil C Smith http://neilcsmith.net
 */
public abstract class AudioServerIO extends AudioIO implements AudioClient {
  
    protected AudioServer server;
    protected AudioConfiguration config;   
    private List<FloatBuffer> inputs;
    
    public AudioServerIO() {
    }
   
    @Override
    protected UGen getAudioInput(int[] channels) {
        return new RTInput(context, channels);
    }

    public void configure(AudioConfiguration ac) throws Exception {
        if (config.getSampleRate() != ac.getSampleRate()
                || config.getInputChannelCount() != ac.getInputChannelCount()
                || config.getOutputChannelCount() != ac.getOutputChannelCount()
                || config.getMaxBufferSize() != ac.getMaxBufferSize()
                || !ac.isFixedBufferSize()) {
            System.out.println("Unexpected audio configuration");
            throw new IllegalArgumentException("Unexpected audio configuration");
        }
    }
	
	
	protected boolean runThread() {
        Thread audioThread = new Thread(new Runnable() {
			
            public void run() {
                try {
                    server.run();
                } catch (Exception ex) {
                    Logger.getLogger(AudioServerIO.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }, "audio");
        audioThread.setPriority(Thread.MAX_PRIORITY);
        audioThread.start();
        return true;
    }

    public boolean process(long time, List<FloatBuffer> inputs, List<FloatBuffer> outputs, int nFrames) {
        if (!context.isRunning()) {
            return false;
        }
        this.inputs = inputs;
        update();
        for (int i=0; i < outputs.size(); i++ ) {
            outputs.get(i).put(context.out.getOutBuffer(i));
        }
        this.inputs = null;
        return true;
    }

    public void shutdown() {
        // no op
    }
    
    private class RTInput extends UGen {

		private int[] channels;

        /**
         * Create UGen with default AudioContext
         * @param channels number of channels
         */
        RTInput(int[] channels){
            this(getDefaultContext(), channels);
        }

		RTInput(AudioContext context, int[] channels) {
			super(context, channels.length);
			this.channels = channels;
		}

		@Override
		public void calculateBuffer() {
			for (int i=0; i < channels.length; i++) {
                            inputs.get(channels[i]-1).get(bufOut[i]);
                        }
		}
		
	}
    
    /**
    *
    * @author Neil C Smith http://neilcsmith.net
    */
    public static class Jack extends AudioServerIO {
	   
    	private String name = "Beads";
         
       	public Jack() {
       		super();
       	}
       	
       	public Jack(String name) {
       		super();
       		this.name = name;
       	}
    
        protected boolean start() {
           System.out.println("Starting Jack implementation of AudioServerIO");
           config = new AudioConfiguration(
                   context.getSampleRate(),
                   context.getAudioFormat().inputs,
                   context.getAudioFormat().outputs,
                   context.getBufferSize(),
                   new ClientID(name),
                   Connections.ALL);
           
           String jaudioLib = "JACK";
           
           AudioServerProvider provider = null;
           for (AudioServerProvider p : ServiceLoader.load(AudioServerProvider.class)) {
               if (jaudioLib.equals(p.getLibraryName())) {
                   provider = p;
                   break;
               }
           }
           if (provider == null) {
               throw new NullPointerException("No AudioServer found that matches : " + jaudioLib);
           }
           
   		   try {
               server = provider.createServer(config, this);
           } catch (Exception e) {
               e.printStackTrace();
           }
   		   
           return runThread();
       }
    }
   
    public static class JavaSound extends AudioServerIO {
	   
       JSTimingMode jsTiming = JSTimingMode.FramePosition;
	   String device = null;
	   String name = "Beads";
	   
	   /**
	    * Creates Javasound Server object as the audio output format,
	    * using JAudioLibs interface. The default audio output device
	    * will be used.
	    */
	   public JavaSound() {
		   super();
		   // If user's OS is Windows, change JSTiming to Estimated for better performance.
		   if (System.getProperty("os.name").toLowerCase().contains("win")) 
		       jsTiming = JSTimingMode.Estimated;
	   }

       /**
        * Creates Javasound Server object as the audio output format,
        * using JAudioLibs interface.
        * 
        * @param device the name of the output device
        */
	   public JavaSound(String device) {
		   super();
		   this.device = device;
	       // If user's OS is Windows, change JSTiming to Estimated for better performance.
           if (System.getProperty("os.name").toLowerCase().contains("win")) 
               jsTiming = JSTimingMode.Estimated;
	   }
	   
	   protected boolean start() {
		   System.out.println("Starting JavaSound implementation of AudioServerIO");
		   
		   if (device != null) {
    		   config = new AudioConfiguration(
                       context.getSampleRate(),
                       context.getAudioFormat().inputs,
                       context.getAudioFormat().outputs,
                       context.getBufferSize(),
                       new ClientID(name),
                       Connections.ALL,
                       jsTiming,
                       new audioDevice(device)
                       );
		   } else {
	           config = new AudioConfiguration(
	                   context.getSampleRate(),
	                   context.getAudioFormat().inputs,
	                   context.getAudioFormat().outputs,
	                   context.getBufferSize(),
	                   new ClientID(name),
	                   Connections.ALL,
	                   jsTiming
	                   );		       
		   }
		   
           String jaudioLib = "JavaSound";
           
           AudioServerProvider provider = null;
           for (AudioServerProvider p : ServiceLoader.load(AudioServerProvider.class)) {
               if (jaudioLib.equals(p.getLibraryName())) {
                   provider = p;
                   break;
               }
           }
           if (provider == null) {
               throw new NullPointerException("No AudioServer found that matches : " + jaudioLib);
           }
           
           try {
               server = provider.createServer(config, this);
           } catch (Exception e) {
               e.printStackTrace();
           }
           
		   return runThread();
	   }
    }
    
    /**
     * Creates a Device object containing information about the desired output 
     * device. AudioDevices may be used as inputs when creating a JavaSound server
     * if you want to specify an output that is not your system's default output
     * device.
     *
     */
    protected class audioDevice extends Device {

        protected audioDevice(String name, int maxInputChannels, int maxOutputChannels) {
            super(name, maxInputChannels, maxOutputChannels);
        }
        
        protected audioDevice(String name) {
            super(name, 1, 1);
        }
    }
}
