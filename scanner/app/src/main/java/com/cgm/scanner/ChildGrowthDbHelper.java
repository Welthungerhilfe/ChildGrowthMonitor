package com.cgm.scanner;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.cgm.scanner.PredictionWriterContract.PredictionEntry;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mmatiaschek on 26.09.2017.
 */

public class ChildGrowthDbHelper extends SQLiteOpenHelper{
    private static final String FILE_DIR = "CGM";
    private static final String DATABASE_NAME = "childgrowthmonitor.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TAG = "ChildGrowthDbHelper";
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    //private DocumentReference mDocRef = FirebaseFirestore.getInstance().document("/child/dummy/scan/dummy/prediction/1");

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

    public ChildGrowthDbHelper(Context context) {
        super(context, DATABASE_NAME ,null, DATABASE_VERSION);// Environment.getExternalStorageDirectory()
                //+ File.separator + FILE_DIR
                //+ File.separator + DATABASE_NAME ,null, DATABASE_VERSION);
        Log.v(TAG, "instanciated " + File.separator + FILE_DIR + File.separator + DATABASE_NAME);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_PREDICTION_TABLE = "CREATE TABLE " +
                PredictionEntry.TABLE_NAME + "(" +
                PredictionEntry.COLUMN_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                PredictionEntry.COLUMN_NAME_TIMESTAMP + " TIMESTAMP DEFAULT ( STRFTIME( '%s', CURRENT_TIMESTAMP )  ), " +
                PredictionEntry.COLUMN_NAME_ISUPLOADED + " boolean, " +
                PredictionEntry.COLUMN_NAME_OVERALL + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_ANKLE1X + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_ANKLE1Y + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_ANKLE1P + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_KNEE1X + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_KNEE1Y + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_KNEE1P + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_HIP1X + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_HIP1Y + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_HIP1P + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_HIP2X + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_HIP2Y + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_HIP2P + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_KNEE2X + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_KNEE2Y + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_KNEE2P + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_ANKLE2X + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_ANKLE2Y + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_ANKLE2P + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_WRIST1X + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_WRIST1Y + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_WRIST1P + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_ELBOW1X + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_ELBOW1Y + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_ELBOW1P + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_SHOULDER1X + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_SHOULDER1Y + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_SHOULDER1P + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_SHOULDER2X + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_SHOULDER2Y + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_SHOULDER2P + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_ELBOW2X + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_ELBOW2Y + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_ELBOW2P + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_WRIST2X + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_WRIST2Y + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_WRIST2P + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_CHINX + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_CHINY + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_CHINP + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_FHEADX + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_FHEADY + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_FHEADP + " REAL NOT NULL, " +
                PredictionEntry.COLUMN_NAME_BITMAP + " BLOB" +
                ");";
        Log.v(TAG, SQL_CREATE_PREDICTION_TABLE);
        db.execSQL(SQL_CREATE_PREDICTION_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String SQL_DROP_PREDICTION_TABLE = "DROP TABLE IF EXISTS " + PredictionEntry.TABLE_NAME + ";";
        Log.v(TAG, SQL_DROP_PREDICTION_TABLE);
        db.execSQL(SQL_DROP_PREDICTION_TABLE);
        onCreate(db);
    }

/*
    public Cursor getAllPredictions() {
        return this.getWritableDatabase().rawQuery(PredictionEntry.SELECT_QUERY, null);
    }

    public int getPredictionCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = (int) DatabaseUtils.queryNumEntries(db, PredictionEntry.TABLE_NAME);
        Log.v(TAG, "Count of recorded predictions: "+ count);
        db.close();
        return count;
    }

    public Cursor getPredictionCursor(int id) {
        String[] selectionArgs;
        selectionArgs = new String[1];
        selectionArgs[0] = String.valueOf(id);
        return this.getWritableDatabase().rawQuery("SELECT * FROM resnet_50 WHERE id = ?",selectionArgs);
    }
*/

    public void addPrediction(List<Recognition> r, long bitmapTime) {
        float overallConfidence = 0.0f;
        //ContentValues cv = new ContentValues();
        Map<String, Object> cv = new HashMap<>();
        for(Recognition joint : r) {
            String jointName = joint.getName();
            cv.put(jointName + "x", joint.getX());
            cv.put(jointName + "y", joint.getY());
            cv.put(jointName + "p", joint.getConfidence());
            overallConfidence += joint.getConfidence();
        }
        cv.put(PredictionEntry.COLUMN_NAME_TIMESTAMP, bitmapTime);
        cv.put(PredictionEntry.COLUMN_NAME_OVERALL, overallConfidence);
        cv.put(PredictionEntry.COLUMN_NAME_BITMAP, "");
        Log.v(TAG, "inserting with overall confidence of " + overallConfidence);
        db.collection("child/dummy/scan/dummy/prediction").add(cv).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Log.d(TAG, "Prediction saved in Cloud with id: "+documentReference.getId());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Fehler beim Speichern in der Cloud");
            }
        });
        /*
        mDocRef.set(cv).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "Prediction saved in Cloud");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Fehler beim Speichern in der Cloud");
            }
        });
*/
        //return mDb.insert(PredictionEntry.TABLE_NAME, null, cv);

    }
}
