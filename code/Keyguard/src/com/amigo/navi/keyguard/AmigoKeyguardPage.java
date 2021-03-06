package com.amigo.navi.keyguard;


import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amigo.navi.keyguard.Guide.GuideState;
import com.amigo.navi.keyguard.haokan.CaptionsView;
import com.amigo.navi.keyguard.haokan.Common;
import com.amigo.navi.keyguard.haokan.PlayerButton;
import com.amigo.navi.keyguard.haokan.PlayerLayout;
import com.amigo.navi.keyguard.haokan.UIController;
import com.amigo.navi.keyguard.modules.AmigoBatteryStatus;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
//import com.gionee.navi.keyguard.KeyguardUpdateMonitor;
//import com.gionee.navi.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.R;
//import com.amigo.navi.keyguard.KeyguardInfoZone;
import com.amigo.navi.keyguard.modules.KeyguardNotificationModule;
import com.amigo.navi.keyguard.notification.ActivatableNotificationView;
import com.amigo.navi.keyguard.notification.NotificationStackScrollLayout;
import com.amigo.navi.keyguard.settings.KeyguardSettings;
import com.amigo.navi.keyguard.util.DataStatistics;
//import com.gionee.navi.keyguard.everydayphoto.WallpaperData;
import com.amigo.navi.keyguard.util.QuickSleepUtil;
import com.amigo.navi.keyguard.haokan.PlayerManager;
import com.amigo.navi.keyguard.haokan.analysis.HKAgent;
import com.amigo.navi.keyguard.haokan.menu.ArcLayout;

public class AmigoKeyguardPage extends RelativeLayout {
	private static final String LOG_TAG = "NaviKg_KeyguardPage";
	private Context mContext=null;
	
//	private KeyguardInfoZone mInfozone = null;
	public static MissedInfo sMsgCache = null;
	
	private RelativeLayout mHaoKanLayout;
	
	private ImageView imageViewScrollUp;
	
	public AmigoKeyguardPage(Context context) {
		this(context, null);
	}

	public AmigoKeyguardPage(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext=context;
//		int padding = getResources().getDimensionPixelSize(R.dimen.kg_maincell_layout_padding_left_and_right);
//        setPadding(padding, 0, padding, 0);
        //
		mStatusBarHeight=KWDataCache.getStatusBarHeight();
		mCarrierMarginTop = getResources().getDimensionPixelSize(R.dimen.kg_maincell_layout_carrier_margin_top);
        mCarrierBatteryGap = getResources().getDimensionPixelSize(R.dimen.kg_maincell_layout_carrier_battery_gap);
        mBatteryNotificationGap = getResources().getDimensionPixelSize(R.dimen.kg_maincell_layout_battery_notification_gap);
        mNotificationHeight = getResources().getDimensionPixelSize(R.dimen.kg_maincell_layout_notification_height);
        
//        setPadding(0, KWDataCache.getStatusBarHeight(), 0, 0);
		showWallpaper();
		UIController.getInstance().setAmigoKeyguardPage(this);
	}
	
	@Override
	public void onFinishInflate() {
		super.onFinishInflate();
//		inflateLockScreenLayout(mContext);
		initUI();
	}
	
	@Override
	protected void onAttachedToWindow() {
		// TODO Auto-generated method stub
		super.onAttachedToWindow();
		
		addCarrierView();
		addBatteryView();
		addHKMainLayout();
		addNotificationClickTipView();
		addNotificationView();
		addGuideNewWallpaperView();
		addGuideSlideFeedBackView();

		addFingerIdentifyTip();
		
		KeyguardUpdateMonitor.getInstance(mContext).registerCallback(
				mUpdateMonitorCallback);
		mContext.registerReceiverAsUser(
                mReceiver, UserHandle.OWNER, new IntentFilter(Intent.ACTION_TIME_TICK), null, null);
	}
	

    @Override
	protected void onDetachedFromWindow() {
		// TODO Auto-generated method stub
		super.onDetachedFromWindow();
		KeyguardUpdateMonitor.getInstance(mContext).removeCallback(
				mUpdateMonitorCallback);
		if(mReceiver != null){
			mContext.unregisterReceiver(mReceiver);
		}
//		KeyguardNotificationModule module = KeyguardUpdateMonitor.getInstance(mContext).getNotificationModule();
//		module.unRegisterCallback();
	}
	
