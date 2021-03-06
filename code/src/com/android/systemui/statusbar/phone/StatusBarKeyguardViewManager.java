/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.systemui.statusbar.phone;

import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManagerGlobal;

import com.android.internal.policy.IKeyguardShowCallback;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.keyguard.KeyguardViewMediator;

import com.amigo.navi.keyguard.KeyguardViewHost;
import com.amigo.navi.keyguard.KeyguardViewHostManager;
import com.amigo.navi.keyguard.AppConstants;

import static com.android.keyguard.KeyguardHostView.OnDismissAction;

import com.amigo.navi.keyguard.AmigoKeyguardBouncer.KeyguardBouncerCallback;
import com.amigo.navi.keyguard.KeyguardViewHostManager.KeyguardNotificationCallback;
import com.amigo.navi.keyguard.skylight.SkylightHost;

import com.amigo.navi.keyguard.KeyguardViewHost;
import com.android.systemui.R;

/**
 * Manages creating, showing, hiding and resetting the keyguard within the status bar. Calls back
 * via {@link ViewMediatorCallback} to poke the wake lock and report that the keyguard is done,
 * which is in turn, reported to this class by the current
 * {@link com.android.keyguard.KeyguardViewBase}.
 */
public class StatusBarKeyguardViewManager implements KeyguardBouncerCallback ,KeyguardNotificationCallback{

    // When hiding the Keyguard with timing supplied from WindowManager, better be early than late.
    private static final long HIDE_TIMING_CORRECTION_MS = -3 * 16;

    // Delay for showing the navigation bar when the bouncer appears. This should be kept in sync
    // with the appear animations of the PIN/pattern/password views.
    private static final long NAV_BAR_SHOW_DELAY_BOUNCER = 320;

    private static String TAG = "StatusBarKeyguardViewManager";

    private final Context mContext;

    private LockPatternUtils mLockPatternUtils;
    private ViewMediatorCallback mViewMediatorCallback;
    private PhoneStatusBar mPhoneStatusBar;
    private ScrimController mScrimController;

    private ViewGroup mContainer;
    private StatusBarWindowManager mStatusBarWindowManager;
    private KeyguardViewHostManager mKeyguardViewHostManager;

    private boolean mScreenOn = false;
//    private KeyguardBouncer mBouncer;
    private boolean mShowing;
    private boolean mOccluded;

    private boolean mFirstUpdate = true;
    private boolean mLastShowing;
    private boolean mLastOccluded;
    private boolean mLastBouncerShowing;
    private boolean mLastBouncerDismissible;
    private OnDismissAction mAfterKeyguardGoneAction;
    
    private KeyguardViewHost mKeyguardViewHost = null;

    public StatusBarKeyguardViewManager(Context context, ViewMediatorCallback callback,
            LockPatternUtils lockPatternUtils) {
        mContext = context;
        mViewMediatorCallback = callback;
        mLockPatternUtils = lockPatternUtils;
    }

    public void registerStatusBar(PhoneStatusBar phoneStatusBar,
            ViewGroup container, StatusBarWindowManager statusBarWindowManager,
            ScrimController scrimController) {
        mPhoneStatusBar = phoneStatusBar;   
        mContainer = container;
        mStatusBarWindowManager = statusBarWindowManager;
        mScrimController = scrimController;
        KeyguardViewHost keyguardViewHost = (KeyguardViewHost) container.findViewById(R.id.keyguardViewHost);
        SkylightHost skylightHost = (SkylightHost) container.findViewById(R.id.skylightHost);
        mKeyguardViewHostManager=new KeyguardViewHostManager(mContext,keyguardViewHost,skylightHost,mLockPatternUtils,mViewMediatorCallback);
        mKeyguardViewHostManager.registerBouncerCallback(this);
        mKeyguardViewHostManager.registerKeyguardNotificationCallback(this);
//        mBouncer = new KeyguardBouncer(mContext, mViewMediatorCallback, mLockPatternUtils,
//                mStatusBarWindowManager, mKeyguardViewHost.tempGetAmigoKeyguardHostView());
    }

