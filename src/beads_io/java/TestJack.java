import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.AudioIO;
import net.beadsproject.beads.core.IOAudioFormat;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.WavePlayer;
import org.jaudiolibs.beads.AudioServerIO;

import java.io.File;

public class TestJack {

    //Essential: must tell JNA where Jack lives. This might vary from system to system.
    //example on current config (Mac M1), Jack installed via downloaded package:
    //Add VM arg -Djna.library.path="/usr/local/lib" when running
    //or, use the first bit of code below to do this in the running program

    public static void main(String[] args) {


        //code required to set up correct path for Jack library
        String libPath = "/usr/local/lib";
        String jnaPath = System.getProperty("jna.library.path", "").trim();
        if (jnaPath.isEmpty()) {
            System.setProperty("jna.library.path", libPath);
        } else {
            System.setProperty("jna.library.path", jnaPath + File.pathSeparator + libPath);
        }

        AudioIO io = new AudioServerIO.Jack();
        AudioContext ac = new AudioContext(io, 256, new IOAudioFormat(44100, 16, 1, 2));
        WavePlayer wp = new WavePlayer(ac,500, Buffer.SINE);
        Gain g = new Gain(ac, 1, 0.1f);
        g.addInput(wp);
        ac.out.addInput(g);
        ac.start();
    }
}
