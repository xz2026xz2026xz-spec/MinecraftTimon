package com.example.myminecraft;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class PlayerModel {
    public FloatBuffer vertexBuffer;
    public FloatBuffer texBuffer;
    public int vertexCount = 0;

    public PlayerModel() {
        float[] verts = new float[7 * 36 * 3];
        float[] tex = new float[7 * 36 * 2];
        int vIdx = 0, tIdx = 0;

        // Голова (8x8x8)
        vIdx = addBox(verts, tex, vIdx, tIdx, 0, 0.2f, 0, 0.25f, 0.25f, 0.25f, 0, 0, 8, 8, 8); tIdx += 72;
        // Шляпа (чуть больше головы)
        vIdx = addBox(verts, tex, vIdx, tIdx, 0, 0.2f, 0, 0.26f, 0.26f, 0.26f, 32, 0, 8, 8, 8); tIdx += 72; 
        // Тело
        vIdx = addBox(verts, tex, vIdx, tIdx, 0, -0.15f, 0, 0.25f, 0.375f, 0.125f, 16, 16, 8, 12, 4); tIdx += 72;
        // Правая рука
        vIdx = addBox(verts, tex, vIdx, tIdx, -0.375f, -0.15f, 0, 0.125f, 0.375f, 0.125f, 40, 16, 4, 12, 4); tIdx += 72;
        // Левая рука
        vIdx = addBox(verts, tex, vIdx, tIdx, 0.375f, -0.15f, 0, 0.125f, 0.375f, 0.125f, 32, 48, 4, 12, 4); tIdx += 72;
        // Правая нога
        vIdx = addBox(verts, tex, vIdx, tIdx, -0.125f, -0.6f, 0, 0.125f, 0.375f, 0.125f, 0, 16, 4, 12, 4); tIdx += 72;
        // Левая нога
        vIdx = addBox(verts, tex, vIdx, tIdx, 0.125f, -0.6f, 0, 0.125f, 0.375f, 0.125f, 16, 48, 4, 12, 4); tIdx += 72;

        vertexCount = 7 * 36;
        ByteBuffer bb = ByteBuffer.allocateDirect(verts.length * 4); bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer(); vertexBuffer.put(verts); vertexBuffer.position(0);
        ByteBuffer tb = ByteBuffer.allocateDirect(tex.length * 4); tb.order(ByteOrder.nativeOrder());
        texBuffer = tb.asFloatBuffer(); texBuffer.put(tex); texBuffer.position(0);
    }

    private int addBox(float[] v, float[] t, int vIdx, int tIdx, float cx, float cy, float cz, float hx, float hy, float hz, int u, int vp, int w, int h, int d) {
        float texSize = 64f;
        float[][] faceUVs = {
            {u + d, vp + d, u + d + w, vp + d + h}, {u + d + w + d, vp + d, u + d + w + d + w, vp + d + h},
            {u + d, vp, u + d + w, vp + d}, {u + d + w, vp + d, u + d + w + d, vp + d + h},
            {u, vp + d, u + d, vp + d + h}, {u + d + w, vp + d, u + d + w + d, vp + d + h}
        };
        for (int f = 0; f < 6; f++) {
            float u1 = faceUVs[f][0] / texSize, v1 = faceUVs[f][1] / texSize;
            float u2 = faceUVs[f][2] / texSize, v2 = faceUVs[f][3] / texSize;
            for (int i = 0; i < 6; i++) {
                int offset = i * 12;
                if (i == 0 || i == 3) { t[tIdx + offset] = u1; t[tIdx + offset + 1] = v2; }
                else if (i == 1 || i == 4) { t[tIdx + offset] = u2; t[tIdx + offset + 1] = v2; }
                else { t[tIdx + offset] = u2; t[tIdx + offset + 1] = v1; }
                t[tIdx + offset + 2] = u1; t[tIdx + offset + 3] = v1;
                t[tIdx + offset + 4] = u2; t[tIdx + offset + 5] = v1;
            }
        }
        float[] f = {
            -hx,-hy, hz,  hx,-hy, hz,  hx, hy, hz, -hx,-hy, hz,  hx, hy, hz, -hx, hy, hz,
             hx,-hy,-hz, -hx,-hy,-hz, -hx, hy,-hz,  hx,-hy,-hz, -hx, hy,-hz,  hx, hy,-hz,
            -hx, hy, hz,  hx, hy, hz,  hx, hy,-hz, -hx, hy, hz,  hx, hy,-hz, -hx, hy,-hz,
            -hx,-hy,-hz,  hx,-hy,-hz,  hx,-hy, hz, -hx,-hy,-hz,  hx,-hy, hz, -hx,-hy, hz,
             hx,-hy, hz,  hx,-hy,-hz,  hx, hy,-hz,  hx,-hy, hz,  hx, hy,-hz,  hx, hy, hz,
            -hx,-hy,-hz, -hx,-hy, hz, -hx, hy, hz, -hx,-hy,-hz, -hx, hy, hz, -hx, hy,-hz
        };
        for(int i=0; i<36; i++) {
            v[vIdx + i*3] = cx + f[i*3];
            v[vIdx + i*3 + 1] = cy + f[i*3+1];
            v[vIdx + i*3 + 2] = cz + f[i*3+2];
        }
        return vIdx + 108;
    }
}
