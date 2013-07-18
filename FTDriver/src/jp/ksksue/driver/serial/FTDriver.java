
package jp.ksksue.driver.serial;

/*
 * FTDI Driver Class
 * 
 * Copyright (C) 2011 @ksksue
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * thanks to @titoi2 @darkukll @yakagawa @yishii @hyokota555 @juju_suu
 * 
 */

import android.app.PendingIntent;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbConstants;
import android.util.Log;

enum FTDICHIPTYPE {
    FT232RL, FT2232C, FT232H, FT2232D, FT2232HL, FT4232HL, FT230X, CDC, NONE;
}

class UsbId {
    int mVid;
    int mPid;
    int mBcdDevice;
    int mNumOfChannels;
    FTDICHIPTYPE mType;

    UsbId(int vid, int pid, int bcdDevice, int numOfChannels, FTDICHIPTYPE type) {
        mVid = vid;
        mPid = pid;
        mBcdDevice = bcdDevice;
        mNumOfChannels = numOfChannels;
        mType = type;
    }
}

public class FTDriver {

    private static final boolean LOCAL_LOGV = true;

    /*
     * USB read packet loss checker If true then checking USB read packet loss
     * and displaying messages to Logcat requirement : microcomputer send
     * 01234567890123456789012... to Android
     */
    private boolean mReadPakcetChecker = false;
    private static final UsbId[] IDS = {
            new UsbId(0x0403, 0x6001, 6, 1, FTDICHIPTYPE.FT232RL), // FT232RL
            new UsbId(0x0403, 0x6014, 9, 1, FTDICHIPTYPE.FT232H), // FT232H
            new UsbId(0x0403, 0x6010, 5, 2, FTDICHIPTYPE.FT2232C), // FT2232C
            new UsbId(0x0403, 0x6010, 5, 2, FTDICHIPTYPE.FT2232D), // FT2232D
            new UsbId(0x0403, 0x6010, 7, 2, FTDICHIPTYPE.FT2232HL), // FT2232HL
            new UsbId(0x0403, 0x6011, 8, 4, FTDICHIPTYPE.FT4232HL), // FT4232HL
            new UsbId(0x0403, 0x6015, 10, 1, FTDICHIPTYPE.FT230X), // FT230X
            new UsbId(0x0584, 0xB020, 4, 1, FTDICHIPTYPE.FT232RL), // REX-USB60F
            // thanks to
            // @hyokota555
            new UsbId(0x0584, 0xB02F, 4, 1, FTDICHIPTYPE.FT232RL), // REX-USB60MI
            new UsbId(0x0000, 0x0000, 0, 1, FTDICHIPTYPE.CDC), // CDC
    };
    
    private static final UsbId IGNORE_IDS = new UsbId(0x1519, 0x0000, 0, 1, FTDICHIPTYPE.NONE);
    
    private UsbId mSelectedDeviceInfo;

    public static final int CH_A = 1;
    public static final int CH_B = 2;
    public static final int CH_C = 3;
    public static final int CH_D = 4;

    public static final int BAUD300 = 300;
    public static final int BAUD600 = 600;
    public static final int BAUD1200 = 1200;
    public static final int BAUD2400 = 2400;
    public static final int BAUD4800 = 4800;
    public static final int BAUD9600 = 9600;
    public static final int BAUD14400 = 14400;
    public static final int BAUD19200 = 19200;
    public static final int BAUD38400 = 38400;
    public static final int BAUD57600 = 57600;
    public static final int BAUD115200 = 115200;
    public static final int BAUD230400 = 230400;

    public static final int FTDI_SET_DATA_BITS_7 = 7;
    public static final int FTDI_SET_DATA_BITS_8 = 8;
    public static final int FTDI_SET_DATA_PARITY_NONE = (0x0 << 8);
    public static final int FTDI_SET_DATA_PARITY_ODD = (0x1 << 8);
    public static final int FTDI_SET_DATA_PARITY_EVEN = (0x2 << 8);
    public static final int FTDI_SET_DATA_PARITY_MARK = (0x3 << 8);
    public static final int FTDI_SET_DATA_PARITY_SPACE = (0x4 << 8);
    public static final int FTDI_SET_DATA_STOP_BITS_1 = (0x0 << 11);
    public static final int FTDI_SET_DATA_STOP_BITS_15 = (0x1 << 11);
    public static final int FTDI_SET_DATA_STOP_BITS_2 = (0x2 << 11);
    public static final int FTDI_SET_NOBREAK = (0x0 << 14);
    public static final int FTDI_SET_BREAK = (0x1 << 14);
    public static final int FTDI_SET_FLOW_CTRL_NONE = 0x0;
    public static final int FTDI_SET_FLOW_RTS_CTS_HS = (0x1 << 8);
    public static final int FTDI_SET_FLOW_DTR_DSR_HS = (0x2 << 8);
    public static final int FTDI_SET_FLOW_XON_XOFF_HS = (0x4 << 8);

