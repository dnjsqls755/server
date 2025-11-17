package dao;

import domain.ChatRoom;
import domain.User;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
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
    //채팅방 생성 및 DB저장
    public void addChatRoom(ChatRoom chatRoom) {
        chatRooms.add(chatRoom);
        String sql = "INSERT INTO ChatRooms (room_id, room_name, creator_id) VALUES (chatroom_seq.NEXTVAL, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, chatRoom.getName());
            pstmt.setString(2, chatRoom.getCreatorId()); 
            pstmt.executeUpdate();
            System.out.println("채팅방 [" + chatRoom.getName() + "] DB 저장 완료");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }              
    
    public Optional<User> findUserById(String id) {
        return users.stream().filter(user -> user.getId().equals(id)).findAny();
        // TODO: DB에서 사용자 조회하는 코드로 변경 가능
    }
    //로비제외 모든 채팅방 조회
    public List<ChatRoom> findAllChatRooms() {
        List<ChatRoom> rooms = new ArrayList<>();
        String sql = "SELECT room_name, creator_id FROM ChatRooms WHERE room_name <> 'Lobby'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                rooms.add(new ChatRoom(rs.getString("room_name"), rs.getString("creator_id")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }
    //채팅방 이름으로 채팅방 조회
    public Optional<ChatRoom> findChatRoomByName(String name) {
        String sql = "SELECT room_name, creator_id FROM ChatRooms WHERE room_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String roomName = rs.getString("room_name");
                String creatorId = rs.getString("creator_id");
                return Optional.of(new ChatRoom(roomName, creatorId));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
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

