package com.cgm.scanner;

/**
 * Created by mmatiaschek on 24.10.2017.
 */

public class Measurement {
    private final String name;
    private final float x1;
    private final float y1;
    private final float x2;
    private final float y2;
    private final double length;
    private final float confidence; // confidence is the lowest confidence of the two points

    public Measurement (String name, float x1, float y1, float x2, float y2, double length, float confidence) {
        this.name = name;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.length = length;
        this.confidence = confidence;
    }

    public String getName() {
        return name;
    }

    public float getX1() {
        return x1;
    }

    public float getY1() {
        return y1;
    }

    public float getX2() {
        return x2;
    }

    public float getY2() {
        return y2;
    }

    public double getLength() {
        return length;
    }
    public float getConfidence() {
        return confidence;
    }

}
