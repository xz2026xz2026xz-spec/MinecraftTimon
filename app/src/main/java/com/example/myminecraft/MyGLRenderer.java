package com.example.myminecraft;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.io.File;
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
    
    public World world;
    public Player player;
    public List<Chunk> chunks = new ArrayList<>();
    
    private final File saveFile;
    private volatile boolean savePending = false;
    
    public MyGLRenderer(Context ctx) {
        saveFile = new File(ctx.getFilesDir(), "world.dat");
        
        // Пробуем загрузить мир
        if (saveFile.exists()) {
            World w = new World();
            float[] pos = w.loadFromFile(saveFile);
            if (pos != null) {
                world = w;
                player = new Player(pos[0], pos[1], pos[2]);
                player.yaw = pos[3];
                player.pitch = pos[4];
            } else {
                // Файл битый — генерируем новый
                world = new World(12345L);
                player = new Player(32f, 25f, 32f);
            }
        } else {
            // Первый запуск — генерируем
            world = new World(12345L);
            player = new Player(32f, 25f, 32f);
        }
        
        // Проверка: не застрял ли игрок в блоках
        while (Block.isSolid(world.getBlock(
                (int)Math.floor(player.x),
                (int)Math.floor(player.y),
                (int)Math.floor(player.z))) && player.y < World.SIZE_Y) {
            player.y += 1;
        }
    }
    
    public void saveWorld() {
        savePending = true;
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
        
        int cs = 16;
        for (int cx = 0; cx < World.SIZE_X; cx += cs) {
            for (int cz = 0; cz < World.SIZE_Z; cz += cs) {
                Chunk c = new Chunk(cx, 0, cz, cs, World.SIZE_Y, cs);
                c.buildMesh(world);
                chunks.add(c);
            }
        }
    }
    
    public void markDirty(int wx, int wy, int wz) {
        for (Chunk c : chunks) {
            if (wx >= c.startX - 1 && wx < c.startX + c.sizeX + 1 &&
                wy >= c.startY - 1 && wy < c.startY + c.sizeY + 1 &&
                wz >= c.startZ - 1 && wz < c.startZ + c.sizeZ + 1) {
                c.buildMesh(world);
            }
        }
        savePending = true;
    }
    
    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        player.update(world);
        
        if (savePending) {
            savePending = false;
            final float px = player.x;
            final float py = player.y;
            final float pz = player.z;
            final float yaw = player.yaw;
            final float pitch = player.pitch;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    world.saveToFile(saveFile, px, py, pz, yaw, pitch);
                }
            }).start();
        }
        
        float yawRad = (float)Math.toRadians(player.yaw);
        float pitchRad = (float)Math.toRadians(player.pitch);
        
        float cosPitch = (float)Math.cos(pitchRad);
        float lookX = player.x + (float)Math.sin(yawRad) * cosPitch;
        float lookY = player.y + 1.6f + (float)Math.sin(pitchRad);
        float lookZ = player.z - (float)Math.cos(yawRad) * cosPitch;
        
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
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 0.1f, 200f);
    }
    
    private int loadShader(int type, String code) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, code);
        GLES20.glCompileShader(s);
        return s;
    }
    
    public int[] raycastBlock() {
        float yawRad = (float)Math.toRadians(player.yaw);
        float pitchRad = (float)Math.toRadians(player.pitch);
        float cosPitch = (float)Math.cos(pitchRad);
        
        float dx = (float)Math.sin(yawRad) * cosPitch;
        float dy = (float)Math.sin(pitchRad);
        float dz = -(float)Math.cos(yawRad) * cosPitch;
        
        float px = player.x;
        float py = player.y + 1.6f;
        float pz = player.z;
        
        for (int i = 0; i < 80; i++) {
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
