package com.wirelessfw;

import android.bluetooth.BluetoothDevice;

public interface BtServiceInterface {
	public static class BtDevice {
		public String name;
		public String macAddress;
	}
	
	public static final int STATE_NONE = 0; // we're doing nothing
	public static final int STATE_LISTEN = 1; // now listening for incoming
												// connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing
													// connection
	public static final int STATE_CONNECTED = 3; // now connected to a remote
													// device

	public void init();

	public void setAsServer();

	public boolean isServer();

	public void start();

	public void write(byte[] out);

	public void setHandler(BtHandler handler);

	public void stop();

	public int getState();

	public void connect(String macAddress);
}