package com.example.myminecraft;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private GLSurfaceView glView;
    private MyGLRenderer renderer;
    private OverlayView overlay;
    private EditText chatInput;
    private SharedPreferences prefs;
    
    private int movePointerId = -1, lookPointerId = -1;
    private int bSwordId = -1, bJumpId = -1, bSprintId = -1, bDropId = -1, bPlaceId = -1, bSneakId = -1;
    
    private float moveCenterX, moveCenterY, knobX, knobY;
    private float moveRadius = 150f;
    private float lastSwipeX, lastSwipeY;
    
    public boolean inventoryOpen = false, settingsOpen = false, craftingOpen = false;
    public Bitmap skinBitmap = null;
    public Paint skinPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    
    public boolean leftHanded = false; 
    public int uiSizeMode = 1; 
    public float getUiScale() { return uiSizeMode == 0 ? 0.8f : (uiSizeMode == 2 ? 1.2f : 1.0f); }
    
    private float settingsScrollY = 0f;
    private float lastScrollY = 0f;
    private boolean isScrollingSettings = false;
    
    private List<Integer> swipedSlots = new ArrayList<>();
    private int dragOriginalCount = 0;
    
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        loadSkin();
        
        prefs = getSharedPreferences("TimonCraftPrefs", Context.MODE_PRIVATE);
        Inventory.loadFromFile(this);
        
        renderer = new MyGLRenderer(this);
        String savedName = prefs.getString("nickname", null);
        if (savedName != null && !savedName.isEmpty()) renderer.network.myName = savedName;
        
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
    
    private void showNicknameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Введите ваш никнейм");
        final EditText input = new EditText(this);
        input.setText(renderer.network.myName);
        builder.setView(input);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) { renderer.network.updateName(newName); prefs.edit().putString("nickname", newName).apply(); }
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showIpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Введите IP друга");
        final EditText input = new EditText(this);
        builder.setView(input);
        builder.setPositiveButton("Подключиться", (dialog, which) -> {
            String ip = input.getText().toString().trim();
            if (!ip.isEmpty()) { renderer.network.connect(ip); }
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showServerIpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Сервер запущен!");
        builder.setMessage("Ваш IP: " + getLocalIp() + "\n\nСкажите его другу.");
        builder.setPositiveButton("ОК", (dialog, which) -> dialog.dismiss());
        builder.show();
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
                loadSkin(); TextureManager.skinNeedsReload = true; overlay.invalidate();
            } catch (Exception e) { }
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
        float btnStartX = leftHanded ? 20 * scale : W - (bSize * 2 + gap) - 20 * scale;
        float btnStartY = H - (bSize * 3 + gap * 2) - 200 * scale;
        float cx1 = btnStartX, cx2 = btnStartX + bSize + gap;
        float cy1 = btnStartY, cy2 = btnStartY + bSize + gap, cy3 = btnStartY + (bSize + gap) * 2;

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            float x = event.getX(index), y = event.getY(index);
            int pId = event.getPointerId(index);
            
            if (settingsOpen) {
                if (handleSettingsUIDown(x, y, W, H)) { overlay.invalidate(); return true; }
                else { isScrollingSettings = true; lastScrollY = y; return true; }
            }
            
            if (craftingOpen) { handleCraftingUIDown(x, y, W, H); overlay.invalidate(); return true; }
            
            if (inventoryOpen) {
                if (inCircle(x, y, W - 60, 60, 45 * scale)) { inventoryOpen = false; Inventory.movingSlot = -1; return true; }
                if (x > 50 && x < 250 && y > 60 && y < 120) { craftingOpen = true; inventoryOpen = false; Inventory.movingSlot = -1; return true; }
                
                int slotSize = 100, hGap = 10;
                int invStartX = (W - (9 * slotSize + 8 * hGap)) / 2, invStartY = 150;
                
                for (int row = 0; row < 3; row++) {
                    for (int i = 0; i < 9; i++) {
                        int sx = invStartX + i*(slotSize+hGap);
                        int sy = invStartY + row*(slotSize+hGap);
                        if (x >= sx && x <= sx + slotSize && y >= sy && y <= sy + slotSize) {
                            int idx = 9 + row * 9 + i;
                            if (Inventory.movingSlot == -1) { 
                                if (Inventory.slots[idx].type != Block.AIR) {
                                    Inventory.movingSlot = idx; 
                                    dragOriginalCount = Inventory.slots[idx].count;
                                    swipedSlots.clear();
                                }
                            } else { 
                                if (Inventory.movingSlot == idx) Inventory.movingSlot = -1; 
                                else { Inventory.swapSlots(Inventory.movingSlot, idx); Inventory.movingSlot = -1; }
                            }
                            return true;
                        }
                    }
                }
                int hotbarY = (int)(H - 200 * scale);
                for (int i = 0; i < 9; i++) {
                    int sx = invStartX + i*(slotSize+hGap);
                    if (x >= sx && x <= sx + slotSize && y >= hotbarY && y <= hotbarY + slotSize) {
                        if (Inventory.movingSlot == -1) { 
                            if (Inventory.slots[i].type != Block.AIR) {
                                Inventory.movingSlot = i; 
                                dragOriginalCount = Inventory.slots[i].count;
                                swipedSlots.clear();
                            }
                        } else { 
                            if (Inventory.movingSlot == i) Inventory.movingSlot = -1; 
                            else { Inventory.swapSlots(Inventory.movingSlot, i); Inventory.movingSlot = -1; }
                        }
                        return true;
                    }
                }
                return true;
            }
            
            if (handleUIDown(x, y, W, H)) { overlay.invalidate(); return true; }
            if (chatInput.getVisibility() == View.VISIBLE) return true;
            
            boolean isJoySide = leftHanded ? (x > W / 2) : (x < W / 2);
            if (isJoySide && movePointerId == -1) { movePointerId = pId; knobX = x; knobY = y; } 
            else if (inBtn(x, y, cx1, cy1, bSize) && bSwordId == -1) { bSwordId = pId; doAction(true); }
            else if (inBtn(x, y, cx2, cy1, bSize) && bJumpId == -1) { bJumpId = pId; renderer.player.jump = true; }
            else if (inBtn(x, y, cx1, cy2, bSize) && bSprintId == -1) { bSprintId = pId; renderer.player.sprinting = true; }
            else if (inBtn(x, y, cx2, cy2, bSize) && bDropId == -1) { bDropId = pId; dropCurrentItem(); }
            else if (inBtn(x, y, cx1, cy3, bSize) && bPlaceId == -1) { bPlaceId = pId; doAction(false); }
            else if (inBtn(x, y, cx2, cy3, bSize) && bSneakId == -1) { bSneakId = pId; renderer.player.sneaking = true; }
            else if (!isJoySide && lookPointerId == -1) { lookPointerId = pId; lastSwipeX = x; lastSwipeY = y; }
            overlay.invalidate();
        } 
        else if (action == MotionEvent.ACTION_MOVE) {
            if (inventoryOpen && Inventory.movingSlot != -1) {
                int slotSize = 100, hGap = 10;
                int invStartX = (W - (9 * slotSize + 8 * hGap)) / 2, invStartY = 150;
                float x = event.getX(index), y = event.getY(index);
                int currentSlot = -1;
                
                for (int row = 0; row < 3; row++) {
                    for (int i = 0; i < 9; i++) {
                        int sx = invStartX + i*(slotSize+hGap);
                        int sy = invStartY + row*(slotSize+hGap);
                        if (x >= sx && x <= sx + slotSize && y >= sy && y <= sy + slotSize) currentSlot = 9 + row * 9 + i;
                    }
                }
                int hotbarY = (int)(H - 200 * scale);
                for (int i = 0; i < 9; i++) {
                    int sx = invStartX + i*(slotSize+hGap);
                    if (x >= sx && x <= sx + slotSize && y >= hotbarY && y <= hotbarY + slotSize) currentSlot = i;
                }
                
                byte itemType = Inventory.slots[Inventory.movingSlot].type;
                if (currentSlot != -1 && currentSlot != Inventory.movingSlot && !swipedSlots.contains(currentSlot) && itemType != Block.AIR) {
                    if (Inventory.slots[currentSlot].type == Block.AIR || Inventory.slots[currentSlot].type == itemType) {
                        swipedSlots.add(currentSlot);
                        int totalSlots = swipedSlots.size() + 1;
                        int amount = dragOriginalCount / totalSlots;
                        int remainder = dragOriginalCount % totalSlots;
                        
                        Inventory.slots[Inventory.movingSlot].count = remainder;
                        if (remainder == 0) Inventory.slots[Inventory.movingSlot].type = Block.AIR;
                        
                        for (int s : swipedSlots) {
                            Inventory.slots[s].type = itemType;
                            Inventory.slots[s].count = amount;
                        }
                    }
                }
                overlay.invalidate();
                return true;
            }

            if (settingsOpen) {
                if (isScrollingSettings) {
                    float dy = event.getY(index) - lastScrollY;
                    settingsScrollY += dy;
                    if (settingsScrollY > 0) settingsScrollY = 0;
                    if (settingsScrollY < -450 * scale) settingsScrollY = -450 * scale;
                    lastScrollY = event.getY(index);
                    overlay.invalidate();
                }
                return true;
            }
            if (craftingOpen) return true;

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
            if (inventoryOpen) {
                if (!swipedSlots.isEmpty()) { Inventory.movingSlot = -1; swipedSlots.clear(); }
                return true;
            }
            if (settingsOpen) { isScrollingSettings = false; return true; }
            if (craftingOpen) return true;
            
            int pId = event.getPointerId(index);
            if (pId == movePointerId) { movePointerId = -1; renderer.player.moveX = 0; renderer.player.moveZ = 0; }
            else if (pId == lookPointerId) lookPointerId = -1;
            else if (pId == bSwordId) bSwordId = -1;
            else if (pId == bJumpId) bJumpId = -1;
            else if (pId == bSprintId) { bSprintId = -1; renderer.player.sprinting = false; }
            else if (pId == bDropId) bDropId = -1;
            else if (pId == bPlaceId) bPlaceId = -1;
            else if (pId == bSneakId) { bSneakId = -1; renderer.player.sneaking = false; }
            overlay.invalidate();
        }
        return true;
    }
    
    private void dropCurrentItem() {
        if (Inventory.getSelectedBlock() != Block.AIR) {
            byte type = Inventory.getSelectedBlock();
            float yawRad = (float)Math.toRadians(renderer.player.yaw);
            float dx = (float)Math.sin(yawRad) * 0.5f;
            float dz = -(float)Math.cos(yawRad) * 0.5f;
            renderer.world.droppedItems.add(new DroppedItem(renderer.player.x + dx, renderer.player.y + 1.2f, renderer.player.z + dz, type));
            Inventory.removeSelected();
        }
    }
    
    private boolean inBtn(float px, float py, float cx, float cy, float size) { return px >= cx && px <= cx + size && py >= cy && py <= cy + size; }
    
    private void doAction(boolean isBreak) {
        if (isBreak) {
            int sheepIdx = renderer.raycastSheep();
            if (sheepIdx != -1) { renderer.world.sheepList.remove(sheepIdx); Inventory.addItem(Block.MEAT); return; }
            int[] block = renderer.raycastBlock();
            if (block != null) {
                byte b = renderer.world.getBlock(block[0], block[1], block[2]);
                if (b != Block.AIR) {
                    renderer.world.setBlock(block[0], block[1], block[2], Block.AIR);
                    renderer.markDirty(block[0], block[1], block[2]);
                    if (b == Block.LEAVES && Math.random() < 0.2) Inventory.addItem(Block.APPLE);
                    else if (b == Block.STONE) Inventory.addItem(Block.COBBLESTONE);
                    else Inventory.addItem(b);
                }
            }
        } else {
            if (Inventory.getSelectedBlock() != Block.AIR && !Block.isFood(Inventory.getSelectedBlock())) {
                int[] block = renderer.raycastBlock();
                if (block != null) {
                    int bx = block[3], by = block[4], bz = block[5];
                    if (!renderer.world.isInsidePlayer(bx, by, bz, renderer.player.x, renderer.player.y, renderer.player.z)) {
                        renderer.world.setBlock(bx, by, bz, Inventory.getSelectedBlock());
                        renderer.markDirty(bx, by, bz);
                        Inventory.removeSelected();
                    }
                }
            }
        }
    }
    
    private void handleCraftingUIDown(float x, float y, int W, int H) {
        float scale = getUiScale();
        if (inCircle(x, y, W - 60, 60, 45 * scale)) { craftingOpen = false; inventoryOpen = true; Inventory.movingSlot = -1; return; }
        
        int slotSize = 100, gap = 10;
        int startX = W/2 - 200, startY = H/2 - 100;
        
        for (int i = 0; i < 4; i++) {
            int sx = startX + (i % 2) * (slotSize + gap);
            int sy = startY + (i / 2) * (slotSize + gap);
            if (x >= sx && x <= sx + slotSize && y >= sy && y <= sy + slotSize) {
                if (Inventory.movingSlot == -1) { 
                    if (Inventory.craftGrid[i].type != Block.AIR) Inventory.movingSlot = 100 + i; 
                } 
                else {
                    if (Inventory.movingSlot >= 100) {
                        Inventory.Slot tmp = Inventory.craftGrid[Inventory.movingSlot - 100];
                        Inventory.craftGrid[Inventory.movingSlot - 100] = Inventory.craftGrid[i];
                        Inventory.craftGrid[i] = tmp;
                    } else {
                        Inventory.Slot tmp = Inventory.craftGrid[i];
                        Inventory.craftGrid[i] = Inventory.slots[Inventory.movingSlot];
                        Inventory.slots[Inventory.movingSlot] = tmp;
                    }
                    Inventory.movingSlot = -1;
                }
                Inventory.updateCraft();
                return;
            }
        }
        
        int outX = startX + 250, outY = startY + 55;
        if (x >= outX && x <= outX + slotSize && y >= outY && y <= outY + slotSize) {
            Inventory.takeCraftResult(); return;
        }
    }
    
    private boolean handleSettingsUIDown(float x, float y, int W, int H) {
        float scale = getUiScale();
        if (inCircle(x, y, W - 60, 60, 45 * scale)) { settingsOpen = false; return true; }
        
        float sy = y - settingsScrollY;
        float baseY = H / 2 - 250 + 40 * scale;
        
        int btnSize = (int)(110 * scale);
        int gap = (int)(25 * scale);
        int totalW = 5 * btnSize + 4 * gap;
        int startX = (W - totalW) / 2;
        
        for (int i = 0; i < 5; i++) {
            int bx = startX + i * (btnSize + gap);
            if (x >= bx && x <= bx + btnSize && sy >= baseY && sy <= baseY + btnSize) {
                if (renderer.currentWorld != i + 1) { renderer.requestSwitchWorld(i + 1); }
                return true;
            }
        }
        baseY += btnSize + 40 * scale;
        
        float commonW = 400 * scale;
        float commonH = 80 * scale;
        float commonX = W/2f - commonW/2f;
        
        if (x > commonX && x < commonX + commonW && sy > baseY && sy < baseY + commonH) { showNicknameDialog(); return true; }
        baseY += commonH + 20 * scale;
        if (x > commonX && x < commonX + commonW && sy > baseY && sy < baseY + commonH) { leftHanded = !leftHanded; return true; }
        baseY += commonH + 20 * scale;
        if (x > commonX && x < commonX + commonW && sy > baseY && sy < baseY + commonH) { uiSizeMode = (uiSizeMode + 1) % 3; return true; }
        baseY += commonH + 20 * scale;
        if (x > commonX && x < commonX + commonW && sy > baseY && sy < baseY + commonH) {
            Intent intent = new Intent(Intent.ACTION_PICK); intent.setType("image/*");
            startActivityForResult(intent, 100); return true;
        }
        baseY += commonH + 40 * scale;
        
        float netW = 200 * scale;
        float netX1 = W/2f - netW - 10 * scale;
        float netX2 = W/2f + 10 * scale;
        
        if (x > netX1 && x < netX1 + netW && sy > baseY && sy < baseY + commonH) { renderer.network.startServer(renderer.world); showServerIpDialog(); return true; }
        if (x > netX2 && x < netX2 + netW && sy > baseY && sy < baseY + commonH) { showIpDialog(); return true; }
        baseY += commonH + 20 * scale;
        
        if (x > commonX && x < commonX + commonW && sy > baseY && sy < baseY + commonH) { 
            renderer.requestDeleteCurrent(); 
            android.widget.Toast.makeText(this, "Мир удален!", android.widget.Toast.LENGTH_SHORT).show();
            settingsOpen = false;
            return true; 
        }
        
        return false;
    }
    
    private boolean handleUIDown(float x, float y, int W, int H) {
        float scale = getUiScale();
        if (inCircle(x, y, W - 320 * scale, 60, 45 * scale)) { 
            chatInput.setVisibility(View.VISIBLE); chatInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(chatInput, InputMethodManager.SHOW_IMPLICIT);
            return true; 
        }
        if (inCircle(x, y, W - 230 * scale, 60, 45 * scale)) { renderer.player.thirdPerson = !renderer.player.thirdPerson; return true; }
        if (inCircle(x, y, W - 140 * scale, 60, 45 * scale)) { inventoryOpen = true; Inventory.movingSlot = -1; return true; }
        if (inCircle(x, y, W - 50 * scale, 60, 45 * scale)) { settingsOpen = true; settingsScrollY = 0; return true; }
        
        int slotSize = (int)(110 * scale), gap = (int)(10 * scale);
        int startX = (W - (9 * slotSize + 8 * gap)) / 2, startY = (int)(H - 140 * scale);
        for (int i = 0; i < 9; i++) {
            int sx = startX + i * (slotSize + gap);
            if (x >= sx && x <= sx + slotSize && y >= startY && y <= startY + slotSize) { 
                if (Inventory.selectedSlot == i && Block.isFood(Inventory.slots[i].type)) {
                    renderer.player.hunger += 6;
                    if (renderer.player.hunger > 20) renderer.player.hunger = 20;
                    Inventory.removeSelected();
                } else { Inventory.selectSlot(i); }
                return true; 
            }
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
            
            c.drawCircle(W - 320 * scale, 60, 45 * scale, bg); c.drawCircle(W - 320 * scale, 60, 45 * scale, stroke); 
            text.setTextSize(45f * scale); c.drawText("💬", W - 320 * scale, 60 + 15 * scale, text);
            c.drawCircle(W - 230 * scale, 60, 45 * scale, bg); c.drawCircle(W - 230 * scale, 60, 45 * scale, stroke); 
            c.drawText(renderer.player.thirdPerson ? "3D" : "1D", W - 230 * scale, 60 + 15 * scale, text);
            c.drawCircle(W - 140 * scale, 60, 45 * scale, bg); c.drawCircle(W - 140 * scale, 60, 45 * scale, stroke); c.drawText("🎒", W - 140 * scale, 60 + 15 * scale, text);
            c.drawCircle(W - 50 * scale, 60, 45 * scale, bg); c.drawCircle(W - 50 * scale, 60, 45 * scale, stroke); c.drawText("⚙", W - 50 * scale, 60 + 15 * scale, text);
            
            Paint listBg = new Paint(); listBg.setColor(Color.argb(150, 0, 0, 0));
            text.setTextAlign(Paint.Align.LEFT); text.setTextSize(35f * scale);
            int listY = 50;
            synchronized(renderer.network.playerNames) {
                int displayCount = Math.min(renderer.network.playerNames.size(), 5);
                c.drawRect(20, listY - 30, 20 + text.measureText("Игроки:") + 40, listY + (displayCount * 45f * scale) + 10, listBg);
                c.drawText("Игроки:", 30, listY, text);
                listY += 45 * scale;
                for (int i = 0; i < displayCount; i++) {
                    c.drawText("- " + renderer.network.playerNames.get(i), 30, listY, text);
                    listY += 45 * scale;
                }
            }
            
            Paint chatBg = new Paint(); chatBg.setColor(Color.argb(120, 0, 0, 0));
            text.setTextSize(35f * scale);
            int msgY = Math.max(listY + 50, 200 * (int)scale);
            long now = System.currentTimeMillis();
            synchronized (renderer.network.chatMessages) {
                for (int i = 0; i < renderer.network.chatMessages.size(); i++) {
                    NetworkManager.ChatMessage msg = renderer.network.chatMessages.get(i);
                    if (now - msg.time < 15000) {
                        c.drawRect(20, msgY - 35*scale, 20 + text.measureText(msg.text) + 20, msgY + 10, chatBg);
                        c.drawText(msg.text, 30, msgY, text);
                        msgY += 45 * scale;
                    }
                }
            }
            
            float joyCX = leftHanded ? W - 160 * scale : 160 * scale;
            float joyCY = H - 160 * scale;
            RectF dpOuter = new RectF(joyCX - moveRadius*scale, joyCY - moveRadius*scale, joyCX + moveRadius*scale, joyCY + moveRadius*scale);
            c.drawRect(dpOuter, fill); c.drawRect(dpOuter, stroke);
            if (movePointerId != -1) { c.drawCircle(knobX, knobY, 50f * scale, bg); c.drawCircle(knobX, knobY, 50f * scale, stroke); }
            else { c.drawRect(joyCX - 40*scale, joyCY - 40*scale, joyCX + 40*scale, joyCY + 40*scale, stroke); }
            
            float bSize = 120 * scale, gap = 15 * scale;
            float btnStartX = leftHanded ? 20 * scale : W - (bSize * 2 + gap) - 20 * scale;
            float btnStartY = H - (bSize * 3 + gap * 2) - 200 * scale;
            float cx1 = btnStartX, cx2 = btnStartX + bSize + gap;
            float cy1 = btnStartY, cy2 = btnStartY + bSize + gap, cy3 = btnStartY + (bSize + gap) * 2;
            
            drawPEBtn(c, cx1, cy1, bSize, "⚔", bSwordId != -1, 100); 
            drawPEBtn(c, cx2, cy1, bSize, "↑", bJumpId != -1, 100);
            drawPEBtn(c, cx1, cy2, bSize, "»", bSprintId != -1, 100); 
            drawPEBtn(c, cx2, cy2, bSize, "↩️", bDropId != -1, 50); 
            drawPEBtn(c, cx1, cy3, bSize, "+", bPlaceId != -1, 100); 
            drawPEBtn(c, cx2, cy3, bSize, "↓", bSneakId != -1, 100); 
            
            float barY = H - 185 * scale;
            float iconSize = 45f * scale;
            text.setTextSize(iconSize); text.setTextAlign(Paint.Align.CENTER);
            
            int hp = (int)Math.ceil(renderer.player.health / 2.0f); 
            int food = (int)Math.ceil(renderer.player.hunger / 2.0f); 
            float hpStartX = W/2f - 20 * scale - (10 * iconSize);
            
            for (int i = 0; i < 10; i++) c.drawText(i < hp ? "❤️" : "🖤", hpStartX + i * iconSize + iconSize/2, barY, text);
            float foodStartX = W/2f + 20 * scale;
            for (int i = 0; i < 10; i++) c.drawText(i < food ? "🍗" : "🍖", foodStartX + i * iconSize + iconSize/2, barY, text);
            
            int slotSize = (int)(110 * scale), hGap = (int)(10 * scale);
            int hotbarStartX = (W - (9 * slotSize + 8 * hGap)) / 2, hotbarStartY = (int)(H - 140 * scale);
            for (int i = 0; i < 9; i++) {
                int sx = hotbarStartX + i * (slotSize + hGap);
                drawSlot(c, sx, hotbarStartY, slotSize, Inventory.slots[i], i == Inventory.selectedSlot, i == Inventory.movingSlot);
            }
            
            if (inventoryOpen) drawInventory(c, W, H);
            if (craftingOpen) drawCrafting(c, W, H);
            if (settingsOpen) drawSettings(c, W, H);
        }
        
        private void drawPEBtn(Canvas c, float x, float y, float size, String label, boolean pressed, int alpha) {
            RectF r = new RectF(x, y, x + size, y + size);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setStyle(Paint.Style.FILL); p.setColor(pressed ? Color.argb(150, 255, 255, 255) : Color.argb(alpha, 50, 50, 50));
            c.drawRect(r, p);
            p.setStyle(Paint.Style.STROKE); p.setColor(Color.WHITE); p.setStrokeWidth(4f); c.drawRect(r, p);
            text.setTextSize(size * 0.6f); text.setTextAlign(Paint.Align.CENTER); c.drawText(label, x + size/2f, y + size/2f + size * 0.2f, text);
        }
        
        private void drawSlot(Canvas c, int sx, int sy, int slotSize, Inventory.Slot slot, boolean isSelected, boolean isMoving) {
        if (isMoving) {
            Paint sel = new Paint(Paint.ANTI_ALIAS_FLAG); sel.setStyle(Paint.Style.STROKE); sel.setStrokeWidth(5f); sel.setColor(Color.YELLOW);
            c.drawRect(sx, sy, sx + slotSize, sy + slotSize, sel);
        }
        
        if (slot.type != Block.AIR) {
            int resId = 0;
            switch(slot.type) {
                case Block.GRASS: resId = com.example.myminecraft.R.drawable.grass_top; break;
                case Block.DIRT: resId = com.example.myminecraft.R.drawable.dirt; break;
                case Block.STONE: resId = com.example.myminecraft.R.drawable.stone; break;
                case Block.WOOD: resId = com.example.myminecraft.R.drawable.wood_side; break;
                case Block.LEAVES: resId = com.example.myminecraft.R.drawable.leaves; break;
                case Block.PLANKS: resId = com.example.myminecraft.R.drawable.planks; break;
                case Block.COBBLESTONE: resId = com.example.myminecraft.R.drawable.cobblestone; break;
                case Block.MEAT: resId = com.example.myminecraft.R.drawable.sheep_face; break;
            }
            if (resId != 0) {
                android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeResource(getResources(), resId);
                if (bmp != null) {
                    android.graphics.Rect src = new android.graphics.Rect(0, 0, bmp.getWidth(), bmp.getHeight());
                    android.graphics.Rect dst = new android.graphics.Rect(sx + 5, sy + 5, sx + slotSize - 5, sy + slotSize - 5);
                    c.drawBitmap(bmp, src, dst, null);
                    bmp.recycle();
                }
            }
            if (slot.count > 1) {
                text.setTextSize(slotSize * 0.3f); text.setTextAlign(Paint.Align.RIGHT);
                c.drawText(String.valueOf(slot.count), sx + slotSize - 5, sy + slotSize - 5, text);
            }
        }
    }
        
        private void drawCrafting(Canvas c, int W, int H) {
            Paint dim = new Paint(); dim.setColor(Color.argb(220, 0, 0, 0)); c.drawRect(0, 0, W, H, dim);
            text.setTextSize(60f); text.setTextAlign(Paint.Align.CENTER); c.drawText("Крафт", W/2f, 100, text);
            c.drawCircle(W - 60, 60, 45, bg); c.drawCircle(W - 60, 60, 45, stroke); c.drawText("X", W - 60, 75, text);
            
            int slotSize = 100, gap = 10;
            int startX = W/2 - 200, startY = H/2 - 100;
            
            for (int i = 0; i < 4; i++) {
                int sx = startX + (i % 2) * (slotSize + gap);
                int sy = startY + (i / 2) * (slotSize + gap);
                drawSlot(c, sx, sy, slotSize, Inventory.craftGrid[i], false, Inventory.movingSlot == 100 + i);
            }
            
            Paint arr = new Paint(Paint.ANTI_ALIAS_FLAG);
            arr.setColor(Color.WHITE); arr.setStrokeWidth(6f); arr.setStyle(Paint.Style.STROKE);
            c.drawLine(startX + 230, startY + 105, startX + 270, startY + 105, arr);
            c.drawLine(startX + 250, startY + 85, startX + 270, startY + 105, arr);
            c.drawLine(startX + 250, startY + 125, startX + 270, startY + 105, arr);
            
            int outX = startX + 250, outY = startY + 55;
            drawSlot(c, outX, outY, slotSize, Inventory.craftOutput, false, false);
            
            text.setTextSize(35f); text.setTextAlign(Paint.Align.CENTER); text.setColor(Color.LTGRAY);
            c.drawText("Рецепт: 1 дерево = 4 доски", W/2f, H/2 + 150, text);
            text.setColor(Color.WHITE);
        }
        
        private void drawInventory(Canvas c, int W, int H) {
        Paint dim = new Paint(); dim.setColor(Color.argb(180, 0, 0, 0)); c.drawRect(0, 0, W, H, dim);
        
        if (bgBitmap != null) {
            float scale = Math.min((float) W / bgBitmap.getWidth(), (float) H / bgBitmap.getHeight()) * 0.9f;
            int bgW = (int)(bgBitmap.getWidth() * scale);
            int bgH = (int)(bgBitmap.getHeight() * scale);
            int left = (W - bgW) / 2, top = (H - bgH) / 2;
            
            android.graphics.Rect dst = new android.graphics.Rect(left, top, left + bgW, top + bgH);
            c.drawBitmap(bgBitmap, null, dst, null);
            
            text.setTextSize(60f); text.setTextAlign(Paint.Align.CENTER);
            Paint p = new Paint(); p.setColor(Color.WHITE); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(6f);
            c.drawCircle(left + bgW - 40, top + 40, 30, p);
            c.drawText("X", left + bgW - 40, top + 62, text);
            
            android.graphics.Bitmap skin = android.graphics.BitmapFactory.decodeResource(getResources(), com.example.myminecraft.R.drawable.skin);
            if (skin != null) {
                android.graphics.Rect srcFace = new android.graphics.Rect(8, 8, 16, 16);
                android.graphics.Rect dstFace = new android.graphics.Rect(left + (int)(bgW * 0.12f), top + (int)(bgH * 0.08f), left + (int)(bgW * 0.24f), top + (int)(bgH * 0.28f));
                c.drawBitmap(skin, srcFace, dstFace, null);
                skin.recycle();
            }

            int slotSize = (int)(bgW * 0.09f);
            int gap = (int)(bgW * 0.005f);
            
            int invStartX = left + (int)(bgW * 0.02f);
            int invStartY = top + (int)(bgH * 0.48f);
            for (int row = 0; row < 3; row++) {
                for (int i = 0; i < 9; i++) {
                    int sx = invStartX + i * (slotSize + gap);
                    int sy = invStartY + row * (slotSize + gap);
                    drawSlot(c, sx, sy, slotSize, Inventory.slots[9 + row * 9 + i], false, 9 + row * 9 + i == Inventory.movingSlot);
                }
            }
            
            int hotbarY = top + (int)(bgH * 0.85f);
            for (int i = 0; i < 9; i++) {
                int sx = invStartX + i * (slotSize + gap);
                drawSlot(c, sx, hotbarY, slotSize, Inventory.slots[i], i == Inventory.selectedSlot, i == Inventory.movingSlot);
            }
            
            int craftX = left + (int)(bgW * 0.56f);
            int craftY = top + (int)(bgH * 0.08f);
            for (int i = 0; i < 4; i++) {
                int sx = craftX + (i % 2) * (slotSize + gap);
                int sy = craftY + (i / 2) * (slotSize + gap);
                drawSlot(c, sx, sy, slotSize, Inventory.craftGrid[i], false, i + 36 == Inventory.movingSlot);
            }
            
            int outX = left + (int)(bgW * 0.82f);
            int outY = top + (int)(bgH * 0.13f);
            drawSlot(c, outX, outY, slotSize, Inventory.craftOutput, false, 40 == Inventory.movingSlot);
        }
    }
        
        private void drawSettings(Canvas c, int W, int H) {
            Paint dim = new Paint(); dim.setColor(Color.argb(220, 0, 0, 0)); c.drawRect(0, 0, W, H, dim);
            text.setTextSize(60f); text.setTextAlign(Paint.Align.CENTER); c.drawText("Настройки", W/2f, 80, text);
            c.drawCircle(W - 60, 60, 45, bg); c.drawCircle(W - 60, 60, 45, stroke); c.drawText("X", W - 60, 75, text);
            
            float scale = getUiScale();
            float baseY = H / 2 - 250 + settingsScrollY;
            float btnY = baseY;
            
            text.setTextSize(40f); c.drawText("Выбор мира:", W/2f, btnY, text);
            btnY += 40 * scale;
            
            int btnSize = (int)(110 * scale);
            int gap = (int)(25 * scale);
            int totalW = 5 * btnSize + 4 * gap;
            int startX = (W - totalW) / 2;
            for (int i = 0; i < 5; i++) {
                int bx = startX + i * (btnSize + gap);
                boolean exists = renderer.worldExists(i + 1);
                boolean isCur = renderer.currentWorld == (i + 1);
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setStyle(Paint.Style.FILL);
                p.setColor(isCur ? Color.rgb(200, 160, 40) : (exists ? Color.rgb(80, 130, 200) : Color.rgb(70, 70, 70)));
                c.drawRect(bx, btnY, bx + btnSize, btnY + btnSize, p);
                c.drawRect(bx, btnY, bx + btnSize, btnY + btnSize, stroke);
                text.setTextSize(60f); c.drawText(String.valueOf(i + 1), bx + btnSize/2f, btnY + btnSize/2f + 20, text);
            }
            btnY += btnSize + 40 * scale;
            
            Paint btnBg = new Paint(); btnBg.setColor(Color.rgb(80, 130, 200));
            float commonW = 400 * scale;
            float commonH = 80 * scale;
            float commonX = W/2f - commonW/2f;
            
            c.drawRect(commonX, btnY, commonX + commonW, btnY + commonH, btnBg); c.drawRect(commonX, btnY, commonX + commonW, btnY + commonH, stroke);
            text.setTextSize(40f); c.drawText("Сменить ник", W/2f, btnY + commonH/2f + 15 * scale, text);
            btnY += commonH + 20 * scale;
            
            text.setTextSize(36f); c.drawText("Раскладка", W/2f, btnY + 25 * scale, text);
            c.drawRect(commonX, btnY, commonX + commonW, btnY + commonH, btnBg); c.drawRect(commonX, btnY, commonX + commonW, btnY + commonH, stroke);
            text.setTextSize(40f); c.drawText(leftHanded ? "Левша" : "Правша", W/2f, btnY + commonH/2f + 15 * scale, text);
            btnY += commonH + 20 * scale;
            
            text.setTextSize(36f); c.drawText("Размер", W/2f, btnY + 25 * scale, text);
            c.drawRect(commonX, btnY, commonX + commonW, btnY + commonH, btnBg); c.drawRect(commonX, btnY, commonX + commonW, btnY + commonH, stroke);
            text.setTextSize(40f); c.drawText(uiSizeMode == 0 ? "Маленький" : (uiSizeMode == 2 ? "Большой" : "Средний"), W/2f, btnY + commonH/2f + 15 * scale, text);
            btnY += commonH + 20 * scale;
            
            text.setTextSize(36f); c.drawText("Скин", W/2f, btnY + 25 * scale, text);
            c.drawRect(commonX, btnY, commonX + commonW, btnY + commonH, btnBg); c.drawRect(commonX, btnY, commonX + commonW, btnY + commonH, stroke);
            text.setTextSize(40f); c.drawText("Загрузить", W/2f, btnY + commonH/2f + 15 * scale, text);
            btnY += commonH + 40 * scale;
            
            float netW = 200 * scale;
            float netX1 = W/2f - netW - 10 * scale;
            float netX2 = W/2f + 10 * scale;
            
            Paint netBtn = new Paint(); netBtn.setColor(Color.rgb(60, 180, 60));
            c.drawRect(netX1, btnY, netX1 + netW, btnY + commonH, netBtn); c.drawRect(netX1, btnY, netX1 + netW, btnY + commonH, stroke);
            text.setTextSize(36f); c.drawText("Открыть мир", netX1 + netW/2f, btnY + commonH/2f + 15 * scale, text);
            
            netBtn.setColor(Color.rgb(180, 60, 60));
            c.drawRect(netX2, btnY, netX2 + netW, btnY + commonH, netBtn); c.drawRect(netX2, btnY, netX2 + netW, btnY + commonH, stroke);
            c.drawText("Подключиться", netX2 + netW/2f, btnY + commonH/2f + 15 * scale, text);
            
            btnY += commonH + 20 * scale;
            
            Paint delBtn = new Paint(); delBtn.setColor(Color.rgb(200, 50, 50));
            c.drawRect(commonX, btnY, commonX + commonW, btnY + commonH, delBtn); 
            c.drawRect(commonX, btnY, commonX + commonW, btnY + commonH, stroke);
            text.setTextSize(40f); c.drawText("Удалить мир", W/2f, btnY + commonH/2f + 15 * scale, text);
            
            if (settingsScrollY < 0) {
                text.setTextSize(30f); text.setTextAlign(Paint.Align.CENTER); text.setColor(Color.LTGRAY);
                c.drawText("↑ Прокрутите вниз ↑", W/2f, H - 40, text);
                text.setColor(Color.WHITE);
            }
        }
    }
    
    private String getLocalIp() {
        try {
            android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
            if (ipAddress != 0) {
                return String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
            }
        } catch (Exception e) {}
        return "Нет Wi-Fi";
    }
    
    @Override protected void onPause() { super.onPause(); if (glView != null) glView.onPause(); if (renderer != null) { renderer.saveNow(); Inventory.saveToFile(this); } }
    @Override protected void onResume() { super.onResume(); if (glView != null) glView.onResume(); }
}
