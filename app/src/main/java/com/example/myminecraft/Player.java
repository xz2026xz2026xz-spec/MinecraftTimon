package com.example.myminecraft;

public class Player {
    public float x, y, z;
    public float yaw = 0f;
    public float pitch = 0f;
    public float vy = 0f;
    public volatile boolean sneaking = false;
    public volatile boolean sprinting = false;
    public boolean thirdPerson = false;
    
    // Здоровье и голод
    public float health = 20.0f; // 0-20 (10 сердечек)
    public float hunger = 20.0f; // 0-20 (10 ножек)
    private float fallDistance = 0f;
    private int regenTimer = 0;
    
    public static final float GRAVITY = -0.0088f;
    public static final float JUMP = 0.15f;
    public static final float SPEED = 0.072f; 
    
    public float moveX = 0f;
    public float moveZ = 0f;
    public volatile boolean jump = false;
    
    public Player(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
    
    public float getEyeHeight() { return sneaking ? 1.0f : 1.6f; }
    public float getHeight() { return sneaking ? 1.1f : 1.7f; }
    
    public void respawn() {
        x = 64f; y = 25f; z = 64f;
        vy = 0; fallDistance = 0;
        health = 20f; hunger = 20f;
    }
    
    public void update(World world) {
        float speed = sneaking ? SPEED * 0.45f : (sprinting ? SPEED * 1.5f : SPEED);
        
        boolean isMoving = false;
        if (moveX != 0 || moveZ != 0) {
            isMoving = true;
            float len = (float)Math.sqrt(moveX*moveX + moveZ*moveZ);
            float nx = moveX / len, nz = moveZ / len;
            float rad = (float)Math.toRadians(yaw);
            float sin = (float)Math.sin(rad), cos = (float)Math.cos(rad);
            float dx = (nx * cos + nz * sin) * speed;
            float dz = (nx * sin - nz * cos) * speed;
            tryMove(world, dx, dz);
        }
        
        if (jump) {
            if (isOnGround(world) && !sneaking) vy = JUMP;
            jump = false;
        }
        
        vy += GRAVITY;
        if (vy < -0.3f) vy = -0.3f; 
        float newY = y + vy;
        
        // Проверка падения (урон)
        if (isOnGround(world)) {
            if (fallDistance > 3.5f) { // Безопасная высота - 3.5 блока
                health -= (fallDistance - 3.0f); // Урон от падения
                if (health <= 0) respawn();
            }
            fallDistance = 0;
        } else {
            if (vy < 0) fallDistance -= vy; // Считаем высоту падения
        }
        
        // --- ЛОГИКА ГОЛОДА ---
        if (isMoving) {
            hunger -= sprinting ? 0.003f : 0.001f; // Тратим голод при ходьбе/беге
            if (hunger < 0) hunger = 0;
        }
        
        // --- ЛОГИКА РЕГЕНЕРАЦИИ / ГОЛОДАНИЯ ---
        regenTimer++;
        if (regenTimer > 120) { // Раз в 2 секунды (при 60 FPS)
            regenTimer = 0;
            if (hunger >= 18.0f && health < 20.0f) {
                health += 1.0f; // Восстанавливаем полсердечка
                hunger -= 0.5f; // Тратим еду на реген
            } else if (hunger <= 0 && health > 0) {
                health -= 1.0f; // Урон от голода
                if (health <= 0) respawn();
            }
        }
        
        // Ограничители
        if (health > 20f) health = 20f;
        if (hunger > 20f) hunger = 20f;
        
        if (y < -5) respawn();
        
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
        int minX = (int)Math.floor(px - 0.25f), maxX = (int)Math.floor(px + 0.25f);
        int minZ = (int)Math.floor(pz - 0.25f), maxZ = (int)Math.floor(pz + 0.25f);
        int minY = (int)Math.floor(py + 0.05f), maxY = (int)Math.floor(py + getHeight() - 0.05f);
        for (int bx = minX; bx <= maxX; bx++)
            for (int by = minY; by <= maxY; by++)
                for (int bz = minZ; bz <= maxZ; bz++)
                    if (Block.isSolid(world.getBlock(bx, by, bz))) return true;
        return false;
    }
    
    private boolean isOnGround(World world) {
        int by = (int)Math.floor(y - 0.1f);
        return Block.isSolid(world.getBlock((int)Math.floor(x), by, (int)Math.floor(z)));
    }
}
