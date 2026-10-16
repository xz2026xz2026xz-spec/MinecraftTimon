package com.example.myminecraft;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

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
    private final float[] tempMatrix = new float[16];
    
    public World world;
    public Player player;
    public Chunk chunk;
    public volatile boolean worldDirty = false;
    
    public MyGLRenderer() {
        player = new Player(32f, 20f, 32f);
    }
    
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
        chunk = new Chunk(0, 0, 0, World.SIZE_X, World.SIZE_Y, World.SIZE_Z);
        chunk.buildMesh(world);
    }
    
    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        player.update(world);
        
        if (worldDirty) {
            worldDirty = false;
            chunk.buildMesh(world);
        }
        
        // Проекция
        float ratio = 1.777f;
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 0.1f, 100);
        
        // Камера от игрока (от первого лица)
        float yawRad = (float)Math.toRadians(player.yaw);
        float pitchRad = (float)Math.toRadians(player.pitch);
        
        float lookX = player.x + (float)(Math.sin(yawRad) * Math.cos(pitchRad));
        float lookY = player.y + 1.6f + (float)Math.sin(pitchRad);
        float lookZ = player.z - (float)(Math.cos(yawRad) * Math.cos(pitchRad));
        
        Matrix.setLookAtM(viewMatrix, 0,
            player.x, player.y + 1.6f, player.z,
            lookX, lookY, lookZ,
            0, 1, 0);
        
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        
        GLES20.glUseProgram(program);
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        colorHandle = GLES20.glGetAttribLocation(program, "vColor");
        
        if (chunk.getVertexCount() > 0) {
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
    }
    
    private int loadShader(int type, String code) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, code);
        GLES20.glCompileShader(s);
        return s;
    }
    
    // Raycast — найти блок под прицелом
    public int[] raycastBlock() {
        float yawRad = (float)Math.toRadians(player.yaw);
        float pitchRad = (float)Math.toRadians(player.pitch);
        
        float dx = (float)(Math.sin(yawRad) * Math.cos(pitchRad));
        float dy = (float)Math.sin(pitchRad);
        float dz = -(float)(Math.cos(yawRad) * Math.cos(pitchRad));
        
        float px = player.x;
        float py = player.y + 1.6f;
        float pz = player.z;
        
        for (int i = 0; i < 60; i++) {
            int bx = (int)Math.floor(px);
            int by = (int)Math.floor(py);
            int bz = (int)Math.floor(pz);
            if (Block.isSolid(world.getBlock(bx, by, bz))) {
                return new int[]{bx, by, bz};
            }
            px += dx * 0.1f;
            py += dy * 0.1f;
            pz += dz * 0.1f;
        }
        return null;
    }
}
