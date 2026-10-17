package com.example.myminecraft;

public class Block {
    public static final byte AIR = 0;
    public static final byte GRASS = 1;
    public static final byte DIRT = 2;
    public static final byte STONE = 3;
    public static final byte WOOD = 4;
    public static final byte LEAVES = 5;
    public static final byte MEAT = 6;
    public static final byte APPLE = 7;
    public static final byte PLANKS = 8;
    public static final byte COBBLESTONE = 9; // НОВЫЙ БЛОК
    
    private static final float[] GRASS_TOP = {0.3f, 0.8f, 0.3f};
    private static final float[] GRASS_BOTTOM = {0.4f, 0.3f, 0.2f};
    private static final float[] GRASS_SIDE = {0.5f, 0.4f, 0.25f};
    private static final float[] DIRT_COLOR = {0.5f, 0.35f, 0.2f};
    private static final float[] STONE_COLOR = {0.5f, 0.5f, 0.5f};
    private static final float[] WOOD_COLOR = {0.45f, 0.3f, 0.15f};
    private static final float[] LEAVES_COLOR = {0.2f, 0.6f, 0.2f};
    private static final float[] MEAT_COLOR = {0.8f, 0.2f, 0.2f};
    private static final float[] APPLE_COLOR = {0.9f, 0.1f, 0.1f};
    private static final float[] PLANKS_COLOR = {0.8f, 0.6f, 0.3f};
    
    public static float[] getColor(byte type, int face) {
        switch (type) {
            case GRASS:
                if (face == 0) return GRASS_TOP;
                if (face == 1) return GRASS_BOTTOM;
                return GRASS_SIDE;
            case DIRT: return DIRT_COLOR;
            case STONE: return STONE_COLOR;
            case WOOD: return WOOD_COLOR;
            case LEAVES: return LEAVES_COLOR;
            case PLANKS: return PLANKS_COLOR;
            case COBBLESTONE: return STONE_COLOR;
            default: return STONE_COLOR;
        }
    }
    
    public static float[] getUITopColor(byte type) {
        switch (type) {
            case GRASS: return GRASS_TOP;
            case DIRT: return DIRT_COLOR;
            case STONE: return STONE_COLOR;
            case WOOD: return WOOD_COLOR;
            case LEAVES: return LEAVES_COLOR;
            case MEAT: return MEAT_COLOR;
            case APPLE: return APPLE_COLOR;
            case PLANKS: return PLANKS_COLOR;
            case COBBLESTONE: return STONE_COLOR;
            default: return STONE_COLOR;
        }
    }
    
    public static String getName(byte type) {
        switch (type) {
            case GRASS: return "Трава";
            case DIRT: return "Земля";
            case STONE: return "Камень";
            case WOOD: return "Дерево";
            case LEAVES: return "Листва";
            case MEAT: return "Мясо";
            case APPLE: return "Яблоко";
            case PLANKS: return "Доски";
            case COBBLESTONE: return "Булыжник";
            default: return "?";
        }
    }
    
    public static boolean isSolid(byte type) { return type != AIR && type != MEAT && type != APPLE; }
    public static boolean isFood(byte type) { return type == MEAT || type == APPLE; }
}
