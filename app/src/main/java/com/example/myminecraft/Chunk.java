package com.example.myminecraft;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public final int startX, startY, startZ;
    public final int sizeX, sizeY, sizeZ;
    
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private int vertexCount = 0;
    
    public Chunk(int startX, int startY, int startZ, int sizeX, int sizeY, int sizeZ) {
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }
    
    public void buildMesh(World world) {
        List<Float> verts = new ArrayList<>();
        List<Float> cols = new ArrayList<>();
        
        for (int lx = 0; lx < sizeX; lx++) {
            for (int ly = 0; ly < sizeY; ly++) {
                for (int lz = 0; lz < sizeZ; lz++) {
                    int wx = startX + lx;
                    int wy = startY + ly;
                    int wz = startZ + lz;
                    
                    byte block = world.getBlock(wx, wy, wz);
                    if (block == Block.AIR) continue;
                    
                    // TOP (+Y)
                    if (!Block.isSolid(world.getBlock(wx, wy + 1, wz))) {
                        addFace(verts, cols, Block.getColor(block, 0),
                            wx, wy+1, wz+1,  wx+1, wy+1, wz+1,
                            wx+1, wy+1, wz, wx, wy+1, wz);
                    }
                    // BOTTOM (-Y)
                    if (!Block.isSolid(world.getBlock(wx, wy - 1, wz))) {
                        addFace(verts, cols, Block.getColor(block, 1),
                            wx, wy, wz,  wx+1, wy, wz,
                            wx+1, wy, wz+1,  wx, wy, wz+1);
                    }
                    // FRONT (-Z)
                    if (!Block.isSolid(world.getBlock(wx, wy, wz - 1))) {
                        addFace(verts, cols, Block.getColor(block, 2),
                            wx, wy, wz,  wx, wy+1, wz,
                            wx+1, wy+1, wz,  wx+1, wy, wz);
                    }
                    // BACK (+Z)
                    if (!Block.isSolid(world.getBlock(wx, wy, wz + 1))) {
                        addFace(verts, cols, Block.getColor(block, 2),
                            wx+1, wy, wz+1,  wx+1, wy+1, wz+1,
                            wx, wy+1, wz+1,  wx, wy, wz+1);
                    }
                    // LEFT (-X)
                    if (!Block.isSolid(world.getBlock(wx - 1, wy, wz))) {
                        addFace(verts, cols, Block.getColor(block, 2),
                            wx, wy, wz+1,  wx, wy+1, wz+1,
                            wx, wy+1, wz,  wx, wy, wz);
                    }
                    // RIGHT (+X)
                    if (!Block.isSolid(world.getBlock(wx + 1, wy, wz))) {
                        addFace(verts, cols, Block.getColor(block, 2),
                            wx+1, wy, wz,  wx+1, wy+1, wz,
                            wx+1, wy+1, wz+1,  wx+1, wy, wz+1);
                    }
                }
            }
        }
        
        vertexCount = verts.size() / 3;
        
        ByteBuffer bb = ByteBuffer.allocateDirect(verts.size() * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        for (int i = 0; i < verts.size(); i++) vertexBuffer.put(verts.get(i));
        vertexBuffer.position(0);
        
        ByteBuffer cb = ByteBuffer.allocateDirect(cols.size() * 4);
        cb.order(ByteOrder.nativeOrder());
        colorBuffer = cb.asFloatBuffer();
        for (int i = 0; i < cols.size(); i++) colorBuffer.put(cols.get(i));
        colorBuffer.position(0);
    }
    
    private void addFace(List<Float> verts, List<Float> cols, float[] c,
            float x1, float y1, float z1, float x2, float y2, float z2,
            float x3, float y3, float z3, float x4, float y4, float z4) {
        verts.add(x1); verts.add(y1); verts.add(z1);
        verts.add(x2); verts.add(y2); verts.add(z2);
        verts.add(x3); verts.add(y3); verts.add(z3);
        verts.add(x1); verts.add(y1); verts.add(z1);
        verts.add(x3); verts.add(y3); verts.add(z3);
        verts.add(x4); verts.add(y4); verts.add(z4);
        for (int i = 0; i < 6; i++) {
            cols.add(c[0]); cols.add(c[1]); cols.add(c[2]);
        }
    }
    
    public FloatBuffer getVertexBuffer() { return vertexBuffer; }
    public FloatBuffer getColorBuffer() { return colorBuffer; }
    public int getVertexCount() { return vertexCount; }
}
