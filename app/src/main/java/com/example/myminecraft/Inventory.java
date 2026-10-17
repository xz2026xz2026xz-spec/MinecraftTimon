package com.example.myminecraft;

public class Inventory {
    public static class Slot {
        public byte type = Block.AIR;
        public int count = 0;
    }
    
    public static Slot[] slots = new Slot[36];
    public static int selectedSlot = 0;
    public static int movingSlot = -1; // Слот, который мы сейчас "держим"
    
    static {
        for (int i = 0; i < 36; i++) slots[i] = new Slot();
    }
    
    public static byte getSelectedBlock() {
        return slots[selectedSlot].type;
    }
    
    public static void selectSlot(int i) {
        if (i >= 0 && i < 36) selectedSlot = i;
    }
    
    public static void swapItems(int idx1, int idx2) {
        if (idx1 == idx2) return;
        Slot temp = slots[idx1];
        slots[idx1] = slots[idx2];
        slots[idx2] = temp;
    }
    
    public static void addItem(byte type) {
        if (type == Block.AIR) return;
        for (int i = 0; i < 36; i++) {
            if (slots[i].type == type && slots[i].count < 64) {
                slots[i].count++;
                return;
            }
        }
        for (int i = 0; i < 36; i++) {
            if (slots[i].type == Block.AIR) {
                slots[i].type = type;
                slots[i].count = 1;
                return;
            }
        }
    }
    
    public static void removeSelected() {
        if (slots[selectedSlot].count > 0) {
            slots[selectedSlot].count--;
            if (slots[selectedSlot].count == 0) slots[selectedSlot].type = Block.AIR;
        }
    }
    
    public static void dropSelected() {
        slots[selectedSlot].count--;
        if (slots[selectedSlot].count <= 0) {
            slots[selectedSlot].type = Block.AIR;
            slots[selectedSlot].count = 0;
        }
    }
}
