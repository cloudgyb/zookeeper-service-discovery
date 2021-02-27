package com.github.cloudgyb;

import com.github.cloudgyb.discovery.ZookeeperWatcher;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) throws InterruptedException {
		ZookeeperWatcher zookeeperWatcher = new ZookeeperWatcher();
		Thread.sleep(Long.MAX_VALUE);
	}
}
