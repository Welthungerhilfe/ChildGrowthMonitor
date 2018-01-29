package com.cgm.scanner;

import android.app.IntentService;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.experimental.TangoImageBuffer;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.AddTrace;
import com.google.firebase.perf.metrics.Trace;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.projecttango.tangosupport.TangoSupport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mmatiaschek on 31.10.2017.
 */

// TODO: can't use Service because of parcelable exception TODO Link to Stackoverflow
public class MeasurementService {// extends IntentService {
    private final String TAG = this.getClass().getSimpleName();

    private FirebaseStorage mStorage;
    private StorageReference mStorageRef;

    private Predict mPredict;
    private static final String MODEL_FILE = "/frozen_resnet_50.pb";
    private Context mContext;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */
    public MeasurementService() {
        //super("MeasurementService");
    }
/*
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.v(TAG, "Service received intent");
        long timestamp = 0;
        try {
            intent.getLongExtra("timestamp", timestamp);
            //TangoPointCloudData pointCloudData = intent.getParcelableExtra("pointCloudData");
            TangoPoseData oglTdepthPose = intent.getParcelableExtra("oglTdepthPose");
            TangoPoseData oglTcolorPose = intent.getParcelableExtra("oglTcolorPose");
            Bitmap inputBitmap = intent.getParcelableExtra("bitmap");
            //getFramesetResult(timestamp,inputBitmap,pointCloudData,oglTdepthPose,oglTcolorPose);
        } catch (Exception e) {
            Log.e(TAG, "Error! Caught Exception getting Intent for measurement");
            Log.e(TAG, e.getMessage(), e);
        }
    }
*/
    public void getFramesetResult(Context context, Predict predict, final long timestamp, Bitmap inputBitmap, TangoPointCloudData pointCloudData, TangoPoseData oglTdepthPose, TangoPoseData oglTcolorPose) {
        Trace getFramesetResultTrace = FirebasePerformance.getInstance().newTrace("getFramesetResult");
        getFramesetResultTrace.start();
        Log.v(TAG, "starting processing of color and depth camera input");
        mStorage = FirebaseStorage.getInstance();
        mStorageRef = mStorage.getReference();
        mContext = context;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        inputBitmap.compress(Bitmap.CompressFormat.PNG,100,out);
        byte[] imageData = out.toByteArray();
        inputBitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(imageData));
        // TODO: Remote Config
        inputBitmap = Bitmap.createScaledBitmap(inputBitmap, 1152, 648, true);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        inputBitmap = Bitmap.createBitmap(inputBitmap , 0, 0, inputBitmap.getWidth(), inputBitmap.getHeight(), matrix, true);

