package com.wftest;

import com.wirelessfw.BtHandler;
import com.wirelessfw.BtServiceHolder;
import com.wirelessfw.BtServiceInterface;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Bluetooth fight activity. Extends Fight Activity with 
 * sending messages via Bluetooth, waiting second player,
 * receiving messages about Bluetooth state
 */
public class BtActivity extends Activity {
	private BtServiceInterface mBtService = null;
	
	private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
   
    private BtHandler mHandler = new BtHandler() {
    	@Override
    	public void onDeviceName(String name) {
    		mSendButton.setText(name);
    	}

    	@Override
    	public void onConnectionFail() {
    		toast("conn fail");
    	}

    	@Override
    	public void onConnectionLost() {
    		toast("conn lost");
    	}

    	@Override
    	public void onDeviceMessage(byte[] buffer, int length) {
    		toast(new String(buffer, 0, length));
    	}

    	@Override
		public void onStateListen() {
    		toast("listening");
		}
    	
    	@Override
		public void onStateConnecting() {
			toast("connecting");
		}

		@Override
		public void onStateConnected() {
			toast("connected");
		}

    };
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		mBtService = BtServiceHolder.getInstance();
		mBtService.setHandler(mHandler);
		mBtService.start();
		
		// Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);
        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);
        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
        });
	}

	public void onDestroy() {
		super.onDestroy();
		if (mBtService != null) {
			mBtService.stop();
			mBtService = null;
		}
	}

	protected void sendMessage(String s) {
		
		if (mBtService.getConnectionState() != BtServiceInterface.STATE_CONNECTED) {
			return;
		}

		byte[] send = s.getBytes();
		// send to 2nd phone
		mBtService.write(send);
		// send to pc if connected
//		WifiService.send(fMessage);
	}
	
	private void toast(String text) {
		Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();	
	}
	
	// The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            Log.i("WF", "END onEditorAction");
            return true;
        }
    };
	
}