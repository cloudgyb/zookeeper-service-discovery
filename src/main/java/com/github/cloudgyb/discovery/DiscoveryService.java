package com.github.cloudgyb.discovery;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.cloudgyb.config.ZookeeperServerConfigProperties;
import com.github.cloudgyb.registry.ServiceInstanceInfo;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务发现实现类
 * @author cloudgyb
 * 2021/2/25 17:52
 */
public class DiscoveryService implements ServiceDiscover {
	private final Logger logger = LoggerFactory.getLogger(DiscoveryService.class);
	private final ZookeeperServerConfigProperties properties;
	private final ZooKeeper zooKeeper;
	private final ConcurrentHashMap<String, Map<String, ServiceInstanceInfo>> registry;

	public DiscoveryService(ZookeeperServerConfigProperties properties, ZooKeeper zooKeeper) {
		this.properties = properties;
		this.zooKeeper = zooKeeper;
		this.registry = new ConcurrentHashMap<>(4);
	}

	public void discovery(WatchedEvent e) throws KeeperException, InterruptedException {
		int type = e.getType().getIntValue();
		if (type == -1)
			return;
		logger.info("注册信息发生变化，type=" + type + ",path=" + e.getPath());
		if (type == 1) {//节点创建（有新的服务注册）
			logger.info("有新的服务注册，添加服务信息到注册表...");
			String path = e.getPath();
			ServiceInstanceInfo serviceInstanceInfo = addServiceInfo(path);
			logger.info("已添加服务：" + serviceInstanceInfo);
		}
		else if (type == 2) {//节点删除（有服务断开）
			logger.info("有服务离线，从注册表删除服务信息...");
			String path = e.getPath();
			ServiceInstanceInfo serviceInstanceInfo = deleteServiceInfo(path);
			logger.info("已移除服务：" + serviceInstanceInfo);
		}

	}

	private ServiceInstanceInfo deleteServiceInfo(String path) {
		String[] split = path.split("/");
		if (split.length < 4)
			return null;
		String serviceName = split[2];
		String serviceInstanceName = split[3];
		Map<String, ServiceInstanceInfo> map = registry.get(serviceName);
		ServiceInstanceInfo serviceInstanceInfo = map.get(serviceInstanceName);
		map.remove(serviceInstanceName);
		return serviceInstanceInfo;
	}

	private ServiceInstanceInfo addServiceInfo(String path) throws KeeperException, InterruptedException {
		//例如：path=/service/serviceName/serviceName0000000000
		String[] split = path.split("/");
		if (split.length < 4)
			return null;
		//分割之后，serviceName就是第2个元素
		String serviceName = split[2];
		String serviceInstanceName = split[3];
		Map<String, ServiceInstanceInfo> map = registry.get(serviceName);
		if(map == null){
			map = new ConcurrentHashMap<>();
			registry.put(serviceName,map);
		}
		byte[] data = zooKeeper.getData(path, false, new Stat());
		ServiceInstanceInfo serviceInstanceInfo = toObject(data);
		map.put(serviceInstanceName, serviceInstanceInfo);
		return serviceInstanceInfo;
	}

	public void flushRegistry() throws KeeperException, InterruptedException {
		List<String> serviceNameList = zooKeeper.getChildren(properties.getNamespace(), true);
		if (serviceNameList == null)
			return;
		for (String serviceName : serviceNameList) {
			Map<String, ServiceInstanceInfo> serviceList = registry.get(serviceName);
			if (serviceList == null)
				serviceList = new ConcurrentHashMap<>();
			List<String> serviceInstanceList = zooKeeper
					.getChildren(properties.getNamespace() + "/" + serviceName,
							false);
			if (serviceInstanceList != null) {
				for (String serviceInstanceName : serviceInstanceList) {
					byte[] data = zooKeeper.getData(
							properties.getNamespace() + "/" + serviceName + "/" +
									serviceInstanceName,
							false, new Stat());
					ServiceInstanceInfo serviceInstanceInfo = toObject(data);
					serviceList.put(serviceInstanceName, serviceInstanceInfo);
				}
			}
			registry.put(serviceName, serviceList);
		}
	}

	private ServiceInstanceInfo toObject(byte[] data) {
		try (
				ByteArrayInputStream bis = new ByteArrayInputStream(data);
				ObjectInputStream ois = new ObjectInputStream(bis)
		) {
			return (ServiceInstanceInfo) ois.readObject();
		}
		catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public ConcurrentHashMap<String, Map<String, ServiceInstanceInfo>> getRegistry() {
		return registry;
	}
}
