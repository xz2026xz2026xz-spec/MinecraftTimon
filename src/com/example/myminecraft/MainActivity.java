package com.example.myminecraft;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    
    private GLSurfaceView glView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Полноэкранный режим
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        // Создаём 3D-вид
        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2); // OpenGL ES 2.0
        glView.setRenderer(new MyGLRenderer());
        
        setContentView(glView);
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