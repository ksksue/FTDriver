package jp.ksksue.driver.serial;
/*
 * FTDI Driver Class
 * 
 * Copyright (C) 2011 @ksksue
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * thanks to @titoi2 @darkukll @yakagawa
 */

/*
 * FT232RL, FT2232C, FT232H
 * Baudrate : any
 * RX Data Size up to 60byte
 * TX Data Size up to 64byte
 */

import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

enum FTDICHIPTYPE {FT232RL, FT2232C, FT232H, ; }
class UsbId {
	int mVid;
	int mPid;
	FTDICHIPTYPE mType;
	UsbId(int vid, int pid, FTDICHIPTYPE type){ mVid = vid; mPid = pid; mType = type;}
}

public class FTDriver {
	
	
	private static final UsbId[] IDS = {
		new UsbId(0x0403, 0x6001, FTDICHIPTYPE.FT232RL),	// FT232RL
		new UsbId(0x0403, 0x6010, FTDICHIPTYPE.FT2232C),	// FT2232C
		new UsbId(0x0403, 0x6014, FTDICHIPTYPE.FT232H),	// FT232H
	};

    public static final int BAUD9600	= 9600;
    public static final int BAUD14400	= 14400;
    public static final int BAUD19200	= 19200;
    public static final int BAUD38400	= 38400;
    public static final int BAUD57600	= 57600;
    public static final int BAUD115200	= 115200;
    public static final int BAUD230400	= 230400;

    private static final String TAG = "FTDriver";

    private UsbManager mManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mDeviceConnection;
    private UsbInterface mInterface;

    private UsbEndpoint mFTDIEndpointIN;
    private UsbEndpoint mFTDIEndpointOUT;
    

    public FTDriver(UsbManager manager) {
        mManager = manager;
    }
    
    // Open an FTDI USB Device
    public boolean begin(int baudrate) {
    	FTDICHIPTYPE chiptype = FTDICHIPTYPE.FT232RL;
        for (UsbDevice device :  mManager.getDeviceList().values()) {
        	  Log.i(TAG,"Devices : "+device.toString());
        	  
        	  // TODO: support any connections(current version find a first device)
        	  for (UsbId usbids : IDS) {
        		  UsbInterface intf = findUSBInterfaceByVIDPID(device,usbids.mVid,usbids.mPid);
        		  if (setUSBInterface(device, intf)) {
        			  Log.i(TAG, "VID:" + usbids.mVid + ", PID:" + usbids.mPid + ", Type:" + usbids.mType);
        			  Log.d(TAG, "#of Interfaces: " + device.getInterfaceCount());
        			  chiptype = usbids.mType;
        			  break;
        		  }
        	  }
        }
         
        if(!setFTDIEndpoints(mInterface)){
        	return false;
        }
        initFTDIChip(mDeviceConnection,baudrate, chiptype);
        
        Log.i(TAG,"Device Serial : "+mDeviceConnection.getSerial());
                
        return true;
    }

    // Close the device
    public void end() {
    	setUSBInterface(null,null);
    }

    // Read Binary Data
    public int read(byte[] buf) {
    	int i,len;
    	byte[] rbuf = new byte[64];
    	
    	if(buf.length > 64) {
    		return -1;
    	}
    	
		len = mDeviceConnection.bulkTransfer(mFTDIEndpointIN, rbuf, buf.length, 0); // RX
		
		// FIXME shift rbuf's pointer 2 to 0. (I don't know how to do.) 
		for(i=0;i<len-2;++i) {
			buf[i] = rbuf[i+2];
		}
		return (len-2);
    }

    // Write 1byte Binary Data
    public int write(byte[] buf) {
		return mDeviceConnection.bulkTransfer(mFTDIEndpointOUT, buf, 1, 0); // TX    	
    }
	
    // Write n byte Binary Data
    public int write(byte[] buf,int length) {
    	if(length > 64) {
    		return -1;
    	}
		return mDeviceConnection.bulkTransfer(mFTDIEndpointOUT, buf, length, 0); // TX    	
    }
    
    // TODO Implement these methods
/*    public void available() {
    	
    }
    
    public void peek() {
    	
    }
    
    public void flush() {
    	
    }
    
    public void print() {
    	
    }
    
    public void println() {
    	
    }
    */

    // Initial control transfer
	private void initFTDIChip(UsbDeviceConnection conn,int baudrate, FTDICHIPTYPE chiptype) {
		int baud = calcFTDIBaudrate(baudrate, chiptype);
		int index = 0;
		if(chiptype == FTDICHIPTYPE.FT232H) {
			index = 0x0200;
		}
		conn.controlTransfer(0x40, 0, 0, 0, null, 0, 0);				//reset
		conn.controlTransfer(0x40, 0, 1, 0, null, 0, 0);				//clear Rx
		conn.controlTransfer(0x40, 0, 2, 0, null, 0, 0);				//clear Tx
		conn.controlTransfer(0x40, 0x02, 0x0000, 0, null, 0, 0);	//flow control none
		conn.controlTransfer(0x40, 0x03, baud, index, null, 0, 0);		//set baudrate
		conn.controlTransfer(0x40, 0x04, 0x0008, 0, null, 0, 0);	//data bit 8, parity none, stop bit 1, tx off
	}
	