        StorageReference imageRef = mStorageRef.child("data/frames/"+timestamp+"_input.png");
        ByteArrayOutputStream inputBitmapBAOS = new ByteArrayOutputStream();
        inputBitmap.compress(Bitmap.CompressFormat.PNG, 100, inputBitmapBAOS);
        ByteArrayInputStream inputImageStream = new ByteArrayInputStream(inputBitmapBAOS.toByteArray());
        UploadTask uploadTask = imageRef.putStream(inputImageStream);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                // Uri downloadUrl = taskSnapshot.getDownloadUrl();
                Log.v(TAG, "Upload of data/frames/"+timestamp+"_input.png completed");
            }
        });


        // inference
        final List<Recognition> recognitions = predict.recognize(inputBitmap);

        // Post-Processing
        final List<Measurement> measurements = measureHeight(pointCloudData, oglTdepthPose, oglTcolorPose, inputBitmap, recognitions);

        float height = 0.0f;
        // TODO: Save Data
        //       - RGB Data and Timestamp
        //       - PointCloudData and Timestamp
        for (Measurement m : measurements) {
            if (m != null) {
                Log.v(TAG, m.getName() + ": length: " + m.getLength() + "cm with confidence: " + m.getConfidence());
                height += m.getLength();
            }
        }
        //MediaStore.Images.Media.insertImage(getContentResolver(), inputBitmap, String.valueOf(timestamp + "_input.png"), String.valueOf(timestamp));

        // setInference
        Bitmap measureBitmap = paintMeasurements(measurements, convertToMutable(inputBitmap));
        measureBitmap = paintRecognitions(recognitions, measureBitmap);
        //MediaStore.Images.Media.insertImage(getContentResolver(), measureBitmap, String.valueOf(timestamp + "_output.png"), String.valueOf(timestamp));

        ByteArrayOutputStream measureBitmapBAOS = new ByteArrayOutputStream();
        measureBitmap.compress(Bitmap.CompressFormat.PNG, 100, measureBitmapBAOS);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(measureBitmapBAOS.toByteArray());
        StorageReference outputImageRef = mStorageRef.child("data/frames/"+timestamp+"_output.png");
        UploadTask uploadOutputImageTask = outputImageRef.putStream(inputStream);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                // Uri downloadUrl = taskSnapshot.getDownloadUrl();
                Log.v(TAG, "Upload of data/frames/"+timestamp+"_output.png completed");
            }
        });
        // Finish
        getFramesetResultTrace.stop();
    }


    @AddTrace(name = "getMeasurementTrace")
    private Measurement getMeasurement(String name, Recognition r1, Recognition r2) {
        if (r1.getPcXYZ() == null || r2.getPcXYZ() == null) {
            // TODO: Throw Exception
            return null;
        }
        float confidence;
        float[] p1 = r1.getPcXYZ();
        float[] p2 = r2.getPcXYZ();
        double separation = Math.sqrt(
                Math.pow(p1[0] - p2[0], 2) +
                        Math.pow(p1[1] - p2[1], 2) +
                        Math.pow(p1[2] - p2[2], 2));
        if (r1.getConfidence() < r2.getConfidence()) {
            confidence = r1.getConfidence();
        } else {
            confidence = r2.getConfidence();
        }
        Measurement measurement = new Measurement(name, r1.getX(), r1.getY(), r2.getX(), r2.getY(), separation, confidence);
        return measurement;
    }

    // TODO: check findEdgesNearPoint
    private List<Measurement> measureHeight(TangoPointCloudData pointCloudData, TangoPoseData oglTdepthPose, TangoPoseData oglTcolorPose, Bitmap inputBitmap, List<Recognition> recognitions) {
        Trace measureHeightTrace = FirebasePerformance.getInstance().newTrace("measureHeightTrace");
        measureHeightTrace.start();

        // add Z to XY Data
        float u;
        float v;
        float[] xyz = new float[3];

        for (Recognition r : recognitions) {
            u = r.getX() / inputBitmap.getWidth();
            v = r.getY() / inputBitmap.getHeight();
            try {
                // TODO: displayRotation
                xyz = TangoSupport.getDepthAtPointNearestNeighbor(pointCloudData,
                        oglTdepthPose.translation, oglTdepthPose.rotation,
                        u, v, 0, oglTcolorPose.translation,
                        oglTcolorPose.rotation);
                // TODO refactor pyramid of doom
                if (xyz != null) {
                    FloatBuffer xyzc = pointCloudData.points.get(xyz);
                    float pcx = xyzc.get();
                    float pcy = xyzc.get();
                    float pcz = xyzc.get();
                    float pcc = xyzc.get();
                    Log.v(TAG, "u: "+u+" v: "+v+" x:" + pcx + " y:" + pcy + " z:" + pcz + " confidence: " + pcc);
                    r.setPcx(pcx);
                    r.setPcy(pcy);
                    r.setPcz(pcz);
                    r.setPcc(pcc);

                    /*
                    Log.v(TAG, "xyz0: "+xyz[0]);
                    Log.v(TAG, "xyz1: "+xyz[1]);
                    Log.v(TAG, "xyz2: "+xyz[2]);
                    Log.v(TAG, "xyz3: "+xyz[3]);
                    */
                    int i = 0;
                    for (float f : xyz) {
                        Log.v(TAG, "Float " + i + " NearestNb XYZ: " + f);
                        i++;
                    }

                    r.setPcXYZ(xyz);
                } else {
                    measureHeightTrace.incrementCounter("xyz_is_null");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error! Caught Exception during measurement");
                Log.e(TAG, e.getMessage(), e);
                measureHeightTrace.incrementCounter("measure_height_exception");
            }
        }

        ArrayList<Measurement> measurements = new ArrayList<>();
        measurements.add(getMeasurement("right lower leg",recognitions.get(0),recognitions.get(1)));
        measurements.add(getMeasurement("right upper leg",recognitions.get(1),recognitions.get(2)));
        measurements.add(getMeasurement("left lower leg",recognitions.get(3),recognitions.get(4)));
        measurements.add(getMeasurement("left upper leg",recognitions.get(4),recognitions.get(5)));
        measurements.add(getMeasurement("right torso",recognitions.get(2),recognitions.get(8)));
        measurements.add(getMeasurement("left torso",recognitions.get(3),recognitions.get(9)));
        measureHeightTrace.stop();
        return measurements;
    }

    @AddTrace(name = "paintRecognitionsTrace")
    public Bitmap paintRecognitions(List<Recognition> recognitions, Bitmap image) {
        Log.v(TAG, "setInference");
        Canvas canvas = new Canvas(image);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Alle Punkte
        paint.setTextSize(20);
        paint.setColor(Color.rgb(255, 50, 78));
        int i = 0;
        for (Recognition r : recognitions) {
            canvas.drawArc(r.getX() - 3, r.getY() - 3, r.getX() + 3, r.getY() + 3, 0, 360, true, paint);
            canvas.drawText(String.format("%.1f", r.getConfidence()*100) + "% "+i,r.getX() + 5, r.getY() + 2, paint);
            i++;
        }
        return image;
    }

    @AddTrace(name = "paintMeasurements")
    public Bitmap paintMeasurements(List<Measurement> measurements, Bitmap image) {
        Log.v(TAG, "paint Measurements");
        Canvas canvas = new Canvas(image);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Alle Punkte
        paint.setTextSize(30);
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(1f);

        for (Measurement m : measurements) {
            if (m != null) {
                canvas.drawLine(m.getX1(),m.getY1(),m.getX2(),m.getY2(), paint);

                float centerX = m.getX1()+m.getX2();
                centerX = centerX / 2;
                float centerY = m.getY1() + m.getY2();
                centerY = centerY / 2;
                if (m.getName().contains("left")) {
                    canvas.drawText(String.format("%.1f", m.getLength()*100) + "cm ",centerX + 8, centerY + 8, paint);
                } else {
                    canvas.drawText(String.format("%.1f", m.getLength()*100) + "cm ",centerX - 100, centerY + 8, paint);
                }

            }
        }
        return image;
    }

    /**
     * Converts a immutable bitmap to a mutable bitmap. This operation doesn't allocates
     * more memory that there is already allocated.
     *
     * @param imgIn - Source image. It will be released, and should not be used more
     * @return a copy of imgIn, but muttable.
     */
    @AddTrace(name = "convertToMutable")
    public Bitmap convertToMutable(Bitmap imgIn) {
        try {
            //this is the file going to use temporally to save the bytes.
            // This file will not be a image, it will store the raw image data.
            ContextWrapper cw = new ContextWrapper(mContext);
            File directory = cw.getDir("predictions", Context.MODE_PRIVATE);
            File file = new File(directory, "temp.tmp");

            //Open an RandomAccessFile
            //Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
            //into AndroidManifest.xml file
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

            // get the width and height of the source bitmap.
            int width = imgIn.getWidth();
            int height = imgIn.getHeight();
            Bitmap.Config type = imgIn.getConfig();

            //Copy the byte to the file
            //Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
            FileChannel channel = randomAccessFile.getChannel();
            MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, imgIn.getRowBytes()*height);
            imgIn.copyPixelsToBuffer(map);
            //recycle the source bitmap, this will be no longer used.
            imgIn.recycle();
            System.gc();// try to force the bytes from the imgIn to be released

            //Create a new bitmap to load the bitmap again. Probably the memory will be available.
            imgIn = Bitmap.createBitmap(width, height, type);
            map.position(0);
            //load it back from temporary
            imgIn.copyPixelsFromBuffer(map);
            //close the temporary file and channel , then delete that also
            channel.close();
            randomAccessFile.close();

            // delete the temp file
            file.delete();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imgIn;
    }

}
