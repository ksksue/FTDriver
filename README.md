Android USB Serial Driver
=====

USB Serial Driver using Android USB Host API  

- Java source code
- **no root**
- baudrate : 9600 - 230400 (be able to setting immediate baudrate number)
- support FTDI chips (FT232RL, FT232H, FT2232C/D/HL, FT4232HL FT230X, REX-USB60F/MI(FT232BL) checked)
- support any channels (FT2232X:2ch, FT4232X:4ch)
- support CDC-ACM(beta)
- like Arduino library's interface

Connection

    Android [USB A port] --- [USB B port] FTDI Chip
- requirement
 - Android : version 3.1 or upper and have an USB host port
 - Board : FTDI Chip or CDC-ACM(beta)

Projects
-----
- Library Project  
 **FTDriver** : Driver for connecting an FTDI chip to Android USB host port

- Sample Projects  
 **FTSampleTerminal** : very simple terminal  
 **FTSerialCSV** : serial communication with a Genet educational board ([www.genet-nara.jp](www.genet-nara.jp "genet")) in CSV format (Thanks for Heima Hayashida)  
![genet](https://lh3.googleusercontent.com/-nj_EGL5D-nY/Tsu-OodpQJI/AAAAAAAABaY/zh6p2mhpg24/s400/DSC_0444.JPG "genet")  
Fig. Connecting an Android tablet to a Genet board(FT232RL) by a USB cable.(ET2011 Forum in Japan)  


Interfaces
----------------


new

    mSerial = new FTDriver((UsbManager)getSystemService(Context.USB_SERVICE));


open

    mSerial.begin(FTDriver.BAUD9600);

+   baud rate (bps)  
BAUD9600  
BAUD14400  
BAUD19200  
BAUD38400  
BAUD57600  
BAUD115200  
BAUD230400  
and be able to setting immediate baud rate (no check).


read n bytes

    byte[] rbuf = new byte[n];
    len = mSerial.read(rbuf);

read n bytes from channel p

    byte[] rbuf = new byte[n];
    len = mSerial.read(rbuf, n, p)


write n bytes

    byte[] wbuf = new byte[n];
    ...(set wbuf)
    len = mSerial.write(wbuf,n);

write n bytes to channel p

    byte[] wbuf = new byte[n];
    ...(set wbuf)
    len = mSerial.write(wbuf,n,p);

close

    mSerial.end();


About me
---
![twitter](http://d.hatena.ne.jp/images/icon-twitter.png "twitter") [@ksksue](http://twitter.com/#!/ksksue "twitter @ksksue")  
![画像1](http://a1.twimg.com/profile_images/549237316/twt_bigger.jpg "icon")  
Web page : Geekle Board - [http://ksksue.com/wiki/](http://ksksue.com/wiki/ "Geekle Board")  

License
----------
Copyright &copy; 2011 @ksksue
Licensed under the [Apache License, Version 2.0][Apache]

[Apache]: http://www.apache.org/licenses/LICENSE-2.0