	private LinearLayout mCarrierText;
	private TextView mBatteryInfoView;
	private TextView mNotificationClickTipView;
	private NotificationStackScrollLayout mNotificationContent;
	private RelativeLayout mFingerIdentifyTip;
	
	private AmigoBatteryStatus mBatteryStatus = null;
	private static final float BATTERY_HIDE_ALPHA = 0.001f;
	private static final int HANDLER_REMOVE_NOTIFICATION_TIP = 1;
	private static final int HANDLER_HIDE_IDENTIFY_TIP = 2;
	private static final long HANDLER_REMOVE_NOTIFICATION_TIP_DELAY_TIME = 
			ActivatableNotificationView.DOUBLETAP_TIMEOUT_MS;
	
	private int mStatusBarHeight=0;
	private int mCarrierMarginTop = 0;
	private int mCarrierHeight = 0;
	private int mCarrierBatteryGap = 0;
	private int mBatteryMarginTop = 0;
	private int mBatteryInfoViewHeight = 0;
	private int mNotificationMarginTop = 0;
	private int mBatteryNotificationGap = 0;
	private int mNotificationHeight = 0;
	
	
	
	
	private void addHKMainLayout() {

	    
	    
	    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
	            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
	    params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.haokan_main_layout_marginbottom);
	    if (mHaoKanLayout != null) {
	        addView(mHaoKanLayout, params);
	        return;
        }
	    
	    LayoutInflater inflater=LayoutInflater.from(mContext);
	    mHaoKanLayout = (RelativeLayout)inflater.inflate(R.layout.haokan_main_layout, null);
	    PlayerLayout playerLayout = (PlayerLayout) mHaoKanLayout.findViewById(R.id.haokan_page_layout_player);
	    CaptionsView captionsView = (CaptionsView) mHaoKanLayout.findViewById(R.id.haokan_page_layout_captions);
        PlayerButton playerButton = (PlayerButton) playerLayout.findViewById(R.id.haokan_page_layout_imageButton);
        
        final UIController controller = UIController.getInstance();
        controller.setPlayerLayout(playerLayout);
        controller.setmCaptionsView(captionsView);
        controller.setmPlayerButton(playerButton);
 
        controller.setHaoKanLayout(mHaoKanLayout);
        
        final PlayerManager playerManager =  PlayerManager.getInstance();
        
        playerButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                playerManager.pauseOrPlayer(controller.getmCurrentWallpaper().getMusic());
            }
        });
        playerManager.setPlayerButton(playerButton);
        playerManager.setPlayerLayout(playerLayout);
        addView(mHaoKanLayout, params);
        
        /*if (Common.isPowerSaverMode()) {
            mHaoKanLayout.setVisibility(GONE);
        }*/
        
    }
	
	
	
	public void addGuideScrollUpView(){
 
	    imageViewScrollUp = new ImageView(getContext());
	    AnimationDrawable animationDrawable = (AnimationDrawable) getResources().getDrawable(R.drawable.keyguard_guide_scroll_up_animation);
	    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
	    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, TRUE);
	    params.addRule(RelativeLayout.CENTER_HORIZONTAL, TRUE);
	    params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.guide_slide_up_view_marginbottom);
	    imageViewScrollUp.setBackground(animationDrawable);
		animationDrawable.start();
		addView(imageViewScrollUp, params);
        Guide.setGuideState(GuideState.SCROLL_UP);
	}
	
	
	public void removeGuideScrollUpView() {

		if (!Guide.needGuideScrollUp()) {
			return;
		}
		if (imageViewScrollUp != null) {
		    removeView(imageViewScrollUp);
		    imageViewScrollUp = null;
        }
		Guide.setNeedGuideScrollUp(false);
		Guide.setBooleanSharedConfig(mContext, Guide.GUIDE_SCROLL_UP, false);
		Guide.resetIdle();
	}
	
	public void setGuideScrollUpVisibility(int visibility) {
	     if (imageViewScrollUp != null) {
	         imageViewScrollUp.setVisibility(visibility);
        }
	}
	
	
	private void addCarrierView(){
	    LayoutInflater inflater=LayoutInflater.from(mContext);
		mCarrierText=(LinearLayout)inflater.inflate(R.layout.amigo_keyguard_carrier_area, null);
		// set carrier text

		AmigoKeyguardPage.LayoutParams lp = new AmigoKeyguardPage.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
		mCarrierText.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
		lp.setMargins(0, mStatusBarHeight+mCarrierMarginTop, 0, 0);
		addView(mCarrierText, lp);
//		mCarrierText.setBackgroundColor(Color.parseColor("#880000ff"));
		if(mCarrierHeight == 0){
			mCarrierHeight = measureViewHeight(mCarrierText);
			if(DebugLog.DEBUG) DebugLog.d(LOG_TAG, "addCarrierView mCarrierHeight:"+mCarrierHeight);
			mBatteryMarginTop = mCarrierMarginTop + mCarrierHeight + mCarrierBatteryGap;
		}
	}
	
	private void addBatteryView() {
		mBatteryInfoView = new TextView(mContext);
		mBatteryInfoView.setTextAppearance(mContext, R.style.kg_maincelllayout_battery_notification_textview);
		mBatteryInfoView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
		AmigoKeyguardPage.LayoutParams lp2 = new AmigoKeyguardPage.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
		
		lp2.setMargins(0, mStatusBarHeight+mBatteryMarginTop, 0, 0);
		addView(mBatteryInfoView, lp2);
		if(mBatteryInfoViewHeight == 0){
			mBatteryInfoViewHeight = measureViewHeight(mBatteryInfoView);
			mNotificationMarginTop = mBatteryMarginTop + mBatteryInfoViewHeight + mBatteryNotificationGap;
			if(DebugLog.DEBUG) DebugLog.d(LOG_TAG, "addBatteryView mBatteryInfoViewHeight:"+mBatteryInfoViewHeight+"--mBatteryMarginTop:"+mBatteryMarginTop+"---mNotificationMarginTop:"+mNotificationMarginTop);
		}
	}
	
	private void addNotificationClickTipView() {
		mNotificationClickTipView = new TextView(mContext);
		mNotificationClickTipView.setTextAppearance(mContext, R.style.kg_maincelllayout_battery_notification_textview);
		mNotificationClickTipView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
		AmigoKeyguardPage.LayoutParams lp = new AmigoKeyguardPage.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
		lp.setMargins(0, mStatusBarHeight+mBatteryMarginTop, 0, 0);
		addView(mNotificationClickTipView, lp);
		mNotificationClickTipView.setVisibility(View.GONE);
	}
	
	private void addNotificationView() {
		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View notificationView =inflater.inflate(
				R.layout.amigo_keyguard_notification_scrollview, this, false);
		UIController controller = UIController.getInstance();
		controller.setmKeyguardNotificationView(notificationView);
		controller.setNotificationMarginTop(mNotificationMarginTop);
		AmigoKeyguardPage.LayoutParams lp = new AmigoKeyguardPage.LayoutParams(LayoutParams.MATCH_PARENT,mNotificationHeight);
		lp.setMargins(0, mNotificationMarginTop, 0, 0);
		addView(notificationView,lp);
		KeyguardNotificationModule module = KeyguardUpdateMonitor.getInstance(mContext).getNotificationModule();
		mNotificationContent = 
				(NotificationStackScrollLayout) notificationView.findViewById(R.id.notification_stack_scroller);
		int notificationRowHeight = getResources().getDimensionPixelSize(R.dimen.notification_min_height);
		mNotificationContent.getLayoutParams().height = mNotificationHeight - notificationRowHeight;
        module.registerCallback(mNotificationContent);
        module.init();
	}
	
	
    public Rect getNotificationContentRect() {
        Rect rect = new Rect(0, 0, 0, 0);
        if (mNotificationContent != null && (mNotificationContent.getVisibility() == View.VISIBLE)) {
            int[] windowLocation = new int[2];
            mNotificationContent.getLocationInWindow(windowLocation);
            int width = mNotificationContent.getWidth();
            int height = mNotificationContent.getHeight() - mNotificationContent.getEmptyBottomMargin();
            rect.left = windowLocation[0];
            rect.top = windowLocation[1];
            rect.right = windowLocation[0] + width;
            rect.bottom = windowLocation[1] + height;
        }

        return rect;
    }
    
    private void addFingerIdentifyTip() {
        LayoutInflater inflater=LayoutInflater.from(mContext);
        mFingerIdentifyTip=(RelativeLayout)inflater.inflate(R.layout.amigo_keyguard_finger_identify_tip_view, null);
        AmigoKeyguardPage.LayoutParams params=new AmigoKeyguardPage.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        params.bottomMargin=getResources().getDimensionPixelSize(R.dimen.fingerprint_fail_tip_margin_bottom);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        mFingerIdentifyTip.setLayoutParams(params);
        addView(mFingerIdentifyTip, params);
        mFingerIdentifyTip.setVisibility(View.GONE);
        
    }
    

	private int measureViewHeight(View view) {
		int w = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);  
		int h = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);  
		view.measure(w, h);  
		return view.getMeasuredHeight();
	}
	
	private KeyguardUpdateMonitorCallback mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
		@Override
		public void onRefreshBatteryInfo(AmigoBatteryStatus status) {
			if (status == null) {
				if(DebugLog.DEBUG) DebugLog.d(LOG_TAG, "BatteryStatus is null");
				return;
			}
			mBatteryStatus = status;
			updateBatteryInfo(status);
		}
		
		public void onNotificationClickTipShow(boolean isShow,ActivatableNotificationView view) {
			handleNotificationClickTip(isShow,view);
		}
		
		public void onResetNotificationClick(ActivatableNotificationView view) {
			handleNotificationClickTip(false,view);
		}
	};
	
	private void handleNotificationClickTip(boolean isShow,ActivatableNotificationView view) {
		if(DebugLog.DEBUG) DebugLog.d(LOG_TAG, "showNotificationClickTip:"+isShow+"view:"+view);
		if(isShow){
			ActivatableNotificationView previousView = mNotificationContent.getActivatedChild();
			if (previousView != null) {
				previousView.makeInactive(true /* animate */);
			}
			mNotificationContent.setActivatedChild(view);
			showNotificationClickTip(true);
		}else{
			if (view == mNotificationContent.getActivatedChild()) {
				mNotificationContent.setActivatedChild(null);
			}
		}
	}
	
	private void showNotificationClickTip(boolean isShow){
		if(DebugLog.DEBUG) DebugLog.d(LOG_TAG, "showNotificationClickTip:"+isShow);
		if(isShow){
			if(mBatteryInfoView != null && mBatteryInfoView.getVisibility() == View.VISIBLE){
				mBatteryInfoView.setAlpha(BATTERY_HIDE_ALPHA);
			}
			if (mNotificationClickTipView != null) {
				mNotificationClickTipView.setVisibility(View.VISIBLE);
				mNotificationClickTipView.setText(R.string.notification_tap_again);
		        mHandler.removeMessages(HANDLER_REMOVE_NOTIFICATION_TIP);
		        mHandler.sendEmptyMessageDelayed(HANDLER_REMOVE_NOTIFICATION_TIP, 
		        		HANDLER_REMOVE_NOTIFICATION_TIP_DELAY_TIME);
			}
		}else{
			if(mBatteryInfoView != null && (Math.abs(mBatteryInfoView.getAlpha() - BATTERY_HIDE_ALPHA) < .0000001)){
				mBatteryInfoView.setAlpha(1f);
			}
			if (mNotificationClickTipView != null) {
				mNotificationClickTipView.setVisibility(View.GONE);
			}
		}
	
	}
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			int what = msg.what;
			switch (what) {
			case HANDLER_REMOVE_NOTIFICATION_TIP:
				showNotificationClickTip(false);
				break;
			case HANDLER_HIDE_IDENTIFY_TIP:
			    mFingerIdentifyTip.setVisibility(View.GONE);
			    break;
			default:
				break;
			}
		}
	};
	
	BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	if(mBatteryStatus != null){
        		updateBatteryInfo(mBatteryStatus);
        	}
        }
    };
	
	private void updateBatteryInfo(AmigoBatteryStatus status) {
		if (mBatteryInfoView != null
				&& mBatteryInfoView.getVisibility() == View.VISIBLE) {
			String text = status.getBatteryInfoText(mContext);
			int color = status.getBatteryInfoTextColor(mContext);
			if(DebugLog.DEBUGMAYBE) DebugLog.d(LOG_TAG, "updateBatteryInfo() text=" + text + ", color="
					+ color);
			mBatteryInfoView.setTextColor(color);
			mBatteryInfoView.setText(text);
		}
	}
	
