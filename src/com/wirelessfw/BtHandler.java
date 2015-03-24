package com.wirelessfw;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public abstract class BtHandler extends Handler {
	@Override
	public void handleMessage(Message msg) {
		AppMessage appMsg = AppMessage.values()[msg.what];

		switch (appMsg) {
		case MESSAGE_CONNECTION_FAIL:
			onConnectionFail();
			break;
		case MESSAGE_CONNECTION_LOST:
			onConnectionLost();
			break;
		case MESSAGE_DEVICE_NAME:
			onDeviceName((String) msg.obj);
			break;
		case MESSAGE_DEVICE_BYTES:
			onDeviceMessage((byte[]) msg.obj, msg.arg1);
			break;
		case MESSAGE_STATE_CHANGE:
			Log.e("WF", "new state: " + msg.arg1);
			int state = msg.arg1;
			switch(state) {
			case BtServiceInterface.STATE_LISTEN:
				onStateListen();
				break;
			case BtServiceInterface.STATE_CONNECTING:
				onStateConnecting();
				break;
			case BtServiceInterface.STATE_CONNECTED:
				onStateConnected();
				break;
			}
			break;
		default:
			break;

		}
	}

	public abstract void onStateListen();
	
	public abstract void onStateConnecting();
	
	public abstract void onStateConnected();

	public abstract void onDeviceName(String name);

	public abstract void onConnectionFail();

	public abstract void onConnectionLost();

	public abstract void onDeviceMessage(byte[] buffer, int length);
}
