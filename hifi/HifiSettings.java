package com.meizu.settings.soundandvibrate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings.System;
import android.util.Log;
import com.android.settings.C0240R;
import com.android.settings.SettingsPreferenceFragment;
import com.meizu.settings.utils.MzUtils;
import com.meizu.settings.widget.TwoLineCheckedTextPreference;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class HifiSettings extends SettingsPreferenceFragment {
    private String HIFI_MUSIC_ENABLED;
    private String HIFI_MUSIC_PARAM;
    private AudioManager mAudioManager;
    private boolean mCanClick = true;
    private TwoLineCheckedTextPreference mHifiAuto;
    private TwoLineCheckedTextPreference mHifiHighGain;
    private TwoLineCheckedTextPreference mHifiLineOut;
    private TwoLineCheckedTextPreference mHifiLowGain;
    private int mImpedance;
    private boolean mIsHifiEnabled = false;
    private BroadcastReceiver mReceiver;

    class C10931 extends BroadcastReceiver {
        C10931() {
        }

        public void onReceive(Context arg0, Intent intent) {
            int state = intent.getIntExtra("state", 0);
            Log.d("HifiSettings", "onReceive state = " + state);
            if (state == 1) {
                HifiSettings.this.mIsHifiEnabled = true;
                HifiSettings.this.readImpedance();
            } else {
                HifiSettings.this.mIsHifiEnabled = false;
                HifiSettings.this.mImpedance = 0;
            }
            HifiSettings.this.updateCheckTextView();
        }
    }

    class C10942 extends TimerTask {
        C10942() {
        }

        public void run() {
            HifiSettings.this.mCanClick = true;
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mAudioManager = (AudioManager) getActivity().getSystemService("audio");
        addPreferencesFromResource(C0240R.xml.hifi_settings);
        MzUtils.showPreferenceScreenBottomDivider(getPreferenceScreen(), false);
        this.mHifiAuto = (TwoLineCheckedTextPreference) findPreference("hifi_auto");
        this.mHifiLowGain = (TwoLineCheckedTextPreference) findPreference("hifi_low_gain");
        this.mHifiHighGain = (TwoLineCheckedTextPreference) findPreference("hifi_high_gain");
        this.mHifiLineOut = (TwoLineCheckedTextPreference) findPreference("hifi_line_out");
        try {
            this.HIFI_MUSIC_ENABLED = "hifi_music_enabled";
            this.HIFI_MUSIC_PARAM = "hifi_music_param";
        } catch (Exception e) {
        }
        this.mReceiver = new C10931();
        getActivity().registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.HEADSET_PLUG"));
        if (!this.mAudioManager.isWiredHeadsetOn()) {
        }
    }

    private void readImpedance() {
        Exception e;
        String impedance = "";
        File file = new File("/sys/class/arizona/wm8998_hp_impedance/hp_impedance");
        if (!file.exists() || file.isDirectory()) {
            Log.w("HifiSettings", "readImpedance file not exist , return");
            return;
        }
        BufferedReader br = null;
        try {
            BufferedReader br2 = new BufferedReader(new FileReader(file));
            try {
                StringBuffer sb = new StringBuffer();
                for (String temp = br2.readLine(); temp != null; temp = br2.readLine()) {
                    sb.append(temp + " ");
                }
                impedance = sb.toString().trim();
                if (impedance != null) {
                    this.mImpedance = Integer.parseInt(impedance) - 35;
                    Log.d("HifiSettings", "readImpedance(): mImpedance = " + this.mImpedance);
                }
                br2.close();
                br = br2;
            } catch (Exception e2) {
                e = e2;
                br = br2;
                Log.w("HifiSettings", "readImpedance error  one= " + e);
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e1) {
                        Log.w("HifiSettings", "readImpedance error two = " + e1);
                    }
                }
            }
        } catch (Exception e3) {
            e = e3;
            Log.w("HifiSettings", "readImpedance error  one= " + e);
            if (br != null) {
                br.close();
            }
        }
    }

    public void onResume() {
        initMode();
        this.mIsHifiEnabled = this.mAudioManager.isWiredHeadsetOn();
        readImpedance();
        updateCheckTextView();
        super.onResume();
    }

    private void initMode() {
        int mode = System.getInt(getActivity().getContentResolver(), this.HIFI_MUSIC_PARAM, 0);
        switch (mode) {
            case 0:
                this.mHifiAuto.setChecked(true);
                this.mHifiLowGain.setChecked(false);
                this.mHifiHighGain.setChecked(false);
                this.mHifiLineOut.setChecked(false);
                return;
            case 1:
                this.mHifiAuto.setChecked(false);
                this.mHifiLowGain.setChecked(true);
                this.mHifiHighGain.setChecked(false);
                this.mHifiLineOut.setChecked(false);
                return;
            case 2:
                this.mHifiAuto.setChecked(false);
                this.mHifiLowGain.setChecked(false);
                this.mHifiHighGain.setChecked(true);
                this.mHifiLineOut.setChecked(false);
                return;
            case 3:
                this.mHifiAuto.setChecked(false);
                this.mHifiLowGain.setChecked(false);
                this.mHifiHighGain.setChecked(false);
                this.mHifiLineOut.setChecked(true);
                return;
            default:
                Log.e("HifiSettings", "unknow HIFI_MUSIC_PARAM:" + mode);
                return;
        }
    }

    private void updateCheckTextView() {
        if (this.mIsHifiEnabled) {
            this.mHifiAuto.setEnabled(true);
            this.mHifiLowGain.setEnabled(true);
            if (this.mImpedance >= 20) {
                this.mHifiHighGain.setEnabled(true);
                this.mHifiLineOut.setEnabled(true);
                return;
            }
            this.mHifiHighGain.setEnabled(false);
            this.mHifiLineOut.setEnabled(false);
            if (System.getInt(getActivity().getContentResolver(), this.HIFI_MUSIC_PARAM, 0) > 1) {
                setMode(this.mHifiAuto);
                return;
            }
            return;
        }
        this.mHifiAuto.setEnabled(false);
        this.mHifiLowGain.setEnabled(false);
        this.mHifiHighGain.setEnabled(false);
        this.mHifiLineOut.setEnabled(false);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Log.v("HifiSettings", "----------------------- onPreferenceTreeClick ------------------------");
        if (this.mCanClick) {
            this.mCanClick = false;
            new Timer().schedule(new C10942(), 150);
            if (preference instanceof TwoLineCheckedTextPreference) {
                setMode(preference);
            }
        }
        return true;
    }

    private void setMode(Preference preference) {
        if (preference == this.mHifiAuto) {
            if (!this.mHifiAuto.isChecked()) {
                this.mHifiAuto.setChecked(true);
                this.mHifiLowGain.setChecked(false);
                this.mHifiHighGain.setChecked(false);
                this.mHifiLineOut.setChecked(false);
                System.putInt(getActivity().getContentResolver(), this.HIFI_MUSIC_PARAM, 0);
                this.mAudioManager.setParameters("hifi_gain=0");
            }
        } else if (preference == this.mHifiLowGain) {
            if (!this.mHifiLowGain.isChecked()) {
                this.mHifiAuto.setChecked(false);
                this.mHifiLowGain.setChecked(true);
                this.mHifiHighGain.setChecked(false);
                this.mHifiLineOut.setChecked(false);
                System.putInt(getActivity().getContentResolver(), this.HIFI_MUSIC_PARAM, 1);
                this.mAudioManager.setParameters("hifi_gain=1");
            }
        } else if (preference == this.mHifiHighGain) {
            if (!this.mHifiHighGain.isChecked()) {
                this.mHifiAuto.setChecked(false);
                this.mHifiLowGain.setChecked(false);
                this.mHifiHighGain.setChecked(true);
                this.mHifiLineOut.setChecked(false);
                checkAutoAdjustStreamVolume();
                System.putInt(getActivity().getContentResolver(), this.HIFI_MUSIC_PARAM, 2);
                this.mAudioManager.setParameters("hifi_gain=2");
            }
        } else if (preference == this.mHifiLineOut && !this.mHifiLineOut.isChecked()) {
            this.mHifiAuto.setChecked(false);
            this.mHifiLowGain.setChecked(false);
            this.mHifiHighGain.setChecked(false);
            this.mHifiLineOut.setChecked(true);
            System.putInt(getActivity().getContentResolver(), this.HIFI_MUSIC_PARAM, 3);
            this.mAudioManager.setParameters("hifi_gain=3");
        }
    }

    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(this.mReceiver);
    }

    private void checkAutoAdjustStreamVolume() {
        if (this.mAudioManager.getStreamVolume(3) > 50) {
            Log.d("HifiSettings", "checkAutoAdjustStreamVolume: auto set music_stream volume index: 50");
            this.mAudioManager.setStreamVolume(3, 50, 0);
        }
    }
}
