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

package com.android.keyguard;

/**
 * The callback used by the keyguard view to tell the {@link KeyguardViewMediator}
 * various things.
 */
public interface ViewMediatorCallback {
    /**
     * Reports user activity and requests that the screen stay on.
     */
    void userActivity();

    /**
     * Report that the keyguard is done.
     * @param authenticated Whether the user securely got past the keyguard.
     *   the only reason for this to be false is if the keyguard was instructed
     *   to appear temporarily to verify the user is supposed to get past the
     *   keyguard, and the user fails to do so.
     */
    void keyguardDone(boolean authenticated);

    /**
     * Report that the keyguard is done drawing.
     */
    void keyguardDoneDrawing();

    /**
     * Tell ViewMediator that the current view needs IME input
     * @param needsInput
     */
    void setNeedsInput(boolean needsInput);

    /**
     * Tell view mediator that the keyguard view's desired user activity timeout
     * has changed and needs to be reapplied to the window.
     */
    void onUserActivityTimeoutChanged();

    /**
     * Report that the keyguard is dismissable, pending the next keyguardDone call.
     */
    void keyguardDonePending();

    /**
     * Report when keyguard is actually gone
     */
    void keyguardGone();

    /**
     * Report when the UI is ready for dismissing the whole Keyguard.
     */
    void readyForKeyguardDone();

    /**
     * Play the "device trusted" sound.
     */
    void playTrustedSound();

    /**
     * @return true if and only if Keyguard is showing or if Keyguard is disabled by an external app
     *         (legacy API)
     */
    boolean isInputRestricted();
    
    //jingyn add begin
    /**
     * Return is keyguard showing or not.
     * @return showing or not
     */
    boolean isShowing() ;
    //jingyn add end
    //jingyn add begin
    /**
     * Return is keyguard showing and  not occluded
     * @return showing or not
     */
    boolean isShowingAndNotOccluded() ;
    //jingyn add end
    //jingyn add begin
    /**
     * Return is keyguard showing and  not occluded
     * @return showing or not
     */
    void adjustStatusBarLocked() ;
    //jingyn add end
    
    //<Amigo_Keyguard> jingyn <2015-05-18> add for hao kan begin 
    /**
     * 
     */
    void setKeyguardWallpaperShow(boolean isShow) ;
    //<Amigo_Keyguard> jingyn <2015-05-18> add for hao kan end 
    
    //<Amigo_Keyguard> jingyn <2015-05-23> add for hao kan begin 
    /**
     * 
     */
    boolean isScreenOn() ;
    //<Amigo_Keyguard> jingyn <2015-05-23> add for hao kan end 
    
  //<Amigo_Keyguard> jiating <2015-05-27> add for other APP use begin
    void unlockKeyguardByOtherApp();			
	void lockKeyguardByOtherApp();
	
	  //<Amigo_Keyguard> jiating <2015-05-27> add for other APP use begin
}