package com.example.myminecraft;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {
    
    private GLSurfaceView glView;
    private MyGLRenderer renderer;
    private OverlayView overlay;
    
    private boolean isLoading = true;
    private int loadingProgress = 0;
    
    private float joyCenterX = 200, joyCenterY = 0;
    private float knobX = 200, knobY = 0;
    private boolean joyActive = false;
    private final float joyRadius = 150f;
    
    private boolean rightSideTouch = false;
    private float swipeStartX, swipeStartY;
    private float lastSwipeX, lastSwipeY;
    
    private boolean inventoryOpen = false;
    private boolean settingsOpen = false;
    private boolean sneakButtonPressed = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        renderer = new MyGLRenderer(this);
        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);
        glView.setRenderer(renderer);
        
        overlay = new OverlayView(this);
        
        FrameLayout layout = new FrameLayout(this);
        layout.addView(glView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        layout.addView(overlay, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        
        glView.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (isLoading) return true;
                return handleTouch(event);
            }
        });
        
        setContentView(layout);
        
        // Имитация загрузки
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isLoading = false;
                overlay.invalidate();
            }
        }, 3000);
    }
    
    private boolean handleTouch(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();
        
        if (action == MotionEvent.ACTION_DOWN) {
            if (handleUIDown(x, y)) { overlay.invalidate(); return true; }
            if (inventoryOpen || settingsOpen) return true;
        } else if (action == MotionEvent.ACTION_UP) {
            if (sneakButtonPressed) {
                renderer.player.sneaking = false;
                sneakButtonPressed = false;
                overlay.invalidate();
                return true;
            }
            if (inventoryOpen || settingsOpen) return true;
        }
        
        return handleGame(event);
    }
    
    private boolean handleUIDown(float x, float y) {
        int W = glView.getWidth();
        int H = glView.getHeight();
        
        if (inventoryOpen) {
            if (inCircle(x, y, W - 60, 60, 45)) { inventoryOpen = false; return true; }
            // Хотбар в инвентаре (внизу)
            int slotSize = 100, gap = 10;
            int startX = (W - (9 * slotSize + 8 * gap)) / 2;
            int startY = H - 120;
            for (int i = 0; i < 9; i++) {
                int sx = startX + i * (slotSize + gap);
                if (x >= sx && x <= sx + slotSize && y >= startY && y <= startY + slotSize) {
                    Inventory.selectSlot(i);
                    return true;
                }
            }
            return false;
        }
        
        if (settingsOpen) {
            if (inCircle(x, y, W - 60, 60, 45)) { settingsOpen = false; return true; }
            int btnW = 500, btnH = 110;
            int delX = (W - btnW) / 2, delY = H / 2 - 100;
            
            if (x >= delX && x <= delX + btnW && y >= delY && y <= delY + btnH) {
                renderer.requestDeleteCurrent();
                Toast.makeText(this, "Мир " + renderer.currentWorld + " удалён", Toast.LENGTH_SHORT).show();
                settingsOpen = false;
                return true;
            }
            int closeY = delY + btnH + 30;
            if (x >= delX && x <= delX + btnW && y >= closeY && y <= closeY + btnH) {
                settingsOpen = false;
                return true;
            }
            return false;
        }
        
        if (inCircle(x, y, W - 60, 60, 45)) { settingsOpen = true; return true; }
        
        // Хотбар (быстрый выбор блока)
        int slotSize = 110, gap = 10;
        int startX = (W - (9 * slotSize + 8 * gap)) / 2;
        int startY = H - 140;
        for (int i = 0; i < 9; i++) {
            int sx = startX + i * (slotSize + gap);
            if (x >= sx && x <= sx + slotSize && y >= startY && y <= startY + slotSize) {
                Inventory.selectSlot(i);
                overlay.invalidate();
                return true;
            }
        }
        
        if (inCircle(x, y, W - 130, H - 280, 85)) { renderer.player.jump = true; return true; }
        if (inCircle(x, y, W - 310, H - 280, 70)) { renderer.player.sneaking = true; sneakButtonPressed = true; return true; }
        
        return false;
    }
    
    private boolean inCircle(float px, float py, float cx, float cy, float r) {
        float dx = px - cx, dy = py - cy;
        return dx*dx + dy*dy <= r*r;
    }
    
    private boolean handleGame(MotionEvent event) {
        int action = event.getActionMasked();
        int halfWidth = glView.getWidth() / 2;
        
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (event.getX() < halfWidth) {
                    joyActive = true;
                    joyCenterX = event.getX(); joyCenterY = event.getY();
                    knobX = joyCenterX; knobY = joyCenterY;
                    rightSideTouch = false;
                } else {
                    rightSideTouch = true;
                    swipeStartX = event.getX(); swipeStartY = event.getY();
                    lastSwipeX = swipeStartX; lastSwipeY = swipeStartY;
                }
                overlay.invalidate(); return true;
                
            case MotionEvent.ACTION_MOVE:
                if (joyActive) {
                    float dx = event.getX() - joyCenterX;
                    float dy = event.getY() - joyCenterY;
                    float dist = (float)Math.sqrt(dx*dx + dy*dy);
                    if (dist > joyRadius) { dx = dx/dist*joyRadius; dy = dy/dist*joyRadius; }
                    knobX = joyCenterX + dx; knobY = joyCenterY + dy;
                    renderer.player.moveX = dx / joyRadius;
                    renderer.player.moveZ = -dy / joyRadius;
                } else if (rightSideTouch) {
                    float dx = event.getX() - lastSwipeX;
                    float dy = event.getY() - lastSwipeY;
                    renderer.player.yaw += dx * 0.4f;
                    renderer.player.pitch -= dy * 0.3f;
                    if (renderer.player.pitch > 89) renderer.player.pitch = 89;
                    if (renderer.player.pitch < -89) renderer.player.pitch = -89;
                    lastSwipeX = event.getX(); lastSwipeY = event.getY();
                }
                overlay.invalidate(); return true;
                
            case MotionEvent.ACTION_UP:
                if (joyActive) {
                    joyActive = false;
                    renderer.player.moveX = 0; renderer.player.moveZ = 0;
                } else if (rightSideTouch) {
                    float totalDx = Math.abs(event.getX() - swipeStartX);
                    float totalDy = Math.abs(event.getY() - swipeStartY);
                    long duration = event.getEventTime() - event.getDownTime();
                    if (totalDx < 25 && totalDy < 25) {
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
                rightSideTouch = false;
                overlay.invalidate(); return true;
        }
        return false;
    }
    
    private class OverlayView extends View {
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint crossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint slotStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint slotFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        public OverlayView(Context ctx) {
            super(ctx);
            strokePaint.setColor(Color.argb(150, 255, 255, 255));
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(5f);
            fillPaint.setColor(Color.argb(140, 255, 255, 255));
            fillPaint.setStyle(Paint.Style.FILL);
            crossPaint.setColor(Color.WHITE);
            crossPaint.setStyle(Paint.Style.STROKE);
            crossPaint.setStrokeWidth(4f);
            slotStroke.setColor(Color.WHITE);
            slotStroke.setStyle(Paint.Style.STROKE);
            slotStroke.setStrokeWidth(5f);
            slotFill.setStyle(Paint.Style.FILL);
            bgPaint.setColor(Color.argb(200, 30, 30, 40));
            bgPaint.setStyle(Paint.Style.FILL);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(48f);
            textPaint.setAntiAlias(true);
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int W = getWidth(), H = getHeight();
            
            if (isLoading) {
                canvas.drawColor(Color.rgb(40, 40, 40)); // Темный фон
                textPaint.setTextSize(120f);
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setColor(Color.YELLOW);
                canvas.drawText("TimonCraft", W/2f, H/2f - 50, textPaint);
                
                textPaint.setTextSize(40f);
                textPaint.setColor(Color.WHITE);
                canvas.drawText("Загрузка мира...", W/2f, H/2f + 50, textPaint);
                return;
            }
            
            // Прицел
            float mx = W/2f, my = H/2f;
            float size = 35f, gap = 10f;
            canvas.drawLine(mx - size, my, mx - gap, my, crossPaint);
            canvas.drawLine(mx + gap, my, mx + size, my, crossPaint);
            canvas.drawLine(mx, my - size, mx, my - gap, crossPaint);
            canvas.drawLine(mx, my + gap, mx, my + size, crossPaint);
            
            // Джойстик
            float jx, jy;
            if (joyActive) { jx = joyCenterX; jy = joyCenterY; }
            else { jx = 200f; jy = H - 250f; }
            float kx = joyActive ? knobX : jx;
            float ky = joyActive ? knobY : jy;
            canvas.drawCircle(jx, jy, joyRadius, strokePaint);
            canvas.drawCircle(kx, ky, 55f, fillPaint);
            
            // Верхние кнопки (Настройки)
            drawCircleButton(canvas, W - 60, 60, 45, "⚙");
            
            // Прыжок и Присяд
            drawJumpButton(canvas, W - 130, H - 280, 85);
            drawSneakButton(canvas, W - 310, H - 280, 70);
            
            if (inventoryOpen) drawInventory(canvas, W, H);
            else drawHotbar(canvas, W, H);
            
            if (settingsOpen) drawSettings(canvas, W, H);
        }
        
        private void drawCircleButton(Canvas c, float cx, float cy, float r, String label) {
            c.drawCircle(cx, cy, r, bgPaint);
            c.drawCircle(cx, cy, r, strokePaint);
            textPaint.setTextSize(45f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            c.drawText(label, cx, cy + 15f, textPaint);
        }
        
        private void drawJumpButton(Canvas c, float cx, float cy, float r) {
            c.drawCircle(cx, cy, r, bgPaint);
            c.drawCircle(cx, cy, r, strokePaint);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.WHITE); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(8f);
            float arrowH = 40f;
            c.drawLine(cx, cy + arrowH/2, cx, cy - arrowH/2, p);
            c.drawLine(cx, cy - arrowH/2, cx - 25, cy - arrowH/2 + 25, p);
            c.drawLine(cx, cy - arrowH/2, cx + 25, cy - arrowH/2 + 25, p);
        }
        
        private void drawSneakButton(Canvas c, float cx, float cy, float r) {
            c.drawCircle(cx, cy, r, bgPaint);
            c.drawCircle(cx, cy, r, strokePaint);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.WHITE); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(8f);
            float arrowH = 35f;
            c.drawLine(cx, cy - arrowH/2, cx, cy + arrowH/2, p);
            c.drawLine(cx, cy + arrowH/2, cx - 22, cy + arrowH/2 - 22, p);
            c.drawLine(cx, cy + arrowH/2, cx + 22, cy + arrowH/2 - 22, p);
        }
        
        private void drawHotbar(Canvas c, int W, int H) {
            int slotSize = 110, gap = 10;
            int startX = (W - (9 * slotSize + 8 * gap)) / 2;
            int startY = H - 140;
            
            for (int i = 0; i < 9; i++) {
                int sx = startX + i * (slotSize + gap);
                boolean isSelected = (i == Inventory.selectedSlot);
                
                Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
                bg.setColor(Color.argb(isSelected ? 220 : 120, 50, 50, 50));
                c.drawRect(sx, startY, sx + slotSize, startY + slotSize, bg);
                
                if (isSelected) {
                    c.drawRect(sx-5, startY-5, sx+slotSize+5, startY+slotSize+5, strokePaint);
                } else {
                    c.drawRect(sx, startY, sx + slotSize, startY + slotSize, slotStroke);
                }
                
                byte b = Inventory.hotbar[i];
                if (b != Block.AIR) {
                    float[] col = Block.getUITopColor(b);
                    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                    p.setColor(Color.rgb((int)(col[0]*255), (int)(col[1]*255), (int)(col[2]*255)));
                    c.drawRect(sx+20, startY+20, sx+slotSize-20, startY+slotSize-20, p);
                }
            }
        }
        
        private void drawInventory(Canvas c, int W, int H) {
            Paint dim = new Paint();
            dim.setColor(Color.argb(200, 0, 0, 0));
            c.drawRect(0, 0, W, H, dim);
            
            // Верхняя панель (вкладки)
            textPaint.setTextSize(56f);
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(Color.WHITE);
            c.drawText("Инвентарь", 50, 80, textPaint);
            
            // Закрыть
            drawCircleButton(c, W - 60, 60, 45, "X");
            
            // Левая панель (Блоки/Крафт)
            int panelW = 400;
            int panelX = W/2 - panelW - 20;
            int panelY = 200;
            
            c.drawRect(panelX, panelY, panelX+panelW, panelY+400, bgPaint);
            c.drawRect(panelX, panelY, panelX+panelW, panelY+400, slotStroke);
            
            textPaint.setTextSize(40f);
            c.drawText("Крафт", panelX + 20, panelY + 50, textPaint);
            
            // Сетка 2x2 крафта
            int gridSize = 80;
            int gx = panelX + 50;
            int gy = panelY + 100;
            for(int i=0; i<2; i++) {
                for(int j=0; j<2; j++) {
                    c.drawRect(gx + i*(gridSize+10), gy + j*(gridSize+10), gx + i*(gridSize+10)+gridSize, gy + j*(gridSize+10)+gridSize, slotStroke);
                }
            }
            
            // Стрелка
            Paint arr = new Paint(Paint.ANTI_ALIAS_FLAG);
            arr.setColor(Color.GRAY); arr.setStrokeWidth(6f); arr.setStyle(Paint.Style.FILL);
            c.drawRect(gx+190, gy+70, gx+250, gy+90, arr);
            c.drawLine(gx+230, gy+50, gx+260, gy+80, arr);
            c.drawLine(gx+230, gy+110, gx+260, gy+80, arr);
            
            // Результат
            c.drawRect(gx+280, gy+60, gx+280+gridSize, gy+60+gridSize, slotStroke);
            
            // Правая панель (Персонаж)
            int rightX = W/2 + 20;
            c.drawRect(rightX, panelY, rightX+400, panelY+400, bgPaint);
            c.drawRect(rightX, panelY, rightX+400, panelY+400, slotStroke);
            
            c.drawText("Персонаж", rightX + 20, panelY + 50, textPaint);
            
            // Рисуем человечка из квадратиков
            Paint skin = new Paint(); skin.setColor(Color.rgb(255, 200, 150));
            Paint shirt = new Paint(); shirt.setColor(Color.rgb(0, 150, 200));
            Paint pants = new Paint(); pants.setColor(Color.rgb(80, 50, 200));
            
            int cx = rightX + 200;
            int cy = panelY + 150;
            c.drawRect(cx-40, cy, cx+40, cy+80, skin); // Голова
            c.drawRect(cx-50, cy+80, cx+50, cy+200, shirt); // Тело
            c.drawRect(cx-50, cy+200, cx-10, cy+320, pants); // Левая нога
            c.drawRect(cx+10, cy+200, cx+50, cy+320, pants); // Правая нога
            c.drawRect(cx-90, cy+80, cx-50, cy+200, shirt); // Левая рука
            c.drawRect(cx+50, cy+80, cx+90, cy+200, shirt); // Правая рука
            
            // Текст
            textPaint.setTextSize(35f);
            c.drawText("TimonCraft Player", rightX + 100, panelY + 360, textPaint);
            
            // Хотбар
            drawHotbar(c, W, H);
        }
        
        private void drawSettings(Canvas c, int W, int H) {
            Paint dim = new Paint();
            dim.setColor(Color.argb(180, 0, 0, 0));
            c.drawRect(0, 0, W, H, dim);
            
            textPaint.setTextSize(72f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            c.drawText("Настройки", W/2f, H/2 - 200, textPaint);
            
            int btnW = 500, btnH = 110;
            int delX = (W - btnW) / 2, delY = H / 2 - 100;
            
            Paint delFill = new Paint(Paint.ANTI_ALIAS_FLAG);
            delFill.setColor(Color.argb(230, 180, 60, 60));
            c.drawRect(delX, delY, delX + btnW, delY + btnH, delFill);
            c.drawRect(delX, delY, delX + btnW, delY + btnH, slotStroke);
            textPaint.setTextSize(46f);
            c.drawText("Удалить мир " + renderer.currentWorld, W/2f, delY + 72, textPaint);
            
            int closeY = delY + btnH + 30;
            Paint cFill = new Paint(Paint.ANTI_ALIAS_FLAG);
            cFill.setColor(Color.argb(230, 80, 80, 80));
            c.drawRect(delX, closeY, delX + btnW, closeY + btnH, cFill);
            c.drawRect(delX, closeY, delX + btnW, closeY + btnH, slotStroke);
            c.drawText("Закрыть", W/2f, closeY + 72, textPaint);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (glView != null) glView.onPause();
        if (renderer != null) renderer.saveNow();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (glView != null) glView.onResume();
    }
}
