package com.example.myminecraft;

public class Frustum {
    private float[][] planes = new float[6][4];

    public void update(float[] m) {
        // Left
        planes[0][0] = m[3] + m[0]; planes[0][1] = m[7] + m[4]; planes[0][2] = m[11] + m[8]; planes[0][3] = m[15] + m[12];
        // Right
        planes[1][0] = m[3] - m[0]; planes[1][1] = m[7] - m[4]; planes[1][2] = m[11] - m[8]; planes[1][3] = m[15] - m[12];
        // Bottom
        planes[2][0] = m[3] + m[1]; planes[2][1] = m[7] + m[5]; planes[2][2] = m[11] + m[9]; planes[2][3] = m[15] + m[13];
        // Top
        planes[3][0] = m[3] - m[1]; planes[3][1] = m[7] - m[5]; planes[3][2] = m[11] - m[9]; planes[3][3] = m[15] - m[13];
        // Near
        planes[4][0] = m[3] + m[2]; planes[4][1] = m[7] + m[6]; planes[4][2] = m[11] + m[10]; planes[4][3] = m[15] + m[14];
        // Far
        planes[5][0] = m[3] - m[2]; planes[5][1] = m[7] - m[6]; planes[5][2] = m[11] - m[10]; planes[5][3] = m[15] - m[14];

        for (int i = 0; i < 6; i++) {
            float length = (float) Math.sqrt(planes[i][0]*planes[i][0] + planes[i][1]*planes[i][1] + planes[i][2]*planes[i][2]);
            if (length != 0) {
                planes[i][0] /= length; planes[i][1] /= length; planes[i][2] /= length; planes[i][3] /= length;
            }
        }
    }

    public boolean testSphere(float x, float y, float z, float radius) {
        for (int i = 0; i < 6; i++) {
            float dist = planes[i][0]*x + planes[i][1]*y + planes[i][2]*z + planes[i][3];
            if (dist < -radius) return false; // Полностью за пределами экрана
        }
        return true; // Виден
    }
}
