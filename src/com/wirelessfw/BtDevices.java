package com.wirelessfw;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.wirelessfw.BtDevicesListener.BtDevice;

public class BtDevices {
	private BluetoothAdapter mBtAdapter;
	// The BroadcastReceiver that listens for discovered devices and
	// changes the title when discovery is finished
	private BtDevicesListener mListener;
	private Set<BtDevice> mPairedDevices;
	private Context mContext;

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// If it's already paired, skip it, because it's been listed
				// already
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					BtDevice newDevice = new BtDevice(device.getName(),
							device.getAddress());
					mListener.onNewDevice(newDevice);
				}
				// When discovery is finished, change the Activity title
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
				mListener.onDiscoveryFinished();
			}
		}
	};

	public BtDevices(BtDevicesListener listener, Context context) {
		mListener = listener;
		mContext = context;

		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBtAdapter == null) {
			return;
		}

		// Register for broadcasts when a device is discovered
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		mContext.registerReceiver(mReceiver, filter);
		// Register for broadcasts when discovery has finished
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		mContext.registerReceiver(mReceiver, filter);
		// Get a set of currently paired devices
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
		mPairedDevices = new HashSet<BtDevice>();

		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				mPairedDevices.add(new BtDevice(device.getName(), device
						.getAddress()));
			}
		}
	}

	public void doDiscovery() {
		// If we're already discovering, stop it
		if (mBtAdapter.isDiscovering()) {
			mBtAdapter.cancelDiscovery();
		}
		// Request discover from BluetoothAdapter
		mBtAdapter.startDiscovery();
	}

	public void cancelDiscovery() {
		if (mBtAdapter.isDiscovering()) {
			mBtAdapter.cancelDiscovery();
		}
	}

	public Set<BtDevice> getPairedDevices() {
		return mPairedDevices;
	}

	public void release() {
		cancelDiscovery();
		mContext.unregisterReceiver(mReceiver);
	}
}
