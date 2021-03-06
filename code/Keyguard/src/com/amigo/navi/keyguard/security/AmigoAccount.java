package com.amigo.navi.keyguard.security;

import android.content.Context;
import android.os.UserHandle;
import android.util.Log;

import com.gionee.account.sdk.GioneeAccount;
import com.gionee.account.sdk.vo.LoginInfo;
import android.os.ServiceManager;
import android.os.UserHandle;

import com.amigo.navi.keyguard.DebugLog;
import com.android.internal.widget.ILockSettings;

public class AmigoAccount {
	
	private final static String TAG="AmigoAccount";
	private GioneeAccount gioneeAccount;
	private Context mContext;
	private String userName;
	private String uid;
	private ILockSettings mGnLockSettingsService;
	public static final String BING_AMIGO_ACCOUNT = "bind_amigo_account";
	private static AmigoAccount instance;
	
	

	private  AmigoAccount() {
		
		instance=this;
		getGnLockSettings();
	}

	


	public static AmigoAccount getInstance() {
		if(instance==null){
			instance=new AmigoAccount();
		}
		return instance;
	}






	public  LoginInfo getAccountNameAndId() {
		LoginInfo loginInfo=null;
		try {
			String userNameID=null;
			if(mGnLockSettingsService!=null){
			 userNameID = mGnLockSettingsService.getString(BING_AMIGO_ACCOUNT,null,UserHandle.USER_OWNER);
			}
			if(DebugLog.DEBUG){
		          DebugLog.i(TAG, "ForgetPasswordButton..userNameID="+userNameID);
		    }
			if (userNameID == null) {
				loginInfo = null;
			} else {
				String[] userNameAndID = userNameID.split(":");
				userName = userNameAndID[0];
				uid = userNameAndID[1];
				loginInfo = new LoginInfo();
				loginInfo.setName(userName);
				loginInfo.setUid(uid);
				if(DebugLog.DEBUG){
			          DebugLog.i(TAG, "ForgetPasswordButton.....userName="+userName+"uid="+uid);
			    }
			}
		} catch (Exception e) {
			loginInfo=null;
			if(DebugLog.DEBUG){
		          DebugLog.e(TAG, "ForgetPasswordButton..Exception.="+e.getMessage());
		    }
		}
		
		return loginInfo;
		
	}
	
	
	
	   private ILockSettings getGnLockSettings() {
			if(DebugLog.DEBUG){
		          DebugLog.v(TAG, "getGnLockSettings");
		    }
	        if (mGnLockSettingsService == null) {
	            mGnLockSettingsService = ILockSettings.Stub.asInterface(ServiceManager.getService("lock_settings")); 
	        }
	        return mGnLockSettingsService;
	    }
	
}
