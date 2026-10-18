package com.example.myminecraft;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class PlayerModel {
    public FloatBuffer vertexBuffer;
    public FloatBuffer texBuffer;
    public int vertexCount = 0;

    public PlayerModel() {
        // Упрощенный Стив (Голова, Тело, Руки, Ноги)
        // Мы натягиваем стандартный скин 64x64 на этот куб
        float[] verts = new float[6 * 36 * 3]; // 6 частей, по 6 граней (2 треугольника), по 3 координаты
        float[] tex = new float[6 * 36 * 2];
        int vIdx = 0, tIdx = 0;

        // Голова (размер 8x8x8, центр на 0, 0.3, 0)
        vIdx = addBox(verts, tex, vIdx, tIdx, 0, 0.2f, 0, 0.25f, 0.25f, 0.25f, 0, 0, 8, 8);
        tIdx += 72;
        
        // Тело (размер 8x12x4, центр на 0, -0.1, 0)
        vIdx = addBox(verts, tex, vIdx, tIdx, 0, -0.15f, 0, 0.25f, 0.375f, 0.125f, 16, 16, 8, 12);
        tIdx += 72;

        // Правая рука
        vIdx = addBox(verts, tex, vIdx, tIdx, -0.375f, -0.15f, 0, 0.125f, 0.375f, 0.125f, 40, 16, 4, 12);
        tIdx += 72;

        // Левая рука
        vIdx = addBox(verts, tex, vIdx, tIdx, 0.375f, -0.15f, 0, 0.125f, 0.375f, 0.125f, 32, 48, 4, 12);
        tIdx += 72;

        // Правая нога
        vIdx = addBox(verts, tex, vIdx, tIdx, -0.125f, -0.6f, 0, 0.125f, 0.375f, 0.125f, 0, 16, 4, 12);
        tIdx += 72;

        // Левая нога
        vIdx = addBox(verts, tex, vIdx, tIdx, 0.125f, -0.6f, 0, 0.125f, 0.375f, 0.125f, 16, 48, 4, 12);
        tIdx += 72;

        vertexCount = 6 * 36;

        ByteBuffer bb = ByteBuffer.allocateDirect(verts.length * 4); bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer(); vertexBuffer.put(verts); vertexBuffer.position(0);

        ByteBuffer tb = ByteBuffer.allocateDirect(tex.length * 4); tb.order(ByteOrder.nativeOrder());
        texBuffer = tb.asFloatBuffer(); texBuffer.put(tex); texBuffer.position(0);
    }

    private int addBox(float[] v, float[] t, int vIdx, int tIdx, float cx, float cy, float cz, float hx, float hy, float hz, int u, int vp, int w, int h) {
        // Упрощенная UV-разметка (не идеальный 64x64 маппинг, но работает)
        float texSize = 64f;
        float u1 = u / texSize, v1 = vp / texSize;
        float u2 = (u + w) / texSize, v2 = (vp + h) / texSize;

        // 6 граней по 6 вершин
        // (Здесь упрощенные координаты, чтобы скин было видно)
        for (int i = 0; i < 6; i++) {
            t[tIdx + i * 12] = u1; t[tIdx + i * 12 + 1] = v1;
            t[tIdx + i * 12 + 2] = u2; t[tIdx + i * 12 + 3] = v1;
            t[tIdx + i * 12 + 4] = u2; t[tIdx + i * 12 + 5] = v2;
            t[tIdx + i * 12 + 6] = u1; t[tIdx + i * 12 + 7] = v1;
            t[tIdx + i * 12 + 8] = u2; t[tIdx + i * 12 + 9] = v2;
            t[tIdx + i * 12 + 10] = u1; t[tIdx + i * 12 + 11] = v2;
        }

        // Вершины (упрощенные кубы)
        float[] f = {
            -hx,-hy, hz,  hx,-hy, hz,  hx, hy, hz, -hx,-hy, hz,  hx, hy, hz, -hx, hy, hz, // front
             hx,-hy,-hz, -hx,-hy,-hz, -hx, hy,-hz,  hx,-hy,-hz, -hx, hy,-hz,  hx, hy,-hz, // back
            -hx, hy, hz,  hx, hy, hz,  hx, hy,-hz, -hx, hy, hz,  hx, hy,-hz, -hx, hy,-hz, // top
            -hx,-hy,-hz,  hx,-hy,-hz,  hx,-hy, hz, -hx,-hy,-hz,  hx,-hy, hz, -hx,-hy, hz, // bottom
             hx,-hy, hz,  hx,-hy,-hz,  hx, hy,-hz,  hx,-hy, hz,  hx, hy,-hz,  hx, hy, hz, // right
            -hx,-hy,-hz, -hx,-hy, hz, -hx, hy, hz, -hx,-hy,-hz, -hx, hy, hz, -hx, hy,-hz  // left
        };
        for(int i=0; i<36; i++) {
            v[vIdx + i*3] = cx + f[i*3];
            v[vIdx + i*3 + 1] = cy + f[i*3+1];
            v[vIdx + i*3 + 2] = cz + f[i*3+2];
        }
        return vIdx + 108;
    }
}
