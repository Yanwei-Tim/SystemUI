
package com.amigo.navi.keyguard.haokan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import com.android.keyguard.R;

public class KeyguardWallpaperContainer extends FrameLayout {

    private int mTop = Integer.MAX_VALUE;
    
    private Paint mPaint = new Paint();
    
    private int mScreenHeight = 2560;
    private int mScreenWidth = 1440;  
    private float mRadius;
    
    private float cx,cy;
    
    private float mBlind=0;
    
    private int mModel;
    
    
    private static final int BLINDDEGREE = 150;
    
    private int[] colors = new int[] {0xffffffff, 0xffffffff, /*0xddffffff, 0xaaffffff, 0x66ffffff, 0x22ffffff, */0x00ffffff,};
    
    private float[] stops = new float[] { 0, 0.82f, /*0.75f, 0.80f, 0.85f, 0.90f,*/ 1.0f};
    
    
    private Paint mBottomPaint = new Paint();
    private LinearGradient mLinearGradient;
    private Rect mBottomRect;
    
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
        
        cx = mScreenWidth / 2.0f;
        cy = mScreenHeight / 2.0f;
        
        mRadius = mScreenHeight * 2.0f;
        
        UIController.getInstance().setmKeyguardWallpaperContainer(this);
        mPaint.setAntiAlias(true);
//        mPaint.setXfermode(new PorterDuffXfermode(Mode.DST_ATOP));
        mPaint.setXfermode(new PorterDuffXfermode(Mode.XOR));
 
        mBottomRect = new Rect(0, mScreenHeight - getResources().getDimensionPixelSize(R.dimen.ketguard_infozone_height), mScreenWidth, mScreenHeight);
        mLinearGradient = new LinearGradient(0, mBottomRect.top, 0, mBottomRect.bottom, new int[] {Color.TRANSPARENT, 0x15000000, 0x40000000}, null,
                Shader.TileMode.CLAMP);
        mBottomPaint.setShader(mLinearGradient); 
    }
    
    
    public void reset() {
        mTop = Integer.MAX_VALUE;
		mBlind=0;
        invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
    
    
    
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        
        canvas.drawRect(mBottomRect, mBottomPaint);
        
        if (mModel!=UIController.SCROLL_TO_SECURTY && mTop != Integer.MAX_VALUE) {
            cy = mScreenHeight * 3 - mTop;
            
            RadialGradient mRadialGradient = new RadialGradient(cx, cy, mRadius ,
                    colors, stops, Shader.TileMode.CLAMP);
            
            mPaint.setShader(mRadialGradient); 
            canvas.drawCircle(cx, cy, mRadius, mPaint);
        }
        
        
        int color = Color.argb((int)(BLINDDEGREE * mBlind), 0, 0, 0);
		canvas.drawColor(color);
    }
    

    
   public void onKeyguardModelChanged(int top,int maxBoundY, int  model) {
        mTop = (int) (top * mScreenHeight * 1.4f / (float)maxBoundY);
        
        mModel=model;
        if(model==UIController.SCROLL_TO_SECURTY){
        	mBlind= (float)top/maxBoundY;
        }else if(model==UIController.SECURITY_SUCCESS_UNLOCK){
        	float blind=(float)top/maxBoundY;
        	mBlind= 1-blind;
        }else{
        	mBlind=0;
        }
        postInvalidate();
    }
  
    
}
