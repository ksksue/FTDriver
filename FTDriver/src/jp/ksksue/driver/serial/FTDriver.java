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

enum FTDICHIPTYPE {FT232RL, FT2232C, FT232H, FT2232D, FT2232HL, FT4232HL ; }
class UsbId {
	int mVid;
	int mPid;
	int mBcdDevice;
	int mPortNum;
	FTDICHIPTYPE mType;
	UsbId(int vid, int pid, int bcdDevice, int portNum, FTDICHIPTYPE type){ mVid = vid; mPid = pid; mBcdDevice = bcdDevice; mPortNum = portNum; mType = type;}
}

public class FTDriver {
	
	
	private static final UsbId[] IDS = {
		new UsbId(0x0403, 0x6001, 6, 1, FTDICHIPTYPE.FT232RL),	// FT232RL
		new UsbId(0x0403, 0x6014, 9, 1, FTDICHIPTYPE.FT232H),	// FT232H
		new UsbId(0x0403, 0x6010, 5, 2, FTDICHIPTYPE.FT2232C),	// FT2232C
		new UsbId(0x0403, 0x6010, 5, 2, FTDICHIPTYPE.FT2232D),	// FT2232D
		new UsbId(0x0403, 0x6010, 7, 2, FTDICHIPTYPE.FT2232HL),	// FT2232HL
		new UsbId(0x0403, 0x6011, 8, 4, FTDICHIPTYPE.FT4232HL),	// FT4232HL
	};
    private UsbId mSelectedDeviceInfo;
	
    public static final int BAUD9600	= 9600;
    public static final int BAUD14400	= 14400;
    public static final int BAUD19200	= 19200;
    public static final int BAUD38400	= 38400;
    public static final int BAUD57600	= 57600;
    public static final int BAUD115200	= 115200;
    public static final int BAUD230400	= 230400;
    
    public static final int FTDI_MAX_INTERFACE_NUM = 4;
    
    private static final String TAG = "FTDriver";
    private final int mPacketSize = 64;

    private UsbManager mManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mDeviceConnection;
    private UsbInterface[] mInterface = new UsbInterface[FTDI_MAX_INTERFACE_NUM];

    private UsbEndpoint[] mFTDIEndpointIN;
    private UsbEndpoint[] mFTDIEndpointOUT;
    
    public static final int READBUF_SIZE = 4096;
    private int mReadbufOffset;
    private int mReadbufRemain;
    private byte[] mReadbuf = new byte[READBUF_SIZE];
    
    public FTDriver(UsbManager manager) {
        mManager = manager;
        mReadbufOffset = 0;
        mReadbufRemain = 0;
    }
    
    // Open an FTDI USB Device
    public boolean begin(int baudrate) {
    	for (UsbDevice device :  mManager.getDeviceList().values()) {
    		Log.i(TAG,"Devices : "+device.toString());
        	  
    		// TODO: support any connections(current version find a first device)
    		if(getUsbInterfaces(device)) {
    			break;
    		}
        }
         
    	if(mSelectedDeviceInfo == null) {
    		return false;
    	}
    	
		mFTDIEndpointIN = new UsbEndpoint[mSelectedDeviceInfo.mPortNum];
		mFTDIEndpointOUT = new UsbEndpoint[mSelectedDeviceInfo.mPortNum];
		
        if(!setFTDIEndpoints(mInterface,mSelectedDeviceInfo.mPortNum)){
        	return false;
        }
        initFTDIChip(mDeviceConnection,baudrate);
        
        Log.i(TAG,"Device Serial : "+mDeviceConnection.getSerial());
                
        return true;
    }

    // Close the device
    public void end() {
    	for(int i=0; i<mSelectedDeviceInfo.mPortNum; ++i) {
    		setUSBInterface(null,null,i);
    	}
    }

    // Read Binary Data
    public int read(byte[] buf) {
    	return read(buf,0);
    }
    
    // TODO: BUG : sometimes miss data transfer
    public int read(byte[] buf, int channel) {
    	if(channel >= mSelectedDeviceInfo.mPortNum) {
    		return -1;
    	}
    	if (buf.length <= mReadbufRemain) {
//        	System.arraycopy(mReadbuf, mReadbufOffset, buf, 0, buf.length);
        	for (int i=0; i<buf.length; i++ ) {
        		buf[i] = mReadbuf[mReadbufOffset++];
        	}
            mReadbufRemain -= buf.length;
        	return buf.length;
        }
        int ofst = 0;
        int needlen = buf.length;
        if (mReadbufRemain>0) {
            needlen -= mReadbufRemain;
            System.arraycopy(mReadbuf, mReadbufOffset, buf, ofst, mReadbufRemain);
//            for (; mReadbufRemain>0 ; mReadbufRemain-- ) {
//            	buf[ofst++] = mReadbuf[mReadbufOffset++];
//            }
        }
        int len = mDeviceConnection.bulkTransfer(mFTDIEndpointIN[channel], mReadbuf, mReadbuf.length,
                0); // RX
        int blocks = len / mPacketSize;
        int remain = len % mPacketSize;
        if (remain>0) {
            blocks++;
        }
        mReadbufRemain = len - (2*blocks);
        int rbufindex = 0;
        for (int block=0; block<blocks; block++) {
            int blockofst = block*mPacketSize;
//            System.arraycopy(mReadbuf, blockofst+2, mReadbuf, rbufindex+1, mPacketSize-2);
            for (int i=2; i<mPacketSize ; i++ ) {
            	mReadbuf[rbufindex++] = mReadbuf[blockofst+i];
            }
        }
        
        mReadbufOffset = 0;
        
        for (;(mReadbufRemain>0) && (needlen>0);mReadbufRemain--,needlen--) {
            buf[ofst++] = mReadbuf[mReadbufOffset++];            
        }
        return ofst;
    }
    
