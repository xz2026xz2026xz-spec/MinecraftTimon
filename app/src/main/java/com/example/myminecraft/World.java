package com.example.myminecraft;

import java.util.Random;

public class World {
    public static final int SIZE_X = 64;
    public static final int SIZE_Z = 64;
    public static final int SIZE_Y = 32;
    public static final int CHUNK_SIZE = 16;
    
    private final byte[][][] blocks = new byte[SIZE_X][SIZE_Y][SIZE_Z];
    
    public World(long seed) {
        generate(seed);
    }
    
    private void generate(long seed) {
        Random rnd = new Random(seed);
        
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                double h = 10
                    + 3 * Math.sin(x * 0.15)
                    + 3 * Math.cos(z * 0.15)
                    + 2 * Math.sin((x + z) * 0.1)
                    + rnd.nextDouble() * 2;
                int height = (int) h;
                
                for (int y = 0; y < SIZE_Y; y++) {
                    if (y < height - 4) blocks[x][y][z] = Block.STONE;
                    else if (y < height - 1) blocks[x][y][z] = Block.DIRT;
                    else if (y < height) blocks[x][y][z] = Block.GRASS;
                    else blocks[x][y][z] = Block.AIR;
                }
                
                if (rnd.nextDouble() < 0.01 && height < SIZE_Y - 6) {
                    placeTree(x, height, z);
                }
            }
        }
    }
    
    private void placeTree(int x, int y, int z) {
        int trunkHeight = 4 + (int)(Math.random() * 2);
        for (int i = 0; i < trunkHeight; i++) {
            if (y + i < SIZE_Y) blocks[x][y + i][z] = Block.WOOD;
        }
        int topY = y + trunkHeight;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    int nx = x + dx, ny = topY + dy, nz = z + dz;
                    if (nx < 0 || nx >= SIZE_X || ny < 0 || ny >= SIZE_Y || nz < 0 || nz >= SIZE_Z) continue;
                    if (blocks[nx][ny][nz] != Block.AIR) continue;
                    if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy) <= 3) {
                        blocks[nx][ny][nz] = Block.LEAVES;
                    }
                }
            }
        }
    }
    
    public byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= SIZE_X || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_Z) {
            return Block.AIR;
        }
        return blocks[x][y][z];
    }
}
