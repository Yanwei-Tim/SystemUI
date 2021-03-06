package com.amigo.navi.keyguard.network.connect;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpStatus;

import com.amigo.navi.keyguard.DebugLog;
import com.amigo.navi.keyguard.KWDataCache;
import com.amigo.navi.keyguard.haokan.BitmapUtil;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.util.Log;

public class BitmapHttpConnect {
    private static final String TAG = "HttpConnect";
    private String mRequestType = null;
    private int mTimeOut = 0;
    Context mContext;
    
    private int mScreenWidth;
    private int mScreenHeight;
    
    
    public BitmapHttpConnect(Context context, int timeOut,String method){
        mTimeOut = timeOut;
        mRequestType = method;
        
        mScreenWidth = KWDataCache.getScreenWidth(context.getResources());
        mScreenHeight = KWDataCache.getScreenHeight(context.getResources());
    }
    
    public Bitmap loadImageFromInternet(URL conUrl) {
        InputStream inputStream = null;
        Bitmap bitmap = null;
        try {
            int reqCode = ConnectionStatus.NETWORK_EXCEPTION;
            HttpURLConnection urlConn = (HttpURLConnection) conUrl
                    .openConnection();
            urlConn.setRequestMethod(mRequestType);
            urlConn.setConnectTimeout(mTimeOut);
            urlConn.setReadTimeout(mTimeOut);
            reqCode = urlConn.getResponseCode();
            byte[] result = null;
            DebugLog.d(TAG,"loadImageFromInternet conUrl:" + conUrl.toString());
            DebugLog.d(TAG,"loadImageFromInternet reqCode:" + reqCode);
            if (reqCode == HttpStatus.SC_OK) {
                int contentLength = urlConn.getContentLength();
                inputStream = urlConn.getInputStream();
                result = readInputStream(inputStream);
                if(result != null && result.length == contentLength){
                	DebugLog.d(TAG,"loadImageFromInternet success");
//                	bitmap = createBitmap(bitmap, result);
                	bitmap = BitmapUtil.resizedBitmap(result, mScreenWidth, mScreenHeight);
                }
            }
        } catch (Exception e) {
            DebugLog.d(TAG,"loadImageFromInternet e:" + e);
        } finally {
            closeStream(inputStream);
        }
        return bitmap;
    }

    public byte[] loadImageFromInternetByByte(URL conUrl) {
        InputStream inputStream = null;
        byte[] result = null;
        try {
            int reqCode = ConnectionStatus.NETWORK_EXCEPTION;
            HttpURLConnection urlConn = (HttpURLConnection) conUrl
                    .openConnection();
            urlConn.setRequestMethod(mRequestType);
            urlConn.setConnectTimeout(mTimeOut);
            urlConn.setReadTimeout(mTimeOut);
            reqCode = urlConn.getResponseCode();
            DebugLog.d(TAG,"loadImageFromInternetByByte conUrl:" + conUrl.toString());
            DebugLog.d(TAG,"loadImageFromInternetByByte reqCode:" + reqCode);
            if (reqCode == HttpStatus.SC_OK) {
                int contentLength = urlConn.getContentLength();
                inputStream = urlConn.getInputStream();
                byte[] resultTemp = readInputStream(inputStream);
                if(resultTemp != null && resultTemp.length == contentLength){
                	result=resultTemp;
                }
            }
        } catch (Exception e) {
            DebugLog.d(TAG,"loadImageFromInternet e:" + e);
        } finally {
            closeStream(inputStream);
        }
        return result;
    }
    
    private void closeStream(InputStream inputStream) {
        if (null != inputStream) {
            try {
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param bitmap
     * @param result
     * @return
     */
    private Bitmap createBitmap(Bitmap bitmap, byte[] result) {
        if(result != null){
            int len = result.length;  
            BitmapFactory.Options options = new BitmapFactory.Options();  
            options.inPreferredConfig = Config.ARGB_8888;  
            options.inJustDecodeBounds = false;  
            try {
                bitmap = BitmapFactory.decodeByteArray(result, 0, len);  
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "", e);
            }
        }
        return bitmap;
    }

 
    private byte[] readInputStream(InputStream in) throws Exception {  
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();  
        byte[] buffer = new byte[1024];  
        int len = -1;  
        while ((len = in.read(buffer)) != -1  && !NetWorkUtils.needInterruptDownloadOrNot()) {  
            baos.write(buffer, 0, len);  
        }  
        
        baos.close();  
        in.close();  
        return baos.toByteArray();
    }  
}