    /**
     * Show the keyguard.  Will handle creating and attaching to the view manager
     * lazily.
     */
    public void show(Bundle options) {
    	Log.i("TAG","show....options="+(options==null ? null : options));
        mShowing = true;
        mStatusBarWindowManager.setKeyguardShowing(true);
        mKeyguardViewHostManager.show(options);
        reset();
    }

    /**
     * Shows the notification keyguard or the bouncer depending on
     * {@link KeyguardBouncer#needsFullscreenBouncer()}.
     */
    private void showBouncerOrKeyguard() {
//        if (mKeyguardViewHostManager.needsFullscreenBouncer()) {
//
//            // The keyguard might be showing (already). So we need to hide it.
//            mPhoneStatusBar.hideKeyguard();
//            mKeyguardViewHostManager.showBouncer(true);
//        } else {
//            mPhoneStatusBar.showKeyguard();
//            mBouncer.hide(false  destroyView );
//            mBouncer.prepare();
//        }
    	Log.i("TAG","showBouncerOrKeyguard");
    	mPhoneStatusBar.showKeyguard();
        mKeyguardViewHostManager.showBouncerOrKeyguard();
        
    }

  /*  private void showBouncer() {
        if (mShowing) {
            mBouncer.show(false  resetSecuritySelection );
        }
        updateStates();
    }*/

    public void dismissWithAction(OnDismissAction r, boolean afterKeyguardGone) {
    	Log.i("TAG","statusBarKeyguard,,,,,,dismissWithAction..afterKeyguardGone="+afterKeyguardGone);
        if (mShowing) {
            if (afterKeyguardGone) {
            	mKeyguardViewHostManager.dismiss();
//            	 mBouncer.show(false  resetSecuritySelection );
                 mAfterKeyguardGoneAction = r;
               
            } else {
            	mKeyguardViewHostManager.dismissWithDismissAction(r);
            }
        }
        updateStates();
    }

    /**
     * Reset the state of the view.
     */
    public void reset() {
    	Log.i("TAG","reset");
        if (mShowing) {
            if (mOccluded) {
//     	        mKeyguardViewHost.setVisibility(View.GONE);
                mPhoneStatusBar.hideKeyguard();
            } else {
//            	mKeyguardViewHost.setVisibility(View.VISIBLE);
//                showBouncerOrKeyguard();
              	mPhoneStatusBar.showKeyguard();
            }
            mKeyguardViewHostManager.reset(mOccluded);
            updateStates();
        }
    }
    
    public void showSkylight() {
        if (mKeyguardViewHostManager != null) {
            mKeyguardViewHostManager.showSkylight();
        }
    }
    
    public void hideSkylight(boolean forceHide) {
        if (mKeyguardViewHostManager != null) {
            mKeyguardViewHostManager.hideSkylight(forceHide);
        }
    }
    public void startFingerIdentify(){
        if(mKeyguardViewHostManager!=null){
            mKeyguardViewHostManager.startFingerIdentify();
        }
    }
    
    public void cancelFingerIdentify(){
        if(mKeyguardViewHostManager!=null){
            mKeyguardViewHostManager.cancelFingerIdentify();
        }
    }

    public void onScreenTurnedOff() {
       	Log.i("TAG","onScreenTurnedOff");
        mScreenOn = false;
        mPhoneStatusBar.onScreenTurnedOff();
        mKeyguardViewHostManager.onScreenTurnedOff();
//        mBouncer.onScreenTurnedOff();
    }

    public void onScreenTurnedOn(final IKeyguardShowCallback callback) {
      	Log.i("TAG","onScreenTurnedOn");
        mScreenOn = true;
        mPhoneStatusBar.onScreenTurnedOn();
        mKeyguardViewHostManager.onScreenTurnedOn();
        if (callback != null) {
            callbackAfterDraw(callback);
        }
    }

