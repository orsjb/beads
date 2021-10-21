import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.AudioIO;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.WavePlayer;
import org.jaudiolibs.beads.AudioServerIO;

public class TestJack {

    public static void main(String[] args) {
        AudioIO io = new AudioServerIO.Jack();
        AudioContext ac = new AudioContext(io);
        WavePlayer wp = new WavePlayer(ac,500, Buffer.SINE);
        Gain g = new Gain(ac, 1, 0.1f);
        g.addInput(wp);
        ac.out.addInput(g);
        ac.start();
    }
}
