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
import android.graphics.RectF;
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
    
    private int movePointerId = -1; // Для квадратного джойстика
    private int lookPointerId = -1;
    
    // ID для кнопок действий (3 ряда по 2 кнопки)
    private int bSwordId = -1, bJumpId = -1;
    private int bSprintId = -1, bPlaceId = -1;
    private int bSneakId = -1, bDownId = -1;
    
    private float moveCenterX, moveCenterY, knobX, knobY;
    private float moveRadius = 150f;
    private float lastSwipeX, lastSwipeY;
    
    public boolean inventoryOpen = false, settingsOpen = false;
    public Bitmap skinBitmap = null;
    public Paint skinPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    
    public boolean leftHanded = false; 
    public int uiSizeMode = 1; 
    public float getUiScale() { return uiSizeMode == 0 ? 0.8f : (uiSizeMode == 2 ? 1.2f : 1.0f); }
    
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
        float scale = getUiScale();
        
        // Центр джойстика
        moveCenterX = leftHanded ? W - 200 * scale : 200 * scale;
        moveCenterY = H - 200 * scale;
        
        // Координаты сетки кнопок (3 ряда по 2 кнопки) справа
        float btnSize = 65 * scale;
        float gap = 10 * scale;
        float startX = leftHanded ? 100 * scale : W - 200 * scale;
        float startY = H - 280 * scale;
        
        float cx1 = startX, cx2 = startX + btnSize * 2 + gap;
        float cy1 = startY, cy2 = startY + btnSize * 2 + gap, cy3 = startY + (btnSize * 2 + gap) * 2;

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            float x = event.getX(index), y = event.getY(index);
            int pId = event.getPointerId(index);
            
            if (handleUIDown(x, y, W, H)) { overlay.invalidate(); return true; }
            if (inventoryOpen || settingsOpen) return true;
            
            // Джойстик (левая часть экрана)
            boolean isJoySide = leftHanded ? (x > W / 2) : (x < W / 2);
            if (isJoySide && movePointerId == -1) {
                movePointerId = pId;
                knobX = x; knobY = y;
            } 
            // Сетка кнопок
            else if (inRect(x, y, cx1, cy1, btnSize) && bSwordId == -1) { bSwordId = pId; doAction(true); }
            else if (inRect(x, y, cx2, cy1, btnSize) && bJumpId == -1) { bJumpId = pId; renderer.player.jump = true; }
            else if (inRect(x, y, cx1, cy2, btnSize) && bSprintId == -1) { bSprintId = pId; renderer.player.sprinting = true; }
            else if (inRect(x, y, cx2, cy2, btnSize) && bPlaceId == -1) { bPlaceId = pId; doAction(false); }
            else if (inRect(x, y, cx1, cy3, btnSize) && bSneakId == -1) { bSneakId = pId; renderer.player.sneaking = true; }
            else if (inRect(x, y, cx2, cy3, btnSize) && bDownId == -1) { bDownId = pId; } // Заглушка для стрелки вниз
            
            // Камера (остальная часть экрана)
            else if (!isJoySide && lookPointerId == -1) {
                lookPointerId = pId;
                lastSwipeX = x; lastSwipeY = y;
            }
            overlay.invalidate();
        } 
        else if (action == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < event.getPointerCount(); i++) {
                int pId = event.getPointerId(i);
                float x = event.getX(i), y = event.getY(i);
                if (pId == movePointerId) {
                    float dx = x - moveCenterX, dy = y - moveCenterY;
                    float dist = (float)Math.sqrt(dx*dx + dy*dy);
                    if (dist > moveRadius * scale) { dx = dx/dist*moveRadius*scale; dy = dy/dist*moveRadius*scale; }
                    knobX = moveCenterX + dx; knobY = moveCenterY + dy;
                    renderer.player.moveX = dx / (moveRadius * scale);
                    renderer.player.moveZ = -dy / (moveRadius * scale);
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
            if (pId == movePointerId) {
                movePointerId = -1; renderer.player.moveX = 0; renderer.player.moveZ = 0;
            } else if (pId == lookPointerId) lookPointerId = -1;
            else if (pId == bSwordId) bSwordId = -1;
            else if (pId == bJumpId) bJumpId = -1;
            else if (pId == bSprintId) { bSprintId = -1; renderer.player.sprinting = false; }
            else if (pId == bPlaceId) bPlaceId = -1;
            else if (pId == bSneakId) { bSneakId = -1; renderer.player.sneaking = false; }
            else if (pId == bDownId) bDownId = -1;
            overlay.invalidate();
        }
        return true;
    }
    
    private boolean inRect(float px, float py, float cx, float cy, float r) {
        return px >= cx && px <= cx + r * 2 && py >= cy && py <= cy + r * 2;
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
        float scale = getUiScale();
        if (inventoryOpen) {
            if (inRect(x, y, W - 60, 60, 45 * scale)) { inventoryOpen = false; return true; }
            return false;
        }
        if (settingsOpen) {
            if (inRect(x, y, W - 60, 60, 45 * scale)) { settingsOpen = false; return true; }
            
            int btnSize = (int)(110 * scale), gap = (int)(25 * scale);
            int totalW = 5 * btnSize + 4 * gap;
            int startX = (W - totalW) / 2, startY = H / 2 - 150;
            for (int i = 0; i < 5; i++) {
                int bx = startX + i * (btnSize + gap);
                if (x >= bx && x <= bx + btnSize && y >= startY && y <= startY + btnSize) {
                    if (renderer.currentWorld != i + 1) { renderer.requestSwitchWorld(i + 1); }
                    settingsOpen = false; return true;
                }
            }
            if (x > W/2 - 200 && x < W/2 + 200 && y > H/2 + 50 && y < H/2 + 130) { leftHanded = !leftHanded; return true; }
            if (x > W/2 - 200 && x < W/2 + 200 && y > H/2 + 150 && y < H/2 + 230) { uiSizeMode = (uiSizeMode + 1) % 3; return true; }
            if (x > W/2 - 200 && x < W/2 + 200 && y > H/2 + 250 && y < H/2 + 330) {
                Intent intent = new Intent(Intent.ACTION_PICK); intent.setType("image/*");
                startActivityForResult(intent, 100); return true;
            }
            return false;
        }
        
        if (inRect(x, y, W - 260 * scale, 60, 45 * scale)) { renderer.player.thirdPerson = !renderer.player.thirdPerson; return true; }
        if (inRect(x, y, W - 160 * scale, 60, 45 * scale)) { inventoryOpen = true; return true; }
        if (inRect(x, y, W - 60, 60, 45 * scale)) { settingsOpen = true; return true; }
        
        int slotSize = (int)(110 * scale), gap = (int)(10 * scale);
        int startX = (W - (9 * slotSize + 8 * gap)) / 2, startY = (int)(H - 140 * scale);
        for (int i = 0; i < 9; i++) {
            int sx = startX + i * (slotSize + gap);
            if (x >= sx && x <= sx + slotSize && y >= startY && y <= startY + slotSize) { Inventory.selectSlot(i); return true; }
        }
        return false;
    }
    
    private class OverlayView extends View {
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG), fill = new Paint(Paint.ANTI_ALIAS_FLAG), text = new Paint(Paint.ANTI_ALIAS_FLAG), bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        public OverlayView(Context ctx) {
            super(ctx);
            stroke.setColor(Color.argb(180, 255, 255, 255)); stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeWidth(4f);
            fill.setColor(Color.argb(100, 0, 0, 0)); fill.setStyle(Paint.Style.FILL);
            text.setColor(Color.WHITE); text.setTextSize(45f); text.setTextAlign(Paint.Align.CENTER); text.setAntiAlias(true);
            bg.setColor(Color.argb(150, 30, 30, 30)); bg.setStyle(Paint.Style.FILL);
        }
        
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            int W = getWidth(), H = getHeight();
            float scale = getUiScale();
            
            // Прицел
            float cx = W/2f, cy = H/2f;
            c.drawLine(cx-30, cy, cx-10, cy, stroke); c.drawLine(cx+10, cy, cx+30, cy, stroke);
            c.drawLine(cx, cy-30, cx, cy-10, stroke); c.drawLine(cx, cy+10, cx, cy+30, stroke);
            
            // Верхнее меню
            c.drawCircle(W - 260 * scale, 60, 45 * scale, bg); c.drawCircle(W - 260 * scale, 60, 45 * scale, stroke); 
            text.setTextSize(45f * scale); c.drawText(renderer.player.thirdPerson ? "3D" : "1D", W - 260 * scale, 60 + 15 * scale, text);
            c.drawCircle(W - 160 * scale, 60, 45 * scale, bg); c.drawCircle(W - 160 * scale, 60, 45 * scale, stroke); c.drawText("🎒", W - 160 * scale, 60 + 15 * scale, text);
            c.drawCircle(W - 60, 60, 45 * scale, bg); c.drawCircle(W - 60, 60, 45 * scale, stroke); c.drawText("⚙", W - 60, 60 + 15 * scale, text);
            
            // --- КВАДРАТНЫЙ ДЖОЙСТИК (D-PAD) ---
            RectF dpOuter = new RectF(moveCenterX - moveRadius*scale, moveCenterY - moveRadius*scale, moveCenterX + moveRadius*scale, moveCenterY + moveRadius*scale);
            c.drawRect(dpOuter, fill);
            c.drawRect(dpOuter, stroke);
            
            if (movePointerId != -1) {
                c.drawCircle(knobX, knobY, 50f * scale, bg);
                c.drawCircle(knobX, knobY, 50f * scale, stroke);
            } else {
                c.drawRect(moveCenterX - 40*scale, moveCenterY - 40*scale, moveCenterX + 40*scale, moveCenterY + 40*scale, stroke);
            }
            
            // --- СЕТКА КНОПОК ДЕЙСТВИЙ (3 ряда по 2) ---
            float btnSize = 65 * scale;
            float gap = 10 * scale;
            float startX = leftHanded ? 100 * scale : W - 200 * scale;
            float startY = H - 280 * scale;
            
            float cx1 = startX, cx2 = startX + btnSize * 2 + gap;
            float cy1 = startY, cy2 = startY + btnSize * 2 + gap, cy3 = startY + (btnSize * 2 + gap) * 2;
            
            drawPEBtn(c, cx1, cy1, btnSize, "⚔", bSwordId != -1);
            drawPEBtn(c, cx2, cy1, btnSize, "↑", bJumpId != -1);
            drawPEBtn(c, cx1, cy2, btnSize, "»", bSprintId != -1);
            drawPEBtn(c, cx2, cy2, btnSize, "+", bPlaceId != -1);
            drawPEBtn(c, cx1, cy3, btnSize, "✛", bSneakId != -1);
            drawPEBtn(c, cx2, cy3, btnSize, "↓", bDownId != -1);
            
            // Хотбар
            int slotSize = (int)(110 * scale), hGap = (int)(10 * scale);
            int hStartX = (W - (9 * slotSize + 8 * hGap)) / 2, hStartY = (int)(H - 140 * scale);
            for (int i = 0; i < 9; i++) {
                int sx = hStartX + i * (slotSize + hGap);
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setStyle(Paint.Style.FILL);
                p.setColor(Color.argb(i == Inventory.selectedSlot ? 220 : 100, 0, 0, 0));
                c.drawRect(sx, hStartY, sx+slotSize, hStartY+slotSize, p);
                c.drawRect(sx, hStartY, sx+slotSize, hStartY+slotSize, stroke);
                byte b = Inventory.slots[i];
                if (b != Block.AIR) {
                    float[] col = Block.getUITopColor(b);
                    p.setColor(Color.rgb((int)(col[0]*255), (int)(col[1]*255), (int)(col[2]*255)));
                    c.drawRect(sx+20, hStartY+20, sx+slotSize-20, hStartY+slotSize-20, p);
                }
            }
            if (inventoryOpen) drawInventory(c, W, H);
            if (settingsOpen) drawSettings(c, W, H);
        }
        
        private void drawPEBtn(Canvas c, float x, float y, float size, String label, boolean pressed) {
            RectF r = new RectF(x, y, x + size * 2, y + size * 2);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setStyle(Paint.Style.FILL);
            p.setColor(pressed ? Color.argb(150, 255, 255, 255) : Color.argb(100, 50, 50, 50));
            c.drawRect(r, p);
            p.setStyle(Paint.Style.STROKE);
            p.setColor(Color.WHITE);
            p.setStrokeWidth(4f);
            c.drawRect(r, p);
            
            text.setTextSize(size * 1.2f);
            c.drawText(label, x + size, y + size + size * 0.4f, text);
        }
        
        private void drawInventory(Canvas c, int W, int H) {
            Paint dim = new Paint(); dim.setColor(Color.argb(220, 0, 0, 0)); c.drawRect(0, 0, W, H, dim);
            text.setTextSize(60f); c.drawText("Инвентарь", W/2f, 100, text);
            c.drawCircle(W - 60, 60, 45, bg); c.drawCircle(W - 60, 60, 45, stroke); c.drawText("X", W - 60, 75, text);
            
            int slotSize = 100, hGap = 10;
            int startX = (W - (9 * slotSize + 8 * hGap)) / 2, startY = 150;
            for (int row = 0; row < 3; row++) for (int i = 0; i < 9; i++) c.drawRect(startX + i*(slotSize+hGap), startY + row*(slotSize+hGap), startX + i*(slotSize+hGap) + slotSize, startY + row*(slotSize+hGap) + slotSize, stroke);
            
            int cx = W/2 + 150, cy = H - 300;
            if (skinBitmap != null) c.drawBitmap(skinBitmap, null, new Rect(cx-100, cy-100, cx+100, cy+100), skinPaint);
            
            int hbY = H - 140, hbX = (W - (9 * 110 + 8 * 10)) / 2;
            for (int i = 0; i < 9; i++) c.drawRect(hbX + i * 120, hbY, hbX + i * 120 + 110, hbY+110, stroke);
        }
        
        private void drawSettings(Canvas c, int W, int H) {
            Paint dim = new Paint(); dim.setColor(Color.argb(220, 0, 0, 0)); c.drawRect(0, 0, W, H, dim);
            text.setTextSize(60f); c.drawText("Настройки", W/2f, 100, text);
            c.drawCircle(W - 60, 60, 45, bg); c.drawCircle(W - 60, 60, 45, stroke); c.drawText("X", W - 60, 75, text);
            
            text.setTextSize(40f); c.drawText("Выбор мира:", W/2f, H/2 - 200, text);
            int btnSize = 110, gap = 25;
            int totalW = 5 * btnSize + 4 * gap;
            int startX = (W - totalW) / 2, startY = H / 2 - 150;
            for (int i = 0; i < 5; i++) {
                int bx = startX + i * (btnSize + gap);
                boolean exists = renderer.worldExists(i + 1);
                boolean isCur = renderer.currentWorld == (i + 1);
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setStyle(Paint.Style.FILL);
                p.setColor(isCur ? Color.rgb(200, 160, 40) : (exists ? Color.rgb(80, 130, 200) : Color.rgb(70, 70, 70)));
                c.drawRect(bx, startY, bx + btnSize, startY + btnSize, p);
                c.drawRect(bx, startY, bx + btnSize, startY + btnSize, stroke);
                text.setTextSize(60f); c.drawText(String.valueOf(i + 1), bx + btnSize/2f, startY + btnSize/2f + 20, text);
            }
            
            Paint btnBg = new Paint(); btnBg.setColor(Color.rgb(80, 130, 200));
            text.setTextSize(36f); c.drawText("Раскладка", W/2f, H/2 + 30, text);
            c.drawRect(W/2 - 200, H/2 + 50, W/2 + 200, H/2 + 130, btnBg); c.drawRect(W/2 - 200, H/2 + 50, W/2 + 200, H/2 + 130, stroke);
            text.setTextSize(40f); c.drawText(leftHanded ? "Режим: Левша" : "Режим: Правша", W/2f, H/2 + 105, text);
            
            text.setTextSize(36f); c.drawText("Размер", W/2f, H/2 + 180, text);
            c.drawRect(W/2 - 200, H/2 + 150, W/2 + 200, H/2 + 230, btnBg); c.drawRect(W/2 - 200, H/2 + 150, W/2 + 200, H/2 + 230, stroke);
            text.setTextSize(40f); c.drawText(uiSizeMode == 0 ? "Маленький" : (uiSizeMode == 2 ? "Большой" : "Средний"), W/2f, H/2 + 205, text);
            
            text.setTextSize(36f); c.drawText("Скин", W/2f, H/2 + 280, text);
            c.drawRect(W/2 - 200, H/2 + 250, W/2 + 200, H/2 + 330, btnBg); c.drawRect(W/2 - 200, H/2 + 250, W/2 + 200, H/2 + 330, stroke);
            text.setTextSize(40f); c.drawText("Загрузить", W/2f, H/2 + 305, text);
        }
    }
    
    @Override protected void onPause() { super.onPause(); if (glView != null) glView.onPause(); if (renderer != null) renderer.saveNow(); }
    @Override protected void onResume() { super.onResume(); if (glView != null) glView.onResume(); }
}
