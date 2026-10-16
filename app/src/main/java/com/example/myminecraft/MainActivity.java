package com.example.myminecraft;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    
    private GLSurfaceView glView;
    private MyGLRenderer renderer;
    
    private float joyStartX, joyStartY;
    private boolean joyActive = false;
    
    private float swipeStartX, swipeStartY;
    private float lastSwipeX, lastSwipeY;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        renderer = new MyGLRenderer();
        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);
        glView.setRenderer(renderer);
        
        glView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                handleTouch(event);
                return true;
            }
        });
        
        setContentView(glView);
    }
    
    private void handleTouch(MotionEvent event) {
        int action = event.getActionMasked();
        int halfWidth = glView.getWidth() / 2;
        
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (event.getX() < halfWidth) {
                    joyActive = true;
                    joyStartX = event.getX();
                    joyStartY = event.getY();
                } else {
                    swipeStartX = event.getX();
                    swipeStartY = event.getY();
                    lastSwipeX = swipeStartX;
                    lastSwipeY = swipeStartY;
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (joyActive) {
                    float dx = event.getX() - joyStartX;
                    float dy = event.getY() - joyStartY;
                    float max = 150f;
                    if (dx > max) dx = max;
                    if (dx < -max) dx = -max;
                    if (dy > max) dy = max;
                    if (dy < -max) dy = -max;
                    renderer.player.moveX = dx / max;
                    renderer.player.moveZ = -dy / max;
                } else {
                    float dx = event.getX() - lastSwipeX;
                    float dy = event.getY() - lastSwipeY;
                    renderer.player.yaw += dx * 0.4f;
                    renderer.player.pitch -= dy * 0.3f;
                    if (renderer.player.pitch > 89) renderer.player.pitch = 89;
                    if (renderer.player.pitch < -89) renderer.player.pitch = -89;
                    lastSwipeX = event.getX();
                    lastSwipeY = event.getY();
                }
                break;
                
            case MotionEvent.ACTION_UP:
                if (joyActive) {
                    joyActive = false;
                    renderer.player.moveX = 0;
                    renderer.player.moveZ = 0;
                } else {
                    float totalDx = Math.abs(event.getX() - swipeStartX);
                    float totalDy = Math.abs(event.getY() - swipeStartY);
                    long duration = event.getEventTime() - event.getDownTime();
                    
                    if (totalDx < 20 && totalDy < 20) {
                        int[] block = renderer.raycastBlock();
                        if (block != null) {
                            if (duration > 400) {
                                renderer.world.setBlock(block[0], block[1], block[2], Block.AIR);
                                renderer.worldDirty = true;
                            } else {
                                renderer.world.setBlock(block[0], block[1] + 1, block[2], Block.GRASS);
                                renderer.worldDirty = true;
                            }
                        }
                    }
                }
                break;
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (glView != null) glView.onPause();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (glView != null) glView.onResume();
    }
}
