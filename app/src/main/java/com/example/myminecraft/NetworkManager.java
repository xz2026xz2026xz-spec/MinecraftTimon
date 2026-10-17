package com.example.myminecraft;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class NetworkManager {
    public boolean isServer = false;
    public boolean isConnected = false;
    public String serverIp = "127.0.0.1";
    public int port = 8080;
    
    public String myName = "Player_" + (int)(Math.random() * 9000 + 1000);
    public List<String> playerNames = new ArrayList<>();
    
    public static class ChatMessage {
        public String text;
        public long time;
        public ChatMessage(String t) { text = t; time = System.currentTimeMillis(); }
    }
    
    public List<ChatMessage> chatMessages = new ArrayList<>();
    private List<DataOutputStream> clientOutputs = new ArrayList<>();
    private Socket socket;
    private DataOutputStream out;
    
    public void startServer(final World world) {
        isServer = true;
        synchronized(playerNames) { playerNames.add(myName + " (Хост)"); }
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(port);
                    isConnected = true;
                    while (true) {
                        Socket client = serverSocket.accept();
                        DataOutputStream clientOut = new DataOutputStream(client.getOutputStream());
                        synchronized (clientOutputs) { clientOutputs.add(clientOut); }
                        new Thread(new ClientHandler(client, clientOut)).start();
                    }
                } catch (Exception e) { isConnected = false; }
            }
        }).start();
    }
    
    public void connect(String ip) {
        this.serverIp = ip;
        isServer = false;
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    socket = new Socket(serverIp, port);
                    out = new DataOutputStream(socket.getOutputStream());
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    isConnected = true;
                    
                    out.writeInt(3); out.writeUTF(myName);
                    
                    while (isConnected) {
                        int type = in.readInt();
                        if (type == 2) {
                            String msg = in.readUTF();
                            addChatMessage(msg);
                        } else if (type == 4) {
                            String listStr = in.readUTF();
                            synchronized(playerNames) {
                                playerNames.clear();
                                if (!listStr.isEmpty()) playerNames.addAll(java.util.Arrays.asList(listStr.split(",")));
                            }
                        }
                    }
                } catch (Exception e) { isConnected = false; }
            }
        }).start();
    }
    
    private void addChatMessage(String msg) {
        synchronized (chatMessages) {
            chatMessages.add(new ChatMessage(msg));
            if (chatMessages.size() > 20) chatMessages.remove(0);
        }
    }
    
    public void sendChat(String msg) {
        addChatMessage(myName + ": " + msg);
        if (!isConnected || out == null) return;
        try { out.writeInt(2); out.writeUTF(msg); } catch (Exception e) {}
    }
    
    private class ClientHandler implements Runnable {
        private Socket client;
        private DataOutputStream cout;
        public ClientHandler(Socket s, DataOutputStream out) { client = s; cout = out; }
        @Override public void run() {
            try {
                DataInputStream cin = new DataInputStream(client.getInputStream());
                while (isConnected) {
                    int type = cin.readInt();
                    if (type == 2) {
                        String msg = cin.readUTF();
                        broadcastChat(msg);
                    } else if (type == 3) {
                        String name = cin.readUTF();
                        synchronized(playerNames) {
                            if (!playerNames.contains(name)) playerNames.add(name);
                        }
                        broadcastPlayerList();
                    }
                }
            } catch (Exception e) {}
        }
    }
    
    private void broadcastChat(String msg) {
        addChatMessage(msg);
        synchronized (clientOutputs) {
            for (DataOutputStream o : clientOutputs) {
                try { o.writeInt(2); o.writeUTF(msg); } catch (Exception e) {}
            }
        }
    }
    
    private void broadcastPlayerList() {
        StringBuilder sb = new StringBuilder();
        synchronized(playerNames) {
            for (String n : playerNames) sb.append(n).append(",");
        }
        synchronized (clientOutputs) {
            for (DataOutputStream o : clientOutputs) {
                try { o.writeInt(4); o.writeUTF(sb.toString()); } catch (Exception e) {}
            }
        }
    }
}
