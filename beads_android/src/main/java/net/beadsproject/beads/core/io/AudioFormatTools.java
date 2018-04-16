package net.beadsproject.beads.core.io;

import android.media.AudioFormat;
import android.os.Build;
import android.util.Log;

import net.beadsproject.beads.core.IOAudioFormat;

public class AudioFormatTools {
    private final static String TAG = "AudioFormatTools";

    public static int getBytesPerSample(int audioFormat) {
        switch (audioFormat) {
            case AudioFormat.ENCODING_PCM_8BIT:
                return 1;
            case AudioFormat.ENCODING_PCM_16BIT:
            case AudioFormat.ENCODING_IEC61937:
            case AudioFormat.ENCODING_DEFAULT:
                return 2;
            case AudioFormat.ENCODING_PCM_FLOAT:
                return 4;
            case AudioFormat.ENCODING_INVALID:
            default:
                throw new IllegalArgumentException("Bad audio format " + audioFormat);
        }
    }

    /**
     * Check if the given ioAudioFormat is a valid encoding for the given parameters.
     *
     * @param signed        If the ioAudioFormat should use signed.
     * @param bigEndian     If the ioAudioFormat should use big endian.
     * @param ioAudioFormat The io audio format to check.
     * @return True if the encoding matches the parameters.
     */
    public static boolean isNotValidEncoding(boolean signed, boolean bigEndian, IOAudioFormat ioAudioFormat) {
        return ioAudioFormat.signed != signed || ioAudioFormat.bigEndian != bigEndian;
    }


    static final class ChannelConfigAndEncoding {

        /**
         * The channelMask or configuration.
         */
        final int channelConfig;

        /**
         * The amount of channels.
         */
        final int channelCount;

        /**
         * The encoding of the data.
         */
        final int encoding;

        /**
         * The bytes size of a frame.
         */
        final int bytesPerFrame;

        /**
         * The sample rate.
         */
        final int sampleRate;

        /**
         * Flag to represent if data is represented as bigEndian.
         */
        final boolean bigEndian;

        /**
         * Create a new data class with the given values.
         * @param channelConfig The channelMask or configuration.
         * @param channelCount The amount of channels.
         * @param encoding The encoding of the data.
         * @param bytesPerFrame The bytes size of a frame.
         */
        ChannelConfigAndEncoding(int channelConfig, int channelCount, int encoding, int bytesPerFrame, int sampleRate, boolean bigEndian) {
            this.channelConfig = channelConfig;
            this.channelCount = channelCount;
            this.encoding = encoding;
            this.bytesPerFrame = bytesPerFrame;
            this.sampleRate = sampleRate;
            this.bigEndian = bigEndian;
        }
    }

    static ChannelConfigAndEncoding calcChannelConfigAndEncodingForOutputs(IOAudioFormat ioAudioFormat) {
        return calc(ioAudioFormat, ioAudioFormat.outputs, "Output");
    }

    static ChannelConfigAndEncoding calcChannelConfigAndEncodingForInputs(IOAudioFormat ioAudioFormat) {
        return calc(ioAudioFormat, ioAudioFormat.inputs, "Input");
    }

