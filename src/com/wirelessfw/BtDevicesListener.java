package com.wirelessfw;

public interface BtDevicesListener {
	public static class BtDevice {
		public String name;
		public String macAddress;
		public BtDevice(String deviceName, String mac) {
			name = deviceName;
			macAddress = mac;
		}
	}
	public void onNewDevice(BtDevice device);
	public void onDiscoveryFinished();
}
