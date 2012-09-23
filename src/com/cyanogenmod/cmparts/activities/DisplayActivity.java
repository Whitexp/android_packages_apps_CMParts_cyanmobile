/*
 * Copyright (C) 2011 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.cmparts.activities;

import com.cyanogenmod.cmparts.R;

import android.app.ActivityManagerNative;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Build;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.IPackageDataObserver;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.widget.EditText;
import android.text.Editable;
import android.text.Spannable;
import android.view.View;
import android.view.LayoutInflater;
import android.view.IWindowManager;
import android.util.Log;
import com.cyanogenmod.cmparts.utils.CMDProcessor;
import com.cyanogenmod.cmparts.utils.CMDProcessor.CommandResult;
import com.cyanogenmod.cmparts.utils.Helpers;
import com.android.internal.app.ShutdownThread;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class DisplayActivity extends PreferenceActivity implements OnPreferenceChangeListener {

    /* Preference Screens */

    public static final String TAG = "DisplayActivity";

    private static final String PREF_SQUADZONE = "squadkeys";

    private static final String BACKLIGHT_SETTINGS = "backlight_settings";

    private static final String GENERAL_CATEGORY = "general_category";

    private static final String ELECTRON_BEAM_ANIMATION_ON = "electron_beam_animation_on";

    private static final String ELECTRON_BEAM_ANIMATION_OFF = "electron_beam_animation_off";

    private static final String ELECTRON_BEAM_ANIMATION_ON_DELAY = "electron_beam_animation_on_delay";

    private static final String ROTATION_ANIMATION_PREF = "pref_rotation_animation";

    private static final String LCD_DENSITY_PREF = "lcd_density";

    private static final String REBOOT_PREF = "reboot";

    private static final String CLEARMARKET_PREF = "clear_market_data";

    private static final String USE_BRAVIA_PREF = "pref_use_bravia";

    private static final String USE_BRAVIA_PERSIST_PROP = "persist.service.swiqi.enable";
    
    private static final String USE_BRAVIA_DEFAULT = "0";

    private static final String WINDOW_ANIMATIONS_PREF = "window_animations";

    private static final String TRANSITION_ANIMATIONS_PREF = "transition_animations";

    private PreferenceScreen mBacklightScreen;

    /* Other */
    private static final String ROTATION_ANIMATION_PROP = "persist.sys.rotationanimation";

    private static final String ROTATION_0_PREF = "pref_rotation_0";
    private static final String ROTATION_90_PREF = "pref_rotation_90";
    private static final String ROTATION_180_PREF = "pref_rotation_180";
    private static final String ROTATION_270_PREF = "pref_rotation_270";

    private static final int ROTATION_0_MODE = 8;
    private static final int ROTATION_90_MODE = 1;
    private static final int ROTATION_180_MODE = 2;
    private static final int ROTATION_270_MODE = 4;

    private static final int DIALOG_DENSITY = 101;
    private static final int DIALOG_WARN_DENSITY = 102;
    private static final int MSG_DATA_CLEARED = 500;

    private CheckBoxPreference mElectronBeamAnimationOn;
    private CheckBoxPreference mElectronBeamAnimationOff;
    private ListPreference mElectronBeamAnimationOnPref;
    private CheckBoxPreference mRotationAnimationPref;

    private CheckBoxPreference mUseBraviaPref;

    private ListPreference mWindowAnimationsPref;

    private ListPreference mTransitionAnimationsPref;

    private IWindowManager mWindowManager;

    int newDensityValue;
    private Preference mReboot;
    private Preference mClearMarketData;
    private Preference mSquadzone;

    private ListPreference mLcdDensity;
    private AlertDialog alertDialog;

    private CheckBoxPreference mRotation0Pref;
    private CheckBoxPreference mRotation90Pref;
    private CheckBoxPreference mRotation180Pref;
    private CheckBoxPreference mRotation270Pref;

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_DATA_CLEARED:
                    mClearMarketData.setSummary(R.string.clear_market_data_cleared);
                    break;
            }

        };
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));

        setTitle(R.string.display_settings_title_subhead);
        addPreferencesFromResource(R.xml.display_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        mSquadzone = (Preference) prefSet.findPreference(PREF_SQUADZONE);
        mSquadzone.setSummary("CyanMobile");

        /* Preference Screens */
        mBacklightScreen = (PreferenceScreen) prefSet.findPreference(BACKLIGHT_SETTINGS);
        // No reason to show backlight if no light sensor on device
        if (((SensorManager) getSystemService(SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_LIGHT) == null) {
            ((PreferenceCategory) prefSet.findPreference(GENERAL_CATEGORY))
                    .removePreference(mBacklightScreen);
        }
        // try to activated backlight control

        mWindowAnimationsPref = (ListPreference) prefSet.findPreference(WINDOW_ANIMATIONS_PREF);
        mWindowAnimationsPref.setOnPreferenceChangeListener(this);
        mTransitionAnimationsPref = (ListPreference) prefSet.findPreference(TRANSITION_ANIMATIONS_PREF);
        mTransitionAnimationsPref.setOnPreferenceChangeListener(this);

        /* Electron Beam control */
        mElectronBeamAnimationOn = (CheckBoxPreference)prefSet.findPreference(ELECTRON_BEAM_ANIMATION_ON);
        mElectronBeamAnimationOff = (CheckBoxPreference)prefSet.findPreference(ELECTRON_BEAM_ANIMATION_OFF);
        int electronBeamAnimationOnPref = Settings.System.getInt(getContentResolver(),
                Settings.System.ELECTRON_BEAM_ANIMATION_ON_DELAY, 100);
        mElectronBeamAnimationOnPref = (ListPreference) prefSet.findPreference(ELECTRON_BEAM_ANIMATION_ON_DELAY);
        if (getResources().getBoolean(com.android.internal.R.bool.config_enableScreenAnimation)) {
            mElectronBeamAnimationOn.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.ELECTRON_BEAM_ANIMATION_ON,
                    getResources().getBoolean(com.android.internal.R.bool.config_enableScreenOnAnimation) ? 1 : 0) == 1);
            mElectronBeamAnimationOff.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.ELECTRON_BEAM_ANIMATION_OFF,
                    getResources().getBoolean(com.android.internal.R.bool.config_enableScreenOffAnimation) ? 1 : 0) == 1);
            mElectronBeamAnimationOnPref.setValue(String.valueOf(electronBeamAnimationOnPref));
            mElectronBeamAnimationOnPref.setOnPreferenceChangeListener(this);
        } else {
            /* Hide Electron Beam controls if disabled */
            ((PreferenceCategory) prefSet.findPreference(GENERAL_CATEGORY))
                .removePreference(mElectronBeamAnimationOn);
            ((PreferenceCategory) prefSet.findPreference(GENERAL_CATEGORY))
                .removePreference(mElectronBeamAnimationOnPref);
            ((PreferenceCategory) prefSet.findPreference(GENERAL_CATEGORY))
                .removePreference(mElectronBeamAnimationOff);
        }

        /* LCD density */
        mLcdDensity = (ListPreference)prefSet.findPreference(LCD_DENSITY_PREF);
        String currentProperty = SystemProperties.get("ro.sf.lcd_density");
        if (currentProperty == null)
            currentProperty = "0";
        try {
            newDensityValue = Integer.parseInt(currentProperty);
        } catch (Exception e) {
            getPreferenceScreen().removePreference(mLcdDensity);
        }
        mLcdDensity.setSummary(currentProperty);
        mLcdDensity.setOnPreferenceChangeListener(this);
        mLcdDensity.setValue(newDensityValue + "");

        mUseBraviaPref = (CheckBoxPreference) prefSet.findPreference(USE_BRAVIA_PREF);
        String useBravia = SystemProperties.get(USE_BRAVIA_PERSIST_PROP, USE_BRAVIA_DEFAULT);
        mUseBraviaPref.setChecked("1".equals(useBravia));

        /* Rotation animation */
        mRotationAnimationPref = (CheckBoxPreference) prefSet.findPreference(ROTATION_ANIMATION_PREF);
        mRotationAnimationPref.setChecked(SystemProperties.getBoolean(ROTATION_ANIMATION_PROP, true));

        mReboot = (Preference) prefSet.findPreference(REBOOT_PREF);
        mClearMarketData = (Preference) prefSet.findPreference(CLEARMARKET_PREF);
        mClearMarketData.setSummary("");

        /* Rotation */
        mRotation0Pref = (CheckBoxPreference) prefSet.findPreference(ROTATION_0_PREF);
        mRotation90Pref = (CheckBoxPreference) prefSet.findPreference(ROTATION_90_PREF);
        mRotation180Pref = (CheckBoxPreference) prefSet.findPreference(ROTATION_180_PREF);
        mRotation270Pref = (CheckBoxPreference) prefSet.findPreference(ROTATION_270_PREF);
        int mode = Settings.System.getInt(getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION_MODE,
                        ROTATION_0_MODE|ROTATION_90_MODE|ROTATION_270_MODE);
        mRotation0Pref.setChecked((mode & ROTATION_0_MODE) != 0);
        mRotation90Pref.setChecked((mode & ROTATION_90_MODE) != 0);
        mRotation180Pref.setChecked((mode & ROTATION_180_MODE) != 0);
        mRotation270Pref.setChecked((mode & ROTATION_270_MODE) != 0);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        /* Preference Screens */
        if (preference == mBacklightScreen) {
            startActivity(mBacklightScreen.getIntent());
        }

        if (preference == mElectronBeamAnimationOn) {
            value = mElectronBeamAnimationOn.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ELECTRON_BEAM_ANIMATION_ON, value ? 1 : 0);
        }

        if (preference == mUseBraviaPref) {
            value = mUseBraviaPref.isChecked();
            SystemProperties.set(USE_BRAVIA_PERSIST_PROP, value ? "1" : "0");
            return true;
        }

        if (preference == mReboot) {
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            pm.reboot("Resetting density");
            return true;
        }

        if (preference == mClearMarketData) {
            new ClearMarketDataTask().execute("");
            return true;
        }

        if (preference == mElectronBeamAnimationOff) {
            value = mElectronBeamAnimationOff.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ELECTRON_BEAM_ANIMATION_OFF, value ? 1 : 0);
        }

        if (preference == mRotationAnimationPref) {
            SystemProperties.set(ROTATION_ANIMATION_PROP,
                    mRotationAnimationPref.isChecked() ? "1" : "0");
            return true;
        }

        if (preference == mRotation0Pref ||
            preference == mRotation90Pref ||
            preference == mRotation180Pref ||
            preference == mRotation270Pref) {
            int mode = 0;
            if (mRotation0Pref.isChecked()) mode |= ROTATION_0_MODE;
            if (mRotation90Pref.isChecked()) mode |= ROTATION_90_MODE;
            if (mRotation180Pref.isChecked()) mode |= ROTATION_180_MODE;
            if (mRotation270Pref.isChecked()) mode |= ROTATION_270_MODE;
            if (mode == 0) {
                mode |= ROTATION_0_MODE;
                mRotation0Pref.setChecked(true);
            }
            Settings.System.putInt(getContentResolver(),
                     Settings.System.ACCELEROMETER_ROTATION_MODE, mode);
        }

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        readAnimationPreference(0, mWindowAnimationsPref);
        readAnimationPreference(1, mTransitionAnimationsPref);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        LayoutInflater factory = LayoutInflater.from(this);

        switch (dialogId) {
            case DIALOG_DENSITY:
                final View textEntryView = factory.inflate(
                        R.layout.alert_dialog_text_entry, null);
                return new AlertDialog.Builder(this)
                        .setTitle("Set custom density")
                        .setView(textEntryView)
                        .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                EditText dpi = (EditText) textEntryView.findViewById(R.id.dpi_edit);
                                Editable text = dpi.getText();
                                Log.i(TAG, text.toString());

                                try {
                                    newDensityValue = Integer.parseInt(text.toString());
                                    showDialog(DIALOG_WARN_DENSITY);
                                } catch (Exception e) {
                                    mLcdDensity.setSummary("INVALID DENSITY!");
                                }

                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                dialog.dismiss();
                            }
                        }).create();
            case DIALOG_WARN_DENSITY:
                return new AlertDialog.Builder(this)
                        .setTitle("WARNING!")
                        .setMessage(
                                "Changing your LCD density can cause unexpected app behavior and cause incompatibility issues with the market. If this occurs, you need to change your density back, reboot, then clear your market data.")
                        .setCancelable(false)
                        .setPositiveButton("Got it!", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setLcdDensity(newDensityValue);
                                dialog.dismiss();
                                mLcdDensity.setSummary(newDensityValue + "");

                            }
                        })
                        .setNegativeButton("Return to safety",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                        .create();
        }
        return null;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLcdDensity) {
            String strValue = (String) newValue;
            if (strValue.equals("custom")) {
                showDialog(DIALOG_DENSITY);
                return true;
            } else {
                newDensityValue = Integer.parseInt((String) newValue);
                showDialog(DIALOG_WARN_DENSITY);
                return true;
            }
        } else if (preference == mWindowAnimationsPref) {
            writeAnimationPreference(0, newValue);
            return true;
        } else if (preference == mTransitionAnimationsPref) {
            writeAnimationPreference(1, newValue);
            return true;
        } else if (preference == mElectronBeamAnimationOnPref) {
            int electronBeamAnimation = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(), Settings.System.ELECTRON_BEAM_ANIMATION_ON_DELAY,
                    electronBeamAnimation);
            return true;
        }
        return false;
    }

    public void writeAnimationPreference(int which, Object newValue) {
        try {
            float val = Float.parseFloat(newValue.toString());
            mWindowManager.setAnimationScale(which, val);
        } catch (NumberFormatException e) {
        } catch (RemoteException e) {
        }
    }

    class ClearUserDataObserver extends IPackageDataObserver.Stub {
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
            mHandler.sendEmptyMessage(MSG_DATA_CLEARED);
        }
    }

    int floatToIndex(float val, int resid) {
        String[] indices = getResources().getStringArray(resid);
        float lastVal = Float.parseFloat(indices[0]);
        for (int i=1; i<indices.length; i++) {
            float thisVal = Float.parseFloat(indices[i]);
            if (val < (lastVal + (thisVal-lastVal)*.5f)) {
                return i-1;
            }
            lastVal = thisVal;
        }
        return indices.length-1;
    }

    public void readAnimationPreference(int which, ListPreference pref) {
        try {
            float scale = mWindowManager.getAnimationScale(which);
            pref.setValueIndex(floatToIndex(scale,
                    R.array.entryvalues_animations));
        } catch (RemoteException e) {
        }
    }

    private class ClearMarketDataTask extends AsyncTask<String, Void, Boolean> {
        protected Boolean doInBackground(String... stuff) {
            String vending = "/data/data/com.android.vending/";
            CommandResult cr = new CMDProcessor().su.runWaitFor("ls " + vending);

            if (cr.stdout == null)
                return false;

            for (String dir : cr.stdout.split("\n")) {
                if (!dir.equals("lib")) {
                    String c = "rm -r " + vending + dir;
                    // Log.i(TAG, c);
                    if (!new CMDProcessor().su.runWaitFor(c).success())
                        return false;
                }
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            mClearMarketData.setSummary(result ? "Market data cleared."
                    : "Market data couldn't be cleared, please clear it yourself!");
        }
    }

    private void setLcdDensity(int newDensity) {
        Helpers.getMount("rw");
        new CMDProcessor().su.runWaitFor("busybox sed -i 's|ro.sf.lcd_density=.*|"
                + "ro.sf.lcd_density" + "=" + newDensity + "|' " + "/system/build.prop");
        new CMDProcessor().su.runWaitFor("busybox sed -i 's|qemu.sf.lcd_density=.*|"
                + "qemu.sf.lcd_density" + "=" + newDensity + "|' " + "/system/build.prop");
        Helpers.getMount("ro");
    }

}
