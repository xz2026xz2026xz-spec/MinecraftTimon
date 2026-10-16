package com.example.myminecraft;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {
    
    private static final String VERTEX_SHADER =
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "attribute vec4 vColor;" +
        "varying vec4 vColorVarying;" +
        "void main() {" +
        "  gl_Position = uMVPMatrix * vPosition;" +
        "  vColorVarying = vColor;" +
        "}";
    
    private static final String FRAGMENT_SHADER =
        "precision mediump float;" +
        "varying vec4 vColorVarying;" +
        "void main() {" +
        "  gl_FragColor = vColorVarying;" +
        "}";
    
    private int program;
    private int positionHandle;
    private int colorHandle;
    private int mvpMatrixHandle;
    
    private final float[] mvpMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] tempMatrix = new float[16];
    
    private World world;
    private final List<Chunk> chunks = new ArrayList<>();
    private float angle = 0f;
    
    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.7f, 1.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        
        int vs = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);
        
        world = new World(12345L);
        
        int cs = World.CHUNK_SIZE;
        for (int cx = 0; cx < World.SIZE_X; cx += cs) {
            for (int cz = 0; cz < World.SIZE_Z; cz += cs) {
                Chunk c = new Chunk(cx, 0, cz,
                        Math.min(cs, World.SIZE_X - cx),
                        World.SIZE_Y,
                        Math.min(cs, World.SIZE_Z - cz));
                c.buildMesh(world);
                chunks.add(c);
            }
        }
    }
    
    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        angle += 0.3f;
        
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, -World.SIZE_X / 2f, -World.SIZE_Y / 2f, -World.SIZE_Z / 2f);
        Matrix.rotateM(modelMatrix, 0, 20, 1, 0, 0);
        Matrix.rotateM(modelMatrix, 0, angle, 0, 1, 0);
        
        Matrix.setLookAtM(viewMatrix, 0,
            0, 0, 70,
            0, 0, 0,
            0, 1, 0);
        
        Matrix.multiplyMM(tempMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, tempMatrix, 0, modelMatrix, 0);
        
        GLES20.glUseProgram(program);
        
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        colorHandle = GLES20.glGetAttribLocation(program, "vColor");
        
        for (Chunk chunk : chunks) {
            if (chunk.getVertexCount() == 0) continue;
            
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, chunk.getVertexBuffer());
            
            GLES20.glEnableVertexAttribArray(colorHandle);
            GLES20.glVertexAttribPointer(colorHandle, 3, GLES20.GL_FLOAT, false, 0, chunk.getColorBuffer());
            
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, chunk.getVertexCount());
            
            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(colorHandle);
        }
    }
    
    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 1, 250);
    }
    
    private int loadShader(int type, String code) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, code);
        GLES20.glCompileShader(s);
        return s;
    }
}
