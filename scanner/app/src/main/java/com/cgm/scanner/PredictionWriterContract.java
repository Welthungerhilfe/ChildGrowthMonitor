package com.cgm.scanner;

import android.provider.BaseColumns;

/**
 * Created by mmatiaschek on 26.09.2017.
 */

public final class PredictionWriterContract {
    private PredictionWriterContract() {}

    /* Inner Class that defines the Table Contents */
    public static class PredictionEntry implements BaseColumns {
        public static final String TABLE_NAME = "resnet_50";
        public static final String COLUMN_NAME_ID = "id"; // TODO: ID Factory hash from gps + Time(ms)?
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_ISUPLOADED = "isuploaded";
        public static final String COLUMN_NAME_UPLOADTIME = "uploadtime";
        public static final String COLUMN_NAME_OVERALL = "overall";
        public static final String COLUMN_NAME_ANKLE1X = "ankle1x";
        public static final String COLUMN_NAME_ANKLE1Y = "ankle1y";
        public static final String COLUMN_NAME_ANKLE1P = "ankle1p";
        public static final String COLUMN_NAME_KNEE1X = "knee1x";
        public static final String COLUMN_NAME_KNEE1Y = "knee1y";
        public static final String COLUMN_NAME_KNEE1P = "knee1p";
        public static final String COLUMN_NAME_HIP1X = "hip1x";
        public static final String COLUMN_NAME_HIP1Y = "hip1y";
        public static final String COLUMN_NAME_HIP1P = "hip1p";
        public static final String COLUMN_NAME_HIP2X = "hip2x";
        public static final String COLUMN_NAME_HIP2Y = "hip2y";
        public static final String COLUMN_NAME_HIP2P = "hip2p";
        public static final String COLUMN_NAME_KNEE2X = "knee2x";
        public static final String COLUMN_NAME_KNEE2Y = "knee2y";
        public static final String COLUMN_NAME_KNEE2P = "knee2p";
        public static final String COLUMN_NAME_ANKLE2X = "ankle2x";
        public static final String COLUMN_NAME_ANKLE2Y = "ankle2y";
        public static final String COLUMN_NAME_ANKLE2P = "ankle2p";
        public static final String COLUMN_NAME_WRIST1X = "wrist1x";
        public static final String COLUMN_NAME_WRIST1Y = "wrist1y";
        public static final String COLUMN_NAME_WRIST1P = "wrist1p";
        public static final String COLUMN_NAME_ELBOW1X = "elbow1x";
        public static final String COLUMN_NAME_ELBOW1Y = "elbow1y";
        public static final String COLUMN_NAME_ELBOW1P = "elbow1p";
        public static final String COLUMN_NAME_SHOULDER1X = "shoulder1x";
        public static final String COLUMN_NAME_SHOULDER1Y = "shoulder1y";
        public static final String COLUMN_NAME_SHOULDER1P = "shoulder1p";
        public static final String COLUMN_NAME_SHOULDER2X = "shoulder2x";
        public static final String COLUMN_NAME_SHOULDER2Y = "shoulder2y";
        public static final String COLUMN_NAME_SHOULDER2P = "shoulder2p";
        public static final String COLUMN_NAME_ELBOW2X = "elbow2x";
        public static final String COLUMN_NAME_ELBOW2Y = "elbow2y";
        public static final String COLUMN_NAME_ELBOW2P = "elbow2p";
        public static final String COLUMN_NAME_WRIST2X = "wrist2x";
        public static final String COLUMN_NAME_WRIST2Y = "wrist2y";
        public static final String COLUMN_NAME_WRIST2P = "wrist2p";
        public static final String COLUMN_NAME_CHINX = "chinx";
        public static final String COLUMN_NAME_CHINY = "chiny";
        public static final String COLUMN_NAME_CHINP = "chinp";
        public static final String COLUMN_NAME_FHEADX = "fheadx";
        public static final String COLUMN_NAME_FHEADY = "fheady";
        public static final String COLUMN_NAME_FHEADP = "fheadp";
        public static final String COLUMN_NAME_XYZDATA = "xyzdata";
        public static final String COLUMN_NAME_BITMAP = "bitmapfile";
    }
}
