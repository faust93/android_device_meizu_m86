/*
 * Copyright (C) 2015 The Dokdo Project
 * Copyright (C) 2016 faust93 at monumentum@gmail.com
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

package com.android.internal.telephony;

import static com.android.internal.telephony.RILConstants.*;

import android.os.AsyncResult;

import android.media.AudioManager;

import android.content.Context;
import android.os.Message;
import android.os.Parcel;
import android.telephony.Rlog;

import android.telephony.SignalStrength;
import android.telephony.PhoneNumberUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccUtils;

/**
 * RIL customization for Meizu PRO5.
 * {@hide}
 */
public class m86RIL extends RIL implements CommandsInterface {

    private static final int RIL_UNSOL_WB_AMR_REPORT_IND = 6004;
    private static final int RIL_REQUEST_SET_WBAMR_CAPABILITY = 5018;
    private static final int RIL_REQUEST_GET_WBAMR_CAPABILITY = 5019;

    private AudioManager mAudioManager;

    private int isWbAmrEnabled = 1;

    private boolean DBG = true;

    public m86RIL(Context context, int preferredNetworkType,
            int cdmaSubscription, Integer instanceId) {
        super(context, preferredNetworkType, cdmaSubscription, instanceId);
        mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        mQANElements = 4;
    }

    public m86RIL(Context context, int networkMode,
            int cdmaSubscription) {
        super(context, networkMode, cdmaSubscription);
        mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        mQANElements = 4;
    }

@Override
    protected Object
    responseIccCardStatus(Parcel p) {
        IccCardApplicationStatus appStatus;

        IccCardStatus cardStatus = new IccCardStatus();
        cardStatus.setCardState(p.readInt());
        cardStatus.setUniversalPinState(p.readInt());
        cardStatus.mGsmUmtsSubscriptionAppIndex = p.readInt();
        cardStatus.mCdmaSubscriptionAppIndex = p.readInt();
        cardStatus.mImsSubscriptionAppIndex = p.readInt();

        int numApplications = p.readInt();

        // limit to maximum allowed applications
        if (numApplications > IccCardStatus.CARD_MAX_APPS) {
            numApplications = IccCardStatus.CARD_MAX_APPS;
        }
        cardStatus.mApplications = new IccCardApplicationStatus[numApplications];

        appStatus = new IccCardApplicationStatus();
        for (int i = 0 ; i < numApplications ; i++) {
            if (i!=0) {
                appStatus = new IccCardApplicationStatus();
            }
            appStatus.app_type       = appStatus.AppTypeFromRILInt(p.readInt());
            appStatus.app_state      = appStatus.AppStateFromRILInt(p.readInt());
            appStatus.perso_substate = appStatus.PersoSubstateFromRILInt(p.readInt());
            appStatus.aid            = p.readString();
            appStatus.app_label      = p.readString();
            appStatus.pin1_replaced  = p.readInt();
            appStatus.pin1           = appStatus.PinStateFromRILInt(p.readInt());
            appStatus.pin2           = appStatus.PinStateFromRILInt(p.readInt());
            p.readInt(); // pin1_num_retries
            p.readInt(); // puk1_num_retries
            p.readInt(); // pin2_num_retries
            p.readInt(); // puk2_num_retries
            p.readInt(); // perso_unblock_retries
            cardStatus.mApplications[i] = appStatus;
        }
        return cardStatus;
}
    protected Object
    responseCallList(Parcel p) {
        int num;
        int voiceSettings;
        ArrayList<DriverCall> response;
        DriverCall dc;

        num = p.readInt();
        response = new ArrayList<DriverCall>(num);

        if (RILJ_LOGV) {
            riljLog("responseCallList: num=" + num +
                    " mEmergencyCallbackModeRegistrant=" + mEmergencyCallbackModeRegistrant +
                    " mTestingEmergencyCall=" + mTestingEmergencyCall.get());
        }
        for (int i = 0 ; i < num ; i++) {
            dc = new DriverCall();

            dc.state = DriverCall.stateFromCLCC(p.readInt());
            dc.index = p.readInt() & 0xff;
            dc.TOA = p.readInt();
            dc.isMpty = (0 != p.readInt());
            dc.isMT = (0 != p.readInt());
            dc.als = p.readInt();
            voiceSettings = p.readInt();
            dc.isVoice = (0 == voiceSettings) ? false : true;
            dc.isVoicePrivacy = (0 != p.readInt());
            dc.number = p.readString();
            int np = p.readInt();
            dc.numberPresentation = DriverCall.presentationFromCLIP(np);
            dc.name = p.readString();
            dc.namePresentation = DriverCall.presentationFromCLIP(p.readInt());
            int uusInfoPresent = p.readInt();
            if (uusInfoPresent == 1) {
                dc.uusInfo = new UUSInfo();
                dc.uusInfo.setType(p.readInt());
                dc.uusInfo.setDcs(p.readInt());
                byte[] userData = p.createByteArray();
                dc.uusInfo.setUserData(userData);
                riljLogv(String.format("Incoming UUS : type=%d, dcs=%d, length=%d",
                                dc.uusInfo.getType(), dc.uusInfo.getDcs(),
                                dc.uusInfo.getUserData().length));
                riljLogv("Incoming UUS : data (string)="
                        + new String(dc.uusInfo.getUserData()));
                riljLogv("Incoming UUS : data (hex): "
                        + IccUtils.bytesToHexString(dc.uusInfo.getUserData()));
            } else {
                riljLogv("Incoming UUS : NOT present!");
            }

            // Make sure there's a leading + on addresses with a TOA of 145
            dc.number = PhoneNumberUtils.stringFromStringAndTOA(dc.number, dc.TOA);

            response.add(dc);

            if (dc.isVoicePrivacy) {
                mVoicePrivacyOnRegistrants.notifyRegistrants();
                riljLog("InCall VoicePrivacy is enabled");
            } else {
                mVoicePrivacyOffRegistrants.notifyRegistrants();
                riljLog("InCall VoicePrivacy is disabled");
            }
        }

        Collections.sort(response);

        if ((num == 0) && mTestingEmergencyCall.getAndSet(false)) {
            if (mEmergencyCallbackModeRegistrant != null) {
                riljLog("responseCallList: call ended, testing emergency call," +
                            " notify ECM Registrants");
                mEmergencyCallbackModeRegistrant.notifyRegistrant();
            }
        }

        return response;
    }

