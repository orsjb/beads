package net.beadsproject.beads.core.io;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.AudioIO;
import net.beadsproject.beads.core.AudioUtils;
import net.beadsproject.beads.core.IOAudioFormat;
import net.beadsproject.beads.core.UGen;

import static net.beadsproject.beads.core.io.AudioFormatTools.ChannelConfigAndEncoding;
import static net.beadsproject.beads.core.io.AudioFormatTools.calcChannelConfigAndEncodingForInputs;
import static net.beadsproject.beads.core.io.AudioFormatTools.calcChannelConfigAndEncodingForOutputs;

// TODO fix audio playback on Android
public class AndroidSoundAudioIO extends AudioIO {
    private static final String TAG = "AndroidAudioIO";
//    public static final int SAMPLES_PER_FRAME = 2;
//    public static final int BYTES_PER_FRAME = SAMPLES_PER_FRAME * BYTES_PER_SAMPLE;

//    public static final int FRAME_RATE = 48000;
//    private static final int FRAMES_PER_BUFFER = 240;

    /**
     * The default system buffer size in frames.
     */
    public static final int DEFAULT_SYSTEM_BUFFER_SIZE = 5000;
//    public static final int DEFAULT_SYSTEM_BUFFER_SIZE = 4430;


    /**
     * The audio track.
     */
    private AudioTrack audioTrack;

    /**
     * The system buffer size in frames.
     */
    private int systemBufferSizeInFrames;

    /**
     * Thread for running realtime audio.
     */
    private Thread audioThread;

    /**
     * The priority of the audio thread.
     */
    private int threadPriority;

    /**
     * The current byte buffer.
     */
    private byte[] bbuf;

    /**
     * The channel config and encoding distilled from the audio format.
     */
    private ChannelConfigAndEncoding CCaEOutput;

    public AndroidSoundAudioIO() {
        this(DEFAULT_SYSTEM_BUFFER_SIZE);
    }

//	public AndroidSoundAudioIO(int systemBufferSize) {
//		this.systemBufferSizeInFrames = systemBufferSize;
//		setThreadPriority(Thread.MAX_PRIORITY);
//	}

    public AndroidSoundAudioIO(int bufferSizeInFrames) {
        this.systemBufferSizeInFrames = bufferSizeInFrames;
        setThreadPriority(Thread.MAX_PRIORITY);
    }

    /**
     * Starts the audio system running.
     */
    @Override
    protected boolean start() {
        audioThread = new Thread(new Runnable() {
            public void run() {
                //create JavaSound stuff only when needed
                create();
                //start the update loop
                runRealTime();
                //return from above method means context got stopped, so now clean up
                destroy();
            }
        });
        audioThread.setPriority(threadPriority);
        audioThread.start();
        return true;
    }

