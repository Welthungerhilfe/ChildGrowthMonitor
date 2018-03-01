/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.welthungerhilfe.cgm.scanner;

/* Copyright 2016 Google Inc. All Rights Reserved.
        *
        * Licensed under the Apache License, Version 2.0 (the "License");
        * you may not use this file except in compliance with the License.
        * You may obtain a copy of the License at
        *
        *      http://www.apache.org/licenses/LICENSE-2.0
        *
        * Unless required by applicable law or agreed to in writing, software
        * distributed under the License is distributed on an "AS IS" BASIS,
        * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        * See the License for the specific language governing permissions and
        * limitations under the License.
        */

//import com.google.tango.support.TangoSupport;

        import android.content.Context;
        import android.graphics.Bitmap;
        import android.graphics.SurfaceTexture;
        import android.opengl.EGL14;
        import android.opengl.GLES11Ext;
        import android.opengl.GLES20;
        import android.opengl.GLSurfaceView;
        import android.util.Log;

        import com.android.grafika.gles.FullFrameRect;
        import com.android.grafika.gles.Texture2dProgram;
        import com.projecttango.tangosupport.TangoSupport;

        import java.io.File;
        import java.nio.ByteBuffer;
        import java.nio.ByteOrder;
        import java.nio.FloatBuffer;
        import java.nio.ShortBuffer;

        import javax.microedition.khronos.egl.EGLConfig;
        import javax.microedition.khronos.opengles.GL10;


        // TODO: port from GL10 to GLES10 for better performance?
        // TODO: projection matrix and camera transform matrix for 3D display of point clouds?
        // TODO: use RecordingSurface

/**
 * A simple OpenGL renderer that renders the Tango RGB camera texture on a full-screen background.
 */
