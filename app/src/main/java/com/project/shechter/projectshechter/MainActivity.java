package com.project.shechter.projectshechter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;


import java.io.IOException;
import java.io.OutputStream;
import java.lang.String;
import java.util.UUID;

import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Button button_tlat_mode;
    Joystick joystick;
    WebView camera;
    static final String camera_url = "https://www.google.com/"; //TODO change url
    String bluetooth_string;
    byte[] joystick_origin;
    int joystick_x,
        joystick_y;

    //TODO temp
    //ImageView tlatControlsTemp;
    final static String SHECH_MAC = "98:D3:31:F4:06:76";
    //TODO temp

    enum Mode{
        DRIVE,TLAT
    }
    Mode mode;

    BluetoothAdapter m_bluetooth_adapter;
    ConnectedThread bluetooth_thread;

    //TODO IDO EDIT
    //Set<BluetoothDevice> pairedDevices;
    //ArrayAdapter<String> mArrayAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO IDO EDIT BETTER SOLUTION
        //KEEP APP FROM LOCKING WHEN IDLE
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //TODO IDO EDIT BETTER SOLUTION

        bluetooth_string = new String();

        //init button
        button_tlat_mode = (Button) findViewById(R.id.buttonTlatMode);
        //define button listeners
        button_tlat_mode.setOnClickListener(this);

        //init joystick
        joystick_origin = ("H" + String.format("%+04d", 0)
                +   "V" + String.format("%+04d", 0)
                +   "E" + Character.toString(checkNum(0))).getBytes();
        joystick = (Joystick) findViewById(R.id.joystick);
        joystick.setJoystickListener(new JoystickListener() {
            @Override
            public void onDown() {
                bluetooth_thread.write(joystick_origin);

                //TODO IDO EDIT PERHAPS WITH DELAY??
                bluetooth_thread.write(joystick_origin);
            }

            @Override
            public void onDrag(float degrees, float offset) {
                joystick_x = (int) Math.round(Math.cos(degrees * Math.PI / 180f) * offset * 100f);
                joystick_y = (int) Math.round(Math.sin(degrees * Math.PI / 180f) * offset * 100f);

                bluetooth_string =  "H" + String.format("%+04d", joystick_x)
                        +   "V" + String.format("%+04d", joystick_y)
                        +   "E" + Character.toString(checkNum(joystick_x + joystick_y));

                bluetooth_thread.write(bluetooth_string.getBytes());
            }

            @Override
            public void onUp() {
                bluetooth_thread.write(joystick_origin);

                //TODO IDO EDIT PERHAPS WITH DELAY??
                bluetooth_thread.write(joystick_origin);
            }
        });

        // init camera
        camera = (WebView) findViewById(R.id.camera);
        camera.loadUrl(camera_url);
        camera.setWebViewClient(new WebViewClient());

        //init tlat controls
        /*tlatControlsTemp = (ImageView) findViewById(R.id.tlatControlsTemp); //TODO temp
        tlatControlsTemp.setVisibility(View.GONE);*/

        // mode
        mode = Mode.DRIVE;

        //Bluetooth
        m_bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();
        // Check whether BT is supported
        if (m_bluetooth_adapter == null) {
            Toast.makeText(getApplicationContext(), "No Bluetooth on this device.", Toast.LENGTH_SHORT).show();
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


        try {
            bluetooth_thread = new ConnectedThread(
                    m_bluetooth_adapter.getRemoteDevice(SHECH_MAC).createRfcommSocketToServiceRecord(
                            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                    )
            );
        } catch (IOException e) {
            //TODO IDO EDIT
            Toast.makeText(getApplicationContext(), "LOLS IDO LOLOLS", Toast.LENGTH_SHORT).show();
            finish();
            //TODO IDO EDIT
        }

        try {
            bluetooth_thread.connect();
        } catch (IOException e) {
            //TODO IDO EDIT
            Toast.makeText(getApplicationContext(), "Can't connect", Toast.LENGTH_SHORT).show();
            finish();
            //TODO IDO EDIT
        }


        /*
        pairedDevices = m_bluetooth_adapter.getBondedDevices();
        mArrayAdapter = new ArrayAdapter<String>();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }*/

        /*
        //TODO IDO ENABLE NOT USING ADMIN, NEED TO FIX
        Integer resultCode = new Integer(0);
        while (!m_bluetooth_adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            onActivityResult(REQUEST_ENABLE_BT, resultCode, enableBtIntent);
            if(resultCode == RESULT_OK ) {
                break;
            }
        }
        */


    }

    @Override
    public void onClick(View v) {
        //for all buttons
        switch (v.getId()) {
            case R.id.buttonTlatMode:
                //change between drive/tlat modes
                if (mode == Mode.DRIVE) {
                    button_tlat_mode.setText(R.string.drive);
                    mode = Mode.TLAT;
                    joystick.setVisibility(View.GONE);
                    //tlatControlsTemp.setVisibility(View.VISIBLE);
                } else {
                    button_tlat_mode.setText(R.string.t_l_a_t_mode);
                    mode = Mode.DRIVE;
                    joystick.setVisibility(View.VISIBLE);
                    //tlatControlsTemp.setVisibility(View.INVISIBLE);
                }
                break;
            default:

        }
    }

    private char checkNum(int i) {
        int a = i / 100;
        int c = i % 100;
        int b = c / 10;
        c = c % 10;
        int x = a + b + c;

        while (x>9){
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
