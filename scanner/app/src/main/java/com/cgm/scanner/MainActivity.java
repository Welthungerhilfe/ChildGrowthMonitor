package com.cgm.scanner;/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Copyright 2018 Welthungerhilfe for parts not from google-tango-samples
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.google.atap.tangoservice.experimental.TangoImageBuffer;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.AddTrace;
import com.google.firebase.perf.metrics.Trace;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.projecttango.tangosupport.TangoSupport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.projecttango.tangosupport.TangoSupport.*;
import static java.lang.Math.abs;

/**
 * This is a stripped-down simple example that shows how to use the Tango APIs to render the Tango
 * RGB camera into an OpenGL texture.
 * It creates a standard Android {@code GLSurfaceView} with a simple renderer and connects to
 * the Tango service with the appropriate configuration for video rendering.
 * Each time a new RGB video frame is available through the Tango APIs, it is updated to the
 * OpenGL texture and the corresponding timestamp is printed on the logcat and on screen.
 * <p/>
 * Only the minimum OpenGL code necessary to understand how to render the specific texture format
 * produced by the Tango RGB camera is provided. You can find these details in
 * {@code ScanVideoRenderer}.
 * If you're looking for an example that also renders an actual 3D object with an augmented reality
 * effect, see java_augmented_reality_example and/or java_augmented_reality_opengl_example.
 */
