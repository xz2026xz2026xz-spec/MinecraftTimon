package com.example.myminecraft;

import android.content.Context;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Inventory {
    public static class Slot {
        public byte type = Block.AIR;
        public int count = 0;
    }
    
    public static Slot[] slots = new Slot[36];
    public static Slot[] craftGrid = new Slot[4]; // Сетка крафта 2x2
    public static Slot craftOutput = new Slot();  // Слот результата
    
    public static int selectedSlot = 0;
    public static int movingSlot = -1;
    
    static {
        for (int i = 0; i < 36; i++) slots[i] = new Slot();
        for (int i = 0; i < 4; i++) craftGrid[i] = new Slot();
    }
    
    public static void updateCraft() {
        // Рецепт: 4 блока дерева в сетке = 1 блок дерева
        if (craftGrid[0].type == Block.WOOD && craftGrid[1].type == Block.WOOD &&
            craftGrid[2].type == Block.WOOD && craftGrid[3].type == Block.WOOD) {
            craftOutput.type = Block.WOOD;
            craftOutput.count = 1;
        } else {
            craftOutput.type = Block.AIR;
            craftOutput.count = 0;
        }
    }
    
    public static void takeCraftResult() {
        if (craftOutput.type != Block.AIR) {
            addItem(craftOutput.type);
            craftOutput.type = Block.AIR;
            craftOutput.count = 0;
            
            // Тратим ресурсы
            for (int i = 0; i < 4; i++) {
                craftGrid[i].count--;
                if (craftGrid[i].count <= 0) craftGrid[i].type = Block.AIR;
            }
            updateCraft();
        }
    }
    
    public static void saveToFile(Context ctx) {
        try {
            FileOutputStream fos = ctx.openFileOutput("inventory.dat", Context.MODE_PRIVATE);
            DataOutputStream out = new DataOutputStream(fos);
            for (int i = 0; i < 36; i++) {
                out.writeByte(slots[i].type);
                out.writeInt(slots[i].count);
            }
            out.close();
        } catch (Exception e) {}
    }
    
    public static void loadFromFile(Context ctx) {
        try {
            FileInputStream fis = ctx.openFileInput("inventory.dat");
            DataInputStream in = new DataInputStream(fis);
            for (int i = 0; i < 36; i++) {
                slots[i].type = in.readByte();
                slots[i].count = in.readInt();
            }
            in.close();
        } catch (Exception e) {}
    }
    
    public static byte getSelectedBlock() { return slots[selectedSlot].type; }
    public static void selectSlot(int i) { if (i >= 0 && i < 36) selectedSlot = i; }
    
    public static void swapItems(int idx1, int idx2) {
        if (idx1 == idx2) return;
        Slot temp = slots[idx1];
        slots[idx1] = slots[idx2];
        slots[idx2] = temp;
    }
    
    public static void addItem(byte type) {
        if (type == Block.AIR) return;
        for (int i = 0; i < 36; i++) {
            if (slots[i].type == type && slots[i].count < 64) { slots[i].count++; return; }
        }
        for (int i = 0; i < 36; i++) {
            if (slots[i].type == Block.AIR) { slots[i].type = type; slots[i].count = 1; return; }
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
        if (slots[selectedSlot].count <= 0) { slots[selectedSlot].type = Block.AIR; slots[selectedSlot].count = 0; }
    }
}
