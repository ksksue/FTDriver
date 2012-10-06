/*
 * FTDriver Tutorial 1
 * 
 * You can learn usage of begin(),end(),read(),write().
 * Check [FTDriver] Tag.
 */

package jp.ksksue.tutorial.ftdrivertutorial1;

import jp.ksksue.driver.serial.FTDriver;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class FTDriverTutorial1 extends Activity {

    // [FTDriver] Object
    FTDriver mSerial;
    
    // [FTDriver] Permission String
    private static final String ACTION_USB_PERMISSION =
            "jp.ksksue.tutorial.USB_PERMISSION";
    
    Button btnBegin,btnRead,btnWrite,btnEnd;
    TextView tvMonitor;
    StringBuilder mText = new StringBuilder();;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftdriver_tutorial1);

        btnBegin = (Button) findViewById(R.id.btnBegin);
        btnRead = (Button) findViewById(R.id.btnRead);
        btnWrite = (Button) findViewById(R.id.btnWrite);
        btnEnd = (Button) findViewById(R.id.btnEnd);
        
        btnRead.setEnabled(false);
        btnWrite.setEnabled(false);
        btnEnd.setEnabled(false);
        
        tvMonitor = (TextView) findViewById(R.id.tvMonitor);
        
        // [FTDriver] Create Instance
        mSerial = new FTDriver((UsbManager)getSystemService(Context.USB_SERVICE));

        // [FTDriver] setPermissionIntent() before begin()
        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                ACTION_USB_PERMISSION), 0);
        mSerial.setPermissionIntent(permissionIntent);
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_ftdriver_tutorial1, menu);
        return true;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        // [FTDriver] Close USB Serial
        mSerial.end();
    }
    
    public void onBeginClick(View view) {
        // [FTDriver] Open USB Serial
        if(mSerial.begin(FTDriver.BAUD9600)) {
            btnBegin.setEnabled(false);
            btnRead.setEnabled(true);
            btnWrite.setEnabled(true);
            btnEnd.setEnabled(true);
            
            Toast.makeText(this, "connected", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "cannot connect", Toast.LENGTH_SHORT).show();
        }
    }
    
    public void onReadClick(View view) {
        int i,len;

        // [FTDriver] Create Read Buffer
        byte[] rbuf = new byte[4096]; // 1byte <--slow-- [Transfer Speed] --fast--> 4096 byte

        // [FTDriver] Read from USB Serial
        len = mSerial.read(rbuf);

        for(i=0; i<len; i++) {
            mText.append((char) rbuf[i]);
        }
        tvMonitor.setText(mText);
    }
    
    public void onWriteClick(View view) {
        String wbuf = "FTDriver Test.";

        // [FTDriver] Wirte to USB Serial
        mSerial.write(wbuf.getBytes());
        
    }
    
    public void onEndClick(View view) {
        // [FTDriver] Close USB Serial
        mSerial.end();
        
        btnBegin.setEnabled(true);
        btnRead.setEnabled(false);
        btnWrite.setEnabled(false);
        btnEnd.setEnabled(false);
        
        Toast.makeText(this, "disconnect", Toast.LENGTH_SHORT).show();
    }
}
