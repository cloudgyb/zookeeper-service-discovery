package com.github.cloudgyb.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * @author cloudgyb
 * 2021/2/26 17:19
 */
public final class IPUtil {

	public static String getIpAddress() {
		try {
			Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
			InetAddress ip;
			while (allNetInterfaces.hasMoreElements()) {
				NetworkInterface netInterface = allNetInterfaces.nextElement();
				String displayName = netInterface.getDisplayName();
				if (!displayName.startsWith("VMware") && !netInterface.isLoopback() &&
						!netInterface.isVirtual() && netInterface.isUp()) {
					Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
					while (addresses.hasMoreElements()) {
						ip = addresses.nextElement();
						if (ip instanceof Inet4Address) {
							return ip.getHostAddress();
						}
					}
				}
			}
		}
		catch (Exception e) {
			System.err.println("IP地址获取失败" + e.toString());
		}
		return "";
	}

	public static void main(String[] args) {
		String ipAddress = getIpAddress();
		System.out.println(ipAddress);
	}
}
