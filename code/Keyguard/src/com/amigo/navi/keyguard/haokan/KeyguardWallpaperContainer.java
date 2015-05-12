
package com.amigo.navi.keyguard.haokan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff.Mode;
import android.graphics.RadialGradient;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import com.android.keyguard.R;

public class KeyguardWallpaperContainer extends FrameLayout {

    private int mTop;
    
    private LinearGradient mLinearGradient;
    private Paint mPaint = new Paint();
    
    private int mScreenHeight = 2560;
    private int mScreenWidth = 1440;  
    int heightOffset = 400; //100dp
    private float mRadius;
    
    float cx,cy;
    
    Bitmap mBitmap = null;
    private float mBlind=1;
    private boolean mIsSecure;
    
    public KeyguardWallpaperContainer(Context context) {
        this(context,null);
    }

    public KeyguardWallpaperContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardWallpaperContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public KeyguardWallpaperContainer(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mScreenHeight = Common.getScreenHeight(getContext().getApplicationContext());
        mScreenWidth =  Common.getScreenWidth(getContext().getApplicationContext());
        mTop = mScreenHeight;
        
        cx = mScreenWidth / 2.0f;
        cy = mScreenHeight / 2.0f;
        heightOffset = getResources().getDimensionPixelSize(R.dimen.haokan_wallpaper_alpha_offset);
        
        mRadius = heightOffset + mScreenHeight;
        
        UIController.getInstance().setmKeyguardWallpaperContainer(this);
        mPaint.setAntiAlias(true);
        mPaint.setXfermode(new PorterDuffXfermode(Mode.DST_ATOP));
        
        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.haokan_wallpaper_alpha).copy(Bitmap.Config.ARGB_8888, true);
    }
    
    public void reset() {
        mTop = mScreenHeight;
        invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
    private static final int BLINDDEGREE = 150;
    
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (!mIsSecure && mTop != mScreenHeight) {
            canvas.drawBitmap(mBitmap, 0, mTop, mPaint);
        }
        
        int color = Color.argb((int)(BLINDDEGREE * mBlind), 0, 0, 0);
		canvas.drawColor(color);
    }
    
    public void onKeyguardScrollChanged(int top,int maxBoundY) {
        
        int bitmapHeight = mBitmap.getHeight();
        mTop = (int) (mScreenHeight - top * (bitmapHeight / (float)maxBoundY));
        postInvalidate();
    }
    
   public void onKeyguardScrollChanged(int top,int maxBoundY, boolean isSecure) {
        
        int bitmapHeight = mBitmap.getHeight();
        mTop = (int) (mScreenHeight - top * (bitmapHeight / (float)maxBoundY));
        mIsSecure=isSecure;
        if(isSecure){
        	mBlind= (float)top/maxBoundY;
        }else{
        	mBlind=0;
        }
        postInvalidate();
    }
    
}
