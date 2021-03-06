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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import java.util.List;

public class GestureMenuActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "Gestures";

    private static final String LOCKSCREEN_GESTURES_ENABLE = "lockscreen_gestures_enable";
    private static final String LOCKSCREEN_GESTURES_TRAIL = "lockscreen_gestures_trail";
    private static final String LOCKSCREEN_GESTURES_SENSITIVITY = "lockscreen_gestures_sensitivity";
    private static final String LOCKSCREEN_GESTURES_COLOR = "lockscreen_gestures_color";
    private static final String LOCKSCREEN_GESTURES_BUILDER = "lockscreen_gestures_builder";

    private CheckBoxPreference mGesturesEnable;
    private CheckBoxPreference mGesturesTrail;
    private ListPreference mGesturesSensitivity;
    private Preference mGesturesColor;
    private PreferenceScreen mGesturesBuilder;

    public static boolean updatePreferenceToSpecificActivityOrRemove(Context context,
            PreferenceGroup parentPreferenceGroup, String preferenceKey, int flags) {

        Preference preference = parentPreferenceGroup.findPreference(preferenceKey);
        if (preference == null) {
            return false;
        }

        Intent intent = preference.getIntent();
        if (intent != null) {
            // Find the activity that is in the system image
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                ResolveInfo resolveInfo = list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)
                        != 0) {
                    preference.setIntent(new Intent().setClassName(
                            resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name));
                    return true;
                }
            }
        }
        parentPreferenceGroup.removePreference(preference);
        return true;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.gesture_settings);
        PreferenceScreen prefSet = getPreferenceScreen();
        mGesturesEnable = (CheckBoxPreference) prefSet.findPreference(LOCKSCREEN_GESTURES_ENABLE);
        mGesturesTrail = (CheckBoxPreference) prefSet.findPreference(LOCKSCREEN_GESTURES_TRAIL);
        mGesturesSensitivity = (ListPreference) prefSet.findPreference(LOCKSCREEN_GESTURES_SENSITIVITY);
        mGesturesColor = (Preference) prefSet.findPreference(LOCKSCREEN_GESTURES_COLOR);

        final PreferenceGroup parentPreference = getPreferenceScreen();
        parentPreference.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        mGesturesColor.setSummary(Integer.toHexString(getGestureColor()));

        mGesturesBuilder = (PreferenceScreen) prefSet.findPreference(LOCKSCREEN_GESTURES_BUILDER);
    }

    private void updateToggles() {
            mGesturesEnable.setChecked(Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.LOCKSCREEN_GESTURES_ENABLED, 0) != 0);
            mGesturesEnable.setEnabled(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_STYLE_PREF, 11) < 6);
            mGesturesTrail.setChecked(Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.LOCKSCREEN_GESTURES_TRAIL, 0) != 0);
            mGesturesSensitivity.setValue(Integer.toString(Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.LOCKSCREEN_GESTURES_SENSITIVITY, 3)));
            mGesturesSensitivity.setSummary(mGesturesSensitivity.getEntry());
            mGesturesColor.setSummary(Integer.toHexString(getGestureColor()));
            mGesturesBuilder.setEnabled(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_STYLE_PREF, 11) < 6);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mGesturesColor) {
            ColorPickerDialog cp = new ColorPickerDialog(this, mGesturesColorListener, getGestureColor());
            cp.show();
            return true;
        }
        return false;
    }

    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (LOCKSCREEN_GESTURES_ENABLE.equals(key)) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_GESTURES_ENABLED,
                    mGesturesEnable.isChecked() ? 1 : 0);
        } else if (LOCKSCREEN_GESTURES_TRAIL.equals(key)) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_GESTURES_TRAIL,
                    mGesturesTrail.isChecked() ? 1 : 0);
        } else if (LOCKSCREEN_GESTURES_SENSITIVITY.equals(key)) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_GESTURES_SENSITIVITY,
                    Integer.parseInt(mGesturesSensitivity.getValue()));
            mGesturesSensitivity.setSummary(mGesturesSensitivity.getEntry());
        }
    }

    private int getGestureColor() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_GESTURES_COLOR, 0xFFFFFF00);
    }

    ColorPickerDialog.OnColorChangedListener mGesturesColorListener =
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_GESTURES_COLOR, color);
                mGesturesColor.setSummary(Integer.toHexString(color));
            }
            public void colorUpdate(int color) {
            }
    };

    @Override
    public void onResume() {
        super.onResume();
        updateToggles();
    }
}