public class ScanVideoRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = ScanVideoRenderer.class.getSimpleName();

    private final String vss =
            "attribute vec2 vPosition;\n" +
                    "attribute vec2 vTexCoord;\n" +
                    "varying vec2 texCoord;\n" +
                    "void main() {\n" +
                    "  texCoord = vTexCoord;\n" +
                    "  gl_Position = vec4(vPosition.x, vPosition.y, 0.0, 1.0);\n" +
                    "}";

    private final String fss =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "varying vec2 texCoord;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture,texCoord);\n" +
                    "}";

    private final float[] textureCoords0 =
            new float[]{1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f};

    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;

    private int mRecordingStatus;

    private TextureMovieEncoder mVideoEncoder;
    private File mOutputFile;
    private boolean mRecordingEnabled;

    private FloatBuffer mVertex;
    private FloatBuffer mTexCoord;
    private ShortBuffer mIndices;
    private int[] mVbos;
    private int[] mTextures = new int[1];
    private int mProgram;
    private RenderCallback mRenderCallback;

    private FullFrameRect mFullScreen;

    private int mTextureId;
    private SurfaceTexture mSurfaceTexture;

    private int mFrameCount;

    public ScanVideoRenderer(Context context, TextureMovieEncoder movieEncoder, File outputFile, RenderCallback callback) {

        mVideoEncoder = movieEncoder;
        mOutputFile = outputFile;

        mRecordingStatus = -1;
        mRecordingEnabled = false;

        mTextureId = -1;

        mFrameCount = -1;

        mRenderCallback = callback;
        mTextures[0] = 0;
        // Vertex positions.
        float[] vtmp = { 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f };
        // Vertex texture coords.
        float[] ttmp = { 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f };
        // Indices.
        short[] itmp = {0, 1, 2, 3};
        mVertex = ByteBuffer.allocateDirect(vtmp.length * Float.SIZE / 8).order(
                ByteOrder.nativeOrder()).asFloatBuffer();
        mVertex.put(vtmp);
        mVertex.position(0);
        mTexCoord = ByteBuffer.allocateDirect(ttmp.length * Float.SIZE / 8).order(
                ByteOrder.nativeOrder()).asFloatBuffer();
        mTexCoord.put(ttmp);
        mTexCoord.position(0);
        mIndices = ByteBuffer.allocateDirect(itmp.length * Short.SIZE / 8).order(
                ByteOrder.nativeOrder()).asShortBuffer();
        mIndices.put(itmp);
        mIndices.position(0);
    }


    /**
     * Notifies the renderer that we want to stop or start recording.
     */
    public void changeRecordingState(boolean isRecording) {
        Log.d(TAG, "changeRecordingState: was " + mRecordingEnabled + " now " + isRecording);
        mRecordingEnabled = isRecording;
    }
    /**
     * A small callback to allow the caller to introduce application-specific code to be executed
     * in the OpenGL thread.
     */
    public interface RenderCallback {
        void preRender();
    }


    /**
     * Notifies the renderer thread that the activity is pausing.
     * <p>
     * For best results, call this *after* disabling Camera preview.
     */
    public void notifyPausing() {
        if (mSurfaceTexture != null) {
            Log.d(TAG, "renderer pausing -- releasing SurfaceTexture");
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }

        if (mFullScreen != null) {
            mFullScreen.release(false);     // assume the GLSurfaceView EGL context is about
            mFullScreen = null;             //  to be destroyed
        }
        //mIncomingWidth = mIncomingHeight = -1;

    }


    public void updateColorCameraTextureUv(int rotation){
        float[] textureCoords =
                TangoSupport.getVideoOverlayUVBasedOnDisplayRotation(textureCoords0, rotation);
        setTextureCoords(textureCoords);
    }

    private void setTextureCoords(float[] textureCoords) {
        mTexCoord.put(textureCoords);
        mTexCoord.position(0);
        if (mVbos != null) {
            // Bind to texcoord buffer.
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbos[1]);
            // Populate it.
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * 2 * Float
                    .SIZE / 8, mTexCoord, GLES20.GL_STATIC_DRAW); // texcoord of floats.
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        createTextures();
        createCameraVbos();
        GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);
        mProgram = getProgram(vss, fss);

        // Set up the texture blitter that will be used for on-screen display.  This
        // is *not* applied to the recording, because that uses a separate shader.
        mFullScreen = new FullFrameRect(
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));

        mTextureId = mFullScreen.createTextureObject();

        mSurfaceTexture = new SurfaceTexture(mTextureId);

        // We're starting up or coming back.  Either way we've got a new EGLContext that will
        // need to be shared with the video encoder, so figure out if a recording is already
        // in progress.
        mRecordingEnabled = mVideoEncoder.isRecording();
        if (mRecordingEnabled) {
            mRecordingStatus = RECORDING_RESUMED;
        } else {
            mRecordingStatus = RECORDING_OFF;
        }

    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        Log.d(TAG, "onSurfaceChanged " + width + "x" + height);
    }


    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        //Log.d(TAG, "onDrawFrame tex=" + mTextureId);
        boolean showBox = false;

        // Call application-specific code that needs to run on the OpenGL thread.
        mRenderCallback.preRender();

        //Log.v(TAG, "drawFrame call update on SurfaceTexture");
        mSurfaceTexture.updateTexImage();

        // Recording
        // If the recording state is changing, take care of it here.  Ideally we wouldn't
        // be doing all this in onDrawFrame(), but the EGLContext sharing with GLSurfaceView
        // makes it hard to do elsewhere.
        if (mRecordingEnabled) {
            switch (mRecordingStatus) {
                case RECORDING_OFF:
                    Log.d(TAG, "START recording");
                    // start recording
                    // TODO: better config
                    // TODO: make sure this is the same EGLContext from GLSurfaceView used by Tango
                    mVideoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(
                            mOutputFile, 640, 480, 1000000, EGL14.eglGetCurrentContext()));
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_RESUMED:
                    Log.d(TAG, "RESUME recording");
                    mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        } else {
            switch (mRecordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    // stop recording
                    Log.d(TAG, "STOP recording");
                    mVideoEncoder.stopRecording();
                    mRecordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        }

        // Set the video encoder's texture name.  We only need to do this once, but in the
        // current implementation it has to happen after the video encoder is started, so
        // we just do it here.
        //
        // TODO: be less lame.
        //mVideoEncoder.setTextureId(mTextureId);

        // Tell the video encoder thread that a new frame is available.
        // This will be ignored if we're not actually recording.
        //Log.v(TAG, "Timestamp onDraw: "+mSurfaceTexture.getTimestamp());
        mVideoEncoder.frameAvailable(mSurfaceTexture);

        GLES20.glUseProgram(mProgram);

        // Don't write depth buffer because we want to draw the camera as background.
        GLES20.glDepthMask(false);

        int ph = GLES20.glGetAttribLocation(mProgram, "vPosition");
        int tch = GLES20.glGetAttribLocation(mProgram, "vTexCoord");
        int th = GLES20.glGetUniformLocation(mProgram, "sTexture");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextures[0]);
        GLES20.glUniform1i(th, 0);

        GLES20.glEnableVertexAttribArray(ph);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbos[0]);
        GLES20.glVertexAttribPointer(ph, 2, GLES20.GL_FLOAT, false, 4 * 2, 0);

        GLES20.glEnableVertexAttribArray(tch);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbos[1]);
        GLES20.glVertexAttribPointer(tch, 2, GLES20.GL_FLOAT, false, 4 * 2, 0);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mVbos[2]);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, 4, GLES20.GL_UNSIGNED_SHORT, 0);

        // Unbind.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        // Enable depth write again for any additional rendering on top of the camera surface.
        GLES20.glDepthMask(true);

        // Draw a flashing box if we're recording.  This only appears on screen.
        showBox = (mRecordingStatus == RECORDING_ON);
        if (showBox && (++mFrameCount & 0x04) == 0) {
            drawBox();
        }

    }

    /**
     * Draws a red box in the corner.
     */
    private void drawBox() {
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(0, 0, 100, 100);
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }

    private void createTextures() {
        mTextures = new int[1];
        GLES20.glGenTextures(1, mTextures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextures[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
    }

    /**
     * Creates and populates vertex buffer objects for rendering the camera.
     */
    private void createCameraVbos() {
        mVbos = new int[3];
        // Generate three buffers: vertex buffer, texture buffer and index buffer.
        GLES20.glGenBuffers(3, mVbos, 0);
        // Bind to vertex buffer.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbos[0]);
        // Populate it.
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVertex.capacity() * Float.SIZE / 8,
                mVertex, GLES20.GL_STATIC_DRAW); // 4 2D vertex of floats.

        // Bind to texture buffer.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbos[1]);
        // Populate it.
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mTexCoord.capacity() * Float.SIZE / 8,
                mTexCoord, GLES20.GL_STATIC_DRAW); // 4 2D texture coords of floats.

        // Bind to indices buffer.
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mVbos[2]);
        // Populate it.
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndices.capacity() * Short.SIZE / 8,
                mIndices, GLES20.GL_STATIC_DRAW); // 4 short indices.

        // Unbind buffer.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    private int getProgram(String vShaderSrc, String fShaderSrc) {
        int program = GLES20.glCreateProgram();
        if (program == 0) {
            return 0;
        }
        int vShader = loadShader(GLES20.GL_VERTEX_SHADER, vShaderSrc);
        int fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fShaderSrc);
        GLES20.glAttachShader(program, vShader);
        GLES20.glAttachShader(program, fShader);
        GLES20.glLinkProgram(program);
        int[] linked = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "Could not link program");
            Log.v(TAG, "Could not link program:" +
                    GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            return 0;
        }
        return program;
    }

    private int loadShader(int type, String shaderSrc) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderSrc);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader");
            Log.v(TAG, "Could not compile shader:" +
                    GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    public int getTextureId() {
        return mTextures[0];
    }
}
