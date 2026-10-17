package com.example.myminecraft;

public class DroppedItem {
    public float x, y, z;
    public float vy = 0;
    public byte type;
    public float rotation = 0;
    public int pickupDelay = 20; // Нельзя подобрать сразу (полсекунды)

    public DroppedItem(float x, float y, float z, byte type) {
        this.x = x; this.y = y; this.z = z;
        this.type = type;
    }

    public void update(World world) {
        // Гравитация
        vy -= 0.02f;
        if (vy < -0.3f) vy = -0.3f;
        
        float newY = y + vy;
        if (vy < 0) {
            int by = (int)Math.floor(newY);
            if (Block.isSolid(world.getBlock((int)Math.floor(x), by, (int)Math.floor(z)))) {
                y = by + 1; vy = 0;
            } else { y = newY; }
        } else { y = newY; }
        
        // Если упал в бездну - удаляем
        if (y < -5) y = 1000; // Телепортируем наверх, чтобы потом удалить

        // Вращение
        rotation += 3f;
        if (rotation > 360) rotation -= 360;

        if (pickupDelay > 0) pickupDelay--;
    }
}
