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
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;

//TODO: new style permissions?
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import android.Manifest;

import de.welthungerhilfe.cgm.scanner.util.ModelMatCalculator;
import de.welthungerhilfe.cgm.scanner.util.ZipWriter;

import static com.projecttango.tangosupport.TangoSupport.initialize;

public class RecorderActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int INVALID_TEXTURE_ID = 0;
    private static final String sTimestampFormat = "Timestamp: %f";

    private static GLSurfaceView mCameraSurfaceView;
    private ScanVideoRenderer mRenderer;
    private TextView mDisplayTextView;

    private static OverlaySurface mOverlaySurfaceView;
    private static SurfaceView mPointCloudSurfaceView;

    private Tango mTango;
    private TangoConfig mConfig;
    private boolean mIsConnected = false;

    private int mValidPoseCallbackCount;
    private int mPreviousPoseStatus;
    private float mDeltaTime;
    private float mPosePreviousTimeStamp;
    private float mPointCloudPreviousTimeStamp;
    private float mCurrentTimeStamp;

    boolean mIsRecording;

    private static final int SECS_TO_MILLISECS = 1000;

    // NOTE: Naming indicates which thread is in charge of updating this variable.
    private int mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
    private AtomicBoolean mIsFrameAvailableTangoThread = new AtomicBoolean(false);

    private int mDisplayRotation = Surface.ROTATION_0;

    private boolean mRecordingEnabled;      // controls button state
    private Semaphore mutex_on_mIsRecording;
    private File outputFile;
    private String mOutputFolder;
    private String mSaveDirAbsPath;
    private String mFilename;
    private int mNumberOfFilesWritten;
    private String mNowTimeString;
    private ArrayList<float[]> mPosePositionBuffer;
    private ArrayList<float[]> mPoseOrientationBuffer;
    private ArrayList<Float> mPoseTimestampBuffer;
    private ArrayList<String> mFilenameBuffer;
    private float[] cam2dev_Transform;
    private int mNumPoseInSequence;
    private Boolean mTimeToTakeSnap;

    private int mPointCloudCallbackCount;

    private TextView mRecordingTextView;

    // this is static so it survives activity restarts
    private static TextureMovieEncoder sVideoEncoder = new TextureMovieEncoder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);

        mCameraSurfaceView = findViewById(R.id.surfaceview);
        mOverlaySurfaceView = findViewById(R.id.overlaySurfaceView);

        // Set up a dummy OpenGL renderer associated with this surface view.

        mDisplayTextView = (TextView) findViewById(R.id.display_textview);
        mDisplayTextView.setText("Starting...");
        mRecordingTextView = findViewById(R.id.recording_textview);

        //mPointCloudSurfaceView = (SurfaceView) findViewById(R.id.pointCloudSurfaceView);
        //mPointCloudSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

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

        outputFile = new File(getExternalFilesDir(Environment.getDataDirectory().getAbsolutePath()).getAbsolutePath(), "/camera-tango.mp4");
        mOutputFolder = getExternalFilesDir(Environment.getDataDirectory().getAbsolutePath()).getAbsolutePath()+"/Tango/";
        mSaveDirAbsPath = getExternalFilesDir(Environment.getDataDirectory().getAbsolutePath()).getAbsolutePath()+"/Tango/PCLData/";
        mFilename = "";

        // must be called after setting outputFile!
        setupRenderer();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_scan_result);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/

                //Intent i = new Intent(getApplicationContext(), MainActivity.class);
                //startActivity(i);

                mIsRecording = !mIsRecording;
                mRecordingTextView.setText("Rec: "+mIsRecording);
                record_SwitchChanged();
                mCameraSurfaceView.queueEvent(new Runnable() {
                    @Override public void run() {
                        // notify the renderer that we want to change the encoder's state
                        mRenderer.changeRecordingState(mIsRecording);
                    }
                });
                //updateControls();
            }
        });

        mRecordingEnabled = sVideoEncoder.isRecording();
        mFilename = "";
        mNumberOfFilesWritten = 0;
        mPosePositionBuffer = new ArrayList<float[]>();
        mPoseOrientationBuffer = new ArrayList<float[]>();
        mPoseTimestampBuffer = new ArrayList<Float>();
        mFilenameBuffer = new ArrayList<String>();
        mNumPoseInSequence = 0;
        mPointCloudCallbackCount = 0;
        mutex_on_mIsRecording = new Semaphore(1,true);
        mIsRecording = false;

        mTimeToTakeSnap = false;

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
                    setUpExtrinsics();
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

                // My pose buffering
                if (mIsRecording && pose.statusCode == TangoPoseData.POSE_VALID) {
                    mPosePositionBuffer.add(mNumPoseInSequence, pose.getTranslationAsFloats());
                    mPoseOrientationBuffer.add(mNumPoseInSequence, pose.getRotationAsFloats());
                    mPoseTimestampBuffer.add((float)pose.timestamp);
                    mNumPoseInSequence++;
                }
                //End of My pose buffering

                /*mRenderer.getModelMatCalculator().updateModelMatrix(
                        pose.getTranslationAsFloats(),
                        pose.getRotationAsFloats());
                mRenderer.updateViewMatrix();
                mGLView.requestRender();
                */
                // Update the UI with TangoPose information
                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
