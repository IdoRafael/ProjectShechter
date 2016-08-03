package com.project.shechter.projectshechter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.Toast;


import java.io.IOException;
import java.io.OutputStream;
import java.lang.String;
import java.util.UUID;

import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;



public class MainActivity extends AppCompatActivity {

    //TODO IDO REMOVE ALL DEBUGS
    private static final boolean DEBUG = true;
    //TODO IDO REMOVE ALL DEBUGS

    //Widgets ==================
    Joystick joystick;
    WebView camera;
    TabHost tab_host_controls;
    ImageView tlatControlsTemp; //TODO IDO DEFINE HERE
    //==========================


    //Camera & Shech's address ====================================================
    //TODO IDO ad hoc? perhaps let user choose?
    static final String CAMERA_URL = "https://www.google.com/";
    static final String SHECH_MAC = "98:D3:31:F4:06:76";
    //=============================================================================


    //Joystick ================================================================
    static final byte[] JOYSTICK_ORIGIN = ("H" + String.format("%+04d", 0)
            +   "V" + String.format("%+04d", 0)
            +   "E" + Character.toString(checkNum(0))).getBytes();;
    int joystick_x,
        joystick_y;
    long previous_joystick_tick = SystemClock.elapsedRealtime();
    boolean joystick_busy = false;
    static final long JOYSTICK_INTERVAL = 10;
    Runnable write_joystick_origins = new Runnable() {
        @Override
        public void run() {
            joystick_x = 0;
            joystick_y = 0;

            if (DEBUG) {
                Log.d("Joystick", new String(JOYSTICK_ORIGIN));
            } else {
                bluetooth_thread.write(JOYSTICK_ORIGIN);
            }

            previous_joystick_tick = SystemClock.elapsedRealtime();
            joystick_busy = false;
        }
    };
    Handler handler = new Handler();
    //=========================================================================


    //Bluetooth ===========================
    BluetoothAdapter m_bluetooth_adapter;
    ConnectedThread bluetooth_thread;
    //=====================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO IDO EDIT BETTER SOLUTION
        //KEEP APP FROM LOCKING WHEN IDLE
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //TODO IDO EDIT BETTER SOLUTION

       /* //init button
        button_tlat_mode = (Button) findViewById(R.id.buttonTlatMode);
        //define button listeners
        button_tlat_mode.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //change between drive/tlat modes
                if (mode == Mode.DRIVE) {
                    button_tlat_mode.setText(R.string.driving_mode);
                    mode = Mode.TLAT;
                } else {
                    button_tlat_mode.setText(R.string.tlat_mode);
                    mode = Mode.DRIVE;
                }
            }
        });*/

        // init camera
        camera = (WebView) findViewById(R.id.camera);
        camera.loadUrl(CAMERA_URL);
        camera.setWebViewClient(new WebViewClient());

        //init controls
        tab_host_controls = (TabHost) findViewById(R.id.tabHostControls);
        //init joystick
        joystick = (Joystick) findViewById(R.id.joystick);
        joystick.setJoystickListener(new JoystickListener() {
            @Override
            public void onDown() {

            }

            @Override
            public void onDrag(float degrees, float offset) {
                if ( joystick_busy ||
                        SystemClock.elapsedRealtime() - previous_joystick_tick < JOYSTICK_INTERVAL) {
                    return;
                }

                int new_joystick_x = (int) Math.round(Math.cos(degrees * Math.PI / 180f) * offset * 100f),
                        new_joystick_y = (int) Math.round(Math.sin(degrees * Math.PI / 180f) * offset * 100f);

                if (new_joystick_x == joystick_x && new_joystick_y == joystick_y) {
                    return;
                }

                joystick_x = new_joystick_x;
                joystick_y = new_joystick_y;

                String bluetooth_string =  "H" + String.format("%+04d", joystick_x)
                        +   "V" + String.format("%+04d", joystick_y)
                        +   "E" + Character.toString(checkNum(joystick_x + joystick_y));

                if (DEBUG) {
                    Log.d("Joystick",bluetooth_string);
                } else{
                    bluetooth_thread.write(bluetooth_string.getBytes());
                }

                previous_joystick_tick = SystemClock.elapsedRealtime();
            }

            @Override
            public void onUp() {
                if(joystick_busy || (joystick_x == 0 && joystick_y == 0)) {
                    return;
                }
                joystick_busy = true;
                long time_left = JOYSTICK_INTERVAL - SystemClock.elapsedRealtime() + previous_joystick_tick;

                if (time_left <= 0 ){
                    write_joystick_origins.run();
                } else {
                    handler.postDelayed(write_joystick_origins, time_left);
                }
            }
        });
        //init tlat controls
        tlatControlsTemp = (ImageView) findViewById(R.id.tlatControlsTemp); //TODO temp


        //Bluetooth
        m_bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();
        // Check whether BT is supported
        if (m_bluetooth_adapter == null) {
            Toast.makeText(getApplicationContext(), "No Bluetooth on this device.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //enable bluetooth if disabled
        //TODO IDO ENABLE USING ADMIN
        if (!m_bluetooth_adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 0);
        }

        if(!DEBUG) {
            try {
                bluetooth_thread = new ConnectedThread(
                        m_bluetooth_adapter.getRemoteDevice(SHECH_MAC).createRfcommSocketToServiceRecord(
                                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                        )
                );
                bluetooth_thread.connect();
            } catch (IOException e) {
                //TODO IDO EDIT
                Toast.makeText(getApplicationContext(), "Can't connect", Toast.LENGTH_LONG).show();
                finish();
                //TODO IDO EDIT
            }
        }
    }

    static private char checkNum(int i) {
        int a = i / 100;
        int c = i % 100;
        int b = c / 10;
        c = c % 10;
        int x = a + b + c;

        while (x > 9){
            x = x / 10 + x % 10;
        }

        return (char)(x + '0');
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e)  {
                e.printStackTrace();
            }

            mmOutStream = tmpOut;
        }

        public void connect() throws IOException {
            mmSocket.connect();
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
