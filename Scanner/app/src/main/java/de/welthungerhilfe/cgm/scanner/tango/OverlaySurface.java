package de.welthungerhilfe.cgm.scanner.tango;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import de.welthungerhilfe.cgm.scanner.R;

/**
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class OverlaySurface extends SurfaceView
        implements SurfaceHolder.Callback, Runnable {


    private static final String TAG = OverlaySurface.class.getSimpleName();
    private Context mContext;
    private Bitmap mOverlay;

    private boolean isReadyToDraw = false;

    SurfaceHolder holder;

    volatile boolean running = false;

    Thread thread = null;

    private float mConfidence = 1.0f;
    private float mDistance = 1.0f;

    Paint mPaint = new Paint();

    public OverlaySurface(Context context) {
        super(context);
        this.mContext = context;
        holder = getHolder();
        holder.addCallback(this);
        holder.setFormat(PixelFormat.TRANSLUCENT);
    }

    public OverlaySurface(Context context, AttributeSet attrs) {

        super(context, attrs);
        this.mContext = context;
        holder = getHolder();
        holder.addCallback(this);
        holder.setFormat(PixelFormat.TRANSLUCENT);
    }

    public OverlaySurface(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        holder = getHolder();
        holder.addCallback(this);
        holder.setFormat(PixelFormat.TRANSLUCENT);
    }

    public OverlaySurface(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mContext = context;
        holder = getHolder();
        holder.addCallback(this);
        holder.setFormat(PixelFormat.TRANSLUCENT);
    }

    public void onResumeOverlaySurfaceView(){
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void onPauseOverlaySurfaceView(){
        boolean retry = true;
        running = false;
        while(retry){
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        /*
        int id = RecorderActivity.getSurfaceId(holder);
        if (id < 0) {
            Log.w(TAG, "surfaceCreated UNKNOWN holder=" + holder);
        } else {
            Log.d(TAG, "surfaceCreated #" + id + " holder=" + holder);

        }
        */
        mOverlay = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.scan_outline_dots);
        isReadyToDraw = true;
    }

    /**
     * SurfaceHolder.Callback method
     * <p>
     *     starts rendering thread when the surface changes
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged fmt=" + format + " size=" + width + "x" + height +
                " holder=" + holder);

        onResumeOverlaySurfaceView();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // ignore
        Log.d(TAG, "Surface destroyed holder=" + holder);
        isReadyToDraw = false;
        onPauseOverlaySurfaceView();
    }

    private void drawFullBodyScan() {

        Surface surface = holder.getSurface();
        Canvas canvas = surface.lockCanvas(null);
        if (canvas != null) {
            // clear screen before redrawing
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            // Source is the whole bitmap
            Rect srcRect = new Rect(0, 0,mOverlay.getWidth(),mOverlay.getHeight());

            // destination is the where to draw it
            // will be drawn in the center and scaled by the distance
            // because distance to take measurements should be around 1 meter
            float left = ((canvas.getWidth() - mOverlay.getWidth()* mDistance) / 2);
            float top = ((canvas.getHeight() - mOverlay.getHeight()*mDistance) / 2);
            float right = (mOverlay.getWidth()+left) * mDistance;
            float bottom = (mOverlay.getHeight()+top)*mDistance;
            RectF dstRectF = new RectF(left,top,right,bottom);

            canvas.drawBitmap(mOverlay, srcRect, dstRectF, mPaint);
            surface.unlockCanvasAndPost(canvas);
        }
    }

    private void drawCenter() {

        Surface surface = holder.getSurface();
        Canvas canvas = surface.lockCanvas(null);
        if (canvas != null) {
            // clear screen before redrawing
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            // Source is the whole bitmap
            Rect srcRect = new Rect(0, 0,mOverlay.getWidth(),mOverlay.getHeight());
            // destination is the where to draw it
            int left = (canvas.getWidth()-mOverlay.getWidth()) / 2;
            int top =  (canvas.getHeight()-mOverlay.getHeight()) / 2;
            Rect dstRect = new Rect(left,top,mOverlay.getWidth()+left,mOverlay.getHeight()+top);

            canvas.drawBitmap(mOverlay, srcRect, dstRect, mPaint);
            surface.unlockCanvasAndPost(canvas);
        }
    }

    public void setConfidence(float mConfidence) {
        this.mConfidence = mConfidence;
    }

    public void setDistance(float mDistance) {
        this.mDistance = mDistance;
    }

    @Override
    public void run() {
        while (running ) {
            if(isReadyToDraw) {
                drawFullBodyScan();
                //drawCenter();
            }
        }
    }
}
