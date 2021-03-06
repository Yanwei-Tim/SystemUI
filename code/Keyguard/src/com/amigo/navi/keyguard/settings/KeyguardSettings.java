package com.amigo.navi.keyguard.settings;

import com.amigo.navi.keyguard.DebugLog;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class KeyguardSettings {
    public static final String NET_CONNECT = "NetConnect";
    public static final String PF_DOUBLE_DESKTOP_LOCK = "double_desktop_lock";
    public static final String PF_KEYGUARD_WALLPAPER_UPDATE = "keyguard_wallpaper_update";
    public static final String PF_ONLY_WLAN = "only_wlan";
    
    /**
     * 锁屏样式开关
     */
    public static final String PF_KEYGUARD_STYLE_SWITCH = "keyguard_style_switch";
    public static final boolean DEFAULT_KEYGUARD_STYLE_STATUS = true;
    
    public static final String PF_KEYGUARD_ALERT = "keyguard_isAlert";
    public static final String PF_KEYGUARD_CONNECT = "keyguard_connectNet";
    
    public static final String PREFERENCE_NAME = "com.gionee.navi.keyguard_preferences";
//    public static final String PREFERENCE_NAME_KEYGUARD_ALERT = "com.gionee.navi.keyguard_alert";
//    public static final String PREFERENCE_NAME_KEYGUARD_CONNECT = "com.gionee.navi.keyguard_connection";
	public static final boolean SWITCH_WALLPAPER_UPDATE = false;
	public static final boolean SWITCH_WALLPAPER_WIFI = true;
	public static final boolean SWITCH_DOUBLE_SCREEN = true;
	public static final boolean DIALOG_KEYGUARD_ALERT = true;
	
    public static final String SWITCH_WALLPAPER_UPDATE_OPENED_ONCE = "keyguard_wallpaper_update_opened_once";
	// for statistics
	public static final int SWITCH_WALLPAPER_UPDATE_ON = 2;
	public static final int SWITCH_WALLPAPER_UPDATE_OFF = 1;
	public static final int SWITCH_ONLY_WLAN_ON = 1;
	public static final int SWITCH_ONLY_WLAN_OFF = 2;
	
	public static final long ANIMATION_DELAY = 33; //弹出动画的延时
	public static final long WALLPAPER_UPDATE_ANIMATION_DELAY = 700;//壁纸更新引导动画延时

	public static final String CLEARNOTIFICATION = "clearNotification_KeyguardSettingsActivity";
	public static final int NOTIFICATION_ID_SETTING = 1007;
	private static final String TAG = "KeyguardSettings";
	
	
	public static final String PF_NEED_COPY_WALLPAPER = "pf_need_copy_wallpaper";
	
	// 日志上传时间间隔控制
	private static final String TIME_LOGSUPLOAD_LAST = "time_logs_upload_last";	
	
	public static final String WALLPAPER_UPDATE_NOTIFICATION_FIRST = "wallpaper_update_notification_first";
    public static final String WALLPAPER_UPDATE_NOTIFICATION_SHOWED = "wallpaper_update_notification_showed";
	public static final String NUMBER_POINT_UPDATE_WALLPAPER = "number_of_point_update_wallpaper";
	public static final String TIME_POINT_UPDATE_WALLPAPER = "time_of_point_update_wallpaper";

	public static void setLogsUploadTime(Context context, long time) {
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putLong(TIME_LOGSUPLOAD_LAST, time);
		editor.commit();
	}

	public static long getLogsUploadTime(Context context, long time) {
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        return sp.getLong(TIME_LOGSUPLOAD_LAST,	time);
	}
	
    public static void setDoubleDesktopLockState(Context context,boolean value){
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putBoolean(PF_DOUBLE_DESKTOP_LOCK, value);
		editor.commit();
    }
    
    public static boolean getDoubleDesktopLockState(Context context){
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        return sp.getBoolean(PF_DOUBLE_DESKTOP_LOCK,
        		SWITCH_DOUBLE_SCREEN);
    }

    
    public static void setOnlyWlanState(Context context,boolean value){
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putBoolean(PF_ONLY_WLAN, value);
		editor.commit();
    }
    
    public static boolean getOnlyWlanState(Context context){
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        return sp.getBoolean(PF_ONLY_WLAN,
        		SWITCH_WALLPAPER_WIFI);
    }
    
    //锁屏样式开关
    public static void setKeyguardStyleSwitch(Context context,boolean enabled){
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putBoolean(PF_KEYGUARD_STYLE_SWITCH, enabled);
		editor.commit();
		
//		notifyKeyguardEnabledStateChanged(enabled);
    }
    
    public static boolean getKeyguardStyleSwitch(Context context){
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        return sp.getBoolean(PF_KEYGUARD_STYLE_SWITCH,
        		DEFAULT_KEYGUARD_STYLE_STATUS);
    }
    
    public static void setWallpaperUpadteState(Context context,boolean value){
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putBoolean(PF_KEYGUARD_WALLPAPER_UPDATE, value);
		editor.commit();
    }
    
    public static boolean getWallpaperUpadteState(Context context){
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        return sp.getBoolean(PF_KEYGUARD_WALLPAPER_UPDATE,
        		SWITCH_WALLPAPER_UPDATE);
    }
    
    public static void setDialogAlertState(Context context,boolean value){
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putBoolean(PF_KEYGUARD_ALERT, value);
		editor.commit();
    }
    
    public static boolean getDialogAlertState(Context context){
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        return sp.getBoolean(PF_KEYGUARD_ALERT, DIALOG_KEYGUARD_ALERT
        		);
    }
    public static void setConnectState(Context context,boolean value){
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putBoolean(PF_KEYGUARD_CONNECT, value);
		editor.commit();
    }
    
    public static boolean getConnectState(Context context){
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        return sp.getBoolean(PF_KEYGUARD_CONNECT,
        		SWITCH_WALLPAPER_UPDATE);
    }
    
    public static void setEverOpened(Context context,boolean value){
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putBoolean(SWITCH_WALLPAPER_UPDATE_OPENED_ONCE, value);
		editor.commit();
    }
    
    public static boolean getEverOpened(Context context){
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        return sp.getBoolean(SWITCH_WALLPAPER_UPDATE_OPENED_ONCE,
        		SWITCH_WALLPAPER_UPDATE);
    }
    
    public static void cancelNotification(Context context) {
    	DebugLog.d(TAG, "cancelNotification");
        NotificationManager mNotificationManager;
        mNotificationManager = (NotificationManager)context.getSystemService("notification");
        try {
            mNotificationManager.cancel(NOTIFICATION_ID_SETTING);
        } catch (Exception e) {
        }
    }
    
    public static boolean getBooleanSharedConfig(Context context, String key, boolean defValue) {
        SharedPreferences sp = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        boolean value = sp.getBoolean(key, defValue);
        return value;
    }

    public static boolean setBooleanSharedConfig(Context context, String key, boolean value) {
        SharedPreferences sp = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(key, value);
        return editor.commit();
    }
    
    public static SharedPreferences getPrefs(Context context) {
    	return context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    public static void saveNumberPointUpdateWallpaper(Context context,int count){
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
        preferences.edit().putInt(NUMBER_POINT_UPDATE_WALLPAPER, count).commit();
    }
    public static int getNumberPointUpdateWallpaper(Context context){
        SharedPreferences preferences = context.getSharedPreferences(KeyguardSettings.PREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
        return preferences.getInt(NUMBER_POINT_UPDATE_WALLPAPER, 0);
    }
    
    
    public static void saveTimePointUpdateWallpaper(Context context,String time){
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
        preferences.edit().putString(TIME_POINT_UPDATE_WALLPAPER, time).commit();
    }
    public static String getTimePointUpdateWallpaper(Context context){
        SharedPreferences preferences = context.getSharedPreferences(KeyguardSettings.PREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
        return preferences.getString(TIME_POINT_UPDATE_WALLPAPER, "");
    }
}
