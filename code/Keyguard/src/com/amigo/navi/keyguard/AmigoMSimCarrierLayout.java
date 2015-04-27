/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 * Copyright (C) 2012 The Android Open Source Project
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

package com.amigo.navi.keyguard;

import android.content.Context;
import android.os.SystemProperties;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.telephony.IccCardConstants;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.R;

public class AmigoMSimCarrierLayout extends LinearLayout {

    private static final String LOG_TAG = "MSimCarrierLayout";

    private Context mContext;
    int mSlotCount = 0;
    PhoneStateListener[] mPhoneStateListener;
    ServiceState[] mServiceState;
    // int[] mDataState;
    int[] mDataNetType;
//    String[] mNetworkName;
    String[] mCarrierText;
    private IccCardConstants.State mSimState[];
    private TextView mCarrierView[];
    private TextView mCarrierDivider[];
    private StatusMode mStatusMode[];

    final TelephonyManager mPhone;
    private KeyguardUpdateMonitor mUpdateMonitor;
    private CustomMadeMode mCustomMadeMode = CustomMadeMode.NONE;

    private KeyguardUpdateMonitorCallback mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        
        @Override
        public void onRefreshCarrierInfo() {
            
        }
        @Override
        public void onSimStateChanged(int subId, int slotId, IccCardConstants.State simState) { 
            updateCarrierText(subId, mUpdateMonitor.getSimState(subId), "", "");
        }
//        @Override
//        public void onRefreshCarrierInfo(long subId, CharSequence plmn, CharSequence spn) {
//            Log.d(LOG_TAG, "onRefreshCarrierInfo state: " + mUpdateMonitor.getSimStateOfSub(subId));
//            updateCarrierText(subId, mUpdateMonitor.getSimStateOfSub(subId), plmn, spn);
//        }
//
//        @Override
//        public void onSimStateChangedUsingSubId(long subId, IccCardConstants.State simState) {
//            Log.d(LOG_TAG, "onSimStateChangedUsingSubId state: " + simState);
//            updateCarrierText(subId, simState, mUpdateMonitor.getTelephonyPlmn(subId),
//                    mUpdateMonitor.getTelephonySpn(subId));
//        }
    	// order for build error end
        @Override
        public void onScreenTurnedOff(int why) {
            // for (int i = 0; i < mNumOfSub; i++) {
            // mCarrierView[i].setSelected(false);
            // }
        };

        @Override
        public void onScreenTurnedOn() {
            // for (int i = 0; i < mNumOfSub; i++) {
            // mCarrierView[i].setSelected(true);
            // }
        };

        public void onAirPlaneModeChanged(boolean airPlaneModeEnabled) {

        };

    };

    public AmigoMSimCarrierLayout(Context context) {
        this(context, null);
    }

    public AmigoMSimCarrierLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mUpdateMonitor = KeyguardUpdateMonitor.getInstance(mContext);
        mPhone = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mSlotCount = mPhone.getPhoneCount();
        mPhoneStateListener = new PhoneStateListener[mSlotCount];
        mServiceState = new ServiceState[mSlotCount];
        mDataNetType = new int[mSlotCount];
        // mDataState = new int[mSlotCount];
