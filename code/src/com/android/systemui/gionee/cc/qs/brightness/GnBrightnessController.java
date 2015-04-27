/*
*
* MODULE DESCRIPTION
* add by huangwt for Android L at 20141210.
* 
*/

package com.android.systemui.gionee.cc.qs.brightness;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.android.systemui.R;
import com.android.systemui.gionee.cc.qs.brightness.GnToggleSlider.Listener;
import com.android.systemui.gionee.GnYouJu;
import java.util.ArrayList;

public class GnBrightnessController implements GnToggleSlider.Listener {
    private static final String TAG = "StatusBar.BrightnessController";
    private static final boolean SHOW_AUTOMATIC_ICON = true;

    /**
     * {@link android.provider.Settings.System#SCREEN_AUTO_BRIGHTNESS_ADJ} uses the range [-1, 1].
     * Using this factor, it is converted to [0, BRIGHTNESS_ADJ_RESOLUTION] for the SeekBar.
     */
    private static final float BRIGHTNESS_ADJ_RESOLUTION = 100;

    private final int mMinimumBacklight;
    private final int mMaximumBacklight;
    private final int mDefaultBackLight;

    private final Context mContext;
    private final ImageView mIcon;
    private final GnToggleSlider mControl;
    private final boolean mAutomaticAvailable;
    private final IPowerManager mPower;
    private final GnCurrentUserTracker mUserTracker;
    private final Handler mHandler;
    private final BrightnessObserver mBrightnessObserver;

    private ArrayList<BrightnessStateChangeCallback> mChangeCallbacks =
            new ArrayList<BrightnessStateChangeCallback>();

    private boolean mAutomatic;
    private boolean mListening;
    private boolean mExternalChange;

    public interface BrightnessStateChangeCallback {
        public void onBrightnessLevelChanged();
    }

    /** ContentObserver to watch brightness **/
    private class BrightnessObserver extends ContentObserver {

