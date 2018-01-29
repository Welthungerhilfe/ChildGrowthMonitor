package com.cgm.scanner;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * Created by mmatiaschek on 28.09.2017.
 */

public class PredictionsList extends AppCompatActivity {

    private PredictionAdapter mPredictionAdapter;
    private RecyclerView mPredictionsList;
    // TODO Create Application and get DB from Main
    private ChildGrowthDbHelper dbHelper;
    private SQLiteDatabase mDb;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prediction_view);
// TODO Create Application and get DB from Main
        //mDb = ((Application)getApplication()).mDb;
        // Create Menu with Back Button
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mPredictionsList = (RecyclerView) findViewById(R.id.rv_prediction);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mPredictionsList.setLayoutManager(layoutManager);
        mPredictionsList.setHasFixedSize(false);

        Cursor cursor = getAllPredictions();
        mPredictionAdapter = new PredictionAdapter(this, cursor );
        mPredictionsList.setAdapter(mPredictionAdapter);

    }

    private Cursor getAllPredictions() {
        // TODO Create Application and get DB from Main
        dbHelper = new ChildGrowthDbHelper(getApplicationContext());
        mDb = dbHelper.getReadableDatabase();
        return mDb.query(PredictionWriterContract.PredictionEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                PredictionWriterContract.PredictionEntry.COLUMN_NAME_TIMESTAMP + " DESC");
    }
}
