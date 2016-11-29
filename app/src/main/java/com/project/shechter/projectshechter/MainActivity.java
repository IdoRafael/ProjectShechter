package com.project.shechter.projectshechter;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.HttpAuthHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.Toast;

import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;


public class MainActivity extends AppCompatActivity {

    //TODO IDO REMOVE ALL DEBUGS
    private static final boolean DEBUG = true;
    //TODO IDO REMOVE ALL DEBUGS


    //Camera ==================================================
    static String camera_url = "http://192.168.43.74:81";
    static String camera_username = "admin";
    static String camera_password = "";
    static final int CAMERA_BUTTON_ALPHA = 150;
    static final int CAMERA_BUTTON_INTERVAL = 100;
    WebView camera;
    ImageButton camera_button_refresh;
    ImageButton camera_button_up;
    ImageButton camera_button_down;
    ImageButton camera_button_left;
    ImageButton camera_button_right;
    //Camera ==================================================


    //Preferences ========================================
    Button settings_button;
    SharedPreferences settings;
    static final int RESULT_SETTINGS = 1;
    //Preferences ========================================


    //Bluetooth ========================================
    static final String POPUP_IS_ON = "popupIsOnState";
    static String shech_mac = "98:D3:31:F4:06:76";
    BluetoothAdapter m_bluetooth_adapter;
    ConnectedThread bluetooth_thread;
    boolean popup_is_on = false;
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                        == BluetoothAdapter.STATE_OFF) {
                    if(bluetooth_thread != null && bluetooth_thread.isConnected()){
                        bluetooth_thread.cancel();
                    }
                    connectPopUp();
                }
            }
        }
    };
    //Bluetooth ========================================


    //Vibrator =============================
    static final long VIBRATE_DURATION = 55;
    Vibrator vibrator;
    boolean has_vibrator;
    //Vibrator =============================


    //Controls ===============
    static final long BLUETOOTH_INTERVAL = 70;
    private static final String STATE_TAB_HOST = "tabHostState";
    TabHost tab_host_controls;

    //Joystick ===========================================================
    static final byte[] JOYSTICK_ORIGIN = ("H" + String.format("%+04d", 0)
            + "V" + String.format("%+04d", 0)
            + "E" + Character.toString(checkNumJoystick(0))).getBytes();
    Joystick joystick;
    int joystick_x,
            joystick_y;
    long previous_joystick_tick;
    boolean joystick_busy;
    Runnable write_joystick_origins;
    Handler joystick_handler;
    //Joystick ===========================================================

    //Tlat ============================================================================
    static final String TLAT_ORIGIN_STRING = "+6";
    static final int TLAT_PLUS_MINUS_INDEX = 0;
    static final int TLAT_DRIVER_INDEX = 1;
    static final int TLAT_BUTTON_AMOUNT = 6;
    static final byte[] TLAT_ORIGIN = ("T" + TLAT_ORIGIN_STRING
            + "E" + Character.toString(checkNumTlat(TLAT_ORIGIN_STRING))).getBytes();
    ImageView tlat_controls;
    ImageView tlat_pressed_controls;
    String tlat_string;
    boolean tlat_busy;
    boolean tlat_pressed;
    int tlat_pressed_minus_id[];
    int tlat_pressed_plus_id[];
    long previous_tlat_tick;
    Runnable write_tlat_origins;
    Handler tlat_handler;
    Bitmap tlat_bitmap;
    boolean tlat_initiated = false;
    //Tlat ============================================================================
    //Controls ===============

    static private char checkNumJoystick(int i) {
        int a = i / 100;
        int c = i % 100;
        int b = c / 10;
        c = c % 10;
        int x = a + b + c;

        while (x > 9) {
            x = x / 10 + x % 10;
        }

        return (char) (x + '0');
    }

    static private char checkNumTlat(String s) {
        int x = ((Character.getNumericValue(s.charAt(TLAT_DRIVER_INDEX)) + 1) *
                (s.charAt(TLAT_PLUS_MINUS_INDEX) == '+' ? 1 : 2));

        return (char) (x + '0');
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        init_preferences();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //Keep application from locking when idle
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Hide Action Bar
        getSupportActionBar().hide();

        init(savedInstanceState);

        updateFromPreferences(false);

        if (savedInstanceState != null) {
            popup_is_on = savedInstanceState.getBoolean(POPUP_IS_ON);
            if (popup_is_on) {
                connectPopUp();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Handle bluetooth turned off while working
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        if (!popup_is_on &&
                !settings.getBoolean(getString(R.string.theme_changed_key), false) &&
                !connect()
                ) {
            connectPopUp();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            if (!DEBUG) {
                if (bluetooth_thread != null && bluetooth_thread.isConnected()) {
                    bluetooth_thread.cancel();
                }
            }
        } catch (Exception e) {

        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit the application?");

        // Set up the buttons
        builder.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MainActivity.super.onBackPressed();
            }
        });
        builder.setNegativeButton("Stay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void init(Bundle savedInstanceState) {
        init_bluetooth();
        init_vibrator();
        init_controls(savedInstanceState);
        init_joystick();
        init_tlat();
        init_camera();
    }

    private void init_preferences() {
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (settings.getBoolean(getString(R.string.dark_theme_key), false)) {
            setTheme(R.style.AppThemeDark);
        } else {
            setTheme(R.style.AppThemeLight);
        }
        settings.edit().putBoolean(getString(R.string.theme_changed_key), false).apply();
    }

    private void init_bluetooth() {
        m_bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();
        // Check whether BT is supported
        if (m_bluetooth_adapter == null) {
            Toast.makeText(getApplicationContext(), "No Bluetooth on this device.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void init_vibrator() {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        has_vibrator = (vibrator != null) && vibrator.hasVibrator();
    }

    private void init_camera() {
        camera = (WebView) findViewById(R.id.camera);

        camera.getSettings().setJavaScriptEnabled(true);

        //disable touch events
        camera.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        //disable zoom
        camera.getSettings().setBuiltInZoomControls(false);
        camera.getSettings().setDisplayZoomControls(false);

        //disable scrolling
        camera.setHorizontalScrollBarEnabled(false);
        camera.setVerticalScrollBarEnabled(false);

        camera.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        camera.setScrollbarFadingEnabled(true);

        //prevent picture from getting cut off by webview
        camera.getSettings().setLoadWithOverviewMode(true);
        camera.getSettings().setUseWideViewPort(true);

        camera.setBackgroundColor(Color.TRANSPARENT);

        //init camera buttons
        camera_button_refresh = (ImageButton) findViewById(R.id.buttonRefresh);
        camera_button_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            updateFromPreferences(false);
            }
        });
        camera_button_refresh.getBackground().setAlpha(CAMERA_BUTTON_ALPHA);

        camera_button_up = (ImageButton) findViewById(R.id.cameraButtonUp);
        camera_button_up.setOnTouchListener(new RepeatListener(
                CAMERA_BUTTON_INTERVAL,
                CAMERA_BUTTON_INTERVAL,
                new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                camera.evaluateJavascript("$($('iframe').contents().find('iframe'))[0].contentWindow.OnPtzMouseUp()",null);
            }
        }));
        camera_button_up.getBackground().setAlpha(CAMERA_BUTTON_ALPHA);

        camera_button_down = (ImageButton) findViewById(R.id.cameraButtonDown);
        camera_button_down.setOnTouchListener(new RepeatListener(
                CAMERA_BUTTON_INTERVAL,
                CAMERA_BUTTON_INTERVAL,
                new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                camera.evaluateJavascript("$($('iframe').contents().find('iframe'))[0].contentWindow.OnPtzMouseDown()",null);
            }
        }));
        camera_button_down.getBackground().setAlpha(CAMERA_BUTTON_ALPHA);

        camera_button_left = (ImageButton) findViewById(R.id.cameraButtonLeft);
        camera_button_left.setOnTouchListener(new RepeatListener(
                CAMERA_BUTTON_INTERVAL,
                CAMERA_BUTTON_INTERVAL,
                new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                camera.evaluateJavascript("$($('iframe').contents().find('iframe'))[0].contentWindow.OnPtzMouseLeft()",null);
            }
        }));
        camera_button_left.getBackground().setAlpha(CAMERA_BUTTON_ALPHA);

        camera_button_right = (ImageButton) findViewById(R.id.cameraButtonRight);
        camera_button_right.setOnTouchListener(new RepeatListener(
                CAMERA_BUTTON_INTERVAL,
                CAMERA_BUTTON_INTERVAL,
                new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                camera.evaluateJavascript("$($('iframe').contents().find('iframe'))[0].contentWindow.OnPtzMouseRight()",null);
            }
        }));
        camera_button_right.getBackground().setAlpha(CAMERA_BUTTON_ALPHA);
    }

    private void init_controls(Bundle savedInstanceState) {
        //init tab host for controls
        tab_host_controls = (TabHost)findViewById(R.id.tabHostControls);
        tab_host_controls.setup();
        //Tab 1
        TabHost.TabSpec spec = tab_host_controls.newTabSpec("Driving Mode");
        spec.setContent(R.id.joystickLayout);
        spec.setIndicator("Driving Mode");
        tab_host_controls.addTab(spec);
        //Tab 2
        spec = tab_host_controls.newTabSpec("Tlat Mode");
        spec.setContent(R.id.tlatLayout);
        spec.setIndicator("Tlat Mode");
        tab_host_controls.addTab(spec);

        if (savedInstanceState != null) {
            tab_host_controls.setCurrentTab(savedInstanceState.getInt(STATE_TAB_HOST));
        }

        settings_button = (Button) findViewById(R.id.buttonSettings);
        settings_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), RESULT_SETTINGS);
            }
        });
    }

    private void init_joystick() {
        previous_joystick_tick = SystemClock.elapsedRealtime();
        joystick_busy = false;
        write_joystick_origins = new Runnable() {
            @Override
            public void run() {
                joystick_x = 0;
                joystick_y = 0;
                try {
                    if (DEBUG) {
                        Log.d("Joystick", new String(JOYSTICK_ORIGIN));
                    } else {
                        bluetooth_thread.write(JOYSTICK_ORIGIN);
                    }
                } catch (Exception e) {
                    if (bluetooth_thread != null) {
                        bluetooth_thread.cancel();
                    }
                    connectPopUp();
                } finally {
                    previous_joystick_tick = SystemClock.elapsedRealtime();
                    joystick_busy = false;
                }
            }
        };
        joystick_handler = new Handler();

        joystick = (Joystick) findViewById(R.id.joystick);
        joystick.setJoystickListener(new JoystickListener() {
            @Override
            public void onDown() {

            }

            @Override
            public void onDrag(float degrees, float offset) {
                if ( joystick_busy ||
                        SystemClock.elapsedRealtime() - previous_joystick_tick < BLUETOOTH_INTERVAL) {
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
                        +   "E" + Character.toString(checkNumJoystick(joystick_x + joystick_y));

                try {
                    if (DEBUG) {
                        Log.d("Joystick", bluetooth_string);
                    } else {
                        bluetooth_thread.write(bluetooth_string.getBytes());
                    }
                } catch (Exception e) {
                    if (bluetooth_thread != null) {
                        bluetooth_thread.cancel();
                    }
                    connectPopUp();
                } finally {
                    previous_joystick_tick = SystemClock.elapsedRealtime();
                }
            }

            @Override
            public void onUp() {
                if(joystick_busy || (joystick_x == 0 && joystick_y == 0)) {
                    return;
                }
                joystick_busy = true;
                long time_left = BLUETOOTH_INTERVAL - SystemClock.elapsedRealtime() + previous_joystick_tick;

                if (time_left <= 0 ){
                    write_joystick_origins.run();
                } else {
                    joystick_handler.postDelayed(write_joystick_origins, time_left);
                }
            }
        });
    }

    private void init_tlat() {
        tlat_string = TLAT_ORIGIN_STRING;
        tlat_busy = false;
        tlat_pressed = false;
        tlat_pressed_minus_id = new int[TLAT_BUTTON_AMOUNT];
        tlat_pressed_plus_id = new int[TLAT_BUTTON_AMOUNT];
        previous_tlat_tick = SystemClock.elapsedRealtime();
        write_tlat_origins = new Runnable() {
            @Override
            public void run() {
                try {
                    if (DEBUG) {
                        Log.d("Tlat", new String(TLAT_ORIGIN));
                    } else {
                        bluetooth_thread.write(TLAT_ORIGIN);
                    }
                } catch (Exception e) {
                    if (bluetooth_thread != null) {
                        bluetooth_thread.cancel();
                    }
                    connectPopUp();
                } finally {
                    previous_tlat_tick = SystemClock.elapsedRealtime();

                    if (tlat_string.charAt(TLAT_PLUS_MINUS_INDEX) == '+') {
                        tlat_pressed_controls.setImageResource(tlat_pressed_plus_id[Character.getNumericValue(tlat_string.charAt(TLAT_DRIVER_INDEX))]);
                    } else {
                        tlat_pressed_controls.setImageResource(tlat_pressed_minus_id[Character.getNumericValue(tlat_string.charAt(TLAT_DRIVER_INDEX))]);
                    }

                    tlat_pressed_controls.setVisibility(View.INVISIBLE);
                    tlat_string = TLAT_ORIGIN_STRING;

                    tlat_pressed = false;
                    tlat_busy = false;
                }
            }
        };
        tlat_handler = new Handler();
        tlat_controls = (ImageView) findViewById(R.id.tlat_controls_invisible_front_view);
        tlat_pressed_controls = (ImageView) findViewById(R.id.tlat_pressed_controls_view);

        tlat_pressed_minus_id[0] = R.drawable.tlat_pressed_minus0;
        tlat_pressed_minus_id[1] = R.drawable.tlat_pressed_minus1;
        tlat_pressed_minus_id[2] = R.drawable.tlat_pressed_minus2;
        tlat_pressed_minus_id[3] = R.drawable.tlat_pressed_minus3;
        tlat_pressed_minus_id[4] = R.drawable.tlat_pressed_minus4;
        tlat_pressed_minus_id[5] = R.drawable.tlat_pressed_minus5;

        tlat_pressed_plus_id[0] = R.drawable.tlat_pressed_plus0;
        tlat_pressed_plus_id[1] = R.drawable.tlat_pressed_plus1;
        tlat_pressed_plus_id[2] = R.drawable.tlat_pressed_plus2;
        tlat_pressed_plus_id[3] = R.drawable.tlat_pressed_plus3;
        tlat_pressed_plus_id[4] = R.drawable.tlat_pressed_plus4;
        tlat_pressed_plus_id[5] = R.drawable.tlat_pressed_plus5;

        tlat_controls.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int action = event.getAction();
                final int evX = (int) event.getX();
                final int evY = (int) event.getY();

                if(action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_DOWN) {
                    if ( tlat_busy ||
                            SystemClock.elapsedRealtime() - previous_tlat_tick < BLUETOOTH_INTERVAL) {
                        return true;
                    }

                    String new_tlat_string = calculateTlatString(evX, evY);

                    if (tlat_pressed && new_tlat_string.equals(tlat_string)) {
                        return true;
                    } else if (tlat_pressed) {
                        onTlatUp();
                        return true;

                    } else if (action == MotionEvent.ACTION_MOVE || new_tlat_string.equals("")) {
                        return true;
                    }

                    tlat_string = new_tlat_string;

                    if(tlat_string.charAt(TLAT_PLUS_MINUS_INDEX) == '+') {
                        tlat_pressed_controls.setImageResource(tlat_pressed_plus_id[Character.getNumericValue(tlat_string.charAt(TLAT_DRIVER_INDEX))]);
                    } else {
                        tlat_pressed_controls.setImageResource(tlat_pressed_minus_id[Character.getNumericValue(tlat_string.charAt(TLAT_DRIVER_INDEX))]);
                    }
                    tlat_pressed_controls.setVisibility(View.VISIBLE);

                    String bluetooth_string =  "T" + tlat_string
                            +   "E" + Character.toString(checkNumTlat(tlat_string));

                    try {
                        if (DEBUG) {
                            Log.d("Tlat", bluetooth_string);
                        } else {
                            bluetooth_thread.write(bluetooth_string.getBytes());
                        }
                    } catch (Exception e) {
                        if (bluetooth_thread != null) {
                            bluetooth_thread.cancel();
                        }
                        connectPopUp();
                    } finally {
                        previous_tlat_tick = SystemClock.elapsedRealtime();
                        vibrate();
                        tlat_pressed = true;
                    }
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    if(onTlatUp()) {
                        return true;
                    }
                } else {
                    return false;
                }

                return true;
            }
        });
    }

    private boolean onTlatUp() {
        if(tlat_busy || !tlat_pressed) {
            return true;
        }
        tlat_busy = true;
        long time_left = BLUETOOTH_INTERVAL - SystemClock.elapsedRealtime() + previous_tlat_tick;

        if (time_left <= 0 ){
            write_tlat_origins.run();
        } else {
            tlat_handler.postDelayed(write_tlat_origins, time_left);
        }
        return false;
    }

    public int getHotspotColor (int x, int y) {
        if(!tlat_initiated) {
            tlat_initiated = true;

            tlat_controls.setDrawingCacheEnabled(true);
            try {
                tlat_bitmap = Bitmap.createBitmap(tlat_controls.getDrawingCache());
            } catch (Throwable e) {
                Toast.makeText(getApplicationContext(), "Failed to load bitmap. No memory.", Toast.LENGTH_LONG).show();
                finish();
            }
            tlat_controls.setDrawingCacheEnabled(false);
        }

        return tlat_bitmap.getPixel(x, y);
    }

    private String calculateTlatString(int tlat_x, int tlat_y) {
        StringBuilder s = new StringBuilder(TLAT_ORIGIN_STRING);

        int touch_color;
        try {
            touch_color = getHotspotColor(tlat_x, tlat_y);
        } catch (IllegalArgumentException e) {
            return "";
        }
        int tolerance = 10;

        if (close_match(touch_color, Color.parseColor("#f23050"), tolerance)) {
            s.setCharAt(TLAT_PLUS_MINUS_INDEX,'-');
            s.setCharAt(TLAT_DRIVER_INDEX,'0');
        } else if (close_match(touch_color, Color.parseColor("#6230f2"), tolerance)) {
            s.setCharAt(TLAT_PLUS_MINUS_INDEX,'+');
            s.setCharAt(TLAT_DRIVER_INDEX,'0');
        } else if (close_match(touch_color, Color.parseColor("#34093c"), tolerance)) {
            s.setCharAt(TLAT_PLUS_MINUS_INDEX,'-');
            s.setCharAt(TLAT_DRIVER_INDEX,'1');
        } else if (close_match(touch_color, Color.parseColor("#309ef2"), tolerance)) {
            s.setCharAt(TLAT_PLUS_MINUS_INDEX,'+');
            s.setCharAt(TLAT_DRIVER_INDEX,'1');
        } else if (close_match(touch_color, Color.parseColor("#30f28b"), tolerance)) {
            s.setCharAt(TLAT_PLUS_MINUS_INDEX,'-');
            s.setCharAt(TLAT_DRIVER_INDEX,'2');
        } else if (close_match(touch_color, Color.parseColor("#226b14"), tolerance)) {
            s.setCharAt(TLAT_PLUS_MINUS_INDEX,'+');
            s.setCharAt(TLAT_DRIVER_INDEX,'2');
        } else if (close_match(touch_color, Color.parseColor("#e57549"), tolerance)) {
            s.setCharAt(TLAT_PLUS_MINUS_INDEX,'-');
            s.setCharAt(TLAT_DRIVER_INDEX,'3');
        } else if (close_match(touch_color, Color.parseColor("#98e11a"), tolerance)) {
            s.setCharAt(TLAT_PLUS_MINUS_INDEX,'+');
            s.setCharAt(TLAT_DRIVER_INDEX,'3');
        } else if (close_match(touch_color, Color.parseColor("#f1e01e"), tolerance)) {
            s.setCharAt(TLAT_PLUS_MINUS_INDEX,'-');
            s.setCharAt(TLAT_DRIVER_INDEX,'4');
        } else if (close_match(touch_color, Color.parseColor("#d87f17"), tolerance)) {
            s.setCharAt(TLAT_PLUS_MINUS_INDEX,'+');
            s.setCharAt(TLAT_DRIVER_INDEX,'4');
        } else if (close_match(touch_color, Color.parseColor("#b20606"), tolerance)) {
            s.setCharAt(TLAT_PLUS_MINUS_INDEX,'-');
            s.setCharAt(TLAT_DRIVER_INDEX,'5');
        } else if (close_match(touch_color, Color.parseColor("#cc87d9"), tolerance)) {
            s.setCharAt(TLAT_PLUS_MINUS_INDEX,'+');
            s.setCharAt(TLAT_DRIVER_INDEX,'5');
        } else {
            return "";
        }

        return s.toString();
    }

    private boolean close_match (int color1, int color2, int tolerance) {
        return !((Math.abs(Color.red(color1) - Color.red(color2)) > tolerance) ||
                (Math.abs(Color.green(color1) - Color.green(color2)) > tolerance) ||
                (Math.abs(Color.blue(color1) - Color.blue(color2)) > tolerance));

    }

    private void connectPopUp() {
        popup_is_on = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.connection_error_title);
        builder.setMessage(R.string.connection_error_message);
        builder.setCancelable(false);

        // Set up the buttons
        builder.setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                popup_is_on = false;
                if (!connect()){
                    connectPopUp();
                }
            }
        });

        builder.setNeutralButton(R.string.settings_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                popup_is_on = false;
                startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), RESULT_SETTINGS);
            }

        });

        builder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                popup_is_on = false;
                finish();
            }
        });

        builder.show();
    }

    private boolean connect() {
        //enable bluetooth if disabled
        if (!m_bluetooth_adapter.isEnabled()) {
            m_bluetooth_adapter.enable();
        }

        while(!m_bluetooth_adapter.isEnabled()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {

            }
        }

        String temp_mac = settings.getString(getString(R.string.bluetooth_mac_key), getString(R.string.bluetooth_mac_default));
        boolean ip_changed = !temp_mac.equals(shech_mac);
        shech_mac = temp_mac;

        if(!DEBUG) {
            try {
                if(bluetooth_thread == null || !bluetooth_thread.isConnected() || ip_changed) {
                    if(bluetooth_thread != null && bluetooth_thread.isConnected() && ip_changed){
                        bluetooth_thread.cancel();
                    }
                    bluetooth_thread = new ConnectedThread(
                            m_bluetooth_adapter.getRemoteDevice(
                                    shech_mac
                            ).createRfcommSocketToServiceRecord(
                                    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                            )
                    );
                    bluetooth_thread.connect();
                }
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    private void vibrate() {
        if(has_vibrator) {
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_TAB_HOST, tab_host_controls.getCurrentTab());
        outState.putBoolean(POPUP_IS_ON, popup_is_on);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SETTINGS:
                updateFromPreferences(true);
                //if changed theme in settings recreate activity
                if (settings.getBoolean(getString(R.string.theme_changed_key), false)) {
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }
                break;
        }
    }

    private void updateFromPreferences(boolean backFromSettingsPage) {
        //set zoom according to current orientation
        try {
            if (getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT) {
                camera.setInitialScale(
                        Integer.parseInt(
                                settings.getString(
                                        getString(R.string.camera_zoom_portrait_key),
                                        getString(R.string.camera_zoom_default)
                                )
                        )
                );
            } else {
                camera.setInitialScale(
                        Integer.parseInt(
                                settings.getString(
                                        getString(R.string.camera_zoom_landscape_key),
                                        getString(R.string.camera_zoom_default)
                                )
                        )
                );
            }
        } catch (Exception e) {
            camera.setInitialScale(Integer.parseInt(getString(R.string.camera_zoom_default)));
        }
        final String url_temp = settings.getString(getString(R.string.camera_url_key), getString(R.string.camera_url_default));
        final String username_temp = settings.getString(
                getString(R.string.camera_username_key), getString(R.string.camera_username_default)
        );
        final String password_temp = settings.getString(
                getString(R.string.camera_password_key), getString(R.string.camera_password_default)
        );

        camera.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                handler.proceed(username_temp,password_temp);
            }
        });

        //reload camera only if url or username or password changed,
        // or we're not back from settings (therefore in onCreate)
        if(!backFromSettingsPage || !url_temp.equals(camera_url) ||
                !username_temp.equals(camera_username) || !password_temp.equals(camera_password)) {
            String html = "<!DOCTYPE html><html>" +
                "<head><script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min.js\"></script>" +
                "<script>" +
                "function goToCameraPage() {" +
                    "$($('iframe').contents().find('iframe'))[0].contentWindow.signin_snapmotion();" +
                "}" +
                "</script>" +
                "</head>" +
                "<body onload=\"goToCameraPage()\">" +
                "<iframe src= \"" + url_temp
                + "\" style=\"position:fixed; top:0px; left:0px; bottom:0px; right:0px; width:100%; height:100%; border:none; margin:0; padding:0; overflow:hidden; z-index:999999;\"></iframe></body></html>";
            camera.loadDataWithBaseURL(url_temp, html, "text/html", "UTF-8", null);
        }
        camera_url = url_temp;
        camera_username = username_temp;
        camera_password = password_temp;

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) throws IOException {
            mmSocket = socket;
            mmOutStream = socket.getOutputStream();
        }

        public void connect() throws IOException {
            mmSocket.connect();
        }

        public boolean isConnected() {
            return mmSocket.isConnected();
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Bluetooth write failed. Try to reconnect.", Toast.LENGTH_LONG).show();
                connectPopUp();
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Failed to close Bluetooth connection.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
