Android USB Serial Driver
=====

USB Serial Driver using Android USB Host API

- no Android root
- baudrate : 9600 - 230400
- some FTDI chip (FT232RL checked)
- driver methods like Arduino library's

Connection

    Android [USB A port] --- [USB B port] FTDI Chip

Projects
-----
- Library Project  
 **FTDriver** : Driver for connecting an FTDI USB port to Android USB host port

- Sample Projects  
 **FTSampleTerminal** : very very simple terminal  
 **FTSerialCSV** : serial communication with Genet educational board ([www.genet-nara.jp](www.genet-nara.jp "genet")) in CSV format

Usage
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
and you can set immediate baud rate (no check).


n byte read (n = 1~60)

    byte[] rbuf = new byte[n];
    len = mSerial.read(rbuf);


n byte write (n = 1~64)

    byte[] wbuf = new byte[n];
    ...(set wbuf)
    len = mSerial.write(wbuf,n);


close

    mSerial.end();


About me
---
![twitter](http://d.hatena.ne.jp/images/icon-twitter.png "twitter") [@ksksue](http://twitter.com/#!/ksksue "twitter @ksksue")  
![画像1](http://a1.twimg.com/profile_images/549237316/twt_bigger.jpg "icon")  
Web page : Geekle Board - [http://d.hatena.ne.jp/ksksue/](http://d.hatena.ne.jp/ksksue/ "Geekle Board")  

License
----------
Copyright &copy; 2011 @ksksue
Licensed under the [Apache License, Version 2.0][Apache]

[Apache]: http://www.apache.org/licenses/LICENSE-2.0

