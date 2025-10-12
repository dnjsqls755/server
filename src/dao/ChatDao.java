package dao;

import domain.ChatRoom;
import domain.User;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChatDao {

    private List<User> users = new ArrayList<>(); // 접속 중인 모든 사용자 리스트
    private List<ChatRoom> chatRooms = new ArrayList<>();
    private ChatRoom lobby; // 로비 채팅방 정보

    public final static String LOBBY_CHAT_NAME = "Lobby";

    private Connection connection; // Oracle DB 연결 객체

    // 생성자에서 DB 연결 객체를 받음
    public ChatDao(Connection connection) {
        this.connection = connection;

        // 로비 채팅방 기본 생성
        lobby = new ChatRoom(LOBBY_CHAT_NAME);
    }

    public void addUser(User user) {
        users.add(user);
        // TODO: DB에 사용자 정보 저장하는 코드 추가 가능
    }

    public void addChatRoom(ChatRoom chatRoom) {
        chatRooms.add(chatRoom);
        // TODO: DB에 채팅방 정보 저장하는 코드 추가 가능
    }

    public Optional<User> findUserById(String id) {
        return users.stream().filter(user -> user.getId().equals(id)).findAny();
        // TODO: DB에서 사용자 조회하는 코드로 변경 가능
    }

    public Optional<ChatRoom> findChatRoomByName(String name) {
        return chatRooms.stream()
                .filter(chatRoom -> chatRoom.getName().equals(name))
                .findAny();
        // TODO: DB에서 채팅방 조회하는 코드로 변경 가능
    }

    public ChatRoom getLobby() {
        return lobby;
    }

    public List<User> getUsers() {
        return users;
    }

    public Optional<User> getUser(String userId) {
        return users.stream().filter(user -> user.getId().equals(userId)).findAny();
    }

    public List<ChatRoom> getChatRooms() {
        return chatRooms;
    }

    public Connection getConnection() {
        return connection;
    }
}

