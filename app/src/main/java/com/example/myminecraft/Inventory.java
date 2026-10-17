package com.example.myminecraft;

public class Inventory {
    public static byte[] slots = new byte[36];
    public static int selectedSlot = 0;
    
    static {
        slots[0] = Block.GRASS;
        slots[1] = Block.DIRT;
        slots[2] = Block.STONE;
        slots[3] = Block.WOOD;
        slots[4] = Block.LEAVES;
    }
    
    public static byte getSelectedBlock() {
        return slots[selectedSlot];
    }
    
    public static void selectSlot(int i) {
        if (i >= 0 && i < 9) selectedSlot = i;
    }
}