//	public Rect getNotificationContentRect(){
//		Rect rect=new Rect(0,0,0,0);
//		if (mNotificationContent != null
//				&& (mNotificationContent.getVisibility() == View.VISIBLE)) {
//			int[] windowLocation = new int[2];
//			mNotificationContent.getLocationInWindow(windowLocation);
//			int width = mNotificationContent.getWidth();
//			int height = mNotificationContent.getHeight()-mNotificationContent.getEmptyBottomMargin();
//			rect.left = windowLocation[0];
//			rect.top = windowLocation[1];
//			rect.right = windowLocation[0] + width;
//			rect.bottom = windowLocation[1] + height;
//		}
//		
//		return rect;
//	}
	
  public static class MissedInfo {
	public String pkgName;
	public String clsName;
	public String detailInfo;
	public String detailContent;
}

	@SuppressLint("NewApi")
	public void updateBackground(boolean launcherShotAdded) {
//		if(launcherShotAdded) {
//			setBackground(null);
//		} else {
//			Drawable wallpaper = AmigoWallpaperUtils.getInstance(getContext()).getSystemWallpaper();
//			setBackground(wallpaper);
//		}
	}
	
	public void showWallpaper() {
		updateBackground(false);
	}
	
	public void removeWallpaper() {
		updateBackground(true);
	}
	
