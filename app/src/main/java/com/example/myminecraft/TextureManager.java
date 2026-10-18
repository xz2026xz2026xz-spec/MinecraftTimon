package com.example.myminecraft;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import java.io.FileInputStream;

public class TextureManager {
    public static int atlasId = -1;
    public static int skinTextureId = -1;
    public static boolean skinNeedsReload = false; // Текстура скина
    public static final int TILE_SIZE = 64;
    public static final int GRID_SIZE = 4; 
    public static final int ATLAS_SIZE = TILE_SIZE * GRID_SIZE;

    public static void loadTextures(Context ctx) {
        Bitmap atlas = Bitmap.createBitmap(ATLAS_SIZE, ATLAS_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(atlas);
        canvas.drawColor(Color.GRAY);
        
        Paint paint = new Paint(); paint.setFilterBitmap(false); 

        drawTile(ctx, canvas, paint, R.drawable.grass_top, 0, 0);   
        drawTile(ctx, canvas, paint, R.drawable.grass_side, 1, 0);  
        drawTile(ctx, canvas, paint, R.drawable.dirt, 2, 0);        
        drawTile(ctx, canvas, paint, R.drawable.stone, 3, 0);       
        drawTile(ctx, canvas, paint, R.drawable.wood_top, 0, 1);    
        drawTile(ctx, canvas, paint, R.drawable.wood_side, 1, 1);   
        drawTile(ctx, canvas, paint, R.drawable.leaves, 2, 1);      
        drawTile(ctx, canvas, paint, R.drawable.planks, 3, 1);      
        drawTile(ctx, canvas, paint, R.drawable.cobblestone, 0, 2); 
        drawTile(ctx, canvas, paint, R.drawable.sheep_face, 1, 2);  
        drawTile(ctx, canvas, paint, R.drawable.sheep_body, 2, 2);  
        drawTile(ctx, canvas, paint, R.drawable.dog_face, 3, 2);    
        drawTile(ctx, canvas, paint, R.drawable.dog_body, 0, 3);    

        atlasId = loadTexture(atlas);
        atlas.recycle();
    }

    public static void loadPlayerSkin(Context ctx) {
        try {
            Bitmap skin = BitmapFactory.decodeResource(ctx.getResources(), com.example.myminecraft.R.drawable.skin);
            if (skin != null) {
                skinTextureId = loadTexture(skin);
                skin.recycle();
            }
        } catch (Exception e) { skinTextureId = -1; }
    }

    private static void drawTile(Context ctx, Canvas canvas, Paint paint, int resId, int col, int row) {
        Bitmap bmp = BitmapFactory.decodeResource(ctx.getResources(), resId);
        Rect src = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
        Rect dst = new Rect(col * TILE_SIZE, row * TILE_SIZE, (col + 1) * TILE_SIZE, (row + 1) * TILE_SIZE);
        canvas.drawBitmap(bmp, src, dst, paint);
        bmp.recycle();
    }

    private static int loadTexture(Bitmap bitmap) {
        final int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);
        if (textureHandle[0] != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        }
        return textureHandle[0];
    }
}