*/
                        // Display pose data on screen in TextViews
                        /*mPoseTextView.setText(translationString);
                        mQuatTextView.setText(quaternionString);
                        mPoseCountTextView.setText(Integer.toString(mValidPoseCallbackCount));
                        mDeltaTextView.setText(threeDec.format(mDeltaTime));
                        if (pose.statusCode == TangoPoseData.POSE_VALID) {
                            mPoseStatusTextView.setText(R.string.pose_valid);
                        } else if (pose.statusCode == TangoPoseData.POSE_INVALID) {
                            mPoseStatusTextView.setText(R.string.pose_invalid);
                        } else if (pose.statusCode == TangoPoseData.POSE_INITIALIZING) {
                            mPoseStatusTextView.setText(R.string.pose_initializing);
                        } else if (pose.statusCode == TangoPoseData.POSE_UNKNOWN) {
                            mPoseStatusTextView.setText(R.string.pose_unknown);
                        }

                    }
                });*/
            }

            @Override
            public void onPointCloudAvailable(final TangoPointCloudData pointCloudData) {

                // TODO: get PointCloud and Camera Data

                StringBuilder stringBuilder = new StringBuilder();
                //stringBuilder.append("Point count: " + pointCloudData.numPoints);
                float[] average = calculateAveragedDepth(pointCloudData.points, pointCloudData.numPoints);
                stringBuilder.append("center depth (m): " + average[0]);
                stringBuilder.append(" confidence: " + average[1]);
                final String pointCloudString = stringBuilder.toString();
                //Log.i(TAG, pointCloudString);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDisplayTextView.setText(pointCloudString);
                    }
                });


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


                mCurrentTimeStamp = (float) pointCloudData.timestamp;
                final float frameDelta = (mCurrentTimeStamp - mPointCloudPreviousTimeStamp)
                        * SECS_TO_MILLISECS;
                mPointCloudPreviousTimeStamp = mCurrentTimeStamp;
                mPointCloudCallbackCount++;
                /*final byte[] buffer = new byte[pointCloudData.numPoints * 3 * 4];
                FileInputStream fileStream = new FileInputStream(pointCloudData.points);
                try {
                    fileStream.read(buffer, pointCloudData.pointCloudParcelFileDescriptorOffset, buffer.length);
                    fileStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
*/
                // My writing to file function


                // Background task for writing to file
                class SendCommandTask extends AsyncTask<Void, Void, Boolean> {
                    /** The system calls this to perform work in a worker thread and
                     * delivers it the parameters given to AsyncTask.execute() */
                    @Override
                    protected Boolean doInBackground(Void... params) {

                        try {
                            mutex_on_mIsRecording.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // Saving the frame or not, depending on the current mode.
                        if ( mIsRecording ) {
                            writePointCloudToFile(pointCloudData, framePairs);
                        }
                        mutex_on_mIsRecording.release();
                        return true;
                    }

                    /** The system calls this to perform work in the UI thread and delivers
                     * the result from doInBackground() */
                    @Override
                    protected void onPostExecute(Boolean done) {

                    }
                }
                new SendCommandTask().execute();

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
        // needed for the actual image buffer to record video?
        // alternative would be the C++ API of Tango that also provides image data
        // or recording directly from GLSurfaceView
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

    /**
     * Calculates the average depth at Center from a point cloud buffer.
     */
    private float[] calculateAveragedDepth(FloatBuffer pointCloudBuffer, int numPoints) {
        float totalZ = 0;
        float averageZ = 0;
        float totalC = 0;
        float averageC = 0;
        float currentX;
        float currentY;
        int countingPoints = 0;

        if (numPoints != 0) {
            int numFloats = 4 * numPoints;
            for (int i = 0; i < numFloats; i++) {
                currentX = pointCloudBuffer.get(i);
                i++;
                currentY = pointCloudBuffer.get(i);
                i++;
                if (currentX < 0.01 && currentX > -0.01 && currentY < 0.01 && currentY > -0.1) {
                    totalZ = totalZ + pointCloudBuffer.get(i);
                    countingPoints++;
                    i++;
                    totalC = totalC + pointCloudBuffer.get(i);
                } else {
                    i++;
                }
            }
            averageZ = totalZ / countingPoints;
            averageC = totalC / countingPoints;
        }
        float[] average = new float[2];
        average[0] = averageZ;
        average[1] = averageC;
        return average;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Intent i = new Intent(this, PredictionsList.class);
        //startActivity(i);
        //mIsRecording = !mIsRecording;
        Toast.makeText(this.getApplicationContext(), "Recording: "+mIsRecording+"!! :)", Toast.LENGTH_SHORT).show();
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

    /**
     * Returns an ordinal value for the SurfaceHolder, or -1 for an invalid surface.
     */
    public static int getSurfaceId(SurfaceHolder holder) {
        if (holder.equals(mCameraSurfaceView.getHolder())) {
            return 1;
        } else if (holder.equals(mOverlaySurfaceView.getHolder())) {
            return 2;
        } else if (holder.equals(mPointCloudSurfaceView.getHolder())) {
            return 3;
        } else {
            return -1;
        }
    }

    // This function writes the XYZ points to .vtk files in binary
    private void writePointCloudToFile(TangoPointCloudData pointCloudData,
                                       ArrayList<TangoCoordinateFramePair> framePairs) {

        /*
        ByteBuffer myBuffer = ByteBuffer.allocate(pointCloudData.numPoints * 3 * 4);
        myBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // TODO: 2nd argument: int offset
        myBuffer.put(pointCloudData.points, pointCloudData.pointCloudParcelFileDescriptorOffset, myBuffer.capacity());
*/

        File mainDir = new File(mOutputFolder);
        if(!mainDir.exists()) {
            boolean created = mainDir.mkdir();
            if (created) {
                Log.i(TAG, "Folder: \"" + mOutputFolder + "\" created\n");
            }
        }

        File dir = new File(mSaveDirAbsPath);
        if(!dir.exists()) {
            boolean created = dir.mkdir();
            if (created) {
                Log.i(TAG, "Folder: \"" + mSaveDirAbsPath + "\" created\n");
            }
        }

        mFilename = "pc_" + mNowTimeString + "_" + String.format("%03d", mNumberOfFilesWritten) +
                ".vtk";
        mFilenameBuffer.add(mSaveDirAbsPath + mFilename);
        File file = new File(dir, mFilename);


        try {

            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(file)));

            out.write(("# vtk DataFile Version 3.0\n" +
                    "vtk output\n" +
                    "BINARY\n" +
                    "DATASET POLYDATA\n" +
                    "POINTS " + pointCloudData.numPoints + " float\n").getBytes());

            for (int i = 0; i < pointCloudData.numPoints; i++) {
/*
                out.writeFloat(myBuffer.getFloat(4 * i * 4));
                out.writeFloat(myBuffer.getFloat((4 * i + 1) * 4));
                out.writeFloat(myBuffer.getFloat((4 * i + 2) * 4));
                i++;*/
                /* Point Clouds have 4*4 bytes now*/
                out.writeFloat(pointCloudData.points.get(i));
                i++;
                out.writeFloat(pointCloudData.points.get(i));
                i++;
                out.writeFloat(pointCloudData.points.get(i));
                i++;
            }

            out.write(("\nVERTICES 1 " + String.valueOf(pointCloudData.numPoints + 1) + "\n").getBytes());
            out.writeInt(pointCloudData.numPoints);
            for (int i = 0; i < pointCloudData.numPoints; i++) {
                out.writeInt(i);
            }

            out.write(("\nFIELD FieldData 1\n" + "timestamp 1 1 float\n").getBytes());
            out.writeFloat((float) pointCloudData.timestamp);

            out.close();
            mNumberOfFilesWritten++;
            //mTimeToTakeSnap = false;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO: proper attribution
    // From ParaView Tango Recorder Copyright Paraview
    // Apache License 2.0
    // https://github.com/Kitware/ParaViewTangoRecorder


    private void setUpExtrinsics() {
        // Set device to imu matrix in Model Matrix Calculator.
        TangoPoseData device2IMUPose = new TangoPoseData();
        TangoCoordinateFramePair framePair = new TangoCoordinateFramePair();
        framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_IMU;
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_DEVICE;
        try {
            device2IMUPose = mTango.getPoseAtTime(0.0, framePair);
        } catch (TangoErrorException e) {
            Toast.makeText(getApplicationContext(), R.string.TangoError,
                    Toast.LENGTH_SHORT).show();
        }
        /*mRenderer.getModelMatCalculator().SetDevice2IMUMatrix(
                device2IMUPose.getTranslationAsFloats(),
                device2IMUPose.getRotationAsFloats());
*/
        // Set color camera to imu matrix in Model Matrix Calculator.
        TangoPoseData color2IMUPose = new TangoPoseData();

        framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_IMU;
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR;
        try {
            color2IMUPose = mTango.getPoseAtTime(0.0, framePair);
        } catch (TangoErrorException e) {
            Toast.makeText(getApplicationContext(), R.string.TangoError,
                    Toast.LENGTH_SHORT).show();
        }
        /*
        mRenderer.getModelMatCalculator().SetColorCamera2IMUMatrix(
                color2IMUPose.getTranslationAsFloats(),
                color2IMUPose.getRotationAsFloats());
*/
        // Get the Camera2Device transform
        float[] rot_Dev2IMU = device2IMUPose.getRotationAsFloats();
        float[] trans_Dev2IMU = device2IMUPose.getTranslationAsFloats();
        float[] rot_Cam2IMU = color2IMUPose.getRotationAsFloats();
        float[] trans_Cam2IMU = color2IMUPose.getTranslationAsFloats();

        float[] dev2IMU = new float[16];
        Matrix.setIdentityM(dev2IMU, 0);
        dev2IMU = ModelMatCalculator.quaternionMatrixOpenGL(rot_Dev2IMU);
        dev2IMU[12] += trans_Dev2IMU[0];
        dev2IMU[13] += trans_Dev2IMU[1];
        dev2IMU[14] += trans_Dev2IMU[2];

        float[] IMU2dev = new float[16];
        Matrix.setIdentityM(IMU2dev, 0);
        Matrix.invertM(IMU2dev, 0, dev2IMU, 0);

        float[] cam2IMU = new float[16];
        Matrix.setIdentityM(cam2IMU, 0);
        cam2IMU = ModelMatCalculator.quaternionMatrixOpenGL(rot_Cam2IMU);
        cam2IMU[12] += trans_Cam2IMU[0];
        cam2IMU[13] += trans_Cam2IMU[1];
        cam2IMU[14] += trans_Cam2IMU[2];

        cam2dev_Transform = new float[16];
        Matrix.setIdentityM(cam2dev_Transform, 0);
        Matrix.multiplyMM(cam2dev_Transform, 0, IMU2dev, 0, cam2IMU, 0);
    }


    // This function is called when the Take Snapshot button is clicked
    private void takeSnapshot_ButtonClicked() {
        mTimeToTakeSnap=true;
    }

    // This function is called when the Record Switch is changed
    private void record_SwitchChanged() {
        try {
            mutex_on_mIsRecording.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //mIsRecording = isChecked;
        // Start Recording
        Log.v(TAG,"record_SwitchChanged to "+mIsRecording);
        if (mIsRecording) {
            // Generate a new date number to create a new group of files
            Calendar rightNow = Calendar.getInstance();
            int hour = rightNow.get(Calendar.HOUR_OF_DAY);
            int minute = rightNow.get(Calendar.MINUTE);
            int sec = rightNow.get(Calendar.SECOND);
            int milliSec = rightNow.get(Calendar.MILLISECOND);
            mNowTimeString = "" + (int)(1000000 * hour + 10000 * minute + 100 * sec +
                    (float)milliSec / 10.0);
            Log.v(TAG,"now: "+mNowTimeString);
            mNumberOfFilesWritten = 0;
            // Enable snapshot button
            //mTakeSnapButton.setEnabled(true);
        }
        // Finish Recording
        else {
            // Disable snapshot button
            //mTakeSnapButton.setEnabled(false);
            // Display a waiting progress bar
            //mWaitingTextView.setText(R.string.waitSavingScan);
            //mWaitingLinearLayout.setVisibility(View.VISIBLE);
            // Background task for writing poses to file
            class SendCommandTask extends AsyncTask<Context, Void, Uri> {
                /** The system calls this to perform work in a worker thread and
                 * delivers it the parameters given to AsyncTask.execute() */
                @Override
                protected Uri doInBackground(Context... myAppContext) {

                    // Stop the Pose Recording, and write them to a file.
                    writePoseToFile(mNumPoseInSequence);
                    // If a snap has been asked just before, but not saved, ignore it, otherwise,
                    // it will be saved at the end dof this function, and the 2nd archive will override
                    // the first.
                    mTimeToTakeSnap = false;
                    mNumPoseInSequence = 0;
                    mPoseOrientationBuffer.clear();
                    mPoseOrientationBuffer.clear();
                    mPoseTimestampBuffer.clear();

                    // Zip all the files from this sequence
                    String zipFilename = mSaveDirAbsPath + "TangoData_" + mNowTimeString +
                            "_" + mFilenameBuffer.size() + "files.zip";
                    String[] fileList = mFilenameBuffer.toArray(new String[mFilenameBuffer.size()]);
                    ZipWriter zipper = new ZipWriter(fileList, zipFilename);
                    zipper.zip();

                    // Delete the data files now that they are archived
                    for (String s : mFilenameBuffer) {
                        File file = new File(s);
                        boolean deleted = file.delete();
                        if (!deleted) {
                            Log.w(TAG, "File \"" + s + "\" not deleted\n");
                        }
                    }

                    mFilenameBuffer.clear();

                    // Send the zip file to another app
                    File myZipFile = new File(zipFilename);

                    // was Uri instead of void
                    //return FileProvider.getUriForFile(myAppContext[0], "com.kitware." +
                    //        "tangoproject.paraviewtangorecorder.fileprovider", myZipFile);

                    return Uri.fromFile(myZipFile);
                }

                /** The system calls this to perform work in the UI thread and delivers
                 * the result from doInBackground() */
/*
                @Override
                protected void onPostExecute(Uri fileURI) {

                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileURI);
                    shareIntent.setType("application/zip");
                    startActivity(Intent.createChooser(shareIntent, "Send Scan To..."));
                    mWaitingLinearLayout.setVisibility(View.GONE);
                    //TODO: Upload to Firebase
                }
                  */

            }
            new SendCommandTask().execute(this);

        }
        mutex_on_mIsRecording.release();

    }

    // This function writes the pose data and timestamps to .vtk files in binary
    private void writePoseToFile(int numPoints) {

        File mainDir = new File(mOutputFolder);
        if(!mainDir.exists()) {
            boolean created = mainDir.mkdir();
            if (created) {
                Log.i(TAG, "Folder: \"" + mOutputFolder + "\" created\n");
            }
        }

        File dir = new File(mSaveDirAbsPath);
        if(!dir.exists()) {
            boolean created = dir.mkdir();
            if (created) {
                Log.i(TAG, "Folder: \"" + mSaveDirAbsPath + "\" created\n");
            }
        }
        String poseFileName = "pc_" + mNowTimeString + "_poses.vtk";
        mFilenameBuffer.add(mSaveDirAbsPath + poseFileName);
        File file = new File(dir, poseFileName);

        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(file)));

            out.write(("# vtk DataFile Version 3.0\n" +
                    "vtk output\n" +
                    "BINARY\n" +
                    "DATASET POLYDATA\n" +
                    "POINTS " + numPoints + " float\n").getBytes());

            for (int i = 0; i < numPoints; i++) {
                out.writeFloat(mPosePositionBuffer.get(i)[0]);
                out.writeFloat(mPosePositionBuffer.get(i)[1]);
                out.writeFloat(mPosePositionBuffer.get(i)[2]);
            }

            out.write(("\nLINES 1 " + String.valueOf(numPoints + 1) + "\n").getBytes());
            out.writeInt(numPoints);
            for (int i = 0; i < numPoints; i++) {
                out.writeInt(i);
            }

            out.write(("\nFIELD FieldData 1\n" +
                    "Cam2Dev_transform 16 1 float\n").getBytes());
            for (int i = 0; i < cam2dev_Transform.length; i++) {
                out.writeFloat(cam2dev_Transform[i]);
            }

            out.write(("\nPOINT_DATA " + String.valueOf(numPoints) + "\n" +
                    "FIELD FieldData 2\n" +
                    "orientation 4 " + String.valueOf(numPoints) + " float\n").getBytes());

            for (int i = 0; i < numPoints; i++) {
                out.writeFloat(mPoseOrientationBuffer.get(i)[0]);
                out.writeFloat(mPoseOrientationBuffer.get(i)[1]);
                out.writeFloat(mPoseOrientationBuffer.get(i)[2]);
                out.writeFloat(mPoseOrientationBuffer.get(i)[3]);
            }

            out.write(("\ntimestamp 1 " + String.valueOf(numPoints) + " float\n").getBytes());
            for (int i = 0; i < numPoints; i++) {
                out.writeFloat(mPoseTimestampBuffer.get(i));
            }

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
