package com.android.systemui.gionee.cc.fakecall;
/*
*
* MODULE DESCRIPTION
* add by huangwt for Android L at 20141210.
* 
*/
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class GnStartTaskReciver extends BroadcastReceiver {
    
    private static final String LOG_TAG = "StartTaskReciver";

    @Override
    public void onReceive(Context context, Intent intent) {

        boolean ringFakeCall = intent.getBooleanExtra(GnConstants.START_TASK_OPTR, false);
        
        Log.d(LOG_TAG, "ring the phone: " + ringFakeCall);
        
        GnFakeCall virtualPhone = new GnFakeCall(context);
        GnFakeCallHelper gnFakeCallHelper = GnFakeCallHelper.getInstance();
        if (ringFakeCall) {
            gnFakeCallHelper.setIsTimerRunning(true);
            virtualPhone.ringPhone();
        } else {
            gnFakeCallHelper.setIsTimerRunning(false);
            virtualPhone.canclePhone();
        }
    }

}