    @Override
    protected Object responseSignalStrength(Parcel p) {
        int numInts = 12;
        int response[];

        // Get raw data
        response = new int[numInts];
        for (int i = 0; i < numInts; i++) {
            response[i] = p.readInt();
        }
        //gsm
        response[0] &= 0xff;
        //cdma
        response[2] %= 256;
        response[4] %= 256;
        response[7] &= 0xff;

        return new SignalStrength(response[0], response[1], response[2], response[3], response[4], response[5], response[6], response[7], response[8], response[9], response[10], response[11], true);
    }

    @Override
    protected RILRequest
    processSolicited (Parcel p, int type) {
        int serial, error;
        boolean found = false;
        int dataPosition = p.dataPosition(); // save off position within the Parcel
        serial = p.readInt();
        error = p.readInt();
        RILRequest rr = null;

        /* Pre-process the reply before popping it */
        synchronized (mRequestList) {
                RILRequest tr = mRequestList.get(serial);
                if (tr != null && tr.mSerial == serial) {
                    if (error == 0 || p.dataAvail() > 0) {
                        try {switch (tr.mRequest) {
                            /* Get those we're interested in */
                            case RIL_REQUEST_DATA_REGISTRATION_STATE:
                                rr = tr;
                                break;
                            /* Get those we're interested in */
                            case RIL_REQUEST_GET_WBAMR_CAPABILITY:
                                rr = tr;
                                break;
                            case RIL_REQUEST_SET_WBAMR_CAPABILITY:
                                rr = tr;
                                break;
                        }} catch (Throwable thr) {
                            // Exceptions here usually mean invalid RIL responses
                            if (tr.mResult != null) {
                                AsyncResult.forMessage(tr.mResult, null, thr);
                                tr.mResult.sendToTarget();
                            }
                            return tr;
                        }
                    }
                }
        }

        if (rr == null) {
            /* Nothing we care about, go up */
            p.setDataPosition(dataPosition);

            // Forward responses that we are not overriding to the super class
            return super.processSolicited(p, type);
        }


        rr = findAndRemoveRequestFromList(serial);

        if (rr == null) {
            return rr;
        }

        Object ret = null;

        if (error == 0 || p.dataAvail() > 0) {
            switch (rr.mRequest) {
                case RIL_REQUEST_DATA_REGISTRATION_STATE: ret =  dataRegState(p); break;
                case RIL_REQUEST_GET_WBAMR_CAPABILITY: ret = getWbAmrSupport(p); isWbAmrEnabled = (int)ret; break;
                case RIL_REQUEST_SET_WBAMR_CAPABILITY: ret = setWbAmr(isWbAmrEnabled); break;
                default:
                    throw new RuntimeException("Unrecognized solicited response: " + rr.mRequest);
            }
            //break;
        }

        if (RILJ_LOGD) riljLog(rr.serialString() + "< " + requestToString(rr.mRequest)
            + " " + retToString(rr.mRequest, ret));

        if (rr.mResult != null) {
            AsyncResult.forMessage(rr.mResult, ret, null);
            rr.mResult.sendToTarget();
        }
        return rr;
    }

