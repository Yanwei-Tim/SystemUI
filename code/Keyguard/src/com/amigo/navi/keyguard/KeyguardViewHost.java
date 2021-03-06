package com.amigo.navi.keyguard;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.LinearLayout;
import com.amigo.navi.keyguard.AmigoKeyguardBouncer.KeyguardBouncerCallback;
import com.amigo.navi.keyguard.haokan.Common;
import com.amigo.navi.keyguard.haokan.RequestNicePicturesFromInternet;
import com.amigo.navi.keyguard.haokan.UIController;
import com.amigo.navi.keyguard.haokan.db.WallpaperDB;
import com.amigo.navi.keyguard.haokan.entity.Wallpaper;
import com.amigo.navi.keyguard.haokan.entity.WallpaperList;
import com.amigo.navi.keyguard.network.ImageLoader;
import com.amigo.navi.keyguard.picturepage.adapter.HorizontalAdapter;
import com.amigo.navi.keyguard.picturepage.widget.HorizontalListView.OnScrollListener;
import com.amigo.navi.keyguard.picturepage.widget.KeyguardListView;
import com.amigo.navi.keyguard.picturepage.widget.OnViewTouchListener;
import com.amigo.navi.keyguard.sensor.KeyguardSensorModule;
import com.amigo.navi.keyguard.util.AmigoKeyguardUtils;
import com.amigo.navi.keyguard.util.KeyguardWidgetUtils;
import com.amigo.navi.keyguard.util.QuickSleepUtil;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView;
import com.android.keyguard.ViewMediatorCallback;

public class KeyguardViewHost extends FrameLayout {

    private static final String TAG = "KeyguardViewHost";

	
    private Configuration mConfiguration = null;
    private String mOldFontStyle="";
    

    final static boolean DEBUG=true;
    private static final String LOG_TAG="KeyguardViewHost";
    
    private int mTouchCallTime = 0;
    
    
    private Context mContext;
    
    private AmigoKeyguardHostView mAmigoKeyguardView;
    private ViewMediatorCallback mViewMediatorCallback;
    
    
    public KeyguardViewHost(Context context) {
        this(context, null);
    }

    public KeyguardViewHost(Context context, AttributeSet attrs) {
       this(context, attrs,0);
       mConfiguration = new Configuration(getContext().getResources().getConfiguration());
       mOldFontStyle  = AmigoKeyguardUtils.getmOldFontStyle();;
       
       if(DEBUG){
    	   DebugLog.d(LOG_TAG, "onConfigurationChanged() ..KeyguardHostView..mOldFontStyle="+mOldFontStyle);            	
       }
    }

    public KeyguardViewHost(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext=context;
        amigoInflateKeyguardView(null);
        UIController.getInstance().setmKeyguardViewHost(this);
    }
    
    
    public void onConfigurationChanged() {
    	Configuration newConfig = mContext.getResources().getConfiguration();
		if (DEBUG)
			DebugLog.d(LOG_TAG, "onConfigurationChanged  mConfiguration:"
					+ mConfiguration);
		String currentFontStyle=AmigoKeyguardUtils.getCurrretFontStyle(newConfig,mOldFontStyle);
		boolean isChangeFontStyle=false;
		if(!currentFontStyle.equals(mOldFontStyle)){
			mOldFontStyle=currentFontStyle;
			isChangeFontStyle=true;
			DebugLog.d(LOG_TAG, "onConfigurationChanged() newConfig....amigoFont1111111="+currentFontStyle+"oldFontStyle="+mOldFontStyle);
		}
		if (DEBUG)
			DebugLog.d(LOG_TAG, "onConfigurationChanged  newConfig:" + newConfig+"---"+"isChangeFontStyle:"+isChangeFontStyle);
		if (mConfiguration != null
				&& (!newConfig.locale.equals(mConfiguration.locale) || newConfig.fontScale != mConfiguration.fontScale) || isChangeFontStyle) {
			// only propagate configuration messages if we're currently
			// showing
			mConfiguration.locale = newConfig.locale;
			mConfiguration.fontScale = newConfig.fontScale;
			
			resetKeyguardView();
			if(mConfigChangeCallback != null){
				mConfigChangeCallback.onConfigChange();
			}
		} else {
			if (DEBUG)
				DebugLog.d(LOG_TAG, "onConfigurationChanged: congfiguration not change");
		}
	}