    private void callbackAfterDraw(final IKeyguardShowCallback callback) {
        mContainer.post(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.onShown(mContainer.getWindowToken());
                } catch (RemoteException e) {
                    Slog.w(TAG, "Exception calling onShown():", e);
                }
            }
        });
    }

    public void verifyUnlock() {
        // dismiss();
		Log.i("TAG","verifyUnlock");
        show(null);
    }
    
    

    public void setNeedsInput(boolean needsInput) {
        mStatusBarWindowManager.setKeyguardNeedsInput(needsInput);
    }

    public void updateUserActivityTimeout() {
    	long timeout=mKeyguardViewHostManager.getUserActivityTimeout();
    	if(timeout<0){
    		timeout=KeyguardViewMediator.AWAKE_INTERVAL_DEFAULT_MS;
    	}
    	
        mStatusBarWindowManager.setKeyguardUserActivityTimeout(timeout);
    }

    public void setOccluded(boolean occluded) {
    	//jiating modify for keyguard begin
       /* if (occluded && !mOccluded && mShowing) {
            if (mPhoneStatusBar.isInLaunchTransition()) {
                mOccluded = true;
                mPhoneStatusBar.fadeKeyguardAfterLaunchTransition(null  beforeFading ,
                        new Runnable() {
                            @Override
                            public void run() {
                                mStatusBarWindowManager.setKeyguardOccluded(mOccluded);
                                reset();
                            }
                        });
                return;
            }
        }*/
    	//jiating modify for keyguard end
     	Log.i("TAG","setOccluded...occluded="+occluded);
        mOccluded = occluded;
        if (mShowing) {
            if (mOccluded) {
                mPhoneStatusBar.hideKeyguard();
            } else {
              	mPhoneStatusBar.showKeyguard();
            }
            mKeyguardViewHostManager.setOccluded(mOccluded);
            updateStates();
        }
        mStatusBarWindowManager.setKeyguardOccluded(occluded);
        //<Amigo_Keyguard>  jingyn <2015-05-12> modify for fingerIdentify begin
//        if(occluded){
//            cancelFingerIdentify();
//        }else{
//            startFingerIdentify();
//        }
        //<Amigo_Keyguard>  jingyn <2015-05-12> modify for fingerIdentify end
    }

    public boolean isOccluded() {
        return mOccluded;
    }

    /**
     * Starts the animation before we dismiss Keyguard, i.e. an disappearing animation on the
     * security view of the bouncer.
     *
     * @param finishRunnable the runnable to be run after the animation finished, or {@code null} if
     *                       no action should be run
     */
    public void startPreHideAnimation(Runnable finishRunnable) {
//    	 mKeyguardViewHostManager.startPreHideAnimation(finishRunnable);
//        if (mBouncer.isShowing()) {
//            mBouncer.startPreHideAnimation(finishRunnable);
//        } else 
    	if (finishRunnable != null) {
            finishRunnable.run();
        }
    	
    	
    }

    /**
     * Hides the keyguard view
     */
    public void hide(long startTime, final long fadeoutDuration) {
    	Log.i("TAG","hide");
        mShowing = false;
        
        // jingyn modify  for  keyguard begin 
        mKeyguardViewHostManager.hide();
        // jingyn modify  for  keyguard end 
        long uptimeMillis = SystemClock.uptimeMillis();
        long delay = Math.max(0, startTime + HIDE_TIMING_CORRECTION_MS - uptimeMillis);
//jiating modify for keyguard begin
  /*      if (mPhoneStatusBar.isInLaunchTransition() ) {
            mPhoneStatusBar.fadeKeyguardAfterLaunchTransition(new Runnable() {
                @Override
                public void run() {
                    mStatusBarWindowManager.setKeyguardShowing(false);
                    mStatusBarWindowManager.setKeyguardFadingAway(true);
                    mBouncer.hide(true  destroyView );
                    updateStates();
                    mScrimController.animateKeyguardFadingOut(
                            PhoneStatusBar.FADE_KEYGUARD_START_DELAY,
                            PhoneStatusBar.FADE_KEYGUARD_DURATION, null);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    mPhoneStatusBar.hideKeyguard();
                    mStatusBarWindowManager.setKeyguardFadingAway(false);
                    mViewMediatorCallback.keyguardGone();
                    executeAfterKeyguardGoneAction();
                }
            });
        } else {*/
            mPhoneStatusBar.setKeyguardFadingAway(delay, fadeoutDuration);
            mPhoneStatusBar.hideKeyguard();
//            boolean staying = mPhoneStatusBar.hideKeyguard();
           /*  if (!staying) {
                mStatusBarWindowManager.setKeyguardFadingAway(true);
                mScrimController.animateKeyguardFadingOut(delay, fadeoutDuration, new Runnable() {
                    @Override
                    public void run() {
                        mStatusBarWindowManager.setKeyguardFadingAway(false);
                        mPhoneStatusBar.finishKeyguardFadingAway();
                        WindowManagerGlobal.getInstance().trimMemory(
                                ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);
                    }
                });
            } else {
                mScrimController.animateGoingToFullShade(delay, fadeoutDuration);
                mPhoneStatusBar.finishKeyguardFadingAway();
            }*/
            mStatusBarWindowManager.setKeyguardShowing(false);
//            mBouncer.hide(true /* destroyView */);
            mViewMediatorCallback.keyguardGone();
            executeAfterKeyguardGoneAction();
            updateStates();
//        }
//jiating modify for keyguard end
    }

    private void executeAfterKeyguardGoneAction() {
        if (mAfterKeyguardGoneAction != null) {
            mAfterKeyguardGoneAction.onDismiss();
            mAfterKeyguardGoneAction = null;
        }
    }

    /**
     * Dismisses the keyguard by going to the next screen or making it gone.
     */
    public void dismiss() {
    	Log.i("TAG","dismiss....mScreenOn="+mScreenOn);
        if (mScreenOn) {
//            showBouncer();
        	mKeyguardViewHostManager.dismiss();
        	
        }
    }

    /**
     * WARNING: This method might cause Binder calls.
     */
    public boolean isSecure() {
//        return mBouncer.isSecure();
    	return mKeyguardViewHostManager.isSecure();
    }

    /**
     * @return Whether the keyguard is showing
     */
    public boolean isShowing() {
        return mShowing;
    }

    
    public boolean getIsSkylightShown(){
        if(mKeyguardViewHostManager!=null){
            return mKeyguardViewHostManager.getIsSkylightShown();
        }
        return false;
    }
    
    /**
     * Notifies this manager that the back button has been pressed.
     *
     * @return whether the back press has been handled
     */
    public boolean onBackPressed() {
    	return mKeyguardViewHostManager.onBackPress();
    }

    public boolean isBouncerShowing() {
//        return mBouncer.isShowing();
    	return false;
    }

    private long getNavBarShowDelay() {
        if (mPhoneStatusBar.isKeyguardFadingAway()) {
            return mPhoneStatusBar.getKeyguardFadingAwayDelay();
        } else {

            // Keyguard is not going away, thus we are showing the navigation bar because the
            // bouncer is appearing.
            return NAV_BAR_SHOW_DELAY_BOUNCER;
        }
    }

    private Runnable mMakeNavigationBarVisibleRunnable = new Runnable() {
        @Override
        public void run() {
            mPhoneStatusBar.getNavigationBarView().setVisibility(View.VISIBLE);
        }
    };

    private void updateStates() {
//    	  int vis = mContainer.getSystemUiVisibility();
          boolean showing = mShowing;
          boolean occluded = mOccluded;
          boolean bouncerShowing = mKeyguardViewHostManager.keyguardBouncerIsShowing();
//          boolean bouncerDismissible = !mBouncer.isFullscreenBouncer();

        /*  if ((bouncerDismissible || !showing) != (mLastBouncerDismissible || !mLastShowing)
                  || mFirstUpdate) {
              if (bouncerDismissible || !showing) {
                  mContainer.setSystemUiVisibility(vis & ~View.STATUS_BAR_DISABLE_BACK);
              } else {
                  mContainer.setSystemUiVisibility(vis | View.STATUS_BAR_DISABLE_BACK);
              }
          }*/
          if (mPhoneStatusBar.getNavigationBarView() != null) {
	          if(showing && !occluded ){
	        	  
	        	  mContainer.removeCallbacks(mMakeNavigationBarVisibleRunnable);
	              mPhoneStatusBar.getNavigationBarView().setVisibility(View.GONE);
	          }else{
	        	  mContainer.postOnAnimationDelayed(mMakeNavigationBarVisibleRunnable,
	                      getNavBarShowDelay());
	          }
          }
      /*   if ((!(showing && !occluded) || bouncerShowing)
                  != (!(mLastShowing && !mLastOccluded) || mLastBouncerShowing) || mFirstUpdate) {
              if (mPhoneStatusBar.getNavigationBarView() != null) {
                  if (!(showing && !occluded) || bouncerShowing) {
                      mContainer.postOnAnimationDelayed(mMakeNavigationBarVisibleRunnable,
                              getNavBarShowDelay());
                  } else {
                      mContainer.removeCallbacks(mMakeNavigationBarVisibleRunnable);
                      mPhoneStatusBar.getNavigationBarView().setVisibility(View.GONE);
                  }
              }
          }*/

          if (bouncerShowing != mLastBouncerShowing || mFirstUpdate) {
              mStatusBarWindowManager.setBouncerShowing(bouncerShowing);
//              mPhoneStatusBar.setBouncerShowing(bouncerShowing);
//              mScrimController.setBouncerShowing(bouncerShowing);
          }

          KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(mContext);
          if ((showing && !occluded) != (mLastShowing && !mLastOccluded) || mFirstUpdate) {
              updateMonitor.sendKeyguardVisibilityChanged(showing && !occluded);
          }
          if (bouncerShowing != mLastBouncerShowing || mFirstUpdate) {
              updateMonitor.sendKeyguardBouncerChanged(bouncerShowing);
          }

          mFirstUpdate = false;
          mLastShowing = showing;
          mLastOccluded = occluded;
          mLastBouncerShowing = bouncerShowing;
          mPhoneStatusBar.onKeyguardViewManagerStatesUpdated();
          
    }

    public boolean onMenuPressed() {
//        return mBouncer.onMenuPressed();
    	return false;
    }

    public boolean interceptMediaKey(KeyEvent event) {
//        return mBouncer.interceptMediaKey(event);
    	return false;
    }

    public void onActivityDrawn() {
        if (mPhoneStatusBar.isCollapsing()) {
            mPhoneStatusBar.addPostCollapseAction(new Runnable() {
                @Override
                public void run() {
                    mViewMediatorCallback.readyForKeyguardDone();
                }
            });
        } else {
            mViewMediatorCallback.readyForKeyguardDone();
        }
    }

    public boolean shouldDisableWindowAnimationsForUnlock() {
        return mPhoneStatusBar.isInLaunchTransition();
    }

    public boolean isGoingToNotificationShade() {
        return mPhoneStatusBar.isGoingToNotificationShade();
    }

    public boolean isSecure(int userId) {
//        return mBouncer.isSecure() || mLockPatternUtils.isSecure(userId);
    	return mKeyguardViewHostManager.isSecure();
    }

    public boolean isInputRestricted() {
        return mViewMediatorCallback.isInputRestricted();
    }
    
    public void bouncerShowing(){
    	mPhoneStatusBar.gotoBouncer();
    	updateStates();
    }
    
    public void KeyguardShowing(){
    	mPhoneStatusBar.showKeyguard();
    	updateStates();
    }
    
   public void  keyguardNotificationdismissWithAction(OnDismissAction r, boolean afterKeyguardGone){
	   dismissWithAction(r, afterKeyguardGone);
   }
   
    public void setKeyguardShowWallpaer(boolean isShow) {
        mStatusBarWindowManager.setKeyguardShowWallpaper(isShow);
    }
   
    public void resetToHomePositionIfNoSecure(){
        mKeyguardViewHostManager.resetToHomePositionIfNoSecure();
    }
    
    public void unlockKeyguardByOtherApp(){
    	mKeyguardViewHostManager.unlockKeyguardByOtherApp();
    }

    public void checkSkylightKeyguardDisabled(boolean expanded){
		mStatusBarWindowManager.setStatusBarExpanded(expanded);
    }
}
