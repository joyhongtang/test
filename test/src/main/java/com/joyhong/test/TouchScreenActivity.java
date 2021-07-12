package com.joyhong.test;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

@SuppressLint("NewApi")
public class TouchScreenActivity extends Activity{

    private int mScreenWidth,mScreenHeight;
    private int mWidth,mHeight;
    private int mCounterWidth;
    private int mRow,r,mCol,c;
    private int mSelRow,mSelCol;
    private float mGetX,mGetY;
    private DrawCanvas drv;

    private int[][] mInArr=new int[50][50];

    @Override
    public void finish() {
        // TODO Auto-generated method stub
        super.finish();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // TODO Auto-generated method stub

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);


        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);

        mScreenWidth = metric.widthPixels;     // 屏幕宽度（像素）
        mScreenHeight = metric.heightPixels;   //

        //480-800
        Log.i("LCD","screen width : " + this.mScreenWidth
                +" // screen height : " + this.mScreenHeight);

        mWidth=0;
        mHeight=0;
        mCounterWidth=40;//pixel
        mGetX=0;
        mGetY=0;

        mRow=(int)(mScreenWidth/mCounterWidth);
        if(mRow >= 30){
            mRow =  29 ;
        }//480/40=12
        mCol=(int)(mScreenHeight/mCounterWidth);//800/40=20
        if(mCol >= 30){
            mCol =  29 ;
        }

        mCol =  50 ;
        for(c=0;c<mCol;c++){
            for(r=0;r<mCol;r++){
                mInArr[r][c]=1;
            }
        }


        drv=new DrawCanvas(this);

        this.setContentView(drv);

    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        // TODO Auto-generated method stub
        return super.onCreateView(name, context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        Log.i("position","x: "+event.getX()+" Y : " + event.getY());

        mGetX=event.getX();
        mGetY=event.getY();

        drv.invalidate();

        return true;
    }

    public class DrawCanvas extends View{

        private Paint mPaint= new Paint();

        public DrawCanvas(Context context) {
            super(context);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // TODO Auto-generated method stub
            super.onDraw(canvas);

            mScreenWidth=canvas.getWidth();
            mScreenHeight=canvas.getHeight();
            mRow=(int)(mScreenWidth/mCounterWidth);//480/40=12
            mCol=(int)(mScreenHeight/mCounterWidth);//800/40=20
            mRow = 20;
            mCol = 50;
            Log.i("LCD canvas","screen width : " + mScreenWidth
                    +" // screen height : " + mScreenHeight);


            for(c=0;c<mCol;c++){
                if(mGetY-72<c*mCounterWidth){
                    Log.i("SEL","select colume : "+c);
                    mSelCol=c-1;
                    if(mSelCol<0){
                        mSelCol=0;
                    }
                    break;
                }
            }

            for(r=0;r<mRow;r++){
                if(mGetX<r*mCounterWidth){
                    Log.i("SEL","select row : "+r);
                    mSelRow=r-1;
                    if(mSelRow<0){
                        mSelRow=0;
                    }
                    break;
                }
            }
            if(mGetY!=0){
                mInArr[mSelRow][mSelCol]=0;
            }

            for(c=0;c<20;c++){
                for(r=0;r<50;r++){

                    Log.i("P","c : "+c+" r : "+r+"value : "+ mInArr[r][c]);

                    if(mInArr[r][c]==1){
                        mPaint.setColor(Color.RED);
                        canvas.drawRect(r*mCounterWidth,c*mCounterWidth, r*mCounterWidth+mCounterWidth-1, c*mCounterWidth+mCounterWidth-1, mPaint);
                        mPaint.setColor(Color.GREEN);
                        canvas.drawLine(r*mCounterWidth, c*mCounterWidth, r*mCounterWidth, c*mCounterWidth+mCounterWidth, mPaint);
                        canvas.drawLine(r*mCounterWidth, c*mCounterWidth+mCounterWidth, r*mCounterWidth+mCounterWidth, c*mCounterWidth+mCounterWidth, mPaint);
                    }else{
                        mPaint.setColor(Color.WHITE);
                        canvas.drawRect(r*mCounterWidth,c*mCounterWidth, r*mCounterWidth+mCounterWidth-1, c*mCounterWidth+mCounterWidth-1, mPaint);
                        mPaint.setColor(Color.GREEN);
                        canvas.drawLine(r*mCounterWidth, c*mCounterWidth, r*mCounterWidth, c*mCounterWidth+mCounterWidth, mPaint);
                        canvas.drawLine(r*mCounterWidth, c*mCounterWidth+mCounterWidth, r*mCounterWidth+mCounterWidth, c*mCounterWidth+mCounterWidth, mPaint);
                    }
                }
            }


        }

    }



}