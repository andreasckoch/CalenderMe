package start;
import org.junit.Test;


public class StartupTest {
	
	
	@Test
	public void testNoStartupInput() {
		
		assert(Startup.startupInput("login") == 1);
		
	}
	
}
