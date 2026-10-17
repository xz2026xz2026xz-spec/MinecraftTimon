package com.example.myminecraft;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    private GLSurfaceView glView;
    private MyGLRenderer renderer;
    private OverlayView overlay;
    
    private int joyPointerId = -1, lookPointerId = -1, breakPointerId = -1, placePointerId = -1;
    private float joyCenterX = 200, joyCenterY = 0, knobX = 200, knobY = 0;
    private float joyRadius = 150f, lastSwipeX, lastSwipeY;
    
    public boolean inventoryOpen = false, settingsOpen = false;
    public Bitmap skinBitmap = null;
    public Paint skinPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        loadSkin();
        
        renderer = new MyGLRenderer(this);
        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);
        glView.setRenderer(renderer);
        
        overlay = new OverlayView(this);
        FrameLayout layout = new FrameLayout(this);
        layout.addView(glView, new FrameLayout.LayoutParams(-1, -1));
        layout.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        
        glView.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) { return handleTouch(event); }
        });
        setContentView(layout);
    }
    
    private void loadSkin() {
        try {
            FileInputStream fis = openFileInput("skin.png");
            skinBitmap = BitmapFactory.decodeStream(fis); fis.close();
        } catch (Exception e) { skinBitmap = null; }
    }
    
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            try {
                InputStream is = getContentResolver().openInputStream(data.getData());
                Bitmap bmp = BitmapFactory.decodeStream(is);
                Bitmap scaled = Bitmap.createScaledBitmap(bmp, 256, 256, true);
                FileOutputStream fos = openFileOutput("skin.png", Context.MODE_PRIVATE);
                scaled.compress(Bitmap.CompressFormat.PNG, 100, fos); fos.close();
                loadSkin(); overlay.invalidate();
            } catch (Exception e) { Toast.makeText(this, "Ошибка загрузки", Toast.LENGTH_SHORT).show(); }
        }
    }
    
    private boolean handleTouch(MotionEvent event) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        int W = glView.getWidth(), H = glView.getHeight();
        
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            float x = event.getX(index), y = event.getY(index);
            if (handleUIDown(x, y, W, H)) { overlay.invalidate(); return true; }
            if (inventoryOpen || settingsOpen) return true;
            
            if (x < W / 3 && joyPointerId == -1) {
                joyPointerId = event.getPointerId(index);
                joyCenterX = x; joyCenterY = y; knobX = x; knobY = y;
            } else if (inCircle(x, y, W - 250, H - 150, 60) && breakPointerId == -1) {
                breakPointerId = event.getPointerId(index); doAction(true);
            } else if (inCircle(x, y, W - 110, H - 280, 60) && placePointerId == -1) {
                placePointerId = event.getPointerId(index); doAction(false);
            } else if (x > W / 3 && lookPointerId == -1) {
                lookPointerId = event.getPointerId(index);
                lastSwipeX = x; lastSwipeY = y;
            }
            overlay.invalidate();
        } 
        else if (action == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < event.getPointerCount(); i++) {
                int pId = event.getPointerId(i);
                float x = event.getX(i), y = event.getY(i);
                if (pId == joyPointerId) {
                    float dx = x - joyCenterX, dy = y - joyCenterY;
                    float dist = (float)Math.sqrt(dx*dx + dy*dy);
                    if (dist > joyRadius) { dx = dx/dist*joyRadius; dy = dy/dist*joyRadius; }
                    knobX = joyCenterX + dx; knobY = joyCenterY + dy;
                    renderer.player.moveX = dx / joyRadius;
                    renderer.player.moveZ = -dy / joyRadius;
                } else if (pId == lookPointerId) {
                    float dx = x - lastSwipeX, dy = y - lastSwipeY;
                    renderer.player.yaw += dx * 0.4f;
                    renderer.player.pitch -= dy * 0.3f;
                    if (renderer.player.pitch > 89) renderer.player.pitch = 89;
                    if (renderer.player.pitch < -89) renderer.player.pitch = -89;
                    lastSwipeX = x; lastSwipeY = y;
                }
            }
            overlay.invalidate();
        } 
        else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            int pId = event.getPointerId(index);
            if (pId == joyPointerId) {
                joyPointerId = -1;
                renderer.player.moveX = 0; renderer.player.moveZ = 0;
            } else if (pId == lookPointerId) lookPointerId = -1;
            else if (pId == breakPointerId) breakPointerId = -1;
            else if (pId == placePointerId) placePointerId = -1;
            overlay.invalidate();
        }
        return true;
    }
    
    private void doAction(boolean isBreak) {
        int[] block = renderer.raycastBlock();
        if (block != null) {
            if (isBreak) {
                renderer.world.setBlock(block[0], block[1], block[2], Block.AIR);
                renderer.markDirty(block[0], block[1], block[2]);
            } else {
                int bx = block[0], by = block[1] + 1, bz = block[2];
                if (!renderer.world.isInsidePlayer(bx, by, bz, renderer.player.x, renderer.player.y, renderer.player.z)) {
                    renderer.world.setBlock(bx, by, bz, Inventory.getSelectedBlock());
                    renderer.markDirty(bx, by, bz);
                }
            }
        }
    }
    
    private boolean handleUIDown(float x, float y, int W, int H) {
        if (inventoryOpen) {
            if (inCircle(x, y, W - 60, 60, 45)) { inventoryOpen = false; return true; }
            return false;
        }
        if (settingsOpen) {
            if (inCircle(x, y, W - 60, 60, 45)) { settingsOpen = false; return true; }
            if (x > W/2 - 200 && x < W/2 + 200 && y > H/2 - 50 && y < H/2 + 30) {
                Intent intent = new Intent(Intent.ACTION_PICK); intent.setType("image/*");
                startActivityForResult(intent, 100); return true;
            }
            return false;
        }
        
        // Кнопка переключения вида (1-е / 3-е лицо)
        if (inCircle(x, y, W - 260, 60, 45)) { renderer.player.thirdPerson = !renderer.player.thirdPerson; return true; }
        if (inCircle(x, y, W - 160, 60, 45)) { inventoryOpen = true; return true; }
        if (inCircle(x, y, W - 60, 60, 45)) { settingsOpen = true; return true; }
        
        int slotSize = 110, gap = 10;
        int startX = (W - (9 * slotSize + 8 * gap)) / 2, startY = H - 140;
        for (int i = 0; i < 9; i++) {
            int sx = startX + i * (slotSize + gap);
            if (x >= sx && x <= sx + slotSize && y >= startY && y <= startY + slotSize) { Inventory.selectSlot(i); return true; }
        }
        if (inCircle(x, y, W - 130, H - 280, 85)) { renderer.player.jump = true; return true; }
        if (inCircle(x, y, W - 310, H - 150, 70)) { renderer.player.sneaking = true; return true; }
        return false;
    }
    
    private boolean inCircle(float px, float py, float cx, float cy, float r) {
        float dx = px - cx, dy = py - cy; return dx*dx + dy*dy <= r*r;
    }
    
    private class OverlayView extends View {
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG), fill = new Paint(Paint.ANTI_ALIAS_FLAG), text = new Paint(Paint.ANTI_ALIAS_FLAG), bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        public OverlayView(Context ctx) {
            super(ctx);
            stroke.setColor(Color.WHITE); stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeWidth(5f);
            fill.setColor(Color.argb(100, 255, 255, 255)); fill.setStyle(Paint.Style.FILL);
            text.setColor(Color.WHITE); text.setTextSize(45f); text.setTextAlign(Paint.Align.CENTER); text.setAntiAlias(true);
            bg.setColor(Color.argb(200, 40, 40, 40)); bg.setStyle(Paint.Style.FILL);
        }
        
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            int W = getWidth(), H = getHeight();
            
            float cx = W/2f, cy = H/2f;
            c.drawLine(cx-30, cy, cx-10, cy, stroke); c.drawLine(cx+10, cy, cx+30, cy, stroke);
            c.drawLine(cx, cy-30, cx, cy-10, stroke); c.drawLine(cx, cy+10, cx, cy+30, stroke);
            
            c.drawCircle(W - 260, 60, 45, bg); c.drawCircle(W - 260, 60, 45, stroke); c.drawText(renderer.player.thirdPerson ? "3D" : "1D", W - 260, 75, text);
            c.drawCircle(W - 160, 60, 45, bg); c.drawCircle(W - 160, 60, 45, stroke); c.drawText("🎒", W - 160, 75, text);
            c.drawCircle(W - 60, 60, 45, bg); c.drawCircle(W - 60, 60, 45, stroke); c.drawText("⚙", W - 60, 75, text);
            
            c.drawCircle(W - 250, H - 150, 60, bg); c.drawCircle(W - 250, H - 150, 60, stroke); c.drawText("⛏", W - 250, H - 130, text);
            c.drawCircle(W - 110, H - 280, 60, bg); c.drawCircle(W - 110, H - 280, 60, stroke); c.drawText("🧱", W - 110, H - 260, text);
            c.drawCircle(W - 130, H - 280, 85, bg); c.drawCircle(W - 130, H - 280, 85, stroke); c.drawText("↑", W - 130, H - 255, text);
            c.drawCircle(W - 310, H - 150, 70, bg); c.drawCircle(W - 310, H - 150, 70, stroke); c.drawText("↓", W - 310, H - 130, text);
            
            if (joyPointerId != -1) {
                c.drawCircle(joyCenterX, joyCenterY, joyRadius, stroke); c.drawCircle(knobX, knobY, 55f, fill);
            } else {
                c.drawCircle(200, H - 250, joyRadius, stroke); c.drawCircle(200, H - 250, 55f, fill);
            }
            
            int slotSize = 110, gap = 10;
            int startX = (W - (9 * slotSize + 8 * gap)) / 2, startY = H - 140;
            for (int i = 0; i < 9; i++) {
                int sx = startX + i * (slotSize + gap);
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setStyle(Paint.Style.FILL);
                p.setColor(Color.argb(i == Inventory.selectedSlot ? 220 : 100, 0, 0, 0));
                c.drawRect(sx, startY, sx+slotSize, startY+slotSize, p);
                c.drawRect(sx, startY, sx+slotSize, startY+slotSize, stroke);
                byte b = Inventory.slots[i];
                if (b != Block.AIR) {
                    float[] col = Block.getUITopColor(b);
                    p.setColor(Color.rgb((int)(col[0]*255), (int)(col[1]*255), (int)(col[2]*255)));
                    c.drawRect(sx+20, startY+20, sx+slotSize-20, startY+slotSize-20, p);
                }
            }
            if (inventoryOpen) drawInventory(c, W, H);
            if (settingsOpen) drawSettings(c, W, H);
        }
        
        private void drawInventory(Canvas c, int W, int H) {
            Paint dim = new Paint(); dim.setColor(Color.argb(220, 0, 0, 0)); c.drawRect(0, 0, W, H, dim);
            text.setTextSize(60f); c.drawText("Инвентарь", W/2f, 100, text);
            c.drawCircle(W - 60, 60, 45, bg); c.drawCircle(W - 60, 60, 45, stroke); c.drawText("X", W - 60, 75, text);
            
            int slotSize = 100, gap = 10;
            int startX = (W - (9 * slotSize + 8 * gap)) / 2, startY = 150;
            for (int row = 0; row < 3; row++) for (int i = 0; i < 9; i++) c.drawRect(startX + i*(slotSize+gap), startY + row*(slotSize+gap), startX + i*(slotSize+gap) + slotSize, startY + row*(slotSize+gap) + slotSize, stroke);
            
            int gs = 80, gx = W/2 - 300, gy = H - 300;
            text.setTextSize(40f); c.drawText("Крафт", gx + 80, gy - 20, text);
            for(int i=0; i<2; i++) for(int j=0; j<2; j++) c.drawRect(gx + i*(gs+10), gy + j*(gs+10), gx + i*(gs+10)+gs, gy + j*(gs+10)+gs, stroke);
            c.drawRect(gx + 200, gy + 45, gx + 200 + gs, gy + 45 + gs, stroke);
            
            int cx = W/2 + 150, cy = H - 350;
            if (skinBitmap != null) c.drawBitmap(skinBitmap, null, new Rect(cx-100, cy, cx+100, cy+200), skinPaint);
            
            int hbY = H - 140, hbX = (W - (9 * 110 + 8 * 10)) / 2;
            for (int i = 0; i < 9; i++) c.drawRect(hbX + i * 120, hbY, hbX + i * 120 + 110, hbY+110, stroke);
        }
        
        private void drawSettings(Canvas c, int W, int H) {
            Paint dim = new Paint(); dim.setColor(Color.argb(220, 0, 0, 0)); c.drawRect(0, 0, W, H, dim);
            text.setTextSize(60f); c.drawText("Настройки", W/2f, 100, text);
            c.drawCircle(W - 60, 60, 45, bg); c.drawCircle(W - 60, 60, 45, stroke); c.drawText("X", W - 60, 75, text);
            text.setTextSize(45f); c.drawText("Скин персонажа:", W/2f, H/2 - 100, text);
            
            Paint btnBg = new Paint(); btnBg.setColor(Color.rgb(80, 130, 200));
            c.drawRect(W/2 - 200, H/2 - 50, W/2 + 200, H/2 + 30, btnBg);
            c.drawRect(W/2 - 200, H/2 - 50, W/2 + 200, H/2 + 30, stroke);
            text.setTextSize(40f); c.drawText("Загрузить из галереи", W/2f, H/2 + 5, text);
        }
    }
    
    @Override protected void onPause() { super.onPause(); if (glView != null) glView.onPause(); if (renderer != null) renderer.saveNow(); }
    @Override protected void onResume() { super.onResume(); if (glView != null) glView.onResume(); }
}
