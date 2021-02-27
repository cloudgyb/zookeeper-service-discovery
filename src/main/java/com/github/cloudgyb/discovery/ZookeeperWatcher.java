package com.github.cloudgyb.discovery;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.github.cloudgyb.config.ZookeeperServerConfigProperties;
import com.github.cloudgyb.registry.RegistryService;
import com.github.cloudgyb.registry.ServiceInstanceInfo;
import org.apache.zookeeper.AddWatchMode;
import org.apache.zookeeper.KeeperException;
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
	private ZooKeeper zooKeeper;
	private DiscoveryService discoveryService;

	public ZookeeperWatcher() {
		try {
			ZookeeperServerConfigProperties zkConfig = ZookeeperServerConfigProperties.config();
			this.zooKeeper = new ZooKeeper(zkConfig.getServerAddress(), zkConfig.getSessionTimeout(),
					this);
			this.discoveryService = new DiscoveryService(zkConfig,zooKeeper);
			registryService();
			this.discoveryService.flushRegistry();
			showRegistry();
			enableWatch(zkConfig.getNamespace());
		}catch (IOException | KeeperException | InterruptedException e){
			logger.error("初始化zookeeper客户端错误！",e);
		}
	}

	/**
	 * 启动路径监听
	 * @param namespace 服务注册的节点路径
	 */
	private void enableWatch(String namespace) {
		try {
			zooKeeper.addWatch(namespace, AddWatchMode.PERSISTENT_RECURSIVE);
		}catch (Exception e){
			logger.error("监听"+namespace+"错误！",e);
		}
	}

	/**
	 * 将自己注册到zookeeper
	 */
	private void registryService() {
		logger.info("开始注册服务....");
		RegistryService registryService = new RegistryService(this.zooKeeper);
		registryService.registry();
		logger.info("注册服务成功！");
	}

	@Override
	public void process(WatchedEvent watchedEvent) {
		Event.EventType type = watchedEvent.getType();
		System.out.println("--------------------");
		System.out.println(type);
		System.out.println("监听到变化。");
		try {
			discoveryService.discovery(watchedEvent);
			showRegistry();
		}
		catch (Exception e) {
			logger.error("更新注册信息失败！", e);
		}
		System.out.println("------------------------");
	}

	private void showRegistry() {
		ConcurrentHashMap<String, Map<String,ServiceInstanceInfo>> registry =
				discoveryService.getRegistry();
		Set<Map.Entry<String, Map<String,ServiceInstanceInfo>>> entries = registry.entrySet();
		for (Map.Entry<String, Map<String,ServiceInstanceInfo>> entry : entries) {
			System.out.println(entry.getKey());
			Map<String,ServiceInstanceInfo> entryValue = entry.getValue();
			if(entryValue != null){
				Set<Map.Entry<String, ServiceInstanceInfo>> entries1 = entryValue.entrySet();
				for (Map.Entry<String, ServiceInstanceInfo> e : entries1) {
					System.out.println(e.getKey()+":"+e.getValue());
				}
			}
		}
	}
}