//	public int getInfozoneHeight() {
//		if(mInfozone == null) return 0;
////		return mInfozone.getMeasuredHeight();
//        int h = KWDataCache.getAllScreenHeigt(mContext) - mInfozone.getFixePanelTop();
//        if(DebugLog.DEBUG){
//        	DebugLog.d(LOG_TAG, "getInfozoneHeight--h="+h);
//		}
//        return h;
//	}
	
//	public void showWeatherCondition(boolean withAnim){
//		if(mInfozone != null) {
//			mInfozone.showWeatherCondition(withAnim);
//		}
//	}
	
//	public void showTimeInfo(boolean withAnim){
//		if(mInfozone != null) {
//			if(DebugLog.DEBUG){
//				DebugLog.d(LOG_TAG, "showTimeInfo()");
//			}
//			mInfozone.showTimeInfo(withAnim);
//		}
//	}
	
//	public void showAllWeatherCondition(boolean withAnim){
//		if(mInfozone != null) {
//			mInfozone.showAllWeatherCondition(withAnim);
//		}
//	}
	
//	public void showAllTimeInfo(boolean withAnim){
//		if(mInfozone != null) {
//			mInfozone.showAllTimeInfo(withAnim);
//		}
//	}
	
//	public void showLauncherInfoZone(boolean withAnim, Bitmap image){
//		
//		
//		if(mInfozone != null) {
////			mInfozone.showWeatherCondition(withAnim);
//			mInfozone.showLauncherImage(withAnim, image);
//		}
//	}
	
    public boolean allowMsgToShow() {
        boolean noSec = true;
//        if (mKeyguardHostView != null) {
//            // noSec = mKeyguardHostView.isHideSecurityLockScreen();
//            noSec = mKeyguardHostView.isSecurityViewHidden();
//        }
//        
//        NaviWindowTransformer wt = NaviWindowTransformer.getInstance(mContext);
//        boolean isDraging = wt.isWindowTransforming() || wt.isAnimatorRunning();
//        
//        boolean isHomePage = true;
//        if (mAppWidgetPagerContainer != null) {
//            int curPage = mAppWidgetPagerContainer.getCurrentPageIndex();
//            if (curPage != mPageIndexHome) {
//                isHomePage = false;
//            }
//        }
//        
//        DebugLog.d(LOG_TAG, "noSec: " + noSec + " isDraging: " + isDraging + " isHomePage: " + isHomePage);
//       
//        
//        return noSec && !isDraging && isHomePage;
        return true;
    }
    