        private final Uri BRIGHTNESS_MODE_URI =
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE);
        private final Uri BRIGHTNESS_URI =
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS);
        private final Uri BRIGHTNESS_ADJ_URI =
                Settings.System.getUriFor(Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ);

        public BrightnessObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (selfChange) return;
            try {
                mExternalChange = true;
                if (BRIGHTNESS_MODE_URI.equals(uri)) {
                    updateMode();
                    updateIcon();
                    updateSlider();
                } else if (BRIGHTNESS_URI.equals(uri) && !mAutomatic) {
                    updateIcon();
                    updateSlider();
                } else if (BRIGHTNESS_ADJ_URI.equals(uri) && mAutomatic) {
                    updateIcon();
                    updateSlider();
                } else {
                    updateMode();
                    updateIcon();
                    updateSlider();
                }
                for (BrightnessStateChangeCallback cb : mChangeCallbacks) {
                    cb.onBrightnessLevelChanged();
                }
            } finally {
                mExternalChange = false;
            }
        }

        public void startObserving() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.unregisterContentObserver(this);
            cr.registerContentObserver(
                    BRIGHTNESS_MODE_URI,
                    false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(
                    BRIGHTNESS_URI,
                    false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(
                    BRIGHTNESS_ADJ_URI,
                    false, this, UserHandle.USER_ALL);
        }

        public void stopObserving() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.unregisterContentObserver(this);
        }

    }

    public GnBrightnessController(Context context, ImageView icon, GnToggleSlider control) {
        mContext = context;
        mControl = control;
        mIcon = icon;
        mIcon.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
            	GnYouJu.onEvent(mContext, "Amigo_SystemUI_CC", "mBrightnessIcon_Clicked");
                int brightness = getBrightnessValue();
                int brightnessLevel = getBrightnessLevel();
                int brightnessMode = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
                
                int oldBrightnessMode;
                if (mAutomatic) {
                    oldBrightnessMode = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
                } else {
                    oldBrightnessMode = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
                }
                
                if (mAutomatic) {
                    brightnessMode = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
                    brightness = mMinimumBacklight;
                } else {
                    if (brightnessLevel == mMinimumBacklight) {
                        brightness = mDefaultBackLight;
                    } else if (brightnessLevel == mDefaultBackLight) {
                        brightness = mMaximumBacklight;
                    } else {
                        brightness = mMaximumBacklight;
                        brightnessMode = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
                    }
                }

                if (oldBrightnessMode != brightnessMode) {
                    Settings.System.putIntForUser(mContext.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE, brightnessMode,
                            UserHandle.USER_CURRENT);
                }
                    
                final int newBrightness = brightness;
                if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) {
                    setBrightness(newBrightness);
                    
                    AsyncTask.execute(new Runnable() {
                        public void run() {
                            Settings.System.putIntForUser(mContext.getContentResolver(),
                                    Settings.System.SCREEN_BRIGHTNESS, newBrightness,
                                    UserHandle.USER_CURRENT);
                        }
                    });
                }
            }
        });
        
        mHandler = new Handler();
        mUserTracker = new GnCurrentUserTracker(mContext) {
            @Override
            public void onUserSwitched(int newUserId) {
                updateMode();
                updateIcon();
                updateSlider();
            }
        };
        mBrightnessObserver = new BrightnessObserver(mHandler);

        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mMinimumBacklight = pm.getMinimumScreenBrightnessSetting();
        mMaximumBacklight = pm.getMaximumScreenBrightnessSetting();
        mDefaultBackLight = pm.getDefaultScreenBrightnessSetting();

        mAutomaticAvailable = context.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);
        mPower = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
    }

    public void addStateChangedCallback(BrightnessStateChangeCallback cb) {
        mChangeCallbacks.add(cb);
    }

    public boolean removeStateChangedCallback(BrightnessStateChangeCallback cb) {
        return mChangeCallbacks.remove(cb);
    }

    @Override
    public void onInit(GnToggleSlider control) {
        // Do nothing
    }

    public void registerCallbacks() {
        if (mListening) {
            return;
        }

        mBrightnessObserver.startObserving();
        mUserTracker.startTracking();

        // Update the slider and mode before attaching the listener so we don't
        // receive the onChanged notifications for the initial values.
        updateMode();
        updateIcon();
        updateSlider();

        mControl.setOnChangedListener(this);
        mListening = true;
    }

    /** Unregister all call backs, both to and from the controller */
    public void unregisterCallbacks() {
        if (!mListening) {
            return;
        }

        mBrightnessObserver.stopObserving();
        mUserTracker.stopTracking();
        mControl.setOnChangedListener(null);
        mListening = false;
    }

    @Override
    public void onChanged(GnToggleSlider view, boolean tracking, boolean automatic, int value) {        
        updateIcon();
        
        if (mExternalChange) return;

        if (!mAutomatic) {
            final int val = value + mMinimumBacklight;
            setBrightness(val);
            if (!tracking) {
                AsyncTask.execute(new Runnable() {
                        public void run() {
                            Settings.System.putIntForUser(mContext.getContentResolver(),
                                    Settings.System.SCREEN_BRIGHTNESS, val,
                                    UserHandle.USER_CURRENT);
                        }
                    });
            }
        } else {
            final float adj = value / (BRIGHTNESS_ADJ_RESOLUTION / 2f) - 1;
            setBrightnessAdj(adj);
            if (!tracking) {
                AsyncTask.execute(new Runnable() {
                    public void run() {
                        Settings.System.putFloatForUser(mContext.getContentResolver(),
                                Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, adj,
                                UserHandle.USER_CURRENT);
                    }
                });
            }
        }

        for (BrightnessStateChangeCallback cb : mChangeCallbacks) {
            cb.onBrightnessLevelChanged();
        }
    }

    private void setMode(int mode) {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, mode,
                mUserTracker.getCurrentUserId());
    }

    private void setBrightness(int brightness) {
        try {
            mPower.setTemporaryScreenBrightnessSettingOverride(brightness);
        } catch (RemoteException ex) {
        }
    }

    private void setBrightnessAdj(float adj) {
        try {
            mPower.setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(adj);
        } catch (RemoteException ex) {
        }
    }

    private void updateIcon() {

        if (mAutomatic) {
            mIcon.setImageResource(R.drawable.gn_ic_qs_brightness_auto);
        } else {
            int brightness = getBrightnessLevel();
            
            if (brightness == mMinimumBacklight) {                
                mIcon.setImageResource(R.drawable.gn_ic_qs_brightness_low);
            } else if (brightness == mMaximumBacklight) {
                mIcon.setImageResource(R.drawable.gn_ic_qs_brightness_full);
            } else {
                mIcon.setImageResource(R.drawable.gn_ic_qs_brightness_middle);
            }
        }
    }

    private int getBrightnessLevel() {
        int brightness = getBrightnessValue();
        
        if (brightness <= mMinimumBacklight) {
            brightness = mMinimumBacklight;
        } else if (brightness < mMaximumBacklight) {
            brightness = mDefaultBackLight;
        } else {
            brightness = mMaximumBacklight;
        }
        return brightness;
    }

    private int getBrightnessValue() {
        int brightness = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, mDefaultBackLight,
                UserHandle.USER_CURRENT);
        return brightness;
    }

    /** Fetch the brightness mode from the system settings and update the icon */
    private void updateMode() {
        if (mAutomaticAvailable) {
            int automatic;
            automatic = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                    UserHandle.USER_CURRENT);
            mAutomatic = automatic != Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
        } else {
            mControl.setChecked(false);
        }
    }

    /** Fetch the brightness from the system settings and update the slider */
    private void updateSlider() {
        if (mAutomatic) {
            float value = Settings.System.getFloatForUser(mContext.getContentResolver(),
                    Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, 0,
                    UserHandle.USER_CURRENT);
            mControl.setMax((int) BRIGHTNESS_ADJ_RESOLUTION);
            mControl.setValue((int) ((value + 1) * BRIGHTNESS_ADJ_RESOLUTION / 2f));
        } else {
            int value;
            value = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, mMaximumBacklight,
                    UserHandle.USER_CURRENT);
            mControl.setMax(mMaximumBacklight - mMinimumBacklight);
            mControl.setValue(value - mMinimumBacklight);
        }
    }

}
