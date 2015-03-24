package com.wftest;

import java.util.Set;

import com.wirelessfw.BtDevices;
import com.wirelessfw.BtDevicesListener;
import com.wirelessfw.BtServiceHolder;
import com.wirelessfw.BtServiceInterface;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class DeviceListActivity extends Activity implements BtDevicesListener {
    // Debugging
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;
    // Member fields
    private BtDevices mBtDevices;
    
    private LinearLayout mPairedList;
    private LinearLayout mNewDevicesList;
    private int mNewDevicesCount;
    private String mNoNewDevices;
    private String mNoPairedDevices;
    // Member object for bluetooth services
    private BtServiceInterface mBtService = null;
 	private boolean mSearchCanceledByUser = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);
        setResult(Activity.RESULT_CANCELED);
        
        mBtService = BtServiceHolder.getInstance();
    	boolean btEnabled = mBtService.init();
    	if(!btEnabled) {
    		Toast.makeText(this, R.string.bt_not_available,
					Toast.LENGTH_LONG).show();
			finish();
			return;
    	}
    	
        mBtDevices = new BtDevices(this, this);
        		
        mNoNewDevices = getResources().getString(R.string.none_found);
        mNoPairedDevices = getResources().getString(R.string.none_paired);

        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });

        mPairedList = (LinearLayout) findViewById(R.id.paired_devices);
        mNewDevicesList = (LinearLayout) findViewById(R.id.new_devices);

        Set<BtDevice> pairedDevices = mBtDevices.getPairedDevices();
        if (pairedDevices.size() > 0) {
        	for(BtDevice device : pairedDevices) {
            	addPairedDevice(device.name + "\n" + device.macAddress);
            }
        } else {
            addPairedDevice(mNoPairedDevices);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBtDevices.release();
    }
    
    
    private void addPairedDevice(String s ) {
    	TextView v = (TextView) View.inflate(this, R.xml.device_list_item, null);
    	v.setOnClickListener(mDeviceClickListener);
    	v.setText(s);
    	mPairedList.addView(v);
    }
    private void addNewDevice(String s ) {
    	TextView v = (TextView) View.inflate(this, R.xml.device_list_item, null);
    	v.setOnClickListener(mDeviceClickListener);
    	v.setText(s);
    	mNewDevicesList.addView(v);
    }
    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
    	mSearchCanceledByUser = false;
        if (D) Log.d(TAG, "doDiscovery()");
        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);
        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        mBtDevices.doDiscovery();
    }

    private final OnClickListener mDeviceClickListener = new OnClickListener() {
        public void onClick(View v) {
        	mSearchCanceledByUser = true;
            // Cancel discovery because it's costly and we're about to connect
            mBtDevices.cancelDiscovery();
            
            String info = ((TextView) v).getText().toString();
            if( info.equals(mNoNewDevices) || info.equals(mNoPairedDevices)) {
            	return;
            }
            
            // parse MAC address
            String address = info.substring(info.length() - 17);
            // Attempt to connect to the device
            Log.e("WF", "on click -> chat service connect");
            mBtService.connect(address);      
            // Start fighting activity
            startFight();
        }
    };
    
    
    private void startFight() {
    	startActivity(new Intent(this, BtActivity.class));
    }
    
    private void toast(String text) {
		Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();	
	}

	@Override
	public void onNewDevice(BtDevice device) {
		toast("new: " + device.name + " " + device.macAddress);
		Log.e("WF", "new: " + device.name + " " + device.macAddress);
		addNewDevice(device.name + device.macAddress);
	}

	@Override
	public void onDiscoveryFinished() {
		toast("discovery finished");
		Log.e("WF", "discovery finished");
		setProgressBarIndeterminateVisibility(false);
		if(mNewDevicesList.getChildCount() == 0) {
			addNewDevice(mNoNewDevices);
		}
	}
}