	/* Calculate a Divisor at 48MHz
	 * 9600	: 0x4138
	 * 11400	: 0xc107
	 * 19200	: 0x809c
	 * 38400	: 0xc04e
	 * 57600	: 0x0034
	 * 115200	: 0x001a
	 * 230400	: 0x000d
	 */
	private int calcFTDIBaudrate(int baud, FTDICHIPTYPE chiptype) {
		int divisor = 0;
		if( chiptype == FTDICHIPTYPE.FT232RL || chiptype == FTDICHIPTYPE.FT2232C ){
			if(baud <= 3000000) {
				divisor = calcFT232bmBaudBaseToDiv(baud, 48000000);
			} else {
				Log.e(TAG,"Cannot set baud rate : " + baud + ", because too high." );
				Log.e(TAG,"Set baud rate : 9600" );
				divisor = calcFT232bmBaudBaseToDiv(9600, 48000000);
			}
		}else if (chiptype == FTDICHIPTYPE.FT232H){
			if(baud <= 12000000 && baud >= 1200) {
				divisor = calcFT232hBaudBaseToDiv(baud, 120000000);
			} else {
				Log.e(TAG,"Cannot set baud rate : " + baud + ", because too high." );
				Log.e(TAG,"Set baud rate : 9600" );
				divisor = calcFT232hBaudBaseToDiv(9600, 120000000);
			}
		}
		return divisor;
	}

	// Calculate a divisor from baud rate and base clock for FT232BM, FT2232C and FT232LR
	// thanks to @titoi2
	private int calcFT232bmBaudBaseToDiv(int baud, int base) {
		int divisor;
		divisor = (base / 16 / baud)
		| (((base / 2 / baud) & 4) != 0 ? 0x4000 // 0.5
				: ((base / 2 / baud) & 2) != 0 ? 0x8000 // 0.25
						: ((base / 2 / baud) & 1) != 0 ? 0xc000 // 0.125
								: 0);
		return divisor;
	}
	// Calculate a divisor from baud rate and base clock for FT2232H and FT232H
	// thanks to @yakagawa
	private int calcFT232hBaudBaseToDiv(int baud, int base) {
		int divisor3, divisor;
		divisor  = (base / 10 / baud);
		divisor3 = divisor * 8;
		divisor |= ((divisor3 & 4) != 0 ? 0x4000 // 0.5
				: (divisor3 & 2) != 0 ? 0x8000 // 0.25
						: (divisor3 & 1) != 0 ? 0xc000 // 0.125
								: 0);

//		divisor |= 0x00020000;
		divisor &= 0xffff;
		return divisor;
	}

	private boolean setFTDIEndpoints(UsbInterface intf) {
		UsbEndpoint epIn,epOut;
		if(intf == null) {
			return false;
		}
    	epIn = intf.getEndpoint(0);
    	epOut = intf.getEndpoint(1);
		
    	if(epIn != null && epOut != null) {
    		mFTDIEndpointIN = intf.getEndpoint(0);
    		mFTDIEndpointOUT = intf.getEndpoint(1);
    		return true;
    	} else {
    		return false;
    	}
	}
	
    // Sets the current USB device and interface
    private boolean setUSBInterface(UsbDevice device, UsbInterface intf) {
        if (mDeviceConnection != null) {
            if (mInterface != null) {
                mDeviceConnection.releaseInterface(mInterface);
                mInterface = null;
            }
            mDeviceConnection.close();
            mDevice = null;
            mDeviceConnection = null;
        }

        if (device != null && intf != null) {
            UsbDeviceConnection connection = mManager.openDevice(device);
            if (connection != null) {
                Log.d(TAG,"open succeeded");
                if (connection.claimInterface(intf, false)) {
                	Log.d(TAG,"claim interface succeeded");
                	
                	// TODO: support any connections(current version find a first device)
                	for(UsbId usbids : IDS) {
                		if(device.getVendorId() == usbids.mVid && device.getProductId() == usbids.mPid) {
                			Log.d(TAG,"Vendor ID : "+device.getVendorId());
                			Log.d(TAG,"Product ID : "+device.getProductId());
                			mDevice = device;
                			mDeviceConnection = connection;
                			mInterface = intf;
                			return true;
                		}
                	}
                } else {
                    Log.d(TAG,"claim interface failed");
                    connection.close();
                }
            } else {
                Log.d(TAG,"open failed");
            }
        }
        return false;
    }
    
    // searches for an interface on the given USB device by VID and PID
    private UsbInterface findUSBInterfaceByVIDPID(UsbDevice device,int vid, int pid) {
        Log.d(TAG, "findUSBInterface " + device);
        int count = device.getInterfaceCount();
        for (int i = 0; i < count; i++) {
            UsbInterface intf = device.getInterface(i);
            if (device.getVendorId() == vid && device.getProductId() == pid) {
                return intf;
              }
        }
        return null;
    }
    
    // when insert the device USB plug into a USB port
	public boolean usbAttached(Intent intent) {
		UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
    	// TODO: support any connections(current version find a first device)
		for(UsbId usbids : IDS){
			UsbInterface intf = findUSBInterfaceByVIDPID(device, usbids.mVid, usbids.mPid);
			if (intf != null) {
				Log.d(TAG, "Found USB interface " + intf);
				setUSBInterface(device, intf);
				return true;
			}
		}
		return false;
	}
	
	// when remove the device USB plug from a USB port
	public void usbDetached(Intent intent) {
		UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		String deviceName = device.getDeviceName();
		if (mDevice != null && mDevice.equals(deviceName)) {
			Log.d(TAG, "USB interface removed");
			setUSBInterface(null, null);
		}
	}

}