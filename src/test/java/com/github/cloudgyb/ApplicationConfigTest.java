package com.github.cloudgyb;

import com.github.cloudgyb.config.ApplicationProperties;
import org.junit.Test;

/**
 * @author cloudgyb
 * 2021/2/26 16:46
 */
public class ApplicationConfigTest {

	@Test
	public void testReadConfig(){
		ApplicationProperties config = ApplicationProperties.config();
		System.out.println(config);
	}
}
