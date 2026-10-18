package com.example.myminecraft;

public class RemotePlayer {
    public String name;
    public float x, y, z, yaw, pitch;
    
    public RemotePlayer(String name) {
        this.name = name;
        this.x = 64; this.y = 25; this.z = 64;
        this.yaw = 0; this.pitch = 0;
    }
}
