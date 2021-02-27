package com.github.cloudgyb;

import com.github.cloudgyb.config.ZookeeperServerConfigProperties;
import org.junit.Test;

/**
 * @author cloudgyb
 * 2021/2/26 16:46
 */
public class ZookeeperConfigTest {

	@Test
	public void testReadConfig(){
		ZookeeperServerConfigProperties config = ZookeeperServerConfigProperties.config();
		System.out.println(config);
	}
}
