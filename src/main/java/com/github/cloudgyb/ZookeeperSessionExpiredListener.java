package com.github.cloudgyb;

import org.apache.zookeeper.ZooKeeper;

/**
 * zookeeper session过期监听器（观察者模式）。
 * 依赖zookeeper对象的类都需要实现该接口。
 * 当session过期后，watcher负责创建新的连接，并通知各监听者
 * @author cloudgyb
 * 2021/3/2 16:37
 */
public interface ZookeeperSessionExpiredListener {
	/**
	 * session过期通知，将新的zookeeper连接发送给监听者
	 * @param zooKeeper 新的zookeeper客户端
	 */
	void sessionExpired(ZooKeeper zooKeeper);
}
