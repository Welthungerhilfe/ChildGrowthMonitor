
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

package de.welthungerhilfe.cgm.scanner.activities;

import android.Manifest;
import android.app.Activity;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;

import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.ViewHolder;
import com.projecttango.tangosupport.TangoSupport;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.fragments.BabyBack0Fragment;
import de.welthungerhilfe.cgm.scanner.fragments.BabyBack1Fragment;
import de.welthungerhilfe.cgm.scanner.fragments.BabyFront0Fragment;
import de.welthungerhilfe.cgm.scanner.fragments.BabyInfantChooserFragment;
import de.welthungerhilfe.cgm.scanner.fragments.InfantBackFragment;
import de.welthungerhilfe.cgm.scanner.fragments.InfantFrontFragment;
import de.welthungerhilfe.cgm.scanner.fragments.InfantFullFrontFragment;
import de.welthungerhilfe.cgm.scanner.fragments.InfantTurnFragment;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.tango.CameraSurfaceRenderer;
import de.welthungerhilfe.cgm.scanner.tango.ModelMatCalculator;
import de.welthungerhilfe.cgm.scanner.tango.OverlaySurface;
import de.welthungerhilfe.cgm.scanner.tango.TextureMovieEncoder;
import de.welthungerhilfe.cgm.scanner.utils.ZipWriter;

import static com.projecttango.tangosupport.TangoSupport.initialize;

public class RecorderActivity extends Activity {

    private static GLSurfaceView mCameraSurfaceView;
    private static OverlaySurface mOverlaySurfaceView;

    private TextView mDisplayTextView;

    // this is static so it survives activity restarts
    private static TextureMovieEncoder sVideoEncoder = new TextureMovieEncoder();
    private CameraSurfaceRenderer mRenderer;

    private Tango mTango;
    private TangoConfig mConfig;
    private boolean mIsConnected = false;

    private static final int SECS_TO_MILLISECS = 1000;


    private static final String TAG = RecorderActivity.class.getSimpleName();
    private static final int INVALID_TEXTURE_ID = 0;
    private static final String sTimestampFormat = "Timestamp: %f";

    // NOTE: Naming indicates which thread is in charge of updating this variable.
    private int mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
    private AtomicBoolean mIsFrameAvailableTangoThread = new AtomicBoolean(false);

    private int mDisplayRotation = Surface.ROTATION_0;

    private Semaphore mutex_on_mIsRecording;
    private File mVideoOutputFile;

    private String mPointCloudFilename;
    private int mNumberOfFilesWritten;
    private ArrayList<float[]> mPosePositionBuffer;
    private ArrayList<float[]> mPoseOrientationBuffer;
    private ArrayList<Float> mPoseTimestampBuffer;
    private ArrayList<String> mPointCloudFilenameBuffer;
    private float[] cam2dev_Transform;
    private int mNumPoseInSequence;
    private Boolean mTimeToTakeSnap;

    private int mValidPoseCallbackCount;
    private int mPreviousPoseStatus;
    private float mDeltaTime;
    private float mPosePreviousTimeStamp;
    private float mPointCloudPreviousTimeStamp;
    private float mCurrentTimeStamp;

    private int mPointCloudCallbackCount;

    boolean mIsRecording;

    private Person person;
    private Measure measure;
    private LinearLayout container;
    private FloatingActionButton fab;

    private File mScanArtefactsOutputFolder;
    private String mPointCloudSaveFolderPath;
    private long mNowTime;
    private String mNowTimeString;
    private String mQrCode;

    private int mScanningWorkflowStep = 0;

    private final String BABY_FRONT_0 = "baby_front_0";
    private final String BABY_FRONT_1 = "baby_front_1";
    private final String BABY_BACK_0 = "baby_back_0";
    private final String BABY_BACK_1 = "baby_back_1";
    private BabyFront0Fragment babyFront0Fragment;
    private BabyBack0Fragment babyBack0Fragment;
    private BabyBack1Fragment babyBack1Fragment;

