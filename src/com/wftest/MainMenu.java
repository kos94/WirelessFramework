package com.wftest;

import com.wirelessfw.BtServiceHolder;
import com.wirelessfw.BtServiceInterface;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

/*
 * Main menu. Checks Bluetooth availability and 
 * checks default screen orientation (for sensor data purposes)
 */
public class MainMenu extends Activity {
	// Debugging
	private static final String TAG = "Wizard Fight";
	private static final boolean D = true;
	private boolean mIsUserCameWithBt;
	private BtServiceInterface mBtService;
	// Intent request codes
	enum BtRequest {
		BT_CREATE_GAME, BT_JOIN_GAME
	}

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/* Full screen */
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.main_menu);
		
		// send context to WifiService to read player name
		//WifiService.setContext(getBaseContext()); //TODO uncomment maybe?
		mBtService = BtServiceHolder.getInstance(); 
		
		if (!mBtService.isBtExist()) {
			// If the adapter is null, then Bluetooth is not supported
			Toast.makeText(this, R.string.bt_not_available, Toast.LENGTH_LONG)
					.show();
		} else {
			// remember user's BT initial state
			mIsUserCameWithBt = mBtService.isBtEnabled();

			// if no player name - set as BT name
			String bluetoothName = mBtService.getBtName();
//			if(bluetoothName != null) {
//				SharedPreferences appPrefs = PreferenceManager
//						.getDefaultSharedPreferences(getBaseContext());
//				String pName = appPrefs.getString("player_name", "");
//				if (pName.equals("")) {
//					SharedPreferences.Editor editor = appPrefs.edit();
//					editor.putString("player_name", bluetoothName);
//					editor.commit();
//				}
//			}
		}

		// volume buttons control multimedia volume
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	public void goToCreateGame(View view) {
		if (!mBtService.isBtEnabled()) {
			requestBluetooth(BtRequest.BT_CREATE_GAME);
		} else {
			//TODO uncomment
			BtServiceInterface btService = BtServiceHolder.getInstance(); 
			btService.init();
			btService.setAsServer();
			startActivity(new Intent(this, BtActivity.class));
		}
	}

	public void goToJoinGame(View view) {
		if (!mBtService.isBtEnabled()) {
			requestBluetooth(BtRequest.BT_JOIN_GAME);
		} else {
			startActivity(new Intent(this, DeviceListActivity.class));
		}
	}

	public void goToDesktopConnection(View view) {
//		startActivity(new Intent(this, DesktopConnection.class));
	}

	public void goToSettings(View view) {
//		startActivity(new Intent(this, WPreferences.class));
		Log.e("Wizard Fight", "go to settings");
	}

	public void exit(View view) {
//		BluetoothService.getInstance().release();
		// return BT state to last one in
		if (mBtService.isBtExist() && !mIsUserCameWithBt
				&& mBtService.isBtEnabled()) {
			mBtService.setBtEnabled(false);
		}
		finish();
	}

	void requestBluetooth(BtRequest r) {
		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableIntent, r.ordinal());
	}

	@Override
	public void onBackPressed() {
		exit(null);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult " + resultCode);
		// When the request to enable Bluetooth returns
		if (resultCode != Activity.RESULT_OK) {
			// User did not enable Bluetooth or an error occured
			if (D)
				Log.d(TAG, "BT not enabled");
			Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		BtRequest request = BtRequest.values()[requestCode];
		switch (request) {
		case BT_CREATE_GAME:
			BtServiceInterface btService = BtServiceHolder.getInstance();
			btService.init();
			btService.setAsServer();
			startActivity(new Intent(this, BtActivity.class));
			break;
		case BT_JOIN_GAME:
			startActivity(new Intent(this, DeviceListActivity.class));
			break;
		}
	}
}
