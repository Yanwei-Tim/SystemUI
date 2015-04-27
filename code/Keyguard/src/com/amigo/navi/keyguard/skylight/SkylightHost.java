package com.amigo.navi.keyguard.skylight;

import java.util.List;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.amigo.navi.keyguard.DebugLog;
import com.amigo.navi.keyguard.modules.KeyguardMusicModule;
import com.amigo.navi.keyguard.modules.WeatherInfoModule;
import com.amigo.navi.keyguard.util.AmigoKeyguardUtils;
import com.amigo.navi.keyguard.util.KeyguardWidgetUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.R ;

public class SkylightHost extends FrameLayout {

    private static final String LOG_TAG="SkyWindowHost";
    private static final int UPDATE_WEATHER_INFO = 9;
    private static final int UPDATE_BACKGROUD = 10;
    private static final String MUSIC_PACKAGE_NAME="com.android.music";
    private static final String WEATHER_PACKAGE_NAME="com.coolwind.weather";
    
    private PowerManager mPowerManager=null;
    Drawable mWallpaperDrawable = null;
    FrameLayout mSkylightLayout;
    private AppWidgetHostView mMusicWidget = null;
    static SkylightLocation sLocation=null;
    
//    private Configuration mConfiguration = null;
    private String mOldFontStyle="";
    
    private boolean mIsMusicFrozen=false;
    
    private LocalReceiver mReceiver=new LocalReceiver();
    
    public SkylightHost(Context context) {
        this(context,null);
        
    }

    public SkylightHost(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public SkylightHost(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
//        mConfiguration = new Configuration(getContext().getResources().getConfiguration());
        mOldFontStyle = AmigoKeyguardUtils.getmOldFontStyle();
        mPowerManager=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
        initView(context);
        requestFocus();
    }

    
    private void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.skylight_host_view, this,true);
        mSkylightLayout = (FrameLayout) findViewById(R.id.skyligt_window);
        RelativeLayout.LayoutParams params=(RelativeLayout.LayoutParams)mSkylightLayout.getLayoutParams();
        params.leftMargin=sLocation.getXaxis();
        params.topMargin=sLocation.getYaxis();
        params.width=sLocation.getWidth();
        params.height=sLocation.getHeight();
        mSkylightLayout.setLayoutParams(params);
        
        mPagerView = (SkyPagerView)findViewById(R.id.zzzzz_gn_navil_id_app_widget_container);
        
        mInfoZone = new SkylightView(context);
        mInfoZone.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mPagerView.addView(mInfoZone);
        
