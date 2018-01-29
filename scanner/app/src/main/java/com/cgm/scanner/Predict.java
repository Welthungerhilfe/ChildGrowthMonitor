package com.cgm.scanner;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.util.Log;

import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by mmatiaschek on 17.09.2017.
 */

public class Predict {

    private static final String DEMO_FILE = "image.png";

    private static final String INPUT_NODE = "Placeholder:0";
    private static final String OUTPUT_NODE = "Sigmoid:0,pose/locref_pred/block4/BiasAdd:0";

    private static final int NUM_JOINTS = 14;
    private static final String TAG = "Predict";

    private String[] jointNames = {"ankle1",
            "knee1",
            "hip1",
            "hip2",
            "knee2",
            "ankle2",
            "wrist1",
            "elbow1",
            "shoulder1",
            "shoulder2",
            "elbow2",
            "wrist2",
            "chin",
            "fhead"};

    private TensorFlowInferenceInterface mInferenceInterface;
    private AssetManager mAssets;

    public Predict(AssetManager assetManager, String modelPath) {
        mInferenceInterface = new TensorFlowInferenceInterface(assetManager, modelPath);
        mAssets = assetManager;
    }

    public Bitmap getBitmapFromAsset(String filePath) {

        InputStream istr;
        Bitmap bitmap = null;

        try {
            istr = mAssets.open(filePath);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            // handle exception
            Log.e (TAG, "Loading failed: " + e.getMessage());
            Log.e(TAG, e.getMessage(), e);
        }

        return bitmap;
    }

    // TODO return scoremap
    public List <Recognition> recognize(Bitmap bitmap) {
        Trace recognizeTrace = FirebasePerformance.getInstance().newTrace("recognizeTrace");
        recognizeTrace.start();
        //bitmap = getBitmapFromAsset(DEMO_FILE);
        //Log.v (TAG, "bitmapWidth: " + String.valueOf(bitmap.getWidth()));
        //Log.v (TAG, "bitmapHeight: " + String.valueOf(bitmap.getHeight()));
        int[] intValues = new int[bitmap.getWidth()*bitmap.getHeight()];
        float[] floatValues = new float[intValues.length * 3];
        String[] outputNames = OUTPUT_NODE.split(",");
        boolean logStats = false;
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int i = 0; i < intValues.length; ++i) {
            floatValues[i * 3 + 0] = ((intValues[i] >> 16) & 0xFF);
            floatValues[i * 3 + 1] = ((intValues[i] >> 8) & 0xFF);
            floatValues[i * 3 + 2] = (intValues[i] & 0xFF);
        }

        mInferenceInterface.feed (INPUT_NODE, floatValues, 1, bitmap.getHeight(), bitmap.getWidth(), 3);
        Log.d(TAG, "Running graph");
        Trace inferenceTrace = FirebasePerformance.getInstance().newTrace("inferenceTrace");
        inferenceTrace.start();
        final long startTime = SystemClock.uptimeMillis();
        mInferenceInterface.run(outputNames, logStats);
        long lap = SystemClock.uptimeMillis() - startTime;
        Log.v(TAG, "run took " + lap + " ms");
        inferenceTrace.stop();

        int patchHeight = (int) Math.ceil(bitmap.getHeight() / 8.0);
        if (patchHeight % 2 != 0)
            patchHeight++;

        int patchWidth= (int) Math.ceil(bitmap.getWidth() / 8.0);
        if (patchWidth % 2 != 0)
            patchWidth++;

        final float[] part_pred =
                new float[patchHeight * patchWidth * NUM_JOINTS];

        final float[] loc_ref =
                new float[patchHeight * patchWidth * NUM_JOINTS * 2];


        //Log.v (TAG, "patchWidth: " + String.valueOf(patchWidth));
        //Log.v (TAG, "patchHeight: " + String.valueOf(patchHeight));

        mInferenceInterface.fetch(outputNames[0], part_pred);
        mInferenceInterface.fetch(outputNames[1], loc_ref);

        final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
        float locref_stdev = 7.2801f;
        float stride = 8.f;

        float[] maxPredictions = new float[NUM_JOINTS];
        float[] maxLocations = new float[NUM_JOINTS * 2];
        for (int z=0; z < NUM_JOINTS; ++z) {
            for (int y = 0; y < patchHeight; ++y) {
                for (int x = 0; x < patchWidth; ++x) {
                    float curPred = part_pred[x * NUM_JOINTS + y * patchWidth * NUM_JOINTS + z];
                    if (curPred > maxPredictions[z]) {
                        maxPredictions[z] = curPred;
                        maxLocations[z * 2] = x * stride + 0.5f * stride + locref_stdev * loc_ref[x * NUM_JOINTS * 2 + y * patchWidth * NUM_JOINTS * 2 + z * 2];
                        maxLocations[z * 2 + 1] = y * stride + 0.5f * stride + locref_stdev * loc_ref[x * NUM_JOINTS * 2 + y * patchWidth * NUM_JOINTS * 2 + z * 2 + 1];
                    }
                }
            }

            recognitions.add(new Recognition(jointNames[z], maxPredictions[z], maxLocations[z*2], maxLocations[z*2+1]));
            //Log.v(TAG, "joint [" + z + "]: " + maxLocations[z * 2] + ", " + maxLocations[z * 2 + 1] + ", " + maxPredictions[z]);
        }

        recognizeTrace.stop();
        return recognitions;
    }
}
