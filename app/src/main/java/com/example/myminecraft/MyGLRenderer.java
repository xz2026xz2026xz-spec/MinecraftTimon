package com.example.myminecraft;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {
    private static final String VERTEX_SHADER = "uniform mat4 uMVPMatrix; attribute vec4 vPosition; attribute vec4 vColor; attribute vec2 aTexCoord; varying vec4 vColorVarying; varying vec2 vTexCoord; void main() { gl_Position = uMVPMatrix * vPosition; vColorVarying = vColor; vTexCoord = aTexCoord; }";
    private static final String FRAGMENT_SHADER = "precision mediump float; uniform sampler2D uTexture; varying vec4 vColorVarying; varying vec2 vTexCoord; void main() { if (vTexCoord.x < 0.0) { gl_FragColor = vColorVarying; } else { gl_FragColor = texture2D(uTexture, vTexCoord); } }";
    
    private int program, positionHandle, colorHandle, mvpMatrixHandle, texCoordHandle, textureUniform;
    private Frustum frustum = new Frustum();
    private final float[] mvpMatrix = new float[16], projectionMatrix = new float[16], viewMatrix = new float[16], modelMatrix = new float[16], tempMatrix = new float[16];
    
    public World world;
    public Player player;
    public NetworkManager network = new NetworkManager();
    public List<Chunk> chunks = new ArrayList<>();
    
    private PlayerModel playerModel;
    
    private final Context ctx;
    private File saveFile;
    public int currentWorld = 1;
    private volatile boolean savePending = false;
    private volatile int pendingSwitch = -1;
    private volatile boolean pendingDelete = false;
    
    private FloatBuffer bodyVertexBuffer, headVertexBuffer;
    private FloatBuffer sheepBodyTex, sheepHeadTex, dogBodyTex, dogHeadTex;
    private FloatBuffer itemTexBuffer;
    
    public MyGLRenderer(Context ctx) {
        this.ctx = ctx;
        saveFile = new File(ctx.getFilesDir(), "world_" + currentWorld + ".dat");
        loadOrGenerate(currentWorld);
        buildMobsMesh();
        playerModel = new PlayerModel();
    }
    
    private FloatBuffer createTexBuffer(int col, int row) {
        float tileStep = 1.0f / TextureManager.GRID_SIZE;
        float epsilon = 0.001f;
        float u1 = col * tileStep + epsilon;
        float u2 = (col + 1) * tileStep - epsilon;
        float v2 = 1.0f - (row * tileStep) - epsilon;
        float v1 = 1.0f - ((row + 1) * tileStep) + epsilon;
        
        float[] tCoords = new float[72];
        for(int i=0; i<6; i++) {
            tCoords[i*12] = u1; tCoords[i*12+1] = v1; tCoords[i*12+2] = u2; tCoords[i*12+3] = v1; tCoords[i*12+4] = u2; tCoords[i*12+5] = v2;
            tCoords[i*12+6] = u1; tCoords[i*12+7] = v1; tCoords[i*12+8] = u2; tCoords[i*12+9] = v2; tCoords[i*12+10] = u1; tCoords[i*12+11] = v2;
        }
        
        ByteBuffer tb = ByteBuffer.allocateDirect(tCoords.length * 4); tb.order(ByteOrder.nativeOrder());
        FloatBuffer texBuf = tb.asFloatBuffer(); texBuf.put(tCoords); texBuf.position(0);
        return texBuf;
    }
    }
    
    private void buildMobsMesh() {
        sheepBodyTex = createTexBuffer(2, 2);
        sheepHeadTex = createTexBuffer(1, 2);
        dogBodyTex = createTexBuffer(0, 3);
        dogHeadTex = createTexBuffer(3, 2);
        
        ByteBuffer itemTb = ByteBuffer.allocateDirect(72 * 4); itemTb.order(ByteOrder.nativeOrder());
        itemTexBuffer = itemTb.asFloatBuffer();
        
        float[] verts = {
            -0.5f,-0.5f, 0.5f,  0.5f,-0.5f, 0.5f,  0.5f, 0.5f, 0.5f, -0.5f,-0.5f, 0.5f,  0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
             0.5f,-0.5f,-0.5f, -0.5f,-0.5f,-0.5f, -0.5f, 0.5f,-0.5f,  0.5f,-0.5f,-0.5f, -0.5f, 0.5f,-0.5f,  0.5f, 0.5f,-0.5f,
            -0.5f, 0.5f, 0.5f,  0.5f, 0.5f, 0.5f,  0.5f, 0.5f,-0.5f, -0.5f, 0.5f, 0.5f,  0.5f, 0.5f,-0.5f, -0.5f, 0.5f,-0.5f,
            -0.5f,-0.5f,-0.5f,  0.5f,-0.5f,-0.5f,  0.5f,-0.5f, 0.5f, -0.5f,-0.5f,-0.5f,  0.5f,-0.5f, 0.5f, -0.5f,-0.5f, 0.5f,
             0.5f,-0.5f, 0.5f,  0.5f,-0.5f,-0.5f,  0.5f, 0.5f,-0.5f,  0.5f,-0.5f, 0.5f,  0.5f, 0.5f,-0.5f,  0.5f, 0.5f, 0.5f,
            -0.5f,-0.5f,-0.5f, -0.5f,-0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f,-0.5f,-0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f,-0.5f
        };
        
        float[] headVerts = {
            -0.3f,-0.3f, 0.3f,  0.3f,-0.3f, 0.3f,  0.3f, 0.3f, 0.3f, -0.3f,-0.3f, 0.3f,  0.3f, 0.3f, 0.3f, -0.3f, 0.3f, 0.3f,
             0.3f,-0.3f,-0.3f, -0.3f,-0.3f,-0.3f, -0.3f, 0.3f,-0.3f,  0.3f,-0.3f,-0.3f, -0.3f, 0.3f,-0.3f,  0.3f, 0.3f,-0.3f,
            -0.3f, 0.3f, 0.3f,  0.3f, 0.3f, 0.3f,  0.3f, 0.3f,-0.3f, -0.3f, 0.3f, 0.3f,  0.3f, 0.3f,-0.3f, -0.3f, 0.3f,-0.3f,
            -0.3f,-0.3f,-0.3f,  0.3f,-0.3f,-0.3f,  0.3f,-0.3f, 0.3f, -0.3f,-0.3f,-0.3f,  0.3f,-0.3f, 0.3f, -0.3f,-0.3f, 0.3f,
             0.3f,-0.3f, 0.3f,  0.3f,-0.3f,-0.3f,  0.3f, 0.3f,-0.3f,  0.3f,-0.3f, 0.3f,  0.3f, 0.3f,-0.3f,  0.3f, 0.3f, 0.3f,
            -0.3f,-0.3f,-0.3f, -0.3f,-0.3f, 0.3f, -0.3f, 0.3f, 0.3f, -0.3f,-0.3f,-0.3f, -0.3f, 0.3f, 0.3f, -0.3f, 0.3f,-0.3f
        };
        
        ByteBuffer vb = ByteBuffer.allocateDirect(verts.length * 4); vb.order(ByteOrder.nativeOrder());
        bodyVertexBuffer = vb.asFloatBuffer(); bodyVertexBuffer.put(verts); bodyVertexBuffer.position(0);
        
        ByteBuffer hvb = ByteBuffer.allocateDirect(headVerts.length * 4); hvb.order(ByteOrder.nativeOrder());
        headVertexBuffer = hvb.asFloatBuffer(); headVertexBuffer.put(headVerts); headVertexBuffer.position(0);
    }
    
    private void loadOrGenerate(int index) {
        File f = new File(ctx.getFilesDir(), "world_" + index + ".dat");
        if (f.exists()) {
            World w = new World();
            float[] pos = w.loadFromFile(f);
            if (pos != null) { world = w; player = new Player(pos[0], pos[1], pos[2]); player.yaw = pos[3]; player.pitch = pos[4]; fixPlayerPos(); return; }
        }
        world = new World(12345L + index * 100000L);
        player = new Player(64f, 25f, 64f);
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
    
    private void doSwitchWorld(int index) {
        world.saveToFile(saveFile, player.x, player.y, player.z, player.yaw, player.pitch);
        currentWorld = index;
        saveFile = new File(ctx.getFilesDir(), "world_" + index + ".dat");
        loadOrGenerate(index);
        rebuildAllChunks();
    }
    
    public void saveNow() { savePending = true; }
    public boolean worldExists(int index) { return new File(ctx.getFilesDir(), "world_" + index + ".dat").exists(); }
    
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
        GLES20.glEnable(GLES20.GL_DEPTH_TEST); GLES20.glDisable(GLES20.GL_CULL_FACE); GLES20.glCullFace(GLES20.GL_BACK);
        
        TextureManager.loadTextures(ctx); 
        TextureManager.loadPlayerSkin(ctx);
        
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
    
    private void drawMobPart(float x, float y, float z, float yaw, FloatBuffer vb, FloatBuffer texBuf, boolean isHead) {
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y + 0.5f, z);
        Matrix.rotateM(modelMatrix, 0, -yaw, 0, 1, 0);
        if (isHead) Matrix.translateM(modelMatrix, 0, 0, 0.3f, 0.6f);
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
        
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glEnableVertexAttribArray(positionHandle); GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vb);
        GLES20.glDisableVertexAttribArray(colorHandle);
        GLES20.glEnableVertexAttribArray(texCoordHandle); GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texBuf);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        GLES20.glDisableVertexAttribArray(positionHandle); GLES20.glDisableVertexAttribArray(texCoordHandle);
    }
    
    private void drawMob(Sheep s) {
        drawMobPart(s.x, s.y, s.z, s.yaw, bodyVertexBuffer, sheepBodyTex, false);
        drawMobPart(s.x, s.y, s.z, s.yaw, headVertexBuffer, sheepHeadTex, true);
    }
    
    private void drawMob(Dog d) {
        drawMobPart(d.x, d.y, d.z, d.yaw, bodyVertexBuffer, dogBodyTex, false);
        drawMobPart(d.x, d.y, d.z, d.yaw, headVertexBuffer, dogHeadTex, true);
    }
    
    private void drawDroppedItem(DroppedItem item) {
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, item.x, item.y + 0.15f, item.z);
        Matrix.rotateM(modelMatrix, 0, item.rotation, 0, 1, 0);
        Matrix.scaleM(modelMatrix, 0, 0.3f, 0.3f, 0.3f);
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        
        int texIdx = getTexIndex(item.type); if (texIdx == -1) texIdx = 0; int col = texIdx % TextureManager.GRID_SIZE;
        int row = texIdx / TextureManager.GRID_SIZE;
        float tileStep = 1.0f / TextureManager.GRID_SIZE;
        float epsilon = 0.001f;
        float u1 = col * tileStep + epsilon;
        float u2 = (col + 1) * tileStep - epsilon;
        float v2 = 1.0f - (row * tileStep) - epsilon;
        float v1 = 1.0f - ((row + 1) * tileStep) + epsilon;
        
        itemTexBuffer.position(0);
        for(int i=0; i<6; i++) {
            itemTexBuffer.put(u1); itemTexBuffer.put(v1); itemTexBuffer.put(u2); itemTexBuffer.put(v1); itemTexBuffer.put(u2); itemTexBuffer.put(v2);
            itemTexBuffer.put(u1); itemTexBuffer.put(v1); itemTexBuffer.put(u2); itemTexBuffer.put(v2); itemTexBuffer.put(u1); itemTexBuffer.put(v2);
        }
        itemTexBuffer.position(0);
        
        GLES20.glEnableVertexAttribArray(positionHandle); GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, bodyVertexBuffer);
        GLES20.glDisableVertexAttribArray(colorHandle);
        GLES20.glEnableVertexAttribArray(texCoordHandle); GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, itemTexBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        GLES20.glDisableVertexAttribArray(positionHandle); GLES20.glDisableVertexAttribArray(texCoordHandle);
    }

    private void drawPlayer(float x, float y, float z, float yaw, float pitch, boolean isLocal) {
        if (TextureManager.skinTextureId == -1) return;

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y + 0.9f, z);
        Matrix.rotateM(modelMatrix, 0, -yaw, 0, 1, 0);
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glEnableVertexAttribArray(positionHandle); GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, playerModel.vertexBuffer);
        GLES20.glDisableVertexAttribArray(colorHandle);
        GLES20.glEnableVertexAttribArray(texCoordHandle); GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, playerModel.texBuffer);
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, TextureManager.skinTextureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, playerModel.vertexCount);
        GLES20.glDisableVertexAttribArray(positionHandle); GLES20.glDisableVertexAttribArray(texCoordHandle);
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, TextureManager.atlasId);
    }
    
    @Override public void onDrawFrame(GL10 unused) {
        if (TextureManager.skinNeedsReload) {
            TextureManager.loadPlayerSkin(ctx);
            TextureManager.skinNeedsReload = false;
        }

        if (pendingSwitch != -1) {
            int n = pendingSwitch;
            pendingSwitch = -1;
            if (n != currentWorld) doSwitchWorld(n);
        }
        
        if (pendingDelete) {
            pendingDelete = false;
            if (saveFile.exists()) saveFile.delete();
            loadOrGenerate(currentWorld);
            rebuildAllChunks();
        }
        
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        player.update(world);
        for (Sheep s : world.sheepList) s.update(world);
        for (Dog d : world.dogList) d.update(world);
        
        for (int i = world.droppedItems.size() - 1; i >= 0; i--) {
            DroppedItem item = world.droppedItems.get(i);
            item.update(world);
            float dx = player.x - item.x, dy = player.y - item.y, dz = player.z - item.z;
            if (dx*dx + dy*dy + dz*dz < 1.5f && item.pickupDelay == 0) {
                Inventory.addItem(item.type); world.droppedItems.remove(i);
            } else if (item.y > 500) world.droppedItems.remove(i);
        }
        
        if (savePending) {
            savePending = false;
            final float px = player.x, py = player.y, pz = player.z, yw = player.yaw, pt = player.pitch;
            final File f = saveFile; final World w = world;
            new Thread(new Runnable() { @Override public void run() { w.saveToFile(f, px, py, pz, yw, pt); } }).start();
        }
        
        float yawRad = (float)Math.toRadians(player.yaw), pitchRad = (float)Math.toRadians(player.pitch);
        float cosPitch = (float)Math.cos(pitchRad);
        float lookX = player.x + (float)Math.sin(yawRad) * cosPitch, lookY = player.y + player.getEyeHeight() + (float)Math.sin(pitchRad), lookZ = player.z - (float)Math.cos(yawRad) * cosPitch;
        float camX = player.x, camY = player.y + player.getEyeHeight(), camZ = player.z;
        
        if (player.thirdPerson) {
            float dist = 4.0f, step = 0.2f, curDist = 0;
            float stepX = -(float)Math.sin(yawRad) * cosPitch * step, stepY = -(float)Math.sin(pitchRad) * step, stepZ = (float)Math.cos(yawRad) * cosPitch * step;
            while (curDist < dist) {
                float nextX = camX + stepX, nextY = camY + stepY, nextZ = camZ + stepZ;
                if (Block.isSolid(world.getBlock((int)Math.floor(nextX), (int)Math.floor(nextY), (int)Math.floor(nextZ)))) break;
                camX = nextX; camY = nextY; camZ = nextZ; curDist += step;
            }
        }
        
        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, lookX, lookY, lookZ, 0, 1, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        frustum.update(mvpMatrix);
        
        GLES20.glUseProgram(program);
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        colorHandle = GLES20.glGetAttribLocation(program, "vColor");
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord");
        textureUniform = GLES20.glGetUniformLocation(program, "uTexture");
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, TextureManager.atlasId);
        GLES20.glUniform1i(textureUniform, 0);
        
        for (Chunk chunk : chunks) {
            if (chunk.getVertexCount() == 0) continue;
            if (!frustum.testSphere(chunk.centerX, chunk.centerY, chunk.centerZ, chunk.radius)) continue;
            GLES20.glEnableVertexAttribArray(positionHandle); GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, chunk.getVertexBuffer());
            GLES20.glEnableVertexAttribArray(colorHandle); GLES20.glVertexAttribPointer(colorHandle, 3, GLES20.GL_FLOAT, false, 0, chunk.getColorBuffer());
            GLES20.glEnableVertexAttribArray(texCoordHandle); GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, chunk.getTextureBuffer());
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, chunk.getVertexCount());
            GLES20.glDisableVertexAttribArray(positionHandle); GLES20.glDisableVertexAttribArray(colorHandle); GLES20.glDisableVertexAttribArray(texCoordHandle);
        }
        
        for (Sheep s : world.sheepList) drawMob(s);
        for (Dog d : world.dogList) drawMob(d);
        for (DroppedItem item : world.droppedItems) drawDroppedItem(item);
        
        if (player.thirdPerson) {
            drawPlayer(player.x, player.y, player.z, player.yaw, player.pitch, true);
        }
    }
    
    @Override public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        Matrix.perspectiveM(projectionMatrix, 0, 70f, (float) width / height, 0.05f, 400f);
    }
    
    private int loadShader(int type, String code) { int s = GLES20.glCreateShader(type); GLES20.glShaderSource(s, code); GLES20.glCompileShader(s); return s; }
    
    private int getTexIndex(byte block) {
        switch (block) {
            case Block.GRASS: return 0;
            case Block.DIRT: return 2;
            case Block.STONE: return 3;
            case Block.WOOD: return 5;
            case Block.LEAVES: return 6;
            case Block.PLANKS: return 7;
            case Block.COBBLESTONE: return 8;
            default: return -1;
        }
    }
    
    public int raycastSheep() {
        float yawRad = (float)Math.toRadians(player.yaw), pitchRad = (float)Math.toRadians(player.pitch);
        float cosPitch = (float)Math.cos(pitchRad);
        float dx = (float)Math.sin(yawRad) * cosPitch, dy = (float)Math.sin(pitchRad), dz = -(float)Math.cos(yawRad) * cosPitch;
        float px = player.x, py = player.y + player.getEyeHeight(), pz = player.z;
        for (int i = 0; i < 40; i++) {
            for (int j = 0; j < world.sheepList.size(); j++) {
                Sheep s = world.sheepList.get(j);
                float ddx = px - s.x, ddy = py - (s.y + 0.5f), ddz = pz - s.z;
                if (ddx*ddx + ddy*ddy + ddz*dz < 0.8f) return j;
            }
            px += dx * 0.1f; py += dy * 0.1f; pz += dz * 0.1f;
        }
        return -1;
    }
    
    public int[] raycastBlock() {
        float yawRad = (float)Math.toRadians(player.yaw), pitchRad = (float)Math.toRadians(player.pitch);
        float cosPitch = (float)Math.cos(pitchRad);
        float dx = (float)Math.sin(yawRad) * cosPitch, dy = (float)Math.sin(pitchRad), dz = -(float)Math.cos(yawRad) * cosPitch;
        float px = player.x, py = player.y + player.getEyeHeight(), pz = player.z;
        float prevX = px, prevY = py, prevZ = pz;
        for (int i = 0; i < 80; i++) {
            int bx = (int)Math.floor(px), by = (int)Math.floor(py), bz = (int)Math.floor(pz);
            if (Block.isSolid(world.getBlock(bx, by, bz))) return new int[]{bx, by, bz, (int)Math.floor(prevX), (int)Math.floor(prevY), (int)Math.floor(prevZ)};
            prevX = px; prevY = py; prevZ = pz;
            px += dx * 0.1f; py += dy * 0.1f; pz += dz * 0.1f;
        }
        return null;
    }
}
