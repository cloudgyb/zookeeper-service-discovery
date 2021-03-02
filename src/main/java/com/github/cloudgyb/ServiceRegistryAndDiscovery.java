package com.github.cloudgyb;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import com.github.cloudgyb.config.ZookeeperServerConfigProperties;
import com.github.cloudgyb.discovery.DiscoveryService;
import com.github.cloudgyb.discovery.ZookeeperWatcher;
import com.github.cloudgyb.registry.RegistryService;
import org.apache.zookeeper.AddWatchMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 入口类，负责启动注册和发现服务
 * @author cloudgyb
 * 2021/3/1 18:19
 */
public class ServiceRegistryAndDiscovery implements ZookeeperSessionExpiredListener {
	private final Logger logger = LoggerFactory.getLogger(ServiceRegistryAndDiscovery.class);
	private RegistryService registryService;
	private DiscoveryService discoveryService;
	private ZooKeeper zooKeeper;
	/**
	 * 用于等待zookeeper链接成功
	 */
	private final CountDownLatch cdl = new CountDownLatch(1);

	public ServiceRegistryAndDiscovery(){
		try {
			//读取配置，实例化zookeeper,Watcher对象
			init();
			//等待zookeeper连接成功
			cdl.await();
			//注册本服务到zookeeper
			registryService();
			//服务发现，从zookeeper获取服务信息
			initDiscoveryService();
			//启用命名空间znode监听
			enableNamespaceWatch();
		}catch (InterruptedException | KeeperException | IOException e){
			logger.error("初始化服务注册与发现错误！",e);
			try {
				zooKeeper.close();
			}
			catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
	}

	private void init() throws IOException {
		ZookeeperWatcher defaultWatcher = new ZookeeperWatcher(this.cdl);
		ZookeeperServerConfigProperties zkConfig = ZookeeperServerConfigProperties.config();
		this.zooKeeper = new ZooKeeper(zkConfig.getServerAddress(), zkConfig.getSessionTimeout(),
				defaultWatcher);
		this.registryService = new RegistryService(this.zooKeeper);
		this.discoveryService= new DiscoveryService(zkConfig, zooKeeper);
		defaultWatcher.setDiscoveryService(this.discoveryService);
		//注意：session过期监听器的注册顺序不能改变，先注册服务，再发现服务，后this
		defaultWatcher.addSessionExpireListener(this.registryService);
		defaultWatcher.addSessionExpireListener(this.discoveryService);
		defaultWatcher.addSessionExpireListener(this);
	}
	/**
	 * 将自己注册到zookeeper
	 */
	private void registryService() {
		logger.info("开始注册服务....");
		this.registryService.registry();
		logger.info("注册服务成功！");
	}

	private void initDiscoveryService() throws KeeperException, InterruptedException {
		logger.info("开始从zookeeper拉取注册的服务信息.");
		discoveryService.flushRegistry();
		logger.info("拉取注册的服务信息完成.");
		discoveryService.showRegistry();
	}

	/**
	 * 启动路径监听
	 */
	private void enableNamespaceWatch() throws KeeperException, InterruptedException {
		ZookeeperServerConfigProperties zkConfig = ZookeeperServerConfigProperties.config();
		String namespace = zkConfig.getNamespace();
		logger.info("启用"+namespace+"监听");
		try {
			zooKeeper.addWatch(namespace, AddWatchMode.PERSISTENT_RECURSIVE);
		}catch (Exception e){
			logger.error("监听"+namespace+"错误！",e);
			throw e;
		}
		logger.info("启用"+namespace+"监听成功！");
	}

	@Override
	public void sessionExpired(ZooKeeper zooKeeper) {
		this.zooKeeper = zooKeeper;
		try {
			//重新启用namespace的监听
			enableNamespaceWatch();
		}
		catch (KeeperException | InterruptedException ignored) {
		}
	}
}
