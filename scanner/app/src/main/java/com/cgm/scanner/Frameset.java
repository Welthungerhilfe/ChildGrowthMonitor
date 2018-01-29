package com.cgm.scanner;

import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.experimental.TangoImageBuffer;

/**
 * Created by mmatiaschek on 30.10.2017.
 */

// TODO: not used, remove

public class Frameset {
    private long timestamp;
    private TangoPoseData oglTdepthPose;
    private TangoPoseData oglTcolorPose;
    private TangoPointCloudData pointCloudData;
    private TangoImageBuffer imageBuffer;

    public Frameset(long timestamp,TangoPoseData oglTdepthPose, TangoPoseData oglTcolorPose, TangoPointCloudData pointCloudData, TangoImageBuffer imageBuffer) {
        this.timestamp = timestamp;
        this.oglTdepthPose = oglTdepthPose;
        this.oglTcolorPose = oglTcolorPose;
        this.pointCloudData = pointCloudData;
        this.imageBuffer = imageBuffer;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public TangoPoseData getOglTdepthPose() {
        return oglTdepthPose;
    }

    public TangoPoseData getOglTcolorPose() {
        return oglTcolorPose;
    }

    public TangoPointCloudData getPointCloudData() {
        return pointCloudData;
    }

    public TangoImageBuffer getImageBuffer() {
        return imageBuffer;
    }
}
