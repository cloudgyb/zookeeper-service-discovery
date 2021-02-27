package com.github.cloudgyb.discovery;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;

/**
 * 服务发现接口
 * @author cloudgyb
 * 2021/2/25 17:48
 */
public interface ServiceDiscover {

	void discovery(WatchedEvent e) throws KeeperException, InterruptedException;
}
