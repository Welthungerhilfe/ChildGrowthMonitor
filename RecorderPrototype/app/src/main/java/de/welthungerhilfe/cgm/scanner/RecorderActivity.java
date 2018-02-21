/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.welthungerhilfe.cgm.scanner;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;

//TODO: new style permissions?
import android.support.v4.app.ActivityCompat;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.tangosupport.TangoSupport;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import android.Manifest;

import static com.projecttango.tangosupport.TangoSupport.initialize;

public class RecorderActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int INVALID_TEXTURE_ID = 0;
    private static final String sTimestampFormat = "Timestamp: %f";

    private GLSurfaceView mCameraSurfaceView;
    private ScanVideoRenderer mRenderer;
    private TextView mDisplayTextView;

    private SurfaceView mOverlaySurfaceView;
    private SurfaceView mPointCloudSurfaceView;

    private Tango mTango;
    private TangoConfig mConfig;
    private boolean mIsConnected = false;

    private int mValidPoseCallbackCount;
    private int mPreviousPoseStatus;
    private float mDeltaTime;
    private float mPosePreviousTimeStamp;

    private boolean mIsRecording = false;

    private static final int SECS_TO_MILLISECS = 1000;

    // NOTE: Naming indicates which thread is in charge of updating this variable.
    private int mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
    private AtomicBoolean mIsFrameAvailableTangoThread = new AtomicBoolean(false);

    private int mDisplayRotation = Surface.ROTATION_0;

    private boolean mRecordingEnabled;      // controls button state

    private File outputFile;

    // this is static so it survives activity restarts
    private static TextureMovieEncoder sVideoEncoder = new TextureMovieEncoder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);

        mDisplayTextView = (TextView) findViewById(R.id.display_textview);
        mCameraSurfaceView = (GLSurfaceView) findViewById(R.id.surfaceview);

        mOverlaySurfaceView = (SurfaceView) findViewById(R.id.overlaySurfaceView);
        mOverlaySurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        mPointCloudSurfaceView = (SurfaceView) findViewById(R.id.pointCloudSurfaceView);
        mPointCloudSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        // The request code used in ActivityCompat.requestPermissions()
        // and returned in the Activity's onRequestPermissionsResult()
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        outputFile = new File(getExternalFilesDir(Environment.getDataDirectory().getAbsolutePath()).getAbsolutePath(), "camera-tango.mp4");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_scan_result);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/

                //Intent i = new Intent(getApplicationContext(), MainActivity.class);
                //startActivity(i);

                mRecordingEnabled = !mRecordingEnabled;
                mCameraSurfaceView.queueEvent(new Runnable() {
                    @Override public void run() {
                        // notify the renderer that we want to change the encoder's state
                        mRenderer.changeRecordingState(mRecordingEnabled);
                    }
                });
                //updateControls();
                mDisplayTextView.setText(String.valueOf(mRecordingEnabled));
            }
        });

        mRecordingEnabled = sVideoEncoder.isRecording();

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (displayManager != null) {
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    synchronized (this) {
                        setDisplayRotation();
                    }
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                }
            }, null);
        }
        // Set up a dummy OpenGL renderer associated with this surface view.
        setupRenderer();
        mRecordingEnabled = sVideoEncoder.isRecording();
    }

    // TODO: implement own code&documentation or attribute Apache License 2.0 Copyright Google
    @Override
    protected void onResume() {
        super.onResume();
        mCameraSurfaceView.onResume();

        // Set render mode to RENDERMODE_CONTINUOUSLY to force getting onDraw callbacks until the
        // Tango Service is properly set up and we start getting onFrameAvailable callbacks.
        mCameraSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Initialize Tango Service as a normal Android Service. Since we call mTango.disconnect()
        // in onPause, this will unbind Tango Service, so every time onResume gets called we
        // should create a new Tango object.
        mTango = new Tango(RecorderActivity.this, new Runnable() {
            // Pass in a Runnable to be called from UI thread when Tango is ready; this Runnable
            // will be running on a new thread.
            // When Tango is ready, we can call Tango functions safely here only when there is no UI
            // thread changes involved.
            @Override
            public void run() {
                // Synchronize against disconnecting while the service is being used in
                // the OpenGL thread or in the UI thread.
                synchronized (RecorderActivity.this) {
                    try {
                        mConfig = setupTangoConfig(mTango);
                        mTango.connect(mConfig);
                        startupTango();
                        initialize(mTango);
                        mIsConnected = true;
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                mDisplayTextView.setText("scanning...");
//                            }
//                        });
                        setDisplayRotation();
                    } catch (TangoOutOfDateException e) {
                        Log.e(TAG, getString(R.string.exception_out_of_date), e);
                        showsToastAndFinishOnUiThread(R.string.exception_out_of_date);
                    } catch (TangoErrorException e) {
                        Log.e(TAG, getString(R.string.exception_tango_error), e);
                        showsToastAndFinishOnUiThread(R.string.exception_tango_error);
                    } catch (TangoInvalidException e) {
                        Log.e(TAG, getString(R.string.exception_tango_invalid), e);
                        showsToastAndFinishOnUiThread(R.string.exception_tango_invalid);
                    }
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraSurfaceView.onPause();
        // Synchronize against disconnecting while the service is being used in the OpenGL
        // thread or in the UI thread.
        // NOTE: DO NOT lock against this same object in the Tango callback thread.
        // Tango.disconnect will block here until all Tango callback calls are finished.
        // If you lock against this object in a Tango callback thread it will cause a deadlock.
        synchronized (this) {
            try {
                mTango.disconnectCamera(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                // We need to invalidate the connected texture ID so that we cause a
                // re-connection in the OpenGL thread after resume.
                mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
                mTango.disconnect();
                mIsConnected = false;
            } catch (TangoErrorException e) {
                Log.e(TAG, getString(R.string.exception_tango_error), e);
            }
        }
    }

    /**
     * Sets up the Tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setupTangoConfig(Tango tango) {
        // Create a new Tango configuration and enable the Camera API.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);
        return config;
    }

    /**
     * Set up the callback listeners for the Tango Service and obtain other parameters required
     * after Tango connection.
     * Listen to updates from the RGB camera.
     */
    private void startupTango() {
        // Lock configuration and connect to Tango.
        // Select coordinate frame pair.
        final ArrayList<TangoCoordinateFramePair> framePairs =
                new ArrayList<TangoCoordinateFramePair>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));

        // Listen for new Tango data.
        mTango.connectListener(framePairs, new Tango.TangoUpdateCallback() {

            String[] poseStatusCode = {"POSE_INITIALIZING","POSE_VALID","POSE_INVALID","POSE_UNKNOWN"};

            // TODO: save pose data
            @Override
            public void onPoseAvailable(final TangoPoseData pose) {
                mDeltaTime = (float) (pose.timestamp - mPosePreviousTimeStamp)
                        * SECS_TO_MILLISECS;
                mPosePreviousTimeStamp = (float) pose.timestamp;
                if (mPreviousPoseStatus != pose.statusCode) {
                    mValidPoseCallbackCount = 0;
                }
                mValidPoseCallbackCount++;
                mPreviousPoseStatus = pose.statusCode;

                DecimalFormat threeDec = new DecimalFormat("0.000");
                String translationString = "["
                        + threeDec.format(pose.translation[0]) + ", "
                        + threeDec.format(pose.translation[1]) + ", "
                        + threeDec.format(pose.translation[2]) + "] ";
                String quaternionString = "["
                        + threeDec.format(pose.rotation[0]) + ", "
                        + threeDec.format(pose.rotation[1]) + ", "
                        + threeDec.format(pose.rotation[2]) + ", "
                        + threeDec.format(pose.rotation[3]) + "] ";
/*
                Log.v(TAG, "timestamp: "+pose.timestamp+
                        " translationString: "+translationString+
                        " quaternionString: "+quaternionString+
                        " mValidPoseCallbackCount: "+Integer.toString(mValidPoseCallbackCount)+
                        " mDeltaTime: "+threeDec.format(mDeltaTime)+
                        " pose.statusCode: "+pose.statusCode+": "+poseStatusCode[pose.statusCode]
                );*/
            }

            @Override
            public void onPointCloudAvailable(final TangoPointCloudData pointCloudData) {

                // TODO: get PointCloud and Camera Data


                // Get pose transforms for openGL to depth/color cameras.
                TangoPoseData oglTdepthPose = TangoSupport.getPoseAtTime(
                        pointCloudData.timestamp,
                        TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                        TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                        TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                        TangoSupport.ROTATION_IGNORED);
                if (oglTdepthPose.statusCode != TangoPoseData.POSE_VALID) {
                    //Log.w(TAG, "Could not get depth camera transform at time "
                    //        + pointCloudData.timestamp);
                }
                /*
                TangoPoseData oglTcolorPose = TangoSupport.getPoseAtTime(
                        rgbTimestamp,
                        TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                        TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                        TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                        TangoSupport.ROTATION_IGNORED);
                if (oglTcolorPose.statusCode != TangoPoseData.POSE_VALID) {
                    //   Log.w(TAG, "Could not get color camera transform at time "
                    //           + rgbTimestamp);
                }*/

                final long startTime = SystemClock.uptimeMillis();
                final long timestamp = System.currentTimeMillis();
/*
                if (!scanningInProgress) {
                    //Log.v(TAG, "User has not started");
                    return;
                }

                // Scanning in progress...

                if (mCurrentScanID == null) {
                    mCurrentScanID = dbHelper.startScan(timestamp);
                }*/
                //Frameset frameset = new Frameset(timestamp, oglTdepthPose, oglTcolorPose, pointCloudData, imageBuffer);
                //Log.v(TAG, "timestamp: " + timestamp + " with pointCloud timestamp " + pointCloudData.timestamp + " with " + pointCloudData.numPoints + " points.");
                /*
                Log.v(TAG, "Scan " + timestamp + " frame number: " + imageBuffer.frameNumber + " with timestamp: " + imageBuffer.timestamp +
                        " and pointCloud timestamp " + pointCloudData.timestamp + " with " + pointCloudData.numPoints + " points.");
                int[] pixels = convertYUV420_NV21toRGB8888(imageBuffer.data.array(), imageBuffer.width, imageBuffer.height);
                Bitmap inputBitmap = Bitmap.createBitmap(pixels, imageBuffer.width, imageBuffer.height, Bitmap.Config.ARGB_8888);
                */
            }


            @Override
            public void onFrameAvailable(int cameraId) {
                // This will get called every time a new RGB camera frame is available to be
                // rendered.
                //Log.d(TAG, "onFrameAvailable");

                if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR) {
                    // Now that we are receiving onFrameAvailable callbacks, we can switch
                    // to RENDERMODE_WHEN_DIRTY to drive the render loop from this callback.
                    // This will result in a frame rate of approximately 30FPS, in synchrony with
                    // the RGB camera driver.
                    // If you need to render at a higher rate (i.e., if you want to render complex
                    // animations smoothly) you  can use RENDERMODE_CONTINUOUSLY throughout the
                    // application lifecycle.
                    if (mCameraSurfaceView.getRenderMode() != GLSurfaceView.RENDERMODE_WHEN_DIRTY) {
                        mCameraSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                    }

                    // Note that the RGB data is not passed as a parameter here.
                    // Instead, this callback indicates that you can call
                    // the {@code updateTexture()} method to have the
                    // RGB data copied directly to the OpenGL texture at the native layer.
                    // Since that call needs to be done from the OpenGL thread, what we do here is
                    // set up a flag to tell the OpenGL thread to do that in the next run.
                    // NOTE: Even if we are using a render-by-request method, this flag is still
                    // necessary since the OpenGL thread run requested below is not guaranteed
                    // to run in synchrony with this requesting call.
                    mIsFrameAvailableTangoThread.set(true);
                    // Trigger an OpenGL render to update the OpenGL scene with the new RGB data.
                    mCameraSurfaceView.requestRender();
                }
            }
        });

        /*
        // needed for the actual image buffer to record video
        // alternative would be the C++ API of Tango that also provides image data
        mTango.experimentalConnectOnFrameListener(TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                new Tango.OnFrameAvailableListener() {
                    @Override
                    public  void onFrameAvailable(TangoImageBuffer tangoImageBuffer, int i) {
                        if (!inferenceReady) {
                            return;
                        }

                        mCurrentImageBuffer = copyImageBuffer(tangoImageBuffer);
                    }

                    TangoImageBuffer copyImageBuffer(TangoImageBuffer imageBuffer) {
                        ByteBuffer clone = ByteBuffer.allocateDirect(imageBuffer.data.capacity());
                        imageBuffer.data.rewind();
                        clone.put(imageBuffer.data);
                        imageBuffer.data.rewind();
                        clone.flip();
                        return new TangoImageBuffer(imageBuffer.width, imageBuffer.height,
                                imageBuffer.stride, imageBuffer.frameNumber,
                                imageBuffer.timestamp, imageBuffer.format, clone);
                    }
                });
                */
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Intent i = new Intent(this, PredictionsList.class);
        //startActivity(i);
        Toast.makeText(this.getApplicationContext(), "Touch!! :)", Toast.LENGTH_SHORT).show();
        return true;
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    // TODO: setup own renderer for scanning process (or attribute Apache License 2.0 from Google)
    private void setupRenderer() {
        mCameraSurfaceView.setEGLContextClientVersion(2);
        mRenderer = new ScanVideoRenderer(getApplicationContext(), sVideoEncoder, outputFile, new ScanVideoRenderer.RenderCallback() {
            @Override
            public void preRender() {
                //Log.d(TAG, "preRender");
                // This is the work that you would do on your main OpenGL render thread.

                // We need to be careful to not run any Tango-dependent code in the OpenGL
                // thread unless we know the Tango Service to be properly set up and connected.
                if (!mIsConnected) {
                    return;
                }

                try {
                    // Synchronize against concurrently disconnecting the service triggered from the
                    // UI thread.
                    synchronized (RecorderActivity.this) {
                        // Connect the Tango SDK to the OpenGL texture ID where we are going to
                        // render the camera.
                        // NOTE: This must be done after the texture is generated and the Tango
                        // service is connected.
                        if (mConnectedTextureIdGlThread == INVALID_TEXTURE_ID) {
                            mConnectedTextureIdGlThread = mRenderer.getTextureId();
                            mTango.connectTextureId(TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                    mRenderer.getTextureId());
                            Log.d(TAG, "connected to texture id: " + mRenderer.getTextureId());
                        }

                        // If there is a new RGB camera frame available, update the texture and
                        // scene camera pose.
                        if (mIsFrameAvailableTangoThread.compareAndSet(true, false)) {
                            double rgbTimestamp =
                                    mTango.updateTexture(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);

                            // {@code rgbTimestamp} contains the exact timestamp at which the
                            // rendered RGB frame was acquired.

                            // In order to see more details on how to use this timestamp to modify
                            // the scene camera and achieve an augmented reality effect,
                            // refer to java_augmented_reality_example and/or
                            // java_augmented_reality_opengl_example projects.

                            // Log and display timestamp for informational purposes.
                            //Log.d(TAG, "Frame updated. Timestamp: " + rgbTimestamp);

                            // Updating the UI needs to be in a separate thread. Do it through a
                            // final local variable to avoid concurrency issues.
                            final String timestampText = String.format(sTimestampFormat,
                                    rgbTimestamp);
                            /*runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mDisplayTextView.setText(timestampText);
                                }
                            });*/
                        }
                    }
                } catch (TangoErrorException e) {
                    Log.e(TAG, "Tango API call error within the OpenGL thread", e);
                } catch (Throwable t) {
                    Log.e(TAG, "Exception on the OpenGL thread", t);
                }
            }
        });
        mCameraSurfaceView.setRenderer(mRenderer);
    }

    /**
     * Set the color camera background texture rotation and save the camera to display rotation.
     */
    private void setDisplayRotation() {
        Display display = getWindowManager().getDefaultDisplay();
        mDisplayRotation = display.getRotation();

        // We also need to update the camera texture UV coordinates. This must be run in the OpenGL
        // thread.
        mCameraSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mIsConnected) {
                    mRenderer.updateColorCameraTextureUv(mDisplayRotation);
                }
            }
        });
    }

    // TODO: attribute Apache License 2.0 from Google or remove
    /**
     * Display toast on UI thread.
     *
     * @param resId The resource id of the string resource to use. Can be formatted text.
     */
    private void showsToastAndFinishOnUiThread(final int resId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RecorderActivity.this,
                        getString(resId), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

}
