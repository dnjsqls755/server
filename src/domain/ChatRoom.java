package domain;

import java.util.ArrayList;
import java.util.List;

public class ChatRoom {

    String name; // 채팅방 이름
    String creatorId;
    List<User> users;
    int memberCount;

    public ChatRoom(String name) {
        this.name = name;
        this.users = new ArrayList<>();
        this.memberCount = 0;
    }

    public ChatRoom(String name, String creatorId) {
        this.name = name;
        this.creatorId = creatorId;
        this.users = new ArrayList<>();
        this.memberCount = 0;
    }

    public ChatRoom(String name, String creatorId, int memberCount) {
        this.name = name;
        this.creatorId = creatorId;
        this.memberCount = memberCount;
        this.users = new ArrayList<>();
    }

    public void addUser(User user) {
        users.add(user);
    }

    public void removeUser(User user) {
        try {
            users.remove(user);
        }
        catch (Exception e) {
            System.out.println("user = [" + user + "] is not exist in chat room = [" + name + "]");
            System.out.println(e.getMessage());
        }
    }

    public String getName() {
        return name;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public List<User> getUsers() {
        return users;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public boolean ieExistUser() {
        return users.size() != 0;
    }
}
