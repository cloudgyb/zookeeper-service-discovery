package com.github.cloudgyb.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * zk Server 配置
 * @author cloudgyb
 * 2021/2/25 14:30
 */
public class ZookeeperServerConfigProperties {
	private static final Logger logger = LoggerFactory.getLogger(ZookeeperServerConfigProperties.class);
	private static final String confFile = "zookeeper.properties";
	private static final String serverAddrKey = "zk.server.addr";
	private static final String sessionTimeoutKey = "zk.server.session.timeout";
	private static final String namespaceKey = "zk.server.namespace";
	private static final int defaultSessionTimeout = 2000;
	private String serverAddress;
	private Integer sessionTimeout;
	private String namespace;

	private static final ZookeeperServerConfigProperties configPropertiesHolder = new ZookeeperServerConfigProperties();

	static {
		logger.info("开始从"+confFile+"加载zookeeper配置.");
		try (InputStream ins = ZookeeperServerConfigProperties.class.getClassLoader()
				.getResourceAsStream(confFile)) {
			if (ins == null)
				throw new ZookeeperConfigException("配置错误，在classpath下未发现'" + confFile + "文件！");
			Properties properties = new Properties();
			properties.load(ins);
			String addr = properties.getProperty(serverAddrKey);
			if (addr == null || "".equals(addr))
				throw new ZookeeperConfigException("配置错误，'" + serverAddrKey + "'未配置！");
			else
				configPropertiesHolder.serverAddress = addr;
			String timeout = properties.getProperty(sessionTimeoutKey);
			if (timeout == null || "".equals(timeout)) {
				configPropertiesHolder.sessionTimeout = defaultSessionTimeout;
			}
			else {
				try {
					configPropertiesHolder.sessionTimeout = Integer.valueOf(timeout);
				}
				catch (NumberFormatException e) {
					throw new ZookeeperConfigException("配置错误，'" + sessionTimeoutKey + "'值不合法！");
				}
			}
			String ns = properties.getProperty(namespaceKey);
			if (ns == null || "".equals(ns)) {
				throw new ZookeeperConfigException("配置错误，'" + namespaceKey + "'未配置！");
			}
			else {
				configPropertiesHolder.namespace = ns;
			}
			logger.info("从" + confFile + "读取zookeeper配置完成.");
		}
		catch (IOException e) {
			logger.error("配置错误," + e.getMessage());
			System.exit(1);
		}
		catch (ZookeeperConfigException e) {
			logger.error(e.getMessage());
			System.exit(1);
		}
	}

	private ZookeeperServerConfigProperties() {
	}

	public static ZookeeperServerConfigProperties config() {
		return configPropertiesHolder;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public Integer getSessionTimeout() {
		return sessionTimeout;
	}

	public String getNamespace() {
		return namespace;
	}
}