    private int[] mSerialProperty = new int[4];

    public static final int FTDI_MPSSE_BITMODE_RESET = 0x00;
    /** < switch off bitbang mode, back to regular serial/FIFO */
    public static final int FTDI_MPSSE_BITMODE_BITBANG = 0x01;
    /** < classical asynchronous bitbang mode, introduced with B-type chips */
    public static final int FTDI_MPSSE_BITMODE_MPSSE = 0x02;
    /** < MPSSE mode, available on 2232x chips */
    public static final int FTDI_MPSSE_BITMODE_SYNCBB = 0x04;
    /** < synchronous bitbang mode, available on 2232x and R-type chips */
    public static final int FTDI_MPSSE_BITMODE_MCU = 0x08;
    /** < MCU Host Bus Emulation mode, available on 2232x chips */
    public static final int FTDI_MPSSE_BITMODE_OPTO = 0x10;
    /** < Fast Opto-Isolated Serial Interface Mode, available on 2232x chips */
    public static final int FTDI_MPSSE_BITMODE_CBUS = 0x20;
    /** < Bitbang on CBUS pins of R-type chips, configure in EEPROM before */
    public static final int FTDI_MPSSE_BITMODE_SYNCFF = 0x40;
    /** < Single Channel Synchronous FIFO mode, available on 2232H chips */
    public static final int FTDI_MPSSE_BITMODE_FT1284 = 0x80;
    /** < FT1284 mode, available on 232H chips */

    final static int FTDI_SIO_SET_BITMODE_REQUEST = 0x0B;
    final static int FTDI_SIO_READ_PINS_REQUEST = 0x0C;

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
    // private int mBitbangMode = FTDI_MPSSE_BITMODE_RESET;

    public static final int WRITEBUF_SIZE = 4096;

    /* for USB read packet loss checker */
    private int incReadCount = 0;
    private int totalReadCount = 0;
    private boolean updateReadCount = false;

    private boolean isCDC = false;

    public FTDriver(UsbManager manager) {
        mManager = manager;
        mReadbufOffset = 0;
        mReadbufRemain = 0;
        for (int i = 0; i < 4; ++i) {
            // Default Serial Property : Data bit : 8, Parity : none, Stop bit :
            // 1, Tx : off
            mSerialProperty[i] = FTDI_SET_DATA_PARITY_NONE
                    | FTDI_SET_DATA_STOP_BITS_1 | 8;
        }
    }

    // Open an FTDI USB Device
    public boolean begin(int baudrate) {
        for (UsbDevice device : mManager.getDeviceList().values()) {
            Log.i(TAG, "Devices : " + device.toString());

            getPermission(device);
            if (!mManager.hasPermission(device)) {
                return false;
            }

            // TODO: support any connections(current version find a first
            // device)
            if (getUsbInterfaces(device)) {
                break;
            }
        }

        if (mSelectedDeviceInfo == null) {
            return false;
        }

        if (mDevice == null) {
            return false;
        }

        if (mDevice.getDeviceClass() == UsbConstants.USB_CLASS_COMM) {
            isCDC = true;
        } else {
            isCDC = false;
        }

        mFTDIEndpointIN = new UsbEndpoint[mSelectedDeviceInfo.mNumOfChannels];
        mFTDIEndpointOUT = new UsbEndpoint[mSelectedDeviceInfo.mNumOfChannels];

        if (isCDC) {
            if (!getCdcEndpoint()) {
                return false;
            }
        } else {
            if (!setFTDIEndpoints(mInterface,
                    mSelectedDeviceInfo.mNumOfChannels)) {
                return false;
            }
        }

        if (isCDC) {
            initCdcAcm(mDeviceConnection, baudrate);
        } else {
            initFTDIChip(mDeviceConnection, baudrate);
        }

        Log.i(TAG, "Device Serial : " + mDeviceConnection.getSerial());

        return true;
    }