	private void resetKeyguardView() {
		removeAllViews();
		amigoInflateKeyguardView(null);
		KeyguardViewHostManager.getInstance().initKeyguardReset();
	}
    
    
    private void amigoInflateKeyguardView(Bundle options) {
        mAmigoKeyguardView = new AmigoKeyguardHostView(mContext);
        mAmigoKeyguardView.setOrientation(LinearLayout.VERTICAL);
//        mAmigoKeyguardView.setLockPatterUtils(mLockPatternUtils);
        addView(mAmigoKeyguardView);
    }


    
    public void initKeyguard(ViewMediatorCallback callback,LockPatternUtils lockPatternUtils){
    	setViewMediatorCallback(callback);
    	if(mAmigoKeyguardView!=null){
    		mAmigoKeyguardView.initKeyguard(callback, lockPatternUtils);
    	}
    	
    }
    
    public void setViewMediatorCallback(ViewMediatorCallback callback){
        mViewMediatorCallback=callback;
    }
//
//    
//    public ViewGroup tempGetAmigoKeyguardHostView(){
//        return mAmigoKeyguardView;
//    }
    
    public void show(Bundle options){
        setVisibility(View.VISIBLE);
        if(mAmigoKeyguardView==null){
            amigoInflateKeyguardView(options);
        }
        mAmigoKeyguardView.show(options);
    }
    
    public void showBouncerOrKeyguard(){
        show(null);
    	mAmigoKeyguardView.showBouncerOrKeyguard();
    }
    	
    
    public void hide() {
        setVisibility(View.GONE);
        mAmigoKeyguardView.hide();
    }
    
    
    public void onScreenTurnedOff(){
        DebugLog.d(LOG_TAG, "onScreenTurnedOff()");
        mAmigoKeyguardView.onScreenTurnedOff();
        cancelFingerUnlock();
        
    }
    
    public void onScreenTurnedOn(){
    	 mAmigoKeyguardView.onScreenTurnedOn();
    }
    
    
    
    
    public void dismissWithAction(KeyguardHostView.OnDismissAction r){
        DebugLog.d(LOG_TAG, "dismissWithAction--- mAmigoKeyguardView != null"+(mAmigoKeyguardView != null));
        if(mAmigoKeyguardView != null){
            mAmigoKeyguardView.dismissWithAction(r);
        }
    }
    
  
    
    public void dismiss(){
        mAmigoKeyguardView.dismiss();
    }
    
    public void resetToHomePositionIfNoSecure(){
        mAmigoKeyguardView.resetToHomePositionIfNoSecure();
    }
    
    public boolean needsFullscreenBouncer(){
       return mAmigoKeyguardView.needsFullscreenBouncer();
    }

    
    public void unLockByOther(boolean animation){
        if(animation){
            mAmigoKeyguardView.scrollToUnlockByOther();
        }else{
            mAmigoKeyguardView.scrollToSnapshotPage();
        }
    }
    
    public void showBouncer(boolean resetSecuritySelection){
        mAmigoKeyguardView.showBouncer(resetSecuritySelection);
    }
    
    public void resetHostYToHomePosition(){
        mAmigoKeyguardView.resetHostYToHomePosition();
    }
    
    public void scrollToKeyguardPageByAnimation(){
    	if(mAmigoKeyguardView != null){
    		mAmigoKeyguardView.scrollToKeyguardPage(300);
    	}
    }
    
    public void scrollToUnlockHeightByOther(boolean withAnim){
    	if(mAmigoKeyguardView != null){
    		if(withAnim){
    			mAmigoKeyguardView.scrollToUnlockByOther();
    		}else{
    			mAmigoKeyguardView.scrollToSnapshotPage();
    		}
    	}
    }
    
