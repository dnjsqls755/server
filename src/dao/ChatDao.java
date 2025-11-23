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
    }
    
    public void removeUser(String userId) {
        users.removeIf(user -> user.getId().equals(userId));
    }
    
    // TODO: 아래 채팅방 관련 메서드들은 필요시 구현
    
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
    public List<ChatRoom> findAllChatRoomsExceptLobby() {
        List<ChatRoom> rooms = new ArrayList<>();
        String sql = "SELECT room_name, creator_id FROM ChatRooms WHERE room_name != ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ChatDao.LOBBY_CHAT_NAME);
            ResultSet rs = pstmt.executeQuery();
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
        // 1차: 메모리에서 먼저 검색 (사용자 목록 등 상태 유지)
        for (ChatRoom room : chatRooms) {
            if (room.getName().equals(name)) {
                return Optional.of(room);
            }
        }

        // 2차: DB에서 조회 후 메모리에 등록
        String sql = "SELECT room_name, creator_id FROM ChatRooms WHERE room_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String roomName = rs.getString("room_name");
                String creatorId = rs.getString("creator_id");
                ChatRoom newRoom = new ChatRoom(roomName, creatorId);
                chatRooms.add(newRoom); // 메모리에 캐싱
                return Optional.of(newRoom);
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

    public Optional<User> findUserByNickname(String nickname) {
        return users.stream()
                .filter(user -> user.getNickName().equals(nickname))
                .findAny();
    }

    public List<ChatRoom> getChatRooms() {
        return chatRooms;
    }

    public Connection getConnection() {
        return connection;
    }

    public void refreshChatRoomsFromDb() {
        List<ChatRoom> rooms = findAllChatRoomsExceptLobby();
        chatRooms.clear();
        chatRooms.addAll(rooms);
    }

    public void saveMessage(String chatRoomName, String senderId, String content) {
        String sql = "INSERT INTO Messages (message_id, room_id, sender_id, content) " +
                "VALUES (messages_seq.NEXTVAL, (SELECT room_id FROM ChatRooms WHERE room_name = ?), ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, chatRoomName);
            pstmt.setString(2, senderId);
            pstmt.setString(3, content);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 채팅 메시지 내부 클래스
    public static class ChatMessage {
        private String nickname;
        private String content;
        private String sentAt;

        public ChatMessage(String nickname, String content, String sentAt) {
            this.nickname = nickname;
            this.content = content;
            this.sentAt = sentAt;
        }

        public String getNickname() { return nickname; }
        public String getContent() { return content; }
        public String getSentAt() { return sentAt; }
    }

    // 채팅방의 이전 메시지 불러오기 (시간 포함)
    public List<ChatMessage> loadMessages(String roomName, int limit) {
        List<ChatMessage> messages = new ArrayList<>();
        String sql = "SELECT u.nickname, m.content, TO_CHAR(m.sent_at, 'HH24:MI') as sent_time " +
                     "FROM Messages m " +
                     "JOIN ChatRooms r ON m.room_id = r.room_id " +
                     "JOIN users u ON m.sender_id = u.user_id " +
                     "WHERE r.room_name = ? " +
                     "ORDER BY m.sent_at ASC " +
                     "FETCH FIRST ? ROWS ONLY";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, roomName);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                messages.add(0, new ChatMessage(
                    rs.getString("nickname"),
                    rs.getString("content"),
                    rs.getString("sent_time")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return messages;
    }

    // 채팅방에 사용자 추가 (DB) - 영향받은 행 수 반환
    public int addUserToChatRoom(String roomName, String userId) {
        String sql = "INSERT INTO ChatRoomUsers (room_id, user_id) " +
                     "SELECT r.room_id, ? FROM ChatRooms r " +
                     "WHERE r.room_name = ? " +
                     "AND NOT EXISTS (SELECT 1 FROM ChatRoomUsers cu WHERE cu.room_id = r.room_id AND cu.user_id = ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, roomName);
            pstmt.setString(3, userId);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // 채팅방에서 사용자 제거 (DB)
    public void removeUserFromChatRoom(String roomName, String userId) {
        String sql = "DELETE FROM ChatRoomUsers " +
                     "WHERE room_id = (SELECT room_id FROM ChatRooms WHERE room_name = ?) " +
                     "AND user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, roomName);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 닉네임으로 사용자 ID 찾기 (DB)
    public String findUserIdByNickname(String nickname) {
        String sql = "SELECT user_id FROM users WHERE nickname = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nickname);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("user_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}

