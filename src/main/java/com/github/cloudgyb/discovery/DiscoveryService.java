package com.github.cloudgyb.discovery;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.github.cloudgyb.ZookeeperSessionExpiredListener;
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
public class DiscoveryService implements ServiceDiscover, ZookeeperSessionExpiredListener {
	private final Logger logger = LoggerFactory.getLogger(DiscoveryService.class);
	private final ZookeeperServerConfigProperties properties;
	private ZooKeeper zooKeeper;
	private ConcurrentHashMap<String, Map<String, ServiceInstanceInfo>> registry;

	public DiscoveryService(ZookeeperServerConfigProperties properties, ZooKeeper zooKeeper) {
		this.properties = properties;
		this.zooKeeper = zooKeeper;
		this.registry = new ConcurrentHashMap<>(4);
	}

	/**
	 * 该方法只处理zookeeper中监听的namespace对应children的增加和删除事件
	 * @param e Zookeeper事件
	 */
	public void discovery(WatchedEvent e) throws KeeperException, InterruptedException {
		int type = e.getType().getIntValue();
		if (type == -1) //事件type=-1是zookeeper客户端连接到server（或者重新连接）这儿不做服务发现
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

	/**
	 * 服务启动成功后调用该方法，进行首次服务发现
	 * 使用CopyOnWrite修改注册表
	 */
	public void flushRegistry() throws KeeperException, InterruptedException {
		ConcurrentHashMap<String, Map<String, ServiceInstanceInfo>> newRegistry =  new ConcurrentHashMap<>(4);;
		List<String> serviceNameList = zooKeeper.getChildren(properties.getNamespace(), true);
		if (serviceNameList == null)
			return;
		for (String serviceName : serviceNameList) {
			Map<String, ServiceInstanceInfo> serviceList = newRegistry.get(serviceName);
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
			newRegistry.put(serviceName, serviceList);
		}
		this.registry = newRegistry;
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

	/**
	 * 给更具服务名查找所有的服务实例信息
	 * @param serviceName 服务名
	 * @return Set集合
	 */
	public Collection<ServiceInstanceInfo> findService(String serviceName){
		if(serviceName == null)
			throw new NullPointerException();
		Map<String, ServiceInstanceInfo> map = registry.get(serviceName);
		if(map == null)
			return Collections.emptySet();
		HashSet<ServiceInstanceInfo> set = new HashSet<>(map.size());
		set.addAll(map.values());
		return set;
	}

	public void showRegistry() {
		ConcurrentHashMap<String, Map<String,ServiceInstanceInfo>> registry = getRegistry();
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

	@Override
	public void sessionExpired(ZooKeeper zooKeeper) {
		this.zooKeeper = zooKeeper;
		try {
			this.flushRegistry();
		}
		catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
