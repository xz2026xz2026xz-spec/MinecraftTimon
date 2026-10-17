package com.example.myminecraft;

public class Inventory {
    public static class Slot {
        public byte type = Block.AIR;
        public int count = 0;
    }
    
    public static Slot[] slots = new Slot[36];
    public static int selectedSlot = 0;
    
    static {
        for (int i = 0; i < 36; i++) slots[i] = new Slot();
    }
    
    public static byte getSelectedBlock() {
        return slots[selectedSlot].type;
    }
    
    public static void selectSlot(int i) {
        if (i >= 0 && i < 9) selectedSlot = i;
    }
    
    public static void addItem(byte type) {
        if (type == Block.AIR) return;
        // Ищем существующий стак
        for (int i = 0; i < 36; i++) {
            if (slots[i].type == type && slots[i].count < 64) {
                slots[i].count++;
                return;
            }
        }
        // Ищем пустой слот
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
    
    public static void dropItem(int index) {
        slots[index].type = Block.AIR;
        slots[index].count = 0;
    }
}