    // Close the device
    public void end() {
        if (mSelectedDeviceInfo != null) {
            if (isCDC) {
                if (mDeviceConnection != null) {
                    if (mInterface[0] != null) {
                        mDeviceConnection.releaseInterface(mInterface[0]);
                        mInterface[0] = null;
                    }
                    if (mInterface[1] != null) {
                        mDeviceConnection.releaseInterface(mInterface[1]);
                        mInterface[1] = null;
                    }
                    mDeviceConnection.close();
                }

                mDevice = null;
                mDeviceConnection = null;
            } else {
                for (int i = 0; i < mSelectedDeviceInfo.mNumOfChannels; ++i) {
                    setUSBInterface(null, null, i);
                }
            }
        }
    }

    // Read Binary Data
    public int read(byte[] buf) {
        return read(buf, 0);
    }

    public int read(byte[] buf, int channel) {

        if (isCDC) {
            int len = mDeviceConnection.bulkTransfer(mFTDIEndpointIN[channel],
                    buf, buf.length, 100); // RX
            if (len < 0) {
                len = 0;
            }
            return len;
        }

        if (channel >= mSelectedDeviceInfo.mNumOfChannels) {
            return -1;
        }
        if (buf.length <= mReadbufRemain) {
            if (!mReadPakcetChecker) {
                System.arraycopy(mReadbuf, mReadbufOffset, buf, 0, buf.length);
            } else {
                /*
                 * USB read packet loss checker check: read byte is
                 * 01234567890123456789012... requirement : microcomputer send
                 * 01234567890123456789012... to Android
                 */
                for (int i = 0; i < buf.length; i++) {
                    buf[i] = mReadbuf[mReadbufOffset++];
                    ++incReadCount;
                    // check count number == read number
                    while ((incReadCount - 1) % 10 != (Byte.valueOf(buf[i]) - '0')) {
                        Log.d(TAG, "!!! Lost Data !!! count : "
                                + (incReadCount - 1) + ", data : " + buf[i]);
                        ++incReadCount;
                    }
                }
                Log.d(TAG,
                        "read buf length 1 : " + Integer.toString(buf.length));
                totalReadCount += buf.length;
                updateReadCount = true;
                /* end of USB read packet loss checker */
            }
            mReadbufOffset += buf.length;
            mReadbufRemain -= buf.length;
            return buf.length;
        }
        int ofst = 0;
        int needlen = buf.length;
        if (mReadbufRemain > 0) {
            needlen -= mReadbufRemain;
            System.arraycopy(mReadbuf, mReadbufOffset, buf, ofst,
                    mReadbufRemain);
            // for (; mReadbufRemain>0 ; mReadbufRemain-- ) {
            // buf[ofst++] = mReadbuf[mReadbufOffset++];
            // }
        }
        int len = mDeviceConnection.bulkTransfer(mFTDIEndpointIN[channel],
                mReadbuf, mReadbuf.length, 0); // RX
        int blocks = len / mPacketSize;
        int remain = len % mPacketSize;
        if (remain > 0) {
            blocks++;
        }
        mReadbufRemain = len - (2 * blocks);
        int rbufindex = 0;
        for (int block = 0; block < blocks; block++) {
            int blockofst = block * mPacketSize;
            // System.arraycopy(mReadbuf, blockofst+2, mReadbuf, rbufindex+1,
            // mPacketSize-2);
            if (isCDC) {
                for (int i = 0; i < mPacketSize; i++) {
                    mReadbuf[rbufindex++] = mReadbuf[blockofst + i];
                }
            } else {
                for (int i = 2; i < mPacketSize; i++) {
                    mReadbuf[rbufindex++] = mReadbuf[blockofst + i];
                }
            }
        }

        mReadbufOffset = 0;

        for (; (mReadbufRemain > 0) && (needlen > 0); mReadbufRemain--, needlen--) {
            buf[ofst++] = mReadbuf[mReadbufOffset++];
            if (mReadPakcetChecker) {
                /*
                 * USB read packet loss checker
                 */
                ++incReadCount;
                while ((incReadCount - 1) % 10 != (Byte.valueOf(buf[ofst - 1]) - '0')) {
                    Log.d(TAG,
                            "!!! Lost Data !!! count : " + (incReadCount - 1)
                                    + ", data : "
                                    + Byte.toString(buf[ofst - 1]));
                    ++incReadCount;
                }
                /* End of packet loss checker */
            }
        }

        /*
         * USB read packet loss checker Display a total of read count
         */
        if (mReadPakcetChecker) {
            if (ofst > 0) {
                Log.d(TAG, "read buf length 2 : " + Integer.toString(ofst));
                totalReadCount += ofst;
                updateReadCount = true;
            }
            if (updateReadCount) {
                Log.d(TAG, "Total of Read Count : " + totalReadCount);
                Log.d(TAG, "Increment Read Count : " + incReadCount);
                updateReadCount = false;
            }
        }
        /* End of packet loss checker */

        return ofst;
    }