    static private ChannelConfigAndEncoding calc(IOAudioFormat ioAudioFormat, int preferredChannelCount, String type) {
        /*
        Terminology
        -----------

        Number of channels:
            The amount of channels in the stream.

        Sample rate:
            The number of sample frames that occur each second.
            A typical value would be 44,100, which is the same as an audio CD.

        Bytes per second (byte rate):
            The number of bytes required for one second of audio data.
            This is equal to the bytes per sample frame times the sample rate.
            So with a bytes per sample frame of 32, and a sample rate of 44,100, this should equal 1,411,200.

        Bytes per sample frame (block align):
            The number of bytes required to store a single sample frame,
            i.e. a single sample for each channel.
            (Sometimes a sample frame is also referred to as a block).
            It should be equal to the number of channels times the bits per sample rounded up to a multiple of 8.

        Bits per sample:
            For PCM data, typical values will be 8, 16, or 32.

        Audio frame = sample

        src: http://wavefilegem.com/how_wave_files_work.html
         */

        int channelConfig;
        int channelCount;
        int encoding;

        Log.i(TAG, String.format("%s: Deciding on channel config", type));
        switch (preferredChannelCount) {
            case 1:
                channelConfig = AudioFormat.CHANNEL_OUT_MONO;
                channelCount = 1;
                Log.i(TAG, "create: Using audio format mono");
                break;
            case 2:
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
                channelCount = 2;
                Log.i(TAG, "create: Using audio format stereo");
                break;
            default:
                Log.e(TAG, "create: unconfigured output channel count: "
                        + ioAudioFormat.outputs);
                channelConfig = AudioFormat.CHANNEL_OUT_MONO;
                channelCount = 1;
                Log.i(TAG, "create: Using audio format mono");
                break;
        }

        Log.i(TAG, String.format("%s: Deciding on encoding", type));
        switch (ioAudioFormat.bitDepth) {
            case 8:
                /*
                The audio sample is a 8 bit unsigned integer in the range [0, 255],
                with a 128 offset for zero.
                This is typically stored as a Java byte in a byte array or ByteBuffer.
                Since the Java byte is <em>signed</em>, be careful with math operations
                and conversions as the most significant bit is inverted.

                Bits per sample: 8
                Singed: False
                bigEndian: True
                 */
                if (AudioFormatTools.isNotValidEncoding(false, true, ioAudioFormat)) {
                    throw new IllegalStateException("Couldn't create an encoding according the io audio format.");
                }
                encoding = AudioFormat.ENCODING_PCM_8BIT;
                break;
            case 16:
                /*
                The audio sample is a 16 bit signed integer typically stored as a
                Java short in a short array, but when the short is stored in a ByteBuffer,
                it is native endian (as compared to the default Java big endian).
                The short has full range from [-32768, 32767],
                and is sometimes interpreted as fixed point Q.15 data.

                Bits per sample: 16
                Singed: True
                bigEndian: True
                 */
                if (AudioFormatTools.isNotValidEncoding(true, true, ioAudioFormat)) {
                    throw new IllegalStateException("Couldn't create an encoding according the io audio format.");
                }
                encoding = AudioFormat.ENCODING_PCM_16BIT;
                break;
            case 32:
                /*
                 * This encoding specifies that
                 * the audio sample is a 32 bit IEEE single precision float. The sample can be
                 * manipulated as a Java float in a float array, though within a ByteBuffer
                 * it is stored in native endian byte order.
                 * The nominal range of <code>ENCODING_PCM_FLOAT</code> audio data is [-1.0, 1.0].
                 * It is implementation dependent whether the positive maximum of 1.0 is included
                 * in the interval. Values outside of the nominal range are clamped before
                 * sending to the endpoint device. Beware that
                 * the handling of NaN is undefined; subnormals may be treated as zero; and
                 * infinities are generally clamped just like other values for <code>AudioTrack</code>
                 * &ndash; try to avoid infinities because they can easily generate a NaN.

                Bits per sample: 32
                Singed: True
                bigEndian: True
                 */
                if (AudioFormatTools.isNotValidEncoding(true, true, ioAudioFormat)) {
                    throw new IllegalStateException("Couldn't create an encoding according the io audio format.");
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    encoding = AudioFormat.ENCODING_PCM_FLOAT;
                } else {
                    throw new IllegalStateException("Couldn't create the PCM FLOAT encoding because the android SDK version is to low");
                }
                break;
            default:
                throw new IllegalStateException("Couldn't create an encoding according the io audio format.");
        }
        int bytesPerFrame = AudioFormatTools.getBytesPerSample(encoding);
        int sampleRate = (int) ioAudioFormat.sampleRate;
        boolean bigEndian = ioAudioFormat.bigEndian;
        return new ChannelConfigAndEncoding(channelConfig, channelCount, encoding, bytesPerFrame, sampleRate, bigEndian);
    }

}
