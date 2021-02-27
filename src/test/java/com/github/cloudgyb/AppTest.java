package com.github.cloudgyb;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertTrue;

import com.github.cloudgyb.config.ZookeeperServerConfigProperties;
import org.apache.zookeeper.AddWatchMode;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Version;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {
	private ZookeeperServerConfigProperties configProperties;
	private ZooKeeper zooKeeper;

	@Before
	public void init() throws Exception{
		configProperties = ZookeeperServerConfigProperties.config();
		zooKeeper = new ZooKeeper(configProperties.getServerAddress(),
				configProperties.getSessionTimeout(), event -> {
			String path = event.getPath();
			System.out.println(path);
			System.out.println("------------------");
			System.out.println(event.getType().getIntValue());
			System.out.println("------------------");
		});
	}

	@Test
	public void shouldAnswerWithTrue() {
		assertTrue(true);
	}

	@Test
	public void testCreate() throws Exception{
		zooKeeper.addWatch("/service", AddWatchMode.PERSISTENT);
		String s = zooKeeper.create("/service/user-service", null,
				ZooDefs.Ids.OPEN_ACL_UNSAFE,
				CreateMode.PERSISTENT_SEQUENTIAL);
		System.out.println(s);
		Thread.sleep(5000);
	}

	@Test
	public void testDelete() throws Exception{
		zooKeeper.delete("/test1", Version.REVISION);
		zooKeeper.close();
	}

	@Test
	public void testGetPath() throws Exception{
		List<String> children = zooKeeper.getChildren("/service", true);
		for (String child : children) {
			System.out.println(child);
		}
	}

	@Test
	public void testSetData() throws Exception{
		zooKeeper.addWatch("/service",AddWatchMode.PERSISTENT_RECURSIVE);
		Stat stat = zooKeeper.setData("/service/user-service0000000000", "11111".getBytes(StandardCharsets.UTF_8),
				Version.REVISION);
		System.out.println(stat.getAversion());
	}
}