    // Write 1byte Binary Data
    public int write(byte[] buf) {
    	return write(buf,buf.length,0);
    }
    	
    // Write n byte Binary Data
    public int write(byte[] buf,int length) {
    	if(length > 64) {
    		return -1;
    	}
    	return write(buf,length,0);
    }

    // Write n byte Binary Data to n channel
    public int write(byte[] buf, int length, int channel) {
    	if(channel >= mSelectedDeviceInfo.mPortNum) {
    		return -1;
    	}
		return mDeviceConnection.bulkTransfer(mFTDIEndpointOUT[channel], buf, length, 0); // TX    	
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

    public boolean setBaudrate(int baudrate, int channel) {
		int baud = calcFTDIBaudrate(baudrate, mSelectedDeviceInfo.mType);
		int index = 0;
		
/*		if(mSelectedDeviceInfo.mType == FTDICHIPTYPE.FT232H) {
			index = 0x0200;
		}*/
		if (mSelectedDeviceInfo.mType == FTDICHIPTYPE.FT2232HL ||
				mSelectedDeviceInfo.mType == FTDICHIPTYPE.FT4232HL ||
				mSelectedDeviceInfo.mType == FTDICHIPTYPE.FT232H ) {
			index = baud >> 8;
			index &= 0xFF00;
		} else{
			index = baud >> 16;
		}
		
		index |= channel+1;	// Ch.A=1, Ch.B=2, ...
		
		mDeviceConnection.controlTransfer(0x40, 0x03, baud, index, null, 0, 0);		//set baudrate
		
		// TODO: check error
		return true;
    }
    // Initial control transfer
	private void initFTDIChip(UsbDeviceConnection conn,int baudrate) {
		
		for(int i=0;i < mSelectedDeviceInfo.mPortNum; ++i) {
			int index = i+1;
			conn.controlTransfer(0x40, 0, 0, index, null, 0, 0);				//reset
			conn.controlTransfer(0x40, 0, 1, index, null, 0, 0);				//clear Rx
			conn.controlTransfer(0x40, 0, 2, index, null, 0, 0);				//clear Tx
			conn.controlTransfer(0x40, 0x02, 0x0000, index, null, 0, 0);	//flow control none
			setBaudrate(baudrate, i);
			conn.controlTransfer(0x40, 0x04, 0x0008, index, null, 0, 0);	//data bit 8, parity none, stop bit 1, tx off
		}
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

	private boolean setFTDIEndpoints(UsbInterface[] intf, int portNum) {
		UsbEndpoint epIn;
		UsbEndpoint epOut;
				
		if(intf == null) {
			return false;
		}
		
		for(int i=0; i<portNum; ++i) {
			epIn = intf[i].getEndpoint(0);
			epOut = intf[i].getEndpoint(1);
			
	    	if(epIn != null && epOut != null) {
	    		mFTDIEndpointIN[i] = epIn;
	    		mFTDIEndpointOUT[i] = epOut;
	    	} else {
	    		return false;
	    	}
		}
		return true;
		
	}
	
    // Sets the current USB device and interface
    private boolean setUSBInterface(UsbDevice device, UsbInterface intf, int intfNum) {
        if (mDeviceConnection != null) {
            if (mInterface[intfNum] != null) {
                mDeviceConnection.releaseInterface(mInterface[intfNum]);
                mInterface[intfNum] = null;
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
                			mInterface[intfNum] = intf;
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

    // find any interfaces and set mInterface
    private boolean getUsbInterfaces(UsbDevice device){
    	UsbInterface[] intf = new UsbInterface[FTDI_MAX_INTERFACE_NUM];
		for(UsbId usbids : IDS){
			intf = findUSBInterfaceByVIDPID(device, usbids.mVid, usbids.mPid);
			if (intf[0] != null) {
				for(int i=0; i<usbids.mPortNum; ++i) {
					Log.d(TAG, "Found USB interface " + intf[i]);
					setUSBInterface(device, intf[i], i);
					mSelectedDeviceInfo = usbids;
				}
				return true;
			}
		}
		return false;
    }
    
    // searches for an interface on the given USB device by VID and PID
    private UsbInterface[] findUSBInterfaceByVIDPID(UsbDevice device,int vid, int pid) {
        Log.d(TAG, "findUSBInterface " + device);
        UsbInterface[] retIntf = new UsbInterface[FTDI_MAX_INTERFACE_NUM];
        int j=0;
        int count = device.getInterfaceCount();
        for (int i = 0; i < count; i++) {
            UsbInterface intf = device.getInterface(i);
            if (device.getVendorId() == vid && device.getProductId() == pid) {
            	retIntf[j]=intf;
            	++j;
              }
        }
        return retIntf;
    }
    
    // get a device descriptor : bcdDevice
    // need Android API Level 13
/*    private int getDescriptorBcdDevice() {
    	byte[] rowDesc = mDeviceConnection.getRawDescriptors();
    	return rowDesc[13] << 8 + rowDesc[12];
    }
*/    
    
    // when insert the device USB plug into a USB port
	public boolean usbAttached(Intent intent) {
		UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		return getUsbInterfaces(device);
	}
	
	// when remove the device USB plug from a USB port
	public void usbDetached(Intent intent) {
		UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		String deviceName = device.getDeviceName();
		if (mDevice != null && mDevice.equals(deviceName)) {
			Log.d(TAG, "USB interface removed");
			setUSBInterface(null, null, 0);
		}
	}

}