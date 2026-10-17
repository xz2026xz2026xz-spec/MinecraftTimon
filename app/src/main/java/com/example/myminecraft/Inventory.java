package com.example.myminecraft;

public class Inventory {
    public static byte[] hotbar = {Block.GRASS, Block.DIRT, Block.STONE, Block.WOOD, Block.LEAVES, Block.AIR, Block.AIR, Block.AIR, Block.AIR};
    public static int selectedSlot = 0;
    
    public static byte getSelectedBlock() {
        return hotbar[selectedSlot];
    }
    
    public static void selectSlot(int i) {
        if (i >= 0 && i < 9) selectedSlot = i;
    }
}