    /** 
     * Writes a Complete String
     * 
     * @param str : outgoing string
     * @return written length
     */
    public int write(String str) {
        return write(str.getBytes());
    }
    
    /**
     * Writes 1byte Binary Data
     * 
     * @param buf : write buffer
     * @return written length
     */
    public int write(byte[] buf) {
        return write(buf, buf.length, 0);
    }

    /**
     * Writes n byte Binary Data
     * 
     * @param buf : write buffer
     * @param length : write length
     * @return written length
     */
    public int write(byte[] buf, int length) {
        return write(buf, length, 0);
    }

    /**
     * Writes n byte Binary Data to n channel
     * 
     * @param buf : write buffer
     * @param length : write length
     * @param channel : write channel
     * @return written length
     */
    public int write(byte[] buf, int length, int channel) {
        if (channel >= mSelectedDeviceInfo.mNumOfChannels) {
            return -1;
        }
        int offset = 0;
        int actual_length;
        byte[] write_buf = new byte[WRITEBUF_SIZE];
        // byte[] tmp_buf=new byte[WRITEBUF_SIZE];

        while (offset < length) {
            int write_size = WRITEBUF_SIZE;

            if (offset + write_size > length) {
                write_size = length - offset;
            }
            System.arraycopy(buf, offset, write_buf, 0, write_size);
            // ByteArrayInputStream write_buf = new ByteArrayInputStream(buf);
            // write_buf.read(tmp_buf, offset, write_size);

            actual_length = mDeviceConnection.bulkTransfer(
                    mFTDIEndpointOUT[channel], write_buf, write_size, 0);

            if (actual_length < 0) {
                return -1;
            }
            offset += actual_length;
        }

        return offset;
    }

    public boolean isConnected() {
        if (mDevice != null && mFTDIEndpointIN != null
                && mFTDIEndpointOUT != null) {
            return true;
        } else {
            return false;
        }
    }

    public byte getPinState() {
        int index = 0;
        byte[] buffer;
        buffer = new byte[1];

        mDeviceConnection.controlTransfer(UsbConstants.USB_TYPE_VENDOR
                | UsbConstants.USB_DIR_IN, FTDI_SIO_READ_PINS_REQUEST, 0,
                index, buffer, 1, 0);

        return buffer[0];
    }

    public String getSerialNumber() {
        if(mDeviceConnection == null) {
            return "";
        } else {
            return mDeviceConnection.getSerial();
        }
    }

    public boolean setBitmode(boolean enable, int bitmask, int mode) {
        short val = 0;
        int result;
        boolean ret = false;
        int index = 0; // for devices that have multiple IFs,need to modify.

        if (isCDC) {
            // setCdcBitmode();
            return true;
        }

        if (enable) {
            val = (short) bitmask;
            val |= (mode << 8);
        }

        result = mDeviceConnection.controlTransfer(UsbConstants.USB_TYPE_VENDOR
                | UsbConstants.USB_DIR_OUT, FTDI_SIO_SET_BITMODE_REQUEST, val,
                index, null, 0, 0);

        if (result >= 0) {
            ret = true;
            // mBitbangMode = mode;
        }

        return ret;
    }

    private void setCdcBaudrate(int baudrate) {
        byte[] baudByte = new byte[4];

        baudByte[0] = (byte) (baudrate & 0x000000FF);
        baudByte[1] = (byte) ((baudrate & 0x0000FF00) >> 8);
        baudByte[2] = (byte) ((baudrate & 0x00FF0000) >> 16);
        baudByte[3] = (byte) ((baudrate & 0xFF000000) >> 24);
        mDeviceConnection.controlTransfer(0x21, 0x20, 0, 0, new byte[] {
                baudByte[0], baudByte[1], baudByte[2], baudByte[3], 0x00, 0x00,
                0x08
        }, 7, 0);
    }

