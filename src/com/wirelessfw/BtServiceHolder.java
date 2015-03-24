package com.wirelessfw;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for incoming
 * connections, a thread for connecting with a device, and a thread for
 * performing data transmissions when connected.
 */

enum AppMessage { 
	MESSAGE_STATE_CHANGE, 
	MESSAGE_DEVICE_NAME, 
	MESSAGE_CONNECTION_FAIL, 
	MESSAGE_CONNECTION_LOST,
	MESSAGE_DEVICE_BYTES;
}

public class BtServiceHolder {
	public static BtServiceInterface getInstance() { 
		return BluetoothService.getInstance();
	}
}

class BluetoothService implements BtServiceInterface {
    // Debugging
    private static final String TAG = "BluetoothService";
    private static final boolean D = true;
    // Name for the SDP record when creating server socket
    private static final String NAME = "WizardFight";
    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString("fa6dc0d0-af9c-1fff-8aaa-08ccccafcaf6");
    // Member fields
    private BtHandler mHandler;
    private BluetoothAdapter mAdapter;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    // Describes player role in connection setup
    private static boolean mIsServer = false;
    private static int MAX_BUFFER_SIZE = 1024; //TODO set message size ili ne nado? 
    private static class InstanceHolder {
        private static final BluetoothService instance = new BluetoothService();
    }

    public static BtServiceInterface getInstance() {
    	return InstanceHolder.instance;
    }
    
    public boolean init() {
    	mAdapter = BluetoothAdapter.getDefaultAdapter();
    	if(mAdapter == null) return false;
        mState = STATE_NONE;
        mIsServer = false;
        setHandler(null);
        return true;
    }
    
    @Override
	public void setHandler(BtHandler handler) {
		if(mHandler != null) mHandler.removeCallbacksAndMessages(null);
    	mHandler = handler;
    	if(mHandler != null) {
        	sendMsgToHandler(AppMessage.MESSAGE_STATE_CHANGE.ordinal(), mState, -1);
        }
	}
    
    public void setAsServer() { mIsServer = true; }
    /*
     * returns if phone was server during Bluetooth connection
     */
    public boolean isServer() { return mIsServer; }
    /**
     * Set the current state of the connection
     * @param state  An integer defining the current BT connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        if(mHandler != null) {
        	sendMsgToHandler(AppMessage.MESSAGE_STATE_CHANGE.ordinal(), state, -1);
        }
    }
    private void sendMsgToHandler(int what, int arg1, int arg2, Object o) {
    	if(mHandler != null)
    		mHandler.obtainMessage(what, arg1, arg2, o).sendToTarget();
    }
    
    private void sendMsgToHandler(int what, int arg1, int arg2) {
    	if(mHandler != null)
    		mHandler.obtainMessage(what, arg1, arg2).sendToTarget();
    }
    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }
    /**
     * Start the BT service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
    	if (!mIsServer) return;
    	
        if (D) Log.d(TAG, "start");
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        // Start the thread to listen on a BluetoothServerSocket
        Log.e("WF", "i am server");
        if (mAcceptThread == null) {
        	mAcceptThread = new AcceptThread();
        	mAcceptThread.start();
        }
        setState(STATE_LISTEN);
    }
    
    public synchronized void connect(String macAddress) {
    	BluetoothDevice device = mAdapter.getRemoteDevice(macAddress);
    	connect(device);
    }
    
    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
    	Log.e("WF", "connect to: " + device);
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        Log.e("WF", "connect thread start called");
        setState(STATE_CONNECTING);
    }
    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        sendMsgToHandler(AppMessage.MESSAGE_DEVICE_NAME.ordinal(), 0, 0, device.getName());
        
        setState(STATE_CONNECTED);
    }
    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        setState(STATE_NONE);
    }
    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }
    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
    	setState(mIsServer? STATE_LISTEN : STATE_NONE);
        
        sendMsgToHandler(AppMessage.MESSAGE_CONNECTION_FAIL.ordinal(), 0, 0);
    }
    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
    	setState(mIsServer? STATE_LISTEN : STATE_NONE);
    	
        sendMsgToHandler(AppMessage.MESSAGE_CONNECTION_LOST.ordinal(), 0, 0);
    }
    
    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }
        public void run() {
            if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket;
            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                        	mIsServer = true;
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.getRemoteDevice());
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }
            if (D) Log.i(TAG, "END mAcceptThread");
        }
        public void cancel() {
            if (D) Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }
    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }
        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");
            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();
            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                BluetoothService.this.start();
                return;
            }
            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }
            // Start the connected thread
            connected(mmSocket, mmDevice);
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            
            int bytes;
            // Keep listening to the InputStream while connected
			while (true) {
				try {
					// Read from the InputStream
					byte[] buffer = new byte[MAX_BUFFER_SIZE];
					bytes = mmInStream.read(buffer);
					// Send the obtained object to the UI Activity
					sendMsgToHandler(AppMessage.MESSAGE_DEVICE_BYTES.ordinal(),
							bytes, -1, buffer);
				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					connectionLost();
					break;
				}
			}
        }
        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to wr	ite
         */
        public void write(byte[] buffer) {
            try {
            	mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
        
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
    
    public void release() {
		// cancel connection threads
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}
	}

}