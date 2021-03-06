/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.EventLog;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import com.android.systemui.EventLogTags;
import com.android.systemui.R;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.gionee.GnUtil;

public class PhoneStatusBarView extends PanelBar {
    private static final String TAG = "PhoneStatusBarView";
    private static final boolean DEBUG = PhoneStatusBar.DEBUG;
    private static final boolean DEBUG_GESTURES = false;

    PhoneStatusBar mBar;

    PanelView mLastFullyOpenedPanel = null;
    PanelView mNotificationPanel;
    private final PhoneStatusBarTransitions mBarTransitions;
    private ScrimController mScrimController;

    public PhoneStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources res = getContext().getResources();
        mBarTransitions = new PhoneStatusBarTransitions(this);
    }

    public BarTransitions getBarTransitions() {
        return mBarTransitions;
    }

    public void setBar(PhoneStatusBar bar) {
        mBar = bar;
    }

    public void setScrimController(ScrimController scrimController) {
        mScrimController = scrimController;
    }

    @Override
    public void onFinishInflate() {
        mBarTransitions.init();
    }

    @Override
    public void addPanel(PanelView pv) {
        super.addPanel(pv);
        if (pv.getId() == R.id.notification_panel) {
            mNotificationPanel = pv;
        }
    }

    @Override
    public boolean panelsEnabled() {
        return mBar.panelsEnabled();
    }

    @Override
    public boolean onRequestSendAccessibilityEvent(View child, AccessibilityEvent event) {
        if (super.onRequestSendAccessibilityEvent(child, event)) {
            // The status bar is very small so augment the view that the user is touching
            // with the content of the status bar a whole. This way an accessibility service
            // may announce the current item as well as the entire content if appropriate.
            AccessibilityEvent record = AccessibilityEvent.obtain();
            onInitializeAccessibilityEvent(record);
            dispatchPopulateAccessibilityEvent(record);
            event.appendRecord(record);
            return true;
        }
        return false;
    }

    @Override
    public PanelView selectPanelForTouch(MotionEvent touch) {
        // No double swiping. If either panel is open, nothing else can be pulled down.
        return mNotificationPanel.getExpandedHeight() > 0
                ? null
                : mNotificationPanel;
    }

    @Override
    public void onPanelPeeked() {
        super.onPanelPeeked();
        mBar.makeExpandedVisible(false);
    }

    @Override
    public void onAllPanelsCollapsed() {
        super.onAllPanelsCollapsed();

        // Close the status bar in the next frame so we can show the end of the animation.
        postOnAnimation(new Runnable() {
            @Override
            public void run() {
                mBar.makeExpandedInvisible();
            }
        });
        mLastFullyOpenedPanel = null;
    }

    @Override
    public void onPanelFullyOpened(PanelView openPanel) {
        super.onPanelFullyOpened(openPanel);
        if (openPanel != mLastFullyOpenedPanel) {
            openPanel.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }
        mLastFullyOpenedPanel = openPanel;
        //GIONEE <wujj> <2015-06-10> add for CR01493908/CR01496984 begin
        //need to call onLayout when notification panel is fully expanded
        mLastFullyOpenedPanel.requestLayout();
        //GIONEE <wujj> <2015-06-10> add for CR01493908/CR01496984 end
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        
        if (GnUtil.getLockState() == GnUtil.STATE_LOCK_BY_CONTROLCENTER) {
            Log.d(TAG, "return lock by cc");
            return true;
        }
        
        if (event.getAction() == MotionEvent.ACTION_DOWN 
                && GnUtil.getLockState() == GnUtil.STATE_LOCK_UNLOCK) {
            Log.d(TAG, "lock by nc");
            GnUtil.setLockState(GnUtil.STATE_LOCK_BY_NOTIFICATION);
        }
        
        boolean barConsumedEvent = mBar.interceptTouchEvent(event);

		// GIONEE <wujj> <2015-04-24> add begin
		mBar.setUpdateNaviFlag(true);
		// GIONEE <wujj> <2015-04-24> add end
        if (DEBUG_GESTURES) {
            if (event.getActionMasked() != MotionEvent.ACTION_MOVE) {
                EventLog.writeEvent(EventLogTags.SYSUI_PANELBAR_TOUCH,
                        event.getActionMasked(), (int) event.getX(), (int) event.getY(),
                        barConsumedEvent ? 1 : 0);
            }
        }

        return barConsumedEvent || super.onTouchEvent(event);
    }

    @Override
    public void onTrackingStarted(PanelView panel) {
        super.onTrackingStarted(panel);
        mBar.onTrackingStarted();
        mScrimController.onTrackingStarted();
    }

    @Override
    public void onClosingFinished() {
        super.onClosingFinished();
        mBar.onClosingFinished();
    }

    @Override
    public void onTrackingStopped(PanelView panel, boolean expand) {
        super.onTrackingStopped(panel, expand);
        mBar.onTrackingStopped(expand);
    }

    @Override
    public void onExpandingFinished() {
        super.onExpandingFinished();
        mScrimController.onExpandingFinished();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mBar.interceptTouchEvent(event) || super.onInterceptTouchEvent(event);
    }

    @Override
    public void panelExpansionChanged(PanelView panel, float frac, boolean expanded) {
        super.panelExpansionChanged(panel, frac, expanded);
        mScrimController.setPanelExpansion(frac);
        mBar.updateCarrierLabelVisibility(false);
    }
    
    // GIONEE <wujj> <2015-03-02> Modify for CR01445888 begin
    protected void updateNavigatorBarBackground() {
    	NavigationBarView navigationBarView = mBar.getNavigationBarView();
    	if (navigationBarView != null) {
    		KeyguardViewMediator keyguardViewMediator = mBar.getComponent(KeyguardViewMediator.class);
    		boolean keyGuardShowing = keyguardViewMediator.isShowingAndNotOccluded();
    		if (keyGuardShowing) {
    			navigationBarView.setBackgroundColor(Color.argb(0xff, 0, 0, 0));
    		}
    	}
    }
    // GIONEE <wujj> <2015-03-02> Modify for CR01445888 end
}