    private Object
    dataRegState(Parcel p) {
        int num;
        String response[];

        response = p.readStringArray();

        /* DANGER WILL ROBINSON
         * In some cases from Vodaphone we are receiving a RAT of 102
         * while in tunnels of the metro.  Lets Assume that if we
         * receive 102 we actually want a RAT of 2 for EDGE service */
        if (response.length > 4 &&
            response[0].equals("1") &&
            response[3].equals("102")) {

            response[3] = "2";
        }
        return response;
    }

    private Object
    getWbAmrSupport(Parcel p) {
        int enabled = p.readInt();

        if(enabled == 1) 
            setWbAmrCapability(true);

        if(DBG) Rlog.d(RILJ_LOG_TAG,"WB AMR:" + enabled);

        return enabled;
    }


    @Override
    protected void
    processUnsolicited (Parcel p, int type) {
        Object ret;
        int dataPosition = p.dataPosition(); // save off position within the Parcel
        int response = p.readInt();

        switch(response) {
            case RIL_UNSOL_WB_AMR_REPORT_IND:
                getWbAmrCapability();
                break;

            default:
                // Rewind the Parcel
                p.setDataPosition(dataPosition);
                if(DBG) Rlog.d("SHRILGET", "UNKNOWN UNSL: " + response);

                // Forward responses that we are not overriding to the super class
                super.processUnsolicited(p, type);
                return;
        }
    }

    public void setWbAmrCapability(boolean enable) {
        int i = 1;
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_WBAMR_CAPABILITY, null);
        rr.mParcel.writeInt(1);
        Parcel parcel = rr.mParcel;
        if (!enable) {
            i = 0;
        }
        parcel.writeInt(i);
        send(rr);
    }

    public void getWbAmrCapability() {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_WBAMR_CAPABILITY, null);
        send(rr);
    }

    private Object setWbAmr(int state) {
        if (state == 1) {
            Rlog.d(RILJ_LOG_TAG, "setWbAmr(): setting audio parameter - wb_amr=on");
            mAudioManager.setParameters("voice_call_wb=on");
        }else if (state == 0) {
            Rlog.d(RILJ_LOG_TAG, "setWbAmr(): setting audio parameter - wb_amr=off");
            mAudioManager.setParameters("voice_call_wb=off");
        }
        return state;
    }

}
