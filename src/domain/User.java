package domain;

import dto.request.LoginRequest;
import java.net.Socket;
import java.util.Date;

public class User {

    private Socket socket; // active socket for the connected client
    private String pw;
    private String id;
    private String nickname;
    private Date createdAt;
    private String role = "USER";
    private boolean online;
    private boolean banned;

    public User() {
        this.createdAt = new Date();
    }

    public User(String id, String nickname) {
        this(id, nickname, "USER", false, false);
    }

    public User(String id, String nickname, String role, boolean online, boolean banned) {
        this.id = id;
        this.nickname = nickname;
        this.role = role;
        this.online = online;
        this.banned = banned;
        this.createdAt = new Date();
    }

    public User(Socket socket, LoginRequest req) {
        this.socket = socket;
        this.id = req.getId();
        this.pw = req.getPw();
        this.nickname = req.getNickname();
        this.createdAt = new Date();
    }

    public String getId() { return id; }
    public String getPw() { return pw; }
    public String getNickName() { return nickname; }
    public Date getCreatedAt() { return createdAt; }
    public Socket getSocket() { return socket; }
    public String getRole() { return role; }
    public boolean isOnline() { return online; }
    public boolean isBanned() { return banned; }

    public void setId(String id) { this.id = id; }
    public void setPw(String pw) { this.pw = pw; }
    public void setNickName(String nickname) { this.nickname = nickname; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setSocket(Socket socket) { this.socket = socket; }
    public void setRole(String role) { this.role = role; }
    public void setOnline(boolean online) { this.online = online; }
    public void setBanned(boolean banned) { this.banned = banned; }

    public String getEnterString() {
        return "[" + nickname + "] entered the room.";
    }

    public String getExitString() {
        return "[" + nickname + "] left the room.";
    }
}
