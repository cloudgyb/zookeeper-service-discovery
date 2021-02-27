package com.github.cloudgyb.registry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import com.github.cloudgyb.config.ApplicationProperties;
import com.github.cloudgyb.config.ZookeeperServerConfigProperties;
import com.github.cloudgyb.util.IPUtil;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/**
 * 注册服务类，向zookeeper注册该应用的信息
 * @author cloudgyb
 * 2021/2/25 17:24
 */
public class RegistryService implements ServiceRegister {
	private final Logger logger = Logger.getLogger(RegistryService.class);
	private final ZooKeeper zooKeeper;
	private final ZookeeperServerConfigProperties configProperties;

	public RegistryService(ZooKeeper zooKeeper) {
		this.zooKeeper = zooKeeper;
		this.configProperties = ZookeeperServerConfigProperties.config();
	}

	/**
	 * 会在zookeeper中创建以下znode结构
	 * /namespace/serviceName/serviceName0000000000
	 *                       /serviceName0000000001
	 *                       /serviceName0000000002
	 * @return 注册成功返回true
	 */
	@Override
	public boolean registry() {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			ServiceInstanceInfo info = generateServiceInfo();
			oos.writeObject(info);
			ensureZNodeExist(configProperties.getNamespace(), info.getServiceName());
			this.zooKeeper.create(configProperties.getNamespace() + "/" +
							info.getServiceName() + "/" + info.getServiceName(),
					baos.toByteArray(),
					ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.EPHEMERAL_SEQUENTIAL);
		}
		catch (IOException | InterruptedException | KeeperException e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		return true;
	}

	private void ensureZNodeExist(String namespace, String serviceName) throws KeeperException, InterruptedException {
		Stat exists = zooKeeper.exists(namespace, false);
		if (exists == null) {
			logger.info("zookeeper namespace:" + namespace + "不存在，开始创建...");
			String s = zooKeeper.create(namespace, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			logger.info("zookeeper namespace:" + s + "已创建！");
		}
		Stat isExist = zooKeeper.exists(namespace + "/" + serviceName, false);
		if (isExist == null) {
			logger.info("zookeeper service Znode:" + serviceName + "不存在，开始创建...");
			String s = zooKeeper.create(namespace + "/" + serviceName, null,
					ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.PERSISTENT);
			logger.info("zookeeper service Znode:" + s + "已创建！");
		}
	}

	private ServiceInstanceInfo generateServiceInfo() {
		ApplicationProperties config = ApplicationProperties.config();
		return new ServiceInstanceInfo(config.getApplicationName(), IPUtil
				.getIpAddress(),
				config.getServerPort());
	}
}
