package com.github.cloudgyb.discovery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.github.cloudgyb.ZookeeperSessionExpiredListener;
import com.github.cloudgyb.config.ZookeeperServerConfigProperties;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * zookeeper watcher
 * @author cloudgyb
 * 2021/2/26 17:01
 */
public class ZookeeperWatcher implements Watcher {
	private final Logger logger = LoggerFactory.getLogger(ZookeeperWatcher.class);
	private CountDownLatch countDownLatch;
	private DiscoveryService discoveryService;
	private final List<ZookeeperSessionExpiredListener> sessionExpiredListenerList;

	public ZookeeperWatcher(CountDownLatch countDownLatch) {
		this.countDownLatch = countDownLatch;
		this.sessionExpiredListenerList = new ArrayList<>(2);
	}


	/**
	 * 当type=None（-1）时，有四种情况：
	 * 	1. zookeeper已连接
	 * 	2. 会话超时!
	 * 	3. zookeeper连接已关闭
	 * 	4. zookeeper认证失败
	 * @param watchedEvent zookeeper event
	 */
	@Override
	public void process(WatchedEvent watchedEvent) {
		Event.EventType type = watchedEvent.getType();
		Event.KeeperState state = watchedEvent.getState();
		int stateValue = state.getIntValue();
		logger.info("监听到变化,type=" + type.getIntValue() + ",name=" + type.name());
		logger.info("state=" + state.getIntValue() + ",name=" + state.name());
		if (type.getIntValue() == Event.EventType.None.getIntValue()) {
			if (stateValue == Event.KeeperState.SyncConnected.getIntValue()) {
				logger.info("zookeeper已连接！");
				countDownLatch.countDown();
			}else if (stateValue == Watcher.Event.KeeperState.Expired.getIntValue()) {
				logger.info("zookeeper会话超时！");
				//zookeeper连接是异步的，所以用CountDownLatch保证zookeeper连接成功后再通知Listeners
				countDownLatch = new CountDownLatch(1);
				ZooKeeper zooKeeper = createNewZookeeper();
				try {
					countDownLatch.await();
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				noticeListener(zooKeeper);
			}else if (stateValue == Watcher.Event.KeeperState.Closed.getIntValue()) {
				logger.info("zookeeper连接已关闭！");
			}else if (stateValue == Watcher.Event.KeeperState.AuthFailed.getIntValue()) {
				logger.info("zookeeper认证失败！");
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

	private void noticeListener(ZooKeeper zooKeeper) {
		logger.info("开始通知监听者...");
		for(ZookeeperSessionExpiredListener listener: sessionExpiredListenerList){
			listener.sessionExpired(zooKeeper);
		}
		logger.info("监听者通知完毕！");
	}

	private ZooKeeper createNewZookeeper() {
		logger.info("开始重新创建新的连接...");
		ZookeeperServerConfigProperties zkConfig = ZookeeperServerConfigProperties.config();
		try {
			ZooKeeper zooKeeper = new ZooKeeper(zkConfig.getServerAddress(), zkConfig.getSessionTimeout(),
					this);
			logger.info("新的连接已创建！");
			return zooKeeper;
		} catch (IOException e) {
			logger.error("创建新的zookeeper连接失败！");
		}
		return null;
	}

	public void setDiscoveryService(DiscoveryService discoveryService) {
		this.discoveryService = discoveryService;
	}

	public void addSessionExpireListener(List<ZookeeperSessionExpiredListener> sessionExpiredListenerList) {
		this.sessionExpiredListenerList.addAll(sessionExpiredListenerList);
	}

	public void addSessionExpireListener(ZookeeperSessionExpiredListener listener) {
		this.sessionExpiredListenerList.add(listener);
	}

}
