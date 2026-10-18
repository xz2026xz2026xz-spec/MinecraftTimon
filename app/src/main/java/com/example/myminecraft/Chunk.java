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
    public float centerX, centerY, centerZ, radius;
    
    public Chunk(int startX, int startY, int startZ, int sizeX, int sizeY, int sizeZ) {
        this.startX = startX; this.startY = startY; this.startZ = startZ;
        this.sizeX = sizeX; this.sizeY = sizeY; this.sizeZ = sizeZ;
        this.centerX = startX + sizeX / 2f;
        this.centerY = startY + sizeY / 2f;
        this.centerZ = startZ + sizeZ / 2f;
        this.radius = (float)Math.sqrt(sizeX*sizeX + sizeY*sizeY + sizeZ*sizeZ) / 2f + 2.0f;
    }
    
    public void buildMesh(World world) {
        List<Float> verts = new ArrayList<>(), cols = new ArrayList<>(), texCoords = new ArrayList<>();
        
        for (int lx = 0; lx < sizeX; lx++) {
            for (int ly = 0; ly < sizeY; ly++) {
                for (int lz = 0; lz < sizeZ; lz++) {
                    int wx = startX + lx, wy = startY + ly, wz = startZ + lz;
                    byte block = world.getBlock(wx, wy, wz);
                    if (block == Block.AIR) continue;
                    
                    if (!Block.isSolid(world.getBlock(wx, wy + 1, wz))) {
                        addFace(verts, cols, texCoords, Block.getColor(block, 0), getTexIndex(block, 0),
                            wx, wy+1, wz+1,  wx+1, wy+1, wz+1,  wx+1, wy+1, wz, wx, wy+1, wz);
                    }
                    if (!Block.isSolid(world.getBlock(wx, wy - 1, wz))) {
                        addFace(verts, cols, texCoords, Block.getColor(block, 1), getTexIndex(block, 1),
                            wx, wy, wz,  wx+1, wy, wz,  wx+1, wy, wz+1,  wx, wy, wz+1);
                    }
                    if (!Block.isSolid(world.getBlock(wx, wy, wz - 1))) {
                        addFace(verts, cols, texCoords, Block.getColor(block, 2), getTexIndex(block, 2),
                            wx, wy, wz,  wx, wy+1, wz,  wx+1, wy+1, wz,  wx+1, wy, wz);
                    }
                    if (!Block.isSolid(world.getBlock(wx, wy, wz + 1))) {
                        addFace(verts, cols, texCoords, Block.getColor(block, 2), getTexIndex(block, 2),
                            wx+1, wy, wz+1,  wx+1, wy+1, wz+1,  wx, wy+1, wz+1,  wx, wy, wz+1);
                    }
                    if (!Block.isSolid(world.getBlock(wx - 1, wy, wz))) {
                        addFace(verts, cols, texCoords, Block.getColor(block, 2), getTexIndex(block, 2),
                            wx, wy, wz+1,  wx, wy+1, wz+1,  wx, wy+1, wz,  wx, wy, wz);
                    }
                    if (!Block.isSolid(world.getBlock(wx + 1, wy, wz))) {
                        addFace(verts, cols, texCoords, Block.getColor(block, 2), getTexIndex(block, 2),
                            wx+1, wy, wz,  wx+1, wy+1, wz,  wx+1, wy+1, wz+1,  wx+1, wy, wz+1);
                    }
                }
            }
        }
        vertexCount = verts.size() / 3;
        
        ByteBuffer bb = ByteBuffer.allocateDirect(verts.size() * 4); bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer(); for (int i = 0; i < verts.size(); i++) vertexBuffer.put(verts.get(i)); vertexBuffer.position(0);
        
        ByteBuffer cb = ByteBuffer.allocateDirect(cols.size() * 4); cb.order(ByteOrder.nativeOrder());
        colorBuffer = cb.asFloatBuffer(); for (int i = 0; i < cols.size(); i++) colorBuffer.put(cols.get(i)); colorBuffer.position(0);
        
        ByteBuffer tb = ByteBuffer.allocateDirect(texCoords.size() * 4); tb.order(ByteOrder.nativeOrder());
        textureBuffer = tb.asFloatBuffer(); for (int i = 0; i < texCoords.size(); i++) textureBuffer.put(texCoords.get(i)); textureBuffer.position(0);
    }
    
    private int getTexIndex(byte block, int face) {
        switch (block) {
            case Block.GRASS: return face == 0 ? 0 : (face == 1 ? 2 : 1);
            case Block.DIRT: return 2;
            case Block.STONE: return 3;
            case Block.WOOD: return face == 0 || face == 1 ? 4 : 5;
            case Block.LEAVES: return 6;
            case Block.PLANKS: return 7;
            case Block.COBBLESTONE: return 8;
            default: return -1;
        }
    }
    
    private void addFace(List<Float> verts, List<Float> cols, List<Float> texCoords, float[] c, int texIndex,
            float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
        
        verts.add(x1); verts.add(y1); verts.add(z1); verts.add(x2); verts.add(y2); verts.add(z2); verts.add(x3); verts.add(y3); verts.add(z3);
        verts.add(x1); verts.add(y1); verts.add(z1); verts.add(x3); verts.add(y3); verts.add(z3); verts.add(x4); verts.add(y4); verts.add(z4);
        for (int i = 0; i < 6; i++) { cols.add(c[0]); cols.add(c[1]); cols.add(c[2]); }
        
        if (texIndex == -1) {
            for (int i = 0; i < 12; i++) texCoords.add(-1f);
            return;
        }
        
        float tileStep = 1.0f / TextureManager.GRID_SIZE;
        int col = texIndex % TextureManager.GRID_SIZE;
        int row = texIndex / TextureManager.GRID_SIZE;
        
        // Отступаем от краев на 1 пиксель (чтобы не захватывать соседние текстуры)
        float epsilon = 0.001f; 
        
        float u1 = col * tileStep + epsilon;
        float u2 = (col + 1) * tileStep - epsilon;
        float v2 = 1.0f - (row * tileStep) - epsilon;
        float v1 = 1.0f - ((row + 1) * tileStep) + epsilon;
        
        texCoords.add(u1); texCoords.add(v1); texCoords.add(u2); texCoords.add(v1); texCoords.add(u2); texCoords.add(v2);
        texCoords.add(u1); texCoords.add(v1); texCoords.add(u2); texCoords.add(v2); texCoords.add(u1); texCoords.add(v2);
    }
    
    public FloatBuffer getVertexBuffer() { return vertexBuffer; }
    public FloatBuffer getColorBuffer() { return colorBuffer; }
    public FloatBuffer getTextureBuffer() { return textureBuffer; }
    public int getVertexCount() { return vertexCount; }
}
