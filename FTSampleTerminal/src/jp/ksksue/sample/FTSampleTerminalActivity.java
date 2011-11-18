package jp.ksksue.sample;
/*
 * Copyright (C) 2011 @ksksue
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

import jp.ksksue.serial.R;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import jp.ksksue.driver.serial.*;
public class FTSampleTerminalActivity extends Activity {
	
	FTDriver mSerial;

	private TextView mTvSerial;
	private String mText;
	private boolean mStop=false;
	private boolean mStopped=true;
		
	String TAG = "FTSampleTerminal";
    
    Handler mHandler = new Handler();

    private Button btWrite;
    private EditText etWrite;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mTvSerial = (TextView) findViewById(R.id.tvSerial);
        btWrite = (Button) findViewById(R.id.btWrite);
        etWrite = (EditText) findViewById(R.id.etWrite);
        
        // get service
        mSerial = new FTDriver((UsbManager)getSystemService(Context.USB_SERVICE));
          
        // listen for new devices
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
        
        if(mSerial.begin(9600)) {	// now only 9600 supported
        	mainloop();
        }
        
        // ---------------------------------------------------------------------------------------
       // Write Button
        // ---------------------------------------------------------------------------------------
        btWrite.setOnClickListener(new View.OnClickListener() {
    		@Override
    		public void onClick(View v) {
    			String strWrite = etWrite.getText().toString();
    			mSerial.write(strWrite.getBytes(),strWrite.length());
    		}
        });
    }
    
    @Override
    public void onDestroy() {
		mSerial.end();
		mStop=true;
       unregisterReceiver(mUsbReceiver);
		super.onDestroy();
    }
        
	private void mainloop() {
		new Thread(mLoop).start();
	}
	
	private Runnable mLoop = new Runnable() {
		@Override
		public void run() {
			int i;
			int len;
			byte[] rbuf = new byte[60];
						
			for(;;){//this is the main loop for transferring
				
				//////////////////////////////////////////////////////////
				// Read and Display to Terminal
				//////////////////////////////////////////////////////////
				len = mSerial.read(rbuf);

				// TODO 行数オーバーした場合、スクロール表示させるよう変更。現状エディットボックスが画面外へ追いやられる。
				if(len > 0) {
					Log.i(TAG,"Read  Length : "+len);
					mText = (String) mTvSerial.getText();
					for(i=0;i<len;++i) {
						Log.i(TAG,"Read  Data["+i+"] : "+rbuf[i]);
						
						// "\r":CR(0x0D) "\n":LF(0x0A)
						if(rbuf[i]==0x0D) {							
							mText = mText + "\r";
						} else if(rbuf[i]==0x0A) {
							mText = mText + "\n";
						} else {
							mText = mText + "" +(char)rbuf[i];
						}
					}
					// FIXME もっとビューティーホーに書きたい 
					mHandler.post(new Runnable() {
						public void run() {
							mTvSerial.setText(mText);
						}
					});
				}
				
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if(mStop) {
					mStopped = true;
					return;
				}
			}
		}
	};
	
    // BroadcastReceiver when insert/remove the device USB plug into/from a USB port  
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
    		String action = intent.getAction();
    		if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
    			mSerial.usbAttached(intent);
				mSerial.begin(9600);	// only 9600 supported 
    			mainloop();
				
    		} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
    			mSerial.usbDetached(intent);
    			mSerial.end();
    			mStop=true;
    		}
        }
    };
}
