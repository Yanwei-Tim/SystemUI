/*******************************************************************************
 * Filename:
 * ---------
 *  EmotionDownLoadMgr.java
 *
 * Project:
 * --------
 *   Emotion
 *
 * Description:
 * ------------
 *   心情壁纸下载管理类
 *
 * Author:
 * -------
 * pengwei@gionee.com
 *
 * Date:
 * 2014.03.05
 ****************************************************************************/
//Gionee <pengwei><2014-03-05> modify for CR01095632 begin
package com.amigo.navi.keyguard.network.theardpool;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoadImagePool {

    private static String LOG_TAG = "GNDownLoadMgr";
    private static LoadImagePool mDownLoadMgr = null;
    private Context mCxt;
    private Object objSync = new Object();

    public static ExecutorService sThreadPool;
    private ArrayList<LoadImageThread> mThreadList = new ArrayList<LoadImageThread>();


    LoadImagePool(Context cxt) {
        mCxt = cxt;
    }

    static {
        sThreadPool = Executors.newCachedThreadPool();
    }

    private static void gnDownLoadExecute(Runnable runnable) {
        sThreadPool.execute(runnable);
    }

    public static synchronized LoadImagePool getInstance(Context cxt) {
        if (null == mDownLoadMgr) {
            mDownLoadMgr = new LoadImagePool(cxt);
        }
        return mDownLoadMgr;
    }

    public void loadImage(LoadImageThread runnable,String url) {
            synchronized (objSync) {
                if (containInThreadPool(url)) {
                    return;
                }
                mThreadList.add(runnable);
            }
            gnDownLoadExecute(runnable);
    }
    
    private boolean containInThreadPool(String url) {
        boolean ret = false;
        LoadImageThread th = null;
        for (int i = 0; i < mThreadList.size(); i++) {
            th = mThreadList.get(i);
            if (null == th) {
                continue;
            }
            ret = th.compareThread(url);
        }
        return ret;
    }

    private URL constructRequestURL(String reqUrl, String queryStr) {
        // URL conUrl = null;
        // StringBuilder url = new StringBuilder();
        // url.append(reqUrl);
        // try {
        // Iterator iter = null;
        // if (null != reqParam) {
        // iter = reqParam.entrySet().iterator();
        // }
        // while (null != iter && iter.hasNext()) {
        // HashMap.Entry entry = (HashMap.Entry) iter.next();
        // String key = (String) entry.getKey();
        // String val = (String) entry.getValue();
        // url.append(key).append("=").append(val).append(CONN_SYMBOL);
        // }
        // if (null != reqParam) {
        // url.deleteCharAt(url.length() - 1);
        // }
        // conUrl = new URL(url.toString());
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        URL conUrl = null;
        try {
            if (queryStr == null) {
                conUrl = new URL(reqUrl);
            } else {
                conUrl = new URL((reqUrl + queryStr));
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return conUrl;
    }

    public ArrayList<LoadImageThread> getDownLoadThreadList() {
        return mThreadList;
    }
    
}
// Gionee <pengwei><2014-03-05> modify for CR01095632 end
