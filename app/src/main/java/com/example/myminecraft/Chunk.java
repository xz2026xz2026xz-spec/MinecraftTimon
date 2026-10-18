package com.example.myminecraft;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public final int startX, startY, startZ, sizeX, sizeY, sizeZ;
    private FloatBuffer vertexBuffer, colorBuffer, textureBuffer;
    private int vertexCount = 0;

    public Chunk(int startX, int startY, int startZ, int sizeX, int sizeY, int sizeZ) {
        this.startX = startX; this.startY = startY; this.startZ = startZ;
        this.sizeX = sizeX; this.sizeY = sizeY; this.sizeZ = sizeZ;
    }

    private int getTexIndex(byte block, int face) {
        switch (block) {
            case Block.GRASS: return (face == 0 || face == 1) ? 0 : 2; // 0 - верх, 2 - бока
            case Block.DIRT: return 2;
            case Block.STONE: return 3;
            case Block.WOOD: return (face == 0 || face == 1) ? 4 : 5; // 4 - срез, 5 - бока
            case Block.LEAVES: return 6;
            case Block.PLANKS: return 7;
            case Block.COBBLESTONE: return 8;
            default: return -1;
        }
    }

    public void buildMesh(World world) {
        List<Float> verts = new ArrayList<>(), cols = new ArrayList<>(), texCoords = new ArrayList<>();
        
        for (int lx = 0; lx < sizeX; lx++) {
            for (int ly = 0; ly < sizeY; ly++) {
                for (int lz = 0; lz < sizeZ; lz++) {
                    int wx = startX + lx, wy = startY + ly, wz = startZ + lz;
                    byte block = world.getBlock(wx, wy, wz);
                    if (block == Block.AIR) continue;

                    if (!Block.isSolid(world.getBlock(wx, wy + 1, wz))) 
                        addFace(verts, cols, texCoords, Block.getColor(block, 0), getTexIndex(block, 0), wx, wy+1, wz, wx, wy+1, wz+1, wx+1, wy+1, wz+1, wx+1, wy+1, wz);
                    if (!Block.isSolid(world.getBlock(wx, wy - 1, wz))) 
                        addFace(verts, cols, texCoords, Block.getColor(block, 1), getTexIndex(block, 1), wx, wy, wz+1, wx, wy, wz, wx+1, wy, wz, wx+1, wy, wz+1);
                    if (!Block.isSolid(world.getBlock(wx, wy, wz - 1))) 
                        addFace(verts, cols, texCoords, Block.getColor(block, 2), getTexIndex(block, 2), wx, wy, wz, wx+1, wy, wz, wx+1, wy+1, wz, wx, wy+1, wz);
                    if (!Block.isSolid(world.getBlock(wx, wy, wz + 1))) 
                        addFace(verts, cols, texCoords, Block.getColor(block, 2), getTexIndex(block, 2), wx+1, wy, wz+1, wx, wy, wz+1, wx, wy+1, wz+1, wx+1, wy+1, wz+1);
                    if (!Block.isSolid(world.getBlock(wx - 1, wy, wz))) 
                        addFace(verts, cols, texCoords, Block.getColor(block, 2), getTexIndex(block, 2), wx, wy, wz+1, wx, wy, wz, wx, wy+1, wz, wx, wy+1, wz+1);
                    if (!Block.isSolid(world.getBlock(wx + 1, wy, wz))) 
                        addFace(verts, cols, texCoords, Block.getColor(block, 2), getTexIndex(block, 2), wx+1, wy, wz, wx+1, wy, wz+1, wx+1, wy+1, wz+1, wx+1, wy+1, wz);
                }
            }
        }
        vertexCount = verts.size() / 3;
        vertexBuffer = createBuffer(verts);
        colorBuffer = createBuffer(cols);
        textureBuffer = createBuffer(texCoords);
    }

    private void addFace(List<Float> v, List<Float> c, List<Float> t, float[] col, int texIndex, 
                         float x1, float y1, float z1, float x2, float y2, float z2, 
                         float x3, float y3, float z3, float x4, float y4, float z4) {
        v.add(x1); v.add(y1); v.add(z1); v.add(x2); v.add(y2); v.add(z2); v.add(x3); v.add(y3); v.add(z3);
        v.add(x1); v.add(y1); v.add(z1); v.add(x3); v.add(y3); v.add(z3); v.add(x4); v.add(y4); v.add(z4);
        for (int i = 0; i < 6; i++) { c.add(col[0]); c.add(col[1]); c.add(col[2]); }

        if (texIndex == -1) {
            for (int i = 0; i < 12; i++) t.add(-1f);
            return;
        }
        float step = 1.0f / TextureManager.GRID_SIZE;
        int cx = texIndex % TextureManager.GRID_SIZE;
        int cy = texIndex / TextureManager.GRID_SIZE;
        float u1 = cx * step + 0.001f, u2 = (cx + 1) * step - 0.001f;
        float v2 = 1.0f - (cy * step) - 0.001f, v1 = 1.0f - ((cy + 1) * step) + 0.001f;

        t.add(u1); t.add(v1); t.add(u2); t.add(v1); t.add(u2); t.add(v2);
        t.add(u1); t.add(v1); t.add(u2); t.add(v2); t.add(u1); t.add(v2);
    }

    private FloatBuffer createBuffer(List<Float> list) {
        ByteBuffer bb = ByteBuffer.allocateDirect(list.size() * 4); bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        for (Float f : list) fb.put(f);
        fb.position(0); return fb;
    }

    public FloatBuffer getVertexBuffer() { return vertexBuffer; }
    public FloatBuffer getColorBuffer() { return colorBuffer; }
    public FloatBuffer getTextureBuffer() { return textureBuffer; }
    public int getVertexCount() { return vertexCount; }
}