//	private void inflateLockScreenLayout(Context context) {
//		mInfozone = (KeyguardInfoZone) findViewById(R.id.amigo_infozone);
//		mInfozone.setLockScreen(this);
//	}
	
    private void initUI() {
//    	mInfozone.loadTimeWidget();
    }
	    
	    public void onScreenTurnedOn(){
//	    	if(mInfozone!=null){
//	    		mInfozone.onResume();
//	    }
	        UIController.getInstance().onScreenTurnedOn();
//	        HKAgent.onEventScreenOn(mContext, UIController.getInstance().getmCurrentWallpaper());
	    }
	    
	    public void onScreenTurnedOff(){
//	    	if(mInfozone!=null){
//	    		mInfozone.onPause();
//	    	}
	        
	        PlayerManager.getInstance().onScreenTurnedOff();
            UIController.getInstance().onScreenTurnedOff();
	    }
	    
	    public void show(){
//			KWWorkspace workSpace = (KWWorkspace)findViewById(R.id.kw_workspace);
//			if(workSpace!=null){
//				workSpace.show();
//			}
	    }
	    
	    public void hide(){
//	        KWWorkspace workSpace = (KWWorkspace)findViewById(R.id.kw_workspace);
//            if(workSpace!=null){
//                workSpace.hide();
//            }
	    }

		public void cleanUp() {
//			mInfozone.cleanUp();
//			mInfozone = null;
		}
		
