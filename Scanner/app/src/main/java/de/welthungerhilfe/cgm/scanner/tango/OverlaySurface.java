package de.welthungerhilfe.cgm.scanner.tango;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
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


    public static final int NO_OVERLAY = 0;
    public static final int BABY_OVERLAY = 1;
    public static final int INFANT_OVERLAY = 2;

    private static final String TAG = OverlaySurface.class.getSimpleName();
    private Context mContext;
    private Bitmap mBabyOverlay;
    private Bitmap mInfantOverlay;

    private boolean isReadyToDraw = false;

    SurfaceHolder holder;

    volatile boolean running = false;

    Thread thread = null;

    private float mConfidence = 1.0f;
    private float mDistance = 1.0f;

    private int mMode = NO_OVERLAY;

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

    public void setMode (int mode)
    {
        mMode = mode;
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

        mBabyOverlay = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.scan_outline_dots);
        mInfantOverlay = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.infant_outline);

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

    private void drawBabyOverlay() {

        Surface surface = holder.getSurface();
        Canvas canvas = surface.lockCanvas(null);
        if (canvas != null) {
            // clear screen before redrawing
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            // Source is the whole bitmap
            Rect srcRect = new Rect(0, 0, mBabyOverlay.getWidth(), mBabyOverlay.getHeight());

            // destination is the where to draw it
            // will be drawn in the center and scaled by the distance
            // because distance to take measurements should be around 1 meter
            float left = ((canvas.getWidth() - mBabyOverlay.getWidth()* mDistance) / 2.0f);
            float top = ((canvas.getHeight() - mBabyOverlay.getHeight()*mDistance) / 2.0f);
            float right = (mBabyOverlay.getWidth() * mDistance )+left;
            float bottom = (mBabyOverlay.getHeight()*mDistance) +top;
            RectF dstRectF = new RectF(left,top,right,bottom);

            setConfidenceColor();

            canvas.drawBitmap(mBabyOverlay, srcRect, dstRectF, mPaint);
            surface.unlockCanvasAndPost(canvas);
        }
    }


    private void setConfidenceColor() {
        float[] redColorTransform = {
                0, 1f, 0, 0, 0,
                0, 0, 0f, 0, 0,
                0, 0, 0, 0f, 0,
                0, 0, 0, 1f, 0};

        float[] greenColorTransform = {
                0, 0, 0, 0, 0,
                0, 1f, 0f, 0, 0,
                0, 0, 0, 0f, 0,
                0, 0, 0, 1f, 0};

        float[] yellowColorTransform = {
                0, 1f, 0, 0, 0,
                0, 1f, 0f, 0, 0,
                0, 0, 0, 0f, 0,
                0, 0, 0, 1f, 0};

        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0f); //Remove Colour

        if (mConfidence < 0.8 || mDistance <0.6f || mDistance > 1.5f) {
            colorMatrix.set(redColorTransform); //Apply the Red
        }
        else if (mConfidence < 0.95 || mDistance < 0.8f || mDistance > 1.2f)
        {
            colorMatrix.set(yellowColorTransform);
        }
        else
        {
            colorMatrix.set(greenColorTransform);
        }

        ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);

        mPaint.setColorFilter(colorFilter);
    }

    private void drawInfantOverlay() {

        Surface surface = holder.getSurface();
        Canvas canvas = surface.lockCanvas(null);
        if (canvas != null) {
            // clear screen before redrawing
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            final float infantScaling = 1.5f;

            // Source is the whole bitmap
            Rect srcRect = new Rect(0, 0, mInfantOverlay.getWidth(), mInfantOverlay.getHeight());

            // destination is the where to draw it
            // will be drawn in the center and scaled by the distance
            // because distance to take measurements should be around 1 meter
            float left = ((canvas.getWidth() - mInfantOverlay.getWidth()* infantScaling) / 2.0f);
            float top = ((canvas.getHeight() - mInfantOverlay.getHeight()*infantScaling) / 2.0f);
            float right = (mInfantOverlay.getWidth() * infantScaling )+left;
            float bottom = (mInfantOverlay.getHeight()*infantScaling) +top;
            RectF dstRectF = new RectF(left,top,right,bottom);

            Paint paint = new Paint();
            canvas.drawBitmap(mInfantOverlay, srcRect, dstRectF, paint);
            surface.unlockCanvasAndPost(canvas);
        }
    }

    public void setConfidence(float confidence) {
        this.mConfidence = confidence;
    }

    public void setDistance(float distance) {
        if (distance < 0.5f || Float.isNaN(distance))
            distance = 0.5f;
        else if(distance > 2.0f || Float.isInfinite(distance))
            distance = 2.0f;

        this.mDistance = distance;
    }

    @Override
    public void run() {
        while (running ) {
            if(isReadyToDraw) {
                if(mMode == BABY_OVERLAY) {
                    drawBabyOverlay();
                }
                else if (mMode == INFANT_OVERLAY) {
                    drawInfantOverlay();
                }
            }
        }
    }
}