public class MainActivity extends ActionBarActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int INVALID_TEXTURE_ID = 0;
    private static final String sTimestampFormat = "Timestamp: %f";

    private GLSurfaceView mSurfaceView;
    private ScanVideoRenderer mRenderer;
    private TextView mDisplayTextView;

    private Tango mTango;
    private TangoConfig mConfig;
    private boolean mIsConnected = false;

    // NOTE: Naming indicates which thread is in charge of updating this variable.
    private int mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
    private AtomicBoolean mIsFrameAvailableTangoThread = new AtomicBoolean(false);

    private int mDisplayRotation = 0;

    public Bitmap mInputBitmap;
    private boolean inferenceReady = false;
    private boolean ready = false;

    private SQLiteDatabase mDb;
    private ChildGrowthDbHelper dbHelper;

    FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();

    private FirebaseStorage mStorage;
    private StorageReference mStorageRef;
    private boolean mDownloadModelUpdate = true;
    // TODO: currently needs to be initialized as true or download starts before update check successful
    private boolean mDownloadingModelUpdate = true;
    private String mCurrentModelMD5Hash;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private Predict predict;
    private static final String MODEL_FILE = "/frozen_resnet_50.pb";

    private Executor executor = Executors.newSingleThreadExecutor();

    private volatile TangoImageBuffer[] mCurrentImageBuffer = new TangoImageBuffer[10];
    private int mCurrentImageBufferPos = 0;
    private Context mContext;

    // TODO: Action Menu does not work wit GlSurfaceView
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Log.v(TAG, "onCreateOptionsMenu called");
        getMenuInflater().inflate(R.menu.main_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();

        if (itemThatWasClickedId == R.id.action_predictions) {
            Intent i = new Intent(getApplicationContext(), PredictionsList.class);
            startActivity(i);
            return true;
        }

        if (itemThatWasClickedId == R.id.action_logout) {
            Intent i = new Intent(getApplicationContext(), GoogleSignInActivity.class);
            i.putExtra("signout", true);
            startActivity(i);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Intent i = new Intent(this, PredictionsList.class);
        //startActivity(i);
        //Toast.makeText(this.getApplicationContext(), "Touch!! :)", Toast.LENGTH_SHORT).show();
        if (!inferenceReady) {
            return true;
        }
        if (!ready) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDisplayTextView.setText("scanning...");
                }
            });
            ready = true;
        }
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

    private void loadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "OnlineModelUpdateCheck: running loadModel");
                final Trace loadModelTrace = FirebasePerformance.getInstance().newTrace("loadModelTrace");
                loadModelTrace.start();
                try {
                    final File localModelFile = new File(getFilesDir() + MODEL_FILE);
                    // TODO: Try to start with current model when offline!
                    Log.v(TAG, "OnlineModelUpdateCheck: trying to get Online Model Metadata");
                    //Log.v(TAG, "OnlineModelUpdateCheck: getMaxDownloadRetryTimeMillis was: "+mStorage.getMaxDownloadRetryTimeMillis()+" now: 1m (60000)");
                    Log.v(TAG, "OnlineModelUpdateCheck: getMaxOperationRetryTimeMillis was: "+mStorage.getMaxOperationRetryTimeMillis()+" now: 10s (10000)");
                    //Log.v(TAG, "OnlineModelUpdateCheck: getMaxUploadRetryTimeMillis: "+mStorage.getMaxUploadRetryTimeMillis()+" now: 2m (120000)");
                    //mStorage.setMaxDownloadRetryTimeMillis(60000);  // wait 1 min for downloads
                    mStorage.setMaxOperationRetryTimeMillis(10000);  // wait 10s for normal ops
                    //mStorage.setMaxUploadRetryTimeMillis(120000);  // wait 2 mins for uploads

                    StorageReference modelRef = mStorage.getReferenceFromUrl("gs://child-growth-monitor.appspot.com/assets/frozen_resnet_50.pb");//"gs://child-growth-monitor.appspot.com/"+MODEL_FILE);
                    modelRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                        @Override
                        public void onSuccess(StorageMetadata storageMetadata) {
                            Log.v(TAG, "OnlineModelUpdateCheck: Online Metadata received");
                            mCurrentModelMD5Hash = storageMetadata.getMd5Hash().trim();
                            String localModelMD5Hash = getMD5(getFilesDir() + MODEL_FILE).trim();
                            Log.i(TAG, "current MD5 Hash: " + mCurrentModelMD5Hash);
                            Log.i(TAG, "Local MD5 Hash:   " + localModelMD5Hash);
                            loadModelTrace.incrementCounter(localModelMD5Hash);
                            //if model files differ then download latest
                            if (mCurrentModelMD5Hash.compareTo(localModelMD5Hash) != 0) {
                                mDownloadModelUpdate = true;
                                mDownloadingModelUpdate = false;
                                loadModelTrace.incrementCounter("modelupdate_needed");
                            } else {
                                mDownloadModelUpdate = false;
                                loadModelTrace.incrementCounter("model_uptodate");
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Uh-oh, an error occurred!
                            Log.v(TAG, "OnlineModelUpdateCheck: failed to get Metadata");
                            mDownloadModelUpdate = false;
                            Log.e(TAG, "Error checking for model updates");
                            Log.e(TAG, exception.getMessage(), exception);
                            loadModelTrace.incrementCounter("model_download_failed");
                        }
                    });

                    Log.v(TAG, "mDownloadModelUpdate: " + mDownloadModelUpdate);

                    while (mDownloadModelUpdate) {
                        Log.v(TAG, "OnlineModelUpdateCheck: "+ mDownloadModelUpdate);
                        if (!mDownloadingModelUpdate) {
                            Log.v(TAG, "OnlineModelUpdateCheck: downloading");
                            mDownloadingModelUpdate = true;
                            downloadModel(modelRef, localModelFile);
                        }
                        Log.v(TAG, "model download not ready, waiting...");
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking for model updates");
                    Log.e(TAG, e.getMessage(), e);
                    loadModelTrace.incrementCounter("exception_while_updatecheck");
                    mDownloadModelUpdate = false;
                }