        mPageIndicator = (KeyguardPagerIndicator)findViewById(R.id.zzzzz_gn_navil_id_indicator);
        mPagerView.setIndicator(mPageIndicator);
//        setWallpaperAsBackground();
//        setBackgroud();
    }
    

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case UPDATE_BACKGROUD:
                
                break;
            case UPDATE_WEATHER_INFO:
                Log.d(LOG_TAG, "UPDATE_WEATHER_INFO--------");
                Context context=mContext.getApplicationContext();
                WeatherInfoModule.getInstance(context, KeyguardUpdateMonitor.getInstance(context)).getWeatherInfo();
                break;
            default:
                break;
            }
        }
    };
    private SkyPagerView mPagerView;
    private View mInfoZone;
    private KeyguardPagerIndicator mPageIndicator;
    
    private void setWallpaperAsBackground() {
        mSkylightLayout.setBackgroundResource(R.drawable.skylight_bg_wallpaper);
//        new UpdateWallpaperThread().start();
    }

    private class UpdateWallpaperThread extends Thread {

        @Override
        public void run() {
            super.run();
            setBackgroud();
            mHandler.sendEmptyMessage(UPDATE_BACKGROUD);
        }
    }
    
    /**
     * @deprecated don't change along with system wallpaper
     * @date 2014-12-4
     */
    public void notifyWallpaperChanged(){
        setWallpaperAsBackground();
    }

    private void setBackgroud() {
        mWallpaperDrawable=getResources().getDrawable(R.drawable.skylight_bg_wallpaper);
        mSkylightLayout.setBackgroundDrawable(mWallpaperDrawable);
    }
    
    public void showSkylight() {
        boolean isMusicPlaying=isMusicPlaying();
        if(DebugLog.DEBUG)Log.d(LOG_TAG, "isMusicPlaying: "+isMusicPlaying);
        if (isMusicPlaying) {
            addMusicWidget();
        } else {
            mPagerView.removeView(mMusicWidget);
            KeyguardWidgetUtils.getInstance(mContext).deleteWidget(KeyguardWidgetUtils.WIDGET_NAME_MUSIC_SKYLIGHT);
            mPagerView.resetIndicator();
            mMusicWidget=null;
        }
       
    }
    
    /**
     * when music is playing, add music widget
     */
    private void addMusicWidget() {
        if(DebugLog.DEBUG)Log.d(LOG_TAG, "isMusicForzen: "+mIsMusicFrozen+"  is MusicWidget null? "+(mMusicWidget==null));
        if (mIsMusicFrozen) {
            mPagerView.removeView(mMusicWidget);
            KeyguardWidgetUtils.getInstance(mContext).deleteWidget(KeyguardWidgetUtils.WIDGET_NAME_MUSIC_SKYLIGHT);
            mMusicWidget = null;
            updateMusicPlayState(false);
        } else {
            if (mMusicWidget != null && mMusicWidget.getParent() != null) {
                //((ViewGroup)mMusicWidget.getParent()).removeView(mMusicWidget);
            } else {
                mMusicWidget = KeyguardWidgetUtils.getInstance(mContext).createMusicWidgetForSkylight();
                if(DebugLog.DEBUG)Log.d(LOG_TAG, "addMusicWidget  widget is null? " + (mMusicWidget == null));
                if (mMusicWidget != null) {
                    int musicPaddingBottom = getResources().getDimensionPixelSize(R.dimen.skylight_music_padding_bottom);
                    mMusicWidget.setPadding(0, 0, 0, musicPaddingBottom);
                    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                    mPagerView.addView(mMusicWidget, 0, params);
                }else{
                    updateMusicPlayState(false);
                }
            }
            mPagerView.resetIndicator();
            setToMusicPageIfPlaying();
        }
    }
    
    public void hideSkylight(){
        
    }
    
    private boolean isMusicPlaying() {
        try {
            KeyguardMusicModule musicModule = KeyguardMusicModule.getInstance();
            return musicModule.isMusicPlayingForSkylight();
        } catch (Exception e) {
        }
        return false;
    }

    private void updateMusicPlayState(boolean isPlaying) {
        try {
            KeyguardMusicModule musicModule = KeyguardMusicModule.getInstance();
            musicModule.updateMusicPlayingStateBySkylignt(isPlaying);
        } catch (Exception e) {
        }
    }
    private void setToMusicPageIfPlaying(){
        boolean isMusicPlaying=isMusicPlaying();
        if(DebugLog.DEBUG)Log.d(LOG_TAG, "isMusicPlaying: "+isMusicPlaying);
        if(isMusicPlaying){
            mPagerView.setCurrentPageIndex(0);
            mPagerView.toast();
            
        }
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mPowerManager.userActivity( SystemClock.uptimeMillis(), false);
        return super.dispatchTouchEvent(ev);
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }
    
    public static final boolean isSkylightSizeExist(){
    	if(sLocation!=null){
    		return ((sLocation.getHeight()>0)&&(sLocation.getWidth()>0));
    	}
    	return false;
    }
    
    public static int getSkylightWidth(){
    	if(sLocation!=null){
    		return sLocation.getWidth();
    	}
    	return 0;
    }

    
    @Override
    protected void onAttachedToWindow() {
    	super.onAttachedToWindow();
    	registerReceivers();
    	if(DebugLog.DEBUG)Log.d(LOG_TAG, "SkylightHost  onAttachedToWindow()");
    }
    
    @Override
    protected void onDetachedFromWindow() {
    	super.onDetachedFromWindow();
    	unregisterReceivers();
    	if(DebugLog.DEBUG)Log.d(LOG_TAG, "SkylightHost  onDetachedFromWindow()");
    }
    
    public void onConfigurationChanged(){
        if(DebugLog.DEBUG)Log.d(LOG_TAG, "skylight host onConfigurationChanged------------ ");
        Configuration newConfig=getResources().getConfiguration();
        String currentFontStyle = AmigoKeyguardUtils.getCurrretFontStyle(newConfig, mOldFontStyle);
        boolean isChangeFontStyle = false;
        if (!currentFontStyle.equals(mOldFontStyle)) {
            mOldFontStyle = currentFontStyle;
            isChangeFontStyle = true;
            DebugLog.d(LOG_TAG, "onConfigurationChanged() newConfig....amigoFont1111111=" + currentFontStyle
                    + "oldFontStyle=" + mOldFontStyle);
        }
            DebugLog.d(LOG_TAG, "isChangeFontStyle:" + isChangeFontStyle);
        if (isChangeFontStyle) {
           reloadSkylightView();
        } else {
           DebugLog.d(LOG_TAG, "onConfigurationChanged: congfiguration not change");
        }
    }
    
    private void reloadSkylightView() {
        mPagerView.removeView(mInfoZone);
        mPagerView.removeView(mMusicWidget);
        KeyguardWidgetUtils.getInstance(mContext).deleteWidget(KeyguardWidgetUtils.WIDGET_NAME_MUSIC_SKYLIGHT);
        mInfoZone = null;
        mMusicWidget = null;
        mInfoZone = new SkylightView(getContext());
        mInfoZone.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mPagerView.addView(mInfoZone);
        if (isMusicPlaying()) {
            addMusicWidget();
        }
    }
    
    private void registerReceivers(){
        IntentFilter filter=new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addDataScheme("package");
        mContext.registerReceiver(mReceiver, filter);
        
    }
    
    private void unregisterReceivers(){
        mContext.unregisterReceiver(mReceiver);
        
    }
    
    private void getIsMusicAppFrozen(){
        new Thread(){
            public void run() {
                PackageManager pm = mContext.getPackageManager();
                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> infos=pm.queryIntentActivities(mainIntent, 0);
                for (ResolveInfo resolveInfo : infos) {
                    String packageName=resolveInfo.activityInfo.packageName;
                    if(MUSIC_PACKAGE_NAME.equals(packageName)){
                        mIsMusicFrozen=false;
                        return;
                    }
                }
                mIsMusicFrozen=true;
            };
        }.start();
      
    }
    class LocalReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final String packageName = intent.getData().getSchemeSpecificPart();
            if(intent.ACTION_PACKAGE_CHANGED.equals(action)
                    || Intent.ACTION_PACKAGE_REMOVED.equals(action)
                    || Intent.ACTION_PACKAGE_ADDED.equals(action)){
                if (MUSIC_PACKAGE_NAME.equals(packageName)) {
                    getIsMusicAppFrozen();
                    if(DebugLog.DEBUG)Log.d(LOG_TAG, "packageName: " + packageName + "  action: " + action+" isMusicFrozen: "+mIsMusicFrozen);
                }else if(WEATHER_PACKAGE_NAME.equals(packageName)){
                    mHandler.sendEmptyMessage(UPDATE_WEATHER_INFO);
                }
            }
        }
    }
}

