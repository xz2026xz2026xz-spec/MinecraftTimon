package com.example.myminecraft;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
    
    private GLSurfaceView glView;
    private MyGLRenderer renderer;
    private OverlayView overlay;
    
    private int joyPointerId = -1;
    private int lookPointerId = -1;
    
    private float joyCenterX = 200, joyCenterY = 0, knobX = 200, knobY = 0;
    private final float joyRadius = 150f;
    
    private float lastSwipeX, lastSwipeY, swipeStartX, swipeStartY;
    private boolean inventoryOpen = false;
    private boolean settingsOpen = false;
    private boolean sneakPressed = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        renderer = new MyGLRenderer(this);
        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);
        glView.setRenderer(renderer);
        
        overlay = new OverlayView(this);
        FrameLayout layout = new FrameLayout(this);
        layout.addView(glView, new FrameLayout.LayoutParams(-1, -1));
        layout.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        
        glView.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                return handleTouch(event);
            }
        });
        setContentView(layout);
    }
    
    private boolean handleTouch(MotionEvent event) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        int W = glView.getWidth();
        int H = glView.getHeight();
        
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            float x = event.getX(index);
            float y = event.getY(index);
            
            // Проверка кнопок UI при первом касании
            if (joyPointerId == -1 && lookPointerId == -1) {
                if (handleUIDown(x, y, W, H)) { overlay.invalidate(); return true; }
            }
            
            // Джойстик (левая половина)
            if (x < W / 2 && joyPointerId == -1 && !inventoryOpen && !settingsOpen) {
                joyPointerId = event.getPointerId(index);
                joyCenterX = x; joyCenterY = y; knobX = x; knobY = y;
            } 
            // Камера (правая половина)
            else if (x >= W / 2 && lookPointerId == -1 && !inventoryOpen && !settingsOpen) {
                lookPointerId = event.getPointerId(index);
                lastSwipeX = x; lastSwipeY = y;
                swipeStartX = x; swipeStartY = y;
            }
            overlay.invalidate();
        } 
        else if (action == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < event.getPointerCount(); i++) {
                int pId = event.getPointerId(i);
                float x = event.getX(i);
                float y = event.getY(i);
                
                if (pId == joyPointerId) {
                    float dx = x - joyCenterX, dy = y - joyCenterY;
                    float dist = (float)Math.sqrt(dx*dx + dy*dy);
                    if (dist > joyRadius) { dx = dx/dist*joyRadius; dy = dy/dist*joyRadius; }
                    knobX = joyCenterX + dx; knobY = joyCenterY + dy;
                    renderer.player.moveX = dx / joyRadius;
                    renderer.player.moveZ = -dy / joyRadius;
                } 
                else if (pId == lookPointerId) {
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
            } else if (pId == lookPointerId) {
                lookPointerId = -1;
                // Тап для ломания/установки
                float x = event.getX(index), y = event.getY(index);
                float totalDx = Math.abs(x - swipeStartX), totalDy = Math.abs(y - swipeStartY);
                long duration = event.getEventTime() - event.getDownTime();
                if (totalDx < 25 && totalDy < 25 && !inventoryOpen && !settingsOpen) {
                    int[] block = renderer.raycastBlock();
                    if (block != null) {
                        if (duration > 400) {
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
            }
            if (sneakPressed) {
                renderer.player.sneaking = false; sneakPressed = false;
            }
            overlay.invalidate();
        }
        return true;
    }
    
    private boolean handleUIDown(float x, float y, int W, int H) {
        if (inventoryOpen) {
            if (inCircle(x, y, W - 60, 60, 45)) { inventoryOpen = false; return true; }
            // Хотбар в инвентаре
            int slotSize = 100, gap = 10;
            int startX = (W - (9 * slotSize + 8 * gap)) / 2;
            int startY = H - 150;
            for (int i = 0; i < 9; i++) {
                int sx = startX + i * (slotSize + gap);
                if (x >= sx && x <= sx + slotSize && y >= startY && y <= startY + slotSize) {
                    Inventory.selectSlot(i); return true;
                }
            }
            return false;
        }
        
        if (settingsOpen) {
            if (inCircle(x, y, W - 60, 60, 45)) { settingsOpen = false; return true; }
            
            // Выбор 5 миров
            int btnSize = 110, gap = 25;
            int totalW = 5 * btnSize + 4 * gap;
            int startX = (W - totalW) / 2, startY = H / 2 - 150;
            for (int i = 0; i < 5; i++) {
                int bx = startX + i * (btnSize + gap);
                if (x >= bx && x <= bx + btnSize && y >= startY && y <= startY + btnSize) {
                    if (renderer.currentWorld != i + 1) {
                        renderer.requestSwitchWorld(i + 1);
                        Toast.makeText(this, "Загрузка мира " + (i + 1), Toast.LENGTH_SHORT).show();
                    }
                    settingsOpen = false; return true;
                }
            }
            return false;
        }
        
        // Кнопки на главном экране
        if (inCircle(x, y, W - 160, 60, 45)) { inventoryOpen = true; return true; } // Рюкзак
        if (inCircle(x, y, W - 60, 60, 45)) { settingsOpen = true; return true; }   // Шестеренка
        
        // Хотбар внизу
        int slotSize = 110, gap = 10;
        int startX = (W - (9 * slotSize + 8 * gap)) / 2, startY = H - 140;
        for (int i = 0; i < 9; i++) {
            int sx = startX + i * (slotSize + gap);
            if (x >= sx && x <= sx + slotSize && y >= startY && y <= startY + slotSize) {
                Inventory.selectSlot(i); return true;
            }
        }
        
        if (inCircle(x, y, W - 130, H - 280, 85)) { renderer.player.jump = true; return true; }
        if (inCircle(x, y, W - 310, H - 280, 70)) { renderer.player.sneaking = true; sneakPressed = true; return true; }
        
        return false;
    }
    
    private boolean inCircle(float px, float py, float cx, float cy, float r) {
        float dx = px - cx, dy = py - cy;
        return dx*dx + dy*dy <= r*r;
    }
    
    private class OverlayView extends View {
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        public OverlayView(Context ctx) {
            super(ctx);
            stroke.setColor(Color.WHITE); stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeWidth(5f);
            fill.setColor(Color.argb(100, 255, 255, 255)); fill.setStyle(Paint.Style.FILL);
            text.setColor(Color.WHITE); text.setTextSize(45f); text.setTextAlign(Paint.Align.CENTER); text.setAntiAlias(true);
            bg.setColor(Color.argb(200, 40, 40, 40)); bg.setStyle(Paint.Style.FILL);
        }
        
        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            int W = getWidth(), H = getHeight();
            
            // Прицел
            float cx = W/2f, cy = H/2f;
            c.drawLine(cx-30, cy, cx-10, cy, stroke);
            c.drawLine(cx+10, cy, cx+30, cy, stroke);
            c.drawLine(cx, cy-30, cx, cy-10, stroke);
            c.drawLine(cx, cy+10, cx, cy+30, stroke);
            
            // Кнопки управления
            c.drawCircle(W - 60, 60, 45, bg); c.drawCircle(W - 60, 60, 45, stroke); c.drawText("⚙", W - 60, 75, text);
            c.drawCircle(W - 160, 60, 45, bg); c.drawCircle(W - 160, 60, 45, stroke); c.drawText("🎒", W - 160, 75, text);
            
            // Прыжок и Присяд
            c.drawCircle(W - 130, H - 280, 85, bg); c.drawCircle(W - 130, H - 280, 85, stroke); c.drawText("↑", W - 130, H - 255, text);
            c.drawCircle(W - 310, H - 280, 70, bg); c.drawCircle(W - 310, H - 280, 70, stroke); c.drawText("↓", W - 310, H - 260, text);
            
            // Джойстик
            if (joyPointerId != -1) {
                c.drawCircle(joyCenterX, joyCenterY, joyRadius, stroke);
                c.drawCircle(knobX, knobY, 55f, fill);
            } else {
                c.drawCircle(200, H - 250, joyRadius, stroke);
                c.drawCircle(200, H - 250, 55f, fill);
            }
            
            // Хотбар
            int slotSize = 110, gap = 10;
            int startX = (W - (9 * slotSize + 8 * gap)) / 2, startY = H - 140;
            for (int i = 0; i < 9; i++) {
                int sx = startX + i * (slotSize + gap);
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setStyle(Paint.Style.FILL);
                p.setColor(Color.argb(i == Inventory.selectedSlot ? 220 : 100, 0, 0, 0));
                c.drawRect(sx, startY, sx+slotSize, startY+slotSize, p);
                c.drawRect(sx, startY, sx+slotSize, startY+slotSize, stroke);
                
                byte b = Inventory.hotbar[i];
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
            
            // 2x2 Крафт (визуально)
            int gs = 100, gx = W/2 - 200, gy = 200;
            text.setTextSize(40f); c.drawText("Крафт", gx + 100, gy - 30, text);
            for(int i=0; i<2; i++) for(int j=0; j<2; j++) {
                c.drawRect(gx + i*(gs+10), gy + j*(gs+10), gx + i*(gs+10)+gs, gy + j*(gs+10)+gs, stroke);
            }
            c.drawRect(gx + 250, gy + 55, gx + 350, gy + 155, stroke); // Слот результата
            
            // Персонаж (2D)
            int cx = W/2 + 200, cy = 250;
            Paint skin = new Paint(); skin.setColor(Color.rgb(255, 200, 150)); skin.setStyle(Paint.Style.FILL);
            Paint shirt = new Paint(); shirt.setColor(Color.rgb(0, 150, 200)); shirt.setStyle(Paint.Style.FILL);
            Paint pants = new Paint(); pants.setColor(Color.rgb(80, 50, 200)); pants.setStyle(Paint.Style.FILL);
            c.drawRect(cx-40, cy, cx+40, cy+80, skin);
            c.drawRect(cx-60, cy+80, cx+60, cy+220, shirt);
            c.drawRect(cx-50, cy+220, cx-10, cy+350, pants); c.drawRect(cx+10, cy+220, cx+50, cy+350, pants);
            c.drawRect(cx-100, cy+80, cx-60, cy+220, shirt); c.drawRect(cx+60, cy+80, cx+100, cy+220, shirt);
        }
        
        private void drawSettings(Canvas c, int W, int H) {
            Paint dim = new Paint(); dim.setColor(Color.argb(220, 0, 0, 0)); c.drawRect(0, 0, W, H, dim);
            text.setTextSize(60f); c.drawText("Настройки", W/2f, 100, text);
            c.drawCircle(W - 60, 60, 45, bg); c.drawCircle(W - 60, 60, 45, stroke); c.drawText("X", W - 60, 75, text);
            
            text.setTextSize(45f); c.drawText("Выбор мира:", W/2f, H/2 - 200, text);
            
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
                c.drawRect(bx, startY, bx + btnSize, startY + btnSize, isCur ? stroke : stroke); // Рамка
                text.setTextSize(60f); c.drawText(String.valueOf(i + 1), bx + btnSize/2f, startY + btnSize/2f + 20, text);
            }
        }
    }
    
    @Override protected void onPause() { super.onPause(); if (glView != null) glView.onPause(); if (renderer != null) renderer.saveNow(); }
    @Override protected void onResume() { super.onResume(); if (glView != null) glView.onResume(); }
}
