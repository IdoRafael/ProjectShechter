package com.project.shechter.projectshechter;


import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.app.AppCompatActivity;

import java.util.Set;


public class SettingsActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.dark_theme_key), false)) {
            setTheme(R.style.AppThemeDark);
        } else {
            setTheme(R.style.AppThemeLight);
        }

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragment {

        EditTextPreference bluetooth_mac;
        ListPreference bluetooth_list;
        BluetoothAdapter m_bluetooth_adapter;
        EditTextPreference camera_url;
        EditTextPreference camera_username;
        EditTextPreference camera_password;
        SwitchPreference dark_theme;
        SwitchPreference theme_changed;

        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                            == BluetoothAdapter.STATE_OFF) {
                        bluetoothEnablePopUp();
                    }
                }
            }
        };

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.app_preferences);

            m_bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();

            bluetooth_mac = (EditTextPreference) findPreference(getString(R.string.bluetooth_mac_key));
            bluetooth_mac.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    updateBluetooth(o.toString());
                    return true;
                }
            });
            bluetooth_list = (ListPreference) findPreference(getString(R.string.paired_devices_list_key));
            bluetooth_list.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    bluetooth_mac.setText(o.toString());
                    updateBluetooth(o.toString());
                    return true;
                }
            });

            updateBluetooth(bluetooth_mac.getText());

            Preference.OnPreferenceChangeListener edit_text_preference_change_listener = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    preference.setSummary(o.toString());
                    return true;
                }
            };

            camera_url = (EditTextPreference) findPreference(getString(R.string.camera_url_key));
            camera_url.setSummary(camera_url.getText());
            camera_url.setOnPreferenceChangeListener(edit_text_preference_change_listener);

            camera_username = (EditTextPreference) findPreference(getString(R.string.camera_username_key));
            camera_username.setSummary(camera_username.getText());
            camera_username.setOnPreferenceChangeListener(edit_text_preference_change_listener);

            camera_password = (EditTextPreference) findPreference(getString(R.string.camera_password_key));
            camera_password.setSummary(camera_password.getText());
            camera_password.setOnPreferenceChangeListener(edit_text_preference_change_listener);

            theme_changed = (SwitchPreference) findPreference(getString(R.string.theme_changed_key));
            ((PreferenceCategory) findPreference(getString(R.string.theme_category_key))).removePreference(theme_changed);

            dark_theme = (SwitchPreference) findPreference(getString(R.string.dark_theme_key));
            dark_theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    theme_changed.setChecked(!(theme_changed.isChecked()));
                    getActivity().recreate();
                    return true;
                }
            });
        }

        @Override
        public void onResume() {
            super.onResume();

            if (!m_bluetooth_adapter.isEnabled()) {
                bluetoothEnablePopUp();
            }

            //Handle bluetooth turned off while working
            getActivity().registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        }

        @Override
        public void onPause() {
            super.onPause();

            getActivity().unregisterReceiver(mReceiver);
        }

        void bluetoothEnablePopUp() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.bluetooth_disabled_title);
            builder.setMessage(R.string.bluetooth_disabled_message);
            builder.setCancelable(false);

            // Set up the buttons
            builder.setPositiveButton(R.string.enable, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    m_bluetooth_adapter.enable();

                    while (!m_bluetooth_adapter.isEnabled()) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {

                        }
                    }

                    updateBluetooth(bluetooth_mac.getText());
                }
            });

            builder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getActivity().moveTaskToBack(true);
                }
            });

            builder.show();
        }

        private void updateBluetooth(String address) {
            Set<BluetoothDevice> pairedDevices = m_bluetooth_adapter.getBondedDevices();
            String deviceName = getString(R.string.no_device);
            String deviceAddress = getString(R.string.invalid_bt_mac);

            CharSequence[] entries = new CharSequence[pairedDevices.size() + 1];
            CharSequence[] entryValues = new CharSequence[pairedDevices.size() + 1];
            entries[0] = deviceName;
            entryValues[0] = deviceAddress;
            if (pairedDevices.size() > 0) {
                int i = 1;
                for (BluetoothDevice device : pairedDevices) {
                    entries[i] = device.getName();
                    entryValues[i] = device.getAddress();
                    i++;
                    if (address.equalsIgnoreCase(device.getAddress())) {
                        deviceName = device.getName();
                        deviceAddress = address;
                    }
                }
            }
            bluetooth_list.setEntries(entries);
            bluetooth_list.setEntryValues(entryValues);

            bluetooth_list.setValue(deviceAddress);
            bluetooth_mac.setSummary(deviceName + " - " + deviceAddress);
        }
    }
}