    private final String INFANT_FULL_FRONT = "infant_full_front";
    private final String INFANT_TURN = "infant_turn";
    private final String INFANT_FRONT = "infant_front";
    private final String INFANT_BACK = "infant_back";
    private InfantFullFrontFragment infantFullFrontFragment;
    private InfantTurnFragment infantTurnFragment;
    private InfantFrontFragment infantFrontFragment;
    private InfantBackFragment infantBackFragment;


    // TODO: make available in Settings
    private boolean onboarding = true;

    private boolean Verbose = true;
    private FirebaseAnalytics mFirebaseAnalytics;

    // Workflow
    public void gotoNextStep(int babyInfantChoice) {
        mScanningWorkflowStep = babyInfantChoice+1;

        gotoNextStep();
    }
    public void gotoNextStep() {
        // mScanningWorkflowStep 0 = choose between infant standing up and baby lying down
        // mScanningWorkflowStep 100+ = baby
        // mScanningWorkflowStep 200+ = infant
        // onBoarding steps are odd, scanning process steps are even 0,2,4,6
        // TODO steps are done when a certain number of points with certain confidence have been collected

        // TODO better icon for fab

        FragmentTransaction ft = getFragmentManager().beginTransaction();

        if (Verbose) Log.v("ScanningWorkflow","starting mScanningWorkflowStep: "+ mScanningWorkflowStep);

        if (mScanningWorkflowStep == AppConstants.CHOOSE_BABY_OR_INFANT) {
            measure = new Measure();
            measure.setDate(mNowTime);
            BabyInfantChooserFragment babyInfantChooserFragment = new BabyInfantChooserFragment();
            ft.add(R.id.container, babyInfantChooserFragment);
            ft.commit();

        } else if (mScanningWorkflowStep == AppConstants.BABY_FULL_BODY_FRONT_ONBOARDING) {
            mOverlaySurfaceView.setMode(OverlaySurface.BABY_OVERLAY);
            babyFront0Fragment = new BabyFront0Fragment();
            ft.replace(R.id.container, babyFront0Fragment, BABY_FRONT_0);
            ft.commit();
            measure.setType("b_v1.0");

        } else if (mScanningWorkflowStep == AppConstants.BABY_FULL_BODY_FRONT_SCAN) {
            mDisplayTextView.setText(R.string.baby_full_body_front_scan_text);
            //TODO:mOverlaySurfaceView.setIsOverlayScanningProcess(true);
            resumeScan();

        } else if (mScanningWorkflowStep == AppConstants.BABY_FULL_BODY_FRONT_RECORDING) {
            fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorPink)));
            mIsRecording = true;

        } else if (mScanningWorkflowStep == AppConstants.BABY_LEFT_RIGHT_ONBOARDING) {
            mDisplayTextView.setText(R.string.empty_string);
            fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorGreen)));
            babyBack0Fragment = new BabyBack0Fragment();
            ft.replace(R.id.container, babyBack0Fragment, BABY_BACK_0);
            ft.commit();
            pauseScan();


        } else if (mScanningWorkflowStep == AppConstants.BABY_LEFT_RIGHT_SCAN) {
            mDisplayTextView.setText(R.string.baby_left_right_scan_text);
            resumeScan();

        } else if (mScanningWorkflowStep == AppConstants.BABY_LEFT_RIGHT_RECORDING) {
            fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorPink)));
            mIsRecording = true;

        } else if (mScanningWorkflowStep == AppConstants.BABY_FULL_BODY_BACK_ONBOARDING) {
            mDisplayTextView.setText(R.string.empty_string);
            fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorGreen)));
            babyBack1Fragment = new BabyBack1Fragment();
            ft.replace(R.id.container, babyBack1Fragment, BABY_BACK_1);
            ft.commit();
            pauseScan();

        } else if (mScanningWorkflowStep == AppConstants.BABY_FULL_BODY_BACK_SCAN) {
            mDisplayTextView.setText(R.string.baby_full_body_back_scan_text);
            resumeScan();

        } else if (mScanningWorkflowStep == AppConstants.BABY_FULL_BODY_BACK_RECORDING) {
            fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorPink)));
            mIsRecording = true;

