package com.cgm.scanner;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;


/**
 * Created by mmatiaschek on 28.09.2017.
 */

public class PredictionAdapter extends RecyclerView.Adapter<PredictionAdapter.PredictionViewHolder> {

    private static final String TAG = "PredictionAdapter";
    private Context mContext;
    //private ChildGrowthDbHelper dbHelper;
    //private int mCount;
    private Cursor mCursor;

    public PredictionAdapter(Context context, Cursor cursor) {
    //    dbHelper = new ChildGrowthDbHelper(context);
        this.mContext = context;
        this.mCursor = cursor;
    }

    @Override
    public PredictionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.prediction_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
        PredictionViewHolder viewHolder = new PredictionViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(PredictionViewHolder holder, int position) {
        // Move the mCursor to the position of the item to be displayed
        if (!mCursor.moveToPosition(position))
            return; // bail if returned null

        long id = mCursor.getLong(mCursor.getColumnIndex(PredictionWriterContract.PredictionEntry.COLUMN_NAME_ID));
        float overall = mCursor.getFloat(mCursor.getColumnIndex(PredictionWriterContract.PredictionEntry.COLUMN_NAME_OVERALL));
        long timestamp = mCursor.getLong(mCursor.getColumnIndex(PredictionWriterContract.PredictionEntry.COLUMN_NAME_TIMESTAMP));

        String predictionThresholdText = Float.toString(overall);
        holder.predictionThresholdView.setText(predictionThresholdText);

        holder.timestamp.setText(String.valueOf("time: " + timestamp));
//        predictionThresholdView.setText(mCursor.getString(PredictionWriterContract.PredictionEntry.COLUMN_NAME_ID));

        ContextWrapper cw = new ContextWrapper(mContext);
        File directory = cw.getDir("predictions", Context.MODE_PRIVATE);
        File mypath = new File(directory, id + ".png");
        Bitmap bitmap = BitmapFactory.decodeFile(mypath.getPath());
        holder.predictionImageView.setImageBitmap(bitmap);
        //holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    class PredictionViewHolder extends RecyclerView.ViewHolder {
        ImageView predictionImageView;
        TextView predictionThresholdView;
        TextView timestamp;


        public PredictionViewHolder(View predictionView) {
            super(predictionView);


            predictionImageView = (ImageView) predictionView.findViewById(R.id.iv_list_item_icon);
            predictionThresholdView = (TextView) predictionView.findViewById(R.id.tv_recordingThreshold);
            timestamp = (TextView) predictionView.findViewById(R.id.tv_timestamp);

        }
/*
        void bind(int listIndex) {
            if( mCursor.moveToFirst() ){
                do{
                    //note.getString(note.getColumnIndexOrThrow(NotesDbAdapter.KEY_TITLE)));
                    //Log.v(TAG, "columnIndex 1: " + result.getString(1));
                    //Log.v(TAG, "columnIndex 3: " + result.getString(3));
                    predictionThresholdView.setText(mCursor.getString(PredictionWriterContract.PredictionEntry.COLUMN_NAME_ID));
                }while( mCursor.moveToNext() );
            }

        }*/

    }
}
