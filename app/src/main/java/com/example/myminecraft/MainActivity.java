package com.example.myminecraft;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    private GLSurfaceView glView;
    private MyGLRenderer renderer;
    private OverlayView overlay;
    private EditText chatInput;
    
    private int movePointerId = -1, lookPointerId = -1;
    private int bSwordId = -1, bJumpId = -1, bSprintId = -1, bPlaceId = -1, bSneakId = -1, bDownId = -1;
    
    private float moveCenterX, moveCenterY, knobX, knobY;
    private float moveRadius = 150f;
    private float lastSwipeX, lastSwipeY;
    
    public boolean inventoryOpen = false, settingsOpen = false;
    public Bitmap skinBitmap = null;
    public Paint skinPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    
    public boolean leftHanded = false; 
    public int uiSizeMode = 1; 
    public float getUiScale() { return uiSizeMode == 0 ? 0.8f : (uiSizeMode == 2 ? 1.2f : 1.0f); }
    
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        loadSkin();
        
        renderer = new MyGLRenderer(this);
        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);
        glView.setRenderer(renderer);
        
        overlay = new OverlayView(this);
        FrameLayout layout = new FrameLayout(this);
        layout.addView(glView, new FrameLayout.LayoutParams(-1, -1));
        layout.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        
        chatInput = new EditText(this);
        chatInput.setHint("Сообщение...");
        chatInput.setTextColor(Color.WHITE);
        chatInput.setHintTextColor(Color.LTGRAY);
        chatInput.setBackgroundColor(Color.argb(200, 30, 30, 30));
        chatInput.setPadding(20, 20, 20, 20);
        chatInput.setVisibility(View.GONE);
        chatInput.setSingleLine(true);
        chatInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2);
        lp.bottomMargin = 200; lp.leftMargin = 50; lp.rightMargin = 50;
        layout.addView(chatInput, lp);
        
        chatInput.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override public boolean onEditorAction(android.widget.TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    String txt = chatInput.getText().toString().trim();
                    if (!txt.isEmpty()) renderer.network.sendChat(txt);
                    closeChat();
                    return true;
                }
                return false;
            }
        });
        
        glView.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) { return handleTouch(event); }
        });
        setContentView(layout);
    }
    
    private void closeChat() {
        chatInput.setText("");
        chatInput.setVisibility(View.GONE);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(chatInput.getWindowToken(), 0);
    }
    
    private void loadSkin() {
        try { FileInputStream fis = openFileInput("skin.png"); skinBitmap = BitmapFactory.decodeStream(fis); fis.close(); } catch (Exception e) { skinBitmap = null; }
    }
    
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            try {
                InputStream is = getContentResolver().openInputStream(data.getData());
                Bitmap bmp = BitmapFactory.decodeStream(is);
                Bitmap scaled = Bitmap.createScaledBitmap(bmp, 256, 256, true);
                FileOutputStream fos = openFileOutput("skin.png", Context.MODE_PRIVATE);
                scaled.compress(Bitmap.CompressFormat.PNG, 100, fos); fos.close();
                loadSkin(); overlay.invalidate();
            } catch (Exception e) { Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show(); }
        }
    }
    
    private boolean handleTouch(MotionEvent event) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        int W = glView.getWidth(), H = glView.getHeight();
        float scale = getUiScale();
        
        moveCenterX = leftHanded ? W - 160 * scale : 160 * scale;
        moveCenterY = H - 160 * scale;
        float bSize = 120 * scale, gap = 15 * scale;
        float startX = leftHanded ? 20 * scale : W - (bSize * 2 + gap) - 20 * scale;
        float startY = H - (bSize * 3 + gap * 2) - 150 * scale;
        float cx1 = startX, cx2 = startX + bSize + gap;
        float cy1 = startY, cy2 = startY + bSize + gap, cy3 = startY + (bSize + gap) * 2;

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            float x = event.getX(index), y = event.getY(index);
            int pId = event.getPointerId(index);
            
            if (handleUIDown(x, y, W, H)) { overlay.invalidate(); return true; }
            if (inventoryOpen || settingsOpen || chatInput.getVisibility() == View.VISIBLE) return true;
            
            boolean isJoySide = leftHanded ? (x > W / 2) : (x < W / 2);
            if (isJoySide && movePointerId == -1) { movePointerId = pId; knobX = x; knobY = y; } 
            else if (inBtn(x, y, cx1, cy1, bSize) && bSwordId == -1) { bSwordId = pId; doAction(true); }
            else if (inBtn(x, y, cx2, cy1, bSize) && bJumpId == -1) { bJumpId = pId; renderer.player.jump = true; }
            else if (inBtn(x, y, cx1, cy2, bSize) && bSprintId == -1) { bSprintId = pId; renderer.player.sprinting = true; }
            else if (inBtn(x, y, cx2, cy2, bSize) && bPlaceId == -1) { bPlaceId = pId; doAction(false); }
            else if (inBtn(x, y, cx1, cy3, bSize) && bSneakId == -1) { bSneakId = pId; renderer.player.sneaking = true; }
            else if (inBtn(x, y, cx2, cy3, bSize) && bDownId == -1) { bDownId = pId; } 
            else if (!isJoySide && lookPointerId == -1) { lookPointerId = pId; lastSwipeX = x; lastSwipeY = y; }
            overlay.invalidate();
        } 
        else if (action == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < event.getPointerCount(); i++) {
                int pId = event.getPointerId(i);
                float x = event.getX(i), y = event.getY(i);
                if (pId == movePointerId) {
                    float dx = x - moveCenterX, dy = y - moveCenterY;
                    float dist = (float)Math.sqrt(dx*dx + dy*dy);
                    if (dist > moveRadius * scale) { dx = dx/dist*moveRadius*scale; dy = dy/dist*moveRadius*scale; }
                    knobX = moveCenterX + dx; knobY = moveCenterY + dy;
                    renderer.player.moveX = dx / (moveRadius * scale);
                    renderer.player.moveZ = -dy / (moveRadius * scale);
                } else if (pId == lookPointerId) {
                    float dx = x - lastSwipeX, dy = y - lastSwipeY;
                    renderer.player.yaw += dx * 0.4f;
                    renderer.player.pitch -= dy * 0.3f;
                    if (renderer.player.pitch > 89) renderer.player.pitch = 89;
                    if (renderer.player.pitch < -89) renderer.player.pitch = -89;
                    lastSwipeX = x; lastSwipeY = y;
                }
            }
            overlay.invalidate();
        } 
        else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            int pId = event.getPointerId(index);
            if (pId == movePointerId) { movePointerId = -1; renderer.player.moveX = 0; renderer.player.moveZ = 0; }
            else if (pId == lookPointerId) lookPointerId = -1;
            else if (pId == bSwordId) bSwordId = -1;
            else if (pId == bJumpId) bJumpId = -1;
            else if (pId == bSprintId) { bSprintId = -1; renderer.player.sprinting = false; }
            else if (pId == bPlaceId) bPlaceId = -1;
            else if (pId == bSneakId) { bSneakId = -1; renderer.player.sneaking = false; }
            else if (pId == bDownId) bDownId = -1;
            overlay.invalidate();
        }
        return true;
    }
    
    private boolean inBtn(float px, float py, float cx, float cy, float size) {
        return px >= cx && px <= cx + size && py >= cy && py <= cy + size;
    }
    
    private void doAction(boolean isBreak) {
        int[] block = renderer.raycastBlock();
        if (block != null) {
            if (isBreak) {
                byte b = renderer.world.getBlock(block[0], block[1], block[2]);
                if (b != Block.AIR) {
                    renderer.world.setBlock(block[0], block[1], block[2], Block.AIR);
                    renderer.markDirty(block[0], block[1], block[2]);
                    Inventory.addItem(b);
                }
            } else {
                if (Inventory.getSelectedBlock() != Block.AIR) {
                    int bx = block[0], by = block[1] + 1, bz = block[2];
                    if (!renderer.world.isInsidePlayer(bx, by, bz, renderer.player.x, renderer.player.y, renderer.player.z)) {
                        renderer.world.setBlock(bx, by, bz, Inventory.getSelectedBlock());
                        renderer.markDirty(bx, by, bz);
                        Inventory.removeSelected();
                    }
                }
            }
        }
    }
    
    private boolean handleUIDown(float x, float y, int W, int H) {
        float scale = getUiScale();
        if (inventoryOpen) {
            if (inCircle(x, y, W - 60, 60, 45 * scale)) { inventoryOpen = false; return true; }
            if (x > W/2 - 150 && x < W/2 + 150 && y > H - 450 && y < H - 380) { Inventory.dropItem(Inventory.selectedSlot); return true; }
            return false;
        }
        if (settingsOpen) {
            if (inCircle(x, y, W - 60, 60, 45 * scale)) { settingsOpen = false; return true; }
            int btnSize = (int)(110 * scale), gap = (int)(25 * scale);
            int totalW = 5 * btnSize + 4 * gap;
            int startX = (W - totalW) / 2, startY = H / 2 - 250;
            for (int i = 0; i < 5; i++) {
                int bx = startX + i * (btnSize + gap);
                if (x >= bx && x <= bx + btnSize && y >= startY && y <= startY + btnSize) {
                    if (renderer.currentWorld != i + 1) { renderer.requestSwitchWorld(i + 1); }
                    settingsOpen = false; return true;
                }
            }
            if (x > W/2 - 200 && x < W/2 + 200 && y > H/2 + 10 && y < H/2 + 90) { leftHanded = !leftHanded; return true; }
            if (x > W/2 - 200 && x < W/2 + 200 && y > H/2 + 110 && y < H/2 + 190) { uiSizeMode = (uiSizeMode + 1) % 3; return true; }
            if (x > W/2 - 200 && x < W/2 + 200 && y > H/2 + 210 && y < H/2 + 290) {
                Intent intent = new Intent(Intent.ACTION_PICK); intent.setType("image/*");
                startActivityForResult(intent, 100); return true;
            }
            if (x > W/2 - 200 && x < W/2 && y > H/2 + 310 && y < H/2 + 390) {
                renderer.network.startServer(renderer.world); Toast.makeText(this, "Сервер запущен!", Toast.LENGTH_SHORT).show(); return true;
            }
            if (x > W/2 && x < W/2 + 200 && y > H/2 + 310 && y < H/2 + 390) {
                renderer.network.connect("127.0.0.1"); Toast.makeText(this, "Подключение...", Toast.LENGTH_SHORT).show(); return true;
            }
            return false;
        }
        
        if (inCircle(x, y, W - 360 * scale, 60, 45 * scale)) { 
            chatInput.setVisibility(View.VISIBLE);
            chatInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(chatInput, InputMethodManager.SHOW_IMPLICIT);
            return true; 
        }
        if (inCircle(x, y, W - 260 * scale, 60, 45 * scale)) { renderer.player.thirdPerson = !renderer.player.thirdPerson; return true; }
        if (inCircle(x, y, W - 160 * scale, 60, 45 * scale)) { inventoryOpen = true; return true; }
        if (inCircle(x, y, W - 60, 60, 45 * scale)) { settingsOpen = true; return true; }
        
        int slotSize = (int)(110 * scale), gap = (int)(10 * scale);
        int startX = (W - (9 * slotSize + 8 * gap)) / 2, startY = (int)(H - 140 * scale);
        for (int i = 0; i < 9; i++) {
            int sx = startX + i * (slotSize + gap);
            if (x >= sx && x <= sx + slotSize && y >= startY && y <= startY + slotSize) { Inventory.selectSlot(i); return true; }
        }
        return false;
    }
    
    private boolean inCircle(float px, float py, float cx, float cy, float r) {
        float dx = px - cx, dy = py - cy; return dx*dx + dy*dy <= r*r;
    }
    
    private class OverlayView extends View {
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG), fill = new Paint(Paint.ANTI_ALIAS_FLAG), text = new Paint(Paint.ANTI_ALIAS_FLAG), bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        public OverlayView(Context ctx) {
            super(ctx);
            stroke.setColor(Color.argb(180, 255, 255, 255)); stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeWidth(4f);
            fill.setColor(Color.argb(100, 0, 0, 0)); fill.setStyle(Paint.Style.FILL);
            text.setColor(Color.WHITE); text.setTextSize(45f); text.setTextAlign(Paint.Align.CENTER); text.setAntiAlias(true);
            bg.setColor(Color.argb(150, 30, 30, 30)); bg.setStyle(Paint.Style.FILL);
        }
        
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            int W = getWidth(), H = getHeight();
            float scale = getUiScale();
            
            float cx = W/2f, cy = H/2f;
            c.drawLine(cx-30, cy, cx-10, cy, stroke); c.drawLine(cx+10, cy, cx+30, cy, stroke);
            c.drawLine(cx, cy-30, cx, cy-10, stroke); c.drawLine(cx, cy+10, cx, cy+30, stroke);
            
            c.drawCircle(W - 360 * scale, 60, 45 * scale, bg); c.drawCircle(W - 360 * scale, 60, 45 * scale, stroke); 
            text.setTextSize(45f * scale); c.drawText("💬", W - 360 * scale, 60 + 15 * scale, text);
            c.drawCircle(W - 260 * scale, 60, 45 * scale, bg); c.drawCircle(W - 260 * scale, 60, 45 * scale, stroke); 
            c.drawText(renderer.player.thirdPerson ? "3D" : "1D", W - 260 * scale, 60 + 15 * scale, text);
            c.drawCircle(W - 160 * scale, 60, 45 * scale, bg); c.drawCircle(W - 160 * scale, 60, 45 * scale, stroke); c.drawText("🎒", W - 160 * scale, 60 + 15 * scale, text);
            c.drawCircle(W - 60, 60, 45 * scale, bg); c.drawCircle(W - 60, 60, 45 * scale, stroke); c.drawText("⚙", W - 60, 60 + 15 * scale, text);
            
            Paint listBg = new Paint(); listBg.setColor(Color.argb(150, 0, 0, 0));
            text.setTextAlign(Paint.Align.LEFT); text.setTextSize(35f * scale);
            int listY = 50;
            synchronized(renderer.network.playerNames) {
                c.drawRect(20, listY - 30, 20 + text.measureText("Игроки:") + 40, listY + (renderer.network.playerNames.size() * 45f * scale) + 10, listBg);
                c.drawText("Игроки:", 30, listY, text);
                listY += 45 * scale;
                for (String name : renderer.network.playerNames) {
                    c.drawText("- " + name, 30, listY, text);
                    listY += 45 * scale;
                }
            }
            
            Paint chatBg = new Paint(); chatBg.setColor(Color.argb(120, 0, 0, 0));
            text.setTextSize(35f * scale);
            int msgY = Math.max(listY + 50, 200 * (int)scale);
            synchronized (renderer.network.chatMessages) {
                int start = Math.max(0, renderer.network.chatMessages.size() - 5);
                for (int i = start; i < renderer.network.chatMessages.size(); i++) {
                    String msg = renderer.network.chatMessages.get(i);
                    c.drawRect(20, msgY - 35*scale, 20 + text.measureText(msg) + 20, msgY + 10, chatBg);
                    c.drawText(msg, 30, msgY, text);
                    msgY += 45 * scale;
                }
            }
            
            float joyCX = leftHanded ? W - 160 * scale : 160 * scale;
            float joyCY = H - 160 * scale;
            RectF dpOuter = new RectF(joyCX - moveRadius*scale, joyCY - moveRadius*scale, joyCX + moveRadius*scale, joyCY + moveRadius*scale);
            c.drawRect(dpOuter, fill); c.drawRect(dpOuter, stroke);
            if (movePointerId != -1) { c.drawCircle(knobX, knobY, 50f * scale, bg); c.drawCircle(knobX, knobY, 50f * scale, stroke); }
            else { c.drawRect(joyCX - 40*scale, joyCY - 40*scale, joyCX + 40*scale, joyCY + 40*scale, stroke); }
            
            float bSize = 120 * scale, gap = 15 * scale;
            float startX = leftHanded ? 20 * scale : W - (bSize * 2 + gap) - 20 * scale;
            float startY = H - (bSize * 3 + gap * 2) - 150 * scale;
            float cx1 = startX, cx2 = startX + bSize + gap;
            float cy1 = startY, cy2 = startY + bSize + gap, cy3 = startY + (bSize + gap) * 2;
            
            drawPEBtn(c, cx1, cy1, bSize, "⚔", bSwordId != -1); drawPEBtn(c, cx2, cy1, bSize, "↑", bJumpId != -1);
            drawPEBtn(c, cx1, cy2, bSize, "»", bSprintId != -1); drawPEBtn(c, cx2, cy2, bSize, "+", bPlaceId != -1);
            drawPEBtn(c, cx1, cy3, bSize, "✛", bSneakId != -1); drawPEBtn(c, cx2, cy3, bSize, "↓", bDownId != -1);
            
            float barY = H - 185 * scale;
            float iconSize = 45f * scale;
            text.setTextSize(iconSize);
            text.setTextAlign(Paint.Align.CENTER);
            
            // Здоровье (10 сердечек)
            float hpStartX = W/2f - 20 * scale - (10 * iconSize);
            for (int i = 0; i < 10; i++) c.drawText("❤️", hpStartX + i * iconSize + iconSize/2, barY, text);
            // Голод (10 ножек)
            float foodStartX = W/2f + 20 * scale;
            for (int i = 0; i < 10; i++) c.drawText("🍗", foodStartX + i * iconSize + iconSize/2, barY, text);
            
            int slotSize = (int)(110 * scale), hGap = (int)(10 * scale);
            int hotbarStartX = (W - (9 * slotSize + 8 * hGap)) / 2, hotbarStartY = (int)(H - 140 * scale);
            for (int i = 0; i < 9; i++) {
                int sx = hotbarStartX + i * (slotSize + hGap);
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setStyle(Paint.Style.FILL);
                p.setColor(Color.argb(i == Inventory.selectedSlot ? 220 : 100, 0, 0, 0));
                c.drawRect(sx, hotbarStartY, sx+slotSize, hotbarStartY+slotSize, p);
                c.drawRect(sx, hotbarStartY, sx+slotSize, hotbarStartY+slotSize, stroke);
                
                byte b = Inventory.slots[i].type;
                if (b != Block.AIR) {
                    float[] col = Block.getUITopColor(b);
                    p.setColor(Color.rgb((int)(col[0]*255), (int)(col[1]*255), (int)(col[2]*255)));
                    c.drawRect(sx+20, hotbarStartY+20, sx+slotSize-20, hotbarStartY+slotSize-20, p);
                    
                    if (Inventory.slots[i].count > 1) {
                        text.setTextSize(30f * scale); text.setTextAlign(Paint.Align.RIGHT);
                        c.drawText(String.valueOf(Inventory.slots[i].count), sx + slotSize - 5, hotbarStartY + slotSize - 5, text);
                    }
                }
            }
            if (inventoryOpen) drawInventory(c, W, H);
            if (settingsOpen) drawSettings(c, W, H);
        }
        
        private void drawPEBtn(Canvas c, float x, float y, float size, String label, boolean pressed) {
            RectF r = new RectF(x, y, x + size, y + size);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setStyle(Paint.Style.FILL); p.setColor(pressed ? Color.argb(150, 255, 255, 255) : Color.argb(100, 50, 50, 50));
            c.drawRect(r, p);
            p.setStyle(Paint.Style.STROKE); p.setColor(Color.WHITE); p.setStrokeWidth(4f); c.drawRect(r, p);
            text.setTextSize(size * 0.6f); text.setTextAlign(Paint.Align.CENTER); c.drawText(label, x + size/2f, y + size/2f + size * 0.2f, text);
        }
        
        private void drawInventory(Canvas c, int W, int H) {
            Paint dim = new Paint(); dim.setColor(Color.argb(220, 0, 0, 0)); c.drawRect(0, 0, W, H, dim);
            text.setTextSize(60f); text.setTextAlign(Paint.Align.CENTER); c.drawText("Инвентарь", W/2f, 100, text);
            c.drawCircle(W - 60, 60, 45, bg); c.drawCircle(W - 60, 60, 45, stroke); c.drawText("X", W - 60, 75, text);
            
            int slotSize = 100, hGap = 10;
            int startX = (W - (9 * slotSize + 8 * hGap)) / 2, startY = 150;
            for (int row = 0; row < 3; row++) {
                for (int i = 0; i < 9; i++) {
                    int sx = startX + i*(slotSize+hGap);
                    int sy = startY + row*(slotSize+hGap);
                    c.drawRect(sx, sy, sx + slotSize, sy + slotSize, stroke);
                    
                    int idx = 9 + row * 9 + i;
                    byte b = Inventory.slots[idx].type;
                    if (b != Block.AIR) {
                        Paint p = new Paint(); 
                        float[] col = Block.getUITopColor(b);
                        p.setColor(Color.rgb((int)(col[0]*255), (int)(col[1]*255), (int)(col[2]*255)));
                        c.drawRect(sx+15, sy+15, sx+slotSize-15, sy+slotSize-15, p);
                        
                        if (Inventory.slots[idx].count > 1) {
                            text.setTextSize(30f); text.setTextAlign(Paint.Align.RIGHT);
                            c.drawText(String.valueOf(Inventory.slots[idx].count), sx + slotSize - 5, sy + slotSize - 5, text);
                        }
                    }
                }
            }
            
            Paint btn = new Paint(); btn.setColor(Color.rgb(180, 60, 60));
            c.drawRect(W/2 - 150, H - 450, W/2 + 150, H - 380, btn);
            c.drawRect(W/2 - 150, H - 450, W/2 + 150, H - 380, stroke);
            text.setTextSize(40f); text.setTextAlign(Paint.Align.CENTER);
            c.drawText("Выбросить", W/2f, H - 405, text);
            
            int cx = W/2 + 150, cy = H - 250;
            if (skinBitmap != null) c.drawBitmap(skinBitmap, null, new Rect(cx-100, cy-100, cx+100, cy+100), skinPaint);
        }
        
        private void drawSettings(Canvas c, int W, int H) {
            Paint dim = new Paint(); dim.setColor(Color.argb(220, 0, 0, 0)); c.drawRect(0, 0, W, H, dim);
            text.setTextSize(60f); text.setTextAlign(Paint.Align.CENTER); c.drawText("Настройки", W/2f, 80, text);
            c.drawCircle(W - 60, 60, 45, bg); c.drawCircle(W - 60, 60, 45, stroke); c.drawText("X", W - 60, 75, text);
            
            text.setTextSize(40f); c.drawText("Выбор мира:", W/2f, H/2 - 250, text);
            int btnSize = 110, gap = 25;
            int totalW = 5 * btnSize + 4 * gap;
            int startX = (W - totalW) / 2, startY = H / 2 - 250 + 40;
            for (int i = 0; i < 5; i++) {
                int bx = startX + i * (btnSize + gap);
                boolean exists = renderer.worldExists(i + 1);
                boolean isCur = renderer.currentWorld == (i + 1);
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setStyle(Paint.Style.FILL);
                p.setColor(isCur ? Color.rgb(200, 160, 40) : (exists ? Color.rgb(80, 130, 200) : Color.rgb(70, 70, 70)));
                c.drawRect(bx, startY, bx + btnSize, startY + btnSize, p);
                c.drawRect(bx, startY, bx + btnSize, startY + btnSize, stroke);
                text.setTextSize(60f); c.drawText(String.valueOf(i + 1), bx + btnSize/2f, startY + btnSize/2f + 20, text);
            }
            
            Paint btnBg = new Paint(); btnBg.setColor(Color.rgb(80, 130, 200));
            text.setTextSize(36f); c.drawText("Раскладка", W/2f, H/2 + 40, text);
            c.drawRect(W/2 - 200, H/2 + 10, W/2 + 200, H/2 + 90, btnBg); c.drawRect(W/2 - 200, H/2 + 10, W/2 + 200, H/2 + 90, stroke);
            text.setTextSize(40f); c.drawText(leftHanded ? "Левша" : "Правша", W/2f, H/2 + 65, text);
            
            text.setTextSize(36f); c.drawText("Размер", W/2f, H/2 + 140, text);
            c.drawRect(W/2 - 200, H/2 + 110, W/2 + 200, H/2 + 190, btnBg); c.drawRect(W/2 - 200, H/2 + 110, W/2 + 200, H/2 + 190, stroke);
            text.setTextSize(40f); c.drawText(uiSizeMode == 0 ? "Маленький" : (uiSizeMode == 2 ? "Большой" : "Средний"), W/2f, H/2 + 165, text);
            
            text.setTextSize(36f); c.drawText("Скин", W/2f, H/2 + 240, text);
            c.drawRect(W/2 - 200, H/2 + 210, W/2 + 200, H/2 + 290, btnBg); c.drawRect(W/2 - 200, H/2 + 210, W/2 + 200, H/2 + 290, stroke);
            text.setTextSize(40f); c.drawText("Загрузить", W/2f, H/2 + 265, text);
            
            Paint netBtn = new Paint(); netBtn.setColor(Color.rgb(60, 180, 60));
            c.drawRect(W/2 - 200, H/2 + 310, W/2 - 10, H/2 + 390, netBtn); c.drawRect(W/2 - 200, H/2 + 310, W/2 - 10, H/2 + 390, stroke);
            text.setTextSize(36f); c.drawText("Открыть мир", W/2 - 105, H/2 + 365, text);
            
            netBtn.setColor(Color.rgb(180, 60, 60));
            c.drawRect(W/2 + 10, H/2 + 310, W/2 + 200, H/2 + 390, netBtn); c.drawRect(W/2 + 10, H/2 + 310, W/2 + 200, H/2 + 390, stroke);
            c.drawText("Подключиться", W/2 + 105, H/2 + 365, text);
        }
    }
    
    @Override protected void onPause() { super.onPause(); if (glView != null) glView.onPause(); if (renderer != null) renderer.saveNow(); }
    @Override protected void onResume() { super.onResume(); if (glView != null) glView.onResume(); }
}
