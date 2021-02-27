package com.github.cloudgyb.registry;

import java.io.Serializable;

/**
 * 服务实例属性信息，序列化后注册到zookeeper
 * @author cloudgyb
 * 2021/2/25 17:26
 */
public class ServiceInstanceInfo implements Serializable {
	private String serviceName;
	private String ip;
	private int port;

	ServiceInstanceInfo(String serviceName, String ip, int port) {
		this.serviceName = serviceName;
		this.ip = ip;
		this.port = port;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String toString() {
		return "ServiceInstanceInfo{" +
				"serviceName='" + serviceName + '\'' +
				", ip='" + ip + '\'' +
				", port=" + port +
				'}';
	}
}
