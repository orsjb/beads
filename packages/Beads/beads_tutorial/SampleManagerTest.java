import net.beadsproject.beads.data.SampleManager;
import org.junit.Test;

import java.io.IOException;

public class SampleManagerTest {

	public static String audioFilePath = "./resources/audio/1234.aif";
	@Test
	public void checkFile()
			throws IOException {
		SampleManager.checkFile(audioFilePath);
	}
}
