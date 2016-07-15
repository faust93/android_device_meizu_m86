package android.media;

import android.Manifest$permission;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AppOpsManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothClass.Device;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.SensorManager;
import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.HdmiPlaybackClient;
import android.hardware.hdmi.HdmiPlaybackClient.DisplayStatusCallback;
import android.hardware.hdmi.HdmiTvClient;
import android.hardware.usb.UsbManager;
import android.media.AudioManagerInternal.RingerModeDelegate;
import android.media.AudioSystem.ErrorCallback;
import android.media.IAudioService.Stub;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.SoundPool.Builder;
import android.media.SoundPool.OnLoadCompleteListener;
import android.media.audiopolicy.AudioPolicyConfig;
import android.media.audiopolicy.IAudioPolicyCallback;
import android.net.ProxyInfo;
import android.os.Binder;
import android.os.BuildExt;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.MzSettings;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DateTimeUrlHelper;
import android.util.Log;
import android.util.MathUtils;
import android.util.Slog;
import android.util.TimedRemoteCaller;
import android.view.OrientationEventListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener;
import com.android.internal.C0833R;
import com.android.internal.telephony.PhoneConstants;
import com.android.server.LocalServices;
import com.flyme.internal.C0997R;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class AudioService extends Stub {
    private static final String ASSET_FILE_VERSION = "1.0";
    private static final String ATTR_ASSET_FILE = "file";
    private static final String ATTR_ASSET_ID = "id";
    private static final String ATTR_GROUP_NAME = "name";
    private static final String ATTR_VERSION = "version";
    private static final int BTA2DP_DOCK_TIMEOUT_MILLIS = 8000;
    private static final int BT_HEADSET_CNCT_TIMEOUT_MS = 3000;
    protected static final boolean DEBUG_AP = Log.isLoggable("AudioService.AP", 3);
    protected static final boolean DEBUG_MODE = true;
    private static final boolean DEBUG_SESSIONS = true;
    protected static final boolean DEBUG_VOL = true;
    private static int[] DEFAULT_STREAM_VOLUME = new int[]{8, 15, 10, 24, 12, 10, 15, 15, 11, 24};
    private static final int FLAG_ADJUST_VOLUME = 1;
    private static final String GROUP_TOUCH_SOUNDS = "touch_sounds";
    private static final int MAX_BATCH_VOLUME_ADJUST_STEPS = 4;
    private static final int MAX_MASTER_VOLUME = 100;
    private static final int[] MAX_STREAM_VOLUME = new int[]{15, 15, 15, 60, 15, 15, 15, 15, 15, 60};
    private static final int MSG_BROADCAST_AUDIO_BECOMING_NOISY = 15;
    private static final int MSG_BROADCAST_BT_CONNECTION_STATE = 19;
    private static final int MSG_BTA2DP_DOCK_TIMEOUT = 6;
    private static final int MSG_BTA2DP_SERVICE_DISCONNECT = 26;
    private static final int MSG_BT_HEADSET_CNCT_FAILED = 9;
    private static final int MSG_CHECK_MUSIC_ACTIVE = 14;
    private static final int MSG_CONFIGURE_SAFE_MEDIA_VOLUME = 16;
    private static final int MSG_CONFIGURE_SAFE_MEDIA_VOLUME_FORCED = 17;
    private static final int MSG_LOAD_SOUND_EFFECTS = 7;
    private static final int MSG_MEDIA_SERVER_DIED = 4;
    private static final int MSG_MUTE_SET_AFFECT_MUSIC = 25;
    private static final int MSG_PERSIST_MASTER_VOLUME = 2;
    private static final int MSG_PERSIST_MASTER_VOLUME_MUTE = 11;
    private static final int MSG_PERSIST_MICROPHONE_MUTE = 23;
    private static final int MSG_PERSIST_MUSIC_ACTIVE_MS = 22;
    private static final int MSG_PERSIST_RINGER_MODE = 3;
    private static final int MSG_PERSIST_SAFE_VOLUME_STATE = 18;
    private static final int MSG_PERSIST_VOLUME = 1;
    private static final int MSG_PLAY_SOUND_EFFECT = 5;
    private static final int MSG_REPORT_NEW_ROUTES = 12;
    private static final int MSG_RINGMODE_SET_AFFECT_MUSIC = 24;
    private static final int MSG_SET_A2DP_SINK_CONNECTION_STATE = 102;
    private static final int MSG_SET_A2DP_SRC_CONNECTION_STATE = 101;
    private static final int MSG_SET_ALL_VOLUMES = 10;
    private static final int MSG_SET_DEVICE_VOLUME = 0;
    private static final int MSG_SET_FORCE_BT_A2DP_USE = 13;
    private static final int MSG_SET_FORCE_USE = 8;
    private static final int MSG_SET_LAST_MUSIC_SPEAKER_VOLUME = 27;
    private static final int MSG_SET_WIRED_DEVICE_CONNECTION_STATE = 100;
    private static final int MSG_SYSTEM_READY = 21;
    private static final int MSG_UNLOAD_SOUND_EFFECTS = 20;
    private static final int MUSIC_ACTIVE_POLL_PERIOD_MS = 60000;
    private static final int NUM_SOUNDPOOL_CHANNELS = 4;
    private static final int PERSIST_DELAY = 500;
    private static final int PLATFORM_DEFAULT = 0;
    private static final int PLATFORM_TELEVISION = 2;
    private static final int PLATFORM_VOICE = 1;
    public static final int PLAY_SOUND_DELAY = 300;
    private static final boolean PREVENT_VOLUME_ADJUSTMENT_IF_SILENT = true;
    private static final String[] RINGER_MODE_NAMES = new String[]{"SILENT", "VIBRATE", "NORMAL"};
    private static final int SAFE_MEDIA_VOLUME_ACTIVE = 3;
    private static final int SAFE_MEDIA_VOLUME_DISABLED = 1;
    private static final int SAFE_MEDIA_VOLUME_INACTIVE = 2;
    private static final int SAFE_MEDIA_VOLUME_NOT_CONFIGURED = 0;
    private static final int SAFE_VOLUME_CONFIGURE_TIMEOUT_MS = 30000;
    private static final int SCO_MODE_MAX = 2;
    private static final int SCO_MODE_RAW = 1;
    private static final int SCO_MODE_UNDEFINED = -1;
    private static final int SCO_MODE_VIRTUAL_CALL = 0;
    private static final int SCO_MODE_VR = 2;
    private static final int SCO_STATE_ACTIVATE_REQ = 1;
    private static final int SCO_STATE_ACTIVE_EXTERNAL = 2;
    private static final int SCO_STATE_ACTIVE_INTERNAL = 3;
    private static final int SCO_STATE_DEACTIVATE_EXT_REQ = 4;
    private static final int SCO_STATE_DEACTIVATE_REQ = 5;
    private static final int SCO_STATE_INACTIVE = 0;
    private static final int SENDMSG_NOOP = 1;
    private static final int SENDMSG_QUEUE = 2;
    private static final int SENDMSG_REPLACE = 0;
    private static final int SOUND_EFFECTS_LOAD_TIMEOUT_MS = 5000;
    private static final String SOUND_EFFECTS_PATH = "/media/audio/ui/";
    private static final List<String> SOUND_EFFECT_FILES = new ArrayList();
    private static final int[] STEAM_VOLUME_OPS = new int[]{34, 36, 35, 36, 37, 38, 39, 36, 36, 36};
    private static final String[] STREAM_NAMES = new String[]{"STREAM_VOICE_CALL", "STREAM_SYSTEM", "STREAM_RING", "STREAM_MUSIC", "STREAM_ALARM", "STREAM_NOTIFICATION", "STREAM_BLUETOOTH_SCO", "STREAM_SYSTEM_ENFORCED", "STREAM_DTMF", "STREAM_TTS"};
    private static final String TAG = "AudioService";
    private static final String TAG_ASSET = "asset";
    private static final String TAG_AUDIO_ASSETS = "audio_assets";
    private static final String TAG_GROUP = "group";
    private static final int UNSAFE_VOLUME_MUSIC_ACTIVE_MS_MAX = 72000000;
    private static final boolean VOLUME_SETS_RINGER_MODE_SILENT = false;
    private static int mCanResumeNormalStreams = 38;
    private static Long mLastDeviceConnectMsgTime = new Long(0);
    private static int sSoundEffectVolumeDb;
    private final int[][] SOUND_EFFECT_FILES_MAP = ((int[][]) Array.newInstance(Integer.TYPE, new int[]{10, 2}));
    private final int[] STREAM_VOLUME_ALIAS_DEFAULT = new int[]{0, 2, 2, 3, 4, 2, 6, 2, 2, 3};
    private final int[] STREAM_VOLUME_ALIAS_TELEVISION = new int[]{3, 3, 3, 3, 3, 3, 3, 3, 3, 3};
    private final int[] STREAM_VOLUME_ALIAS_VOICE = new int[]{0, 5, 2, 3, 4, 5, 6, 5, 5, 3};
    private BluetoothA2dp mA2dp;
    private final Object mA2dpAvrcpLock = new Object();
    private final AppOpsManager mAppOps;
    private WakeLock mAudioEventWakeLock;
    private AudioHandler mAudioHandler;
    private HashMap<IBinder, AudioPolicyProxy> mAudioPolicies = new HashMap();
    private int mAudioPolicyCounter = 0;
    private final ErrorCallback mAudioSystemCallback = new C02241();
    private AudioSystemThread mAudioSystemThread;
    private boolean mAvrcpAbsVolSupported = false;
    int mBecomingNoisyIntentDevices = 163724;
    private boolean mBluetoothA2dpEnabled;
    private final Object mBluetoothA2dpEnabledLock = new Object();
    private BluetoothHeadset mBluetoothHeadset;
    private BluetoothDevice mBluetoothHeadsetDevice;
    private ServiceListener mBluetoothProfileServiceListener = new C02252();
    private Boolean mCameraSoundForced;
    private final HashMap<Integer, String> mConnectedDevices = new HashMap();
    private final ContentResolver mContentResolver;
    private final Context mContext;
    final AudioRoutesInfo mCurAudioRoutes = new AudioRoutesInfo();
    private int mDeviceOrientation = 0;
    private int mDeviceRotation = 0;
    private String mDockAddress;
    private boolean mDockAudioMediaEnabled = true;
    private int mDockState = 0;
    int mFixedVolumeDevices = 2915328;
    private ForceControlStreamClient mForceControlStreamClient = null;
    private final Object mForceControlStreamLock = new Object();
    private int mForcedUseForComm;
    int mFullVolumeDevices = 0;
    private final boolean mHasVibrator;
    private boolean mHdmiCecSink;
    private MyDisplayStatusCallback mHdmiDisplayStatusCallback = new MyDisplayStatusCallback();
    private HdmiControlManager mHdmiManager;
    private HdmiPlaybackClient mHdmiPlaybackClient;
    private boolean mHdmiSystemAudioSupported = false;
    private HdmiTvClient mHdmiTvClient;
    private KeyguardManager mKeyguardManager;
    private final int[] mMasterVolumeRamp;
    private int mMcc = 0;
    private final MediaFocusControl mMediaFocusControl;
    private int mMode = 0;
    private final boolean mMonitorOrientation;
    private final boolean mMonitorRotation;
    private int mMusicActiveMs;
    private int mMuteAffectedStreams;
    private NotificationManager mNotificationManager;
    private AudioOrientationEventListener mOrientationListener;
    private StreamVolumeCommand mPendingVolumeCommand;
    private PhoneStateListener mPhoneStateListener = new C02274();
    private final int mPlatformType;
    private int mPrevVolDirection = 0;
    private final BroadcastReceiver mReceiver = new AudioServiceBroadcastReceiver();
    private int mRingerMode;
    private int mRingerModeAffectedStreams = 0;
    private RingerModeDelegate mRingerModeDelegate;
    private int mRingerModeExternal = -1;
    private int mRingerModeMutedStreams;
    private volatile IRingtonePlayer mRingtonePlayer;
    private ArrayList<RmtSbmxFullVolDeathHandler> mRmtSbmxFullVolDeathHandlers = new ArrayList();
    private int mRmtSbmxFullVolRefCount = 0;
    final RemoteCallbackList<IAudioRoutesObserver> mRoutesObservers = new RemoteCallbackList();
    private final int mSafeMediaVolumeDevices = 0;
    private int mSafeMediaVolumeIndex;
    private Integer mSafeMediaVolumeState;
    private int mScoAudioMode;
    private int mScoAudioState;
    private final ArrayList<ScoClient> mScoClients = new ArrayList();
    private int mScoConnectionState;
    private final ArrayList<SetModeDeathHandler> mSetModeDeathHandlers = new ArrayList();
    private final Object mSettingsLock = new Object();
    private SettingsObserver mSettingsObserver;
    private final Object mSoundEffectsLock = new Object();
    private SoundPool mSoundPool;
    private SoundPoolCallback mSoundPoolCallBack;
    private SoundPoolListenerThread mSoundPoolListenerThread;
    private Looper mSoundPoolLooper = null;
    private VolumeStreamState[] mStreamStates;
    private int[] mStreamVolumeAlias;
    private boolean mSystemReady;
    private final boolean mUseFixedVolume;
    private final boolean mUseMasterVolume;
    private int mVibrateSetting;
    private int mVolumeControlStream = -1;
    private final VolumeController mVolumeController = new VolumeController();

    class C02241 implements ErrorCallback {
        C02241() {
        }

        public void onError(int error) {
            switch (error) {
                case 100:
                    AudioService.sendMsg(AudioService.this.mAudioHandler, 4, 1, 0, 0, null, 0);
                    return;
                default:
                    return;
            }
        }
    }

    class C02252 implements ServiceListener {
        C02252() {
        }

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            List<BluetoothDevice> deviceList;
            BluetoothDevice btDevice;
            switch (profile) {
                case 1:
                    synchronized (AudioService.this.mScoClients) {
                        AudioService.this.mAudioHandler.removeMessages(9);
                        AudioService.this.mBluetoothHeadset = (BluetoothHeadset) proxy;
                        deviceList = AudioService.this.mBluetoothHeadset.getConnectedDevices();
                        if (deviceList.size() > 0) {
                            AudioService.this.mBluetoothHeadsetDevice = (BluetoothDevice) deviceList.get(0);
                        } else {
                            AudioService.this.mBluetoothHeadsetDevice = null;
                        }
                        AudioService.this.checkScoAudioState();
                        if (AudioService.this.mScoAudioState == 1 || AudioService.this.mScoAudioState == 5 || AudioService.this.mScoAudioState == 4) {
                            boolean status = false;
                            if (AudioService.this.mBluetoothHeadsetDevice != null) {
                                switch (AudioService.this.mScoAudioState) {
                                    case 1:
                                        AudioService.this.mScoAudioState = 3;
                                        if (AudioService.this.mScoAudioMode != 1) {
                                            if (AudioService.this.mScoAudioMode != 0) {
                                                if (AudioService.this.mScoAudioMode == 2) {
                                                    status = AudioService.this.mBluetoothHeadset.startVoiceRecognition(AudioService.this.mBluetoothHeadsetDevice);
                                                    break;
                                                }
                                            }
                                            status = AudioService.this.mBluetoothHeadset.startScoUsingVirtualVoiceCall(AudioService.this.mBluetoothHeadsetDevice);
                                            break;
                                        }
                                        status = AudioService.this.mBluetoothHeadset.connectAudio();
                                        break;
                                        break;
                                    case 4:
                                        status = AudioService.this.mBluetoothHeadset.stopVoiceRecognition(AudioService.this.mBluetoothHeadsetDevice);
                                        break;
                                    case 5:
                                        if (AudioService.this.mScoAudioMode != 1) {
                                            if (AudioService.this.mScoAudioMode != 0) {
                                                if (AudioService.this.mScoAudioMode == 2) {
                                                    status = AudioService.this.mBluetoothHeadset.stopVoiceRecognition(AudioService.this.mBluetoothHeadsetDevice);
                                                    break;
                                                }
                                            }
                                            status = AudioService.this.mBluetoothHeadset.stopScoUsingVirtualVoiceCall(AudioService.this.mBluetoothHeadsetDevice);
                                            break;
                                        }
                                        status = AudioService.this.mBluetoothHeadset.disconnectAudio();
                                        break;
                                        break;
                                }
                            }
                            if (!status) {
                                AudioService.sendMsg(AudioService.this.mAudioHandler, 9, 0, 0, 0, null, 0);
                            }
                        }
                    }
                    return;
                case 2:
                    synchronized (AudioService.this.mConnectedDevices) {
                        synchronized (AudioService.this.mA2dpAvrcpLock) {
                            AudioService.this.mA2dp = (BluetoothA2dp) proxy;
                            deviceList = AudioService.this.mA2dp.getConnectedDevices();
                            if (deviceList.size() > 0) {
                                btDevice = (BluetoothDevice) deviceList.get(0);
                                int state = AudioService.this.mA2dp.getConnectionState(btDevice);
                                AudioService.this.queueMsgUnderWakeLock(AudioService.this.mAudioHandler, 102, state, 0, btDevice, AudioService.this.checkSendBecomingNoisyIntent(128, state == 2 ? 1 : 0));
                            }
                        }
                    }
                    return;
                case 10:
                    deviceList = proxy.getConnectedDevices();
                    if (deviceList.size() > 0) {
                        btDevice = (BluetoothDevice) deviceList.get(0);
                        synchronized (AudioService.this.mConnectedDevices) {
                            AudioService.this.queueMsgUnderWakeLock(AudioService.this.mAudioHandler, 101, proxy.getConnectionState(btDevice), 0, btDevice, 0);
                        }
                        return;
                    }
                    return;
                default:
                    return;
            }
        }

        public void onServiceDisconnected(int profile) {
            switch (profile) {
                case 1:
                    synchronized (AudioService.this.mScoClients) {
                        AudioService.this.mBluetoothHeadset = null;
                    }
                    return;
                case 2:
                    synchronized (AudioService.this.mConnectedDevices) {
                        synchronized (AudioService.this.mA2dpAvrcpLock) {
                            AudioService.this.mA2dp = null;
                            AudioService.this.queueMsgUnderWakeLock(AudioService.this.mAudioHandler, 26, 0, 0, null, AudioService.this.checkSendBecomingNoisyIntent(128, 0));
                        }
                    }
                    return;
                case 10:
                    synchronized (AudioService.this.mConnectedDevices) {
                        if (AudioService.this.mConnectedDevices.containsKey(Integer.valueOf(AudioSystem.DEVICE_IN_BLUETOOTH_A2DP))) {
                            AudioService.this.makeA2dpSrcUnavailable((String) AudioService.this.mConnectedDevices.get(Integer.valueOf(AudioSystem.DEVICE_IN_BLUETOOTH_A2DP)));
                        }
                    }
                    return;
                default:
                    return;
            }
        }
    }

    class C02274 extends PhoneStateListener {
        C02274() {
        }

        public void onCallStateChanged(int state, String incomingNumber) {
            Log.m30v(AudioService.TAG, "onCallStateChanged state = " + state);
            if (state == 1 || state == 2) {
                AudioSystem.setParameters("callAction=on");
            } else {
                AudioSystem.setParameters("callAction=off");
            }
        }
    }

    private class AudioHandler extends Handler {

        class C02281 implements OnCompletionListener {
            C02281() {
            }

            public void onCompletion(MediaPlayer mp) {
                AudioHandler.this.cleanupPlayer(mp);
            }
        }

        class C02292 implements OnErrorListener {
            C02292() {
            }

            public boolean onError(MediaPlayer mp, int what, int extra) {
                AudioHandler.this.cleanupPlayer(mp);
                return true;
            }
        }

        private AudioHandler() {
        }

        private void setDeviceVolume(VolumeStreamState streamState, int device) {
            synchronized (VolumeStreamState.class) {
                streamState.applyDeviceVolume_syncVSS(device);
                int streamType = AudioSystem.getNumStreamTypes() - 1;
                while (streamType >= 0) {
                    if (streamType != streamState.mStreamType && AudioService.this.mStreamVolumeAlias[streamType] == streamState.mStreamType) {
                        int streamDevice = AudioService.this.getDeviceForStream(streamType);
                        if (!(device == streamDevice || !AudioService.this.mAvrcpAbsVolSupported || (device & AudioSystem.DEVICE_OUT_ALL_A2DP) == 0)) {
                            AudioService.this.mStreamStates[streamType].applyDeviceVolume_syncVSS(device);
                        }
                        AudioService.this.mStreamStates[streamType].applyDeviceVolume_syncVSS(streamDevice);
                    }
                    streamType--;
                }
            }
            AudioService.sendMsg(AudioService.this.mAudioHandler, 1, 2, device, 0, streamState, 500);
        }

        private void setAllVolumes(VolumeStreamState streamState) {
            streamState.applyAllVolumes();
            int streamType = AudioSystem.getNumStreamTypes() - 1;
            while (streamType >= 0) {
                if (streamType != streamState.mStreamType && AudioService.this.mStreamVolumeAlias[streamType] == streamState.mStreamType) {
                    AudioService.this.mStreamStates[streamType].applyAllVolumes();
                }
                streamType--;
            }
        }

        private void persistVolume(VolumeStreamState streamState, int device) {
            if (!AudioService.this.mUseFixedVolume) {
                if (!AudioService.this.isPlatformTelevision() || streamState.mStreamType == 3) {
                    System.putIntForUser(AudioService.this.mContentResolver, streamState.getSettingNameForDevice(device), (streamState.getIndex(device) + 5) / 10, -2);
                }
            }
        }

        private void persistRingerMode(int ringerMode) {
            if (!AudioService.this.mUseFixedVolume) {
                Global.putInt(AudioService.this.mContentResolver, "mode_ringer", ringerMode);
            }
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private boolean onLoadSoundEffects() {
            synchronized (AudioService.this.mSoundEffectsLock) {
                if (!AudioService.this.mSystemReady) {
                    Log.m32w(AudioService.TAG, "onLoadSoundEffects() called before boot complete");
                    return false;
                } else if (AudioService.this.mSoundPool != null) {
                    return true;
                } else {
                    int attempts;
                    AudioService.this.loadTouchSoundAssets();
                    AudioService.this.mSoundPool = new Builder().setMaxStreams(4).setAudioAttributes(new AudioAttributes.Builder().setUsage(13).setContentType(4).build()).build();
                    AudioService.this.mSoundPoolCallBack = null;
                    AudioService.this.mSoundPoolListenerThread = new SoundPoolListenerThread();
                    AudioService.this.mSoundPoolListenerThread.start();
                    int attempts2 = 3;
                    while (AudioService.this.mSoundPoolCallBack == null) {
                        attempts = attempts2 - 1;
                        if (attempts2 <= 0) {
                            break;
                        }
                        try {
                            AudioService.this.mSoundEffectsLock.wait(TimedRemoteCaller.DEFAULT_CALL_TIMEOUT_MILLIS);
                            attempts2 = attempts;
                        } catch (InterruptedException e) {
                            Log.m32w(AudioService.TAG, "Interrupted while waiting sound pool listener thread.");
                            attempts2 = attempts;
                        }
                    }
                    attempts = attempts2;
                    if (AudioService.this.mSoundPoolCallBack == null) {
                        Log.m32w(AudioService.TAG, "onLoadSoundEffects() SoundPool listener or thread creation error");
                        if (AudioService.this.mSoundPoolLooper != null) {
                            AudioService.this.mSoundPoolLooper.quit();
                            AudioService.this.mSoundPoolLooper = null;
                        }
                        AudioService.this.mSoundPoolListenerThread = null;
                        AudioService.this.mSoundPool.release();
                        AudioService.this.mSoundPool = null;
                        return false;
                    }
                    int effect;
                    int status;
                    int[] poolId = new int[AudioService.SOUND_EFFECT_FILES.size()];
                    for (int fileIdx = 0; fileIdx < AudioService.SOUND_EFFECT_FILES.size(); fileIdx++) {
                        poolId[fileIdx] = -1;
                    }
                    int numSamples = 0;
                    for (effect = 0; effect < 10; effect++) {
                        if (AudioService.this.SOUND_EFFECT_FILES_MAP[effect][1] != 0) {
                            if (poolId[AudioService.this.SOUND_EFFECT_FILES_MAP[effect][0]] == -1) {
                                String filePath = Environment.getRootDirectory() + AudioService.SOUND_EFFECTS_PATH + ((String) AudioService.SOUND_EFFECT_FILES.get(AudioService.this.SOUND_EFFECT_FILES_MAP[effect][0]));
                                int sampleId = AudioService.this.mSoundPool.load(filePath, 0);
                                if (sampleId <= 0) {
                                    Log.m32w(AudioService.TAG, "Soundpool could not load file: " + filePath);
                                } else {
                                    AudioService.this.SOUND_EFFECT_FILES_MAP[effect][1] = sampleId;
                                    poolId[AudioService.this.SOUND_EFFECT_FILES_MAP[effect][0]] = sampleId;
                                    numSamples++;
                                }
                            } else {
                                AudioService.this.SOUND_EFFECT_FILES_MAP[effect][1] = poolId[AudioService.this.SOUND_EFFECT_FILES_MAP[effect][0]];
                            }
                        }
                    }
                    if (numSamples > 0) {
                        AudioService.this.mSoundPoolCallBack.setSamples(poolId);
                        status = 1;
                        attempts2 = 3;
                        while (status == 1) {
                            attempts = attempts2 - 1;
                            if (attempts2 <= 0) {
                                break;
                            }
                            try {
                                AudioService.this.mSoundEffectsLock.wait(TimedRemoteCaller.DEFAULT_CALL_TIMEOUT_MILLIS);
                                status = AudioService.this.mSoundPoolCallBack.status();
                                attempts2 = attempts;
                            } catch (InterruptedException e2) {
                                Log.m32w(AudioService.TAG, "Interrupted while waiting sound pool callback.");
                                attempts2 = attempts;
                            }
                        }
                    } else {
                        status = -1;
                    }
                    if (AudioService.this.mSoundPoolLooper != null) {
                        AudioService.this.mSoundPoolLooper.quit();
                        AudioService.this.mSoundPoolLooper = null;
                    }
                    AudioService.this.mSoundPoolListenerThread = null;
                    if (status != 0) {
                        Log.m32w(AudioService.TAG, "onLoadSoundEffects(), Error " + status + " while loading samples");
                        for (effect = 0; effect < 10; effect++) {
                            if (AudioService.this.SOUND_EFFECT_FILES_MAP[effect][1] > 0) {
                                AudioService.this.SOUND_EFFECT_FILES_MAP[effect][1] = -1;
                            }
                        }
                        AudioService.this.mSoundPool.release();
                        AudioService.this.mSoundPool = null;
                    }
                }
            }
        }

        private void onUnloadSoundEffects() {
            synchronized (AudioService.this.mSoundEffectsLock) {
                if (AudioService.this.mSoundPool == null) {
                    return;
                }
                int[] poolId = new int[AudioService.SOUND_EFFECT_FILES.size()];
                for (int fileIdx = 0; fileIdx < AudioService.SOUND_EFFECT_FILES.size(); fileIdx++) {
                    poolId[fileIdx] = 0;
                }
                int effect = 0;
                while (effect < 10) {
                    if (AudioService.this.SOUND_EFFECT_FILES_MAP[effect][1] > 0 && poolId[AudioService.this.SOUND_EFFECT_FILES_MAP[effect][0]] == 0) {
                        AudioService.this.mSoundPool.unload(AudioService.this.SOUND_EFFECT_FILES_MAP[effect][1]);
                        AudioService.this.SOUND_EFFECT_FILES_MAP[effect][1] = -1;
                        poolId[AudioService.this.SOUND_EFFECT_FILES_MAP[effect][0]] = -1;
                    }
                    effect++;
                }
                AudioService.this.mSoundPool.release();
                AudioService.this.mSoundPool = null;
            }
        }

        private void onPlaySoundEffect(int effectType, int volume) {
            synchronized (AudioService.this.mSoundEffectsLock) {
                onLoadSoundEffects();
                if (AudioService.this.mSoundPool == null) {
                    return;
                }
                float volFloat;
                if (volume < 0) {
                    volFloat = (float) Math.pow(10.0d, (double) (((float) AudioService.sSoundEffectVolumeDb) / 20.0f));
                } else {
                    volFloat = ((float) volume) / 1000.0f;
                }
                if (AudioService.this.SOUND_EFFECT_FILES_MAP[effectType][1] > 0) {
                    AudioService.this.mSoundPool.play(AudioService.this.SOUND_EFFECT_FILES_MAP[effectType][1], volFloat, volFloat, 0, 0, 1.0f);
                } else {
                    MediaPlayer mediaPlayer = new MediaPlayer();
                    try {
                        mediaPlayer.setDataSource(Environment.getRootDirectory() + AudioService.SOUND_EFFECTS_PATH + ((String) AudioService.SOUND_EFFECT_FILES.get(AudioService.this.SOUND_EFFECT_FILES_MAP[effectType][0])));
                        mediaPlayer.setAudioStreamType(1);
                        mediaPlayer.prepare();
                        mediaPlayer.setVolume(volFloat);
                        mediaPlayer.setOnCompletionListener(new C02281());
                        mediaPlayer.setOnErrorListener(new C02292());
                        mediaPlayer.start();
                    } catch (IOException ex) {
                        Log.m32w(AudioService.TAG, "MediaPlayer IOException: " + ex);
                    } catch (IllegalArgumentException ex2) {
                        Log.m32w(AudioService.TAG, "MediaPlayer IllegalArgumentException: " + ex2);
                    } catch (IllegalStateException ex3) {
                        Log.m32w(AudioService.TAG, "MediaPlayer IllegalStateException: " + ex3);
                    }
                }
            }
        }

        private void cleanupPlayer(MediaPlayer mp) {
            if (mp != null) {
                try {
                    mp.stop();
                    mp.release();
                } catch (IllegalStateException ex) {
                    Log.m32w(AudioService.TAG, "MediaPlayer IllegalStateException: " + ex);
                }
            }
        }

        private void setForceUse(int usage, int config) {
            AudioSystem.setForceUse(usage, config);
        }

        private void onPersistSafeVolumeState(int state) {
            Global.putInt(AudioService.this.mContentResolver, Global.AUDIO_SAFE_VOLUME_STATE, state);
        }

        private void onRingerModeChangedAffectMusic(int ringerMode) {
            String prefix = "last_";
            AudioService.log("[GJ_DEBUG] onRingerModeChangedAffectMusic: ringerMode = " + ringerMode);
            if (ringerMode == 0 || ringerMode == 1) {
                AudioService.this.mStreamStates[3].setAllIndexesForMusic(prefix, true, true);
            } else if (ringerMode == 2) {
                AudioService.this.mStreamStates[3].setAllIndexesForMusic(prefix, false, true);
            }
            setAllVolumes(AudioService.this.mStreamStates[3]);
            AudioService.this.broadcastRingerMode(AudioManager.RINGER_MODE_CHANGED_ACTION, ringerMode);
            AudioService.this.broadcastRingerMode(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION, ringerMode);
        }

        private void onMuteChangedAffectMusic(int value) {
            boolean mute;
            String prefix = "last_mute_";
            if (value > 0) {
                mute = true;
            } else {
                mute = false;
            }
            if (mute) {
                AudioService.this.mStreamStates[3].setAllIndexesForMusic(prefix, true, false);
            } else {
                AudioService.this.mStreamStates[3].setAllIndexesForMusic(prefix, false, false);
            }
            setAllVolumes(AudioService.this.mStreamStates[3]);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    setDeviceVolume((VolumeStreamState) msg.obj, msg.arg1);
                    return;
                case 1:
                    persistVolume((VolumeStreamState) msg.obj, msg.arg1);
                    return;
                case 2:
                    if (!AudioService.this.mUseFixedVolume) {
                        System.putFloatForUser(AudioService.this.mContentResolver, System.VOLUME_MASTER, ((float) msg.arg1) / 1000.0f, -2);
                        return;
                    }
                    return;
                case 3:
                    persistRingerMode(AudioService.this.getRingerModeInternal());
                    return;
                case 4:
                    if (AudioService.this.mSystemReady && AudioSystem.checkAudioFlinger() == 0) {
                        Log.m26e(AudioService.TAG, "Media server started.");
                        AudioSystem.setParameters("restarting=true");
                        AudioService.readAndSetLowRamDevice();
                        AudioService.this.MzReadAndSetHifiParam();
                        synchronized (AudioService.this.mConnectedDevices) {
                            for (Entry device : AudioService.this.mConnectedDevices.entrySet()) {
                                AudioSystem.setDeviceConnectionState(((Integer) device.getKey()).intValue(), 1, (String) device.getValue());
                            }
                        }
                        AudioSystem.setPhoneState(AudioService.this.mMode);
                        AudioSystem.setForceUse(0, AudioService.this.mForcedUseForComm);
                        AudioSystem.setForceUse(2, AudioService.this.mForcedUseForComm);
                        AudioSystem.setForceUse(4, AudioService.this.mCameraSoundForced.booleanValue() ? 11 : 0);
                        for (int streamType = AudioSystem.getNumStreamTypes() - 1; streamType >= 0; streamType--) {
                            VolumeStreamState streamState = AudioService.this.mStreamStates[streamType];
                            AudioSystem.initStreamVolume(streamType, 0, (streamState.mIndexMax + 5) / 10);
                            streamState.applyAllVolumes();
                        }
                        AudioService.this.setRingerModeInt(AudioService.this.getRingerModeInternal(), false);
                        AudioService.this.restoreMasterVolume();
                        if (AudioService.this.mMonitorOrientation) {
                            AudioService.this.setOrientationForAudioSystem();
                        }
                        if (AudioService.this.mMonitorRotation) {
                            AudioService.this.setRotationForAudioSystem();
                        }
                        synchronized (AudioService.this.mBluetoothA2dpEnabledLock) {
                            AudioSystem.setForceUse(1, AudioService.this.mBluetoothA2dpEnabled ? 0 : 10);
                        }
                        synchronized (AudioService.this.mSettingsLock) {
                            AudioSystem.setForceUse(3, AudioService.this.mDockAudioMediaEnabled ? 8 : 0);
                        }
                        if (AudioService.this.mHdmiManager != null) {
                            synchronized (AudioService.this.mHdmiManager) {
                                if (AudioService.this.mHdmiTvClient != null) {
                                    AudioService.this.setHdmiSystemAudioSupported(AudioService.this.mHdmiSystemAudioSupported);
                                }
                            }
                        }
                        synchronized (AudioService.this.mAudioPolicies) {
                            for (AudioPolicyProxy policy : AudioService.this.mAudioPolicies.values()) {
                                policy.connectMixes();
                            }
                        }
                        AudioSystem.setParameters("restarting=false");
                        return;
                    }
                    Log.m26e(AudioService.TAG, "Media server died.");
                    AudioService.sendMsg(AudioService.this.mAudioHandler, 4, 1, 0, 0, null, 500);
                    return;
                case 5:
                    onPlaySoundEffect(msg.arg1, msg.arg2);
                    return;
                case 6:
                    synchronized (AudioService.this.mConnectedDevices) {
                        AudioService.this.makeA2dpDeviceUnavailableNow((String) msg.obj);
                    }
                    return;
                case 7:
                    boolean loaded = onLoadSoundEffects();
                    if (msg.obj != null) {
                        LoadSoundEffectReply reply = msg.obj;
                        synchronized (reply) {
                            reply.mStatus = loaded ? 0 : -1;
                            reply.notify();
                        }
                        return;
                    }
                    return;
                case 8:
                case 13:
                    setForceUse(msg.arg1, msg.arg2);
                    return;
                case 9:
                    AudioService.this.resetBluetoothSco();
                    return;
                case 10:
                    setAllVolumes((VolumeStreamState) msg.obj);
                    return;
                case 11:
                    if (!AudioService.this.mUseFixedVolume) {
                        System.putIntForUser(AudioService.this.mContentResolver, System.VOLUME_MASTER_MUTE, msg.arg1, msg.arg2);
                        return;
                    }
                    return;
                case 12:
                    int N = AudioService.this.mRoutesObservers.beginBroadcast();
                    if (N > 0) {
                        AudioRoutesInfo audioRoutesInfo;
                        synchronized (AudioService.this.mCurAudioRoutes) {
                            audioRoutesInfo = new AudioRoutesInfo(AudioService.this.mCurAudioRoutes);
                        }
                        while (N > 0) {
                            N--;
                            try {
                                ((IAudioRoutesObserver) AudioService.this.mRoutesObservers.getBroadcastItem(N)).dispatchAudioRoutesChanged(audioRoutesInfo);
                            } catch (RemoteException e) {
                            }
                        }
                    }
                    AudioService.this.mRoutesObservers.finishBroadcast();
                    return;
                case 14:
                    AudioService.this.onCheckMusicActive();
                    return;
                case 15:
                    AudioService.this.onSendBecomingNoisyIntent();
                    return;
                case 16:
                case 17:
                    AudioService.this.onConfigureSafeVolume(msg.what == 17);
                    return;
                case 18:
                    onPersistSafeVolumeState(msg.arg1);
                    return;
                case 19:
                    AudioService.this.onBroadcastScoConnectionState(msg.arg1);
                    return;
                case 20:
                    onUnloadSoundEffects();
                    return;
                case 21:
                    AudioService.this.onSystemReady();
                    return;
                case 22:
                    Secure.putIntForUser(AudioService.this.mContentResolver, Secure.UNSAFE_VOLUME_MUSIC_ACTIVE_MS, msg.arg1, -2);
                    return;
                case 23:
                    System.putIntForUser(AudioService.this.mContentResolver, System.MICROPHONE_MUTE, msg.arg1, msg.arg2);
                    return;
                case 24:
                    onRingerModeChangedAffectMusic(msg.arg1);
                    return;
                case 25:
                    onMuteChangedAffectMusic(msg.arg1);
                    return;
                case 26:
                    synchronized (AudioService.this.mConnectedDevices) {
                        if (AudioService.this.mConnectedDevices.containsKey(Integer.valueOf(128))) {
                            AudioService.this.makeA2dpDeviceUnavailableNow((String) AudioService.this.mConnectedDevices.get(Integer.valueOf(128)));
                        }
                    }
                    AudioService.this.mAudioEventWakeLock.release();
                    return;
                case 27:
                    System.putIntForUser(AudioService.this.mContentResolver, (String) msg.obj, msg.arg1, -2);
                    return;
                case 100:
                    AudioService.this.onSetWiredDeviceConnectionState(msg.arg1, msg.arg2, (String) msg.obj);
                    AudioService.this.mAudioEventWakeLock.release();
                    return;
                case 101:
                    AudioService.this.onSetA2dpSourceConnectionState((BluetoothDevice) msg.obj, msg.arg1);
                    AudioService.this.mAudioEventWakeLock.release();
                    return;
                case 102:
                    AudioService.this.onSetA2dpSinkConnectionState((BluetoothDevice) msg.obj, msg.arg1);
                    AudioService.this.mAudioEventWakeLock.release();
                    return;
                default:
                    return;
            }
        }
    }

    private class AudioOrientationEventListener extends OrientationEventListener {
        public AudioOrientationEventListener(Context context) {
            super(context);
        }

        public void onOrientationChanged(int orientation) {
            int newRotation = ((WindowManager) AudioService.this.mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            if (newRotation != AudioService.this.mDeviceRotation) {
                AudioService.this.mDeviceRotation = newRotation;
                AudioService.this.setRotationForAudioSystem();
            }
        }
    }

    public class AudioPolicyProxy extends AudioPolicyConfig implements DeathRecipient {
        private static final String TAG = "AudioPolicyProxy";
        AudioPolicyConfig mConfig;
        int mFocusDuckBehavior = 0;
        boolean mHasFocusListener;
        IAudioPolicyCallback mPolicyToken;

        AudioPolicyProxy(AudioPolicyConfig config, IAudioPolicyCallback token, boolean hasFocusListener) {
            super(config);
            setRegistration(new String(config.hashCode() + ":ap:" + AudioService.this.mAudioPolicyCounter = AudioService.this.mAudioPolicyCounter + 1));
            this.mPolicyToken = token;
            this.mHasFocusListener = hasFocusListener;
            if (this.mHasFocusListener) {
                AudioService.this.mMediaFocusControl.addFocusFollower(this.mPolicyToken);
            }
            connectMixes();
        }

        public void binderDied() {
            synchronized (AudioService.this.mAudioPolicies) {
                Log.m28i(TAG, "audio policy " + this.mPolicyToken + " died");
                release();
                AudioService.this.mAudioPolicies.remove(this.mPolicyToken.asBinder());
            }
        }

        String getRegistrationId() {
            return getRegistration();
        }

        void release() {
            if (this.mFocusDuckBehavior == 1) {
                AudioService.this.mMediaFocusControl.setDuckingInExtPolicyAvailable(false);
            }
            if (this.mHasFocusListener) {
                AudioService.this.mMediaFocusControl.removeFocusFollower(this.mPolicyToken);
            }
            AudioSystem.registerPolicyMixes(this.mMixes, false);
        }

        void connectMixes() {
            AudioSystem.registerPolicyMixes(this.mMixes, true);
        }
    }

    private class AudioServiceBroadcastReceiver extends BroadcastReceiver {
        private AudioServiceBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_DOCK_EVENT)) {
                int config;
                int dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, 0);
                switch (dockState) {
                    case 1:
                        config = 7;
                        break;
                    case 2:
                        config = 6;
                        break;
                    case 3:
                        config = 8;
                        break;
                    case 4:
                        config = 9;
                        break;
                    default:
                        config = 0;
                        break;
                }
                if (!(dockState == 3 || (dockState == 0 && AudioService.this.mDockState == 3))) {
                    AudioSystem.setForceUse(3, config);
                }
                AudioService.this.mDockState = dockState;
            } else if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
                int outDevice = 16;
                BluetoothDevice btDevice = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (btDevice != null) {
                    String address = btDevice.getAddress();
                    BluetoothClass btClass = btDevice.getBluetoothClass();
                    if (btClass != null) {
                        switch (btClass.getDeviceClass()) {
                            case 1028:
                            case 1032:
                                outDevice = 32;
                                break;
                            case Device.AUDIO_VIDEO_CAR_AUDIO /*1056*/:
                                outDevice = 64;
                                break;
                        }
                    }
                    if (!BluetoothAdapter.checkBluetoothAddress(address)) {
                        address = ProxyInfo.LOCAL_EXCL_LIST;
                    }
                    boolean connected = state == 2;
                    boolean success = AudioService.this.handleDeviceConnection(connected, outDevice, address) && AudioService.this.handleDeviceConnection(connected, -2147483640, address);
                    if (success) {
                        synchronized (AudioService.this.mScoClients) {
                            if (connected) {
                                AudioService.this.mBluetoothHeadsetDevice = btDevice;
                            } else {
                                AudioService.this.mBluetoothHeadsetDevice = null;
                                AudioService.this.resetBluetoothSco();
                            }
                        }
                    }
                }
            } else if (action.equals(AudioManager.ACTION_USB_AUDIO_ACCESSORY_PLUG)) {
                state = intent.getIntExtra("state", 0);
                alsaCard = intent.getIntExtra("card", -1);
                alsaDevice = intent.getIntExtra(UsbManager.EXTRA_DEVICE, -1);
                params = (alsaCard == -1 && alsaDevice == -1) ? ProxyInfo.LOCAL_EXCL_LIST : "card=" + alsaCard + ";device=" + alsaDevice;
                AudioService.this.setWiredDeviceConnectionState(8192, state, params);
            } else if (action.equals(AudioManager.ACTION_USB_AUDIO_DEVICE_PLUG)) {
                if (Secure.getInt(AudioService.this.mContentResolver, Secure.USB_AUDIO_AUTOMATIC_ROUTING_DISABLED, 0) == 0) {
                    state = intent.getIntExtra("state", 0);
                    alsaCard = intent.getIntExtra("card", -1);
                    alsaDevice = intent.getIntExtra(UsbManager.EXTRA_DEVICE, -1);
                    boolean hasPlayback = intent.getBooleanExtra("hasPlayback", false);
                    boolean hasCapture = intent.getBooleanExtra("hasCapture", false);
                    boolean hasMIDI = intent.getBooleanExtra("hasMIDI", false);
                    if (alsaCard == -1 && alsaDevice == -1) {
                        params = ProxyInfo.LOCAL_EXCL_LIST;
                    } else {
                        params = "card=" + alsaCard + ";device=" + alsaDevice;
                    }
                    if (hasPlayback) {
                        AudioService.this.setWiredDeviceConnectionState(16384, state, params);
                    }
                    if (hasCapture) {
                        AudioService.this.setWiredDeviceConnectionState(-2147479552, state, params);
                    }
                }
            } else if (action.equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)) {
                boolean broadcast = false;
                int scoAudioState = -1;
                synchronized (AudioService.this.mScoClients) {
                    int btState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                    if (!AudioService.this.mScoClients.isEmpty() && (AudioService.this.mScoAudioState == 3 || AudioService.this.mScoAudioState == 1 || AudioService.this.mScoAudioState == 5)) {
                        broadcast = true;
                    }
                    switch (btState) {
                        case 10:
                            scoAudioState = 0;
                            AudioService.this.mScoAudioState = 0;
                            AudioService.this.clearAllScoClients(0, false);
                            break;
                        case 11:
                            if (!(AudioService.this.mScoAudioState == 3 || AudioService.this.mScoAudioState == 5 || AudioService.this.mScoAudioState == 4)) {
                                AudioService.this.mScoAudioState = 2;
                                break;
                            }
                        case 12:
                            scoAudioState = 1;
                            if (!(AudioService.this.mScoAudioState == 3 || AudioService.this.mScoAudioState == 5 || AudioService.this.mScoAudioState == 4)) {
                                AudioService.this.mScoAudioState = 2;
                                break;
                            }
                    }
                    broadcast = false;
                }
                if (broadcast) {
                    AudioService.this.broadcastScoConnectionState(scoAudioState);
                    Intent intent2 = new Intent(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED);
                    intent2.putExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, scoAudioState);
                    AudioService.this.sendStickyBroadcastToAll(intent2);
                }
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                if (AudioService.this.mMonitorRotation) {
                    AudioService.this.mOrientationListener.onOrientationChanged(0);
                    AudioService.this.mOrientationListener.enable();
                }
                AudioSystem.setParameters("screen_state=on");
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                if (AudioService.this.mMonitorRotation) {
                    AudioService.this.mOrientationListener.disable();
                }
                AudioSystem.setParameters("screen_state=off");
            } else if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
                AudioService.this.handleConfigurationChanged(context);
            } else if (action.equals(Intent.ACTION_USER_SWITCHED)) {
                AudioService.sendMsg(AudioService.this.mAudioHandler, 15, 0, 0, 0, null, 0);
                AudioService.this.mMediaFocusControl.discardAudioFocusOwner();
                AudioService.this.readAudioSettings(true);
                AudioService.sendMsg(AudioService.this.mAudioHandler, 10, 2, 0, 0, AudioService.this.mStreamStates[3], 0);
            }
        }
    }

    final class AudioServiceInternal extends AudioManagerInternal {
        AudioServiceInternal() {
        }

        public void setRingerModeDelegate(RingerModeDelegate delegate) {
            AudioService.this.mRingerModeDelegate = delegate;
            if (AudioService.this.mRingerModeDelegate != null) {
                setRingerModeInternal(getRingerModeInternal(), "AudioService.setRingerModeDelegate");
            }
        }

        public void adjustSuggestedStreamVolumeForUid(int streamType, int direction, int flags, String callingPackage, int uid) {
            AudioService.this.adjustSuggestedStreamVolume(direction, streamType, flags, callingPackage, uid);
        }

        public void adjustStreamVolumeForUid(int streamType, int direction, int flags, String callingPackage, int uid) {
            AudioService.this.adjustStreamVolume(streamType, direction, flags, callingPackage, uid);
        }

        public void setStreamVolumeForUid(int streamType, int direction, int flags, String callingPackage, int uid) {
            AudioService.this.setStreamVolume(streamType, direction, flags, callingPackage, uid);
        }

        public void adjustMasterVolumeForUid(int steps, int flags, String callingPackage, int uid) {
            AudioService.this.adjustMasterVolume(steps, flags, callingPackage, uid);
        }

        public int getRingerModeInternal() {
            return AudioService.this.getRingerModeInternal();
        }

        public void setRingerModeInternal(int ringerMode, String caller) {
            AudioService.this.setRingerModeInternal(ringerMode, caller);
        }

        public void setMasterMuteForUid(boolean state, int flags, String callingPackage, IBinder cb, int uid) {
            AudioService.this.setMasterMuteInternal(state, flags, callingPackage, cb, uid);
        }
    }

    private class AudioSystemThread extends Thread {
        AudioSystemThread() {
            super(AudioService.TAG);
        }

        public void run() {
            Looper.prepare();
            synchronized (AudioService.this) {
                AudioService.this.mAudioHandler = new AudioHandler();
                AudioService.this.notify();
            }
            Looper.loop();
        }
    }

    private class ForceControlStreamClient implements DeathRecipient {
        private IBinder mCb;

        ForceControlStreamClient(IBinder cb) {
            if (cb != null) {
                try {
                    cb.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    Log.m32w(AudioService.TAG, "ForceControlStreamClient() could not link to " + cb + " binder death");
                    cb = null;
                }
            }
            this.mCb = cb;
        }

        public void binderDied() {
            synchronized (AudioService.this.mForceControlStreamLock) {
                Log.m32w(AudioService.TAG, "SCO client died");
                if (AudioService.this.mForceControlStreamClient != this) {
                    Log.m32w(AudioService.TAG, "unregistered control stream client died");
                } else {
                    AudioService.this.mForceControlStreamClient = null;
                    AudioService.this.mVolumeControlStream = -1;
                }
            }
        }

        public void release() {
            if (this.mCb != null) {
                this.mCb.unlinkToDeath(this, 0);
                this.mCb = null;
            }
        }
    }

    class LoadSoundEffectReply {
        public int mStatus = 1;

        LoadSoundEffectReply() {
        }
    }

    private class MyDisplayStatusCallback implements DisplayStatusCallback {
        private MyDisplayStatusCallback() {
        }

        public void onComplete(int status) {
            if (AudioService.this.mHdmiManager != null) {
                synchronized (AudioService.this.mHdmiManager) {
                    AudioService.this.mHdmiCecSink = status != -1;
                    if (AudioService.this.isPlatformTelevision() && !AudioService.this.mHdmiCecSink) {
                        AudioService audioService = AudioService.this;
                        audioService.mFixedVolumeDevices &= -1025;
                    }
                    AudioService.this.checkAllFixedVolumeDevices();
                }
            }
        }
    }

    private class RmtSbmxFullVolDeathHandler implements DeathRecipient {
        private IBinder mICallback;

        RmtSbmxFullVolDeathHandler(IBinder cb) {
            this.mICallback = cb;
            try {
                cb.linkToDeath(this, 0);
            } catch (RemoteException e) {
                Log.m27e(AudioService.TAG, "can't link to death", e);
            }
        }

        boolean isHandlerFor(IBinder cb) {
            return this.mICallback.equals(cb);
        }

        void forget() {
            try {
                this.mICallback.unlinkToDeath(this, 0);
            } catch (NoSuchElementException e) {
                Log.m27e(AudioService.TAG, "error unlinking to death", e);
            }
        }

        public void binderDied() {
            Log.m32w(AudioService.TAG, "Recorder with remote submix at full volume died " + this.mICallback);
            AudioService.this.forceRemoteSubmixFullVolume(false, this.mICallback);
        }
    }

    private class ScoClient implements DeathRecipient {
        private IBinder mCb;
        private int mCreatorPid = Binder.getCallingPid();
        private int mStartcount = 0;

        ScoClient(IBinder cb) {
            this.mCb = cb;
        }

        public void binderDied() {
            synchronized (AudioService.this.mScoClients) {
                Log.m32w(AudioService.TAG, "SCO client died");
                if (AudioService.this.mScoClients.indexOf(this) < 0) {
                    Log.m32w(AudioService.TAG, "unregistered SCO client died");
                } else {
                    clearCount(true);
                    AudioService.this.mScoClients.remove(this);
                }
            }
        }

        public void incCount(int scoAudioMode) {
            synchronized (AudioService.this.mScoClients) {
                requestScoState(12, scoAudioMode);
                if (this.mStartcount == 0) {
                    try {
                        this.mCb.linkToDeath(this, 0);
                    } catch (RemoteException e) {
                        Log.m32w(AudioService.TAG, "ScoClient  incCount() could not link to " + this.mCb + " binder death");
                    }
                }
                this.mStartcount++;
            }
        }

        public void decCount() {
            synchronized (AudioService.this.mScoClients) {
                if (this.mStartcount == 0) {
                    Log.m32w(AudioService.TAG, "ScoClient.decCount() already 0");
                } else {
                    this.mStartcount--;
                    if (this.mStartcount == 0) {
                        try {
                            this.mCb.unlinkToDeath(this, 0);
                        } catch (NoSuchElementException e) {
                            Log.m32w(AudioService.TAG, "decCount() going to 0 but not registered to binder");
                        }
                    }
                    requestScoState(10, 0);
                }
            }
        }

        public void clearCount(boolean stopSco) {
            synchronized (AudioService.this.mScoClients) {
                if (this.mStartcount != 0) {
                    try {
                        this.mCb.unlinkToDeath(this, 0);
                    } catch (NoSuchElementException e) {
                        Log.m32w(AudioService.TAG, "clearCount() mStartcount: " + this.mStartcount + " != 0 but not registered to binder");
                    }
                }
                this.mStartcount = 0;
                if (stopSco) {
                    requestScoState(10, 0);
                }
            }
        }

        public int getCount() {
            return this.mStartcount;
        }

        public IBinder getBinder() {
            return this.mCb;
        }

        public int getPid() {
            return this.mCreatorPid;
        }

        public int totalCount() {
            int count;
            synchronized (AudioService.this.mScoClients) {
                count = 0;
                for (int i = 0; i < AudioService.this.mScoClients.size(); i++) {
                    count += ((ScoClient) AudioService.this.mScoClients.get(i)).getCount();
                }
            }
            return count;
        }

        private void requestScoState(int state, int scoAudioMode) {
            AudioService.this.checkScoAudioState();
            if (totalCount() != 0) {
                return;
            }
            boolean status;
            if (state == 12) {
                AudioService.this.broadcastScoConnectionState(2);
                synchronized (AudioService.this.mSetModeDeathHandlers) {
                    if ((!AudioService.this.mSetModeDeathHandlers.isEmpty() && ((SetModeDeathHandler) AudioService.this.mSetModeDeathHandlers.get(0)).getPid() != this.mCreatorPid) || (AudioService.this.mScoAudioState != 0 && AudioService.this.mScoAudioState != 5)) {
                        AudioService.this.broadcastScoConnectionState(0);
                    } else if (AudioService.this.mScoAudioState == 0) {
                        AudioService.this.mScoAudioMode = scoAudioMode;
                        if (scoAudioMode == -1) {
                            if (AudioService.this.mBluetoothHeadsetDevice != null) {
                                AudioService.this.mScoAudioMode = new Integer(Global.getInt(AudioService.this.mContentResolver, "bluetooth_sco_channel_" + AudioService.this.mBluetoothHeadsetDevice.getAddress(), 0)).intValue();
                                if (AudioService.this.mScoAudioMode > 2 || AudioService.this.mScoAudioMode < 0) {
                                    AudioService.this.mScoAudioMode = 0;
                                }
                            } else {
                                AudioService.this.mScoAudioMode = 1;
                            }
                        }
                        if (AudioService.this.mBluetoothHeadset != null && AudioService.this.mBluetoothHeadsetDevice != null) {
                            status = false;
                            if (AudioService.this.mScoAudioMode == 1) {
                                status = AudioService.this.mBluetoothHeadset.connectAudio();
                            } else if (AudioService.this.mScoAudioMode == 0) {
                                status = AudioService.this.mBluetoothHeadset.startScoUsingVirtualVoiceCall(AudioService.this.mBluetoothHeadsetDevice);
                            } else if (AudioService.this.mScoAudioMode == 2) {
                                status = AudioService.this.mBluetoothHeadset.startVoiceRecognition(AudioService.this.mBluetoothHeadsetDevice);
                            }
                            if (status) {
                                AudioService.this.mScoAudioState = 3;
                            } else {
                                AudioService.this.broadcastScoConnectionState(0);
                            }
                        } else if (AudioService.this.getBluetoothHeadset()) {
                            AudioService.this.mScoAudioState = 1;
                        }
                    } else {
                        AudioService.this.mScoAudioState = 3;
                        AudioService.this.broadcastScoConnectionState(1);
                    }
                }
            } else if (state != 10) {
            } else {
                if (AudioService.this.mScoAudioState != 3 && AudioService.this.mScoAudioState != 1) {
                    return;
                }
                if (AudioService.this.mScoAudioState != 3) {
                    AudioService.this.mScoAudioState = 0;
                    AudioService.this.broadcastScoConnectionState(0);
                } else if (AudioService.this.mBluetoothHeadset != null && AudioService.this.mBluetoothHeadsetDevice != null) {
                    status = false;
                    if (AudioService.this.mScoAudioMode == 1) {
                        status = AudioService.this.mBluetoothHeadset.disconnectAudio();
                    } else if (AudioService.this.mScoAudioMode == 0) {
                        boolean isConnected = AudioService.this.mBluetoothHeadset.isAudioConnected(AudioService.this.mBluetoothHeadsetDevice);
                        status = AudioService.this.mBluetoothHeadset.stopScoUsingVirtualVoiceCall(AudioService.this.mBluetoothHeadsetDevice);
                        if (!isConnected) {
                            status = false;
                        }
                    } else if (AudioService.this.mScoAudioMode == 2) {
                        status = AudioService.this.mBluetoothHeadset.stopVoiceRecognition(AudioService.this.mBluetoothHeadsetDevice);
                    }
                    if (!status) {
                        AudioService.this.mScoAudioState = 0;
                        AudioService.this.broadcastScoConnectionState(0);
                    }
                } else if (AudioService.this.getBluetoothHeadset()) {
                    AudioService.this.mScoAudioState = 5;
                }
            }
        }
    }

    private class SetModeDeathHandler implements DeathRecipient {
        private IBinder mCb;
        private int mMode = 0;
        private int mPid;

        SetModeDeathHandler(IBinder cb, int pid) {
            this.mCb = cb;
            this.mPid = pid;
        }

        public void binderDied() {
            int newModeOwnerPid = 0;
            synchronized (AudioService.this.mSetModeDeathHandlers) {
                Log.m32w(AudioService.TAG, "setMode() client died");
                if (AudioService.this.mSetModeDeathHandlers.indexOf(this) < 0) {
                    Log.m32w(AudioService.TAG, "unregistered setMode() client died");
                } else {
                    newModeOwnerPid = AudioService.this.setModeInt(0, this.mCb, this.mPid);
                }
            }
            if (newModeOwnerPid != 0) {
                long ident = Binder.clearCallingIdentity();
                AudioService.this.disconnectBluetoothSco(newModeOwnerPid);
                Binder.restoreCallingIdentity(ident);
            }
        }

        public int getPid() {
            return this.mPid;
        }

        public void setMode(int mode) {
            this.mMode = mode;
        }

        public int getMode() {
            return this.mMode;
        }

        public IBinder getBinder() {
            return this.mCb;
        }
    }

    private class SettingsObserver extends ContentObserver {
        SettingsObserver() {
            super(new Handler());
            AudioService.this.mContentResolver.registerContentObserver(System.getUriFor(System.MODE_RINGER_STREAMS_AFFECTED), false, this);
            AudioService.this.mContentResolver.registerContentObserver(Global.getUriFor(Global.DOCK_AUDIO_MEDIA_ENABLED), false, this);
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            synchronized (AudioService.this.mSettingsLock) {
                if (AudioService.this.updateRingerModeAffectedStreams()) {
                    AudioService.this.setRingerModeInt(AudioService.this.getRingerModeInternal(), false);
                }
                AudioService.this.readDockAudioSettings(AudioService.this.mContentResolver);
            }
        }
    }

    private final class SoundPoolCallback implements OnLoadCompleteListener {
        List<Integer> mSamples;
        int mStatus;

        private SoundPoolCallback() {
            this.mStatus = 1;
            this.mSamples = new ArrayList();
        }

        public int status() {
            return this.mStatus;
        }

        public void setSamples(int[] samples) {
            for (int i = 0; i < samples.length; i++) {
                if (samples[i] > 0) {
                    this.mSamples.add(Integer.valueOf(samples[i]));
                }
            }
        }

        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
            synchronized (AudioService.this.mSoundEffectsLock) {
                int i = this.mSamples.indexOf(Integer.valueOf(sampleId));
                if (i >= 0) {
                    this.mSamples.remove(i);
                }
                if (status != 0 || this.mSamples.isEmpty()) {
                    this.mStatus = status;
                    AudioService.this.mSoundEffectsLock.notify();
                }
            }
        }
    }

    class SoundPoolListenerThread extends Thread {
        public SoundPoolListenerThread() {
            super("SoundPoolListenerThread");
        }

        public void run() {
            Looper.prepare();
            AudioService.this.mSoundPoolLooper = Looper.myLooper();
            synchronized (AudioService.this.mSoundEffectsLock) {
                if (AudioService.this.mSoundPool != null) {
                    AudioService.this.mSoundPoolCallBack = new SoundPoolCallback();
                    AudioService.this.mSoundPool.setOnLoadCompleteListener(AudioService.this.mSoundPoolCallBack);
                }
                AudioService.this.mSoundEffectsLock.notify();
            }
            Looper.loop();
        }
    }

    private static class StreamOverride implements TouchExplorationStateChangeListener {
        private static final int DEFAULT_STREAM_TYPE_OVERRIDE_DELAY_MS = 5000;
        private static final int TOUCH_EXPLORE_STREAM_TYPE_OVERRIDE_DELAY_MS = 1000;
        static int sDelayMs;

        private StreamOverride() {
        }

        static void init(Context ctxt) {
            AccessibilityManager accessibilityManager = (AccessibilityManager) ctxt.getSystemService(Context.ACCESSIBILITY_SERVICE);
            updateDefaultStreamOverrideDelay(accessibilityManager.isTouchExplorationEnabled());
            accessibilityManager.addTouchExplorationStateChangeListener(new StreamOverride());
        }

        public void onTouchExplorationStateChanged(boolean enabled) {
            updateDefaultStreamOverrideDelay(enabled);
        }

        private static void updateDefaultStreamOverrideDelay(boolean touchExploreEnabled) {
            if (touchExploreEnabled) {
                sDelayMs = 1000;
            } else {
                sDelayMs = 5000;
            }
            AudioService.log("Touch exploration enabled=" + touchExploreEnabled + " stream override delay is now " + sDelayMs + " ms");
        }
    }

    class StreamVolumeCommand {
        public final int mDevice;
        public final int mFlags;
        public final int mIndex;
        public final int mStreamType;

        StreamVolumeCommand(int streamType, int index, int flags, int device) {
            this.mStreamType = streamType;
            this.mIndex = index;
            this.mFlags = flags;
            this.mDevice = device;
        }

        public String toString() {
            return "{streamType=" + this.mStreamType + ",index=" + this.mIndex + ",flags=" + this.mFlags + ",device=" + this.mDevice + '}';
        }
    }

    public static class VolumeController {
        private static final String TAG = "VolumeController";
        private IVolumeController mController;
        private int mLongPressTimeout;
        private long mNextLongPress;
        private boolean mVisible;

        public void setController(IVolumeController controller) {
            this.mController = controller;
            this.mVisible = false;
        }

        public void loadSettings(ContentResolver cr) {
            this.mLongPressTimeout = Secure.getIntForUser(cr, Secure.LONG_PRESS_TIMEOUT, 500, -2);
        }

        public boolean suppressAdjustment(int resolvedStream, int flags) {
            if (resolvedStream != 2 || this.mController == null) {
                return false;
            }
            long now = SystemClock.uptimeMillis();
            if ((flags & 1) != 0 && !this.mVisible) {
                if (this.mNextLongPress < now) {
                    this.mNextLongPress = ((long) this.mLongPressTimeout) + now;
                }
                return true;
            } else if (this.mNextLongPress <= 0) {
                return false;
            } else {
                if (now <= this.mNextLongPress) {
                    return true;
                }
                this.mNextLongPress = 0;
                return false;
            }
        }

        public void setVisible(boolean visible) {
            this.mVisible = visible;
        }

        public boolean isSameBinder(IVolumeController controller) {
            return Objects.equals(asBinder(), binder(controller));
        }

        public IBinder asBinder() {
            return binder(this.mController);
        }

        private static IBinder binder(IVolumeController controller) {
            return controller == null ? null : controller.asBinder();
        }

        public String toString() {
            return "VolumeController(" + asBinder() + ",mVisible=" + this.mVisible + ")";
        }

        public void postDisplaySafeVolumeWarning(int flags) {
            if (this.mController != null) {
                try {
                    this.mController.displaySafeVolumeWarning(flags);
                } catch (RemoteException e) {
                    Log.m33w(TAG, "Error calling displaySafeVolumeWarning", e);
                }
            }
        }

        public void postVolumeChanged(int streamType, int flags) {
            AudioService.log("[GJ_DEBUG] postVolumeChanged: streamType = " + streamType);
            if (this.mController == null) {
                AudioService.log("[GJ_DEBUG] postVolumeChanged: mController == null");
                return;
            }
            try {
                this.mController.volumeChanged(streamType, flags);
            } catch (RemoteException e) {
                Log.m33w(TAG, "Error calling volumeChanged", e);
            }
        }

        public void postMasterVolumeChanged(int flags) {
            if (this.mController != null) {
                try {
                    this.mController.masterVolumeChanged(flags);
                } catch (RemoteException e) {
                    Log.m33w(TAG, "Error calling masterVolumeChanged", e);
                }
            }
        }

        public void postMasterMuteChanged(int flags) {
            if (this.mController != null) {
                try {
                    this.mController.masterMuteChanged(flags);
                } catch (RemoteException e) {
                    Log.m33w(TAG, "Error calling masterMuteChanged", e);
                }
            }
        }

        public void setLayoutDirection(int layoutDirection) {
            if (this.mController != null) {
                try {
                    this.mController.setLayoutDirection(layoutDirection);
                } catch (RemoteException e) {
                    Log.m33w(TAG, "Error calling setLayoutDirection", e);
                }
            }
        }

        public void postDismiss() {
            if (this.mController != null) {
                try {
                    this.mController.dismiss();
                } catch (RemoteException e) {
                    Log.m33w(TAG, "Error calling dismiss", e);
                }
            }
        }
    }

    public class VolumeStreamState {
        private ArrayList<VolumeDeathHandler> mDeathHandlers;
        private final ConcurrentHashMap<Integer, Integer> mIndex;
        private int mIndexMax;
        private final int mStreamType;
        private String mVolumeIndexSettingName;

        private class VolumeDeathHandler implements DeathRecipient {
            private IBinder mICallback;
            private int mMuteCount;

            VolumeDeathHandler(IBinder cb) {
                this.mICallback = cb;
            }

            public void mute_syncVSS(boolean state) {
                boolean updateVolume = false;
                if (state) {
                    if (this.mMuteCount == 0) {
                        try {
                            if (this.mICallback != null) {
                                this.mICallback.linkToDeath(this, 0);
                            }
                            VolumeStreamState.this.mDeathHandlers.add(this);
                            if (!VolumeStreamState.this.isMuted_syncVSS()) {
                                updateVolume = true;
                            }
                        } catch (RemoteException e) {
                            binderDied();
                            return;
                        }
                    }
                    Log.m32w(AudioService.TAG, "stream: " + VolumeStreamState.this.mStreamType + " was already muted by this client");
                    this.mMuteCount++;
                } else if (this.mMuteCount == 0) {
                    Log.m26e(AudioService.TAG, "unexpected unmute for stream: " + VolumeStreamState.this.mStreamType);
                } else {
                    this.mMuteCount--;
                    if (this.mMuteCount == 0) {
                        VolumeStreamState.this.mDeathHandlers.remove(this);
                        if (this.mICallback != null) {
                            this.mICallback.unlinkToDeath(this, 0);
                        }
                        if (!VolumeStreamState.this.isMuted_syncVSS()) {
                            updateVolume = true;
                        }
                    }
                }
                if (updateVolume) {
                    AudioService.sendMsg(AudioService.this.mAudioHandler, 10, 2, 0, 0, VolumeStreamState.this, 0);
                }
            }

            public void binderDied() {
                Log.m32w(AudioService.TAG, "Volume service client died for stream: " + VolumeStreamState.this.mStreamType);
                synchronized (VolumeStreamState.class) {
                    if (this.mMuteCount != 0) {
                        this.mMuteCount = 1;
                        mute_syncVSS(false);
                    }
                }
            }
        }

        private VolumeStreamState(String settingName, int streamType) {
            this.mIndex = new ConcurrentHashMap(8, 0.75f, 4);
            this.mVolumeIndexSettingName = settingName;
            this.mStreamType = streamType;
            this.mIndexMax = AudioService.MAX_STREAM_VOLUME[streamType];
            AudioSystem.initStreamVolume(streamType, 0, this.mIndexMax);
            this.mIndexMax *= 10;
            this.mDeathHandlers = new ArrayList();
            readSettings();
        }

        public String getSettingNameForDevice(int device) {
            String name = this.mVolumeIndexSettingName;
            String suffix = AudioSystem.getOutputDeviceName(device);
            return suffix.isEmpty() ? name : name + "_" + suffix;
        }

        public void readSettings() {
            synchronized (VolumeStreamState.class) {
                if (AudioService.this.mUseFixedVolume || AudioService.this.mUseMasterVolume) {
                    this.mIndex.put(Integer.valueOf(1073741824), Integer.valueOf(this.mIndexMax));
                } else if (this.mStreamType == 1 || this.mStreamType == 7) {
                    index = AudioService.DEFAULT_STREAM_VOLUME[this.mStreamType] * 10;
                    synchronized (AudioService.this.mCameraSoundForced) {
                        if (AudioService.this.mCameraSoundForced.booleanValue()) {
                            index = this.mIndexMax;
                        }
                    }
                    this.mIndex.put(Integer.valueOf(1073741824), Integer.valueOf(index));
                } else {
                    int remainingDevices = AudioSystem.DEVICE_OUT_ALL;
                    int i = 0;
                    while (remainingDevices != 0) {
                        int device = 1 << i;
                        if ((device & remainingDevices) != 0) {
                            remainingDevices &= device ^ -1;
                            String name = getSettingNameForDevice(device);
                            index = System.getIntForUser(AudioService.this.mContentResolver, name, device == 1073741824 ? AudioService.DEFAULT_STREAM_VOLUME[this.mStreamType] : -1, -2);
                            if (index == -1) {
                                if (device == 1 && this.mStreamType == 3) {
                                    index = (int) (((double) AudioService.MAX_STREAM_VOLUME[this.mStreamType]) * 0.8d);
                                    this.mIndex.put(Integer.valueOf(device), Integer.valueOf(getValidIndex(index * 10)));
                                    System.putIntForUser(AudioService.this.mContentResolver, name, index, -2);
                                }
                                if ((device == 2 && this.mStreamType == 2) || (device == 2 && this.mStreamType == 3)) {
                                    this.mIndex.put(Integer.valueOf(device), Integer.valueOf(getValidIndex(AudioService.DEFAULT_STREAM_VOLUME[this.mStreamType] * 10)));
                                }
                            } else if (AudioService.this.mStreamVolumeAlias[this.mStreamType] != 3 || (AudioService.this.mFixedVolumeDevices & device) == 0) {
                                this.mIndex.put(Integer.valueOf(device), Integer.valueOf(getValidIndex(index * 10)));
                            } else {
                                this.mIndex.put(Integer.valueOf(device), Integer.valueOf(this.mIndexMax));
                            }
                        }
                        i++;
                    }
                }
            }
        }

        public void applyDeviceVolume_syncVSS(int device) {
            int index;
            if (isMuted_syncVSS()) {
                index = 0;
            } else if (((device & AudioSystem.DEVICE_OUT_ALL_A2DP) == 0 || !AudioService.this.mAvrcpAbsVolSupported) && (AudioService.this.mFullVolumeDevices & device) == 0) {
                index = (getIndex(device) + 5) / 10;
            } else {
                index = (this.mIndexMax + 5) / 10;
            }
            AudioService.log("applyDeviceVolumemStreamType:" + this.mStreamType + ",device:" + device);
            AudioSystem.setStreamVolumeIndex(this.mStreamType, index, device);
        }

        public void applyAllVolumes() {
            synchronized (VolumeStreamState.class) {
                int index;
                if (isMuted_syncVSS()) {
                    index = 0;
                } else {
                    index = (getIndex(1073741824) + 5) / 10;
                }
                AudioSystem.setStreamVolumeIndex(this.mStreamType, index, 1073741824);
                for (Entry entry : this.mIndex.entrySet()) {
                    int device = ((Integer) entry.getKey()).intValue();
                    if (device != 1073741824) {
                        if (isMuted_syncVSS()) {
                            index = 0;
                        } else if (((device & AudioSystem.DEVICE_OUT_ALL_A2DP) == 0 || !AudioService.this.mAvrcpAbsVolSupported) && (AudioService.this.mFullVolumeDevices & device) == 0) {
                            index = (((Integer) entry.getValue()).intValue() + 5) / 10;
                        } else {
                            index = (this.mIndexMax + 5) / 10;
                        }
                        AudioSystem.setStreamVolumeIndex(this.mStreamType, index, device);
                    }
                }
            }
        }

        public boolean adjustIndex(int deltaIndex, int device) {
            return setIndex(getIndex(device) + deltaIndex, device);
        }

        public boolean setIndex(int index, int device) {
            synchronized (VolumeStreamState.class) {
                int oldIndex = getIndex(device);
                index = getValidIndex(index);
                synchronized (AudioService.this.mCameraSoundForced) {
                    if (this.mStreamType == 7 && AudioService.this.mCameraSoundForced.booleanValue()) {
                        index = this.mIndexMax;
                    }
                }
                this.mIndex.put(Integer.valueOf(device), Integer.valueOf(index));
                if (oldIndex != index) {
                    boolean currentDevice;
                    if (device == AudioService.this.getDeviceForStream(this.mStreamType)) {
                        currentDevice = true;
                    } else {
                        currentDevice = false;
                    }
                    int streamType = AudioSystem.getNumStreamTypes() - 1;
                    while (streamType >= 0) {
                        if (streamType != this.mStreamType && AudioService.this.mStreamVolumeAlias[streamType] == this.mStreamType) {
                            int scaledIndex = AudioService.this.rescaleIndex(index, this.mStreamType, streamType);
                            AudioService.this.mStreamStates[streamType].setIndex(scaledIndex, device);
                            if (currentDevice) {
                                AudioService.this.mStreamStates[streamType].setIndex(scaledIndex, AudioService.this.getDeviceForStream(streamType));
                            }
                        }
                        streamType--;
                    }
                    return true;
                }
                return false;
            }
        }

        public int getIndex(int device) {
            int intValue;
            synchronized (VolumeStreamState.class) {
                Integer index = (Integer) this.mIndex.get(Integer.valueOf(device));
                if (index == null) {
                    index = (Integer) this.mIndex.get(Integer.valueOf(1073741824));
                }
                intValue = index.intValue();
            }
            return intValue;
        }

        public int getMaxIndex() {
            return this.mIndexMax;
        }

        public void setAllIndexes(VolumeStreamState srcStream) {
            synchronized (VolumeStreamState.class) {
                int srcStreamType = srcStream.getStreamType();
                int index = AudioService.this.rescaleIndex(srcStream.getIndex(1073741824), srcStreamType, this.mStreamType);
                for (Entry entry : this.mIndex.entrySet()) {
                    entry.setValue(Integer.valueOf(index));
                }
                for (Entry entry2 : srcStream.mIndex.entrySet()) {
                    setIndex(AudioService.this.rescaleIndex(((Integer) entry2.getValue()).intValue(), srcStreamType, this.mStreamType), ((Integer) entry2.getKey()).intValue());
                }
            }
        }

        public void setAllIndexesToMax() {
            synchronized (VolumeStreamState.class) {
                for (Entry entry : this.mIndex.entrySet()) {
                    entry.setValue(Integer.valueOf(this.mIndexMax));
                }
            }
        }

        public void setAllIndexesForMusic(String prefix, boolean setToMin, boolean byRingerMode) {
            synchronized (VolumeStreamState.class) {
                for (Entry entry : this.mIndex.entrySet()) {
                    int device = ((Integer) entry.getKey()).intValue();
                    if (this.mStreamType == 3 && ((byRingerMode && device == 2) || !byRingerMode)) {
                        int index;
                        int defaultIndex = AudioService.DEFAULT_STREAM_VOLUME[this.mStreamType];
                        String name = getSettingNameForDevice(device);
                        if (setToMin) {
                            System.putIntForUser(AudioService.this.mContentResolver, prefix + name, System.getIntForUser(AudioService.this.mContentResolver, name, defaultIndex, -2), -2);
                            index = 0;
                        } else {
                            index = System.getIntForUser(AudioService.this.mContentResolver, prefix + name, defaultIndex, -2);
                        }
                        System.putIntForUser(AudioService.this.mContentResolver, name, index, -2);
                        entry.setValue(Integer.valueOf(getValidIndex(index * 10)));
                    }
                }
            }
        }

        public void mute(IBinder cb, boolean state) {
            synchronized (VolumeStreamState.class) {
                VolumeDeathHandler handler = getDeathHandler_syncVSS(cb, state);
                if (handler == null) {
                    Log.m26e(AudioService.TAG, "Could not get client death handler for stream: " + this.mStreamType);
                    return;
                }
                handler.mute_syncVSS(state);
            }
        }

        public int getStreamType() {
            return this.mStreamType;
        }

        public void checkFixedVolumeDevices() {
            synchronized (VolumeStreamState.class) {
                if (AudioService.this.mStreamVolumeAlias[this.mStreamType] == 3) {
                    for (Entry entry : this.mIndex.entrySet()) {
                        int device = ((Integer) entry.getKey()).intValue();
                        int index = ((Integer) entry.getValue()).intValue();
                        if (!((AudioService.this.mFullVolumeDevices & device) == 0 && ((AudioService.this.mFixedVolumeDevices & device) == 0 || index == 0))) {
                            entry.setValue(Integer.valueOf(this.mIndexMax));
                        }
                        applyDeviceVolume_syncVSS(device);
                    }
                }
            }
        }

        private int getValidIndex(int index) {
            if (index < 0) {
                return 0;
            }
            if (AudioService.this.mUseFixedVolume || AudioService.this.mUseMasterVolume || index > this.mIndexMax) {
                return this.mIndexMax;
            }
            return index;
        }

        private int muteCount() {
            int count = 0;
            for (int i = 0; i < this.mDeathHandlers.size(); i++) {
                count += ((VolumeDeathHandler) this.mDeathHandlers.get(i)).mMuteCount;
            }
            return count;
        }

        private boolean isMuted_syncVSS() {
            return muteCount() != 0;
        }

        private VolumeDeathHandler getDeathHandler_syncVSS(IBinder cb, boolean state) {
            VolumeDeathHandler handler;
            int size = this.mDeathHandlers.size();
            for (int i = 0; i < size; i++) {
                handler = (VolumeDeathHandler) this.mDeathHandlers.get(i);
                if (cb == handler.mICallback) {
                    return handler;
                }
            }
            if (state) {
                handler = new VolumeDeathHandler(cb);
            } else {
                Log.m32w(AudioService.TAG, "stream was not muted by this client");
                handler = null;
            }
            return handler;
        }

        private void dump(PrintWriter pw) {
            pw.print("   Mute count: ");
            pw.println(muteCount());
            pw.print("   Max: ");
            pw.println((this.mIndexMax + 5) / 10);
            pw.print("   Current: ");
            Iterator i = this.mIndex.entrySet().iterator();
            while (i.hasNext()) {
                Entry entry = (Entry) i.next();
                int device = ((Integer) entry.getKey()).intValue();
                pw.print(Integer.toHexString(device));
                String deviceName = device == 1073741824 ? PhoneConstants.APN_TYPE_DEFAULT : AudioSystem.getOutputDeviceName(device);
                if (!deviceName.isEmpty()) {
                    pw.print(" (");
                    pw.print(deviceName);
                    pw.print(")");
                }
                pw.print(": ");
                pw.print((((Integer) entry.getValue()).intValue() + 5) / 10);
                if (i.hasNext()) {
                    pw.print(", ");
                }
            }
        }
    }

    private void loadTouchSoundAssets() {
        /* JADX: method processing error */
/*
Error: java.util.NoSuchElementException
	at java.util.HashMap$HashIterator.nextNode(HashMap.java:1431)
	at java.util.HashMap$KeyIterator.next(HashMap.java:1453)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.applyRemove(BlockFinallyExtract.java:535)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.extractFinally(BlockFinallyExtract.java:175)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.processExceptionHandler(BlockFinallyExtract.java:79)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.visit(BlockFinallyExtract.java:51)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:37)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:306)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler$1.run(JadxDecompiler.java:199)
*/
        /*
        r14 = this;
        r9 = 0;
        r11 = SOUND_EFFECT_FILES;
        r11 = r11.isEmpty();
        if (r11 != 0) goto L_0x000a;
    L_0x0009:
        return;
    L_0x000a:
        r14.loadTouchSoundAssetDefaults();
        r11 = r14.mContext;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r11 = r11.getResources();	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r12 = 17891329; // 0x1110001 float:2.6632297E-38 double:8.839491E-317;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r9 = r11.getXml(r12);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r11 = "audio_assets";	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        com.android.internal.util.XmlUtils.beginDocument(r9, r11);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r11 = 0;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r12 = "version";	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r10 = r9.getAttributeValue(r11, r12);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r7 = 0;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r11 = "1.0";	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r11 = r11.equals(r10);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        if (r11 == 0) goto L_0x0044;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
    L_0x0030:
        com.android.internal.util.XmlUtils.nextElement(r9);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r1 = r9.getName();	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        if (r1 != 0) goto L_0x004a;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
    L_0x0039:
        if (r7 == 0) goto L_0x0044;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
    L_0x003b:
        com.android.internal.util.XmlUtils.nextElement(r9);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r1 = r9.getName();	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        if (r1 != 0) goto L_0x0065;
    L_0x0044:
        if (r9 == 0) goto L_0x0009;
    L_0x0046:
        r9.close();
        goto L_0x0009;
    L_0x004a:
        r11 = "group";	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r11 = r1.equals(r11);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        if (r11 == 0) goto L_0x0030;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
    L_0x0052:
        r11 = 0;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r12 = "name";	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r8 = r9.getAttributeValue(r11, r12);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r11 = "touch_sounds";	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r11 = r11.equals(r8);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        if (r11 == 0) goto L_0x0030;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
    L_0x0063:
        r7 = 1;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        goto L_0x0039;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
    L_0x0065:
        r11 = "asset";	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r11 = r1.equals(r11);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        if (r11 == 0) goto L_0x0044;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
    L_0x006d:
        r11 = 0;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r12 = "id";	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r6 = r9.getAttributeValue(r11, r12);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r11 = 0;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r12 = "file";	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r3 = r9.getAttributeValue(r11, r12);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r11 = android.media.AudioManager.class;	 Catch:{ Exception -> 0x00b1 }
        r2 = r11.getField(r6);	 Catch:{ Exception -> 0x00b1 }
        r11 = 0;	 Catch:{ Exception -> 0x00b1 }
        r4 = r2.getInt(r11);	 Catch:{ Exception -> 0x00b1 }
        r11 = SOUND_EFFECT_FILES;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r5 = r11.indexOf(r3);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r11 = -1;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        if (r5 != r11) goto L_0x009a;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
    L_0x008f:
        r11 = SOUND_EFFECT_FILES;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r5 = r11.size();	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r11 = SOUND_EFFECT_FILES;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r11.add(r3);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
    L_0x009a:
        r11 = r14.SOUND_EFFECT_FILES_MAP;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r11 = r11[r4];	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r12 = 0;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r11[r12] = r5;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        goto L_0x0039;
    L_0x00a2:
        r0 = move-exception;
        r11 = "AudioService";	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r12 = "audio assets file not found";	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        android.util.Log.m33w(r11, r12, r0);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        if (r9 == 0) goto L_0x0009;
    L_0x00ac:
        r9.close();
        goto L_0x0009;
    L_0x00b1:
        r0 = move-exception;
        r11 = "AudioService";	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r12 = new java.lang.StringBuilder;	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r12.<init>();	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r13 = "Invalid touch sound ID: ";	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r12 = r12.append(r13);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r12 = r12.append(r6);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r12 = r12.toString();	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        android.util.Log.m32w(r11, r12);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        goto L_0x0039;
    L_0x00cc:
        r0 = move-exception;
        r11 = "AudioService";	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r12 = "XML parser exception reading touch sound assets";	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        android.util.Log.m33w(r11, r12, r0);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        if (r9 == 0) goto L_0x0009;
    L_0x00d6:
        r9.close();
        goto L_0x0009;
    L_0x00db:
        r0 = move-exception;
        r11 = "AudioService";	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        r12 = "I/O exception reading touch sound assets";	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        android.util.Log.m33w(r11, r12, r0);	 Catch:{ NotFoundException -> 0x00a2, XmlPullParserException -> 0x00cc, IOException -> 0x00db, all -> 0x00ea }
        if (r9 == 0) goto L_0x0009;
    L_0x00e5:
        r9.close();
        goto L_0x0009;
    L_0x00ea:
        r11 = move-exception;
        if (r9 == 0) goto L_0x00f0;
    L_0x00ed:
        r9.close();
    L_0x00f0:
        throw r11;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.media.AudioService.loadTouchSoundAssets():void");
    }

    private boolean isPlatformVoice() {
        return this.mPlatformType == 1;
    }

    private boolean isPlatformTelevision() {
        return this.mPlatformType == 2;
    }

    public AudioService(Context context) {
        int i;
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mAppOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (this.mContext.getResources().getBoolean(C0833R.bool.config_voice_capable)) {
            this.mPlatformType = 1;
        } else if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            this.mPlatformType = 2;
        } else {
            this.mPlatformType = 0;
        }
        this.mAudioEventWakeLock = ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).newWakeLock(1, "handleAudioEvent");
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        this.mHasVibrator = vibrator == null ? false : vibrator.hasVibrator();
        int maxVolume = SystemProperties.getInt("ro.config.vc_call_vol_steps", MAX_STREAM_VOLUME[0]);
        if (maxVolume != MAX_STREAM_VOLUME[0]) {
            MAX_STREAM_VOLUME[0] = maxVolume;
            DEFAULT_STREAM_VOLUME[0] = (maxVolume * 3) / 4;
        }
        maxVolume = SystemProperties.getInt("ro.config.media_vol_steps", MAX_STREAM_VOLUME[3]);
        if (maxVolume != MAX_STREAM_VOLUME[3]) {
            MAX_STREAM_VOLUME[3] = maxVolume;
            DEFAULT_STREAM_VOLUME[3] = (maxVolume * 3) / 4;
        }
        sSoundEffectVolumeDb = context.getResources().getInteger(C0833R.integer.config_soundEffectVolumeDb);
        this.mForcedUseForComm = 0;
        createAudioSystemThread();
        this.mMediaFocusControl = new MediaFocusControl(this.mAudioHandler.getLooper(), this.mContext, this.mVolumeController, this);
        AudioSystem.setErrorCallback(this.mAudioSystemCallback);
        boolean cameraSoundForced = this.mContext.getResources().getBoolean(C0833R.bool.config_camera_sound_forced);
        this.mCameraSoundForced = new Boolean(cameraSoundForced);
        Handler handler = this.mAudioHandler;
        if (cameraSoundForced) {
            i = 11;
        } else {
            i = 0;
        }
        sendMsg(handler, 8, 2, 4, i, null, 0);
        this.mSafeMediaVolumeState = new Integer(Global.getInt(this.mContentResolver, Global.AUDIO_SAFE_VOLUME_STATE, 0));
        this.mSafeMediaVolumeIndex = this.mContext.getResources().getInteger(C0833R.integer.config_safe_media_volume_index) * 10;
        this.mUseFixedVolume = this.mContext.getResources().getBoolean(C0833R.bool.config_useFixedVolume);
        this.mUseMasterVolume = context.getResources().getBoolean(C0833R.bool.config_useMasterVolume);
        this.mMasterVolumeRamp = context.getResources().getIntArray(C0833R.array.config_masterVolumeRamp);
        updateStreamVolumeAlias(false);
        readPersistedSettings();
        this.mSettingsObserver = new SettingsObserver();
        createStreamStates();
        readAndSetLowRamDevice();
        MzReadAndSetHifiParam();
        this.mRingerModeMutedStreams = 0;
        setRingerModeInt(getRingerModeInternal(), false);
        IntentFilter intentFilter = new IntentFilter(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(Intent.ACTION_DOCK_EVENT);
        intentFilter.addAction(AudioManager.ACTION_USB_AUDIO_ACCESSORY_PLUG);
        intentFilter.addAction(AudioManager.ACTION_USB_AUDIO_DEVICE_PLUG);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_USER_SWITCHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        this.mMonitorOrientation = SystemProperties.getBoolean("ro.audio.monitorOrientation", false);
        if (this.mMonitorOrientation) {
            Log.m30v(TAG, "monitoring device orientation");
            setOrientationForAudioSystem();
        }
        this.mMonitorRotation = SystemProperties.getBoolean("ro.audio.monitorRotation", false);
        if (this.mMonitorRotation) {
            this.mDeviceRotation = ((WindowManager) this.mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            Log.m30v(TAG, "monitoring device rotation, initial=" + this.mDeviceRotation);
            this.mOrientationListener = new AudioOrientationEventListener(this.mContext);
            this.mOrientationListener.enable();
            setRotationForAudioSystem();
        }
        context.registerReceiver(this.mReceiver, intentFilter);
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        restoreMasterVolume();
        LocalServices.addService(AudioManagerInternal.class, new AudioServiceInternal());
        ((TelephonyManager) this.mContext.getSystemService("phone")).listen(this.mPhoneStateListener, 32);
    }

    public void systemReady() {
        sendMsg(this.mAudioHandler, 21, 2, 0, 0, null, 0);
    }

    public void onSystemReady() {
        this.mSystemReady = true;
        sendMsg(this.mAudioHandler, 7, 2, 0, 0, null, 0);
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService(Context.KEYGUARD_SERVICE);
        this.mScoConnectionState = -1;
        resetBluetoothSco();
        getBluetoothHeadset();
        Intent newIntent = new Intent(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED);
        newIntent.putExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, 0);
        sendStickyBroadcastToAll(newIntent);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(this.mContext, this.mBluetoothProfileServiceListener, 2);
        }
        this.mHdmiManager = (HdmiControlManager) this.mContext.getSystemService(Context.HDMI_CONTROL_SERVICE);
        if (this.mHdmiManager != null) {
            synchronized (this.mHdmiManager) {
                this.mHdmiTvClient = this.mHdmiManager.getTvClient();
                if (this.mHdmiTvClient != null) {
                    this.mFixedVolumeDevices &= -2883587;
                }
                this.mHdmiPlaybackClient = this.mHdmiManager.getPlaybackClient();
                this.mHdmiCecSink = false;
            }
        }
        sendMsg(this.mAudioHandler, 17, 0, 0, 0, null, 30000);
        StreamOverride.init(this.mContext);
    }

    private void createAudioSystemThread() {
        this.mAudioSystemThread = new AudioSystemThread();
        this.mAudioSystemThread.start();
        waitForAudioHandlerCreation();
    }

    private void waitForAudioHandlerCreation() {
        synchronized (this) {
            while (this.mAudioHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Log.m26e(TAG, "Interrupted while waiting on volume handler.");
                }
            }
        }
    }

    private void checkAllAliasStreamVolumes() {
        synchronized (VolumeStreamState.class) {
            int numStreamTypes = AudioSystem.getNumStreamTypes();
            for (int streamType = 0; streamType < numStreamTypes; streamType++) {
                if (streamType != this.mStreamVolumeAlias[streamType]) {
                    this.mStreamStates[streamType].setAllIndexes(this.mStreamStates[this.mStreamVolumeAlias[streamType]]);
                }
                if (!this.mStreamStates[streamType].isMuted_syncVSS()) {
                    this.mStreamStates[streamType].applyAllVolumes();
                }
            }
        }
    }

    private void checkAllFixedVolumeDevices() {
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        for (int streamType = 0; streamType < numStreamTypes; streamType++) {
            this.mStreamStates[streamType].checkFixedVolumeDevices();
        }
    }

    private void checkAllFixedVolumeDevices(int streamType) {
        this.mStreamStates[streamType].checkFixedVolumeDevices();
    }

    private void createStreamStates() {
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        VolumeStreamState[] streams = new VolumeStreamState[numStreamTypes];
        this.mStreamStates = streams;
        for (int i = 0; i < numStreamTypes; i++) {
            streams[i] = new VolumeStreamState(System.VOLUME_SETTINGS[this.mStreamVolumeAlias[i]], i);
        }
        checkAllFixedVolumeDevices();
        checkAllAliasStreamVolumes();
    }

    private void dumpStreamStates(PrintWriter pw) {
        pw.println("\nStream volumes (device: index)");
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        for (int i = 0; i < numStreamTypes; i++) {
            pw.println("- " + STREAM_NAMES[i] + DateTimeUrlHelper.COLON_STRING);
            this.mStreamStates[i].dump(pw);
            pw.println(ProxyInfo.LOCAL_EXCL_LIST);
        }
        pw.print("\n- mute affected streams = 0x");
        pw.println(Integer.toHexString(this.mMuteAffectedStreams));
    }

    public static String streamToString(int stream) {
        if (stream >= 0 && stream < STREAM_NAMES.length) {
            return STREAM_NAMES[stream];
        }
        if (stream == Integer.MIN_VALUE) {
            return "USE_DEFAULT_STREAM_TYPE";
        }
        return "UNKNOWN_STREAM_" + stream;
    }

    private void updateStreamVolumeAlias(boolean updateVolumes) {
        int dtmfStreamAlias;
        switch (this.mPlatformType) {
            case 1:
                this.mStreamVolumeAlias = this.STREAM_VOLUME_ALIAS_VOICE;
                dtmfStreamAlias = 5;
                break;
            case 2:
                this.mStreamVolumeAlias = this.STREAM_VOLUME_ALIAS_TELEVISION;
                dtmfStreamAlias = 3;
                break;
            default:
                this.mStreamVolumeAlias = this.STREAM_VOLUME_ALIAS_DEFAULT;
                dtmfStreamAlias = 3;
                break;
        }
        if (isPlatformTelevision()) {
            this.mRingerModeAffectedStreams = 0;
        } else if (isInCommunication()) {
            dtmfStreamAlias = 0;
            this.mRingerModeAffectedStreams &= -257;
        } else {
            this.mRingerModeAffectedStreams |= 256;
        }
        this.mStreamVolumeAlias[8] = dtmfStreamAlias;
        if (updateVolumes) {
            this.mStreamStates[8].setAllIndexes(this.mStreamStates[dtmfStreamAlias]);
            setRingerModeInt(getRingerModeInternal(), false);
            sendMsg(this.mAudioHandler, 10, 2, 0, 0, this.mStreamStates[8], 0);
        }
    }

    private void readDockAudioSettings(ContentResolver cr) {
        int i;
        boolean z = true;
        if (Global.getInt(cr, Global.DOCK_AUDIO_MEDIA_ENABLED, 0) != 1) {
            z = false;
        }
        this.mDockAudioMediaEnabled = z;
        if (this.mDockAudioMediaEnabled) {
            this.mBecomingNoisyIntentDevices |= 2048;
        } else {
            this.mBecomingNoisyIntentDevices &= -2049;
        }
        Handler handler = this.mAudioHandler;
        if (this.mDockAudioMediaEnabled) {
            i = 8;
        } else {
            i = 0;
        }
        sendMsg(handler, 8, 2, 3, i, null, 0);
    }

    private void readPersistedSettings() {
        boolean masterMute;
        boolean microphoneMute;
        int i = 2;
        ContentResolver cr = this.mContentResolver;
        int ringerModeFromSettings = Global.getInt(cr, "mode_ringer", 2);
        int ringerMode = ringerModeFromSettings;
        if (!isValidRingerMode(ringerMode)) {
            ringerMode = 2;
        }
        if (ringerMode == 1 && !this.mHasVibrator) {
            ringerMode = 0;
        }
        if (ringerMode != ringerModeFromSettings) {
            Global.putInt(cr, "mode_ringer", ringerMode);
        }
        if (this.mUseFixedVolume || isPlatformTelevision()) {
            ringerMode = 2;
        }
        synchronized (this.mSettingsLock) {
            int i2;
            this.mRingerMode = ringerMode;
            if (this.mRingerModeExternal == -1) {
                this.mRingerModeExternal = this.mRingerMode;
            }
            if (this.mHasVibrator) {
                i2 = 2;
            } else {
                i2 = 0;
            }
            this.mVibrateSetting = getValueForVibrateSetting(0, 1, i2);
            i2 = this.mVibrateSetting;
            if (!this.mHasVibrator) {
                i = 0;
            }
            this.mVibrateSetting = getValueForVibrateSetting(i2, 0, i);
            updateRingerModeAffectedStreams();
            readDockAudioSettings(cr);
        }
        this.mMuteAffectedStreams = System.getIntForUser(cr, System.MUTE_STREAMS_AFFECTED, 14, -2);
        if (System.getIntForUser(cr, System.VOLUME_MASTER_MUTE, 0, -2) == 1) {
            masterMute = true;
        } else {
            masterMute = false;
        }
        if (this.mUseFixedVolume) {
            masterMute = false;
            AudioSystem.setMasterVolume(1.0f);
        }
        AudioSystem.setMasterMute(masterMute);
        broadcastMasterMuteStatus(masterMute);
        if (System.getIntForUser(cr, System.MICROPHONE_MUTE, 0, -2) == 1) {
            microphoneMute = true;
        } else {
            microphoneMute = false;
        }
        AudioSystem.muteMicrophone(microphoneMute);
        broadcastRingerMode(AudioManager.RINGER_MODE_CHANGED_ACTION, this.mRingerModeExternal);
        broadcastRingerMode(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION, this.mRingerMode);
        broadcastVibrateSetting(0);
        broadcastVibrateSetting(1);
        this.mVolumeController.loadSettings(cr);
    }

    private int rescaleIndex(int index, int srcStream, int dstStream) {
        return ((this.mStreamStates[dstStream].getMaxIndex() * index) + (this.mStreamStates[srcStream].getMaxIndex() / 2)) / this.mStreamStates[srcStream].getMaxIndex();
    }

    public void adjustSuggestedStreamVolume(int direction, int suggestedStreamType, int flags, String callingPackage) {
        adjustSuggestedStreamVolume(direction, suggestedStreamType, flags, callingPackage, Binder.getCallingUid());
    }

    private void adjustSuggestedStreamVolume(int direction, int suggestedStreamType, int flags, String callingPackage, int uid) {
        int streamType;
        log("adjustSuggestedStreamVolume() stream=" + suggestedStreamType + ", flags=" + flags);
        if (this.mVolumeControlStream != -1) {
            streamType = this.mVolumeControlStream;
        } else {
            streamType = getActiveStreamType(suggestedStreamType);
        }
        int resolvedStream = this.mStreamVolumeAlias[streamType];
        if (!((flags & 4) == 0 || resolvedStream == 2)) {
            flags &= -5;
        }
        adjustStreamVolume(streamType, direction, flags, callingPackage, uid);
    }

    public void adjustStreamVolume(int streamType, int direction, int flags, String callingPackage) {
        adjustStreamVolume(streamType, direction, flags, callingPackage, Binder.getCallingUid());
    }

    private void adjustStreamVolume(int streamType, int direction, int flags, String callingPackage, int uid) {
        if (!this.mUseFixedVolume) {
            log("adjustStreamVolume() stream=" + streamType + ", dir=" + direction + ", flags=" + flags);
            if (MzcheckResumeRingerModeNormal(streamType, direction > 0)) {
                setStreamVolume(streamType, 1, 1, callingPackage);
                return;
            }
            ensureValidDirection(direction);
            ensureValidStreamType(streamType);
            int streamTypeAlias = this.mStreamVolumeAlias[streamType];
            VolumeStreamState streamState = this.mStreamStates[streamTypeAlias];
            int device = getDeviceForStream(streamTypeAlias);
            int aliasIndex = streamState.getIndex(device);
            if (((device & AudioSystem.DEVICE_OUT_ALL_A2DP) != 0 || (flags & 64) == 0) && this.mAppOps.noteOp(STEAM_VOLUME_OPS[streamTypeAlias], uid, callingPackage) == 0) {
                int step;
                synchronized (this.mSafeMediaVolumeState) {
                    this.mPendingVolumeCommand = null;
                }
                flags &= -33;
                if (streamTypeAlias != 3 || (this.mFixedVolumeDevices & device) == 0) {
                    step = rescaleIndex(10, streamType, streamTypeAlias);
                    if (streamType == 3 && flags != 0) {
                        step *= 4;
                    }
                } else {
                    flags |= 32;
                    if (this.mSafeMediaVolumeState.intValue() != 3 || (device & 0) == 0) {
                        step = streamState.getMaxIndex();
                    } else {
                        step = this.mSafeMediaVolumeIndex;
                    }
                    if (aliasIndex != 0) {
                        aliasIndex = step;
                    }
                    direction = 1;
                }
                int oldIndex = this.mStreamStates[streamType].getIndex(device);
                if (true && direction != 0) {
                    int newIndex;
                    int keyCode;
                    if (streamTypeAlias == 3 && (device & AudioSystem.DEVICE_OUT_ALL_A2DP) != 0 && (flags & 64) == 0) {
                        synchronized (this.mA2dpAvrcpLock) {
                            if (this.mA2dp != null && this.mAvrcpAbsVolSupported) {
                                this.mA2dp.adjustAvrcpAbsoluteVolume(direction);
                            }
                        }
                    }
                    if (direction == 1) {
                        if (!checkSafeMediaVolume(streamTypeAlias, aliasIndex + step, device)) {
                            Log.m26e(TAG, "adjustStreamVolume() safe volume index = " + oldIndex);
                            this.mVolumeController.postDisplaySafeVolumeWarning(flags);
                            newIndex = this.mStreamStates[streamType].getIndex(device);
                            if (streamTypeAlias == 3) {
                                setSystemAudioVolume(oldIndex, newIndex, getStreamMaxVolume(streamType), flags);
                            }
                            if (this.mHdmiManager != null) {
                                synchronized (this.mHdmiManager) {
                                    if (this.mHdmiCecSink && streamTypeAlias == 3 && oldIndex != newIndex) {
                                        synchronized (this.mHdmiPlaybackClient) {
                                            keyCode = direction != -1 ? 25 : 24;
                                            this.mHdmiPlaybackClient.sendKeyEvent(keyCode, true);
                                            this.mHdmiPlaybackClient.sendKeyEvent(keyCode, false);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (streamState.adjustIndex(direction * step, device)) {
                        sendMsg(this.mAudioHandler, 0, 2, device, 0, streamState, 0);
                        if (streamState.mStreamType == 3 && device == 2 && this.mRingerMode != 2) {
                            sendMsg(this.mAudioHandler, 27, 2, streamState.getIndex(device) / 10, 0, "last_" + streamState.getSettingNameForDevice(device), 0);
                        }
                    }
                    newIndex = this.mStreamStates[streamType].getIndex(device);
                    if (streamTypeAlias == 3) {
                        setSystemAudioVolume(oldIndex, newIndex, getStreamMaxVolume(streamType), flags);
                    }
                    if (this.mHdmiManager != null) {
                        synchronized (this.mHdmiManager) {
                            synchronized (this.mHdmiPlaybackClient) {
                                if (direction != -1) {
                                }
                                this.mHdmiPlaybackClient.sendKeyEvent(keyCode, true);
                                this.mHdmiPlaybackClient.sendKeyEvent(keyCode, false);
                            }
                        }
                    }
                }
                int index = this.mStreamStates[streamType].getIndex(device);
                sendVolumeUpdate(streamType, oldIndex, index, flags);
                log("[GJ_DEBUG] adjustStreamVolume: streamType = " + streamType + ", oldIndex = " + oldIndex + ", index = " + index + ", flags = " + flags);
            }
        }
    }

    private void setSystemAudioVolume(int oldVolume, int newVolume, int maxVolume, int flags) {
        if (this.mHdmiManager != null && this.mHdmiTvClient != null && oldVolume != newVolume && (flags & 256) == 0) {
            synchronized (this.mHdmiManager) {
                if (this.mHdmiSystemAudioSupported) {
                    synchronized (this.mHdmiTvClient) {
                        long token = Binder.clearCallingIdentity();
                        try {
                            this.mHdmiTvClient.setSystemAudioVolume((oldVolume + 5) / 10, (newVolume + 5) / 10, maxVolume);
                            Binder.restoreCallingIdentity(token);
                        } catch (Throwable th) {
                            Binder.restoreCallingIdentity(token);
                        }
                    }
                    return;
                }
            }
        }
    }

    public void adjustMasterVolume(int steps, int flags, String callingPackage) {
        adjustMasterVolume(steps, flags, callingPackage, Binder.getCallingUid());
    }

    public void adjustMasterVolume(int steps, int flags, String callingPackage, int uid) {
        if (!this.mUseFixedVolume) {
            ensureValidSteps(steps);
            int volume = Math.round(AudioSystem.getMasterVolume() * SensorManager.LIGHT_CLOUDY);
            int numSteps = Math.abs(steps);
            int direction = steps > 0 ? 1 : -1;
            for (int i = 0; i < numSteps; i++) {
                volume += findVolumeDelta(direction, volume);
            }
            setMasterVolume(volume, flags, callingPackage, uid);
        }
    }

    private void onSetStreamVolume(int streamType, int index, int flags, int device) {
        setStreamVolumeInt(this.mStreamVolumeAlias[streamType], index, device, false);
    }

    public void setStreamVolume(int streamType, int index, int flags, String callingPackage) {
        setStreamVolume(streamType, index, flags, callingPackage, Binder.getCallingUid());
    }

    private void setStreamVolume(int streamType, int index, int flags, String callingPackage, int uid) {
        if (!this.mUseFixedVolume) {
            MzcheckResumeRingerModeNormal(streamType, index > 0);
            ensureValidStreamType(streamType);
            int streamTypeAlias = this.mStreamVolumeAlias[streamType];
            VolumeStreamState streamState = this.mStreamStates[streamTypeAlias];
            int device = getDeviceForStream(streamType);
            if (((device & AudioSystem.DEVICE_OUT_ALL_A2DP) != 0 || (flags & 64) == 0) && this.mAppOps.noteOp(STEAM_VOLUME_OPS[streamTypeAlias], uid, callingPackage) == 0) {
                int oldIndex;
                synchronized (this.mSafeMediaVolumeState) {
                    this.mPendingVolumeCommand = null;
                    oldIndex = streamState.getIndex(device);
                    index = rescaleIndex(index * 10, streamType, streamTypeAlias);
                    if (oldIndex != index && streamState.mStreamType == 3 && device == 2 && this.mRingerMode != 2) {
                        sendMsg(this.mAudioHandler, 27, 2, index / 10, 0, "last_" + streamState.getSettingNameForDevice(device), 0);
                    }
                    if (streamTypeAlias == 3 && (device & AudioSystem.DEVICE_OUT_ALL_A2DP) != 0 && (flags & 64) == 0) {
                        synchronized (this.mA2dpAvrcpLock) {
                            if (this.mA2dp != null && this.mAvrcpAbsVolSupported) {
                                this.mA2dp.setAvrcpAbsoluteVolume(index / 10);
                            }
                        }
                    }
                    if (streamTypeAlias == 3) {
                        setSystemAudioVolume(oldIndex, index, getStreamMaxVolume(streamType), flags);
                    }
                    flags &= -33;
                    if (streamTypeAlias == 3 && (this.mFixedVolumeDevices & device) != 0) {
                        flags |= 32;
                        index = (this.mSafeMediaVolumeState.intValue() != 3 || (device & 0) == 0) ? streamState.getMaxIndex() : this.mSafeMediaVolumeIndex;
                    }
                    if (checkSafeMediaVolume(streamTypeAlias, index, device)) {
                        onSetStreamVolume(streamType, index, flags, device);
                        index = this.mStreamStates[streamType].getIndex(device);
                    } else {
                        this.mVolumeController.postDisplaySafeVolumeWarning(flags);
                        this.mPendingVolumeCommand = new StreamVolumeCommand(streamType, index, flags, device);
                    }
                }
                sendVolumeUpdate(streamType, oldIndex, index, flags);
                log("[GJ_DEBUG] setStreamVolume: streamType = " + streamType + ", oldIndex = " + oldIndex + ", index = " + index + ", flags = " + flags);
            }
        }
    }

    public void forceVolumeControlStream(int streamType, IBinder cb) {
        log("[GJ_DEBUG] forceVolumeControlStream: streamType = " + streamType);
        synchronized (this.mForceControlStreamLock) {
            this.mVolumeControlStream = streamType;
            if (this.mVolumeControlStream != -1) {
                this.mForceControlStreamClient = new ForceControlStreamClient(cb);
            } else if (this.mForceControlStreamClient != null) {
                this.mForceControlStreamClient.release();
                this.mForceControlStreamClient = null;
            }
        }
    }

    private int findVolumeDelta(int direction, int volume) {
        int delta = 0;
        int i;
        if (direction == 1) {
            if (volume == 100) {
                return 0;
            }
            delta = this.mMasterVolumeRamp[1];
            for (i = this.mMasterVolumeRamp.length - 1; i > 1; i -= 2) {
                if (volume >= this.mMasterVolumeRamp[i - 1]) {
                    delta = this.mMasterVolumeRamp[i];
                    break;
                }
            }
        } else if (direction == -1) {
            if (volume == 0) {
                return 0;
            }
            int length = this.mMasterVolumeRamp.length;
            delta = -this.mMasterVolumeRamp[length - 1];
            for (i = 2; i < length; i += 2) {
                if (volume <= this.mMasterVolumeRamp[i]) {
                    delta = -this.mMasterVolumeRamp[i - 1];
                    break;
                }
            }
        }
        return delta;
    }

    private void sendBroadcastToAll(Intent intent) {
        intent.addFlags(67108864);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void sendStickyBroadcastToAll(Intent intent) {
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void sendVolumeUpdate(int streamType, int oldIndex, int index, int flags) {
        if (!isPlatformVoice() && streamType == 2) {
            streamType = 5;
        }
        if (streamType == 3) {
            flags = updateFlagsForSystemAudio(flags);
        }
        this.mVolumeController.postVolumeChanged(streamType, flags);
        if ((flags & 32) == 0) {
            oldIndex = (oldIndex + 5) / 10;
            index = (index + 5) / 10;
            Intent intent = new Intent(AudioManager.VOLUME_CHANGED_ACTION);
            intent.putExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, streamType);
            intent.putExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, index);
            intent.putExtra(AudioManager.EXTRA_PREV_VOLUME_STREAM_VALUE, oldIndex);
            log("sendVolumeUpdate: StreamType = " + streamType + ", oldIndex = " + oldIndex + ", newIndex = " + index);
            sendBroadcastToAll(intent);
        }
    }

    private int updateFlagsForSystemAudio(int flags) {
        if (this.mHdmiTvClient != null) {
            synchronized (this.mHdmiTvClient) {
                if (this.mHdmiSystemAudioSupported && (flags & 256) == 0) {
                    flags &= -2;
                }
            }
        }
        return flags;
    }

    private void sendMasterVolumeUpdate(int flags, int oldVolume, int newVolume) {
        this.mVolumeController.postMasterVolumeChanged(updateFlagsForSystemAudio(flags));
        Intent intent = new Intent(AudioManager.MASTER_VOLUME_CHANGED_ACTION);
        intent.putExtra(AudioManager.EXTRA_PREV_MASTER_VOLUME_VALUE, oldVolume);
        intent.putExtra(AudioManager.EXTRA_MASTER_VOLUME_VALUE, newVolume);
        sendBroadcastToAll(intent);
    }

    private void sendMasterMuteUpdate(boolean muted, int flags) {
        this.mVolumeController.postMasterMuteChanged(updateFlagsForSystemAudio(flags));
        broadcastMasterMuteStatus(muted);
    }

    private void broadcastMasterMuteStatus(boolean muted) {
        Intent intent = new Intent(AudioManager.MASTER_MUTE_CHANGED_ACTION);
        intent.putExtra(AudioManager.EXTRA_MASTER_VOLUME_MUTED, muted);
        intent.addFlags(603979776);
        sendStickyBroadcastToAll(intent);
    }

    private void setStreamVolumeInt(int streamType, int index, int device, boolean force) {
        VolumeStreamState streamState = this.mStreamStates[streamType];
        if (streamState.setIndex(index, device) || force) {
            sendMsg(this.mAudioHandler, 0, 2, device, 0, streamState, 0);
        }
    }

    public void setStreamSolo(int streamType, boolean state, IBinder cb) {
        if (!this.mUseFixedVolume) {
            int streamAlias = this.mStreamVolumeAlias[streamType];
            int stream = 0;
            while (stream < this.mStreamStates.length) {
                if (isStreamAffectedByMute(streamAlias) && streamAlias != this.mStreamVolumeAlias[stream]) {
                    this.mStreamStates[stream].mute(cb, state);
                }
                stream++;
            }
        }
    }

    public void setStreamMute(int streamType, boolean state, IBinder cb) {
        if (!this.mUseFixedVolume) {
            if (streamType == Integer.MIN_VALUE) {
                streamType = getActiveStreamType(streamType);
            }
            int streamAlias = this.mStreamVolumeAlias[streamType];
            if (isStreamAffectedByMute(streamAlias)) {
                if (streamAlias == 3) {
                    setSystemAudioMute(state);
                }
                for (int stream = 0; stream < this.mStreamStates.length; stream++) {
                    if (streamAlias == this.mStreamVolumeAlias[stream]) {
                        this.mStreamStates[stream].mute(cb, state);
                        Intent intent = new Intent(AudioManager.STREAM_MUTE_CHANGED_ACTION);
                        intent.putExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, stream);
                        intent.putExtra(AudioManager.EXTRA_STREAM_VOLUME_MUTED, state);
                        sendBroadcastToAll(intent);
                    }
                }
            }
        }
    }

    private void setSystemAudioMute(boolean state) {
        if (this.mHdmiManager != null && this.mHdmiTvClient != null) {
            synchronized (this.mHdmiManager) {
                if (this.mHdmiSystemAudioSupported) {
                    synchronized (this.mHdmiTvClient) {
                        long token = Binder.clearCallingIdentity();
                        try {
                            this.mHdmiTvClient.setSystemAudioMute(state);
                            Binder.restoreCallingIdentity(token);
                        } catch (Throwable th) {
                            Binder.restoreCallingIdentity(token);
                        }
                    }
                    return;
                }
            }
        }
    }

    public boolean isStreamMute(int streamType) {
        boolean access$300;
        if (streamType == Integer.MIN_VALUE) {
            streamType = getActiveStreamType(streamType);
        }
        synchronized (VolumeStreamState.class) {
            access$300 = this.mStreamStates[streamType].isMuted_syncVSS();
        }
        return access$300;
    }

    private boolean discardRmtSbmxFullVolDeathHandlerFor(IBinder cb) {
        Iterator<RmtSbmxFullVolDeathHandler> it = this.mRmtSbmxFullVolDeathHandlers.iterator();
        while (it.hasNext()) {
            RmtSbmxFullVolDeathHandler handler = (RmtSbmxFullVolDeathHandler) it.next();
            if (handler.isHandlerFor(cb)) {
                handler.forget();
                this.mRmtSbmxFullVolDeathHandlers.remove(handler);
                return true;
            }
        }
        return false;
    }

    private boolean hasRmtSbmxFullVolDeathHandlerFor(IBinder cb) {
        Iterator<RmtSbmxFullVolDeathHandler> it = this.mRmtSbmxFullVolDeathHandlers.iterator();
        while (it.hasNext()) {
            if (((RmtSbmxFullVolDeathHandler) it.next()).isHandlerFor(cb)) {
                return true;
            }
        }
        return false;
    }

    public void forceRemoteSubmixFullVolume(boolean startForcing, IBinder cb) {
        if (cb != null) {
            if (this.mContext.checkCallingOrSelfPermission(Manifest$permission.CAPTURE_AUDIO_OUTPUT) != 0) {
                Log.m32w(TAG, "Trying to call forceRemoteSubmixFullVolume() without CAPTURE_AUDIO_OUTPUT");
                return;
            }
            synchronized (this.mRmtSbmxFullVolDeathHandlers) {
                boolean applyRequired = false;
                if (startForcing) {
                    if (!hasRmtSbmxFullVolDeathHandlerFor(cb)) {
                        this.mRmtSbmxFullVolDeathHandlers.add(new RmtSbmxFullVolDeathHandler(cb));
                        if (this.mRmtSbmxFullVolRefCount == 0) {
                            this.mFullVolumeDevices |= 32768;
                            this.mFixedVolumeDevices |= 32768;
                            applyRequired = true;
                        }
                        this.mRmtSbmxFullVolRefCount++;
                    }
                } else if (discardRmtSbmxFullVolDeathHandlerFor(cb) && this.mRmtSbmxFullVolRefCount > 0) {
                    this.mRmtSbmxFullVolRefCount--;
                    if (this.mRmtSbmxFullVolRefCount == 0) {
                        this.mFullVolumeDevices &= -32769;
                        this.mFixedVolumeDevices &= -32769;
                        applyRequired = true;
                    }
                }
                if (applyRequired) {
                    checkAllFixedVolumeDevices(3);
                    this.mStreamStates[3].applyAllVolumes();
                }
            }
        }
    }

    public void setMasterMute(boolean state, int flags, String callingPackage, IBinder cb) {
        setMasterMuteInternal(state, flags, callingPackage, cb, Binder.getCallingUid());
    }

    private void setMasterMuteInternal(boolean state, int flags, String callingPackage, IBinder cb, int uid) {
        if (!this.mUseFixedVolume && this.mAppOps.noteOp(33, uid, callingPackage) == 0 && state != AudioSystem.getMasterMute()) {
            int i;
            setSystemAudioMute(state);
            AudioSystem.setMasterMute(state);
            Handler handler = this.mAudioHandler;
            if (state) {
                i = 1;
            } else {
                i = 0;
            }
            sendMsg(handler, 11, 0, i, UserHandle.getCallingUserId(), null, 500);
            sendMasterMuteUpdate(state, flags);
            Intent intent = new Intent(AudioManager.MASTER_MUTE_CHANGED_ACTION);
            intent.putExtra(AudioManager.EXTRA_MASTER_VOLUME_MUTED, state);
            sendBroadcastToAll(intent);
        }
    }

    public boolean isMasterMute() {
        return AudioSystem.getMasterMute();
    }

    protected static int getMaxStreamVolume(int streamType) {
        return MAX_STREAM_VOLUME[streamType];
    }

    public static int getDefaultStreamVolume(int streamType) {
        return DEFAULT_STREAM_VOLUME[streamType];
    }

    public int getStreamVolume(int streamType) {
        int i;
        ensureValidStreamType(streamType);
        int device = getDeviceForStream(streamType);
        synchronized (VolumeStreamState.class) {
            int index = this.mStreamStates[streamType].getIndex(device);
            if (this.mStreamStates[streamType].isMuted_syncVSS()) {
                index = 0;
            }
            if (this.mStreamVolumeAlias[streamType] == 3 && (this.mFixedVolumeDevices & device) != 0) {
                index = this.mStreamStates[streamType].getMaxIndex();
            }
            log("getStreamVolume = " + index);
            i = (index + 5) / 10;
        }
        return i;
    }

    public int getMasterVolume() {
        if (isMasterMute()) {
            return 0;
        }
        return getLastAudibleMasterVolume();
    }

    public void setMasterVolume(int volume, int flags, String callingPackage) {
        setMasterVolume(volume, flags, callingPackage, Binder.getCallingUid());
    }

    public void setMasterVolume(int volume, int flags, String callingPackage, int uid) {
        if (!this.mUseFixedVolume && this.mAppOps.noteOp(33, uid, callingPackage) == 0) {
            if (volume < 0) {
                volume = 0;
            } else if (volume > 100) {
                volume = 100;
            }
            doSetMasterVolume(((float) volume) / SensorManager.LIGHT_CLOUDY, flags);
        }
    }

    private void doSetMasterVolume(float volume, int flags) {
        if (!AudioSystem.getMasterMute()) {
            int oldVolume = getMasterVolume();
            AudioSystem.setMasterVolume(volume);
            int newVolume = getMasterVolume();
            if (newVolume != oldVolume) {
                sendMsg(this.mAudioHandler, 2, 0, Math.round(1000.0f * volume), 0, null, 500);
                setSystemAudioVolume(oldVolume, newVolume, getMasterMaxVolume(), flags);
            }
            sendMasterVolumeUpdate(flags, oldVolume, newVolume);
        }
    }

    public int getStreamMaxVolume(int streamType) {
        ensureValidStreamType(streamType);
        return (this.mStreamStates[streamType].getMaxIndex() + 5) / 10;
    }

    public int getMasterMaxVolume() {
        return 100;
    }

    public int getLastAudibleStreamVolume(int streamType) {
        ensureValidStreamType(streamType);
        return (this.mStreamStates[streamType].getIndex(getDeviceForStream(streamType)) + 5) / 10;
    }

    public int getLastAudibleMasterVolume() {
        return Math.round(AudioSystem.getMasterVolume() * SensorManager.LIGHT_CLOUDY);
    }

    public int getMasterStreamType() {
        return this.mStreamVolumeAlias[1];
    }

    public void setMicrophoneMute(boolean on, String callingPackage) {
        if (this.mAppOps.noteOp(44, Binder.getCallingUid(), callingPackage) == 0 && checkAudioSettingsPermission("setMicrophoneMute()")) {
            AudioSystem.muteMicrophone(on);
            sendMsg(this.mAudioHandler, 23, 0, on ? 1 : 0, UserHandle.getCallingUserId(), null, 500);
        }
    }

    public int getRingerModeExternal() {
        int i;
        synchronized (this.mSettingsLock) {
            i = this.mRingerModeExternal;
        }
        return i;
    }

    public int getRingerModeInternal() {
        int i;
        synchronized (this.mSettingsLock) {
            i = this.mRingerMode;
        }
        return i;
    }

    private void ensureValidRingerMode(int ringerMode) {
        if (!isValidRingerMode(ringerMode)) {
            throw new IllegalArgumentException("Bad ringer mode " + ringerMode);
        }
    }

    public boolean isValidRingerMode(int ringerMode) {
        return ringerMode >= 0 && ringerMode <= 2;
    }

    public void setRingerModeExternal(int ringerMode, String caller) {
        setRingerMode(ringerMode, caller, true);
    }

    public void setRingerModeInternal(int ringerMode, String caller) {
        enforceSelfOrSystemUI("setRingerModeInternal");
        setRingerMode(ringerMode, caller, false);
    }

    private void setRingerMode(int ringerMode, String caller, boolean external) {
        if (!this.mUseFixedVolume && !isPlatformTelevision()) {
            if (caller == null || caller.length() == 0) {
                throw new IllegalArgumentException("Bad caller: " + caller);
            }
            ensureValidRingerMode(ringerMode);
            if (ringerMode == 1 && !this.mHasVibrator) {
                ringerMode = 0;
            }
            log("[GJ_DEBUG] setRingerMode: ringerMode = " + ringerMode + ", external = " + external);
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mSettingsLock) {
                    int ringerModeInternal = getRingerModeInternal();
                    log("[GJ_DEBUG] setRingerMode: ringerModeInternal = " + ringerModeInternal + ", ringerModeExternal = " + getRingerModeExternal());
                    if (external) {
                        setRingerModeExt(ringerMode);
                        if (ringerMode != ringerModeInternal) {
                            setRingerModeInt(ringerMode, true);
                        }
                    } else {
                        if (ringerMode != ringerModeInternal) {
                            setRingerModeInt(ringerMode, true);
                        }
                        setRingerModeExt(ringerMode);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private void setRingerModeExt(int ringerMode) {
        synchronized (this.mSettingsLock) {
            if (ringerMode == this.mRingerModeExternal) {
                return;
            }
            this.mRingerModeExternal = ringerMode;
            broadcastRingerMode(AudioManager.RINGER_MODE_CHANGED_ACTION, ringerMode);
        }
    }

    private void setRingerModeInt(int ringerMode, boolean persist) {
        int oldRingerMode = getRingerModeInternal();
        synchronized (this.mSettingsLock) {
            boolean change = this.mRingerMode != ringerMode;
            this.mRingerMode = ringerMode;
        }
        log("setRingerModeInt: Ringermode = " + ringerMode);
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        boolean ringerModeMute = ringerMode == 1 || ringerMode == 0;
        int streamType = numStreamTypes - 1;
        while (streamType >= 0) {
            boolean isMuted = isStreamMutedByRingerMode(streamType);
            boolean shouldMute = ringerModeMute && isStreamAffectedByRingerMode(streamType);
            if (isMuted != shouldMute) {
                if (shouldMute) {
                    this.mStreamStates[streamType].mute(null, true);
                    this.mRingerModeMutedStreams |= 1 << streamType;
                } else {
                    if ((isPlatformVoice() || this.mHasVibrator) && this.mStreamVolumeAlias[streamType] == 2) {
                        synchronized (VolumeStreamState.class) {
                            for (Entry entry : this.mStreamStates[streamType].mIndex.entrySet()) {
                                if (((Integer) entry.getValue()).intValue() == 0) {
                                    entry.setValue(Integer.valueOf(10));
                                }
                            }
                        }
                    }
                    this.mStreamStates[streamType].mute(null, false);
                    this.mRingerModeMutedStreams &= (1 << streamType) ^ -1;
                }
            }
            streamType--;
        }
        if (persist) {
            if (BuildExt.isProductInternational()) {
                broadcastRingerMode(AudioManager.RINGER_MODE_CHANGED_ACTION, ringerMode);
                broadcastRingerMode(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION, ringerMode);
            } else if (oldRingerMode == 2 || ringerMode == 2) {
                sendMsg(this.mAudioHandler, 24, 2, ringerMode, 0, null, 0);
            }
            sendMsg(this.mAudioHandler, 3, 0, 0, 0, null, 500);
        }
        if (change) {
            broadcastRingerMode(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION, ringerMode);
        }
    }

    private void restoreMasterVolume() {
        if (this.mUseFixedVolume) {
            AudioSystem.setMasterVolume(1.0f);
        } else if (this.mUseMasterVolume) {
            float volume = System.getFloatForUser(this.mContentResolver, System.VOLUME_MASTER, LayoutParams.BRIGHTNESS_OVERRIDE_NONE, -2);
            if (volume >= 0.0f) {
                AudioSystem.setMasterVolume(volume);
            }
        }
    }

    public boolean shouldVibrate(int vibrateType) {
        boolean z = true;
        if (!this.mHasVibrator) {
            return false;
        }
        switch (getVibrateSetting(vibrateType)) {
            case 0:
                return false;
            case 1:
                if (getRingerModeExternal() == 0) {
                    z = false;
                }
                return z;
            case 2:
                if (getRingerModeExternal() != 1) {
                    z = false;
                }
                return z;
            default:
                return false;
        }
    }

    public int getVibrateSetting(int vibrateType) {
        if (this.mHasVibrator) {
            return (this.mVibrateSetting >> (vibrateType * 2)) & 3;
        }
        return 0;
    }

    public void setVibrateSetting(int vibrateType, int vibrateSetting) {
        if (this.mHasVibrator) {
            this.mVibrateSetting = getValueForVibrateSetting(this.mVibrateSetting, vibrateType, vibrateSetting);
            broadcastVibrateSetting(vibrateType);
        }
    }

    public static int getValueForVibrateSetting(int existingValue, int vibrateType, int vibrateSetting) {
        return (existingValue & ((3 << (vibrateType * 2)) ^ -1)) | ((vibrateSetting & 3) << (vibrateType * 2));
    }

    public void setMode(int mode, IBinder cb) {
        Log.m30v(TAG, "setMode(mode=" + mode + ")");
        if (!checkAudioSettingsPermission("setMode()")) {
            Log.m26e(TAG, "setMode: No permission!");
        } else if (mode == 2 && this.mContext.checkCallingOrSelfPermission(Manifest$permission.MODIFY_PHONE_STATE) != 0) {
            Log.m32w(TAG, "MODIFY_PHONE_STATE Permission Denial: setMode(MODE_IN_CALL) from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
        } else if (mode < -1 || mode >= 4) {
            Log.m26e(TAG, "setMode: Invalid mode!");
        } else {
            int newModeOwnerPid;
            synchronized (this.mSetModeDeathHandlers) {
                if (mode == -1) {
                    mode = this.mMode;
                }
                log("setMode mMode = " + this.mMode + ", mode = " + mode);
                newModeOwnerPid = setModeInt(mode, cb, Binder.getCallingPid());
            }
            if (newModeOwnerPid != 0) {
                disconnectBluetoothSco(newModeOwnerPid);
            }
        }
    }

    private int setModeInt(int mode, IBinder cb, int pid) {
        Log.m30v(TAG, "setModeInt(mode=" + mode + ", pid=" + pid + ")");
        int newModeOwnerPid = 0;
        if (cb == null) {
            Log.m26e(TAG, "setModeInt() called with null binder");
            return 0;
        }
        int status;
        SetModeDeathHandler hdlr = null;
        Iterator iter = this.mSetModeDeathHandlers.iterator();
        while (iter.hasNext()) {
            SetModeDeathHandler h = (SetModeDeathHandler) iter.next();
            if (h.getPid() == pid) {
                hdlr = h;
                iter.remove();
                hdlr.getBinder().unlinkToDeath(hdlr, 0);
                break;
            }
        }
        do {
            if (mode != 0) {
                if (hdlr == null) {
                    hdlr = new SetModeDeathHandler(cb, pid);
                }
                try {
                    cb.linkToDeath(hdlr, 0);
                } catch (RemoteException e) {
                    Log.m32w(TAG, "setMode() could not link to " + cb + " binder death");
                }
                this.mSetModeDeathHandlers.add(0, hdlr);
                hdlr.setMode(mode);
            } else if (!this.mSetModeDeathHandlers.isEmpty()) {
                hdlr = (SetModeDeathHandler) this.mSetModeDeathHandlers.get(0);
                cb = hdlr.getBinder();
                mode = hdlr.getMode();
                Log.m32w(TAG, " using mode=" + mode + " instead due to death hdlr at pid=" + hdlr.mPid);
            }
            log("setModeInt mMode = " + this.mMode + ", mode = " + mode);
            if (mode != this.mMode) {
                status = AudioSystem.setPhoneState(mode);
                if (status == 0) {
                    Log.m30v(TAG, " mode successfully set to " + mode);
                    this.mMode = mode;
                } else {
                    if (hdlr != null) {
                        this.mSetModeDeathHandlers.remove(hdlr);
                        cb.unlinkToDeath(hdlr, 0);
                    }
                    Log.m32w(TAG, " mode set to MODE_NORMAL after phoneState pb");
                    mode = 0;
                }
            } else {
                status = 0;
            }
            if (status == 0) {
                break;
            }
        } while (!this.mSetModeDeathHandlers.isEmpty());
        if (status == 0) {
            if (mode != 0) {
                if (this.mSetModeDeathHandlers.isEmpty()) {
                    Log.m26e(TAG, "setMode() different from MODE_NORMAL with empty mode client stack");
                } else {
                    newModeOwnerPid = ((SetModeDeathHandler) this.mSetModeDeathHandlers.get(0)).getPid();
                }
            }
            int streamType = getActiveStreamType(Integer.MIN_VALUE);
            int device = getDeviceForStream(streamType);
            setStreamVolumeInt(this.mStreamVolumeAlias[streamType], this.mStreamStates[this.mStreamVolumeAlias[streamType]].getIndex(device), device, true);
            updateStreamVolumeAlias(true);
        }
        return newModeOwnerPid;
    }

    public int getMode() {
        log("getMode mMode = " + this.mMode);
        return this.mMode;
    }

    private void loadTouchSoundAssetDefaults() {
        SOUND_EFFECT_FILES.add("Effect_Tick.ogg");
        for (int i = 0; i < 10; i++) {
            this.SOUND_EFFECT_FILES_MAP[i][0] = 0;
            this.SOUND_EFFECT_FILES_MAP[i][1] = -1;
        }
    }

    public void playSoundEffect(int effectType) {
        playSoundEffectVolume(effectType, LayoutParams.BRIGHTNESS_OVERRIDE_NONE);
    }

    public void playSoundEffectVolume(int effectType, float volume) {
        if (effectType >= 10 || effectType < 0) {
            Log.m32w(TAG, "AudioService effectType value " + effectType + " out of range");
            return;
        }
        sendMsg(this.mAudioHandler, 5, 2, effectType, (int) (1000.0f * volume), null, 0);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean loadSoundEffects() {
        Throwable th;
        LoadSoundEffectReply reply = new LoadSoundEffectReply();
        synchronized (reply) {
            int attempts;
            sendMsg(this.mAudioHandler, 7, 2, 0, 0, reply, 0);
            int attempts2 = 3;
            while (reply.mStatus == 1) {
                try {
                    attempts = attempts2 - 1;
                    if (attempts2 <= 0) {
                        break;
                    }
                    try {
                        reply.wait(TimedRemoteCaller.DEFAULT_CALL_TIMEOUT_MILLIS);
                        attempts2 = attempts;
                    } catch (InterruptedException e) {
                        Log.m32w(TAG, "loadSoundEffects Interrupted while waiting sound pool loaded.");
                        attempts2 = attempts;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    attempts = attempts2;
                }
            }
            attempts = attempts2;
        }
        throw th;
    }

    public void unloadSoundEffects() {
        sendMsg(this.mAudioHandler, 20, 2, 0, 0, null, 0);
    }

    public void reloadAudioSettings() {
        readAudioSettings(false);
    }

    private void readAudioSettings(boolean userSwitch) {
        readPersistedSettings();
        int numStreamTypes = AudioSystem.getNumStreamTypes();
        int streamType = 0;
        while (streamType < numStreamTypes) {
            VolumeStreamState streamState = this.mStreamStates[streamType];
            if (!userSwitch || this.mStreamVolumeAlias[streamType] != 3) {
                streamState.readSettings();
                synchronized (VolumeStreamState.class) {
                    if (streamState.isMuted_syncVSS() && (!(isStreamAffectedByMute(streamType) || isStreamMutedByRingerMode(streamType)) || this.mUseFixedVolume)) {
                        int size = streamState.mDeathHandlers.size();
                        for (int i = 0; i < size; i++) {
                            ((VolumeDeathHandler) streamState.mDeathHandlers.get(i)).mMuteCount = 1;
                            ((VolumeDeathHandler) streamState.mDeathHandlers.get(i)).mute_syncVSS(false);
                        }
                    }
                }
            }
            streamType++;
        }
        setRingerModeInt(getRingerModeInternal(), false);
        checkAllFixedVolumeDevices();
        checkAllAliasStreamVolumes();
        synchronized (this.mSafeMediaVolumeState) {
            this.mMusicActiveMs = MathUtils.constrain(Secure.getIntForUser(this.mContentResolver, Secure.UNSAFE_VOLUME_MUSIC_ACTIVE_MS, 0, -2), 0, (int) UNSAFE_VOLUME_MUSIC_ACTIVE_MS_MAX);
            if (this.mSafeMediaVolumeState.intValue() == 3) {
                enforceSafeMediaVolume();
            }
        }
    }

    public void setSpeakerphoneOn(boolean on) {
        if (checkAudioSettingsPermission("setSpeakerphoneOn()")) {
            if (on) {
                if (this.mForcedUseForComm == 3) {
                    sendMsg(this.mAudioHandler, 8, 2, 2, 0, null, 0);
                }
                this.mForcedUseForComm = 1;
            } else if (this.mForcedUseForComm == 1) {
                this.mForcedUseForComm = 0;
            }
            sendMsg(this.mAudioHandler, 8, 2, 0, this.mForcedUseForComm, null, 0);
            log("setSpeakerphoneOn " + this.mForcedUseForComm);
        }
    }

    public boolean isSpeakerphoneOn() {
        return this.mForcedUseForComm == 1;
    }

    public void setBluetoothScoOn(boolean on) {
        if (checkAudioSettingsPermission("setBluetoothScoOn()")) {
            if (on) {
                this.mForcedUseForComm = 3;
            } else if (this.mForcedUseForComm == 3) {
                this.mForcedUseForComm = 0;
            }
            sendMsg(this.mAudioHandler, 8, 2, 0, this.mForcedUseForComm, null, 0);
            sendMsg(this.mAudioHandler, 8, 2, 2, this.mForcedUseForComm, null, 0);
            log("setBluetoothScoOn " + this.mForcedUseForComm);
        }
    }

    public boolean isBluetoothScoOn() {
        log("isBluetoothScoOn " + this.mForcedUseForComm);
        return this.mForcedUseForComm == 3;
    }

    public void setBluetoothA2dpOn(boolean on) {
        int i = 0;
        synchronized (this.mBluetoothA2dpEnabledLock) {
            this.mBluetoothA2dpEnabled = on;
            Handler handler = this.mAudioHandler;
            if (!this.mBluetoothA2dpEnabled) {
                i = 10;
            }
            sendMsg(handler, 13, 2, 1, i, null, 0);
        }
    }

    public void setBluetoothA2dpOnCheck(boolean on, String address) {
        boolean isConnected = true;
        int i = 0;
        synchronized (this.mBluetoothA2dpEnabledLock) {
            if (!this.mConnectedDevices.containsKey(Integer.valueOf(128)) || ((String) this.mConnectedDevices.get(Integer.valueOf(128))).equals(ProxyInfo.LOCAL_EXCL_LIST) || ((String) this.mConnectedDevices.get(Integer.valueOf(128))).equals(address)) {
                isConnected = false;
            }
            if (!isConnected || on) {
                this.mBluetoothA2dpEnabled = on;
                Handler handler = this.mAudioHandler;
                if (!this.mBluetoothA2dpEnabled) {
                    i = 10;
                }
                sendMsg(handler, 13, 2, 1, i, null, 0);
            }
        }
    }

    public boolean isBluetoothA2dpOn() {
        boolean z;
        synchronized (this.mBluetoothA2dpEnabledLock) {
            z = this.mBluetoothA2dpEnabled;
        }
        return z;
    }

    public void startBluetoothSco(IBinder cb, int targetSdkVersion) {
        log("startBluetoothSco");
        startBluetoothScoInt(cb, targetSdkVersion < 18 ? 0 : -1);
    }

    public void startBluetoothScoVirtualCall(IBinder cb) {
        startBluetoothScoInt(cb, 0);
    }

    void startBluetoothScoInt(IBinder cb, int scoAudioMode) {
        if (checkAudioSettingsPermission("startBluetoothSco()") && this.mSystemReady) {
            ScoClient client = getScoClient(cb, true);
            long ident = Binder.clearCallingIdentity();
            client.incCount(scoAudioMode);
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void stopBluetoothSco(IBinder cb) {
        log("stopBluetoothSco");
        if (checkAudioSettingsPermission("stopBluetoothSco()") && this.mSystemReady) {
            ScoClient client = getScoClient(cb, false);
            long ident = Binder.clearCallingIdentity();
            if (client != null) {
                client.decCount();
            }
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void checkScoAudioState() {
        if (this.mBluetoothHeadset != null && this.mBluetoothHeadsetDevice != null && this.mScoAudioState == 0 && this.mBluetoothHeadset.getAudioState(this.mBluetoothHeadsetDevice) != 10) {
            this.mScoAudioState = 2;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private ScoClient getScoClient(IBinder cb, boolean create) {
        synchronized (this.mScoClients) {
            try {
                ScoClient client;
                int size = this.mScoClients.size();
                int i = 0;
                ScoClient client2 = null;
                while (i < size) {
                    try {
                        client = (ScoClient) this.mScoClients.get(i);
                        if (client.getBinder() == cb) {
                            return client;
                        }
                        i++;
                        client2 = client;
                    } catch (Throwable th) {
                        Throwable th2 = th;
                        client = client2;
                    }
                }
                if (create) {
                    client = new ScoClient(cb);
                    this.mScoClients.add(client);
                } else {
                    client = client2;
                }
            } catch (Throwable th3) {
                th2 = th3;
            }
        }
        throw th2;
    }

    public void clearAllScoClients(int exceptPid, boolean stopSco) {
        synchronized (this.mScoClients) {
            ScoClient savedClient = null;
            int size = this.mScoClients.size();
            for (int i = 0; i < size; i++) {
                ScoClient cl = (ScoClient) this.mScoClients.get(i);
                if (cl.getPid() != exceptPid) {
                    cl.clearCount(stopSco);
                } else {
                    savedClient = cl;
                }
            }
            this.mScoClients.clear();
            if (savedClient != null) {
                this.mScoClients.add(savedClient);
            }
        }
    }

    private boolean getBluetoothHeadset() {
        int i;
        boolean result = false;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            result = adapter.getProfileProxy(this.mContext, this.mBluetoothProfileServiceListener, 1);
        }
        Handler handler = this.mAudioHandler;
        if (result) {
            i = 3000;
        } else {
            i = 0;
        }
        sendMsg(handler, 9, 0, 0, 0, null, i);
        return result;
    }

    private void disconnectBluetoothSco(int exceptPid) {
        synchronized (this.mScoClients) {
            checkScoAudioState();
            if (this.mScoAudioState != 2 && this.mScoAudioState != 4) {
                clearAllScoClients(exceptPid, true);
            } else if (this.mBluetoothHeadsetDevice != null) {
                if (this.mBluetoothHeadset != null) {
                    if (!this.mBluetoothHeadset.stopVoiceRecognition(this.mBluetoothHeadsetDevice)) {
                        sendMsg(this.mAudioHandler, 9, 0, 0, 0, null, 0);
                    }
                } else if (this.mScoAudioState == 2 && getBluetoothHeadset()) {
                    this.mScoAudioState = 4;
                }
            }
        }
    }

    private void resetBluetoothSco() {
        synchronized (this.mScoClients) {
            clearAllScoClients(0, false);
            this.mScoAudioState = 0;
            broadcastScoConnectionState(0);
        }
    }

    private void broadcastScoConnectionState(int state) {
        sendMsg(this.mAudioHandler, 19, 2, state, 0, null, 0);
    }

    private void onBroadcastScoConnectionState(int state) {
        if (state != this.mScoConnectionState) {
            Intent newIntent = new Intent(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
            newIntent.putExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, state);
            newIntent.putExtra(AudioManager.EXTRA_SCO_AUDIO_PREVIOUS_STATE, this.mScoConnectionState);
            sendStickyBroadcastToAll(newIntent);
            this.mScoConnectionState = state;
        }
    }

    private void onCheckMusicActive() {
        synchronized (this.mSafeMediaVolumeState) {
            if (this.mSafeMediaVolumeState.intValue() == 2) {
                int device = getDeviceForStream(3);
                if ((device & 0) != 0) {
                    sendMsg(this.mAudioHandler, 14, 0, 0, 0, null, MUSIC_ACTIVE_POLL_PERIOD_MS);
                    int index = this.mStreamStates[3].getIndex(device);
                    if (AudioSystem.isStreamActive(3, 0) && index > this.mSafeMediaVolumeIndex) {
                        this.mMusicActiveMs += MUSIC_ACTIVE_POLL_PERIOD_MS;
                        if (this.mMusicActiveMs > UNSAFE_VOLUME_MUSIC_ACTIVE_MS_MAX) {
                            setSafeMediaVolumeEnabled(true);
                            this.mMusicActiveMs = 0;
                        }
                        saveMusicActiveMs();
                    }
                }
            }
        }
    }

    private void saveMusicActiveMs() {
        this.mAudioHandler.obtainMessage(22, this.mMusicActiveMs, 0).sendToTarget();
    }

    private void onConfigureSafeVolume(boolean force) {
        boolean safeMediaVolumeEnabled = false;
        synchronized (this.mSafeMediaVolumeState) {
            int mcc = this.mContext.getResources().getConfiguration().mcc;
            if (this.mMcc != mcc || (this.mMcc == 0 && force)) {
                int persistedState;
                this.mSafeMediaVolumeIndex = this.mContext.getResources().getInteger(C0833R.integer.config_safe_media_volume_index) * 10;
                if (SystemProperties.getBoolean("audio.safemedia.force", false) || this.mContext.getResources().getBoolean(C0833R.bool.config_safe_media_volume_enabled)) {
                    safeMediaVolumeEnabled = true;
                }
                if (safeMediaVolumeEnabled) {
                    persistedState = 3;
                    if (this.mSafeMediaVolumeState.intValue() != 2) {
                        if (this.mMusicActiveMs == 0) {
                            this.mSafeMediaVolumeState = Integer.valueOf(3);
                            enforceSafeMediaVolume();
                        } else {
                            this.mSafeMediaVolumeState = Integer.valueOf(2);
                        }
                    }
                } else {
                    persistedState = 1;
                    this.mSafeMediaVolumeState = Integer.valueOf(1);
                }
                this.mMcc = mcc;
                sendMsg(this.mAudioHandler, 18, 2, persistedState, 0, null, 0);
            }
        }
    }

    private int checkForRingerModeChange(int oldIndex, int direction, int step) {
        int result = 1;
        int ringerMode = getRingerModeInternal();
        switch (ringerMode) {
            case 0:
                if (direction == 1) {
                    result = 1 | 128;
                }
                result &= -2;
                break;
            case 1:
                if (!this.mHasVibrator) {
                    Log.m26e(TAG, "checkForRingerModeChange() current ringer mode is vibratebut no vibrator is present");
                    break;
                }
                if (direction == -1) {
                    if (this.mPrevVolDirection != -1) {
                        result = 1 | 2048;
                    }
                } else if (direction == 1) {
                    ringerMode = 2;
                }
                result &= -2;
                break;
            case 2:
                if (direction == -1) {
                    if (!this.mHasVibrator) {
                        if (oldIndex < step) {
                            break;
                        }
                    } else if (step <= oldIndex && oldIndex < step * 2) {
                        ringerMode = 1;
                        break;
                    }
                }
                break;
            default:
                Log.m26e(TAG, "checkForRingerModeChange() wrong ringer mode: " + ringerMode);
                break;
        }
        setRingerMode(ringerMode, "AudioService.checkForRingerModeChange", false);
        this.mPrevVolDirection = direction;
        return result;
    }

    public boolean isStreamAffectedByRingerMode(int streamType) {
        return (this.mRingerModeAffectedStreams & (1 << streamType)) != 0;
    }

    private boolean isStreamMutedByRingerMode(int streamType) {
        return (this.mRingerModeMutedStreams & (1 << streamType)) != 0;
    }

    boolean updateRingerModeAffectedStreams() {
        int ringerModeAffectedStreams = System.getIntForUser(this.mContentResolver, System.MODE_RINGER_STREAMS_AFFECTED, 166, -2) | 38;
        switch (this.mPlatformType) {
            case 2:
                ringerModeAffectedStreams = 0;
                break;
            default:
                ringerModeAffectedStreams &= -9;
                break;
        }
        synchronized (this.mCameraSoundForced) {
            if (this.mCameraSoundForced.booleanValue()) {
                ringerModeAffectedStreams &= -129;
            } else {
                ringerModeAffectedStreams |= 128;
            }
        }
        if (this.mStreamVolumeAlias[8] == 2) {
            ringerModeAffectedStreams |= 256;
        } else {
            ringerModeAffectedStreams &= -257;
        }
        if (ringerModeAffectedStreams == this.mRingerModeAffectedStreams) {
            return false;
        }
        System.putIntForUser(this.mContentResolver, System.MODE_RINGER_STREAMS_AFFECTED, ringerModeAffectedStreams, -2);
        this.mRingerModeAffectedStreams = ringerModeAffectedStreams;
        return true;
    }

    public boolean isStreamAffectedByMute(int streamType) {
        return (this.mMuteAffectedStreams & (1 << streamType)) != 0;
    }

    private void ensureValidDirection(int direction) {
        if (direction < -1 || direction > 1) {
            throw new IllegalArgumentException("Bad direction " + direction);
        }
    }

    private void ensureValidSteps(int steps) {
        if (Math.abs(steps) > 4) {
            throw new IllegalArgumentException("Bad volume adjust steps " + steps);
        }
    }

    private void ensureValidStreamType(int streamType) {
        if (streamType < 0 || streamType >= this.mStreamStates.length) {
            throw new IllegalArgumentException("Bad stream type " + streamType);
        }
    }

    private boolean isInCommunication() {
        TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService(Context.TELECOM_SERVICE);
        long ident = Binder.clearCallingIdentity();
        boolean IsInCall = telecomManager.isInCall();
        Binder.restoreCallingIdentity(ident);
        log("[GJ_DEBUG] isInCommunication: isInCall = " + IsInCall);
        return IsInCall || getMode() == 3;
    }

    private boolean isAfMusicActiveRecently(int delay_ms) {
        return AudioSystem.isStreamActive(3, delay_ms) || AudioSystem.isStreamActiveRemotely(3, delay_ms);
    }

    private int getActiveStreamType(int suggestedStreamType) {
        switch (this.mPlatformType) {
            case 1:
                if (isInCommunication()) {
                    if (AudioSystem.getForceUse(0) == 3) {
                        Log.m30v(TAG, "getActiveStreamType: Forcing STREAM_BLUETOOTH_SCO");
                        return 6;
                    }
                    Log.m30v(TAG, "getActiveStreamType: Forcing STREAM_VOICE_CALL");
                    return 0;
                } else if (suggestedStreamType == Integer.MIN_VALUE) {
                    if (isAfMusicActiveRecently(0)) {
                        Log.m30v(TAG, "getActiveStreamType: Forcing STREAM_MUSIC stream active");
                        return 3;
                    } else if (AudioSystem.isStreamActive(2, 0)) {
                        Log.m30v(TAG, "getActiveStreamType: Ring is active, so return STREAM_RING");
                        return 2;
                    } else if (!AudioSystem.isStreamActive(0, 0)) {
                        Log.m30v(TAG, "getActiveStreamType: Forcing STREAM_MUSIC b/c default");
                        return 3;
                    } else if (AudioSystem.getForceUse(0) == 3) {
                        Log.m30v(TAG, "getActiveStreamType:getActiveStreamType: Forcing STREAM_BLUETOOTH_SCO");
                        return 6;
                    } else {
                        Log.m30v(TAG, "getActiveStreamType: Voice_call is active, so return STREAM_VOICE_CALL");
                        return 0;
                    }
                } else if (isAfMusicActiveRecently(0)) {
                    Log.m30v(TAG, "getActiveStreamType: Forcing STREAM_MUSIC stream active");
                    return 3;
                }
                break;
            case 2:
                if (suggestedStreamType == Integer.MIN_VALUE) {
                    return 3;
                }
                break;
            default:
                if (isInCommunication()) {
                    if (AudioSystem.getForceUse(0) == 3) {
                        Log.m30v(TAG, "getActiveStreamType: Forcing STREAM_BLUETOOTH_SCO");
                        return 6;
                    }
                    Log.m30v(TAG, "getActiveStreamType: Forcing STREAM_VOICE_CALL");
                    return 0;
                } else if (AudioSystem.isStreamActive(5, StreamOverride.sDelayMs) || AudioSystem.isStreamActive(2, StreamOverride.sDelayMs)) {
                    Log.m30v(TAG, "getActiveStreamType: Forcing STREAM_NOTIFICATION");
                    return 5;
                } else if (suggestedStreamType == Integer.MIN_VALUE) {
                    if (isAfMusicActiveRecently(StreamOverride.sDelayMs)) {
                        Log.m30v(TAG, "getActiveStreamType: forcing STREAM_MUSIC");
                        return 3;
                    }
                    Log.m30v(TAG, "getActiveStreamType: using STREAM_NOTIFICATION as default");
                    return 5;
                }
                break;
        }
        Log.m30v(TAG, "getActiveStreamType: Returning suggested type " + suggestedStreamType);
        return suggestedStreamType;
    }

    private void broadcastRingerMode(String action, int ringerMode) {
        Intent broadcast = new Intent(action);
        broadcast.putExtra(AudioManager.EXTRA_RINGER_MODE, ringerMode);
        broadcast.addFlags(603979776);
        sendStickyBroadcastToAll(broadcast);
    }

    private void broadcastVibrateSetting(int vibrateType) {
        if (ActivityManagerNative.isSystemReady()) {
            Intent broadcast = new Intent(AudioManager.VIBRATE_SETTING_CHANGED_ACTION);
            broadcast.putExtra(AudioManager.EXTRA_VIBRATE_TYPE, vibrateType);
            broadcast.putExtra(AudioManager.EXTRA_VIBRATE_SETTING, getVibrateSetting(vibrateType));
            sendBroadcastToAll(broadcast);
        }
    }

    private void queueMsgUnderWakeLock(Handler handler, int msg, int arg1, int arg2, Object obj, int delay) {
        long ident = Binder.clearCallingIdentity();
        this.mAudioEventWakeLock.acquire();
        Binder.restoreCallingIdentity(ident);
        sendMsg(handler, msg, 2, arg1, arg2, obj, delay);
    }

    private static void sendMsg(Handler handler, int msg, int existingMsgPolicy, int arg1, int arg2, Object obj, int delay) {
        if (existingMsgPolicy == 0) {
            handler.removeMessages(msg);
        } else if (existingMsgPolicy == 1 && handler.hasMessages(msg)) {
            return;
        }
        synchronized (mLastDeviceConnectMsgTime) {
            long time = SystemClock.uptimeMillis() + ((long) delay);
            handler.sendMessageAtTime(handler.obtainMessage(msg, arg1, arg2, obj), time);
            if (msg == 100 || msg == 101 || msg == 26 || msg == 102) {
                mLastDeviceConnectMsgTime = Long.valueOf(time);
            }
        }
    }

    boolean checkAudioSettingsPermission(String method) {
        if (this.mContext.checkCallingOrSelfPermission(Manifest$permission.MODIFY_AUDIO_SETTINGS) == 0) {
            return true;
        }
        Log.m32w(TAG, "Audio Settings Permission Denial: " + method + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
        return false;
    }

    private int getDeviceForStream(int stream) {
        int device = AudioSystem.getDevicesForStream(stream);
        if (((device - 1) & device) == 0) {
            return device;
        }
        if ((device & 2) != 0) {
            return 2;
        }
        if ((262144 & device) != 0) {
            return 262144;
        }
        if ((524288 & device) != 0) {
            return 524288;
        }
        if ((2097152 & device) != 0) {
            return 2097152;
        }
        return device & AudioSystem.DEVICE_OUT_ALL_A2DP;
    }

    public void setWiredDeviceConnectionState(int device, int state, String name) {
        synchronized (this.mConnectedDevices) {
            queueMsgUnderWakeLock(this.mAudioHandler, 100, device, state, name, checkSendBecomingNoisyIntent(device, state));
        }
    }

    public int setBluetoothA2dpDeviceConnectionState(BluetoothDevice device, int state, int profile) {
        int i = 0;
        if (profile == 2 || profile == 10) {
            int delay;
            synchronized (this.mConnectedDevices) {
                if (profile == 2) {
                    if (state == 2) {
                        i = 1;
                    }
                    delay = checkSendBecomingNoisyIntent(128, i);
                } else {
                    delay = 0;
                }
                log("[GJ_DEBUG] setBluetoothA2dpDeviceConnectionState: device = " + device + "; state = " + state + "; delay = " + delay);
                queueMsgUnderWakeLock(this.mAudioHandler, profile == 2 ? 102 : 101, state, 0, device, delay);
            }
            return delay;
        }
        throw new IllegalArgumentException("invalid profile " + profile);
    }

    private void makeA2dpDeviceAvailable(String address) {
        sendMsg(this.mAudioHandler, 0, 2, 128, 0, this.mStreamStates[3], 0);
        setBluetoothA2dpOnInt(true);
        this.mConnectedDevices.put(new Integer(128), address);
        AudioSystem.setDeviceConnectionState(128, 1, address);
        AudioSystem.setParameters("A2dpSuspended=false");
    }

    private void onSendBecomingNoisyIntent() {
        sendBroadcastToAll(new Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }

    private void makeA2dpDeviceUnavailableNow(String address) {
        synchronized (this.mA2dpAvrcpLock) {
            this.mAvrcpAbsVolSupported = false;
        }
        AudioSystem.setDeviceConnectionState(128, 0, address);
        this.mConnectedDevices.remove(Integer.valueOf(128));
        synchronized (this.mCurAudioRoutes) {
            if (this.mCurAudioRoutes.mBluetoothName != null) {
                this.mCurAudioRoutes.mBluetoothName = null;
                sendMsg(this.mAudioHandler, 12, 1, 0, 0, null, 0);
            }
        }
    }

    private void makeA2dpDeviceUnavailableLater(String address) {
        AudioSystem.setParameters("A2dpSuspended=true");
        this.mConnectedDevices.remove(Integer.valueOf(128));
        this.mAudioHandler.sendMessageDelayed(this.mAudioHandler.obtainMessage(6, address), 8000);
    }

    private void makeA2dpSrcAvailable(String address) {
        AudioSystem.setDeviceConnectionState(AudioSystem.DEVICE_IN_BLUETOOTH_A2DP, 1, address);
        this.mConnectedDevices.put(new Integer(AudioSystem.DEVICE_IN_BLUETOOTH_A2DP), address);
    }

    private void makeA2dpSrcUnavailable(String address) {
        AudioSystem.setDeviceConnectionState(AudioSystem.DEVICE_IN_BLUETOOTH_A2DP, 0, address);
        this.mConnectedDevices.remove(Integer.valueOf(AudioSystem.DEVICE_IN_BLUETOOTH_A2DP));
    }

    private void cancelA2dpDeviceTimeout() {
        this.mAudioHandler.removeMessages(6);
    }

    private boolean hasScheduledA2dpDockTimeout() {
        return this.mAudioHandler.hasMessages(6);
    }

    private void onSetA2dpSinkConnectionState(BluetoothDevice btDevice, int state) {
        boolean isConnected = true;
        log("onSetA2dpSinkConnectionState btDevice=" + btDevice + "state=" + state);
        if (btDevice != null) {
            String address = btDevice.getAddress();
            if (!BluetoothAdapter.checkBluetoothAddress(address)) {
                address = ProxyInfo.LOCAL_EXCL_LIST;
            }
            synchronized (this.mConnectedDevices) {
                if (!(this.mConnectedDevices.containsKey(Integer.valueOf(128)) && ((String) this.mConnectedDevices.get(Integer.valueOf(128))).equals(address))) {
                    isConnected = false;
                }
                if (isConnected && state != 2) {
                    if (!btDevice.isBluetoothDock()) {
                        log("[GJ_DEBUG] onSetA2dpSinkConnectionState: makeA2dpDeviceUnavailableNow ----1");
                        makeA2dpDeviceUnavailableNow(address);
                    } else if (state == 0) {
                        makeA2dpDeviceUnavailableLater(address);
                    }
                    synchronized (this.mCurAudioRoutes) {
                        if (this.mCurAudioRoutes.mBluetoothName != null) {
                            this.mCurAudioRoutes.mBluetoothName = null;
                            this.mCurAudioRoutes.mBluetoothAddress = address;
                            sendMsg(this.mAudioHandler, 12, 1, 0, 0, null, 0);
                        }
                    }
                } else if (!isConnected && state == 2) {
                    if (btDevice.isBluetoothDock()) {
                        cancelA2dpDeviceTimeout();
                        this.mDockAddress = address;
                    } else if (hasScheduledA2dpDockTimeout()) {
                        cancelA2dpDeviceTimeout();
                        log("[GJ_DEBUG] onSetA2dpSinkConnectionState: makeA2dpDeviceUnavailableNow ----2");
                        makeA2dpDeviceUnavailableNow(this.mDockAddress);
                    }
                    makeA2dpDeviceAvailable(address);
                    synchronized (this.mCurAudioRoutes) {
                        String name = btDevice.getAliasName();
                        if (!TextUtils.equals(this.mCurAudioRoutes.mBluetoothName, name)) {
                            this.mCurAudioRoutes.mBluetoothName = name;
                            this.mCurAudioRoutes.mBluetoothAddress = address;
                            sendMsg(this.mAudioHandler, 12, 1, 0, 0, null, 0);
                        }
                    }
                }
            }
        }
    }

    private void onSetA2dpSourceConnectionState(BluetoothDevice btDevice, int state) {
        log("onSetA2dpSourceConnectionState btDevice=" + btDevice + " state=" + state);
        if (btDevice != null) {
            String address = btDevice.getAddress();
            if (!BluetoothAdapter.checkBluetoothAddress(address)) {
                address = ProxyInfo.LOCAL_EXCL_LIST;
            }
            synchronized (this.mConnectedDevices) {
                boolean isConnected = this.mConnectedDevices.containsKey(Integer.valueOf(AudioSystem.DEVICE_IN_BLUETOOTH_A2DP)) && ((String) this.mConnectedDevices.get(Integer.valueOf(AudioSystem.DEVICE_IN_BLUETOOTH_A2DP))).equals(address);
                if (isConnected && state != 2) {
                    makeA2dpSrcUnavailable(address);
                } else if (!isConnected && state == 2) {
                    makeA2dpSrcAvailable(address);
                }
            }
        }
    }

    public void avrcpSupportsAbsoluteVolume(String address, boolean support) {
        synchronized (this.mA2dpAvrcpLock) {
            this.mAvrcpAbsVolSupported = support;
            sendMsg(this.mAudioHandler, 0, 2, 128, 0, this.mStreamStates[3], 0);
            sendMsg(this.mAudioHandler, 0, 2, 128, 0, this.mStreamStates[2], 0);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean handleDeviceConnection(boolean connected, int device, String params) {
        synchronized (this.mConnectedDevices) {
            boolean isConnected;
            if (this.mConnectedDevices.containsKey(Integer.valueOf(device)) && (params.isEmpty() || ((String) this.mConnectedDevices.get(Integer.valueOf(device))).equals(params))) {
                isConnected = true;
            } else {
                isConnected = false;
            }
            log("handleDeviceConnection:isConnected" + isConnected + ",connected:" + connected + ",device:" + device + ",params:" + params);
            if (isConnected && !connected) {
                AudioSystem.setDeviceConnectionState(device, 0, (String) this.mConnectedDevices.get(Integer.valueOf(device)));
                log("handleDeviceConnection remove:connected:" + connected + ",device:" + device);
                this.mConnectedDevices.remove(Integer.valueOf(device));
                return true;
            } else if (isConnected || !connected) {
            } else {
                AudioSystem.setDeviceConnectionState(device, 1, params);
                log("handleDeviceConnection connect:connected:" + connected + ",device:" + device);
                this.mConnectedDevices.put(new Integer(device), params);
                return true;
            }
        }
    }

    private int checkSendBecomingNoisyIntent(int device, int state) {
        int delay = 0;
        if (state == 0 && (this.mBecomingNoisyIntentDevices & device) != 0) {
            int devices = 0;
            for (Integer intValue : this.mConnectedDevices.keySet()) {
                int dev = intValue.intValue();
                if ((Integer.MIN_VALUE & dev) == 0 && (this.mBecomingNoisyIntentDevices & dev) != 0) {
                    devices |= dev;
                }
            }
            if (devices == device) {
                sendMsg(this.mAudioHandler, 15, 0, 0, 0, null, 0);
                delay = 1000;
            }
        }
        if (this.mAudioHandler.hasMessages(101) || this.mAudioHandler.hasMessages(102) || this.mAudioHandler.hasMessages(26) || this.mAudioHandler.hasMessages(100)) {
            synchronized (mLastDeviceConnectMsgTime) {
                long time = SystemClock.uptimeMillis();
                if (mLastDeviceConnectMsgTime.longValue() > time) {
                    delay = ((int) (mLastDeviceConnectMsgTime.longValue() - time)) + 30;
                }
            }
        }
        log("[GJ_DEBUG] checkSendBecomingNoisyIntent: device = " + device + "; state = " + state + "; delay = " + delay);
        return delay;
    }

    private void sendDeviceConnectionIntent(int device, int state, String name) {
        Intent intent = new Intent();
        intent.putExtra("state", state);
        intent.putExtra("name", name);
        intent.addFlags(1073741824);
        int connType = 0;
        if (device == 4) {
            connType = 1;
            intent.setAction("android.intent.action.HEADSET_PLUG");
            intent.putExtra("microphone", 1);
        } else if (device == 8 || device == 131072) {
            connType = 2;
            intent.setAction("android.intent.action.HEADSET_PLUG");
            intent.putExtra("microphone", 0);
        } else if (device == 2048) {
            connType = 4;
            intent.setAction(AudioManager.ACTION_ANALOG_AUDIO_DOCK_PLUG);
        } else if (device == 4096) {
            connType = 4;
            intent.setAction(AudioManager.ACTION_DIGITAL_AUDIO_DOCK_PLUG);
        } else if (device == 1024 || device == 262144) {
            connType = 8;
            configureHdmiPlugIntent(intent, state);
        }
        synchronized (this.mCurAudioRoutes) {
            if (connType != 0) {
                int newConn = this.mCurAudioRoutes.mMainType;
                if (state != 0) {
                    newConn |= connType;
                } else {
                    newConn &= connType ^ -1;
                }
                if (newConn != this.mCurAudioRoutes.mMainType) {
                    this.mCurAudioRoutes.mMainType = newConn;
                    sendMsg(this.mAudioHandler, 12, 1, 0, 0, null, 0);
                }
            }
        }
        long ident = Binder.clearCallingIdentity();
        try {
            ActivityManagerNative.broadcastStickyIntent(intent, null, -1);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void onSetWiredDeviceConnectionState(int device, int state, String name) {
        boolean z = false;
        synchronized (this.mConnectedDevices) {
            boolean isUsb;
            log("onSetWiredDeviceConnectionState:device:" + device + ",state:" + state);
            if (state == 0 && (device == 4 || device == 8 || device == 131072)) {
                setBluetoothA2dpOnInt(true);
            }
            if ((device & -24577) == 0 || ((Integer.MIN_VALUE & device) != 0 && (2147477503 & device) == 0)) {
                isUsb = true;
            } else {
                isUsb = false;
            }
            if ((device & -24577) == 0) {
                if (state == 1) {
                    MzAddUsbAudioIntent();
                } else if (state == 0) {
                    MzRemoveUsbAudioIntent();
                }
            }
            if (state == 1) {
                z = true;
            }
            handleDeviceConnection(z, device, isUsb ? name : ProxyInfo.LOCAL_EXCL_LIST);
            if (state != 0) {
                if (device == 4 || device == 8 || device == 131072) {
                    setBluetoothA2dpOnInt(false);
                }
                if ((device & 0) != 0) {
                    sendMsg(this.mAudioHandler, 14, 0, 0, 0, null, MUSIC_ACTIVE_POLL_PERIOD_MS);
                }
                if (isPlatformTelevision() && (device & 1024) != 0) {
                    this.mFixedVolumeDevices |= 1024;
                    checkAllFixedVolumeDevices();
                    if (this.mHdmiManager != null) {
                        synchronized (this.mHdmiManager) {
                            if (this.mHdmiPlaybackClient != null) {
                                this.mHdmiCecSink = false;
                                this.mHdmiPlaybackClient.queryDisplayStatus(this.mHdmiDisplayStatusCallback);
                            }
                        }
                    }
                }
            } else if (!(!isPlatformTelevision() || (device & 1024) == 0 || this.mHdmiManager == null)) {
                synchronized (this.mHdmiManager) {
                    this.mHdmiCecSink = false;
                }
            }
            if (!(isUsb || device == -2147483632)) {
                sendDeviceConnectionIntent(device, state, name);
            }
//BZZ1
            if (device == 4 || device == 8) {
                MzReadAndSetHifiParam();
            }
        }
    }

    private void configureHdmiPlugIntent(Intent intent, int state) {
        intent.setAction(AudioManager.ACTION_HDMI_AUDIO_PLUG);
        intent.putExtra(AudioManager.EXTRA_AUDIO_PLUG_STATE, state);
        if (state == 1) {
            ArrayList<AudioPort> ports = new ArrayList();
            if (AudioSystem.listAudioPorts(ports, new int[1]) == 0) {
                Iterator it = ports.iterator();
                while (it.hasNext()) {
                    AudioPort port = (AudioPort) it.next();
                    if (port instanceof AudioDevicePort) {
                        AudioDevicePort devicePort = (AudioDevicePort) port;
                        if (devicePort.type() == 1024 || devicePort.type() == 262144) {
                            int[] formats = devicePort.formats();
                            if (formats.length > 0) {
                                ArrayList<Integer> encodingList = new ArrayList(1);
                                for (int format : formats) {
                                    if (format != 0) {
                                        encodingList.add(Integer.valueOf(format));
                                    }
                                }
                                int[] encodingArray = new int[encodingList.size()];
                                for (int i = 0; i < encodingArray.length; i++) {
                                    encodingArray[i] = ((Integer) encodingList.get(i)).intValue();
                                }
                                intent.putExtra(AudioManager.EXTRA_ENCODINGS, encodingArray);
                            }
                            int maxChannels = 0;
                            for (int mask : devicePort.channelMasks()) {
                                int channelCount = AudioFormat.channelCountFromOutChannelMask(mask);
                                if (channelCount > maxChannels) {
                                    maxChannels = channelCount;
                                }
                            }
                            intent.putExtra(AudioManager.EXTRA_MAX_CHANNEL_COUNT, maxChannels);
                        }
                    }
                }
            }
        }
    }

    public boolean registerRemoteController(IRemoteControlDisplay rcd, int w, int h, ComponentName listenerComp) {
        return this.mMediaFocusControl.registerRemoteController(rcd, w, h, listenerComp);
    }

    public boolean registerRemoteControlDisplay(IRemoteControlDisplay rcd, int w, int h) {
        return this.mMediaFocusControl.registerRemoteControlDisplay(rcd, w, h);
    }

    public void unregisterRemoteControlDisplay(IRemoteControlDisplay rcd) {
        this.mMediaFocusControl.unregisterRemoteControlDisplay(rcd);
    }

    public void remoteControlDisplayUsesBitmapSize(IRemoteControlDisplay rcd, int w, int h) {
        this.mMediaFocusControl.remoteControlDisplayUsesBitmapSize(rcd, w, h);
    }

    public void remoteControlDisplayWantsPlaybackPositionSync(IRemoteControlDisplay rcd, boolean wantsSync) {
        this.mMediaFocusControl.remoteControlDisplayWantsPlaybackPositionSync(rcd, wantsSync);
    }

    public void setRemoteStreamVolume(int index) {
        enforceSelfOrSystemUI("set the remote stream volume");
        this.mMediaFocusControl.setRemoteStreamVolume(index);
    }

    public int requestAudioFocus(AudioAttributes aa, int durationHint, IBinder cb, IAudioFocusDispatcher fd, String clientId, String callingPackageName, int flags, IAudioPolicyCallback pcb) {
        if ((flags & 4) == 4) {
            MediaFocusControl mediaFocusControl = this.mMediaFocusControl;
            if (!"AudioFocus_For_Phone_Ring_And_Calls".equals(clientId)) {
                synchronized (this.mAudioPolicies) {
                    if (!this.mAudioPolicies.containsKey(pcb.asBinder())) {
                        Log.m26e(TAG, "Invalid unregistered AudioPolicy to (un)lock audio focus");
                        return 0;
                    }
                }
            } else if (this.mContext.checkCallingOrSelfPermission(Manifest$permission.MODIFY_PHONE_STATE) != 0) {
                Log.m27e(TAG, "Invalid permission to (un)lock audio focus", new Exception());
                return 0;
            }
        }
        return this.mMediaFocusControl.requestAudioFocus(aa, durationHint, cb, fd, clientId, callingPackageName, flags);
    }

    public int abandonAudioFocus(IAudioFocusDispatcher fd, String clientId, AudioAttributes aa) {
        return this.mMediaFocusControl.abandonAudioFocus(fd, clientId, aa);
    }

    public void unregisterAudioFocusClient(String clientId) {
        this.mMediaFocusControl.unregisterAudioFocusClient(clientId);
    }

    public int getCurrentAudioFocus() {
        return this.mMediaFocusControl.getCurrentAudioFocus();
    }

    private void handleConfigurationChanged(Context context) {
        try {
            Configuration config = context.getResources().getConfiguration();
            if (this.mMonitorOrientation) {
                int newOrientation = config.orientation;
                if (newOrientation != this.mDeviceOrientation) {
                    this.mDeviceOrientation = newOrientation;
                    setOrientationForAudioSystem();
                }
            }
            sendMsg(this.mAudioHandler, 16, 0, 0, 0, null, 0);
            boolean cameraSoundForced = this.mContext.getResources().getBoolean(C0833R.bool.config_camera_sound_forced);
            synchronized (this.mSettingsLock) {
                boolean cameraSoundForcedChanged = false;
                synchronized (this.mCameraSoundForced) {
                    if (cameraSoundForced != this.mCameraSoundForced.booleanValue()) {
                        this.mCameraSoundForced = Boolean.valueOf(cameraSoundForced);
                        cameraSoundForcedChanged = true;
                    }
                }
                if (cameraSoundForcedChanged) {
                    if (!isPlatformTelevision()) {
                        VolumeStreamState s = this.mStreamStates[7];
                        if (cameraSoundForced) {
                            s.setAllIndexesToMax();
                            this.mRingerModeAffectedStreams &= -129;
                        } else {
                            s.setAllIndexes(this.mStreamStates[1]);
                            this.mRingerModeAffectedStreams |= 128;
                        }
                        setRingerModeInt(getRingerModeInternal(), false);
                    }
                    sendMsg(this.mAudioHandler, 8, 2, 4, cameraSoundForced ? 11 : 0, null, 0);
                    sendMsg(this.mAudioHandler, 10, 2, 0, 0, this.mStreamStates[7], 0);
                }
            }
            this.mVolumeController.setLayoutDirection(config.getLayoutDirection());
        } catch (Exception e) {
            Log.m27e(TAG, "Error handling configuration change: ", e);
        }
    }

    private void setOrientationForAudioSystem() {
        switch (this.mDeviceOrientation) {
            case 0:
                AudioSystem.setParameters("orientation=undefined");
                return;
            case 1:
                AudioSystem.setParameters("orientation=portrait");
                return;
            case 2:
                AudioSystem.setParameters("orientation=landscape");
                return;
            case 3:
                AudioSystem.setParameters("orientation=square");
                return;
            default:
                Log.m26e(TAG, "Unknown orientation");
                return;
        }
    }

    private void setRotationForAudioSystem() {
        switch (this.mDeviceRotation) {
            case 0:
                AudioSystem.setParameters("rotation=0");
                return;
            case 1:
                AudioSystem.setParameters("rotation=90");
                return;
            case 2:
                AudioSystem.setParameters("rotation=180");
                return;
            case 3:
                AudioSystem.setParameters("rotation=270");
                return;
            default:
                Log.m26e(TAG, "Unknown device rotation");
                return;
        }
    }

    public void setBluetoothA2dpOnInt(boolean on) {
        synchronized (this.mBluetoothA2dpEnabledLock) {
            this.mBluetoothA2dpEnabled = on;
            this.mAudioHandler.removeMessages(13);
            AudioSystem.setForceUse(1, this.mBluetoothA2dpEnabled ? 0 : 10);
        }
    }

    public void setRingtonePlayer(IRingtonePlayer player) {
        this.mContext.enforceCallingOrSelfPermission(Manifest$permission.REMOTE_AUDIO_PLAYBACK, null);
        this.mRingtonePlayer = player;
    }

    public IRingtonePlayer getRingtonePlayer() {
        return this.mRingtonePlayer;
    }

    public AudioRoutesInfo startWatchingRoutes(IAudioRoutesObserver observer) {
        AudioRoutesInfo routes;
        synchronized (this.mCurAudioRoutes) {
            routes = new AudioRoutesInfo(this.mCurAudioRoutes);
            this.mRoutesObservers.register(observer);
        }
        return routes;
    }

    private void setSafeMediaVolumeEnabled(boolean on) {
        synchronized (this.mSafeMediaVolumeState) {
            if (!(this.mSafeMediaVolumeState.intValue() == 0 || this.mSafeMediaVolumeState.intValue() == 1)) {
                if (on && this.mSafeMediaVolumeState.intValue() == 2) {
                    this.mSafeMediaVolumeState = Integer.valueOf(3);
                    enforceSafeMediaVolume();
                } else if (!on && this.mSafeMediaVolumeState.intValue() == 3) {
                    this.mSafeMediaVolumeState = Integer.valueOf(2);
                    this.mMusicActiveMs = 1;
                    saveMusicActiveMs();
                    sendMsg(this.mAudioHandler, 14, 0, 0, 0, null, MUSIC_ACTIVE_POLL_PERIOD_MS);
                }
            }
        }
    }

    private void enforceSafeMediaVolume() {
        VolumeStreamState streamState = this.mStreamStates[3];
        int devices = 0;
        int i = 0;
        while (devices != 0) {
            int i2 = i + 1;
            int device = 1 << i;
            if ((device & devices) == 0) {
                i = i2;
            } else {
                if (streamState.getIndex(device) > this.mSafeMediaVolumeIndex) {
                    streamState.setIndex(this.mSafeMediaVolumeIndex, device);
                    sendMsg(this.mAudioHandler, 0, 2, device, 0, streamState, 0);
                }
                devices &= device ^ -1;
                i = i2;
            }
        }
    }

    private boolean checkSafeMediaVolume(int streamType, int index, int device) {
        boolean z;
        synchronized (this.mSafeMediaVolumeState) {
            if (this.mSafeMediaVolumeState.intValue() != 3 || this.mStreamVolumeAlias[streamType] != 3 || (device & 0) == 0 || index <= this.mSafeMediaVolumeIndex) {
                z = true;
            } else {
                z = false;
            }
        }
        return z;
    }

    public void disableSafeMediaVolume() {
        enforceSelfOrSystemUI("disable the safe media volume");
        synchronized (this.mSafeMediaVolumeState) {
            setSafeMediaVolumeEnabled(false);
            if (this.mPendingVolumeCommand != null) {
                onSetStreamVolume(this.mPendingVolumeCommand.mStreamType, this.mPendingVolumeCommand.mIndex, this.mPendingVolumeCommand.mFlags, this.mPendingVolumeCommand.mDevice);
                this.mPendingVolumeCommand = null;
            }
        }
    }

    public int setHdmiSystemAudioSupported(boolean on) {
        int device = 0;
        if (this.mHdmiManager != null) {
            synchronized (this.mHdmiManager) {
                if (this.mHdmiTvClient == null) {
                    Log.m32w(TAG, "Only Hdmi-Cec enabled TV device supports system audio mode.");
                    return 0;
                }
                synchronized (this.mHdmiTvClient) {
                    if (this.mHdmiSystemAudioSupported != on) {
                        this.mHdmiSystemAudioSupported = on;
                        AudioSystem.setForceUse(5, on ? 12 : 0);
                    }
                    device = AudioSystem.getDevicesForStream(3);
                }
            }
        }
        return device;
    }

    public boolean isHdmiSystemAudioSupported() {
        return this.mHdmiSystemAudioSupported;
    }

    public boolean isCameraSoundForced() {
        boolean booleanValue;
        synchronized (this.mCameraSoundForced) {
            booleanValue = this.mCameraSoundForced.booleanValue();
        }
        return booleanValue;
    }

    private void dumpRingerMode(PrintWriter pw) {
        pw.println("\nRinger mode: ");
        pw.println("- mode (internal) = " + RINGER_MODE_NAMES[this.mRingerMode]);
        pw.println("- mode (external) = " + RINGER_MODE_NAMES[this.mRingerModeExternal]);
        pw.print("- ringer mode affected streams = 0x");
        pw.println(Integer.toHexString(this.mRingerModeAffectedStreams));
        pw.print("- ringer mode muted streams = 0x");
        pw.println(Integer.toHexString(this.mRingerModeMutedStreams));
        pw.print("- delegate = ");
        pw.println(this.mRingerModeDelegate);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mContext.enforceCallingOrSelfPermission(Manifest$permission.DUMP, TAG);
        this.mMediaFocusControl.dump(pw);
        dumpStreamStates(pw);
        dumpRingerMode(pw);
        pw.println("\nmMode=" + this.mMode);
        pw.println("\nStream alias:");
        for (int i = 0; i < this.mStreamVolumeAlias.length; i++) {
            pw.print("  [" + i + "]");
            pw.println(streamToString(this.mStreamVolumeAlias[i]));
        }
        pw.println("\nAudio routes:");
        pw.print("  mMainType=0x");
        pw.println(Integer.toHexString(this.mCurAudioRoutes.mMainType));
        pw.print("  mBluetoothName=");
        pw.println(this.mCurAudioRoutes.mBluetoothName);
        pw.println("\nOther state:");
        pw.print("  mVolumeController=");
        pw.println(this.mVolumeController);
        pw.print("  mSafeMediaVolumeState=");
        pw.println(safeMediaVolumeStateToString(this.mSafeMediaVolumeState));
        pw.print("  mSafeMediaVolumeIndex=");
        pw.println(this.mSafeMediaVolumeIndex);
        pw.print("  mPendingVolumeCommand=");
        pw.println(this.mPendingVolumeCommand);
        pw.print("  mMusicActiveMs=");
        pw.println(this.mMusicActiveMs);
        pw.print("  mMcc=");
        pw.println(this.mMcc);
        pw.print("  mHasVibrator=");
        pw.println(this.mHasVibrator);
        dumpAudioPolicies(pw);
    }

    private static String safeMediaVolumeStateToString(Integer state) {
        switch (state.intValue()) {
            case 0:
                return "SAFE_MEDIA_VOLUME_NOT_CONFIGURED";
            case 1:
                return "SAFE_MEDIA_VOLUME_DISABLED";
            case 2:
                return "SAFE_MEDIA_VOLUME_INACTIVE";
            case 3:
                return "SAFE_MEDIA_VOLUME_ACTIVE";
            default:
                return null;
        }
    }

    private static void readAndSetLowRamDevice() {
        int status = AudioSystem.setLowRamDevice(ActivityManager.isLowRamDeviceStatic());
        if (status != 0) {
            Log.m32w(TAG, "AudioFlinger informed of device's low RAM attribute; status " + status);
        }
    }

    private void enforceSelfOrSystemUI(String action) {
        this.mContext.enforceCallingOrSelfPermission(Manifest$permission.STATUS_BAR_SERVICE, "Only SystemUI can " + action);
    }

    public void setVolumeController(final IVolumeController controller) {
        enforceSelfOrSystemUI("set the volume controller");
        if (!this.mVolumeController.isSameBinder(controller)) {
            this.mVolumeController.postDismiss();
            if (controller != null) {
                log("[GJ_DEBUG] setVolumeController: controller != null");
                try {
                    controller.asBinder().linkToDeath(new DeathRecipient() {
                        public void binderDied() {
                            if (AudioService.this.mVolumeController.isSameBinder(controller)) {
                                Log.m32w(AudioService.TAG, "Current remote volume controller died, unregistering");
                                AudioService.this.setVolumeController(null);
                            }
                        }
                    }, 0);
                } catch (RemoteException e) {
                }
            }
            this.mVolumeController.setController(controller);
            log("[GJ_DEBUG] Volume controller: " + this.mVolumeController);
        }
    }

    public void notifyVolumeControllerVisible(IVolumeController controller, boolean visible) {
        enforceSelfOrSystemUI("notify about volume controller visibility");
        if (this.mVolumeController.isSameBinder(controller)) {
            this.mVolumeController.setVisible(visible);
            log("Volume controller visible: " + visible);
        }
    }

    public String registerAudioPolicy(AudioPolicyConfig policyConfig, IAudioPolicyCallback pcb, boolean hasFocusListener) {
        boolean hasPermissionForPolicy = false;
        if (DEBUG_AP) {
            log("registerAudioPolicy for " + pcb.asBinder() + " with config:" + policyConfig);
        }
        if (this.mContext.checkCallingPermission(Manifest$permission.MODIFY_AUDIO_ROUTING) == 0) {
            hasPermissionForPolicy = true;
        }
        if (hasPermissionForPolicy) {
            synchronized (this.mAudioPolicies) {
                try {
                    if (this.mAudioPolicies.containsKey(pcb.asBinder())) {
                        Slog.m37e(TAG, "Cannot re-register policy");
                        return null;
                    }
                    AudioPolicyProxy app = new AudioPolicyProxy(policyConfig, pcb, hasFocusListener);
                    pcb.asBinder().linkToDeath(app, 0);
                    String regId = app.getRegistrationId();
                    this.mAudioPolicies.put(pcb.asBinder(), app);
                    return regId;
                } catch (RemoteException e) {
                    Slog.m44w(TAG, "Audio policy registration failed, could not link to " + pcb + " binder death", e);
                    return null;
                }
            }
        }
        Slog.m43w(TAG, "Can't register audio policy for pid " + Binder.getCallingPid() + " / uid " + Binder.getCallingUid() + ", need MODIFY_AUDIO_ROUTING");
        return null;
    }

    public void unregisterAudioPolicyAsync(IAudioPolicyCallback pcb) {
        if (DEBUG_AP) {
            log("unregisterAudioPolicyAsync for " + pcb.asBinder());
        }
        synchronized (this.mAudioPolicies) {
            AudioPolicyProxy app = (AudioPolicyProxy) this.mAudioPolicies.remove(pcb.asBinder());
            if (app == null) {
                Slog.m43w(TAG, "Trying to unregister unknown audio policy for pid " + Binder.getCallingPid() + " / uid " + Binder.getCallingUid());
                return;
            }
            pcb.asBinder().unlinkToDeath(app, 0);
            app.release();
        }
    }

    public int setFocusPropertiesForPolicy(int duckingBehavior, IAudioPolicyCallback pcb) {
        boolean hasPermissionForPolicy;
        if (DEBUG_AP) {
            log("setFocusPropertiesForPolicy() duck behavior=" + duckingBehavior + " policy " + pcb.asBinder());
        }
        if (this.mContext.checkCallingPermission(Manifest$permission.MODIFY_AUDIO_ROUTING) == 0) {
            hasPermissionForPolicy = true;
        } else {
            hasPermissionForPolicy = false;
        }
        if (hasPermissionForPolicy) {
            synchronized (this.mAudioPolicies) {
                if (this.mAudioPolicies.containsKey(pcb.asBinder())) {
                    boolean z;
                    AudioPolicyProxy app = (AudioPolicyProxy) this.mAudioPolicies.get(pcb.asBinder());
                    if (duckingBehavior == 1) {
                        for (AudioPolicyProxy policy : this.mAudioPolicies.values()) {
                            if (policy.mFocusDuckBehavior == 1) {
                                Slog.m37e(TAG, "Cannot change audio policy ducking behavior, already handled");
                                return -1;
                            }
                        }
                    }
                    app.mFocusDuckBehavior = duckingBehavior;
                    MediaFocusControl mediaFocusControl = this.mMediaFocusControl;
                    if (duckingBehavior == 1) {
                        z = true;
                    } else {
                        z = false;
                    }
                    mediaFocusControl.setDuckingInExtPolicyAvailable(z);
                    return 0;
                }
                Slog.m37e(TAG, "Cannot change audio policy focus properties, unregistered policy");
                return -1;
            }
        }
        Slog.m43w(TAG, "Cannot change audio policy ducking handling for pid " + Binder.getCallingPid() + " / uid " + Binder.getCallingUid() + ", need MODIFY_AUDIO_ROUTING");
        return -1;
    }

    private void dumpAudioPolicies(PrintWriter pw) {
        pw.println("\nAudio policies:");
        synchronized (this.mAudioPolicies) {
            for (AudioPolicyProxy policy : this.mAudioPolicies.values()) {
                pw.println(policy.toLogFriendlyString());
            }
        }
    }

    private boolean MzcheckResumeRingerModeNormal(int streamType, boolean isUnMute) {
        log("MzcheckResumeRingerModeNormal: streamType = " + streamType + "; isUnMute = " + isUnMute);
        if (((1 << streamType) & mCanResumeNormalStreams) == 0 || !isUnMute || (getRingerModeInternal() != 0 && getRingerModeInternal() != 1)) {
            return false;
        }
        setRingerModeInt(2, true);
        setRingerModeExt(2);
        return true;
    }

    private void MzReadAndSetHifiParam() {
        String hifiEnable = SystemProperties.get("ro.meizu.hardware.hifi", "false");
        log("MzReadAndSetHifiParam: hifiEnable = " + hifiEnable);
        if (hifiEnable.equals("true")) {
            String gainKeyPairs = "hifi_gain=" + System.getIntForUser(this.mContentResolver, MzSettings.System.HIFI_MUSIC_PARAM, 0, -2);
            synchronized (this.mConnectedDevices) {
                if ((this.mConnectedDevices.containsKey(Integer.valueOf(4)) || this.mConnectedDevices.containsKey(Integer.valueOf(8))) && true) {
                    AudioSystem.setParameters("hifi_state=on");
                    AudioSystem.setParameters(gainKeyPairs);
                    log("MzReadAndSetHifiParam: hifi_state=on | " + gainKeyPairs);
                } else {
                    AudioSystem.setParameters("hifi_state=off");
                    log("MzReadAndSetHifiParam: hifi_state=off");
                }
            }
        }
    }

    protected void MzAddUsbAudioIntent() {
        Resources r = this.mContext.getResources();
        CharSequence title = r.getString(C0997R.string.usbaudio_title);
        CharSequence message = r.getString(C0997R.string.usbaudio_subtitle);
        Notification notification = new Notification();
        notification.icon = C0997R.drawable.mz_stat_sys_usbaudio;
        notification.mFlymeNotification.notificationIcon = C0997R.drawable.mz_status_ic_usbaudio;
        notification.tickerText = title;
        notification.when = 0;
        notification.defaults = 0;
        notification.sound = null;
        notification.vibrate = null;
        notification.flags |= 2;
        Intent intent = new Intent();
        intent.addFlags(872415232);
        notification.setLatestEventInfo(this.mContext, title, message, PendingIntent.getActivity(this.mContext, 0, intent, 0));
        this.mNotificationManager.notify(C0997R.drawable.mz_status_ic_usbaudio, notification);
    }

    protected void MzRemoveUsbAudioIntent() {
        this.mNotificationManager.cancel(C0997R.drawable.mz_status_ic_usbaudio);
    }

    public void mzSwitchBluetoothToDevice(int device) {
        if (checkAudioSettingsPermission("mzSwitchBluetoothToDevice()")) {
            if (device == 2) {
                this.mForcedUseForComm = 1;
            } else {
                this.mForcedUseForComm = 0;
            }
            sendMsg(this.mAudioHandler, 8, 2, 0, this.mForcedUseForComm, null, 0);
            sendMsg(this.mAudioHandler, 8, 2, 2, 0, null, 0);
        }
    }

    static void log(String s) {
        Log.m28i(TAG, s);
    }
}
