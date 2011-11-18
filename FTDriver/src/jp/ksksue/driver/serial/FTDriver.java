package jp.ksksue.driver.serial;
/*
 * FTDI Driver Class
 * 
 * Copyright (C) 2011 @ksksue
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

/*
 * FT232RL
 * Baudrate : 9600
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

public class FTDriver {
	
    private static final int FTDI_VID = 0x0403;
    private static final int FTDI_PID = 0x6001;

    private static final int FTDI_RESET_REQUEST = 0;
    private static final int FTDI_RESET_REQUEST_TYPE = 0x40;
    private static final int FTDI_RESET_SIO = 0;
    private static final int FTDI_RESET_PURGE_RX = 1;
    private static final int FTDI_RESET_PURGE_TX = 2;
    
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

        for (UsbDevice device :  mManager.getDeviceList().values()) {
        	  Log.i(TAG,"Devices : "+device.toString());
            UsbInterface intf = findUSBInterfaceByVIDPID(device,FTDI_VID,FTDI_PID);
            if (setUSBInterface(device, intf)) {
                break;
            }
        }
         
        if(!setFTDIEndpoints(mInterface)){
        	return false;
        }
        
        initFTDIChip(mDeviceConnection,baudrate);
        
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
    	
		len = mDeviceConnection.bulkTransfer(mFTDIEndpointIN, rbuf, 64, 0); // RX
		
		// FIXME shift rbuf's pointer 2 to 0. (I don't know how to do.) 
		for(i=0;i<len;++i) {
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
	private void initFTDIChip(UsbDeviceConnection conn,int baudrate) {
		conn.controlTransfer(0x40, 0, 0, 0, null, 0, 0);				//reset
		conn.controlTransfer(0x40, 0, 1, 0, null, 0, 0);				//clear Rx
		conn.controlTransfer(0x40, 0, 2, 0, null, 0, 0);				//clear Tx
		conn.controlTransfer(0x40, 0x02, 0x0000, 0, null, 0, 0);	//flow control none
		conn.controlTransfer(0x40, 0x03, 0x4138, 0, null, 0, 0);	//baudrate 9600
		conn.controlTransfer(0x40, 0x04, 0x0008, 0, null, 0, 0);	//data bit 8, parity none, stop bit 1, tx off
	}
	
	private boolean setFTDIEndpoints(UsbInterface intf) {
		UsbEndpoint epIn,epOut;
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
                	
                	if(device.getVendorId() == FTDI_VID && device.getProductId() == FTDI_PID) {
                    	Log.d(TAG,"Vendor ID : "+device.getVendorId());
                    	Log.d(TAG,"Product ID : "+device.getProductId());
                    	mDevice = device;
                    	mDeviceConnection = connection;
                    	mInterface = intf;
                    	return true;
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
		UsbInterface intf = findUSBInterfaceByVIDPID(device, FTDI_VID,FTDI_PID);
		if (intf != null) {
			Log.d(TAG, "Found USB interface " + intf);
			setUSBInterface(device, intf);
			return true;
		} else {
			return false;
		}
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