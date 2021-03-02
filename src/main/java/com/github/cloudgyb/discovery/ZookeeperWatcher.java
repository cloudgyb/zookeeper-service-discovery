package com.github.cloudgyb.discovery;

import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * zookeeper watcher
 * @author cloudgyb
 * 2021/2/26 17:01
 */
public class ZookeeperWatcher implements Watcher {
	private final Logger logger = LoggerFactory.getLogger(ZookeeperWatcher.class);
	private final CountDownLatch countDownLatch;
	private DiscoveryService discoveryService;

	public ZookeeperWatcher(CountDownLatch countDownLatch) {
		this.countDownLatch = countDownLatch;
	}


	@Override
	public void process(WatchedEvent watchedEvent) {
		Event.EventType type = watchedEvent.getType();
		int state = watchedEvent.getState().getIntValue();
		logger.info("监听到变化,type="+type.getIntValue()+",name="+type.name());
		if(state == Event.KeeperState.SyncConnected.getIntValue()){
			if(type.getIntValue() == Event.EventType.None.getIntValue()) {
				logger.info("zookeeper已连接！");
				countDownLatch.countDown();
			}
		}
		try {
			discoveryService.discovery(watchedEvent);
			discoveryService.showRegistry();
		}
		catch (Exception e) {
			logger.error("更新注册信息失败！", e);
		}
	}

	public void setDiscoveryService(DiscoveryService discoveryService) {
		this.discoveryService = discoveryService;
	}
}