    public boolean setBaudrate(int baudrate, int channel) {
        if (mDeviceConnection == null) {
            return false;
        }

        if (isCDC) {
            setCdcBaudrate(baudrate);
            return true;
        }
        int baud = calcFTDIBaudrate(baudrate, mSelectedDeviceInfo.mType);
        int index = 0;

        /*
         * if(mSelectedDeviceInfo.mType == FTDICHIPTYPE.FT232H) { index =
         * 0x0200; }
         */
        if (mSelectedDeviceInfo.mType == FTDICHIPTYPE.FT2232HL
                || mSelectedDeviceInfo.mType == FTDICHIPTYPE.FT4232HL
                || mSelectedDeviceInfo.mType == FTDICHIPTYPE.FT232H) {
            index = baud >> 8;
            index &= 0xFF00;
        } else {
            index = baud >> 16;
        }

        index |= channel; // Ch.A=1, Ch.B=2, ...

        mDeviceConnection.controlTransfer(0x40, 0x03, baud, index, null, 0, 0); // set
        // baudrate

        // TODO: check error
        return true;
    }

    // Initial control transfer
    private void initFTDIChip(UsbDeviceConnection conn, int baudrate) {

        for (int i = 0; i < mSelectedDeviceInfo.mNumOfChannels; ++i) {
            int index = i + 1;
            conn.controlTransfer(0x40, 0, 0, index, null, 0, 0); // reset
            conn.controlTransfer(0x40, 0, 1, index, null, 0, 0); // clear Rx
            conn.controlTransfer(0x40, 0, 2, index, null, 0, 0); // clear Tx
            conn.controlTransfer(0x40, 0x02, 0x0000, index, null, 0, 0); // flow
            // control
            // none
            setBaudrate(baudrate, index);
            conn.controlTransfer(0x40, 0x04, 0x0008, index, null, 0, 0); // data
            // bit
            // 8,
            // parity
            // none,
            // stop
            // bit
            // 1,
            // tx
            // off
        }
    }

    // FIXME ISSUE: When re-connect usb, cannot acvive cdc.
    private void initCdcAcm(UsbDeviceConnection conn, int baudrate) {
        int ret;
        if (!conn.claimInterface(mInterface[0], true)) {
            return;
        }

        ret = conn.controlTransfer(0x21, 0x22, 0x00, 0, null, 0, 0);

        setCdcBaudrate(baudrate);
        // ret = conn.controlTransfer(0x21, 0x20, 0, 0, new byte[] { (byte)
        // 0x80,
        // 0x25, 0x00, 0x00, 0x00, 0x00, 0x08 }, 7, 0);

        isCDC = true;
    }

