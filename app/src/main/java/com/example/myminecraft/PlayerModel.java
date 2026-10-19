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

        vIdx = addBox(verts, tex, vIdx, tIdx, 0, 0.2f, 0, 0.25f, 0.25f, 0.25f, 0, 0); tIdx += 72;
        vIdx = addBox(verts, tex, vIdx, tIdx, 0, 0.2f, 0, 0.26f, 0.26f, 0.26f, 32, 0); tIdx += 72;
        vIdx = addBox(verts, tex, vIdx, tIdx, 0, -0.15f, 0, 0.25f, 0.375f, 0.125f, 16, 16); tIdx += 72;
        vIdx = addBox(verts, tex, vIdx, tIdx, -0.375f, -0.15f, 0, 0.125f, 0.375f, 0.125f, 40, 16); tIdx += 72;
        vIdx = addBox(verts, tex, vIdx, tIdx, 0.375f, -0.15f, 0, 0.125f, 0.375f, 0.125f, 32, 48); tIdx += 72;
        vIdx = addBox(verts, tex, vIdx, tIdx, -0.125f, -0.6f, 0, 0.125f, 0.375f, 0.125f, 0, 16); tIdx += 72;
        vIdx = addBox(verts, tex, vIdx, tIdx, 0.125f, -0.6f, 0, 0.125f, 0.375f, 0.125f, 16, 48); tIdx += 72;

        vertexCount = 7 * 36;
        ByteBuffer bb = ByteBuffer.allocateDirect(verts.length * 4); bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer(); vertexBuffer.put(verts); vertexBuffer.position(0);
        ByteBuffer tb = ByteBuffer.allocateDirect(tex.length * 4); tb.order(ByteOrder.nativeOrder());
        texBuffer = tb.asFloatBuffer(); texBuffer.put(tex); texBuffer.position(0);
    }

    private int addBox(float[] v, float[] t, int vIdx, int tIdx, float cx, float cy, float cz, float hx, float hy, float hz, int u, int vp) {
        float texSize = 64f;
        float[][] faceUVs = {
            {u + 8, vp + 8, u + 16, vp + 16}, {u + 24, vp + 8, u + 32, vp + 16},
            {u + 8, vp, u + 16, vp + 8}, {u + 16, vp, u + 24, vp + 8},
            {u, vp + 8, u + 8, vp + 16}, {u + 16, vp + 8, u + 24, vp + 16}
        };
        for (int f = 0; f < 6; f++) {
            float u1 = faceUVs[f][0] / texSize, v1 = faceUVs[f][1] / texSize;
            float u2 = faceUVs[f][2] / texSize, v2 = faceUVs[f][3] / texSize;
            t[tIdx + f*12] = u1; t[tIdx + f*12 + 1] = v2; t[tIdx + f*12 + 2] = u2; t[tIdx + f*12 + 3] = v2; t[tIdx + f*12 + 4] = u2; t[tIdx + f*12 + 5] = v1;
            t[tIdx + f*12 + 6] = u1; t[tIdx + f*12 + 7] = v2; t[tIdx + f*12 + 8] = u2; t[tIdx + f*12 + 9] = v1; t[tIdx + f*12 + 10] = u1; t[tIdx + f*12 + 11] = v1;
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
            v[vIdx + i*3] = cx + f[i*3]; v[vIdx + i*3 + 1] = cy + f[i*3+1]; v[vIdx + i*3 + 2] = cz + f[i*3+2];
        }
        return vIdx + 108;
    }
}
