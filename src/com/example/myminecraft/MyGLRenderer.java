package com.example.myminecraft;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {
    
    // Вершины куба (12 треугольников = 36 вершин)
    // Каждая вершина: x, y, z
    private static final float[] CUBE_VERTICES = {
        // Передняя грань
        -1,-1, 1,   1,-1, 1,   1, 1, 1,
        -1,-1, 1,   1, 1, 1,  -1, 1, 1,
        // Задняя грань
         1,-1,-1,  -1,-1,-1,  -1, 1,-1,
         1,-1,-1,  -1, 1,-1,   1, 1,-1,
        // Верхняя грань
        -1, 1, 1,   1, 1, 1,   1, 1,-1,
        -1, 1, 1,   1, 1,-1,  -1, 1,-1,
        // Нижняя грань
        -1,-1,-1,   1,-1,-1,   1,-1, 1,
        -1,-1,-1,   1,-1, 1,  -1,-1, 1,
        // Правая грань
         1,-1, 1,   1,-1,-1,   1, 1,-1,
         1,-1, 1,   1, 1,-1,   1, 1, 1,
        // Левая грань
        -1,-1,-1,  -1,-1, 1,  -1, 1, 1,
        -1,-1,-1,  -1, 1, 1,  -1, 1,-1
    };
    
    // Цвета для каждой вершины (R, G, B)
    // 36 вершин × 3 = 108 чисел
    private static final float[] CUBE_COLORS = new float[36 * 3];
    static {
        // Закрашиваем каждый треугольник своим цветом
        // Передняя — коричневая (земля)
        for (int i = 0; i < 6; i++) { CUBE_COLORS[i*3] = 0.55f; CUBE_COLORS[i*3+1] = 0.4f; CUBE_COLORS[i*3+2] = 0.25f; }
        // Задняя — коричневая
        for (int i = 6; i < 12; i++) { CUBE_COLORS[i*3] = 0.5f; CUBE_COLORS[i*3+1] = 0.35f; CUBE_COLORS[i*3+2] = 0.2f; }
        // Верх — зелёная (трава)
        for (int i = 12; i < 18; i++) { CUBE_COLORS[i*3] = 0.3f; CUBE_COLORS[i*3+1] = 0.8f; CUBE_COLORS[i*3+2] = 0.3f; }
        // Низ — тёмная
        for (int i = 18; i < 24; i++) { CUBE_COLORS[i*3] = 0.3f; CUBE_COLORS[i*3+1] = 0.2f; CUBE_COLORS[i*3+2] = 0.1f; }
        // Правая — коричневая
        for (int i = 24; i < 30; i++) { CUBE_COLORS[i*3] = 0.5f; CUBE_COLORS[i*3+1] = 0.35f; CUBE_COLORS[i*3+2] = 0.2f; }
        // Левая — коричневая
        for (int i = 30; i < 36; i++) { CUBE_COLORS[i*3] = 0.5f; CUBE_COLORS[i*3+1] = 0.35f; CUBE_COLORS[i*3+2] = 0.2f; }
    }
    
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    
    private int program;
    private int positionHandle;
    private int colorHandle;
    private int mvpMatrixHandle;
    
    private final float[] mvpMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] tempMatrix = new float[16];
    
    private float angle = 0f;
    
    // Шейдеры (написаны на GLSL)
    private static final String VERTEX_SHADER =
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "attribute vec4 vColor;" +
        "varying vec4 vColorVarying;" +
        "void main() {" +
        "  gl_Position = uMVPMatrix * vPosition;" +
        "  vColorVarying = vColor;" +
        "}";
    
    private static final String FRAGMENT_SHADER =
        "precision mediump float;" +
        "varying vec4 vColorVarying;" +
        "void main() {" +
        "  gl_FragColor = vColorVarying;" +
        "}";
    
    public MyGLRenderer() {
        // Буфер вершин
        ByteBuffer bb = ByteBuffer.allocateDirect(CUBE_VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(CUBE_VERTICES);
        vertexBuffer.position(0);
        
        // Буфер цветов
        ByteBuffer cb = ByteBuffer.allocateDirect(CUBE_COLORS.length * 4);
        cb.order(ByteOrder.nativeOrder());
        colorBuffer = cb.asFloatBuffer();
        colorBuffer.put(CUBE_COLORS);
        colorBuffer.position(0);
    }
    
    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Небо — голубое
        GLES20.glClearColor(0.5f, 0.7f, 1.0f, 1.0f);
        
        // Включаем проверку глубины (чтобы грани куба рисовались правильно)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        
        // Компилируем шейдеры
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
    }
    
    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        // Вращение куба
        angle += 1f;
        
        // Матрица модели — вращаем куб
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, angle, 0.5f, 1f, 0.3f);
        
        // Матрица вида — отодвигаем камеру назад
        Matrix.setLookAtM(viewMatrix, 0,
            0, 0, 5,    // позиция камеры
            0, 0, 0,    // куда смотрит
            0, 1, 0);   // вектор "вверх"
        
        // Объединяем: MVP = Projection × View × Model
        Matrix.multiplyMM(tempMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, tempMatrix, 0, modelMatrix, 0);
        
        // Используем программу шейдеров
        GLES20.glUseProgram(program);
        
        // Передаём матрицу в шейдер
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        
        // Передаём вершины
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        
        // Передаём цвета
        colorHandle = GLES20.glGetAttribLocation(program, "vColor");
        GLES20.glEnableVertexAttribArray(colorHandle);
        GLES20.glVertexAttribPointer(colorHandle, 3, GLES20.GL_FLOAT, false, 0, colorBuffer);
        
        // Рисуем 36 вершин = 12 треугольников = 6 граней
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        
        // Отключаем атрибуты
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
    }
    
    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        
        // Перспективная проекция (угол обзора 45°, соотношение сторон, ближняя и дальняя плоскости)
        float ratio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }
    
    // Вспомогательный метод для компиляции шейдеров
    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}