/*
                if (mCurrentModelMD5Hash != null && localModelFile.exists()) {
                    String localModelMD5Hash = getMD5(getFilesDir() + MODEL_FILE).trim();
                    Log.i(TAG, "current MD5 Hash: "+mCurrentModelMD5Hash);
                    Log.i(TAG, "local MD5 Hash:   "+localModelMD5Hash);
                } else {
                    Log.e(TAG, "Model update check not successful!!");
                    return;
                }
*/
                try {
                    predict = new Predict(getApplicationContext().getAssets(),
                            getFilesDir()+MODEL_FILE);
                    inferenceReady = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDisplayTextView.setText("Touch to start scanning!");
                        }
                    });

                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
                loadModelTrace.stop();
            }
        });
    }

    private void downloadModel(StorageReference modelRef, final File modelFile) {
        Log.i(TAG, "Downloading model");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDisplayTextView.setText("Downloading brains...");
            }
        });

        modelRef.getFile(modelFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Log.i(TAG, "Successfully downloaded model to " + modelFile.toString());
                        mDownloadModelUpdate = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mDisplayTextView.setText("Download successful...");
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Handle failed download
                // ...
                Log.e(TAG, "Error downloading model file");
                Log.e(TAG, e.getMessage(), e);
            }
        });
        // TODO: File gets overwritten at startup if not read only ...but read only should not be necessary?
        modelFile.setReadOnly();
    }

    private String getMD5(String filePath) {
        String base64Digest = "";
        try {
            InputStream input = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            MessageDigest md5Hash = MessageDigest.getInstance("MD5");
            int numRead = 0;
            while (numRead != -1) {
                numRead = input.read(buffer);
                if (numRead > 0) {
                    md5Hash.update(buffer, 0, numRead);
                }
            }
            input.close();
            byte[] md5Bytes = md5Hash.digest();
            base64Digest = Base64.encodeToString(md5Bytes, Base64.DEFAULT);

       /*for (byte md5Byte : md5Bytes) {
            returnVal += Integer.toString((md5Byte & 0xff) + 0x100, 16).substring(1);
        }*/
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return base64Digest;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // TODO: change from actionbar to toolbar?
        mContext = getApplicationContext();

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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_scan_result);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                Intent i = new Intent(getApplicationContext(), ScanResultActivity.class);
                startActivity(i);
            }
        });

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };

        mDisplayTextView = (TextView) findViewById(R.id.display_textview);
        mSurfaceView = (GLSurfaceView) findViewById(R.id.surfaceview);
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
        mInputBitmap = Bitmap.createBitmap(1152, 648, Bitmap.Config.ARGB_8888);
        dbHelper = new ChildGrowthDbHelper(this);
        mDb = dbHelper.getWritableDatabase();
        mStorage = FirebaseStorage.getInstance();
        mStorageRef = mStorage.getReference();
        loadModel();
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSurfaceView.onResume();

        // Set render mode to RENDERMODE_CONTINUOUSLY to force getting onDraw callbacks until the
        // Tango Service is properly set up and we start getting onFrameAvailable callbacks.
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Initialize Tango Service as a normal Android Service. Since we call mTango.disconnect()
        // in onPause, this will unbind Tango Service, so every time onResume gets called we
        // should create a new Tango object.
        mTango = new Tango(MainActivity.this, new Runnable() {
            // Pass in a Runnable to be called from UI thread when Tango is ready; this Runnable
            // will be running on a new thread.
            // When Tango is ready, we can call Tango functions safely here only when there is no UI
            // thread changes involved.
            @Override
            public void run() {
                // Synchronize against disconnecting while the service is being used in
                // the OpenGL thread or in the UI thread.
                synchronized (MainActivity.this) {
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
        mSurfaceView.onPause();
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
            @Override
            public void onPoseAvailable(final TangoPoseData pose) {
                // We are not using TangoPoseData for this application.
            }

            @Override
            public void onPointCloudAvailable(final TangoPointCloudData pointCloudData) {
                Trace onPointCloudTrace = FirebasePerformance.getInstance().newTrace("pointcloud_trace");
                onPointCloudTrace.start();
                if (!inferenceReady) {
                    //Log.w(TAG, "model not loaded yet");
                    onPointCloudTrace.incrementCounter("inference_not_ready");
                    onPointCloudTrace.stop();
                    return;
                }
                // get PointCloud and Camera Data
                // TODO: remove again: ImageBuffer with 10 frames has no better sync than just last image
                double rgbTimestamp;
                double minDiff = 10000;
                double currentDiff;
                int bestTimestampPos = 0;
                for (int i=0;i<10;i++){
                    if (mCurrentImageBuffer[i] != null) {
                        currentDiff = mCurrentImageBuffer[i].timestamp - pointCloudData.timestamp;
                        currentDiff = abs(currentDiff);
                        if (currentDiff < minDiff) {
                            minDiff = currentDiff;
                            bestTimestampPos = i;
                            onPointCloudTrace.incrementCounter("min_diff_updated");
                        } else {
                            onPointCloudTrace.incrementCounter("min_diff_not_updated");
                        }
                    }
                }
                //Log.v(TAG, "Best timestamp Position "+bestTimestampPos+": minimum difference of Timestamps: "+minDiff);
                TangoImageBuffer imageBuffer = mCurrentImageBuffer[bestTimestampPos];
                if (imageBuffer == null) {
                    Log.w(TAG, "imageBuffer is null");
                    onPointCloudTrace.incrementCounter("imagebuffer_null");
                    onPointCloudTrace.stop();
                    return;
                }

                rgbTimestamp = imageBuffer.timestamp;

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
                    onPointCloudTrace.incrementCounter("depth_pose_invalid");
                }
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
                    onPointCloudTrace.incrementCounter("color_pose_invalid");
                }

                final long startTime = SystemClock.uptimeMillis();
                final long timestamp = System.currentTimeMillis();

                if (!ready) {
                    //Log.v(TAG, "User has not started");
                    onPointCloudTrace.incrementCounter("user_not_ready");
                    onPointCloudTrace.stop();
                    return;
                }

                //Frameset frameset = new Frameset(timestamp, oglTdepthPose, oglTcolorPose, pointCloudData, imageBuffer);
                Log.v(TAG, "Scan "+timestamp+" frame number: "+imageBuffer.frameNumber+" with timestamp: "+imageBuffer.timestamp+
                        " and pointCloud timestamp "+pointCloudData.timestamp+" with "+pointCloudData.numPoints+" points.");
                int[] pixels = convertYUV420_NV21toRGB8888(imageBuffer.data.array(), imageBuffer.width, imageBuffer.height);
                Bitmap inputBitmap = Bitmap.createBitmap(pixels, imageBuffer.width, imageBuffer.height, Bitmap.Config.ARGB_8888);

                /* Intent measurementIntent = new Intent(mContext, MeasurementService.class);
                measurementIntent.putExtra("timestamp", timestamp);
                //measurementIntent.putExtra("pointCloudData", pointCloudData);
                measurementIntent.putExtra("oglTdepthPose", oglTdepthPose);
                measurementIntent.putExtra("oglTcolorPose", oglTcolorPose);
                //measurementIntent.putExtra("bitmap", inputBitmap);
*/
                try {
                    MeasurementService measurementService = new MeasurementService();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDisplayTextView.setText("processing...");
                        }
                    });
                    measurementService.getFramesetResult(getApplicationContext(), predict, timestamp, inputBitmap, pointCloudData, oglTdepthPose, oglTcolorPose);

//                    mContext.startService(measurementIntent);
                } catch (Exception e) {
                    Log.e(TAG, "Error! Caught Exception invoking measurement");
                    Log.e(TAG, e.getMessage(), e);
                    ready = false;
                    onPointCloudTrace.incrementCounter("start_service_exception");
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDisplayTextView.setText("ready... touch for next scan");
                    }
                });
                ready = false;
                onPointCloudTrace.stop();
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
                    if (mSurfaceView.getRenderMode() != GLSurfaceView.RENDERMODE_WHEN_DIRTY) {
                        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
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
                    mSurfaceView.requestRender();
                }
            }
        });

        // TODO: Check if needed
        mTango.experimentalConnectOnFrameListener(TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                new Tango.OnFrameAvailableListener() {
                    @Override
                    public  void onFrameAvailable(TangoImageBuffer tangoImageBuffer, int i) {
                        if (!inferenceReady) {
                            return;
                        }
                        // ringbuffer with timestamps and data to use the Image nearest to PointCloud Timestamp
                        // TODO: remove again because no use for more operations?
                        mCurrentImageBuffer[mCurrentImageBufferPos] = copyImageBuffer(tangoImageBuffer);
                        // Log.v(TAG,"current Image Buffer Position: "+mCurrentImageBufferPos);
                        if (mCurrentImageBufferPos == 9) {
                            mCurrentImageBufferPos = 0;
                        } else {
                            mCurrentImageBufferPos++;
                        }
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
    }

    /**
     * Converts YUV420 NV21 to RGB8888
     *
     * @param data byte array on YUV420 NV21 format.
     * @param width pixels width
     * @param height pixels height
     * @return a RGB8888 pixels int array. Where each int is a pixels ARGB.
     */
    @AddTrace(name = "convertYUV420_NV21toRGB8888")
    public static int[] convertYUV420_NV21toRGB8888(byte [] data, int width, int height) {
        int size = width*height;
        int offset = size;
        int[] pixels = new int[size];
        int u, v, y1, y2, y3, y4;

        // i percorre os Y and the final pixels
        // k percorre os pixles U e V
        for(int i=0, k=0; i < size; i+=2, k+=2) {
            y1 = data[i  ]&0xff;
            y2 = data[i+1]&0xff;
            y3 = data[width+i  ]&0xff;
            y4 = data[width+i+1]&0xff;

            u = data[offset+k  ]&0xff;
            v = data[offset+k+1]&0xff;
            u = u-128;
            v = v-128;

            pixels[i  ] = convertYUVtoRGB(y1, u, v);
            pixels[i+1] = convertYUVtoRGB(y2, u, v);
            pixels[width+i  ] = convertYUVtoRGB(y3, u, v);
            pixels[width+i+1] = convertYUVtoRGB(y4, u, v);

            if (i!=0 && (i+2)%width==0)
                i+=width;
        }

        return pixels;
    }

    private static int convertYUVtoRGB(int y, int u, int v) {
        int r,g,b;

        r = y + (int)(1.402f*v);
        g = y - (int)(0.344f*u +0.714f*v);
        b = y + (int)(1.772f*u);
        r = r>255? 255 : r<0 ? 0 : r;
        g = g>255? 255 : g<0 ? 0 : g;
        b = b>255? 255 : b<0 ? 0 : b;
        return 0xff000000 | (b<<16) | (g<<8) | r;
    }

    private void setupRenderer() {
        mSurfaceView.setEGLContextClientVersion(2);
        mRenderer = new ScanVideoRenderer(getApplicationContext(), new ScanVideoRenderer.RenderCallback() {
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
                    synchronized (MainActivity.this) {
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
        mSurfaceView.setRenderer(mRenderer);
    }

    /**
     * Set the color camera background texture rotation and save the camera to display rotation.
     */
    private void setDisplayRotation() {
        Display display = getWindowManager().getDefaultDisplay();
        mDisplayRotation = display.getRotation();

        // We also need to update the camera texture UV coordinates. This must be run in the OpenGL
        // thread.
        mSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mIsConnected) {
                    mRenderer.updateColorCameraTextureUv(mDisplayRotation);
                }
            }
        });
    }

    /**
     * Display toast on UI thread.
     *
     * @param resId The resource id of the string resource to use. Can be formatted text.
     */
    private void showsToastAndFinishOnUiThread(final int resId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,
                        getString(resId), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}
