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
    private static final String VERTEX_SHADER = "uniform mat4 uMVPMatrix; attribute vec4 vPosition; attribute vec4 vColor; varying vec4 vColorVarying; void main() { gl_Position = uMVPMatrix * vPosition; vColorVarying = vColor; }";
    private static final String FRAGMENT_SHADER = "precision mediump float; varying vec4 vColorVarying; void main() { gl_FragColor = vColorVarying; }";
    
    private int program, positionHandle, colorHandle, mvpMatrixHandle;
    private final float[] mvpMatrix = new float[16], projectionMatrix = new float[16], viewMatrix = new float[16];
    
    public World world;
    public Player player;
    public List<Chunk> chunks = new ArrayList<>();
    
    private final Context ctx;
    private File saveFile;
    public int currentWorld = 1;
    private volatile boolean savePending = false;
    private volatile int pendingSwitch = -1;
    private volatile boolean pendingDelete = false;
    
    public MyGLRenderer(Context ctx) {
        this.ctx = ctx;
        saveFile = new File(ctx.getFilesDir(), "world_" + currentWorld + ".dat");
        loadOrGenerate(currentWorld);
    }
    
    private void loadOrGenerate(int index) {
        File f = new File(ctx.getFilesDir(), "world_" + index + ".dat");
        if (f.exists()) {
            World w = new World();
            float[] pos = w.loadFromFile(f);
            if (pos != null) {
                world = w;
                player = new Player(pos[0], pos[1], pos[2]);
                player.yaw = pos[3]; player.pitch = pos[4];
                fixPlayerPos(); return;
            }
        }
        world = new World(12345L + index * 100000L);
        player = new Player(32f, 25f, 32f);
        fixPlayerPos();
    }
    
    private void fixPlayerPos() {
        for (int i = 0; i < 30; i++) {
            if (!Block.isSolid(world.getBlock((int)Math.floor(player.x), (int)Math.floor(player.y), (int)Math.floor(player.z))) &&
                !Block.isSolid(world.getBlock((int)Math.floor(player.x), (int)Math.floor(player.y + 1), (int)Math.floor(player.z)))) break;
            player.y += 1;
        }
    }
    
    public void requestSwitchWorld(int index) { pendingSwitch = index; }
    public void requestDeleteCurrent() { pendingDelete = true; }
    public void saveNow() { savePending = true; }
    public boolean worldExists(int index) { return new File(ctx.getFilesDir(), "world_" + index + ".dat").exists(); }
    
    private void switchWorld(int index) {
        world.saveToFile(saveFile, player.x, player.y, player.z, player.yaw, player.pitch);
        currentWorld = index;
        saveFile = new File(ctx.getFilesDir(), "world_" + index + ".dat");
        loadOrGenerate(index);
        rebuildAllChunks();
    }
    
    private void rebuildAllChunks() {
        chunks.clear();
        for (int cx = 0; cx < World.SIZE_X; cx += 16)
            for (int cz = 0; cz < World.SIZE_Z; cz += 16) {
                Chunk c = new Chunk(cx, 0, cz, 16, World.SIZE_Y, 16);
                c.buildMesh(world); chunks.add(c);
            }
    }
    
    @Override public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.7f, 1.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        int vs = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        program = GLES20.glCreateProgram(); GLES20.glAttachShader(program, vs); GLES20.glAttachShader(program, fs); GLES20.glLinkProgram(program);
        rebuildAllChunks();
    }
    
    public void markDirty(int wx, int wy, int wz) {
        for (Chunk c : chunks)
            if (wx >= c.startX - 1 && wx < c.startX + c.sizeX + 1 && wy >= c.startY - 1 && wy < c.startY + c.sizeY + 1 && wz >= c.startZ - 1 && wz < c.startZ + c.sizeZ + 1)
                c.buildMesh(world);
        savePending = true;
    }
    
    @Override public void onDrawFrame(GL10 unused) {
        if (pendingDelete) { pendingDelete = false; if (saveFile.exists()) saveFile.delete(); loadOrGenerate(currentWorld); rebuildAllChunks(); }
        if (pendingSwitch > 0) { int n = pendingSwitch; pendingSwitch = -1; if (n != currentWorld) switchWorld(n); }
        
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        player.update(world);
        
        if (savePending) {
            savePending = false;
            final float px = player.x, py = player.y, pz = player.z, yw = player.yaw, pt = player.pitch;
            final File f = saveFile; final World w = world;
            new Thread(new Runnable() { @Override public void run() { w.saveToFile(f, px, py, pz, yw, pt); } }).start();
        }
        
        float yawRad = (float)Math.toRadians(player.yaw), pitchRad = (float)Math.toRadians(player.pitch);
        float cosPitch = (float)Math.cos(pitchRad);
        
        float lookX = player.x + (float)Math.sin(yawRad) * cosPitch;
        float lookY = player.y + player.getEyeHeight() + (float)Math.sin(pitchRad);
        float lookZ = player.z - (float)Math.cos(yawRad) * cosPitch;
        
        float camX = player.x, camY = player.y + player.getEyeHeight(), camZ = player.z;
        
        if (player.thirdPerson) {
            float dist = 4.0f, step = 0.2f, curDist = 0;
            float stepX = -(float)Math.sin(yawRad) * cosPitch * step;
            float stepY = -(float)Math.sin(pitchRad) * step;
            float stepZ = (float)Math.cos(yawRad) * cosPitch * step;
            while (curDist < dist) {
                float nextX = camX + stepX, nextY = camY + stepY, nextZ = camZ + stepZ;
                if (Block.isSolid(world.getBlock((int)Math.floor(nextX), (int)Math.floor(nextY), (int)Math.floor(nextZ)))) break;
                camX = nextX; camY = nextY; camZ = nextZ; curDist += step;
            }
        }
        
        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, lookX, lookY, lookZ, 0, 1, 0);
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
    
    @Override public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        Matrix.perspectiveM(projectionMatrix, 0, 70f, (float) width / height, 0.05f, 300f);
    }
    
    private int loadShader(int type, String code) {
        int s = GLES20.glCreateShader(type); GLES20.glShaderSource(s, code); GLES20.glCompileShader(s); return s;
    }
    
    public int[] raycastBlock() {
        float yawRad = (float)Math.toRadians(player.yaw), pitchRad = (float)Math.toRadians(player.pitch);
        float cosPitch = (float)Math.cos(pitchRad);
        float dx = (float)Math.sin(yawRad) * cosPitch, dy = (float)Math.sin(pitchRad), dz = -(float)Math.cos(yawRad) * cosPitch;
        float px = player.x, py = player.y + player.getEyeHeight(), pz = player.z;
        for (int i = 0; i < 80; i++) {
            int bx = (int)Math.floor(px), by = (int)Math.floor(py), bz = (int)Math.floor(pz);
            if (Block.isSolid(world.getBlock(bx, by, bz))) return new int[]{bx, by, bz};
            px += dx * 0.1f; py += dy * 0.1f; pz += dz * 0.1f;
        }
        return null;
    }
}
