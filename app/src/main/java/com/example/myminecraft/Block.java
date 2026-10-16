package com.example.myminecraft;

public class Block {
    public static final byte AIR = 0;
    public static final byte GRASS = 1;
    public static final byte DIRT = 2;
    public static final byte STONE = 3;
    public static final byte WOOD = 4;
    public static final byte LEAVES = 5;
    
    private static final float[] GRASS_TOP = {0.3f, 0.8f, 0.3f};
    private static final float[] GRASS_BOTTOM = {0.4f, 0.3f, 0.2f};
    private static final float[] GRASS_SIDE = {0.5f, 0.4f, 0.25f};
    private static final float[] DIRT_COLOR = {0.5f, 0.35f, 0.2f};
    private static final float[] STONE_COLOR = {0.5f, 0.5f, 0.5f};
    private static final float[] WOOD_COLOR = {0.45f, 0.3f, 0.15f};
    private static final float[] LEAVES_COLOR = {0.2f, 0.6f, 0.2f};
    
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
            default: return "?";
        }
    }
    
    public static boolean isSolid(byte type) {
        return type != AIR;
    }
    
    public static final byte[] PLACEABLE = {GRASS, DIRT, STONE, WOOD, LEAVES};
}
