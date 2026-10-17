package com.example.myminecraft;

public class Player {
    public float x, y, z;
    public float yaw = 0f;
    public float pitch = 0f;
    public float vy = 0f;
    public boolean sneaking = false;
    
    // Правильная физика (прыжок ~1.2 блока)
    public static final float GRAVITY = -0.045f;
    public static final float JUMP = 0.36f;
    public static final float SPEED = 0.12f; 
    
    public float moveX = 0f;
    public float moveZ = 0f;
    public boolean jump = false;
    
    public Player(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
    }
    
    public float getEyeHeight() { return sneaking ? 1.0f : 1.6f; }
    public float getHeight() { return sneaking ? 1.1f : 1.7f; }
    
    public void update(World world) {
        float speed = sneaking ? SPEED * 0.45f : SPEED;
        
        if (moveX != 0 || moveZ != 0) {
            float len = (float)Math.sqrt(moveX*moveX + moveZ*moveZ);
            float nx = moveX / len, nz = moveZ / len;
            float rad = (float)Math.toRadians(yaw);
            float sin = (float)Math.sin(rad), cos = (float)Math.cos(rad);
            float dx = (nx * cos + nz * sin) * speed;
            float dz = (nx * sin - nz * cos) * speed;
            tryMove(world, dx, dz);
        }
        
        if (jump && isOnGround(world) && !sneaking) vy = JUMP;
        jump = false;
        
        vy += GRAVITY;
        if (vy < -0.5f) vy = -0.5f; // Ограничение скорости падения
        float newY = y + vy;
        
        if (vy < 0) {
            int by = (int)Math.floor(newY);
            if (Block.isSolid(world.getBlock((int)Math.floor(x), by, (int)Math.floor(z)))) {
                y = by + 1; vy = 0;
            } else { y = newY; }
        } else if (vy > 0) {
            int by = (int)Math.floor(newY + getHeight());
            if (Block.isSolid(world.getBlock((int)Math.floor(x), by, (int)Math.floor(z)))) {
                vy = 0;
            } else { y = newY; }
        } else { y = newY; }
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
        
        // Отступ 0.05f защищает от застревания в полу, но позволяет заходить на блоки
        int minY = (int)Math.floor(py + 0.05f); 
        int maxY = (int)Math.floor(py + getHeight() - 0.05f);
        
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
