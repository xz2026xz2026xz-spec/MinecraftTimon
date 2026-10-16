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
                return handleTouch(event);
            }
        });
        
        setContentView(layout);
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
            byte[] place = Block.PLACEABLE;
            int slotSize = 130, gap = 20;
            int totalWidth = place.length * slotSize + (place.length - 1) * gap;
            int startX = (W - totalWidth) / 2, startY = H - 190;
            for (int i = 0; i < place.length; i++) {
                int sx = startX + i * (slotSize + gap);
                if (x >= sx && x <= sx + slotSize && y >= startY && y <= startY + slotSize) {
                    renderer.player.selectedBlock = place[i];
                    inventoryOpen = false;
                    return true;
                }
            }
            return false;
        }
        
        if (settingsOpen) {
            if (inCircle(x, y, W - 60, 60, 45)) { settingsOpen = false; return true; }
            
            int btnSize = 110, gap = 25;
            int totalW = 5 * btnSize + 4 * gap;
            int startX = (W - totalW) / 2;
            int startY = H / 2 - btnSize - 30;
            for (int i = 0; i < 5; i++) {
                int bx = startX + i * (btnSize + gap);
                if (x >= bx && x <= bx + btnSize && y >= startY && y <= startY + btnSize) {
                    if (renderer.currentWorld != i + 1) {
                        renderer.requestSwitchWorld(i + 1);
                        Toast.makeText(this, "Мир " + (i + 1), Toast.LENGTH_SHORT).show();
                    }
                    settingsOpen = false;
                    return true;
                }
            }
            
            int btnW = 500, btnH = 110;
            int delX = (W - btnW) / 2, delY = startY + btnSize + 40;
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
        
        if (inCircle(x, y, W - 160, 60, 45)) { inventoryOpen = true; return true; }
        if (inCircle(x, y, W - 60, 60, 45)) { settingsOpen = true; return true; }
        
        if (inCircle(x, y, W - 130, H - 130, 85)) {
            renderer.player.jump = true;
            return true;
        }
        
        if (inCircle(x, y, W - 310, H - 130, 70)) {
            renderer.player.sneaking = true;
            sneakButtonPressed = true;
            return true;
        }
        
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
                    joyCenterX = event.getX();
                    joyCenterY = event.getY();
                    knobX = joyCenterX;
                    knobY = joyCenterY;
                    rightSideTouch = false;
                } else {
                    rightSideTouch = true;
                    swipeStartX = event.getX();
                    swipeStartY = event.getY();
                    lastSwipeX = swipeStartX;
                    lastSwipeY = swipeStartY;
                }
                overlay.invalidate();
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (joyActive) {
                    float dx = event.getX() - joyCenterX;
                    float dy = event.getY() - joyCenterY;
                    float dist = (float)Math.sqrt(dx*dx + dy*dy);
                    if (dist > joyRadius) { dx = dx/dist*joyRadius; dy = dy/dist*joyRadius; }
                    knobX = joyCenterX + dx;
                    knobY = joyCenterY + dy;
                    renderer.player.moveX = dx / joyRadius;
                    renderer.player.moveZ = -dy / joyRadius;
                } else if (rightSideTouch) {
                    float dx = event.getX() - lastSwipeX;
                    float dy = event.getY() - lastSwipeY;
                    renderer.player.yaw += dx * 0.4f;
                    renderer.player.pitch -= dy * 0.3f;
                    if (renderer.player.pitch > 89) renderer.player.pitch = 89;
                    if (renderer.player.pitch < -89) renderer.player.pitch = -89;
                    lastSwipeX = event.getX();
                    lastSwipeY = event.getY();
                }
                overlay.invalidate();
                return true;
                
            case MotionEvent.ACTION_UP:
                if (joyActive) {
                    joyActive = false;
                    renderer.player.moveX = 0;
                    renderer.player.moveZ = 0;
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
                                if (!renderer.world.isInsidePlayer(bx, by, bz,
                                        renderer.player.x, renderer.player.y, renderer.player.z)) {
                                    renderer.world.setBlock(bx, by, bz, renderer.player.selectedBlock);
                                    renderer.markDirty(bx, by, bz);
                                }
                            }
                        }
                    }
                }
                rightSideTouch = false;
                overlay.invalidate();
                return true;
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
            
            drawCircleButton(canvas, W - 160, 60, 45, "инв");
            drawCircleButton(canvas, W - 60, 60, 45, "наст");
            drawJumpButton(canvas, W - 130, H - 130, 85);
            drawSneakButton(canvas, W - 310, H - 130, 70);
            drawSelectedBlock(canvas, 30, 30);
            
            textPaint.setTextSize(42f);
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(Color.WHITE);
            canvas.drawText("Мир " + renderer.currentWorld, 30, 220, textPaint);
            
            float mx = W/2f, my = H/2f;
            float size = 35f, gap = 10f;
            canvas.drawLine(mx - size, my, mx - gap, my, crossPaint);
            canvas.drawLine(mx + gap, my, mx + size, my, crossPaint);
            canvas.drawLine(mx, my - size, mx, my - gap, crossPaint);
            canvas.drawLine(mx, my + gap, mx, my + size, crossPaint);
            
            float jx, jy;
            if (joyActive) { jx = joyCenterX; jy = joyCenterY; }
            else { jx = 200f; jy = H - 200f; }
            float kx = joyActive ? knobX : jx;
            float ky = joyActive ? knobY : jy;
            canvas.drawCircle(jx, jy, joyRadius, strokePaint);
            canvas.drawCircle(kx, ky, 55f, fillPaint);
            
            if (renderer.player.sneaking) {
                Paint s = new Paint(Paint.ANTI_ALIAS_FLAG);
                s.setColor(Color.YELLOW);
                s.setTextSize(48f);
                s.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("ПРИСЕЛ", W/2f, H - 60, s);
            }
            
            if (inventoryOpen) drawInventory(canvas, W, H);
            if (settingsOpen) drawSettings(canvas, W, H);
        }
        
        private void drawCircleButton(Canvas c, float cx, float cy, float r, String label) {
            c.drawCircle(cx, cy, r, bgPaint);
            c.drawCircle(cx, cy, r, strokePaint);
            textPaint.setTextSize(38f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            c.drawText(label, cx, cy + 13f, textPaint);
        }
        
        private void drawJumpButton(Canvas c, float cx, float cy, float r) {
            c.drawCircle(cx, cy, r, bgPaint);
            c.drawCircle(cx, cy, r, strokePaint);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.WHITE);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(8f);
            float arrowH = 40f;
            c.drawLine(cx, cy + arrowH/2, cx, cy - arrowH/2, p);
            c.drawLine(cx, cy - arrowH/2, cx - 25, cy - arrowH/2 + 25, p);
            c.drawLine(cx, cy - arrowH/2, cx + 25, cy - arrowH/2 + 25, p);
        }
        
        private void drawSneakButton(Canvas c, float cx, float cy, float r) {
            c.drawCircle(cx, cy, r, bgPaint);
            c.drawCircle(cx, cy, r, strokePaint);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.WHITE);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(8f);
            float arrowH = 35f;
            c.drawLine(cx, cy - arrowH/2, cx, cy + arrowH/2, p);
            c.drawLine(cx, cy + arrowH/2, cx - 22, cy + arrowH/2 - 22, p);
            c.drawLine(cx, cy + arrowH/2, cx + 22, cy + arrowH/2 - 22, p);
        }
        
        private void drawSelectedBlock(Canvas c, float x, float y) {
            byte sel = renderer.player.selectedBlock;
            float[] color = Block.getUITopColor(sel);
            slotFill.setColor(Color.rgb((int)(color[0]*255), (int)(color[1]*255), (int)(color[2]*255)));
            int size = 100;
            c.drawRect(x, y, x + size, y + size, slotFill);
            c.drawRect(x, y, x + size, y + size, slotStroke);
            textPaint.setTextSize(38f);
            textPaint.setTextAlign(Paint.Align.LEFT);
            c.drawText(Block.getName(sel), x + size + 20, y + 65, textPaint);
        }
        
        private void drawInventory(Canvas c, int W, int H) {
            c.drawRect(0, H - 280, W, H, bgPaint);
            drawCloseX(c, W - 60, 60, 45);
            textPaint.setTextSize(56f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            c.drawText("Выберите блок", W/2f, H - 220, textPaint);
            
            byte[] place = Block.PLACEABLE;
            int slotSize = 130, gap = 20;
            int totalWidth = place.length * slotSize + (place.length - 1) * gap;
            int startX = (W - totalWidth) / 2, startY = H - 190;
            
            for (int i = 0; i < place.length; i++) {
                int sx = startX + i * (slotSize + gap);
                float[] color = Block.getUITopColor(place[i]);
                slotFill.setColor(Color.rgb((int)(color[0]*255), (int)(color[1]*255), (int)(color[2]*255)));
                c.drawRect(sx, startY, sx + slotSize, startY + slotSize, slotFill);
                c.drawRect(sx, startY, sx + slotSize, startY + slotSize, slotStroke);
                if (place[i] == renderer.player.selectedBlock) {
                    Paint b = new Paint(Paint.ANTI_ALIAS_FLAG);
                    b.setColor(Color.YELLOW);
                    b.setStyle(Paint.Style.STROKE);
                    b.setStrokeWidth(8f);
                    c.drawRect(sx-5, startY-5, sx+slotSize+5, startY+slotSize+5, b);
                }
                textPaint.setTextSize(34f);
                textPaint.setTextAlign(Paint.Align.CENTER);
                c.drawText(Block.getName(place[i]), sx + slotSize/2f, startY + slotSize + 40, textPaint);
            }
        }
        
        private void drawSettings(Canvas c, int W, int H) {
            Paint dim = new Paint();
            dim.setColor(Color.argb(180, 0, 0, 0));
            c.drawRect(0, 0, W, H, dim);
            drawCloseX(c, W - 60, 60, 45);
            
            textPaint.setTextSize(72f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            c.drawText("Настройки", W/2f, H/2 - 300, textPaint);
            
            textPaint.setTextSize(46f);
            c.drawText("Выберите мир:", W/2f, H/2 - 220, textPaint);
            
            int btnSize = 110, gap = 25;
            int totalW = 5 * btnSize + 4 * gap;
            int startX = (W - totalW) / 2;
            int startY = H/2 - btnSize - 30;
            
            for (int i = 0; i < 5; i++) {
                int bx = startX + i * (btnSize + gap);
                boolean exists = renderer.worldExists(i + 1);
                boolean isCurrent = renderer.currentWorld == (i + 1);
                
                Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
                if (isCurrent) fill.setColor(Color.argb(255, 200, 160, 40));
                else if (exists) fill.setColor(Color.argb(230, 80, 130, 200));
                else fill.setColor(Color.argb(200, 70, 70, 70));
                c.drawRect(bx, startY, bx + btnSize, startY + btnSize, fill);
                
                Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
                border.setStyle(Paint.Style.STROKE);
                border.setStrokeWidth(isCurrent ? 8f : 4f);
                border.setColor(isCurrent ? Color.YELLOW : Color.WHITE);
                c.drawRect(bx, startY, bx + btnSize, startY + btnSize, border);
                
                textPaint.setTextSize(58f);
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setColor(Color.WHITE);
                c.drawText(String.valueOf(i + 1), bx + btnSize/2f, startY + btnSize/2f + 20, textPaint);
            }
            
            int btnW = 500, btnH = 110;
            int delX = (W - btnW) / 2;
            int delY = startY + btnSize + 40;
            
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
        
        private void drawCloseX(Canvas c, float cx, float cy, float r) {
            c.drawCircle(cx, cy, r, bgPaint);
            c.drawCircle(cx, cy, r, strokePaint);
            Paint xp = new Paint(Paint.ANTI_ALIAS_FLAG);
            xp.setColor(Color.WHITE);
            xp.setStyle(Paint.Style.STROKE);
            xp.setStrokeWidth(7f);
            c.drawLine(cx - 15, cy - 15, cx + 15, cy + 15, xp);
            c.drawLine(cx + 15, cy - 15, cx - 15, cy + 15, xp);
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
