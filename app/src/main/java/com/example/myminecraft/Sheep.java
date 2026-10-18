package com.example.myminecraft;

import java.util.Random;

public class Sheep {
    public float x, y, z;
    public float yaw;
    public float vy = 0;
    public float moveX = 0, moveZ = 0;
    private float moveTimer = 40; // Пауза перед новым поворотом
    private Random rnd = new Random();

    public Sheep(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
        this.yaw = rnd.nextFloat() * 360;
    }

    public void update(World world) {
        vy -= 0.025f;
        if (vy < -0.4f) vy = -0.4f;
        float newY = y + vy;
        
        if (vy < 0) {
            int by = (int)Math.floor(newY);
            if (Block.isSolid(world.getBlock((int)Math.floor(x), by, (int)Math.floor(z)))) {
                y = by + 1; vy = 0;
            } else { y = newY; }
        } else { y = newY; }

        moveTimer -= 1;
        if (moveTimer <= 0) {
            moveTimer = 200 + rnd.nextInt(200);
            if (rnd.nextBoolean()) {
                moveX = 0; moveZ = 0;
            } else {
                float angle = rnd.nextFloat() * 360;
                // ИСПРАВЛЕНО: sin для X, cos для Z
                moveX = (float)Math.sin(Math.toRadians(angle)) * 0.015f; 
                moveZ = (float)Math.cos(Math.toRadians(angle)) * 0.015f;
                yaw = angle;
            }
        }

        if (moveX != 0 || moveZ != 0) {
            float nx = x + moveX;
            float nz = z + moveZ;
            if (!Block.isSolid(world.getBlock((int)Math.floor(nx), (int)Math.floor(y), (int)Math.floor(nz))) &&
                !Block.isSolid(world.getBlock((int)Math.floor(nx), (int)Math.floor(y + 1), (int)Math.floor(nz)))) {
                x = nx; z = nz;
            } else {
                moveTimer = 40; // Пауза перед новым поворотом
            }
        }
    }
}
