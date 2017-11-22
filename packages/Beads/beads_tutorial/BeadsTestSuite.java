import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses(
		{
				SampleManagerTest.class,
				Lesson01_AudioContext_Test.class,
				Lesson02_EnvelopeAndWavePlayer_Test.class,
				Lesson03_FMSynthesis_Test.class,
				Lesson04_SamplePlayer_Test.class,
				Lesson05_Clock_Test.class,
				Lesson06_Trigger_Test.class,
				Lesson07_Music_Test.class,
				Lesson08_Granulation_Test.class,
				Lesson09_RecordToSample_Test.class,
		}
)
public class BeadsTestSuite {
}