//        mNetworkName = new String[mSlotCount];

        Log.d(LOG_TAG, "MSimCarrierLayout()");
        isActived();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initMembers();
    }

    private void initMembers() {
        mCarrierView = new TextView[4];
        mCarrierDivider = new TextView[3];
        mCarrierView[0] = (TextView) this.findViewById(R.id.carrier_text_1);
        Log.d(LOG_TAG, "initMembers  mCarrierView[0] is null? " + (mCarrierView[0] == null));
        mCarrierView[1] = (TextView) this.findViewById(R.id.carrier_text_2);
        mCarrierView[2] = (TextView) this.findViewById(R.id.carrier_text_3);
        mCarrierView[3] = (TextView) this.findViewById(R.id.carrier_text_4);
        mCarrierDivider[0] = (TextView) findViewById(R.id.carrier_divider_1);
        mCarrierDivider[1] = (TextView) findViewById(R.id.carrier_divider_2);
        mCarrierDivider[2] = (TextView) findViewById(R.id.carrier_divider_3);
        mCarrierView[0].setSelected(true);
        mCarrierView[1].setSelected(true);
        mCarrierView[2].setSelected(true);
        mCarrierView[3].setSelected(true);

        mSimState = new IccCardConstants.State[mSlotCount];
        mStatusMode = new StatusMode[mSlotCount];

        for (int i = 0; i < mSlotCount; i++) {
        	// order for build error begin
//            mSimState[i] = mUpdateMonitor.getSimStateOfSub(mUpdateMonitor.getSubIdUsingSubIndex(i));
        	// order for build error end
            mStatusMode[i] = getStatusForIccState(mSimState[i]);
         // order for build error begin
//            mDataNetType[i] = mPhone.getNetworkType(mUpdateMonitor.getSubIdUsingSubIndex(i));
         // order for build error end
        }
        mCarrierText = new String[4];
        for (int i = 0; i < 4; i++) {
            mCarrierText[i] = getResources().getString(R.string.no_sim_card);
        }
        refreshViews();

    }

    private void registerPhoneStateListener() {
        for (int i = 0; i < mSlotCount; i++) {
            Log.d(LOG_TAG, "registerPhoneStateListener slotId: " + i);
            // final long subId = getFirstSubInSlot(i);
         // order for build error begin
//            final long subId = mUpdateMonitor.getSubIdUsingSubIndex(i);
         // order for build error end
            final int subId = 0;
            
            if (subId >= 0) {
                mPhoneStateListener[i] = getPhoneStateListener(subId, i);
                mPhone.listen(mPhoneStateListener[i], PhoneStateListener.LISTEN_SERVICE_STATE
                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE | PhoneStateListener.LISTEN_DATA_ACTIVITY
                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
                Log.d(LOG_TAG, "Register PhoneStateListener");
            } else {
                mPhoneStateListener[i] = null;
            }
        }
    }

    private void unregisterPhoneStateListener() {
        for (int i = 0; i < mSlotCount; i++) {
            if (mPhoneStateListener[i] != null) {
                mPhone.listen(mPhoneStateListener[i], PhoneStateListener.LISTEN_NONE);
            }
        }
    }

    private PhoneStateListener getPhoneStateListener(final int subId, final int slotId) {
        return new PhoneStateListener(subId) {

            @Override
            public void onServiceStateChanged(ServiceState state) {
                if (isValidSlotId(slotId)) {
                    mServiceState[slotId] = state;
                    mDataNetType[slotId] = mPhone.getNetworkType(subId);
                    mCarrierText[slotId] = updateOperatorText(slotId, subId).toString();
                    Log.d(LOG_TAG, "onServiceStateChanged state: " + state.getRoaming() + "  networkType: "
                            + mDataNetType[slotId]);
                    refreshViews();
                }

            }

            @Override
            public void onDataConnectionStateChanged(int state, int networkType) {
                Log.d(LOG_TAG, "onDataConnectionStateChanged networkType: " + networkType + " slotId: " + slotId);
                if (isValidSlotId(slotId)) {
                    mDataNetType[slotId] = networkType;
                    mCarrierText[slotId] = updateOperatorText(slotId, subId).toString();
                    refreshViews();
                }
            }

            @Override
            public void onDataActivity(int direction) {
                super.onDataActivity(direction);
                if (isValidSlotId(slotId)) {
                    mDataNetType[slotId] = mPhone.getNetworkType(subId);
                    mCarrierText[slotId] = updateOperatorText(slotId, subId).toString();
                    refreshViews();
                    Log.d(LOG_TAG, "onDataActivity direction: " + direction + "  networkType: " + mDataNetType[slotId]
                            + " slotId: " + slotId);
                }

            }

            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                mDataNetType[slotId] = mPhone.getNetworkType(subId);
                mCarrierText[slotId] = updateOperatorText(slotId, subId).toString();
                Log.d(LOG_TAG, "onSignalStrengthsChanged slotId: " + slotId + "  networkType: " + mDataNetType[slotId]);
                refreshViews();
            }

        };
    }

    // / M: Support "Service Network Type on Statusbar". @{
    private final int getNWTypeByPriority(int cs, int ps) {
        // / By Network Class.
        if (TelephonyManager.getNetworkClass(cs) > TelephonyManager.getNetworkClass(ps)) {
            return cs;
        } else {
            return ps;
        }
    }

    // / M: Support "Service Network Type on Statusbar". @}

    protected void updateCarrierText(int subId, IccCardConstants.State simState, CharSequence plmn, CharSequence spn) {
        // TextView toSetCarrierView;

    	// order for build error begin
//        int slotId = mUpdateMonitor.getSlotIdUsingSubId(subId);
//        if (!mUpdateMonitor.isValidSlotId(slotId)) {
//            Log.d(LOG_TAG, "updateCarrierText, invalidate slotId=" + slotId);
//            return;
//        }
    	int slotId = 0;
    	// order for build error end
        Log.d(LOG_TAG, "updateCarrierText, simState=" + simState + " subId=" + subId + " slotId: " + slotId);

        mSimState[slotId] = simState;
        mDataNetType[slotId] = mPhone.getDataNetworkType(subId);
        CharSequence text = getCarrierTextForSimState(slotId, subId, simState);
        mCarrierText[slotId] = text.toString();
        Log.d(LOG_TAG, "slotId: " + slotId + "  carrierText: " + text);
        refreshViews();
    }

    /**
     * Top-level function for creating carrier text. Makes text based on
     * simState, PLMN and SPN as well as device capabilities, such as being
     * emergency call capable.
     * 
     * @return
     */
    private CharSequence getCarrierTextForSimState(int slotId, int subId, IccCardConstants.State simState) {
        CharSequence carrierText = "";
        StatusMode status = getStatusForIccState(simState);

        mStatusMode[slotId] = status;
        switch (status) {
        case Normal:
            if (mPhone.getNetworkType(subId) == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
                carrierText = getResources().getString(R.string.no_service);
                mStatusMode[slotId] = StatusMode.NetworkLocked;
            } else {
                carrierText = updateOperatorText(slotId, subId);
            }
            break;

        case SimNotReady:
        case NetworkLocked:
        case SimPermDisabled:
        case SimMissingLocked:
        case SimLocked:
        case SimPukLocked:
            carrierText = getResources().getString(R.string.no_service);
            break;
        case SimMissing:
        case SimUnknown:
            carrierText = getResources().getString(R.string.no_sim_card);
            break;
        default:
            carrierText = getResources().getString(R.string.no_sim_card);
            break;
        }
        Log.d(LOG_TAG, "getCarrierTextForSimState statusMode: " + status + "  carrierText: " + carrierText
                + "  slotId: " + slotId);
        return carrierText;
    }

    /**
     * Top-level function for creating carrier text. Makes text based on
     * simState, PLMN and SPN as well as device capabilities, such as being
     * emergency call capable.
     * 
     * @param simState
     * @param plmn
     * @param spn
     * @param hnbName
     * @param csgid
     * @return
     * @deprecated by jingyn
     */
