/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.systemui.recent;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.animation.TimeInterpolator;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.TaskStackBuilder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewRootImpl;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.StatusBarPanel;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

public class RecentsPanelView extends FrameLayout implements OnItemClickListener, RecentsCallback,
        StatusBarPanel, Animator.AnimatorListener {
    static final String TAG = "RecentsPanelView";
    static final boolean DEBUG = true;// PhoneStatusBar.DEBUG || false;
    private PopupMenu mPopup;
    private View mRecentsScrim;
    private View mRecentsNoApps;
    private RecentsScrollView mRecentsContainer;

    private boolean mShowing;
    private boolean mWaitingToShow;
    private ViewHolder mItemToAnimateInWhenWindowAnimationIsFinished;
    private boolean mAnimateIconOfFirstTask;
    private boolean mWaitingForWindowAnimation;
    private long mWindowAnimationStartTime;
    private boolean mCallUiHiddenBeforeNextReload;

    private RecentTasksLoader mRecentTasksLoader;
    private ArrayList<TaskDescription> mRecentTaskDescriptions;
    private ArrayList<TaskDescription> mTaskDescriptionList = new ArrayList<TaskDescription>();
    private TaskDescriptionAdapter mListAdapter;
    private int mThumbnailWidth;
    private boolean mFitThumbnailToXY;
    private int mRecentItemLayoutId;
    private boolean mHighEndGfx;
    
    private Editor mWhiteListEditor;
    private SharedPreferences mWhiteListPreferences;

    private final static String LOCK_APP_NAME_FILE = "lock";
    private final static String PACKAGE_NAME = "packageName";
    private final int MSG_STOP_SCAN_ANIM = 0;
    
    private boolean mLockToAppEnabled;
    private int mLockTaskId;

    public static interface RecentsScrollView {
        public int numItemsInOneScreenful();
        public void setAdapter(TaskDescriptionAdapter adapter);
        public void setCallback(RecentsCallback callback);
        public void setMinSwipeAlpha(float minAlpha);
        public View findViewForTask(int persistentTaskId);
        public void drawFadedEdges(Canvas c, int left, int right, int top, int bottom);
        public void setOnScrollListener(Runnable listener);
        public void clearRecentApps();
    }

    private final class OnLongClickDelegate implements View.OnLongClickListener {
        View mOtherView;
        OnLongClickDelegate(View other) {
            mOtherView = other;
        }
        public boolean onLongClick(View v) {
            return mOtherView.performLongClick();
        }
    }

    public void saveLockAppName(String packageName) {
    	if(mWhiteListPreferences == null) {
    		mWhiteListPreferences = getContext()
    				.getSharedPreferences(LOCK_APP_NAME_FILE, Context.MODE_PRIVATE);
    	}
    	
    	if(mWhiteListEditor == null) {
    		mWhiteListEditor = mWhiteListPreferences.edit();  
    	}
        mWhiteListEditor.putString(packageName, PACKAGE_NAME);
        //mWhiteListEditor.commit();
    }

	public void commitWhiteList() {
		if(mWhiteListPreferences == null) {
    		mWhiteListPreferences = getContext()
    				.getSharedPreferences(LOCK_APP_NAME_FILE, Context.MODE_PRIVATE);
    	}
    	
    	if(mWhiteListEditor == null) {
    		mWhiteListEditor = mWhiteListPreferences.edit();  
    	}
		mWhiteListEditor.commit();
	}
    
    public void clearLockApp() {
    	if(mWhiteListPreferences == null) {
    		mWhiteListPreferences = getContext()
    				.getSharedPreferences(LOCK_APP_NAME_FILE, Context.MODE_PRIVATE);
    	}
    	
    	if(mWhiteListEditor == null) {
    		mWhiteListEditor = mWhiteListPreferences.edit();  
    	}
		mWhiteListEditor.clear();
		mWhiteListEditor.commit();
	}
    
    private void removeLockAppName (String packageName) {
    	if(mWhiteListPreferences == null) {
    		mWhiteListPreferences = getContext()
    				.getSharedPreferences(LOCK_APP_NAME_FILE, Context.MODE_PRIVATE);
    	}
    	
    	if(mWhiteListEditor == null) {
    		mWhiteListEditor = mWhiteListPreferences.edit();  
    	}         
        if (mWhiteListPreferences.contains(packageName)) {
        	mWhiteListEditor.remove(packageName);
        	mWhiteListEditor.commit();
        }
    }
    
    private boolean isLockAppName (String packageName) {
    	if(mWhiteListPreferences == null) {
    		mWhiteListPreferences = getContext()
    				.getSharedPreferences(LOCK_APP_NAME_FILE, Context.MODE_PRIVATE);
    	}
        if (mWhiteListPreferences.contains(packageName)) {
            return true;
        }else {
            return false;
        }
    }

    /* package */ final static class ViewHolder {
        View thumbnailView;
        ImageView thumbnailViewImage;
        Drawable thumbnailViewDrawable;
        ImageView iconView;
        TextView labelView;
        TextView descriptionView;
        View calloutLine;
        TaskDescription taskDescription;
        boolean loadedThumbnailAndIcon;
        ImageView lockView;
        boolean isLockApp;
        //screen lock
        ImageView lockToScreenView;
    }
    
    // Gionee <hunagwt> <2015-3-14> add for CR01453589 begin
    private boolean isInitialized(ViewHolder v) {
        return v != null
                && v.thumbnailView != null
                && v.thumbnailViewImage != null
                && v.thumbnailViewDrawable != null
                && v.iconView != null
                && v.labelView != null
                && v.descriptionView != null
                && v.calloutLine != null
                && v.taskDescription != null
                && v.lockView != null
        		&& v.lockToScreenView != null;
    }
    // Gionee <hunagwt> <2015-3-14> add for CR01453589 end

    /* package */ final class TaskDescriptionAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public TaskDescriptionAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        public int getCount() {
        	Log.d(TAG, " mRecentTaskDescriptions.size() = " + (mRecentTaskDescriptions != null ? mRecentTaskDescriptions.size() : 0));
            return mRecentTaskDescriptions != null ? mRecentTaskDescriptions.size() : 0;
        }

        public Object getItem(int position) {
            return position; // we only need the index
        }

        public long getItemId(int position) {
            return position; // we just need something unique for this position
        }

        public View createView(ViewGroup parent) {
            View convertView = mInflater.inflate(mRecentItemLayoutId, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.thumbnailView = convertView.findViewById(R.id.app_thumbnail);
            holder.thumbnailViewImage =
                    (ImageView) convertView.findViewById(R.id.app_thumbnail_image);
            // If we set the default thumbnail now, we avoid an onLayout when we update
            // the thumbnail later (if they both have the same dimensions)
            updateThumbnail(holder, mRecentTasksLoader.getDefaultThumbnail(), false, false);
            holder.iconView = (ImageView) convertView.findViewById(R.id.app_icon);
            holder.iconView.setImageDrawable(mRecentTasksLoader.getDefaultIcon());
            holder.labelView = (TextView) convertView.findViewById(R.id.app_label);
            holder.calloutLine = convertView.findViewById(R.id.recents_callout_line);
            holder.descriptionView = (TextView) convertView.findViewById(R.id.app_description);

            holder.lockView = (ImageView) convertView.findViewById(R.id.app_lock);
            holder.lockToScreenView = (ImageView) convertView.findViewById(R.id.lock_to_app_fab);
            
            convertView.setTag(holder);
            return convertView;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = createView(parent);
            }
            final ViewHolder holder = (ViewHolder) convertView.getTag();

            // index is reverse since most recent appears at the bottom...
            final int index = mRecentTaskDescriptions.size() - position - 1;

            final TaskDescription td = mRecentTaskDescriptions.get(index);

            holder.labelView.setText(td.getLabel());
            holder.thumbnailView.setContentDescription(td.getLabel());
            holder.loadedThumbnailAndIcon = td.isLoaded();
            if (td.isLoaded()) {
                updateThumbnail(holder, td.getThumbnail(), true, false);
                //updateIcon(holder, td.getIcon(), true, false);
                updateLock(holder, isLockAppName(td.packageName), false);
            }
            if (index == 0) {
                if (mAnimateIconOfFirstTask) {
                    ViewHolder oldHolder = mItemToAnimateInWhenWindowAnimationIsFinished;
                    if (oldHolder != null) {
                        oldHolder.iconView.setAlpha(1f);
                        oldHolder.iconView.setTranslationX(0f);
                        oldHolder.iconView.setTranslationY(0f);
                        oldHolder.labelView.setAlpha(1f);
                        oldHolder.labelView.setTranslationX(0f);
                        oldHolder.labelView.setTranslationY(0f);
                        if (oldHolder.calloutLine != null) {
                            oldHolder.calloutLine.setAlpha(1f);
                            oldHolder.calloutLine.setTranslationX(0f);
                            oldHolder.calloutLine.setTranslationY(0f);
                        }
                    }
                    mItemToAnimateInWhenWindowAnimationIsFinished = holder;
                    int translation = -getResources().getDimensionPixelSize(
                            R.dimen.status_bar_recents_app_icon_translate_distance);
                    final Configuration config = getResources().getConfiguration();
                    if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        if (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                            translation = -translation;
                        }
                        holder.iconView.setAlpha(0f);
                        holder.iconView.setTranslationX(translation);
                        holder.labelView.setAlpha(0f);
                        holder.labelView.setTranslationX(translation);
                        holder.calloutLine.setAlpha(0f);
                        holder.calloutLine.setTranslationX(translation);
                    } else {
                        holder.iconView.setAlpha(0f);
                        holder.iconView.setTranslationY(translation);
                    }
                    if (!mWaitingForWindowAnimation) {
                        animateInIconOfFirstTask();
                    }
                }
            }

            holder.thumbnailView.setTag(td);
            holder.thumbnailView.setOnLongClickListener(new OnLongClickDelegate(convertView));
            holder.taskDescription = td;
            return convertView;
        }

        public void recycleView(View v) {
            ViewHolder holder = (ViewHolder) v.getTag();
            updateThumbnail(holder, mRecentTasksLoader.getDefaultThumbnail(), false, false);
            if(holder.iconView != null 
            		&& holder.labelView != null 
            		&& holder.thumbnailView != null 
            		&& holder.calloutLine != null) {
            	holder.iconView.setImageDrawable(mRecentTasksLoader.getDefaultIcon());
            	holder.iconView.setVisibility(INVISIBLE);
            	holder.iconView.animate().cancel();
            	holder.labelView.setText(null);
            	holder.labelView.animate().cancel();
            	holder.thumbnailView.setContentDescription(null);
            	holder.thumbnailView.setTag(null);
            	holder.thumbnailView.setOnLongClickListener(null);
            	holder.thumbnailView.setVisibility(INVISIBLE);
            	holder.iconView.setAlpha(1f);
            	holder.iconView.setTranslationX(0f);
            	holder.iconView.setTranslationY(0f);
            	holder.labelView.setAlpha(1f);
            	holder.labelView.setTranslationX(0f);
            	holder.labelView.setTranslationY(0f);
            	holder.calloutLine.setAlpha(1f);
            	holder.calloutLine.setTranslationX(0f);
            	holder.calloutLine.setTranslationY(0f);
            	holder.calloutLine.animate().cancel();
            	holder.taskDescription = null;
            	holder.loadedThumbnailAndIcon = false;
            	holder.lockView.setVisibility(INVISIBLE);
            	holder.lockView.animate().cancel();
            }
            //holder.isLockApp = false;
        }
    }

    public RecentsPanelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecentsPanelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        updateValuesFromResources();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RecentsPanelView,
                defStyle, 0);

        mRecentItemLayoutId = a.getResourceId(R.styleable.RecentsPanelView_recentItemLayout, 0);
        mRecentTasksLoader = RecentTasksLoader.getInstance(context);
        a.recycle();
        context.getContentResolver().registerContentObserver(
        		Settings.System.getUriFor(Settings.System.LOCK_TO_APP_ENABLED), false, mScreenLockObserver);
        mLockToAppEnabled = Settings.System.getInt(getContext().getContentResolver(), Settings.System.LOCK_TO_APP_ENABLED, 0) != 0;
    }

    public int numItemsInOneScreenful() {
        return mRecentsContainer.numItemsInOneScreenful();
    }

    private boolean pointInside(int x, int y, View v) {
        final int l = v.getLeft();
        final int r = v.getRight();
        final int t = v.getTop();
        final int b = v.getBottom();
        return x >= l && x < r && y >= t && y < b;
    }

    public boolean isInContentArea(int x, int y) {
        return pointInside(x, y, (View) mRecentsContainer);
    }

    public void show(boolean show) {
        show(show, null, false, false);
    }

    public void show(boolean show, ArrayList<TaskDescription> recentTaskDescriptions,
            boolean firstScreenful, boolean animateIconOfFirstTask) {
        if (show && mCallUiHiddenBeforeNextReload) {
            onUiHidden();
            recentTaskDescriptions = null;
            mAnimateIconOfFirstTask = false;
            mWaitingForWindowAnimation = false;
        } else {
            mAnimateIconOfFirstTask = animateIconOfFirstTask;
            mWaitingForWindowAnimation = animateIconOfFirstTask;
        }
        if (show) {
            mWaitingToShow = true;
            refreshRecentTasksList(recentTaskDescriptions, firstScreenful);
            showIfReady();
        } else {
            showImpl(false);
        }
    }

    private void showIfReady() {
        // mWaitingToShow => there was a touch up on the recents button
        // mRecentTaskDescriptions != null => we've created views for the first screenful of items
        if (mWaitingToShow && mRecentTaskDescriptions != null) {
            showImpl(true);
        }
    }

    static void sendCloseSystemWindows(Context context, String reason) {
        if (ActivityManagerNative.isSystemReady()) {
            try {
                ActivityManagerNative.getDefault().closeSystemDialogs(reason);
            } catch (RemoteException e) {
            }
        }
    }

    private void showImpl(boolean show) {

        mShowing = show;

        if (show) {
        	sendCloseSystemWindows(getContext(), BaseStatusBar.SYSTEM_DIALOG_REASON_RECENT_APPS);
            // if there are no apps, bring up a "No recent apps" message
            boolean noApps = mRecentTaskDescriptions != null
                    && (mRecentTaskDescriptions.size() == 0);
            mRecentsNoApps.setAlpha(1f);
            mRecentsNoApps.setVisibility(noApps ? View.VISIBLE : View.INVISIBLE);

            onAnimationEnd(null);
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();
        } else {
            mWaitingToShow = false;
            // call onAnimationEnd() and clearRecentTasksList() in onUiHidden()
            mCallUiHiddenBeforeNextReload = true;
            if (mPopup != null) {
                mPopup.dismiss();
            }
        }
    }

    protected void onAttachedToWindow () {
        super.onAttachedToWindow();
        final ViewRootImpl root = getViewRootImpl();
        if (root != null) {
            root.setDrawDuringWindowsAnimating(true);
        }
    }

    public void onUiHidden() {
        mCallUiHiddenBeforeNextReload = false;
        if (!mShowing && mRecentTaskDescriptions != null) {
            onAnimationEnd(null);
            clearRecentTasksList();
        }
    }

    public void dismiss() {
        ((RecentsActivity) getContext()).dismissAndGoHome();
    }

    public void dismissAndGoBack() {
        ((RecentsActivity) getContext()).dismissAndGoBack();
    }

    public void onAnimationCancel(Animator animation) {
    }

    public void onAnimationEnd(Animator animation) {
        if (mShowing) {
            final LayoutTransition transitioner = new LayoutTransition();
            ((ViewGroup)mRecentsContainer).setLayoutTransition(transitioner);
            createCustomAnimations(transitioner);
        } else {
            ((ViewGroup)mRecentsContainer).setLayoutTransition(null);
        }
    }

    public void onAnimationRepeat(Animator animation) {
    }

    public void onAnimationStart(Animator animation) {
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        // Ignore hover events outside of this panel bounds since such events
        // generate spurious accessibility events with the panel content when
        // tapping outside of it, thus confusing the user.
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        if (x >= 0 && x < getWidth() && y >= 0 && y < getHeight()) {
            return super.dispatchHoverEvent(event);
        }
        return true;
    }

    /**
     * Whether the panel is showing, or, if it's animating, whether it will be
     * when the animation is done.
     */
    public boolean isShowing() {
        return mShowing;
    }

    public void setRecentTasksLoader(RecentTasksLoader loader) {
        mRecentTasksLoader = loader;
    }

    public void updateValuesFromResources() {
        final Resources res = getContext().getResources();
        mThumbnailWidth = Math.round(res.getDimension(com.android.internal.R.dimen.thumbnail_width));
        mFitThumbnailToXY = res.getBoolean(R.bool.config_recents_thumbnail_image_fits_to_xy);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mRecentsContainer = (RecentsScrollView) findViewById(R.id.recents_container);
        mRecentsContainer.setOnScrollListener(new Runnable() {
            public void run() {
                // need to redraw the faded edges
                invalidate();
            }
        });
        mListAdapter = new TaskDescriptionAdapter(getContext());
        mRecentsContainer.setAdapter(mListAdapter);
        mRecentsContainer.setCallback(this);

        mRecentsScrim = findViewById(R.id.recents_bg_protect);
        mRecentsNoApps = findViewById(R.id.recents_no_apps);

        if (mRecentsScrim != null) {
            mHighEndGfx = ActivityManager.isHighEndGfx();
            if (!mHighEndGfx) {
                mRecentsScrim.setBackground(null);
            } else if (mRecentsScrim.getBackground() instanceof BitmapDrawable) {
                // In order to save space, we make the background texture repeat in the Y direction
                ((BitmapDrawable) mRecentsScrim.getBackground()).setTileModeY(TileMode.REPEAT);
            }
        }
    }

    public void setMinSwipeAlpha(float minAlpha) {
        mRecentsContainer.setMinSwipeAlpha(minAlpha);
    }

    private void createCustomAnimations(LayoutTransition transitioner) {
        transitioner.setDuration(200);
        transitioner.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0);
        transitioner.setAnimator(LayoutTransition.DISAPPEARING, null);
    }

    private void updateIcon(ViewHolder h, Drawable icon, boolean show, boolean anim) {
        if (icon != null) {
            h.iconView.setVisibility(View.GONE);
            h.iconView.setImageDrawable(icon);
            if (show && h.iconView.getVisibility() != View.VISIBLE) {
                if (anim) {
                    h.iconView.setAnimation(
                            AnimationUtils.loadAnimation(getContext(), R.anim.recent_appear));
                }
                h.iconView.setVisibility(View.GONE/*View.VISIBLE*/);
            }
        }
    }

    public void updateLock(ViewHolder h, boolean lock, boolean anim) {
        if (h != null) {
        	if(lock && h.lockView != null) {
        		h.lockView.setImageDrawable(getContext().getResources()
        				.getDrawable(R.drawable.gn_app_lock));
        		h.lockView.setVisibility(View.VISIBLE);
        		if (anim) {
        			h.iconView.setAnimation(
        					AnimationUtils.loadAnimation(getContext(), R.anim.recent_appear));
        		}
        	}
        	h.isLockApp = lock;
        }
    }

    private void updateThumbnail(ViewHolder h, Drawable thumbnail, boolean show, boolean anim) {
        if (thumbnail != null) {
            // Should remove the default image in the frame
            // that this now covers, to improve scrolling speed.
            // That can't be done until the anim is complete though.
            h.thumbnailViewImage.setImageDrawable(thumbnail);

            // scale the image to fill the full width of the ImageView. do this only if
            // we haven't set a bitmap before, or if the bitmap size has changed
            if (h.thumbnailViewDrawable == null ||
                h.thumbnailViewDrawable.getIntrinsicWidth() != thumbnail.getIntrinsicWidth() ||
                h.thumbnailViewDrawable.getIntrinsicHeight() != thumbnail.getIntrinsicHeight()) {
                if (mFitThumbnailToXY) {
                    h.thumbnailViewImage.setScaleType(ScaleType.FIT_XY);
                } else {
                    Matrix scaleMatrix = new Matrix();
                    float scale = mThumbnailWidth / (float) thumbnail.getIntrinsicWidth();
                    scaleMatrix.setScale(scale, scale);
                    h.thumbnailViewImage.setScaleType(ScaleType.MATRIX);
                    h.thumbnailViewImage.setImageMatrix(scaleMatrix);
                }
            }
            if (show && h.thumbnailView.getVisibility() != View.VISIBLE) {
                if (anim) {
                    h.thumbnailView.setAnimation(
                            AnimationUtils.loadAnimation(getContext(), R.anim.recent_appear));
                }
                h.thumbnailView.setVisibility(View.VISIBLE);
            }
            h.thumbnailViewDrawable = thumbnail;
        }
    }

    void onTaskThumbnailLoaded(TaskDescription td) {
        synchronized (td) {
            if (mRecentsContainer != null) {
                ViewGroup container = (ViewGroup) mRecentsContainer;
                if (container instanceof RecentsScrollView) {
                    container = (ViewGroup) container.findViewById(
                            R.id.recents_linear_layout);
                }
                // Look for a view showing this thumbnail, to update.
                for (int i=0; i < container.getChildCount(); i++) {
                    View v = container.getChildAt(i);
                    if (v.getTag() instanceof ViewHolder) {
                        ViewHolder h = (ViewHolder)v.getTag();
                        // Gionee <hunagwt> <2015-3-14> modify for CR01453589 begin
                        if (!h.loadedThumbnailAndIcon && h.taskDescription == td && isInitialized(h)) {
                            // Gionee <hunagwt> <2015-3-14> modify for CR01453589 end
                            // only fade in the thumbnail if recents is already visible-- we
                            // show it immediately otherwise
                            //boolean animateShow = mShowing &&
                            //    mRecentsContainer.getAlpha() > ViewConfiguration.ALPHA_THRESHOLD;
                            boolean animateShow = false;
                            updateLock(h, isLockAppName(td.packageName), animateShow);
                            if(i == 0 && mLockToAppEnabled) {
        						h.lockToScreenView.setVisibility(View.VISIBLE);
        						mLockTaskId = h.taskDescription.persistentTaskId;
        					}
                            //updateIcon(h, td.getIcon(), true, animateShow);
                            updateThumbnail(h, td.getThumbnail(), true, animateShow);
                            h.loadedThumbnailAndIcon = true;
                        }
                    }
                }
            }
        }
        showIfReady();
    }
    
    public void updateRecentAppLockState() {
		if(mRecentsContainer != null) {
			ViewGroup container = (ViewGroup) mRecentsContainer;
			if(container instanceof RecentsScrollView) {
				container = (ViewGroup) container.findViewById(R.id.recents_linear_layout);
			}
			for(int i = container.getChildCount(); i >= 0; i--) {
				View view = container.getChildAt(i);
				if(view != null && view.getTag() instanceof ViewHolder) {
					ViewHolder holder = (ViewHolder) view.getTag();
					updateLock(holder, isLockAppName(holder.taskDescription.packageName), true);
					if(i == 0 && mLockToAppEnabled) {
						holder.lockToScreenView.setVisibility(View.VISIBLE);
						mLockTaskId = holder.taskDescription.persistentTaskId;
					}
				}
			}
		}
	}

    private void animateInIconOfFirstTask() {
        if (mItemToAnimateInWhenWindowAnimationIsFinished != null &&
                !mRecentTasksLoader.isFirstScreenful()) {
            int timeSinceWindowAnimation =
                    (int) (System.currentTimeMillis() - mWindowAnimationStartTime);
            final int minStartDelay = 150;
            final int startDelay = Math.max(0, Math.min(
                    minStartDelay - timeSinceWindowAnimation, minStartDelay));
            final int duration = 250;
            final ViewHolder holder = mItemToAnimateInWhenWindowAnimationIsFinished;
            final TimeInterpolator cubic = new DecelerateInterpolator(1.5f);
            FirstFrameAnimatorHelper.initializeDrawListener(holder.iconView);
            for (View v :
                new View[] { holder.iconView, holder.labelView, holder.calloutLine }) {
                if (v != null) {
                    ViewPropertyAnimator vpa = v.animate().translationX(0).translationY(0)
                            .alpha(1f).setStartDelay(startDelay)
                            .setDuration(duration).setInterpolator(cubic);
                    FirstFrameAnimatorHelper h = new FirstFrameAnimatorHelper(vpa, v);
                }
            }
            mItemToAnimateInWhenWindowAnimationIsFinished = null;
            mAnimateIconOfFirstTask = false;
        }
    }

    public void onWindowAnimationStart() {
        mWaitingForWindowAnimation = false;
        mWindowAnimationStartTime = System.currentTimeMillis();
        animateInIconOfFirstTask();
    }

    public void clearRecentTasksList() {
        // Clear memory used by screenshots
        if (mRecentTaskDescriptions != null) {
            mRecentTasksLoader.cancelLoadingThumbnailsAndIcons(this);
            onTaskLoadingCancelled();
        }
    }

    public void onTaskLoadingCancelled() {
        // Gets called by RecentTasksLoader when it's cancelled
        if (mRecentTaskDescriptions != null) {
            mRecentTaskDescriptions = null;
            mListAdapter.notifyDataSetInvalidated();
        }
    }

    public void refreshViews() {
        mListAdapter.notifyDataSetInvalidated();
        updateUiElements();
        showIfReady();
    }

    public void refreshRecentTasksList() {
        refreshRecentTasksList(null, false);
    }

    private void refreshRecentTasksList(
            ArrayList<TaskDescription> recentTasksList, boolean firstScreenful) {
    	Log.d(TAG, " mRecentTaskDescriptions = " + mRecentTaskDescriptions + " recentTasksList = " + recentTasksList);
        if (mRecentTaskDescriptions == null && recentTasksList != null) {
            onTasksLoaded(recentTasksList, firstScreenful);
        } else {
            mRecentTasksLoader.loadTasksInBackground();
        }
    }

    public void onTasksLoaded(ArrayList<TaskDescription> tasks, boolean firstScreenful) {
        if (mRecentTaskDescriptions == null) {
            mRecentTaskDescriptions = new ArrayList<TaskDescription>(tasks);
        } else {
            mRecentTaskDescriptions.addAll(tasks);
        }
        if (((RecentsActivity) getContext()).isActivityShowing()) {
            refreshViews();
        }
    }

    private void updateUiElements() {
        final int items = mRecentTaskDescriptions != null
                ? mRecentTaskDescriptions.size() : 0;

        ((View) mRecentsContainer).setVisibility(items > 0 ? View.VISIBLE : View.GONE);

        // Set description for accessibility
        int numRecentApps = mRecentTaskDescriptions != null
                ? mRecentTaskDescriptions.size() : 0;
        String recentAppsAccessibilityDescription;
        Log.d(TAG, "updateUiElements mRecentTaskDescriptions.size() = " + items);
        if (numRecentApps == 0) {
            recentAppsAccessibilityDescription =
                getResources().getString(R.string.status_bar_no_recent_apps);
        } else {
            recentAppsAccessibilityDescription = getResources().getQuantityString(
                R.plurals.status_bar_accessibility_recent_apps, numRecentApps, numRecentApps);
        }
        setContentDescription(recentAppsAccessibilityDescription);
    }

    public boolean simulateClick(int persistentTaskId) {
        View v = mRecentsContainer.findViewForTask(persistentTaskId);
        if (v != null) {
            handleOnClick(v);
            return true;
        }
        return false;
    }

    public void handleOnClick(View view) {
        ViewHolder holder = (ViewHolder) view.getTag();
        TaskDescription ad = holder.taskDescription;
        final Context context = view.getContext();
        final ActivityManager am = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);

        Bitmap bm = null;
        boolean usingDrawingCache = true;
        if (holder.thumbnailViewDrawable instanceof BitmapDrawable) {
            bm = ((BitmapDrawable) holder.thumbnailViewDrawable).getBitmap();
            if (bm.getWidth() == holder.thumbnailViewImage.getWidth() &&
                    bm.getHeight() == holder.thumbnailViewImage.getHeight()) {
                usingDrawingCache = false;
            }
        }
        if (usingDrawingCache) {
            holder.thumbnailViewImage.setDrawingCacheEnabled(true);
            bm = holder.thumbnailViewImage.getDrawingCache();
        }
        Bundle opts = (bm == null) ?
                null :
                ActivityOptions.makeThumbnailScaleUpAnimation(
                        holder.thumbnailViewImage, bm, 0, 0, null).toBundle();

        show(false);
        if (ad.taskId >= 0) {
            // This is an active task; it should just go to the foreground.
            /*am.moveTaskToFront(ad.taskId, ActivityManager.MOVE_TASK_WITH_HOME,
                    opts);*/
        	boolean successful;
        	try {
        		Method method = Class.forName("android.app.ActivityManager")
        				.getMethod("moveTaskToFrontWithResult", int.class, int.class, Bundle.class);
        		successful = (Boolean) method.invoke(am, ad.taskId, ActivityManager.MOVE_TASK_WITH_HOME, opts);
        		Log.d(TAG, "invoke moveTaskToFrontWithResult successful: " + successful);
        		if(!successful) {
        			startActivityFromRecent(ad, context, opts);
        		}
        	} catch (NoSuchMethodException e) {
        		am.moveTaskToFront(ad.taskId, ActivityManager.MOVE_TASK_WITH_HOME, opts);
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(TAG, "invoke moveTaskToFrontWithResult failed: " + e);
				// TODO: handle exception
			}
            Log.v(TAG, "Move Task To Front for " + ad.taskId);
        } else {
            startActivityFromRecent(ad, context, opts);
        }
        if (usingDrawingCache) {
            holder.thumbnailViewImage.setDrawingCacheEnabled(false);
        }
    }

	private void startActivityFromRecent(TaskDescription ad,
			final Context context, Bundle opts) {
		Intent intent = ad.intent;
		intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
		        | Intent.FLAG_ACTIVITY_TASK_ON_HOME
		        | Intent.FLAG_ACTIVITY_NEW_TASK);
		if (DEBUG) Log.v(TAG, "Starting activity " + intent);
		try {
		    context.startActivityAsUser(intent, opts,
		            new UserHandle(ad.userId));
		} catch (SecurityException e) {
		    Log.e(TAG, "Recents does not have the permission to launch " + intent, e);
		} catch (ActivityNotFoundException e) {
		    Log.e(TAG, "Error launching activity " + intent, e);
		}
	}

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        handleOnClick(view);
    }

    public void handleSwipe(View view) {
        final TaskDescription ad = ((ViewHolder) view.getTag()).taskDescription;
        if (ad == null) {
            Log.v(TAG, "Not able to find activity description for swiped task; view=" + view +
                    " tag=" + view.getTag());
            return;
        }
        if (DEBUG) Log.v(TAG, "Jettison " + ad.getLabel());
        
        // Gionee <huangwt> <20150209> modify for CR01447164 begin
        if (mRecentTaskDescriptions != null) {
            
            mRecentTaskDescriptions.remove(ad);
            
            if (mRecentTaskDescriptions.size() == 0) {
                dismissAndGoBack();
            }
        }
        
        if (mRecentTasksLoader != null) {
            mRecentTasksLoader.remove(ad);
        }
        // Gionee <huangwt> <20150209> modify for CR01447164 end

        // Currently, either direction means the same thing, so ignore direction and remove
        // the task.
        final ActivityManager am = (ActivityManager)
                getContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            am.removeTask(ad.persistentTaskId);
            am.forceStopPackage(ad.packageName);

            // Accessibility feedback
            setContentDescription(
                    getContext().getString(R.string.accessibility_recents_item_dismissed, ad.getLabel()));
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
            setContentDescription(null);
        }
    }
    
    public void clearTaskDescription() {
        mTaskDescriptionList.clear();
    }
    
    public void removeTaskDescription(View view) {
        final TaskDescription ad = ((ViewHolder) view.getTag()).taskDescription;
        if (ad == null) {
            Log.v(TAG, "Not able to find activity description for swiped task; view=" + view +
                    " tag=" + view.getTag());
            return;
        }
        
        if (DEBUG) Log.v(TAG, "Jettison " + ad.getLabel());
        
        mTaskDescriptionList.add(ad);
        
        // Gionee <huangwt> <20150209> modify for CR01447164 begin
        if (mRecentTaskDescriptions != null) {
            mRecentTaskDescriptions.remove(ad);
        }
        
        if (mRecentTasksLoader != null) {
            mRecentTasksLoader.remove(ad);
        }
        // Gionee <huangwt> <20150209> modify for CR01447164 end

    }
    
    public void removeTask() {
        // Currently, either direction means the same thing, so ignore direction and remove
        // the task.
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                final ActivityManager am = (ActivityManager)
                        getContext().getSystemService(Context.ACTIVITY_SERVICE);
                
                List<String> musicApps = ((RecentsActivity) getContext()).getMusicAppsList();
                try {
                    for (TaskDescription ad : mTaskDescriptionList) {
                        if (am != null) {
                        	if((musicApps != null && musicApps.contains(ad.packageName))) {
								if (DEBUG) Log.d(TAG, "ignore apps: " + ad.packageName);
                        		continue;
                        	}
                            am.removeTask(ad.persistentTaskId);
                            am.forceStopPackage(ad.packageName);
							if (DEBUG) Log.d(TAG, "Jettison app: " + ad.packageName);

                            // Accessibility feedback
                            setContentDescription(getContext().getString(
                                    R.string.accessibility_recents_item_dismissed, ad.getLabel()));
                            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
                            setContentDescription(null);
                        }
                    }
                    mHandler.sendEmptyMessage(MSG_STOP_SCAN_ANIM);
                } catch (ConcurrentModificationException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void startApplicationDetailsActivity(String packageName, int userId) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null));
        intent.setComponent(intent.resolveActivity(getContext().getPackageManager()));
        TaskStackBuilder.create(getContext())
                .addNextIntentWithParentStack(intent).startActivities(null, new UserHandle(userId));
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mPopup != null) {
            return true;
        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }

    public void handleLongPress(
            final View selectedView, final View anchorView, final View thumbnailView) {
        thumbnailView.setSelected(true);
        final PopupMenu popup =
            new PopupMenu(getContext(), anchorView == null ? selectedView : anchorView);
        mPopup = popup;
        popup.getMenuInflater().inflate(R.menu.recent_popup_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.recent_remove_item) {
                    ((ViewGroup) mRecentsContainer).removeViewInLayout(selectedView);
                } else if (item.getItemId() == R.id.recent_inspect_item) {
                    ViewHolder viewHolder = (ViewHolder) selectedView.getTag();
                    if (viewHolder != null) {
                        final TaskDescription ad = viewHolder.taskDescription;
                        startApplicationDetailsActivity(ad.packageName, ad.userId);
                        show(false);
                    } else {
                        throw new IllegalStateException("Oops, no tag on view " + selectedView);
                    }
                } else {
                    return false;
                }
                return true;
            }
        });
        popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
            public void onDismiss(PopupMenu menu) {
                thumbnailView.setSelected(false);
                mPopup = null;
            }
        });
        popup.show();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        int paddingLeft = getPaddingLeft();
        final boolean offsetRequired = isPaddingOffsetRequired();
        if (offsetRequired) {
            paddingLeft += getLeftPaddingOffset();
        }

        int left = getScrollX() + paddingLeft;
        int right = left + getRight() - getLeft() - getPaddingRight() - paddingLeft;
        int top = getScrollY() + getFadeTop(offsetRequired);
        int bottom = top + getFadeHeight(offsetRequired);

        if (offsetRequired) {
            right += getRightPaddingOffset();
            bottom += getBottomPaddingOffset();
        }
        mRecentsContainer.drawFadedEdges(canvas, left, right, top, bottom);
    }
    
    public void clearRecentApps() {
		if(mRecentsContainer != null) {
			mRecentsContainer.clearRecentApps();
		}
	}
    
    private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if(MSG_STOP_SCAN_ANIM == msg.what) {
				((RecentsActivity) getContext()).showMemorySaved();
				clearTaskDescription();
			}
		}
    };

	@Override
	public void handleLockToScreen(View selectedView) {
		// TODO Auto-generated method stub
		((RecentsActivity) getContext()).onScreenPinningRequest();
		simulateClick(mLockTaskId);
	}
	
	private ContentObserver mScreenLockObserver = new ContentObserver(new Handler()) {

		@Override
		public void onChange(boolean selfChange) {
			mLockToAppEnabled = Settings.System.getInt(getContext().getContentResolver(), Settings.System.LOCK_TO_APP_ENABLED, 0) != 0;
		}
	};
	
	public void unRegisterScreenLockObserver(Context context) {
		context.getContentResolver().unregisterContentObserver(mScreenLockObserver);
	}
}
