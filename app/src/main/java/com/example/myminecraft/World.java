package com.example.myminecraft;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class World {
    public static final int SIZE_X = 128;
    public static final int SIZE_Z = 128;
    public static final int SIZE_Y = 32;
    
    private final byte[][][] blocks = new byte[SIZE_X][SIZE_Y][SIZE_Z];
    public List<Sheep> sheepList = new ArrayList<>();
    public List<Dog> dogList = new ArrayList<>();
    
    public World(long seed) { generate(seed); }
    public World() {}
    
    private void generate(long seed) {
        Random rnd = new Random(seed);
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                int height = 10;
                for (int y = 0; y < SIZE_Y; y++) {
                    if (y == 0) blocks[x][y][z] = Block.STONE;
                    else if (y < height - 3) blocks[x][y][z] = Block.STONE;
                    else if (y < height - 1) blocks[x][y][z] = Block.DIRT;
                    else if (y < height) blocks[x][y][z] = Block.GRASS;
                    else blocks[x][y][z] = Block.AIR;
                }
                if (rnd.nextDouble() < 0.01 && height < SIZE_Y - 6) placeTree(x, height, z);
            }
        }
        
        // 15 овечек
        sheepList.clear();
        for (int i = 0; i < 15; i++) {
            int sx = rnd.nextInt(SIZE_X);
            int sz = rnd.nextInt(SIZE_Z);
            sheepList.add(new Sheep(sx + 0.5f, 11f, sz + 0.5f));
        }

        // 5 собачек
        dogList.clear();
        for (int i = 0; i < 5; i++) {
            int dx = rnd.nextInt(SIZE_X);
            int dz = rnd.nextInt(SIZE_Z);
            dogList.add(new Dog(dx + 0.5f, 11f, dz + 0.5f));
        }
    }
    
    private void placeTree(int x, int y, int z) {
        int trunkHeight = 4 + (int)(Math.random() * 2);
        for (int i = 0; i < trunkHeight; i++) if (y + i < SIZE_Y) blocks[x][y + i][z] = Block.WOOD;
        int topY = y + trunkHeight;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    int nx = x + dx, ny = topY + dy, nz = z + dz;
                    if (nx < 0 || nx >= SIZE_X || ny < 0 || ny >= SIZE_Y || nz < 0 || nz >= SIZE_Z) continue;
                    if (blocks[nx][ny][nz] != Block.AIR) continue;
                    if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy) <= 3) blocks[nx][ny][nz] = Block.LEAVES;
                }
            }
        }
    }
    
    public byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= SIZE_X || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_Z) return Block.AIR;
        return blocks[x][y][z];
    }
    
    public void setBlock(int x, int y, int z, byte type) {
        if (x < 0 || x >= SIZE_X || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_Z) return;
        if (y == 0) return;
        blocks[x][y][z] = type;
    }
    
    public boolean isInsidePlayer(int bx, int by, int bz, float px, float py, float pz) {
        return bx + 1 > px - 0.3f && bx < px + 0.3f && bz + 1 > pz - 0.3f && bz < pz + 0.3f && by + 1 > py && by < py + 1.7f;
    }
    
    public boolean saveToFile(File f, float px, float py, float pz, float yaw, float pitch) {
        try {
            DataOutputStream out = new DataOutputStream(new FileOutputStream(f));
            out.writeInt(SIZE_X); out.writeInt(SIZE_Y); out.writeInt(SIZE_Z);
            for (int x = 0; x < SIZE_X; x++) for (int y = 0; y < SIZE_Y; y++) for (int z = 0; z < SIZE_Z; z++) out.writeByte(blocks[x][y][z]);
            out.writeFloat(px); out.writeFloat(py); out.writeFloat(pz); out.writeFloat(yaw); out.writeFloat(pitch);
            out.close(); return true;
        } catch (Exception e) { return false; }
    }
    
    public float[] loadFromFile(File f) {
        try {
            DataInputStream in = new DataInputStream(new FileInputStream(f));
            int sx = in.readInt(); int sy = in.readInt(); int sz = in.readInt();
            if (sx != SIZE_X || sy != SIZE_Y || sz != SIZE_Z) { in.close(); return null; }
            for (int x = 0; x < SIZE_X; x++) for (int y = 0; y < SIZE_Y; y++) for (int z = 0; z < SIZE_Z; z++) blocks[x][y][z] = in.readByte();
            float px = in.readFloat(); float py = in.readFloat(); float pz = in.readFloat(); float yaw = in.readFloat(); float pitch = in.readFloat();
            in.close(); return new float[]{px, py, pz, yaw, pitch};
        } catch (Exception e) { return null; }
    }
}