	public boolean isAmigoHostYAtHomePostion() {
		if (mAmigoKeyguardView != null) {
			return mAmigoKeyguardView.isHostYAtHomePostion();
		}
		return false;
	}

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardWidgetUtils.getInstance(mContext).startHostListening();
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardWidgetUtils.getInstance(mContext).stopHostListening();
    }

    public boolean isSecure(){
    	return mAmigoKeyguardView.isSecure();
    }
    
    public long getUserActivityTimeout(){
    	return mAmigoKeyguardView.getUserActivityTimeout();
    }
    
    public boolean keyguardBouncerIsShowing(){
    	return mAmigoKeyguardView.keyguardBouncerIsShowing();
    }

    public void registerBouncerCallback(KeyguardBouncerCallback bouncerCallback){
    	mAmigoKeyguardView.registerBouncerCallback(bouncerCallback);
    }
    
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    	if(DebugLog.DEBUG){
	          DebugLog.v(LOG_TAG, "hostView...dispatchKeyEvent...event.getKeyCode()="+event.getKeyCode());
	    }
    	 boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
         switch (event.getKeyCode()) {
             case KeyEvent.KEYCODE_BACK:
                 if (!down && mAmigoKeyguardView!=null) {
                	 mAmigoKeyguardView.onBackPress();
                	 
                 }
                 return true;
         }
     
    	return super.dispatchKeyEvent(event);
    }
    
    public boolean  onBackPress(){
    	if(DebugLog.DEBUG){
	          DebugLog.v(LOG_TAG, "hostView...onBackPress");
	    }
    	if ( mAmigoKeyguardView!=null) {
       	 return mAmigoKeyguardView.onBackPress(); 
        }
    	return false;
    }
    
    
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        userActivity(ev);
        return super.dispatchTouchEvent(ev);
    }
    
    private void userActivity(MotionEvent ev){
        switch (ev.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
            mViewMediatorCallback.userActivity();
            break;
        default:
            if (mTouchCallTime++ % 50 == 0) {
                mTouchCallTime = 1;
                mViewMediatorCallback.userActivity();
            }
            break;
        }
    }
 
    
    public void setOnViewTouchListener(OnViewTouchListener listener){
        mAmigoKeyguardView.setOnViewTouchListener(listener);
    }
    
    public int getKeyguardPage(){
        return 0;
    }
    
    public int getHkCaptionsViewHeight(){
        return 0;
    }
    
    
    public void showBouncerOrKeyguardDone(){
    	if ( mAmigoKeyguardView!=null) {
          	  mAmigoKeyguardView.showBouncerOrKeyguardDone(); 
           }
    }
    
	    
    public interface ConfigChangeCallback{
    	public void onConfigChange();
    }

    public void shakeFingerIdentifyTip() {
        mAmigoKeyguardView.shakeFingerIdentifyTip();
    }
    
    
    private AnimatorSet mScaleHostAnimator = null;
    private boolean isStartunlockByFinger=false;
    public void unlockByFingerIdentify() {
    	
    	if(isStartunlockByFinger ||  !KeyguardViewHostManager.getInstance().isScreenOn()){
    		DebugLog.d(LOG_TAG, "unlockByFingerIdentify....isStartunlockByFinger="+isStartunlockByFinger);
    		return;
    	}
    	isStartunlockByFinger=true;
        if (mScaleHostAnimator == null) {
            ObjectAnimator animator1 = ObjectAnimator.ofFloat(this, "scaleX", 1f, 0.6f);
            ObjectAnimator animator2 = ObjectAnimator.ofFloat(this, "scaleY", 1f, 0.6f);
            ObjectAnimator animator3 = ObjectAnimator.ofFloat(this, "alpha", 1f, 0.0f);
            mScaleHostAnimator = new AnimatorSet();
            mScaleHostAnimator.setDuration(500);
//            mScaleHostAnimator.playTogether(animator1, animator2, animator3);
            mScaleHostAnimator.play(animator3);
            mScaleHostAnimator.addListener(new Animator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                	mAmigoKeyguardView.finish();
                    setVisibility(View.GONE);
                    resetHostView();
                    mViewMediatorCallback.userActivity();
                    isStartunlockByFinger=false;
               
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                	DebugLog.d(LOG_TAG, "unlockByFingerIdentify....onAnimationCancel=");
                    resetHostView();
                    isStartunlockByFinger=false;
                }
            });
        }
        mScaleHostAnimator.start();

    }
    
	public void unlockByBlackFingerIdentify() {

		mAmigoKeyguardView.finish();
		setVisibility(View.GONE);
		resetHostView();
		mViewMediatorCallback.userActivity();
		isStartunlockByFinger = false;

	}
    
    private void cancelFingerUnlock() {
        if (mScaleHostAnimator != null) {
            mScaleHostAnimator.cancel();
        }
    }

    private void resetHostView() {
        setScaleX(1f);
        setScaleY(1f);
        setAlpha(1f);
    }
    
    public void fingerPrintFailed() {
        mAmigoKeyguardView.fingerPrintFailed();
    }

    public void fingerPrintSuccess() {
        mAmigoKeyguardView.fingerPrintSuccess();
    }
    
    public boolean passwordViewIsForzen() {
        return mAmigoKeyguardView.passwordViewIsForzen();
    }
 
     private ConfigChangeCallback mConfigChangeCallback = null;
     
    public void setConfigChangeCallback(ConfigChangeCallback callback){
    	mConfigChangeCallback = callback;
    }
    
	public  boolean isTriggerMove(){
		return mAmigoKeyguardView.isTriggerMove();
	}

	public void setOccluded(boolean occluded) {
		
		mAmigoKeyguardView.setOccluded(occluded);
	}
}
