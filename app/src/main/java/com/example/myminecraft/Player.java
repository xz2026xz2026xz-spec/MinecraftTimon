package com.example.myminecraft;

public class Player {
    public float x, y, z;
    public float yaw = 0f;
    public float pitch = 0f;
    public float vy = 0f;
    
    public static final float GRAVITY = -0.015f;
    public static final float JUMP = 0.20f;
    public static final float SPEED = 0.10f;
    
    public float moveX = 0f;
    public float moveZ = 0f;
    public boolean jump = false;
    
    public Player(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public void update(World world) {
        if (moveX != 0 || moveZ != 0) {
            float len = (float)Math.sqrt(moveX*moveX + moveZ*moveZ);
            float nx = moveX / len;
            float nz = moveZ / len;
            
            float rad = (float)Math.toRadians(yaw);
            float sin = (float)Math.sin(rad);
            float cos = (float)Math.cos(rad);
            
            // forward = (sin, 0, -cos); right = (cos, 0, sin)
            float dx = (nx * cos + nz * sin) * SPEED;
            float dz = (nx * sin - nz * cos) * SPEED;
            
            tryMove(world, dx, dz);
        }
        
        if (jump && isOnGround(world)) vy = JUMP;
        jump = false;
        
        vy += GRAVITY;
        if (vy < -0.5f) vy = -0.5f;
        float newY = y + vy;
        
        if (vy < 0) {
            int by = (int)Math.floor(newY);
            if (Block.isSolid(world.getBlock((int)Math.floor(x), by, (int)Math.floor(z)))) {
                y = by + 1;
                vy = 0;
            } else {
                y = newY;
            }
        } else if (vy > 0) {
            int by = (int)Math.floor(newY + 1.7f);
            if (Block.isSolid(world.getBlock((int)Math.floor(x), by, (int)Math.floor(z)))) {
                vy = 0;
            } else {
                y = newY;
            }
        } else {
            y = newY;
        }
    }
    
    private void tryMove(World world, float dx, float dz) {
        float nx = x + dx;
        if (!collides(world, nx, y, z)) x = nx;
        float nz = z + dz;
        if (!collides(world, x, y, nz)) z = nz;
    }
    
    private boolean collides(World world, float px, float py, float pz) {
        int minX = (int)Math.floor(px - 0.25f);
        int maxX = (int)Math.floor(px + 0.25f);
        int minZ = (int)Math.floor(pz - 0.25f);
        int maxZ = (int)Math.floor(pz + 0.25f);
        int minY = (int)Math.floor(py);
        int maxY = (int)Math.floor(py + 1.6f);
        
        for (int bx = minX; bx <= maxX; bx++)
            for (int by = minY; by <= maxY; by++)
                for (int bz = minZ; bz <= maxZ; bz++)
                    if (Block.isSolid(world.getBlock(bx, by, bz))) return true;
        return false;
    }
    
    private boolean isOnGround(World world) {
        int by = (int)Math.floor(y - 0.05f);
        return Block.isSolid(world.getBlock((int)Math.floor(x), by, (int)Math.floor(z)));
    }
}
