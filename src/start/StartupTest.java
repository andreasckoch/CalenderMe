package start;
import static org.junit.Assert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.Test;


public class StartupTest {
	
	
	@Test
	public void testNoStartupInput() {
		
		assert(Startup.startupInput("login") == 1);
		
	}
	
}
