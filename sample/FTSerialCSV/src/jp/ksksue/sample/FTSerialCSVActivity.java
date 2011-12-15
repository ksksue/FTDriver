package jp.ksksue.sample;
/*
 * Copyright (C) 2011 @ksksue
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

import java.util.StringTokenizer;

import jp.ksksue.serial.R;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.DropBoxManager;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.drawable.Drawable;

import jp.ksksue.driver.serial.*;
public class FTSerialCSVActivity extends Activity {
	
	FTDriver mSerial;
    
	private TextView mTvTemp;
	private TextView mTvHumid;
	private TextView mTvIllumi;
	private TextView mTvButton;
	private TextView mTvRTC;
	
	private boolean mStop=false;
	private boolean mStopped=true;

	String stTemp;
	String stHumid;
	String stIllumi;
	String stButton1;
	String stButton2;
	String stButton3;
	String stButton4;
	String stButton5;
	String stRTC;
	
	String TAG = "FTSerialCSV";
    
    Handler mHandler = new Handler();

    private Button btSpeakerOff;
    private Button btSpeaker1;
    private Button btSpeaker2;
    private Button btSpeaker3;
    private Button btLED1;
    private Button btLED2;
    private Button btLCDup;
    private Button btLCDdown;
    private Button btRTC;

	private ImageView ivAndroid;
	
    private EditText etWrite;

	private Drawable mCurrentImage;
	private Drawable mOffImage;
	private Drawable mOnImage;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mTvTemp		= (TextView) findViewById(R.id.tvTemp);
        mTvHumid		= (TextView) findViewById(R.id.tvHumid);
        mTvIllumi		= (TextView) findViewById(R.id.tvIllumi);
//        mTvButton		= (TextView) findViewById(R.id.tvButton);
//        mTvRTC		= (TextView) findViewById(R.id.tvRTC);

        btSpeakerOff	= (Button) findViewById(R.id.btSpeakerOff);
        btSpeaker1	= (Button) findViewById(R.id.btSpeaker1);
        btSpeaker2	= (Button) findViewById(R.id.btSpeaker2);
        btSpeaker3	= (Button) findViewById(R.id.btSpeaker3);
        btLED1		= (Button) findViewById(R.id.btLED1);
        btLED2		= (Button) findViewById(R.id.btLED2);
        btLCDup		= (Button) findViewById(R.id.btLCDup);
        btLCDdown		= (Button) findViewById(R.id.btLCDdown);
        btRTC			= (Button) findViewById(R.id.btRTC);
        
        ivAndroid		= (ImageView) findViewById(R.id.btAndroid);
        etWrite = (EditText) findViewById(R.id.etWrite);
        
        int onImageId = R.drawable.indicator_button_capacitive_on_noglow;
		 int offImageId = R.drawable.indicator_button_capacitive_off_noglow;

		 mOffImage = this.getResources().getDrawable(offImageId);
		 mOnImage = this.getResources().getDrawable(onImageId);
		 mCurrentImage = mOffImage;
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
        // Speaker Button
         // ---------------------------------------------------------------------------------------
         btSpeakerOff.setOnClickListener(new View.OnClickListener() {
     		@Override
     		public void onClick(View v) {
     			String stCommand = "sp,"+"0"+"\n";
     			mSerial.write(stCommand.getBytes(),stCommand.length());
     		}
         });
         
         // ---------------------------------------------------------------------------------------
         // Speaker Button
          // ---------------------------------------------------------------------------------------
          btSpeaker1.setOnClickListener(new View.OnClickListener() {
      		@Override
      		public void onClick(View v) {
      			String stCommand = "sp,"+"1"+"\n";
      			mSerial.write(stCommand.getBytes(),stCommand.length());
      		}
          });
          // ---------------------------------------------------------------------------------------
          // Speaker Button
           // ---------------------------------------------------------------------------------------
           btSpeaker2.setOnClickListener(new View.OnClickListener() {
       		@Override
       		public void onClick(View v) {
       			String stCommand = "sp,"+"2"+"\n";
       			mSerial.write(stCommand.getBytes(),stCommand.length());
       		}
           });
           // ---------------------------------------------------------------------------------------
           // Speaker Button
            // ---------------------------------------------------------------------------------------
            btSpeaker3.setOnClickListener(new View.OnClickListener() {
        		@Override
        		public void onClick(View v) {
        			String stCommand = "sp,"+"3"+"\n";
        			mSerial.write(stCommand.getBytes(),stCommand.length());
        		}
            });

         // ---------------------------------------------------------------------------------------
         // LED1 Button
          // ---------------------------------------------------------------------------------------
          btLED1.setOnClickListener(new View.OnClickListener() {
      		@Override
      		public void onClick(View v) {
     			String stCommand = "ld,"+"1"+"\n";
     			mSerial.write(stCommand.getBytes(),stCommand.length());
      		}
          });

          // ---------------------------------------------------------------------------------------
          // LED2 Button
           // ---------------------------------------------------------------------------------------
           btLED2.setOnClickListener(new View.OnClickListener() {
       		@Override
       		public void onClick(View v) {
       			String stCommand = "ld,"+"1"+"\n";
       			mSerial.write(stCommand.getBytes(),stCommand.length());
       		}
           });

           // ---------------------------------------------------------------------------------------
           // LCDup Button
            // ---------------------------------------------------------------------------------------
            btLCDup.setOnClickListener(new View.OnClickListener() {
        		@Override
        		public void onClick(View v) {
         			String stCommand = "l1,"+"1"+"\n";
         			mSerial.write(stCommand.getBytes(),stCommand.length());
        		}
            });

            // ---------------------------------------------------------------------------------------
            // Speaker Button
             // ---------------------------------------------------------------------------------------
             btLCDdown.setOnClickListener(new View.OnClickListener() {
         		@Override
         		public void onClick(View v) {
         			String stCommand = "l2,"+"1"+"\n";
         			mSerial.write(stCommand.getBytes(),stCommand.length());
         		}
             });

             // ---------------------------------------------------------------------------------------
             // RTC Button
              // ---------------------------------------------------------------------------------------
              btRTC.setOnClickListener(new View.OnClickListener() {
          		@Override
          		public void onClick(View v) {
          			String stCommand = "rt,"+"1"+"\n";
          			mSerial.write(stCommand.getBytes(),stCommand.length());
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
			String stReadBuf;
			boolean breakwhile;
			boolean androidOn=false;
			
			for(;;){//this is the main loop for transferring
				
				stReadBuf = "";
				
				breakwhile = true;
				//////////////////////////////////////////////////////////
				// Read
				//////////////////////////////////////////////////////////
				while(breakwhile) {
					len = mSerial.read(rbuf);
					for(i=0;i<len;++i) {
						Log.i(TAG,"Read  Data["+i+"] : "+rbuf[i]);

						if(rbuf[i]==0x0A) {	// "\n":LF(0x0A)
							breakwhile = false;
						} else {
							stReadBuf = stReadBuf + "" +(char)rbuf[i];
						}
					}
				}

				// Androidを点滅
				if(androidOn) {
					mCurrentImage = mOffImage;
					androidOn = false;
				} else {
					mCurrentImage = mOnImage;
					androidOn = true;
				}

				StringTokenizer st = new StringTokenizer(stReadBuf,",");

				stTemp		= st.nextToken();
				stHumid	= st.nextToken();
				stIllumi	= st.nextToken();
				stButton1	= st.nextToken();
				stButton2	= st.nextToken();
				stButton3	= st.nextToken();
				stButton4	= st.nextToken();
				stButton5	= st.nextToken();
				stRTC		= st.nextToken();

				Log.i(TAG,"Temp : " + stTemp + ", stHumid : " + stHumid + ", stIllumi : " + stIllumi);
				Log.i(TAG,"Button1 : " + stButton1 + ", Button2 : " + stButton2 + ", Button3 : " + stButton3 + ", Button4 : " + stButton4 +"Button5 : " + stButton5 );
				Log.i(TAG,"RTC : " + stRTC);

				// FIXME もっとビューティーホーに書きたい 
				mHandler.post(new Runnable() {
					public void run() {
						ivAndroid.setImageDrawable(mCurrentImage);
						mTvTemp.setText(stTemp);
						mTvHumid.setText(stHumid);
						mTvIllumi.setText(stIllumi);
//						mTvButton.setText("b1 : "+stButton1+",b2 : "+stButton2+",b3 : "+stButton3+",b4 : "+stButton4+",b5 : "+stButton5);
//						mTvRTC.setText(stRTC);
					}
				});

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