/*

 */
        // INFANT
        } else if (mScanningWorkflowStep == AppConstants.INFANT_FULL_BODY_FRONT_ONBOARDING) {
            mOverlaySurfaceView.setMode(OverlaySurface.INFANT_OVERLAY);
            mDisplayTextView.setText(R.string.empty_string);
            infantFullFrontFragment = new InfantFullFrontFragment();
            ft.replace(R.id.container, infantFullFrontFragment, INFANT_FULL_FRONT);
            ft.commit();
            pauseScan();

        } else if (mScanningWorkflowStep == AppConstants.INFANT_FULL_BODY_FRONT_SCAN) {
            mDisplayTextView.setText(R.string.infant_full_body_front_scan_text);
            resumeScan();

        } else if (mScanningWorkflowStep == AppConstants.INFANT_FULL_BODY_FRONT_RECORDING) {
            fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorPink)));
            mIsRecording = true;

        } else if (mScanningWorkflowStep == AppConstants.INFANT_360_TURN_ONBOARDING) {
            mDisplayTextView.setText(R.string.empty_string);
            fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorGreen)));
            infantTurnFragment = new InfantTurnFragment();
            ft.replace(R.id.container, infantTurnFragment, INFANT_TURN);
            ft.commit();
            pauseScan();

        } else if (mScanningWorkflowStep == AppConstants.INFANT_360_TURN_SCAN) {
            mDisplayTextView.setText(R.string.infant_360_turn_scan_text);
            resumeScan();
            mOverlaySurfaceView.setMode(OverlaySurface.INFANT_CLOSE_DOWN_UP_OVERLAY);

        } else if (mScanningWorkflowStep == AppConstants.INFANT_360_TURN_RECORDING) {
            fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorPink)));
            mOverlaySurfaceView.setMode(OverlaySurface.NO_OVERLAY);
            mIsRecording = true;

        } else if (mScanningWorkflowStep == AppConstants.INFANT_FRONT_UP_DOWN_ONBOARDING) {
            mDisplayTextView.setText(R.string.empty_string);
            fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorGreen)));
            infantFrontFragment = new InfantFrontFragment();
            ft.replace(R.id.container, infantFrontFragment, INFANT_FRONT);
            ft.commit();
            pauseScan();

        } else if (mScanningWorkflowStep == AppConstants.INFANT_FRONT_UP_DOWN_SCAN) {
            mDisplayTextView.setText(R.string.infant_front_up_down_scan_text);
            mOverlaySurfaceView.setMode(OverlaySurface.INFANT_CLOSE_DOWN_UP_OVERLAY);
            resumeScan();

        } else if (mScanningWorkflowStep == AppConstants.INFANT_FRONT_UP_DOWN_RECORDING) {
            fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorPink)));
            mOverlaySurfaceView.setMode(OverlaySurface.NO_OVERLAY);
            mIsRecording = true;

        } else if (mScanningWorkflowStep == AppConstants.INFANT_BACK_UP_DOWN_ONBOARDING) {
            mDisplayTextView.setText(R.string.empty_string);
            fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorGreen)));
            infantBackFragment = new InfantBackFragment();
            ft.replace(R.id.container, infantBackFragment, INFANT_BACK);
            ft.commit();
            pauseScan();

        } else if (mScanningWorkflowStep == AppConstants.INFANT_BACK_UP_DOWN_SCAN) {
            mDisplayTextView.setText(R.string.infant_back_up_down_scan_text);
            mOverlaySurfaceView.setMode(OverlaySurface.INFANT_CLOSE_DOWN_UP_OVERLAY);
            resumeScan();

        } else if (mScanningWorkflowStep == AppConstants.INFANT_BACK_UP_DOWN_RECORDING) {
            fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorPink)));
            mOverlaySurfaceView.setMode(OverlaySurface.NO_OVERLAY);
            mIsRecording=true;

            
        } else {
            measurementFinished();
            Log.v(TAG,"ScanningWorkflow finished for person "+person.getSurname());
            Intent i = new Intent(getApplicationContext(), CreateDataActivity.class);
            i.putExtra(AppConstants.EXTRA_PERSON, person);
            startActivity(i);
        }
        mScanningWorkflowStep++;
        if (!onboarding) mScanningWorkflowStep++;
        if (Verbose) Log.v("ScanningWorkflow","next mScanningWorkflowStep: "+ mScanningWorkflowStep);
    }

    private ViewHolder scanDialogViewHolder;
    private DialogPlus scanResultDialog;

    private void waitScanResult() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                showScanResultDialog();
            }
        };

        new Timer().schedule(task, 3000);
    }

    private void showScanResultDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView txtHeight = scanResultDialog.getHolderView().findViewById(R.id.txtHeight);

                txtHeight.setText(Integer.toString(24));

                scanResultDialog.show();
            }
        });
    }

    private void pauseScan() {
        container.setVisibility(View.VISIBLE);
        mCameraSurfaceView.setVisibility(View.INVISIBLE);
        mOverlaySurfaceView.setVisibility(View.INVISIBLE);
        mDisplayTextView.setVisibility(View.INVISIBLE);
        fab.setVisibility(View.INVISIBLE);
    }

    private void resumeScan() {
        container.setVisibility(View.INVISIBLE);
        mCameraSurfaceView.setVisibility(View.VISIBLE);
        mOverlaySurfaceView.setVisibility(View.VISIBLE);
        mDisplayTextView.setVisibility(View.VISIBLE);
        fab.setVisibility(View.VISIBLE);
    }

    /**
     * Overridable  method to get layout id.  Any provided layout needs to include
     * the same views (or compatible) as active_play_movie_surface
     *
     */
    protected int getContentViewId() {
        return R.layout.activity_recorder;
    }

    /*
     * get path to video output
     */
    protected File getVideoOutputFile ()
    {
        return mVideoOutputFile;
    }

    protected void measurementFinished ()
    {

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        person = (Person) getIntent().getSerializableExtra(AppConstants.EXTRA_PERSON);
        if (person == null) Log.e(TAG,"person was null!");
        setContentView(getContentViewId());
        gotoNextStep();

        //ButterKnife.bind(this);

        mCameraSurfaceView = findViewById(R.id.surfaceview);
        mOverlaySurfaceView = findViewById(R.id.overlaySurfaceView);
        mDisplayTextView = findViewById(R.id.display_textview);
        container = findViewById(R.id.container);



        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            //Activity.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        fab = findViewById(R.id.fab_scan_result);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsRecording) {
                    mIsRecording = false;
                    record_SwitchChanged();
                    fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorGreen)));
                }
                gotoNextStep();
            }
        });

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

        mPointCloudFilename = "";
        mNumberOfFilesWritten = 0;
        mPosePositionBuffer = new ArrayList<float[]>();
        mPoseOrientationBuffer = new ArrayList<float[]>();
        mPoseTimestampBuffer = new ArrayList<Float>();
        mPointCloudFilenameBuffer = new ArrayList<String>();
        mNumPoseInSequence = 0;
        mPointCloudCallbackCount = 0;
        mutex_on_mIsRecording = new Semaphore(1,true);
        mIsRecording = false;

        mNowTime = System.currentTimeMillis();
        mNowTimeString = String.valueOf(mNowTime);
        mQrCode = person.getQrcode();
        File extFileDir = getExternalFilesDir(Environment.getDataDirectory().getAbsolutePath());
        File personalFilesDir = new File(extFileDir,mQrCode+"/");
        if(!personalFilesDir.exists()) {
            boolean created = personalFilesDir.mkdir();
            if (created) {
                Log.i(TAG, "Folder: \"" + personalFilesDir + "\" created\n");
            } else {
                Log.e(TAG,"Folder: \"" + personalFilesDir + "\" could not be created!\n");
            }
        }
        mScanArtefactsOutputFolder  = new File(extFileDir,mQrCode+"/"+mNowTimeString+"/");
        // TODO Create when needed!
        if(!mScanArtefactsOutputFolder.exists()) {
            boolean created = mScanArtefactsOutputFolder.mkdir();
            if (created) {
                Log.i(TAG, "Folder: \"" + mScanArtefactsOutputFolder + "\" created\n");
            } else {
                Log.e(TAG,"Folder: \"" + mScanArtefactsOutputFolder + "\" could not be created!\n");
            }
        }
        mVideoOutputFile = new File(mScanArtefactsOutputFolder,mQrCode+".mp4");
        mPointCloudSaveFolderPath = mScanArtefactsOutputFolder.getAbsolutePath()+"/pc/";
        Log.v(TAG,"mPointCloudSaveFolderPath: "+mPointCloudSaveFolderPath);
        // must be called after setting mVideoOutputFile and sVideoEncoder was created!
        setupRenderer();
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
        mRenderer = new CameraSurfaceRenderer(sVideoEncoder, mVideoOutputFile, new CameraSurfaceRenderer.RenderCallback() {

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
                            //sVideoEncoder.setTextureId(mConnectedTextureIdGlThread);
                            mTango.connectTextureId(TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                    mConnectedTextureIdGlThread);

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
        mCameraSurfaceView.onResume();
        Log.d(TAG, "onResume complete: " + this);
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
            }

            @Override
            public void onPointCloudAvailable(final TangoPointCloudData pointCloudData) {
                StringBuilder stringBuilder = new StringBuilder();
                //stringBuilder.append("Point count: " + pointCloudData.numPoints);
                float[] average = calculateAveragedDepth(pointCloudData.points, pointCloudData.numPoints);
                stringBuilder.append("center depth (m): " + average[0]);
                stringBuilder.append(" confidence: " + average[1]);
                final String pointCloudString = stringBuilder.toString();
                //Log.i(TAG, pointCloudString);
                /*
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDisplayTextView.setText(pointCloudString);
                    }
                });
                */
                mOverlaySurfaceView.setDistance(average[0]);
                mOverlaySurfaceView.setConfidence(average[1]);


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

                mCurrentTimeStamp = (float) pointCloudData.timestamp;
                final float frameDelta = (mCurrentTimeStamp - mPointCloudPreviousTimeStamp)
                        * SECS_TO_MILLISECS;
                mPointCloudPreviousTimeStamp = mCurrentTimeStamp;
                mPointCloudCallbackCount++;

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
                    // TODO: be less lame
                    //sVideoEncoder.setTextureId(mConnectedTextureIdGlThread);
                    // we have no surfacetexture? sVideoEncoder.frameAvailable();
                }
            }
        });
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
        /*
        mIsRecording = !mIsRecording;
        if (mIsRecording) {
            fab.setColorFilter(Color.RED);
        } else {
            fab.setColorFilter(Color.GREEN);
        }
        record_SwitchChanged();
        mCameraSurfaceView.queueEvent(new Runnable() {
            @Override public void run() {
                // notify the renderer that we want to change the encoder's state
                mRenderer.changeRecordingState(mIsRecording);
            }
        });
        */
        Toast.makeText(this.getApplicationContext(), "Recording: "+mIsRecording+"!! :)", Toast.LENGTH_SHORT).show();
        return true;
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
            Toast.makeText(getApplicationContext(), R.string.exception_tango_error,
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
            Toast.makeText(getApplicationContext(), R.string.exception_tango_error,
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


    // This function writes the XYZ points to .vtk files in binary
    private void writePointCloudToFile(TangoPointCloudData pointCloudData,
                                       ArrayList<TangoCoordinateFramePair> framePairs) {


        ByteBuffer myBuffer = ByteBuffer.allocate(pointCloudData.numPoints * 4 * 4);
        myBuffer.order(ByteOrder.LITTLE_ENDIAN);

        myBuffer.asFloatBuffer().put(pointCloudData.points);

        if(!mScanArtefactsOutputFolder.exists()) {
            boolean created = mScanArtefactsOutputFolder.mkdir();
            if (created) {
                Log.i(TAG, "Folder: \"" + mScanArtefactsOutputFolder.getAbsolutePath() + "\" created\n");
            }
        }

        File dir = new File(mPointCloudSaveFolderPath);
        if(!dir.exists()) {
            boolean created = dir.mkdir();
            if (created) {
                Log.i(TAG, "Folder: \"" + mPointCloudSaveFolderPath + "\" created\n");
            }
        }

        mPointCloudFilename = "pc_" +mQrCode+"_" + mNowTimeString + "_" + String.format("%03d", mNumberOfFilesWritten) +
                ".vtk";
        mPointCloudFilenameBuffer.add(mPointCloudSaveFolderPath + mPointCloudFilename);
        Log.v(TAG,"added pointcloud "+mPointCloudSaveFolderPath + mPointCloudFilename);
        File file = new File(dir, mPointCloudFilename);


        try {

            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(file)));
            float confidence;

            out.write(("# vtk DataFile Version 3.0\n" +
                    "vtk output\n" +
                    "BINARY\n" +
                    "DATASET POLYDATA\n" +
                    "POINTS " + pointCloudData.numPoints + " float\n").getBytes());

            for (int i = 0; i < pointCloudData.numPoints; i++) {

                out.writeFloat(myBuffer.getFloat(4 * i * 4));
                out.writeFloat(myBuffer.getFloat((4 * i + 1) * 4));
                out.writeFloat(myBuffer.getFloat((4 * i + 2) * 4));
                confidence = myBuffer.getFloat((4 * i + 3) * 4);
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


    // This function is called when the Record Switch is changed
    private void record_SwitchChanged() {
        try {
            mutex_on_mIsRecording.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Start Recording
        Log.v(TAG,"record_SwitchChanged to "+mIsRecording);
        if (mIsRecording) {
            Log.v(TAG,"now: "+mNowTimeString);
            mNumberOfFilesWritten = 0;
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
                    String zipFilename = mPointCloudSaveFolderPath + "/TangoData_" +mQrCode+"_"+ mNowTimeString +
                            "_" + mPointCloudFilenameBuffer.size() + "files.zip";
                    String[] fileList = mPointCloudFilenameBuffer.toArray(new String[mPointCloudFilenameBuffer.size()]);
                    ZipWriter zipper = new ZipWriter(fileList, zipFilename);
                    zipper.zip();

                    // Delete the data files now that they are archived
                    for (String s : mPointCloudFilenameBuffer) {
                        File file = new File(s);
                        boolean deleted = file.delete();
                        if (!deleted) {
                            Log.w(TAG, "File \"" + s + "\" not deleted\n");
                        }
                    }

                    mPointCloudFilenameBuffer.clear();

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

        File dir = new File(mPointCloudSaveFolderPath);
        if(!dir.exists()) {
            boolean created = dir.mkdir();
            if (created) {
                Log.i(TAG, "Folder: \"" + mPointCloudSaveFolderPath + "\" created\n");
            }
        }
        String poseFileName = "pc_" +mQrCode+"_"+ mNowTimeString + "_poses.vtk";
        mPointCloudFilenameBuffer.add(mPointCloudSaveFolderPath + poseFileName);
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
