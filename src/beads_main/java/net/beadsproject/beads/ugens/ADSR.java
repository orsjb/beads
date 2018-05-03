package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.events.KillTrigger;

/**
 * A convenience class that wraps a Gain/Envelope combo to make an ADSR that destroys itself upon completion.
 *
 * Created by ollie on 2/07/2016.
 */
public class ADSR extends UGen {

    private Envelope env;
    private Gain gain;

    /**
     * Create an ADSR with the given {@link UGen} as input. The array of float arguments should consist of a series of pairs [x,t] where x is the destination value and t is the time to destination, finishing with a single time value to zero. For example the array 1, 0, 1000 would give you an instant attack to 1, followed by a 1s decay to zero. The array 1, 500, 1000 would give you a ramp up from 0 to 1 over 500ms followed by a 1s decay to zero. An array with an even number of elements is incorrectly formed, but as a rule, the final value in the array will always dictate the decay time to zero. This object will self-destruct once the envelope has completed. The number of channels of this {@link UGen} is dictated by the number of channels of the src {@link UGen}.
      *
     * @param context the {@link AudioContext}.
     * @param src the source UGen, which is plugged into this ADSR {@link UGen}.
     * @param adsr the adsr parameters.
     */
    public ADSR(AudioContext context, UGen src, float... adsr) {
        this(context, src.getOuts(), adsr);
        addInput(src);
    }

    /**
     * Create an ADSR with the given {@link UGen} as input. The array of float arguments should consist of a series of pairs [x,t] where x is the destination value and t is the time to destination, finishing with a single time value to zero. For example the array 1, 0, 1000 would give you an instant attack to 1, followed by a 1s decay to zero. The array 1, 500, 1000 would give you a ramp up from 0 to 1 over 500ms followed by a 1s decay to zero. An array with an even number of elements is incorrectly formed, but as a rule, the final value in the array will always dictate the decay time to zero. This object will self-destruct once the envelope has completed. The number of channels of this {@link UGen} is dictated by the number of channels of the src {@link UGen}.
     * @param src the source UGen, which is plugged into this ADSR {@link UGen}.
     * @param adsr the adsr parameters.
     */
    public ADSR(UGen src, float... adsr) {
        this(getDefaultContext(), src, adsr);
    }

    /**
     * Create an ADSR with the given number of channels. The array of float arguments should consist of a series of pairs [x,t] where x is the destination value and t is the time to destination, finishing with a single time value to zero. For example the array 1, 0, 1000 would give you an instant attack to 1, followed by a 1s decay to zero. The array 1, 500, 1000 would give you a ramp up from 0 to 1 over 500ms followed by a 1s decay to zero. An array with an even number of elements is incorrectly formed, but as a rule, the final value in the array will always dictate the decay time to zero. This object will self-destruct once the envelope has completed.
     * @param context the {@link AudioContext}.
     * @param inouts the number of channels.
     * @param adsr the adsr parameters.
     */
    public ADSR(AudioContext context, int inouts, float... adsr) {
        super(context, inouts, inouts);
        env = new Envelope(context, 0);
        gain = new Gain(context, inouts, env);
        for(int i = 0; i < adsr.length - 1; i += 2) {
            env.addSegment(adsr[i], adsr[i+1]);
        }
        env.addSegment(0, adsr[adsr.length - 1], new KillTrigger(this));
    }

    /**
     * Create an ADSR with the given number of channels. The array of float arguments should consist of a series of pairs [x,t] where x is the destination value and t is the time to destination, finishing with a single time value to zero. For example the array 1, 0, 1000 would give you an instant attack to 1, followed by a 1s decay to zero. The array 1, 500, 1000 would give you a ramp up from 0 to 1 over 500ms followed by a 1s decay to zero. An array with an even number of elements is incorrectly formed, but as a rule, the final value in the array will always dictate the decay time to zero. This object will self-destruct once the envelope has completed.
     * @param inouts the number of channels.
     * @param adsr the adsr parameters.
     */
    public ADSR(int inouts, float... adsr) {
        this(getDefaultContext(), inouts, adsr);
    }


    public void calculateBuffer() {
        env.update();
        gain.update();
        /*
         * simple transfer of audio buffers, per channel, from gain to this.
         */
        for(int i = 0; i < ins; i++) {
            bufOut[i] = gain.getOutBuffer(i);
        }
    }

    @Override
    public synchronized void addInput(int inputIndex, UGen sourceUGen, int sourceOutputIndex) {
        /*
         * Forward any incoming connections to the gain. This works for addInput(UGen) too because that one forwards to this method.
         */
        gain.addInput(inputIndex, sourceUGen, sourceOutputIndex);
    }

    @Override
    public synchronized boolean removeConnection(int inputChannel, UGen sourceUGen, int sourceOutputChannel) {
        /*
         * As with addInput().
         */
        return gain.removeConnection(inputChannel, sourceUGen, sourceOutputChannel);
    }
}