    /**
     * Sets flow control to an FTDI chip register
     * 
     * @param channel CH_A CH_B CH_C CH_D
     * @param flowControl FTDI_SET_FLOW_CTRL_NONE FTDI_SET_FLOW_RTS_CTS_HS
     *            FTDI_SET_FLOW_DTR_DSR_HS FTDI_SET_FLOW_XON_XOFF_HS
     * @return true : succeed, false : not succeed
     */
    public boolean setFlowControl(int channel, int flowControl) {
        if (mDeviceConnection == null) {
            return false;
        }

        if (isCDC) {
            // setCdcFlowControl();
            return true;
        }

        if (CH_A > channel || channel > CH_D) {
            return false;
        }

        if (flowControl == FTDI_SET_FLOW_CTRL_NONE
                || flowControl == FTDI_SET_FLOW_RTS_CTS_HS
                || flowControl == FTDI_SET_FLOW_DTR_DSR_HS
                || flowControl == FTDI_SET_FLOW_XON_XOFF_HS) {
            int mask=0;
            if(flowControl == FTDI_SET_FLOW_RTS_CTS_HS) {
                mask = 0x1;
            }
            if(flowControl == FTDI_SET_FLOW_DTR_DSR_HS) {
                mask |= 0x2;
            }
            int send = flowControl | mask ;
            if (mDeviceConnection.controlTransfer(0x40, 0x01, send, channel,
                    null, 0, 0) < 0) {
                return false;
            } else {
                if(LOCAL_LOGV){ Log.v(TAG, "setFlowControl : " + toHexStr(send)); }
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Sets the serial properties to an FTDI chip register
     * 
     * @param channel CH_A CH_B CH_C CH_D
     * @return true : succeed, false : not succeed
     */
    public boolean setSerialPropertyToChip(int channel) {
        // TODO : test this method
        if (mDeviceConnection == null) {
            return false;
        }

        if (isCDC) {
            // setCdcSerialPropertyToChip();
            return true;
        }

        if (CH_A > channel || channel > CH_D) {
            return false;
        }

        if (mDeviceConnection.controlTransfer(0x40, 0x04,
                mSerialProperty[channel - 1], channel, null, 0, 0) < 0) {
            return false;
        } else {
            if(LOCAL_LOGV){ Log.v(TAG, "setSerialPropertyToChip : " + toHexStr(mSerialProperty[channel-1])); }
            return true;
        }
    }

    /**
     * Sets the serial property of data bit
     * 
     * @param numOfDataBit : number of data bit(8 or 7)
     * @param channel CH_A CH_B CH_C CH_D
     * @return true : succeed, false : not succeed
     */
    public boolean setSerialPropertyDataBit(int numOfDataBit, int channel) {
        // TODO : test this method
        if ((0 < numOfDataBit) || (numOfDataBit <= 8)) {
            mSerialProperty[channel - 1] = (mSerialProperty[channel - 1] & 0xFFF0)
                    | (numOfDataBit & 0x000F);
            if(LOCAL_LOGV){ Log.v(TAG, "setSerialPropertyDataBit : " + toHexStr(mSerialProperty[channel-1])); }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the serial property of parity bit
     * 
     * @param parity none : FTDI_SET_DATA_PARITY_NONE odd :
     *            FTDI_SET_DATA_PARITY_ODD even : FTDI_SET_DATA_PARITY_EVEN mark
     *            : FTDI_SET_DATA_PARITY_MARK space : FTDI_SET_DATA_PARITY_SPACE
     * @param channel CH_A CH_B CH_C CH_D
     * @return true : succeed, false : not succeed
     */
    public boolean setSerialPropertyParity(int parity, int channel) {
        // TODO : test this method
        if (parity == FTDI_SET_DATA_PARITY_NONE
                || parity == FTDI_SET_DATA_PARITY_ODD
                || parity == FTDI_SET_DATA_PARITY_EVEN
                || parity == FTDI_SET_DATA_PARITY_MARK
                || parity == FTDI_SET_DATA_PARITY_SPACE) {
            mSerialProperty[channel - 1] = (mSerialProperty[channel - 1] & 0xF8FF)
                    | (parity & 0x0700);
            if(LOCAL_LOGV){ Log.v(TAG, "setSerialPropertyParity : " + toHexStr(mSerialProperty[channel-1])); }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the serial property of stop bits
     * 
     * @param stopBits 1 : FTDI_SET_DATA_STOP_BITS_1 1.5 :
     *            FTDI_SET_DATA_STOP_BITS_15 2 : FTDI_SET_DATA_STOP_BITS_2
     * @param channel CH_A CH_B CH_C CH_D
     * @return true : succeed, false : not succeed
     */
    public boolean setSerialPropertyStopBits(int stopBits, int channel) {
        // TODO : test this method
        if (stopBits == FTDI_SET_DATA_STOP_BITS_1
                || stopBits == FTDI_SET_DATA_STOP_BITS_15
                || stopBits == FTDI_SET_DATA_STOP_BITS_2) {
            mSerialProperty[channel - 1] = (mSerialProperty[channel - 1] & 0xE7FF)
                    | (stopBits & 0x1800);
            if(LOCAL_LOGV){ Log.v(TAG, "setSerialPropertyStopBits : " + toHexStr(mSerialProperty[channel-1])); }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the serial property of TX ON/OFF
     * 
     * @param tx TX OFF : FTDI_SET_NOBREAK TX ON : FTDI_SET_BREAK
     * @param channel CH_A CH_B CH_C CH_D
     * @return true : succeed, false : not succeed
     */
    public boolean setSerialPropertyBreak(int tx, int channel) {
        // TODO : test this method
        if (tx == FTDI_SET_NOBREAK || tx == FTDI_SET_BREAK) {
            mSerialProperty[channel - 1] = (mSerialProperty[channel - 1] & 0xBFFF)
                    | (tx & 0x4000);
            if(LOCAL_LOGV){ Log.v(TAG, "setSerialPropertyBreak : " + toHexStr(mSerialProperty[channel-1])); }
            return true;
        } else {
            return false;
        }
    }

    /*
     * Calculate a Divisor at 48MHz 9600 : 0x4138 11400 : 0xc107 19200 : 0x809c
     * 38400 : 0xc04e 57600 : 0x0034 115200 : 0x001a 230400 : 0x000d
     */
    private int calcFTDIBaudrate(int baud, FTDICHIPTYPE chiptype) {
        int divisor = 0;
        if (chiptype == FTDICHIPTYPE.FT232RL
                || chiptype == FTDICHIPTYPE.FT2232C
                || chiptype == FTDICHIPTYPE.FT230X) {
            if (baud <= 3000000) {
                divisor = calcFT232bmBaudBaseToDiv(baud, 48000000);
            } else {
                Log.e(TAG, "Cannot set baud rate : " + baud
                        + ", because too high.");
                Log.e(TAG, "Set baud rate : 9600");
                divisor = calcFT232bmBaudBaseToDiv(9600, 48000000);
            }
        } else if (chiptype == FTDICHIPTYPE.FT232H) {
            if (baud <= 12000000 && baud >= 1200) {
                divisor = calcFT232hBaudBaseToDiv(baud, 120000000);
            } else {
                Log.e(TAG, "Cannot set baud rate : " + baud
                        + ", because too high.");
                Log.e(TAG, "Set baud rate : 9600");
                divisor = calcFT232hBaudBaseToDiv(9600, 120000000);
            }
        }
        return divisor;
    }

    // Calculate a divisor from baud rate and base clock for FT232BM, FT2232C
    // and FT232LR
    // thanks to @titoi2
    private int calcFT232bmBaudBaseToDiv(int baud, int base) {
        int divisor;
        divisor = (base / 16 / baud) | (((base / 2 / baud) & 4) != 0 ? 0x4000 // 0.5
                : ((base / 2 / baud) & 2) != 0 ? 0x8000 // 0.25
                        : ((base / 2 / baud) & 1) != 0 ? 0xc000 // 0.125
                                : 0);
        return divisor;
    }

    // Calculate a divisor from baud rate and base clock for FT2232H and FT232H
    // thanks to @yakagawa
    private int calcFT232hBaudBaseToDiv(int baud, int base) {
        int divisor3, divisor;
        divisor = (base / 10 / baud);
        divisor3 = divisor * 8;
        divisor |= ((divisor3 & 4) != 0 ? 0x4000 // 0.5
                : (divisor3 & 2) != 0 ? 0x8000 // 0.25
                        : (divisor3 & 1) != 0 ? 0xc000 // 0.125
                                : 0);

        // divisor |= 0x00020000;
        divisor &= 0xffff;
        return divisor;
    }

    private boolean getCdcEndpoint() {
        UsbEndpoint ep;

        if (mInterface[0] == null) {
            return false;
        }
        for (int i = 0; i < 2; ++i) {
            ep = mInterface[0].getEndpoint(i);
            if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                mFTDIEndpointIN[0] = ep;
            } else {
                mFTDIEndpointOUT[0] = ep;
            }
        }
        if (mFTDIEndpointIN == null || mFTDIEndpointOUT == null) {
            return false;
        }
        return true;

    }

    private boolean setFTDIEndpoints(UsbInterface[] intf, int portNum) {
        UsbEndpoint epIn;
        UsbEndpoint epOut;

        if (intf[0] == null) {
            return false;
        }

        for (int i = 0; i < portNum; ++i) {
            epIn = intf[i].getEndpoint(0);
            epOut = intf[i].getEndpoint(1);

            if (epIn != null && epOut != null) {
                mFTDIEndpointIN[i] = epIn;
                mFTDIEndpointOUT[i] = epOut;
            } else {
                return false;
            }
        }
        return true;

    }

    // Sets the current USB device and interface
    private boolean setUSBInterface(UsbDevice device, UsbInterface intf,
            int intfNum) {
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
                Log.d(TAG, "open succeeded");
                if (connection.claimInterface(intf, true)) {
                    Log.d(TAG, "claim interface succeeded");

                    // TODO: support any connections(current version find a
                    // first
                    // device)
                    for (UsbId usbids : IDS) {
                        if (device.getVendorId() == IGNORE_IDS.mVid) {
                            break;
                        }
                        // TODO: Refactor it for CDC
                        if ((usbids.mVid == 0 && usbids.mPid == 0 && device
                                .getDeviceClass() == UsbConstants.USB_CLASS_COMM) // CDC
                                || (device.getVendorId() == usbids.mVid && device
                                        .getProductId() == usbids.mPid)) {
                            Log.d(TAG, "Vendor ID : " + device.getVendorId());
                            Log.d(TAG, "Product ID : " + device.getProductId());
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
                Log.d(TAG, "open failed");
            }
        }

        return false;
    }

    // find any interfaces and set mInterface
    private boolean getUsbInterfaces(UsbDevice device) {
        UsbInterface[] intf = new UsbInterface[FTDI_MAX_INTERFACE_NUM];
        boolean ret=false;
        for (UsbId usbids : IDS) {
            
            if(device.getVendorId() == IGNORE_IDS.mVid ) {
                break;
            }
            
            // TODO: Refactor it for CDC
            if (usbids.mVid == 0 && usbids.mPid == 0
                    && device.getDeviceClass() == UsbConstants.USB_CLASS_COMM) {
                for (int i = 0; i < device.getInterfaceCount(); ++i) {
                    if (device.getInterface(i).getInterfaceClass() == UsbConstants.USB_CLASS_CDC_DATA) {
                        intf[0] = device.getInterface(i);
                    }
                }
                if (intf[0] == null) {
                    return false;
                }
            } else {
                intf = findUSBInterfaceByVIDPID(device, usbids.mVid,
                        usbids.mPid);
            }
            if (intf[0] != null) {
                for (int i = 0; i < usbids.mNumOfChannels; ++i) {
                    Log.d(TAG, "Found USB interface " + intf[i]);
                    if(!setUSBInterface(device, intf[i], i)) {
                        return false;
                    }
                    mSelectedDeviceInfo = usbids;
                }
                return true;
            }
        }
        return false;
    }

    // searches for an interface on the given USB device by VID and PID
    private UsbInterface[] findUSBInterfaceByVIDPID(UsbDevice device, int vid,
            int pid) {
        Log.d(TAG, "findUSBInterface " + device);
        UsbInterface[] retIntf = new UsbInterface[FTDI_MAX_INTERFACE_NUM];
        int j = 0;
        int count = device.getInterfaceCount();
        for (int i = 0; i < count; i++) {
            UsbInterface intf = device.getInterface(i);
            if (device.getVendorId() == vid && device.getProductId() == pid) {
                retIntf[j] = intf;
                ++j;
            }
        }
        return retIntf;
    }

    // get a device descriptor : bcdDevice
    // need Android API Level 13
    /*
     * private int getDescriptorBcdDevice() { byte[] rowDesc =
     * mDeviceConnection.getRawDescriptors(); return rowDesc[13] << 8 +
     * rowDesc[12]; }
     */
    private PendingIntent mPermissionIntent;

    /**
     * Sets PendingIntent for requestPermission
     * 
     * @param pi
     * @see getPermission
     */
    public void setPermissionIntent(PendingIntent pi) {
        mPermissionIntent = pi;
    }

    /**
     * Gets an USB permission if no permission
     * 
     * @param device
     * @see setPermissionIntent
     */
    public void getPermission(UsbDevice device) {
        if (device != null && mPermissionIntent != null) {
            if (!mManager.hasPermission(device)) {
                mManager.requestPermission(device, mPermissionIntent);
            }
        }
    }

    /**
     * Gets number of channels
     * 
     * @return Number of channels
     */
    public int getNumberOfChannels() {
        if (mSelectedDeviceInfo != null) {
            return mSelectedDeviceInfo.mNumOfChannels;
        } else {
            return 0;
        }
    }

    // when insert the device USB plug into a USB port
    public boolean usbAttached(Intent intent) {
        UsbDevice device = (UsbDevice) intent
                .getParcelableExtra(UsbManager.EXTRA_DEVICE);
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
    
    private String toHexStr(int val) {
        return String.format("0x%04x", val);
    }
}