/*    private CharSequence getCarrierTextForSimState(int slotId, long subId, IccCardConstants.State simState,
            CharSequence plmn, CharSequence spn, CharSequence hnbName, CharSequence csgId) {
        CharSequence carrierText = null;
        StatusMode status = getStatusForIccState(simState);
        switch (status) {
        case Normal:
            if (mPhone.getNetworkType(subId) == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
                concatenate(plmn, spn);
            } else {
                carrierText = updateOperatorText(slotId, subId);
            }
            break;

        case SimNotReady:
            carrierText = null; // nothing to display yet.
            break;

        case NetworkLocked:
            carrierText = makeCarrierStringOnEmergencyCapable(
                    mContext.getText(R.string.keyguard_network_locked_message), plmn);
            break;

        case SimMissing:
            // Shows "No SIM card | Emergency calls only" on devices that are
            // voice-capable.
            // This depends on mPlmn containing the text "Emergency calls only"
            // when the radio
            // has some connectivity. Otherwise, it should be null or empty and
            // just show
            // "No SIM card"
            carrierText = makeCarrierStringOnEmergencyCapable(
                    getContext().getText(R.string.keyguard_missing_sim_message_short), plmn);
            break;

        case SimPermDisabled:
            carrierText = getContext().getText(R.string.keyguard_permanent_disabled_sim_message_short);
            break;

        case SimMissingLocked:
            carrierText = makeCarrierStringOnEmergencyCapable(
                    getContext().getText(R.string.keyguard_missing_sim_message_short), plmn);
            break;

        case SimLocked:
            carrierText = makeCarrierStringOnEmergencyCapable(getContext()
                    .getText(R.string.keyguard_sim_locked_message), plmn);
            break;

        case SimPukLocked:
            carrierText = makeCarrierStringOnEmergencyCapable(
                    getContext().getText(R.string.keyguard_sim_puk_locked_message), plmn);
            break;
        default:
            break;
        }

        return carrierText;
    }*/

    /*
     * Add emergencyCallMessage to carrier string only if phone supports
     * emergency calls.
     */
   /* private CharSequence makeCarrierStringOnEmergencyCapable(CharSequence simMessage, CharSequence emergencyCallMessage) {
        if (mLockPatternUtils.isEmergencyCallCapable()) {
            return concatenate(simMessage, emergencyCallMessage);
        }
        return simMessage;
    }*/

    /*private static CharSequence concatenate(CharSequence str1, CharSequence str2) {
        final boolean str1Valid = !TextUtils.isEmpty(str1);
        final boolean str2Valid = !TextUtils.isEmpty(str2);
        if (str1Valid && str2Valid) {
            return new StringBuilder().append(str1).append("|").append(str2).toString();
        } else if (str1Valid) {
            return str1;
        } else if (str2Valid) {
            return str2;
        } else {
            return "";
        }
    }*/

    /**
     * Determine the current status of the lock screen given the SIM state and
     * other stuff.
     */
    private StatusMode getStatusForIccState(IccCardConstants.State simState) {
        // Since reading the SIM may take a while, we assume it is present until
        // told otherwise.
        if (simState == null) {
            return StatusMode.SimUnknown;
        }

        final boolean missingAndNotProvisioned = !KeyguardUpdateMonitor.getInstance(mContext).isDeviceProvisioned()
                && (simState == IccCardConstants.State.ABSENT || simState == IccCardConstants.State.PERM_DISABLED);

        // M: Directly maps missing and not Provisioned to SimMissingLocked
        // Status.
        if (missingAndNotProvisioned) {
            return StatusMode.SimMissingLocked;
        }
        // simState = missingAndNotProvisioned ?
        // IccCardConstants.State.NETWORK_LOCKED : simState;
        // @}

        switch (simState) {
        case ABSENT:
            return StatusMode.SimMissing;
        case NETWORK_LOCKED:
            // M: correct IccCard state NETWORK_LOCKED maps to NetowrkLocked.
            return StatusMode.NetworkLocked;
        case NOT_READY:
            return StatusMode.SimNotReady;
        case PIN_REQUIRED:
            return StatusMode.SimLocked;
        case PUK_REQUIRED:
            return StatusMode.SimPukLocked;
        case READY:
            return StatusMode.Normal;
        case PERM_DISABLED:
            return StatusMode.SimPermDisabled;
        case UNKNOWN:
            return StatusMode.SimUnknown;
        default:
            break;
        }
        return StatusMode.SimMissing;
    }

    private String updateOperatorText(int slotId, int subId) {
        String operatorName = "";
        if (mServiceState[slotId] != null) {
            mDataNetType[slotId] = getNWTypeByPriority(mServiceState[slotId].getVoiceNetworkType(),
                    mDataNetType[slotId]);
            Log.d(LOG_TAG, "getNWTypeByPriority networkTye: " + mDataNetType[slotId]);
        }
        IccCardConstants.State state = mSimState[slotId];
        if (state != IccCardConstants.State.ABSENT) {
            switch (mCustomMadeMode) {
            case CMCC:
                operatorName = getCmccOperatorName(slotId, subId);
                break;
            case CTCC:
                operatorName = getCtccOperatorName(slotId, subId);
                break;
            case CUCC:
                operatorName = getCuccOperatorName(slotId, subId);
                break;
            default:
                break;
            }

        } else {
            operatorName = getResources().getString(R.string.no_sim_card);
        }
        String operator = mPhone.getNetworkOperatorForSubscription(subId);
        Log.d(LOG_TAG, "updateDataNetType  operator: " + operator + "  operatorName: " + operatorName + " slotId: "
                + slotId);
        return operatorName;
    }

    private String getCmccOperatorName(int slotId, int subId) {
        String operateName = getResources().getString(R.string.cmcc);

        int networkType = mDataNetType[slotId];
        IccCardConstants.State state = mSimState[slotId];
        Log.d(LOG_TAG, "getCmccOperatorName  sim state: " + state + " networkType: " + networkType + "  slotId: "
                + slotId);
        mStatusMode[slotId] = StatusMode.Normal;
        if (networkType == TelephonyManager.NETWORK_TYPE_LTE) {
            operateName = getResources().getString(R.string.cmcc_4g);
        } else if (networkType == TelephonyManager.NETWORK_TYPE_CDMA) {
            operateName = getResources().getString(R.string.cmcc_3g);
        } else if (networkType == TelephonyManager.NETWORK_TYPE_GSM
                || networkType == TelephonyManager.NETWORK_TYPE_EDGE) {
            operateName = getResources().getString(R.string.cmcc);
        } else if (networkType == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            operateName = getResources().getString(R.string.no_service);
            mStatusMode[slotId] = StatusMode.NetworkLocked;
        }

        return operateName;
    }

    private String getCuccOperatorName(int slotId, int subId) {
        String operateName = getResources().getString(R.string.cucc);
        mStatusMode[slotId] = StatusMode.Normal;
        // IccCardConstants.State state = mSimState[slotId];
        // int networkType=mPhone.getNetworkType(slotId);
        int networkType = mDataNetType[slotId];
        if (networkType == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            operateName = getResources().getString(R.string.no_service);
            mStatusMode[slotId] = StatusMode.NetworkLocked;
        }
        return operateName;
    }

    private String getCtccOperatorName(int slotId, int subId) {
        String operateName = getResources().getString(R.string.ctcc);
        mStatusMode[slotId] = StatusMode.Normal;
        // int networkType=mPhone.getNetworkType(slotId);
        int networkType = mDataNetType[slotId];
        if (networkType == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            operateName = getResources().getString(R.string.no_service);
            mStatusMode[slotId] = StatusMode.NetworkLocked;
        }
        return operateName;
    }

    private void refreshViews() {
        post(new Runnable() {
            @Override
            public void run() {
                if (mCustomMadeMode == CustomMadeMode.NONE) {
                    return;
                }
                if (mSlotCount > 1) {
                    refreshMutiCardViews();
                } else {
                    refreshSingleCardInfoViews();
                }
            }
        });
    }

    private void refreshSingleCardInfoViews() {
        mCarrierView[0].setVisibility(View.VISIBLE);
        mCarrierView[0].setText(mCarrierText[0]);
    }

    private void refreshMutiCardViews() {
        boolean isAllSimCardDismiss = true;
        boolean isAllSimCardNoService = true;
        for (int i = 0; i < mSlotCount; i++) {
            mCarrierView[i].setText(mCarrierText[i]);
            boolean isSimExit = mStatusMode[i] == StatusMode.Normal && mSimState[i] == IccCardConstants.State.READY;
            Log.d(LOG_TAG, "refreshMutiCardViews slotId: " + i + "  carrierText: " + mCarrierText[i] + "  isSimExit: "
                    + isSimExit);
            if (isSimExit) {
                mCarrierView[i].setVisibility(View.VISIBLE);
                if (i > 0) {
                    if (mCarrierView[i - 1].getVisibility() == View.VISIBLE) {
                        mCarrierDivider[i - 1].setVisibility(View.VISIBLE);
                    } else {
                        mCarrierDivider[i - 1].setVisibility(View.GONE);
                    }
                }
                isAllSimCardDismiss = false;
            } else {
                mCarrierView[i].setVisibility(View.GONE);
                if (i > 0) {
                    mCarrierDivider[i - 1].setVisibility(View.GONE);
                }
            }
        }

        int noServiceIndex = 0;
        String noSimCard = getResources().getString(R.string.no_sim_card);
        String noService = getResources().getString(R.string.no_service);
        for (int i = 0; i < mSlotCount; i++) {
            String carrierText = mCarrierText[i];
            if (!noSimCard.equals(carrierText)) {
                isAllSimCardDismiss = false;
            }
            if (!noService.equals(carrierText) && !noSimCard.equals(carrierText)) {
                isAllSimCardNoService = false;
            } else if (noService.equals(carrierText)) {
                noServiceIndex = i;
            }
        }
        if (isAllSimCardDismiss) {
            mCarrierView[0].setVisibility(View.VISIBLE);
            return;
        }

        if (isAllSimCardNoService) {
            mCarrierView[noServiceIndex].setVisibility(View.VISIBLE);
        }

    }

    public static int getFirstSubInSlot(int slotId) {
        int[] subIds = SubscriptionManager.getSubId(slotId);
        if (subIds != null && subIds.length > 0) {
            return subIds[0];
        }
        Log.d(LOG_TAG, "Cannot get first sub in slot: " + slotId);
        return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    private boolean isValidSlotId(int slotId) {
        return (0 <= slotId) && (slotId < mSlotCount);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mCustomMadeMode != CustomMadeMode.NONE) {
            registerPhoneStateListener();
            KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mUpdateMonitorCallback);
        }

        Log.d(LOG_TAG, "onAttachedToWindow()");
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mCustomMadeMode != CustomMadeMode.NONE) {
            KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mUpdateMonitorCallback);
            unregisterPhoneStateListener();
        }
    }

    /**
     * The status of this lock screen. Primarily used for widgets on LockScreen.
     */
    private static enum StatusMode {
        Normal, // Normal case (sim card present, it's not locked)
        NetworkLocked, // SIM card is 'network locked'.
        SimMissing, // SIM card is missing.
        SimMissingLocked, // SIM card is missing, and device isn't provisioned;
                          // don't allow access
        SimPukLocked, // SIM card is PUK locked because SIM entered wrong too
                      // many times
        SimLocked, // SIM card is currently locked
        SimPermDisabled, // SIM card is permanently disabled due to PUK unlock
                         // failure
        SimNotReady, // SIM is not ready yet. May never be on devices w/o a SIM.

        // / M: mediatek add sim state
        SimUnknown, NetworkSearching; // The sim card is ready, but searching
                                      // network
    }

    private enum CustomMadeMode {
        NONE, CUCC, //
        CMCC, CTCC
    }

    private void isActived() {
        String customMadeRom=SystemProperties.get("ro.gn.custom.operators");
        boolean isCmccRom = "cmcc".equals(customMadeRom); 
        boolean isCuccRom = "cucc".equals(customMadeRom); 
        boolean isCtccRom = "ctcc".equals(customMadeRom);
        mCustomMadeMode = CustomMadeMode.CUCC;
        if (isCmccRom) {
            mCustomMadeMode = CustomMadeMode.CMCC;
        } else if (isCuccRom) {
            mCustomMadeMode = CustomMadeMode.CUCC;
        } else if (isCtccRom) {
            mCustomMadeMode = CustomMadeMode.CTCC;
        }
        Log.d(LOG_TAG, "onAttachedToWindow,cm:" + isCmccRom + "cu:" + isCuccRom + "ct:" + isCtccRom);
    }

    /**
     * M: Used to check weather this device is wifi only.
     */
    /*private boolean isWifiOnlyDevice() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return !(cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE));
    }*/

}
