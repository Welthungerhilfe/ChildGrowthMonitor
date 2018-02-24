package de.welthungerhilfe.cgm.scanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

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
public class OverlaySurface extends SurfaceView implements SurfaceHolder.Callback {


    private static final String TAG = OverlaySurface.class.getSimpleName();
    private Context mContext;
    private Bitmap mOverlay;

    public OverlaySurface(Context context) {
        super(context);
        this.mContext = context;
        getHolder().addCallback(this);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }

    public OverlaySurface(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OverlaySurface(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public OverlaySurface(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        int id = RecorderActivity.getSurfaceId(holder);
        if (id < 0) {
            Log.w(TAG, "surfaceCreated UNKNOWN holder=" + holder);
        } else {
            Log.d(TAG, "surfaceCreated #" + id + " holder=" + holder);

        }
        mOverlay = BitmapFactory.decodeResource(mContext.getResources(),R.drawable.scan_outline_dots);
    }

    /**
     * SurfaceHolder.Callback method
     * <p>
     * Draws when the surface changes.  Since nothing else is touching the surface, and
     * we're not animating, we just draw here and ignore it.
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged fmt=" + format + " size=" + width + "x" + height +
                " holder=" + holder);

        int id = RecorderActivity.getSurfaceId(holder);
        boolean portrait = height > width;
        Surface surface = holder.getSurface();

        switch (id) {
            case 1:

                break;
            case 2:
                drawOverlaySurface();
                break;
            case 3:
                // top layer: alpha stripes
                if (portrait) {
                    int halfLine = width / 16 + 1;
                    //drawRectSurface(surface, width/2 - halfLine, 0, halfLine*2, height);
                } else {
                    int halfLine = height / 16 + 1;
                    //drawRectSurface(surface, 0, height/2 - halfLine, width, halfLine*2);
                }
                break;
            default:
                throw new RuntimeException("wha?");
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // ignore
        Log.d(TAG, "Surface destroyed holder=" + holder);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawOverlaySurface();
    }

    private void drawOverlaySurface() {
        Surface surface = getHolder().getSurface();
        Canvas canvas = surface.lockCanvas(null);
        //canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(mOverlay, 150, 200, new Paint());
        //outline.draw(canvas);
        surface.unlockCanvasAndPost(canvas);
    }




}