//		public void showComment(WallpaperData data){
//			KWWorkspace workSpace = (KWWorkspace)findViewById(R.id.kw_workspace);
//			if(workSpace!=null){
//				workSpace.setComment(data);
//			}
//	    }
		
		public void hiddenComment(){
//			KWWorkspace workSpace = (KWWorkspace)findViewById(R.id.kw_workspace);
//			if(workSpace!=null){
//				workSpace.hiddenComment();
//			}
	    }
		
		
		
		public void onShowMissCount(){
			setMissInfoVisible(true);
		}
		
		public void onHiddenMissCount(){
			setMissInfoVisible(false);
		}
		
		
		private void setMissInfoVisible(boolean showOrHidden){
//			KWWorkspace workspace = (KWWorkspace) findViewById(R.id.kw_workspace);
//			if(workspace!=null){
//				workspace.setMissInfoVisible(showOrHidden);
//			}
		}
		
		public void hiddenMissCout(){
//			mInfozone.hiddenMissCount();
		}
		
		public void showMissCount(){
//			mInfozone.showMissCount();
		}
		
    public void gotoSleepIfDoubleTap(MotionEvent event) {
        // if(!mIsDoubleTapSwitchOpen){
        // if(DebugLog.DEBUG){
        // DebugLog.d(LOG_TAG,
        // "gotoSleepIfDoubleTap mIsDoubleTapSwitchOpen:"+mIsDoubleTapSwitchOpen);
        // }
        // return;
        // }
        // if(isPageMoving()||!isHomePage()){
        // return;
        // }
        boolean isDoubleTapOpen=KeyguardSettings.getDoubleDesktopLockState(getContext());
        if (DebugLog.DEBUG) {
            DebugLog.d(LOG_TAG, "gotoSleepIfDoubleTap QuickSleepUtil.gotoSleepIfDoubleTap isOpen: "+isDoubleTapOpen);
        }
        if (isDoubleTapOpen) {
            QuickSleepUtil.gotoSleepIfDoubleTap(mContext, event, this);
            DataStatistics.getInstance().doubleTapSleepEvent(mContext);
        }
    }
    
    
    ObjectAnimator mShakeAnimator = null;

    public void shakeIdentifyTip() {
        mFingerIdentifyTip.setVisibility(View.VISIBLE);
        if (mShakeAnimator == null) {
            mShakeAnimator = ObjectAnimator.ofFloat(mFingerIdentifyTip, "translationX",
                    new float[] { 0.0F, -40.0F, 40.0F, -20.0F, 20.0F, 0.0F }).setDuration(
                    700);
            mShakeAnimator.addListener(new AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {
                    mFingerIdentifyTip.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mHandler.sendEmptyMessageDelayed(HANDLER_HIDE_IDENTIFY_TIP, 1500);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }
            });
        }
        if (mShakeAnimator.isRunning()) {
            mShakeAnimator.cancel();
        }
        mHandler.removeMessages(HANDLER_HIDE_IDENTIFY_TIP);
        mShakeAnimator.start();
    }
    
    
    public void startGuideSlideAround() {
        DebugLog.d("guide", "startGuideSlideAround ");
        LayoutInflater inflater = LayoutInflater.from(mContext);
        RelativeLayout view = (RelativeLayout)inflater.inflate(R.layout.guide_slide_around, null);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.topMargin = getResources().getDimensionPixelSize(R.dimen.guide_slide_margin_top);
        addView(view, params);
        ImageView left = (ImageView) view.findViewById(R.id.guide_slide_around_left);
        ImageView right = (ImageView) view.findViewById(R.id.guide_slide_around_right);
        ((AnimationDrawable)left.getDrawable()).start();
        ((AnimationDrawable)right.getDrawable()).start();
        Guide.setGuideState(GuideState.SLIDE_AROUND);
    }
    
    public void stopGuideSlideAround() {
        DebugLog.d("guide", "stopGuideSlideAround ");
        final RelativeLayout view = (RelativeLayout) findViewById(R.id.guide_slide_around);
        if (view != null) {
            removeView(view);
        }
        Guide.resetIdle();
        Guide.setNeedGuideSlideAround(false);
        Guide.setBooleanSharedConfig(getContext(), Guide.GUIDE_SLIDE_AROUND, false);
        
    }
    
    private ImageView mGuideNewWallpaperView;
    private ObjectAnimator mGuideNewWallpaperAnimator;
    
    private void addGuideNewWallpaperView() {
        if (mGuideNewWallpaperView == null) {
            mGuideNewWallpaperView = new ImageView(getContext());
        }
        int size =getResources().getDimensionPixelSize(R.dimen.guide_new_wallpaper_size);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size, size);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        params.topMargin = getResources().getDimensionPixelSize(R.dimen.guide_slide_margin_top);
        mGuideNewWallpaperView.setBackgroundResource(R.drawable.guide_new_wallpaper);
        mGuideNewWallpaperView.setVisibility(GONE);
        addView(mGuideNewWallpaperView, params);
    }
    
    public void startGuideNewWallpaper() {
        
        DebugLog.d("guide", "startGuideNewWallpaper ");
        
        if (mGuideNewWallpaperAnimator != null) {
            if (mGuideNewWallpaperAnimator.isRunning()) {
                mGuideNewWallpaperAnimator.cancel();
            }
        }
        int size =getResources().getDimensionPixelSize(R.dimen.guide_new_wallpaper_size);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.0f, 0.4f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.0f, 0.4f);
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1.0f, 0f);
        PropertyValuesHolder translationX = PropertyValuesHolder.ofFloat("translationX", size, -getResources().getDimensionPixelSize(
                                R.dimen.guide_new_wallpaper_max_translationX));

        mGuideNewWallpaperAnimator = ObjectAnimator.ofPropertyValuesHolder(mGuideNewWallpaperView, translationX,scaleX,scaleY,alpha).setDuration(1500);
        mGuideNewWallpaperAnimator.addListener(new AnimatorListener() {
            
            private boolean cancel = false;
            
            @Override
            public void onAnimationStart(Animator arg0) {
                
            }
            
            @Override
            public void onAnimationRepeat(Animator arg0) {
            }
            
            @Override
            public void onAnimationEnd(Animator arg0) {
                if (!cancel) {
                    arg0.setStartDelay(500);
                    arg0.start();
                }
                cancel = false;
            }
            
            @Override
            public void onAnimationCancel(Animator arg0) {
                cancel = true;
            }
        });
        mGuideNewWallpaperView.setVisibility(VISIBLE);
        mGuideNewWallpaperAnimator.start();
        Guide.setGuideState(GuideState.NEW_WALLPAPER);
        

    }
    
    public void stopGuideNewWallpaper() {
        if (mGuideNewWallpaperAnimator != null) {
            if (mGuideNewWallpaperAnimator.isRunning()) {
                mGuideNewWallpaperAnimator.cancel();
            }
        }
        mGuideNewWallpaperView.setVisibility(GONE);
        Guide.resetIdle();
    }
    
 
    private RelativeLayout mGuideSlideFeedBackView;
    private void addGuideSlideFeedBackView() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        RelativeLayout view = (RelativeLayout)inflater.inflate(R.layout.guide_slide_feedback, null);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.topMargin = getResources().getDimensionPixelSize(R.dimen.guide_slide_margin_top);
        addView(view, params);
        mGuideSlideFeedBackView = view;
        
    }
    
    public void setGuideSlideFeedBackVisibility(int visibility) {
        mGuideSlideFeedBackView.setVisibility(visibility);
    }
   
}
