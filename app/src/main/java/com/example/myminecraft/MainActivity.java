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

public class MainActivity extends Activity {
    
    private GLSurfaceView glView;
    private MyGLRenderer renderer;
    private OverlayView overlay;
    
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
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));
        layout.addView(overlay, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));
        
        setContentView(layout);
    }
    
    private class OverlayView extends View {
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint crossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        private float joyCenterX, joyCenterY;
        private float knobX, knobY;
        private boolean joyActive = false;
        private final float joyRadius = 150f;
        private final float knobRadius = 55f;
        
        private boolean rightSideTouch = false;
        private float swipeStartX, swipeStartY;
        private float lastSwipeX, lastSwipeY;
        
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
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            if (joyActive) {
                canvas.drawCircle(joyCenterX, joyCenterY, joyRadius, strokePaint);
                canvas.drawCircle(knobX, knobY, knobRadius, fillPaint);
            } else {
                float cx = 200f;
                float cy = getHeight() - 200f;
                canvas.drawCircle(cx, cy, joyRadius, strokePaint);
                canvas.drawCircle(cx, cy, knobRadius, fillPaint);
            }
            
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float size = 35f;
            float gap = 10f;
            canvas.drawLine(cx - size, cy, cx - gap, cy, crossPaint);
            canvas.drawLine(cx + gap, cy, cx + size, cy, crossPaint);
            canvas.drawLine(cx, cy - size, cx, cy - gap, crossPaint);
            canvas.drawLine(cx, cy + gap, cx, cy + size, crossPaint);
        }
        
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            int halfWidth = getWidth() / 2;
            
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
                    invalidate();
                    return true;
                    
                case MotionEvent.ACTION_MOVE:
                    if (joyActive) {
                        float dx = event.getX() - joyCenterX;
                        float dy = event.getY() - joyCenterY;
                        float dist = (float)Math.sqrt(dx*dx + dy*dy);
                        if (dist > joyRadius) {
                            dx = dx / dist * joyRadius;
                            dy = dy / dist * joyRadius;
                        }
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
                    invalidate();
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
                                    // Сломать
                                    renderer.world.setBlock(block[0], block[1], block[2], Block.AIR);
                                    renderer.markDirty(block[0], block[1], block[2]);
                                } else {
                                    // Поставить
                                    renderer.world.setBlock(block[0], block[1] + 1, block[2], Block.GRASS);
                                    renderer.markDirty(block[0], block[1] + 1, block[2]);
                                }
                            }
                        }
                    }
                    rightSideTouch = false;
                    invalidate();
                    return true;
            }
            return super.onTouchEvent(event);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (glView != null) glView.onPause();
        // Сохраняем мир при сворачивании
        if (renderer != null) {
            renderer.world.saveToFile(
                new java.io.File(getFilesDir(), "world.dat"),
                renderer.player.x,
                renderer.player.y,
                renderer.player.z,
                renderer.player.yaw,
                renderer.player.pitch
            );
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (glView != null) glView.onResume();
    }
}