    public boolean create() {
        IOAudioFormat ioAudioFormat = getContext().getAudioFormat();

        CCaEOutput = calcChannelConfigAndEncodingForOutputs(ioAudioFormat);


        // The buffer size in bytes is equal to the buffer size in frames multiplied with the amount of channels and the amount of bytes per frame (sample):
        int systemBufferSizeInBytes = systemBufferSizeInFrames * CCaEOutput.channelCount * CCaEOutput.bytesPerFrame;

        // From the Android API about getMinBufferSize():
        // The total size (in bytes) of the internal buffer where audio data is read from for playback.
        // If track's creation mode is MODE_STREAM, you can write data into this buffer in chunks less than or equal to this size,
        // and it is typical to use chunks of 1/2 of the total size to permit double-buffering. If the track's creation mode is MODE_STATIC,
        // this is the maximum length sample, or audio clip, that can be played by this instance. See getMinBufferSize(int, int, int) to determine
        // the minimum required buffer size for the successful creation of an AudioTrack instance in streaming mode. Using values smaller
        // than getMinBufferSize() will result in an initialization failure.
        int minBufferSizeInBytes = AudioTrack.getMinBufferSize(CCaEOutput.sampleRate, CCaEOutput.channelConfig, CCaEOutput.encoding);
        int minBufferSizeInFrames = minBufferSizeInBytes / CCaEOutput.bytesPerFrame;
        Log.i(TAG, String.format("AudioTrack.minBufferSize = %d bytes = %d frames", minBufferSizeInBytes, minBufferSizeInFrames));
        if (minBufferSizeInBytes > systemBufferSizeInBytes) {
            throw new IllegalArgumentException(String.format("The buffer size should be at least %d (samples) according to AudioTrack.getMinBufferSize().", minBufferSizeInFrames));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(CCaEOutput.encoding)
                            .setSampleRate(CCaEOutput.sampleRate)
                            .setChannelMask(CCaEOutput.channelConfig)
                            .build())
                    .setBufferSizeInBytes(systemBufferSizeInBytes)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
        } else {
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, CCaEOutput.sampleRate, CCaEOutput.channelConfig, CCaEOutput.encoding, systemBufferSizeInBytes, AudioTrack.MODE_STREAM);
        }
        audioTrack.play();
        return true;
    }

    /**
     * Update loop called from within audio thread (created in start() method).
     */
    private void runRealTime() {
        AudioContext context = getContext();
        int bufferSizeInFrames = context.getBufferSize();
        bbuf = new byte[bufferSizeInFrames * CCaEOutput.bytesPerFrame * CCaEOutput.channelCount];

        float[] interleavedOutput = new float[CCaEOutput.channelCount * bufferSizeInFrames];

        while (context.isRunning()) {
            update(); // this propagates update call to context
            for (int i = 0, counter = 0; i < bufferSizeInFrames; ++i) {
                for (int j = 0; j < CCaEOutput.channelCount; ++j) {
                    interleavedOutput[counter++] = context.out.getValue(j, i);
                }
            }
            AudioUtils.floatToByte(bbuf, interleavedOutput, CCaEOutput.bigEndian);
            int ret;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ret = audioTrack.write(bbuf, 0, bbuf.length, AudioTrack.WRITE_BLOCKING);
            } else {
                ret = audioTrack.write(bbuf, 0, bbuf.length);
            }
            writeBuffer(bbuf);
            if (ret < 0) {
                Log.e(TAG, String.format("AudioTrack.write returned error code %d", ret));
            }
        }
    }

    private boolean destroy() {
        if (audioTrack != null) {
            audioTrack.flush();
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
        return true;
    }


    /**
     * Sets the priority of the audio thread.
     * Default priority is Thread.MAX_PRIORITY.
     *
     * @param priority
     */
    public void setThreadPriority(int priority) {
        this.threadPriority = priority;
        if (audioThread != null) audioThread.setPriority(threadPriority);
    }

    /**
     * @return The priority of the audio thread.
     */
    public int getThreadPriority() {
        return this.threadPriority;
    }

    @Override
    protected UGen getAudioInput(int[] channels) {
        //TODO not properly implemented, this does not respond to channels arg.
        AudioContext context = getContext();
        IOAudioFormat ioAudioFormat = context.getAudioFormat();
        ChannelConfigAndEncoding CCaEInput = calcChannelConfigAndEncodingForInputs(ioAudioFormat);
        return new AndroidMicInput(context, CCaEInput);
    }

    private class AndroidMicInput extends UGen {

        /**
         * The target recorder.
         */
        private AudioRecord audioInputStream;

        /**
         * The channel config and encoding distilled from the audio format.
         */
        private ChannelConfigAndEncoding CCaEInput;

        /**
         * Flag to tell whether AudioRecord has been initialised.
         */
        private boolean audioRecorderInitialized = false;

        private float[] interleavedSamples;
        private byte[] bbuf;

        /**
         * Instantiates a new AndroidMicInput.
         *
         * @param context
         *            the AudioContext.
         * @param CCaEInput
         *            the ChannelConfigAndEncoding.
         */
        public AndroidMicInput(AudioContext context, ChannelConfigAndEncoding CCaEInput) {
            super(context, CCaEInput.channelCount);
            this.CCaEInput = CCaEInput;
        }

        private void initAudioRecorder() {
            AudioContext context = getContext();
            int audioBufferSizeInFrames = context.getBufferSize();
            int audioBufferSizeInBytes = audioBufferSizeInFrames * CCaEInput.bytesPerFrame * CCaEInput.channelCount;

            int minAudioBufferSizeInBytes = AudioRecord.getMinBufferSize(CCaEInput.sampleRate,
                    CCaEInput.channelConfig, CCaEInput.encoding);
            int minAudioBufferSizeInFrames = minAudioBufferSizeInBytes / CCaEInput.bytesPerFrame;
            if (minAudioBufferSizeInBytes <= audioBufferSizeInBytes) {
                audioInputStream = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, CCaEInput.sampleRate,
                        CCaEInput.channelConfig,
                        CCaEInput.encoding,
                        audioBufferSizeInBytes);
                audioInputStream.getSampleRate();
                bbuf = new byte[bufferSize * CCaEInput.bytesPerFrame * CCaEInput.channelCount];
                audioInputStream.startRecording();
                audioRecorderInitialized = true;
            } else {
                throw new IllegalArgumentException(String.format("Buffer size too small should be at least %d", minAudioBufferSizeInFrames));
            }
        }

        @Override
        public void calculateBuffer() {
            if (!audioRecorderInitialized) {
                initAudioRecorder();
            }
            audioInputStream.read(bbuf, 0, bbuf.length);
            AudioUtils.byteToFloat(interleavedSamples, bbuf, CCaEInput.bigEndian);
            AudioUtils.deinterleave(interleavedSamples, CCaEInput.channelCount, bufferSize, bufOut);
        }
    }

    private void writeBuffer(byte[] bbuf) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Buffer: [");
        for (int i = 0; i < bbuf.length; i++) {
            byte bbbuf = bbuf[i];
            stringBuilder.append(bbbuf);
            if (i < (bbuf.length - 1)) stringBuilder.append(", ");
        }
        stringBuilder.append("]");
        System.out.println(stringBuilder.toString());
    }
}
