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
    public static Slot[] craftGrid = new Slot[4];
    public static Slot craftOutput = new Slot();
    
    public static int selectedSlot = 0;
    public static int movingSlot = -1;
    
    static {
        for (int i = 0; i < 36; i++) slots[i] = new Slot();
        for (int i = 0; i < 4; i++) craftGrid[i] = new Slot();
    }
    
    public static void updateCraft() {
        if (craftGrid[0].type == Block.WOOD && craftGrid[1].type == Block.WOOD &&
            craftGrid[2].type == Block.WOOD && craftGrid[3].type == Block.WOOD) {
            craftOutput.type = Block.PLANKS; // 1 дерево = 4 доски
            craftOutput.count = 4;
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
            for (int i = 0; i < 4; i++) {
                craftGrid[i].count--;
                if (craftGrid[i].count <= 0) craftGrid[i].type = Block.AIR;
            }
            updateCraft();
        }
    }
    
    public static void swapSlots(int i1, int i2) {
        Slot temp;
        if (i1 < 36 && i2 < 36) {
            temp = slots[i1]; slots[i1] = slots[i2]; slots[i2] = temp;
        } else if (i1 >= 36 && i2 >= 36) {
            temp = craftGrid[i1 - 36]; craftGrid[i1 - 36] = craftGrid[i2 - 36]; craftGrid[i2 - 36] = temp;
            updateCraft();
        } else if (i1 < 36 && i2 >= 36) {
            temp = slots[i1]; slots[i1] = craftGrid[i2 - 36]; craftGrid[i2 - 36] = temp;
            updateCraft();
        } else if (i1 >= 36 && i2 < 36) {
            temp = craftGrid[i1 - 36]; craftGrid[i1 - 36] = slots[i2]; slots[i2] = temp;
            updateCraft();
        }
    }
    
    public static void saveToFile(Context ctx) {
        try {
            FileOutputStream fos = ctx.openFileOutput("inventory.dat", Context.MODE_PRIVATE);
            DataOutputStream out = new DataOutputStream(fos);
            for (int i = 0; i < 36; i++) { out.writeByte(slots[i].type); out.writeInt(slots[i].count); }
            out.close();
        } catch (Exception e) {}
    }
    
    public static void loadFromFile(Context ctx) {
        try {
            FileInputStream fis = ctx.openFileInput("inventory.dat");
            DataInputStream in = new DataInputStream(fis);
            for (int i = 0; i < 36; i++) { slots[i].type = in.readByte(); slots[i].count = in.readInt(); }
            in.close();
        } catch (Exception e) {}
    }
    
    public static byte getSelectedBlock() { return slots[selectedSlot].type; }
    public static void selectSlot(int i) { if (i >= 0 && i < 9) selectedSlot = i; }
    
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
