package com.cgm.scanner;

/**
 * Created by yoshi on 26.09.17.
 */

public class Recognition {
    private final String name;
    private final float confidence;
    private final float x;
    private final float y;
    private float[] txyz;
    private float pcx;
    private float pcy;
    private float pcz;
    private float pcc;

    public Recognition (
            final String name, final float confidence, final float x, final float y)
    {
        this.name = name;
        this.confidence = confidence;
        this.x = x;
        this.y = y;
    }

    public  String getName () { return name; }

    public float getConfidence () { return confidence; }

    public float getX () { return x; }

    public float getY () { return y; }

    public float getPcx() {return pcx; }
    public void setPcx(float pcx) { this.pcx = pcx; }

    public float getPcy() {return pcy; }
    public void setPcy(float pcy) { this.pcy = pcy; }

    public float getPcz() {return pcz; }
    public void setPcz(float pcz) { this.pcz = pcz; }

    public float getPcc() {
        return pcc;
    }

    public void setPcc(float pcc) {
        this.pcc = pcc;
    }


    public float[] getPcXYZ () { return txyz; }
    public void setPcXYZ (float[] xyz) { this.txyz = xyz; } /*
        this.pcx = xyz[0];
        this.pcy = xyz[1];
        this.pcz = xyz[2];
    }*/

}
